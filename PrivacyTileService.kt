package com.privacy.display

import android.content.Intent
import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

/**
 * Quick Settings tile that lets the user toggle privacy mode from the notification shade
 * without opening the app. Works on Android 7.0+.
 */
class PrivacyTileService : TileService() {

    private lateinit var prefs: SharedPreferences

    override fun onStartListening() {
        super.onStartListening()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val isActive = PrivacyOverlayService.isRunning

        if (isActive) {
            stopOverlay()
        } else {
            startOverlay()
        }
        syncTile()
    }

    private fun startOverlay() {
        val level    = prefs.getInt(MainActivity.KEY_PRIVACY_LEVEL, 50)
        val pattern  = prefs.getInt(MainActivity.KEY_PATTERN_TYPE, 0)
        val clarity  = prefs.getBoolean(MainActivity.KEY_CENTER_CLARITY, true)
        val amoled   = prefs.getBoolean(MainActivity.KEY_AMOLED_MODE, true)
        val opacity  = prefs.getInt("opacity", 60)
        val pSize    = prefs.getInt("pattern_size", 3) + 2

        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_START
            putExtra("privacy_level", level)
            putExtra("opacity", opacity)
            putExtra("pattern_size", pSize)
            putExtra("pattern_type", pattern)
            putExtra("center_clarity", clarity)
            putExtra("amoled_mode", amoled)
        }
        ContextCompat.startForegroundService(this, intent)
        prefs.edit().putBoolean(MainActivity.KEY_OVERLAY_ACTIVE, true).apply()
    }

    private fun stopOverlay() {
        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_STOP
        }
        startService(intent)
        prefs.edit().putBoolean(MainActivity.KEY_OVERLAY_ACTIVE, false).apply()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val active = PrivacyOverlayService.isRunning
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) "Privacy: ON" else "Privacy: OFF"
        tile.updateTile()
    }
}
