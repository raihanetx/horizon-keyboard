package com.horizon.keyboard.voice

import com.horizon.keyboard.voice.VoiceLanguage
import com.horizon.keyboard.VoiceTranscriptionEngine
import com.horizon.keyboard.core.VoiceEngineType

/**
 * Routes voice recording to the correct engine based on settings, language, and API key availability.
 *
 * Engines:
 * - **Gemma** — Google AI Studio (Gemini 2.5 Flash, audio transcription)
 * - **Whisper** — English specialist (Groq)
 * - **Android** — Offline fallback (SpeechRecognizer)
 * - **Auto** — Routes by language: Bangla → Gemma, English → Whisper, fallback → Android
 *
 * @param voiceEngine The transcription engine that handles actual recording + API calls.
 * @param getEngineType Lambda to read current engine type from settings.
 * @param getCurrentLang Lambda to read current voice language code.
 */
class VoiceEngineRouter(
    private val voiceEngine: VoiceTranscriptionEngine,
    private val getEngineType: () -> VoiceEngineType,
    private val getCurrentLang: () -> String
) {

    /**
     * Which engine should handle the next recording session.
     */
    enum class Engine { WHISPER, GEMMA, ANDROID }

    /**
     * Determine which engine to use right now.
     *
     * @return The engine to use, considering settings, language, and API key availability.
     */
    fun resolve(): Engine = resolveWithStatus().first

    /**
     * Resolve engine and return a user-facing warning if the selected engine
     * fell back to Android due to a missing API key.
     *
     * @return Pair of (engine, optional warning message).
     */
    fun resolveWithStatus(): Pair<Engine, String?> {
        val engineType = getEngineType()
        val lang = getCurrentLang()

        return when (engineType) {
            VoiceEngineType.GEMMA_API -> {
                if (voiceEngine.gemmaApiKey.isNotEmpty()) Engine.GEMMA to null
                else Engine.ANDROID to "⚠️ Gemma API key not set — using Android built-in"
            }
            VoiceEngineType.WHISPER_GROQ -> {
                if (voiceEngine.groqApiKey.isNotEmpty()) Engine.WHISPER to null
                else Engine.ANDROID to "⚠️ Groq API key not set — using Android built-in"
            }
            VoiceEngineType.AUTO -> {
                when {
                    lang == VoiceLanguage.BANGLA.gemmaCode && voiceEngine.gemmaApiKey.isNotEmpty() -> Engine.GEMMA to null
                    voiceEngine.groqApiKey.isNotEmpty() -> Engine.WHISPER to null
                    else -> Engine.ANDROID to null
                }
            }
            VoiceEngineType.ANDROID_BUILTIN -> Engine.ANDROID to null
        }
    }

    /**
     * Check if the currently active engine is recording audio (for Whisper/Gemma only).
     */
    fun isRecording(): Boolean = voiceEngine.isRecordingAudio

    /**
     * Start recording with the resolved engine.
     */
    fun startRecording(engine: Engine) {
        when (engine) {
            Engine.GEMMA -> voiceEngine.startGemmaRecording()
            Engine.WHISPER -> voiceEngine.startWhisperRecording()
            Engine.ANDROID -> { /* Handled by VoiceSessionManager (SpeechRecognizer) */ }
        }
    }

    /**
     * Stop recording and transcribe with the resolved engine.
     */
    fun stopAndTranscribe(engine: Engine) {
        when (engine) {
            Engine.GEMMA -> voiceEngine.stopGemmaAndTranscribe()
            Engine.WHISPER -> voiceEngine.stopWhisperAndTranscribe()
            Engine.ANDROID -> { /* Handled by VoiceSessionManager */ }
        }
    }
}
