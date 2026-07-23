# Watch ↔ phone protocol

This is the single source of truth for the values shared between the watch
app (`watch/package.json` + `watch/src/c/find_my_phone.c`) and the Android
companion app (`android/app/src/main/kotlin/.../Protocol.kt`). If you change
anything here, update both sides and re-check §Consistency at the bottom.

## Watch app UUID

```
2cec5357-9346-45f1-926e-2af0b79cb149
```

This must be identical in:
- `watch/package.json` → `pebble.uuid`
- `android/app/src/main/kotlin/com/pebblephonefinder/android/Protocol.kt` → `APP_UUID`

## AppMessage key

One direction only: **watch → phone**. Fire-and-forget, no ack.

| Key name  | Numeric key | Type   |
|-----------|-------------|--------|
| `COMMAND` | `0`         | uint8  |

Declared explicitly (not auto-numbered) in `watch/package.json`:

```json
"messageKeys": {
  "COMMAND": 0
}
```

Explicit numbering was verified against pebble-tool 5.0.39 / SDK 4.17: the
array form (`["COMMAND"]`) auto-assigns keys starting at `10000`, which is an
extra thing to keep in sync on the Android side for no benefit. The object
form pins the key to exactly `0`, which is what `Protocol.kt` hardcodes.

## Command values

| Value | Meaning                                            |
|-------|-----------------------------------------------------|
| `0`   | `STOP` — stop the alarm on the phone                 |
| `1`   | `START` — start the loud, looping alarm on the phone |

## Known MVP limitation

The protocol is one-directional. If the alarm is stopped from the phone's
notification (the STOP action button), the watch has no way to find out and
will keep showing "Playing... press to stop" until the button is pressed
again. That next press sends `STOP` a second time, which the Android side
must treat as a harmless no-op (see `FindPhoneService`). A `STATUS`
phone→watch key is a reasonable future addition but is out of scope for this
one-button MVP.

## Consistency checklist

Before considering a change to this file done, confirm the same UUID and
`COMMAND`/`START`/`STOP` values appear in:

- [x] `watch/package.json` (`pebble.uuid`, `pebble.messageKeys.COMMAND`)
- [x] `watch/src/c/find_my_phone.c` (uses `MESSAGE_KEY_COMMAND`, values `0`/`1`)
- [x] `android/app/src/main/kotlin/com/pebblephonefinder/android/Protocol.kt`

Last checked: matches confirmed after a full `pebble build` (emery) and
`./gradlew assembleDebug` / `testDebugUnitTest` / `lintDebug` pass — see
repo root README.md for exact commands and results.
