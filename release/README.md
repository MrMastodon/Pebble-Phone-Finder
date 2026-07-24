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

## Current build: `0.2.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.2.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.2.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.2.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.2.0-debug.apk` |

`0.2.0-debug` adds diagnostics for tracking down why a watch button press
might not reach the phone: the watch now shows whether the phone's
Bluetooth stack actually acknowledged the message ("Playing (sent)" vs
stuck on "Sending..." vs "Not connected"), and the companion app logs every
`PebbleListenerService` callback (app opened/closed, any message, even for
a UUID that isn't ours) to an on-screen diagnostics panel in `MainActivity`
— reopen the app after pressing the watch button to see it.

### SHA-256 checksums

```
d1d51e1c5188c5ea7258325ab38c464a6898144801c7e0291cc982bc07a07e5b  find-my-phone-watch-v0.2.0-debug.pbw
6de307b06489e7c0da9e0dc8f98d6ad9eb42f1a413594d28fa348c8f7ae09c6e  find-my-phone-companion-v0.2.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
