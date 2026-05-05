package com.horizon.keyboard.core

/**
 * Voice engine selection options.
 *
 * Used by [SettingsPanel] for UI and [VoiceEngineRouter] for routing decisions.
 */
enum class VoiceEngineType { ANDROID_BUILTIN, WHISPER_GROQ, GEMMA_API, AUTO }
