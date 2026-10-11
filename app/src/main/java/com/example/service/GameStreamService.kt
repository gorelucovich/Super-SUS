package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ai.CoPilotSpeechEngine
import com.example.ai.GeminiLiveCoPilotEngine
import com.example.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameStreamService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var streamJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var speechEngine: CoPilotSpeechEngine? = null

    companion object {
        const val ACTION_START_STREAM = "com.example.action.START_STREAM"
        const val ACTION_STOP_STREAM = "com.example.action.STOP_STREAM"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _isStreaming = MutableStateFlow(false)
        val isStreaming = _isStreaming.asStateFlow()

        private val _lastCapturedSummary = MutableStateFlow("Ожидание запуска стрима")
        val lastCapturedSummary = _lastCapturedSummary.asStateFlow()

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, GameStreamService::class.java).apply {
                action = ACTION_START_STREAM
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameStreamService::class.java).apply {
                action = ACTION_STOP_STREAM
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speechEngine = CoPilotSpeechEngine(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAM -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startForegroundWithNotification()
                    initMediaProjection(resultCode, resultData)
                    speechEngine?.startContinuousListening()
                    _isStreaming.value = true
                }
            }
            ACTION_STOP_STREAM -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "game_stream_channel"
        val channelName = "Super Sus Gemini Live Stream"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gemini Live Co-Pilot активен")
            .setContentText("ИИ наблюдает за экраном игры и слушает голос в режиме Hands-free")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    102,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (_: Exception) {
                try {
                    startForeground(102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                } catch (_: Exception) {
                    startForeground(102, notification)
                }
            }
        } else {
            startForeground(102, notification)
        }
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        // Register callback for API 34+
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                cleanup()
            }
        }, null)

        val metrics = resources.displayMetrics
        val width = 720
        val height = 1280
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "SuperSusCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        startFrameSamplingLoop(width, height)
    }

    private fun startFrameSamplingLoop(width: Int, height: Int) {
        streamJob?.cancel()
        streamJob = scope.launch {
            AppLogger.i("GameStream", "Запуск цикла захвата кадров и анализа Gemini")
            while (isActive) {
                delay(3500) // Анализ каждые 3.5 секунды для оптимизации запросов и аккумулятора

                val bitmap = acquireLatestBitmap(width, height)
                if (bitmap != null) {
                    try {
                        val observation = GeminiLiveCoPilotEngine.analyzeGameFrame(bitmap)
                        _lastCapturedSummary.value = observation.adviceShort

                        // Если обнаружен прыжок в люк или визуал-таск, озвучиваем это голосом!
                        if (observation.ventJumpers.isNotEmpty()) {
                            speechEngine?.speak("Внимание! Прыжок в люк: ${observation.ventJumpers.joinToString()}")
                        }
                        if (observation.visualTaskConfirmations.isNotEmpty()) {
                            speechEngine?.speak("Визуал подтверждён: ${observation.visualTaskConfirmations.joinToString()}")
                        }

                        // Обновляем оверлей
                        OverlayRadarService.parseQuickMessage(observation.adviceShort)
                    } catch (e: Exception) {
                        AppLogger.e("GameStream", "Ошибка анализа кадра", e)
                    }
                }
            }
        }
    }

    private fun acquireLatestBitmap(width: Int, height: Int): Bitmap? {
        val reader = imageReader ?: return null
        val image = try {
            reader.acquireLatestImage()
        } catch (_: Exception) {
            null
        } ?: return null

        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            // Trim row padding if present
            if (rowPadding != 0) {
                Bitmap.createBitmap(bitmap, 0, 0, width, height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            AppLogger.e("GameStream", "Ошибка конвертации Image в Bitmap", e)
            null
        } finally {
            image.close()
        }
    }

    private fun cleanup() {
        _isStreaming.value = false
        streamJob?.cancel()
        speechEngine?.stopContinuousListening()
        speechEngine?.release()
        speechEngine = null

        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        AppLogger.i("GameStream", "GameStreamService уничтожен")
    }
}
