package com.horizon.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.DarkToolbarBorder
import com.horizon.keyboard.ui.theme.DarkToolbarIcon
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.ui.theme.LightToolbarBorder
import com.horizon.keyboard.ui.theme.LightToolbarIcon

enum class ToolbarMode {
    DEFAULT,
    TYPING,
    VOICE
}

@Composable
fun ToolbarView(
    mode: ToolbarMode,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
    voiceLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) DarkToolbarIcon else LightToolbarIcon
    val borderColor = if (isDark) DarkToolbarBorder else LightToolbarBorder
    val textColor = if (isDark) DarkKeyText else LightKeyText

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 8.dp)
            .drawBehind {
                val borderWidth = 1.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height - borderWidth / 2f)
                    lineTo(size.width, size.height - borderWidth / 2f)
                }
                drawPath(
                    path = path,
                    color = borderColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = borderWidth
                    )
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (mode) {
            ToolbarMode.DEFAULT -> DefaultToolbarContent(onToolbarAction, iconColor)
            ToolbarMode.TYPING -> TypingToolbarContent(onToolbarAction, iconColor)
            ToolbarMode.VOICE -> VoiceToolbarContent(onToolbarAction, iconColor, textColor, voiceLanguage)
        }
    }
}

@Composable
private fun DefaultToolbarContent(
    onToolbarAction: (ToolbarAction) -> Unit,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(Icons.Filled.Keyboard, "Keyboard Layout", { onToolbarAction(ToolbarAction.KEYBOARD_LAYOUT) }, iconColor)
        ToolbarButton(Icons.Filled.SentimentSatisfied, "Emojis", { onToolbarAction(ToolbarAction.EMOJIS) }, iconColor)
        ToolbarButton(Icons.Filled.Mic, "Voice Typing", { onToolbarAction(ToolbarAction.VOICE_TYPING) }, iconColor)
        ToolbarButton(Icons.Filled.ContentCopy, "Clipboard", { onToolbarAction(ToolbarAction.CLIPBOARD) }, iconColor)
        ToolbarButton(Icons.Filled.Language, "Translate", { onToolbarAction(ToolbarAction.TRANSLATE) }, iconColor)
        ToolbarButton(Icons.Filled.Settings, "Settings", { onToolbarAction(ToolbarAction.SETTINGS) }, iconColor)
    }
}

@Composable
private fun TypingToolbarContent(
    onToolbarAction: (ToolbarAction) -> Unit,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(Icons.Filled.Keyboard, "Keyboard Layout", { onToolbarAction(ToolbarAction.KEYBOARD_LAYOUT) }, iconColor)
        ToolbarButton(Icons.Filled.SentimentSatisfied, "Emojis", { onToolbarAction(ToolbarAction.EMOJIS) }, iconColor)
        ToolbarButton(Icons.Filled.Mic, "Voice Typing", { onToolbarAction(ToolbarAction.VOICE_TYPING) }, iconColor)
        ToolbarButton(Icons.Filled.ContentCopy, "Clipboard", { onToolbarAction(ToolbarAction.CLIPBOARD) }, iconColor)
        ToolbarButton(Icons.Filled.Language, "Translate", { onToolbarAction(ToolbarAction.TRANSLATE) }, iconColor)
        ToolbarButton(Icons.Filled.Settings, "Settings", { onToolbarAction(ToolbarAction.SETTINGS) }, iconColor)
    }
}

@Composable
private fun RowScope.VoiceToolbarContent(
    onToolbarAction: (ToolbarAction) -> Unit,
    iconColor: Color,
    textColor: Color,
    voiceLanguage: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language toggle button (EN / BN)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToolbarAction(ToolbarAction.EN_BN_TOGGLE) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "EN",
                fontSize = 14.sp,
                fontWeight = if (voiceLanguage == "EN") FontWeight.Bold else FontWeight.Normal,
                color = if (voiceLanguage == "EN") textColor else textColor.copy(alpha = 0.5f)
            )
            Text(
                text = "/",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.4f)
            )
            Text(
                text = "BN",
                fontSize = 14.sp,
                fontWeight = if (voiceLanguage == "BN") FontWeight.Bold else FontWeight.Normal,
                color = if (voiceLanguage == "BN") textColor else textColor.copy(alpha = 0.5f)
            )
        }

        // Listening indicator with mic icon
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Listening",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = " Listening...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4CAF50)
            )
        }

        // Stop button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToolbarAction(ToolbarAction.STOP_VOICE) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = "Stop",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Stop",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

enum class ToolbarAction {
    KEYBOARD_LAYOUT,
    EMOJIS,
    VOICE_TYPING,
    CLIPBOARD,
    TRANSLATE,
    SETTINGS,
    STOP_VOICE,
    EN_BN_TOGGLE
}
