## HighSpin v2.5.0 - 10 Major Additions

HighSpin v2.5.0 introduces **10 powerful, audiophile-grade features and aesthetic upgrades**, while staying 100% offline with zero network permissions and zero tracking.

---

### ✨ The 10 Major Additions

1. 🔁 **A-B Repeat & Loop Section Tool**:
   - Set pinpoint **Point A** and **Point B** loop timestamps with **±100ms and ±1s** millisecond fine-tuning (`m:ss.S`).
   - Seamless loop boundary clamping inside `PlaybackService` with seek latency throttling.
   - Interactive loop boundaries visualized directly on the seekbar with instant reset and start/end defaults.
2. 🎧 **Headset & Bluetooth Automation**:
   - **Auto-Pause on Disconnect:** Automatically pauses playback when wired headphones or Bluetooth devices disconnect (`ACTION_AUDIO_BECOMING_NOISY`).
   - **Bluetooth Auto-Resume:** Automatically resumes playback upon connecting to saved Bluetooth devices.
   - **Hardware Media Button Controls:** Full multi-click headset hook handling in `SessionCallback` (single tap: play/pause, double tap: next, triple tap: previous).
3. 🎚️ **Volume Normalization & ReplayGain**:
   - Dynamic per-track loudness leveling via `ReplayGainCalculator` and hardware `LoudnessEnhancer`.
   - Prevents sudden jarring volume spikes or whisper-quiet songs when shuffling eclectic audio libraries.
4. 🧠 **Per-Song & Per-Genre Equalizer Memory**:
   - **Dedicated Equalizer Database:** Stores per-track and per-genre custom 5-band EQ, bass boost, virtualizer, and loudness profiles in Room table `song_equalizers` (with `MIGRATION_6_7`).
   - **Clean Global Baseline Isolation:** Applying a per-song EQ temporarily overrides active DSP StateFlows without corrupting your global default settings; transitioning to a standard track automatically restores your global baseline profile.
5. 📑 **Library Tab Customizer & Default Tab Picker**:
   - **Custom Tab Ordering:** Reorder your library tabs (Songs, Albums, Artists, Folders, Playlists) via intuitive up/down controls.
   - **Hide Unused Tabs:** Declutter the navigation bar by hiding tabs you don't use.
   - **Default Launch Tab:** Choose which tab opens first whenever you launch HighSpin.
6. 🎨 **Custom Playlist Cover Art & Gradients**:
   - **Gallery Photo Picker:** Choose any photo from your gallery as a custom playlist cover, safely cloned into private app storage (`filesDir/playlist_covers/`) to prevent Android Scoped Storage permission expiration.
   - **Custom Color Gradients:** Assign custom vibrant gradients per playlist.
   - **Dynamic 4-Song Album Art Mosaic:** Playlists without custom covers automatically render an album art mosaic collage from their first 4 songs.
7. 🏷️ **Audio Tag & Metadata Editor (ID3/FLAC/M4A)**:
   - Offline tag editing sheet for **Title, Artist, Album, Genre, Track Number, and Year**.
   - In-place Room database updates with instant in-memory cache invalidation, updating the Now Playing card and queue in real time.
8. 🌊 **Interactive Waveform Seekbar**:
   - Canvas-rendered vertical audio amplitude energy bars replacing standard flat sliders.
   - Haptic scrub preview with distinct played and unplayed progress coloring and A-B loop marker overlays.
9. 🌌 **Dynamic Fluid Mesh Lyrics Background**:
   - Apple Music-style multi-color fluid radial animated gradient mesh drifting behind synced lyrics.
   - Real-time color extraction from album art with active line spotlight glow and smooth auto-scrolling.
10. 🚗 **Car Mode / Driving Dashboard UI**:
    - High-contrast, distraction-free driving interface in both portrait and landscape orientations.
    - Giant touch targets for Previous, Play/Pause, Next, and ±15s jump buttons, with keep-screen-on and speed/time readouts.

---

### 📦 Checksums & Verification
- **HighSpin-release.apk (2.96 MB)**:
  `3F9E830B67ECB6477B12F020ED239F22986EC192A0144A0A3E52B685F2B1B2CE`
- **HighSpin-debug.apk (20.6 MB)**:
  `C1702306245441107EBC60E0AD0DAB923AECFDA6ADFC12E7DE0D13ED4B2BDEEE`

*100% offline, zero network permissions (`android.permission.INTERNET` stripped), zero analytics, zero ads.*
