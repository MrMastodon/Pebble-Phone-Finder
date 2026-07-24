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

## Current build: `0.4.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.4.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.4.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.4.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.4.0-debug.apk` |

`0.4.0-debug` is a UI/polish pass now that the core round trip works
end to end (confirmed on real hardware with `0.3.0-debug`):
- Watch: bigger status text, a 25x25 app icon (shown both in the watch's
  app launcher list and, since this app isn't published through an
  appstore, in the phone's list of installed watchapps too — see
  `watch/resources/images/icon.png`), and an orange arrow drawn at
  SELECT-button height pointing at it.
- Phone: the diagnostics panel added in `0.2.0-debug` is now hidden by
  default behind a "Show diagnostics" button instead of always visible.

### SHA-256 checksums

```
d14da662822a077cfba6c177bdcbb26f6f5a5492349a4fccd408804eddf97aa3  find-my-phone-watch-v0.4.0-debug.pbw
71133beb4f1f942fd920359f22003b8f1365640618ac64a48459721fe3a06068  find-my-phone-companion-v0.4.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
