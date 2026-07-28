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

## Current build: `0.12.0-debug`

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v0.12.0-debug.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v0.12.0-debug.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v0.12.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v0.12.0-debug.apk` |

`0.12.0-debug` finishes the pre-release review with the remaining
security and ANR items:

- **Alarm delivery is pinned to one Pebble host app.** PebbleKitAndroid2
  otherwise accepts messages from *any* installed app claiming to be a
  Pebble host, so a malicious app could set off the alarm. The normal case
  (exactly one host app installed) is pinned silently at startup, so
  nothing changes for you; you're only prompted if there's genuine
  ambiguity. **The About screen now shows which app is pinned** — that
  line is the only way to confirm the lockdown is actually live, since
  the alarm behaves identically either way.
- Watch-connection status is collected off the main thread. The library
  marks that call `@WorkerThread` and it goes over binder, so collecting
  it on the main thread risked ANRs — a metric Play Console tracks.
- Backup rules exclude the diagnostics log and the pinned-host-app state,
  so neither is carried to the cloud or inherited by a new device.

### Verifying the lockdown after installing

Open **About** and check the "Alarm accepted from" line. On a normal setup
it should name the official Pebble app package (`coredevices.coreapp`). If
it says nothing is pinned, the alarm still works but is not locked down.

`0.11.0-debug` was a stability/robustness pass from the same review — no
new features, but several crash and stuck-state paths closed:

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
330b499c4cf931e42aa4d58841c57bc0d9b8dbe170886cc7d686e655e3c6af13  find-my-phone-watch-v0.12.0-debug.pbw
85713212471ae5403cce34ba4e87799deb1436f643a989d11561f4639ca4259f  find-my-phone-companion-v0.12.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
