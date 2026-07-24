package com.pebblephonefinder.android

import android.content.Context
import android.net.Uri

/**
 * Persists the user's chosen alarm sound (picked from the phone's built-in
 * sounds via [android.media.RingtoneManager]'s system picker in
 * [MainActivity]). Null means "use the app's bundled default sound".
 *
 * Deliberately scoped to built-in system sounds only: their `content://`
 * URIs live in a shared system database readable by any app at any time, so
 * there's no Storage Access Framework grant that can expire on reboot -
 * unlike a URI for an arbitrary user-picked file, which would need a
 * persistable permission (and a fallback for when it's revoked anyway).
 */
object AlarmSoundPreference {
    private const val PREFS_NAME = "alarm_sound"
    private const val KEY_URI = "uri"

    fun get(context: Context): Uri? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)
        return stored?.let { Uri.parse(it) }
    }

    fun set(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URI, uri?.toString())
            .apply()
    }
}
