package com.horizon.keyboard.voice.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google AI Studio Gemma API client for audio transcription.
 *
 * Sends Base64-encoded PCM audio to the Generative Language API.
 * Uses Gemma 4 models optimized for Bangla speech recognition.
 *
 * Thread-safe: each call is a standalone HTTP request.
 */
object GemmaApi {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_RETRIES = 2

    /**
     * Transcribe Base64-encoded PCM audio via Gemma API.
     *
     * @param base64Audio Base64-encoded raw PCM audio data.
     * @param apiKey Google AI Studio API key.
     * @param language Human-readable language name ("English" or "Bangla").
     * @param model Model identifier (e.g. "gemma-4-e4b-it").
     * @return Transcribed text, or empty string if no speech detected.
     * @throws IOException on network errors or non-200 responses.
     */
    fun transcribe(base64Audio: String, apiKey: String, language: String, model: String): String {
        return withRetry(MAX_RETRIES) {
            callApi(base64Audio, apiKey, language, model)
        }
    }

    private fun callApi(base64Audio: String, apiKey: String, language: String, model: String): String {
        val url = URL("$BASE_URL/$model:generateContent?key=$apiKey")
        val jsonBody = """
        {
            "contents": [{
                "parts": [
                    {"inline_data": {"mime_type": "audio/pcm", "data": "$base64Audio"}},
                    {"text": "Transcribe this audio into $language text. Return ONLY the transcribed text, nothing else."}
                ]
            }],
            "generationConfig": {"temperature": 0.1, "maxOutputTokens": 200}
        }
        """.trimIndent()

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.outputStream.use { os -> os.write(jsonBody.toByteArray()) }

        val responseCode = connection.responseCode
        when {
            responseCode == 200 -> {
                parseTranscriptionResult(connection.inputStream.bufferedReader().readText())
            }
            responseCode == 429 -> {
                throw IOException("Rate limited (429) — try again later")
            }
            else -> {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText()?.take(200)
                } catch (_: Exception) { null }
                throw IOException("Gemma API error $responseCode: ${errorBody ?: "unknown"}")
            }
        }
    }

    /**
     * Parse the transcription text from Gemma's JSON response.
     * Response format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
     */
    private fun parseTranscriptionResult(response: String): String {
        val textStart = response.indexOf("\"text\":")
        if (textStart == -1) return ""
        val afterText = response.substring(textStart + 7).trim()
        if (afterText.startsWith("\"")) {
            val endQuote = afterText.indexOf("\"", 1)
            if (endQuote > 0) return afterText.substring(1, endQuote).trim()
        }
        return ""
    }

    /**
     * Retry wrapper with linear backoff (1s, 2s, ...).
     * Only retries on [IOException] (network/timeout errors).
     * Empty results (no speech detected) are returned immediately without retry.
     */
    private fun withRetry(maxRetries: Int, block: () -> String): String {
        var lastException: IOException? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                val result = block()
                return result
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: IOException("Gemma transcription failed after ${maxRetries + 1} attempts")
    }
}
