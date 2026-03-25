# Tasks: Theme Color Customization

## Phase 1: Foundation — Data Model & Utilities

- [x] 1.1 In `Color.kt`, define `data class ThemeColor(val name: String, val hex: String, val color: Color)` with display name, canonical hex identifier, and Compose Color object
- [x] 1.2 In `Color.kt`, create `val themeColors: List<ThemeColor>` with 10 predefined Material 3 colors: Azul `#2196F3`, Rojo `#F44336`, Rosa `#E91E63`, Púrpura `#9C27B0`, Violeta `#673AB7`, Índigo `#3F51B5`, Cian `#00BCD4`, Verde `#4CAF50`, Naranja `#FF9800`, Ámbar `#FFC107`
- [x] 1.3 In `Color.kt`, add `fun computeOnPrimary(primary: Color): Color` — returns `Color.Black` if `primary.luminance() > 0.3f`, else `Color.White`
- [x] 1.4 In `Color.kt`, add `fun resolvePrimaryColor(hex: String): Color` — finds matching color in `themeColors` by hex, falls back to `Color(0xFF2196F3)` (Azul default) if not found

## Phase 2: Persistence — AppPrefs Layer

- [x] 2.1 In `AppPrefs.kt`, add `fun getPrimaryColor(): String` — reads `"primary_color_hex"` from SharedPreferences with default `"#2196F3"` (handles null return from `getString`)
- [x] 2.2 In `AppPrefs.kt`, add `fun setPrimaryColor(hex: String)` — writes hex string to SharedPreferences key `"primary_color_hex"` using `apply()` (async)

## Phase 3: State — ViewModel Layer

- [ ] 3.1 In `MainViewModel.kt`, add `private val _primaryColorHex = MutableStateFlow(appPrefs.getPrimaryColor())` initialized from persisted preference on ViewModel construction
- [ ] 3.2 In `MainViewModel.kt`, expose `val primaryColorHex: StateFlow<String>` as public read-only flow for UI collection
- [ ] 3.3 In `MainViewModel.kt`, add `fun setPrimaryColor(hex: String)` — calls `appPrefs.setPrimaryColor(hex)` to persist, then sets `_primaryColorHex.value = hex` to trigger UI recomposition

## Phase 4: Theme Construction — Theme.kt Refactor

- [ ] 4.1 In `Theme.kt`, remove the file-level `private val DarkColorScheme` and `private val LightColorScheme` (they are computed at class-load time and cannot respond to runtime changes)
- [ ] 4.2 In `Theme.kt`, add `primaryColor: Color = Color(0xFF2196F3)` parameter to `LocalPlayerTheme` composable signature
- [ ] 4.3 In `Theme.kt`, compute `onPrimary` inside the composable using `remember(primaryColor) { computeOnPrimary(primaryColor) }` for luminance-based contrast text color
- [ ] 4.4 In `Theme.kt`, build `colorScheme` inside the composable using `remember(primaryColor, darkTheme)` — calls `darkColorScheme(...)` or `lightColorScheme(...)` with `primary = primaryColor`, `onPrimary = onPrimary`, and existing surface/background/outline values from the current `md_*` constants
- [ ] 4.5 In `Theme.kt`, ensure `extendedColors` (CompositionLocal for extended color tokens) remain unchanged — they are not affected by primary color selection

## Phase 5: Integration — MainActivity Wiring

- [ ] 5.1 In `MainActivity.kt`, collect `viewModel.primaryColorHex` as Compose `State` using `collectAsState()`
- [ ] 5.2 In `MainActivity.kt`, resolve the hex string to a `Color` using `resolvePrimaryColor(primaryColorHex)` — this maps the persisted hex to the actual Compose Color via the `themeColors` list lookup
- [ ] 5.3 In `MainActivity.kt`, pass the resolved `primaryColor` to `LocalPlayerTheme(primaryColor = ...)` — this triggers scheme recomputation inside the theme composable
- [ ] 5.4 Verify that `LocalPlayerTheme` retains its existing `darkTheme` parameter and that dynamic color (album art extraction) remains completely independent from the manual primary color

## Phase 6: UI — SettingsScreen Palette Selector

- [ ] 6.1 In `SettingsScreen.kt`, remove the broken `Switch` component that references the undefined `primaryColor` variable (lines ~142–162 in current file)
- [ ] 6.2 In `SettingsScreen.kt`, collect `viewModel.primaryColorHex` as state: `val primaryColorHex by viewModel.primaryColorHex.collectAsState()`
- [ ] 6.3 In `SettingsScreen.kt`, add a `LazyRow` with `horizontalArrangement = Arrangement.spacedBy(10.dp)` and `modifier = Modifier.fillMaxWidth().padding(top = 8.dp)` inside the "Color de acento" section
- [ ] 6.4 In `SettingsScreen.kt`, render each `themeColor` as a `Box` circle: `Modifier.size(36.dp).clip(CircleShape).background(themeColor.color)` — the circle displays the palette color as its background
- [ ] 6.5 In `SettingsScreen.kt`, add selection border: selected circle gets `border(width = 3.dp, color = onSurface)` while unselected circles get `border(width = 1.dp, color = outline.copy(alpha = 0.3f))` — uses `CircleShape` for round borders
- [ ] 6.6 In `SettingsScreen.kt`, add a checkmark `Icon(Icons.Default.Check)` centered inside the selected circle with `tint = computeOnPrimary(themeColor.color)` and `modifier = Modifier.size(18.dp)` for white/black contrast
- [ ] 6.7 In `SettingsScreen.kt`, add `Modifier.clickable { viewModel.setPrimaryColor(themeColor.hex) }` to each circle — tapping triggers the full persist + recompose flow

## Phase 7: Verification & Edge Cases

- [ ] 7.1 Verify default behavior: on first launch (no stored preference), Azul `#2196F3` is used as primary and shown as selected in the palette
- [ ] 7.2 Verify persistence round-trip: select a color → force-kill app → reopen → the same color is selected and the theme reflects it
- [ ] 7.3 Verify fallback for invalid hex: manually set SharedPreferences to `"#XXXXXX"` → app loads with Azul default, palette shows Azul selected, no crash
- [ ] 7.4 Verify dynamic color independence: enable dynamic color toggle + select manual color → player screen shows album art colors, rest of app uses manual primary
- [ ] 7.5 Verify immediate theme update: select a different color → all components using `MaterialTheme.colorScheme.primary` (sliders, icons, nav indicators) update instantly without restart
- [ ] 7.6 Verify all 10 palette colors produce acceptable contrast: check each circle's checkmark icon is visible (luminance threshold 0.3 should give black text on Ámbar/Verde, white on the rest)

## Tasks Summary

| Phase | Tasks | Focus |
|-------|-------|-------|
| Phase 1 | 4 | ThemeColor data model, palette definition, utility functions |
| Phase 2 | 2 | SharedPreferences persistence for primary color hex |
| Phase 3 | 3 | ViewModel StateFlow + setter wiring |
| Phase 4 | 5 | Refactor Theme.kt from static schemes to parameterized inline construction |
| Phase 5 | 4 | MainActivity wiring — collect state, resolve hex, pass to theme |
| Phase 6 | 7 | SettingsScreen — replace broken Switch with LazyRow palette circles |
| Phase 7 | 6 | Verification scenarios from spec |
| **Total** | **31** | |

## Implementation Order

Follow the phases sequentially — each phase depends on the previous:

1. **Phase 1** first — `ThemeColor`, palette, and utilities are referenced by every subsequent phase
2. **Phase 2** next — AppPrefs persistence must exist before ViewModel can read/write
3. **Phase 3** next — ViewModel must expose `primaryColorHex` before UI can collect it
4. **Phase 4** next — Theme.kt must accept `primaryColor` before MainActivity can pass it
5. **Phase 5** next — MainActivity wires ViewModel → Theme flow
6. **Phase 6** next — SettingsScreen is the final consumer; it writes to ViewModel, which triggers the whole chain
7. **Phase 7** last — manual verification of all spec scenarios

Within each phase, tasks are ordered by dependency (1.1 → 1.2 → 1.3 → 1.4, etc.).

## Next Step

Ready for implementation (sdd-apply).
