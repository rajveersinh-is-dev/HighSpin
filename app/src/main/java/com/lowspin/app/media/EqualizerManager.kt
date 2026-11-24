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
                // Apply custom band levels if any are stored
                val custom = settings.customBandLevels.value
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

    fun setBandLevel(band: Int, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            if (range != null && range.size >= 2) {
                val min = range[0].toInt()
                val max = range[1].toInt()
                val clamped = levelMb.coerceIn(min, max).toShort()
                eq.setBandLevel(band.toShort(), clamped)
                settings.setBandLevel(band, clamped.toInt())
            }
        }
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
        val eq = equalizer ?: return emptyList()
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

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
        runCatching { virtualizer?.release() }
        virtualizer = null
    }
}
