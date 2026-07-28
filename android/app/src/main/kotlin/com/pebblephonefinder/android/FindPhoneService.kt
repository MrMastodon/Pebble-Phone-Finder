package com.pebblephonefinder.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that owns the alarm's lifecycle end to end: notification,
 * wake lock, audio focus, and the actual [AlarmPlayer]. Started/stopped either
 * by [PebbleListenerService] (a COMMAND from the watch) or by the user tapping
 * the notification's Stop action ([StopActionReceiver]) or the in-app test
 * button ([MainActivity]). Start/stop are both idempotent so a stray repeat
 * command (e.g. the watch not knowing the phone was already stopped — see
 * docs/PROTOCOL.md) is a harmless no-op.
 */
class FindPhoneService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.pebblephonefinder.android.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.pebblephonefinder.android.action.STOP_ALARM"

        private const val TAG = "FindPhoneService"
        private const val NOTIFICATION_CHANNEL_ID = "find_my_phone_alarm"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var alarmPlayer: AlarmPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        alarmPlayer = AlarmPlayer(applicationContext) {
            // Playback died mid-alarm - tear down rather than sit in the
            // foreground holding a wake lock for silence.
            Log.w(TAG, "Playback failed mid-alarm, stopping")
            stopAlarm()
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() promises the system that startForeground()
        // follows promptly. That promise applies on the stop path too, so call
        // it up front - otherwise a Stop tap that arrives when the service
        // isn't already in the foreground gets us killed with a
        // RemoteServiceException.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Android 12+ can refuse a foreground start from the background.
            // Nothing useful left to do, but crashing helps no one.
            Log.e(TAG, "Could not enter the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START_ALARM -> startAlarm()
            // Anything else - including the null intent the system hands us
            // when it recreates the service - tears down. Never start a
            // max-volume alarm off an intent nobody explicitly sent.
            else -> stopAlarm()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun startAlarm() {
        if (alarmPlayer.isPlaying) return // already running: no-op

        acquireWakeLock()
        requestAudioFocus()
        try {
            alarmPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't start alarm", e)
            stopAlarm()
            return
        }
        if (!alarmPlayer.isPlaying) {
            // No sound source could be opened at all - don't hold the
            // foreground service and wake lock for nothing.
            Log.w(TAG, "Alarm produced no playable sound, stopping")
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        try {
            alarmPlayer.stop() // idempotent; also restores the alarm volume
        } catch (e: Exception) {
            Log.w(TAG, "Error while stopping playback", e)
        } finally {
            // Must run even if teardown above threw, or we leak the wake lock
            // and leave an un-dismissable notification behind.
            releaseAudioFocus()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FindMyPhoneCompanion:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
            // Safety cap only, in case stop() is somehow never reached (the
            // OS also auto-releases this if this process dies). Deliberately
            // long: the alarm is meant to keep sounding until the user finds
            // the phone or explicitly stops it (watch button or notification
            // Stop button), so a short cutoff here would silently defeat that.
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // An alarm should keep sounding through transient interruptions
            // (e.g. another app briefly grabbing focus), same as the stock
            // alarm clock — so we intentionally do not stop playback on
            // onAudioFocusChange.
            .setOnAudioFocusChangeListener {}
            .build()
        audioFocusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun releaseAudioFocus() {
        audioFocusRequest?.let {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, StopActionReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            // Must be a white-on-transparent silhouette: Android treats the
            // small icon as a mask and tints it.
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .build()
    }
}
