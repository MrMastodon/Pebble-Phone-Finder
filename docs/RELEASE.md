# Release checklist

Working list for the first published release. Nothing here is urgent — the
debug series (`0.x.y-debug`) is what's in use today; see `release/README.md`
for the versioning scheme.

## Android — Google Play

- [x] **1. Release signing configured.** `app/build.gradle.kts` reads
  credentials from `android/keystore.properties` (gitignored) or from
  `FMP_*` environment variables, and leaves the build unsigned if neither
  is present. See "Signing a release build" below.
- [ ] **2. Generate the upload key** and record it somewhere safe. This is
  yours to create and keep — see the warning below.
- [ ] **3. Replace the launcher icon.** The app currently uses
  `@android:drawable/ic_lock_silent_mode_off`, a *system* drawable. That is
  not acceptable for a published app: it's low resolution, it isn't a real
  adaptive icon, and its appearance is not guaranteed across OEMs or OS
  versions. Needs a proper adaptive icon (foreground/background layers) plus
  a 512×512 PNG for the store listing.
- [ ] **4. Switch to the 1.0.0 release series.** `versionName = "1.0.0"`,
  and `versionCode` continuing upward from wherever the debug series ended
  — never back down to 1. Play rejects a `versionCode` that isn't higher
  than the last one uploaded.
- [ ] **5. Decide on R8/minify.** Currently `isMinifyEnabled = false`.
  Enabling it needs `proguard-rules.pro` checked against PebbleKitAndroid2
  and DataStore (both use reflection/serialization), and the resulting
  build re-tested on a real device — a broken keep rule shows up at
  runtime, not at build time.
- [ ] **6. Build an App Bundle**, not an APK: `gradle bundleRelease`. Play
  requires `.aab` for new apps.
- [ ] **7. Test the signed release build on a real device** before
  uploading. The debug build has been the only thing tested so far.
- [ ] **8. Play Console — foreground service declaration.** The app
  declares the `mediaPlayback` foreground service type. Play asks for a
  justification and a demo video for FGS types; be ready to explain that
  the service plays a user-triggered alarm tone.
- [ ] **9. Play Console — Data safety form.** The app collects and
  transmits nothing; the diagnostics log stays on the device and is
  excluded from backup. Answer accordingly.
- [ ] **10. Privacy policy URL.** Required by Play even for apps that
  collect nothing. A short static page saying exactly that is enough.
- [ ] **11. Store listing**: title, short/full description (NO + EN, to
  match the app's own localisation), screenshots, feature graphic.
- [ ] **12. Content rating questionnaire** and target-audience declaration.

## Pebble — Rebble appstore

- [ ] **13. Appstore listing assets.** Separate from the 25×25 `menuIcon`
  bundled in the `.pbw` — the store listing has its own icon and screenshot
  requirements. This is also the likely fix for the blank icon in the
  phone's installed-apps list (see the root README).
- [ ] **14. Publish the `.pbw`** under the MrMastodon developer account.

## Signing a release build

Generate the upload key once:

```
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

> **Back this file and its passwords up, outside the repo.** With Play App
> Signing, Google holds the app signing key and this is the *upload* key —
> a lost upload key can be reset by Google support, but it's a slow detour.
> Losing it with Play App Signing disabled means you can never update the
> app under the same listing again. Treat it like a password you cannot
> reset.

Then copy `android/keystore.properties.example` to
`android/keystore.properties` and fill in the four values. Both that file
and `*.jks` are gitignored.

```
cd android
gradle bundleRelease      # or assembleRelease for a plain APK
```

Verify what you're about to upload is actually signed:

```
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

An output filename ending in `-unsigned.apk` means the credentials weren't
picked up — the build says so on stdout too.
