package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.engine.VoiceSpeechParser
import com.example.model.PlayerColor
import com.example.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlayPlayerState(
    val color: PlayerColor,
    val isDead: Boolean = false,
    val claimedRole: String = "",
    val suspicionLevel: String = "Нейтрален", // "Мирный", "Нейтрален", "Подозрителен", "Предатель"
    val notes: String = ""
)

class OverlayRadarService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        val initialPlayers = listOf(
            OverlayPlayerState(PlayerColor.RED),
            OverlayPlayerState(PlayerColor.GARNET_RED),
            OverlayPlayerState(PlayerColor.BLUE),
            OverlayPlayerState(PlayerColor.CYAN),
            OverlayPlayerState(PlayerColor.YELLOW),
            OverlayPlayerState(PlayerColor.SILVER),
            OverlayPlayerState(PlayerColor.WHITE),
            OverlayPlayerState(PlayerColor.BROWN),
            OverlayPlayerState(PlayerColor.PINK),
            OverlayPlayerState(PlayerColor.PURPLE)
        )

        private val _playersState = MutableStateFlow(initialPlayers)
        val playersState = _playersState.asStateFlow()

        private val _deductionVerdict = MutableStateFlow("Следите за чатом собрания и отмечайте роли игроков")
        val deductionVerdict = _deductionVerdict.asStateFlow()

        private val _lastActionLog = MutableStateFlow("Оверлей активен")
        val lastActionLog = _lastActionLog.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun updatePlayers(players: List<com.example.model.PlayerInGame>) {
            // Optional sync from ViewModel
        }

        fun updateVoiceMessage(msg: String) {
            parseQuickMessage(msg)
        }

        fun togglePlayerDead(color: PlayerColor) {
            _playersState.value = _playersState.value.map {
                if (it.color == color) it.copy(isDead = !it.isDead) else it
            }
            recomputeVerdict()
        }

        fun updatePlayerRole(color: PlayerColor, role: String) {
            _playersState.value = _playersState.value.map {
                if (it.color == color) it.copy(claimedRole = role) else it
            }
            _lastActionLog.value = "${color.ruName}: роль '$role'"
            recomputeVerdict()
        }

        fun updatePlayerSuspicion(color: PlayerColor, level: String) {
            _playersState.value = _playersState.value.map {
                if (it.color == color) it.copy(suspicionLevel = level) else it
            }
            _lastActionLog.value = "${color.ruName}: статус $level"
            recomputeVerdict()
        }

        fun resetOverlay() {
            _playersState.value = initialPlayers
            _deductionVerdict.value = "Новое лобби: отмечайте заявления в чате"
            _lastActionLog.value = "Сброс лобби"
        }

        fun parseQuickMessage(text: String) {
            val parsed = VoiceSpeechParser.parse(text)
            if (parsed.targetColor != null) {
                val color = parsed.targetColor
                when {
                    text.contains("пчела", ignoreCase = true) -> {
                        updatePlayerRole(color, "Пчела 🐝")
                        updatePlayerSuspicion(color, "Мирный")
                    }
                    text.contains("предатель", ignoreCase = true) || text.contains("кик", ignoreCase = true) -> {
                        updatePlayerSuspicion(color, "Предатель")
                    }
                    text.contains("чист", ignoreCase = true) || text.contains("визуал", ignoreCase = true) -> {
                        updatePlayerSuspicion(color, "Мирный")
                    }
                }
            }
            _lastActionLog.value = text
            recomputeVerdict()
        }

        private fun recomputeVerdict() {
            val players = _playersState.value
            val deadCount = players.count { it.isDead }
            val impSuspects = players.filter { it.suspicionLevel == "Предатель" && !it.isDead }
            val beePlayer = players.find { it.claimedRole.contains("Пчела") }

            val builder = StringBuilder()
            if (beePlayer != null) {
                builder.append("${beePlayer.color.ruName} — Пчела (не угадывать!) ")
            }
            if (impSuspects.isNotEmpty()) {
                builder.append("Подозреваемые: ${impSuspects.joinToString { it.color.ruName }}. ")
            } else {
                builder.append("Погибших: $deadCount/10. ")
            }
            _deductionVerdict.value = builder.toString()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (_: Exception) {}

        startForegroundServiceWithNotification()
        initOverlayWindow()
        _isServiceRunning.value = true
        AppLogger.i("OverlayService", "Тактический оверлей запущен")
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "sus_radar_channel"
        val channelName = "Super Sus Radar Overlay"
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
            .setContentTitle("SusRadar Тактический HUD")
            .setContentText("Дедуктивный радар активен поверх Super Sus")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (_: Exception) {
                startForeground(101, notification)
            }
        } else {
            startForeground(101, notification)
        }
    }

    private fun initOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 140
        }

        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayRadarService)
            setViewTreeViewModelStoreOwner(this@OverlayRadarService)
            setViewTreeSavedStateRegistryOwner(this@OverlayRadarService)

            setContent {
                TacticalOverlayWidgetContent(
                    onDragDelta = { dx, dy ->
                        try {
                            params?.let { p ->
                                p.x += dx.toInt()
                                p.y += dy.toInt()
                                windowManager?.updateViewLayout(this@apply, p)
                            }
                        } catch (_: Exception) {}
                    },
                    onCloseService = {
                        stopSelf()
                    }
                )
            }
        }

        try {
            windowManager?.addView(overlayComposeView, params)
        } catch (e: Exception) {
            AppLogger.e("OverlayService", "Ошибка добавления оверлея", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        try {
            overlayComposeView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
    }
}

@Composable
fun TacticalOverlayWidgetContent(
    onDragDelta: (Float, Float) -> Unit,
    onCloseService: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedPlayerColor by remember { mutableStateOf<PlayerColor?>(null) }
    var customQuickText by remember { mutableStateOf("") }

    val players by OverlayRadarService.playersState.collectAsState()
    val verdict by OverlayRadarService.deductionVerdict.collectAsState()
    val lastLog by OverlayRadarService.lastActionLog.collectAsState()

    if (!isExpanded) {
        // Collapsed Draggable Mini Icon
        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xF00F172A))
                .border(2.dp, Color(0xFF6366F1), CircleShape)
                .clickable { isExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = "Детектор",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI HUD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    } else {
        // Expanded Tactical Deduction Card over Super Sus
        Card(
            modifier = Modifier
                .width(320.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF7090D16)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SUS ДЕТЕКТОР HUD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { OverlayRadarService.resetOverlay() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Сброс", tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Свернуть", tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCloseService,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // AI Deduction Verdict Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1B4B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ДЕДУКТИВНЫЙ ВЕРДИКТ:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = verdict,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick message input for chat analysis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customQuickText,
                        onValueChange = { customQuickText = it },
                        placeholder = { Text("Фраза чата (напр. Белый пчела)", fontSize = 10.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF111827),
                            unfocusedContainerColor = Color(0xFF111827),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (customQuickText.isNotBlank()) {
                                OverlayRadarService.parseQuickMessage(customQuickText)
                                customQuickText = ""
                            }
                        },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Анализ", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 1-Tap Quick Chat Presets from real matches
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "Белый: Я Пчела 🐝" to "Белый пчела",
                        "Синий: Я в электричке" to "Синий электричка алиби",
                        "Красный: Кик Белого 🚨" to "Красный голосуйте за белый",
                        "Коричневый: В складе 📦" to "Коричневый склад",
                        "Серебряный: Репорт 📣" to "Серебряный репорт",
                        "Жёлтый: Пророк 👁️" to "Желтый пророк",
                        "Голубой: В медотсеке 🏥" to "Голубой медотсек"
                    )
                    items(presets) { (label, phrase) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.clickable {
                                OverlayRadarService.parseQuickMessage(phrase)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Игроки собрания (нажмите для заметок / роли):",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 10 Players Grid / List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(players) { p ->
                        val isSelected = selectedPlayerColor == p.color
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF312E81) else if (p.isDead) Color(0xFF1F2937) else Color(0xFF111827),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlayerColor = if (isSelected) null else p.color }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(p.color.composeColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = p.color.ruName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.isDead) Color.Gray else Color.White
                                    )
                                    if (p.claimedRole.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "[${p.claimedRole}]",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Status Badge
                                    val statusColor = when (p.suspicionLevel) {
                                        "Мирный" -> Color(0xFF10B981)
                                        "Предатель" -> Color(0xFFEF4444)
                                        "Подозрителен" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF6B7280)
                                    }
                                    Text(
                                        text = p.suspicionLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Dead toggle button
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (p.isDead) Color(0xFFEF4444) else Color(0xFF374151))
                                            .clickable { OverlayRadarService.togglePlayerDead(p.color) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = if (p.isDead) "💀" else "✓", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // If a player is selected, show Quick Role / Suspicion Selector bar
                selectedPlayerColor?.let { color ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "Быстрая отметка для: ${color.ruName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Suspicion Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { OverlayRadarService.updatePlayerSuspicion(color, "Мирный") },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("Мирный", fontSize = 9.sp)
                                }
                                Button(
                                    onClick = { OverlayRadarService.updatePlayerSuspicion(color, "Предатель") },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("Предатель", fontSize = 9.sp)
                                }
                                Button(
                                    onClick = { OverlayRadarService.updatePlayerSuspicion(color, "Подозрителен") },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("Мутный", fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Role Buttons (Bee, Sheriff, Seer, Joker, Doctor)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val quickRoles = listOf("Пчела 🐝", "Шериф ⭐", "Пророк 👁️", "Доктор 💓", "Инженер 🔧", "Джокер 🃏", "Гуль 👾", "Шпион 🎩")
                                items(quickRoles) { r ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF334155),
                                        modifier = Modifier.clickable {
                                            OverlayRadarService.updatePlayerRole(color, r)
                                        }
                                    ) {
                                        Text(
                                            text = r,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
