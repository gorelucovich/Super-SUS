package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LearnedWeightEntity
import com.example.model.PlayerColor
import com.example.viewmodel.SusRadarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LearningCenterScreen(
    viewModel: SusRadarViewModel,
    modifier: Modifier = Modifier
) {
    val weights by viewModel.learnedWeights.collectAsState()
    val matches by viewModel.matchHistory.collectAsState()
    val players by viewModel.players.collectAsState()

    var showCalibrationDialog by remember { mutableStateOf(false) }

    val averageAccuracy = if (matches.isNotEmpty()) {
        matches.map { it.accuracyPercent }.average().toInt()
    } else 82

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("learning_center_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Центр самообучения AI",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Адаптивные веса и дообучение модели по реальным матчам",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stats summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Точность детекции",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$averageAccuracy%",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Матчей в базе",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${matches.size}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }

        // Action: Post-match calibration button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1B4B)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Послематчевая калибровка",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Игра закончилась? Укажите, кем реально оказались игроки, чтобы нейросеть обучилась под стиль вашего сервера.",
                        fontSize = 12.sp,
                        color = Color(0xFFA5B4FC)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showCalibrationDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.fillMaxWidth().testTag("open_calibration_btn")
                    ) {
                        Text("Откалибровать текущий матч", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Learned Bayesian weights section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Адаптивные веса модели",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${weights.size} параметров",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(weights) { weightItem ->
            LearnedWeightRow(weightItem = weightItem)
        }

        // Match history
        item {
            Text(
                text = "История откалиброванных матчей",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(matches) { matchRecord ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = matchRecord.outcomeSummary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(matchRecord.timestamp))
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (matchRecord.notes.isNotBlank()) {
                            Text(
                                text = matchRecord.notes,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        contentColor = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "${matchRecord.accuracyPercent}%",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            players = players.map { it.color },
            onDismiss = { showCalibrationDialog = false },
            onCalibrate = { actualMap ->
                viewModel.calibrateMatch(actualMap)
                showCalibrationDialog = false
            }
        )
    }
}

@Composable
fun LearnedWeightRow(weightItem: LearnedWeightEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = weightItem.descriptionRu,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(weightItem.weight * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Сэмплов обучения: ${weightItem.samplesCount}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { weightItem.weight },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF6366F1),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun CalibrationDialog(
    players: List<PlayerColor>,
    onDismiss: () -> Unit,
    onCalibrate: (Map<PlayerColor, String>) -> Unit
) {
    var garnetRedRole by remember { mutableStateOf("Шпион") }
    var cyanRole by remember { mutableStateOf("Подрыватель") }
    var yellowRole by remember { mutableStateOf("Пророк") }
    var redRole by remember { mutableStateOf("Шериф") }
    var blueRole by remember { mutableStateOf("Доктор") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Калибровка ролей по итогам матча", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Отметьте подтверждённые роли из экрана победы Super Sus для обучения:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Гранатово-красный:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(garnetRedRole, fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Голубой:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(cyanRole, fontSize = 12.sp, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Жёлтый:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(yellowRole, fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Красный:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(redRole, fontSize = 12.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val map = mapOf(
                        PlayerColor.GARNET_RED to garnetRedRole,
                        PlayerColor.CYAN to cyanRole,
                        PlayerColor.YELLOW to yellowRole,
                        PlayerColor.RED to redRole,
                        PlayerColor.BLUE to blueRole
                    )
                    onCalibrate(map)
                },
                modifier = Modifier.testTag("confirm_calibration_btn")
            ) {
                Text("Обучить модель")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
