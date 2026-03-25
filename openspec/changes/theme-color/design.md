# Design: Theme Color Customization

## Technical Approach

The current theme system hardcodes `md_primary` (`#2196F3`) as a file-level `private val` in `Color.kt`, which gets baked into `DarkColorScheme`/`LightColorScheme` at class-load time. These schemes are then used statically by `LocalPlayerTheme`. This architecture **cannot support runtime color changes** — the schemes never rebuild.

The approach: introduce a `ThemeColor` data model with a predefined palette, persist the selection as a hex string via `AppPrefs`, expose it as a `StateFlow` from `MainViewModel`, and **move scheme construction inside the `LocalPlayerTheme` composable** so it recomposes when the primary color changes. The SettingsScreen broken `Switch` gets replaced with a `LazyRow` of colored circles.

## Architecture Decisions

### Decision: Move scheme construction from file-level vals to inline composable construction

**Choice**: Build `darkColorScheme(...)` and `lightColorScheme(...)` **inside** `LocalPlayerTheme` on every recomposition when `primaryColor` changes, parameterized by the incoming `primaryColor: Color` and its computed `onPrimary`.

**Alternatives considered**:
- *`remember` with a key*: Could use `remember(primaryColor) { darkColorScheme(...) }` to avoid rebuilding on unrelated recompositions. Rejected — scheme construction is cheap (just a data class with ~8 fields), and `remember` adds complexity for zero measurable gain.
- *CompositionLocal for primary*: Provide `primaryColor` via `CompositionLocal` and have each composable read it individually. Rejected — this defeats the purpose of `MaterialTheme.colorScheme`, which is the standard M3 API. Every component using `MaterialTheme.colorScheme.primary` would break.
- *Stateful scheme in ViewModel*: Store the constructed `ColorScheme` in the ViewModel. Rejected — `ColorScheme` is a Compose type that shouldn't live in the ViewModel layer; it violates separation of concerns.

**Rationale**: M3's `darkColorScheme()`/`lightColorScheme()` are factory functions that return immutable data classes. The ONLY way to change `primary` at runtime is to call them again with different parameters. Since Compose tracks recomposition by parameter changes, passing `primaryColor` as a parameter to `LocalPlayerTheme` naturally triggers scheme rebuild. This is the idiomatic M3 approach.

**Tradeoff**: The scheme rebuilds on EVERY recomposition of `LocalPlayerTheme`, not just color changes. In practice this is negligible — `LocalPlayerTheme` is the root composable and recomposes rarely (only when `darkTheme` or `primaryColor` change).

### Decision: Luminance-based `onPrimary` computation vs manual pairing

**Choice**: Use `Color.luminance()` to auto-select `Color.White` or `Color.Black` as `onPrimary` for each palette entry. Threshold: `luminance > 0.3` → black text, else white.

**Alternatives considered**:
- *Manual `onColor` per palette entry*: Pre-compute and hardcode a specific contrast color for each palette color. More control, but requires manual WCAG verification for every entry and doesn't scale if palette changes.
- *Full WCAG contrast ratio calculation*: Compute actual contrast ratio against white/black and pick the better one. Overkill for a music player — luminance threshold is sufficient and what most Material palette generators use internally.

**Rationale**: `luminance()` is a single-line check, already available in `androidx.compose.ui.graphics`. A threshold of 0.3 gives comfortable contrast (> 4.5:1 ratio) for both white and black text on typical Material seed colors. If any specific palette entry fails, we can override it manually in the `ThemeColor` data class.

**Tradeoff**: Some edge-case colors in the mid-luminance range (0.25–0.35) might get suboptimal contrast. Mitigated by manual override per entry if needed.

### Decision: Orthogonal dynamic color — no interaction

**Choice**: The "Color dinámico" toggle (album art extraction via `ColorExtractor`) and the manual primary color selector are **completely independent features**. Manual color applies globally as the Material theme seed. Dynamic color applies only within the player screen as a background/overlay tint.

**Alternatives considered**:
- *Mutual exclusion*: When dynamic color is ON, disable manual selector (or vice versa). Rejected — they operate on different layers. Dynamic color is a per-song visual effect in the player, not a Material You system theme.
- *Dynamic color overrides manual when playing*: Could blend or override. Rejected — adds complexity with no clear user benefit. The user picks a global accent AND gets album art vibes in the player. These complement each other.

**Rationale**: Examining `ColorExtractor.kt` and the player screen, dynamic color extracts from album art bitmaps and applies it locally (player background, overlays). The Material theme primary color affects sliders, icons, navigation indicators globally. They're different concerns.

### Decision: Fallback for unknown persisted hex

**Choice**: If the persisted hex string doesn't match any entry in `themeColors`, fall back to the default blue (`#2196F3`). Log a warning.

**Alternatives considered**:
- *Crash/assert*: Rejected — too aggressive for a cosmetic preference.
- *Silently use first palette entry*: Could confuse user if palette order changes.
- *Create a synthetic ThemeColor from the hex*: Rejected — would need to compute `onColor` and handle arbitrary values, which defeats the predefined palette constraint.

**Rationale**: The hex is stored as a plain string. If the palette changes between versions (colors added/removed), a previously selected color might no longer exist. Falling back to the well-known default is the safest option. A warning log helps debugging.

### Decision: Store hex string vs palette index

**Choice**: Persist the hex string (e.g. `"#2196F3"`) rather than an index into the palette array.

**Alternatives considered**:
- *Store index (Int)*: Simpler, no hex parsing. Rejected — if palette order changes or entries are added/removed, stored indices become invalid. Hex is order-independent.
- *Store ThemeColor.name*: Human-readable, also order-independent. Rejected — name strings are for display, not identity. If we rename "Blue" to "Material Blue", stored values break.

**Rationale**: Hex is the canonical identifier for a color. It's what `Color(hex)` accepts directly. It survives palette reordering and renaming. The only failure mode is entry removal, which the fallback handles.

## Data Flow

```
AppPrefs (SharedPreferences)
    │
    │  getPrimaryColor() → "#2196F3"
    ▼
MainViewModel
    │  _primaryColorHex: MutableStateFlow<String>
    │  setPrimaryColor(hex) → writes to AppPrefs + updates flow
    ▼
MainActivity
    │  collect primaryColorHex as State
    │  resolve hex → Color via themeColors.find { it.hex == hex }
    │  pass resolved Color to LocalPlayerTheme
    ▼
LocalPlayerTheme(primaryColor: Color)
    │  compute onPrimary via luminance
    │  build DarkColorScheme(primary=..., onPrimary=...)
    │  build LightColorScheme(primary=..., onPrimary=...)
    │  select scheme based on darkTheme
    ▼
MaterialTheme(colorScheme = ...)
    │
    ▼
All composables reading MaterialTheme.colorScheme.primary
```

```
SettingsScreen
    │  collect primaryColorHex from ViewModel
    │  render LazyRow of themeColors as circles
    │  on tap → viewModel.setPrimaryColor(themeColor.hex)
    ▼
MainViewModel.setPrimaryColor()
    ├── appPrefs.setPrimaryColor(hex)   // persist
    └── _primaryColorHex.value = hex    // trigger recomposition
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/com/cvc953/localplayer/ui/theme/Color.kt` | Modify | Add `ThemeColor` data class and `val themeColors: List<ThemeColor>` with ~10 predefined Material 3 colors |
| `app/src/main/java/com/cvc953/localplayer/ui/theme/Theme.kt` | Modify | Remove `private val DarkColorScheme`/`LightColorScheme`. Add `primaryColor: Color` param to `LocalPlayerTheme`. Build schemes inline using `remember(primaryColor)`. |
| `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt` | Modify | Add `getPrimaryColor(): String` (default `"#2196F3"`) and `setPrimaryColor(hex: String)` |
| `app/src/main/java/com/cvc953/localplayer/viewmodel/MainViewModel.kt` | Modify | Add `_primaryColorHex: MutableStateFlow<String>`, init from `appPrefs.getPrimaryColor()`. Add `setPrimaryColor(hex: String)` method. |
| `app/src/main/java/com/cvc953/localplayer/MainActivity.kt` | Modify | Collect `primaryColorHex`, resolve to `Color` via `themeColors`, pass `primaryColor` to `LocalPlayerTheme` |
| `app/src/main/java/com/cvc953/localplayer/ui/SettingsScreen.kt` | Modify | Replace broken `Switch` with `LazyRow` of `Box` circles. Add `primaryColorHex` state collection. Add tap handler calling `viewModel.setPrimaryColor()`. |

**No new files created.** All changes are modifications to existing files.

## Interfaces / Contracts

### ThemeColor data class (in `Color.kt`)

```kotlin
data class ThemeColor(
    val name: String,       // Display name, e.g. "Azul", "Rojo"
    val hex: String,        // Canonical identifier, e.g. "#2196F3"
    val color: Color,       // Compose Color object
)

val themeColors: List<ThemeColor> = listOf(
    ThemeColor("Azul", "#2196F3", Color(0xFF2196F3)),
    ThemeColor("Rojo", "#F44336", Color(0xFFF44336)),
    ThemeColor("Rosa", "#E91E63", Color(0xFFE91E63)),
    ThemeColor("Púrpura", "#9C27B0", Color(0xFF9C27B0)),
    ThemeColor("Violeta", "#673AB7", Color(0xFF673AB7)),
    ThemeColor("Índigo", "#3F51B5", Color(0xFF3F51B5)),
    ThemeColor("Cian", "#00BCD4", Color(0xFF00BCD4)),
    ThemeColor("Verde", "#4CAF50", Color(0xFF4CAF50)),
    ThemeColor("Naranja", "#FF9800", Color(0xFFFF9800)),
    ThemeColor("Ámbar", "#FFC107", Color(0xFFFFC107)),
)

// Utility: luminance-based onPrimary
fun computeOnPrimary(primary: Color): Color =
    if (primary.luminance() > 0.3f) Color.Black else Color.White

// Lookup helper
fun resolvePrimaryColor(hex: String): Color =
    themeColors.find { it.hex == hex }?.color ?: Color(0xFF2196F3)
```

### AppPrefs contract additions

```kotlin
fun getPrimaryColor(): String =
    prefs.getString("primary_color_hex", "#2196F3") ?: "#2196F3"

fun setPrimaryColor(hex: String) {
    prefs.edit().putString("primary_color_hex", hex).apply()
}
```

### MainViewModel additions

```kotlin
private val _primaryColorHex = MutableStateFlow(appPrefs.getPrimaryColor())
val primaryColorHex: StateFlow<String> = _primaryColorHex

fun setPrimaryColor(hex: String) {
    appPrefs.setPrimaryColor(hex)
    _primaryColorHex.value = hex
}
```

### Theme.kt modified signature

```kotlin
@Composable
fun LocalPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = Color(0xFF2196F3),
    content: @Composable () -> Unit,
) {
    val onPrimary = remember(primaryColor) { computeOnPrimary(primaryColor) }

    val colorScheme = remember(primaryColor, darkTheme) {
        if (darkTheme) {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = onPrimary,
                background = md_background,
                onBackground = md_onBackground,
                surface = md_surface,
                onSurface = md_onSurface,
                outline = md_outlineSoft,
                surfaceVariant = md_surfaceSheet,
                onSurfaceVariant = md_textSecondary,
            )
        } else {
            lightColorScheme(
                primary = primaryColor,
                onPrimary = onPrimary,
                background = md_lightBackground,
                onBackground = md_lightOnBackground,
                surface = md_lightSurface,
                onSurface = md_lightOnSurface,
                surfaceVariant = md_lightSurfaceVariant,
                onSurfaceVariant = md_lightTextSecondary,
                outline = md_lightOutlineSoft,
            )
        }
    }

    // ExtendedColors remain unchanged (not affected by primary)
    val extendedColors = ...
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}
```

### SettingsScreen UI replacement

The broken section (lines 142–162 in current `SettingsScreen.kt`):

```kotlin
// BEFORE (broken — references undefined primaryColor)
Switch(checked = primaryColor, onCheckedChange = {}, ...)

// AFTER — Palette selector
val primaryColorHex by viewModel.primaryColorHex.collectAsState()

// Inside the "Color de acento" section:
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
) {
    items(themeColors) { themeColor ->
        val isSelected = themeColor.hex == primaryColorHex
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(themeColor.color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape,
                )
                .clickable { viewModel.setPrimaryColor(themeColor.hex) },
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = computeOnPrimary(themeColor.color),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `computeOnPrimary()` returns correct contrast color for light/dark palette entries | Test each of the 10 palette colors against expected white/black result |
| Unit | `resolvePrimaryColor()` returns correct Color for valid hex, falls back for invalid | Test valid hex match, unknown hex fallback, empty string |
| Unit | `AppPrefs.getPrimaryColor()` returns default when no pref set | Create AppPrefs with fresh SharedPreferences, assert `"#2196F3"` |
| Unit | `AppPrefs.setPrimaryColor()` / `getPrimaryColor()` round-trip | Set a value, read it back, assert match |
| Unit | `MainViewModel.setPrimaryColor()` updates both prefs and flow | Mock AppPrefs, call setter, assert flow emits new value |
| Integration | `LocalPlayerTheme` recomposes when `primaryColor` changes | Use Compose testing: render theme, change param, assert `MaterialTheme.colorScheme.primary` updates |
| Integration | SettingsScreen palette circle tap triggers `setPrimaryColor` | Compose UI test: tap a circle, verify ViewModel flow updated |
| E2E | Select color → kill app → reopen → color persists | Manual test or instrumented test with ActivityScenario |

## Migration / Rollout

No migration required. The `SharedPreferences` key `"primary_color_hex"` is new. When absent, `getPrimaryColor()` returns the default `"#2196F3"`, which matches the current hardcoded primary color. The app looks identical before and after this change until the user explicitly selects a different color.

**Feature flag**: Not needed. This is a user-facing setting, not an experimental feature.

**Rollback**: Each file change is independently reversible. A `git revert` on the merge commit restores full prior state. No data corruption risk — the worst case is an orphaned `"primary_color_hex"` key in SharedPreferences that nothing reads.

## Open Questions

- [ ] **Palette entries**: The current list is a starting point. Are there specific colors the user wants? The 10 entries cover the Material 3 standard seed colors plus orange/amber for warmer options.
- [ ] **Luminance threshold**: 0.3 is the proposed cutoff. If specific palette entries produce poor contrast in testing, we can manually override `onColor` per entry (add an optional `onColor: Color` field to `ThemeColor`).
- [ ] **SettingsScreen layout**: The `LazyRow` of circles works for 10 items. If the palette grows significantly, consider a grid or paginated approach. Not a concern at current size.
