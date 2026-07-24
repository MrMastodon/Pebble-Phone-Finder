package com.pebblephonefinder.android

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small on-device event log, persisted in SharedPreferences, so problems
 * with the watch -> phone path can be diagnosed from [MainActivity] alone
 * when adb isn't available. Records every [PebbleListenerService] callback
 * (even for a UUID that isn't ours), not just successful command handling.
 */
object DiagnosticsLog {
    private const val PREFS_NAME = "diagnostics"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 10

    fun record(context: Context, event: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val existing = prefs.getString(KEY_EVENTS, "")
            ?.split("\n")
            .orEmpty()
            .filter { it.isNotBlank() }
        val updated = (listOf("[$timestamp] $event") + existing).take(MAX_EVENTS)
        prefs.edit().putString(KEY_EVENTS, updated.joinToString("\n")).apply()
    }

    fun events(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EVENTS, null)?.takeIf { it.isNotBlank() }
            ?: "No events received yet — press the watch button, then reopen this app."
    }
}
