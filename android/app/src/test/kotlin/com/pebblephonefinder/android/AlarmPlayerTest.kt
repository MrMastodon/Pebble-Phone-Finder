package com.pebblephonefinder.android

import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPlayerTest {

    @Test
    fun `forceAlarmStreamToMax raises volume when below max`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) } returns 7
        every { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) } returns 2

        AlarmPlayer.forceAlarmStreamToMax(audioManager)

        verify(exactly = 1) { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 7, 0) }
    }

    @Test
    fun `forceAlarmStreamToMax is a no-op when already at max`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) } returns 7
        every { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) } returns 7

        AlarmPlayer.forceAlarmStreamToMax(audioManager)

        verify(exactly = 0) { audioManager.setStreamVolume(any(), any(), any()) }
    }

    @Test
    fun `restoreAlarmVolume sets the stream back to the given level`() {
        val audioManager = mockk<AudioManager>(relaxed = true)

        AlarmPlayer.restoreAlarmVolume(audioManager, 3)

        verify(exactly = 1) { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 3, 0) }
    }

    @Test
    fun `protocol command values match docs PROTOCOL md`() {
        assertEquals(0, Protocol.COMMAND_STOP)
        assertEquals(1, Protocol.COMMAND_START)
        assertEquals(0, Protocol.COMMAND_KEY)
        assertEquals("2cec5357-9346-45f1-926e-2af0b79cb149", Protocol.APP_UUID.toString())
    }
}
