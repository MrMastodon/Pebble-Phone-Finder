package com.pebblephonefinder.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Receives the notification's Stop action tap and forwards it to
 * [FindPhoneService] as the safety-net stop mechanism (in addition to
 * toggling the watch button again) — see docs/PROTOCOL.md.
 */
class StopActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, FindPhoneService::class.java).apply {
            action = FindPhoneService.ACTION_STOP_ALARM
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
