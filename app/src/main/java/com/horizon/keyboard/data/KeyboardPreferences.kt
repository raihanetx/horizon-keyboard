package com.horizon.keyboard.data

import android.content.Context
import android.content.SharedPreferences
import com.horizon.keyboard.voice.VoiceLanguage

/**
 * Type-safe wrapper around SharedPreferences for keyboard settings.
 */
class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var voiceEngine: String
        get() = prefs.getString(KEY_VOICE_ENGINE, ENGINE_ANDROID) ?: ENGINE_ANDROID
        set(value) = prefs.edit().putString(KEY_VOICE_ENGINE, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_SELECTED_LANGUAGE, VoiceLanguage.ENGLISH.name)
            ?: VoiceLanguage.ENGLISH.name
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).apply()

    var whisperModel: String
        get() = prefs.getString(KEY_WHISPER_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WHISPER_MODEL, value).apply()

    companion object {
        private const val PREFS_NAME = "horizon_keyboard"

        const val ENGINE_ANDROID = "android"
        const val ENGINE_WHISPER_GROQ = "whisper_groq"
        const val ENGINE_AUTO = "auto"

        private const val KEY_VOICE_ENGINE = "voice_engine"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val KEY_WHISPER_MODEL = "whisper_model"
    }
}
