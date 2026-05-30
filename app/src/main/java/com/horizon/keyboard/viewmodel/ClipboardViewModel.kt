package com.horizon.keyboard.viewmodel

import androidx.lifecycle.ViewModel
import com.horizon.keyboard.data.clipboard.ClipboardItem
import com.horizon.keyboard.data.clipboard.ClipboardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for clipboard panel.
 */
data class ClipboardUiState(
    val items: List<ClipboardItem> = emptyList(),
    val isPanelOpen: Boolean = false,
    val searchText: String = ""
)

/**
 * ViewModel for clipboard functionality.
 */
class ClipboardViewModel : ViewModel() {
    
    private val clipboardManager = ClipboardManager()
    
    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()
    
    /**
     * Copy text to clipboard.
     */
    fun copy(text: String) {
        clipboardManager.copy(text)
        refreshItems()
    }
    
    /**
     * Toggle pin status of an item.
     */
    fun togglePin(itemId: String) {
        clipboardManager.togglePin(itemId)
        refreshItems()
    }
    
    /**
     * Remove a specific item.
     */
    fun remove(itemId: String) {
        clipboardManager.remove(itemId)
        refreshItems()
    }
    
    /**
     * Clear all unpinned items.
     */
    fun clearUnpinned() {
        clipboardManager.clearUnpinned()
        refreshItems()
    }
    
    /**
     * Clear all items.
     */
    fun clearAll() {
        clipboardManager.clearAll()
        refreshItems()
    }
    
    /**
     * Toggle clipboard panel visibility.
     */
    fun togglePanel() {
        _uiState.value = _uiState.value.copy(
            isPanelOpen = !_uiState.value.isPanelOpen
        )
    }
    
    /**
     * Open clipboard panel.
     */
    fun openPanel() {
        _uiState.value = _uiState.value.copy(isPanelOpen = true)
    }
    
    /**
     * Close clipboard panel.
     */
    fun closePanel() {
        _uiState.value = _uiState.value.copy(isPanelOpen = false)
    }
    
    /**
     * Update search text for filtering.
     */
    fun updateSearch(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        refreshItems()
    }
    
    /**
     * Get filtered items based on search.
     */
    fun getFilteredItems(): List<ClipboardItem> {
        val items = clipboardManager.getSortedItems()
        val search = _uiState.value.searchText
        
        return if (search.isBlank()) {
            items
        } else {
            items.filter { it.text.contains(search, ignoreCase = true) }
        }
    }
    
    private fun refreshItems() {
        _uiState.value = _uiState.value.copy(
            items = getFilteredItems()
        )
    }
}
