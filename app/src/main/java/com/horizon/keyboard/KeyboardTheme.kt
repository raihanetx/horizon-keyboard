package com.horizon.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

/**
 * Theme utilities for the keyboard — colors, drawables, dimension helpers.
 * Extracted from KeyboardView for single-responsibility.
 */
object KeyboardTheme {

    // ─── Color Constants ─────────────────────────────────────────

    const val BG_DARK = "#1C1C1E"
    const val BG_KEY = "#2C2C2E"
    const val BG_KEY_PRESSED = "#505052"
    const val BG_KEY_SOLID = "#48484A"
    const val BG_KEY_SOLID_PRESSED = "#636366"
    const val BG_PILL = "#3A3A3C"
    const val BG_PILL_PRESSED = "#505052"
    const val ACCENT_BLUE = "#0A84FF"
    const val ACCENT_BLUE_PRESSED = "#0060CC"
    const val ACCENT_GREEN = "#34C759"
    const val ACCENT_ORANGE = "#FF9F0A"
    const val ACCENT_RED = "#FF453A"
    const val TEXT_PRIMARY = "#FFFFFF"
    const val TEXT_SECONDARY = "#A0A0A8"
    const val TEXT_TERTIARY = "#636366"
    const val TEXT_DIM = "#8E8E93"
    const val DIVIDER = "#333336"
    const val SAVED_CLIP_BG = "#2A2A1C"
    const val SAVED_CLIP_BORDER = "#FF9F0A"
    const val SETTINGS_SELECTED_BG = "#1A3A5C"
    const val SETTINGS_SELECTED_BORDER = "#0A84FF"

    // ─── Background Drawables ────────────────────────────────────

    fun keyBgNormal(): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor("#3D3D3F"), Color.parseColor(BG_KEY))
            cornerRadius = 10f
        }
    }

    fun keyBgPressed(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(BG_KEY_PRESSED))
            cornerRadius = 10f
        }
    }

    fun keyBgSolid(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = 10f
        }
    }

    fun pillBg(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = 20f
        }
    }

    fun roundedBg(color: String, radiusDp: Float = 6f, strokeColor: String? = null, strokeDp: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusDp
            if (strokeColor != null) setStroke(strokeDp.toInt(), Color.parseColor(strokeColor))
        }
    }

    // ─── Dimension Helper ────────────────────────────────────────

    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics
        ).toInt()
    }

    // ─── Key Masking ─────────────────────────────────────────────

    fun maskKey(key: String): String {
        if (key.length <= 12) return key
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        val masked = "•".repeat((key.length - 8).coerceAtMost(16))
        return "$prefix$masked$suffix"
    }
}
