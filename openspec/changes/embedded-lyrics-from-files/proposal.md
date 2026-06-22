# Proposal: Embedded lyrics reading from audio file metadata

## Intent

Add a 3rd fallback to lyrics loading: read lyrics embedded in audio file metadata (ID3v2 USLT/SYLT for MP3, Vorbis LYRICS for FLAC/OGG, ©lyr for M4A). Users with tagged files get lyrics without needing external `.lrc`/`.ttml` files.

## Scope

### In Scope
- Add `jaudiotagger` dependency for tag reading
- New `EmbeddedLyricsExtractor` in `util/` package
- Insert extraction as step 3 in `LyricsViewModel.loadLyricsForSong()` between LRC fallback and error state
- Handle **three cases** of extracted text:
  - **SYLT frames** (synchronized MP3 lyrics with per-line timestamps) → map directly to `LrcLine(timeMs = ...)`
  - **USLT/Vorbis/©lyr with LRC-formatted text** (plain text field that happens to contain `[MM:SS.xx]` timestamps) → parse with existing `LrcParser`
  - **Plain text** (no timestamps) → distribute lines evenly across `song.duration`

### Out of Scope
- UI changes — embedded text reuses existing `LrcLine` rendering via `LyricsView`
- Writing/editing audio tags
- CRSF-based tag reading

## Capabilities

### New Capabilities
- `embedded-lyrics`: Read lyrics from audio metadata as a fallback lyrics source. Supports both synchronized (SYLT) and unsynchronized (USLT/Vorbis/©lyr) lyrics.

### Modified Capabilities
None.

## Approach

1. **Dependency**: Add `net.jthink:jaudiotagger:3.0.2` to `app/build.gradle.kts`
2. **New file**: `app/src/main/java/com/cvc953/localplayer/util/EmbeddedLyricsExtractor.kt`
   - `fun extractLines(audioFilePath: String, mimeType: String?, songDuration: Long): List<LrcLine>?`
   - Uses `org.jaudiotagger.audio.AudioFileIO.read()`
   - **Priority inside the extractor**:
     a. Try synchronized first: SYLT frames (MP3) → map timestamps to `LrcLine`
     b. If no SYLT, try text fields (USLT for MP3, Vorbis LYRICS for FLAC/OGG, ©lyr for M4A)
        - Check if text matches LRC pattern (`[MM:SS.xx]`) → use `LrcParser`
        - If not LRC → split by lines, distribute evenly
     c. Return null if nothing found
3. **Insertion point** in `LyricsViewModel.loadLyricsForSong()` (~line 95): after LRC fallback fails, before error state, call extractor
4. No embedded lyrics found → existing error state (no change)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/build.gradle.kts` (~line 96) | Modified | Add jaudiotagger dependency |
| `viewmodel/LyricsViewModel.kt` (~line 95) | Modified | Insert embedded lyrics extraction step |
| `util/EmbeddedLyricsExtractor.kt` | New | Tag reading and lyrics extraction logic |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| LGPL license compatibility | Low | LGPL 2.1+ permits dynamic linking in commercial apps |
| ~500KB APK size increase | Medium | Single jar; negligible vs. total app size |
| Encoding issues (ID3v2) | Low | jaudiotagger handles charset detection internally |
| File read failure on corrupt tags | Low | Wrapped in try-catch; falls back to error state |

## Rollback Plan

Remove jaudiotagger from `build.gradle.kts`, delete `EmbeddedLyricsExtractor.kt`, revert `LyricsViewModel.kt` changes (`git checkout` those files).

## Dependencies

- `net.jthink:jaudiotagger:3.0.1` (Maven Central)

## Success Criteria

- [ ] MP3 with SYLT frame shows synchronized lyrics with correct timestamps
- [ ] MP3 with USLT frame (LRC-formatted text) shows synchronized lyrics
- [ ] MP3 with USLT frame (plain text) shows lyrics distributed evenly
- [ ] FLAC/OGG with Vorbis LYRICS tag shows lyrics
- [ ] M4A with ©lyr atom shows lyrics
- [ ] Untagged file shows existing error state ("No se encontraron letras")
- [ ] Embedded lyrics respect priority: external .ttml > .lrc > embedded
