package com.pebblephonefinder.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.pebblephonefinder.android.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Minimal UI: shows whether a Pebble watch is currently reachable through the
 * official Pebble companion app, and a "Test alarm" button that drives
 * [FindPhoneService] directly so the phone-side alarm can be verified without
 * needing a paired watch at all.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pebbleConnection: PebbleConnection
    private var isTestAlarmPlaying = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pebbleConnection = PebbleConnection(applicationContext)

        requestNotificationPermissionIfNeeded()
        observeConnectionStatus()

        binding.testAlarmButton.setOnClickListener { toggleTestAlarm() }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun observeConnectionStatus() {
        lifecycleScope.launch {
            pebbleConnection.connectedWatchName().collect { watchName ->
                binding.statusText.text = if (watchName != null) {
                    getString(R.string.status_connected) + "\n(" + watchName + ")"
                } else {
                    getString(R.string.status_not_connected)
                }
            }
        }
    }

    private fun toggleTestAlarm() {
        isTestAlarmPlaying = !isTestAlarmPlaying
        val action = if (isTestAlarmPlaying) {
            FindPhoneService.ACTION_START_ALARM
        } else {
            FindPhoneService.ACTION_STOP_ALARM
        }
        val intent = Intent(this, FindPhoneService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(this, intent)

        binding.testAlarmButton.setText(
            if (isTestAlarmPlaying) R.string.test_alarm_stop else R.string.test_alarm_start
        )
    }
}
