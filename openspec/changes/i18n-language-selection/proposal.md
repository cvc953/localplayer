# Proposal: i18n-language-selection

## Intent

Enable per-app language selection and proper localization by moving UI strings to Android resources and applying app-specific locale at runtime.

## Scope

### In Scope
- Add a language selector in Settings UI and persist selection in `AppPrefs`/`MainViewModel`.
- Apply app-specific locale at runtime using AppCompat locale APIs.
- Migrate user-facing UI strings from Compose screens to `strings.xml` and add localized resource files for the initial language set.

### Out of Scope
- Full content translation beyond the initial language set (future expansion).
- Refactoring the unused `SettingsController`/`SettingsViewModel` path.

## Capabilities

### New Capabilities
- `i18n-language-selection`: per-app language preference, runtime locale application, and resource-backed UI strings.

### Modified Capabilities
- None.

## Approach

Adopt AppCompat per-app locale APIs (`AppCompatDelegate.setApplicationLocales`) and wire language state through `AppPrefs` → `MainViewModel` → Settings UI. Migrate Compose hardcoded strings to `stringResource(...)` backed by `values/` and `values-xx/` resources.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modified | Add AppCompat dependency if missing for locale APIs. |
| `app/src/main/java/com/cvc953/localplayer/MainActivity.kt` | Modified | Apply selected locale at startup / on change. |
| `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt` | Modified | Persist language preference. |
| `app/src/main/java/com/cvc953/localplayer/viewmodel/MainViewModel.kt` | Modified | Expose language flow and update handling. |
| `app/src/main/java/com/cvc953/localplayer/ui/screens/PlaylistsScreen.kt` | Modified | Add language selector in `SettingsScreen`. |
| `app/src/main/java/com/cvc953/localplayer/ui/screens/*` | Modified | Replace hardcoded strings with `stringResource(...)`. |
| `app/src/main/res/values/strings.xml` | Modified | Base strings moved from hardcoded UI. |
| `app/src/main/res/values-*/strings.xml` | New | Localized strings for selected languages. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Mixed-language UI due to incomplete migration | High | Track string migration by screen; verify all user-visible strings use resources. |
| Locale change not fully applied at runtime | Medium | Recreate activity on change if needed; verify on API 33+ and lower. |
| Settings state split between two systems | Medium | Keep language in `AppPrefs`/`MainViewModel` only. |

## Rollback Plan

Revert to system locale by removing language preference usage and locale application; keep base `strings.xml` as Spanish (or default) so the UI remains consistent without per-app selection.

## Dependencies

- Confirm initial language set and UX (system default option vs explicit list).
- AndroidX AppCompat locale APIs availability.

## Success Criteria

- [ ] User can select a language in Settings and the app UI switches to that language.
- [ ] All user-facing strings on the main screens are resource-backed (no hardcoded UI strings).
- [ ] Language preference persists across app restarts.
