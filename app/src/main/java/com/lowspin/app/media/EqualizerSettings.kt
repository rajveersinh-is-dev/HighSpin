package com.lowspin.app.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val EqualizerPresets = listOf(
    "Flat",
    "Bass Boost",
    "Vocal Boost",
    "Acoustic",
    "Rock",
    "Pop",
    "Jazz",
    "Classical",
    "Hip Hop",
    "Electronic"
)

class EqualizerSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("highspin_equalizer", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _preset = MutableStateFlow(prefs.getInt(KEY_PRESET, 0))
    val preset: StateFlow<Int> = _preset.asStateFlow()

    private val _bassBoost = MutableStateFlow(prefs.getInt(KEY_BASS_BOOST, 0))
    val bassBoost: StateFlow<Int> = _bassBoost.asStateFlow()

    private val _virtualizer = MutableStateFlow(prefs.getInt(KEY_VIRTUALIZER, 0))
    val virtualizer: StateFlow<Int> = _virtualizer.asStateFlow()

    private val _customBandLevels = MutableStateFlow(loadBandLevels())
    val customBandLevels: StateFlow<Map<Int, Int>> = _customBandLevels.asStateFlow()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setPreset(value: Int) {
        val safe = if (EqualizerPresets.isEmpty()) 0 else value.coerceIn(0, EqualizerPresets.lastIndex)
        _preset.value = safe
        prefs.edit().putInt(KEY_PRESET, safe).apply()
        clearCustomBandLevels()
    }

    fun clearCustomBandLevels() {
        _customBandLevels.value = emptyMap()
        prefs.edit().remove(KEY_BAND_LEVELS).apply()
    }

    fun setBassBoost(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _bassBoost.value = safe
        prefs.edit().putInt(KEY_BASS_BOOST, safe).apply()
    }

    fun setVirtualizer(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _virtualizer.value = safe
        prefs.edit().putInt(KEY_VIRTUALIZER, safe).apply()
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val updated = _customBandLevels.value.toMutableMap()
        updated[band] = levelMb
        _customBandLevels.value = updated
        saveBandLevels(updated)
    }

    private fun loadBandLevels(): Map<Int, Int> {
        val raw = prefs.getString(KEY_BAND_LEVELS, null) ?: return emptyMap()
        return raw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val band = parts[0].toIntOrNull()
                    val level = parts[1].toIntOrNull()
                    if (band != null && level != null) band to level else null
                } else null
            }
            .toMap()
    }

    private fun saveBandLevels(levels: Map<Int, Int>) {
        val serialized = levels.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString(KEY_BAND_LEVELS, serialized).apply()
    }

    companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_PRESET = "preset"
        const val KEY_BASS_BOOST = "bass_boost"
        const val KEY_VIRTUALIZER = "virtualizer"
        const val KEY_BAND_LEVELS = "band_levels"
    }
}
