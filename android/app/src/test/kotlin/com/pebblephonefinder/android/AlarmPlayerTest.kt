package com.pebblephonefinder.android

import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmPlayerTest {

    private fun audioManager(current: Int, max: Int = 7) = mockk<AudioManager>(relaxed = true).also {
        every { it.getStreamMaxVolume(AudioManager.STREAM_ALARM) } returns max
        every { it.getStreamVolume(AudioManager.STREAM_ALARM) } returns current
    }

    @Test
    fun `forceAlarmStreamToMax raises volume and returns the previous level`() {
        val audioManager = audioManager(current = 2, max = 7)

        val previous = AlarmPlayer.forceAlarmStreamToMax(audioManager)

        assertEquals(2, previous)
        verify(exactly = 1) { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 7, 0) }
    }

    @Test
    fun `forceAlarmStreamToMax is a no-op when already at max`() {
        val audioManager = audioManager(current = 7, max = 7)

        val previous = AlarmPlayer.forceAlarmStreamToMax(audioManager)

        // Null means "nothing to restore" - we never touched the volume.
        assertNull(previous)
        verify(exactly = 0) { audioManager.setStreamVolume(any(), any(), any()) }
    }

    @Test
    fun `forceAlarmStreamToMax survives the OS refusing the change`() {
        // Raising the alarm volume can count as a Do Not Disturb policy
        // change, which throws without ACCESS_NOTIFICATION_POLICY. That must
        // not propagate - it would crash the whole alarm.
        val audioManager = audioManager(current = 2, max = 7)
        every {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 7, 0)
        } throws SecurityException("Not allowed to change Do Not Disturb state")

        val previous = AlarmPlayer.forceAlarmStreamToMax(audioManager)

        // Nothing to restore, because the change never took effect.
        assertNull(previous)
    }

    @Test
    fun `restoreAlarmVolume sets the stream back to the given level`() {
        val audioManager = mockk<AudioManager>(relaxed = true)

        AlarmPlayer.restoreAlarmVolume(audioManager, 3)

        verify(exactly = 1) { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 3, 0) }
    }

    @Test
    fun `restoreAlarmVolume survives the OS refusing the change`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 3, 0)
        } throws SecurityException("Not allowed to change Do Not Disturb state")

        // Must not throw: this runs in the service's teardown path, where an
        // exception would leak the wake lock and strand the notification.
        AlarmPlayer.restoreAlarmVolume(audioManager, 3)
    }

    @Test
    fun `protocol command values match docs PROTOCOL md`() {
        assertEquals(0, Protocol.COMMAND_STOP)
        assertEquals(1, Protocol.COMMAND_START)
        assertEquals(0, Protocol.COMMAND_KEY)
        assertEquals("2cec5357-9346-45f1-926e-2af0b79cb149", Protocol.APP_UUID.toString())
    }
}
