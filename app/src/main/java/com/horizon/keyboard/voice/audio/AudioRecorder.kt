package com.horizon.keyboard.voice.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat

/**
 * Wraps Android [AudioRecord] for PCM 16-bit mono recording at 16kHz.
 *
 * Usage:
 * ```
 * val recorder = AudioRecorder(context)
 * if (recorder.hasPermission()) {
 *     recorder.start()
 *     // ... later ...
 *     val pcmData = recorder.stop()
 * }
 * ```
 */
class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null

    /** Whether the recorder is actively capturing audio. */
    var isRecording: Boolean = false
        private set

    /**
     * Check if RECORD_AUDIO permission is granted.
     * Must be true before calling [start].
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording audio from the microphone.
     * @return true if recording started successfully, false on error.
     */
    fun start(): Boolean {
        if (!hasPermission()) return false

        return try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            audioRecord?.startRecording()
            isRecording = true
            true
        } catch (e: Exception) {
            isRecording = false
            false
        }
    }

    /**
     * Stop recording and return the captured PCM data.
     * @return Raw PCM bytes, or empty ByteArray if nothing was captured.
     */
    fun stop(): ByteArray {
        isRecording = false

        return try {
            audioRecord?.stop()
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val audioData = ByteArray(bufferSize * 10)
            val readCount = audioRecord?.read(audioData, 0, audioData.size) ?: 0
            audioRecord?.release()
            audioRecord = null

            if (readCount > 0) audioData.copyOf(readCount) else ByteArray(0)
        } catch (e: Exception) {
            audioRecord?.release()
            audioRecord = null
            ByteArray(0)
        }
    }

    /**
     * Force-stop and release resources. Safe to call multiple times.
     */
    fun release() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
