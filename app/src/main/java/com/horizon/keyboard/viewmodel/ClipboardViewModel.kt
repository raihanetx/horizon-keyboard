package com.horizon.keyboard.viewmodel

import androidx.lifecycle.ViewModel
import com.horizon.keyboard.data.clipboard.ClipboardItem
import com.horizon.keyboard.data.clipboard.ClipboardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClipboardUiState(
    val items: List<ClipboardItem> = emptyList(),
    val isPanelOpen: Boolean = false,
    val toastMessage: String? = null
)

class ClipboardViewModel : ViewModel() {

    private val clipboardManager = ClipboardManager()

    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

    fun copy(text: String) {
        clipboardManager.copy(text)
        refreshItems()
    }

    fun togglePin(itemId: String) {
        val item = clipboardManager.getItem(itemId)
        clipboardManager.togglePin(itemId)
        if (item != null) {
            showToast(if (item.isPinned) "Unpinned" else "Pinned")
        }
        refreshItems()
    }

    fun remove(itemId: String) {
        clipboardManager.remove(itemId)
        refreshItems()
    }

    fun clearUnpinned() {
        clipboardManager.clearUnpinned()
        refreshItems()
    }

    fun clearAll() {
        clipboardManager.clearAll()
        refreshItems()
    }

    fun togglePanel() {
        _uiState.value = _uiState.value.copy(
            isPanelOpen = !_uiState.value.isPanelOpen
        )
    }

    fun openPanel() {
        _uiState.value = _uiState.value.copy(isPanelOpen = true)
    }

    fun closePanel() {
        _uiState.value = _uiState.value.copy(isPanelOpen = false)
    }

    fun showToast(message: String) {
        _uiState.value = _uiState.value.copy(toastMessage = message)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun getRecentItems(): List<ClipboardItem> {
        return clipboardManager.history
            .filter { !it.isPinned }
            .sortedByDescending { it.timestamp }
    }

    fun getPinnedItems(): List<ClipboardItem> {
        return clipboardManager.history
            .filter { it.isPinned }
            .sortedByDescending { it.timestamp }
    }

    private fun refreshItems() {
        _uiState.value = _uiState.value.copy(
            items = clipboardManager.getSortedItems()
        )
    }
}
