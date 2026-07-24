# Prebuilt release artifacts

Ready-to-install builds, so you don't have to set up the Pebble SDK or an
Android build environment yourself. These are rebuilt from the source in
`watch/` and `android/` in this repo — see the root `README.md` for how to
build them yourself instead, and for the "vibe-coded" disclaimer that applies
to everything here.

## Versioning scheme

- **Debug builds** (what's here right now) use a `0.x.y-debug` series: the
  minor version bumps on every new debug build we ship (`0.1.0-debug` ->
  `0.2.0-debug` -> ...). The watch app's `package.json` `version` field must
  be strict `X.Y.Z` (pebble-tool rejects a `-debug` suffix there), so only
  the release filename and the Android `versionName` carry the `-debug` tag
  for the watch/phone pair.
- **Release builds** (once there's an actual signed release, not just a
  debug-keystore build) start a fresh series at `1.0.0`, independent of
  wherever the debug series was left off.

## Current build: `0.5.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.5.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.5.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.5.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.5.0-debug.apk` |

`0.5.0-debug`:
- Watch icon (`watch/resources/images/icon.png`) now has a solid opaque
  background instead of a transparent one — a transparent PNG background
  can render as blank in some UIs, and it's a more typical style for
  color-platform icons regardless. If the phone's installed-apps overview
  is still blank after a clean reinstall, that's likely a limitation of
  the current Pebble mobile app version for non-appstore-published
  (sideloaded) apps, not something fixable from this project's side — see
  the root README.
- Both apps now auto-detect Norwegian (Bokmål) as the system
  language and show Norwegian text instead of English when it is: the
  watch via a runtime `i18n_get_system_locale()` check, the phone via a
  standard Android `values-nb` resource set. English remains the
  fallback in every other locale.

### SHA-256 checksums

```
f41f4b419a8f983428fc238fcf3f9419b1d860f369ac4e2a0c940014978e6d98  find-my-phone-watch-v0.5.0-debug.pbw
4775635c7071c9caaf81bb144876a3a50dbcdcaaf31b10b83bb059d34b2881b7  find-my-phone-companion-v0.5.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
