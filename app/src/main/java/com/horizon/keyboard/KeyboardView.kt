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
import com.horizon.keyboard.data.ClipboardRepository
import com.horizon.keyboard.core.dictionary.SuggestionManager
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

    // Word prediction state
    private val suggestionManager = SuggestionManager()
    private var currentInput = StringBuilder()  // what user is currently typing
    private var previousWord: String? = null     // last completed word

    // Panel containers
    private var keyboardContainer: LinearLayout? = null
    private var symbolContainer: LinearLayout? = null
    private var mainContentContainer: FrameLayout? = null

    // ─── Extracted Components ────────────────────────────────────

    private val keyFactory = KeyViewFactory(
        context = context,
        onKeyPress = { char ->
            onKeyPress?.invoke(char)
            handleCharInput(char)
        },
        onBackspace = {
            onBackspace?.invoke()
            handleBackspace()
        },
        onEnter = {
            onEnter?.invoke()
            handleWordComplete()
        },
        onSpace = {
            onSpace?.invoke()
            handleWordComplete()
        },
        allLetterKeys = allLetterKeys,
        getIsShift = { isShift },
        onToggleShift = { toggleShift() }
    )

    private val rowBuilder = KeyRowBuilder(
        context = context,
        keyFactory = keyFactory,
        onToggleSymbol = { toggleSymbolPanel() },
        onSpace = {
            onSpace?.invoke()
            handleWordComplete()
        },
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
        onSaved = { toggleSavedPanel() },
        onVoice = { voiceManager.showVoiceBarForEngine() },
        onSettings = { toggleSettingsPanel() }
    )

    private val suggestionBar = SuggestionBar(context) { word ->
        // When user taps a suggestion, insert it and learn
        val cleanWord = word.trim()
        if (cleanWord.isNotEmpty()) {
            suggestionManager.onWordCompleted(cleanWord, previousWord)
            previousWord = cleanWord
            currentInput.clear()
        }
        onKeyPress?.invoke(word)
        // Update suggestions for next word
        updateSuggestions()
    }

    // ─── Panel Host ──────────────────────────────────────────────

    private val panelHost = PanelHost(context)

    // ─── Managers ────────────────────────────────────────────────

    private val voiceEngine = VoiceTranscriptionEngine(context)
    private val settingsPanel = SettingsPanel(context, voiceEngine) {
        panelHost.showKeyboard()
    }
    private val clipboardPanel = ClipboardPanel(
        context = context,
        repository = ClipboardRepository(context),
        onPaste = { onPaste?.invoke(it) }
    ) {
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
        // Initialize word prediction
        suggestionManager.initialize(context)
        buildKeyboard()
        // Load current system clipboard into history
        loadInitialClipboard()
    }

    // ─── Public API ──────────────────────────────────────────────

    fun updateImeOptions(imeOptions: Int) {
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        keyFactory.updateEnterKeyAppearance(action)
    }

    fun stopVoice() {
        voiceManager.cleanup()
    }

    /**
     * Stop all voice recording and release mic resources.
     * Called when keyboard is hidden/dismissed — ensures mic is fully off.
     */
    fun stopAllVoice() {
        voiceManager.cleanup()
    }

    fun cleanup() {
        keyFactory.cleanup()
        voiceManager.cleanup()
        clipboardPanel.cleanup()
        // Save learned word data
        suggestionManager.saveAll()
    }

    /**
     * Called by service when clipboard changes in any app.
     * Feeds the clip to the clipboard panel for history tracking.
     */
    fun onClipboardChanged(text: String) {
        clipboardPanel.addClip(text)
    }

    /**
     * Load current system clipboard into history on first keyboard show.
     */
    fun loadInitialClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = cm?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty()) {
                clipboardPanel.addClip(text)
            }
        }
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
    private fun toggleSavedPanel() {
        if (panelHost.isClipboard && clipboardPanel.isShowingSaved) {
            panelHost.showKeyboard()
        } else {
            clipboardPanel.showSaved()
            panelHost.showClipboard()
        }
    }
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

    // ─── Word Prediction Tracking ────────────────────────────────

    /**
     * Called when a character key is pressed.
     * Tracks the current input word and updates suggestions.
     */
    private fun handleCharInput(char: String) {
        if (char.length == 1 && char[0].isLetter()) {
            currentInput.append(char)
        } else if (char == " ") {
            handleWordComplete()
        } else {
            // Punctuation — treat as word boundary
            if (currentInput.isNotEmpty()) {
                handleWordComplete()
            }
        }
        updateSuggestions()
    }

    /**
     * Called when backspace is pressed.
     * Removes last char from tracked input and updates suggestions.
     */
    private fun handleBackspace() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.lastIndex)
        }
        updateSuggestions()
    }

    /**
     * Called when space or enter is pressed.
     * Completes the current word — feeds it to the learning engine.
     */
    private fun handleWordComplete() {
        if (currentInput.isNotEmpty()) {
            val word = currentInput.toString()
            suggestionManager.onWordCompleted(word, previousWord)
            previousWord = word
            currentInput.clear()
        }
    }

    /**
     * Update the suggestion bar with predictions from SuggestionManager.
     * Called on every keystroke.
     */
    private fun updateSuggestions() {
        val suggestions = suggestionManager.getSuggestions(
            currentInput = currentInput.toString(),
            previousWord = previousWord,
            limit = SuggestionManager.MAX_SUGGESTIONS
        )
        if (suggestions.isNotEmpty()) {
            suggestionBar.updateSuggestions(suggestions)
        } else {
            // Show fallback when no predictions
            suggestionBar.updateSuggestions(listOf("I", "Hello", "The", "Thanks", "How"))
        }
    }
}
