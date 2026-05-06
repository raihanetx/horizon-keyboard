package com.horizon.keyboard

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import com.horizon.keyboard.ui.bar.VoiceBar
import com.horizon.keyboard.ui.panel.SettingsPanel
import com.horizon.keyboard.voice.VoiceEngineRouter
import com.horizon.keyboard.voice.VoiceSessionManager
import com.horizon.keyboard.voice.VoiceCommandProcessor
import com.horizon.keyboard.ui.theme.Colors
import com.horizon.keyboard.voice.VoiceLanguage


/**
 * Voice recognition coordinator — wires together voice UI, engine routing, and session management.
 *
 * Optimizations over original:
 * - VAD integration — auto-transcribes when user stops speaking (no manual tap needed)
 * - Continuous listening mode — mic stays on between utterances
 * - Better state machine — no race conditions between start/stop
 * - Smart auto-hide with activity awareness
 * - Mute/unmute handled atomically
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
    private var userStoppedListening = false
    private var currentVoiceLang = "en-US"
    private var isContinuousMode = true  // Keep listening between utterances
    private lateinit var voiceBar: VoiceBar
    private var pendingHideRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val FADE_DURATION = 200L
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // References to other panels for visibility toggling
    var headerBar: LinearLayout? = null
    var keyboardContainer: LinearLayout? = null
    var clipboardPanel: LinearLayout? = null
    var settingsPanelView: View? = null

    // ─── Extracted Components ────────────────────────────────────

    private val engineRouter = VoiceEngineRouter(
        voiceEngine = voiceEngine,
        getEngineType = { settingsPanel.voiceEngineType }
    )

    private val sessionManager = VoiceSessionManager(
        context = context,
        onResult = { rawText ->
            if (!userStoppedListening) {
                voiceBar.updateStatus("\"$rawText\"")
                processVoiceInput(rawText)
            }
        },
        onPartial = { partial ->
            if (!userStoppedListening) voiceBar.updateStatus(partial)
        },
        onStatusChange = { message, listening ->
            if (!userStoppedListening) {
                val engineMsg = if (listening) "📱 Android · Listening" else "📱 Android · $message"
                voiceBar.updateStatus(engineMsg, if (listening) Colors.ACCENT_GREEN else Colors.TEXT_DIM)
                voiceBar.updateListeningState(listening)
            }
        },
        isStopped = { userStoppedListening }
    )

    // ─── Voice Bar Creation ──────────────────────────────────────

    fun createVoiceBar(): LinearLayout {
        voiceBar = VoiceBar(
            context = context,
            onToggleLanguage = { toggleVoiceLanguage() },
            onStartListening = { toggleVoiceRecognition() },
            onStopAndTranscribe = {
                stopRecordingAndTranscribe()
            },
            onExit = {
                cancelAndClose()
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

        cancelPendingHide()
        userStoppedListening = false
        sessionManager.destroy()
        muteSystemSounds()

        hideHeaderShowVoiceBar()
        sessionManager.language = currentVoiceLang
        voiceBar.updateStatus("📱 Android · Listening", Colors.ACCENT_GREEN)
        mainHandler.postDelayed({ sessionManager.start() }, FADE_DURATION + 100)
        scheduleHide(120_000L)
    }

    fun showVoiceBarForEngine() {
        clipboardPanel?.visibility = View.GONE
        settingsPanelView?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        cancelPendingHide()
        userStoppedListening = false

        sessionManager.destroy()
        muteSystemSounds()
        hideHeaderShowVoiceBar()

        // Sync model and VAD settings from settings panel
        syncEngineSettings()

        val (engine, warning) = engineRouter.resolveWithStatus()
        val engineLabel = when (engine) {
            VoiceEngineRouter.Engine.WHISPER -> "🎤 ${voiceEngine.whisperModel.displayName}"
            VoiceEngineRouter.Engine.ANDROID -> "📱 Android"
        }

        mainHandler.postDelayed({
            if (warning != null) {
                voiceBar.updateStatus(warning, Colors.ACCENT_ORANGE)
                mainHandler.postDelayed({ startEngine(engine, engineLabel) }, 1500)
            } else {
                startEngine(engine, engineLabel)
            }
        }, FADE_DURATION + 100)

        scheduleHide(120_000L)
    }

    private fun startEngine(engine: VoiceEngineRouter.Engine, engineLabel: String) {
        when (engine) {
            VoiceEngineRouter.Engine.WHISPER -> {
                voiceBar.updateListeningState(true)
                voiceBar.updateStatus("$engineLabel · Listening", Colors.ACCENT_GREEN)
                engineRouter.startRecording(engine)
            }
            VoiceEngineRouter.Engine.ANDROID -> {
                sessionManager.language = currentVoiceLang
                sessionManager.start()
            }
        }
    }

    fun hideVoiceBar() {
        userStoppedListening = true
        sessionManager.destroy()
        voiceEngine.stopRecording()

        cancelPendingHide()

        voiceBar.hide()
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
        mainHandler.postDelayed({ unmuteSystemSounds() }, 500)
    }

    /**
     * Stop recording and send audio for transcription.
     * For Whisper: calls stopAndTranscribe (record → encode → API → insert).
     * For Android: stops SpeechRecognizer (results come via callbacks).
     */
    private fun stopRecordingAndTranscribe() {
        userStoppedListening = true
        val engine = engineRouter.resolve()
        when (engine) {
            VoiceEngineRouter.Engine.WHISPER -> {
                voiceBar.updateStatus("⏳ Transcribing...", Colors.ACCENT_ORANGE)
                engineRouter.stopAndTranscribe(engine)
                // Auto-hide after transcription completes (handled in callbacks)
                scheduleHide(15_000L)
            }
            VoiceEngineRouter.Engine.ANDROID -> {
                sessionManager.stop()
                hideVoiceBar()
            }
        }
    }

    /**
     * Cancel everything — discard audio, stop recording, close immediately.
     */
    private fun cancelAndClose() {
        userStoppedListening = true
        sessionManager.destroy()
        voiceEngine.stopRecording()
        cancelPendingHide()

        voiceBar.hide()
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
        mainHandler.postDelayed({ unmuteSystemSounds() }, 500)
    }

    private fun stopEverythingAndClose() {
        userStoppedListening = true
        sessionManager.destroy()
        voiceEngine.stopRecording()
        cancelPendingHide()

        voiceBar.hide()
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.animate().alpha(1f).setDuration(FADE_DURATION).start()
        }
        mainHandler.postDelayed({ unmuteSystemSounds() }, 500)
    }

    private fun hideHeaderShowVoiceBar() {
        headerBar?.animate()?.alpha(0f)?.setDuration(FADE_DURATION)?.withEndAction {
            headerBar?.visibility = View.GONE
        }
        voiceBar.show()
    }

    private fun muteSystemSounds() {
        try { audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true) } catch (_: Exception) {}
    }

    private fun unmuteSystemSounds() {
        try { audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false) } catch (_: Exception) {}
    }

    private fun scheduleHide(delayMs: Long) {
        cancelPendingHide()
        val runnable = Runnable { hideVoiceBar() }
        pendingHideRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelPendingHide() {
        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
    }

    // ─── Voice Engine Callbacks ──────────────────────────────────

    fun setupVoiceEngineCallbacks() {
        voiceEngine.onStatusUpdate = { message, color ->
            voiceBar.updateStatus(message, color)
        }
        voiceEngine.isUserStopped = { userStoppedListening }
        voiceEngine.onShouldContinue = { isContinuousMode && !userStoppedListening }
        voiceEngine.onTranscriptionResult = { rawText ->
            voiceBar.updateStatus("\"$rawText\"")
            processVoiceInput(rawText)

            if (isContinuousMode && !userStoppedListening) {
                // Continuous mode: brief pause then restart listening
                voiceBar.updateStatus("Listening...", Colors.ACCENT_GREEN)
                // Don't auto-hide — keep voice bar open
            } else {
                // Single-shot mode: auto-hide after result
                mainHandler.postDelayed({ hideVoiceBar() }, 800)
            }
        }
    }

    // ─── Voice Toggle ────────────────────────────────────────────

    fun toggleVoiceRecognition() {
        if (userStoppedListening || !engineRouter.isRecording() && !sessionManager.isActive) {
            // Start listening
            userStoppedListening = false
            muteSystemSounds()
            syncEngineSettings()

            val (engine, warning) = engineRouter.resolveWithStatus()
            val engineLabel = when (engine) {
                VoiceEngineRouter.Engine.WHISPER -> "🎤 ${voiceEngine.whisperModel.displayName}"
                VoiceEngineRouter.Engine.ANDROID -> "📱 Android"
            }

            if (warning != null) {
                voiceBar.updateStatus(warning, Colors.ACCENT_ORANGE)
                mainHandler.postDelayed({ startEngine(engine, engineLabel) }, 1500)
            } else {
                startEngine(engine, engineLabel)
            }
        } else {
            // Stop listening
            stopEverythingAndClose()
        }
    }

    private fun toggleVoiceLanguage() {
        currentVoiceLang = if (currentVoiceLang == VoiceLanguage.ENGLISH.localeCode)
            VoiceLanguage.BANGLA.localeCode else VoiceLanguage.ENGLISH.localeCode
        voiceBar.updateLanguageLabel(if (currentVoiceLang == VoiceLanguage.ENGLISH.localeCode) "EN" else "BN")
        voiceBar.updateStatus(
            if (currentVoiceLang == VoiceLanguage.ENGLISH.localeCode) "Language: English" else "Language: বাংলা"
        )
        if (sessionManager.isActive) {
            sessionManager.stop()
            mainHandler.postDelayed({
                sessionManager.language = currentVoiceLang
                sessionManager.start()
            }, 200)
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

    // ─── Settings Sync ───────────────────────────────────────────

    /**
     * Sync engine model and VAD settings from the settings panel.
     * Called before every voice session to pick up any changes.
     */
    private fun syncEngineSettings() {
        // Model is already synced via settingsPanel → voiceEngine
        // VAD is always enabled for Whisper (the main optimization)
        engineRouter.setVAD(true)
    }

    // ─── Lifecycle ───────────────────────────────────────────────

    /**
     * Force-stop everything: mic, session, voice bar. Called when keyboard is hidden.
     * Guarantees mic is OFF after this call.
     */
    fun cleanup() {
        userStoppedListening = true
        cancelPendingHide()
        sessionManager.destroy()
        voiceEngine.stopRecording()
        voiceEngine.shutdown()
        voiceBar.hide()
        headerBar?.let { header ->
            header.visibility = View.VISIBLE
            header.alpha = 1f
        }
        unmuteSystemSounds()
    }

    fun syncLanguage() {
        currentVoiceLang = settingsPanel.selectedLanguage.localeCode
        voiceBar.updateLanguageLabel(settingsPanel.selectedLanguage.whisperCode.uppercase())
    }
}
