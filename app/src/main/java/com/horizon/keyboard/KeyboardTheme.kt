package com.horizon.keyboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Dimensions
import com.horizon.keyboard.ui.theme.Drawables

/**
 * Compatibility shim — delegates to [Colors], [Drawables], and [Dimensions].
 *
 * Existing code keeps working unchanged. When each file is refactored in
 * later steps, imports will switch directly to the new classes and this
 * file will be deleted.
 */
object KeyboardTheme {

    // ─── Color Constants (delegated) ────────────────────────────

    const val BG_DARK = Colors.BG_DARK
    const val BG_KEY = Colors.BG_KEY
    const val BG_KEY_PRESSED = Colors.BG_KEY_PRESSED
    const val BG_KEY_SOLID = Colors.BG_KEY_SOLID
    const val BG_KEY_SOLID_PRESSED = Colors.BG_KEY_SOLID_PRESSED
    const val BG_PILL = Colors.BG_PILL
    const val BG_PILL_PRESSED = Colors.BG_PILL_PRESSED
    const val ACCENT_BLUE = Colors.ACCENT_BLUE
    const val ACCENT_BLUE_PRESSED = Colors.ACCENT_BLUE_PRESSED
    const val ACCENT_GREEN = Colors.ACCENT_GREEN
    const val ACCENT_ORANGE = Colors.ACCENT_ORANGE
    const val ACCENT_RED = Colors.ACCENT_RED
    const val TEXT_PRIMARY = Colors.TEXT_PRIMARY
    const val TEXT_SECONDARY = Colors.TEXT_SECONDARY
    const val TEXT_TERTIARY = Colors.TEXT_TERTIARY
    const val TEXT_DIM = Colors.TEXT_DIM
    const val DIVIDER = Colors.DIVIDER
    const val SAVED_CLIP_BG = Colors.SAVED_CLIP_BG
    const val SAVED_CLIP_BORDER = Colors.SAVED_CLIP_BORDER
    const val SETTINGS_SELECTED_BG = Colors.SETTINGS_SELECTED_BG
    const val SETTINGS_SELECTED_BORDER = Colors.SETTINGS_SELECTED_BORDER

    // ─── Background Drawables (delegated) ───────────────────────

    fun keyBgNormal(): GradientDrawable = Drawables.keyBgNormal()
    fun keyBgPressed(): GradientDrawable = Drawables.keyBgPressed()
    fun keyBgSolid(color: String): GradientDrawable = Drawables.keyBgSolid(color)
    fun pillBg(color: String): GradientDrawable = Drawables.pillBg(color)
    fun roundedBg(color: String, radiusDp: Float = 6f, strokeColor: String? = null, strokeDp: Int = 1): GradientDrawable =
        Drawables.roundedBg(color, radiusDp, strokeColor, strokeDp)

    // ─── Dimension Helper (delegated) ───────────────────────────

    fun dp(context: Context, value: Int): Int = Dimensions.dp(context, value)

    // ─── Key Masking (delegated) ────────────────────────────────

    fun maskKey(key: String): String = Dimensions.maskKey(key)
}
