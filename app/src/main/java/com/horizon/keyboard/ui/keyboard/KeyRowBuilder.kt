package com.horizon.keyboard.ui.keyboard

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.horizon.keyboard.R
import com.horizon.keyboard.ui.theme.Dimensions


/**
 * Builds keyboard row layouts (number row, letter rows, bottom row).
 *
 * Each row is a horizontal [LinearLayout] containing keys created by [KeyViewFactory].
 *
 * @param context Android context.
 * @param keyFactory Factory for creating individual key views.
 * @param onToggleSymbol Callback to toggle between keyboard and symbol panel.
 * @param onSpace Callback when space is pressed.
 * @param onToggleShift Callback to toggle shift state.
 * @param shiftKeys Mutable list to track shift key views for visual state updates.
 */
class KeyRowBuilder(
    private val context: Context,
    private val keyFactory: KeyViewFactory,
    private val onToggleSymbol: () -> Unit,
    private val onSpace: (() -> Unit)?,
    private val onToggleShift: () -> Unit,
    private val shiftKeys: MutableList<android.widget.TextView>
) {

    // ─── Base Row ────────────────────────────────────────────────

    fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Dimensions.dp(context, 44)
            ).apply {
                bottomMargin = Dimensions.dp(context, 5)
            }
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    // ─── Number Row (1-0) ────────────────────────────────────────

    fun createNumberRow(): LinearLayout {
        val row = createRow()
        "1234567890".forEach { keyFactory.addKey(row, it.toString(), textSize = 15f) }
        return row
    }

    // ─── Row 1: q w e r t y u i o p ─────────────────────────────

    fun createKeyRow1(): LinearLayout {
        val row = createRow()
        "qwertyuiop".forEach { keyFactory.addKey(row, it.toString()) }
        return row
    }

    // ─── Row 2: a s d f g h j k l ───────────────────────────────

    fun createKeyRow2(): LinearLayout {
        val row = createRow()
        "asdfghjkl".forEach { keyFactory.addKey(row, it.toString()) }
        return row
    }

    // ─── Row 3: shift z x c v b n m backspace ───────────────────

    fun createKeyRow3(): LinearLayout {
        val row = createRow()
        val shiftTv = keyFactory.addSpecialKeyWithIcon(row, R.drawable.ic_shift, 1.5f) {
            onToggleShift()
        }
        shiftKeys.add(shiftTv)
        "zxcvbnm".forEach { keyFactory.addKey(row, it.toString()) }
        keyFactory.addBackspaceKey(row)
        return row
    }

    // ─── Row 4: 123 , SPACE . enter ─────────────────────────────

    fun createKeyRow4(): LinearLayout {
        val row = createRow()
        keyFactory.addSpecialKey(row, "123", 1.5f, textSize = 11f) { onToggleSymbol() }
        keyFactory.addKeyWithIcon(row, ",", R.drawable.ic_comma)
        keyFactory.addSpecialKey(row, "SPACE", 5f, textSize = 11f) { onSpace?.invoke() }
        keyFactory.addKey(row, ".")
        row.addView(keyFactory.createEnterKey())
        return row
    }
}
