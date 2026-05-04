package com.horizon.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Handles audio recording and transcription via multiple engines:
 * - Whisper via Groq (English specialist)
 * - Gemma 4 (Bangla specialist)
 *
 * Extracted from KeyboardView for single-responsibility.
 */
class VoiceTranscriptionEngine(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    // ── Configuration ────────────────────────────────────────────

    var groqApiKey: String = ""
    var gemmaApiKey: String = ""
    var gemmaModelEn: String = "gemma-4-e4b-it"
    var gemmaModelBn: String = "gemma-4-e4b-it"
    var currentVoiceLang: String = "en-US"

    // ── State ────────────────────────────────────────────────────

    private var audioRecord: AudioRecord? = null
    var isRecordingAudio: Boolean = false
        private set

    private val audioHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val SAMPLE_RATE = 16000
    private val MAX_RECORD_SECONDS = 30
    private val MAX_RETRIES = 2

    // ── Callbacks ────────────────────────────────────────────────

    /** Called when transcription produces text */
    var onTranscriptionResult: ((String) -> Unit)? = null

    /** Called with status messages for UI display */
    var onStatusUpdate: ((String, String?) -> Unit)? = null  // (message, colorHex)

    /** Called to check if user stopped listening */
    var isUserStopped: () -> Boolean = { false }

    /** Called after transcription to decide whether to continue recording */
    var onShouldContinue: () -> Boolean = { false }

    /** Called to restart recording after transcription */
    var onRestartRecording: (() -> Unit)? = null

    // ── Whisper (Groq) ───────────────────────────────────────────

    fun startWhisperRecording() {
        if (!checkMicPermission()) return

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2
            )

            audioRecord?.startRecording()
            isRecordingAudio = true
            onStatusUpdate?.invoke("Listening (Whisper)...", "#34C759")

            audioHandler.postDelayed({
                if (isRecordingAudio) stopWhisperAndTranscribe()
            }, MAX_RECORD_SECONDS * 1000L)

        } catch (e: Exception) {
            onStatusUpdate?.invoke("Audio error: ${e.message}", null)
        }
    }

    fun stopWhisperAndTranscribe() {
        if (!isRecordingAudio) return
        isRecordingAudio = false

        onStatusUpdate?.invoke("Transcribing (Whisper)...", "#FF9F0A")

        try {
            audioRecord?.stop()
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val audioData = ByteArray(bufferSize * 10)
            val readCount = audioRecord?.read(audioData, 0, audioData.size) ?: 0
            audioRecord?.release()
            audioRecord = null

            if (readCount <= 0) {
                onStatusUpdate?.invoke("No audio captured", null)
                return
            }

            val trimmedData = audioData.copyOf(readCount)
            val wavData = pcmToWav(trimmedData, SAMPLE_RATE, 1, 16)
            val langHint = if (currentVoiceLang == "bn-BD") "bn" else "en"

            executor.execute {
                try {
                    val result = callWhisperGroqWithRetry(wavData, langHint)
                    mainHandler.post {
                        if (result.isNotEmpty()) {
                            onTranscriptionResult?.invoke(result)
                        } else {
                            onStatusUpdate?.invoke("No speech detected", null)
                        }
                        if (!isUserStopped() && onShouldContinue()) {
                            mainHandler.postDelayed({ onRestartRecording?.invoke() }, 300)
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        onStatusUpdate?.invoke("Transcription failed: ${e.message}", null)
                        if (!isUserStopped() && onShouldContinue()) {
                            mainHandler.postDelayed({ onRestartRecording?.invoke() }, 1000)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            onStatusUpdate?.invoke("Error: ${e.message}", null)
            audioRecord?.release()
            audioRecord = null
        }
    }

    // ── Gemma ────────────────────────────────────────────────────

    fun startGemmaRecording() {
        if (!checkMicPermission()) return

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2
            )

            audioRecord?.startRecording()
            isRecordingAudio = true
            onStatusUpdate?.invoke("Listening (Gemma)...", "#34C759")

            audioHandler.postDelayed({
                if (isRecordingAudio) stopGemmaAndTranscribe()
            }, MAX_RECORD_SECONDS * 1000L)

        } catch (e: Exception) {
            onStatusUpdate?.invoke("Audio error: ${e.message}", null)
        }
    }

    fun stopGemmaAndTranscribe() {
        if (!isRecordingAudio) return
        isRecordingAudio = false

        onStatusUpdate?.invoke("Transcribing...", "#FF9F0A")

        try {
            audioRecord?.stop()
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val audioData = ByteArray(bufferSize * 10)
            val readCount = audioRecord?.read(audioData, 0, audioData.size) ?: 0
            audioRecord?.release()
            audioRecord = null

            if (readCount <= 0) {
                onStatusUpdate?.invoke("No audio captured", null)
                return
            }

            val trimmedData = audioData.copyOf(readCount)
            val base64Audio = Base64.encodeToString(trimmedData, Base64.NO_WRAP)
            val langName = if (currentVoiceLang == "bn-BD") "Bangla" else "English"
            val model = if (currentVoiceLang == "bn-BD") gemmaModelBn else gemmaModelEn

            executor.execute {
                try {
                    val result = callGemmaApiWithRetry(base64Audio, langName, model)
                    mainHandler.post {
                        if (result.isNotEmpty()) {
                            onTranscriptionResult?.invoke(result)
                        } else {
                            onStatusUpdate?.invoke("No speech detected", null)
                        }
                        if (!isUserStopped() && onShouldContinue()) {
                            mainHandler.postDelayed({ onRestartRecording?.invoke() }, 500)
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        onStatusUpdate?.invoke("Transcription failed: ${e.message}", null)
                        if (!isUserStopped() && onShouldContinue()) {
                            mainHandler.postDelayed({ onRestartRecording?.invoke() }, 1000)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            onStatusUpdate?.invoke("Error: ${e.message}", null)
            audioRecord?.release()
            audioRecord = null
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────

    fun stopRecording() {
        isRecordingAudio = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun shutdown() {
        executor.shutdownNow()
        stopRecording()
    }

    // ── Private: Retry Wrappers ──────────────────────────────────

    private fun callWhisperGroqWithRetry(wavData: ByteArray, language: String): String {
        var lastException: Exception? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val result = callWhisperGroq(wavData, language)
                if (result.isNotEmpty()) return result
                // Empty result on 200 means no speech — don't retry
                return ""
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < MAX_RETRIES) Thread.sleep(1000L * (attempt + 1))
            } catch (e: java.io.IOException) {
                lastException = e
                if (attempt < MAX_RETRIES) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: Exception("Whisper transcription failed after ${MAX_RETRIES + 1} attempts")
    }

    private fun callGemmaApiWithRetry(base64Audio: String, language: String, model: String): String {
        var lastException: Exception? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val result = callGemmaApi(base64Audio, language, model)
                if (result.isNotEmpty()) return result
                return ""
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < MAX_RETRIES) Thread.sleep(1000L * (attempt + 1))
            } catch (e: java.io.IOException) {
                lastException = e
                if (attempt < MAX_RETRIES) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: Exception("Gemma transcription failed after ${MAX_RETRIES + 1} attempts")
    }

    // ── Private: API Calls ───────────────────────────────────────

    private fun callWhisperGroq(wavData: ByteArray, language: String): String {
        val boundary = "----HorizonKeyboard${System.currentTimeMillis()}"
        val url = java.net.URL("https://api.groq.com/openai/v1/audio/transcriptions")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $groqApiKey")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        val outputStream = connection.outputStream
        val writer = outputStream.bufferedWriter()

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
        writer.write("whisper-large-v3-turbo\r\n")

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

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            return connection.inputStream.bufferedReader().readText().trim()
        } else if (responseCode == 429) {
            throw java.io.IOException("Rate limited (429) — try again later")
        } else {
            val errorBody = try { connection.errorStream?.bufferedReader()?.readText()?.take(200) } catch (_: Exception) { null }
            throw java.io.IOException("Whisper API error $responseCode: ${errorBody ?: "unknown"}")
        }
    }

    private fun callGemmaApi(base64Audio: String, language: String, model: String): String {
        val url = java.net.URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$gemmaApiKey"
        )
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

        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.outputStream.use { os -> os.write(jsonBody.toByteArray()) }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            if (responseCode == 429) throw java.io.IOException("Rate limited (429) — try again later")
            val errorBody = try { connection.errorStream?.bufferedReader()?.readText()?.take(200) } catch (_: Exception) { null }
            throw java.io.IOException("Gemma API error $responseCode: ${errorBody ?: "unknown"}")
        }

        val response = connection.inputStream.bufferedReader().readText()
        val textStart = response.indexOf("\"text\":")
        if (textStart == -1) return ""
        val afterText = response.substring(textStart + 7).trim()
        if (afterText.startsWith("\"")) {
            val endQuote = afterText.indexOf("\"", 1)
            if (endQuote > 0) return afterText.substring(1, endQuote).trim()
        }
        return ""
    }

    // ── Private: PCM → WAV ───────────────────────────────────────

    private fun pcmToWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val totalSize = 44 + dataSize

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalSize - 8)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1)
        writeShortLE(header, 22, channels)
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, blockAlign)
        writeShortLE(header, 34, bitsPerSample)
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, dataSize)

        return header + pcmData
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    // ── Private: Helpers ─────────────────────────────────────────

    private fun checkMicPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onStatusUpdate?.invoke("Microphone permission needed", null)
            return false
        }
        return true
    }
}
