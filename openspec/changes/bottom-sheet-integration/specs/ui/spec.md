# Delta for UI - BottomSheet Integration

## Purpose

This spec describes the changes to integrate BottomSheetScaffold for the player experience, replacing the current overlay system.

## ADDED Requirements

### Requirement: BottomSheetScaffold Integration

The system MUST use `BottomSheetScaffold` from Material3 as the primary container for the main screen, replacing the current Box-based overlay system.

The BottomSheetScaffold MUST provide:
- A collapsible bottom sheet for player functionality
- Native swipe gestures for expand/collapse
- Smooth animations between states

#### Scenario: BottomSheetScaffold Initialization

- GIVEN The app starts with a song in the queue
- WHEN The MusicScreen composable is initialized
- THEN A BottomSheetScaffold MUST be rendered with:
  - The main Scaffold content (navigation + bottom bar)
  - The sheetContent set to the PlayerScreen
  - The sheetState configured with initial value PartiallyExpanded

#### Scenario: No Song Playing

- GIVEN No song is currently playing
- WHEN The MusicScreen is rendered
- THEN The BottomSheetScaffold sheetState MUST be set to Hidden
- AND No bottom sheet content should be visible

### Requirement: MiniPlayer as Collapsed State

The system SHALL display the MiniPlayer as the collapsed (peek) state of the BottomSheet.

When the bottom sheet is in PartiallyExpanded state:
- The MiniPlayer content MUST be visible
- The peek height MUST accommodate the MiniPlayer widget

#### Scenario: MiniPlayer Visible

- GIVEN A song is playing
- WHEN The user is on any main navigation screen
- THEN The MiniPlayer MUST be visible at the bottom of the screen above the BottomNavigationBar
- AND The user MUST be able to tap it to expand to full PlayerScreen

### Requirement: PlayerScreen as Expanded State

The system MUST display the PlayerScreen as the expanded state of the BottomSheet.

When the bottom sheet is in Expanded state:
- The full PlayerScreen content MUST be visible
- The navigation content should be hidden or obscured

#### Scenario: PlayerScreen Expansion

- GIVEN The MiniPlayer is visible (song playing)
- WHEN The user swipes up or taps the MiniPlayer
- THEN The bottom sheet MUST expand to show the full PlayerScreen
- AND The user MUST be able to swipe down to collapse back to MiniPlayer

### Requirement: Gesture Handling

The system MUST support native swipe gestures for bottom sheet interaction.

- Swipe up: Expand from Collapsed to Expanded
- Swipe down: Collapse from Expanded to Collapsed
- Tap on MiniPlayer: Expand to full PlayerScreen
- Tap back while Expanded: Collapse to MiniPlayer

#### Scenario: Swipe to Expand

- GIVEN The MiniPlayer is visible
- WHEN The user performs a swipe up gesture
- THEN The bottom sheet MUST animate to the Expanded state
- AND The PlayerScreen becomes visible

#### Scenario: Swipe to Collapse

- GIVEN The PlayerScreen is visible (Expanded)
- WHEN The user performs a swipe down gesture
- THEN The bottom sheet MUST animate to the Collapsed state
- AND The MiniPlayer becomes visible

### Requirement: Back Navigation

The system MUST handle back navigation appropriately when the player is expanded.

- When in Expanded state: Back button SHOULD collapse to Collapsed
- When in Collapsed state: Back button SHOULD close the app (existing behavior)

#### Scenario: Back from Expanded Player

- GIVEN The PlayerScreen is visible (Expanded)
- WHEN The user presses the back button
- THEN The bottom sheet MUST collapse to the Collapsed state
- AND The navigation content becomes visible again

## MODIFIED Requirements

### Requirement: Overlay System Removal

The current overlay system using Box with zIndex MUST be removed and replaced by BottomSheetScaffold.

(Previously: PlayerScreen, Equalizer, and Settings used Box overlays with zIndex values)

#### Scenario: No zIndex Overlays

- GIVEN The BottomSheet integration is complete
- WHEN The app is rendered
- THEN No Box overlays with zIndex for player functionality SHOULD exist
- AND The BottomSheetScaffold MUST handle all player state transitions

### Requirement: PlayerViewModel State

The PlayerViewModel MUST expose BottomSheet state instead of isPlayerScreenVisible boolean.

(Previously: isPlayerScreenVisible (Boolean) controlled overlay visibility)

#### Scenario: ViewModel State Conversion

- GIVEN The PlayerViewModel
- WHEN The state is observed by the UI
- THEN The state MUST reflect BottomSheet values (PartiallyExpanded, Expanded, Hidden)
- AND NOT a simple boolean for overlay visibility

## REMOVED Requirements

### Requirement: zIndex-based Overlay System

The system MUST NOT use zIndex-based overlays for player functionality.

(Reason: Replaced by BottomSheetScaffold with native gesture and animation support)

### Requirement: Manual Visibility Control

The system MUST NOT require manual zIndex management for the player UI layers.

(Reason: BottomSheetScaffold manages visibility and layering natively)

## Implementation Notes

- The BottomSheetScaffold should use `rememberStandardBottomSheetState` for state management
- The initial state should be PartiallyExpanded when a song is playing, Hidden otherwise
- The sheetPeekHeight should be configured to show the MiniPlayer appropriately
- Equalizer and Settings may remain as overlays or be integrated into the BottomSheet pattern
