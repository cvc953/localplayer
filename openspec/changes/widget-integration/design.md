# Design: Widget Integration

> **STATUS: OBSOLETE (2026-10-23).** This design was never implemented. The shipping
> widget uses classic `android.widget.RemoteViews`, not Glance, and the Glance
> dependencies were removed. See `state.yaml`.

## Technical Approach

Add a Glance AppWidget that renders playback state and accepts transport commands. The widget is a **pure consumer** of `PlayerController.state` — it never owns state, only observes it. Commands flow back through `ActionCallback` → `MusicService.onStartCommand` → `PlayerController`, reusing all existing intent constants. Cold-start reads from `AppPrefs`; hot updates push via `PlayerWidget().updateAll(this)` from `MusicService`.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Widget framework | Glance 1.1.1 `GlanceAppWidget` | Custom `AppWidgetProvider` | Glance integrates with Compose, aligns with existing Jetpack Compose stack; `glance-material3` provides dark-theme components |
| State source for cold-start | `AppPrefs` (SharedPreferences) | Separate DataStore | `AppPrefs` already persists `loadLastSongUri()`, `loadPlaybackPosition()`, `loadIsPlaying()` — zero new infrastructure |
| State push mechanism | `PlayerWidget().updateAll(this)` from `MusicService` | `BroadcastReceiver` + `AppWidgetManager` | `updateAll` is the idiomatic Glance pattern; mirrors existing notification update pattern in `MusicService` |
| Seek interaction | ±10s buttons | Tap-to-seek on progress bar | Glance `LinearProgressIndicator` is non-interactive; ±10s is confirmed UX decision |
| Command dispatch | `ActionCallback` → `startService(MusicService, ACTION_*)` | Direct `PendingIntent` to `PlayerController` | Reuses 4 existing intent constants (`ACTION_PLAY_PAUSE`, `ACTION_NEXT`, `ACTION_PREV`, `ACTION_UPDATE_STATE`) — zero service logic changes |
| Artwork pipeline | `ImageProvider(Bitmap)` from `ArtworkLoader.loadThumbnail` | Custom `RemoteViews` ImageView | Reuses existing `ArtworkLoader` with LruCache; `ImageProvider(Bitmap)` is the Glance-provided mechanism |
| Throttle interval | 1s | 500ms / 2s | 1s balances widget freshness against `MusicService` `state.collect` already throttling notification updates at 1s |

## Data Flow

### Cold-Start (Widget Placement)

```
AppPrefs (loadLastSongUri, loadPlaybackPosition, loadIsPlaying)
    │
    ▼
provideGlance(context, appWidgetManager, appWidgetId)
    │
    ▼
ArtworkLoader.loadThumbnail(context, uri, targetSizePx=256) → Bitmap?
    │
    ▼
MyContent() composes with GlanceTheme(dark, md_primary=#2196F3)
    │
    ▼
Widget renders: artwork, title, artist, progress, transport controls
```

### Hot Update (State Change)

```
User taps play/pause on widget
    │
    ▼
PlayPauseAction.onAction → startService(MusicService, ACTION_PLAY_PAUSE)
    │
    ▼
MusicService.onStartCommand → playerController.togglePlayPause()
    │
    ▼
playerController.state.emit(newState)  // StateFlow
    │
    ▼
MusicService.state.collect block → PlayerWidget().updateAll(this)  // throttled 1s
    │
    ▼
GlanceAppWidgetManager → provideGlance re-reads AppPrefs → render
```

### Seek Command Flow

```
Widget SeekBackward10 button
    │
    ▼
SeekBackward10Action.onAction → startService(MusicService, ACTION_SEEK_BACKWARD)
    │
    ▼
MusicService.onStartCommand → playerController.seekTo(max(0, position - 10_000))
    │
    ▼
playerController.state.update → updateAll → widget progress bar updates
```

### File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modify | Add `glance-appwidget`, `glance-material3`, `glance-testing`, `kotlinCompilerExtensionVersion = "1.5.15"` |
| `widget/PlayerWidget.kt` | Create | `GlanceAppWidget` subclass with `provideGlance` and `MyContent()` composable |
| `widget/PlayerWidgetActions.kt` | Create | `PlayPauseAction`, `NextAction`, `PrevAction`, `SeekBackward10Action`, `SeekForward10Action` — each extends `ActionCallback` |
| `res/xml/player_widget_info.xml` | Create | `<appwidget-provider>` metadata (minWidth, minHeight, updatePeriod, initialLayout) |
| `AndroidManifest.xml` | Modify | Add `<receiver>` for `GlanceAppWidgetReceiver` with `APPWIDGET_UPDATE` filter |
| `Services/MusicService.kt` | Modify | Add `ACTION_SEEK_BACKWARD`, `ACTION_SEEK_FORWARD` constants; handle them in `onStartCommand` via `playerController.seekTo(max(0, position - 10_000))` / `playerController.seekTo(min(duration, position + 10_000))`; add `PlayerWidget().updateAll(this)` inside `playerController.state.collect` block (throttled 1s) |
| `gradle/libs.versions.toml` | Modify | Add Glance version entries |

## Interfaces / Contracts

### PlayerWidget.kt

```kotlin
class PlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val prefs = AppPrefs(context)
        val lastSongUri = prefs.loadLastSongUri()
        val position = prefs.loadPlaybackPosition()
        val isPlaying = prefs.loadIsPlaying()

        // ArtworkLoader.loadThumbnail is suspend — call directly in provideGlance
        val bitmap = lastSongUri?.let { uri ->
            withContext(Dispatchers.IO) {
                ArtworkLoader.loadThumbnail(context, Uri.parse(uri), targetSizePx = 256)
            }
        }

        MyContent(
            isPlaying = isPlaying,
            position = position,
            duration = 0L, // Updated by state push; default for cold-start
            artwork = bitmap,
        )
    }
}

@Composable
fun MyContent(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    artwork: Bitmap?,
) {
    GlanceTheme(darkTheme = true, primaryColor = md_primary) {
        Column {
            Image(
                provider = artwork?.let { ImageProvider(it) }
                    ?: ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = "Album Art",
                modifier = GlanceModifier.size(256.dp),
            )
            Text("Reproduciendo", style = MaterialTheme.typography.titleMedium)
            Text("", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
            )
            Row {
                Button(actionRunCallback<PrevAction>) { Text("‹") }
                Button(actionRunCallback<PlayPauseAction>) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.Play,
                        if (isPlaying) "Pause" else "Play",
                    )
                }
                Button(actionRunCallback<NextAction>) { Text("›") }
            }
            Row {
                Button(actionRunCallback<SeekBackward10Action>) { Text("-10") }
                Button(actionRunCallback<SeekForward10Action>) { Text("+10") }
            }
        }
    }
}
```

### PlayerWidgetActions.kt

```kotlin
abstract class MusicServiceAction(
    private val action: String,
) : ActionCallback() {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.startService(
            Intent(context, MusicService::class.java).apply { this.action = action }
        )
    }
}

class PlayPauseAction : MusicServiceAction(MusicService.ACTION_PLAY_PAUSE)
class NextAction : MusicServiceAction(MusicService.ACTION_NEXT)
class PrevAction : MusicServiceAction(MusicService.ACTION_PREV)
class SeekBackward10Action : MusicServiceAction("ACTION_SEEK_BACKWARD")
class SeekForward10Action : MusicServiceAction("ACTION_SEEK_FORWARD")
```

Note: `ACTION_SEEK_BACKWARD` and `ACTION_SEEK_FORWARD` need to be added to `MusicService.companion` constants and handled in `onStartCommand` with `playerController.seekTo()` calls. The `MusicService` modification includes both the `updateAll` call AND the new seek intent handling.

### player_widget_info.xml

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/player_widget"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
```

### AndroidManifest.xml Addition

```xml
<receiver
    android:name="androidx.glance.appwidget.GlanceAppWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/player_widget_info" />
</receiver>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `PlayPauseAction.onAction` starts `MusicService` with `ACTION_PLAY_PAUSE` | Mock `Context`; verify `startService` intent action equals `MusicService.ACTION_PLAY_PAUSE` |
| Unit | `SeekBackward10Action.onAction` starts `MusicService` with `ACTION_SEEK_BACKWARD` | Same mock strategy |
| Unit | `provideGlance` reads `AppPrefs` for cold-start state | Mock `AppPrefs`; verify `loadLastSongUri`, `loadPlaybackPosition`, `loadIsPlaying` called |
| Unit | `SeekBackward10Action` computes `max(0, position - 10_000)` | Test boundary: position=30_000 → 20_000; position=5_000 → 0 |
| Unit | `SeekForward10Action` computes `min(duration, position + 10_000)` | Test boundary: pos=30_000 dur=180_000 → 40_000; pos=175_000 dur=180_000 → 180_000 |
| Instrumented | Widget placement, state updates, `updateAll` triggers | `androidTestImplementation("androidx.glance:glance-testing:1.1.1")`; place widget on emulator; trigger `playerController.state` changes; verify widget updates within 1s |
| Instrumented | Artwork recovery after process death | Kill app process; re-render widget; verify `ArtworkLoader.loadThumbnail` produces correct `Bitmap` from `loadLastSongUri()` |

**Mock strategy**: Mock `Context`, `AppPrefs`, `ArtworkLoader` for unit tests. Use `glance-testing` `GlanceTestRule` for instrumented tests.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. The widget interacts only with `MusicService` via standard Android `Intent`/`Service` mechanisms and `AppWidgetManager` system calls.

## Migration / Rollout

No migration required. The widget is additive — existing `MusicService`, `PlayerController`, and `AppPrefs` remain unchanged except for the `updateAll` call addition. Rollback plan from proposal: remove Glance dependencies, delete `widget/` package, revert `MusicService.kt`, remove manifest receiver.

## Open Questions

- [ ] `ACTION_SEEK_BACKWARD` and `ACTION_SEEK_FORWARD` constants need to be added to `MusicService.companion` — not yet confirmed in codebase
- [ ] Whether `MusicService` needs to handle the seek intent actions in `onStartCommand` — currently `ACTION_SEEK_*` are not in the existing `when` block
- [ ] Glance `ImageProvider` requires a `Context`-based `Bitmap` provider — need to verify the exact Glance API for passing `Bitmap` to `ImageProvider`
- [ ] Whether `AppPrefs` needs `saveAlbumArtUri`/`loadAlbumArtUri` — decision: derive from `loadLastSongUri()` via `ArtworkLoader.loadThumbnail`, so no new AppPrefs entries needed
