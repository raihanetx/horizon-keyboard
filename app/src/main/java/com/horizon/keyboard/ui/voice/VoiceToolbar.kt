package com.horizon.keyboard.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.horizon.keyboard.ui.keyboard.ToolbarAction
import com.horizon.keyboard.ui.theme.Dimens

/**
 * Voice toolbar with language selection and controls.
 */
@Composable
fun VoiceToolbar(
    onToolbarAction: (ToolbarAction) -> Unit,
    textColor: Color,
    voiceLanguage: String,
    modifier: Modifier = Modifier
) {
    val isEnglish = voiceLanguage == "English"
    val isBangla = voiceLanguage == "বাংলা"
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        // Language buttons
        LanguageButton(
            text = "English",
            isSelected = isEnglish,
            selectedColor = Color(0xFF4CAF50),
            onClick = { if (!isEnglish) onToolbarAction(ToolbarAction.EN_BN_TOGGLE) }
        )
        
        LanguageButton(
            text = "বাংলা",
            isSelected = isBangla,
            selectedColor = Color(0xFF2196F3),
            onClick = { if (!isBangla) onToolbarAction(ToolbarAction.EN_BN_TOGGLE) }
        )
        
        // Listening indicator
        ListeningIndicator(
            isBangla = isBangla,
            modifier = Modifier.weight(1f)
        )
        
        // Stop button
        StopButton(
            onClick = { onToolbarAction(ToolbarAction.STOP_VOICE) }
        )
    }
}

@Composable
private fun LanguageButton(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (isSelected) selectedColor else Color(0xFF757575))
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimens.VoiceButtonPaddingH,
                vertical = Dimens.VoiceButtonPaddingV
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = Dimens.TextMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ListeningIndicator(
    isBangla: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Listening",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(Dimens.VoiceIconSize)
        )
        Text(
            text = if (isBangla) " শুনছি..." else " Listening...",
            fontSize = Dimens.TextSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(Color(0xFFE53935))
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimens.SpacingMd,
                vertical = Dimens.VoiceButtonPaddingV
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
        ) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = "Stop",
                tint = Color.White,
                modifier = Modifier.size(Dimens.IconSmall)
            )
            Text(
                text = "Stop",
                fontSize = Dimens.TextSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
