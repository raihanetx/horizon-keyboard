package com.horizon.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.horizon.keyboard.voice.api.GemmaApi
import com.horizon.keyboard.voice.api.WhisperApi
import com.horizon.keyboard.voice.audio.AudioRecorder
import com.horizon.keyboard.voice.audio.WavEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Orchestrates voice transcription: record → encode → API call → callback.
 *
 * Delegates to:
 * - [AudioRecorder] for microphone capture
 * - [WavEncoder] for PCM → WAV conversion
 * - [WhisperApi] for Groq Whisper transcription
 * - [GemmaApi] for Google AI Gemma transcription
 */
class VoiceTranscriptionEngine(
    context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    // ── Configuration ────────────────────────────────────────────

    var groqApiKey: String = ""
    var gemmaApiKey: String = ""
    var gemmaModelEn: String = "gemma-4-e4b-it"
    var gemmaModelBn: String = "gemma-4-e4b-it"
    var currentVoiceLang: String = "en-US"

    // ── Internal Components ──────────────────────────────────────

    private val recorder = AudioRecorder(context)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioHandler = Handler(Looper.getMainLooper())
    private val MAX_RECORD_SECONDS = 30

    /** Whether the recorder is actively capturing audio. */
    val isRecordingAudio: Boolean get() = recorder.isRecording

    // ── Callbacks ────────────────────────────────────────────────

    var onTranscriptionResult: ((String) -> Unit)? = null
    var onStatusUpdate: ((String, String?) -> Unit)? = null
    var isUserStopped: () -> Boolean = { false }
    var onShouldContinue: () -> Boolean = { false }
    var onRestartRecording: (() -> Unit)? = null

    // ── Whisper (Groq) ───────────────────────────────────────────

    fun startWhisperRecording() {
        if (!recorder.hasPermission()) {
            onStatusUpdate?.invoke("Microphone permission needed", null)
            return
        }
        if (!recorder.start()) {
            onStatusUpdate?.invoke("Audio error: failed to start recorder", null)
            return
        }

        onStatusUpdate?.invoke("Listening (Whisper)...", "#34C759")
        scheduleAutoStop { stopWhisperAndTranscribe() }
    }

    fun stopWhisperAndTranscribe() {
        if (!recorder.isRecording) return
        val pcmData = recorder.stop()

        onStatusUpdate?.invoke("Transcribing (Whisper)...", "#FF9F0A")

        if (pcmData.isEmpty()) {
            onStatusUpdate?.invoke("No audio captured", null)
            return
        }

        val wavData = WavEncoder.encode(pcmData, AudioRecorder.SAMPLE_RATE, 1, 16)
        val langHint = if (currentVoiceLang == "bn-BD") "bn" else "en"

        executor.execute {
            try {
                val result = WhisperApi.transcribe(wavData, groqApiKey, langHint)
                handleTranscriptionResult(result)
            } catch (e: Exception) {
                handleTranscriptionError(e)
            }
        }
    }

    // ── Gemma ────────────────────────────────────────────────────

    fun startGemmaRecording() {
        if (!recorder.hasPermission()) {
            onStatusUpdate?.invoke("Microphone permission needed", null)
            return
        }
        if (!recorder.start()) {
            onStatusUpdate?.invoke("Audio error: failed to start recorder", null)
            return
        }

        onStatusUpdate?.invoke("Listening (Gemma)...", "#34C759")
        scheduleAutoStop { stopGemmaAndTranscribe() }
    }

    fun stopGemmaAndTranscribe() {
        if (!recorder.isRecording) return
        val pcmData = recorder.stop()

        onStatusUpdate?.invoke("Transcribing...", "#FF9F0A")

        if (pcmData.isEmpty()) {
            onStatusUpdate?.invoke("No audio captured", null)
            return
        }

        val wavData = WavEncoder.encode(pcmData, AudioRecorder.SAMPLE_RATE, 1, 16)
        val base64Audio = Base64.encodeToString(wavData, Base64.NO_WRAP)
        val langName = if (currentVoiceLang == "bn-BD") "Bangla" else "English"
        val model = if (currentVoiceLang == "bn-BD") gemmaModelBn else gemmaModelEn

        executor.execute {
            try {
                val result = GemmaApi.transcribe(base64Audio, gemmaApiKey, langName, model)
                handleTranscriptionResult(result)
            } catch (e: Exception) {
                handleTranscriptionError(e)
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────

    fun stopRecording() {
        recorder.release()
    }

    fun shutdown() {
        executor.shutdownNow()
        recorder.release()
    }

    // ── Private: Result Handling ─────────────────────────────────

    private fun handleTranscriptionResult(result: String) {
        mainHandler.post {
            if (result.isNotEmpty()) {
                onTranscriptionResult?.invoke(result)
            } else {
                onStatusUpdate?.invoke("No speech detected", null)
            }
            if (!isUserStopped() && onShouldContinue()) {
                mainHandler.postDelayed({ onRestartRecording?.invoke() }, 300)
            }
        }
    }

    private fun handleTranscriptionError(e: Exception) {
        mainHandler.post {
            onStatusUpdate?.invoke("Transcription failed: ${e.message}", null)
            if (!isUserStopped() && onShouldContinue()) {
                mainHandler.postDelayed({ onRestartRecording?.invoke() }, 1000)
            }
        }
    }

    /**
     * Schedule automatic stop after [MAX_RECORD_SECONDS].
     * Prevents infinite recording if user forgets to tap stop.
     */
    private fun scheduleAutoStop(onAutoStop: () -> Unit) {
        audioHandler.postDelayed({
            if (recorder.isRecording) onAutoStop()
        }, MAX_RECORD_SECONDS * 1000L)
    }
}
