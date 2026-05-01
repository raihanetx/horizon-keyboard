package com.horizon.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

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
        }
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
        ic.deleteSurroundingText(1, 0)
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
}
