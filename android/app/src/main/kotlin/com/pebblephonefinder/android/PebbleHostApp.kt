package com.pebblephonefinder.android

import android.content.Context
import android.util.Log
import io.rebble.pebblekit2.client.DefaultPebbleAndroidAppPicker
import io.rebble.pebblekit2.client.PebbleAndroidAppPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controls which Pebble mobile app is allowed to deliver watch messages to
 * [PebbleListenerService].
 *
 * PebbleKitAndroid2 defaults to accepting *any* installed app that
 * advertises itself as a Pebble host, so a malicious app could impersonate
 * one and set off the alarm. We turn that default off and pin a single host
 * package instead: trust on first use, locked down afterwards.
 *
 * Two things about the library's picker drive the design here:
 *
 * 1. `enableAutoSelect` is an in-memory field that resets to `true` on every
 *    process start, so it must be set from [FindMyPhoneApplication] rather
 *    than an Activity — the Pebble app can cold start our process straight
 *    into the listener service without any Activity ever running.
 * 2. The *selection* itself is persisted (in the library's own DataStore),
 *    so it only has to be written once per install.
 */
object PebbleHostApp {

    private const val TAG = "PebbleHostApp"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun picker(context: Context): PebbleAndroidAppPicker =
        DefaultPebbleAndroidAppPicker.getInstance(context.applicationContext)

    /**
     * Locks delivery down to one host app. Call from
     * [FindMyPhoneApplication.onCreate].
     *
     * The lockdown itself is applied synchronously, so we're never briefly
     * permissive; only the convenience auto-pin runs in the background. Worst
     * case on a brand new install is that the very first watch message
     * arrives before the pin lands and gets dropped — pressing the watch
     * button again fixes it.
     */
    fun pinOnStartup(context: Context) {
        val picker = picker(context)
        picker.enableAutoSelect = false

        scope.launch {
            try {
                if (picker.getCurrentlySelectedApp() != null) return@launch

                val candidates = picker.getAllEligibleApps()
                // Exactly one installed host app is the normal case: pin it
                // silently. Anything else is ambiguous and needs a deliberate
                // choice from the user (see MainActivity).
                if (candidates.size == 1) {
                    picker.selectApp(candidates.single())
                    Log.i(TAG, "Pinned Pebble host app: ${candidates.single()}")
                } else {
                    Log.i(TAG, "Not auto-pinning; ${candidates.size} eligible host apps")
                }
            } catch (e: Exception) {
                // selectApp() rejects a package that isn't a Pebble host with
                // IllegalArgumentException; DataStore can fail on its own too.
                Log.w(TAG, "Could not pin a Pebble host app", e)
            }
        }
    }

    // All three below reach the PackageManager and/or DataStore, i.e. binder
    // and disk. The dispatcher is forced here rather than left to callers, so
    // there's no way to accidentally do this work on the main thread.

    /** Package name of the pinned host app, or null if none is pinned yet. */
    suspend fun selected(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            picker(context).getCurrentlySelectedApp()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the selected host app", e)
            null
        }
    }

    /** Every installed app advertising itself as a Pebble host. */
    suspend fun eligible(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            picker(context).getAllEligibleApps()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list eligible host apps", e)
            emptyList()
        }
    }

    /** Pins [packageName], or clears the pin when given null. */
    suspend fun select(context: Context, packageName: String?) = withContext(Dispatchers.IO) {
        try {
            picker(context).selectApp(packageName)
            Log.i(TAG, "Pinned Pebble host app: ${packageName ?: "(cleared)"}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not pin $packageName", e)
        }
    }
}
