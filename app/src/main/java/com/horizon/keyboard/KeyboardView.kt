package com.horizon.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.horizon.keyboard.ui.bar.HeaderBar
import com.horizon.keyboard.ui.bar.SuggestionBar
import com.horizon.keyboard.ui.keyboard.KeyPopupManager
import com.horizon.keyboard.ui.keyboard.KeyRowBuilder
import com.horizon.keyboard.ui.keyboard.KeyViewFactory
import com.horizon.keyboard.ui.keyboard.SymbolPanel

/**
 * Horizon Keyboard — Core keyboard layout coordinator.
 *
 * Assembles the keyboard from extracted components:
 * - [HeaderBar] for the top toolbar
 * - [SuggestionBar] for word suggestions
 * - [KeyViewFactory] for key creation
 * - [KeyRowBuilder] for row construction
 * - [SymbolPanel] for the symbol grid
 * - [KeyboardVoiceManager] for voice recognition
 * - [KeyboardSettingsManager] for settings panel
 * - [KeyboardClipboardManager] for clipboard panel
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    // ─── Callbacks ───────────────────────────────────────────────

    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onPaste: ((String) -> Unit)? = null
    var onArrowKey: ((Int) -> Unit)? = null

    // ─── State ───────────────────────────────────────────────────

    private var isShift = false
    private val shiftKeys = mutableListOf<android.widget.TextView>()
    private val allLetterKeys = mutableListOf<android.widget.TextView>()

    // Panel containers
    private var keyboardContainer: LinearLayout? = null
    private var symbolContainer: LinearLayout? = null
    private var mainContentContainer: FrameLayout? = null

    // ─── Extracted Components ────────────────────────────────────

    private val keyFactory = KeyViewFactory(
        context = context,
        onKeyPress = { onKeyPress?.invoke(it) },
        onBackspace = { onBackspace?.invoke() },
        onEnter = { onEnter?.invoke() },
        onSpace = { onSpace?.invoke() },
        allLetterKeys = allLetterKeys,
        getIsShift = { isShift },
        onToggleShift = { toggleShift() }
    )

    private val rowBuilder = KeyRowBuilder(
        context = context,
        keyFactory = keyFactory,
        onToggleSymbol = { toggleSymbolPanel() },
        onSpace = { onSpace?.invoke() },
        onToggleShift = { toggleShift() },
        shiftKeys = shiftKeys
    )

    private val symbolPanel = SymbolPanel(
        context = context,
        keyFactory = keyFactory,
        rowBuilder = rowBuilder,
        onToggleKeyboard = { toggleSymbolPanel() }
    )

    private val headerBar = HeaderBar(
        context = context,
        onTranslate = { android.widget.Toast.makeText(context, "Translate — Coming Soon", android.widget.Toast.LENGTH_SHORT).show() },
        onClipboard = { toggleClipboardPanel() },
        onVoice = { voiceManager.showVoiceBarForEngine() },
        onSettings = { toggleSettingsPanel() }
    )

    private val suggestionBar = SuggestionBar(context) { word ->
        onKeyPress?.invoke(word)
    }

    // ─── Managers ────────────────────────────────────────────────

    private val voiceEngine = VoiceTranscriptionEngine(context)
    private val settingsManager = KeyboardSettingsManager(context, voiceEngine) {
        keyboardContainer?.visibility = View.VISIBLE
    }
    private val clipboardManager = KeyboardClipboardManager(context, { onPaste?.invoke(it) }) {
        keyboardContainer?.visibility = View.VISIBLE
    }
    private val voiceManager = KeyboardVoiceManager(
        context, voiceEngine, settingsManager,
        onKeyPress, onBackspace, onEnter, onSpace, onArrowKey
    )

    // ─── Init ────────────────────────────────────────────────────

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor(KeyboardTheme.BG_DARK))
        val p = KeyboardTheme.dp(context, 6)
        setPadding(p, p, p, p)
        settingsManager.loadSettings()
        voiceManager.setupVoiceEngineCallbacks()
        buildKeyboard()
    }

    // ─── Public API ──────────────────────────────────────────────

    fun updateImeOptions(imeOptions: Int) {
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        keyFactory.updateEnterKeyAppearance(action)
    }

    fun cleanup() {
        keyFactory.cleanup()
        voiceManager.cleanup()
    }

    fun onClipboardChanged(text: String) {
        clipboardManager.onClipboardChanged(text)
    }

    // ─── Build Keyboard ──────────────────────────────────────────

    private fun buildKeyboard() {
        // Header / Voice slot
        addView(createHeaderVoiceSlot())
        // Suggestion bar
        addView(suggestionBar.create())

        mainContentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        keyFactory.mainContentContainer = mainContentContainer

        // Keyboard container
        keyboardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardContainer?.addView(rowBuilder.createNumberRow())
        keyboardContainer?.addView(rowBuilder.createKeyRow1())
        keyboardContainer?.addView(rowBuilder.createKeyRow2())
        keyboardContainer?.addView(rowBuilder.createKeyRow3())
        keyboardContainer?.addView(rowBuilder.createKeyRow4())
        mainContentContainer?.addView(keyboardContainer)

        // Symbol panel
        symbolContainer = symbolPanel.create()
        mainContentContainer?.addView(symbolContainer)

        // Clipboard panel
        mainContentContainer?.addView(clipboardManager.createPanel())

        // Settings panel
        mainContentContainer?.addView(settingsManager.createPanel())

        addView(mainContentContainer)

        // Wire up voice manager references
        voiceManager.headerBar = headerBar.view
        voiceManager.keyboardContainer = keyboardContainer
        voiceManager.clipboardPanel = clipboardManager.panel
        voiceManager.settingsPanel = settingsManager.panel
    }

    // ─── Header / Voice Slot ─────────────────────────────────────

    private fun createHeaderVoiceSlot(): FrameLayout {
        val slot = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, KeyboardTheme.dp(context, 40)).apply {
                bottomMargin = KeyboardTheme.dp(context, 4)
            }
        }

        slot.addView(headerBar.create())
        slot.addView(voiceManager.createVoiceBar())

        return slot
    }

    // ─── Panel Toggling ──────────────────────────────────────────

    private fun toggleClipboardPanel() {
        if (clipboardManager.isVisible) {
            clipboardManager.hide()
            keyboardContainer?.visibility = View.VISIBLE
        } else {
            keyboardContainer?.visibility = View.GONE
            symbolContainer?.visibility = View.GONE
            settingsManager.hide()
            clipboardManager.show()
        }
    }

    private fun toggleSettingsPanel() {
        if (settingsManager.isVisible) {
            settingsManager.hide()
            keyboardContainer?.visibility = View.VISIBLE
        } else {
            keyboardContainer?.visibility = View.GONE
            symbolContainer?.visibility = View.GONE
            clipboardManager.hide()
            settingsManager.show()
        }
    }

    private fun toggleSymbolPanel() {
        if (symbolContainer?.visibility == View.GONE) {
            symbolContainer?.visibility = View.VISIBLE
            keyboardContainer?.visibility = View.GONE
            clipboardManager.hide()
            settingsManager.hide()
        } else {
            symbolContainer?.visibility = View.GONE
            keyboardContainer?.visibility = View.VISIBLE
        }
    }

    // ─── Shift Toggle ────────────────────────────────────────────

    private fun toggleShift() {
        isShift = !isShift
        allLetterKeys.forEach { tv ->
            val label = tv.text.toString()
            if (label.length == 1 && label[0].isLetter()) {
                tv.text = if (isShift) label.uppercase() else label.lowercase()
            }
        }
        shiftKeys.forEach { tv ->
            val actualView = tv.tag as? LinearLayout ?: return@forEach
            actualView.background = if (isShift) KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE) else KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID)
        }
    }
}
