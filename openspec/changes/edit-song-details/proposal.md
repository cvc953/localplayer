# Proposal: Edit Song & Album Details

## Intent

Users have no way to correct missing or incorrect metadata in their music library. This change adds the ability to edit audio file tags and cover art for individual songs and entire albums, directly from the player and album screens.

## Scope

### In Scope
- Song tag editing: title, artist, album, genre, year, track/disc number
- Song cover art editing: pick image via `GetContent`, resize to 1024x1024, embed in audio file
- Album metadata editing: album title, artist, year, genre, cover art
- Dedicated SongEditScreen and AlbumEditScreen
- `TagWriter` utility for jaudiotagger-based tag writing
- SAF-based write for API 29+, direct file for API < 29
- Library refresh: immediate in-memory update + scanFile() + forceRescan()

### Out of Scope
- Batch editing multiple songs at once
- Editing lyrics (read-only remains)
- Coil integration for cover art
- Predefined genre picker (free-text only)
- File renaming based on tags

## Capabilities

### New Capabilities
- `song-editing`: Modify embedded audio tags and cover art for individual songs
- `album-editing`: Modify album-level metadata and cover art for all songs in an album

### Modified Capabilities
None — no existing specs to modify.

## Approach

1. **TagWriter** utility: wraps `AudioFileIO.write()` with API-level-aware write paths (direct File < 29, SAF temp-copy-write ≥ 29)
2. **SongEditScreen**: Compose form with fields for all editable tags, cover art preview + picker
3. **SongEditViewModel**: form state, save lifecycle, cover art selection, validation
4. **AlbumEditScreen**: same form concept but iterates over all tracks in the album
5. **Write strategy**: API<29 direct File write. API≥29 copy to temp, edit via jaudiotagger, write back via `ContentResolver.openOutputStream` on MediaStore URI
6. **Refresh**: immediate Song cache update in-memory, then async `scanFile()` + `forceRescan()`

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `util/TagWriter.kt` | New | jaudiotagger write wrapper with SAF fallback |
| `ui/screens/SongEditScreen.kt` | New | Song edit form screen |
| `ui/screens/AlbumEditScreen.kt` | New | Album edit form screen |
| `viewmodel/SongEditViewModel.kt` | New | Edit form state management |
| `viewmodel/AlbumEditViewModel.kt` | New | Album edit state management |
| `model/SongRepository.kt` | Modified | Add `writeTags()`, `updateSongInCache()`, `scanSingleFile()` |
| `controller/SongController.kt` | Modified | Add `updateSongTags()`, `updateAlbumTags()` |
| `ui/screens/PlayerScreen.kt` | Modified | Edit button entry point |
| `ui/components/SongTitleSection.kt` | Modified | "Edit" option in bottom sheet dropdown |
| `ui/screens/AlbumDetailScreen.kt` | Modified | Edit button entry point |
| `strings.xml` | Modified | Edit UI string resources |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| File corruption on SAF write | Low | Write to temp file first, then copy via ContentResolver |
| null filePath on API 29+ | Medium | Fall back to ContentResolver.openFileDescriptor |
| Large cover art OOM | Medium | Resize to 1024x1024 max before embedding |
| Library cache stale after edit | High | Immediate in-memory update + async rescan |

## Rollback Plan

No schema or DB changes — revert single commit. If SAF write corrupts files, user restores from backup or re-rips.

## Dependencies

- jaudiotagger 3.0.1 (already present)
- No new dependencies required

## Success Criteria

- [ ] User edits all 7 tag fields and changes persist after app restart
- [ ] Cover art embeds and displays correctly in player
- [ ] Album edits apply to all tracks in that album
- [ ] Changes survive MediaStore rescan
- [ ] Works on both API 28 and API 33 emulators
