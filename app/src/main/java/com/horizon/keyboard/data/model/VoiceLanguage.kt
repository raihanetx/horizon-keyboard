package com.horizon.keyboard.data.model

import com.horizon.keyboard.util.Constants

/**
 * Represents a supported voice recognition language.
 */
data class VoiceLanguage(
    val code: String,
    val displayName: String,
    val prompt: String,
    val locale: String
) {
    companion object {
        val ENGLISH = VoiceLanguage(
            code = Constants.Language.ENGLISH_CODE,
            displayName = Constants.Language.ENGLISH_DISPLAY,
            prompt = "Speak in English...",
            locale = Constants.Language.ENGLISH_CODE
        )
        
        val BANGLA = VoiceLanguage(
            code = Constants.Language.BANGLA_CODE,
            displayName = Constants.Language.BANGLA_DISPLAY,
            prompt = "বাংলায় বলুন...",
            locale = Constants.Language.BANGLA_CODE
        )
        
        fun getAll(): List<VoiceLanguage> = listOf(ENGLISH, BANGLA)
        
        fun fromDisplayName(name: String): VoiceLanguage {
            return getAll().find { it.displayName == name } ?: ENGLISH
        }
    }
}
