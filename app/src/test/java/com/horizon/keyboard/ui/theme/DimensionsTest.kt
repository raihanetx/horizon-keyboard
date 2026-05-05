package com.horizon.keyboard.ui.theme

import org.junit.Assert.*
import org.junit.Test

class DimensionsTest {

    // ─── maskKey: Short Keys (≤12 chars) ─────────────────────────

    @Test
    fun `short key returned as-is`() {
        assertEquals("abc123", Dimensions.maskKey("abc123"))
    }

    @Test
    fun `key of exactly 12 chars returned as-is`() {
        val key = "a".repeat(12)
        assertEquals(key, Dimensions.maskKey(key))
    }

    @Test
    fun `empty key returned as-is`() {
        assertEquals("", Dimensions.maskKey(""))
    }

    // ─── maskKey: Long Keys (>12 chars) ──────────────────────────

    @Test
    fun `long key is masked`() {
        val key = "sk-abcdefghijklmnop"
        val masked = Dimensions.maskKey(key)
        assertTrue(masked.startsWith("sk-a"))
        assertTrue(masked.endsWith("mnop"))
        assertTrue(masked.contains("•"))
    }

    @Test
    fun `masked key preserves first 4 and last 4`() {
        val key = "1234567890123456"
        val masked = Dimensions.maskKey(key)
        assertEquals("1234", masked.take(4))
        assertEquals("3456", masked.takeLast(4))
    }

    @Test
    fun `masked key has correct total length`() {
        val key = "a".repeat(20)
        val masked = Dimensions.maskKey(key)
        // 4 prefix + masked (up to 16 dots) + 4 suffix
        val expectedMaskLen = (20 - 8).coerceAtMost(16)
        assertEquals(4 + expectedMaskLen + 4, masked.length)
    }

    @Test
    fun `very long key masks at most 16 chars`() {
        val key = "a".repeat(100)
        val masked = Dimensions.maskKey(key)
        val dotCount = masked.count { it == '•' }
        assertTrue(dotCount <= 16)
    }

    @Test
    fun `13 char key is masked`() {
        val key = "abcdefghijklm"
        val masked = Dimensions.maskKey(key)
        assertNotEquals(key, masked)
        assertTrue(masked.contains("•"))
    }

    // ─── maskKey: Edge Cases ─────────────────────────────────────

    @Test
    fun `key of 1 char returned as-is`() {
        assertEquals("x", Dimensions.maskKey("x"))
    }

    @Test
    fun `groq-style key is masked correctly`() {
        val key = "gsk_abcdefghij1234567890abcdefgh"
        val masked = Dimensions.maskKey(key)
        assertTrue(masked.startsWith("gsk_"))
        assertTrue(masked.endsWith("cdef"))
    }
}
