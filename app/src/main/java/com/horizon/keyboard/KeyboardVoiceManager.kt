package com.horizon.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import com.horizon.keyboard.ui.bar.VoiceBar
import com.horizon.keyboard.ui.panel.SettingsPanel
import com.horizon.keyboard.voice.VoiceEngineRouter
import com.horizon.keyboard.voice.VoiceSessionManager
import com.horizon.keyboard.voice.VoiceCommandProcessor

/**
 * Voice recognition coordinator — wires together voice UI, engine routing, and session management.
 *
 * Delegates to:
 * - [VoiceBar] for voice recording UI
 * - [VoiceEngineRouter] for engine selection (Whisper/Gemma/Android)
 * - [VoiceSessionManager] for Android SpeechRecognizer lifecycle
 * - [VoiceTranscriptionEngine] for Whisper/Gemma API recording
 * - [VoiceCommandProcessor] for voice command interpretation
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
    private lateinit var voiceBar: VoiceBar
    private var pendingHideRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val FADE_DURATION = 200L

    // References to other panels for visibility toggling
    var headerBar: LinearLayout? = null
    var keyboardContainer: LinearLayout? = null
    var clipboardPanel: LinearLayout? = null
    var settingsPanelView: View? = null

    // ─── Extracted Components ────────────────────────────────────

    private val engineRouter = VoiceEngineRouter(
        voiceEngine = voiceEngine,
        getEngineType = { settingsPanel.voiceEngineType },
        getCurrentLang = { currentVoiceLang }
    )

    private val sessionManager = VoiceSessionManager(
        context = context,
        onResult = { rawText ->
            voiceBar.updateStatus("\"$rawText\"")
            processVoiceInput(rawText)
        },
        onPartial = { partial -> voiceBar.updateStatus(partial) },
        onStatusChange = { message, listening ->
            voiceBar.updateStatus(message, if (listening) KeyboardTheme.ACCENT_GREEN else KeyboardTheme.TEXT_DIM)
            voiceBar.updateListeningState(listening)
        },
        isStopped = { userStoppedListening }
    )

    // ─── Voice Bar Creation ──────────────────────────────────────

    fun createVoiceBar(): LinearLayout {
        voiceBar = VoiceBar(
            context = context,
            onToggleLanguage = { toggleVoiceLanguage() },
            onStartListening = { toggleVoiceRecognition() },
            onStopListening = {
                userStoppedListening = true
                sessionManager.stop()
                hideVoiceBar()
            },
            onExit = {
                userStoppedListening = true
                sessionManager.stop()
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
        sessionManager.destroy()

        hideHeaderShowVoiceBar()
        sessionManager.language = currentVoiceLang
        mainHandler.postDelayed({ sessionManager.start() }, FADE_DURATION + 100)
        scheduleHide(120_000L)
    }

    fun showVoiceBarForEngine() {
        clipboardPanel?.visibility = View.GONE
        settingsPanelView?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE

        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        userStoppedListening = false

        sessionManager.destroy()
        hideHeaderShowVoiceBar()

        val engine = engineRouter.resolve()
        mainHandler.postDelayed({
            when (engine) {
                VoiceEngineRouter.Engine.GEMMA, VoiceEngineRouter.Engine.WHISPER -> {
                    voiceBar.updateListeningState(true)
                    engineRouter.startRecording(engine)
                }
                VoiceEngineRouter.Engine.ANDROID -> {
                    sessionManager.language = currentVoiceLang
                    sessionManager.start()
                }
            }
        }, FADE_DURATION + 100)

        scheduleHide(120_000L)
    }

    fun hideVoiceBar() {
        userStoppedListening = true
        sessionManager.stop()

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

    // ─── Voice Engine Callbacks ──────────────────────────────────

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

    // ─── Voice Toggle ────────────────────────────────────────────

    fun toggleVoiceRecognition() {
        if (userStoppedListening || !engineRouter.isRecording() && !sessionManager.isActive) {
            // Start listening
            userStoppedListening = false
            val engine = engineRouter.resolve()
            when (engine) {
                VoiceEngineRouter.Engine.GEMMA, VoiceEngineRouter.Engine.WHISPER -> {
                    voiceBar.updateListeningState(true)
                    engineRouter.startRecording(engine)
                }
                VoiceEngineRouter.Engine.ANDROID -> {
                    sessionManager.language = currentVoiceLang
                    sessionManager.start()
                }
            }
        } else {
            // Stop listening
            userStoppedListening = true
            val engine = engineRouter.resolve()
            when (engine) {
                VoiceEngineRouter.Engine.GEMMA, VoiceEngineRouter.Engine.WHISPER -> {
                    engineRouter.stopAndTranscribe(engine)
                }
                VoiceEngineRouter.Engine.ANDROID -> {
                    sessionManager.stop()
                }
            }
            hideVoiceBar()
        }
    }

    private fun toggleVoiceLanguage() {
        currentVoiceLang = if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode)
            VoiceLanguage.BANGLA.gemmaCode else VoiceLanguage.ENGLISH.gemmaCode
        voiceBar.updateLanguageLabel(if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "EN" else "BN")
        voiceBar.updateStatus(
            if (currentVoiceLang == VoiceLanguage.ENGLISH.gemmaCode) "Language: English" else "Language: বাংলা"
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

    // ─── Lifecycle ───────────────────────────────────────────────

    fun cleanup() {
        pendingHideRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingHideRunnable = null
        sessionManager.destroy()
        voiceEngine.stopRecording()
    }

    fun syncLanguage() {
        currentVoiceLang = settingsPanel.selectedLanguage.gemmaCode
        voiceBar.updateLanguageLabel(settingsPanel.selectedLanguage.whisperCode.uppercase())
    }
}
