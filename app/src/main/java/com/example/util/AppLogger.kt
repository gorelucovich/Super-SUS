package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val level: String, // "DEBUG", "INFO", "WARN", "ERROR"
    val tag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

object AppLogger {
    private const val LOG_FILE_NAME = "susradar_debug.log"
    private const val MAX_IN_MEMORY_LOGS = 300

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, LOG_FILE_NAME)
        log("INFO", "AppLogger", "=== SusRadar Запущен (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}) ===")

        // Global crash / uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            e("CrashHandler", "НЕОБРАБОТАННОЕ ИСКЛЮЧЕНИЕ в потоке ${thread.name}: $stackTrace", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message)
    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val extra = throwable?.let { "\n" + Log.getStackTraceString(it) } ?: ""
        log("WARN", tag, message + extra)
    }
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val extra = throwable?.let { "\n" + Log.getStackTraceString(it) } ?: ""
        log("ERROR", tag, message + extra)
    }

    private fun log(level: String, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)

        // Native android log
        when (level) {
            "DEBUG" -> Log.d(tag, message)
            "INFO" -> Log.i(tag, message)
            "WARN" -> Log.w(tag, message)
            "ERROR" -> Log.e(tag, message)
        }

        // Keep in memory for UI viewer
        val currentList = _logs.value
        val updated = if (currentList.size >= MAX_IN_MEMORY_LOGS) {
            listOf(entry) + currentList.take(MAX_IN_MEMORY_LOGS - 1)
        } else {
            listOf(entry) + currentList
        }
        _logs.value = updated

        // Append to local log file
        try {
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append("[${entry.formattedTime}] [${entry.level}] [${entry.tag}] ${entry.message}\n")
                }
            }
        } catch (_: Exception) {}
    }

    fun getAllLogsText(): String {
        return _logs.value.reversed().joinToString("\n") { entry ->
            "[${entry.formattedTime}] [${entry.level}] [${entry.tag}] ${entry.message}"
        }
    }

    fun copyLogsToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SusRadar Logs", getAllLogsText())
        clipboard.setPrimaryClip(clip)
        i("AppLogger", "Логи скопированы в буфер обмена")
    }

    fun shareLogFile(context: Context) {
        try {
            val text = getAllLogsText()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "SusRadar Debug Log")
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Экспортировать susradar_debug.log"))
            i("AppLogger", "Запущен диалог экспорта логов")
        } catch (e: Exception) {
            e("AppLogger", "Ошибка при экспорте логов", e)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        try {
            logFile?.delete()
            logFile?.createNewFile()
        } catch (_: Exception) {}
        i("AppLogger", "Логи очищены")
    }
}
