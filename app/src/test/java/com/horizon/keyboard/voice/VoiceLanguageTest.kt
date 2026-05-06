package com.horizon.keyboard.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceLanguageTest {

    // ─── Enum Values ─────────────────────────────────────────────

    @Test
    fun `has exactly two languages`() {
        assertEquals(2, VoiceLanguage.entries.size)
    }

    @Test
    fun `english has correct codes`() {
        assertEquals("English", VoiceLanguage.ENGLISH.displayName)
        assertEquals("en", VoiceLanguage.ENGLISH.whisperCode)
        assertEquals("en-US", VoiceLanguage.ENGLISH.localeCode)
    }

    @Test
    fun `bangla has correct codes`() {
        assertEquals("বাংলা (Bangla)", VoiceLanguage.BANGLA.displayName)
        assertEquals("bn", VoiceLanguage.BANGLA.whisperCode)
        assertEquals("bn-BD", VoiceLanguage.BANGLA.localeCode)
    }

    // ─── fromName Lookup ─────────────────────────────────────────

    @Test
    fun `fromName ENGLISH returns english`() {
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromName("ENGLISH"))
    }

    @Test
    fun `fromName BANGLA returns bangla`() {
        assertEquals(VoiceLanguage.BANGLA, VoiceLanguage.fromName("BANGLA"))
    }

    @Test
    fun `fromName unknown defaults to english`() {
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromName("FRENCH"))
    }

    @Test
    fun `fromName empty defaults to english`() {
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromName(""))
    }

    @Test
    fun `fromName case sensitive`() {
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromName("english"))
    }

    // ─── Whisper Code Mapping ────────────────────────────────────

    @Test
    fun `whisper codes are two letter`() {
        VoiceLanguage.entries.forEach { lang ->
            assertEquals(2, lang.whisperCode.length)
        }
    }

    @Test
    fun `locale codes are locale format`() {
        VoiceLanguage.entries.forEach { lang ->
            assertTrue(lang.localeCode.contains("-"))
        }
    }
}
