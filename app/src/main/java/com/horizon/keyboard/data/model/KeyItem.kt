package com.horizon.keyboard.data.model

import androidx.compose.ui.unit.Dp

/**
 * Represents a single key on the keyboard.
 */
data class KeyItem(
    val displayText: String,
    val value: String,
    val type: KeyType,
    val widthDp: Dp? = null
)

/**
 * Types of keyboard keys.
 */
enum class KeyType {
    LETTER,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    NUMBERS,
    ABC,
    COMMA,
    PERIOD
}

/**
 * Represents a row of keys on the keyboard.
 */
data class KeyboardRow(
    val keys: List<KeyItem>,
    val widthFraction: Float = 1.0f
)
