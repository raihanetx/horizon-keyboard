package com.horizon.keyboard.ui.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.horizon.keyboard.data.clipboard.ClipboardItem
import com.horizon.keyboard.ui.theme.Dimens

/**
 * Single clipboard item display.
 */
@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onPaste: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(Color(0xFF2D3748))
            .clickable { onPaste(item.text) }
            .padding(Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
    ) {
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.text,
                color = Color.White,
                fontSize = Dimens.TextMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTimestamp(item.timestamp),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = Dimens.TextSmall
            )
        }
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
        ) {
            // Pin button
            IconButton(
                icon = Icons.Filled.PushPin,
                tint = if (item.isPinned) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                onClick = { onTogglePin(item.id) }
            )
            
            // Copy button
            IconButton(
                icon = Icons.Filled.ContentCopy,
                tint = Color.White.copy(alpha = 0.7f),
                onClick = { onPaste(item.text) }
            )
            
            // Delete button
            IconButton(
                icon = Icons.Filled.Delete,
                tint = Color(0xFFE53935).copy(alpha = 0.7f),
                onClick = { onRemove(item.id) }
            )
        }
    }
}

@Composable
private fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .clickable(onClick = onClick)
            .padding(Dimens.SpacingXs),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
