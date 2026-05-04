package com.horizon.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Horizon Keyboard — Core keyboard layout and key handling.
 *
 * Delegates to:
 * - [KeyboardVoiceManager] for voice recognition
 * - [KeyboardSettingsManager] for settings panel
 * - [KeyboardClipboardManager] for clipboard panel
 * - [KeyboardTheme] for colors and drawables
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

    // Long press backspace
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val BACKSPACE_INITIAL_DELAY = 400L
    private val BACKSPACE_REPEAT_DELAY = 70L

    // Enter key
    private var enterKeyView: TextView? = null
    private var currentImeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED

    // Panel containers
    private var keyboardContainer: LinearLayout? = null
    private var symbolContainer: LinearLayout? = null
    private var mainContentContainer: FrameLayout? = null

    // Header
    private var headerBar: LinearLayout? = null
    private var headerVoiceSlot: FrameLayout? = null

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
        currentImeAction = imeOptions and EditorInfo.IME_MASK_ACTION
        updateEnterKeyAppearance()
    }

    fun cleanup() {
        backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
        backspaceRunnable = null
        voiceManager.cleanup()
    }

    fun onClipboardChanged(text: String) {
        clipboardManager.onClipboardChanged(text)
    }

    // ─── Build Keyboard ──────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun buildKeyboard() {
        // Header / Voice slot
        addView(createHeaderVoiceSlot())
        // Suggestion bar
        addView(createSuggestionBar())

        mainContentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Keyboard container
        keyboardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        keyboardContainer?.addView(createNumberRow())
        keyboardContainer?.addView(createKeyRow1())
        keyboardContainer?.addView(createKeyRow2())
        keyboardContainer?.addView(createKeyRow3())
        keyboardContainer?.addView(createKeyRow4())
        mainContentContainer?.addView(keyboardContainer)

        // Symbol panel
        symbolContainer = createSymbolPanel()
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

        headerVoiceSlot = slot
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

    // ─── Enter Key ───────────────────────────────────────────────

    private fun updateEnterKeyAppearance() {
        val key = enterKeyView ?: return
        when (currentImeAction) {
            EditorInfo.IME_ACTION_SEARCH -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            }
            EditorInfo.IME_ACTION_SEND -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_send, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_GREEN)
            }
            EditorInfo.IME_ACTION_GO -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            }
            EditorInfo.IME_ACTION_DONE -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            }
            EditorInfo.IME_ACTION_NEXT -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            }
            else -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            }
        }
    }

    // ─── Number Row ──────────────────────────────────────────────

    private fun createNumberRow(): LinearLayout {
        val row = createRow()
        "1234567890".forEach { addKey(row, it.toString(), textSize = 15f) }
        return row
    }

    // ─── Keyboard Rows ───────────────────────────────────────────

    private fun createKeyRow1(): LinearLayout {
        val row = createRow()
        "qwertyuiop".forEach { addKey(row, it.toString()) }
        return row
    }

    private fun createKeyRow2(): LinearLayout {
        val row = createRow()
        "asdfghjkl".forEach { addKey(row, it.toString()) }
        return row
    }

    private fun createKeyRow3(): LinearLayout {
        val row = createRow()
        val shiftTv = addSpecialKeyWithIcon(row, R.drawable.ic_shift, 1.5f) { toggleShift() }
        shiftKeys.add(shiftTv)
        "zxcvbnm".forEach { addKey(row, it.toString()) }
        addBackspaceKey(row)
        return row
    }

    private fun createKeyRow4(): LinearLayout {
        val row = createRow()
        addSpecialKey(row, "123", 1.5f, textSize = 11f) { toggleSymbolPanel() }
        addKeyWithIcon(row, ",", R.drawable.ic_comma)
        addSpecialKey(row, "SPACE", 5f, textSize = 11f) { onSpace?.invoke() }
        addKey(row, ".")
        enterKeyView = createEnterKey()
        row.addView(enterKeyView)
        updateEnterKeyAppearance()
        return row
    }

    // ─── Symbol Panel ────────────────────────────────────────────

    private fun createSymbolPanel(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
        }

        val row1 = createRow()
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { addSymbolKey(row1, it) }
        panel.addView(row1)

        val row2 = createRow()
        listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "/").forEach { addSymbolKey(row2, it) }
        panel.addView(row2)

        val row3 = createRow()
        addSpecialKey(row3, "ABC", 1.5f, textSize = 11f) { toggleSymbolPanel() }
        listOf("(", ")", "\"", "'", ":", ";", "!", "?", ",").forEach { addSymbolKey(row3, it) }
        panel.addView(row3)

        val row4 = createRow()
        addBackspaceKey(row4)
        listOf("_", "~", "`", "|", "\\", "{", "}", "[", "]").forEach { addSymbolKey(row4, it) }
        panel.addView(row4)

        return panel
    }

    // ─── Key Builders ────────────────────────────────────────────

    private fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, KeyboardTheme.dp(context, 44)).apply {
                bottomMargin = KeyboardTheme.dp(context, 5)
            }
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addKey(row: LinearLayout, label: String, textSize: Float = 18f) {
        val tv = createKeyView(label, textSize)
        tv.layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), 1f).apply {
            marginStart = KeyboardTheme.dp(context, 3)
            marginEnd = KeyboardTheme.dp(context, 3)
        }

        if (label[0].isLetter()) allLetterKeys.add(tv)

        val popup = createKeyPopup(label)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply { setTextColor(Color.WHITE); background = KeyboardTheme.keyBgPressed() }
                    showKeyPopup(tv, popup)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply { setTextColor(Color.WHITE); background = KeyboardTheme.keyBgNormal() }
                    hideKeyPopup(popup)
                    if (event.action == MotionEvent.ACTION_UP) {
                        val output = if (isShift) label.uppercase() else label.lowercase()
                        onKeyPress?.invoke(output)
                        if (isShift && label[0].isLetter()) toggleShift()
                    }
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addKeyWithIcon(row: LinearLayout, label: String, iconRes: Int) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), 1f).apply {
                marginStart = KeyboardTheme.dp(context, 3)
                marginEnd = KeyboardTheme.dp(context, 3)
            }
            background = KeyboardTheme.keyBgNormal()
        }

        container.addView(ImageView(context).apply {
            layoutParams = LayoutParams(KeyboardTheme.dp(context, 16), KeyboardTheme.dp(context, 16))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = KeyboardTheme.keyBgPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = KeyboardTheme.keyBgNormal()
                    if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke(label)
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBackspaceKey(row: LinearLayout) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), 1.5f).apply {
                marginStart = KeyboardTheme.dp(context, 3)
                marginEnd = KeyboardTheme.dp(context, 3)
            }
            background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID)
        }

        container.addView(ImageView(context).apply {
            layoutParams = LayoutParams(KeyboardTheme.dp(context, 20), KeyboardTheme.dp(context, 20))
            setImageResource(R.drawable.ic_backspace)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID_PRESSED)
                    onBackspace?.invoke()
                    backspaceRunnable = object : Runnable {
                        override fun run() {
                            onBackspace?.invoke()
                            backspaceHandler.postDelayed(this, BACKSPACE_REPEAT_DELAY)
                        }
                    }
                    backspaceHandler.postDelayed(backspaceRunnable!!, BACKSPACE_INITIAL_DELAY)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID)
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpecialKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        bg: String? = null,
        textSize: Float = 14f,
        bold: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val tv = createKeyView(label, textSize, bold)
        tv.layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), weight).apply {
            marginStart = KeyboardTheme.dp(context, 3)
            marginEnd = KeyboardTheme.dp(context, 3)
        }

        val bgColor = bg ?: KeyboardTheme.BG_KEY_SOLID
        tv.background = KeyboardTheme.keyBgSolid(bgColor)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).background = KeyboardTheme.keyBgSolid(bgColor)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
        return tv
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpecialKeyWithIcon(
        row: LinearLayout,
        iconRes: Int,
        weight: Float,
        onClick: () -> Unit
    ): TextView {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), weight).apply {
                marginStart = KeyboardTheme.dp(context, 3)
                marginEnd = KeyboardTheme.dp(context, 3)
            }
            background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID)
        }

        container.addView(ImageView(context).apply {
            layoutParams = LayoutParams(KeyboardTheme.dp(context, 20), KeyboardTheme.dp(context, 20))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = if (isShift) KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE) else KeyboardTheme.keyBgSolid(KeyboardTheme.BG_KEY_SOLID)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(container)

        // Return dummy TextView for shift state tracking
        return TextView(context).apply { tag = container }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSymbolKey(row: LinearLayout, label: String) {
        val tv = createKeyView(label)
        tv.layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), 1f).apply {
            marginStart = KeyboardTheme.dp(context, 2)
            marginEnd = KeyboardTheme.dp(context, 2)
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply { setTextColor(Color.WHITE); background = KeyboardTheme.keyBgPressed() }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply { setTextColor(Color.WHITE); background = KeyboardTheme.keyBgNormal() }
                    if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke(label)
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
    }

    private fun createEnterKey(): TextView {
        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE)
            compoundDrawablePadding = 0
            layoutParams = LinearLayout.LayoutParams(0, KeyboardTheme.dp(context, 44), 2f).apply {
                marginStart = KeyboardTheme.dp(context, 3)
                marginEnd = KeyboardTheme.dp(context, 3)
            }
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = KeyboardTheme.keyBgSolid(KeyboardTheme.ACCENT_BLUE_PRESSED)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    updateEnterKeyAppearance()
                    if (event.action == MotionEvent.ACTION_UP) onEnter?.invoke()
                    true
                }
                else -> false
            }
        }

        return tv
    }

    private fun createKeyView(label: String, textSize: Float = 18f, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            gravity = Gravity.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            background = KeyboardTheme.keyBgNormal()
        }
    }

    private fun createKeyPopup(label: String): TextView {
        return TextView(context).apply {
            text = label.uppercase()
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor(KeyboardTheme.BG_PILL))
                cornerRadius = KeyboardTheme.dp(context, 8).toFloat()
                setStroke(KeyboardTheme.dp(context, 1), Color.parseColor("#5A5A5C"))
            }
            elevation = 12f
            visibility = View.GONE
        }
    }

    private fun showKeyPopup(anchor: View, popup: TextView) {
        try {
            val parent = anchor.parent as? ViewGroup ?: return
            if (popup.parent != null) (popup.parent as? ViewGroup)?.removeView(popup)

            popup.layoutParams = FrameLayout.LayoutParams(KeyboardTheme.dp(context, 48), KeyboardTheme.dp(context, 52))
            popup.visibility = View.VISIBLE

            val parentFrame = parent as? FrameLayout
            if (parentFrame != null) {
                parentFrame.addView(popup)
                popup.x = anchor.left.toFloat()
                popup.y = anchor.top.toFloat() - KeyboardTheme.dp(context, 54)
            } else {
                mainContentContainer?.addView(popup)
                val loc = IntArray(2)
                anchor.getLocationOnScreen(loc)
                val parentLoc = IntArray(2)
                mainContentContainer?.getLocationOnScreen(parentLoc)
                popup.x = (loc[0] - parentLoc[0]).toFloat()
                popup.y = (loc[1] - parentLoc[1]).toFloat() - KeyboardTheme.dp(context, 54)
            }
        } catch (_: Exception) {}
    }

    private fun hideKeyPopup(popup: TextView) {
        try {
            popup.visibility = View.GONE
            (popup.parent as? ViewGroup)?.removeView(popup)
        } catch (_: Exception) {}
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
