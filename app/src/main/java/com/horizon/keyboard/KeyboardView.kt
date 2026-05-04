package com.horizon.keyboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.graphics.Canvas
import android.graphics.Color

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
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Horizon Keyboard — Professional Edition
 * Uses Material Design vector drawables for all icons.
 * Features: number row, contextual enter key, improved UX.
 */
class KeyboardView(context: Context) : LinearLayout(context) {

    var onKeyPress: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    var onSpace: (() -> Unit)? = null
    var onPaste: ((String) -> Unit)? = null
    var onArrowKey: ((Int) -> Unit)? = null

    private var isShift = false
    private val shiftKeys = mutableListOf<TextView>()
    private val allLetterKeys = mutableListOf<TextView>()

    // Long press backspace
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private val BACKSPACE_INITIAL_DELAY = 400L
    private val BACKSPACE_REPEAT_DELAY = 70L

    // Voice typing
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var userStoppedListening = false
    private var currentVoiceLang = "en-US"
    private var voiceBarContainer: LinearLayout? = null
    private var voiceStatusText: TextView? = null
    private var voiceLangButton: TextView? = null
    private var voiceStartStopButton: ImageView? = null
    private var pendingHideRunnable: Runnable? = null

    // Voice Engine Settings
    // Supports: Android Built-in, Whisper via Groq (English specialist), Gemma 4 (Bangla specialist), Auto
    enum class VoiceEngineType { ANDROID_BUILTIN, WHISPER_GROQ, GEMMA_API, AUTO }
    private var voiceEngineType = VoiceEngineType.ANDROID_BUILTIN

    // Language selection — English or Bangla
    private var selectedLanguage = VoiceLanguage.ENGLISH

    // Voice engine (audio recording + API transcription)
    private val voiceEngine = VoiceTranscriptionEngine(context)

    // Header / Voice switching
    private var headerBar: LinearLayout? = null
    private var headerVoiceSlot: FrameLayout? = null

    // Keyboard / Symbol / Clipboard / Settings panel switching
    private var keyboardContainer: LinearLayout? = null
    private var symbolContainer: LinearLayout? = null
    private var mainContentContainer: FrameLayout? = null
    private var settingsPanel: View? = null

    // Clipboard history
    private var clipboardPanel: LinearLayout? = null
    private var clipboardListContainer: LinearLayout? = null
    private var savedClipboardListContainer: LinearLayout? = null
    private val clipHistory = mutableListOf<String>()
    private val savedClipHistory = mutableListOf<String>()

    // Enter key contextual styling
    private var enterKeyView: TextView? = null
    private var currentImeAction: Int = EditorInfo.IME_ACTION_UNSPECIFIED

    // Voice bar animation
    private val FADE_DURATION = 200L

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        val p = dp(6)
        setPadding(p, p, p, p)
        loadSettings()
        // Initialize voice language from settings
        currentVoiceLang = selectedLanguage.gemmaCode
        buildKeyboard()
    }

    /**
     * Call from service to set the enter key style based on EditorInfo
     */
    fun updateImeOptions(imeOptions: Int) {
        currentImeAction = imeOptions and EditorInfo.IME_MASK_ACTION
        updateEnterKeyAppearance()
    }

    private fun updateEnterKeyAppearance() {
        val key = enterKeyView ?: return
        when (currentImeAction) {
            EditorInfo.IME_ACTION_SEARCH -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0)
                key.text = ""
                key.background = keyBgSolid("#0A84FF")
            }
            EditorInfo.IME_ACTION_SEND -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_send, 0)
                key.text = ""
                key.background = keyBgSolid("#34C759")
            }
            EditorInfo.IME_ACTION_GO -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = keyBgSolid("#0A84FF")
            }
            EditorInfo.IME_ACTION_DONE -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = keyBgSolid("#0A84FF")
            }
            EditorInfo.IME_ACTION_NEXT -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_forward, 0)
                key.text = ""
                key.background = keyBgSolid("#0A84FF")
            }
            else -> {
                key.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_enter, 0)
                key.text = ""
                key.background = keyBgSolid("#0A84FF")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildKeyboard() {
        addView(createHeaderVoiceSlot())
        addView(createSuggestionBar())

        mainContentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Keyboard Container
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

        symbolContainer = createSymbolPanel()
        mainContentContainer?.addView(symbolContainer)

        clipboardPanel = createClipboardPanel()
        mainContentContainer?.addView(clipboardPanel)

        settingsPanel = createSettingsPanel()
        mainContentContainer?.addView(settingsPanel)

        addView(mainContentContainer)
    }

    // ─── Header/Voice Slot ───────────────────────────────────────

    private fun createHeaderVoiceSlot(): FrameLayout {
        val slot = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).apply {
                bottomMargin = dp(4)
            }
        }

        headerBar = createHeaderBar()
        voiceBarContainer = createVoiceBar()

        slot.addView(headerBar)
        slot.addView(voiceBarContainer)

        headerVoiceSlot = slot
        return slot
    }

    // ─── Header Bar with Vector Drawable Icons ───────────────────

    private fun createHeaderBar(): LinearLayout {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left: keyboard icon (active indicator)
        header.addView(createIconImageView(R.drawable.ic_keyboard, dp(20), tint = "#0A84FF"))

        // Spacer
        header.addView(View(context).apply {
            layoutParams = LayoutParams(0, 1, 1f)
        })

        // Right: action icons with vector drawables
        val icons = listOf(
            R.drawable.ic_translate to { Toast.makeText(context, "Translate — Coming Soon", Toast.LENGTH_SHORT).show() },
            R.drawable.ic_clipboard to { toggleClipboardPanel() },
            R.drawable.ic_voice to { showVoiceBarForEngine() },
            R.drawable.ic_settings to { toggleSettingsPanel() }
        )

        icons.forEach { (drawableRes, action) ->
            header.addView(createHeaderIconButton(drawableRes, action))
        }

        return header
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderButton(iconType: IconView.IconType): LinearLayout {
        // Kept for compatibility but no longer used in main header
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(36), dp(32)).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            val p = dp(7)
            setPadding(p, p, p, p)
            background = pillBg("#3A3A3C")
        }

        val iconView = IconView(context, iconType).apply {
            layoutParams = LayoutParams(dp(16), dp(16))
        }

        container.addView(iconView)
        return container
    }

    private fun createIconImageView(drawableRes: Int, size: Int, tint: String? = null): ImageView {
        return ImageView(context).apply {
            layoutParams = LayoutParams(size, size)
            setImageResource(drawableRes)
            if (tint != null) {
                setColorFilter(Color.parseColor(tint))
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeaderIconButton(drawableRes: Int, onClick: () -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(36), dp(32)).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            val p = dp(6)
            setPadding(p, p, p, p)
            background = pillBg("#3A3A3C")
        }

        val iconView = ImageView(context).apply {
            layoutParams = LayoutParams(dp(18), dp(18))
            setImageResource(drawableRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = pillBg("#505052")
                    iconView.setColorFilter(Color.WHITE)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = pillBg("#3A3A3C")
                    iconView.clearColorFilter()
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        return container
    }

    // ─── Voice Bar ───────────────────────────────────────────────

    private fun createVoiceBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
            visibility = GONE
            alpha = 0f
        }

        // Left: Language toggle with globe icon
        val langContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(56), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C1E"))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.parseColor("#3A3A3C"))
            }
            setOnClickListener { toggleVoiceLanguage() }
        }
        val globeIcon = ImageView(context).apply {
            layoutParams = LayoutParams(dp(14), dp(14))
            setImageResource(R.drawable.ic_globe)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        langContainer.addView(globeIcon)
        voiceLangButton = TextView(context).apply {
            text = selectedLanguage.whisperCode.uppercase()
            setTextColor(Color.parseColor("#A0A0A8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(dp(4), 0, 0, 0)
        }
        langContainer.addView(voiceLangButton)
        bar.addView(langContainer)

        // Middle: Status text
        voiceStatusText = TextView(context).apply {
            text = "Tap mic to start"
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        bar.addView(voiceStatusText)

        // Right: Stop/Exit buttons (when listening) or Start button
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(28))
        }

        // Stop button (hidden by default)
        val stopBtn = ImageView(context).apply {
            layoutParams = LayoutParams(dp(24), dp(24))
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor("#FF453A"))
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = GONE
            setOnClickListener {
                userStoppedListening = true
                stopVoiceRecognition()
                hideVoiceBar()
            }
        }
        buttonContainer.addView(stopBtn)

        // Exit button (hidden by default)
        val exitBtn = ImageView(context).apply {
            layoutParams = LayoutParams(dp(24), dp(24)).apply {
                marginStart = dp(8)
            }
            setImageResource(R.drawable.ic_keyboard_dismiss)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = GONE
            setOnClickListener {
                stopListening()
                hideVoiceBar()
            }
        }
        buttonContainer.addView(exitBtn)

        // Start button (visible by default)
        voiceStartStopButton = ImageView(context).apply {
            layoutParams = LayoutParams(dp(28), dp(28))
            setImageResource(R.drawable.ic_voice)
            setColorFilter(Color.parseColor("#34C759"))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { toggleVoiceRecognition() }
        }
        buttonContainer.addView(voiceStartStopButton)

        bar.addView(buttonContainer)

        // Store references for state management
        bar.tag = arrayOf(stopBtn, exitBtn)

        voiceBarContainer = bar
        return bar
    }

    private fun showVoiceBar() {
        clipboardPanel?.visibility = GONE
        keyboardContainer?.visibility = VISIBLE

        pendingHideRunnable?.let { removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false
        destroyRecognizer()

        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = GONE
        }
        voiceBarContainer?.let { bar ->
            bar.visibility = VISIBLE
            bar.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }

        postDelayed({
            initSpeechRecognizer()
            startVoiceRecognition()
        }, FADE_DURATION + 100)

        scheduleHide(120_000L)
    }

    private fun hideVoiceBar() {
        userStoppedListening = true
        stopVoiceRecognition()

        voiceBarContainer?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            voiceBarContainer?.visibility = GONE
        }
        headerBar?.let { header ->
            header.visibility = VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
    }

    private fun scheduleHide(delayMs: Long) {
        pendingHideRunnable?.let { removeCallbacks(it) }
        val runnable = Runnable { hideVoiceBar() }
        pendingHideRunnable = runnable
        postDelayed(runnable, delayMs)
    }

    // ─── Speech Recognizer ───────────────────────────────────────

    private fun initSpeechRecognizer() {
        destroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceStatusText?.text = "Speech not available"
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voiceStatusText?.text = "Microphone permission needed"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    updateVoiceBarListeningState(true)
                    voiceStatusText?.text = "Listening..."
                    voiceStatusText?.setTextColor(Color.parseColor("#34C759"))
                }
                override fun onBeginningOfSpeech() {
                    voiceStatusText?.text = "Listening..."
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceStatusText?.text = "Processing..."
                    voiceStatusText?.setTextColor(Color.parseColor("#8E8E93"))
                }
                override fun onError(error: Int) {
                    isListening = false
                    if (userStoppedListening) {
                        resetVoiceButton()
                        return
                    }
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening..."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error — retrying..."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error — retrying..."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission needed"
                        else -> "Restarting..."
                    }
                    voiceStatusText?.text = msg
                    voiceStatusText?.setTextColor(Color.parseColor("#8E8E93"))
                    resetVoiceButton()
                    if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        postDelayed({
                            if (!userStoppedListening) {
                                initSpeechRecognizer()
                                startVoiceRecognition()
                            }
                        }, 500)
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawText = matches?.firstOrNull() ?: ""
                    if (rawText.isNotEmpty()) {
                        val processed = processVoiceInput(rawText)
                        voiceStatusText?.text = "\"$rawText\""
                        processed.forEach { action ->
                            when (action) {
                                is VoiceCommandProcessor.Action.Text -> onKeyPress?.invoke(action.value)
                                is VoiceCommandProcessor.Action.Backspace -> onBackspace?.invoke()
                                is VoiceCommandProcessor.Action.Enter -> onEnter?.invoke()
                                is VoiceCommandProcessor.Action.Space -> onSpace?.invoke()
                                is VoiceCommandProcessor.Action.Escape -> onKeyPress?.invoke("\u001B")
                                is VoiceCommandProcessor.Action.ArrowUp -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_UP)
                                is VoiceCommandProcessor.Action.ArrowDown -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                                is VoiceCommandProcessor.Action.ArrowLeft -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
                                is VoiceCommandProcessor.Action.ArrowRight -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
                            }
                        }
                    } else {
                        voiceStatusText?.text = "Listening..."
                    }
                    resetVoiceButton()
                    if (!userStoppedListening) {
                        postDelayed({
                            if (!userStoppedListening) {
                                initSpeechRecognizer()
                                startVoiceRecognition()
                            }
                        }, 300)
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

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }

    private fun updateVoiceBarListeningState(listening: Boolean) {
        val buttons = voiceBarContainer?.tag as? Array<*> ?: return
        val stopBtn = buttons[0] as? ImageView
        val exitBtn = buttons[1] as? ImageView

        if (listening) {
            voiceStartStopButton?.visibility = GONE
            stopBtn?.visibility = VISIBLE
            exitBtn?.visibility = VISIBLE
        } else {
            voiceStartStopButton?.visibility = VISIBLE
            stopBtn?.visibility = GONE
            exitBtn?.visibility = GONE
        }
    }

    // ─── Voice Command Processing ────────────────────────────────

    private fun processVoiceInput(raw: String): List<VoiceCommandProcessor.Action> =
        VoiceCommandProcessor.process(raw)


    private fun toggleVoiceRecognition() {
        if (isListening) {
            userStoppedListening = true
            if (voiceEngine == VoiceEngineType.GEMMA_API && voiceEngine.isRecordingAudio) {
                stopGemmaRecordingAndTranscribe()
            } else if (voiceEngine == VoiceEngineType.WHISPER_GROQ && voiceEngine.isRecordingAudio) {
                stopWhisperRecordingAndTranscribe()
            } else if (voiceEngine == VoiceEngineType.AUTO && voiceEngine.isRecordingAudio) {
                // Auto mode: use the appropriate stop function based on current language
                if (currentVoiceLang == VoiceLanguage.BANGLA.gemmaCode && voiceEngine.gemmaApiKey.isNotEmpty()) {
                    stopGemmaRecordingAndTranscribe()
                } else if (voiceEngine.groqApiKey.isNotEmpty()) {
                    stopWhisperRecordingAndTranscribe()
                } else {
                    stopVoiceRecognition()
                }
            } else {
                stopVoiceRecognition()
            }
            hideVoiceBar()
        } else {
            userStoppedListening = false
            when (voiceEngine) {
                VoiceEngineType.GEMMA_API -> {
                    if (voiceEngine.gemmaApiKey.isNotEmpty()) startGemmaRecording()
                }
                VoiceEngineType.WHISPER_GROQ -> {
                    if (voiceEngine.groqApiKey.isNotEmpty()) startWhisperRecording()
                }
                VoiceEngineType.AUTO -> {
                    // Auto: Bangla → Gemma, English → Whisper
                    if (currentVoiceLang == VoiceLanguage.BANGLA.gemmaCode && voiceEngine.gemmaApiKey.isNotEmpty()) {
                        startGemmaRecording()
                    } else if (voiceEngine.groqApiKey.isNotEmpty()) {
                        startWhisperRecording()
                    } else {
                        destroyRecognizer()
                        initSpeechRecognizer()
                        startVoiceRecognition()
                    }
                }
                else -> {
                    destroyRecognizer()
                    initSpeechRecognizer()
                    startVoiceRecognition()
                }
            }
        }
    }

    private fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentVoiceLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentVoiceLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
            voiceStatusText?.text = "Failed — tap mic to retry"
        }
    }

    private fun stopVoiceRecognition() {
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        isListening = false
        resetVoiceButton()
    }

    private fun stopListening() {
        userStoppedListening = true
        stopVoiceRecognition()
    }

    private fun resetVoiceButton() {
        updateVoiceBarListeningState(false)
    }

    private fun toggleVoiceLanguage() {
        // Toggle between English and Bangla
        currentVoiceLang = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode)
            VoiceLanguage.BANGLA.gemmaCode else VoiceLanguage.ENGLISH.gemmaCode
        voiceLangButton?.text = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "EN" else "BN"
        voiceStatusText?.text = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode)
            "Language: English" else "Language: বাংলা"
        if (isListening) {
            stopVoiceRecognition()
            postDelayed({ startVoiceRecognition() }, 200)
        }
    }

    fun cleanup() {
        backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
        backspaceRunnable = null
        pendingHideRunnable?.let { removeCallbacks(it) }
        pendingHideRunnable = null
        destroyRecognizer()
        stopAudioRecording()
    }

    // ─── Settings Management ─────────────────────────────────────

    private fun loadSettings() {
        try {
            val prefs = context.getSharedPreferences("horizon_keyboard", Context.MODE_PRIVATE)
            voiceEngineType = when (prefs.getString("voice_engine", "android")) {
                "whisper_groq" -> VoiceEngineType.WHISPER_GROQ
                "gemma" -> VoiceEngineType.GEMMA_API
                "auto" -> VoiceEngineType.AUTO
                else -> VoiceEngineType.ANDROID_BUILTIN
            }
            // API keys stored securely via Android Keystore + EncryptedSharedPreferences
            voiceEngine.groqApiKey = SecureKeyStore.getGroqKey(context)
            voiceEngine.gemmaApiKey = SecureKeyStore.getGemmaKey(context)
            voiceEngine.gemmaModelEn = prefs.getString("gemma_model_en", "gemma-4-e4b-it") ?: "gemma-4-e4b-it"
            voiceEngine.gemmaModelBn = prefs.getString("gemma_model_bn", "gemma-4-e4b-it") ?: "gemma-4-e4b-it"
            selectedLanguage = VoiceLanguage.fromName(
                prefs.getString("selected_language", VoiceLanguage.ENGLISH.name) ?: VoiceLanguage.ENGLISH.name
            )
            voiceEngine.currentVoiceLang = selectedLanguage.gemmaCode
            setupVoiceEngineCallbacks()
        } catch (_: Exception) {}
    }

    private fun saveSettings() {
        try {
            val prefs = context.getSharedPreferences("horizon_keyboard", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("voice_engine", when (voiceEngineType) {
                    VoiceEngineType.WHISPER_GROQ -> "whisper_groq"
                    VoiceEngineType.GEMMA_API -> "gemma"
                    VoiceEngineType.AUTO -> "auto"
                    else -> "android"
                })
                putString("gemma_model_en", voiceEngine.gemmaModelEn)
                putString("gemma_model_bn", voiceEngine.gemmaModelBn)
                putString("selected_language", selectedLanguage.name)
                apply()
            }
            // API keys stored securely via Android Keystore + EncryptedSharedPreferences
            if (voiceEngine.groqApiKey.isNotEmpty()) SecureKeyStore.setGroqKey(context, voiceEngine.groqApiKey)
            if (voiceEngine.gemmaApiKey.isNotEmpty()) SecureKeyStore.setGemmaKey(context, voiceEngine.gemmaApiKey)
        } catch (_: Exception) {}
    }

    // ─── Gemma Audio Transcription ────────────────────────────────

    // ─── Voice Engine Delegation ──────────────────────────────────

    private fun setupVoiceEngineCallbacks() {
        voiceEngine.onStatusUpdate = { message, color ->
            voiceStatusText?.text = message
            if (color != null) voiceStatusText?.setTextColor(Color.parseColor(color))
        }
        voiceEngine.isUserStopped = { userStoppedListening }
        voiceEngine.onShouldContinue = { !userStoppedListening }
        voiceEngine.onTranscriptionResult = { rawText ->
            voiceStatusText?.text = "\"$rawText\""
            val processed = processVoiceInput(rawText)
            processed.forEach { action ->
                when (action) {
                    is VoiceCommandProcessor.Action.Text -> onKeyPress?.invoke(action.value)
                    is VoiceCommandProcessor.Action.Backspace -> onBackspace?.invoke()
                    is VoiceCommandProcessor.Action.Enter -> onEnter?.invoke()
                    is VoiceCommandProcessor.Action.Space -> onSpace?.invoke()
                    is VoiceCommandProcessor.Action.Escape -> onKeyPress?.invoke("\u001B")
                    is VoiceCommandProcessor.Action.ArrowUp -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_UP)
                    is VoiceCommandProcessor.Action.ArrowDown -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                    is VoiceCommandProcessor.Action.ArrowLeft -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_LEFT)
                    is VoiceCommandProcessor.Action.ArrowRight -> onArrowKey?.invoke(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
                }
            }
        }
    }

    private fun startGemmaRecording() {
        isListening = true
        updateVoiceBarListeningState(true)
        voiceEngine.startGemmaRecording()
    }

    private fun stopGemmaRecordingAndTranscribe() {
        isListening = false
        voiceEngine.stopGemmaAndTranscribe()
    }

    private fun startWhisperRecording() {
        isListening = true
        updateVoiceBarListeningState(true)
        voiceEngine.startWhisperRecording()
    }

    private fun stopWhisperRecordingAndTranscribe() {
        isListening = false
        voiceEngine.stopWhisperAndTranscribe()
    }

    private fun showVoiceBarForEngine() {
        clipboardPanel?.visibility = GONE
        settingsPanel?.visibility = GONE
        keyboardContainer?.visibility = VISIBLE

        pendingHideRunnable?.let { removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false

        destroyRecognizer()
        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = GONE
        }
        voiceBarContainer?.let { bar ->
            bar.visibility = VISIBLE
            bar.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }

        when {
            voiceEngine == VoiceEngineType.GEMMA_API && voiceEngine.gemmaApiKey.isNotEmpty() -> {
                postDelayed({ startGemmaRecording() }, FADE_DURATION + 100)
            }
            voiceEngine == VoiceEngineType.WHISPER_GROQ && voiceEngine.groqApiKey.isNotEmpty() -> {
                postDelayed({ startWhisperRecording() }, FADE_DURATION + 100)
            }
            voiceEngine == VoiceEngineType.AUTO -> {
                // Auto: Indian language → Gemma, English → Whisper
                postDelayed({
                    if (currentVoiceLang == VoiceLanguage.BANGLA.gemmaCode && voiceEngine.gemmaApiKey.isNotEmpty()) {
                        startGemmaRecording()
                    } else if (voiceEngine.groqApiKey.isNotEmpty()) {
                        startWhisperRecording()
                    } else {
                        initSpeechRecognizer()
                        startVoiceRecognition()
                    }
                }, FADE_DURATION + 100)
            }
            else -> {
                postDelayed({
                    initSpeechRecognizer()
                    startVoiceRecognition()
                }, FADE_DURATION + 100)
            }
        }

        scheduleHide(120_000L)
    }

    // ─── Clipboard Panel ──────────────────────────────────────────

    private fun createClipboardPanel(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(200))
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
            visibility = GONE
        }

        // Header row with icon
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(30))
            gravity = Gravity.CENTER_VERTICAL
        }

        val clipIcon = ImageView(context).apply {
            layoutParams = LayoutParams(dp(16), dp(16))
            setImageResource(R.drawable.ic_clipboard)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        headerRow.addView(clipIcon)

        headerRow.addView(TextView(context).apply {
            text = "  CLIPBOARD"
            setTextColor(Color.parseColor("#636366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        })

        headerRow.addView(TextView(context).apply {
            text = "Clear All"
            setTextColor(Color.parseColor("#FF453A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setOnClickListener {
                clipHistory.clear()
                refreshClipboardPanel()
            }
        })

        val closeBtn = ImageView(context).apply {
            layoutParams = LayoutParams(dp(20), dp(20)).apply { marginStart = dp(8) }
            setImageResource(R.drawable.ic_close)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener {
                clipboardPanel?.visibility = GONE
                keyboardContainer?.visibility = VISIBLE
            }
        }
        headerRow.addView(closeBtn)

        panel.addView(headerRow)

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val scrollContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        clipboardListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scrollContent.addView(clipboardListContainer)

        val savedHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(24)).apply { topMargin = dp(8) }
            gravity = Gravity.CENTER_VERTICAL
        }
        savedHeader.addView(TextView(context).apply {
            text = "⭐ SAVED CLIPS"
            setTextColor(Color.parseColor("#FF9F0A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
        })
        scrollContent.addView(savedHeader)

        savedClipboardListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scrollContent.addView(savedClipboardListContainer)

        scrollView.addView(scrollContent)
        panel.addView(scrollView)

        clipboardPanel = panel
        loadInitialClipboard()
        return panel
    }

    private fun loadInitialClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty() && !clipHistory.contains(text)) {
                clipHistory.add(0, text)
            }
        }
        refreshClipboardPanel()
    }

    private fun toggleClipboardPanel() {
        clipboardPanel?.let { panel ->
            if (panel.visibility == GONE) {
                keyboardContainer?.visibility = GONE
                symbolContainer?.visibility = GONE
                settingsPanel?.visibility = GONE
                loadInitialClipboard()
                panel.visibility = VISIBLE
            } else {
                panel.visibility = GONE
                keyboardContainer?.visibility = VISIBLE
            }
        }
    }

    // ─── Settings Panel ──────────────────────────────────────────

    private fun toggleSettingsPanel() {
        settingsPanel?.let { panel ->
            if (panel.visibility == GONE) {
                keyboardContainer?.visibility = GONE
                symbolContainer?.visibility = GONE
                clipboardPanel?.visibility = GONE
                refreshSettingsPanel()
                panel.visibility = VISIBLE
            } else {
                panel.visibility = GONE
                keyboardContainer?.visibility = VISIBLE
            }
        }
    }

    private fun createSettingsPanel(): ScrollView {
        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(260))
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            visibility = GONE
        }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val pad = dp(10)
            setPadding(pad, pad, pad, pad)
        }

        scrollView.addView(panel)
        settingsPanel = scrollView
        return scrollView
    }

    private fun refreshSettingsPanel() {
        val scrollView = settingsPanel as? android.widget.ScrollView ?: return
        val panel = scrollView.getChildAt(0) as? LinearLayout ?: return
        panel.removeAllViews()

            // Title with icon
            val titleRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(8))
            }
            titleRow.addView(ImageView(context).apply {
                layoutParams = LayoutParams(dp(16), dp(16))
                setImageResource(R.drawable.ic_settings)
                scaleType = ImageView.ScaleType.FIT_CENTER
            })
            titleRow.addView(TextView(context).apply {
                text = "  SETTINGS"
                setTextColor(Color.parseColor("#A0A0A8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.05f
            })
            panel.addView(titleRow)

            // Voice Engine Section
            panel.addView(createSettingsSectionHeader("VOICE ENGINE"))

            val engines = listOf(
                "auto" to "Auto (Best for each language)",
                "whisper_groq" to "Whisper via Groq (English)",
                "gemma" to "Gemma 4 (Bangla)",
                "android" to "Android Built-in (Offline)"
            )
            engines.forEach { (key, label) ->
                val isSelected = (key == "auto" && voiceEngine == VoiceEngineType.AUTO) ||
                    (key == "whisper_groq" && voiceEngine == VoiceEngineType.WHISPER_GROQ) ||
                    (key == "gemma" && voiceEngine == VoiceEngineType.GEMMA_API) ||
                    (key == "android" && voiceEngine == VoiceEngineType.ANDROID_BUILTIN)
                panel.addView(createSettingsOption(label, isSelected) {
                    voiceEngine = when (key) {
                        "auto" -> VoiceEngineType.AUTO
                        "whisper_groq" -> VoiceEngineType.WHISPER_GROQ
                        "gemma" -> VoiceEngineType.GEMMA_API
                        else -> VoiceEngineType.ANDROID_BUILTIN
                    }
                    saveSettings()
                    refreshSettingsPanel()
                })
            }

            // ─── Language Selection ──────────────────────────────
            panel.addView(createSettingsSectionHeader("VOICE LANGUAGE"))

            VoiceLanguage.entries.forEach { lang ->
                panel.addView(createSettingsOption(
                    lang.displayName, currentVoiceLang == lang.gemmaCode
                ) {
                    currentVoiceLang = lang.gemmaCode
                    selectedLanguage = lang
                    saveSettings()
                    refreshSettingsPanel()
                })
            }

            // Groq API Key (if Whisper or Auto selected)
            if (voiceEngine == VoiceEngineType.WHISPER_GROQ || voiceEngine == VoiceEngineType.AUTO) {
                panel.addView(createSettingsSectionHeader("GROQ API KEY (WHISPER)"))
                val maskedGroq = maskKey(voiceEngine.groqApiKey)
                panel.addView(createSettingsTextInput(maskedGroq.ifEmpty { "Enter Groq API key..." }) { newKey ->
                    voiceEngine.groqApiKey = newKey
                    saveSettings()
                })
                panel.addView(TextView(context).apply {
                    text = "🔒 Encrypted with Android Keystore · Free: 2,000 RPD"
                    setTextColor(Color.parseColor("#636366"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                    setPadding(dp(10), 0, 0, dp(4))
                })
            }

            // Gemma API Key (if Gemma or Auto selected)
            if (voiceEngine == VoiceEngineType.GEMMA_API || voiceEngine == VoiceEngineType.AUTO) {
                panel.addView(createSettingsSectionHeader("GOOGLE AI STUDIO API KEY (GEMMA)"))
                val maskedGemma = maskKey(voiceEngine.gemmaApiKey)
                panel.addView(createSettingsTextInput(maskedGemma.ifEmpty { "Enter API key..." }) { newKey ->
                    voiceEngine.gemmaApiKey = newKey
                    saveSettings()
                })
                panel.addView(TextView(context).apply {
                    text = "🔒 Encrypted with Android Keystore · Free · Best for Bangla"
                    setTextColor(Color.parseColor("#636366"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                    setPadding(dp(10), 0, 0, dp(4))
                })
            }

            if (voiceEngine == VoiceEngineType.GEMMA_API || voiceEngine == VoiceEngineType.AUTO) {
                panel.addView(createSettingsSectionHeader("GEMMA MODEL"))
                val models = listOf(
                    "gemma-4-e4b-it" to "Gemma 4 E4B (4B — Better)",
                    "gemma-4-e2b-it" to "Gemma 4 E2B (2B — Faster)"
                )
                models.forEach { (model, label) ->
                    val isSelected = (currentVoiceLang == "bn-BD" && gemmaModelBn == model) ||
                        (currentVoiceLang == "en-US" && gemmaModelEn == model)
                    panel.addView(createSettingsOption(label, isSelected) {
                        if (currentVoiceLang == "bn-BD") gemmaModelBn = model else gemmaModelEn = model
                        saveSettings()
                        refreshSettingsPanel()
                    })
                }
            }

            // Close button with icon
            val closeRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
                setOnClickListener {
                    settingsPanel?.visibility = GONE
                    keyboardContainer?.visibility = VISIBLE
                }
            }
            closeRow.addView(ImageView(context).apply {
                layoutParams = LayoutParams(dp(14), dp(14))
                setImageResource(R.drawable.ic_close)
                setColorFilter(Color.parseColor("#636366"))
                scaleType = ImageView.ScaleType.FIT_CENTER
            })
            closeRow.addView(TextView(context).apply {
                text = " Close"
                setTextColor(Color.parseColor("#636366"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            })
            panel.addView(closeRow)
    }

    private fun createSettingsSectionHeader(title: String): TextView {
        return TextView(context).apply {
            text = title
            setTextColor(Color.parseColor("#FF9F0A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(0, dp(10), 0, dp(4))
        }
    }

    private fun createSettingsOption(label: String, selected: Boolean, onClick: () -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(36)).apply { bottomMargin = dp(2) }
            gravity = Gravity.CENTER_VERTICAL
            val pad = dp(10)
            setPadding(pad, 0, pad, 0)
            background = GradientDrawable().apply {
                setColor(if (selected) Color.parseColor("#1A3A5C") else Color.parseColor("#2C2C2E"))
                cornerRadius = dp(6).toFloat()
                if (selected) setStroke(dp(1), Color.parseColor("#0A84FF"))
            }
            setOnClickListener { onClick() }
        }

        container.addView(TextView(context).apply {
            text = if (selected) "● $label" else "   $label"
            setTextColor(if (selected) Color.parseColor("#0A84FF") else Color.parseColor("#A0A0A8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        })

        return container
    }

    private fun createSettingsTextInput(hint: String, onTextChanged: (String) -> Unit): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(36)).apply { bottomMargin = dp(4) }
            gravity = Gravity.CENTER_VERTICAL
            val pad = dp(10)
            setPadding(pad, 0, pad, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2C2C2E"))
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.parseColor("#3A3A3C"))
            }
        }

        container.addView(TextView(context).apply {
            text = if (hint.length > 8) "${hint.take(4)}...${hint.takeLast(4)}" else hint
            setTextColor(if (hint.length > 8) Color.parseColor("#A0A0A8") else Color.parseColor("#636366"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        })

        return container
    }

    private fun refreshClipboardPanel() {
        clipboardListContainer?.let { container ->
            container.removeAllViews()

            if (clipHistory.isEmpty()) {
                container.addView(TextView(context).apply {
                    text = "No clips yet.\nCopy text anywhere to track it here."
                    setTextColor(Color.parseColor("#636366"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    gravity = Gravity.CENTER
                    val pad = dp(12)
                    setPadding(0, pad, 0, pad)
                })
                return
            }

            clipHistory.forEachIndexed { index, clipText ->
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(4)
                    }
                    val pad = dp(10)
                    setPadding(pad, pad, pad, pad)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#2C2C2E"))
                        cornerRadius = dp(6).toFloat()
                    }
                    gravity = Gravity.CENTER_VERTICAL
                }

                item.addView(TextView(context).apply {
                    text = clipText
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.MONOSPACE
                    maxLines = 2
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                })

                item.isLongClickable = true
                item.setOnLongClickListener {
                    if (!savedClipHistory.contains(clipText)) {
                        savedClipHistory.add(0, clipText)
                        refreshClipboardPanel()
                        Toast.makeText(context, "⭐ Saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Already saved", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                // Paste button with icon
                val pasteContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, 0, 0)
                    setOnClickListener {
                        onPaste?.invoke(clipText)
                        clipboardPanel?.visibility = GONE
                        keyboardContainer?.visibility = VISIBLE
                    }
                }
                pasteContainer.addView(ImageView(context).apply {
                    layoutParams = LayoutParams(dp(14), dp(14))
                    setImageResource(R.drawable.ic_paste)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
                item.addView(pasteContainer)

                // Delete button with icon
                val deleteContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, 0, 0)
                    setOnClickListener {
                        clipHistory.removeAt(index)
                        refreshClipboardPanel()
                    }
                }
                deleteContainer.addView(ImageView(context).apply {
                    layoutParams = LayoutParams(dp(14), dp(14))
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Color.parseColor("#636366"))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
                item.addView(deleteContainer)

                container.addView(item)
            }
        }

        savedClipboardListContainer?.let { container ->
            container.removeAllViews()

            if (savedClipHistory.isEmpty()) {
                container.addView(TextView(context).apply {
                    text = "Long press any clip to save it here"
                    setTextColor(Color.parseColor("#48484A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    gravity = Gravity.CENTER
                    val pad = dp(8)
                    setPadding(0, pad, 0, pad)
                })
                return
            }

            savedClipHistory.forEachIndexed { index, clipText ->
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(4)
                    }
                    val pad = dp(10)
                    setPadding(pad, pad, pad, pad)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#2A2A1C"))
                        cornerRadius = dp(6).toFloat()
                        setStroke(dp(1), Color.parseColor("#FF9F0A"))
                    }
                    gravity = Gravity.CENTER_VERTICAL
                }

                item.addView(TextView(context).apply {
                    text = "⭐"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setPadding(0, 0, dp(6), 0)
                })

                item.addView(TextView(context).apply {
                    text = clipText
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.MONOSPACE
                    maxLines = 2
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                })

                val pasteContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, 0, 0)
                    setOnClickListener {
                        onPaste?.invoke(clipText)
                        clipboardPanel?.visibility = GONE
                        keyboardContainer?.visibility = VISIBLE
                    }
                }
                pasteContainer.addView(ImageView(context).apply {
                    layoutParams = LayoutParams(dp(14), dp(14))
                    setImageResource(R.drawable.ic_paste)
                    setColorFilter(Color.parseColor("#FF9F0A"))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
                item.addView(pasteContainer)

                val deleteContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), 0, 0, 0)
                    setOnClickListener {
                        savedClipHistory.removeAt(index)
                        refreshClipboardPanel()
                    }
                }
                deleteContainer.addView(ImageView(context).apply {
                    layoutParams = LayoutParams(dp(14), dp(14))
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Color.parseColor("#636366"))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
                item.addView(deleteContainer)

                container.addView(item)
            }
        }
    }

    fun onClipboardChanged(text: String) {
        if (text.isNotEmpty() && (clipHistory.isEmpty() || clipHistory.first() != text)) {
            clipHistory.add(0, text)
            if (clipHistory.size > 30) clipHistory.removeAt(clipHistory.lastIndex)
            if (clipboardPanel?.visibility == VISIBLE) refreshClipboardPanel()
        }
    }

    // ─── Suggestion Bar ──────────────────────────────────────────

    private fun createSuggestionBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).apply {
                bottomMargin = dp(4)
            }
            setBackgroundColor(Color.parseColor("#1E1E20"))
            val p = dp(6)
            setPadding(p, 0, p, 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val words = listOf("I", "Hello", "The", "Thanks", "How")

        words.forEachIndexed { index, word ->
            val tv = TextView(context).apply {
                text = word
                setTextColor(Color.parseColor("#A0A0A8"))
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
                                setColor(Color.parseColor("#3A3A3C"))
                                cornerRadius = dp(6).toFloat()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v as TextView).apply {
                            setTextColor(Color.parseColor("#A0A0A8"))
                            background = null
                        }
                        if (event.action == MotionEvent.ACTION_UP) {
                            onKeyPress?.invoke("$word ")
                        }
                        true
                    }
                    else -> false
                }
            }

            bar.addView(tv)

            if (index < words.size - 1) {
                bar.addView(View(context).apply {
                    layoutParams = LayoutParams(dp(1), dp(22))
                    setBackgroundColor(Color.parseColor("#333336"))
                })
            }
        }

        return bar
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
        // Shift key with icon
        val shiftTv = addSpecialKeyWithIcon(row, R.drawable.ic_shift, 1.5f) { toggleShift() }
        shiftKeys.add(shiftTv)
        "zxcvbnm".forEach { addKey(row, it.toString()) }
        addBackspaceKey(row)
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBackspaceKey(row: LinearLayout) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1.5f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            background = keyBgSolid("#48484A")
        }

        val iconView = ImageView(context).apply {
            layoutParams = LayoutParams(dp(20), dp(20))
            setImageResource(R.drawable.ic_backspace)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = keyBgSolid("#636366")
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
                    v.background = keyBgSolid("#48484A")
                    backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                    backspaceRunnable = null
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    private fun createKeyRow4(): LinearLayout {
        val row = createRow()
        addSpecialKey(row, "123", 1.5f, textSize = 11f) { toggleSymbolPanel() }

        // Comma key with icon
        addKeyWithIcon(row, ",", R.drawable.ic_comma)

        addSpecialKey(row, "SPACE", 5f, textSize = 11f) { onSpace?.invoke() }

        // Period key
        addKey(row, ".")

        // Enter/Done key with contextual icon
        enterKeyView = createEnterKey()
        row.addView(enterKeyView)
        updateEnterKeyAppearance()

        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEnterKey(): TextView {
        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = keyBgSolid("#0A84FF")
            compoundDrawablePadding = 0
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 2f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
        }

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = keyBgSolid("#0060CC")
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

    private fun createRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44)).apply {
                bottomMargin = dp(5)
            }
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addKey(row: LinearLayout, label: String, textSize: Float = 18f) {
        val tv = createKeyView(label, textSize)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        if (label[0].isLetter()) allLetterKeys.add(tv)

        val popup = createKeyPopup(label)

        tv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    (v as TextView).apply {
                        setTextColor(Color.WHITE)
                        background = keyBgPressed()
                    }
                    showKeyPopup(tv, popup)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v as TextView).apply {
                        setTextColor(Color.WHITE)
                        background = keyBgNormal()
                    }
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
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            background = keyBgNormal()
        }

        val iconView = ImageView(context).apply {
            layoutParams = LayoutParams(dp(16), dp(16))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = keyBgPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = keyBgNormal()
                    if (event.action == MotionEvent.ACTION_UP) {
                        onKeyPress?.invoke(label)
                    }
                    true
                }
                else -> false
            }
        }

        row.addView(container)
    }

    private fun createKeyPopup(label: String): TextView {
        return TextView(context).apply {
            text = label.uppercase()
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3A3A3C"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#5A5A5C"))
            }
            elevation = 12f
            visibility = GONE
        }
    }

    private fun showKeyPopup(anchor: View, popup: TextView) {
        try {
            val parent = anchor.parent as? ViewGroup ?: return
            if (popup.parent != null) (popup.parent as? ViewGroup)?.removeView(popup)

            popup.layoutParams = FrameLayout.LayoutParams(dp(48), dp(52))
            popup.visibility = VISIBLE

            val parentFrame = parent as? FrameLayout
            if (parentFrame != null) {
                parentFrame.addView(popup)
                popup.x = anchor.left.toFloat()
                popup.y = anchor.top.toFloat() - dp(54)
            } else {
                mainContentContainer?.addView(popup)
                val loc = IntArray(2)
                anchor.getLocationOnScreen(loc)
                val parentLoc = IntArray(2)
                mainContentContainer?.getLocationOnScreen(parentLoc)
                popup.x = (loc[0] - parentLoc[0]).toFloat()
                popup.y = (loc[1] - parentLoc[1]).toFloat() - dp(54)
            }
        } catch (_: Exception) {}
    }

    private fun hideKeyPopup(popup: TextView) {
        try {
            popup.visibility = GONE
            (popup.parent as? ViewGroup)?.removeView(popup)
        } catch (_: Exception) {}
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
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(44), weight).apply {
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

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpecialKeyWithIcon(
        row: LinearLayout,
        iconRes: Int,
        weight: Float,
        onClick: () -> Unit
    ): TextView {
        // Returns a TextView for shift state tracking, but displays icon
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(44), weight).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            background = keyBgSolid("#48484A")
        }

        val iconView = ImageView(context).apply {
            layoutParams = LayoutParams(dp(20), dp(20))
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.background = keyBgSolid("#636366")
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = if (isShift) keyBgSolid("#0A84FF") else keyBgSolid("#48484A")
                    if (event.action == MotionEvent.ACTION_UP) onClick()
                    true
                }
                else -> false
            }
        }

        row.addView(container)

        // Return a dummy TextView for shift state tracking
        return TextView(context).apply {
            tag = container // store reference to actual container
        }
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

    // ─── Symbol Panel ─────────────────────────────────────────────

    private fun toggleSymbolPanel() {
        if (symbolContainer?.visibility == GONE) {
            symbolContainer?.visibility = VISIBLE
            keyboardContainer?.visibility = GONE
            clipboardPanel?.visibility = GONE
            settingsPanel?.visibility = GONE
        } else {
            symbolContainer?.visibility = GONE
            keyboardContainer?.visibility = VISIBLE
        }
    }

    private fun createSymbolPanel(): LinearLayout {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            visibility = GONE
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

    @SuppressLint("ClickableViewAccessibility")
    private fun addSymbolKey(row: LinearLayout, label: String) {
        val tv = createKeyView(label)
        tv.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            marginStart = dp(2)
            marginEnd = dp(2)
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
                    if (event.action == MotionEvent.ACTION_UP) onKeyPress?.invoke(label)
                    true
                }
                else -> false
            }
        }

        row.addView(tv)
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
            // The actual view is stored in tag
            val actualView = tv.tag as? LinearLayout ?: return@forEach
            actualView.background = if (isShift) keyBgSolid("#0A84FF") else keyBgSolid("#48484A")
        }
    }

    // ─── Background Drawables ────────────────────────────────────

    private fun keyBgNormal(): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(Color.parseColor("#3D3D3F"), Color.parseColor("#2C2C2E"))
            cornerRadius = dp(10).toFloat()
        }
    }

    private fun keyBgPressed(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#505052"))
            cornerRadius = dp(10).toFloat()
        }
    }

    private fun keyBgSolid(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(10).toFloat()
        }
    }

    private fun pillBg(color: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(20).toFloat()
        }
    }

    /**
     * Mask an API key for display: show first 4 and last 4 chars, mask the rest.
     * "gsk_abc123def456ghi789" → "gsk_••••••••••i789"
     */
    private fun maskKey(key: String): String {
        if (key.length <= 12) return key
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        val masked = "•".repeat((key.length - 8).coerceAtMost(16))
        return "$prefix$masked$suffix"
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics
        ).toInt()
    }
}

// ─── Professional Canvas Icons (kept for backward compat) ────────

// IconView extracted to IconView.kt
