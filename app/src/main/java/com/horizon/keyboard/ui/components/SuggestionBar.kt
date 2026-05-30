package com.horizon.keyboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.DarkSeparator
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.ui.theme.LightSeparator

@Composable
fun SuggestionBar(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val color = if (isDark) DarkKeyText else LightKeyText
    val dividerColor = if (isDark) DarkSeparator else LightSeparator

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val lineY = 0f
                drawLine(
                    color = dividerColor,
                    start = androidx.compose.ui.geometry.Offset(0f, lineY),
                    end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        val itemWeight = 1f / suggestions.size
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .drawBehind {
                            drawLine(
                                color = dividerColor,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }
                )
            }
            Box(
                modifier = Modifier
                    .weight(itemWeight)
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion,
                    fontSize = 14.sp,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
