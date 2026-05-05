package com.horizon.keyboard.ui.theme

import android.content.Context
import android.util.TypedValue

/**
 * Dimension helpers and utility functions for the keyboard UI.
 */
object Dimensions {

    /**
     * Convert dp value to pixels based on device display metrics.
     * Use this everywhere instead of raw pixel values.
     */
    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Mask an API key for display in the settings panel.
     * Shows first 4 and last 4 characters, masks the middle with bullets.
     * Short keys (≤12 chars) are shown in full.
     */
    fun maskKey(key: String): String {
        if (key.length <= 12) return key
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        val masked = "•".repeat((key.length - 8).coerceAtMost(16))
        return "$prefix$masked$suffix"
    }
}
