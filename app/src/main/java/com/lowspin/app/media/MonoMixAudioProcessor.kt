package com.lowspin.app.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Real-time ExoPlayer AudioProcessor that averages multi-channel PCM into equal
 * dual-mono output across all channels when [enabled] is true (single-earphone mode).
 */
@UnstableApi
class MonoMixAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var enabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val supported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        return if (supported && inputAudioFormat.channelCount >= 2) {
            inputAudioFormat
        } else {
            AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val channels = inputAudioFormat.channelCount
        if (!enabled || channels < 2) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val out = replaceOutputBuffer(remaining)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                val frameBytes = channels * 2
                val frames = remaining / frameBytes
                for (f in 0 until frames) {
                    var sum = 0
                    for (c in 0 until channels) {
                        sum += inputBuffer.getShort().toInt()
                    }
                    val mono = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    for (c in 0 until channels) {
                        out.putShort(mono)
                    }
                }
                while (inputBuffer.hasRemaining()) {
                    out.put(inputBuffer.get())
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                val frameBytes = channels * 4
                val frames = remaining / frameBytes
                for (f in 0 until frames) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        sum += inputBuffer.getFloat()
                    }
                    val mono = (sum / channels).coerceIn(-1.0f, 1.0f)
                    for (c in 0 until channels) {
                        out.putFloat(mono)
                    }
                }
                while (inputBuffer.hasRemaining()) {
                    out.put(inputBuffer.get())
                }
            }
            else -> out.put(inputBuffer)
        }
        out.flip()
    }
}
