package com.franz.speedometer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.franz.speedometer.data.ThemeMode

private val NightColors = darkColorScheme(
    primary = Color(0xFF4CAF50),
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
)

/** Resolves [ThemeMode] (AUTO follows the system) into a Material3 theme. */
@Composable
fun SpeedoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.NIGHT -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Dark icons on a light background, light icons on a dark background.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) NightColors else LightColors,
        content = content,
    )
}
