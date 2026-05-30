package com.horizon.keyboard.ui.keyboard

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
import com.horizon.keyboard.data.model.KeyboardRow
import com.horizon.keyboard.ui.theme.DarkContainerBorder
import com.horizon.keyboard.ui.theme.DarkKeyboardContainer
import com.horizon.keyboard.ui.theme.Dimens
import com.horizon.keyboard.ui.theme.LightContainerBorder
import com.horizon.keyboard.ui.theme.LightKeyboardContainer

/**
 * Container for the entire keyboard UI.
 * Wraps toolbar, suggestions, and key rows.
 */
@Composable
fun KeyboardContainer(
    toolbarMode: ToolbarMode,
    onToolbarAction: (ToolbarAction) -> Unit,
    voiceLanguage: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    keyboardRows: List<KeyboardRow>,
    isShiftActive: Boolean,
    onKeyClick: (com.horizon.keyboard.data.model.KeyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDark) DarkKeyboardContainer else LightKeyboardContainer)
            .drawTopBorder(isDark)
            .padding(
                start = Dimens.SpacingSm,
                end = Dimens.SpacingSm,
                top = Dimens.SpacingSm,
                bottom = Dimens.SpacingXxl
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.KeyboardMaxWidth)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Toolbar
            KeyboardToolbar(
                mode = toolbarMode,
                onToolbarAction = onToolbarAction,
                voiceLanguage = voiceLanguage
            )
            
            // Keyboard content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.SpacingXs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
            ) {
                // Suggestions
                if (suggestions.isNotEmpty()) {
                    SuggestionRow(
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick
                    )
                }
                
                // Key rows
                keyboardRows.forEach { row ->
                    KeyboardRowView(
                        row = row,
                        isShiftActive = isShiftActive,
                        onKeyClick = onKeyClick
                    )
                }
            }
        }
    }
}

private fun Modifier.drawTopBorder(isDark: Boolean): Modifier = this.drawBehind {
    val borderColor = if (isDark) DarkContainerBorder else LightContainerBorder
    val borderWidth = Dimens.BorderNormal.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, borderWidth / 2f)
        lineTo(size.width, borderWidth / 2f)
    }
    drawPath(
        path = path,
        color = borderColor,
        style = Stroke(width = borderWidth)
    )
}
