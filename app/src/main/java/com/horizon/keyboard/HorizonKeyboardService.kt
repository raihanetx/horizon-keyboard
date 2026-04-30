package com.horizon.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class HorizonKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // CRITICAL FIX: Set lifecycle owner on the IME window's decor view.
        // ComposeView's WindowRecomposer walks up the view tree to find a
        // LifecycleOwner. If it hits the window root without finding one, it crashes.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setContent {
                HorizonKeyboardUI(
                    onKeyPress = { char -> handleCharInput(char) },
                    onBackspace = { handleBackspace() },
                    onEnter = { handleEnter() },
                    onShift = { handleShift() },
                    onSpace = { handleSpace() },
                    onSymbol = { handleSymbolToggle() },
                    onVoiceText = { text -> handleVoiceText(text) }
                )
            }
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private var isShifted = false
    private var isSymbolMode = false

    private fun handleCharInput(char: String) {
        val ic = currentInputConnection ?: return
        val output = if (isShifted) char.uppercase() else char
        ic.commitText(output, 1)
        if (isShifted) {
            isShifted = false
        }
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

    private fun handleShift() {
        isShifted = !isShifted
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
    }

    private fun handleSymbolToggle() {
        isSymbolMode = !isSymbolMode
    }

    /**
     * Commits voice-recognized text to the current input field.
     * Adds a trailing space for natural flow.
     */
    private fun handleVoiceText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText("$text ", 1)
    }
}
