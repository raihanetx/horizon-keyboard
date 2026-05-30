package com.horizon.keyboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyType

/**
 * Color scheme for keyboard keys.
 */
data class KeyColorScheme(
    val background: Color,
    val text: Color,
    val border: Color
)

/**
 * Get colors for a key based on its type and state.
 */
@Composable
fun getKeyColors(
    keyItem: KeyItem,
    isShiftActive: Boolean,
    isDark: Boolean,
    isPressed: Boolean
): KeyColorScheme {
    if (isPressed) {
        return KeyColorScheme(
            background = if (isDark) DarkKeyPressActive else LightKeyPressActive,
            text = if (isDark) DarkKeyText else LightKeyText,
            border = if (isDark) DarkKeyBorder else LightKeyBorder
        )
    }
    
    return when (keyItem.type) {
        KeyType.SHIFT -> getShiftColors(isShiftActive, isDark)
        KeyType.BACKSPACE, KeyType.ENTER, KeyType.NUMBERS, KeyType.ABC -> getSpecialKeyColors(isDark)
        else -> getLetterKeyColors(isDark)
    }
}

@Composable
private fun getShiftColors(isActive: Boolean, isDark: Boolean): KeyColorScheme {
    return if (isActive) {
        KeyColorScheme(
            background = if (isDark) DarkShiftActiveBackground else LightShiftActiveBackground,
            text = if (isDark) DarkShiftActiveText else LightShiftActiveText,
            border = if (isDark) DarkShiftActiveBorder else LightShiftActiveBorder
        )
    } else {
        getSpecialKeyColors(isDark)
    }
}

@Composable
private fun getSpecialKeyColors(isDark: Boolean): KeyColorScheme {
    return KeyColorScheme(
        background = if (isDark) DarkSpecialKeyBackground else LightSpecialKeyBackground,
        text = if (isDark) DarkKeyText else LightKeyText,
        border = if (isDark) DarkKeyBorder else LightKeyBorder
    )
}

@Composable
private fun getLetterKeyColors(isDark: Boolean): KeyColorScheme {
    return KeyColorScheme(
        background = if (isDark) DarkKeyBackground else LightKeyBackground,
        text = if (isDark) DarkKeyText else LightKeyText,
        border = if (isDark) DarkKeyBorder else LightKeyBorder
    )
}
