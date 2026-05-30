package com.horizon.keyboard.data.local

import androidx.compose.ui.unit.dp
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyType
import com.horizon.keyboard.data.model.KeyboardRow

/**
 * Defines all keyboard layouts with 9 keys per row.
 */
object KeyboardLayouts {
    
    /**
     * Standard QWERTY layout - 9 keys per row.
     */
    val qwerty: List<KeyboardRow> = listOf(
        // Row 1: QWERTYUIO (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("q", "q", KeyType.LETTER, 36.dp),
                KeyItem("w", "w", KeyType.LETTER, 36.dp),
                KeyItem("e", "e", KeyType.LETTER, 36.dp),
                KeyItem("r", "r", KeyType.LETTER, 36.dp),
                KeyItem("t", "t", KeyType.LETTER, 36.dp),
                KeyItem("y", "y", KeyType.LETTER, 36.dp),
                KeyItem("u", "u", KeyType.LETTER, 36.dp),
                KeyItem("i", "i", KeyType.LETTER, 36.dp),
                KeyItem("o", "o", KeyType.LETTER, 36.dp)
            )
        ),
        // Row 2: ASDFGHJKL (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("a", "a", KeyType.LETTER, 36.dp),
                KeyItem("s", "s", KeyType.LETTER, 36.dp),
                KeyItem("d", "d", KeyType.LETTER, 36.dp),
                KeyItem("f", "f", KeyType.LETTER, 36.dp),
                KeyItem("g", "g", KeyType.LETTER, 36.dp),
                KeyItem("h", "h", KeyType.LETTER, 36.dp),
                KeyItem("j", "j", KeyType.LETTER, 36.dp),
                KeyItem("k", "k", KeyType.LETTER, 36.dp),
                KeyItem("l", "l", KeyType.LETTER, 36.dp)
            ),
            widthFraction = 0.95f
        ),
        // Row 3: SHIFT + ZXCVBNM + BACKSPACE (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("⇧", "shift", KeyType.SHIFT, 42.dp),
                KeyItem("z", "z", KeyType.LETTER, 36.dp),
                KeyItem("x", "x", KeyType.LETTER, 36.dp),
                KeyItem("c", "c", KeyType.LETTER, 36.dp),
                KeyItem("v", "v", KeyType.LETTER, 36.dp),
                KeyItem("b", "b", KeyType.LETTER, 36.dp),
                KeyItem("n", "n", KeyType.LETTER, 36.dp),
                KeyItem("m", "m", KeyType.LETTER, 36.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, 42.dp)
            )
        ),
        // Row 4: 123 + , + SPACE + . + ENTER (5 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("123", "123", KeyType.NUMBERS, 50.dp),
                KeyItem(",", ",", KeyType.COMMA, 40.dp),
                KeyItem("space", " ", KeyType.SPACE, 180.dp),
                KeyItem(".", ".", KeyType.PERIOD, 40.dp),
                KeyItem("↵", "enter", KeyType.ENTER, 50.dp)
            )
        )
    )
    
    /**
     * Numbers and symbols layout - 9 keys per row.
     */
    val numbers: List<KeyboardRow> = listOf(
        // Row 1: 123456789 (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("1", "1", KeyType.LETTER, 36.dp),
                KeyItem("2", "2", KeyType.LETTER, 36.dp),
                KeyItem("3", "3", KeyType.LETTER, 36.dp),
                KeyItem("4", "4", KeyType.LETTER, 36.dp),
                KeyItem("5", "5", KeyType.LETTER, 36.dp),
                KeyItem("6", "6", KeyType.LETTER, 36.dp),
                KeyItem("7", "7", KeyType.LETTER, 36.dp),
                KeyItem("8", "8", KeyType.LETTER, 36.dp),
                KeyItem("9", "9", KeyType.LETTER, 36.dp)
            )
        ),
        // Row 2: Symbols (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("@", "@", KeyType.LETTER, 36.dp),
                KeyItem("#", "#", KeyType.LETTER, 36.dp),
                KeyItem("$", "$", KeyType.LETTER, 36.dp),
                KeyItem("%", "%", KeyType.LETTER, 36.dp),
                KeyItem("&", "&", KeyType.LETTER, 36.dp),
                KeyItem("*", "*", KeyType.LETTER, 36.dp),
                KeyItem("-", "-", KeyType.LETTER, 36.dp),
                KeyItem("+", "+", KeyType.LETTER, 36.dp),
                KeyItem("=", "=", KeyType.LETTER, 36.dp)
            ),
            widthFraction = 0.95f
        ),
        // Row 3: More symbols (9 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, 42.dp),
                KeyItem("(", "(", KeyType.LETTER, 36.dp),
                KeyItem(")", ")", KeyType.LETTER, 36.dp),
                KeyItem("!", "!", KeyType.LETTER, 36.dp),
                KeyItem("?", "?", KeyType.LETTER, 36.dp),
                KeyItem("\"", "\"", KeyType.LETTER, 36.dp),
                KeyItem("'", "'", KeyType.LETTER, 36.dp),
                KeyItem("/", "/", KeyType.LETTER, 36.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, 42.dp)
            )
        ),
        // Row 4: ABC + SPACE + ENTER (5 keys)
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, 50.dp),
                KeyItem(":", ":", KeyType.LETTER, 40.dp),
                KeyItem("space", " ", KeyType.SPACE, 180.dp),
                KeyItem(";", ";", KeyType.LETTER, 40.dp),
                KeyItem("↵", "enter", KeyType.ENTER, 50.dp)
            )
        )
    )
}
