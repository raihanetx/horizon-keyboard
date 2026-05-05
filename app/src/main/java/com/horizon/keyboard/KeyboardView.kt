package com.horizon.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.horizon.keyboard.ui.keyboard.KeyPopupManager
import com.horizon.keyboard.ui.keyboard.KeyRowBuilder
import com.horizon.keyboard.ui.keyboard.KeyViewFactory
import com.horizon.keyboard.ui.keyboard.SymbolPanel

/**
 * Horizon Keyboard — Core keyboard layout coordinator.
 *
 * Assembles the keyboard from extracted components:
 * - [KeyViewFactory] for key creation
 * - [KeyRowBuilder] for row construction
 * - [KeyPopupManager] for key popups
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
    private val shiftKeys = mutableListOf<TextView>()
    private val allLetterKeys = mutableListOf<TextView>()

    // Panel containers
    private var keyboardContainer: LinearLayout? = null
    private var symbolContainer: LinearLayout? = null
    private var mainContentContainer: FrameLayout? = null

    // Header
    private var headerBar: LinearLayout? = null

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
        addView(createSuggestionBar())

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
        voiceManager.headerBar = headerBar
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

        headerBar = createHeaderBar()
        val voiceBar = voiceManager.createVoiceBar()

        slot.addView(headerBar)
        slot.addView(voiceBar)

        return slot
    }

    private fun createHeaderBar(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(KeyboardTheme.BG_KEY))
            val pad = KeyboardTheme.dp(context, 8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(createIconImageView(R.drawable.ic_keyboard, KeyboardTheme.dp(context, 20), tint = KeyboardTheme.ACCENT_BLUE))

        // Spacer
        header.addView(View(context).apply {
            layoutParams = LayoutParams(0, 1, 1f)
        })

        val icons = listOf(
            R.drawable.ic_translate to { android.widget.Toast.makeText(context, "Translate — Coming Soon", android.widget.Toast.LENGTH_SHORT).show() },
            R.drawable.ic_clipboard to { toggleClipboardPanel() },
            R.drawable.ic_voice to { voiceManager.showVoiceBarForEngine() },
            R.drawable.ic_settings to { toggleSettingsPanel() }
        )

        icons.forEach { (drawableRes, action) ->
            header.addView(createHeaderIconButton(drawableRes, action))
        }

        return header
    }

    private fun createIconImageView(drawableRes: Int, size: Int, tint: String? = null): ImageView {
        return ImageView(context).apply {
            layoutParams = LayoutParams(size, size)
            setImageResource(drawableRes)
            if (tint != null) setColorFilter(Color.parseColor(tint))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderIconButton(drawableRes: Int, onClick: () -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(KeyboardTheme.dp(context, 36), KeyboardTheme.dp(context, 32)).apply {
                marginStart = KeyboardTheme.dp(context, 3)
                marginEnd = KeyboardTheme.dp(context, 3)
            }
            val p = KeyboardTheme.dp(context, 6)
            setPadding(p, p, p, p)
            background = KeyboardTheme.pillBg(KeyboardTheme.BG_PILL)
        }

        val iconView = ImageView(context).apply {
            layoutParams = LayoutParams(KeyboardTheme.dp(context, 18), KeyboardTheme.dp(context, 18))
            setImageResource(drawableRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = KeyboardTheme.pillBg(KeyboardTheme.BG_PILL_PRESSED)
                    iconView.setColorFilter(Color.WHITE)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = KeyboardTheme.pillBg(KeyboardTheme.BG_PILL)
                    iconView.clearColorFilter()
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        return container
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

    // ─── Suggestion Bar ──────────────────────────────────────────

    private fun createSuggestionBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, KeyboardTheme.dp(context, 40)).apply {
                bottomMargin = KeyboardTheme.dp(context, 4)
            }
            setBackgroundColor(Color.parseColor("#1E1E20"))
            val p = KeyboardTheme.dp(context, 6)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val words = listOf("I", "Hello", "The", "Thanks", "How")

        words.forEachIndexed { index, word ->
            val tv = TextView(context).apply {
                text = word
                setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }

            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v as TextView).apply {
                            setTextColor(Color.WHITE)
                            background = GradientDrawable().apply {
                                setColor(Color.parseColor(KeyboardTheme.BG_PILL))
                                cornerRadius = KeyboardTheme.dp(context, 6).toFloat()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).apply {
                            setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
                            background = null
                        }
                        if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke("$word ")
                        true
                    }
                    else -> false
                }
            }

            bar.addView(tv)

            if (index < words.size - 1) {
                bar.addView(View(context).apply {
                    layoutParams = LayoutParams(KeyboardTheme.dp(context, 1), KeyboardTheme.dp(context, 22))
                    setBackgroundColor(Color.parseColor(KeyboardTheme.DIVIDER))
                })
            }
        }

        return bar
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
