package com.horizon.keyboard.voice.api

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google AI Studio Gemma API client for audio transcription.
 */
object GemmaApi {

    private const val TAG = "GemmaApi"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_RETRIES = 2

    fun transcribe(base64Audio: String, apiKey: String, language: String, model: String): String {
        Log.d(TAG, "Transcribing: audio=${base64Audio.length}B base64, key=${apiKey.take(4)}..., lang=$language, model=$model")
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
                    {"inline_data": {"mime_type": "audio/wav", "data": "$base64Audio"}},
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
        Log.d(TAG, "Response code: $responseCode")

        return when {
            responseCode == 200 -> {
                val result = parseTranscriptionResult(connection.inputStream.bufferedReader().readText())
                Log.d(TAG, "Result: ${result.take(100)}")
                result
            }
            responseCode == 400 -> {
                val errorBody = try { connection.errorStream?.bufferedReader()?.readText()?.take(300) } catch (_: Exception) { null }
                Log.e(TAG, "Bad request (400): $errorBody")
                throw IOException("Invalid request (400). Check your API key and model.")
            }
            responseCode == 401 || responseCode == 403 -> {
                throw IOException("Access denied ($responseCode). Check your Google AI Studio API key.")
            }
            responseCode == 429 -> {
                throw IOException("Rate limited (429). Try again later.")
            }
            else -> {
                val errorBody = try { connection.errorStream?.bufferedReader()?.readText()?.take(300) } catch (_: Exception) { null }
                Log.e(TAG, "API error $responseCode: $errorBody")
                throw IOException("Gemma API error $responseCode: ${errorBody ?: "unknown"}")
            }
        }
    }

    private fun parseTranscriptionResult(response: String): String {
        return try {
            val json = JSONObject(response)
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (_: Exception) {
            ""
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
        throw lastException ?: IOException("Gemma transcription failed")
    }
}
