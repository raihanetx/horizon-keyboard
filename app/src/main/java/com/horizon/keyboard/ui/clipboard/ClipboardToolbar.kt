package com.horizon.keyboard.ui.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Toolbar for clipboard panel.
 */
@Composable
fun ClipboardToolbar(
    itemCount: Int,
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF1A202C))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side - Title and count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Clipboard",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "($itemCount)",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        
        // Right side - Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search button
            ToolbarButton(
                icon = Icons.Filled.Search,
                tint = if (isSearchVisible) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
                onClick = onToggleSearch
            )
            
            // Clear all button
            ToolbarButton(
                icon = Icons.Filled.DeleteSweep,
                tint = Color.White.copy(alpha = 0.7f),
                onClick = onClearAll
            )
            
            // Close button
            ToolbarButton(
                icon = Icons.Filled.Close,
                tint = Color.White.copy(alpha = 0.7f),
                onClick = onClose
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
