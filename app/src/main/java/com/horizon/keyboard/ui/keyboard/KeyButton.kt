package com.horizon.keyboard.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyType
import com.horizon.keyboard.ui.theme.Dimens
import com.horizon.keyboard.ui.theme.getKeyColors
import com.horizon.keyboard.ui.theme.KeyColorScheme

/**
 * Individual keyboard key button with press animation.
 */
@Composable
fun KeyButton(
    keyItem: KeyItem,
    isShiftActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 50),
        label = "key_press_scale"
    )
    
    val colors = getKeyColors(keyItem, isShiftActive, isDark, isPressed)
    val shape = RoundedCornerShape(Dimens.RadiusSm)
    
    Box(
        modifier = modifier
            .height(Dimens.KeyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.background)
            .drawBorder(colors.border)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        KeyContent(keyItem, colors)
    }
}

@Composable
private fun KeyContent(keyItem: KeyItem, colors: KeyColorScheme) {
    when (keyItem.type) {
        KeyType.SHIFT -> ShiftIcon(colors.text)
        KeyType.BACKSPACE -> BackspaceIcon(colors.text)
        KeyType.ENTER -> EnterIcon(colors.text)
        else -> LetterText(keyItem.displayText, colors.text)
    }
}

@Composable
private fun ShiftIcon(tint: Color) {
    Icon(
        imageVector = Icons.Filled.ArrowUpward,
        contentDescription = "Shift",
        tint = tint,
        modifier = Modifier.size(Dimens.IconLarge)
    )
}

@Composable
private fun BackspaceIcon(tint: Color) {
    Icon(
        imageVector = Icons.Filled.Backspace,
        contentDescription = "Backspace",
        tint = tint,
        modifier = Modifier.size(Dimens.IconLarge)
    )
}

@Composable
private fun EnterIcon(tint: Color) {
    Icon(
        imageVector = Icons.Filled.KeyboardReturn,
        contentDescription = "Enter",
        tint = tint,
        modifier = Modifier.size(Dimens.IconLarge)
    )
}

@Composable
private fun LetterText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = Dimens.TextKey,
        fontWeight = FontWeight.Normal
    )
}

private fun Modifier.drawBorder(borderColor: Color): Modifier = this.drawBehind {
    val borderWidth = Dimens.BorderNormal.toPx()
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, size.height - borderWidth / 2f)
        lineTo(size.width, size.height - borderWidth / 2f)
    }
    drawPath(
        path = path,
        color = borderColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth)
    )
}
