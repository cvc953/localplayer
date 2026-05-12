# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- 🌐 Full localization in Spanish, English, and Italian across the app
- 🗣️ Global language preference management applied from MainActivity and SettingsScreen
- 📂 Storage permission flow and folder selection adapted for i18n
- 🖼️ Updated screenshots for the published English variants
- 📋 Expanded queue operations to add all tracks without duplicates and preserve next-up playback

### Changed
- ⚙️ Refactored MainActivity, SettingsScreen, and several core screens to support i18n
- 🎨 Cleaned up hardcoded strings in albums, artists, playlists, lyrics, equalizer, and detail screens
- 📱 Navigation and UI component adjustments for a consistent experience across languages

### Fixed
- 🐛 Remaining toasts, song counters, and action labels localized
- 🐛 Duplicates prevented when playing the next song in the queue

### Technical
- Implemented `LocaleUtil` and language persistence in `AppPrefs`
- Improved queue logic tests in `PlaybackQueueLogicTest`

## [1.0.7] - 2026-05-03

### Added
- 🇪🇸🇺🇸 Full localization in English and Spanish with metadata and images
- 📲 Updated app icons for English and Spanish
- 🍞 Toast notifications for queue actions in Albums, Artists, Playlists, and MusicScreen
- 👆 Swipe-left action in DraggableSwipeRow to add songs to the end of the queue
- 📂 Improved MusicScreen, PlaylistsScreen, PlaylistDetailScreen, and SettingsScreen
- 📋 Fastlane moved to the repository root for compatibility with F-Droid maintainers

### Changed
- ⚙️ Refactored MusicScreen and PlaylistsScreen with advanced functionality
- 🎨 Improved screen UI with additional features

### Fixed
- 🐛 Dependency metadata inclusion disabled in APK and bundle
- 🐛 Updated toast messages for consistent queue insertion
- 🐛 Optimized progress updates and lyrics position synchronization
- 🐛 Adjusted drag gesture animation duration
- 🐛 Fixed inactive lyric color opacity
- 🐛 Fixed toast message rendering on ArtistDetailScreen
- 🐛 Fixed a bug that caused Japanese lyrics to collapse into each other

### Technical
- 🏗️ Dedicated ViewModels for enhanced screen functionality

## [1.0.5] - 2026-03-25

### Added
- 🎨 Dynamic accent color selection in the settings screen
- 📱 Play Store screenshots for the app (album, library, lyrics, player) in English and Spanish
- 📄 Fastlane metadata for Play Store publishing (EN and ES)

### Changed
- 🎨 Refactored theme handling to support custom accent colors
- 💾 Persisted the selected accent color in preferences

### Fixed
- 🐛 TTML parser treated multiple words separated by `-` as a single word

### Technical
- Openspec documentation for bottom-sheet integration (planning)

## [1.0.4] - 2026-03-23

### Added
- 🎵 TTML lyric support (syllables, word-by-word sync, dot animations for instrumental gaps)
- 🎛️ Built-in equalizer with user preset management and state persistence
- 📂 Music folder management through FolderViewModel
- 🎤 Secondary vocal support in synchronized lyrics
- 🔤 Alphabet scroller in albums, artists, and songs screens
- 🎨 Player and lyric colors based on background luminance
- ⚙️ Dynamic colors toggle in the settings screen
- 🔄 Library auto-scan with debounce to avoid unnecessary rescans
- 💿 Songs loaded grouped by album and artist (AlbumViewModel)
- ▶️ Direct playback by artist (playArtist)
- 🔁 Repeat mode synchronized between PlayerController and PlaybackViewModel
- 🔊 Audio focus handling in the player
- 📋 Persisted playlist sorting preferences
- 📦 Export and import playlists to and from JSON files
- ➕ Create new playlists and add songs from the player dialog
- ⭐ Favorites functionality for songs
- 🔍 Dropdown menus in albums and artists for playback options
- 🎵 Track number and disc number in the song data model

### Changed
- 🧭 Navigation architecture refactored to Navigation Compose
- 🖥️ Player UI completely redesigned
- 📊 Playback logic delegated to PlaybackViewModel for cleaner architecture
- 🎛️ Equalizer refactored into a dedicated EqualizerViewModel
- 🎨 Theme system improved with ExtendedColors and consistent color handling
- ⚙️ Settings screen refactored with equalizer and dynamic color toggles
- 🎯 Song list drag handling improved for better responsiveness
- 📱 Status bar handling refactored in MainActivity

### Fixed
- 🐛 Gap duration calculation in the lyric dot animation
- 🐛 Kotlin compilation errors and UI responsiveness improvements
- 🐛 Aspect ratio handling on the player screen
- 🐛 Lyrics scroll not centering correctly on the current position
- 🐛 Equalizer initialization when audio session changes
- 🐛 Album name normalization for better matching

### Removed
- 🗑️ Unused Firebase Crashlytics and ExoPlayer dependencies
- 🗑️ Commented-out code and unnecessary Spacer components

### Technical
- Dedicated ViewModels: Artist, Equalizer, Folder, Lyrics, Playback, Player, Playlist, Settings
- TTML parser with continuous syllable support and line-join handling
- Library auto-scan in ViewModels with debounce
- Automated changelog extraction in GitHub Actions workflow

## [1.0.0] - 2026-01-25

### Added
- 🎵 Local music playback
- 📝 Synchronized lyrics support (LRC format)
- 📋 Playback queue management with reordering
- 🔍 Search by title and artist
- 🔀 Shuffle mode
- 🔁 Repeat modes (one song, all songs)
- 📊 Audio format and bitrate display
- 🔄 Automatic detection of new songs
- 🎨 Modern Material Design 3 UI
- 📱 Notification and lock-screen controls
- 🎯 Miniplayer for quick control
- 📂 Song sorting (A-Z, Z-A, by artist)
- ℹ️ About screen with app information
- 🔄 Manual library refresh
- 📱 Support for Android 7.0 (API 24)

### Technical
- MVVM architecture
- Jetpack Compose for UI
- Kotlin Coroutines for asynchronous operations
- ContentObserver for library change detection
- JSON cache for fast loading
- MediaSession for system multimedia controls

---

## Changelog Format

### Change Types
- `Added` for new features
- `Changed` for changes in existing functionality
- `Deprecated` for soon-to-be removed features
- `Removed` for removed features
- `Fixed` for bug fixes
- `Security` for vulnerability fixes

[Unreleased]: https://github.com/cvc953/localplayer/compare/v1.0.7...HEAD
[1.0.7]: https://github.com/cvc953/localplayer/compare/v1.0.5...v1.0.7
[1.0.5]: https://github.com/cvc953/localplayer/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/cvc953/localplayer/compare/v1.0.0...v1.0.4
[1.0.0]: https://github.com/cvc953/localplayer/releases/tag/v1.0.0