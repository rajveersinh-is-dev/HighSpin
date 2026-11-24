# HighSpin Architectural Overhaul & Engineering Report

## Executive Summary
HighSpin represents a complete structural overhaul of the offline music player Lowspin. While the original codebase provided a basic Spotify-like prototype, it contained latent architectural bugs, thread safety issues, incomplete features from its roadmap, and zero automated tests.

This overhaul transforms the project into **HighSpin**: an offline Android music player built on clean architecture, Room persistence, ExoPlayer (Media3), and 100% Jetpack Compose with Material 3.

---

## 1. Architectural Flaws Identified & Root-Cause Remediations

### 1.1 ID3v2 Embedded Lyrics Header Offset Defect
- **Location:** `com.lowspin.app.data.lyrics.EmbeddedId3LyricsSource.kt`
- **Root Cause:** When parsing ID3v2 tags from an audio file input stream, the parser previously read and validated the 10-byte ID3 header into `header: ByteArray(10)`, extracted `tagSize`, and then read the tag payload into `tag = ByteArray(tagSize)`. In `parseFrames(tag, version)`, the loop initialized `var position = HEADER_SIZE (10)`. Because `tag` only held the payload (the 10-byte header was already stripped), this skipped the first 10 bytes of actual frame identifiers in every file, corrupting frame headers and causing `USLT` lyrics frame detection to fail silently for almost all valid ID3 tags.
- **Fix:** Initialized `var position = 0` when iterating across `tag` frames, ensuring all frames (including the initial frame) are parsed accurately.

### 1.2 Main-Thread Room Database Blocking during Service Termination
- **Location:** `com.lowspin.app.media.PlaybackService.kt`
- **Root Cause:** In `onDestroy()`, the service invoked `runBlocking { saveProgress() }` directly on Android's main looper. Under Android Room runtime enforcement, database queries on the main thread throw an `IllegalStateException`. Because this was wrapped in `runCatching`, the exception was silently caught and swallowed, but playback progress, last played track, and queue positions were never actually persisted when the app was swiped away or closed.
- **Fix:** Refactored state saving to execute via `runBlocking(Dispatchers.IO) { saveProgress() }`, ensuring safe thread switching and reliable persistence of playback state before process teardown.

### 1.3 MediaStore Audio Scanner Double-Wildcard SQL Error
- **Location:** `com.lowspin.app.data.scanner.MediaStoreScanner.kt`
- **Root Cause:** In the MediaStore query construction, the lossless format extension array contained elements like `"%.flac"`, `"%.wav"`. The query builder interpolated these with `"$DISPLAY_NAME LIKE '%$ext'"`, resulting in `LIKE '%%.flac'`. While SQLite tolerates double percents in some cases, it introduced query parsing ambiguities and excluded valid files across OEM MediaProvider content providers.
- **Fix:** Cleaned the extension definitions to `"flac"`, `"wav"`, etc., and parameterized query construction to ensure standard wildcard matching.

### 1.4 Equalizer Index Modulo Exception & Feature Absence
- **Location:** `com.lowspin.app.media.EqualizerManager.kt` & `EqualizerSettings.kt`
- **Root Cause:** The original equalizer manager was a stub with only preset indices. If a negative index was encountered, `presetIndex % count` produced a negative number passed to Android's `AudioEffect` framework, throwing runtime exceptions. Furthermore, there were no manual band-level controls, no BassBoost effect, and no 3D Virtualizer effect.
- **Fix:** Implemented full manual 5-band gain adjustment (-1500 mB to +1500 mB), preset selection with bounded clamping, Android `BassBoost` audio effect integration (0 to 1000 strength), and `Virtualizer` audio effect integration (0 to 1000 strength).

---

## 2. New Features & Capabilities

### 2.1 Multi-Tier Lyrics Pipeline (`CompositeLyricsSource` & `LrcParser`)
- **External `.lrc` Parser:** HighSpin inspects the local directory of the currently playing track and candidate storage directories for matching `.lrc` files.
- **Timestamp Parsing Engine:** Supports `[mm:ss.xx]`, `[mm:ss.xxx]`, multiple timestamps per line (e.g. repeated chorus lines), and `[offset: +/-ms]` headers.
- **Synchronized Compose Rendering:** The `LyricsCard` component tracks real-time player position updates and smoothly scrolls to keep the active singing line centered and highlighted with accent coloring.
- **Fallback Hierarchy:** Checks external `.lrc` file first; if absent, falls back to embedded ID3 `USLT` frames; if neither exists, presents clean placeholder state.

### 2.2 Complete 5-Tab Library Navigation
Expanded `LibraryScreen` from a simple 2-tab view into a comprehensive 5-tab browser:
1. **Songs:** Complete catalog with sort order, instant search, and playing track indicator.
2. **Playlists:** Custom playlist management with song addition/removal and reordering.
3. **Albums (`AlbumDetailScreen`):** Displays album art with dynamic seed gradient, track list with sequential track numbers, year, and aggregate runtime.
4. **Artists (`ArtistDetailScreen`):** Circular artist hero, track count, total listening duration, and complete artist track list.
5. **Folders (`FolderDetailScreen`):** Filesystem-level browsing for organized local music libraries.

### 2.3 Interactive Queue Sheet & Playback Speed Picker
- **QueueSheet:** Bottom sheet allowing users to view the current playing track, inspect the upcoming track queue, reorder upcoming items, and swipe/tap to remove tracks from the queue.
- **SpeedPickerSheet:** Pitch-preserving playback speed selector supporting `0.5x`, `0.75x`, `0.9x`, `1.0x`, `1.1x`, `1.25x`, `1.5x`, `1.75x`, `2.0x`, and `2.5x`.

### 2.4 Settings, Storage & Privacy Enhancements
- **Live Cache Metering:** Calculates exact disk space consumed by album art images and enables one-tap clearing.
- **Library Rescan:** Explicit rescan action directly available in Settings with progress feedback.
- **Zero-Network Guarantee:** Confirms absence of `android.permission.INTERNET`, ensuring zero telemetry or data leakage.

---

## 3. Verification & Testing

### 3.1 Automated Unit Tests
- `LrcParserTest`: Verifies timestamp calculations, sorting, multi-timestamp duplication, offset handling, and un-synced text fallback.
- `TrackFilterTest`: Verifies exclusion of call recordings, WhatsApp voice notes, screen recordings, while preserving legitimate music tracks.
- `SmartShuffleTest`: Verifies repeat avoidance on track loop, repeat-one mode respect, and single-item queue boundaries.
- `FormattersTest`: Verifies duration string formatting across zero, minute, and multi-hour boundaries.
- `EqualizerPresetsTest`: Verifies preset array integrity, bounds safety, and BassBoost/Virtualizer clamping.

### 3.2 CI/CD Automation
Added `.github/workflows/android.yml` to automatically run unit tests and build the debug APK on every push and pull request to `main`.
