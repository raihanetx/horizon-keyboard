package com.horizon.keyboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Horizon Keyboard — Traditional View-based, reliable in InputMethodService.
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onPaste: ((String) -> Unit)? = null

    private var isShift = false
    private val shiftKeys = mutableListOf<TextView>()
    private val allLetterKeys = mutableListOf<TextView>()

    // Voice typing
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentVoiceLang = "en-US" // en-US or bn-BD
    private var voiceBarContainer: LinearLayout? = null
    private var voiceStatusText: TextView? = null
    private var voiceLangButton: TextView? = null
    private var voiceStartStopButton: TextView? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        val p = dp(6)
        setPadding(p, p, p, p)
        buildKeyboard()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildKeyboard() {
        // === Voice Typing Bar (hidden by default) ===
        addView(createVoiceBar())

        // === Header Bar ===
        addView(createHeaderBar())

        // === Word Suggestion Bar (4 words) ===
        addView(createSuggestionBar())

        // === Keyboard Rows ===
        addView(createKeyRow1())
        addView(createKeyRow2())
        addView(createKeyRow3())
        addView(createKeyRow4())
    }

    // ─── Voice Bar ───────────────────────────────────────────────

    private fun createVoiceBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44)).apply {
                bottomMargin = dp(4)
            }
            setBackgroundColor(Color.parseColor("#121212"))
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
            visibility = GONE
        }

        // Start/Stop button
        voiceStartStopButton = TextView(context).apply {
            text = "Start"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(60), dp(28)).apply {
                marginEnd = dp(8)
            }
            background = pillBg("#3A3A3C")
            setOnClickListener { toggleVoiceRecognition() }
        }
        bar.addView(voiceStartStopButton)

        // Status text
        voiceStatusText = TextView(context).apply {
            text = "Tap Start to voice type..."
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
        }
        bar.addView(voiceStatusText)

        // Language toggle
        voiceLangButton = TextView(context).apply {
            text = "EN"
            setTextColor(Color.parseColor("#66FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
            layoutParams = LayoutParams(dp(40), dp(28))
            background = pillBg("#2A2A2C")
            setOnClickListener { toggleVoiceLanguage() }
        }
        bar.addView(voiceLangButton)

        // Close button
        val closeBtn = TextView(context).apply {
            text = "▲"
            setTextColor(Color.parseColor("#636366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(30), dp(28)).apply {
                marginStart = dp(4)
            }
            setOnClickListener { hideVoiceBar() }
        }
        bar.addView(closeBtn)

        voiceBarContainer = bar
        return bar
    }

    private fun toggleVoiceBar() {
        voiceBarContainer?.let { bar ->
            if (bar.visibility == GONE) {
                bar.visibility = VISIBLE
                initSpeechRecognizer()
            } else {
                hideVoiceBar()
            }
        }
    }

    private fun hideVoiceBar() {
        stopVoiceRecognition()
        voiceBarContainer?.visibility = GONE
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer != null) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceStatusText?.text = "Speech recognition not available"
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voiceStatusText?.text = "Microphone permission needed. Grant in app settings."
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    voiceStartStopButton?.text = "Stop"
                    voiceStatusText?.text = "Listening..."
                }
                override fun onBeginningOfSpeech() {
                    voiceStatusText?.text = "Listening..."
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    voiceStartStopButton?.text = "Start"
                    voiceStatusText?.text = "Processing..."
                }
                override fun onError(error: Int) {
                    isListening = false
                    voiceStartStopButton?.text = "Start"
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                        else -> "Error occurred. Tap Start to retry."
                    }
                    voiceStatusText?.text = msg
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    voiceStartStopButton?.text = "Start"
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) {
                        voiceStatusText?.text = "\"$text\""
                        onKeyPress?.invoke(text)
                    } else {
                        voiceStatusText?.text = "No speech detected. Try again."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: ""
                    if (partial.isNotEmpty()) {
                        voiceStatusText?.text = partial
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun toggleVoiceRecognition() {
        if (isListening) {
            stopVoiceRecognition()
        } else {
            startVoiceRecognition()
        }
    }

    private fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: run {
            initSpeechRecognizer()
            speechRecognizer ?: return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentVoiceLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentVoiceLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            voiceStatusText?.text = "Failed to start. Tap Start to retry."
        }
    }

    private fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        voiceStartStopButton?.text = "Start"
    }

    private fun toggleVoiceLanguage() {
        currentVoiceLang = if (currentVoiceLang == "en-US") "bn-BD" else "en-US"
        voiceLangButton?.text = if (currentVoiceLang == "en-US") "EN" else "BN"
        voiceStatusText?.text = if (currentVoiceLang == "en-US") "Language: English" else "Language: বাংলা"

        // Restart if currently listening
        if (isListening) {
            stopVoiceRecognition()
            postDelayed({ startVoiceRecognition() }, 200)
        }
    }

    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    // ─── Clipboard Paste ─────────────────────────────────────────

    private fun handleClipboardPaste() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                onPaste?.invoke(text)
                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Header Bar with proper Canvas icons ─────────────────────

    private fun createHeaderBar(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44)).apply {
                bottomMargin = dp(6)
            }
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left: keyboard icon
        header.addView(IconView(context, IconView.IconType.KEYBOARD).apply {
            layoutParams = LayoutParams(dp(28), dp(28))
        })

        // Spacer
        header.addView(View(context).apply {
            layoutParams = LayoutParams(0, 1, 1f)
        })

        // Right: action buttons (icons only)
        val icons = listOf(
            IconView.IconType.TRANSLATE,
            IconView.IconType.CLIPBOARD,
            IconView.IconType.VOICE,
            IconView.IconType.SETTINGS
        )

        icons.forEach { iconType ->
            header.addView(createHeaderButton(iconType))
        }

        return header
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderButton(iconType: IconView.IconType): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                dp(38), dp(34)
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            val p = dp(8)
            setPadding(p, p, p, p)
            background = pillBg("#3A3A3C")
        }

        val iconView = IconView(context, iconType).apply {
            layoutParams = LayoutParams(dp(18), dp(18))
        }

        container.addView(iconView)

        val iconName = when (iconType) {
            IconView.IconType.TRANSLATE -> "Translate"
            IconView.IconType.CLIPBOARD -> "Clipboard"
            IconView.IconType.VOICE -> "Voice"
            IconView.IconType.SETTINGS -> "Settings"
            IconView.IconType.KEYBOARD -> "Keyboard"
        }

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = pillBg("#48484A")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = pillBg("#3A3A3C")
                    if (event.action == MotionEvent.ACTION_UP) {
                        when (iconType) {
                            IconView.IconType.VOICE -> toggleVoiceBar()
                            IconView.IconType.CLIPBOARD -> handleClipboardPaste()
                            IconView.IconType.TRANSLATE -> Toast.makeText(context, "Translate — Type in the text field, then switch to Translate tab", Toast.LENGTH_SHORT).show()
                            IconView.IconType.SETTINGS -> Toast.makeText(context, "Settings — Coming Soon!", Toast.LENGTH_SHORT).show()
                            else -> {}
                        }
                    }
                    true
                }
                else -> false
            }
        }

        return container
    }

    // ─── Suggestion Bar — 4 words, evenly spaced, divider lines ──

    private fun createSuggestionBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).apply {
                bottomMargin = dp(6)
            }
            setBackgroundColor(Color.parseColor("#252528"))
            val p = dp(4)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val words = listOf("Hello", "The", "Thanks", "How")

        words.forEachIndexed { index, word ->
            // Word cell
            val tv = TextView(context).apply {
                text = word
                setTextColor(Color.parseColor("#B0B0B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }

            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v as TextView).setTextColor(Color.WHITE)
                        v.setBackgroundColor(Color.parseColor("#3A3A3C"))
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).setTextColor(Color.parseColor("#B0B0B8"))
                        v.setBackgroundColor(Color.TRANSPARENT)
                        if (event.action == MotionEvent.ACTION_UP) {
                            onKeyPress?.invoke("$word ")
                        }
                        true
                    }
                    else -> false
                }
            }

            bar.addView(tv)

            // Divider line between words
            if (index < words.size - 1) {
                bar.addView(View(context).apply {
                    layoutParams = LayoutParams(dp(1), dp(20))
                    setBackgroundColor(Color.parseColor("#3A3A3C"))
                })
            }
        }

        return bar
    }

    // ─── Keyboard Row Builders ───────────────────────────────────

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
        addSpecialKey(row, "⇧", 1.5f) { toggleShift() }.also { shiftKeys.add(it) }
        "zxcvbnm".forEach { addKey(row, it.toString()) }
        addSpecialKey(row, "⌫", 1.5f) { onBackspace?.invoke() }
        return row
    }

    private fun createKeyRow4(): LinearLayout {
        val row = createRow()
        addSpecialKey(row, "123", 1.5f, textSize = 11f) {}
        addKey(row, "@")
        addSpecialKey(row, "SPACE", 5f, textSize = 11f) { onSpace?.invoke() }
        addKey(row, ".")
        addSpecialKey(row, "GO", 2f, bg = "#0A84FF", textSize = 12f, bold = true) { onEnter?.invoke() }
        return row
    }

    private fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply {
                bottomMargin = dp(6)
            }
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    // ─── Key Builders ────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun addKey(row: LinearLayout, label: String) {
        val tv = createKeyView(label)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        if (label[0].isLetter()) {
            allLetterKeys.add(tv)
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply {
                        setTextColor(Color.WHITE)
                        background = keyBgPressed()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply {
                        setTextColor(Color.WHITE)
                        background = keyBgNormal()
                    }
                    if (event.action == MotionEvent.ACTION_UP) {
                        val output = if (isShift) label.uppercase() else label.lowercase()
                        onKeyPress?.invoke(output)
                        if (isShift) toggleShift()
                    }
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
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
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(48), weight).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        val bgColor = bg ?: "#48484A"
        tv.background = keyBgSolid(bgColor)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).background = keyBgSolid("#636366")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).background = keyBgSolid(bgColor)
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
        return tv
    }

    private fun createKeyView(label: String, textSize: Float = 18f, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            gravity = Gravity.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            background = keyBgNormal()
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
        shiftKeys.forEach {
            it.background = if (isShift) keyBgSolid("#0A84FF") else keyBgSolid("#48484A")
        }
    }

    // ─── Background Drawables ────────────────────────────────────

    private fun keyBgNormal(): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(
                Color.parseColor("#3A3A3C"),
                Color.parseColor("#2E2E30")
            )
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun keyBgPressed(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#48484A"))
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun keyBgSolid(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(8).toFloat()
        }
    }

    private fun pillBg(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(20).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}

// ─── Custom Canvas Icon View ─────────────────────────────────────

class IconView(context: Context, val iconType: IconType) : View(context) {

    enum class IconType { KEYBOARD, TRANSLATE, CLIPBOARD, VOICE, SETTINGS }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = width.toFloat()
        val h = height.toFloat()
        val cx = s / 2f
        val cy = h / 2f

        paint.color = Color.parseColor("#A0A0A8")
        fillPaint.color = Color.parseColor("#A0A0A8")

        when (iconType) {
            IconType.KEYBOARD -> drawKeyboard(canvas, s, h)
            IconType.TRANSLATE -> drawTranslate(canvas, s, h)
            IconType.CLIPBOARD -> drawClipboard(canvas, s, h)
            IconType.VOICE -> drawVoice(canvas, s, h)
            IconType.SETTINGS -> drawSettings(canvas, s, h)
        }
    }

    private fun drawKeyboard(c: Canvas, s: Float, h: Float) {
        paint.color = Color.parseColor("#0A84FF")
        fillPaint.color = Color.parseColor("#0A84FF")
        val sw = s * 0.08f
        paint.strokeWidth = sw

        // Keyboard body outline
        val left = s * 0.1f
        val top = h * 0.2f
        val right = s * 0.9f
        val bottom = h * 0.8f
        val r = s * 0.1f
        c.drawRoundRect(left, top, right, bottom, r, r, paint)

        // Key dots (3x3 grid)
        fillPaint.color = Color.parseColor("#0A84FF")
        val gx = (right - left) / 4f
        val gy = (bottom - top) / 3f
        for (row in 0..2) {
            for (col in 0..2) {
                val kx = left + gx * (col + 1)
                val ky = top + gy * (row + 0.7f)
                c.drawCircle(kx, ky, sw * 0.8f, fillPaint)
            }
        }
        // Space bar
        val spaceY = bottom - gy * 0.4f
        c.drawLine(left + gx, spaceY, right - gx, spaceY, paint)
    }

    private fun drawTranslate(c: Canvas, s: Float, h: Float) {
        val sw = s * 0.07f
        paint.strokeWidth = sw

        // Two overlapping rectangles (languages)
        val off = s * 0.12f
        val left = s * 0.15f
        val top = h * 0.18f
        val right = s * 0.65f
        val bottom = h * 0.72f
        val r = s * 0.08f

        paint.style = Paint.Style.STROKE
        c.drawRoundRect(left + off, top + off, right + off, bottom + off, r, r, paint)
        c.drawRoundRect(left, top, right, bottom, r, r, paint)

        // "A" letter in center
        paint.style = Paint.Style.FILL
        paint.textSize = h * 0.35f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        c.drawText("A", s * 0.45f, h * 0.62f, paint)

        // Arrow at bottom-right
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sw
        val ax = s * 0.7f
        val ay = h * 0.75f
        c.drawLine(ax, ay - h * 0.12f, ax + s * 0.12f, ay, paint)
        c.drawLine(ax + s * 0.12f, ay, ax + s * 0.04f, ay - h * 0.04f, paint)
        c.drawLine(ax + s * 0.12f, ay, ax + s * 0.04f, ay + h * 0.04f, paint)
    }

    private fun drawClipboard(c: Canvas, s: Float, h: Float) {
        val sw = s * 0.07f
        paint.strokeWidth = sw

        val left = s * 0.2f
        val top = h * 0.12f
        val right = s * 0.8f
        val bottom = h * 0.88f
        val r = s * 0.06f

        // Clipboard body
        c.drawRoundRect(left, top, right, bottom, r, r, paint)

        // Clip at top
        val clipL = s * 0.35f
        val clipR = s * 0.65f
        val clipT = top - h * 0.04f
        val clipB = top + h * 0.12f
        c.drawRoundRect(clipL, clipT, clipR, clipB, s * 0.06f, s * 0.06f, paint)

        // Lines inside
        val lineY1 = top + h * 0.3f
        val lineY2 = top + h * 0.45f
        val lineY3 = top + h * 0.6f
        val lineL = left + s * 0.15f
        val lineR = right - s * 0.15f
        c.drawLine(lineL, lineY1, lineR, lineY1, paint)
        c.drawLine(lineL, lineY2, lineR * 0.85f, lineY2, paint)
        c.drawLine(lineL, lineY3, lineR * 0.7f, lineY3, paint)
    }

    private fun drawVoice(c: Canvas, s: Float, h: Float) {
        val sw = s * 0.07f
        paint.strokeWidth = sw
        val cx = s / 2f
        val cy = h * 0.4f

        // Mic body (rounded rect)
        val micW = s * 0.2f
        val micH = h * 0.3f
        c.drawRoundRect(
            cx - micW, cy - micH * 0.5f,
            cx + micW, cy + micH * 0.5f,
            micW, micW, paint
        )

        // Arc below mic
        val arcR = s * 0.25f
        val arcRect = RectF(cx - arcR, cy + micH * 0.2f, cx + arcR, cy + micH * 0.2f + arcR * 2f)
        c.drawArc(arcRect, 0f, 180f, false, paint)

        // Stand
        c.drawLine(cx, cy + micH * 0.5f + arcR, cx, h * 0.85f, paint)

        // Base
        val baseW = s * 0.15f
        c.drawLine(cx - baseW, h * 0.85f, cx + baseW, h * 0.85f, paint)
    }

    private fun drawSettings(c: Canvas, s: Float, h: Float) {
        val sw = s * 0.07f
        paint.strokeWidth = sw
        val cx = s / 2f
        val cy = h / 2f
        val outerR = s * 0.35f
        val innerR = s * 0.15f
        val toothH = s * 0.1f

        // Gear teeth (8 segments)
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0))
            val cos = Math.cos(angle).toFloat()
            val sin = Math.sin(angle).toFloat()
            val x1 = cx + cos * (outerR - toothH)
            val y1 = cy + sin * (outerR - toothH)
            val x2 = cx + cos * outerR
            val y2 = cy + sin * outerR
            c.drawLine(x1, y1, x2, y2, paint)
        }

        // Outer circle
        c.drawCircle(cx, cy, outerR - toothH, paint)

        // Inner circle
        c.drawCircle(cx, cy, innerR, fillPaint)
    }
}
