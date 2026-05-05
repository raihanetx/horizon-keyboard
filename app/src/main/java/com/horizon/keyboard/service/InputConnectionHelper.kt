package com.horizon.keyboard.service

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * Helper for common [InputConnection] operations used by the keyboard.
 *
 * Extracted from [HorizonKeyboardService] for single-responsibility
 * and testability. All methods are pure functions on the connection.
 */
object InputConnectionHelper {

    /**
     * Commit text at the current cursor position.
     *
     * @param ic The input connection.
     * @param text Text to insert.
     */
    fun commitText(ic: InputConnection, text: String) {
        ic.commitText(text, 1)
    }

    /**
     * Handle backspace: delete selection if present, otherwise delete one character before cursor.
     *
     * @param ic The input connection.
     */
    fun handleBackspace(ic: InputConnection) {
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.selectionStart != extracted.selectionEnd) {
            // Delete selected text
            val start = minOf(extracted.selectionStart, extracted.selectionEnd)
            val end = maxOf(extracted.selectionStart, extracted.selectionEnd)
            ic.setSelection(start, end)
            ic.commitText("", 1)
        } else {
            // Delete character before cursor
            ic.deleteSurroundingText(1, 0)
        }
    }

    /**
     * Handle enter: perform IME action if set, otherwise insert newline.
     *
     * @param ic The input connection.
     * @param editorInfo Current editor info (for IME action detection).
     */
    fun handleEnter(ic: InputConnection, editorInfo: EditorInfo?) {
        val imeAction = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (imeAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(imeAction)
        } else {
            ic.commitText("\n", 1)
        }
    }

    /**
     * Send an arrow key event (up, down, left, right).
     *
     * @param ic The input connection.
     * @param keyCode The key code (e.g. [KeyEvent.KEYCODE_DPAD_UP]).
     */
    fun handleArrowKey(ic: InputConnection, keyCode: Int) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
