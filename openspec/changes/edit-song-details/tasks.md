# Tasks: Edit Song & Album Details

## Dependency Graph

```
T1 (TagWriter) ──► T2 (SongRepository) ──► T9 (SongController) ──► T3 (SongEditViewModel) ──► T4 (SongEditScreen)
                                                      │                                              │
                                                      │                                    T10 (Navigation) ◄──┘
                                                      │                                              │
                                                      └──► T6 (AlbumEditViewModel) ──► T7 (AlbumEditScreen)
                                                                                            │
                                                                                     T10 (Navigation) ◄──┘
                                                                                              │
                                                                                    ┌─────────┴─────────┐
                                                                                    │                   │
                                                                               T5 (PlayerScreen)   T8 (AlbumDetailScreen)

T11 (Strings) — no deps, can be done in parallel with any phase
```

## Tasks

---

### Phase 1: Foundation

---

#### T1: Create `util/TagWriter.kt`

| Field | Value |
|-------|-------|
| **Title** | Core tag writing utility with SAF fallback |
| **Description** | Create `object TagWriter` that wraps `AudioFileIO.write()` with API-level-aware write paths. Supports two write strategies: **(1) Direct file write (API < 29)**: `AudioFileIO.read(File)` → modify tag fields → `AudioFileIO.write(File)`. **(2) SAF copy-write (API ≥ 29)**: copy `File(filePath)` → `tempFile` in `context.cacheDir`, edit via `AudioFileIO.write(tempFile)`, write back via `ContentResolver.openOutputStream(song.uri)`, delete temp file. Handle `setArtworkData(tag, mimeType, coverData)` for format-aware artwork embedding (APIC for MP3, covr for M4A, Vorbis comment pictures for FLAC/OGG). Expose `data class TagWriteInput(title?, artist?, album?, genre?, year?, trackNumber?, discNumber?, coverArt?)` with nullable fields (null = keep existing). Return `Result<TagWriteResult(filePath, success, error?)>.` Handle errors: file locked, corrupt file, disk full, unsupported format. |
| **Files** | **Create:** `app/src/main/java/com/cvc953/localplayer/util/TagWriter.kt` |
| **Dependencies** | None |
| **Verification** | 1. Build succeeds with `TagWriter` importable from other packages. 2. Unit test verifies `TagWriter.writeTags()` sets fields correctly on a temporary MP3 copy (mocked `AudioFileIO`). 3. Unit test verifies `embedCoverArt()` writes correct frame types for MP3 vs M4A vs FLAC. |

**Key implementation details:**
- Mirror the `object` pattern from `EmbeddedLyricsExtractor`
- Accept `Context` as a parameter per-call (not constructor)
- `writeTags(context, filePath?, uri, input): Result<TagWriteResult>`
- `embedCoverArt(tag, mimeType, coverData)` — detect mime type from input to decide APIC vs covr vs Vorbis picture
- For API level check: `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`
- Import `org.jaudiotagger.audio.AudioFileIO` (already used in `EmbeddedLyricsExtractor`)
- Import `org.jaudiotagger.tag.*`, `org.jaudiotagger.tag.id3.*`, `org.jaudiotagger.tag.mp4.*`, `org.jaudiotagger.tag.flac.*`, `org.jaudiotagger.tag.vorbiscomment.*`
- Use `AudioFileIO.read()` to get the `Tag` object, modify fields via `tag.setField()`, then `AudioFileIO.write()`
- MIME-type-based dispatch: `input.mimeType` on `Song` to determine format
- SAF path: `context.cacheDir` for temp, `ContentResolver.openOutputStream(uri)` for write-back
- On IOException mid-write: preserve temp file in `context.cacheDir/edit-recovery/` for debugging

---

#### T2: Add write methods to `SongRepository`

| Field | Value |
|-------|-------|
| **Title** | Repository write support — `writeTags`, `updateSongInCache`, `scanSingleFile` |
| **Description** | Add three methods to `SongRepository`: **(1)** `writeTags(songUri, filePath, tags): Result<TagWriteResult>` — delegates to `TagWriter.writeTags()` with the song's URI and filePath. Accepts `TagWriteInput`. Returns the result from `TagWriter`. **(2)** `updateSongInCache(songId: Long, updates: TagWriteInput): Song?` — finds the song in the in-memory cache (the `_songs` list in the ViewModel or the internal `loadSongs()` results), creates a new `Song` with updated fields, re-saves the full cache to `songs_cache.json`. Returns the updated `Song` (or null if not found). Only writes non-null `updates` fields. **(3)** `scanSingleFile(filePath: String)` — calls `MediaScannerConnection.scanFile(context, filePath, null, null)` to update the MediaStore entry for the specific file. Companion to the existing `forceRescanSongs()` but for a single file. |
| **Files** | **Modify:** `app/src/main/java/com/cvc953/localplayer/model/SongRepository.kt` |
| **Dependencies** | T1 (`TagWriter` must exist) |
| **Verification** | 1. Build succeeds. 2. `updateSongInCache()` correctly serializes updated fields to `songs_cache.json` (verify with unit test using mock file output). 3. `writeTags()` delegates correctly to `TagWriter` (verify mock interaction). 4. `scanSingleFile()` calls `MediaScannerConnection.scanFile()` (verify with integration test or log assertion). |

**Key implementation details:**
- Import `android.media.MediaScannerConnection` for `scanSingleFile()`
- `updateSongInCache()` needs to:
  1. Load current songs via `loadSongsFromCache()` or use `loadSongs()` method
  2. Find matching song by ID
  3. Create updated `Song` copy: `song.copy(title = updates.title ?: song.title, ...)`
  4. Replace in list and call `saveSongsToCache()`
  5. Return the updated song
- Note: `Song` is a data class, so `copy()` with named args works
- `writeTags()` needs both `uri` (for SAF write-back on API 29+) and `filePath` (for direct write on < 29)
- Use `Build.VERSION.SDK_INT` check inside `writeTags()` to route to correct path
- Keep `TagWriteInput` and `TagWriteResult` imports from `TagWriter`

---

### Phase 2: Song Editing UI

---

#### T3: Create `SongEditViewModel`

| Field | Value |
|-------|-------|
| **Title** | Song edit form state and save orchestration |
| **Description** | Create `SongEditViewModel` as an `AndroidViewModel` subclass (following `SongViewModel` pattern). Responsibilities: **(1)** Load current song tags into form state when initialized. **(2)** Expose `SongEditFormState` as `StateFlow` — fields: `title`, `artist`, `album`, `genre`, `year`, `track`, `disc`, plus error states `titleError`, `yearError`. **(3)** Cover art state as `CoverArtState(currentBitmap?, selectedUri?, tempData?)` — load existing cover from file via `MediaMetadataRetriever.embeddedPicture` on init. **(4)** `onFieldChanged(field, value)` — update form state, clear errors on edit. **(5)** `onCoverSelected(uri: Uri)` — decode via `BitmapFactory.decodeStream` with `inSampleSize`, create scaled bitmap (max 1024×1024), compress to JPEG byte array, update cover art state. **(6)** `save(): Boolean` — validate (empty title → `titleError`, invalid year → `yearError`), if valid call `SongController.updateSongTags()` with current song id and form values + cover bytes. On success: update cache, trigger `MediaScannerConnection.scanFile()`, call `forceRescanSongs()`, emit `_saveComplete` event. On failure: emit `_saveError`. **(7)** Expose `_saveComplete` event as `SharedFlow` (one-shot) so the UI can navigate back on success. **(8)** Expose `coverArtError` for invalid image selection. |
| **Files** | **Create:** `app/src/main/java/com/cvc953/localplayer/viewmodel/SongEditViewModel.kt` |
| **Dependencies** | T9 (`SongController.updateSongTags()` must exist) |
| **Verification** | 1. Build succeeds. 2. Form state initializes with correct song values when `loadSong(songId)` is called. 3. Validation rejects empty title and invalid year. 4. Calling `save()` with valid data invokes controller method with correct arguments. 5. Cover art pipeline decodes and resizes correctly (verify output ≤ 1024×1024). |

**Key implementation details:**
- `AndroidViewModel(application)` — need `Context` for `ContentResolver`, `MediaScannerConnection`
- Constructor parameter: `songId: Long` (set via `SavedStateHandle` or factory)
- Load song from `SongController.getSongById(songId)` or from repository
- Use `viewModelScope.launch(Dispatchers.IO)` for all write operations
- Cover art load: `MediaMetadataRetriever` → `embeddedPicture` → `BitmapFactory.decodeByteArray` on init
- Cover art save: store resized JPEG `ByteArray` in `_coverArtState`, pass to `TagWriteInput.coverArt` on save
- Validation: `title.isBlank()` → set `titleError = "..."` and return false; `year.toIntOrNull() == null` and year not blank → set `yearError = "..."` and return false
- Use `_saveComplete = MutableSharedFlow<Unit>()` for navigation trigger
- Use `_saveError = MutableSharedFlow<String>()` for error toasts
- `save()` returns `Boolean` (true = success, false = validation error or write failure)
- Cover art resize: `BitmapFactory.Options.inSampleSize = computeSampleSize(uri)` → decode → `Bitmap.createScaledBitmap(original, targetW, targetH, true)` → `Bitmap.compress(Bitmap.CompressFormat.JPEG, 90, ByteArrayOutputStream())`

---

#### T4: Create `SongEditScreen`

| Field | Value |
|-------|-------|
| **Title** | Song edit form UI — Compose screen |
| **Description** | Create `SongEditScreen` as a full-screen Compose destination. Layout: **(1)** Top bar with "Edit Song" title, back arrow, and save button (checkmark icon). **(2)** Cover art section: circular/rounded image preview of current album art (or placeholder), tap to launch `ActivityResultContracts.GetContent("image/*")` for picking new cover. Shows loading spinner while decoding. **(3)** Form fields (scrollable `LazyColumn` or `Column` inside `verticalScroll`): title (`OutlinedTextField`, required, shows error), artist, album, genre, year (numeric keyboard), track number (numeric), disc number (numeric). Each field shows validation error text below when applicable. **(4)** Save button at bottom (or in top bar) — disabled while saving, shows `CircularProgressIndicator` during save operation. **(5)** Handle `saveComplete` event by navigating back. Handle `saveError` by showing `Snackbar`/`Toast`. Use `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` for cover picker. Pass `onNavigateBack: () -> Unit` for navigation after save. |
| **Files** | **Create:** `app/src/main/java/com/cvc953/localplayer/ui/screens/SongEditScreen.kt` |
| **Dependencies** | T3 (`SongEditViewModel` must exist) |
| **Verification** | 1. Build succeeds. 2. Screen renders with 7 editable fields + cover preview. 3. Tapping save with empty title shows validation error. 4. Cover picker launches system gallery. 5. Successful save navigates back. |

**Key implementation details:**
- `@OptIn(ExperimentalMaterial3Api::class)` for `TopAppBar`
- Use `TopAppBar` with `navigationIcon = { IconButton(onClick = onBack) }` and `actions = { IconButton(onClick = onSave) }`
- Cover preview: `Box` with `Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))` containing `Image` or placeholder `Icon`
- Form fields wrapped in `Column` with `Modifier.verticalScroll(rememberScrollState())`
- Each `OutlinedTextField`: `value = formState.field`, `onValueChange = { viewModel.onFieldChanged(...) }`, `isError = fieldError != null`, `supportingText = { Text(fieldError) }` if error
- `year`, `track`, `disc` use `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)`
- `LaunchedEffect(saveComplete)` to trigger `onNavigateBack()`
- `LaunchedEffect(saveError)` to show `Toast`
- Collect form state: `val formState by viewModel.formState.collectAsStateWithLifecycle()`
- Collect cover state: `val coverState by viewModel.coverArtState.collectAsStateWithLifecycle()`
- Cover picker: `val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> viewModel.onCoverSelected(uri) }`
- Cover preview click: `Modifier.clickable { coverLauncher.launch("image/*") }`

---

#### T5: Add edit button to `PlayerScreen` + `SongTitleSection`

| Field | Value |
|-------|-------|
| **Title** | Entry points for song editing from player |
| **Description** | Modify two files to provide access to the song edit screen: **(A)** `PlayerScreen.kt`: Add `onNavigateToSongEdit: (Long) -> Unit = {}` callback parameter. Add an "Edit" icon button (`Icons.Default.Edit` or `Material-icons`) in the player controls area — positioned near the song title/artist display. When tapped, calls `onNavigateToSongEdit(song.id)`. Wire this callback up through `PlayerScreen` callsite in calling context (the `AppNavigation` composable will pass it). **(B)** `SongTitleSection.kt`: Add `onEditClick: (() -> Unit)? = null` parameter. Add an "Edit details" row in the bottom sheet menu (alongside "Go to artist" and "Go to album"), with a pencil/edit icon. When tapped, dismisses bottom sheet and calls `onEditClick()`. The PlayerScreen passes `onEditClick = { onNavigateToSongEdit(song.id) }` to `SongTitleSection`. |
| **Files** | **Modify:** `app/src/main/java/com/cvc953/localplayer/ui/screens/PlayerScreen.kt`, `app/src/main/java/com/cvc953/localplayer/ui/components/SongTitleSection.kt` |
| **Dependencies** | T4 (`SongEditScreen` must be navigable), T10 (navigation action `navigateSongEdit` must exist) |
| **Verification** | 1. PlayerScreen shows edit icon button. 2. Tapping edit icon navigates to SongEditScreen. 3. SongTitleSection bottom sheet shows "Edit details" row. 4. Tapping "Edit details" navigates to SongEditScreen. |

**Key implementation details:**
- Add `onNavigateToSongEdit: (Long) -> Unit = {}` parameter to `PlayerScreen` function signature
- Place edit button in the `Row` with song controls, using `Icons.Default.Edit`
- In `SongTitleSection`: add `onEditClick: (() -> Unit)? = null` parameter
- In the bottom sheet `Column`, add a third `Row` after "Go to album" with edit icon + "Edit details" text
- Use `Icons.Default.Edit` for the icon
- On click: `showMenu = false; onEditClick?.invoke()`
- In `PlayerScreen` where `SongTitleSection` is called: pass `onEditClick = { onNavigateToSongEdit(song.id) }`

---

### Phase 3: Album Editing

---

#### T6: Create `AlbumEditViewModel`

| Field | Value |
|-------|-------|
| **Title** | Album edit form state with batch track iteration |
| **Description** | Create `AlbumEditViewModel` as an `AndroidViewModel` subclass. Similar to `SongEditViewModel` but for album-level operations. Responsibilities: **(1)** Initialize with album name and artist name. Load all songs for this album from `SongController` (or `SongRepository` via `AlbumController`). **(2)** Form state for album-level fields only: `album` (the album name itself), `artist` (album artist), `year`, `genre`. No track number or disc number fields. Pre-fill from first song's current values. **(3)** Cover art selection: same pipeline as SongEditViewModel (GetContent → decode → resize → compress). **(4)** `save(): Boolean` — validate (empty album name rejected). For **each track** in the album: call `SongController.updateSongTags()` with track-specific fields preserved (title, trackNumber, discNumber unchanged) and album-level fields applied. Track results as list of `AlbumTrackResult(songId, title, success, error?)`. Cover art applied to every track. **(5)** On partial failure: emit `_results` with both successes and failures. Do NOT roll back successful writes. **(6)** After all writes: call `updateSongInCache()` for each successful track, `scanSingleFile()` for each, `forceRescanSongs()` once. Emit `_saveComplete` event. **(7)** Expose `_results: StateFlow<List<AlbumTrackResult>>` for UI to show progress/failure. |
| **Files** | **Create:** `app/src/main/java/com/cvc953/localplayer/viewmodel/AlbumEditViewModel.kt` |
| **Dependencies** | T9 (`SongController.updateSongTags()` and `updateAlbumTags()` must exist), T2 (repository methods for cache updates and scanning) |
| **Verification** | 1. Build succeeds. 2. Form initializes with first song's current album/artist/year/genre. 3. Empty album name rejected. 4. Album edit with 3 tracks successfully writes to all with correct fields. 5. Simulated failure on track 2 preserves tracks 1 and 3. 6. Cover art writes to all tracks. |

**Key implementation details:**
- Constructor params: `albumName: String`, `artistName: String` (via `SavedStateHandle`)
- Load album songs: use `AlbumController.getSongsForAlbum()` or direct repository query filtered by album+artist
- `data class AlbumTrackResult(songId: Long, title: String, success: Boolean, error: String? = null)`
- `_results = MutableStateFlow<List<AlbumTrackResult>>(emptyList())`
- Album-level `TagWriteInput`: only set `album`, `artist`, `year`, `genre`, `coverArt` fields (keep title/track/disc as null → preserve existing)
- On each track: `val result = runCatching { controller.updateSongTags(track, input) }` → map success/failure to `AlbumTrackResult`
- After loop: update cache for successful tracks, scan files, force rescan
- Use `_isSaving = MutableStateFlow(false)` for UI progress indicator
- Use `_progress = MutableStateFlow(Pair(current, total))` for optional progress display
- May need `AlbumController` injected — requires `AlbumController` or direct `SongRepository` access

---

#### T7: Create `AlbumEditScreen`

| Field | Value |
|-------|-------|
| **Title** | Album edit form UI — Compose screen |
| **Description** | Create `AlbumEditScreen` as a full-screen Compose destination. Similar layout to `SongEditScreen` but with album-level fields only. Layout: **(1)** Top bar with "Edit Album" title, back arrow, save button. **(2)** Cover art section: same picker + preview as SongEditScreen but shows "Change album cover" label. **(3)** Form fields: album name (`OutlinedTextField`, required, shows error), album artist (free text), year, genre. No track number or disc number fields. **(4)** Below form: show track count ("Applies to N tracks"). **(5)** Save: disabled while saving, shows progress indicator. On save completion with partial failures: show `AlertDialog` listing which tracks succeeded and which failed. Pass `albumName: String` and `artistName: String` parameters. |
| **Files** | **Create:** `app/src/main/java/com/cvc953/localplayer/ui/screens/AlbumEditScreen.kt` |
| **Dependencies** | T6 (`AlbumEditViewModel` must exist) |
| **Verification** | 1. Build succeeds. 2. Screen renders with album-level 4 fields + cover preview + track count. 3. Empty album name rejected. 4. Successful save navigates back. 5. Partial failure shows track results dialog. |

**Key implementation details:**
- Similar structure to `SongEditScreen` but:
  - Title: "Edit Album"
  - 4 fields instead of 7 (no track, disc)
  - Track count text: `"Se aplicará a las N canciones del álbum"` / `"Applies to N album tracks"`
  - On save: collect `_results` and show `AlertDialog` with success/failure breakdown after save completes
  - `LaunchedEffect(saveComplete)` → show results dialog, then navigate back on dismiss
- Use same cover picker pattern as SongEditScreen
- Same validation pattern for album name (cannot be empty)

---

#### T8: Add edit button to `AlbumDetailScreen`

| Field | Value |
|-------|-------|
| **Title** | Entry point for album editing from album detail |
| **Description** | Modify `AlbumDetailScreen.kt` to add an edit button in the top toolbar area. **(1)** Add `onNavigateToAlbumEdit: (String, String) -> Unit = { _, _ -> }` callback parameter. **(2)** Add an edit icon button (`Icons.Default.Edit`) in the top `Row` alongside the back button and search button. Position: after the album title column, before the search button (or after it — less prominent position). **(3)** On click, call `onNavigateToAlbumEdit(albumName, artistName)`. The exact album name and artist name are already available as function parameters. |
| **Files** | **Modify:** `app/src/main/java/com/cvc953/localplayer/ui/screens/AlbumDetailScreen.kt` |
| **Dependencies** | T7 (`AlbumEditScreen` must be navigable), T10 (navigation action `navigateAlbumEdit` must exist) |
| **Verification** | 1. AlbumDetailScreen shows edit icon in toolbar. 2. Tapping edit icon navigates to AlbumEditScreen with correct album/artist params. |

**Key implementation details:**
- Add `onNavigateToAlbumEdit: (String, String) -> Unit = { _, _ -> }` parameter
- Add `IconButton` with `Icons.Default.Edit` in the toolbar `Row`
- Position it after the search button: back ← album title → search → edit
- On click: `onNavigateToAlbumEdit(albumName, artistName)`

---

### Phase 4: Controllers & Navigation

---

#### T9: Update `SongController` with `updateSongTags` / `updateAlbumTags`

| Field | Value |
|-------|-------|
| **Title** | Controller layer for tag write operations |
| **Description** | Add two methods to `SongController`: **(1)** `updateSongTags(songId: Long, input: TagWriteInput): Result<TagWriteResult>` — find the song by ID from `getSongById()`, call `SongRepository.writeTags()` with the song's URI and filePath. On success: call `SongRepository.updateSongInCache()` with the song ID and new fields, then `SongRepository.scanSingleFile()` for the file path, then `forceRescan()`. Return the result. **(2)** `updateAlbumTags(albumName: String, artistName: String, input: TagWriteInput): List<AlbumTrackResult>` — find all songs in the album (filter from `getAllSongs()` matching album name and artist), iterate calling `updateSongTags()` for each, collect results into `AlbumTrackResult` list. Do NOT roll back on partial failure. Matches the existing controller pattern (thin delegation to repository). |
| **Files** | **Modify:** `app/src/main/java/com/cvc953/localplayer/controller/SongController.kt` |
| **Dependencies** | T2 (`SongRepository` write methods must exist) |
| **Verification** | 1. Build succeeds. 2. `updateSongTags()` correctly identifies song by ID and delegates to repository. 3. `updateAlbumTags()` correctly iterates over all songs in an album. 4. Both methods return proper success/failure results. |

**Key implementation details:**
- `updateSongTags(songId: Long, input: TagWriteInput): Result<TagWriteResult>`:
  ```kotlin
  val song = getSongById(songId) ?: return Result.failure(Exception("Song not found"))
  return repository.writeTags(song.uri, song.filePath, input)
  ```
- `updateAlbumTags(albumName: String, artistName: String, input: TagWriteInput): List<AlbumTrackResult>`:
  ```kotlin
  val songs = getAllSongs().filter { ... }
  return songs.map { song ->
      val result = updateSongTags(song.id, input)
      AlbumTrackResult(song.id, song.title, result.isSuccess, result.exceptionOrNull()?.message)
  }
  ```
- Import `AlbumTrackResult` (define in this file or in `AlbumEditViewModel.kt`)
- `SongController` already has `repository` instance — use it directly

---

#### T10: Wire up navigation — Destinations, NavigationActions, NavGraph

| Field | Value |
|-------|-------|
| **Title** | Navigation routes, actions, and graph entries |
| **Description** | Modify three navigation files: **(A)** `Destinations.kt`: Add two new `Screen` entries: `Screen.SongEdit(songId: Long)` with route `"song-edit/{songId}"` (argument: `songId` of type `NavType.LongType`) and `Screen.AlbumEdit` with route `"album-edit/{albumName}/{artistName}"` (arguments: `albumName`, `artistName` both `NavType.StringType`). Add corresponding argument lists (`songEditArguments`, `albumEditArguments`). Add `createRoute()` methods for each. Follow the existing URL-encode pattern for string params. **(B)** `NavigationActions.kt`: Add two extension functions: `fun NavController.navigateSongEdit(songId: Long)` → `navigate(Screen.SongEdit.createRoute(songId))` and `fun NavController.navigateAlbumEdit(albumName: String, artistName: String)` → `navigate(Screen.AlbumEdit.createRoute(albumName, artistName))`. **(C)** `NavGraph.kt`: Add two `composable()` entries: one for `Screen.SongEdit.route` with `songEditArguments` that instantiates `SongEditScreen` with its ViewModel (using `viewModel()` factory or `SongEditViewModel(songId)` via `backStackEntry.arguments.getLong("songId")`), and one for `Screen.AlbumEdit.route` with `albumEditArguments` that instantiates `AlbumEditScreen`. Wire the `onNavigateBack` to `navController.navigateBack()`. Wire `updateSongInCache` and rescan callbacks through the screen composables. |
| **Files** | **Modify:** `app/src/main/java/com/cvc953/localplayer/ui/navigation/Destinations.kt`, `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavigationActions.kt`, `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavGraph.kt` |
| **Dependencies** | T4 (`SongEditScreen` composable must exist), T7 (`AlbumEditScreen` composable must exist) |
| **Verification** | 1. Build succeeds. 2. Navigation route `song-edit/123` creates correct Screen.SongEdit instance. 3. Navigation route `album-edit/Greatest%20Hits/AC%2FDC` creates correct Screen.AlbumEdit instance. 4. Calling `navigateSongEdit(123)` navigates to the SongEditScreen. 5. Calling `navigateAlbumEdit("Greatest Hits", "AC/DC")` navigates to AlbumEditScreen. 6. Back navigation from both screens works. |

**Key implementation details:**
- `Destinations.kt` additions:
  ```kotlin
  object SongEdit : Screen("song-edit/{songId}") {
      const val ARG_SONG_ID = "songId"
      fun createRoute(songId: Long): String = "song-edit/$songId"
  }
  object AlbumEdit : Screen("album-edit/{albumName}/{artistName}") {
      const val ARG_ALBUM_NAME = "albumName"
      const val ARG_ARTIST_NAME = "artistName"
      fun createRoute(albumName: String, artistName: String): String {
          val encodedAlbum = URLEncoder.encode(albumName, StandardCharsets.UTF_8.toString())
          val encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
          return "album-edit/$encodedAlbum/$encodedArtist"
      }
  }
  ```
- `songEditArguments`, `albumEditArguments` val definitions
- In `NavGraph.kt`:
  ```kotlin
  composable(Screen.SongEdit.route, arguments = songEditArguments) { backStackEntry ->
      val songId = backStackEntry.arguments?.getLong(Screen.SongEdit.ARG_SONG_ID) ?: return@composable
      val viewModel: SongEditViewModel = viewModel()
      SongEditScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.navigateBack() },
      )
  }
  ```
  Note: `SongEditViewModel` needs `songId`. Use `SavedStateHandle` in the ViewModel or a factory. The simplest approach: create the ViewModel with a factory that passes `songId`. Alternative: have the Screen load the song and pass it to the VM.

Actually, looking at the codebase pattern — ViewModels are created via `viewModel()` default factory in many composables. But `SongEditViewModel` needs `songId`. One clean approach: use `SavedStateHandle` which automatically gets the nav arguments:

```kotlin
class SongEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val songId: Long = savedStateHandle.get<Long>("songId") ?: error("songId required")
    ...
}
```

This is the standard Navigation Compose approach. No factory needed.

Similarly for `AlbumEditViewModel`:
```kotlin
class AlbumEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val albumName: String = savedStateHandle.get<String>("albumName") ?: ""
    private val artistName: String = savedStateHandle.get<String>("artistName") ?: ""
    ...
}
```

This works because Navigation Compose puts route arguments into the `SavedStateHandle` automatically.

---

#### T11: String resources (3 locales)

| Field | Value |
|-------|-------|
| **Title** | UI string resources for edit screens |
| **Description** | Add string resources for the edit feature across all 3 locales. Required strings: `edit_song_title` ("Edit Song" / "Editar canción" / "Modifica brano"), `edit_album_title` ("Edit Album" / "Editar álbum" / "Modifica album"), `action_save` ("Save" / "Guardar" / "Salva"), `action_edit` ("Edit" / "Editar" / "Modifica"), `field_title` ("Title" / "Título" / "Titolo"), `field_artist` ("Artist" / "Artista" / "Artista"), `field_album` ("Album" / "Álbum" / "Album"), `field_genre` ("Genre" / "Género" / "Genere"), `field_year` ("Year" / "Año" / "Anno"), `field_track_number` ("Track #" / "N° pista" / "N° traccia"), `field_disc_number` ("Disc #" / "N° disco" / "N° disco"), `cover_art_change` ("Change cover" / "Cambiar carátula" / "Cambia copertina"), `validation_title_required` ("Title cannot be empty" / "El título no puede estar vacío" / "Il titolo non può essere vuoto"), `validation_year_invalid` ("Year must be a valid number" / "El año debe ser un número válido" / "L'anno deve essere un numero valido"), `error_save_failed` ("Save failed — file may be in use" / "Error al guardar — el archivo puede estar en uso" / "Salvataggio fallito — il file potrebbe essere in uso"), `edit_details` ("Edit details" / "Editar detalles" / "Modifica dettagli"), `album_edit_track_count` ("Applies to %d tracks" / "Se aplica a %d canciones" / "Si applica a %d brani"), `album_edit_partial_failure_title` ("Some tracks failed" / "Algunas canciones fallaron" / "Alcuni brani non sono stati modificati"), `album_edit_partial_failure_message` ("%d tracks updated successfully, %d failed" / "%d canciones actualizadas, %d fallaron" / "%d brani aggiornati, %d falliti"), `error_invalid_image` ("Invalid image" / "Imagen inválida" / "Immagine non valida"), `error_file_in_use` ("File in use — close other apps" / "Archivo en uso — cerrá otras apps" / "File in uso — chiudi altre app"), `error_format_not_writable` ("This file format does not support tag writing" / "Este formato no soporta escritura de etiquetas" / "Questo formato non supporta la scrittura dei tag"), `error_disk_full` ("Not enough storage space" / "No hay suficiente espacio" / "Spazio insufficiente"), `error_cannot_access_file` ("Cannot access this file on this device" / "No se puede acceder a este archivo" / "Impossibile accedere a questo file"). |
| **Files** | **Modify:** `res/values/strings.xml` (Spanish — base), `res/values-en/strings.xml` (English), `res/values-it/strings.xml` (Italian) |
| **Dependencies** | None (can be done in parallel with any phase) |
| **Verification** | 1. All 3 files have matching string keys. 2. No missing translations. 3. Strings render correctly in their respective edit screens. |

---

## Summary

| Phase | Tasks | Files Created | Files Modified | Deps |
|-------|-------|---------------|----------------|------|
| 1 — Foundation | T1, T2 | `util/TagWriter.kt` | `model/SongRepository.kt` | T1→T2 |
| 2 — Song Editing UI | T3, T4, T5 | `viewmodel/SongEditViewModel.kt`, `ui/screens/SongEditScreen.kt` | `ui/screens/PlayerScreen.kt`, `ui/components/SongTitleSection.kt` | T2→T3→T4→T5 |
| 3 — Album Editing | T6, T7, T8 | `viewmodel/AlbumEditViewModel.kt`, `ui/screens/AlbumEditScreen.kt` | `ui/screens/AlbumDetailScreen.kt` | T2→T6→T7→T8 |
| 4 — Wiring | T9, T10, T11 | — | `controller/SongController.kt`, `ui/navigation/Destinations.kt`, `NavigationActions.kt`, `NavGraph.kt`, `strings.xml` (×3) | T2→T9→T3/T6; T4/T7→T10→T5/T8 |

### Recommended implementation order

```
T1 → T2 → T11 (parallel) → T9 → T3 → T4 → T6 → T7 → T10 → T5 → T8
```

Or in practice: T1 + T11 first (foundation + strings are independent), then T2, T9, then song edit (T3, T4), album edit (T6, T7), then wire everything (T10, T5, T8).
