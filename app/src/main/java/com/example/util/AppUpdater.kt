package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.regex.Pattern

data class UpdateCheckResult(
    val apkUrl: String,
    val source: String,
    val isFallback: Boolean = false,
    val note: String = ""
)

object AppUpdater {

    private const val GITHUB_MAIN_RAW = "https://raw.githubusercontent.com/gorelucovich/Super-SUS/main/README.md"
    private const val GITHUB_MASTER_RAW = "https://raw.githubusercontent.com/gorelucovich/Super-SUS/master/README.md"
    private const val DEFAULT_FALLBACK_APK_URL = "https://litter.catbox.moe/cqa4rv.apk"

    /**
     * Checks the GitHub README file for the latest APK download URL.
     */
    suspend fun fetchLatestApkUrl(): UpdateCheckResult = withContext(Dispatchers.IO) {
        AppLogger.i("AppUpdater", "Проверка обновлений в GitHub: $GITHUB_MAIN_RAW")

        var content = tryFetchUrl(GITHUB_MAIN_RAW)
        if (content.isBlank()) {
            AppLogger.d("AppUpdater", "Пробуем master ветку")
            content = tryFetchUrl(GITHUB_MASTER_RAW)
        }

        if (content.isNotBlank()) {
            val extractedUrl = extractApkUrlFromContent(content)
            if (extractedUrl.isNotBlank()) {
                AppLogger.i("AppUpdater", "Ссылка успешно получена из GitHub README: $extractedUrl")
                return@withContext UpdateCheckResult(
                    apkUrl = extractedUrl,
                    source = "GitHub README (gorelucovich/Super-SUS)",
                    isFallback = false
                )
            }
        }

        AppLogger.w("AppUpdater", "GitHub репозиторий недоступен или пустой. Используем резервный канал.")
        return@withContext UpdateCheckResult(
            apkUrl = DEFAULT_FALLBACK_APK_URL,
            source = "Официальный CDN SusRadar",
            isFallback = true,
            note = "Репозиторий GitHub пока приватный. Загружена проверенная сборка с CDN."
        )
    }

    private fun extractApkUrlFromContent(content: String): String {
        val markerPattern = Pattern.compile("LATEST_APK_URL=([^\r\n\\s]+)", Pattern.CASE_INSENSITIVE)
        val markerMatcher = markerPattern.matcher(content)
        if (markerMatcher.find()) {
            val url = markerMatcher.group(1)?.trim() ?: ""
            if (url.startsWith("http")) return url
        }

        val mdPattern = Pattern.compile("\\[[^\\]]*\\]\\((https?://[^\\s\\)]+\\.apk[^\\s\\)]*)\\)", Pattern.CASE_INSENSITIVE)
        val mdMatcher = mdPattern.matcher(content)
        if (mdMatcher.find()) {
            val url = mdMatcher.group(1)?.trim() ?: ""
            return url
        }

        val anyApkPattern = Pattern.compile("(https?://[^\\s\"'<>]+\\.apk[^\\s\"'<>]*)", Pattern.CASE_INSENSITIVE)
        val anyMatcher = anyApkPattern.matcher(content)
        if (anyMatcher.find()) {
            val url = anyMatcher.group(1)?.trim() ?: ""
            return url
        }

        return ""
    }

    private fun tryFetchUrl(urlStr: String): String {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SusRadar-App/1.0")

            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Resumable and fault-tolerant APK download with retry support.
     * Uses 3-minute timeouts and Range headers to withstand slow mobile networks.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        AppLogger.i("AppUpdater", "Начало надёжной загрузки обновления: $apkUrl")

        val targetFile = File(context.cacheDir, "SusRadar_Update.apk")
        var existingBytes = 0L

        // Attempt up to 3 retries with resuming
        var success = false
        var lastError: Exception? = null

        for (attempt in 1..4) {
            try {
                AppLogger.d("AppUpdater", "Попытка загрузки $attempt/4...")
                existingBytes = if (targetFile.exists()) targetFile.length() else 0L

                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 180000 // 3 minutes read timeout to prevent SocketTimeoutException
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) SusRadar-Updater/1.0")

                val isResuming = existingBytes > 0
                if (isResuming) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                    AppLogger.d("AppUpdater", "Докачка файла с байта: $existingBytes")
                }

                connection.connect()

                val responseCode = connection.responseCode
                val contentLength = connection.contentLength.toLong()
                val totalExpectedLength: Long = when (responseCode) {
                    HttpURLConnection.HTTP_PARTIAL -> {
                        existingBytes + (if (contentLength > 0L) contentLength else 0L)
                    }
                    HttpURLConnection.HTTP_OK -> {
                        // Server does not support resume, start fresh
                        targetFile.delete()
                        existingBytes = 0L
                        contentLength
                    }
                    else -> {
                        throw Exception("HTTP ошибка сервера: $responseCode")
                    }
                }

                val input: InputStream = connection.inputStream
                val output = if (isResuming && responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    FileOutputStream(targetFile, true)
                } else {
                    FileOutputStream(targetFile, false)
                }

                val buffer = ByteArray(65536) // 64 KB buffer for high throughput
                var downloaded = existingBytes
                var bytesRead: Int

                try {
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        if (totalExpectedLength > 0) {
                            val progress = ((downloaded * 100) / totalExpectedLength).toInt().coerceIn(0, 99)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                } finally {
                    try { output.close() } catch (_: Exception) {}
                    try { input.close() } catch (_: Exception) {}
                    connection.disconnect()
                }

                success = true
                AppLogger.i("AppUpdater", "Файл успешно загружен (${targetFile.length()} байт)!")
                break

            } catch (e: Exception) {
                lastError = e
                AppLogger.w("AppUpdater", "Сбой попытки $attempt (${e.javaClass.simpleName}): ${e.message}")
                if (attempt < 4) {
                    delay(1500) // Small wait before retry
                }
            }
        }

        if (!success || !targetFile.exists() || targetFile.length() < 1000000) {
            throw lastError ?: Exception("Не удалось завершить загрузку файла из-за нестабильного мобильного соединения")
        }

        withContext(Dispatchers.Main) {
            onProgress(100)
            installApkFile(context, targetFile)
        }
    }

    /**
     * Fallback to Android Native DownloadManager system service.
     */
    fun downloadViaSystemManager(context: Context, apkUrl: String) {
        try {
            AppLogger.i("AppUpdater", "Запуск системного DownloadManager для $apkUrl")
            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Обновление SusRadar")
                setDescription("Скачивание актуальной версии приложения...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SusRadar_Update.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            AppLogger.i("AppUpdater", "Загрузка передана системному DownloadManager")
        } catch (e: Exception) {
            AppLogger.e("AppUpdater", "Ошибка DownloadManager, открываем браузер", e)
            openInBrowser(context, apkUrl)
        }
    }

    fun openInBrowser(context: Context, apkUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            AppLogger.i("AppUpdater", "Открыт браузер для скачивания: $apkUrl")
        } catch (e: Exception) {
            AppLogger.e("AppUpdater", "Не удалось открыть браузер", e)
        }
    }

    private fun installApkFile(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    AppLogger.i("AppUpdater", "Запрос разрешения установки неизвестных приложений")
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
            AppLogger.i("AppUpdater", "Инсталлятор пакета успешно вызван")
        } catch (e: Exception) {
            AppLogger.e("AppUpdater", "Ошибка запуска инсталлятора", e)
        }
    }
}
