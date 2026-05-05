package com.horizon.keyboard.core

/**
 * Represents the current mode of the keyboard.
 *
 * Only one mode is active at any time. Transitions are explicit
 * and managed by the coordinator (KeyboardView + PanelHost).
 *
 * This replaces scattered boolean flags and visibility checks with
 * a single, predictable state.
 */
sealed class KeyboardMode {

    /** Standard QWERTY typing mode. */
    object Typing : KeyboardMode()

    /** Symbol/number grid (toggled via "123" key). */
    object Symbol : KeyboardMode()

    /** Voice recording mode (one of the voice engines is active). */
    data class Voice(val engine: VoiceEngine) : KeyboardMode()

    /** Clipboard history panel is open. */
    object Clipboard : KeyboardMode()

    /** Settings panel is open. */
    object Settings : KeyboardMode()

    /**
     * Which voice engine is currently active.
     */
    enum class VoiceEngine {
        ANDROID_BUILTIN,
        WHISPER_GROQ,
        GEMMA_API
    }
}
