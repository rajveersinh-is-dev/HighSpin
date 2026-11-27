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
import org.json.JSONObject

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

/** Swipe gesture actions assignable to Now Playing screen directions. */
enum class SwipeAction(val id: String, val label: String) {
    NONE("none", "Off"),
    DISMISS("dismiss", "Dismiss Player"),
    NEXT("next", "Next Track"),
    PREVIOUS("previous", "Previous Track"),
    SEEK_FORWARD_10("seek_fwd_10", "Seek +10s"),
    SEEK_BACK_10("seek_back_10", "Seek -10s"),
    TOGGLE_LIKE("toggle_like", "Toggle Like"),
    CYCLE_SPEED("cycle_speed", "Cycle Speed"),
    TOGGLE_LYRICS("toggle_lyrics", "Toggle Lyrics"),
    OPEN_QUEUE("open_queue", "Open Queue");

    companion object {
        fun fromId(id: String?): SwipeAction =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

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

    private val _lyricsFontSize = MutableStateFlow(prefs.getFloat(KEY_LYRICS_FONT_SIZE, 21f).coerceIn(16f, 34f))
    val lyricsFontSize: StateFlow<Float> = _lyricsFontSize.asStateFlow()

    private val _lyricsSyncOffset = MutableStateFlow(prefs.getInt(KEY_LYRICS_SYNC_OFFSET, 0))
    val lyricsSyncOffset: StateFlow<Int> = _lyricsSyncOffset.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _crossfadeSec = MutableStateFlow(prefs.getInt(KEY_CROSSFADE_SEC, 0))
    val crossfadeSec: StateFlow<Int> = _crossfadeSec.asStateFlow()

    private fun loadTabsOrder(): List<String> {
        val raw = prefs.getString(KEY_LIBRARY_TABS_ORDER, null) ?: return DEFAULT_LIBRARY_TABS
        val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val valid = DEFAULT_LIBRARY_TABS.toSet()
        val existing = list.filter { it in valid }.distinct()
        val missing = DEFAULT_LIBRARY_TABS.filter { it !in existing }
        return existing + missing
    }

    private fun loadHiddenTabs(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN_LIBRARY_TABS, null) ?: return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private val _volumeNormalization = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false))
    val volumeNormalization: StateFlow<Boolean> = _volumeNormalization.asStateFlow()

    private val _pauseOnHeadsetDisconnect = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_ON_HEADSET_DISCONNECT, true))
    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = _pauseOnHeadsetDisconnect.asStateFlow()

    private val _autoResumeBluetooth = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RESUME_BLUETOOTH, false))
    val autoResumeBluetooth: StateFlow<Boolean> = _autoResumeBluetooth.asStateFlow()

    private val _waveformSeekbar = MutableStateFlow(prefs.getBoolean(KEY_WAVEFORM_SEEKBAR, true))
    val waveformSeekbar: StateFlow<Boolean> = _waveformSeekbar.asStateFlow()

    private val _fluidMeshLyrics = MutableStateFlow(prefs.getBoolean(KEY_FLUID_MESH_LYRICS, true))
    val fluidMeshLyrics: StateFlow<Boolean> = _fluidMeshLyrics.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true))
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _monoAudio = MutableStateFlow(prefs.getBoolean(KEY_MONO_AUDIO, false))
    val monoAudio: StateFlow<Boolean> = _monoAudio.asStateFlow()

    private val _spectrumWallpaper = MutableStateFlow(prefs.getBoolean(KEY_SPECTRUM_WALLPAPER, false))
    val spectrumWallpaper: StateFlow<Boolean> = _spectrumWallpaper.asStateFlow()

    private val _glassMiniPlayer = MutableStateFlow(prefs.getBoolean(KEY_GLASS_MINI_PLAYER, true))
    val glassMiniPlayer: StateFlow<Boolean> = _glassMiniPlayer.asStateFlow()

    private val _swipeLeftAction = MutableStateFlow(SwipeAction.fromId(prefs.getString(KEY_SWIPE_LEFT, SwipeAction.NEXT.id)))
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(SwipeAction.fromId(prefs.getString(KEY_SWIPE_RIGHT, SwipeAction.PREVIOUS.id)))
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    private val _swipeUpAction = MutableStateFlow(SwipeAction.fromId(prefs.getString(KEY_SWIPE_UP, SwipeAction.OPEN_QUEUE.id)))
    val swipeUpAction: StateFlow<SwipeAction> = _swipeUpAction.asStateFlow()

    private val _swipeDownAction = MutableStateFlow(SwipeAction.fromId(prefs.getString(KEY_SWIPE_DOWN, SwipeAction.DISMISS.id)))
    val swipeDownAction: StateFlow<SwipeAction> = _swipeDownAction.asStateFlow()

    private val _libraryTabsOrder = MutableStateFlow(loadTabsOrder())
    val libraryTabsOrder: StateFlow<List<String>> = _libraryTabsOrder.asStateFlow()

    private val _hiddenLibraryTabs = MutableStateFlow(loadHiddenTabs())
    val hiddenLibraryTabs: StateFlow<Set<String>> = _hiddenLibraryTabs.asStateFlow()

    private val _defaultLibraryTab = MutableStateFlow(prefs.getString(KEY_DEFAULT_LIBRARY_TAB, "songs") ?: "songs")
    val defaultLibraryTab: StateFlow<String> = _defaultLibraryTab.asStateFlow()

    private val _pitchSemitones = MutableStateFlow(prefs.getInt(KEY_PITCH_SEMITONES, 0).coerceIn(-6, 6))
    val pitchSemitones: StateFlow<Int> = _pitchSemitones.asStateFlow()

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
            KEY_LYRICS_FONT_SIZE -> _lyricsFontSize.value = prefs.getFloat(KEY_LYRICS_FONT_SIZE, 21f).coerceIn(16f, 34f)
            KEY_LYRICS_SYNC_OFFSET -> _lyricsSyncOffset.value = prefs.getInt(KEY_LYRICS_SYNC_OFFSET, 0)
            KEY_KEEP_SCREEN_ON -> _keepScreenOn.value = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
            KEY_CROSSFADE_SEC -> _crossfadeSec.value = prefs.getInt(KEY_CROSSFADE_SEC, 0)
            KEY_VOLUME_NORMALIZATION -> _volumeNormalization.value = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false)
            KEY_PAUSE_ON_HEADSET_DISCONNECT -> _pauseOnHeadsetDisconnect.value = prefs.getBoolean(KEY_PAUSE_ON_HEADSET_DISCONNECT, true)
            KEY_AUTO_RESUME_BLUETOOTH -> _autoResumeBluetooth.value = prefs.getBoolean(KEY_AUTO_RESUME_BLUETOOTH, false)
            KEY_WAVEFORM_SEEKBAR -> _waveformSeekbar.value = prefs.getBoolean(KEY_WAVEFORM_SEEKBAR, true)
            KEY_FLUID_MESH_LYRICS -> _fluidMeshLyrics.value = prefs.getBoolean(KEY_FLUID_MESH_LYRICS, true)
            KEY_GAPLESS_PLAYBACK -> _gaplessPlayback.value = prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true)
            KEY_MONO_AUDIO -> _monoAudio.value = prefs.getBoolean(KEY_MONO_AUDIO, false)
            KEY_SPECTRUM_WALLPAPER -> _spectrumWallpaper.value = prefs.getBoolean(KEY_SPECTRUM_WALLPAPER, false)
            KEY_GLASS_MINI_PLAYER -> _glassMiniPlayer.value = prefs.getBoolean(KEY_GLASS_MINI_PLAYER, true)
            KEY_SWIPE_LEFT -> _swipeLeftAction.value = SwipeAction.fromId(prefs.getString(KEY_SWIPE_LEFT, SwipeAction.NEXT.id))
            KEY_SWIPE_RIGHT -> _swipeRightAction.value = SwipeAction.fromId(prefs.getString(KEY_SWIPE_RIGHT, SwipeAction.PREVIOUS.id))
            KEY_SWIPE_UP -> _swipeUpAction.value = SwipeAction.fromId(prefs.getString(KEY_SWIPE_UP, SwipeAction.OPEN_QUEUE.id))
            KEY_SWIPE_DOWN -> _swipeDownAction.value = SwipeAction.fromId(prefs.getString(KEY_SWIPE_DOWN, SwipeAction.DISMISS.id))
            KEY_LIBRARY_TABS_ORDER -> _libraryTabsOrder.value = loadTabsOrder()
            KEY_HIDDEN_LIBRARY_TABS -> _hiddenLibraryTabs.value = loadHiddenTabs()
            KEY_DEFAULT_LIBRARY_TAB -> _defaultLibraryTab.value = prefs.getString(KEY_DEFAULT_LIBRARY_TAB, "songs") ?: "songs"
            KEY_PITCH_SEMITONES -> _pitchSemitones.value = prefs.getInt(KEY_PITCH_SEMITONES, 0).coerceIn(-6, 6)
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

    fun setLyricsFontSize(size: Float) = store { putFloat(KEY_LYRICS_FONT_SIZE, size.coerceIn(16f, 34f)) }
        .also { _lyricsFontSize.value = size.coerceIn(16f, 34f) }

    fun setLyricsSyncOffset(offsetMs: Int) = store { putInt(KEY_LYRICS_SYNC_OFFSET, offsetMs.coerceIn(-10000, 10000)) }
        .also { _lyricsSyncOffset.value = offsetMs.coerceIn(-10000, 10000) }

    fun setKeepScreenOn(value: Boolean) = store { putBoolean(KEY_KEEP_SCREEN_ON, value) }
        .also { _keepScreenOn.value = value }

    fun setCrossfadeSec(seconds: Int) = store { putInt(KEY_CROSSFADE_SEC, seconds.coerceIn(0, 12)) }
        .also { _crossfadeSec.value = seconds.coerceIn(0, 12) }

    fun setVolumeNormalization(value: Boolean) = store { putBoolean(KEY_VOLUME_NORMALIZATION, value) }
        .also { _volumeNormalization.value = value }

    fun setPauseOnHeadsetDisconnect(value: Boolean) = store { putBoolean(KEY_PAUSE_ON_HEADSET_DISCONNECT, value) }
        .also { _pauseOnHeadsetDisconnect.value = value }

    fun setAutoResumeBluetooth(value: Boolean) = store { putBoolean(KEY_AUTO_RESUME_BLUETOOTH, value) }
        .also { _autoResumeBluetooth.value = value }

    fun setWaveformSeekbar(value: Boolean) = store { putBoolean(KEY_WAVEFORM_SEEKBAR, value) }
        .also { _waveformSeekbar.value = value }

    fun setFluidMeshLyrics(value: Boolean) = store { putBoolean(KEY_FLUID_MESH_LYRICS, value) }
        .also { _fluidMeshLyrics.value = value }

    fun setGaplessPlayback(value: Boolean) = store { putBoolean(KEY_GAPLESS_PLAYBACK, value) }
        .also { _gaplessPlayback.value = value }

    fun setMonoAudio(value: Boolean) = store { putBoolean(KEY_MONO_AUDIO, value) }
        .also { _monoAudio.value = value }

    fun setSpectrumWallpaper(value: Boolean) = store { putBoolean(KEY_SPECTRUM_WALLPAPER, value) }
        .also { _spectrumWallpaper.value = value }

    fun setGlassMiniPlayer(value: Boolean) = store { putBoolean(KEY_GLASS_MINI_PLAYER, value) }
        .also { _glassMiniPlayer.value = value }

    fun setSwipeLeftAction(action: SwipeAction) = store { putString(KEY_SWIPE_LEFT, action.id) }
        .also { _swipeLeftAction.value = action }

    fun setSwipeRightAction(action: SwipeAction) = store { putString(KEY_SWIPE_RIGHT, action.id) }
        .also { _swipeRightAction.value = action }

    fun setSwipeUpAction(action: SwipeAction) = store { putString(KEY_SWIPE_UP, action.id) }
        .also { _swipeUpAction.value = action }

    fun setSwipeDownAction(action: SwipeAction) = store { putString(KEY_SWIPE_DOWN, action.id) }
        .also { _swipeDownAction.value = action }

    fun setLibraryTabsOrder(order: List<String>) {
        val serialized = order.joinToString(",")
        store { putString(KEY_LIBRARY_TABS_ORDER, serialized) }
        _libraryTabsOrder.value = order
    }

    fun setHiddenLibraryTabs(hidden: Set<String>) {
        val serialized = hidden.joinToString(",")
        store { putString(KEY_HIDDEN_LIBRARY_TABS, serialized) }
        _hiddenLibraryTabs.value = hidden
    }

    fun setDefaultLibraryTab(tab: String) {
        store { putString(KEY_DEFAULT_LIBRARY_TAB, tab) }
        _defaultLibraryTab.value = tab
    }

    fun setPitchSemitones(semitones: Int) {
        val safe = semitones.coerceIn(-6, 6)
        store { putInt(KEY_PITCH_SEMITONES, safe) }
        _pitchSemitones.value = safe
    }

    fun getSongSpeed(songId: Long): Float {
        val jsonStr = prefs.getString(KEY_SONG_SPEED_MAP, null) ?: return 1.0f
        return try {
            val json = JSONObject(jsonStr)
            val key = songId.toString()
            if (json.has(key)) json.getDouble(key).toFloat().coerceIn(0.5f, 2.5f) else 1.0f
        } catch (_: Exception) {
            1.0f
        }
    }

    fun setSongSpeed(songId: Long, speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.5f)
        val jsonStr = prefs.getString(KEY_SONG_SPEED_MAP, null)
        val json = try {
            if (jsonStr != null) JSONObject(jsonStr) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        json.put(songId.toString(), safeSpeed.toDouble())
        store { putString(KEY_SONG_SPEED_MAP, json.toString()) }
    }

    private fun store(edit: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(edit).apply()
    }

    companion object {
        val DEFAULT_LIBRARY_TABS = listOf("songs", "recents", "albums", "artists", "folders", "playlists")

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
        const val KEY_VOLUME_NORMALIZATION = "volume_normalization"
        const val KEY_PAUSE_ON_HEADSET_DISCONNECT = "pause_on_headset_disconnect"
        const val KEY_AUTO_RESUME_BLUETOOTH = "auto_resume_bluetooth"
        const val KEY_WAVEFORM_SEEKBAR = "waveform_seekbar"
        const val KEY_FLUID_MESH_LYRICS = "fluid_mesh_lyrics"
        const val KEY_GAPLESS_PLAYBACK = "gapless_playback"
        const val KEY_MONO_AUDIO = "mono_audio"
        const val KEY_SPECTRUM_WALLPAPER = "spectrum_wallpaper"
        const val KEY_GLASS_MINI_PLAYER = "glass_mini_player"
        const val KEY_SWIPE_LEFT = "swipe_left_action"
        const val KEY_SWIPE_RIGHT = "swipe_right_action"
        const val KEY_SWIPE_UP = "swipe_up_action"
        const val KEY_SWIPE_DOWN = "swipe_down_action"
        const val KEY_LIBRARY_TABS_ORDER = "library_tabs_order"
        const val KEY_HIDDEN_LIBRARY_TABS = "hidden_library_tabs"
        const val KEY_DEFAULT_LIBRARY_TAB = "default_library_tab"
        const val KEY_PITCH_SEMITONES = "pitch_semitones"
        const val KEY_SONG_SPEED_MAP = "song_speed_map"
    }
}
