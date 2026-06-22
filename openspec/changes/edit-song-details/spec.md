# Edit Song & Album Details — Specification

## Song Editing

### Requirements

**R1 — Tag Field Editing**: The system MUST allow editing title, artist, album, genre, year, track number, and disc number via a form.

- **Edit title and artist from player**: GIVEN a song is playing on PlayerScreen, WHEN the user opens the edit form, modifies title and artist, and saves, THEN all modified tags are written to the audio file AND the player displays the new values immediately.
- **Preserve unmodified fields**: GIVEN the edit form is pre-filled with current tags, WHEN the user edits only the genre, THEN only the genre tag changes in the file and all other tags remain unchanged.

**R2 — Cover Art Editing**: The system MUST allow replacing embedded cover art via `ActivityResultContracts.GetContent("image/*")`, resizing to max 1024x1024 before embedding.

- **Change single song cover**: GIVEN the user taps the cover preview on the edit screen, WHEN they select a JPEG from the gallery, THEN the image is decoded, resized to 1024x1024 max, and embedded in the audio file.
- **OOM prevention on large images**: GIVEN the user picks a 12MP+ photo from the gallery, WHEN the system decodes it, THEN the bitmap is resized to 1024x1024 before embedding AND the source bitmap is recycled to free memory.
- **Invalid image selection**: GIVEN the user picks a corrupt or non-image file, WHEN the system fails to decode a bitmap, THEN the error "Invalid image" is shown and the previous cover art is preserved.

**R3 — Tag Writing with SAF Fallback**: The system MUST write audio tags using an API-level-appropriate strategy to work across Android 9 through 14+.

- **Direct file write (API < 29)**: GIVEN the device runs Android 9 or below AND `Song.filePath` is non-null, WHEN the user saves edits, THEN `AudioFileIO.write()` is called on the `File` directly.
- **SAF copy-write (API >= 29)**: GIVEN the device runs Android 10+ AND `Song.filePath` is non-null, WHEN the user saves edits, THEN the file is copied to app temp storage, edited via `AudioFileIO.write()`, and written back through `ContentResolver.openOutputStream(song.uri)`.
- **Fallback on null filePath**: GIVEN `Song.filePath` is null (Android 10+ DATA column empty), WHEN the user saves, THEN the system falls back to `ContentResolver.openFileDescriptor(song.uri, "w")` for write access.
- **SAF stream write failure**: GIVEN the `ContentResolver` stream throws `IOException` during write-back, WHEN write fails, THEN the original temp copy is preserved for recovery AND the user sees "Save failed — file may be in use".

**R4 — Library Refresh**: The system MUST update the in-memory song cache immediately after a successful write AND trigger an asynchronous MediaStore rescan.

- **Immediate in-memory update**: GIVEN a tag write succeeds, WHEN the write completes, THEN `SongRepository` updates the in-memory cached `Song` object with new metadata instantly AND the observing UI reflects the change without waiting for rescan.
- **MediaStore rescan**: GIVEN the in-memory cache is updated, WHEN the write succeeds, THEN `MediaScannerConnection.scanFile()` is called for the edited file AND `forceRescanSongs()` runs asynchronously to fully refresh `songs_cache.json`.

**R5 — Input Validation**: The system MUST validate form inputs before attempting any file write.

- **Empty title rejected**: GIVEN the user clears the title field to empty, WHEN they tap Save, THEN "Title cannot be empty" is shown AND no write operation is attempted.
- **Invalid year format**: GIVEN the user enters "abc" in the year field, WHEN they tap Save, THEN "Year must be a valid number" is shown AND no write is attempted.

## Album Editing

### Requirements

**R6 — Album Metadata Editing**: The system MUST allow editing album title, artist, year, and genre, applying the changes to EVERY track in the album.

- **Rename album from album detail**: GIVEN the user is on AlbumDetailScreen, WHEN they tap Edit, change the album name from "Best Of" to "Greatest Hits", and save, THEN every track in that album has its album tag updated AND the album is displayed under the new name.
- **Album artist override preserves per-track artists**: GIVEN an album with feature tracks ("Song A feat. Artist B"), WHEN the user changes the album artist field, THEN only the album and album artist tags are written per track AND individual song artist fields are NOT overwritten.

**R7 — Album Cover Art**: The system MUST allow changing cover art for all tracks in an album in a single operation.

- **Change entire album cover**: GIVEN the user edits the album from AlbumDetailScreen, WHEN they pick a new image for the album cover and save, THEN the image is resized and embedded as cover art in every track file in that album.
- **Partial write failure during album edit**: GIVEN an album with 12 tracks, WHEN writing tags to track #7 fails due to a locked file, THEN tracks 1-6 remain successfully updated AND the user is shown which specific tracks failed AND the system does NOT roll back successful writes.

**R8 — Track-Specific Fields Protected**: The system MUST NOT modify title, track number, disc number, or per-track genre when performing an album-level edit.

- **Album edit preserves track titles and numbers**: GIVEN an album containing "Track A" (track 1), "Track B" (track 2), "Track C" (track 3), WHEN the user changes the album title and cover art, THEN each track retains its original title, track number, and disc number unchanged.

## Error Cases

| Condition | Expected Behavior |
|-----------|-------------------|
| File locked / permission denied | Show "File in use — close other apps", abort write |
| Invalid or corrupt cover image (>20MB or decode failure) | Show "Invalid image", preserve existing cover art |
| SAF stream throws IOException mid-write | Preserve temp file for recovery, show failure message |
| null filePath AND openFileDescriptor fails | Show "Cannot access this file on this device" |
| Audio format not writable (WMA, AAC) | Show "This file format does not support tag writing" |
| Disk full during write-back | Show "Not enough storage space", preserve original file |
