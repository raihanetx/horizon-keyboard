package com.horizon.keyboard.ui.components

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
import androidx.compose.ui.unit.sp
import com.horizon.keyboard.data.KeyItem
import com.horizon.keyboard.data.KeyType
import com.horizon.keyboard.ui.theme.DarkKeyBackground
import com.horizon.keyboard.ui.theme.DarkKeyBorder
import com.horizon.keyboard.ui.theme.DarkKeyPressActive
import com.horizon.keyboard.ui.theme.DarkKeyText
import com.horizon.keyboard.ui.theme.DarkShiftActiveBackground
import com.horizon.keyboard.ui.theme.DarkShiftActiveBorder
import com.horizon.keyboard.ui.theme.DarkShiftActiveText
import com.horizon.keyboard.ui.theme.DarkSpecialKeyBackground
import com.horizon.keyboard.ui.theme.LightKeyBackground
import com.horizon.keyboard.ui.theme.LightKeyBorder
import com.horizon.keyboard.ui.theme.LightKeyPressActive
import com.horizon.keyboard.ui.theme.LightKeyText
import com.horizon.keyboard.ui.theme.LightShiftActiveBackground
import com.horizon.keyboard.ui.theme.LightShiftActiveBorder
import com.horizon.keyboard.ui.theme.LightShiftActiveText
import com.horizon.keyboard.ui.theme.LightSpecialKeyBackground

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
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .height(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.background)
            .drawBehind {
                val borderWidth = 1.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height - borderWidth / 2f)
                    lineTo(size.width, size.height - borderWidth / 2f)
                }
                drawPath(
                    path = path,
                    color = colors.border,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = borderWidth
                    )
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        KeyContent(keyItem, colors)
    }
}

@Composable
private fun KeyContent(
    keyItem: KeyItem,
    colors: KeyColors
) {
    when (keyItem.type) {
        KeyType.SHIFT -> {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Shift",
                tint = colors.text,
                modifier = Modifier.size(20.dp)
            )
        }
        KeyType.BACKSPACE -> {
            Icon(
                imageVector = Icons.Filled.Backspace,
                contentDescription = "Backspace",
                tint = colors.text,
                modifier = Modifier.size(20.dp)
            )
        }
        KeyType.ENTER -> {
            Icon(
                imageVector = Icons.Filled.KeyboardReturn,
                contentDescription = "Enter",
                tint = colors.text,
                modifier = Modifier.size(20.dp)
            )
        }
        else -> {
            Text(
                text = keyItem.displayText,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private data class KeyColors(
    val background: Color,
    val text: Color,
    val border: Color
)

@Composable
private fun getKeyColors(
    keyItem: KeyItem,
    isShiftActive: Boolean,
    isDark: Boolean,
    isPressed: Boolean
): KeyColors {
    if (isPressed) {
        return KeyColors(
            background = if (isDark) DarkKeyPressActive else LightKeyPressActive,
            text = if (isDark) DarkKeyText else LightKeyText,
            border = if (isDark) DarkKeyBorder else LightKeyBorder
        )
    }

    return when (keyItem.type) {
        KeyType.SHIFT -> {
            if (isShiftActive) {
                KeyColors(
                    background = if (isDark) DarkShiftActiveBackground else LightShiftActiveBackground,
                    text = if (isDark) DarkShiftActiveText else LightShiftActiveText,
                    border = if (isDark) DarkShiftActiveBorder else LightShiftActiveBorder
                )
            } else {
                KeyColors(
                    background = if (isDark) DarkSpecialKeyBackground else LightSpecialKeyBackground,
                    text = if (isDark) DarkKeyText else LightKeyText,
                    border = if (isDark) DarkKeyBorder else LightKeyBorder
                )
            }
        }
        KeyType.BACKSPACE, KeyType.ENTER, KeyType.NUMBERS, KeyType.ABC -> {
            KeyColors(
                background = if (isDark) DarkSpecialKeyBackground else LightSpecialKeyBackground,
                text = if (isDark) DarkKeyText else LightKeyText,
                border = if (isDark) DarkKeyBorder else LightKeyBorder
            )
        }
        else -> {
            KeyColors(
                background = if (isDark) DarkKeyBackground else LightKeyBackground,
                text = if (isDark) DarkKeyText else LightKeyText,
                border = if (isDark) DarkKeyBorder else LightKeyBorder
            )
        }
    }
}
