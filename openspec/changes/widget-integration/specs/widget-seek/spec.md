# Delta for Widget Seek

> **STATUS: OBSOLETE (2026-10-23).** Never applied. The shipping widget seeks through
> `MusicService` intents dispatched by `RemoteViews` PendingIntents; there is no Glance
> `LinearProgressIndicator` in the app. See `../../state.yaml`.

## Purpose

Seek interaction in the Glance widget. Because `LinearProgressIndicator` is non-interactive in Glance, seek is implemented via ±10s forward/backward buttons instead of tap-to-seek.

## UX Decision (Confirmed)

`LinearProgressIndicator` in Glance is non-interactive. The accepted resolution is **±10s seek buttons** (Previous 10s / Next 10s). This is confirmed and specified as the seek mechanism.

## ADDED Requirements

### Requirement: Seek backward by 10 seconds

The system MUST support a "seek-back-10" action that calls `playerController.seekTo(max(0, position - 10_000))` when tapped.

#### Scenario: Seek backward within bounds

- GIVEN the current position is 30_000 ms
- WHEN the user taps the seek-back-10 button
- THEN `playerController.seekTo(20_000)` is called

#### Scenario: Seek backward clamps at zero

- GIVEN the current position is 5_000 ms
- WHEN the user taps the seek-back-10 button
- THEN `playerController.seekTo(0)` is called (clamped by `max(0, ...)`)

### Requirement: Seek forward by 10 seconds

The system MUST support a "seek-forward-10" action that calls `playerController.seekTo(min(duration, position + 10_000))` when tapped.

#### Scenario: Seek forward within bounds

- GIVEN the current position is 30_000 ms and duration is 180_000 ms
- WHEN the user taps the seek-forward-10 button
- THEN `playerController.seekTo(40_000)` is called

#### Scenario: Seek forward clamps at duration

- GIVEN the current position is 175_000 ms and duration is 180_000 ms
- WHEN the user taps the seek-forward-10 button
- THEN `playerController.seekTo(180_000)` is called (clamped by `min(duration, ...)`)

### Requirement: Widget progress bar reflects current position and duration

The system MUST display a `LinearProgressIndicator` whose progress is derived from `position / duration` provided by `PlayerController.state`.

#### Scenario: Progress bar updates with playback position

- GIVEN `PlayerController.state` emits `position = 45_000`, `duration = 180_000`
- WHEN the widget renders
- THEN the progress bar shows 25% progress

#### Scenario: Progress bar at start

- GIVEN `position = 0`
- WHEN the widget renders
- THEN the progress bar shows 0%

## New Files

- `widget/PlayerWidgetActions.kt` — contains `SeekBackward10Action` and `SeekForward10Action` `ActionCallback` subclasses

## Testing

- Unit test: `SeekBackward10Action.onAction` calls `playerController.seekTo(max(0, position - 10_000))`
- Unit test: `SeekForward10Action.onAction` calls `playerController.seekTo(min(duration, position + 10_000))`
- Unit test: progress bar correctly computes `position / duration` ratio
