package com.pebblephonefinder.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
 *
 * @param onPlaybackError invoked (on the main thread) if playback dies
 *   mid-alarm, so the caller can tear down instead of holding a wake lock
 *   and an ongoing notification for silence.
 */
class AlarmPlayer(
    private val context: Context,
    private val onPlaybackError: (() -> Unit)? = null,
) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null

    /** Non-null only while we owe the user a volume restore. */
    private var savedAlarmVolume: Int? = null

    val isPlaying: Boolean
        get() = mediaPlayer != null

    private val defaultSoundUri: Uri
        get() = Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_sound}")

    fun start() {
        if (mediaPlayer != null) return

        savedAlarmVolume = forceAlarmStreamToMax(audioManager)

        val chosenUri = AlarmSoundPreference.get(context)
        val player = chosenUri?.let { buildPlayer(it) } ?: buildPlayer(defaultSoundUri)

        if (player == null) {
            // Nothing is going to play. Don't leave the user's alarm volume
            // pinned at max with no sound to show for it.
            restoreSavedVolume()
            return
        }
        mediaPlayer = player
    }

    fun stop() {
        mediaPlayer?.let { player ->
            try {
                player.stop()
            } catch (e: IllegalStateException) {
                // Already stopped, or in the Error state after onError fired.
                Log.w(TAG, "MediaPlayer.stop() rejected, releasing anyway", e)
            }
            player.release()
        }
        mediaPlayer = null
        restoreSavedVolume()
    }

    /**
     * Builds and starts a [MediaPlayer] for [soundUri], or returns null if it
     * couldn't be loaded (e.g. a previously-chosen built-in sound was removed
     * by an OS update) - the caller falls back to the bundled default sound
     * in that case, so the alarm always plays *something*.
     */
    private fun buildPlayer(soundUri: Uri): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error (what=$what, extra=$extra); giving up")
                // Posted rather than run inline so we're not tearing the
                // player down from inside its own callback.
                Handler(Looper.getMainLooper()).post { onPlaybackError?.invoke() }
                true // handled; the player stays in the Error state
            }
            player.setDataSource(context, soundUri)
            player.prepare()
            player.start()
            player
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't play $soundUri, falling back", e)
            player.release() // otherwise this native instance leaks
            null
        }
    }

    private fun restoreSavedVolume() {
        savedAlarmVolume?.let { restoreAlarmVolume(audioManager, it) }
        savedAlarmVolume = null
    }

    companion object {
        private const val TAG = "AlarmPlayer"

        /**
         * Raises the alarm stream to its maximum.
         *
         * @return the previous volume, so it can be restored later, or null
         *   if nothing needs restoring - either it was already at max, or the
         *   OS refused the change.
         */
        fun forceAlarmStreamToMax(audioManager: AudioManager): Int? {
            return try {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                if (current >= max) return null
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
                current
            } catch (e: SecurityException) {
                // Changing the alarm volume can count as a Do Not Disturb
                // policy change, which the OS refuses without
                // ACCESS_NOTIFICATION_POLICY. The alarm still plays, just at
                // whatever volume was already set - better than crashing.
                Log.w(TAG, "Not allowed to raise alarm volume (DND policy)", e)
                null
            }
        }

        /** Best-effort restore; see [forceAlarmStreamToMax] for why this can fail. */
        fun restoreAlarmVolume(audioManager: AudioManager, previousVolume: Int) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
            } catch (e: SecurityException) {
                Log.w(TAG, "Not allowed to restore alarm volume (DND policy)", e)
            }
        }
    }
}
