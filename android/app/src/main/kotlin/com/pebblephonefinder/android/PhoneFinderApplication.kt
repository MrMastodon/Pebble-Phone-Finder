package com.pebblephonefinder.android

import android.app.Application

/**
 * Exists so [PebbleHostApp.pinOnStartup] runs on every process start,
 * including when the Pebble app cold starts us straight into
 * [PebbleListenerService] with no Activity involved. See [PebbleHostApp] for
 * why an Activity would be too late.
 */
class PhoneFinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PebbleHostApp.pinOnStartup(this)
    }
}
