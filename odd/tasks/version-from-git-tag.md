# Version from git tag

## Objective

Any build of a tagged commit must report that tag's version. F-Droid stopped seeing updates
for 1.1.4 because the APK it built declared itself 1.1.3.

## Problem

The 1.1.4 release made the tag the source of truth, but only through a Gradle property:

```kotlin
val appVersionName: String =
    (findProperty("appVersionName") as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: "1.1.3"
```

The GitHub workflow passes `-PappVersionName`. Every other builder does not. F-Droid's metadata
for `com.cvc953.localplayer` runs `gradle: yes` on the tag's commit, i.e. `./gradlew
assembleRelease` with no properties, so it produced 1.1.3 / 10103 — a version F-Droid already
had (`CurrentVersion: 1.1.3`, `CurrentVersionCode: 14`). No new version, no update.

Verified locally with the two real invocations:

| Invocation | `versionName` | `versionCode` |
|---|---|---|
| `./gradlew :app:assembleDebug` (F-Droid) | `1.1.3` | `10103` |
| `./gradlew :app:assembleDebug -PappVersionName=1.1.4` (GitHub) | `1.1.4` | `10104` |

## Why

The version must be derivable from the checkout alone. A release artefact that only knows its
version when a specific CI passes a specific flag is not reproducible anywhere else, and
F-Droid is exactly that "anywhere else".

## Scope

In scope:

- Resolve the version from the git tag when the property is absent.
- Keep `-PappVersionName` as the highest-priority override so the workflow is unchanged.
- Document the two paths in `RELEASE_SETUP.md`.

Out of scope:

- The `Binaries:` URL in the fdroiddata metadata, which expects `localplayer-%v.apk` while the
  releases publish `LocalPlayer-%v.apk`. That file lives in the fdroiddata repository, not
  here. Flagged for a separate decision.

## Design decisions

1. **Exact tag first.** `git describe --tags --exact-match --match "v[0-9]*"` is precisely the
   F-Droid case: the build server checks out the tag. It cannot silently pick a neighbouring
   release.
2. **Nearest tag second.** A development build on a branch gets the last released version
   instead of a stale literal, so a sideloaded test APK still installs over the current one.
3. **Literal last.** Only when git is unavailable, which means the source was unpacked from an
   archive.
4. **`ProcessBuilder` at configuration time.** No configuration cache is enabled in this
   project, and the value is needed by `defaultConfig`.

## Acceptance criteria

- A build of a tagged commit, with no properties, reports that tag's version.
- `-PappVersionName` still wins over any tag.
- A build on a branch reports the nearest tag.
- The full test suite stays green and the APK still assembles.

## Tasks

- [x] T1 `app/build.gradle.kts`: resolution chain property → exact tag → nearest tag → literal.
- [x] T2 `RELEASE_SETUP.md`: document both paths.
- [x] T3 Verification: build with no property, exact tag, and explicit property.

## Checks

- `./gradlew :app:assembleDebug` and read `versionName`/`versionCode` from the merged manifest.
- `./gradlew :app:testDebugUnitTest`

## Delivery

One branch, one pull request. Small, self-contained change to the build script.

## Progress

Started 2026-11-09 on `fix/version-from-git-tag` from master `9ebe7aa`.

Evidence:

- No property: `1.1.4` / `10104` (nearest tag), was `1.1.3` / `10103`.
- Tag `v9.9.9` on a commit distinct from `v1.1.4`: `9.9.9` / `90909`, which is the exact-tag
  path F-Droid exercises. Temp tag and branch removed afterwards.
- `-PappVersionName=1.2.0`: `1.2.0` / `10200`, so the property still wins.

Pending: an F-Droid rebuild to confirm it picks up 1.1.4. That happens on their schedule.

## Revision 2026-11-18: the premise was wrong, superseded by literals

### What the first attempt got wrong

F-Droid did not pick up 1.1.4 or 1.1.5, and the evidence showed why: the tag resolution fixed
what the APK declares, not what F-Droid reads to detect a release. The updater never parsed an
APK.

`UpdateCheckMode: Tags` runs fdroidserver's `check_tags`. It clones the repository, walks the
tags matching the pattern, and for each one runs `common.parse_androidmanifests`, which parses
`app/build.gradle.kts` with static regexes:

```python
vcsearch_g = r'''\b[Vv]ersionCode\s*=?\s*["'(]*([0-9][0-9_]*)["')]*'''
vnsearch_g = r'''\b[Vv]ersionName\s*=?\s*\(?(["'])((?:(?=(\\?))\3.)*?)\1'''
```

They require a numeric `versionCode` and a quoted `versionName`. Since 1.1.4 the file had
`versionCode = appVersionCode` and `versionName = appVersionName`, so both regexes matched
nothing. Reproduced against the real tag content of `v1.1.5`: `[]` and `[]`. With no `vercode`,
`check_tags` aborts with "Couldn't find any version information" and the bot never writes the
metadata. That is why `CurrentVersion` stayed at `1.1.3` / `14` with a last bot commit on
2026-10-28.

Two side findings that were previously misjudged:

- The build clone runs `git fetch --prune --tags --force origin`, so the tags are there and
  `git describe` would resolve `1.1.5` / `10105` if the build ever ran. The tag resolution was
  not useless; it was simply never reached.
- The lowercase `Binaries:` URL is fine. GitHub resolves release assets case-insensitively and
  redirects to `LocalPlayer-1.1.5.apk` with HTTP 200.

1.1.5 cannot be repaired retroactively: the tag content is frozen without literals. F-Droid
skips it and 1.1.6 carries everything.

### What replaced it

Literal values, overrides applied after them:

```kotlin
versionCode = 10106
versionName = "1.1.6"

overrideVersionCode?.let { versionCode = it }
overrideVersionName?.let { versionName = it }
```

`release.yml` gained a "Verify version literals for F-Droid" step that fails when the literals
do not match the tag, so a release cannot ship with them stale.

Tasks:

- [x] T4 `app/build.gradle.kts`: literals in `defaultConfig`, remove the tag-resolution chain.
- [x] T5 `release.yml`: fail the release when the literals disagree with the tag.
- [x] T6 `RELEASE_SETUP.md`: the bump is now part of the release PR.
- [x] T7 Verification: exact fdroidserver regexes on the edited file, overrides, tests, build.

Acceptance criteria that still hold: `-PappVersionName` wins over the literal, and the build
stays reproducible with no properties.
