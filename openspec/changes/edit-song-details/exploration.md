## Exploration: Edit Song Details & Tags

### Current State

#### Song Model
- `Song` data class in `model/Song.kt` with fields: `id` (Long, MediaStore `_ID`), `title`, `artist`, `album`, `year`, `uri` (Uri), `duration`, `albumArt` (ByteArray? always null from scan), `filePath` (String?), `trackNumber`, `discNumber`, `sampleRate`, `mimeType`, `dateAdded`, `genre`.
- No Room database. No ORM. Songs are read from `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` via `ContentResolver.query()` in `SongRepository`.
- Results are cached to `songs_cache.json` (JSON file in app-internal storage).

#### How Songs Are Read
- `SongRepository.scanSongsFromMediaStore()` queries MediaStore with projection for all fields.
- Album art is NOT loaded during scan — done on-demand via `MediaMetadataRetriever.embeddedPicture` + `BitmapFactory.decodeByteArray`.
- Song `id` is the MediaStore `_ID`. Song `uri` is `content://media/external/audio/media/{id}`.
- `filePath` comes from `MediaStore.Audio.Media.DATA` column (deprecated on Android 10+ but still populated).

#### Cover Art Loading
- Every cover art load uses `MediaMetadataRetriever.setDataSource(context, song.uri)` → `embeddedPicture` → `BitmapFactory.decodeByteArray`.
- **Coil is NOT used despite being declared as a dependency.** No `AsyncImage` or `Coil` imports anywhere.
- `AlbumHeader` additionally checks for local files (`cover.jpg`, `folder.jpg`, etc.) in the song's directory as fallback.
- No caching layer beyond per-composable `remember { mutableStateOf<Bitmap?>(null) }` — each composable re-fetches on re-composition.
- Default fallback: `R.drawable.ic_default_album`.

#### Audio Tag Reading (jaudiotagger)
- `EmbeddedLyricsExtractor` uses `org.jaudiotagger:jaudiotagger:3.0.1` to read embedded lyrics.
- Pattern: `AudioFileIO.read(File)` → get tag → extract fields. Already works with `filePath`.
- Supports MP3 (ID3v2), MP4/M4A, FLAC (VorbisComment), OGG.
- **No write operations to audio files exist anywhere in the codebase.**

#### Permission Model
- AndroidManifest declares: `READ_MEDIA_AUDIO` (Android 13+), `READ_EXTERNAL_STORAGE` (maxSdkVersion=32).
- **No `WRITE_EXTERNAL_STORAGE` permission.**
- SAF (Storage Access Framework) used for folder selection via `OpenDocumentTree`, with `takePersistableUriPermission(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION)`.
- `StoragePermissionHandler` screens: permission first, then folder selection.

#### Library Refresh
- `SongRepository.forceRescanSongs()` → re-queries MediaStore → rewrites `songs_cache.json`.
- `SongViewModel.loadSongs(forceRescan=true)` triggers this from IO dispatcher.
- No `ContentObserver` for automatic refresh on file changes.

#### UI Patterns
- PlayerScreen: Uses `LaunchedEffect` + `Dispatchers.IO` for async work.
- Dialogs: `AlertDialog` (create playlist, delete folder), `ModalBottomSheet` (queue, artist/album nav), `DropdownMenu` (song actions).
- Theming: Material3 with custom `ExtendedColors`. `LocalExtendedColors.current` for secondary text, surface sheets.
- ViewModels: `AndroidViewModel` pattern, `StateFlow` exposure, `viewModelScope.launch(Dispatchers.IO)` for background work.

### Affected Areas

| File | Why Affected |
|------|-------------|
| `model/Song.kt` | May need a `copy()` with edited fields, or a new `MutableSong` state holder. No DB to update — changes go directly to file tags. |
| `model/SongRepository.kt` | Needs `writeTagsToFile(song, newTags)` method + `updateSongInCache(updatedSong)`. Needs `scanSingleFile(filePath)` for post-edit refresh. |
| `controller/SongController.kt` | Needs `updateSongTags(song, updatedFields)` method. |
| `viewmodel/SongViewModel.kt` or new `SongEditViewModel.kt` | Needs state for edit form fields, save action, cover art selection state. Could go in existing SongViewModel or be a new ViewModel. |
| `ui/screens/PlayerScreen.kt` | Needs "Edit" button/icon in the player controls or in the SongTitleSection bottom sheet. |
| `ui/components/SongTitleSection.kt` | Add "Edit details" option to the existing modal bottom sheet. |
| `ui/screens/SongEditScreen.kt` (NEW) | New screen or bottom sheet for editing all song fields. |
| `util/EmbeddedLyricsExtractor.kt` | Reference pattern for jaudiotagger usage. May need `TagWriter.kt` counterpart. |
| `util/TagWriter.kt` (NEW) | New utility class for writing audio tags via jaudiotagger, mirroring EmbeddedLyricsExtractor's read pattern. |
| `app/build.gradle.kts` | May need no new dependencies — jaudiotagger already supports write. But may need `coil-compose` for image picking, or rely on `ActivityResultContracts.GetContent`. |
| `AndroidManifest.xml` | May need `WRITE_EXTERNAL_STORAGE` if targeting Android < 10 direct file write. May need `MANAGE_EXTERNAL_STORAGE` if using direct file access on Android 11+. |
| `strings.xml` | New string resources for edit UI labels, button text, toast messages. |
| `ui/screens/AlbumDetailScreen.kt` | May want edit shortcut in album detail context. |
| `ui/screens/ArtistDetailScreen.kt` | Similar — edit from artist context. |

### Approaches

#### 1. Tag Editing: jaudiotagger direct write

Directly use `AudioFileIO.write(tagFile)` on the file path obtained from `Song.filePath`.

- **Pros**: Simple, one-step, validated library already in the project. Supports all formats (MP3 ID3v2, MP4, FLAC, OGG). Writes actual file metadata (not just MediaStore DB).
- **Cons**: Direct file path access is restricted on Android 10+ for files not owned by the app. `Song.filePath` may be null for content:// URIs on newer Androids. Requires `WRITE_EXTERNAL_STORAGE` on older Androids for external files. Needs fallback for scoped storage.
- **Effort**: Low-Medium

#### 2. Tag Editing: SAF copy-edit-replace

Copy file to app-private temp directory → edit with jaudiotagger → write back using `ContentResolver.openOutputStream` on the MediaStore URI → delete temp.

- **Pros**: Fully compatible with scoped storage. No special permissions needed beyond what's already declared.
- **Cons**: Two file operations per edit (copy + write back). Potential for data loss if interrupted. Temp files take space. Complex error handling.
- **Effort**: High

#### 3. Cover Art: jaudiotagger embed

Use `Tag.setField(Artwork)` with jaudiotagger to embed cover art directly into the audio file.

- **Pros**: Same library, same code path as tag editing. Embedded art is portable.
- **Cons**: Same scoped storage concerns as tag editing. Large images increase file size.
- **Effort**: Low (if tag writing is already implemented)

#### 4. Cover Art: write cover.jpg to directory

Write a `cover.jpg` (or `folder.jpg`) file to the song's album directory.

- **Pros**: Already partially supported by `AlbumHeader`. Doesn't modify audio file. Easy to undo.
- **Cons**: Requires directory write access. Not all players read folder.jpg. Only benefits this app unless you also embed.
- **Effort**: Medium

#### 5. Cover Art Picking: SAF `GetContent`

Use `ActivityResultContracts.GetContent` with `image/*` MIME type to pick from gallery.

- **Pros**: Standard Android pattern. No extra storage permissions needed. Works on all API levels.
- **Cons**: Returns a content URI that needs to be read and possibly decoded/resized before embedding.
- **Effort**: Low

#### 6. UI: Modal Bottom Sheet (from PlayerScreen)

Add an "Edit" option to the existing `SongTitleSection` bottom sheet. Opens a new bottom sheet with text fields for each tag and an image picker for cover art.

- **Pros**: Consistent with existing patterns (queue, artist/album navigation). Accessible from player where the user is most likely to want it. No navigation stack changes.
- **Cons**: Limited space for many fields. Scrolling required for all 7+ fields + cover art.
- **Effort**: Medium

#### 7. UI: Dedicated Screen

Full Compose screen with scrollable form, cover art preview at top.

- **Pros**: Best UX — all fields visible, easy to layout. Can show cover art preview. Natural save/cancel toolbar.
- **Cons**: Navigation added. Need to pass song data between screens. More code.
- **Effort**: Medium-High

#### 8. Scoped Storage Strategy

**For Android 10+ (API 29+)**: Use `ContentResolver.openOutputStream(song.uri)` to get a writable stream to the MediaStore URI. Write modified bytes from jaudiotagger temp file back through this stream. This avoids needing file path access.

**For Android 9 and below (API < 29)**: Direct file path access works. Use `java.io.File` with `AudioFileIO.write()`. Already have WRITE permissions via READ permission group on these versions.

**Edge case — MediaStore DATA column empty/null**: Some Android 11+ devices don't populate DATA. In this case, fall back to `ContentResolver.openFileDescriptor(song.uri, "w")` pattern.

**Post-edit refresh**: After writing, call `MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)` to notify MediaStore. Then force-rescan the SongRepository cache.

- **Effort**: High (must handle multiple API levels correctly)

#### 9. Library Refresh Strategy

After tag write:
1. `MediaScannerConnection.scanFile()` to update MediaStore
2. Small delay for scan to complete (~500ms)
3. `SongViewModel.forceRescanSongs()` to rebuild cache
4. Notify UI observers via StateFlow update

- **Pros**: Simple, reliable
- **Cons**: Scan delay means the UI doesn't update instantly. Could also update the in-memory song list immediately after edit for instant feedback.
- **Effort**: Low

### Recommendation

| Decision | Recommended Approach | Rationale |
|----------|-------------------|-----------|
| **Tag writing** | jaudiotagger direct write with SAF fallback for Android 10+ | jaudiotagger is already in the project. Direct file path works on older Androids. For Android 10+, use SAF-based write via ContentResolver. This avoids adding new dependencies. |
| **Cover art editing** | jaudiotagger embed artwork, with SAF content URI picker | Keep all tag manipulation in one place. jaudiotagger supports embedding artwork natively. Use `ActivityResultContracts.GetContent()` for picking images. |
| **Cover art picking** | `ActivityResultContracts.GetContent("image/*")` | Standard Android pattern, no extra permissions. Already similar to SAF pattern used in SettingsScreen. |
| **UI placement** | Dedicated screen | Editing 7+ fields + cover art in a bottom sheet is cramped. A dedicated screen from PlayerScreen gives better UX and room for image preview. Opens with a button in the player controls area. |
| **Scoped storage** | Android 10+: write via ContentResolver.openOutputStream on MediaStore URI. Android <10: direct file write. | Safest approach that works across all API levels without requesting MANAGE_EXTERNAL_STORAGE. |
| **Library refresh** | scanFile() + forceRescan() + immediate in-memory update | Instant feedback (update in-memory Song), then async MediaStore refresh. |
| **New VM or extend** | New `SongEditViewModel` | Keeps editing state separate from playback concerns. SongViewModel is for library loading, not form editing. |
| **Image handling** | Stay with Bitmap/MediaMetadataRetriever for reading. Use Bitmap.compress for writing. | App doesn't use Coil despite it being declared. Adding it now for just this feature adds complexity without proportional benefit. |

### Risks

1. **File corruption**: Writing tags directly to audio files carries inherent risk. Always write to a temp file first, then replace. jaudiotagger's `commit()` does atomic write internally, but SAF streams are not atomic.

2. **Scoped storage compatibility**: The `DATA` column is deprecated on Android 10+ and may be empty on Android 11+. Must handle null filePath gracefully.

3. **Format-specific tag limits**: ID3v1 only supports 30-char title/artist. ID3v2 has no limit but some old readers don't support it. jaudiotagger handles this well but the code needs to know which tag versions to write.

4. **Large cover art**: Full-resolution phone camera images (12MP+) can be 4-10MB. Decoding + re-encoding for embedding risks OOM. Must resize cover art to a reasonable max (e.g., 1024x1024).

5. **Encoding issues**: jaudiotagger may not handle all character encodings correctly for non-ASCII metadata (Japanese, Cyrillic, etc.).

6. **Permission gap**: No `WRITE_EXTERNAL_STORAGE` declared. Must NOT add it — instead use SAF/MediaStore streams. Adding `WRITE_EXTERNAL_STORAGE` would require new permission request flow.

7. **Library cache staleness**: After editing tags, the app's `songs_cache.json` will have stale data until forceRescan. Must either update cache immediately or force rescan.

8. **Album art source ambiguity**: If editing cover art for a single song in an album, should it apply to all songs in that album? Only to that specific file? This needs UX clarification.

9. **File path nullability**: `Song.filePath` is nullable (`String?`). In files from MediaStore on Android 10+, this may be null even when file exists. Must use `ContentResolver.openFileDescriptor` as fallback.

10. **Format write support**: jaudiotagger 3.0.1 supports writing to MP3 (ID3v1/v2), MP4/M4A, FLAC, OGG. WMA and other formats may have limited write support. Must validate before attempting write.

### Ready for Proposal
Yes
