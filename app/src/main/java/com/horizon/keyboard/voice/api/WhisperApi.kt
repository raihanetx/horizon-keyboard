package com.horizon.keyboard.voice.api

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq Whisper API client for audio transcription.
 *
 * Sends WAV audio to `api.groq.com/openai/v1/audio/transcriptions`
 * using the `whisper-large-v3-turbo` model.
 */
object WhisperApi {

    private const val TAG = "WhisperApi"
    private const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val MODEL = "whisper-large-v3"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_RETRIES = 2

    fun transcribe(wavData: ByteArray, apiKey: String, language: String): String {
        Log.d(TAG, "Transcribing: wavData=${wavData.size}B, key=${apiKey.take(4)}..., lang=$language")
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

        // Build multipart body using ONLY raw bytes (no character encoding issues)
        val body = ByteArrayOutputStream()

        // Model field
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n".toByteArray())
        body.write("$MODEL\r\n".toByteArray())

        // Language field
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"language\"\r\n\r\n".toByteArray())
        body.write("$language\r\n".toByteArray())

        // Response format field
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n".toByteArray())
        body.write("text\r\n".toByteArray())

        // File field (binary WAV data)
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n".toByteArray())
        body.write("Content-Type: audio/wav\r\n\r\n".toByteArray())
        body.write(wavData)
        body.write("\r\n".toByteArray())

        // Closing boundary
        body.write("--$boundary--\r\n".toByteArray())

        val bodyBytes = body.toByteArray()
        Log.d(TAG, "Request body: ${bodyBytes.size}B")

        // Write everything at once
        connection.outputStream.use { os ->
            os.write(bodyBytes)
        }

        // Read response
        val responseCode = connection.responseCode
        Log.d(TAG, "Response code: $responseCode")

        return when {
            responseCode == 200 -> {
                val result = connection.inputStream.bufferedReader().readText().trim()
                Log.d(TAG, "Transcription result: ${result.take(100)}")
                result
            }
            responseCode == 401 -> {
                throw IOException("Invalid API key (401). Check your Groq API key in settings.")
            }
            responseCode == 403 -> {
                throw IOException("Access denied (403). Your API key may not have Whisper access.")
            }
            responseCode == 429 -> {
                throw IOException("Rate limited (429). Try again later.")
            }
            else -> {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText()?.take(300)
                } catch (_: Exception) { null }
                Log.e(TAG, "API error $responseCode: $errorBody")
                throw IOException("Whisper API error $responseCode: ${errorBody ?: "unknown"}")
            }
        }
    }

    private fun withRetry(maxRetries: Int, block: () -> String): String {
        var lastException: IOException? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < maxRetries) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: IOException("Whisper transcription failed")
    }
}
