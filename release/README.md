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

## Current build: `0.9.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.9.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.9.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.9.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.9.0-debug.apk` |

`0.9.0-debug`, prepping for the first real release build: adds an About
screen (`AboutActivity`) reachable from a new "About" button on the main
screen, showing the app name/version and "Developed by MrMastodon". The
"Show diagnostics" panel has moved off the main screen and now lives on
this About page instead, out of the way of everyday use but still
reachable for troubleshooting. A "Buy me a coffee" button is planned for
this screen but not wired up yet - pending the actual link. The watch app
itself is unchanged in this release; only rebuilt to keep the pair's
version number in sync.

### SHA-256 checksums

```
9341bdafb8e6f502c9650238dd339ce97df7345328883de73fba9e21aab9aed4  find-my-phone-watch-v0.9.0-debug.pbw
79b8b280c4e02584daa1094da224bbbe27efe96a5b11c859a846705693d6e55e  find-my-phone-companion-v0.9.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
