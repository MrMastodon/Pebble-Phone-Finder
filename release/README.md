# Release artifacts

## Versioning scheme

- **`versionName`** restarted at `1.0.0` for the first real release, after
  the `0.x.y-debug` series used during development.
- **`versionCode` did not restart with it.** It only ever counts upward —
  Play rejects an upload whose `versionCode` isn't higher than the last one.
  It carried on from the debug series, so `1.0.0` is `versionCode` 17.
- The watch app's `package.json` `version` must be strict `X.Y.Z`;
  pebble-tool rejects anything else, so it carries no suffix.

## 1.0.0 — the first release

| File | Where it is | Status |
|------|-------------|--------|
| `phonefinder-watch-v1.0.0.pbw` | here, in this folder | **Ready to publish** to the Rebble appstore as-is. Pebble apps aren't signed, so this is the finished article. |
| `phonefinder-companion-v1.0.0-UNSIGNED.aab` | *not* in this repo | Built and verified, but **unsigned** — it needs the upload key, which lives only with the developer and must never be committed. Rebuild or sign it locally, see below. |

`versionCode 17` / `versionName 1.0.0`, verified from the merged manifest
and from a built APK (`aapt2 dump badging`), not just from the build file.

### Why no ready-to-install Android download here

Earlier versions of this folder carried a debug APK anyone could sideload.
A release build can't work that way: it has to be signed with the upload
key, and putting that key — or anything signed with it — in a public repo
would hand over the ability to publish as this app. So the Android half now
stops at "build it yourself, sign it with your own key".

### Producing the signed bundle

With the Android build environment set up, this is one step. Put the
credentials in `android/keystore.properties` (gitignored — copy
`keystore.properties.example`) and:

```
cd android
gradle bundleRelease
```

The output lands at `app/build/outputs/bundle/release/app-release.aab`,
signed. The build prints a warning if it couldn't find the credentials and
silently produces an unsigned bundle instead, so read that line.

Without an Android toolchain, an already-built unsigned `.aab` can be
signed with nothing but a JDK — App Bundles use JAR signing:

```
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore upload-keystore.jks \
  phonefinder-companion-v1.0.0-UNSIGNED.aab upload
jarsigner -verify -verbose:summary phonefinder-companion-v1.0.0-UNSIGNED.aab
```

The trailing `upload` is the **key alias**, not a flag — omit it and
jarsigner answers "Please specify alias name". Use whatever alias you
created; `keytool -list -keystore upload-keystore.jks` prints the aliases in
a keystore if you've forgotten.

On Windows PowerShell, `\` is not a line continuation (its continuation
character is a backtick), so run it as one line:

```powershell
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore upload-keystore.jks phonefinder-companion-v1.0.0-UNSIGNED.aab upload
```

A successful verify prints `jar verified`.

### What's in the bundle

R8 is on for release builds: it strips the APK from 5.7 MB to 2.3 MB, and
the bundle is 2.8 MB before Play splits it. Those splits are per-ABI —
`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` — because DataStore pulls in one
native library, so a real device downloads noticeably less than any of
these numbers.

The R8 keep rules were validated on real hardware; see `docs/RELEASE.md`.

### SHA-256 checksums

```
2197481bbeaf326ce8b18d757b7c6666dd7a1480332bc95b896825cb50296ae9  phonefinder-watch-v1.0.0.pbw
7e9a14874b180030cfaee0447a2bcf8582058a626b6990545d30800ced2c2650  phonefinder-companion-v1.0.0-UNSIGNED.aab
```

The `.aab` checksum is for the unsigned bundle as built here. Signing
changes the file, so it won't match afterwards — that's expected.

## History

Development ran through a `0.x.y-debug` series. The notable steps:

- **0.16.0** — R8 enabled for release builds, with keep rules written
  against what PebbleKitAndroid2 actually needs. It ships no consumer
  rules of its own while doing IPC across AIDL and Parcelables resolved by
  class name, so renaming them would have failed silently at runtime.
  Validated on real hardware.
- **0.15.0** — renamed to **PhoneFinder**, treated as a brand name and left
  untranslated in the Norwegian UI. The `applicationId` and the
  notification channel *ID* deliberately kept their old values.
- **0.14.0** — replaced the placeholder icons. The launcher and
  notification icons had been system drawables; all icons and store banners
  are now generated from one motif by `tools/generate_assets.py`.
- **0.13.0** — reviewed the code 0.12.0 added, and moved a PackageManager
  call off the main thread.
- **0.12.0** — pinned alarm delivery to a single Pebble host app, moved
  watch-connection polling off the main thread, added backup rules.
- **0.11.0** — closed the crash and stuck-state paths found in a code
  review: a Do Not Disturb `SecurityException` that took the process down,
  a null intent that could start a max-volume alarm, an unguarded
  foreground-service start, a wake lock that leaked if teardown threw, and
  a `MediaPlayer` leak.
- **0.3.0** — fixed the bug that stopped the whole thing working: the
  watch's `dict_write_uint8()` arrives as `UInt32`, not `UInt8`, so the
  command was silently dropped.
