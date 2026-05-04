package com.horizon.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Manages voice recognition — Android SpeechRecognizer, Whisper/Gemma engine delegation,
 * voice bar UI, and language toggling.
 * Extracted from KeyboardView for single-responsibility.
 */
class KeyboardVoiceManager(
    private val context: Context,
    private val voiceEngine: VoiceTranscriptionEngine,
    private val settingsManager: KeyboardSettingsManager,
    private val onKeyPress: ((String) -> Unit)? = null,
    private val onBackspace: (() -> Unit)? = null,
    private val onEnter: (() -> Unit)? = null,
    private val onSpace: (() -> Unit)? = null,
    private val onArrowKey: ((Int) -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var userStoppedListening = false
    private var currentVoiceLang = "en-US"

    // Voice bar views
    private var voiceBarContainer: LinearLayout? = null
    private var voiceStatusText: TextView? = null
    private var voiceLangButton: TextView? = null
    private var voiceStartStopButton: ImageView? = null
    private var pendingHideRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val FADE_DURATION = 200L

    // References to other panels for visibility toggling
    var headerBar: LinearLayout? = null
    var keyboardContainer: LinearLayout? = null
    var clipboardPanel: LinearLayout? = null
    var settingsPanel: View? = null

    private fun dp(value: Int): Int = KeyboardTheme.dp(context, value)

    // ─── Voice Bar Creation ──────────────────────────────────────

    fun createVoiceBar(): LinearLayout {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(KeyboardTheme.BG_KEY))
            val pad = dp(8)
            setPadding(pad, 0, pad, 0)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            alpha = 0f
        }

        // Language toggle
        val langContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(KeyboardTheme.BG_DARK))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.parseColor(KeyboardTheme.BG_PILL))
            }
            setOnClickListener { toggleVoiceLanguage() }
        }
        langContainer.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
            setImageResource(R.drawable.ic_globe)
            scaleType = ImageView.ScaleType.FIT_CENTER
        })
        voiceLangButton = TextView(context).apply {
            text = settingsManager.selectedLanguage.whisperCode.uppercase()
            setTextColor(Color.parseColor(KeyboardTheme.TEXT_SECONDARY))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
            setPadding(dp(4), 0, 0, 0)
        }
        langContainer.addView(voiceLangButton)
        bar.addView(langContainer)

        // Status text
        voiceStatusText = TextView(context).apply {
            text = "Tap mic to start"
            setTextColor(Color.parseColor(KeyboardTheme.TEXT_DIM))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            maxLines = 1
        }
        bar.addView(voiceStatusText)

        // Button container
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(28))
        }

        // Stop button
        val stopBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            setImageResource(R.drawable.ic_close)
            setColorFilter(Color.parseColor(KeyboardTheme.ACCENT_RED))
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setOnClickListener {
                userStoppedListening = true
                stopVoiceRecognition()
                hideVoiceBar()
            }
        }
        buttonContainer.addView(stopBtn)

        // Exit button
        val exitBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(8) }
            setImageResource(R.drawable.ic_keyboard_dismiss)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setOnClickListener {
                stopListening()
                hideVoiceBar()
            }
        }
        buttonContainer.addView(exitBtn)

        // Start button
        voiceStartStopButton = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            setImageResource(R.drawable.ic_voice)
            setColorFilter(Color.parseColor(KeyboardTheme.ACCENT_GREEN))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { toggleVoiceRecognition() }
        }
        buttonContainer.addView(voiceStartStopButton)

        bar.addView(buttonContainer)
        bar.tag = arrayOf(stopBtn, exitBtn)

        voiceBarContainer = bar
        return bar
    }

    // ─── Voice Bar Visibility ────────────────────────────────────

    fun showVoiceBar() {
        clipboardPanel?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false
        destroyRecognizer()

        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = View.GONE
        }
        voiceBarContainer?.let { bar ->
            bar.visibility = View.VISIBLE
            bar.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }

        mainHandler.postDelayed({
            initSpeechRecognizer()
            startVoiceRecognition()
        }, FADE_DURATION + 100)

        scheduleHide(120_000L)
    }

    fun showVoiceBarForEngine() {
        clipboardPanel?.visibility = View.GONE
        settingsPanel?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false

        destroyRecognizer()
        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = View.GONE
        }
        voiceBarContainer?.let { bar ->
            bar.visibility = View.VISIBLE
            bar.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }

        val engineType = settingsManager.voiceEngineType
        when {
            engineType == KeyboardSettingsManager.VoiceEngineType.GEMMA_API && voiceEngine.gemmaApiKey.isNotEmpty() -> {
                mainHandler.postDelayed({ startGemmaRecording() }, FADE_DURATION + 100)
            }
            engineType == KeyboardSettingsManager.VoiceEngineType.WHISPER_GROQ && voiceEngine.groqApiKey.isNotEmpty() -> {
                mainHandler.postDelayed({ startWhisperRecording() }, FADE_DURATION + 100)
            }
            engineType == KeyboardSettingsManager.VoiceEngineType.AUTO -> {
                mainHandler.postDelayed({
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
                mainHandler.postDelayed({
                    initSpeechRecognizer()
                    startVoiceRecognition()
                }, FADE_DURATION + 100)
            }
        }

        scheduleHide(120_000L)
    }

    fun hideVoiceBar() {
        userStoppedListening = true
        stopVoiceRecognition()

        voiceBarContainer?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            voiceBarContainer?.visibility = View.GONE
        }
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
    }

    private fun scheduleHide(delayMs: Long) {
        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable { hideVoiceBar() }
        pendingHideRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    // ─── Speech Recognizer (Android Built-in) ────────────────────

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
                    voiceStatusText?.setTextColor(Color.parseColor(KeyboardTheme.ACCENT_GREEN))
                }
                override fun onBeginningOfSpeech() {
                    voiceStatusText?.text = "Listening..."
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceStatusText?.text = "Processing..."
                    voiceStatusText?.setTextColor(Color.parseColor(KeyboardTheme.TEXT_DIM))
                }
                override fun onError(error: Int) {
                    isListening = false
                    if (userStoppedListening) { resetVoiceButton(); return }
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening..."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error — retrying..."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error — retrying..."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission needed"
                        else -> "Restarting..."
                    }
                    voiceStatusText?.text = msg
                    voiceStatusText?.setTextColor(Color.parseColor(KeyboardTheme.TEXT_DIM))
                    resetVoiceButton()
                    if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        mainHandler.postDelayed({
                            if (!userStoppedListening) { initSpeechRecognizer(); startVoiceRecognition() }
                        }, 500)
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawText = matches?.firstOrNull() ?: ""
                    if (rawText.isNotEmpty()) {
                        processVoiceInput(rawText)
                        voiceStatusText?.text = "\"$rawText\""
                    } else {
                        voiceStatusText?.text = "Listening..."
                    }
                    resetVoiceButton()
                    if (!userStoppedListening) {
                        mainHandler.postDelayed({
                            if (!userStoppedListening) { initSpeechRecognizer(); startVoiceRecognition() }
                        }, 300)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: ""
                    if (partial.isNotEmpty()) voiceStatusText?.text = partial
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun destroyRecognizer() {
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
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
        try { recognizer.startListening(intent) } catch (_: Exception) {
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

    private fun updateVoiceBarListeningState(listening: Boolean) {
        val buttons = voiceBarContainer?.tag as? Array<*> ?: return
        val stopBtn = buttons[0] as? ImageView
        val exitBtn = buttons[1] as? ImageView

        if (listening) {
            voiceStartStopButton?.visibility = View.GONE
            stopBtn?.visibility = View.VISIBLE
            exitBtn?.visibility = View.VISIBLE
        } else {
            voiceStartStopButton?.visibility = View.VISIBLE
            stopBtn?.visibility = View.GONE
            exitBtn?.visibility = View.GONE
        }
    }

    // ─── Voice Engine Delegation (Whisper/Gemma) ─────────────────

    fun setupVoiceEngineCallbacks() {
        voiceEngine.onStatusUpdate = { message, color ->
            voiceStatusText?.text = message
            if (color != null) voiceStatusText?.setTextColor(Color.parseColor(color))
        }
        voiceEngine.isUserStopped = { userStoppedListening }
        voiceEngine.onShouldContinue = { !userStoppedListening }
        voiceEngine.onTranscriptionResult = { rawText ->
            voiceStatusText?.text = "\"$rawText\""
            processVoiceInput(rawText)
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

    // ─── Voice Toggle ────────────────────────────────────────────

    fun toggleVoiceRecognition() {
        val engineType = settingsManager.voiceEngineType
        if (isListening) {
            userStoppedListening = true
            if (engineType == KeyboardSettingsManager.VoiceEngineType.GEMMA_API && voiceEngine.isRecordingAudio) {
                stopGemmaRecordingAndTranscribe()
            } else if (engineType == KeyboardSettingsManager.VoiceEngineType.WHISPER_GROQ && voiceEngine.isRecordingAudio) {
                stopWhisperRecordingAndTranscribe()
            } else if (engineType == KeyboardSettingsManager.VoiceEngineType.AUTO && voiceEngine.isRecordingAudio) {
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
            when (engineType) {
                KeyboardSettingsManager.VoiceEngineType.GEMMA_API -> {
                    if (voiceEngine.gemmaApiKey.isNotEmpty()) startGemmaRecording()
                }
                KeyboardSettingsManager.VoiceEngineType.WHISPER_GROQ -> {
                    if (voiceEngine.groqApiKey.isNotEmpty()) startWhisperRecording()
                }
                KeyboardSettingsManager.VoiceEngineType.AUTO -> {
                    if (currentVoiceLang == VoiceLanguage.BANGLA.gemmaCode && voiceEngine.gemmaApiKey.isNotEmpty()) {
                        startGemmaRecording()
                    } else if (voiceEngine.groqApiKey.isNotEmpty()) {
                        startWhisperRecording()
                    } else {
                        destroyRecognizer(); initSpeechRecognizer(); startVoiceRecognition()
                    }
                }
                else -> {
                    destroyRecognizer(); initSpeechRecognizer(); startVoiceRecognition()
                }
            }
        }
    }

    private fun toggleVoiceLanguage() {
        currentVoiceLang = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode)
            VoiceLanguage.BANGLA.gemmaCode else VoiceLanguage.ENGLISH.gemmaCode
        voiceLangButton?.text = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "EN" else "BN"
        voiceStatusText?.text = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode)
            "Language: English" else "Language: বাংলা"
        if (isListening) {
            stopVoiceRecognition()
            mainHandler.postDelayed({ startVoiceRecognition() }, 200)
        }
    }

    // ─── Voice Command Processing ────────────────────────────────

    private fun processVoiceInput(raw: String) {
        val processed = VoiceCommandProcessor.process(raw)
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

    // ─── Lifecycle ───────────────────────────────────────────────

    fun cleanup() {
        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        destroyRecognizer()
        voiceEngine.stopRecording()
    }

    fun syncLanguage() {
        currentVoiceLang = settingsManager.selectedLanguage.gemmaCode
        voiceLangButton?.text = settingsManager.selectedLanguage.whisperCode.uppercase()
    }
}
