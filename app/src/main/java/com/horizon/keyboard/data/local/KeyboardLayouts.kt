package com.horizon.keyboard.data.local

import androidx.compose.ui.unit.dp
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyType
import com.horizon.keyboard.data.model.KeyboardRow

/**
 * Defines all keyboard layouts.
 * Separated from data classes for cleaner organization.
 */
object KeyboardLayouts {
    
    /**
     * Standard QWERTY layout.
     */
    val qwerty: List<KeyboardRow> = listOf(
        // Row 1: QWERTYUIOP
        KeyboardRow(
            keys = listOf(
                KeyItem("q", "q", KeyType.LETTER, 32.dp),
                KeyItem("w", "w", KeyType.LETTER, 32.dp),
                KeyItem("e", "e", KeyType.LETTER, 32.dp),
                KeyItem("r", "r", KeyType.LETTER, 32.dp),
                KeyItem("t", "t", KeyType.LETTER, 32.dp),
                KeyItem("y", "y", KeyType.LETTER, 32.dp),
                KeyItem("u", "u", KeyType.LETTER, 32.dp),
                KeyItem("i", "i", KeyType.LETTER, 32.dp),
                KeyItem("o", "o", KeyType.LETTER, 32.dp),
                KeyItem("p", "p", KeyType.LETTER, 32.dp)
            )
        ),
        // Row 2: ASDFGHJKL
        KeyboardRow(
            keys = listOf(
                KeyItem("a", "a", KeyType.LETTER, 34.dp),
                KeyItem("s", "s", KeyType.LETTER, 34.dp),
                KeyItem("d", "d", KeyType.LETTER, 34.dp),
                KeyItem("f", "f", KeyType.LETTER, 34.dp),
                KeyItem("g", "g", KeyType.LETTER, 34.dp),
                KeyItem("h", "h", KeyType.LETTER, 34.dp),
                KeyItem("j", "j", KeyType.LETTER, 34.dp),
                KeyItem("k", "k", KeyType.LETTER, 34.dp),
                KeyItem("l", "l", KeyType.LETTER, 34.dp)
            ),
            widthFraction = 0.95f
        ),
        // Row 3: SHIFT + ZXCVBNM + BACKSPACE
        KeyboardRow(
            keys = listOf(
                KeyItem("⇧", "shift", KeyType.SHIFT, 42.dp),
                KeyItem("z", "z", KeyType.LETTER, 35.dp),
                KeyItem("x", "x", KeyType.LETTER, 35.dp),
                KeyItem("c", "c", KeyType.LETTER, 35.dp),
                KeyItem("v", "v", KeyType.LETTER, 35.dp),
                KeyItem("b", "b", KeyType.LETTER, 35.dp),
                KeyItem("n", "n", KeyType.LETTER, 35.dp),
                KeyItem("m", "m", KeyType.LETTER, 35.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, 42.dp)
            )
        ),
        // Row 4: 123 + , + SPACE + . + ENTER
        KeyboardRow(
            keys = listOf(
                KeyItem("123", "123", KeyType.NUMBERS, 50.dp),
                KeyItem(",", ",", KeyType.COMMA, 36.dp),
                KeyItem("space", " ", KeyType.SPACE, 160.dp),
                KeyItem(".", ".", KeyType.PERIOD, 36.dp),
                KeyItem("↵", "enter", KeyType.ENTER, 50.dp)
            )
        )
    )
    
    /**
     * Numbers and symbols layout.
     */
    val numbers: List<KeyboardRow> = listOf(
        // Row 1: 1234567890
        KeyboardRow(
            keys = listOf(
                KeyItem("1", "1", KeyType.LETTER, 32.dp),
                KeyItem("2", "2", KeyType.LETTER, 32.dp),
                KeyItem("3", "3", KeyType.LETTER, 32.dp),
                KeyItem("4", "4", KeyType.LETTER, 32.dp),
                KeyItem("5", "5", KeyType.LETTER, 32.dp),
                KeyItem("6", "6", KeyType.LETTER, 32.dp),
                KeyItem("7", "7", KeyType.LETTER, 32.dp),
                KeyItem("8", "8", KeyType.LETTER, 32.dp),
                KeyItem("9", "9", KeyType.LETTER, 32.dp),
                KeyItem("0", "0", KeyType.LETTER, 32.dp)
            )
        ),
        // Row 2: Symbols
        KeyboardRow(
            keys = listOf(
                KeyItem("-", "-", KeyType.LETTER, 34.dp),
                KeyItem("/", "/", KeyType.LETTER, 34.dp),
                KeyItem(":", ":", KeyType.LETTER, 34.dp),
                KeyItem(";", ";", KeyType.LETTER, 34.dp),
                KeyItem("(", "(", KeyType.LETTER, 34.dp),
                KeyItem(")", ")", KeyType.LETTER, 34.dp),
                KeyItem("$", "$", KeyType.LETTER, 34.dp),
                KeyItem("&", "&", KeyType.LETTER, 34.dp),
                KeyItem("@", "@", KeyType.LETTER, 34.dp),
                KeyItem("\"", "\"", KeyType.LETTER, 34.dp)
            ),
            widthFraction = 0.95f
        ),
        // Row 3: ABC + more symbols + BACKSPACE
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, 50.dp),
                KeyItem("#", "#", KeyType.LETTER, 28.dp),
                KeyItem("*", "*", KeyType.LETTER, 28.dp),
                KeyItem(".", ".", KeyType.PERIOD, 36.dp),
                KeyItem(",", ",", KeyType.COMMA, 36.dp),
                KeyItem("?", "?", KeyType.LETTER, 28.dp),
                KeyItem("!", "!", KeyType.LETTER, 28.dp),
                KeyItem("'", "'", KeyType.LETTER, 28.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, 42.dp)
            )
        ),
        // Row 4: ABC + () + SPACE + _+ + ENTER
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, 50.dp),
                KeyItem("(", "(", KeyType.LETTER, 28.dp),
                KeyItem(")", ")", KeyType.LETTER, 28.dp),
                KeyItem("space", " ", KeyType.SPACE, 160.dp),
                KeyItem("_", "_", KeyType.LETTER, 28.dp),
                KeyItem("+", "+", KeyType.LETTER, 28.dp),
                KeyItem("↵", "enter", KeyType.ENTER, 50.dp)
            )
        )
    )
}
