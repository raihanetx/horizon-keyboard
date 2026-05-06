package com.horizon.keyboard.voice

/**
 * Supported voice languages: English and Bangla only.
 * Each language has codes for Whisper (Groq) and locale format.
 */
enum class VoiceLanguage(
    val displayName: String,
    val whisperCode: String,
    val localeCode: String
) {
    ENGLISH("English", "en", "en-US"),
    BANGLA("বাংলা (Bangla)", "bn", "bn-BD");

    companion object {
        fun fromName(name: String): VoiceLanguage {
            return entries.find { it.name == name } ?: ENGLISH
        }
    }
}
