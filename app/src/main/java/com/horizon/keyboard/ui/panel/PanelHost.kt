package com.horizon.keyboard.ui.panel

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.horizon.keyboard.core.KeyboardMode

/**
 * Manages which panel is visible at any time.
 *
 * Panels: keyboard | symbol | clipboard | settings
 * Only one panel is visible at a time — transitions are explicit.
 * Tracks current mode via [currentMode] for state-driven logic.
 */
class PanelHost(private val context: Context) {

    /** Keyboard rows container */
    var keyboardPanel: LinearLayout? = null

    /** Symbol grid container */
    var symbolPanel: LinearLayout? = null

    /** Clipboard panel */
    var clipboardPanel: View? = null

    /** Settings panel */
    var settingsPanel: View? = null

    /** Callback when keyboard should be shown (e.g. panel closed). */
    var onShowKeyboard: (() -> Unit)? = null

    /** The current keyboard mode — drives all state decisions. */
    var currentMode: KeyboardMode = KeyboardMode.Typing
        private set

    // ─── Panel Switching ─────────────────────────────────────────

    fun showKeyboard() {
        currentMode = KeyboardMode.Typing
        keyboardPanel?.visibility = View.VISIBLE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.GONE
    }

    fun showSymbol() {
        currentMode = KeyboardMode.Symbol
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.VISIBLE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.GONE
    }

    fun showClipboard() {
        currentMode = KeyboardMode.Clipboard
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.VISIBLE
        settingsPanel?.visibility = View.GONE
    }

    fun showSettings() {
        currentMode = KeyboardMode.Settings
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.VISIBLE
    }

    // ─── Toggle Methods ──────────────────────────────────────────

    fun toggleClipboard() {
        if (currentMode is KeyboardMode.Clipboard) {
            showKeyboard()
        } else {
            showClipboard()
        }
    }

    fun toggleSettings() {
        if (currentMode is KeyboardMode.Settings) {
            showKeyboard()
        } else {
            showSettings()
        }
    }

    fun toggleSymbol() {
        if (currentMode is KeyboardMode.Symbol) {
            showKeyboard()
        } else {
            showSymbol()
        }
    }

    // ─── Mode Queries ────────────────────────────────────────────

    val isTyping: Boolean get() = currentMode is KeyboardMode.Typing
    val isSymbol: Boolean get() = currentMode is KeyboardMode.Symbol
    val isClipboard: Boolean get() = currentMode is KeyboardMode.Clipboard
    val isSettings: Boolean get() = currentMode is KeyboardMode.Settings
    val isVoice: Boolean get() = currentMode is KeyboardMode.Voice
}
