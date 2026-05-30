package com.horizon.keyboard.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.DarkToolbarBorder
import com.horizon.keyboard.ui.theme.DarkToolbarIcon
import com.horizon.keyboard.ui.theme.Dimens
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.ui.theme.LightToolbarBorder
import com.horizon.keyboard.ui.theme.LightToolbarIcon

/**
 * Toolbar modes.
 */
enum class ToolbarMode {
    DEFAULT,
    TYPING,
    VOICE
}

/**
 * Toolbar actions.
 */
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

/**
 * Main toolbar with different modes.
 */
@Composable
fun KeyboardToolbar(
    mode: ToolbarMode,
    onToolbarAction: (ToolbarAction) -> Unit,
    voiceLanguage: String = "English",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) DarkToolbarIcon else LightToolbarIcon
    val borderColor = if (isDark) DarkToolbarBorder else LightToolbarBorder
    val textColor = if (isDark) DarkKeyText else LightKeyText
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ToolbarHeight)
            .padding(horizontal = Dimens.SpacingSm)
            .drawBottomBorder(borderColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (mode) {
            ToolbarMode.DEFAULT -> DefaultToolbar(onToolbarAction, iconColor)
            ToolbarMode.TYPING -> DefaultToolbar(onToolbarAction, iconColor)
            ToolbarMode.VOICE -> VoiceToolbar(
                onToolbarAction = onToolbarAction,
                textColor = textColor,
                voiceLanguage = voiceLanguage
            )
        }
    }
}

@Composable
private fun DefaultToolbar(
    onToolbarAction: (ToolbarAction) -> Unit,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(Icons.Filled.Keyboard, "Keyboard", { onToolbarAction(ToolbarAction.KEYBOARD_LAYOUT) }, iconColor)
        ToolbarButton(Icons.Filled.SentimentSatisfied, "Emojis", { onToolbarAction(ToolbarAction.EMOJIS) }, iconColor)
        ToolbarButton(Icons.Filled.Mic, "Voice", { onToolbarAction(ToolbarAction.VOICE_TYPING) }, iconColor)
        ToolbarButton(Icons.Filled.ContentCopy, "Clipboard", { onToolbarAction(ToolbarAction.CLIPBOARD) }, iconColor)
        ToolbarButton(Icons.Filled.Language, "Translate", { onToolbarAction(ToolbarAction.TRANSLATE) }, iconColor)
        ToolbarButton(Icons.Filled.Settings, "Settings", { onToolbarAction(ToolbarAction.SETTINGS) }, iconColor)
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
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(Dimens.SpacingSm),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(Dimens.ToolbarIconSize)
        )
    }
}

private fun Modifier.drawBottomBorder(
    color: Color
): Modifier = this.drawBehind {
    val borderWidth = Dimens.BorderNormal.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height - borderWidth / 2f)
        lineTo(size.width, size.height - borderWidth / 2f)
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth)
    )
}
