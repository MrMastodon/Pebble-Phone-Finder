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

## Current build: `0.8.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.8.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.8.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.8.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.8.0-debug.apk` |

`0.8.0-debug` adds a small always-visible status line at the top of the
watch screen showing whether the watch is currently connected to the
phone's Pebble app (green "Phone: connected" / red "Phone: disconnected"),
using `connection_service_subscribe()`. This is independent of the
existing per-press ack feedback in the big status text — it updates live
whenever the Bluetooth link itself changes, not just after pressing SELECT.
The phone app itself is unchanged in this release; only rebuilt to keep
the pair's version number in sync.

### SHA-256 checksums

```
04254c877da2f6e80c1e7ec66e3cc62fc1be14e3e7f1b159d70968da4908dc97  find-my-phone-watch-v0.8.0-debug.pbw
2e8a06b742354f9496ec716cc8f1984affee7503aa92a52395a5e4431ec497fc  find-my-phone-companion-v0.8.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
