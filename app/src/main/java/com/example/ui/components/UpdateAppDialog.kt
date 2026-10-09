package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLogger
import com.example.util.AppUpdater
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Checking : UpdateUiState()
    data class Found(val apkUrl: String) : UpdateUiState()
    data class Downloading(val progress: Int) : UpdateUiState()
    data class Completed(val message: String) : UpdateUiState()
    data class Error(val error: String) : UpdateUiState()
}

@Composable
fun UpdateAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Checking) }

    fun checkUpdate() {
        uiState = UpdateUiState.Checking
        coroutineScope.launch {
            try {
                val apkUrl = AppUpdater.fetchLatestApkUrl()
                uiState = UpdateUiState.Found(apkUrl)
            } catch (e: Exception) {
                AppLogger.e("UpdateDialog", "Ошибка проверки обновления", e)
                uiState = UpdateUiState.Error(e.message ?: "Ошибка проверки обновления")
            }
        }
    }

    LaunchedEffect(Unit) {
        checkUpdate()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Облачное обновление", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (val state = uiState) {
                    is UpdateUiState.Checking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Проверка GitHub README...", fontSize = 13.sp)
                        }
                    }

                    is UpdateUiState.Found -> {
                        Text(
                            text = "Найдено обновление на GitHub!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Файл обновления доступен для загрузки. Нажмите кнопку ниже для скачивания и автоматической установки:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.apkUrl,
                                fontSize = 10.sp,
                                maxLines = 2,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        Text(
                            text = "Загрузка обновления: ${state.progress}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "По завершении загрузки откроется установщик пакета...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateUiState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, fontSize = 13.sp, color = Color(0xFF10B981))
                        }
                    }

                    is UpdateUiState.Error -> {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Не удалось обновиться", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(state.error, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (val state = uiState) {
                is UpdateUiState.Found -> {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                uiState = UpdateUiState.Downloading(0)
                                try {
                                    AppUpdater.downloadAndInstallApk(
                                        context = context,
                                        apkUrl = state.apkUrl,
                                        onProgress = { progress ->
                                            uiState = UpdateUiState.Downloading(progress)
                                        }
                                    )
                                    uiState = UpdateUiState.Completed("Установщик запущен!")
                                } catch (e: Exception) {
                                    AppLogger.e("UpdateDialog", "Ошибка загрузки APK", e)
                                    uiState = UpdateUiState.Error(e.message ?: "Ошибка скачивания")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.testTag("download_update_btn")
                    ) {
                        Text("Скачать и обновить")
                    }
                }

                is UpdateUiState.Error -> {
                    Button(onClick = { checkUpdate() }) {
                        Text("Повторить")
                    }
                }

                else -> {
                    Button(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }
            }
        },
        dismissButton = {
            if (uiState !is UpdateUiState.Downloading) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}
