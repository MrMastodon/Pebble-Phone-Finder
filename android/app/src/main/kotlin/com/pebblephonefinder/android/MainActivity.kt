package com.pebblephonefinder.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pebblephonefinder.android.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        promptForHostAppIfAmbiguous()
        updateAlarmSoundText()

        binding.testAlarmButton.setOnClickListener { toggleTestAlarm() }
        binding.chooseSoundButton.setOnClickListener { launchSoundPicker() }
        binding.resetSoundButton.setOnClickListener {
            AlarmSoundPreference.set(applicationContext, null)
            updateAlarmSoundText()
        }
        binding.aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
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
        binding.resetSoundButton.visibility = if (uri != null) View.VISIBLE else View.GONE
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
            // Stops collecting (and stops the binder traffic behind it) while
            // the activity isn't visible.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pebbleConnection.connectedWatchName().collect { watchName ->
                    binding.statusText.text = if (watchName != null) {
                        getString(R.string.status_connected) + "\n(" + watchName + ")"
                    } else {
                        getString(R.string.status_not_connected)
                    }
                }
            }
        }
    }

    /**
     * Message delivery is pinned to a single Pebble host app (see
     * [PebbleHostApp]). The usual case — exactly one installed — is pinned
     * automatically at startup; only genuine ambiguity reaches this prompt.
     */
    private fun promptForHostAppIfAmbiguous() {
        lifecycleScope.launch {
            if (PebbleHostApp.selected(applicationContext) != null) return@launch

            val candidates = withContext(Dispatchers.IO) {
                PebbleHostApp.eligible(applicationContext)
            }
            if (candidates.size < 2) return@launch

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.choose_host_app_title)
                .setItems(candidates.toTypedArray()) { _, which ->
                    lifecycleScope.launch {
                        PebbleHostApp.select(applicationContext, candidates[which])
                    }
                }
                .setCancelable(false)
                .show()
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
