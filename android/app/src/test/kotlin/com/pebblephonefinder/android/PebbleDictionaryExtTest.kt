package com.pebblephonefinder.android

import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PebbleDictionaryExtTest {

    @Test
    fun `reads value regardless of integer width`() {
        assertEquals(1, PebbleDictionaryItem.UInt8(1.toUByte()).toIntOrNull())
        assertEquals(1, PebbleDictionaryItem.UInt16(1.toUShort()).toIntOrNull())
        assertEquals(1, PebbleDictionaryItem.UInt32(1.toUInt()).toIntOrNull())
        assertEquals(1, PebbleDictionaryItem.Int8(1.toByte()).toIntOrNull())
        assertEquals(1, PebbleDictionaryItem.Int16(1.toShort()).toIntOrNull())
        assertEquals(1, PebbleDictionaryItem.Int32(1).toIntOrNull())
    }

    @Test
    fun `matches the START and STOP values actually observed on real hardware`() {
        // The watch sends dict_write_uint8(), but real devices have been
        // observed delivering it as UInt32 here - this is the exact shape
        // that broke command handling before toIntOrNull() was introduced.
        assertEquals(Protocol.COMMAND_START, PebbleDictionaryItem.UInt32(1.toUInt()).toIntOrNull())
        assertEquals(Protocol.COMMAND_STOP, PebbleDictionaryItem.UInt32(0.toUInt()).toIntOrNull())
    }

    @Test
    fun `returns null for non-integer dictionary items`() {
        assertNull(PebbleDictionaryItem.Text("hello").toIntOrNull())
    }
}
