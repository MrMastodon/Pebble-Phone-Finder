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

## Current build: `0.6.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.6.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.6.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.6.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.6.0-debug.apk` |

`0.6.0-debug` adds a "Choose alarm sound" button on the phone, using
Android's built-in sound picker (`RingtoneManager.ACTION_RINGTONE_PICKER`,
scoped to alarm-type sounds, silent option hidden). Deliberately limited to
the phone's built-in sounds rather than arbitrary files: built-in sound
URIs live in a shared system database with no expiring permission grant,
so there's no extra permission-handling complexity, and playback silently
falls back to the app's bundled default tone if the chosen sound is ever
unavailable (e.g. removed by an OS update). The watch app itself is
unchanged in this release; only rebuilt to keep the pair's version number
in sync.

### SHA-256 checksums

```
8f6c6e4b77224dbfb1a58b25ed38764b5ad0755d3e91fe2420193b999f142124  find-my-phone-watch-v0.6.0-debug.pbw
49032d8b9121c992c96a1ec4135b2fc3d6f6eccfb6cd81245243e27d2e75df4d  find-my-phone-companion-v0.6.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
