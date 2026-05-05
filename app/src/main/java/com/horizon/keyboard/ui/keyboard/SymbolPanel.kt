package com.horizon.keyboard.ui.keyboard

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.horizon.keyboard.R

/**
 * Builds the symbol/number panel (4 rows of symbols).
 *
 * Shown when user taps "123" key. Has an "ABC" key to switch back.
 *
 * @param context Android context.
 * @param keyFactory Factory for creating symbol and special keys.
 * @param onToggleKeyboard Callback to switch back to the main keyboard.
 */
class SymbolPanel(
    private val context: Context,
    private val keyFactory: KeyViewFactory,
    private val rowBuilder: KeyRowBuilder,
    private val onToggleKeyboard: () -> Unit
) {

    /**
     * Create the full symbol panel (hidden by default).
     * Contains 4 rows: numbers, common symbols, brackets/punctuation, remaining symbols.
     */
    fun create(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }

        // Row 1: 1 2 3 4 5 6 7 8 9 0
        val row1 = rowBuilder.createRow()
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach {
            keyFactory.addSymbolKey(row1, it)
        }
        panel.addView(row1)

        // Row 2: @ # $ % & * - + = /
        val row2 = rowBuilder.createRow()
        listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "/").forEach {
            keyFactory.addSymbolKey(row2, it)
        }
        panel.addView(row2)

        // Row 3: ABC ( " ' : ; ! ? ,
        val row3 = rowBuilder.createRow()
        keyFactory.addSpecialKey(row3, "ABC", 1.5f, textSize = 11f) { onToggleKeyboard() }
        listOf("(", ")", "\"", "'", ":", ";", "!", "?", ",").forEach {
            keyFactory.addSymbolKey(row3, it)
        }
        panel.addView(row3)

        // Row 4: backspace _ ~ ` | \ { } [ ]
        val row4 = rowBuilder.createRow()
        keyFactory.addBackspaceKey(row4)
        listOf("_", "~", "`", "|", "\\", "{", "}", "[", "]").forEach {
            keyFactory.addSymbolKey(row4, it)
        }
        panel.addView(row4)

        return panel
    }
}
