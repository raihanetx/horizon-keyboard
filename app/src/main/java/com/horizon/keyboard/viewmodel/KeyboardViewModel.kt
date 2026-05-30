package com.horizon.keyboard.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.horizon.keyboard.data.KeyItem
import com.horizon.keyboard.data.KeyType
import com.horizon.keyboard.data.KeyboardLayoutData
import com.horizon.keyboard.data.KeyboardRow
import com.horizon.keyboard.data.WordDictionary

class KeyboardViewModel : ViewModel() {

    var onSelectSuggestion: ((String) -> Unit)? = null

    fun getSuggestions(text: String): List<String> {
        val currentWord = if (text.contains(" ")) {
            text.substringAfterLast(" ")
        } else {
            text
        }
        if (currentWord.isBlank()) return emptyList()
        return WordDictionary.getSuggestions(currentWord)
    }

    var isShift by mutableStateOf(false)
        private set

    var isNumbers by mutableStateOf(false)
        private set

    var onKeyPress: ((String) -> Unit)? = null

    var onBackspace: (() -> Unit)? = null

    fun getSuggestionReplacement(currentText: String, word: String): String {
        val lastSpaceIndex = currentText.lastIndexOf(" ")
        return if (lastSpaceIndex == -1) {
            "$word "
        } else {
            currentText.substring(0, lastSpaceIndex + 1) + "$word "
        }
    }

    fun getLayout(): List<KeyboardRow> {
        val baseLayout = if (isNumbers) KeyboardLayoutData.numbersLayout else KeyboardLayoutData.layout
        return baseLayout.map { row ->
            row.copy(
                keys = row.keys.map { key ->
                    if (isShift && key.value.length == 1 && key.type == KeyType.LETTER) {
                        key.copy(displayText = key.value.uppercase())
                    } else {
                        key
                    }
                }
            )
        }
    }

    fun handleKeyPress(key: KeyItem) {
        when (key.type) {
            KeyType.SHIFT -> {
                isShift = !isShift
            }
            KeyType.ABC -> {
                isNumbers = false
            }
            KeyType.NUMBERS -> {
                isNumbers = true
            }
            KeyType.BACKSPACE -> {
                onBackspace?.invoke()
            }
            KeyType.ENTER -> {
                onKeyPress?.invoke("\n")
            }
            KeyType.SPACE -> {
                onKeyPress?.invoke(" ")
            }
            KeyType.LETTER -> {
                val value = if (isShift) key.value.uppercase() else key.value
                onKeyPress?.invoke(value)
                if (isShift) {
                    isShift = false
                }
            }
            KeyType.COMMA -> {
                onKeyPress?.invoke(",")
            }
            KeyType.PERIOD -> {
                onKeyPress?.invoke(".")
            }
        }
    }
}
