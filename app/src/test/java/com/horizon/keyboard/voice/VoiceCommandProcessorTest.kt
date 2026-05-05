package com.horizon.keyboard.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceCommandProcessorTest {

    // ─── Plain Text ──────────────────────────────────────────────

    @Test
    fun `plain text returns single Text action`() {
        val result = VoiceCommandProcessor.process("hello world")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Text)
        assertEquals("hello world", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    @Test
    fun `empty string returns empty list`() {
        val result = VoiceCommandProcessor.process("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `whitespace-only returns empty list`() {
        val result = VoiceCommandProcessor.process("   ")
        assertTrue(result.isEmpty())
    }

    // ─── Basic Commands ──────────────────────────────────────────

    @Test
    fun `enter command`() {
        val result = VoiceCommandProcessor.process("enter")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `return command`() {
        val result = VoiceCommandProcessor.process("return")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `submit command`() {
        val result = VoiceCommandProcessor.process("submit")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `backspace command`() {
        val result = VoiceCommandProcessor.process("backspace")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Backspace)
    }

    @Test
    fun `delete command`() {
        val result = VoiceCommandProcessor.process("delete")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Backspace)
    }

    @Test
    fun `space command`() {
        val result = VoiceCommandProcessor.process("space")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Space)
    }

    @Test
    fun `blank command maps to space`() {
        val result = VoiceCommandProcessor.process("blank")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Space)
    }

    @Test
    fun `escape command`() {
        val result = VoiceCommandProcessor.process("escape")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Escape)
    }

    @Test
    fun `skip command maps to escape`() {
        val result = VoiceCommandProcessor.process("skip")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Escape)
    }

    // ─── Arrow Commands ──────────────────────────────────────────

    @Test
    fun `down arrow command`() {
        val result = VoiceCommandProcessor.process("down arrow")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowDown)
    }

    @Test
    fun `up arrow command`() {
        val result = VoiceCommandProcessor.process("up arrow")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowUp)
    }

    @Test
    fun `left arrow command`() {
        val result = VoiceCommandProcessor.process("left arrow")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowLeft)
    }

    @Test
    fun `right arrow command`() {
        val result = VoiceCommandProcessor.process("right arrow")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowRight)
    }

    // ─── Command Prefix (command + action) ───────────────────────

    @Test
    fun `command down prefix`() {
        val result = VoiceCommandProcessor.process("command down")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowDown)
    }

    @Test
    fun `command up prefix`() {
        val result = VoiceCommandProcessor.process("command up")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.ArrowUp)
    }

    @Test
    fun `cmd enter prefix`() {
        val result = VoiceCommandProcessor.process("cmd enter")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `command backspace prefix`() {
        val result = VoiceCommandProcessor.process("command backspace")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Backspace)
    }

    @Test
    fun `command space prefix`() {
        val result = VoiceCommandProcessor.process("command space")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Space)
    }

    @Test
    fun `command escape prefix`() {
        val result = VoiceCommandProcessor.process("command escape")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Escape)
    }

    // ─── Punctuation Words ───────────────────────────────────────

    @Test
    fun `dot becomes period`() {
        val result = VoiceCommandProcessor.process("hello dot com")
        assertEquals(1, result.size)
        assertEquals("hello.com", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    @Test
    fun `comma punctuation`() {
        val result = VoiceCommandProcessor.process("hello comma world")
        assertEquals(1, result.size)
        assertEquals("hello,world", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    @Test
    fun `at the rate becomes at sign`() {
        val result = VoiceCommandProcessor.process("user at the rate email dot com")
        assertEquals(1, result.size)
        assertEquals("user@email.com", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    @Test
    fun `question mark punctuation`() {
        val result = VoiceCommandProcessor.process("hello question mark")
        assertEquals(1, result.size)
        assertEquals("hello?", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    @Test
    fun `exclamation mark punctuation`() {
        val result = VoiceCommandProcessor.process("wow exclamation mark")
        assertEquals(1, result.size)
        assertEquals("wow!", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    // ─── Slash Commands ──────────────────────────────────────────

    @Test
    fun `slash agent produces slash text`() {
        val result = VoiceCommandProcessor.process("slash agent")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Text)
        assertEquals("/agent", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }

    // ─── Mixed Input ─────────────────────────────────────────────

    @Test
    fun `text followed by enter command`() {
        val result = VoiceCommandProcessor.process("hello enter")
        assertEquals(2, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Text)
        assertEquals("hello", (result[0] as VoiceCommandProcessor.Action.Text).value)
        assertTrue(result[1] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `text followed by backspace`() {
        val result = VoiceCommandProcessor.process("oops backspace")
        assertEquals(2, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Text)
        assertTrue(result[1] is VoiceCommandProcessor.Action.Backspace)
    }

    // ─── Case Insensitivity ──────────────────────────────────────

    @Test
    fun `uppercase input is normalized`() {
        val result = VoiceCommandProcessor.process("ENTER")
        assertEquals(1, result.size)
        assertTrue(result[0] is VoiceCommandProcessor.Action.Enter)
    }

    @Test
    fun `mixed case input is normalized`() {
        val result = VoiceCommandProcessor.process("Hello World")
        assertEquals(1, result.size)
        assertEquals("hello world", (result[0] as VoiceCommandProcessor.Action.Text).value)
    }
}
