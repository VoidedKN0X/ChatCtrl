# ChatCtrl

## Dependency verification

Gradle checks every library and plugin download against its SHA-256 in `gradle/verification-metadata.xml` (checksums only, no PGP). When a dependency or plugin changes, run the `verification-metadata.yml` workflow (`gh api -X POST repos/VoidedKN0X/ChatCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), decode the file from its log, check the diff (only Maven Central, Google's Maven and the two GitHub-hosted repos in `settings.gradle.kts` for the Rust SDK and rustls-platform-verifier) and commit it in the same push. Never turn verification off or make it lenient. The workflow and CodeQL use a placeholder for `element-call/dist`; only the build workflow builds Element Call.

`settings.gradle.kts` lists only `google()` and `mavenCentral()` for plugins, not `gradlePluginPortal()`: the portal serves different files for the Kotlin plugin markers, which made verification fail whenever Maven Central had a hiccup. Don't add it back unfiltered (see the comment in that file).

## Build and signing (CI)

`.github/workflows/build.yml` has five jobs: `version` (is this a release?), `element-call` (builds Element Call from source, no secrets), `build` (Gradle, **no secrets**, `UNSIGNED_RELEASE=true` gives `app-release-unsigned.apk`; runs every check), `sign` (no checkout, no Gradle: `zipalign` + `apksigner` with the release key, then checks the signer against `EXPECTED_CERT_SHA256` and that the APK's contents didn't change) and `release` (attestation, release notes, publish).

- Never give the `build` or `element-call` job a secret, and never run Gradle or check out the repo in `sign`. New build checks (tests, lint, APK checks) go in `build`.
- `EXPECTED_CERT_SHA256` in `build.yml` must match the fingerprint in `SECURITY.md` and `README.md`; the build fails if it doesn't.
- No Gradle cache in any workflow (`cache-disabled: true`): a cache saved by a failed run used to break every later build. The pnpm cache of the `element-call` job is a different cache and stays.

## Encryption at rest

What ChatCtrl stores itself (outside the Matrix SDK) and how:

- Encrypted with the Keystore key `chatctrl_sessions` (`data/SecureBox`): the saved sign-in (`session` prefs, also sealed by the app lock vault when that applies), the drafts index (`drafts` prefs) and the recent custom servers (`settings` prefs, `recent_servers_enc`; `Prefs.encryptRecentServers` moves an old plain `recent_servers` over at start, off the main thread). Read and write these off the main thread where you can.
- The SDK's stores under `files/sessions` and `cache/sessions` (and the search index) are encrypted by the SDK with the session's database key. Never touch them.
- Plain on purpose: `cache/media`, `cache/upload`, `cache/shared` (opened by viewers, players and other apps by path or FileProvider; purged after a day / an hour and on lock), `cache/osmdroid` (read by the map library), `notified`, `push_queue`, `push`, `unifiedpush` and `notify_links` prefs (read and written on the push and launch paths, which must not wait for or depend on a key), `connection.log` (appended to from everywhere; no names, servers or content) and plain settings.
- There is no separate data key or sealed-file format in ChatCtrl (unlike RideCtrl): nothing it stores as its own plain files needed one.

## Releasing

- A release is published only when `versionName` in `app/build.gradle.kts` is bumped. Bump `versionCode` by 1 at the same time.
- Tags are `vX.Y.Z` and releases are named "ChatCtrl X.Y.Z". Never add a build number (`-build.N`, "(build N)").
- Use the same version for every change that ships together; README, licence and CI changes don't need a bump.

## Changelog

Every new version needs a detailed entry at the top of `CHANGELOG.md`, under a heading for that version (match the existing headings). CI refuses to release without one, and the entry becomes the release notes on GitHub, in Obtainium and in the CtrlF-Droid repo.

- Write for people using the app, not for developers: what's new, what changed, what was fixed.
- Cover every user-visible change in the version, one bullet each, with enough detail to understand it without opening the app.
- Group with `### New`, `### Changed`, `### Fixed` and `### Security` when there's more than a couple of items.
