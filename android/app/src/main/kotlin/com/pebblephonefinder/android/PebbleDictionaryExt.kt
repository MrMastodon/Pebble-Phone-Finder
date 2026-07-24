package com.pebblephonefinder.android

import io.rebble.pebblekit2.common.model.PebbleDictionaryItem

/**
 * The watch's `dict_write_uint8()` produces a 1-byte tuple on the wire, but
 * in practice the Pebble app / PebbleKitAndroid2 has been observed
 * delivering it as [PebbleDictionaryItem.UInt32] rather than
 * [PebbleDictionaryItem.UInt8] (confirmed via the on-device diagnostics
 * panel on real hardware) - so read whichever integer width actually shows
 * up instead of assuming one.
 */
fun PebbleDictionaryItem.toIntOrNull(): Int? = when (this) {
    is PebbleDictionaryItem.UInt8 -> value.toInt()
    is PebbleDictionaryItem.UInt16 -> value.toInt()
    is PebbleDictionaryItem.UInt32 -> value.toInt()
    is PebbleDictionaryItem.Int8 -> value.toInt()
    is PebbleDictionaryItem.Int16 -> value.toInt()
    is PebbleDictionaryItem.Int32 -> value
    else -> null
}
