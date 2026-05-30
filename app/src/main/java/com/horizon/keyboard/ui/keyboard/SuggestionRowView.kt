package com.horizon.keyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.horizon.keyboard.ui.theme.DarkKeyBorder
import com.horizon.keyboard.ui.theme.DarkSuggestionText
import com.horizon.keyboard.ui.theme.Dimens
import com.horizon.keyboard.ui.theme.LightKeyBorder
import com.horizon.keyboard.ui.theme.LightSuggestionText

/**
 * Row displaying word suggestions above the keyboard.
 */
@Composable
fun SuggestionRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) DarkSuggestionText else LightSuggestionText
    val dividerColor = if (isDark) DarkKeyBorder else LightKeyBorder
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.SuggestionRowHeight)
            .drawBottomBorder(dividerColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) {
                VerticalDivider(dividerColor)
            }
            SuggestionItem(
                text = suggestion,
                color = textColor,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun RowScope.SuggestionItem(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = Dimens.TextMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(Dimens.BorderNormal)
            .height(Dimens.SuggestionDividerHeight)
            .background(color)
    )
}

private fun Modifier.drawBottomBorder(color: Color): Modifier = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = Dimens.BorderThin.toPx()
    )
}
