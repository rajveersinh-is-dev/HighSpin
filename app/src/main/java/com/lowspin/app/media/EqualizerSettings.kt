package com.lowspin.app.media

import android.content.Context
import com.lowspin.app.data.db.SongEqualizerEntity
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

    private var globalEnabled = prefs.getBoolean(KEY_GLOBAL_ENABLED, prefs.getBoolean(KEY_ENABLED, false))
    private var globalPreset = prefs.getInt(KEY_GLOBAL_PRESET, prefs.getInt(KEY_PRESET, 0))
    private var globalBassBoost = prefs.getInt(KEY_GLOBAL_BASS_BOOST, prefs.getInt(KEY_BASS_BOOST, 0))
    private var globalVirtualizer = prefs.getInt(KEY_GLOBAL_VIRTUALIZER, prefs.getInt(KEY_VIRTUALIZER, 0))
    private var globalLoudnessGain = prefs.getInt(KEY_GLOBAL_LOUDNESS, prefs.getInt(KEY_LOUDNESS, 0))
    private var globalCustomBandLevels = loadBandLevelsFromPref(KEY_GLOBAL_BAND_LEVELS)
        .ifEmpty { loadBandLevels() }

    private var isSongSpecificActive = false

    private val _enabled = MutableStateFlow(globalEnabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _preset = MutableStateFlow(globalPreset)
    val preset: StateFlow<Int> = _preset.asStateFlow()

    private val _bassBoost = MutableStateFlow(globalBassBoost)
    val bassBoost: StateFlow<Int> = _bassBoost.asStateFlow()

    private val _virtualizer = MutableStateFlow(globalVirtualizer)
    val virtualizer: StateFlow<Int> = _virtualizer.asStateFlow()

    private val _loudnessGain = MutableStateFlow(globalLoudnessGain)
    val loudnessGain: StateFlow<Int> = _loudnessGain.asStateFlow()

    private val _customBandLevels = MutableStateFlow(globalCustomBandLevels)
    val customBandLevels: StateFlow<Map<Int, Int>> = _customBandLevels.asStateFlow()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        if (!isSongSpecificActive) {
            globalEnabled = value
            prefs.edit().putBoolean(KEY_GLOBAL_ENABLED, value).apply()
        }
    }

    fun setPreset(value: Int) {
        val safe = if (EqualizerPresets.isEmpty()) 0 else value.coerceIn(0, EqualizerPresets.lastIndex)
        _preset.value = safe
        prefs.edit().putInt(KEY_PRESET, safe).apply()
        clearCustomBandLevels()
        if (!isSongSpecificActive) {
            globalPreset = safe
            prefs.edit().putInt(KEY_GLOBAL_PRESET, safe).apply()
            globalCustomBandLevels = emptyMap()
            prefs.edit().remove(KEY_GLOBAL_BAND_LEVELS).apply()
        }
    }

    fun clearCustomBandLevels() {
        _customBandLevels.value = emptyMap()
        prefs.edit().remove(KEY_BAND_LEVELS).apply()
        if (!isSongSpecificActive) {
            globalCustomBandLevels = emptyMap()
            prefs.edit().remove(KEY_GLOBAL_BAND_LEVELS).apply()
        }
    }

    fun setBassBoost(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _bassBoost.value = safe
        prefs.edit().putInt(KEY_BASS_BOOST, safe).apply()
        if (!isSongSpecificActive) {
            globalBassBoost = safe
            prefs.edit().putInt(KEY_GLOBAL_BASS_BOOST, safe).apply()
        }
    }

    fun setVirtualizer(strength: Int) {
        val safe = strength.coerceIn(0, 1000)
        _virtualizer.value = safe
        prefs.edit().putInt(KEY_VIRTUALIZER, safe).apply()
        if (!isSongSpecificActive) {
            globalVirtualizer = safe
            prefs.edit().putInt(KEY_GLOBAL_VIRTUALIZER, safe).apply()
        }
    }

    fun setLoudnessGain(gainMb: Int) {
        val safe = gainMb.coerceIn(0, 1000)
        _loudnessGain.value = safe
        prefs.edit().putInt(KEY_LOUDNESS, safe).apply()
        if (!isSongSpecificActive) {
            globalLoudnessGain = safe
            prefs.edit().putInt(KEY_GLOBAL_LOUDNESS, safe).apply()
        }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val updated = _customBandLevels.value.toMutableMap()
        updated[band] = levelMb
        _customBandLevels.value = updated
        saveBandLevels(updated)
        if (!isSongSpecificActive) {
            globalCustomBandLevels = updated
            saveBandLevelsToPref(KEY_GLOBAL_BAND_LEVELS, updated)
        }
    }

    fun applySongEqualizer(entity: SongEqualizerEntity) {
        isSongSpecificActive = true
        _enabled.value = true
        _preset.value = entity.presetIndex
        _bassBoost.value = entity.bassBoost
        _virtualizer.value = entity.virtualizer
        _loudnessGain.value = entity.loudnessGain
        val bands = entity.customBands?.let { raw ->
            raw.split(";")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val band = parts[0].toIntOrNull()
                        val level = parts[1].toIntOrNull()
                        if (band != null && level != null) band to level else null
                    } else null
                }
                .toMap()
        } ?: emptyMap()
        _customBandLevels.value = bands
    }

    fun restoreGlobalEqualizer() {
        isSongSpecificActive = false
        _enabled.value = globalEnabled
        _preset.value = globalPreset
        _bassBoost.value = globalBassBoost
        _virtualizer.value = globalVirtualizer
        _loudnessGain.value = globalLoudnessGain
        _customBandLevels.value = globalCustomBandLevels
    }

    fun saveGenreEqualizer(genre: String, presetIndex: Int, bassBoost: Int) {
        val clean = genre.trim().lowercase()
        if (clean.isBlank()) return
        prefs.edit()
            .putInt("genre_eq_preset_$clean", presetIndex)
            .putInt("genre_eq_bass_$clean", bassBoost)
            .apply()
    }

    fun getGenreEqualizer(genre: String): Pair<Int, Int>? {
        val clean = genre.trim().lowercase()
        if (clean.isBlank()) return null
        val presetKey = "genre_eq_preset_$clean"
        if (!prefs.contains(presetKey)) return null
        val preset = prefs.getInt(presetKey, 0)
        val bass = prefs.getInt("genre_eq_bass_$clean", 0)
        return Pair(preset, bass)
    }

    fun applyGenreEqualizer(genre: String): Boolean {
        val eq = getGenreEqualizer(genre) ?: return false
        isSongSpecificActive = true
        _enabled.value = true
        _preset.value = eq.first
        _bassBoost.value = eq.second
        _customBandLevels.value = emptyMap()
        return true
    }

    private fun loadBandLevels(): Map<Int, Int> = loadBandLevelsFromPref(KEY_BAND_LEVELS)

    private fun loadBandLevelsFromPref(key: String): Map<Int, Int> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
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
        saveBandLevelsToPref(KEY_BAND_LEVELS, levels)
    }

    private fun saveBandLevelsToPref(key: String, levels: Map<Int, Int>) {
        val serialized = levels.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString(key, serialized).apply()
    }

    companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_PRESET = "preset"
        const val KEY_BASS_BOOST = "bass_boost"
        const val KEY_VIRTUALIZER = "virtualizer"
        const val KEY_LOUDNESS = "loudness_gain"
        const val KEY_BAND_LEVELS = "band_levels"

        const val KEY_GLOBAL_ENABLED = "global_enabled"
        const val KEY_GLOBAL_PRESET = "global_preset"
        const val KEY_GLOBAL_BASS_BOOST = "global_bass_boost"
        const val KEY_GLOBAL_VIRTUALIZER = "global_virtualizer"
        const val KEY_GLOBAL_LOUDNESS = "global_loudness"
        const val KEY_GLOBAL_BAND_LEVELS = "global_band_levels"
    }
}
