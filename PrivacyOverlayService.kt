package com.privacy.display

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat

/**
 * PrivacyOverlayService
 *
 * A foreground service that:
 *   1. Draws a full-screen PrivacyOverlayView via WindowManager (TYPE_APPLICATION_OVERLAY).
 *   2. Keeps a persistent notification so Android doesn't kill it.
 *   3. Exposes START / STOP / UPDATE intents for MainActivity and the Quick Tile.
 *
 * The overlay is non-touchable and non-focusable so it never blocks user interaction.
 */
class PrivacyOverlayService : Service() {

    companion object {
        const val ACTION_START  = "com.privacy.display.ACTION_START"
        const val ACTION_STOP   = "com.privacy.display.ACTION_STOP"
        const val ACTION_UPDATE = "com.privacy.display.ACTION_UPDATE"

        const val CHANNEL_ID         = "PrivacyOverlayChannel"
        const val NOTIFICATION_ID    = 101

        @Volatile
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: PrivacyOverlayView? = null

    /* ---------- Lifecycle ---------- */

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showOverlay(intent)
            }
            ACTION_STOP   -> stopSelf()
            ACTION_UPDATE -> updateOverlay(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------- Overlay Management ---------- */

    private fun showOverlay(intent: Intent?) {
        if (overlayView != null) {
            updateOverlay(intent)
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val view = PrivacyOverlayView(this)
        intent?.extras?.let { view.applyConfig(it) }

        windowManager?.addView(view, params)
        overlayView = view
        isRunning = true
    }

    private fun updateOverlay(intent: Intent?) {
        intent?.extras?.let { overlayView?.applyConfig(it) }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
    }

    /* ---------- Notification ---------- */

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Privacy Display Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active privacy overlay notification"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Privacy Mode Active")
            .setContentText("Screen is protected from shoulder surfing")
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_shield, "Disable", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }
}
