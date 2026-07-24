package com.pebblephonefinder.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
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

    private val pickAlarmSound =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val uri: Uri? = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            AlarmSoundPreference.set(applicationContext, uri)
            updateAlarmSoundText()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pebbleConnection = PebbleConnection(applicationContext)

        requestNotificationPermissionIfNeeded()
        observeConnectionStatus()
        updateAlarmSoundText()

        binding.testAlarmButton.setOnClickListener { toggleTestAlarm() }
        binding.toggleDiagnosticsButton.setOnClickListener { toggleDiagnosticsPanel() }
        binding.chooseSoundButton.setOnClickListener { launchSoundPicker() }
    }

    private fun launchSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, AlarmSoundPreference.get(applicationContext))
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.choose_sound_title))
        }
        pickAlarmSound.launch(intent)
    }

    private fun updateAlarmSoundText() {
        val uri = AlarmSoundPreference.get(applicationContext)
        binding.currentSoundText.text = if (uri != null) {
            val title = RingtoneManager.getRingtone(applicationContext, uri)?.getTitle(applicationContext)
            getString(R.string.current_sound, title ?: getString(R.string.default_sound_name))
        } else {
            getString(R.string.current_sound, getString(R.string.default_sound_name))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.diagnosticsText.text = DiagnosticsLog.events(applicationContext)
    }

    private fun toggleDiagnosticsPanel() {
        val showing = binding.diagnosticsPanel.visibility == View.VISIBLE
        binding.diagnosticsPanel.visibility = if (showing) View.GONE else View.VISIBLE
        binding.toggleDiagnosticsButton.setText(
            if (showing) R.string.show_diagnostics else R.string.hide_diagnostics
        )
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
