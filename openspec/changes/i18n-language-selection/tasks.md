# Tasks: i18n-language-selection

## Phase 1: Foundation / Infrastructure
- [ ] 1.1 Confirm initial language list + locale tags and “system default” label (document in `openspec/changes/i18n-language-selection/tasks.md`).
- [ ] 1.2 Update `app/build.gradle.kts` to add AppCompat dependency for locale APIs.
- [ ] 1.3 Extend `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt` with `getAppLanguageTag()` + `setAppLanguageTag(tag)`.
- [ ] 1.4 Add language state + setter + locale-changed event in `app/src/main/java/com/cvc953/localplayer/viewmodel/MainViewModel.kt`.
- [ ] 1.5 Add base strings in `app/src/main/res/values/strings.xml` for Settings language UI + core screens.
- [ ] 1.6 Create localized resources `app/src/main/res/values-<locale>/strings.xml` for initial language set.

## Phase 2: Core Implementation
- [ ] 2.1 Apply stored locale on startup in `app/src/main/java/com/cvc953/localplayer/MainActivity.kt` before Compose content.
- [ ] 2.2 Observe locale change event in `MainActivity.kt` and call `recreate()` to refresh resources.
- [ ] 2.3 Add language selector UI in `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistsScreen.kt` (Settings section) wired to `MainViewModel.setAppLanguage(...)`.
- [ ] 2.4 Replace hardcoded strings with `stringResource(...)` in:
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistsScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/MusicScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/MusicScreenUpdated.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/ArtistsScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/ArtistDetailScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/ArtistSongsScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/AlbumsScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/AlbumDetailScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistDetailScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/LyricsScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/EqualizerScreen.kt`
  - `app/src/main/java/com/cvc953/localplayer/ui/screens/AboutScreen.kt`

## Phase 3: Integration / Verification Wiring
- [ ] 3.1 Ensure `MainViewModel` loads persisted language on init and updates `AppCompatDelegate.setApplicationLocales` when changed.
- [ ] 3.2 Verify `Settings` selector shows current language and “system default” when preference is null/empty.

## Phase 4: Testing / Validation
- [ ] 4.1 Unit test `AppPrefs` persistence for language tag (set/get/clear) in `app/src/test/.../AppPrefsTest.kt`.
- [ ] 4.2 Manual verification checklist vs spec scenarios: selection persists across restart, system default clears preference, locale applied on launch, locale updates during use.
- [ ] 4.3 Manual UI sweep: confirm all listed screens render strings via resources and fallback works when translation missing.
