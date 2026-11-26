package com.lowspin.app.data.scanner

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AudioAnalysisResult(
    val bpm: Int,
    val musicalKey: String
)

/**
 * Pure on-device DSP audio analyzer using MediaExtractor + MediaCodec PCM decoding,
 * onset-envelope autocorrelation for BPM detection, and Goertzel chromagram +
 * Krumhansl-Schmuckler tonal profile correlation for musical key estimation.
 */
object AudioAnalyzer {

    private const val MIN_BPM = 60
    private const val MAX_BPM = 185
    private const val ENVELOPE_RATE_HZ = 100
    private const val MAX_DECODE_SECONDS = 12

    private val KEY_NAMES = arrayOf(
        "C", "C\u266F", "D", "E\u266D", "E", "F",
        "F\u266F", "G", "A\u266D", "A", "B\u266D", "B"
    )

    // Krumhansl & Kessler (1982) major & minor tonal hierarchy probe profiles
    private val MAJOR_PROFILE = floatArrayOf(
        6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f,
        2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f
    )
    private val MINOR_PROFILE = floatArrayOf(
        6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f,
        2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f
    )

    fun analyzeUri(context: Context, uri: Uri, durationMs: Long): AudioAnalysisResult? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = fmt
                    break
                }
            }
            if (audioTrack < 0 || format == null) return null

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
            var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)

            extractor.selectTrack(audioTrack)
            if (durationMs > 25_000L) {
                extractor.seekTo(durationMs * 250L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val targetSamples = sampleRate * MAX_DECODE_SECONDS
            val monoSamples = FloatArray(targetSamples)
            var sampleWritePos = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var iterations = 0

            while (!outputDone && sampleWritePos < targetSamples && iterations < 1200) {
                iterations++
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val size = extractor.readSampleData(inBuf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 5_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(8000)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 8)
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val frameCount = shortBuf.remaining() / channelCount
                            var f = 0
                            while (f < frameCount && sampleWritePos < targetSamples) {
                                var sum = 0f
                                for (ch in 0 until channelCount) {
                                    sum += shortBuf.get().toFloat() / 32768f
                                }
                                monoSamples[sampleWritePos++] = sum / channelCount
                                f++
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (sampleWritePos < sampleRate) return null
            val trimmed = if (sampleWritePos == monoSamples.size) monoSamples else monoSamples.copyOf(sampleWritePos)
            val bpm = detectBpm(trimmed, sampleRate)
            val key = detectKey(trimmed, sampleRate)
            AudioAnalysisResult(bpm = bpm, musicalKey = key)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * Estimates tempo in BPM [60..185] from mono PCM samples using half-wave
     * rectified RMS onset flux and weighted comb autocorrelation.
     */
    fun detectBpm(samples: FloatArray, sampleRate: Int): Int {
        if (sampleRate <= 0 || samples.size < sampleRate / 2) return 0
        val hop = (sampleRate / ENVELOPE_RATE_HZ).coerceAtLeast(1)
        val frameCount = samples.size / hop
        if (frameCount < ENVELOPE_RATE_HZ * 2) return 0

        val energy = FloatArray(frameCount)
        var maxEnergy = 0f
        for (f in 0 until frameCount) {
            val start = f * hop
            val end = minOf(start + hop, samples.size)
            var sumSq = 0f
            for (i in start until end) {
                val s = samples[i]
                sumSq += s * s
            }
            val rms = sqrt(sumSq / (end - start).coerceAtLeast(1))
            energy[f] = rms
            if (rms > maxEnergy) maxEnergy = rms
        }
        if (maxEnergy < 1e-5f) return 0

        // Half-wave rectified onset novelty minus local mean
        val onset = FloatArray(frameCount)
        val smoothWindow = 8
        for (f in 1 until frameCount) {
            val diff = (energy[f] - energy[f - 1]).coerceAtLeast(0f)
            var localSum = 0f
            var count = 0
            val wStart = (f - smoothWindow).coerceAtLeast(0)
            val wEnd = (f + smoothWindow).coerceAtMost(frameCount - 1)
            for (k in wStart..wEnd) {
                localSum += energy[k]
                count++
            }
            val avg = if (count > 0) localSum / count else 0f
            onset[f] = (diff - avg * 0.2f).coerceAtLeast(0f)
        }

        val minLag = (ENVELOPE_RATE_HZ * 60) / MAX_BPM
        val maxLag = (ENVELOPE_RATE_HZ * 60) / MIN_BPM
        if (maxLag >= frameCount) return 0

        var bestLag = minLag
        var bestScore = -1.0

        for (lag in minLag..maxLag) {
            var corr = 0.0
            val limit = frameCount - lag
            for (t in 0 until limit) {
                corr += onset[t] * onset[t + lag]
            }
            // Add harmonic support at 2*lag to reduce octave ambiguity
            val doubleLag = lag * 2
            if (doubleLag < frameCount) {
                var harmCorr = 0.0
                val hLimit = frameCount - doubleLag
                for (t in 0 until hLimit) {
                    harmCorr += onset[t] * onset[t + doubleLag]
                }
                corr += 0.45 * harmCorr
            }
            val bpm = (60.0 * ENVELOPE_RATE_HZ) / lag
            // Gentle log-tempo weighting centered around 115 BPM
            val logDist = ln(bpm / 115.0)
            val prior = exp(-0.5 * (logDist / 0.55).pow(2.0))
            val score = (corr / limit.coerceAtLeast(1)) * prior
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (bestScore <= 0.0) return 0
        return ((60.0 * ENVELOPE_RATE_HZ) / bestLag).roundToInt().coerceIn(MIN_BPM, MAX_BPM)
    }

    /**
     * Estimates musical key & mode (e.g. "C Maj", "A Min", "E♭ Maj") by computing
     * a 12-pitch-class Goertzel chromagram across octaves 2..5 and correlating against
     * the Krumhansl-Schmuckler tonal profiles.
     */
    fun detectKey(samples: FloatArray, sampleRate: Int): String {
        if (sampleRate <= 0 || samples.size < 2048) return ""

        // Downsample via simple decimation toward ~11025 Hz so Goertzel stays fast & crisp
        val decimate = (sampleRate / 11025).coerceAtLeast(1)
        val effRate = sampleRate.toDouble() / decimate
        val workSize = minOf(samples.size / decimate, 11025 * 10)
        if (workSize < 2048) return ""

        val windowSize = 2048
        val hop = 2048
        val chroma = FloatArray(12)
        val window = FloatArray(windowSize)
        var maxSample = 0f

        // Precompute Hann window
        val hann = FloatArray(windowSize) { i ->
            (0.5 * (1.0 - cos(2.0 * PI * i / (windowSize - 1)))).toFloat()
        }

        // Precompute Goertzel coefficients for MIDI notes 36 (C2) .. 83 (B5)
        val coeffs = ArrayList<Pair<Int, Double>>(48)
        for (midi in 36..83) {
            val freq = 440.0 * 2.0.pow((midi - 69) / 12.0)
            if (freq < effRate * 0.45) {
                val omega = 2.0 * PI * freq / effRate
                coeffs.add((midi % 12) to (2.0 * cos(omega)))
            }
        }

        var pos = 0
        var blockCount = 0
        while (pos + windowSize <= samples.size / decimate && blockCount < 48) {
            for (i in 0 until windowSize) {
                val s = samples[(pos + i) * decimate]
                val a = abs(s)
                if (a > maxSample) maxSample = a
                window[i] = s * hann[i]
            }
            for ((pc, coeff) in coeffs) {
                var q0: Double
                var q1 = 0.0
                var q2 = 0.0
                for (i in 0 until windowSize) {
                    q0 = coeff * q1 - q2 + window[i]
                    q2 = q1
                    q1 = q0
                }
                val magSq = (q1 * q1 + q2 * q2 - q1 * q2 * coeff).coerceAtLeast(0.0)
                chroma[pc] += sqrt(magSq).toFloat()
            }
            pos += hop
            blockCount++
        }

        if (maxSample < 1e-5f) return ""

        var chromaMean = 0f
        for (v in chroma) chromaMean += v
        chromaMean /= 12f
        if (chromaMean <= 1e-6f) return ""

        var bestKey = ""
        var bestCorr = Float.NEGATIVE_INFINITY

        for (tonic in 0 until 12) {
            val majorCorr = pearsonCorrelation(chroma, tonic, MAJOR_PROFILE)
            if (majorCorr > bestCorr) {
                bestCorr = majorCorr
                bestKey = "${KEY_NAMES[tonic]} Maj"
            }
            val minorCorr = pearsonCorrelation(chroma, tonic, MINOR_PROFILE)
            if (minorCorr > bestCorr) {
                bestCorr = minorCorr
                bestKey = "${KEY_NAMES[tonic]} Min"
            }
        }
        return bestKey
    }

    private fun pearsonCorrelation(chroma: FloatArray, shift: Int, profile: FloatArray): Float {
        var meanC = 0f
        var meanP = 0f
        for (i in 0 until 12) {
            meanC += chroma[(i + shift) % 12]
            meanP += profile[i]
        }
        meanC /= 12f
        meanP /= 12f

        var num = 0f
        var denC = 0f
        var denP = 0f
        for (i in 0 until 12) {
            val dc = chroma[(i + shift) % 12] - meanC
            val dp = profile[i] - meanP
            num += dc * dp
            denC += dc * dc
            denP += dp * dp
        }
        val denom = sqrt(denC * denP)
        return if (denom > 1e-8f) num / denom else 0f
    }
}
