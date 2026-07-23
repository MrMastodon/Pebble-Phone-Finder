# Pebble Phone Finder

A "Find My Phone" pair for the Pebble Time 2 (sold today as **Core Time 2** by
Core Devices): a one-button watch app plus an Android companion app that
plays a loud, looping alarm on the phone **regardless of the phone's volume
or silent-mode setting** — unlike existing Pebble find-my-phone watchapps,
which just play a media-stream sound at whatever the media volume happens to
be.

## How it works

1. `watch/` — a Pebble C watchapp for the `emery` platform (Pebble Time 2 /
   Core Time 2). One screen, one button (SELECT): press once to start the
   alarm, press again to stop it. Sends a single AppMessage command to the
   phone each time.
2. `android/` — a native Android companion app. It does **not** talk to the
   watch directly; it registers with the official Core Devices "Pebble" app
   (which holds the actual Bluetooth connection) via the third-party
   [PebbleKitAndroid2](https://github.com/pebble-dev/PebbleKitAndroid2)
   library, receives the watch's command, and plays a looping alarm tone on
   `AudioManager.STREAM_ALARM` — the same audio stream the stock alarm clock
   uses, forced to max volume — from a foreground service with a
   non-dismissable notification carrying its own Stop button.

See `docs/PROTOCOL.md` for the exact shared UUID and AppMessage key/values.

## Prerequisites

- A Pebble Time 2 / Core Time 2 paired with an Android phone running the
  official **Pebble** companion app (Core Devices' app, available on Google
  Play / F-Droid / GitHub — `coredevices/mobileapp`). This project's Android
  app is a *second*, independent app that piggybacks on that connection; it
  does not replace it.
- If you have more than one third-party companion app installed for the same
  Pebble watchapp, the Pebble app may need you to pick which one receives
  events — see `DefaultPebbleAndroidAppPicker` in PebbleKitAndroid2's docs.

## Building the watch app

```
pip install pebble-tool
cd watch
pebble build
pebble install --phone <phone-ip>   # or --emulator emery for local testing
```

Verified in this repo against `pebble-tool` 5.0.39 / SDK 4.17: `pebble build`
produces `build/watch.pbw` targeting `emery` only, with the `COMMAND`
AppMessage key pinned to `0` (see `docs/PROTOCOL.md` for why the numbering
is pinned rather than auto-assigned).

## Building the Android app

```
cd android
gradle wrapper          # one-time: generates ./gradlew for this checkout
./gradlew assembleDebug
```

A `gradlew` wrapper isn't checked in because generating one requires
downloading the Gradle distribution from `services.gradle.org`, which this
sandbox's network policy blocked (403) — run `gradle wrapper` once yourself
(or just open the project in Android Studio, which will offer to do this for
you) and commit the result if you want a checked-in wrapper.

Everything in `android/` was verified in this sandbox with:
- `gradle assembleDebug` — builds the debug APK (compileSdk 36, AGP 8.13.2,
  Kotlin 2.3.20 — bumped from more conservative defaults specifically because
  `io.rebble.pebblekit2:client:1.2.0`'s transitive AndroidX/Kotlin metadata
  required it; see git history/comments in `android/build.gradle.kts` and
  `android/app/build.gradle.kts` if bumping further later).
- `gradle testDebugUnitTest` — runs `AlarmPlayerTest` (mocked `AudioManager`,
  no device needed).
- `gradle lintDebug` — clean.

None of this exercises the real Bluetooth path — that needs your actual
watch, phone, and the official Pebble app; see Known limitations below.

## Installing on your phone

1. Install and pair the official Pebble app with your watch as normal.
2. Install this repo's watch app (`watch/build/watch.pbw`) the same way you'd
   install any Pebble watchapp.
3. Build and side-load `android/app/build/outputs/apk/debug/app-debug.apk`
   (or open `android/` in Android Studio and run it).
4. Grant the notification permission when the companion app asks for it —
   it's required for the alarm's Stop-button notification.
5. Press the watch's SELECT button to start the alarm on the phone; press it
   again (or tap Stop on the phone's notification) to stop it. The app's
   "Test alarm" button drives the same alarm without needing the watch at
   all, useful for confirming the phone-side behavior works before pairing.

## Known limitations

- **One-directional protocol.** The watch has no way to learn that the phone
  stopped the alarm via its own notification button — it'll keep showing
  "Playing... press to stop" until pressed again. That next press sends
  `STOP` a second time, which the Android side treats as a harmless no-op.
  See `docs/PROTOCOL.md`.
- **Total "Do Not Disturb" silence** is an OS-level restriction this app
  cannot bypass without you separately granting it Notification Policy
  Access — it is not something a normal app can silently override, and this
  project doesn't try to.
- **Battery optimization**: some Android OEM skins (Samsung, Xiaomi, etc.)
  are aggressive about killing background services. If the alarm doesn't
  reliably sound, exempt this app from battery optimization in system
  settings.
- **Manual sync required** between `watch/package.json` and
  `android/app/src/main/kotlin/com/pebblephonefinder/android/Protocol.kt` —
  there's no shared build step linking the two projects, only the
  consistency checklist in `docs/PROTOCOL.md`.

## Repo layout

```
docs/PROTOCOL.md   Shared UUID + AppMessage key/value contract
watch/              Pebble C watchapp (pebble-tool project, emery only)
android/            Android companion app (Gradle project)
```
