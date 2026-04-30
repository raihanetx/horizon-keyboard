package com.horizon.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val qwertyRows = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M")
)

private val symbolRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
    listOf("!", "\"", "'", ":", ";", ",", ".", "?", "/")
)

@Composable
fun HorizonKeyboardUI(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onShift: () -> Unit,
    onSpace: () -> Unit,
    onSymbol: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }

    val rows = if (isSymbolMode) symbolRows else qwertyRows
    val keyColor = Color(0xFF2C2C2E)
    val textColor = Color(0xFFE5E5E7)
    val specialKeyColor = Color(0xFF3A3A3C)
    val pressedColor = Color(0xFF636366)
    val bgColor = Color(0xFF1C1C1E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Letter rows
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { key ->
                    val displayKey = if (!isSymbolMode && isShifted) key.uppercase() else key.lowercase()
                    KeyButton(
                        label = displayKey,
                        backgroundColor = keyColor,
                        textColor = textColor,
                        pressedColor = pressedColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onKeyPress(key)
                            if (isShifted) isShifted = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Bottom row: Shift | Symbol | Space | Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift / ABC toggle
            KeyButton(
                label = if (isSymbolMode) "ABC" else if (isShifted) "⬆" else "⇧",
                backgroundColor = if (isShifted) Color(0xFF0A84FF) else specialKeyColor,
                textColor = textColor,
                pressedColor = pressedColor,
                modifier = Modifier.weight(1.5f),
                onClick = {
                    if (isSymbolMode) {
                        isSymbolMode = false
                    } else {
                        isShifted = !isShifted
                        onShift()
                    }
                }
            )

            // Symbol / Number toggle
            KeyButton(
                label = if (isSymbolMode) "ABC" else "123",
                backgroundColor = specialKeyColor,
                textColor = textColor,
                pressedColor = pressedColor,
                modifier = Modifier.weight(1.2f),
                onClick = {
                    isSymbolMode = !isSymbolMode
                    onSymbol()
                }
            )

            // Space bar
            KeyButton(
                label = "space",
                backgroundColor = keyColor,
                textColor = textColor,
                pressedColor = pressedColor,
                modifier = Modifier.weight(4f),
                onClick = onSpace
            )

            // Backspace
            KeyButton(
                label = "⌫",
                backgroundColor = specialKeyColor,
                textColor = textColor,
                pressedColor = pressedColor,
                modifier = Modifier.weight(1.5f),
                onClick = onBackspace
            )

            // Enter / Return
            KeyButton(
                label = "⏎",
                backgroundColor = Color(0xFF0A84FF),
                textColor = Color.White,
                pressedColor = Color(0xFF409CFF),
                modifier = Modifier.weight(1.3f),
                onClick = onEnter
            )
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    pressedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg = if (isPressed) pressedColor else backgroundColor

    Box(
        modifier = modifier
            .padding(2.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (label.length > 2) 11.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
