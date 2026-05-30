package com.horizon.keyboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.horizon.keyboard.data.KeyboardRow

@Composable
fun KeyboardRowView(
    row: KeyboardRow,
    isShiftActive: Boolean,
    onKeyClick: (com.horizon.keyboard.data.KeyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(row.widthFraction),
        horizontalArrangement = Arrangement.spacedBy(
            space = 6.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        row.keys.forEach { keyItem ->
            val widthModifier = if (keyItem.widthDp != null) {
                Modifier.width(keyItem.widthDp)
            } else {
                Modifier
            }
            KeyButton(
                keyItem = keyItem,
                isShiftActive = isShiftActive,
                onClick = { onKeyClick(keyItem) },
                modifier = widthModifier
            )
        }
    }
}
