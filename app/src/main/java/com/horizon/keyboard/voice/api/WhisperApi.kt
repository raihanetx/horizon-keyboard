package com.horizon.keyboard.voice.api

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq Whisper API client — optimized for production use.
 *
 * Optimizations:
 * - Configurable model (large-v3 vs turbo)
 * - Proper retry with exponential backoff + jitter
 * - Permanent errors (401/403) never retried
 * - Streaming-ready response reading
 * - Configurable timeout based on audio length
 * - Language auto-detection when no hint provided
 */
object WhisperApi {

    private const val TAG = "WhisperApi"
    private const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_BASE_MS = 15_000
    private const val READ_TIMEOUT_PER_SEC_MS = 1_000 // +1s per second of audio
    private const val MAX_RETRIES = 2

    /** Available Whisper models on Groq. */
    enum class Model(val id: String, val displayName: String, val speedLabel: String) {
        LARGE_V3("whisper-large-v3", "Whisper Large V3", "Accurate"),
        LARGE_V3_TURBO("whisper-large-v3-turbo", "Whisper Large V3 Turbo", "Fast");

        companion object {
            fun fromId(id: String): Model =
                entries.find { it.id == id } ?: LARGE_V3_TURBO // default to turbo (faster)
        }
    }

    /**
     * Transcribe audio bytes via Groq Whisper API.
     *
     * @param wavData WAV-encoded audio bytes.
     * @param apiKey Groq API key.
     * @param language ISO-639-1 language code ("en", "bn") or null for auto-detect.
     * @param model Whisper model to use.
     * @return Transcribed text, or empty string if no speech detected.
     */
    fun transcribe(
        wavData: ByteArray,
        apiKey: String,
        language: String? = null,
        model: Model = Model.LARGE_V3_TURBO
    ): String {
        Log.d(TAG, "Transcribing: ${wavData.size}B, model=${model.id}, lang=${language ?: "auto"}")
        return withSmartRetry(MAX_RETRIES) {
            callApi(wavData, apiKey, language, model)
        }
    }

    private fun callApi(
        wavData: ByteArray,
        apiKey: String,
        language: String?,
        model: Model
    ): String {
        val boundary = "----HorizonKB${System.nanoTime()}"
        val url = URL(ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection

        // Dynamic timeout based on audio size (rough: 16kHz 16-bit mono = 32KB/s)
        val audioSeconds = wavData.size / 32000.0
        val readTimeout = (READ_TIMEOUT_BASE_MS + audioSeconds * READ_TIMEOUT_PER_SEC_MS).toInt()

        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = readTimeout

        // Build multipart body efficiently — single ByteArrayOutputStream
        val body = ByteArrayOutputStream(wavData.size + 512)

        fun writeField(name: String, value: String) {
            body.write("--$boundary\r\n".toByteArray())
            body.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
            body.write("$value\r\n".toByteArray())
        }

        writeField("model", model.id)
        if (!language.isNullOrEmpty()) {
            writeField("language", language)
        }
        writeField("response_format", "text")

        // Audio file field
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n".toByteArray())
        body.write("Content-Type: audio/wav\r\n\r\n".toByteArray())
        body.write(wavData)
        body.write("\r\n".toByteArray())

        body.write("--$boundary--\r\n".toByteArray())

        val bodyBytes = body.toByteArray()

        // Single write — no chunked encoding overhead
        connection.outputStream.use { it.write(bodyBytes) }

        val responseCode = connection.responseCode

        return when {
            responseCode == 200 -> {
                val result = connection.inputStream.bufferedReader().readText().trim()
                Log.d(TAG, "OK: ${result.take(80)}")
                result
            }
            responseCode == 401 -> throw PermanentException("Invalid API key")
            responseCode == 403 -> throw PermanentException("API key lacks Whisper access")
            responseCode == 429 -> throw RetryableException("Rate limited — try later")
            responseCode in 500..599 -> throw RetryableException("Server error ($responseCode)")
            else -> {
                val err = try { connection.errorStream?.bufferedReader()?.readText()?.take(200) } catch (_: Exception) { null }
                throw PermanentException("API error $responseCode: ${err ?: "unknown"}")
            }
        }
    }

    // ─── Smart Retry ────────────────────────────────────────────

    class PermanentException(message: String) : IOException(message)
    class RetryableException(message: String) : IOException(message)

    /**
     * Retry with exponential backoff + jitter. Never retries permanent errors.
     */
    private fun withSmartRetry(maxRetries: Int, block: () -> String): String {
        var lastException: IOException? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: PermanentException) {
                throw e // Never retry 401/403
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s... + random jitter (0-500ms)
                    val backoffMs = (1000L shl attempt) + (Math.random() * 500).toLong()
                    Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}, retry in ${backoffMs}ms")
                    Thread.sleep(backoffMs)
                }
            }
        }

        throw lastException ?: IOException("Whisper transcription failed")
    }
}
