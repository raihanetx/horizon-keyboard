package com.horizon.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.horizon.keyboard.ui.bar.VoiceBar
import com.horizon.keyboard.ui.panel.SettingsPanel

/**
 * Manages voice recognition — Android SpeechRecognizer, Whisper/Gemma engine delegation,
 * voice bar UI, and language toggling.
 *
 * Uses [VoiceBar] for the voice recording UI.
 * Uses [VoiceCommandProcessor] for voice command interpretation.
 */
class KeyboardVoiceManager(
    private val context: Context,
    private val voiceEngine: VoiceTranscriptionEngine,
    private val settingsPanel: SettingsPanel,
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

    private lateinit var voiceBar: VoiceBar
    private var pendingHideRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val FADE_DURATION = 200L

    // References to other panels for visibility toggling
    var headerBar: LinearLayout? = null
    var keyboardContainer: LinearLayout? = null
    var clipboardPanel: LinearLayout? = null
    var settingsPanelView: View? = null

    // ─── Voice Bar Creation ──────────────────────────────────────

    fun createVoiceBar(): LinearLayout {
        voiceBar = VoiceBar(
            context = context,
            onToggleLanguage = { toggleVoiceLanguage() },
            onStartListening = { toggleVoiceRecognition() },
            onStopListening = {
                userStoppedListening = true
                stopVoiceRecognition()
                hideVoiceBar()
            },
            onExit = {
                stopListening()
                hideVoiceBar()
            }
        )
        voiceBar.create()
        voiceBar.updateLanguageLabel(settingsPanel.selectedLanguage.whisperCode.uppercase())
        return voiceBar.view
    }

    // ─── Voice Bar Visibility ────────────────────────────────────

    fun showVoiceBar() {
        clipboardPanel?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false
        destroyRecognizer()

        hideHeaderShowVoiceBar()

        mainHandler.postDelayed({
            initSpeechRecognizer()
            startVoiceRecognition()
        }, FADE_DURATION + 100)

        scheduleHide(120_000L)
    }

    fun showVoiceBarForEngine() {
        clipboardPanel?.visibility = View.GONE
        settingsPanelView?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false

        destroyRecognizer()
        hideHeaderShowVoiceBar()

        val engineType = settingsPanel.voiceEngineType
        when {
            engineType == SettingsPanel.VoiceEngineType.GEMMA_API && voiceEngine.gemmaApiKey.isNotEmpty() -> {
                mainHandler.postDelayed({ startGemmaRecording() }, FADE_DURATION + 100)
            }
            engineType == SettingsPanel.VoiceEngineType.WHISPER_GROQ && voiceEngine.groqApiKey.isNotEmpty() -> {
                mainHandler.postDelayed({ startWhisperRecording() }, FADE_DURATION + 100)
            }
            engineType == SettingsPanel.VoiceEngineType.AUTO -> {
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

        voiceBar.hide()
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
    }

    private fun hideHeaderShowVoiceBar() {
        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = View.GONE
        }
        voiceBar.show()
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
            voiceBar.updateStatus("Speech not available")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voiceBar.updateStatus("Microphone permission needed")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    voiceBar.updateListeningState(true)
                    voiceBar.updateStatus("Listening...", KeyboardTheme.ACCENT_GREEN)
                }
                override fun onBeginningOfSpeech() {
                    voiceBar.updateStatus("Listening...")
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceBar.updateStatus("Processing...", KeyboardTheme.TEXT_DIM)
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
                    voiceBar.updateStatus(msg, KeyboardTheme.TEXT_DIM)
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
                        voiceBar.updateStatus("\"$rawText\"")
                    } else {
                        voiceBar.updateStatus("Listening...")
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
                    if (partial.isNotEmpty()) voiceBar.updateStatus(partial)
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
            voiceBar.updateStatus("Failed — tap mic to retry")
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
        voiceBar.updateListeningState(false)
    }

    // ─── Voice Engine Delegation (Whisper/Gemma) ─────────────────

    fun setupVoiceEngineCallbacks() {
        voiceEngine.onStatusUpdate = { message, color ->
            voiceBar.updateStatus(message, color)
        }
        voiceEngine.isUserStopped = { userStoppedListening }
        voiceEngine.onShouldContinue = { !userStoppedListening }
        voiceEngine.onTranscriptionResult = { rawText ->
            voiceBar.updateStatus("\"$rawText\"")
            processVoiceInput(rawText)
        }
    }

    private fun startGemmaRecording() {
        isListening = true
        voiceBar.updateListeningState(true)
        voiceEngine.startGemmaRecording()
    }

    private fun stopGemmaRecordingAndTranscribe() {
        isListening = false
        voiceEngine.stopGemmaAndTranscribe()
    }

    private fun startWhisperRecording() {
        isListening = true
        voiceBar.updateListeningState(true)
        voiceEngine.startWhisperRecording()
    }

    private fun stopWhisperRecordingAndTranscribe() {
        isListening = false
        voiceEngine.stopWhisperAndTranscribe()
    }

    // ─── Voice Toggle ────────────────────────────────────────────

    fun toggleVoiceRecognition() {
        val engineType = settingsPanel.voiceEngineType
        if (isListening) {
            userStoppedListening = true
            if (engineType == SettingsPanel.VoiceEngineType.GEMMA_API && voiceEngine.isRecordingAudio) {
                stopGemmaRecordingAndTranscribe()
            } else if (engineType == SettingsPanel.VoiceEngineType.WHISPER_GROQ && voiceEngine.isRecordingAudio) {
                stopWhisperRecordingAndTranscribe()
            } else if (engineType == SettingsPanel.VoiceEngineType.AUTO && voiceEngine.isRecordingAudio) {
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
                SettingsPanel.VoiceEngineType.GEMMA_API -> {
                    if (voiceEngine.gemmaApiKey.isNotEmpty()) startGemmaRecording()
                }
                SettingsPanel.VoiceEngineType.WHISPER_GROQ -> {
                    if (voiceEngine.groqApiKey.isNotEmpty()) startWhisperRecording()
                }
                SettingsPanel.VoiceEngineType.AUTO -> {
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
        voiceBar.updateLanguageLabel(if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "EN" else "BN")
        voiceBar.updateStatus(
            if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "Language: English" else "Language: বাংলা"
        )
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
        currentVoiceLang = settingsPanel.selectedLanguage.gemmaCode
        voiceBar.updateLanguageLabel(settingsPanel.selectedLanguage.whisperCode.uppercase())
    }
}
