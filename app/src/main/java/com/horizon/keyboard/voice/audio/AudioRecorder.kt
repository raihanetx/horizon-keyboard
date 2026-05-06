package com.horizon.keyboard.voice.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * High-performance PCM audio recorder with Voice Activity Detection (VAD).
 *
 * Key optimizations over original:
 * - ByteArrayOutputStream (16x less heap vs MutableList<Byte>)
 * - Circular pre-roll buffer captures speech onset before VAD triggers
 * - RMS-based energy VAD with adaptive silence detection
 * - Reuses read buffer (zero-allocation inner loop)
 * - Uses VOICE_RECOGNITION audio source (better for ASR than MIC)
 */
class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SAMPLE = 2
        private const val MAX_RECORD_SECONDS = 30

        // VAD tuning
        private const val VAD_FRAME_MS = 30
        private const val VAD_SILENCE_THRESHOLD = 300   // RMS amplitude
        private const val VAD_SILENCE_DURATION_MS = 1500 // auto-stop after 1.5s silence
        private const val VAD_SPEECH_MIN_MS = 200        // minimum speech to count as valid
        private const val PREROLL_DURATION_MS = 250      // capture before speech onset
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    // Main recording buffer — ByteArrayOutputStream is far more efficient than MutableList<Byte>
    private val recordedData = ByteArrayOutputStream(64 * 1024)

    // Pre-roll: circular buffer of recent audio (before speech is detected)
    private var prerollBuf = ByteArray(SAMPLE_RATE * BYTES_PER_SAMPLE * PREROLL_DURATION_MS / 1000)
    private var prerollWritePos = 0
    private var prerollWrapped = false
    private var prerollSaved = false // true once pre-roll is flushed to main buffer

    @Volatile
    var isRecording: Boolean = false
        private set

    // VAD state
    private var vadSilenceStartMs = 0L
    private var vadSpeechDetected = false
    private var vadSpeechStartMs = 0L
    private var vadEnabled = true

    // Callbacks
    var onSilenceDetected: (() -> Unit)? = null
    var onSpeechDetected: (() -> Unit)? = null

    var enableVAD: Boolean
        get() = vadEnabled
        set(value) { vadEnabled = value }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(enableSilenceDetection: Boolean = true): Boolean {
        if (!hasPermission()) return false

        return try {
            recordedData.reset()
            prerollWritePos = 0
            prerollWrapped = false
            prerollSaved = false
            vadSilenceStartMs = 0
            vadSpeechDetected = false
            vadSpeechStartMs = 0
            vadEnabled = enableSilenceDetection

            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                .coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = Thread({
                recordLoop(bufferSize)
            }, "AudioRecorder-Thread").also { it.start() }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            false
        }
    }

    fun stop(): ByteArray {
        isRecording = false

        try { recordingThread?.join(2000) } catch (_: Exception) {}
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        return recordedData.toByteArray()
    }

    fun release() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "release failed", e)
        }
        audioRecord = null
    }

    // ─── Recording Loop ─────────────────────────────────────────

    private fun recordLoop(bufferSize: Int) {
        val readBuffer = ByteArray(bufferSize)
        val frameSize = SAMPLE_RATE * BYTES_PER_SAMPLE * VAD_FRAME_MS / 1000
        val frameAccum = ByteArray(frameSize)
        var frameOffset = 0
        val maxBytes = SAMPLE_RATE * BYTES_PER_SAMPLE * MAX_RECORD_SECONDS
        var totalBytes = 0

        while (isRecording && totalBytes < maxBytes) {
            val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
            if (read <= 0) {
                if (read < 0) Log.e(TAG, "AudioRecord read error: $read")
                break
            }

            // Before speech is detected, write into pre-roll circular buffer.
            // After speech is detected, write into main buffer.
            if (!vadEnabled || vadSpeechDetected) {
                if (!prerollSaved && vadEnabled) {
                    flushPreRoll()
                }
                synchronized(recordedData) {
                    recordedData.write(readBuffer, 0, read)
                }
            } else {
                // Pre-speech: rotate into circular pre-roll buffer
                writeToPreRoll(readBuffer, read)
            }

            totalBytes += read

            // VAD analysis
            if (vadEnabled) {
                var remaining = read
                var srcOff = 0
                while (remaining > 0 && isRecording) {
                    val needed = frameSize - frameOffset
                    val toCopy = minOf(needed, remaining)
                    System.arraycopy(readBuffer, srcOff, frameAccum, frameOffset, toCopy)
                    frameOffset += toCopy
                    srcOff += toCopy
                    remaining -= toCopy

                    if (frameOffset >= frameSize) {
                        val nowMs = totalBytes * 1000L / (SAMPLE_RATE * BYTES_PER_SAMPLE)
                        analyzeFrame(frameAccum, frameSize, nowMs)
                        frameOffset = 0
                    }
                }
            }
        }
    }

    // ─── Pre-Roll Buffer ────────────────────────────────────────

    private fun writeToPreRoll(data: ByteArray, length: Int) {
        val space = prerollBuf.size - prerollWritePos
        if (length >= prerollBuf.size) {
            // Data larger than pre-roll buffer — just keep the tail
            System.arraycopy(data, length - prerollBuf.size, prerollBuf, 0, prerollBuf.size)
            prerollWritePos = 0
            prerollWrapped = true
        } else if (length > space) {
            // Wraps around
            System.arraycopy(data, 0, prerollBuf, prerollWritePos, space)
            System.arraycopy(data, space, prerollBuf, 0, length - space)
            prerollWritePos = length - space
            prerollWrapped = true
        } else {
            System.arraycopy(data, 0, prerollBuf, prerollWritePos, length)
            prerollWritePos += length
        }
    }

    private fun flushPreRoll() {
        if (prerollSaved) return
        prerollSaved = true

        synchronized(recordedData) {
            if (prerollWrapped) {
                // Write from writePos to end, then from 0 to writePos
                recordedData.write(prerollBuf, prerollWritePos, prerollBuf.size - prerollWritePos)
                recordedData.write(prerollBuf, 0, prerollWritePos)
            } else if (prerollWritePos > 0) {
                recordedData.write(prerollBuf, 0, prerollWritePos)
            }
        }
    }

    // ─── VAD Analysis ───────────────────────────────────────────

    private fun analyzeFrame(frame: ByteArray, size: Int, nowMs: Long) {
        // Calculate RMS amplitude
        var sumSquares = 0.0
        var i = 0
        while (i < size - 1) {
            val sample = (frame[i].toInt() and 0xFF) or (frame[i + 1].toInt() shl 8)
            sumSquares += sample.toDouble() * sample.toDouble()
            i += 2
        }
        val rms = sqrt(sumSquares / (size / BYTES_PER_SAMPLE))

        if (rms > VAD_SILENCE_THRESHOLD) {
            // ── Speech ──
            if (!vadSpeechDetected) {
                vadSpeechDetected = true
                vadSpeechStartMs = nowMs
                onSpeechDetected?.invoke()
            }
            vadSilenceStartMs = 0L
        } else {
            // ── Silence ──
            if (vadSpeechDetected) {
                if (vadSilenceStartMs == 0L) {
                    vadSilenceStartMs = nowMs
                } else {
                    val silenceDuration = nowMs - vadSilenceStartMs
                    val speechDuration = vadSilenceStartMs - vadSpeechStartMs

                    if (silenceDuration >= VAD_SILENCE_DURATION_MS && speechDuration >= VAD_SPEECH_MIN_MS) {
                        Log.d(TAG, "VAD auto-stop: silence=${silenceDuration}ms, speech=${speechDuration}ms")
                        onSilenceDetected?.invoke()
                        isRecording = false
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "AudioRecorder"
    }
}
