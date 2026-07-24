package com.pebblephonefinder.android

import android.content.Intent
import androidx.core.content.ContextCompat
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

/**
 * Registered in AndroidManifest.xml with the
 * `io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH` intent-filter action. The
 * official Pebble companion app delivers AppMessage data from our watch app
 * (matched by UUID, see docs/PROTOCOL.md) straight to this service.
 */
class PebbleListenerService : BasePebbleListenerService() {

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier,
    ): ReceiveResult {
        // Logged unconditionally (even for a UUID that isn't ours) so a
        // mismatch or "nothing ever arrives" can be told apart from inside
        // the app, without needing adb - see MainActivity's diagnostics.
        DiagnosticsLog.record(this, "onMessageReceived uuid=$watchappUUID data=$data")

        if (watchappUUID == Protocol.APP_UUID) {
            val commandItem = data[Protocol.COMMAND_KEY.toUInt()]
            if (commandItem is PebbleDictionaryItem.UInt8) {
                when (commandItem.value.toInt()) {
                    Protocol.COMMAND_START -> sendServiceAction(FindPhoneService.ACTION_START_ALARM)
                    Protocol.COMMAND_STOP -> sendServiceAction(FindPhoneService.ACTION_STOP_ALARM)
                }
            }
        }
        return ReceiveResult.Ack
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        DiagnosticsLog.record(this, "onAppOpened uuid=$watchappUUID")
    }

    override fun onAppClosed(watchappUUID: UUID, watch: WatchIdentifier) {
        DiagnosticsLog.record(this, "onAppClosed uuid=$watchappUUID")
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, FindPhoneService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(this, intent)
    }
}
