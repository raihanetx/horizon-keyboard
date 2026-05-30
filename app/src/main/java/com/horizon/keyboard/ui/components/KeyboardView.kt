package com.horizon.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.horizon.keyboard.ui.theme.DarkContainerBorder
import com.horizon.keyboard.ui.theme.DarkKeyboardContainer
import com.horizon.keyboard.ui.theme.LightContainerBorder
import com.horizon.keyboard.ui.theme.LightKeyboardContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.horizon.keyboard.ui.theme.DarkKeyBackground
import com.horizon.keyboard.ui.theme.DarkKeyBorder
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.DarkSuggestionText
import com.horizon.keyboard.ui.theme.LightKeyBackground
import com.horizon.keyboard.ui.theme.LightKeyBorder
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.ui.theme.LightSuggestionText
import com.horizon.keyboard.viewmodel.KeyboardViewModel

@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    toolbarMode: ToolbarMode,
    onToolbarAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDark) DarkKeyboardContainer else LightKeyboardContainer)
            .drawBehind {
                val borderWidth = 1.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, borderWidth / 2f)
                    lineTo(size.width, borderWidth / 2f)
                }
                drawPath(
                    path = path,
                    color = if (isDark) DarkContainerBorder else LightContainerBorder,
                    style = Stroke(width = borderWidth)
                )
            }
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 32.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 896.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ToolbarView(
                mode = toolbarMode,
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                onToolbarAction = onToolbarAction
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                // Word suggestions row - shown when typing and suggestions available
                if (suggestions.isNotEmpty()) {
                    SuggestionRow(
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick
                    )
                }

                val layout = viewModel.getLayout()
                layout.forEach { row ->
                    KeyboardRowView(
                        row = row,
                        isShiftActive = viewModel.isShift,
                        onKeyClick = { keyItem ->
                            viewModel.handleKeyPress(keyItem)
                        }
                    )
                }
            }
        }
    }
}

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
            .height(36.dp)
            .drawBehind {
                // Single bottom border line only
                val borderWidth = 0.5.dp.toPx()
                drawLine(
                    color = dividerColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = borderWidth
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            // Vertical divider between words
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(dividerColor)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSuggestionClick(suggestion) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}
