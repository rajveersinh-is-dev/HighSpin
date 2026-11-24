# HighSpin 🎵

**HighSpin** is a blazingly fast, modern, 100% offline local music player for Android, built with Jetpack Compose, ExoPlayer (Media3), Room Database, and Kotlin Coroutines.

No accounts, zero network permissions, zero tracking, and zero ads. HighSpin turns your device into an audiophile listening station with manual equalizer DSP, synchronized `.lrc` lyrics, discography exploration, and queue management.

---

## ✨ Features

### 🎧 Precision Audio & Equalizer DSP
- **Hardware-Accelerated Equalizer:** 5-band manual frequency sliders with real-time millibel adjustments.
- **10 Audio Presets:** Flat, Bass Boost, Vocal Boost, Acoustic, Rock, Pop, Jazz, Classical, Hip Hop, and Electronic.
- **BassBoost & 3D Virtualizer:** Dynamic spatial audio and low-end frequency enhancement with smooth slider controls.
- **Playback Speed Controller:** Granular pitch-preserving speed picker ranging from `0.5x` up to `2.5x`.
- **ExoPlayer Media3 Architecture:** Gapless playback, audio focus handling, and automatic ducking during calls/navigation.

### 📝 Synced Lyrics & Tag Parsing
- **External `.lrc` Auto-Detection:** Automatically discovers local `.lrc` files in track directories and device storage.
- **Sub-Second Synced Scrolling:** Auto-scrolls and highlights the active lyric line synchronously as playback advances.
- **Multi-Timestamp & Offset Support:** Supports combined timestamps (e.g., repeated chorus lines `[00:10.00][00:20.00]`) and `[offset: +/-ms]` tags.
- **Embedded ID3 Tag Decoder:** Pure Kotlin binary parser for ID3v2.2, ID3v2.3, and ID3v2.4 `USLT` frames.

### 📚 Expanded 5-Tab Library Browsing
- **Songs:** Complete catalog with instant filtering, sort order, and playing indicator.
- **Playlists:** Custom playlist creation, song reordering, and smart playlist management.
- **Albums (`AlbumDetailScreen`):** Parallax album hero, dominant-color gradient backdrop, release year, total duration, and track-numbered song list.
- **Artists (`ArtistDetailScreen`):** Circular artist avatar hero, discography statistics, shuffle play, and aggregated track discography.
- **Folders (`FolderDetailScreen`):** Hierarchical filesystem navigation, dedicated directory cards, and instant batch-play capabilities.

### 🎛️ Queue Management & Player UI
- **Interactive Queue Sheet:** Reorder upcoming tracks, view live playback state, and swipe or tap to remove songs.
- **Full-Screen Now Playing:** Rotating vinyl glyph animation, blurred dynamic palette gradients, seek bar, repeat modes, and quick sleep timer.
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
│   ├── components/   # SongRow, AlbumArtImage, QueueSheet, SpeedPickerSheet, MiniPlayer
│   ├── screens/      # Library, AlbumDetail, ArtistDetail, FolderDetail, Equalizer, Settings
│   ├── settings/     # Appearance preferences & accessibility styling
│   └── theme/        # Spotify-inspired Material3 dark theme & dynamic accent colors
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

### Build Debug APK
```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🚀 CI/CD

HighSpin includes a GitHub Actions workflow in `.github/workflows/android.yml` that automatically:
1. Checks out code on Ubuntu runners
2. Sets up Temurin JDK 17 with Gradle caching
3. Runs all unit tests via `gradlew testDebugUnitTest`
4. Builds the debug APK via `gradlew assembleDebug`
5. Uploads the resulting APK artifact for every push and PR to `main`

---

## 📄 License

HighSpin is licensed under the MIT License.
