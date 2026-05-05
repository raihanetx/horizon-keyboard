package com.horizon.keyboard.voice.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq Whisper API client for audio transcription.
 *
 * Sends WAV audio to `api.groq.com/openai/v1/audio/transcriptions`
 * using the `whisper-large-v3-turbo` model.
 *
 * Thread-safe: each call is a standalone HTTP request.
 */
object WhisperApi {

    private const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val MODEL = "whisper-large-v3-turbo"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_RETRIES = 2

    /**
     * Transcribe WAV audio data via Groq Whisper API.
     *
     * @param wavData Complete WAV file bytes (header + PCM).
     * @param apiKey Groq API key.
     * @param language ISO language code ("en" or "bn").
     * @return Transcribed text, or empty string if no speech detected.
     * @throws IOException on network errors or non-200 responses.
     */
    fun transcribe(wavData: ByteArray, apiKey: String, language: String): String {
        return withRetry(MAX_RETRIES) {
            callApi(wavData, apiKey, language)
        }
    }

    private fun callApi(wavData: ByteArray, apiKey: String, language: String): String {
        val boundary = "----HorizonKeyboard${System.currentTimeMillis()}"
        val url = URL(ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        // Write multipart body
        val outputStream = connection.outputStream
        val writer = outputStream.bufferedWriter()

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
        writer.write("$MODEL\r\n")

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"language\"\r\n\r\n")
        writer.write("$language\r\n")

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n")
        writer.write("text\r\n")

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n")
        writer.write("Content-Type: audio/wav\r\n\r\n")
        writer.flush()

        outputStream.write(wavData)
        outputStream.flush()

        writer.write("\r\n")
        writer.write("--$boundary--\r\n")
        writer.flush()
        writer.close()

        // Read response
        val responseCode = connection.responseCode
        when {
            responseCode == 200 -> {
                connection.inputStream.bufferedReader().readText().trim()
            }
            responseCode == 429 -> {
                throw IOException("Rate limited (429) — try again later")
            }
            else -> {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText()?.take(200)
                } catch (_: Exception) { null }
                throw IOException("Whisper API error $responseCode: ${errorBody ?: "unknown"}")
            }
        }
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
                return result  // Empty = no speech, non-empty = success. Either way, don't retry.
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: IOException("Whisper transcription failed after ${maxRetries + 1} attempts")
    }
}
