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

        private const val NOTIFICATION_CHANNEL_ID = "find_my_phone_alarm"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var alarmPlayer: AlarmPlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        alarmPlayer = AlarmPlayer(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> stopAlarm()
            else -> startAlarm()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun startAlarm() {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (alarmPlayer.isPlaying) return // already running: no-op

        acquireWakeLock()
        requestAudioFocus()
        alarmPlayer.start()
    }

    private fun stopAlarm() {
        if (alarmPlayer.isPlaying) {
            alarmPlayer.stop()
        }
        releaseAudioFocus()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FindMyPhoneCompanion:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L /* 10 min safety timeout */)
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
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .build()
    }
}
