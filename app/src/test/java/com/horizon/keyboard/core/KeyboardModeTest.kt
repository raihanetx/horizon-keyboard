package com.horizon.keyboard.core

import org.junit.Assert.*
import org.junit.Test

class KeyboardModeTest {

    // ─── Mode Identity ───────────────────────────────────────────

    @Test
    fun `typing is singleton`() {
        assertSame(KeyboardMode.Typing, KeyboardMode.Typing)
    }

    @Test
    fun `symbol is singleton`() {
        assertSame(KeyboardMode.Symbol, KeyboardMode.Symbol)
    }

    @Test
    fun `clipboard is singleton`() {
        assertSame(KeyboardMode.Clipboard, KeyboardMode.Clipboard)
    }

    @Test
    fun `settings is singleton`() {
        assertSame(KeyboardMode.Settings, KeyboardMode.Settings)
    }

    // ─── Mode Distinctness ───────────────────────────────────────

    @Test
    fun `all modes are distinct`() {
        val modes = listOf(
            KeyboardMode.Typing,
            KeyboardMode.Symbol,
            KeyboardMode.Clipboard,
            KeyboardMode.Settings,
            KeyboardMode.Voice(KeyboardMode.VoiceEngine.ANDROID_BUILTIN)
        )
        assertEquals(modes.size, modes.toSet().size)
    }

    @Test
    fun `typing is not symbol`() {
        assertNotEquals(KeyboardMode.Typing, KeyboardMode.Symbol)
    }

    @Test
    fun `typing is not clipboard`() {
        assertNotEquals(KeyboardMode.Typing, KeyboardMode.Clipboard)
    }

    // ─── Voice Mode ──────────────────────────────────────────────

    @Test
    fun `voice mode with different engines are distinct`() {
        val v1 = KeyboardMode.Voice(KeyboardMode.VoiceEngine.ANDROID_BUILTIN)
        val v2 = KeyboardMode.Voice(KeyboardMode.VoiceEngine.WHISPER_GROQ)
        assertNotEquals(v1, v2)
    }

    @Test
    fun `voice mode with same engine are equal`() {
        val v1 = KeyboardMode.Voice(KeyboardMode.VoiceEngine.WHISPER_GROQ)
        val v2 = KeyboardMode.Voice(KeyboardMode.VoiceEngine.WHISPER_GROQ)
        assertEquals(v1, v2)
    }

    @Test
    fun `voice mode is not typing`() {
        val voice = KeyboardMode.Voice(KeyboardMode.VoiceEngine.ANDROID_BUILTIN)
        assertNotEquals(KeyboardMode.Typing, voice)
    }

    // ─── VoiceEngine Enum ────────────────────────────────────────

    @Test
    fun `voice engine has exactly three values`() {
        assertEquals(3, KeyboardMode.VoiceEngine.entries.size)
    }

    @Test
    fun `voice engine values are distinct`() {
        val engines = KeyboardMode.VoiceEngine.entries
        assertEquals(engines.size, engines.toSet().size)
    }

    // ─── Pattern Matching ────────────────────────────────────────

    @Test
    fun `when expression covers all modes`() {
        val modes: List<KeyboardMode> = listOf(
            KeyboardMode.Typing,
            KeyboardMode.Symbol,
            KeyboardMode.Clipboard,
            KeyboardMode.Settings,
            KeyboardMode.Voice(KeyboardMode.VoiceEngine.WHISPER_GROQ)
        )

        val labels = modes.map { mode ->
            when (mode) {
                is KeyboardMode.Typing -> "typing"
                is KeyboardMode.Symbol -> "symbol"
                is KeyboardMode.Clipboard -> "clipboard"
                is KeyboardMode.Settings -> "settings"
                is KeyboardMode.Voice -> "voice"
            }
        }

        assertEquals(listOf("typing", "symbol", "clipboard", "settings", "voice"), labels)
    }
}
