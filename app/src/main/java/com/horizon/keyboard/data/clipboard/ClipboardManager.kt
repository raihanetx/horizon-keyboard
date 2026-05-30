package com.horizon.keyboard.data.clipboard

import java.util.UUID

/**
 * Manages clipboard history and operations.
 */
class ClipboardManager {
    
    private val _history = mutableListOf<ClipboardItem>()
    val history: List<ClipboardItem> get() = _history.toList()
    
    private val maxHistorySize = 20
    
    /**
     * Add text to clipboard history.
     */
    fun copy(text: String) {
        if (text.isBlank()) return
        
        val item = ClipboardItem(
            id = UUID.randomUUID().toString(),
            text = text.trim()
        )
        
        // Remove duplicate if exists
        _history.removeAll { it.text == item.text }
        
        // Add to beginning
        _history.add(0, item)
        
        // Trim history (keep pinned items)
        trimHistory()
    }
    
    /**
     * Get all items sorted by pinned first, then by time.
     */
    fun getSortedItems(): List<ClipboardItem> {
        return _history.sortedWith(
            compareByDescending<ClipboardItem> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
    }
    
    /**
     * Toggle pin status of an item.
     */
    fun togglePin(itemId: String) {
        val index = _history.indexOfFirst { it.id == itemId }
        if (index != -1) {
            _history[index] = _history[index].copy(isPinned = !_history[index].isPinned)
        }
    }
    
    /**
     * Remove a specific item.
     */
    fun remove(itemId: String) {
        _history.removeAll { it.id == itemId }
    }
    
    /**
     * Clear all unpinned items.
     */
    fun clearUnpinned() {
        _history.removeAll { !it.isPinned }
    }
    
    /**
     * Clear all items.
     */
    fun clearAll() {
        _history.clear()
    }
    
    /**
     * Get item by ID.
     */
    fun getItem(itemId: String): ClipboardItem? {
        return _history.find { it.id == itemId }
    }
    
    private fun trimHistory() {
        val unpinned = _history.filter { !it.isPinned }
        if (unpinned.size > maxHistorySize) {
            val toRemove = unpinned.drop(maxHistorySize)
            _history.removeAll { item -> toRemove.any { it.id == item.id } }
        }
    }
}
