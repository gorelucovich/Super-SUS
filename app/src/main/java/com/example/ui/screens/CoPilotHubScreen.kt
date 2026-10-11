package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.MainActivity
import com.example.ai.GeminiLiveCoPilotEngine
import com.example.data.PlayerProfileEntity
import com.example.service.GameStreamService
import com.example.service.OverlayRadarService
import com.example.viewmodel.SusRadarViewModel

@Composable
fun CoPilotHubScreen(
    viewModel: SusRadarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Live Co-Pilot, 1 = Игроки (Никнеймы), 2 = База знаний
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    val isKeyConfigured by GeminiLiveCoPilotEngine.isKeyConfigured.collectAsState()
    val isStreaming by GameStreamService.isStreaming.collectAsState()
    val lastSummary by GameStreamService.lastCapturedSummary.collectAsState()
    val aiVerdict by GeminiLiveCoPilotEngine.lastAiVerdict.collectAsState()
    val myRole by GeminiLiveCoPilotEngine.myRoleInfo.collectAsState()
    val playerProfiles by viewModel.playerProfiles.collectAsState()
    val knowledgeList by viewModel.knowledgeBase.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("copilot_hub_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Live Co-Pilot",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Интеллектуальный напарник Super Sus",
                                fontSize = 12.sp,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key Status Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isKeyConfigured) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isKeyConfigured) Color(0xFF34D399) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isKeyConfigured) "Google Gemini API подключен" else "Ключ GEMINI_API_KEY не задан",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isKeyConfigured)
                                        "Мультимодальный анализ экрана и детекция лжи активны"
                                    else
                                        "Добавьте GEMINI_API_KEY в панели Secrets",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Navigation Tabs (Live, Players, Knowledge)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF818CF8),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Эфир и подсказки", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Игроки (${playerProfiles.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("База знаний", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Live Stream Controls & HUD Status
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Управление захватом игры Super Sus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Запустите трансляцию экрана и оверлей перед входом в матч. ИИ будет непрерывно слушать ваш голос в гарнитуре без нажатия кнопок.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isStreaming) {
                                            GameStreamService.stop(context)
                                        } else {
                                            MainActivity.requestScreenCapture()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isStreaming) Color(0xFFDC2626) else Color(0xFF4F46E5)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("stream_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isStreaming) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isStreaming) "Остановить эфир" else "Запустить захват",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = android.content.Intent(context, OverlayRadarService::class.java)
                                        context.startService(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("open_overlay_button")
                                ) {
                                    Text("Открыть HUD", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Status Indicator Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Hands-free Mic Indicator
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D9488)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🎙️ Hands-Free микрофон активен", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    Text("Говорите свободно: 'Я шериф с уникальным модулем', 'Красный прыгнул в люк'", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }

                        // My Role Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Ваша роль в текущем матче:", fontSize = 10.sp, color = Color.Gray)
                                    Text(myRole ?: "Определяется с экрана...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFBBF24))
                                }
                            }
                        }

                        // AI Live Advice
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("💡 Оперативный совет Gemini Live:", fontSize = 10.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(aiVerdict, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Players Profiles by NICKNAME
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "База игроков (строго по никнеймам)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Button(
                            onClick = { showAddPlayerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить", fontSize = 11.sp)
                        }
                    }
                }

                if (playerProfiles.isEmpty()) {
                    item {
                        Text("Пока нет сохранённых профилей игроков", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    items(playerProfiles) { profile ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.nickname,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val trustColor = when {
                                            profile.trustScore >= 70 -> Color(0xFF10B981)
                                            profile.trustScore >= 40 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = trustColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Доверие: ${profile.trustScore}%",
                                                color = trustColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePlayerProfile(profile.nickname) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Матчей: ${profile.totalMatches} | Пойман на лжи: ${profile.liesDetected} раз",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                if (profile.favoriteRoles.isNotBlank()) {
                                    Text(
                                        text = "Любимые роли: ${profile.favoriteRoles}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFBBF24)
                                    )
                                }
                                if (profile.equippedModulesNotes.isNotBlank()) {
                                    Text(
                                        text = "Модули: ${profile.equippedModulesNotes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                                if (profile.notes.isNotBlank()) {
                                    Text(
                                        text = "Заметка: ${profile.notes}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Knowledge Base
                item {
                    Text(
                        text = "База знаний ИИ (самообучение)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                if (knowledgeList.isEmpty()) {
                    item {
                        Text("База знаний пуста. ИИ заполняет её во время ваших игр.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    items(knowledgeList) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "[${item.category}] ${item.subject}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5B4FC)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteKnowledge(item.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.content, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlayerDialog) {
        var nickname by remember { mutableStateOf("") }
        var favoriteRoles by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text("Новый игрок по никнейму") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Цвет в игре меняется каждый матч, поэтому профиль создаётся строго по Никнейму:", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Никнейм игрока") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = favoriteRoles,
                        onValueChange = { favoriteRoles = it },
                        label = { Text("Замеченные роли (напр. Шериф, Шпион)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки (стиль игры, привычки)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nickname.isNotBlank()) {
                            viewModel.upsertPlayerProfile(
                                PlayerProfileEntity(
                                    nickname = nickname.trim(),
                                    favoriteRoles = favoriteRoles.trim(),
                                    notes = notes.trim()
                                )
                            )
                            showAddPlayerDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
