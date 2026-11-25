package com.lowspin.app.ui.settings

import android.content.Context
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
    }
}
