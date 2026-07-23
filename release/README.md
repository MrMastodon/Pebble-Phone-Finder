# Prebuilt release artifacts

Ready-to-install builds, so you don't have to set up the Pebble SDK or an
Android build environment yourself. These are rebuilt from the source in
`watch/` and `android/` in this repo — see the root `README.md` for how to
build them yourself instead, and for the "vibe-coded" disclaimer that applies
to everything here.

| File | What it is | Install with |
|------|------------|---------------|
| `find-my-phone-watch-v1.0.0.pbw` | Pebble watchapp, `emery` platform only (Pebble Time 2 / Core Time 2) | `pebble install --phone <ip> find-my-phone-watch-v1.0.0.pbw`, or sideload through the Pebble app the same way you'd install any `.pbw` |
| `find-my-phone-companion-v1.0.0-debug.apk` | Android companion app, **debug build** (not signed for release/Play Store) | Sideload directly, or `adb install find-my-phone-companion-v1.0.0-debug.apk` |

## SHA-256 checksums

```
7657f8c57988fd969af77b484382a7be8ca56f8f1684f485690d7b9fe208445e  find-my-phone-watch-v1.0.0.pbw
73627ed1aad8fe6617c2f3db7ef434bb36d2df020c38ae12b9a4e8f35d219127  find-my-phone-companion-v1.0.0-debug.apk
```

Both were built and verified in this repo's CI-less sandbox environment
(`pebble build` for the watchapp, `gradle assembleDebug` for the APK) — see
the root README for exact toolchain versions used. Neither has been
installed on real hardware by the person building them; see the "Known
limitations" section of the root README.
