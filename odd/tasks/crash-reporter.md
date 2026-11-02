# Crash reporter

## Objective

Make a crash diagnosable without adb, logcat or developer tools. When the app dies from an
uncaught exception, it must leave the stack trace on disk and show it to the user on the next
launch, with a button that copies the text.

## Problem

Issue #59 reports that opening the Favorites playlist crashes, but there is no stack trace
anywhere. The app installs no `Thread.setDefaultUncaughtExceptionHandler` and uses no crash
reporting service, so the only evidence lives in a logcat the reporter will never run.

The favorites code path was audited: `PlaylistDetailScreen` and `PlaylistsScreen` resolve
playlist IDs with `mapNotNull`, `PlaylistHeader` guards both `.first()` calls with
`isNotEmpty()`, and `PlaylistAlbumArt` falls back to the default cover. Nothing there can be
fixed honestly without knowing which frame threw.

## Why

A user who cannot produce a stack trace cannot report a bug, and a maintainer who cannot
reproduce it cannot fix it. This closes that gap permanently: the next report arrives with
the exception type, the message, the device, and the exact frames.

## Scope

In scope:

- Capture uncaught exceptions and write one file per crash in internal storage.
- Keep the newest reports and delete the rest.
- On the next launch, show the most recent report with "Copy" and "Discard".
- Unit tests for report formatting, writing, retention, and ordering.

Out of scope:

- Sending anything over the network. The user copies the text and decides where it goes.
- Crashes in other processes (the widget and the media service). They inherit the handler
  only if they share the process, which they do today; a separate process would need its
  own installation.
- Native crashes (`DEBUG`) and ANRs.

## Design decisions

1. **Internal storage.** `filesDir` needs no permission and is always available, unlike
   `getExternalFilesDir`, which can be null when the volume is not mounted.
2. **Never swallow the crash.** The handler writes, then delegates to the previous handler,
   so the system still kills the process and shows its own dialog.
3. **Bounded.** At most five reports. A crash loop must not fill the disk.
4. **Sort by file name.** Names carry the millisecond timestamp, so ordering does not depend
   on `lastModified`, which some file systems round.
5. **Testable core.** Formatting, writing, retention and listing operate on a `File`
   directory, so they run on the JVM with a temporary folder. Only `install` touches Android.

## Acceptance criteria

- An uncaught exception leaves a report with type, message, thread, device, and frames.
- The handler delegates to the previous handler, so the process still dies.
- At most five reports are kept, newest first.
- The dialog shows the newest report and offers copy and discard.
- The existing suite stays green.

## Tasks

- [x] T1 `CrashReporter`: capture, format, write, retention, listing, read, delete.
- [x] T2 Wire `install` in `LocalPlayerApp.onCreate`.
- [x] T3 `CrashReportDialog` shown from `MainActivity`, with copy and discard.
- [x] T4 Strings in `values`, `values-en`, `values-it`.
- [x] T5 Tests: formatting, writing, retention, ordering.
- [x] T6 Verification: new tests, full `testDebugUnitTest`, diff review.

## Checks

- `./gradlew testDebugUnitTest`
- Manual: throw from a debug build and confirm the dialog appears on the next launch.

## Delivery

Forecast: ~200 authored changed lines across two new source files, two touched files, three
string files and one test. Under the ~400-line heuristic. One branch, one pull request.

## Progress

Started 2026-11-01 on `feature/crash-reporter` from master `9ebe7aa`.

Evidence:

- RED: `CrashReporterTest` failed to compile with `Unresolved reference 'formatReport'`.
- GREEN: 5/5 in `CrashReporterTest`.
- Full suite: 44 tests, 0 failures. `:app:assembleDebug` builds.

Pending: the manual check. Throwing from a debug build and confirming the dialog on the next
launch needs a device or emulator, neither of which is available here.
