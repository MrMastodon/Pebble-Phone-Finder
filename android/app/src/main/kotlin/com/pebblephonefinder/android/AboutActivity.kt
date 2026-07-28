package com.pebblephonefinder.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pebblephonefinder.android.databinding.ActivityAboutBinding
import kotlinx.coroutines.launch

/**
 * Colophon: app name/version, developer credit, a donation link, and the
 * on-device diagnostics panel (moved here from [MainActivity] so it's out
 * of the way of the everyday flow, but still reachable for troubleshooting).
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionText.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        binding.toggleDiagnosticsButton.setOnClickListener { toggleDiagnosticsPanel() }
        binding.buyMeCoffeeButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BUY_ME_COFFEE_URL)))
            } catch (e: ActivityNotFoundException) {
                // No browser installed to handle https.
                Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.diagnosticsText.text = DiagnosticsLog.events(applicationContext)
        showPinnedHostApp()
    }

    /**
     * Surfaces which Pebble app is allowed to reach us. This is the only way
     * to actually confirm the lockdown in [PebbleHostApp] is live — the
     * alarm behaves identically whether it's on or off.
     */
    private fun showPinnedHostApp() {
        lifecycleScope.launch {
            val pinned = PebbleHostApp.selected(applicationContext)
            binding.hostAppText.text = if (pinned != null) {
                getString(R.string.host_app_pinned, pinned)
            } else {
                getString(R.string.host_app_none)
            }
        }
    }

    private fun toggleDiagnosticsPanel() {
        val showing = binding.diagnosticsPanel.visibility == View.VISIBLE
        binding.diagnosticsPanel.visibility = if (showing) View.GONE else View.VISIBLE
        binding.toggleDiagnosticsButton.setText(
            if (showing) R.string.show_diagnostics else R.string.hide_diagnostics
        )
    }

    private companion object {
        // PayPal.me-style payment page, provided directly by the developer.
        const val BUY_ME_COFFEE_URL = "https://www.paypal.com/ncp/payment/CAYUMPJRNCYQG"
    }
}
