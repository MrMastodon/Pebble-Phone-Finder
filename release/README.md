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

## Current build: `0.11.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.11.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.11.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.11.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.11.0-debug.apk` |

`0.11.0-debug` is a stability/robustness pass from a pre-release code
review — no new features, but several crash and stuck-state paths closed:

- Raising or restoring the alarm volume no longer crashes when the OS
  refuses it as a Do Not Disturb policy change (the app has no
  `ACCESS_NOTIFICATION_POLICY`). The alarm now plays at whatever volume
  is already set instead of taking the process down.
- The service no longer starts a max-volume alarm off a null or unknown
  intent (e.g. when the system recreates it).
- Foreground-service starts are guarded against Android 12+'s background
  start restriction; a refusal is logged to the About screen's
  diagnostics instead of crashing.
- Teardown now runs in a `finally`, so a failure mid-stop can't leak the
  wake lock or strand the ongoing notification.
- The alarm volume is restored even when no sound could be played at all
  (previously it could be left pinned at max permanently).
- `MediaPlayer` is released if `prepare()` fails, and a mid-playback
  error now tears the alarm down instead of holding a wake lock for
  silence.
- Stop taps that arrive when the service isn't already in the foreground
  no longer risk a `RemoteServiceException`.
- Watch: the SELECT arrow's path is built once instead of on every
  redraw, and the system locale is null-checked (Nynorsk and generic
  `no` now get Norwegian text too).

### SHA-256 checksums

```
5ecc00d5e8872adab832c1789ffb663fef13bf25ee0549ad109cd4e6453398d8  find-my-phone-watch-v0.11.0-debug.pbw
4163d1b6ddadd491ced2de98ad39b14a8b99713afe52bc2440b67b29bf56ae4d  find-my-phone-companion-v0.11.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
