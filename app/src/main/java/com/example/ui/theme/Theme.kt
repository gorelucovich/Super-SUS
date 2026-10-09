package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SpacePrimary,
    onPrimary = Color.White,
    primaryContainer = SpacePrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = SpaceSecondary,
    onSecondary = Color.Black,
    tertiary = SpaceTertiary,
    onTertiary = Color.White,
    background = SpaceDarkBg,
    onBackground = Color(0xFFF9FAFB),
    surface = SpaceDarkSurface,
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = SpaceDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF9CA3AF)
)

private val LightColorScheme = lightColorScheme(
    primary = SpacePrimaryDark,
    onPrimary = Color.White,
    primaryContainer = SpacePrimary,
    onPrimaryContainer = Color.White,
    secondary = SpaceSecondary,
    onSecondary = Color.White,
    tertiary = SpaceTertiary,
    onTertiary = Color.White,
    background = SpaceLightBg,
    onBackground = Color(0xFF111827),
    surface = SpaceLightSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = SpaceLightSurfaceElevated,
    onSurfaceVariant = Color(0xFF6B7280)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
