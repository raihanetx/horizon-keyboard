package com.horizon.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.horizon.keyboard.service.InputConnectionHelper

/**
 * Horizon Keyboard IME Service.
 *
 * Thin lifecycle shell — delegates all text operations to [InputConnectionHelper]
 * and all UI logic to [KeyboardView].
 *
 * Also handles clipboard monitoring — detects when user copies text in any app
 * and feeds it to the clipboard panel for history tracking.
 */
class HorizonKeyboardService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

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

    override fun onCreate() {
        super.onCreate()
        startClipboardMonitor()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.updateImeOptions(attribute?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.updateImeOptions(info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED)
    }

    /**
     * Called when the keyboard window is hidden (user dismisses keyboard, switches apps, etc).
     * This is the MOST RELIABLE place to stop the mic — fires even if onFinishInput doesn't.
     */
    override fun onWindowHidden() {
        super.onWindowHidden()
        keyboardView?.stopAllVoice()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        keyboardView?.stopAllVoice()
    }

    override fun onDestroy() {
        stopClipboardMonitor()
        keyboardView?.cleanup()
        keyboardView = null
        super.onDestroy()
    }

    // ─── Clipboard Monitoring ────────────────────────────────────

    private fun startClipboardMonitor() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    // Feed to keyboard view's clipboard panel
                    keyboardView?.onClipboardChanged(text)
                }
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun stopClipboardMonitor() {
        clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        clipboardListener = null
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
