# Design: Edit Song & Album Details

## Technical Approach

Dual-path write strategy for audio tag editing: **direct File write** (API < 29) vs **SAF copy-write** (API ≥ 29). Cover art follows the same write path after a decode → resize 1024×1024 → compress pipeline. Album editing iterates over all tracks with per-track partial failure reporting. Library refresh hooks into existing `SongRepository.forceRescanSongs()` after immediate in-memory cache update.

## Architecture Decisions

### Decision: TagWriter as Object (not class)

**Choice**: `object TagWriter` — stateless utility, mirrors `EmbeddedLyricsExtractor` pattern.
**Alternatives**: Class with injected `Context` (more testable but breaks pattern).
**Rationale**: The codebase uses `object` for utilities (`EmbeddedLyricsExtractor`, `RouteParams`). Consistency wins. Context is passed per-call.

### Decision: Cover art resize to 1024×1024 max

**Choice**: Decode with `BitmapFactory.Options.inSampleSize` → create scaled bitmap → `Bitmap.compress(JPEG, 90)`.
**Alternatives**: Let user pick full resolution (OOM risk), use Coil transformation (declared but unused).
**Rationale**: 1024×1024 is enough for album art display. Prevents OOM on 12MP+ photos. JPEG at 90% quality balances size/quality.

### Decision: Album edit iterates, does NOT roll back

**Choice**: Loop over tracks, track successes/failures, report partial failures to UI.
**Alternatives**: Transaction-style all-or-nothing (complex, risky), async batch (progress UX complexity).
**Rationale**: Album tracks are independent files. Failing on track 7 shouldn't undo tracks 1–6. User sees which tracks failed.

### Decision: New ViewModels, not extending existing ones

**Choice**: `SongEditViewModel` and `AlbumEditViewModel` as new `AndroidViewModel` subclasses.
**Alternatives**: Add edit state to `SongViewModel` / `AlbumViewModel`.
**Rationale**: Edit state (form fields, dirty tracking, validation, cover picker) is orthogonal to playback and library loading. Follows existing separation — `SongViewModel` handles library, not editing.

### Decision: Dedicated Compose screens, not bottom sheets

**Choice**: `SongEditScreen` and `AlbumEditScreen` as full-screen destinations in NavGraph.
**Alternatives**: Modal bottom sheet (cramped for 7+ fields + cover preview + save).
**Rationale**: Dedicated screen provides room for cover preview, form fields, validation messages, and save/cancel toolbar. Navigate via URL-encoded params matching existing pattern.

## Data Flow

### Tag Write Flow (API < 29)

```
SongEditScreen → ViewModel.save()
  → validate fields (no empty title, valid year)
  → TagWriter.writeTags(filePath, tags, coverArt?)
    → AudioFileIO.read(File) → modify tag fields → AudioFileIO.write(File)
  → SongRepository.updateSongInCache(songId, newFields)
  → MediaScannerConnection.scanFile(filePath)
  → SongRepository.forceRescanSongs()
  → navigateBack()
```

### Tag Write Flow (API ≥ 29)

```
ViewModel.save()
  → copy File(filePath) → tempFile in cacheDir
  → TagWriter.writeTags(tempFile.absolutePath, tags, coverArt?)
    → AudioFileIO.read(tempFile) → modify → write(tempFile)
  → ContentResolver.openOutputStream(song.uri)
  → tempFile.inputStream() → copyTo(outputStream)
  → tempFile.delete()
  → (same refresh as above)
```

### Cover Art Selection → Embed Flow

```
User taps cover preview
  → ActivityResultContracts.GetContent("image/*")
  → ViewModel receives Uri
  → LaunchedEffect(selectedImageUri)
    → context.contentResolver.openInputStream(uri)
    → BitmapFactory.decodeStream() with inSampleSize
    → Bitmap.createScaledBitmap(max 1024×1024)
    → compress to JPEG byte array
    → store in _coverArtState
  → On save: pass byte[] to TagWriter → setArtwork(tag)
```

### Album Edit → Iterate Tracks Flow

```
AlbumEditScreen → ViewModel.save()
  → for each song in albumSongs:
      result = TagWriter.writeTags(song.filePath, albumFields, coverArt?)
      trackResults.add(song.id → result)
  → updateSongInCache() for successful tracks
  → scanFile() for each edited file
  → forceRescanSongs()
  → emit _results with successes and failures
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `util/TagWriter.kt` | **Create** | `object TagWriter` — `writeTags(path, tags, cover?): Result<TagWriteResult>`, format-aware artwork embedding |
| `viewmodel/SongEditViewModel.kt` | **Create** | `AndroidViewModel` — form `StateFlow`, cover art state, validation, save lifecycle |
| `viewmodel/AlbumEditViewModel.kt` | **Create** | `AndroidViewModel` — same + batch progress + per-track results |
| `ui/screens/SongEditScreen.kt` | **Create** | Full-screen Compose form — cover preview, 7 fields, save button |
| `ui/screens/AlbumEditScreen.kt` | **Create** | Same layout, album-level fields only (no track/disc number) |
| `controller/SongController.kt` | **Modify** | Add `updateSongTags(song, newFields)`, `updateAlbumTags(albumId, newFields)` |
| `model/SongRepository.kt` | **Modify** | Add `writeTags(songUri, filePath, tags): Result`, `updateSongInCache(id, updates)`, `scanSingleFile(path)` |
| `ui/navigation/Destinations.kt` | **Modify** | Add `Screen.SongEdit` and `Screen.AlbumEdit` with URL-encoded params |
| `ui/navigation/NavigationActions.kt` | **Modify** | Add `navigateSongEdit(songId)`, `navigateAlbumEdit(albumName, artistName)` |
| `ui/navigation/NavGraph.kt` | **Modify** | Add `composable` entries for SongEdit and AlbumEdit |
| `ui/screens/PlayerScreen.kt` | **Modify** | Add "Edit" button in controls area, call `onNavigateToSongEdit` |
| `ui/components/SongTitleSection.kt` | **Modify** | Add "Edit details" row in bottom sheet |
| `ui/screens/AlbumDetailScreen.kt` | **Modify** | Add Edit button in top toolbar area |
| `strings.xml` | **Modify** | Add edit UI strings (ES, EN, IT) |

## Interfaces / Contracts

```kotlin
// util/TagWriter.kt
data class TagWriteInput(
    val title: String? = null,     // null = keep existing
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val coverArt: ByteArray? = null, // null = keep existing
)

data class TagWriteResult(
    val filePath: String,
    val success: Boolean,
    val error: String? = null,
)

object TagWriter {
    fun writeTags(
        context: Context,
        filePath: String?,
        uri: Uri,
        input: TagWriteInput,
    ): Result<TagWriteResult>

    fun embedCoverArt(
        tag: Tag,
        mimeType: String,  // "audio/mpeg", "audio/flac", "audio/mp4"
        coverData: ByteArray,
    )
}

// viewmodel/SongEditViewModel.kt
data class SongEditFormState(
    val title: String, val artist: String, val album: String,
    val genre: String, val year: String, val track: String, val disc: String,
    val titleError: String? = null, val yearError: String? = null,
)

data class CoverArtState(
    val currentBitmap: Bitmap?, // existing art loaded from file
    val selectedUri: Uri?,      // user-picked image URI
    val tempData: ByteArray?,   // decoded + resized image bytes
)

// viewmodel/AlbumEditViewModel.kt
data class AlbumTrackResult(
    val songId: Long, val title: String, val success: Boolean, val error: String? = null,
)
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `TagWriter.writeTags` — direct file path | Mock `AudioFileIO`, verify fields set correctly per format |
| Unit | Cover art resize — decode + compress with known image | Verify output ≤ 1024×1024, JPEG format |
| Unit | Form validation — empty title, invalid year, edge cases | Pure function tests on `SongEditFormState` |
| Unit | Album edit — partial failure aggregation | Mock `TagWriter` to fail on specific songs, verify results |
| Integration | SAF write flow on API 29+ emulator | Real `ContentResolver` with temp audio file |
| E2E | Full save → rescan → data persists after restart | Instrumented test on emulator |

## Migration / Rollout

No migration required. No existing data schema changes. Feature is purely additive — new screens and ViewModels. If SAF write fails on a device, user sees error message and original file is untouched.

## Open Questions

- [ ] Confirm that `Song.filePath` is reliably non-null on API 24–28 devices in this codebase (exploration says yes, but worth testing on API 26 emulator)
- [ ] Decide save button placement: top-right in toolbar vs bottom-center floating button (propose toolbar to match existing patterns)
- [ ] Confirm whether album edit should also allow individual track title/number edits (spec says NO — R8 protects them — design respects this)
