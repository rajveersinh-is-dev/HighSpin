# HighSpin 🎵

**HighSpin** is a blazingly fast, modern, ultra-lightweight, 100% offline local music player for Android, built with Jetpack Compose, ExoPlayer (Media3), Room Database, and Kotlin Coroutines.

No accounts, zero network permissions, zero tracking, and zero ads. HighSpin turns your device into an audiophile listening station with manual equalizer DSP, synchronized `.lrc` lyrics with click-to-seek, turntable vinyl animations, animated real-time audio visualizers, and customizable aesthetic themes.

---

## 📦 Download Latest APK

Grab the ready-to-install APK directly from GitHub Releases:

- **[Download HighSpin Release APK (v2.3.6)](https://github.com/rajveersinh-is-dev/HighSpin/releases/latest)** (~2.88 MB - Ultra lightweight, R8 minified & resource-shrunk)
- **[Download HighSpin Debug APK (v2.3.6)](https://github.com/rajveersinh-is-dev/HighSpin/releases/latest)** (~20 MB - Full debug runtime & symbols)

---

## ✨ Features

### 🔔 Android 15/16 Live Notifications & OnePlus OxygenOS 15 Live Alerts
- **OnePlus OxygenOS 15 / ColorOS 15 Live Alerts & Fluid Cloud:** Full support for punch-hole capsule, status bar live chip, and lock screen focus mode with native `lyricInfo` and `oplus_capsule` protocol integration.
- **Enforced Ongoing Flags:** Eliminates `FLAG_ONGOING_EVENT` stripping by `DefaultMediaNotificationProvider`, ensuring Android 15 & OxygenOS recognize HighSpin as an active live activity while cleanly releasing ongoing status when playback ends or is stopped.
- **Refreshed High-Priority Silent Media Channel:** Upgraded to `IMPORTANCE_DEFAULT` with disabled sound/vibrations, allowing status-bar live chips and heads-up capsules without noisy notification chimes.
- **Promoted Ongoing Notifications:** Leverages Android 15 & 16 Live Updates API (`POST_PROMOTED_NOTIFICATIONS`) for prioritized lock screen and notification tray placement.
- **Live Seek & Chronometer Synchronization:** Restores `android.showWhen` to enable native chronometer ticking on status-bar capsules and lock screen cards while actively playing, stopping immediately upon pause to prevent chronometer drift.
- **Background Battery Exemption Helper:** Declares `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with safe fallback to allow users to whitelist HighSpin from aggressive OEM battery killer daemons.
- **Clean Transport Controls:** High-resolution embedded album art with `Notification.EXTRA_LARGE_ICON` parcelable Icon fallback, previous/pause/next controls, and safe IPC bitmap downsampling.

### 🎨 Clean & Minimal Aesthetic Modern UI
- **12 Aesthetic Color Themes:** Choose between 12 breathtaking colorways: *HighSpin Violet*, *Cyberpunk Neon*, *Emerald Groove*, *Sunset Horizon*, *Electric Ocean*, *Sakura Synth*, *Golden Pulse*, *Midnight Aurora*, *Synthwave 84*, *Nordic Frost*, plus newly added *Neon Tokyo* (Electric Yellow & Neon Cyan) and *Velvet Twilight* (Warm Coral & Lavender Twilight).
- **5 Animated Audio Visualizer Modes:** High-framerate Compose Canvas visualizer with: *Dynamic Equalizer Bars with Peak Caps*, *Glowing Neon Waves*, *Minimalist Pulse Dots*, *Liquid Aura / Harmonic Flow*, and the new *Orbital Pulsar / Concentric Frequency Halo*.
- **Enhanced Photorealistic Turntable Vinyl:** Interactive 33.3 RPM turntable with dynamic album art center label, brushed chrome spindle reflections, concentric physical micro-grooves, specular sheen reflections, golden run-out groove ring, moving tonearm stylus with real-time micro-tracking flutter, and ambient now-playing aura.
- **Aesthetic Mini Player:** Modern 14dp rounded floating pill with spinning vinyl playback glyph, smooth marquee text, and dual-tone gradient progress indicator.
- **Refined Library Header:** Sleek minimalist branding with spinning vinyl glyph, track counter badge, instant search, and direct equalizer access.
- **Pure Window Theme:** Zero redundant platform action bars or decor titles across every device and orientation.
- **Pure Black (AMOLED) Mode:** True `#000000` pitch-black backgrounds for OLED battery savings and maximum contrast.

### ⚡ Ultra-Lightweight & Fast
- **Sub-3MB Footprint:** Aggressive R8 tree-shaking, dead code elimination, and resource shrinking reduced APK size by 86% (from 21.4 MB down to 2.9 MB).
- **Fast Startup & Low Memory Usage:** Memory-efficient bitmap downsampling, bounded LRU caches, and zero heavy third-party framework dependencies.

### 🎧 Precision Audio DSP, Speed & Pitch Shifting
- **Hardware-Accelerated Equalizer:** 5-band manual frequency sliders with real-time millibel adjustments.
- **10 Audio Presets:** Flat, Bass Boost, Vocal Boost, Acoustic, Rock, Pop, Jazz, Classical, Hip Hop, and Electronic.
- **BassBoost & 3D Virtualizer:** Dynamic spatial audio and low-end frequency enhancement with smooth slider controls.
- **Independent Speed & Pitch Control:** Granular playback tempo slider (`0.5x` - `2.5x`) and independent pitch shifting (`0.5x` - `1.8x`).
- **DJ DSP Presets:** Instant one-tap presets for *Nightcore* (+15% tempo & pitch), *Slowed & Reverb* (0.85x tempo & pitch), *Sped Up*, and *Deep Pitch*.
- **ExoPlayer Media3 Architecture:** Gapless playback, audio focus handling, and automatic ducking during calls/navigation.

### 📝 Synced Lyrics with Click-to-Seek
- **Click-to-Seek Karaoke:** Tapping any lyric line instantly seeks the song to that exact timestamp with sub-second accuracy.
- **Active Line Glowing Highlight:** Bold, neon-accented karaoke line with timestamp tags and smooth auto-scrolling.
- **Embedded & External Lyrics:** Discovers local `.lrc` files and decodes embedded ID3v2 `USLT` frames.

### 🔍 Technical Audio Specs & File Sharing
- **Song Info Inspector:** Detailed audio specifications bottom sheet displaying format container badge (FLAC, MP3, AAC, WAV, OGG, OPUS), bitrate (kbps), sample rate (kHz), file size (MB), track/disc number, date added, and storage path.
- **One-Tap File Sharing:** Seamlessly share audio files via standard Android sharing sheet to other apps, chats, or devices without requiring cloud uploads.

### 📚 Expanded 5-Tab Library Browsing
- **Songs:** Complete catalog with instant filtering, sort order, and playing indicator.
- **Playlists:** Custom playlist creation, song reordering, and smart playlist management.
- **Albums (`AlbumDetailScreen`):** Parallax album hero, dominant-color gradient backdrop, release year, total duration, and track-numbered song list.
- **Artists (`ArtistDetailScreen`):** Circular artist avatar hero, discography statistics, shuffle play, and aggregated track discography.
- **Folders (`FolderDetailScreen`):** Hierarchical filesystem navigation, dedicated directory cards, and instant batch-play capabilities.

### 🎛️ Queue Management & Player UI
- **Interactive Queue Sheet:** Reorder upcoming tracks, view live playback state, and swipe or tap to remove songs.
- **Sleep Timer:** Sleep after minutes (`5`, `10`, `15`, `30`, `45`, `60`) or gracefully stop at the end of the current track.
- **Mini Player:** Persistent, dismissable compact player bar with play/pause and like controls.

### 🔒 100% Offline Guarantee & Privacy
- **Zero Internet Permissions:** `android.permission.INTERNET` is **NOT** declared in `AndroidManifest.xml`.
- **Zero Analytics / Telemetry:** No crash analytics, telemetry, or remote telemetry SDKs.
- **Storage Management:** Live album art cache size calculation with one-tap cache clear and complete library rescan.

---

## 🏗️ Architecture & Tech Stack

```text
com.lowspin.app/
├── data/
│   ├── art/          # Offline album art pipeline (memory cache -> disk cache -> MediaStore)
│   ├── db/           # Room Database, DAOs, Entities, and Queries
│   ├── lyrics/       # Composite lyrics engine (external .lrc parser + embedded ID3 decoder)
│   ├── repository/   # MusicRepository, PlaylistRepository
│   └── scanner/      # MediaStore audio scanner & recorder exclusion filter
├── di/               # Lightweight manual Dependency Injection container (AppContainer)
├── media/            # ExoPlayer Media3 PlaybackService, EqualizerManager, SmartShuffle
├── ui/
│   ├── components/   # AudioVisualizer, TurntableVinyl, SongInfoSheet, SongRow, QueueSheet, MiniPlayer
│   ├── screens/      # Library, AlbumDetail, ArtistDetail, FolderDetail, Equalizer, Settings
│   ├── settings/     # Appearance preferences & 10 aesthetic theme presets
│   └── theme/        # Material3 dynamic dark & AMOLED pure black themes
└── util/             # Millisecond formatting & time utilities
```

- **UI Framework:** 100% Jetpack Compose with Material 3
- **Audio Engine:** `androidx.media3:media3-exoplayer:1.4.1`, `media3-session`
- **Database:** `androidx.room:room-ktx:2.6.1` with KSP compiler
- **Image Palette:** `androidx.palette:palette-ktx:1.0.0`
- **Language & Runtime:** Kotlin 2.0.21, Coroutines 1.9.0, Java 17

---

## 🧪 Verification & Testing

HighSpin includes a suite of JUnit 4 and Kotlin unit tests verifying all algorithmic invariants:

- **`AudioModPresetsTest`:** Verifies tempo and pitch DSP parameter ranges, available speeds integrity, and theme palette validity.
- **`LrcParserTest`:** Verifies standard LRC parsing, multi-timestamp line duplication, positive/negative offset shifts, millisecond fraction lengths, and un-synced plaintext fallbacks.
- **`TrackFilterTest`:** Verifies exclusion of OEM call recordings, WhatsApp/Telegram audio notes, screencasts, and preserves legitimate music titles.
- **`SmartShuffleTest`:** Verifies repeat prevention across playback loops, queue boundary safety, and repeat-one mode respect.
- **`FormattersTest`:** Verifies duration strings (`mm:ss`, `hh:mm:ss`, total album runtime).
- **`EqualizerPresetsTest`:** Verifies preset array integrity, preset index bounds clamping, and BassBoost/Virtualizer decibel clamping.

Run the test suite locally:
```bash
./gradlew testDebugUnitTest
```

---

## 🔨 Building from Source

### Prerequisites
- JDK 17 (e.g. OpenJDK 17)
- Android SDK with `build-tools;34.0.0` and `platforms;android-34`
- Set `ANDROID_HOME` and `JAVA_HOME` environment variables

### Build Release APK (Lightweight ~2.9MB)
```bash
# Windows
gradlew.bat assembleRelease

# Linux / macOS
./gradlew assembleRelease
```
The compiled APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

### Build Debug APK
```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🚀 CI/CD & Automated Releases

HighSpin includes a GitHub Actions workflow in `.github/workflows/android.yml` that automatically:
1. Checks out code on Ubuntu runners
2. Sets up Temurin JDK 17 with Gradle caching
3. Runs all unit tests via `gradlew testDebugUnitTest`
4. Builds both release and debug APKs via `gradlew assembleRelease assembleDebug`
5. Uploads APK artifacts for every push and PR to `main`
6. Automatically publishes a GitHub Release with attached APK binaries and SHA256 checksums whenever a tag `v*` is pushed

---

## 📄 License

HighSpin is licensed under the MIT License.
