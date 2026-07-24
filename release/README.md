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

## Current build: `0.3.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.3.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.3.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.3.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.3.0-debug.apk` |

`0.3.0-debug` fixes a real bug found using `0.2.0-debug`'s diagnostics on
real hardware: the watch's `dict_write_uint8()` arrives at the phone as
`PebbleDictionaryItem.UInt32`, not `UInt8` as the Android code assumed, so
the command was silently dropped even though the message was delivered
correctly end to end. `PebbleListenerService` now reads whichever integer
width actually shows up (`PebbleDictionaryExt.kt`). The `0.2.0-debug`
diagnostics panel and watch-side ack states are unchanged and still present.

### SHA-256 checksums

```
b02c7e061893b51a6d80ecea4c0d7e1a3ee76ccea7ba66e75cda8e8869aaa175  find-my-phone-watch-v0.3.0-debug.pbw
17f47e5dae881a47a54b27c530cb226220b0ffc23a72c8aed18958d070a1f4af  find-my-phone-companion-v0.3.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
