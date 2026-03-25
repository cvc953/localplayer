# Delta for UI — Theme Color Customization

## Purpose

This specification defines the requirements for runtime primary color customization in the LocalPlayer music app. Users SHALL be able to select from a predefined palette of Material 3 colors, with the selection persisted across app restarts and the theme updated immediately upon selection.

## ADDED Requirements

### Requirement: Primary Color Palette MUST Be Defined

The system MUST provide a predefined palette of at least 10 Material 3 color options. Each palette entry MUST contain a display name, hex identifier, and Compose Color object.

#### Scenario: Palette contains required colors

- GIVEN the app is launched for the first time
- WHEN the user navigates to Settings
- THEN the palette selector SHALL display at least 10 distinct color options
- AND each color SHALL have a human-readable name in Spanish

---

### Requirement: Selected Color MUST Persist Across App Restarts

The system MUST store the user's selected primary color hex value in SharedPreferences and retrieve it on app launch.

#### Scenario: Color selection persists after app restart

- GIVEN the user has selected "Rojo" (#F44336) from the palette
- WHEN the user closes the app completely and reopens it
- THEN the app SHALL display "Rojo" as the active primary color
- AND the theme SHALL reflect "#F44336" as the primary color

#### Scenario: Default color is applied when no preference exists

- GIVEN the app is launched for the first time (no stored preference)
- WHEN the app loads
- THEN the default color "#2196F3" (Azul) SHALL be used
- AND the palette selector SHALL show "Azul" as selected

---

### Requirement: Theme MUST Update Immediately on Color Selection

The system SHALL recompose the Material Theme with the new primary color immediately when the user selects a color from the palette, without requiring app restart.

#### Scenario: Theme updates without app restart

- GIVEN the app is displaying the default blue theme
- WHEN the user taps "Verde" (#4CAF50) in the palette selector
- THEN the app's primary color SHALL change to green immediately
- AND all UI components using `MaterialTheme.colorScheme.primary` SHALL reflect the new color
- AND the user SHALL NOT need to restart the app

---

### Requirement: Palette Selector MUST Display as Colored Circles

The system SHALL render the color palette as a horizontal scrollable row of circular color swatches in the Settings screen.

#### Scenario: Palette renders as horizontal row of circles

- GIVEN the user is on the Settings screen
- WHEN the "Color de acento" section is visible
- THEN the palette SHALL be displayed as a horizontal `LazyRow`
- AND each color SHALL be rendered as a circular `Box`
- AND each circle SHALL display its corresponding primary color as background

---

### Requirement: Selected Color MUST Show Visual Indicator

The system MUST indicate which color is currently selected with a visible border or checkmark.

#### Scenario: Selected circle shows selection state

- GIVEN the palette is displayed
- WHEN a color circle is currently selected
- THEN that circle SHALL have a thicker border (approximately 3dp) compared to unselected circles (approximately 1dp)
- OR the selected circle SHALL display a checkmark icon
- AND unselected circles SHALL have a subtle border

---

### Requirement: Color Selection MUST Be Independent of Dynamic Color

The manual primary color selection and the dynamic color (album art extraction) feature SHALL operate independently without interference.

#### Scenario: Dynamic color toggles independently

- GIVEN the user has selected "Naranja" as the manual primary color
- WHEN the user toggles "Color dinámico" on or off
- THEN the manual color selection SHALL remain unchanged
- AND the dynamic color effect SHALL apply only to the player screen background

#### Scenario: Both features can coexist

- GIVEN the user has selected a manual primary color
- AND the dynamic color feature is enabled
- WHEN the user plays a song with album art
- THEN the player screen SHALL show dynamic colors from album art
- AND the rest of the app SHALL use the manually selected color as primary

---

### Requirement: Invalid Persisted Color MUST Fall Back to Default

The system SHALL handle unknown hex values gracefully by falling back to the default color.

#### Scenario: Unknown hex falls back to default

- GIVEN the SharedPreferences contains an invalid hex value "#XXXXXX" (non-existent in palette)
- WHEN the app loads and attempts to resolve the color
- THEN the app SHALL fall back to default "#2196F3"
- AND the palette SHALL display "Azul" as selected

---

## MODIFIED Requirements

### Requirement: Settings Screen Color Section

The existing broken "Color de acento" Switch section MUST be replaced with the functioning palette selector.

(Previously: A Switch was displayed that referenced an undefined `primaryColor` variable)

#### Scenario: Broken Switch is replaced with functional palette

- GIVEN the user navigates to the Settings screen
- WHEN the "Color de acento" section is rendered
- THEN the Switch component SHALL NOT be displayed
- AND the functional palette selector SHALL be displayed instead
- AND tapping any color circle SHALL trigger the new color selection

---

### Requirement: Theme Construction Architecture

The `LocalPlayerTheme` composable MUST accept a `primaryColor` parameter and construct color schemes inline to enable runtime updates.

(Previously: Color schemes were constructed at file-level as private vals, making runtime changes impossible)

#### Scenario: Theme recomposes when primary color changes

- GIVEN `LocalPlayerTheme` is called with a `primaryColor` parameter
- WHEN the `primaryColor` value changes
- THEN the color scheme SHALL be recomputed
- AND `MaterialTheme.colorScheme.primary` SHALL reflect the new color

---

## REMOVED Requirements

### Requirement: Hardcoded Primary Color

The system SHALL NOT use a hardcoded primary color value as the sole theme color.

(Reason: This was a temporary implementation before customization was added)

---

## Acceptance Criteria Summary

| Criterion | Requirement | Scenario |
|-----------|------------|----------|
| Palette displays in Settings | MUST show colored circles | Palette renders as horizontal row |
| Color selection works | MUST update immediately | Theme updates without restart |
| Persistence | MUST persist across restarts | Color selection survives app restart |
| Visual indication | MUST show selected state | Selected circle has border/checkmark |
| Independence | MUST be independent from dynamic color | Both features work together |
| Fallback handling | MUST handle invalid values | Unknown hex uses default |

---

## Next Step

Ready for design review (sdd-design). Design already exists, so ready for task breakdown (sdd-tasks).