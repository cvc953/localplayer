# Library scan hardening

## Objective

A MediaStore row with a NULL metadata column, a vendor column the provider refuses, or a
failed query must not empty the whole library. The scan should skip the bad row, fall back
to a reduced projection, and never overwrite a good library with a failed scan.

## Problem

Reported in issue #59 (Android 10, Huawei P20 Pro, fresh install of 1.1.4): Settings →
Configured Folders reports 179 songs for the selected folder while the Songs tab is empty
and "refresh library" changes nothing.

The folder counter and the scan share the same selection (`LibraryDataSource.buildSelection`),
so MediaStore does return 179 rows. The count only projects `_ID`; the scan projects 13
columns and maps each row into `Song`.

Two defects explain the asymmetry:

1. `LibraryDataSource.kt:220-222` assigns `Cursor.getString(...)` (platform type `String!`)
   directly to the non-null `Song.title/artist/album`. One NULL value throws NPE inside the
   row mapping, aborting the whole cursor loop. `SongRepository.kt:179-181` swallows it and
   returns the previous list, which is empty on a fresh install. The same defect was already
   fixed for `GENRE` in `0e9f807`; the other columns were left unguarded.
2. The full projection includes `sample_rate` plus vendor-sensitive columns, and the retry
   only catches `IllegalArgumentException` (`LibraryDataSource.kt:192-193`). Any other
   exception reaches the same catch-all.

A third defect makes any failure destructive: `currentSignature()` never returns null, so
the guard at `SongRepository.kt:163` is dead code and a failed scan replaces a populated
library with an empty one and persists it.

## Why

Losing the entire library because one file lacks an album tag is disproportionate, and the
user cannot tell it apart from "no songs found". The count in Settings proves the data is
there, which makes it worse: the app contradicts itself.

## Scope

In scope:

- Null-safe column reads and a pure, testable row mapper.
- Per-row failure isolation: a bad row is skipped and logged, not fatal.
- Projection fallback for any query failure, not just `IllegalArgumentException`.
- `querySongs()` signals total failure instead of silently returning an empty list.
- `currentSignature()` returns null when a volume query fails, making the existing
  preserve-previous guard reachable.
- Regression tests.

Out of scope:

- The favorites crash reported in the same issue. Needs the logcat stack trace; the read
  paths were reviewed and are guarded, so guessing a fix would be speculation.
- The `duration < 30 s` filter. It could produce the same symptom on a device whose
  MediaStore reports `DURATION = 0`, but that is a different cause and needs device evidence.

## Design decisions

1. **Sanitize at the boundary.** `songFromRow` takes nullable metadata and substitutes an
   empty string, so the non-null `Song` contract is satisfied without throwing.
2. **Missing columns are not fatal.** Use `getColumnIndex` and treat `-1` as absent instead
   of `getColumnIndexOrThrow`, except for `_ID`, which MediaStore always provides.
3. **Never persist a failed scan.** `refresh` returns early when the signature is null and
   the scan produced nothing, leaving both state and cache untouched.
4. **A genuinely empty library still clears.** The guard only fires when the source reported
   failure, so deleting every song keeps working.

## Acceptance criteria

- A row with NULL title/artist/album/genre produces a `Song` with empty strings.
- A query failure keeps the previous library and does not write the cache.
- A successful scan that returns no songs still clears the library.
- The existing suite stays green.

## Tasks

- [x] T1 `LibraryDataSource.kt`: extract `songFromRow`, null-safe column reads, per-row
      isolation, projection fallback, failure signalling from `querySongs`/`currentSignature`.
- [x] T2 `SongRepository.kt`: make the preserve-previous guard reachable and skip persisting
      a failed scan.
- [x] T3 Tests: null columns, preserve on failure, preserve on exception, clear on a genuine
      empty scan.
- [x] T4 Verification: new tests, full `testDebugUnitTest`, review of the diff.
- [x] T5 `MediaStoreCursorTest.kt`: extract `readSongsFromCursor` so a fake cursor can
      reproduce the provider behavior on the JVM, without a device.

## Checks

- `./gradlew testDebugUnitTest`
- Diff review of the cursor loop and the refresh early-return.

## Delivery

Forecast: ~230 authored changed lines across 2 source files and 1 test file, under the
~400-line heuristic. One branch, one pull request.

## Progress

Started 2026-11-01 on `fix/library-scan-hardening` from master `9ebe7aa`.

Implementation evidence:

- RED: `LibraryScanHardeningTest` failed to compile with `Unresolved reference 'songFromRow'`.
- GREEN: `LibraryScanHardeningTest` 5/5 passing.
- Full suite: 36 tests, 0 failures (`:app:testDebugUnitTest`).
- Diff reviewed: cursor loop reads every column through `getColumnIndex` plus a tolerant
  reader, so a provider that hides a column no longer aborts the scan; only `_ID` still
  throws when absent, which MediaStore never does.

Reproduction without a device (`581fa2e`):

- `readSongsFromCursor(cursor, contentUri, out)` is a standalone function over `Cursor`.
- `MediaStoreCursorTest` drives it with a fake cursor that returns NULL metadata and with a
  projection that hides a column.
- Restoring the previous row mapping makes all three tests fail (the rows with NULL tags are
  dropped); with the hardened mapping they pass. The failure mode is therefore reproducible
  on the JVM, even though the reporter's device is not available.

Not proven: that the reporter's Huawei P20 Pro returns NULL in those columns. The fix removes
the failure mode, but only a logcat or a database dump from that device can confirm the cause.
