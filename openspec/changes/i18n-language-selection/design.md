# Design: i18n-language-selection

## Technical Approach

Implement per-app language by persisting a locale tag in `AppPrefs`, exposing it as a `StateFlow` in `MainViewModel`, and applying it via `AppCompatDelegate.setApplicationLocales`. All user-facing strings in Compose screens move to `strings.xml` and localized `values-xx/` resources, consumed via `stringResource(...)`.

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Use AppCompat per-app locale APIs (`AppCompatDelegate.setApplicationLocales`) | Requires AppCompat dependency; may need activity recreate on change | **Chosen**. Aligns with proposal and supports per-app language on API 24+.
| Store language in `AppPrefs` and expose via `MainViewModel` | Tightens coupling to existing prefs/viewmodel patterns | **Chosen**. Matches existing preference management for theme/scan and keeps a single source of truth.
| Apply locale change by recreating `MainActivity` on selection | Small UX flicker; simplest consistent resource reload | **Chosen**. Ensures `stringResource` resolves new `Resources` across API levels.

## Data Flow

Language selection (runtime change):

```
SettingsScreen ──(select language)──→ MainViewModel ──→ AppPrefs
      │                                   │
      └────────────(update)───────────────┘
                           │
                           ├─> AppCompatDelegate.setApplicationLocales
                           └─> Activity.recreate() (via MainActivity observer)
```

Startup flow:

```
MainActivity.onCreate
   └─> MainViewModel loads AppPrefs language
        └─> AppCompatDelegate.setApplicationLocales (before Compose content)
             └─> stringResource(...) resolves localized strings
```

### Sequence Diagram (Locale change)

```
User -> SettingsScreen: Select language
SettingsScreen -> MainViewModel: setLanguage(tag|system)
MainViewModel -> AppPrefs: persist tag
MainViewModel -> AppCompatDelegate: setApplicationLocales
MainViewModel -> MainActivity: emit localeChanged event
MainActivity -> Activity: recreate()
Activity -> Compose: recomposition with new Resources
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modify | Add AppCompat dependency for locale APIs. |
| `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt` | Modify | Add getters/setters for language preference (locale tag or empty for system). |
| `app/src/main/java/com/cvc953/localplayer/viewmodel/MainViewModel.kt` | Modify | Add language `StateFlow`, update method, and locale-changed event. |
| `app/src/main/java/com/cvc953/localplayer/MainActivity.kt` | Modify | Apply locale on startup and observe locale changes to recreate activity. |
| `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistsScreen.kt` | Modify | Add language selector section in `SettingsScreen`. |
| `app/src/main/java/com/cvc953/localplayer/ui/screens/*` | Modify | Replace hardcoded strings with `stringResource(...)`. |
| `app/src/main/res/values/strings.xml` | Modify | Add base strings. |
| `app/src/main/res/values-*/strings.xml` | Create | Add localized strings for initial language set. |

## Interfaces / Contracts

```kotlin
// AppPrefs
fun getAppLanguageTag(): String? // null or empty => system
fun setAppLanguageTag(tag: String?)

// MainViewModel
val appLanguageTag: StateFlow<String?>
fun setAppLanguage(tag: String?)
val localeChanged: StateFlow<Boolean> // or SharedFlow<Unit>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `AppPrefs` language persistence | Unit test with shared prefs. |
| Integration | Locale change triggers AppCompat update and activity recreation | Instrumented test (if available) or manual verification on API 24+ and 33+. |
| UI | Strings resolved from resources | Manual checklist per screen after migration. |

## Migration / Rollout

No data migration required. Introduce localized `strings.xml` and backfill base strings; fallback behavior relies on Android resource resolution.

## Open Questions

- [ ] Confirm initial language list and locale tags (e.g., `es`, `en`).
- [ ] Confirm UX for “system default” labeling in Settings.
- [ ] Confirm if activity recreation is acceptable on language change or prefer alternative (e.g., navigation reset).
