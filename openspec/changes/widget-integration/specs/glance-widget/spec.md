# Delta for Glance Widget

> **STATUS: OBSOLETE (2026-10-23).** Never applied. The shipping widget uses classic
> `android.widget.RemoteViews`, not Glance. See `../../state.yaml`.

## Purpose

Home-screen Glance AppWidget rendering current playback state and accepting transport commands. Widget reads cold-start data from `AppPrefs` and receives live updates via `PlayerWidget().updateAll(this)` throttled to 1s.

## ADDED Requirements

### Requirement: Widget renders on first placement from AppPrefs

The system MUST display the last-known playback state on cold-start by reading `loadLastSongUri()`, `loadPlaybackPosition()`, and `loadIsPlaying()` from `AppPrefs` inside `provideGlance`.

#### Scenario: Cold-start renders last-known state

- GIVEN the widget is placed on the home screen for the first time
- AND `AppPrefs` contains a previously saved song URI, position, and playing state
- WHEN `provideGlance` is invoked
- THEN the widget renders the last-known artwork, title, artist, and transport state

#### Scenario: Cold-start with no prior state

- GIVEN the widget is placed on the home screen
- AND `AppPrefs` has no saved playback data
- WHEN `provideGlance` is invoked
- THEN the widget renders with default/fallback artwork (`R.drawable.ic_launcher_foreground`) and idle state

### Requirement: Widget updates on playback state changes

The system MUST update the widget within 1 second of any play/pause/next/prev state change via `PlayerWidget().updateAll(this)` called from the `playerController.state.collect` block in `MusicService`.

#### Scenario: Play/pause toggle updates widget

- GIVEN the widget is visible on the home screen
- WHEN the user taps the play/pause button in the widget
- AND `MusicService` dispatches `ACTION_PLAY_PAUSE`
- THEN `playerController.state` emits a new state
- AND `PlayerWidget().updateAll(this)` is called within 1s
- AND the widget reflects the new playing state

#### Scenario: Next/prev track changes artwork and metadata

- GIVEN the widget is visible
- WHEN the user taps next or previous
- AND `MusicService` dispatches `ACTION_NEXT` or `ACTION_PREV`
- THEN the widget updates artwork, title, and artist within 1s

### Requirement: Widget displays artwork, title, artist, and transport controls

The system MUST render album artwork, song title, artist name, and four transport buttons (prev, play/pause, next) using `glance-material3` components.

#### Scenario: Playing state shows pause button

- GIVEN `isPlaying` is true
- WHEN the widget renders
- THEN the transport controls show the pause icon

#### Scenario: Paused state shows play button

- GIVEN `isPlaying` is false
- WHEN the widget renders
- THEN the transport controls show the play icon

### Requirement: Widget uses dark-theme styling matching the app

The system MUST apply dark-theme Material3 styling: background black, surface `#1A1A1A`, `md_primary = #2196F3`, and `Icons.Rounded`.

#### Scenario: Themed widget rendering

- GIVEN the widget is rendered
- WHEN dark-theme styling is applied
- THEN background is black, surface is `#1A1A1A`, primary accent is `#2196F3`, and icons use `Icons.Rounded`

### Requirement: Tapping artwork or controls opens MainActivity

The system MUST navigate to `MainActivity` when the user taps the album artwork or any transport control button.

#### Scenario: Tapping artwork opens MainActivity

- GIVEN the user taps the album artwork in the widget
- WHEN the click action is triggered
- THEN `MainActivity` is launched via the appropriate Glance `ActionCallback`

#### Scenario: Tapping transport control opens MainActivity

- GIVEN the user taps play/pause, next, or prev
- WHEN the click action is triggered
- THEN `MainActivity` is launched AND the corresponding command is dispatched to `MusicService`

### Requirement: Widget persists artwork URI for re-render after process death

The system MUST derive the artwork thumbnail from `loadLastSongUri()` via `ArtworkLoader.loadThumbnail` so the widget can re-render correct artwork after process death.

#### Scenario: Process death recovery

- GIVEN the app process was killed while the widget was visible
- WHEN the widget is re-rendered after process death
- THEN `provideGlance` reads `loadLastSongUri()` from `AppPrefs`
- AND `ArtworkLoader.loadThumbnail` produces the correct `Bitmap`
- AND the widget displays the recovered artwork

## Dependencies

- `androidx.glance:glance-appwidget:1.1.1`
- `androidx.glance:glance-material3:1.1.1`
- `kotlinCompilerExtensionVersion = "1.5.15"` in `composeOptions`

## New Files

- `widget/PlayerWidget.kt` — Glance AppWidget composable
- `widget/PlayerWidgetActions.kt` — `ActionCallback` subclasses for transport commands
- `res/xml/player_widget_info.xml` — AppWidget provider metadata
- `AndroidManifest.xml` — `<receiver>` registration

## Testing

- Unit test: `PlayPauseAction.onAction` starts `MusicService` with correct `ACTION_PLAY_PAUSE` intent
- Unit test: `provideGlance` reads `AppPrefs` correctly for cold-start data
- Instrumented test: `glance-testing:1.1.1` validates widget composition and state updates
