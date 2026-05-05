package com.horizon.keyboard.voice.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat

/**
 * Wraps Android [AudioRecord] for PCM 16-bit mono recording at 16kHz.
 *
 * Records audio into a buffer while [isRecording] is true.
 * Call [stop] to get the captured PCM data.
 */
class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_RECORD_SECONDS = 30
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val recordedData = mutableListOf<Byte>()

    /** Whether the recorder is actively capturing audio. */
    var isRecording: Boolean = false
        private set

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording audio from the microphone.
     * Spawns a background thread that continuously reads PCM data into a buffer.
     *
     * @return true if recording started successfully, false on error.
     */
    fun start(): Boolean {
        if (!hasPermission()) return false

        return try {
            recordedData.clear()

            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                .coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            // Read audio data in background thread
            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                val maxBytes = SAMPLE_RATE * 2 * MAX_RECORD_SECONDS // 16-bit = 2 bytes per sample

                while (isRecording && recordedData.size < maxBytes) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        synchronized(recordedData) {
                            for (i in 0 until read) {
                                recordedData.add(buffer[i])
                            }
                        }
                    } else if (read < 0) {
                        Log.e("AudioRecorder", "AudioRecord read error: $read")
                        break
                    }
                }
            }, "AudioRecorder-Thread").also { it.start() }

            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            isRecording = false
            audioRecord?.release()
            audioRecord = null
            false
        }
    }

    /**
     * Stop recording and return the captured PCM data.
     * @return Raw PCM bytes, or empty ByteArray if nothing was captured.
     */
    fun stop(): ByteArray {
        isRecording = false

        // Wait for recording thread to finish
        try {
            recordingThread?.join(2000)
        } catch (_: Exception) {}
        recordingThread = null

        // Stop and release AudioRecord
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Error stopping AudioRecord", e)
        }
        audioRecord = null

        // Return recorded data
        return synchronized(recordedData) {
            if (recordedData.isNotEmpty()) recordedData.toByteArray() else ByteArray(0)
        }
    }

    /**
     * Force-stop and release resources. Safe to call multiple times.
     */
    fun release() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("AudioRecorder", "release failed", e)
        }
        audioRecord = null
    }
}
