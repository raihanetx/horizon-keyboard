package com.horizon.keyboard.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography matching Tailwind's font-sans stack:
 * ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont,
 * "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif
 *
 * Key font sizes:
 * - Default: text-sm (14px / 20px)
 * - md+: md:text-base (16px / 24px)
 */
object KeyboardTypography {

    val keyTextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val keyTextStyleCompact = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val toolbarTextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}
