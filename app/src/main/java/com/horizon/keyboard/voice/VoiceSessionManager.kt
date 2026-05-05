package com.horizon.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * Manages the Android [SpeechRecognizer] lifecycle for offline/fallback voice input.
 *
 * Handles: initialization, start/stop listening, auto-restart on timeout/error,
 * error recovery, and cleanup.
 *
 * @param context Android context.
 * @param onResult Called when speech is recognized (text result).
 * @param onPartial Called with partial recognition results.
 * @param onStatusChange Called with status messages (message, isListening).
 * @param isStopped Called to check if user has stopped listening.
 */
class VoiceSessionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartial: (String) -> Unit,
    private val onStatusChange: (String, Boolean) -> Unit,
    private val isStopped: () -> Boolean
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Current voice language code (e.g. "en-US", "bn-BD"). */
    var language: String = "en-US"

    /** Whether the recognizer is actively listening. */
    val isActive: Boolean get() = isListening

    // ─── Lifecycle ───────────────────────────────────────────────

    /**
     * Initialize and start the speech recognizer.
     * Checks permissions and availability first.
     */
    fun start() {
        destroy()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatusChange("Speech not available", false)
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onStatusChange("Microphone permission needed", false)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        startListening()
    }

    /**
     * Stop listening but keep the recognizer alive.
     */
    fun stop() {
        try { speechRecognizer?.stopListening() } catch (e: Exception) { Log.w("VoiceSessionManager", "stopListening failed", e) }
        isListening = false
    }

    /**
     * Destroy the recognizer and release resources.
     */
    fun destroy() {
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (e: Exception) { Log.w("VoiceSessionManager", "destroy failed", e) }
        speechRecognizer = null
        isListening = false
    }

    // ─── Private ─────────────────────────────────────────────────

    private fun startListening() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
            onStatusChange("Failed — tap mic to retry", false)
        }
    }

    private fun restart() {
        destroy()
        mainHandler.postDelayed({
            if (!isStopped()) {
                start()
            }
        }, 500)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onStatusChange("Listening...", true)
            }

            override fun onBeginningOfSpeech() {
                onStatusChange("Listening...", true)
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onStatusChange("Processing...", false)
            }

            override fun onError(error: Int) {
                isListening = false
                if (isStopped()) return

                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening..."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error — retrying..."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error — retrying..."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission needed"
                    else -> "Restarting..."
                }
                onStatusChange(msg, false)

                if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    restart()
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val rawText = matches?.firstOrNull() ?: ""

                if (rawText.isNotEmpty()) {
                    onResult(rawText)
                }

                if (!isStopped()) {
                    mainHandler.postDelayed({
                        if (!isStopped()) {
                            destroy()
                            start()
                        }
                    }, 300)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotEmpty()) onPartial(partial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
