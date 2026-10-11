package com.example

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.SusDatabase
import com.example.service.GameStreamService
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLogger
import com.example.viewmodel.SusRadarViewModel
import com.example.viewmodel.SusRadarViewModelFactory
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    companion object {
        private var currentActivity: WeakReference<MainActivity>? = null

        fun requestScreenCapture() {
            currentActivity?.get()?.launchScreenCapture()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            AppLogger.i("MainActivity", "Разрешение на захват экрана предоставлено пользователем")
            GameStreamService.start(this, result.resultCode, result.data!!)
        } else {
            AppLogger.w("MainActivity", "Пользователь отклонил запрос на захват экрана")
        }
    }

    private val viewModel: SusRadarViewModel by viewModels {
        val database = SusDatabase.getInstance(applicationContext)
        SusRadarViewModelFactory(database.susDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = WeakReference(this)
        AppLogger.init(applicationContext)
        com.example.ai.GeminiLiveCoPilotEngine.init(applicationContext)
        AppLogger.i("MainActivity", "onCreate вызван, запуск UI SusRadar")
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentActivity?.get() == this) {
            currentActivity = null
        }
    }

    private fun launchScreenCapture() {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent())
    }
}
