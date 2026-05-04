package com.horizon.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest

class HorizonKeyboardService : InputMethodService() {

    private var keyboardView: KeyboardView? = null

    override fun onCreateInputView(): View {
        return KeyboardView(this).apply {
            keyboardView = this
            onKeyPress = { char -> commitText(char) }
            onBackspace = { handleBackspace() }
            onEnter = { handleEnter() }
            onSpace = { commitText(" ") }
            onPaste = { text -> commitText(text) }
            onArrowKey = { keyCode -> handleArrowKey(keyCode) }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Update enter key appearance based on the editor's IME options
        keyboardView?.updateImeOptions(attribute?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Also update when the input view becomes visible
        keyboardView?.updateImeOptions(info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    override fun onDestroy() {
        keyboardView?.cleanup()
        keyboardView = null
        super.onDestroy()
    }

    private fun commitText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.selectionStart != extracted.selectionEnd) {
            val start = minOf(extracted.selectionStart, extracted.selectionEnd)
            val end = maxOf(extracted.selectionStart, extracted.selectionEnd)
            ic.setSelection(start, end)
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val imeAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (imeAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(imeAction)
        } else {
            ic.commitText("\n", 1)
        }
    }

    private fun handleArrowKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
