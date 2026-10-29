# Pre-release support in the release pipeline

## Objective

Tag-driven releases that support pre-release versions (e.g. `v1.1.4-beta.1`) and publish
them as GitHub pre-releases, with a single source of truth: the git tag.

## Problem

The release pipeline only half-uses the tag:

- `app/build.gradle.kts` hardcodes `versionCode = 14` and `versionName = "1.1.3"`.
- `.github/workflows/release.yml` extracts the version from the tag but uses it only for the
  APK asset name and the changelog lookup. The version that ends up **inside the APK** is the
  Gradle one, so tag and artifact can silently disagree.
- `prerelease: false` is hardcoded, so every tag publishes as a stable release.
- The changelog extraction matches `## [<full version>]` exactly, so a pre-release tag finds
  nothing and falls back to "no changelog found".

## Why

Android rejects an APK whose internal identity disagrees with what the release claims, and a
release tagged `1.1.4-beta.1` that reports `1.1.3` inside is untrustworthy to troubleshoot.
Pre-releases are also the mechanism that keeps risky work out of the stable channel, which
only works if GitHub does not mark them as Latest.

## Scope

In scope:

- Version resolution driven by `-PappVersionName`, with `versionCode` derived from the
  semantic version.
- Pre-release detection, formatting of the release notes, and the `prerelease` flag.
- Changelog fallback from the pre-release version to its base version.
- Correcting `.github/RELEASE_SETUP.md`, which documents secret names that do not match the
  workflow and still instructs manual version bumps.

Out of scope:

- `test-apk.yml` (test builds keep using the Gradle fallback version; unchanged behaviour).
- fastlane store metadata (no Play publishing in this project).
- Branches for pre-releases; the tag alone decides.

## Design decisions

1. **The tag is the single source of truth.** CI passes `-PappVersionName=<tag version>`.
   Gradle keeps a fallback so local builds work without properties.
2. **`versionCode` is derived, not counted.** `MAJOR * 10000 + MINOR * 100 + PATCH`, so
   `1.1.4 -> 10104`. No more manual counter. `10104 > 14`, so the switch upgrades cleanly.
3. **Pre-releases share their base `versionCode`.** `1.1.4-beta.1` -> `10104`, same as
   `1.1.4`. Android accepts an equal-versionCode update, so the stable release installs over
   the pre-release without asking users to uninstall.
4. **Any SemVer pre-release suffix counts.** `-beta.1`, `-rc.2`, `-alpha.1` all map to
   `prerelease: true`. No suffix means a stable release.
5. **Invalid versions fail the workflow loudly** instead of shipping a mislabelled APK.

## Acceptance criteria

- `v1.1.4` produces an APK with `versionName=1.1.4`, `versionCode=10104`, `prerelease=false`.
- `v1.1.4-beta.1` produces an APK with `versionName=1.1.4-beta.1`, `versionCode=10104`,
  `prerelease=true`, and a visible pre-release notice in the release body.
- A pre-release with no `## [1.1.4-beta.1]` changelog section uses the `## [1.1.4]` section.
- A malformed version fails before building.
- Local builds without any `-P` property still resolve to the previous fallback.
- The existing unit test suite stays green.

## Tasks

- [x] T1 `app/build.gradle.kts`: read `-PappVersionName`, derive `versionCode` from the
      version, fail loudly on a malformed version.
- [x] T2 `.github/workflows/release.yml`: parse the tag into `VERSION` / `BASE_VERSION` /
      `PRERELEASE`, validate the format, pass `-PappVersionName`, add the changelog fallback,
      make the `prerelease` flag dynamic, prepend the pre-release notice.
- [x] T3 `.github/RELEASE_SETUP.md`: fix the stale secret names, document the tag convention
      and the pre-release flow, drop the manual version-bump instructions.
- [x] T4 Verification: merged-manifest version checks for both a stable and a pre-release
      version, shell simulation of the version parsing for valid and invalid tags, YAML
      parse check, full unit test suite, and the release-workflow review.

## Checks

- `./gradlew :app:processDebugMainManifest -PappVersionName=1.1.4-beta.1` then grep the merged
  manifest for `versionName` / `versionCode`.
- Same for `1.1.4` and for no property at all.
- Run the version-parsing shell block in isolation against valid and invalid tags.
- `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/release.yml'))"`.
- `./gradlew testDebugUnitTest` -> 31 tests, 0 failures.

## Delivery

Forecast: ~110 authored changed lines across 3 files, under the ~400-line heuristic. One
branch, one pull request. Independent from the TTML hotfix branch.

## Progress

All tasks implemented and verified on 2026-10-28. Not committed yet.

Verification evidence:

- Merged manifest (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`):
  - `-PappVersionName=1.1.4` -> `versionCode=10104`, `versionName=1.1.4`
  - `-PappVersionName=1.1.4-beta.1` -> `versionCode=10104`, `versionName=1.1.4-beta.1`
  - `-PappVersionName=2.0.0-rc.2` -> `versionCode=20000`, `versionName=2.0.0-rc.2`
  - no property -> `versionCode=10103`, `versionName=1.1.3` (fallback preserved)
  - `-PappVersionName=foo` and `-PappVersionName=1.1` -> build fails with
    "Versión inválida ..."
- Version parsing step executed directly from `release.yml` against 12 cases:
  `v1.1.4`, `v1.1.4-beta.1`, `v2.0.0-rc.2`, `v10.20.30`, manual `1.1.4`, plus six invalid
  inputs (`1.1`, `foo`, `1.1.4.1`, `1.1.4-`, `v1.1.4`, `1.1.4+build.5`) and an empty manual
  input. 12/12 as expected.
- Changelog step executed from `release.yml` against the real CHANGELOG files: stable
  section, pre-release fallback to the base version, pre-release with its own section, and
  the unknown-version branch. 4/4 as expected.
- `yq` parses `release.yml`; all 10 build steps still present and the release step receives
  `prerelease: ${{ steps.extract_version.outputs.PRERELEASE }}`.
- `./gradlew testDebugUnitTest` -> 31 tests, 0 failures, 0 errors, 0 skipped.

Note: `test-apk.yml` is untouched. It builds with the Gradle fallback, so test APKs report
`1.1.3` / `10103` until the fallback changes — same class of mismatch that already existed
before this change.

Next step: review the diff, then commit on a feature branch and open the PR.
