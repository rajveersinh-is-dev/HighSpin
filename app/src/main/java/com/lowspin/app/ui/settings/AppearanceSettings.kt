package com.lowspin.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The three calm violet shades kept for backwards compatibility. */
val LowspinAccentShades = listOf(
    Color(0xFFA78BFA),
    Color(0xFFB39DFF),
    Color(0xFF8E7BEA)
)

data class ThemePreset(
    val id: String,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val background: Color
)

val HighSpinThemes = listOf(
    ThemePreset(
        id = "violet",
        name = "HighSpin Violet",
        primary = Color(0xFFA78BFA),
        secondary = Color(0xFF7C5CD6),
        surface = Color(0xFF1A1625),
        background = Color(0xFF0E0B14)
    ),
    ThemePreset(
        id = "cyberpunk",
        name = "Cyberpunk Neon",
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFFFF007F),
        surface = Color(0xFF101926),
        background = Color(0xFF060B12)
    ),
    ThemePreset(
        id = "emerald",
        name = "Emerald Groove",
        primary = Color(0xFF00E676),
        secondary = Color(0xFF00B0FF),
        surface = Color(0xFF0F2216),
        background = Color(0xFF06130B)
    ),
    ThemePreset(
        id = "sunset",
        name = "Sunset Horizon",
        primary = Color(0xFFFF7043),
        secondary = Color(0xFFFFB74D),
        surface = Color(0xFF241512),
        background = Color(0xFF130907)
    ),
    ThemePreset(
        id = "ocean",
        name = "Electric Ocean",
        primary = Color(0xFF00B0FF),
        secondary = Color(0xFF2979FF),
        surface = Color(0xFF0E1A2C),
        background = Color(0xFF060D17)
    ),
    ThemePreset(
        id = "sakura",
        name = "Sakura Synth",
        primary = Color(0xFFFF4081),
        secondary = Color(0xFFEA80FC),
        surface = Color(0xFF22111D),
        background = Color(0xFF12060F)
    ),
    ThemePreset(
        id = "amber",
        name = "Golden Pulse",
        primary = Color(0xFFFFD54F),
        secondary = Color(0xFFFFB300),
        surface = Color(0xFF221B0F),
        background = Color(0xFF130E07)
    ),
    ThemePreset(
        id = "aurora",
        name = "Midnight Aurora",
        primary = Color(0xFF00F5D4),
        secondary = Color(0xFF7B2CBF),
        surface = Color(0xFF0D1B2A),
        background = Color(0xFF060B12)
    ),
    ThemePreset(
        id = "synthwave",
        name = "Synthwave 84",
        primary = Color(0xFFFF2A85),
        secondary = Color(0xFF05D9E8),
        surface = Color(0xFF1E152A),
        background = Color(0xFF100A1A)
    ),
    ThemePreset(
        id = "frost",
        name = "Nordic Frost",
        primary = Color(0xFF64D2FF),
        secondary = Color(0xFF80E5FF),
        surface = Color(0xFF121B26),
        background = Color(0xFF080E16)
    ),
    ThemePreset(
        id = "tokyo",
        name = "Neon Tokyo",
        primary = Color(0xFFFFEA00),
        secondary = Color(0xFF00F0FF),
        surface = Color(0xFF14141F),
        background = Color(0xFF0A0A10)
    ),
    ThemePreset(
        id = "velvet",
        name = "Velvet Twilight",
        primary = Color(0xFFFF5E7E),
        secondary = Color(0xFFA55EEA),
        surface = Color(0xFF1F1424),
        background = Color(0xFF0F0A12)
    ),
    ThemePreset(
        id = "amoled",
        name = "AMOLED Pitch Black",
        primary = Color(0xFFA78BFA),
        secondary = Color(0xFF7C5CD6),
        surface = Color(0xFF000000),
        background = Color(0xFF000000)
    ),
    ThemePreset(
        id = "noir",
        name = "Cherry Blossom Noir",
        primary = Color(0xFFFF4081),
        secondary = Color(0xFFEA80FC),
        surface = Color(0xFF0D0609),
        background = Color(0xFF000000)
    ),
    ThemePreset(
        id = "platinum",
        name = "Cyber Platinum Minimal",
        primary = Color(0xFFE2E8F0),
        secondary = Color(0xFF94A3B8),
        surface = Color(0xFF0A0A0C),
        background = Color(0xFF000000)
    )
)

/** Plain values provided once at the root so list rows never collect state. */
data class LowspinListStyle(
    val rowHeight: Dp = 64.dp,
    val showArt: Boolean = true,
    val largeTouchTargets: Boolean = false
)

val LocalAppearance = staticCompositionLocalOf<AppearanceSettings> {
    error("AppearanceSettings has not been provided")
}

val LocalListStyle = staticCompositionLocalOf { LowspinListStyle() }

/**
 * Appearance and accessibility preferences. Stored locally, no permissions.
 */
class AppearanceSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("highspin_appearance", Context.MODE_PRIVATE)

    private val _themeIndex = MutableStateFlow(prefs.getInt(KEY_THEME, 0).coerceIn(0, HighSpinThemes.lastIndex))
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    private val _accentIndex = MutableStateFlow(prefs.getInt(KEY_ACCENT, 0))
    val accentIndex: StateFlow<Int> = _accentIndex.asStateFlow()

    private val _pureBlack = MutableStateFlow(prefs.getBoolean(KEY_PURE_BLACK, false))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _dynamicPalette = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_PALETTE, true))
    val dynamicPalette: StateFlow<Boolean> = _dynamicPalette.asStateFlow()

    private val _turntableMode = MutableStateFlow(prefs.getBoolean(KEY_TURNTABLE, false))
    val turntableMode: StateFlow<Boolean> = _turntableMode.asStateFlow()

    private val _visualizerEnabled = MutableStateFlow(prefs.getBoolean(KEY_VISUALIZER, true))
    val visualizerEnabled: StateFlow<Boolean> = _visualizerEnabled.asStateFlow()

    private val _visualizerStyle = MutableStateFlow(prefs.getInt(KEY_VISUALIZER_STYLE, 0).coerceIn(0, 4))
    val visualizerStyle: StateFlow<Int> = _visualizerStyle.asStateFlow()

    private val _textScale = MutableStateFlow(prefs.getFloat(KEY_TEXT_SCALE, 1f))
    val textScale: StateFlow<Float> = _textScale.asStateFlow()

    private val _compact = MutableStateFlow(prefs.getBoolean(KEY_COMPACT, false))
    val compact: StateFlow<Boolean> = _compact.asStateFlow()

    private val _showArt = MutableStateFlow(prefs.getBoolean(KEY_ART, true))
    val showArt: StateFlow<Boolean> = _showArt.asStateFlow()

    private val _reduceMotion = MutableStateFlow(prefs.getBoolean(KEY_MOTION, false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _highContrast = MutableStateFlow(prefs.getBoolean(KEY_CONTRAST, false))
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _largeTargets = MutableStateFlow(prefs.getBoolean(KEY_TARGETS, false))
    val largeTargets: StateFlow<Boolean> = _largeTargets.asStateFlow()

    private val _liveNotifications = MutableStateFlow(prefs.getBoolean(KEY_LIVE_NOTIFICATIONS, true))
    val liveNotifications: StateFlow<Boolean> = _liveNotifications.asStateFlow()

    private val _shakeToSkip = MutableStateFlow(prefs.getBoolean(KEY_SHAKE_TO_SKIP, false))
    val shakeToSkip: StateFlow<Boolean> = _shakeToSkip.asStateFlow()

    private val _flipToPause = MutableStateFlow(prefs.getBoolean(KEY_FLIP_TO_PAUSE, false))
    val flipToPause: StateFlow<Boolean> = _flipToPause.asStateFlow()

    private val _ambientGlow = MutableStateFlow(prefs.getBoolean(KEY_AMBIENT_GLOW, true))
    val ambientGlow: StateFlow<Boolean> = _ambientGlow.asStateFlow()

    private val _lyricsFontSize = MutableStateFlow(prefs.getFloat(KEY_LYRICS_FONT_SIZE, 16f))
    val lyricsFontSize: StateFlow<Float> = _lyricsFontSize.asStateFlow()

    private val _lyricsSyncOffset = MutableStateFlow(prefs.getInt(KEY_LYRICS_SYNC_OFFSET, 0))
    val lyricsSyncOffset: StateFlow<Int> = _lyricsSyncOffset.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _crossfadeSec = MutableStateFlow(prefs.getInt(KEY_CROSSFADE_SEC, 0))
    val crossfadeSec: StateFlow<Int> = _crossfadeSec.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_THEME -> _themeIndex.value = prefs.getInt(KEY_THEME, 0).coerceIn(0, HighSpinThemes.lastIndex)
            KEY_ACCENT -> _accentIndex.value = prefs.getInt(KEY_ACCENT, 0)
            KEY_PURE_BLACK -> _pureBlack.value = prefs.getBoolean(KEY_PURE_BLACK, false)
            KEY_DYNAMIC_PALETTE -> _dynamicPalette.value = prefs.getBoolean(KEY_DYNAMIC_PALETTE, true)
            KEY_TURNTABLE -> _turntableMode.value = prefs.getBoolean(KEY_TURNTABLE, false)
            KEY_VISUALIZER -> _visualizerEnabled.value = prefs.getBoolean(KEY_VISUALIZER, true)
            KEY_VISUALIZER_STYLE -> _visualizerStyle.value = prefs.getInt(KEY_VISUALIZER_STYLE, 0).coerceIn(0, 4)
            KEY_TEXT_SCALE -> _textScale.value = prefs.getFloat(KEY_TEXT_SCALE, 1f)
            KEY_COMPACT -> _compact.value = prefs.getBoolean(KEY_COMPACT, false)
            KEY_ART -> _showArt.value = prefs.getBoolean(KEY_ART, true)
            KEY_MOTION -> _reduceMotion.value = prefs.getBoolean(KEY_MOTION, false)
            KEY_CONTRAST -> _highContrast.value = prefs.getBoolean(KEY_CONTRAST, false)
            KEY_TARGETS -> _largeTargets.value = prefs.getBoolean(KEY_TARGETS, false)
            KEY_LIVE_NOTIFICATIONS -> _liveNotifications.value = prefs.getBoolean(KEY_LIVE_NOTIFICATIONS, true)
            KEY_SHAKE_TO_SKIP -> _shakeToSkip.value = prefs.getBoolean(KEY_SHAKE_TO_SKIP, false)
            KEY_FLIP_TO_PAUSE -> _flipToPause.value = prefs.getBoolean(KEY_FLIP_TO_PAUSE, false)
            KEY_AMBIENT_GLOW -> _ambientGlow.value = prefs.getBoolean(KEY_AMBIENT_GLOW, true)
            KEY_LYRICS_FONT_SIZE -> _lyricsFontSize.value = prefs.getFloat(KEY_LYRICS_FONT_SIZE, 16f)
            KEY_LYRICS_SYNC_OFFSET -> _lyricsSyncOffset.value = prefs.getInt(KEY_LYRICS_SYNC_OFFSET, 0)
            KEY_KEEP_SCREEN_ON -> _keepScreenOn.value = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
            KEY_CROSSFADE_SEC -> _crossfadeSec.value = prefs.getInt(KEY_CROSSFADE_SEC, 0)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setTheme(index: Int) {
        val safe = index.coerceIn(0, HighSpinThemes.lastIndex)
        store { putInt(KEY_THEME, safe) }
        _themeIndex.value = safe
    }

    fun setAccent(index: Int) = store { putInt(KEY_ACCENT, index.coerceIn(0, 2)) }
        .also { _accentIndex.value = index.coerceIn(0, 2) }

    fun setPureBlack(enabled: Boolean) = store { putBoolean(KEY_PURE_BLACK, enabled) }
        .also { _pureBlack.value = enabled }

    fun setDynamicPalette(enabled: Boolean) = store { putBoolean(KEY_DYNAMIC_PALETTE, enabled) }
        .also { _dynamicPalette.value = enabled }

    fun setTurntableMode(enabled: Boolean) = store { putBoolean(KEY_TURNTABLE, enabled) }
        .also { _turntableMode.value = enabled }

    fun setVisualizerEnabled(enabled: Boolean) = store { putBoolean(KEY_VISUALIZER, enabled) }
        .also { _visualizerEnabled.value = enabled }

    fun setVisualizerStyle(style: Int) {
        val safe = style.coerceIn(0, 4)
        store { putInt(KEY_VISUALIZER_STYLE, safe) }
        _visualizerStyle.value = safe
    }

    fun setTextScale(scale: Float) = store { putFloat(KEY_TEXT_SCALE, scale) }
        .also { _textScale.value = scale }

    fun setCompact(value: Boolean) = store { putBoolean(KEY_COMPACT, value) }
        .also { _compact.value = value }

    fun setShowArt(value: Boolean) = store { putBoolean(KEY_ART, value) }
        .also { _showArt.value = value }

    fun setReduceMotion(value: Boolean) = store { putBoolean(KEY_MOTION, value) }
        .also { _reduceMotion.value = value }

    fun setHighContrast(value: Boolean) = store { putBoolean(KEY_CONTRAST, value) }
        .also { _highContrast.value = value }

    fun setLargeTargets(value: Boolean) = store { putBoolean(KEY_TARGETS, value) }
        .also { _largeTargets.value = value }

    fun setLiveNotifications(value: Boolean) = store { putBoolean(KEY_LIVE_NOTIFICATIONS, value) }
        .also { _liveNotifications.value = value }

    fun setShakeToSkip(value: Boolean) = store { putBoolean(KEY_SHAKE_TO_SKIP, value) }
        .also { _shakeToSkip.value = value }

    fun setFlipToPause(value: Boolean) = store { putBoolean(KEY_FLIP_TO_PAUSE, value) }
        .also { _flipToPause.value = value }

    fun setAmbientGlow(value: Boolean) = store { putBoolean(KEY_AMBIENT_GLOW, value) }
        .also { _ambientGlow.value = value }

    fun setLyricsFontSize(size: Float) = store { putFloat(KEY_LYRICS_FONT_SIZE, size.coerceIn(12f, 26f)) }
        .also { _lyricsFontSize.value = size.coerceIn(12f, 26f) }

    fun setLyricsSyncOffset(offsetMs: Int) = store { putInt(KEY_LYRICS_SYNC_OFFSET, offsetMs.coerceIn(-10000, 10000)) }
        .also { _lyricsSyncOffset.value = offsetMs.coerceIn(-10000, 10000) }

    fun setKeepScreenOn(value: Boolean) = store { putBoolean(KEY_KEEP_SCREEN_ON, value) }
        .also { _keepScreenOn.value = value }

    fun setCrossfadeSec(seconds: Int) = store { putInt(KEY_CROSSFADE_SEC, seconds.coerceIn(0, 12)) }
        .also { _crossfadeSec.value = seconds.coerceIn(0, 12) }

    private fun store(edit: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(edit).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_preset"
        const val KEY_ACCENT = "accent"
        const val KEY_PURE_BLACK = "pure_black"
        const val KEY_DYNAMIC_PALETTE = "dynamic_palette"
        const val KEY_TURNTABLE = "turntable_mode"
        const val KEY_VISUALIZER = "visualizer_enabled"
        const val KEY_VISUALIZER_STYLE = "visualizer_style"
        const val KEY_TEXT_SCALE = "text_scale"
        const val KEY_COMPACT = "compact"
        const val KEY_ART = "show_art"
        const val KEY_MOTION = "reduce_motion"
        const val KEY_CONTRAST = "high_contrast"
        const val KEY_TARGETS = "large_targets"
        const val KEY_LIVE_NOTIFICATIONS = "live_notifications"
        const val KEY_SHAKE_TO_SKIP = "shake_to_skip"
        const val KEY_FLIP_TO_PAUSE = "flip_to_pause"
        const val KEY_AMBIENT_GLOW = "ambient_glow"
        const val KEY_LYRICS_FONT_SIZE = "lyrics_font_size"
        const val KEY_LYRICS_SYNC_OFFSET = "lyrics_sync_offset"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_CROSSFADE_SEC = "crossfade_sec"
    }
}
