package com.horizon.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest

class HorizonKeyboardService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

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

    override fun onCreate() {
        super.onCreate()
        // Listen for clipboard changes system-wide
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                keyboardView?.onClipboardChanged(text)
            }
        }
        cm.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        clipboardListener?.let {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.removePrimaryClipChangedListener(it)
        }
        clipboardListener = null
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

        // Check if text is selected — delete the selection
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
}
