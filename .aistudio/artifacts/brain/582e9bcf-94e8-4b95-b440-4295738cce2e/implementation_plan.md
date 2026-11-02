# PixelPlayer for Android 10 & Older Devices

A high-fidelity, Material You-inspired offline and local audio player engineered for Android 10 (API 29) through Android 15+. PixelPlayer reproduces the iconic Pixel aesthetic—including the animated wavy scrubber, dynamic berry/dark slate themes, synced karaoke-style lyrics, robust MediaStore discovery with legacy storage fallbacks, and a versatile Playlists engine supporting individual files as playlists.

## User Review & Critical Decisions

> [!IMPORTANT]
> Key architectural decisions tailored to your requirements:
> - **Android 10 Compatibility & Zero-Crash Guarantee**: Full backward compatibility for API 29+ by requesting `READ_EXTERNAL_STORAGE` for Android 10–12 alongside `READ_MEDIA_AUDIO` for Android 13+, applying `requestLegacyExternalStorage="true"`, using safe MediaStore column lookups to prevent `CursorWindow` / `IllegalStateException` crashes, and adding defensive foreground service lifecycle guards.
> - **Individual Files as Playlists**: A dedicated "Use File as Playlist" feature in the Playlists section allowing users to pick individual audio tracks (`.mp3`, `.flac`, `.wav`, `.m4a`) or playlist files (`.m3u`, `.m3u8`) to create instant single-track loop playlists or imported playlists with full playback controls.
> - **Pixel Design & Polish**: Faithful implementation of the screens shown in your screenshots—"Your Mix" Home with expressive pill shapes and floating art circles, Library with tab navigation (Songs, Albums, Artists, Playlists), Lyrics viewer with Synced and Static modes, and the Now Playing screen featuring the signature wavy progress bar.

---

## 1. Overview & Core Concept

- **What It Does**: PixelPlayer is a lightweight, audio player crafted in Jetpack Compose and Kotlin. It scans and indexes local music with full tag metadata, provides queuing and shuffle/repeat modes, presents real-time synced and static lyrics, and offers custom playlist management with direct individual file integration.
- **Target Audience / Devices**: Audiophiles and music listeners on older Android devices (such as Android 10 / API 29) as well as modern flagship devices who want the Google Pixel audio experience without crashes, memory bloat, or background service drops.
- **Key Value**: Rock-solid stability on legacy storage systems without relying on high-API-only features, paired with high-polish Material 3 design and flexible playlist creation from raw device files.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Home Screen ("Your Mix")**:
   - Giant bold typography ("Your Mix - Today's Mix for you") with top quick-action buttons (Queue & Settings).
   - Dynamic hero collage showcasing curved oval and circular album artwork arrangements (matching Screenshot 1).
   - Prominent floating circular play button for instant random or daily mix playback.
   - Persistent floating mini-player with current track info, responsive play/pause, next track button, and bottom navigation pill.

2. **Now Playing Screen & Wavy Scrubber**:
   - Large rounded album artwork card with smooth corner radius.
   - Song title and artist with top bar controls (minimize arrow, lyrics shortcut, queue shortcut).
   - Signature **wavy/squiggly progress bar** that dynamically animates during active playback, accompanied by elapsed and total duration timestamps.
   - Primary control dock: Large pill-shaped Play/Pause toggle, Previous/Next skip controls, Shuffle toggle, Repeat toggle, and Favorite heart button (matching Screenshot 2).

3. **Lyrics Screen (Synced & Static)**:
   - Dedicated header with back navigation and pill selector between **Synced** and **Static** lyrics.
   - Synced karaoke view with smooth vertical auto-scroll, dimming inactive verses and highlighting the current timestamp line in bold white text (matching Screenshot 3).
   - Static view for reading full song lyrics in readable typography.
   - Floating compact pause button and mini wavy progress indicator at the bottom.

4. **Library & Playlist Management ("Files as Playlists")**:
   - Tab row: **SONGS**, **ALBUMS**, **ARTISTS**, and **PLAYLISTS**.
   - Songs tab: Quick shuffle button, sort filter pill, and clean list items with album art, title, artist, and 3-dot overflow menu (matching Screenshot 4).
   - Playlists tab: Includes an "Add Individual File as Playlist" action launcher enabling users to select any audio file from storage to generate an isolated, single-track looping playlist or import `.m3u` playlists directly.

### Visual Identity & Theme Tokens

- **Vibe**: Material You / Android 14 expressive audio styling with organic rounded corners (`CircleShape` and `RoundedCornerShape(24.dp)`).
- **Color Palette** (as extracted from user screenshots):
  - Primary Background: Deep berry maroon (`#2E1825`) transitioning to rich dark navy (`#0B1320`) on Library.
  - Container Surfaces: Velvet plum (`#3D2434`) and slate surfaces (`#1B2638`).
  - Active Accents: Pastel rose (`#F5C6D6`), soft mint cream (`#D2E7D6`), and soft periwinkle (`#B9CEEB`).
  - Text: High-contrast soft white (`#FFFFFF`) and secondary lilac grey (`#C7B4C0`).
- **Motion & Polish**: Smooth spring transitions when expanding the Now Playing sheet, sinusoidal canvas math for the wavy scrubber, and fluid text highlight transitions in lyrics.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Android 10 (API 29) Storage & Permissions
- **Decision**: Dual-permission strategy + `android:requestLegacyExternalStorage="true"`.
- **Why**: Android 10 introduced Scoped Storage changes that caused major crashes in earlier music players querying raw file paths or requiring `READ_EXTERNAL_STORAGE`. On Android 13+, `READ_MEDIA_AUDIO` is used. PixelPlayer detects runtime SDK level and requests the appropriate permission while querying MediaStore with fallback content URIs (`ContentUris.withAppendedId`).

### 2. Audio Engine & Service Resilience
- **Decision**: Android `MediaPlayer` wrapped in a resilient Foreground Service with AudioFocus management, MediaSession support, and graceful error recovery.
- **Why**: Avoids heavy native binaries or ExoPlayer version mismatches that trigger `ForegroundServiceDidNotStartInTimeException` on Android 10. Memory consumption is minimized for low-RAM older devices.

### 3. Individual File Playlist Implementation
- **Decision**: Persist playlists and playlist items in Room Database with a `PlaylistType` flag (`STANDARD`, `SINGLE_FILE_PLAYLIST`, `IMPORTED_M3U`).
- **Why**: Allows users to turn any file on storage into a named playlist or quick loop playlist without duplicating files, preserving storage on older phones.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│   ┌──────────────┐   ┌──────────────┐   ┌───────────────┐   │
│   │  Home (Mix)  │   │   Library    │   │  Now Playing  │   │
│   └───────┬──────┘   └──────┬───────┘   └───────┬───────┘   │
│           │                 │                   │           │
│           └────────────► ViewModel ◄────────────┘           │
│                             │                               │
│                   StateFlow / AudioState                    │
└─────────────────────────────┼───────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 PixelPlayerService & Engine                 │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  MediaPlayer Engine (AudioFocus, Looping, Scrubber) │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  MediaStore Scanner & File-to-Playlist Importer     │   │
│   └─────────────────────────────────────────────────────┘   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  Room DB: Playlists, Favorite Tracks, Local Cache   │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Component Breakdown

1. **`data/`**:
   - `model/Track.kt`: ID, title, artist, album, duration, uri, path, albumArtUri.
   - `model/Playlist.kt`: ID, name, trackCount, type (`SINGLE_FILE`, `CUSTOM`, `M3U`), fileUri.
   - `db/AppDatabase.kt`, `TrackDao.kt`, `PlaylistDao.kt`: Room persistence for favorites, history, and playlists.
   - `repository/MusicRepository.kt`: Safe MediaStore querying with legacy Android 10 column safety, SAF picker integration, and built-in demo tracks fallback.
2. **`player/`**:
   - `PixelAudioPlayer.kt`: MediaPlayer controller with state flow (`isPlaying`, `currentTrack`, `currentPosition`, `duration`, `shuffle`, `repeatMode`).
   - `lyrics/LyricsParser.kt`: Fast LRC parser extracting synced lines with millisecond timestamps and fallback static text.
3. **`ui/`**:
   - `components/WavySlider.kt`: Dynamic custom canvas wavy scrubber with sinusoidal curve math matching Pixel UI.
   - `screens/HomeScreen.kt`: "Your Mix" with floating organic collage and primary mix launcher.
   - `screens/LibraryScreen.kt`: Songs, Albums, Artists, and Playlists (with "Pick File as Playlist" button).
   - `screens/NowPlayingScreen.kt`: Full-bleed player with controls and wavy scrubber.
   - `screens/LyricsScreen.kt`: Synced / Static lyrics viewer.
   - `components/MiniPlayer.kt`: Compact persistent dock.
