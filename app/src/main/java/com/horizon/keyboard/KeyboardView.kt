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
import com.horizon.keyboard.ui.panel.ClipboardPanel
import com.horizon.keyboard.ui.panel.PanelHost
import com.horizon.keyboard.ui.panel.SettingsPanel
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.ui.theme.Drawables
import com.horizon.keyboard.ui.theme.Dimensions


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
 * - [SettingsPanel] for settings panel
 * - [ClipboardPanel] for clipboard panel
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

    // ─── Panel Host ──────────────────────────────────────────────

    private val panelHost = PanelHost(context)

    // ─── Managers ────────────────────────────────────────────────

    private val voiceEngine = VoiceTranscriptionEngine(context)
    private val settingsPanel = SettingsPanel(context, voiceEngine) {
        panelHost.showKeyboard()
    }
    private val clipboardPanel = ClipboardPanel(context, onPaste = { onPaste?.invoke(it) }) {
        panelHost.showKeyboard()
    }
    private val voiceManager = KeyboardVoiceManager(
        context, voiceEngine, settingsPanel,
        onKeyPress = { onKeyPress?.invoke(it) },
        onBackspace = { onBackspace?.invoke() },
        onEnter = { onEnter?.invoke() },
        onSpace = { onSpace?.invoke() },
        onArrowKey = { onArrowKey?.invoke(it) }
    )

    // ─── Init ────────────────────────────────────────────────────

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor(Colors.BG_DARK))
        val p = Dimensions.dp(context, 6)
        setPadding(p, p, p, p)
        settingsPanel.loadSettings()
        voiceManager.setupVoiceEngineCallbacks()
        buildKeyboard()
    }

    // ─── Public API ──────────────────────────────────────────────

    fun updateImeOptions(imeOptions: Int) {
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        keyFactory.updateEnterKeyAppearance(action)
    }

    fun stopVoice() {
        voiceManager.hideVoiceBar()
    }

    fun cleanup() {
        keyFactory.cleanup()
        voiceManager.cleanup()
    }

    fun onClipboardChanged(text: String) {
        clipboardPanel.onClipboardChanged(text)
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
        mainContentContainer?.addView(clipboardPanel.createPanel())

        // Settings panel
        mainContentContainer?.addView(settingsPanel.createPanel())

        addView(mainContentContainer)

        // Wire up panel host
        panelHost.keyboardPanel = keyboardContainer
        panelHost.symbolPanel = symbolContainer
        panelHost.clipboardPanel = clipboardPanel.panel
        panelHost.settingsPanel = settingsPanel.panel

        // Wire up voice manager references
        voiceManager.headerBar = headerBar.view
        voiceManager.keyboardContainer = keyboardContainer
        voiceManager.clipboardPanel = clipboardPanel.panel
        voiceManager.settingsPanelView = settingsPanel.panel
    }

    // ─── Header / Voice Slot ─────────────────────────────────────

    private fun createHeaderVoiceSlot(): FrameLayout {
        val slot = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, Dimensions.dp(context, 40)).apply {
                bottomMargin = Dimensions.dp(context, 4)
            }
        }

        slot.addView(headerBar.create())
        slot.addView(voiceManager.createVoiceBar())

        return slot
    }

    // ─── Panel Toggling ──────────────────────────────────────────

    private fun toggleClipboardPanel() = panelHost.toggleClipboard()
    private fun toggleSettingsPanel() {
        if (panelHost.isSettings) {
            panelHost.showKeyboard()
        } else {
            settingsPanel.show()  // builds panel content
            panelHost.showSettings()
        }
    }
    private fun toggleSymbolPanel() = panelHost.toggleSymbol()

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
            actualView.background = if (isShift) Drawables.keyBgSolid(Colors.ACCENT_BLUE) else Drawables.keyBgSolid(Colors.BG_KEY_SOLID)
        }
    }
}
