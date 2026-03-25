# Proposal: Theme Color Customization

## Intent

The app currently hardcodes a single primary color (`md_primary = Color(0xFF2196F3)`) with no runtime customization. Users expect music players to offer aesthetic personalization. The existing SettingsScreen has a broken "Color de acento" section (undefined `primaryColor` reference) that signals this was planned but never completed. This change delivers a working primary color selector using a predefined palette of Material colors, persisted across sessions.

## Scope

### In Scope
- Predefined palette of ~10 Material 3 colors with proper `onColor` (contrast) values
- Persistent storage of selected color hex via `AppPrefs`
- Reactive state flow from `MainViewModel` → `MainActivity` → `LocalPlayerTheme`
- Settings UI: horizontal scrollable row of color circles with selection indicator
- Theme recomposition on color change (rebuild `darkColorScheme`/`lightColorScheme` inside the composable)
- Proper `onPrimary` luminance-based pairing for each palette entry

### Out of Scope
- Free-form color picker (hex input, wheel, sliders)
- Per-component theming (only global primary)
- Custom user-defined colors beyond the predefined palette
- Removing dynamic color (Material You) — they are orthogonal features
- Cleaning up dead code (`Settings.kt`, `SettingsController.kt`, `SettingsViewModel.kt`)

## Approach

1. **Data Model** (`Color.kt`): Define `ThemeColor(name, hex, color, onColor)` and a `val themeColors` list. Each entry pairs its primary with a computed/preset `onColor` based on relative luminance.

2. **Persistence** (`AppPrefs.kt`): Add `getPrimaryColor(): String` (default `#2196F3`) and `setPrimaryColor(hex: String)`. Plain SharedPreferences, no new dependencies.

3. **State** (`MainViewModel.kt`): Add `_primaryColorHex: MutableStateFlow<String>`, load from prefs on init, expose `setPrimaryColor(hex)` method.

4. **Theme** (`Theme.kt`): Change `LocalPlayerTheme` signature to accept `primaryColor: Color`. Move scheme construction inside the composable (currently private vals at file level). On each recomposition with a new color, rebuild `dynamicDarkColorScheme`/`dynamicLightColorScheme` with the updated seed.

5. **Wiring** (`MainActivity.kt`): Collect `primaryColorHex` from ViewModel, resolve to `Color` via the palette lookup, pass to `LocalPlayerTheme`.

6. **UI** (`SettingsScreen.kt`): Replace the broken Switch with a `LazyRow` of `Box` circles. Each circle shows the palette color, selected state indicated by a border/icon. Dynamic color toggle remains separate.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/com/christian/localplayer/ui/theme/Color.kt` | Modified | Add `ThemeColor` data class and palette list |
| `app/src/main/java/com/christian/localplayer/ui/theme/Theme.kt` | Modified | Parameterize `LocalPlayerTheme`, move scheme construction inside composable |
| `app/src/main/java/com/christian/localplayer/data/preferences/AppPrefs.kt` | Modified | Add `getPrimaryColor()` / `setPrimaryColor()` |
| `app/src/main/java/com/christian/localplayer/ui/viewmodel/MainViewModel.kt` | Modified | Add `_primaryColorHex` state flow and setter |
| `app/src/main/java/com/christian/localplayer/MainActivity.kt` | Modified | Collect color state, pass to theme composable |
| `app/src/main/java/com/christian/localplayer/ui/screens/settings/SettingsScreen.kt` | Modified | Replace broken section with palette selector UI |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scheme recomposition causes visual flicker on rapid color changes | Low | Debounce not needed — user taps one circle at a time. `remember` + `derivedStateOf` for scheme objects |
| `onPrimary` contrast fails WCAG on certain palette entries | Medium | Use `Color.luminance()` to auto-select white or black as `onColor`, or preset each entry manually |
| Dynamic color override conflicts with manual selection | Medium | Keep them independent: dynamic color toggle ignores manual color, manual color only applies when dynamic is OFF. Make this explicit in UI |
| SharedPreferences race if ViewModel loads before prefs ready | Low | Prefs init is synchronous in `onCreate`, ViewModel reads after. No async gap |
| Palette lookup fails for custom hex from future migrations | Low | Fall back to default `md_primary` if no match found |

## Rollback Plan

Each file change is independent and reversible:

1. `Color.kt` — remove `ThemeColor` and palette list
2. `Theme.kt` — restore original `LocalPlayerTheme` with hardcoded `md_primary`
3. `AppPrefs.kt` — remove getter/setter methods
4. `MainViewModel.kt` — remove `_primaryColorHex` flow and setter
5. `MainActivity.kt` — restore original `LocalPlayerTheme()` call without params
6. `SettingsScreen.kt` — restore broken Switch or remove section entirely

Git revert on the merge commit restores the full prior state. No data migration needed — prefs default handles missing key.

## Dependencies

- No new library dependencies required
- Requires `androidx.compose.ui.graphics.luminance()` (already available in Compose UI)
- SharedPreferences infrastructure already exists in `AppPrefs`

## Success Criteria

- [ ] User can select a color from the palette in SettingsScreen and the app's primary color updates immediately (no restart)
- [ ] Selected color persists across app restarts via SharedPreferences
- [ ] `onPrimary` provides sufficient contrast (luminance ratio > 4.5:1) for all palette entries
- [ ] Dynamic color (Material You) toggle and manual color selection work independently
- [ ] No compilation errors or warnings in affected files
- [ ] Palette renders as a horizontal scrollable row of color circles with visual selection indicator
