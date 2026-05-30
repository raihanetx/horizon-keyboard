package com.horizon.keyboard.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyboardRow
import com.horizon.keyboard.ui.theme.Dimens

/**
 * A single row of keyboard keys.
 */
@Composable
fun KeyboardRowView(
    row: KeyboardRow,
    isShiftActive: Boolean,
    onKeyClick: (KeyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(row.widthFraction),
        horizontalArrangement = Arrangement.spacedBy(
            space = Dimens.KeySpacing,
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
