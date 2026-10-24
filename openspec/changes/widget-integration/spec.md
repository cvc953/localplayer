# Delta for Widget Integration

> **STATUS: OBSOLETE (2026-10-23).** This delta was never applied. The shipping widget
> uses classic `android.widget.RemoteViews`, not Glance. See `../state.yaml`.

## Purpose

Home-screen Glance AppWidget with playback state rendering, transport commands, and ±10s seek interaction.

## Capability 1: glance-widget

### ADDED Requirements

#### Requirement: Widget renders on first placement from AppPrefs

The system MUST display the last-known playback state on cold-start by reading `loadLastSongUri()`, `loadPlaybackPosition()`, and `loadIsPlaying()` from `AppPrefs` inside `provideGlance`.

- **Scenario: Cold-start renders last-known state** — GIVEN the widget is placed on the home screen for the first time AND `AppPrefs` contains saved playback data WHEN `provideGlance` is invoked THEN the widget renders the last-known artwork, title, artist, and transport state.
- **Scenario: Cold-start with no prior state** — GIVEN `AppPrefs` has no saved playback data WHEN `provideGlance` is invoked THEN the widget renders with fallback artwork and idle state.

#### Requirement: Widget updates on playback state changes

The system MUST update the widget within 1 second of any play/pause/next/prev state change via `PlayerWidget().updateAll(this)` from the `playerController.state.collect` block in `MusicService`.

- **Scenario: Play/pause toggle updates widget** — GIVEN the widget is visible WHEN the user taps play/pause AND `MusicService` dispatches `ACTION_PLAY_PAUSE` THEN `PlayerWidget().updateAll(this)` is called within 1s AND the widget reflects the new state.

#### Requirement: Widget displays artwork, title, artist, and transport controls

The system MUST render album artwork, song title, artist name, and four transport buttons using `glance-material3` components.

- **Scenario: Playing shows pause icon** — GIVEN `isPlaying` is true WHEN the widget renders THEN the transport controls show the pause icon.

#### Requirement: Widget uses dark-theme styling matching the app

The system MUST apply dark-theme Material3 styling: background black, surface `#1A1A1A`, `md_primary = #2196F3`, and `Icons.Rounded`.

- **Scenario: Themed widget rendering** — WHEN dark-theme styling is applied THEN background is black, surface is `#1A1A1A`, primary accent is `#2196F3`, and icons use `Icons.Rounded`.

#### Requirement: Tapping artwork or controls opens MainActivity

The system MUST navigate to `MainActivity` when the user taps album artwork or any transport control button.

- **Scenario: Tapping artwork opens MainActivity** — GIVEN the user taps the album artwork WHEN the click action is triggered THEN `MainActivity` is launched.

#### Requirement: Widget persists artwork URI for re-render after process death

The system MUST derive the artwork thumbnail from `loadLastSongUri()` via `ArtworkLoader.loadThumbnail` for recovery after process death.

- **Scenario: Process death recovery** — GIVEN the app process was killed WHEN the widget is re-rendered THEN `provideGlance` reads `loadLastSongUri()` from `AppPrefs` AND `ArtworkLoader.loadThumbnail` produces the correct `Bitmap` AND the widget displays the recovered artwork.

## Capability 2: widget-seek

### UX Decision (Confirmed)

`LinearProgressIndicator` in Glance is non-interactive. The accepted resolution is **±10s seek buttons** (Previous 10s / Next 10s).

### ADDED Requirements

#### Requirement: Seek backward by 10 seconds

The system MUST support a "seek-back-10" action calling `playerController.seekTo(max(0, position - 10_000))`.

- **Scenario: Seek backward within bounds** — GIVEN position is 30_000 ms WHEN seek-back-10 is tapped THEN `playerController.seekTo(20_000)` is called.
- **Scenario: Seek backward clamps at zero** — GIVEN position is 5_000 ms WHEN seek-back-10 is tapped THEN `playerController.seekTo(0)` is called.

#### Requirement: Seek forward by 10 seconds

The system MUST support a "seek-forward-10" action calling `playerController.seekTo(min(duration, position + 10_000))`.

- **Scenario: Seek forward within bounds** — GIVEN position is 30_000 ms and duration is 180_000 ms WHEN seek-forward-10 is tapped THEN `playerController.seekTo(40_000)` is called.
- **Scenario: Seek forward clamps at duration** — GIVEN position is 175_000 ms and duration is 180_000 ms WHEN seek-forward-10 is tapped THEN `playerController.seekTo(180_000)` is called.

#### Requirement: Widget progress bar reflects current position and duration

The system MUST display a `LinearProgressIndicator` whose progress is derived from `position / duration` from `PlayerController.state`.

- **Scenario: Progress bar updates** — GIVEN `position = 45_000`, `duration = 180_000` WHEN the widget renders THEN the progress bar shows 25%.

## Dependencies

- `androidx.glance:glance-appwidget:1.1.1`
- `androidx.glance:glance-material3:1.1.1`
- `kotlinCompilerExtensionVersion = "1.5.15"`

## Testing

- Strict TDD: `./gradlew test` passes
- Unit tests for command dispatch (`PlayPauseAction`, `SeekBackward10Action`, `SeekForward10Action`)
- Unit tests for state observation (`provideGlance` reads `AppPrefs`)
- `androidx.glance:glance-testing:1.1.1` for instrumented widget tests
