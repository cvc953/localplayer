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
