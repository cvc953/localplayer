## Exploration: i18n-language-selection

### Current State
- UI stack is Jetpack Compose + Material3 (per `openspec/config.yaml` and `MainActivity`/`Theme.kt`).
- Strings are hardcoded in Compose UI (e.g., `MusicScreen.kt`, `PlaylistsScreen.kt`), with only a minimal `strings.xml` containing `app_name`. There are no `stringResource(...)` usages or `R.string` references found.
- No runtime locale handling found (`AppCompatDelegate.setApplicationLocales`, `LocaleListCompat`, or `Configuration` changes are not used).
- Settings live in two places:
  - `Settings` model + `SettingsController`/`SettingsViewModel` (simple in-memory flow, currently unused for UI settings).
  - `AppPrefs` (SharedPreferences) used by `MainViewModel` for theme, dynamic color, auto-scan, etc. Settings UI (`SettingsScreen`) uses `MainViewModel` and `AppPrefs`-backed flows.
- Settings UI is currently implemented inside `PlaylistsScreen.kt` as `SettingsScreen` and shown as an overlay in `MusicScreenUpdated.kt` via `PlayerViewModel.isSettingsVisible`.

### Affected Areas
- `app/src/main/res/values/strings.xml` — currently only contains `app_name`; would need localization resources to support multiple languages.
- `app/src/main/java/com/cvc953/localplayer/ui/screens/*` — extensive hardcoded Spanish strings in Compose UI; these should be migrated to string resources for i18n.
- `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistsScreen.kt` — contains `SettingsScreen` UI and all settings labels; likely where language selector UI would live.
- `app/src/main/java/com/cvc953/localplayer/viewmodel/MainViewModel.kt` — central place for settings flows backed by `AppPrefs`; probable place to add language preference state.
- `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt` — shared preferences store for settings; likely place to persist selected language.
- `app/src/main/java/com/cvc953/localplayer/MainActivity.kt` — top-level Compose entry; potential place to apply locale change at runtime.
- `app/build.gradle.kts` — dependencies; currently no explicit AppCompat locale APIs, but can be added if needed.

### Approaches
1. **AppCompat Locale APIs (recommended for runtime language switching)** — Use `AppCompatDelegate.setApplicationLocales(LocaleListCompat)` to apply app-specific locale at runtime, and store selected language in `AppPrefs`. Update Compose UI to use `stringResource(...)` and add `values-xx/strings.xml` resources.
   - Pros: Official AndroidX path for per-app language; runtime changes without app restart (depending on API and Activity recreation); consistent with Android 13 per-app language settings.
   - Cons: Requires adding AppCompat dependency and wiring locale changes in `MainActivity` (or Application); still must migrate UI strings to resources.
   - Effort: Medium (string migration is the bulk of work).

2. **Manual Configuration Update + Activity recreate** — Store language in prefs and update `Resources`/`Configuration` in `MainActivity` before `setContent`, then recreate Activity on change.
   - Pros: No new dependency; more control over timing.
   - Cons: Easy to get wrong; can conflict with system locale handling; more boilerplate and edge cases.
   - Effort: Medium/High (manual locale plumbing + lifecycle concerns).

### Recommendation
Go with **Approach 1 (AppCompat Locale APIs)**. It’s the supported path for per-app language selection and integrates better with modern Android settings. Main work is migrating UI strings from hardcoded Spanish to `strings.xml` and introducing a language selector in `SettingsScreen` backed by `AppPrefs` + `MainViewModel` state.

### Risks
- **Large string migration**: many screens use hardcoded strings; missing a string will leave mixed-language UI.
- **Runtime update behavior**: depending on Compose/Activity setup, locale changes may require Activity recreation to fully apply.
- **Settings fragmentation**: two settings mechanisms exist (`SettingsController` vs `AppPrefs`); language should align with the `AppPrefs`/`MainViewModel` path to avoid split state.

### Ready for Proposal
Yes — but confirm target languages and expected UX (system default option vs explicit language list) before spec/design.
