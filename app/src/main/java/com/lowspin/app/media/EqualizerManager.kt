package com.lowspin.app.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

data class BandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int,
    val currentLevelMb: Int
)

/**
 * Wraps the platform Equalizer, BassBoost, and Virtualizer attached to ExoPlayer's audio session.
 * Fault-tolerant on hardware quirks; failures leave playback untouched.
 */
class EqualizerManager(
    private val settings: EqualizerSettings,
    @Suppress("UNUSED_PARAMETER") context: Context
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    val available: Boolean get() = equalizer != null

    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()

        applyAll()
    }

    fun applyAll() {
        val enabled = settings.enabled.value
        applyEqualizer(enabled, settings.preset.value)
        applyBassBoost(enabled, settings.bassBoost.value)
        applyVirtualizer(enabled, settings.virtualizer.value)
    }

    fun applyEqualizer(enabled: Boolean, presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = enabled
            if (enabled) {
                val numPresets = eq.numberOfPresets.toInt()
                if (numPresets > 0) {
                    val safeIndex = presetIndex.coerceIn(0, numPresets - 1)
                    eq.usePreset(safeIndex.toShort())
                }
                // Apply custom band levels if any are explicitly set
                val custom = settings.customBandLevels.value
                if (custom.isNotEmpty()) {
                    val range = eq.bandLevelRange
                    if (range != null && range.size >= 2) {
                        val min = range[0].toInt()
                        val max = range[1].toInt()
                        val numBands = eq.numberOfBands.toInt()
                        custom.forEach { (band, level) ->
                            if (band in 0 until numBands) {
                                val clamped = level.coerceIn(min, max).toShort()
                                eq.setBandLevel(band.toShort(), clamped)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val eq = equalizer
        val clamped = if (eq != null) {
            val range = eq.bandLevelRange
            val min = range?.get(0)?.toInt() ?: -1500
            val max = range?.get(1)?.toInt() ?: 1500
            val c = levelMb.coerceIn(min, max).toShort()
            runCatching { eq.setBandLevel(band.toShort(), c) }
            c.toInt()
        } else {
            levelMb.coerceIn(-1500, 1500)
        }
        settings.setBandLevel(band, clamped)
    }

    fun applyBassBoost(enabled: Boolean, strength: Int) {
        val bb = bassBoost ?: return
        runCatching {
            bb.enabled = enabled && strength > 0
            if (bb.strengthSupported) {
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    fun applyVirtualizer(enabled: Boolean, strength: Int) {
        val virt = virtualizer ?: return
        runCatching {
            virt.enabled = enabled && strength > 0
            if (virt.strengthSupported) {
                virt.setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    fun getBands(): List<BandInfo> {
        val eq = equalizer
        if (eq == null) {
            val custom = settings.customBandLevels.value
            val presetGains = presetFallbackGains(settings.preset.value)
            val defaultFreqs = listOf(60, 230, 910, 3600, 14000)
            return defaultFreqs.mapIndexed { i, freq ->
                val level = custom[i] ?: presetGains.getOrElse(i) { 0 }
                BandInfo(
                    index = i,
                    centerFreqHz = freq,
                    minLevelMb = -1500,
                    maxLevelMb = 1500,
                    currentLevelMb = level
                )
            }
        }
        return runCatching {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
            val min = range[0].toInt()
            val max = range[1].toInt()
            val custom = settings.customBandLevels.value

            (0 until numBands).map { i ->
                val freq = eq.getCenterFreq(i.toShort()) / 1000 // Convert mHz to Hz
                val current = custom[i] ?: eq.getBandLevel(i.toShort()).toInt()
                BandInfo(
                    index = i,
                    centerFreqHz = freq,
                    minLevelMb = min,
                    maxLevelMb = max,
                    currentLevelMb = current
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        fun presetFallbackGains(presetIndex: Int): List<Int> = when (presetIndex) {
            0 -> listOf(0, 0, 0, 0, 0) // Flat
            1 -> listOf(600, 400, 100, 0, 0) // Bass Boost
            2 -> listOf(-100, 200, 600, 300, -100) // Vocal Boost
            3 -> listOf(400, 200, 100, 300, 300) // Acoustic
            4 -> listOf(500, 300, -100, 200, 500) // Rock
            5 -> listOf(-100, 200, 500, 200, -100) // Pop
            6 -> listOf(400, 200, 100, 200, 400) // Jazz
            7 -> listOf(400, 300, -100, 200, 300) // Classical
            8 -> listOf(600, 400, 0, 200, 300) // Hip Hop
            9 -> listOf(500, 400, 0, 200, 400) // Electronic
            else -> listOf(0, 0, 0, 0, 0)
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
        runCatching { virtualizer?.release() }
        virtualizer = null
    }
}
