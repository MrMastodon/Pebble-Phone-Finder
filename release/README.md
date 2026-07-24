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

## Current build: `0.7.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.7.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.7.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.7.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.7.0-debug.apk` |

`0.7.0-debug` adds a "Reset to default sound" button, shown only once a
non-default sound is chosen. `0.6.0-debug`'s sound picker deliberately hides
the system picker's own "Default" option (it would point at the phone's
general default alarm, not this app's bundled tone), so there was
previously no way back to the bundled sound once you'd picked something
else. The watch app itself is unchanged in this release; only rebuilt to
keep the pair's version number in sync.

### SHA-256 checksums

```
cf737d3a63ba01f12787f6c2d882b0683840b939c13acfe8597bece64421f94c  find-my-phone-watch-v0.7.0-debug.pbw
156867d67fa4283ad60aea2cd0831434c057ba6b8acf19b11056f5afd3781a0b  find-my-phone-companion-v0.7.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
