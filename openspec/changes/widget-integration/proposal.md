# Proposal: Widget Integration

> **STATUS: OBSOLETE (2026-10-23).** This change never landed. The Glance-based widget
> was abandoned mid-migration; the shipping widget renders with classic
> `android.widget.RemoteViews` (`PlayerWidget.kt` + `AppWidgetProvider` receivers) and
> works. The `androidx.glance`, `androidx.work`, `androidx.room` and `androidx.datastore`
> dependencies below were removed as dead code, which also removed the unused
> `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` permissions from the
> merged manifest. Nothing in this document describes the current code.
> See `state.yaml` for the full rationale.

## Intent

Add home-screen app widgets so users can control music playback without opening the app. The user needs quick access to play/pause, next/previous, progress, and album artwork on the home screen. This extends the existing MediaSession infrastructure to a Glance-based AppWidget, leveraging already-persisted state in `AppPrefs`.

## Scope

### In Scope
- Glance AppWidget with play/pause/next/prev buttons, progress bar, and album artwork
- State observation from `AppPrefs` for cold-start data (no separate DataStore)
- Command dispatch via existing `MusicService` intent constants (zero service logic changes)
- Seek interaction via ±10s forward/backward buttons (resolves Glance `LinearProgressIndicator` non-interactivity)
- Artwork display via `ImageProvider(Bitmap)` with fallback to `R.drawable.ic_launcher_foreground`
- State push via `PlayerWidget().updateAll(this)` from `MusicService`, throttled to 1s

### Out of Scope
- Lockscreen controls (already handled by MediaSession layer; `onSeekTo` fix enables this)
- Full widget customization (sizes, themes, configuration)
- Notification modifications (existing `NotificationUtil` shell remains separate)

## Capabilities

### New Capabilities
- `glance-widget`: Home-screen AppWidget rendering playback state and accepting transport commands
- `widget-seek`: Seek interaction via ±10s buttons since Glance progress bar is non-interactive

### Modified Capabilities
- `music-service`: `onStartCommand` reuses existing intent constants for widget commands; `onSeekTo(pos)` callback added to `MediaSessionCompat.Callback`
- `player-controller`: `state.collect` block extended to call `PlayerWidget().updateAll(this)` for widget state push

## Approach

1. **Dependencies**: Add `androidx.glance:glance-appwidget:1.1.1`, `androidx.glance:glance-material3:1.1.1`, `kotlinCompilerExtensionVersion = "1.5.15"` in composeOptions.
2. **State observation**: `provideGlance` reads from `AppPrefs` (already persists `loadLastSongUri()`, `loadPlaybackPosition()`, `loadIsPlaying()`). No separate DataStore.
3. **Command dispatch**: Glance `ActionCallback` subclasses launch `MusicService` via `context.startService(Intent(MusicService).apply { action = ACTION_PLAY_PAUSE/NEXT/PREV })`. Reuses 4 existing intent constants — zero changes to service logic.
4. **Artwork**: `ImageProvider(Bitmap)` accepts runtime `albumArt: Bitmap?`; fallback to `R.drawable.ic_launcher_foreground` when null.
5. **State push**: `PlayerWidget().updateAll(this)` from `MusicService` inside existing `playerController.state.collect`, throttled to 1s.
6. **Seek gap**: ±10s forward/backward buttons recommended over tap-to-seek or opening PlayerScreen.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modified | Add Glance dependencies and compiler extension version |
| `widget/PlayerWidget.kt` | New | Glance AppWidget composable |
| `widget/WidgetActionCallback.kt` | New | ActionCallback handling transport commands |
| `widget/WidgetStateObserver.kt` | New | State observation from AppPrefs |
| `service/MusicService.kt` | Modified | Add `updateAll` call in `state.collect` block |
| `AndroidManifest.xml` | Modified | Register AppWidget provider |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Seek interaction ambiguity (±10s vs tap-to-seek) | Medium | Recommend ±10s buttons; final decision in spec phase |
| `updateAll` from Service context | Low | Same pattern as notification; verified safe |
| Bitmap memory budget for album art | Low | 512px art reused from existing pipeline; Glance handles caching |

## Rollback Plan

1. Remove Glance dependencies from `build.gradle.kts`
2. Delete `widget/` package files
3. Revert `MusicService.kt` changes to `state.collect` block
4. Remove AppWidget provider from `AndroidManifest.xml`

## Dependencies

- `androidx.glance:glance-appwidget:1.1.1`
- `androidx.glance:glance-material3:1.1.1`
- `kotlinCompilerExtensionVersion = "1.5.15"` in composeOptions

## Success Criteria

- [ ] Home-screen widget displays album art, progress, and transport controls
- [ ] Play/pause, next, prev buttons dispatch commands to MusicService
- [ ] Seek buttons advance/retreat 10s
- [ ] Widget updates within 1s of player state change
- [ ] Cold-start shows last known state from AppPrefs
- [ ] All tests pass (`./gradlew test` / `./gradlew connectedAndroidTest`)
