package com.pebblephonefinder.android

import android.content.Context
import io.rebble.pebblekit2.client.DefaultPebbleInfoRetriever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Thin status wrapper around PebbleKitAndroid2's [DefaultPebbleInfoRetriever],
 * used by [MainActivity] to show whether a watch is currently reachable
 * through the official Pebble companion app. The actual watch -> phone
 * COMMAND messages are delivered separately, straight from the Pebble app to
 * [PebbleListenerService] (a manifest-registered listener), not through this
 * class — PebbleKitAndroid2 doesn't require holding an open connection object
 * to receive them.
 */
class PebbleConnection(context: Context) {
    private val infoRetriever = DefaultPebbleInfoRetriever(context)

    /** Emits the first connected watch's name, or null if none is connected. */
    fun connectedWatchName(): Flow<String?> =
        infoRetriever.getConnectedWatches().map { watches -> watches.firstOrNull()?.name }
}
