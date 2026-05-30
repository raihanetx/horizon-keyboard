package com.horizon.keyboard.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class KeyItem(
    val displayText: String,
    val value: String,
    val type: KeyType,
    val widthDp: Dp? = null
)

data class KeyboardRow(
    val keys: List<KeyItem>,
    val widthFraction: Float = 1.0f
)

object KeyboardLayoutData {

    val layout: List<KeyboardRow> = listOf(
        KeyboardRow(
            keys = listOf(
                KeyItem("q", "q", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("w", "w", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("e", "e", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("r", "r", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("t", "t", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("y", "y", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("u", "u", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("i", "i", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("o", "o", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("p", "p", KeyType.LETTER, widthDp = 32.dp)
            ),
            widthFraction = 1.0f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("a", "a", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("s", "s", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("d", "d", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("f", "f", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("g", "g", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("h", "h", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("j", "j", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("k", "k", KeyType.LETTER, widthDp = 34.14.dp),
                KeyItem("l", "l", KeyType.LETTER, widthDp = 34.14.dp)
            ),
            widthFraction = 0.95f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("⇧", "shift", KeyType.SHIFT, widthDp = 40.dp),
                KeyItem("z", "z", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("x", "x", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("c", "c", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("v", "v", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("b", "b", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("n", "n", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("m", "m", KeyType.LETTER, widthDp = 35.14.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, widthDp = 40.dp)
            ),
            widthFraction = 1.0f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("123", "123", KeyType.NUMBERS, widthDp = 48.dp),
                KeyItem(",", ",", KeyType.COMMA, widthDp = 40.dp),
                KeyItem("space", " ", KeyType.SPACE, widthDp = 174.dp),
                KeyItem(".", ".", KeyType.PERIOD, widthDp = 40.dp),
                KeyItem("↵", "enter", KeyType.ENTER, widthDp = 48.dp)
            ),
            widthFraction = 1.0f
        )
    )

    val numbersLayout: List<KeyboardRow> = listOf(
        KeyboardRow(
            keys = listOf(
                KeyItem("1", "1", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("2", "2", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("3", "3", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("4", "4", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("5", "5", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("6", "6", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("7", "7", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("8", "8", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("9", "9", KeyType.LETTER, widthDp = 32.dp),
                KeyItem("0", "0", KeyType.LETTER, widthDp = 32.dp)
            ),
            widthFraction = 1.0f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("-", "-", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("/", "/", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem(":", ":", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem(";", ";", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("(", "(", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem(")", ")", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("$", "$", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("&", "&", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("@", "@", KeyType.LETTER, widthDp = 33.53.dp),
                KeyItem("\"", "\"", KeyType.LETTER, widthDp = 33.53.dp)
            ),
            widthFraction = 0.95f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, widthDp = 48.dp),
                KeyItem("#", "#", KeyType.LETTER, widthDp = 26.33.dp),
                KeyItem("*", "*", KeyType.LETTER, widthDp = 26.33.dp),
                KeyItem(".", ".", KeyType.PERIOD, widthDp = 40.dp),
                KeyItem(",", ",", KeyType.COMMA, widthDp = 40.dp),
                KeyItem("?", "?", KeyType.LETTER, widthDp = 26.33.dp),
                KeyItem("!", "!", KeyType.LETTER, widthDp = 26.33.dp),
                KeyItem("'", "'", KeyType.LETTER, widthDp = 26.33.dp),
                KeyItem("⌫", "backspace", KeyType.BACKSPACE, widthDp = 40.dp)
            ),
            widthFraction = 1.0f
        ),
        KeyboardRow(
            keys = listOf(
                KeyItem("ABC", "abc", KeyType.ABC, widthDp = 48.dp),
                KeyItem("(", "(", KeyType.LETTER, widthDp = 26.67.dp),
                KeyItem(")", ")", KeyType.LETTER, widthDp = 26.67.dp),
                KeyItem("space", " ", KeyType.SPACE, widthDp = 174.dp),
                KeyItem("_", "_", KeyType.LETTER, widthDp = 26.67.dp),
                KeyItem("+", "+", KeyType.LETTER, widthDp = 26.67.dp),
                KeyItem("↵", "enter", KeyType.ENTER, widthDp = 48.dp)
            ),
            widthFraction = 1.0f
        )
    )
}
