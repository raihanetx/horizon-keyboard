package com.horizon.keyboard.ui.panel

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Manages which panel is visible at any time.
 *
 * Panels: keyboard | symbol | clipboard | settings
 * Only one panel is visible at a time — transitions are explicit.
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

    // ─── Panel Switching ─────────────────────────────────────────

    fun showKeyboard() {
        keyboardPanel?.visibility = View.VISIBLE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.GONE
    }

    fun showSymbol() {
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.VISIBLE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.GONE
    }

    fun showClipboard() {
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.VISIBLE
        settingsPanel?.visibility = View.GONE
    }

    fun showSettings() {
        keyboardPanel?.visibility = View.GONE
        symbolPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.VISIBLE
    }

    // ─── Toggle Methods ──────────────────────────────────────────

    fun toggleClipboard() {
        if (clipboardPanel?.visibility == View.VISIBLE) {
            showKeyboard()
        } else {
            showClipboard()
        }
    }

    fun toggleSettings() {
        if (settingsPanel?.visibility == View.VISIBLE) {
            showKeyboard()
        } else {
            showSettings()
        }
    }

    fun toggleSymbol() {
        if (symbolPanel?.visibility == View.VISIBLE) {
            showKeyboard()
        } else {
            showSymbol()
        }
    }
}
