package com.horizon.keyboard.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.horizon.keyboard.data.local.KeyboardLayouts
import com.horizon.keyboard.data.model.KeyItem
import com.horizon.keyboard.data.model.KeyType
import com.horizon.keyboard.data.model.KeyboardRow
import com.horizon.keyboard.data.repository.WordRepository

/**
 * UI state for the keyboard.
 */
data class KeyboardUiState(
    val isShift: Boolean = false,
    val isNumbers: Boolean = false,
    val text: String = ""
)

/**
 * ViewModel for keyboard functionality.
 * Manages keyboard state, layouts, and text input.
 */
class KeyboardViewModel : ViewModel() {
    
    private val _uiState = mutableStateOf(KeyboardUiState())
    val uiState: KeyboardUiState get() = _uiState.value
    
    // Callbacks for text input
    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    
    /**
     * Get the current keyboard layout based on state.
     */
    fun getLayout(): List<KeyboardRow> {
        val baseLayout = if (uiState.isNumbers) {
            KeyboardLayouts.numbers
        } else {
            KeyboardLayouts.qwerty
        }
        
        return applyShiftToLayout(baseLayout)
    }
    
    /**
     * Get word suggestions for the current text.
     */
    fun getSuggestions(text: String): List<String> {
        val currentWord = getCurrentWord(text)
        if (currentWord.isBlank()) return emptyList()
        return WordRepository.getSuggestions(currentWord)
    }
    
    /**
     * Replace the current word with a suggestion.
     */
    fun getSuggestionReplacement(currentText: String, word: String): String {
        val lastSpaceIndex = currentText.lastIndexOf(" ")
        return if (lastSpaceIndex == -1) {
            "$word "
        } else {
            currentText.substring(0, lastSpaceIndex + 1) + "$word "
        }
    }
    
    /**
     * Handle a key press event.
     */
    fun handleKeyPress(key: KeyItem) {
        when (key.type) {
            KeyType.SHIFT -> toggleShift()
            KeyType.ABC -> setNumbers(false)
            KeyType.NUMBERS -> setNumbers(true)
            KeyType.BACKSPACE -> onBackspace?.invoke()
            KeyType.ENTER -> onKeyPress?.invoke("\n")
            KeyType.SPACE -> onKeyPress?.invoke(" ")
            KeyType.LETTER -> handleLetter(key.value)
            KeyType.COMMA -> onKeyPress?.invoke(",")
            KeyType.PERIOD -> onKeyPress?.invoke(".")
        }
    }
    
    // Private helpers
    
    private fun toggleShift() {
        _uiState.value = _uiState.value.copy(isShift = !uiState.isShift)
    }
    
    private fun setNumbers(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isNumbers = enabled)
    }
    
    private fun handleLetter(value: String) {
        val char = if (uiState.isShift) value.uppercase() else value
        onKeyPress?.invoke(char)
        
        if (uiState.isShift) {
            _uiState.value = _uiState.value.copy(isShift = false)
        }
    }
    
    private fun getCurrentWord(text: String): String {
        return if (text.contains(" ")) {
            text.substringAfterLast(" ")
        } else {
            text
        }
    }
    
    private fun applyShiftToLayout(layout: List<KeyboardRow>): List<KeyboardRow> {
        if (!uiState.isShift) return layout
        
        return layout.map { row ->
            row.copy(
                keys = row.keys.map { key ->
                    if (key.value.length == 1 && key.type == KeyType.LETTER) {
                        key.copy(displayText = key.value.uppercase())
                    } else {
                        key
                    }
                }
            )
        }
    }
}
