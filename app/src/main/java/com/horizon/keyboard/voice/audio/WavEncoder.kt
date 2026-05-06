package com.horizon.keyboard.voice.audio

/**
 * Optimized WAV encoder — zero-copy header injection.
 *
 * Optimization: single allocation (44 + pcmData.size) instead of
 * header allocation + concatenation copy. Saves one full buffer copy
 * on every transcription (~960KB for 30s recording).
 */
object WavEncoder {

    /**
     * Encode raw PCM to WAV. Single allocation, zero concatenation.
     *
     * @param pcmData Raw PCM audio bytes.
     * @param sampleRate Sample rate in Hz (e.g. 16000).
     * @param channels Number of channels (1 = mono).
     * @param bitsPerSample Bits per sample (e.g. 16).
     * @return Complete WAV file bytes.
     */
    fun encode(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val totalSize = 44 + dataSize

        // Single allocation — header + data together
        val wav = ByteArray(totalSize)

        // RIFF header
        wav[0] = 'R'.code.toByte()
        wav[1] = 'I'.code.toByte()
        wav[2] = 'F'.code.toByte()
        wav[3] = 'F'.code.toByte()
        writeIntLE(wav, 4, totalSize - 8)
        wav[8] = 'W'.code.toByte()
        wav[9] = 'A'.code.toByte()
        wav[10] = 'V'.code.toByte()
        wav[11] = 'E'.code.toByte()

        // fmt sub-chunk
        wav[12] = 'f'.code.toByte()
        wav[13] = 'm'.code.toByte()
        wav[14] = 't'.code.toByte()
        wav[15] = ' '.code.toByte()
        writeIntLE(wav, 16, 16)
        writeShortLE(wav, 20, 1) // PCM
        writeShortLE(wav, 22, channels)
        writeIntLE(wav, 24, sampleRate)
        writeIntLE(wav, 28, byteRate)
        writeShortLE(wav, 32, blockAlign)
        writeShortLE(wav, 34, bitsPerSample)

        // data sub-chunk
        wav[36] = 'd'.code.toByte()
        wav[37] = 'a'.code.toByte()
        wav[38] = 't'.code.toByte()
        wav[39] = 'a'.code.toByte()
        writeIntLE(wav, 40, dataSize)

        // Copy PCM data directly into pre-allocated buffer (no concatenation)
        System.arraycopy(pcmData, 0, wav, 44, dataSize)

        return wav
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
}
