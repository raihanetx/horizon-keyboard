package com.horizon.keyboard.voice

import com.horizon.keyboard.VoiceTranscriptionEngine
import com.horizon.keyboard.core.VoiceEngineType

/**
 * Routes voice recording to the correct engine based on settings and API key availability.
 *
 * Engines:
 * - **Whisper** — Groq Whisper API (English specialist)
 * - **Android** — Offline fallback (SpeechRecognizer)
 * - **Auto** — Uses Whisper if key available, else Android
 */
class VoiceEngineRouter(
    private val voiceEngine: VoiceTranscriptionEngine,
    private val getEngineType: () -> VoiceEngineType
) {

    enum class Engine { WHISPER, ANDROID }

    fun resolve(): Engine = resolveWithStatus().first

    /**
     * Resolve engine and return a warning if falling back due to missing API key.
     */
    fun resolveWithStatus(): Pair<Engine, String?> {
        val engineType = getEngineType()

        return when (engineType) {
            VoiceEngineType.WHISPER_GROQ -> {
                if (voiceEngine.groqApiKey.isNotEmpty()) Engine.WHISPER to null
                else Engine.ANDROID to "⚠️ Groq API key not set — using Android built-in"
            }
            VoiceEngineType.AUTO -> {
                if (voiceEngine.groqApiKey.isNotEmpty()) Engine.WHISPER to null
                else Engine.ANDROID to null
            }
            VoiceEngineType.ANDROID_BUILTIN -> Engine.ANDROID to null
        }
    }

    fun isRecording(): Boolean = voiceEngine.isRecordingAudio

    fun startRecording(engine: Engine) {
        when (engine) {
            Engine.WHISPER -> voiceEngine.startWhisperRecording()
            Engine.ANDROID -> { /* Handled by VoiceSessionManager */ }
        }
    }

    fun stopAndTranscribe(engine: Engine) {
        when (engine) {
            Engine.WHISPER -> voiceEngine.stopWhisperAndTranscribe()
            Engine.ANDROID -> { /* Handled by VoiceSessionManager */ }
        }
    }
}
