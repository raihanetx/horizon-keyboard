package com.horizon.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.horizon.keyboard.service.InputConnectionHelper

/**
 * Horizon Keyboard IME Service.
 *
 * Thin lifecycle shell — delegates all text operations to [InputConnectionHelper]
 * and all UI logic to [KeyboardView].
 */
class HorizonKeyboardService : InputMethodService() {

    private var keyboardView: KeyboardView? = null

    // ─── Lifecycle ───────────────────────────────────────────────

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
        keyboardView?.updateImeOptions(attribute?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.updateImeOptions(info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    override fun onDestroy() {
        keyboardView?.cleanup()
        keyboardView = null
        super.onDestroy()
    }

    // ─── Input Delegation ────────────────────────────────────────

    private fun commitText(text: String) {
        val ic = currentInputConnection ?: return
        InputConnectionHelper.commitText(ic, text)
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        InputConnectionHelper.handleBackspace(ic)
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        InputConnectionHelper.handleEnter(ic, currentInputEditorInfo)
    }

    private fun handleArrowKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        InputConnectionHelper.handleArrowKey(ic, keyCode)
    }
}
