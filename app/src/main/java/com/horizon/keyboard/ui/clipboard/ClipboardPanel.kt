package com.horizon.keyboard.ui.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.keyboard.data.clipboard.ClipboardItem

/**
 * Main clipboard panel with history and actions.
 */
@Composable
fun ClipboardPanel(
    isVisible: Boolean,
    items: List<ClipboardItem>,
    isSearchVisible: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    onPaste: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF1A202C))
        ) {
            // Toolbar
            ClipboardToolbar(
                itemCount = items.size,
                isSearchVisible = isSearchVisible,
                onToggleSearch = onToggleSearch,
                onClearAll = onClearAll,
                onClose = onClose
            )
            
            // Search field
            if (isSearchVisible) {
                SearchField(
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange
                )
            }
            
            // Items list
            if (items.isEmpty()) {
                EmptyClipboard()
            } else {
                ClipboardList(
                    items = items,
                    onPaste = onPaste,
                    onTogglePin = onTogglePin,
                    onRemove = onRemove
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    TextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        placeholder = {
            Text(
                text = "Search clipboard...",
                color = Color.White.copy(alpha = 0.4f)
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF2D3748),
            unfocusedContainerColor = Color(0xFF2D3748),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun ColumnScope.ClipboardList(
    items: List<ClipboardItem>,
    onPaste: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items) { item ->
            ClipboardItemCard(
                item = item,
                onPaste = onPaste,
                onTogglePin = onTogglePin,
                onRemove = onRemove
            )
        }
    }
}

@Composable
private fun ColumnScope.EmptyClipboard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Clipboard is empty",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}
