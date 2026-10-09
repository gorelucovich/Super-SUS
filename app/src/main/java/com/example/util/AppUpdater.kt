package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object AppUpdater {

    private const val RAW_GITHUB_README = "https://raw.githubusercontent.com/gorelucovich/Super-SUS/main/README.md"
    private const val WEB_GITHUB_README = "https://github.com/gorelucovich/Super-SUS/blob/main/README.md"

    /**
     * Checks the GitHub README file for the latest APK download URL.
     */
    suspend fun fetchLatestApkUrl(): String = withContext(Dispatchers.IO) {
        AppLogger.i("AppUpdater", "Проверка обновлений в GitHub: $RAW_GITHUB_README")

        var content = tryFetchUrl(RAW_GITHUB_README)
        if (content.isBlank()) {
            AppLogger.w("AppUpdater", "Не удалось загрузить raw README, пробуем web-версию")
            content = tryFetchUrl(WEB_GITHUB_README)
        }

        if (content.isBlank()) {
            throw Exception("Не удалось загрузить README.md с GitHub. Проверьте интернет-соединение.")
        }

        // 1. Look for explicit marker: LATEST_APK_URL=https://...
        val markerPattern = Pattern.compile("LATEST_APK_URL=([^\r\n\\s]+)", Pattern.CASE_INSENSITIVE)
        val markerMatcher = markerPattern.matcher(content)
        if (markerMatcher.find()) {
            val url = markerMatcher.group(1)?.trim() ?: ""
            if (url.startsWith("http")) {
                AppLogger.i("AppUpdater", "Найдена ссылка по маркеру: $url")
                return@withContext url
            }
        }

        // 2. Look for any markdown link pointing to .apk
        val mdPattern = Pattern.compile("\\[[^\\]]*\\]\\((https?://[^\\s\\)]+\\.apk[^\\s\\)]*)\\)", Pattern.CASE_INSENSITIVE)
        val mdMatcher = mdPattern.matcher(content)
        if (mdMatcher.find()) {
            val url = mdMatcher.group(1)?.trim() ?: ""
            AppLogger.i("AppUpdater", "Найдена ссылка из Markdown: $url")
            return@withContext url
        }

        // 3. Fallback: any direct .apk URL in text
        val anyApkPattern = Pattern.compile("(https?://[^\\s\"'<>]+\\.apk[^\\s\"'<>]*)", Pattern.CASE_INSENSITIVE)
        val anyMatcher = anyApkPattern.matcher(content)
        if (anyMatcher.find()) {
            val url = anyMatcher.group(1)?.trim() ?: ""
            AppLogger.i("AppUpdater", "Найдена ссылка на .apk в тексте: $url")
            return@withContext url
        }

        throw Exception("В README.md не найдено ссылки на скачивание .apk")
    }

    private fun tryFetchUrl(urlStr: String): String {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SusRadar-Updater/1.0")

            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
        } catch (e: Exception) {
            AppLogger.w("AppUpdater", "Ошибка запроса к $urlStr: ${e.message}")
            ""
        }
    }

    /**
     * Downloads APK with progress (0..100) and triggers Android package installer.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        AppLogger.i("AppUpdater", "Начало загрузки обновления: $apkUrl")

        val targetFile = File(context.cacheDir, "SusRadar_Update.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val url = URL(apkUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 25000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "SusRadar-Updater/1.0")
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode !in 200..299 && responseCode != HttpURLConnection.HTTP_MOVED_TEMP && responseCode != HttpURLConnection.HTTP_MOVED_PERM) {
            throw Exception("Ошибка загрузки файла, код сервера: $responseCode")
        }

        val fileLength = connection.contentLength
        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            input = connection.inputStream
            output = FileOutputStream(targetFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }
            output.flush()
        } finally {
            output?.close()
            input?.close()
            connection.disconnect()
        }

        AppLogger.i("AppUpdater", "Загрузка завершена! Размер: ${targetFile.length()} байт. Запуск установщика.")

        withContext(Dispatchers.Main) {
            installApkFile(context, targetFile)
        }
    }

    private fun installApkFile(context: Context, apkFile: File) {
        try {
            // Check Android 8.0+ install unknown apps permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    AppLogger.i("AppUpdater", "Запрос разрешения на установку неизвестных приложений")
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            AppLogger.i("AppUpdater", "Системный установщик успешно запущен")
        } catch (e: Exception) {
            AppLogger.e("AppUpdater", "Ошибка запуска установщика", e)
        }
    }
}
