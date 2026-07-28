# Pebble Phone Finder

> **⚠️ Vibe-coded project.** Every line of code, config, and this README was
> written by an AI coding assistant (Claude) from a conversational prompt, not
> by an experienced Pebble or Android developer reviewing each decision by
> hand. It builds and passes the checks described below, but it has **not**
> been installed or exercised on real watch/phone hardware by a human. Read
> it, review it, and test it yourself before you trust it with anything —
> especially the prebuilt binaries in `release/`.
>
> *(Norsk: Dette er et "vibe-kodet" prosjekt — all kode og konfigurasjon her
> er skrevet av en KI-assistent (Claude) ut fra en samtale, ikke av en
> erfaren Pebble- eller Android-utvikler som har gått gjennom hver linje.
> Det bygger og består testene beskrevet under, men er ikke installert eller
> utprøvd på ekte klokke/telefon av et menneske. Les, vurder og test selv før
> du stoler på det — særlig de ferdigbygde filene i `release/`.)*

A "Find My Phone" pair for the Pebble Time 2 (sold today as **Core Time 2** by
Core Devices): a one-button watch app plus an Android companion app that
plays a loud, looping alarm on the phone **regardless of the phone's volume
or silent-mode setting** — unlike existing Pebble find-my-phone watchapps,
which just play a media-stream sound at whatever the media volume happens to
be.

## Prebuilt binaries

Don't want to set up the Pebble SDK or an Android build environment? Ready
built artifacts (a `.pbw` for the watch, a debug `.apk` for the phone) are in
[`release/`](release/), with checksums and install instructions.

## How it works

1. `watch/` — a Pebble C watchapp for the `emery` platform (Pebble Time 2 /
   Core Time 2). One screen, one button (SELECT): press once to start the
   alarm, press again to stop it. Sends a single AppMessage command to the
   phone each time. A small always-visible line at the top of the screen
   shows live whether the watch is currently connected to the phone's
   Pebble app (`connection_service_subscribe()`), independent of the
   per-press ack feedback in the main status text.
2. `android/` — a native Android companion app. It does **not** talk to the
   watch directly; it registers with the official Core Devices "Pebble" app
   (which holds the actual Bluetooth connection) via the third-party
   [PebbleKitAndroid2](https://github.com/pebble-dev/PebbleKitAndroid2)
   library, receives the watch's command, and plays a looping alarm tone on
   `AudioManager.STREAM_ALARM` — the same audio stream the stock alarm clock
   uses, forced to max volume — from a foreground service with a
   non-dismissable notification carrying its own Stop button.

See `docs/PROTOCOL.md` for the exact shared UUID and AppMessage key/values.

### Choosing a different alarm sound

The phone app's "Choose alarm sound" button opens Android's built-in sound
picker (`RingtoneManager.ACTION_RINGTONE_PICKER`), scoped to alarm-type
sounds with the silent option hidden. It's deliberately limited to the
phone's built-in sounds rather than an arbitrary file: those live in a
shared system database with URIs that never expire or need a permission
grant, unlike a one-off Storage Access Framework grant for a picked file
(which can be revoked, e.g. across a reboot). The choice is stored in
`AlarmSoundPreference` and used by `AlarmPlayer`, which falls back to the
app's bundled default tone if the chosen sound is ever unavailable (e.g.
removed by an OS update) - the alarm always plays *something*. A "Reset to
default sound" button appears once a non-default sound is chosen, since
the picker's own "Default" option is deliberately hidden (it would point
at the phone's general default alarm sound, not this app's bundled tone).

### Where does the watch app's icon come from?

`watch/resources/images/icon.png` (25x25 px, declared in `watch/package.json`
under `resources.media` with `"menuIcon": true`) is the **only** icon
resource type the Pebble SDK supports (confirmed against the installed
SDK's own resource schema — there is no separate "large icon" field). It's
guaranteed to show up in the watch's own app launcher list. Whether the
phone's Pebble app also uses it for this watchapp's entry in its
installed-apps overview is less certain: that UI historically expects a
separate icon submitted through an appstore listing, which doesn't apply
to a sideloaded, non-appstore-published app like this one. If it stays
blank there after a clean reinstall, that's most likely a limitation of
the current Pebble mobile app version for sideloaded apps rather than
something fixable from this project's side. Regenerate the icon with
`watch/resources/images/generate_icon.py` (needs Pillow) or swap in your
own 25x25 PNG at that path.

### Localization

Both apps default to English and switch to Norwegian (Bokmål) when the
device's system language is Norwegian — no manual toggle. The watch checks
`i18n_get_system_locale()` at startup and picks between two hardcoded
string tables (Pebble's SDK generation has no string-resource localization
system, so this is a plain runtime switch); the phone app uses a standard
Android `res/values-nb/` resource set. To add another language: watch
add a case in `prv_text_for_state()` in `find_my_phone.c`, phone add
another `res/values-<code>/strings.xml`.

### About screen and on-device diagnostics

The phone app has an "About" button on the main screen that opens
`AboutActivity` — app name/version, a "Developed by MrMastodon" credit, a
"Buy me a coffee" button (opens the developer's PayPal.me page in the
browser), and a "Show diagnostics" button (hidden by default) that reveals
a log of
every `PebbleListenerService` callback received — app opened/closed, any
message, even for a UUID that isn't ours — with timestamps, persisted
across app restarts. It's kept off the main screen so it's out of the way
of everyday use, but still reachable for troubleshooting: there's no
adb/logcat access in most real-world situations, so if the watch shows its
message was sent but nothing happens on the phone, this panel is the first
place to check whether anything arrived at all. The watch side has
matching feedback: it distinguishes "sent but not yet acknowledged by the
phone" from "acknowledged" from "not connected" in its
own status text (see `find_my_phone.c`).

### Does the companion app need to stay running in the background?

No — it's event-driven, not an always-on background service. `PebbleListenerService`
is a plain manifest-declared service; the official Pebble app (which is the
one holding the persistent Bluetooth connection) starts it via an explicit
Intent only when a message actually arrives, the same way FCM wakes an app
for a push notification even if that app isn't currently running. Nothing in
this project needs to run 24/7 waiting for a button press.

Only once a `START` command actually arrives does `FindPhoneService` promote
itself to a real foreground service (persistent notification + wake lock),
and only for as long as the alarm needs to keep sounding.

The two things that *can* break this, neither of which this app can work
around from inside itself:
- **Force-stopping the app** from Android's app settings. This is an OS-level
  restriction that blocks *any* component (services, receivers) from being
  started again until the user manually reopens the app — it applies to
  every Android app, not something specific to how this one is built.
- **Aggressive OEM battery managers** (Xiaomi/MIUI, Huawei, some Samsung
  models, etc.) that go beyond stock Android and can treat a swiped-away or
  long-unused app as if it were force-stopped. If the alarm doesn't reliably
  fire, check that app's battery/autostart settings and allow it to run
  unrestricted in the background.

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
6. Optional: use "Choose alarm sound" to pick a different built-in phone
   sound, and "Show diagnostics" if you need to check what's actually being
   received from the watch.

## Known limitations

- **One-directional protocol.** The watch has no way to learn that the phone
  stopped the alarm via its own notification button — it'll keep showing its
  "playing" status until pressed again. That next press sends `STOP` a
  second time, which the Android side treats as a harmless no-op. See
  `docs/PROTOCOL.md`.
- **Total "Do Not Disturb" silence** is an OS-level restriction this app
  cannot bypass without you separately granting it Notification Policy
  Access — it is not something a normal app can silently override, and this
  project doesn't try to.
- **Battery optimization / OEM restrictions** — see "Does the companion app
  need to stay running in the background?" above.
- **Wake lock safety cap**: `FindPhoneService` holds its wake lock for up to
  30 minutes as a fallback in case `stop()` is somehow never reached (it's
  also auto-released by the OS if the process dies) — the alarm is meant to
  keep sounding until you stop it via the watch button or the notification's
  Stop button, not to time out on its own, but if you legitimately need it
  to ring longer than 30 minutes unattended, that cap needs raising in
  `FindPhoneService.kt`.
- **Manual sync required** between `watch/package.json` and
  `android/app/src/main/kotlin/com/pebblephonefinder/android/Protocol.kt` —
  there's no shared build step linking the two projects, only the
  consistency checklist in `docs/PROTOCOL.md`.

## Repo layout

```
docs/PROTOCOL.md   Shared UUID + AppMessage key/value contract
watch/              Pebble C watchapp (pebble-tool project, emery only)
android/            Android companion app (Gradle project)
release/            Prebuilt .pbw / .apk, with checksums (see release/README.md)
```
