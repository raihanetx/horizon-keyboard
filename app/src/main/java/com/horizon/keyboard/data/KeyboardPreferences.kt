package com.horizon.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import com.horizon.keyboard.voice.VoiceLanguage

/**
 * Type-safe wrapper around SharedPreferences for keyboard settings.
 *
 * Handles all non-sensitive configuration:
 * - Voice engine type selection
 * - Language preference
 * - Gemma model selection
 *
 * API keys are stored separately in [SecureKeyStore] (encrypted).
 */
class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Voice Engine Type ───────────────────────────────────────

    var voiceEngine: String
        get() = prefs.getString(KEY_VOICE_ENGINE, ENGINE_ANDROID) ?: ENGINE_ANDROID
        set(value) = prefs.edit().putString(KEY_VOICE_ENGINE, value).apply()

    // ─── Language ────────────────────────────────────────────────

    var selectedLanguage: String
        get() = prefs.getString(KEY_SELECTED_LANGUAGE, VoiceLanguage.ENGLISH.name)
            ?: VoiceLanguage.ENGLISH.name
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).apply()

    // ─── Gemma Models ────────────────────────────────────────────

    var gemmaModelEn: String
        get() = prefs.getString(KEY_GEMMA_MODEL_EN, DEFAULT_GEMMA_MODEL)
            ?: DEFAULT_GEMMA_MODEL
        set(value) = prefs.edit().putString(KEY_GEMMA_MODEL_EN, value).apply()

    var gemmaModelBn: String
        get() = prefs.getString(KEY_GEMMA_MODEL_BN, DEFAULT_GEMMA_MODEL)
            ?: DEFAULT_GEMMA_MODEL
        set(value) = prefs.edit().putString(KEY_GEMMA_MODEL_BN, value).apply()

    // ─── Constants ───────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME = "horizon_keyboard"

        const val ENGINE_ANDROID = "android"
        const val ENGINE_WHISPER_GROQ = "whisper_groq"
        const val ENGINE_GEMMA = "gemma"
        const val ENGINE_AUTO = "auto"

        private const val KEY_VOICE_ENGINE = "voice_engine"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val KEY_GEMMA_MODEL_EN = "gemma_model_en"
        private const val KEY_GEMMA_MODEL_BN = "gemma_model_bn"

        const val DEFAULT_GEMMA_MODEL = "gemma-4-e4b-it"
    }
}
