package com.pebblephonefinder.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pebblephonefinder.android.databinding.ActivityAboutBinding

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
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BUY_ME_COFFEE_URL)))
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

    private companion object {
        // PayPal.me-style payment page, provided directly by the developer.
        const val BUY_ME_COFFEE_URL = "https://www.paypal.com/ncp/payment/CAYUMPJRNCYQG"
    }
}
