package com.horizon.keyboard.ui.theme

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * GradientDrawable builders for keyboard keys, pills, and panels.
 * All visual shape/background creation lives here.
 */
object Drawables {

    /**
     * Standard key background — subtle top-to-bottom gradient.
     * Used for letter keys in normal state.
     */
    fun keyBgNormal(): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor("#3D3D3F"), Color.parseColor(Colors.BG_KEY))
            cornerRadius = 10f
        }
    }

    /**
     * Key background when pressed — flat solid color.
     */
    fun keyBgPressed(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(Colors.BG_KEY_PRESSED))
            cornerRadius = 10f
        }
    }

    /**
     * Solid-color key background for special keys (shift, backspace, enter).
     * @param color Hex color string from [Colors].
     */
    fun keyBgSolid(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = 10f
        }
    }

    /**
     * Pill-shaped background for header icons and tags.
     * @param color Hex color string from [Colors].
     */
    fun pillBg(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = 20f
        }
    }

    /**
     * Generic rounded background with optional stroke.
     * Used for settings options, clipboard items, etc.
     *
     * @param color Fill color hex string.
     * @param radiusDp Corner radius in dp (default 6).
     * @param strokeColor Stroke color hex string (null = no stroke).
     * @param strokeDp Stroke width in dp (default 1).
     */
    fun roundedBg(
        color: String,
        radiusDp: Float = 6f,
        strokeColor: String? = null,
        strokeDp: Int = 1
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusDp
            if (strokeColor != null) {
                setStroke(strokeDp, Color.parseColor(strokeColor))
            }
        }
    }
}
