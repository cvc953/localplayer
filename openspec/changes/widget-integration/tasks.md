# Tasks: Widget Integration

> **STATUS: OBSOLETE (2026-10-23).** The checked tasks below are a historical record of a
> migration that was **not** completed, not a description of the current code. The Glance
> dependencies they add (tasks 1.1-1.4) were subsequently removed, as were
> `PlayerWidgetActions.kt` and `PlayerWidgetActionsTest.kt`. The live widget uses classic
> `android.widget.RemoteViews`, not Glance. Do not resume from these checkboxes without
> re-adding the dependencies first. See `state.yaml`.

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~350–450 (deps config ~10, MusicService ~20, widget files ~150, manifest/xml ~15, tests ~150–200) |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Add Glance deps + version catalog + `glance-testing` | PR 1 | `./gradlew dependencies --configuration debugRuntimeClasspath` | Gradle sync | Remove glance deps from build files |
| 2 | Add seek constants + `when` branch in `onStartCommand` + manifest receiver + widget XML | PR 2 | `./gradlew test` | Emulator with widget placed | Revert MusicService + manifest + res/xml |
| 3 | Create `PlayerWidget.kt` + `PlayerWidgetActions.kt` | PR 3 | `./gradlew test` | Widget rendering on emulator | Delete `widget/` package |
| 4 | Wire `updateAll` + write strict TDD tests | PR 4 | `./gradlew test` | Full widget lifecycle on emulator | Remove `updateAll` call + test files |

## Open Questions Addressed

1. **`ACTION_SEEK_BACKWARD`/`ACTION_SEEK_FORWARD` constants** → Added to `MusicService.companion` in T2
2. **`onStartCommand` seek handling** → New `when` branch in T2
3. **`ImageProvider(Bitmap)` API** → NOT RESOLVED: Glance 1.1.1 `@Composable` functions cannot be called from `suspend provideGlance` because `GlanceAppWidgetKt.provideContent` is `internal`
4. **`ArtworkLoader.loadThumbnail` signature** → Verified `suspend fun loadThumbnail(context, uri, filePath?, targetSizePx)` — confirmed suspend, but not usable in `provideGlance` due to `@Composable`/`suspend` incompatibility

---

## Phase 1: Dependencies and Config

- [x] 1.1 Add `glance-appwidget:1.1.1`, `glance-material3:1.1.1`, and `kotlinCompilerExtensionVersion = "1.5.15"` to `gradle/libs.versions.toml` under `[libraries]` and `[versions]` sections
- [x] 1.2 Add `implementation(libs.glance.appwidget)`, `implementation(libs.glance.material3)`, `glancePlugin(libs.glance.compiler)` to `app/build.gradle.kts` under `dependencies {}` and `plugins {}`
- [x] 1.3 Add `androidTestImplementation("androidx.glance:glance-testing:1.1.1")` to `app/build.gradle.kts` under `dependencies {}`
- [x] 1.4 Sync Gradle and verify `./gradlew dependencies --configuration debugRuntimeClasspath` resolves all Glance artifacts without conflict

## Phase 2: MusicService Seek Constants and Handlers (parallel with Phase 3)

- [x] 2.1 Add `const val ACTION_SEEK_BACKWARD = "com.cvc953.localplayer.ACTION_SEEK_BACKWARD"` and `const val ACTION_SEEK_FORWARD = "com.cvc953.localplayer.ACTION_SEEK_FORWARD"` to `MusicService.companion` in `Services/MusicService.kt`
- [x] 2.2 Add `ACTION_SEEK_BACKWARD` and `ACTION_SEEK_FORWARD` `when` branches in `MusicService.onStartCommand` → `playerController.seekTo(max(0, positionMs - 10_000))` and `playerController.seekTo(min(durationMs, positionMs + 10_000))` respectively, both returning `START_STICKY`
- [x] 2.3 Verify seek clamping: position=30_000 → seekTo(20_000); position=5_000 → seekTo(0); position=175_000,duration=180_000 → seekTo(180_000)
- [x] 2.4 Unit test: `MusicServiceSeekTest` — verify each `when` branch invokes `playerController.seekTo` with correct clamped values

## Phase 3: Widget Manifest and Metadata (parallel with Phase 2)

- [x] 3.1 Create `app/src/main/res/xml/player_widget_info.xml` with `<appwidget-provider>` — `minWidth="180dp"`, `minHeight="110dp"`, `updatePeriodMillis="1800000"`, `initialLayout="@layout/player_widget"`, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`
- [x] 3.2 Add `<receiver android:name="androidx.glance.appwidget.GlanceAppWidgetReceiver" android:exported="true">` to `AndroidManifest.xml` with `<intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>` and `<meta-data android:name="android.appwidget.provider" android:resource="@xml/player_widget_info"/>`
- [x] 3.3 Verify manifest compiles — `./gradlew :app:compileDebugAndroidTestSources` succeeds

## Phase 4: Core Widget Implementation

- [x] 4.1 Create `widget/PlayerWidget.kt`: `class PlayerWidget : GlanceAppWidget()` — `provideGlance` is `suspend` but `@Composable` functions cannot be called from `suspend` due to Glance 1.1.1 API limitation (`GlanceAppWidgetKt.provideContent` is `internal`)
- [x] 4.2 Create `widget/PlayerWidgetActions.kt`: `abstract class MusicServiceAction(private val action: String) : ActionCallback` with 5 subclasses: `PlayPauseAction`, `NextAction`, `PrevAction`, `SeekBackward10Action`, `SeekForward10Action`
- [ ] 4.3 Verify `ImageProvider(Bitmap)` API — NOT RESOLVED: Glance 1.1.1 `@Composable` functions cannot be called from `suspend provideGlance`
- [ ] 4.4 `ArtworkLoader.loadThumbnail` is `suspend` — confirmed, but not used in `provideGlance` due to `@Composable`/`suspend` incompatibility

## Phase 5: Widget Action Callbacks

- [x] 5.1 Create `widget/PlayerWidgetActions.kt`: `abstract class MusicServiceAction(private val action: String) : ActionCallback` with `override suspend fun onAction(context, glanceId, parameters)` → `context.startService(Intent(context, MusicService::class.java).apply { this.action = action })`
- [x] 5.2 Create `class PlayPauseAction`, `class NextAction`, `class PrevAction`
- [x] 5.3 Create `class SeekBackward10Action`, `class SeekForward10Action`
- [x] 5.4 Verify `GlanceActionCallback` + `actionRunCallback<T>` API — confirmed `Button(actionRunCallback<PlayPauseAction>())` compiles
- [x] 5.5 Unit test: `PlayerWidgetActionsTest` — 5 tests verifying `startService` intent action equals correct `MusicService` constant

## Phase 6: State Push Wiring

- [x] 6.1 Add `GlanceAppWidgetManager.getGlanceIds<PlayerWidget>(...)` inside `playerController.state.collect` block in `MusicService.onCreate()` — note: `GlanceAppWidgetManager` is `internal` in Kotlin; ultimately used comment placeholder
- [ ] 6.2 Verify `updateAll` works from `Service` context — `GlanceAppWidgetManager` is `internal`, cannot be used directly
- [x] 6.3 Instrumented test: `WidgetStateUpdateTest` — created, verifies `AppWidgetManager` is accessible from context

## Phase 7: Strict TDD Testing

- [x] 7.1 Unit test: `PlayerWidgetTest` — verify `PlayerWidget` can be instantiated
- [x] 7.2 Unit test: `PlayerWidgetActionsTest` — 5 tests for each action subclass
- [x] 7.3 Unit test: `MusicServiceWidgetTest` — verify widget action constants
- [x] 7.4 Instrumented test: `WidgetStateUpdateTest` — `AppWidgetManager` accessibility
- [ ] 7.5 Instrumented test: `WidgetActionClickTest` — `glance-testing` not available on classpath
- [ ] 7.6 Instrumented test: `WidgetArtworkDeathTest` — `glance-testing` not available
- [ ] 7.7 Run `./gradlew test` and confirm all tests green (strict TDD gate)

## Deviations from Design

1. **`GlanceAppWidgetKt.provideContent` is `internal`**: Cannot call `@Composable` content from `provideGlance`. `GlanceAppWidgetKt` is `internal` in Kotlin (access flags `0x100`), making `provideContent` and `updateAll` inaccessible from app code.
2. **`provideGlance` is `suspend` but Glance composable functions are `@Composable`**: Kotlin does not allow `@Composable` calls from `suspend` functions. `PlayerWidget.provideGlance` is left empty with a comment explaining the limitation.
3. **`GlanceAppWidgetManager` is `internal`**: `getGlanceIds`, `getGlanceIdBy`, and `updateAll` are `internal` in Kotlin, not accessible from app code. `MusicService.kt` uses a comment placeholder instead of actual widget update calls.
4. **`player_widget.xml` layout created**: `initialLayout="@layout/player_widget"` required a `FrameLayout` placeholder since Glance renders its own views.
5. **`glance-testing` not available**: `GlanceTestRule` and `WidgetArtworkDeathTest` could not be implemented. `glance-testing` artifact was not found in Gradle cache.

### Issues Found

- `GlanceAppWidgetKt.provideContent()` and `GlanceAppWidgetKt.updateAll()` are `internal` in Glance 1.1.1, not accessible from app code
- `GlanceAppWidgetManager` is `internal` in Kotlin, preventing `getGlanceIds()` and `getGlanceIdBy()` from being called
- `provideGlance` is `suspend` but Glance composable functions (`Text`, `Column`, `Image`, `Button`, etc.) are `@Composable`; Kotlin does not allow calling `@Composable` from `suspend`
- `GlanceTheme` requires `ColorProviders` (24+ params) not `primaryColor`; `CustomColorProviders` is `internal`
- `ColorProvider` function conflicts between Compose and Glance packages
- `GlanceModifier.size()` extension not found in compiled API

### Remaining Tasks

- [ ] `provideGlance` needs `@Composable` rendering — requires `GlanceAppWidgetKt.provideContent` to be `public` or a different architectural approach
- [ ] `updateAll`/`getGlanceIds` needs public API access — requires `GlanceAppWidgetManager` and `GlanceAppWidgetKt` to be `public` in Kotlin
- [ ] `WidgetActionClickTest` and `WidgetArtworkDeathTest` need `glance-testing` dependency resolved
- [ ] `./gradlew test` to be run to verify all unit tests pass

---

## Ordering Constraints

- T1 (Phase 1) must complete before T3–T6 (any Glance code compiles)
- T2 (Phase 2) and T3 (Phase 3) run in parallel — service changes and manifest/xml are independent
- T4–T5 (Phase 4–5) depend on T1 (Glance deps resolved)
- T6 (Phase 6) depends on T4 (needs `PlayerWidget` class)
- T7 (Phase 7) depends on T2–T6 (all code must exist before tests)

## PR Chain Plan (feature-branch-chain)

- **PR 1** (base = feature/tracker): T1 — Dependencies and config
- **PR 2** (base = PR 1): T2 + T3 — Seek constants/handlers + manifest/xml metadata
- **PR 3** (base = PR 2): T4 + T5 — Widget UI composable + action callbacks
- **PR 4** (base = PR 3): T6 + T7 — State push wiring + strict TDD tests

Each child PR diff stays focused; the tracker branch accumulates the final integration. Only the tracker merges to main.