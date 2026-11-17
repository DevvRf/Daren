package com.gamespace.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

/**
 * Polls the foreground app every 3 seconds (via ForegroundAppDetector —
 * usage-stats first, privileged shell as fallback) and auto-applies a
 * GameProfile's settings when that profile has autoApply=true and its
 * package comes to the foreground, reverting when it leaves. This is
 * what makes GS Renji behave like a real game suite instead of something
 * you have to remember to turn on/off by hand.
 *
 * 3-second polling is a deliberate trade-off: fast enough to feel
 * responsive, slow enough not to meaningfully drain battery or spam
 * UsageStatsManager/dumpsys calls.
 */
class GameMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "gs_monitor"
        const val NOTIF_ID = 3001
        private const val POLL_INTERVAL_MS = 3000L
    }

    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var currentAppliedProfileId: String? = null
    private lateinit var store: GameProfileStore

    override fun onCreate() {
        super.onCreate()
        store = GameProfileStore(this)
        handlerThread = HandlerThread("gs-monitor")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        startForeground(NOTIF_ID, buildNotification())
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private val pollRunnable = object : Runnable {
        override fun run() {
            val result = ForegroundAppDetector.detect(this@GameMonitorService)
            val profiles = store.loadAll().filter { it.autoApply && it.packageName != null }
            val matched = profiles.firstOrNull { it.packageName == result.packageName }

            if (matched != null) {
                if (currentAppliedProfileId != matched.id) {
                    GameSessionManager.applyProfile(this@GameMonitorService, matched)
                    currentAppliedProfileId = matched.id
                }
            } else if (currentAppliedProfileId != null) {
                // The previously-matched game is no longer in the foreground.
                GameSessionManager.revertAll(this@GameMonitorService)
                currentAppliedProfileId = null
            }

            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
        if (currentAppliedProfileId != null) {
            GameSessionManager.revertAll(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Game detection", NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GS Renji — mendeteksi game otomatis")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }
}
