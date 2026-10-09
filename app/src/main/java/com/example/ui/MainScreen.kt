package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DebugLogDialog
import com.example.ui.screens.LearningCenterScreen
import com.example.ui.screens.LiveMatchScreen
import com.example.ui.screens.OverlayControlScreen
import com.example.ui.screens.RolesWikiScreen
import com.example.viewmodel.SusRadarViewModel

enum class SusDestination(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    RADAR("Радар", Icons.Default.Radar, "nav_radar"),
    OVERLAY("Оверлей", Icons.Default.Layers, "nav_overlay"),
    LEARNING("Обучение", Icons.Default.Psychology, "nav_learning"),
    ROLES("Роли", Icons.Default.MenuBook, "nav_roles")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SusRadarViewModel) {
    var currentDestination by remember { mutableStateOf(SusDestination.RADAR) }
    var showDebugDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentDestination != SusDestination.RADAR) {
        currentDestination = SusDestination.RADAR
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SusRadar",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showDebugDialog = true },
                        modifier = Modifier.testTag("top_bar_debug_button")
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Debug & Логи",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                SusDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                destination.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentDestination,
                label = "screen_transition"
            ) { destination ->
                when (destination) {
                    SusDestination.RADAR -> LiveMatchScreen(viewModel = viewModel)
                    SusDestination.OVERLAY -> OverlayControlScreen(viewModel = viewModel)
                    SusDestination.LEARNING -> LearningCenterScreen(viewModel = viewModel)
                    SusDestination.ROLES -> RolesWikiScreen()
                }
            }
        }
    }

    if (showDebugDialog) {
        DebugLogDialog(
            onDismiss = { showDebugDialog = false }
        )
    }
}
