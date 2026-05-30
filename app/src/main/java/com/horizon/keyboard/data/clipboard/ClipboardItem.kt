package com.horizon.keyboard.data.clipboard

/**
 * Represents a single clipboard item.
 */
data class ClipboardItem(
    val id: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
