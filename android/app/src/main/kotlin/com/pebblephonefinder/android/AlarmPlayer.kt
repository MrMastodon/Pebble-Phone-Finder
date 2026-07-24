package com.pebblephonefinder.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Plays a looping alarm tone on [AudioManager.STREAM_ALARM], which (like the
 * stock alarm clock) is unaffected by the ringer/media volume slider and by
 * silent mode. On [start] the alarm stream is forced to its max volume so the
 * alert is loud regardless of whatever the user had it set to; on [stop] the
 * previous alarm-stream volume is restored.
 *
 * Total "Do Not Disturb" silence is an OS-level restriction this cannot
 * bypass without the user separately granting Notification Policy Access.
 */
class AlarmPlayer(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var savedAlarmVolume: Int? = null

    val isPlaying: Boolean
        get() = mediaPlayer != null

    private val defaultSoundUri: Uri
        get() = Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_sound}")

    fun start() {
        if (mediaPlayer != null) return

        savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        forceAlarmStreamToMax(audioManager)

        val chosenUri = AlarmSoundPreference.get(context)
        mediaPlayer = chosenUri?.let { buildPlayer(it) } ?: buildPlayer(defaultSoundUri)
    }

    /**
     * Builds and starts a [MediaPlayer] for [soundUri], or returns null if it
     * couldn't be loaded (e.g. a previously-chosen built-in sound was removed
     * by an OS update) - the caller falls back to the bundled default sound
     * in that case, so the alarm always plays *something*.
     */
    private fun buildPlayer(soundUri: Uri): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setDataSource(context, soundUri)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w("AlarmPlayer", "Couldn't play $soundUri, falling back", e)
            null
        }
    }

    fun stop() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null

        savedAlarmVolume?.let { restoreAlarmVolume(audioManager, it) }
        savedAlarmVolume = null
    }

    companion object {
        /** Pulled out for unit testing against a mocked [AudioManager]. */
        fun forceAlarmStreamToMax(audioManager: AudioManager) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < max) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
        }

        /** Pulled out for unit testing against a mocked [AudioManager]. */
        fun restoreAlarmVolume(audioManager: AudioManager, previousVolume: Int) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
        }
    }
}
