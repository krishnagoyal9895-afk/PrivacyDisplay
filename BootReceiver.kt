package com.privacy.display

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Restores the privacy overlay after device reboot if it was active before shutdown.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val wasActive = prefs.getBoolean(MainActivity.KEY_OVERLAY_ACTIVE, false)

        if (wasActive) {
            val level   = prefs.getInt(MainActivity.KEY_PRIVACY_LEVEL, 50)
            val pattern = prefs.getInt(MainActivity.KEY_PATTERN_TYPE, 0)
            val clarity = prefs.getBoolean(MainActivity.KEY_CENTER_CLARITY, true)
            val amoled  = prefs.getBoolean(MainActivity.KEY_AMOLED_MODE, true)
            val opacity = prefs.getInt("opacity", 60)
            val pSize   = prefs.getInt("pattern_size", 3) + 2

            val serviceIntent = Intent(context, PrivacyOverlayService::class.java).apply {
                action = PrivacyOverlayService.ACTION_START
                putExtra("privacy_level", level)
                putExtra("opacity", opacity)
                putExtra("pattern_size", pSize)
                putExtra("pattern_type", pattern)
                putExtra("center_clarity", clarity)
                putExtra("amoled_mode", amoled)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
