package com.horizon.keyboard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme.
 */
private val LightColorScheme = lightColorScheme(
    primary = LightToolbarIcon,
    onPrimary = LightShiftActiveText,
    background = LightBackground,
    surface = LightKeyboardContainer,
    onSurface = LightKeyText,
    outline = LightContainerBorder
)

/**
 * Dark color scheme.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkToolbarIcon,
    onPrimary = DarkShiftActiveText,
    background = DarkBackground,
    surface = DarkKeyboardContainer,
    onSurface = DarkKeyText,
    outline = DarkContainerBorder
)

/**
 * Main theme for Horizon Keyboard.
 */
@Composable
fun HorizonKeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkBackground.toArgb() else LightBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
