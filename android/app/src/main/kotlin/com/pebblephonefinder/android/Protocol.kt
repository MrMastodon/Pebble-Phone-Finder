package com.pebblephonefinder.android

import java.util.UUID

/**
 * Mirrors docs/PROTOCOL.md in the repo root. Keep both in sync manually —
 * there is no shared build step linking this file to watch/package.json.
 */
object Protocol {
    val APP_UUID: UUID = UUID.fromString("2cec5357-9346-45f1-926e-2af0b79cb149")

    const val COMMAND_KEY: Int = 0

    const val COMMAND_STOP: Int = 0
    const val COMMAND_START: Int = 1
}
