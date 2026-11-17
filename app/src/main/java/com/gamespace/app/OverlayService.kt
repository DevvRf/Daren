package com.gamespace.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import org.json.JSONObject

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "game_space_overlay"
        const val NOTIF_ID = 1001

        const val ACTION_SET_CROSSHAIR = "com.gamespace.app.SET_CROSSHAIR"
        const val ACTION_CLEAR_CROSSHAIR = "com.gamespace.app.CLEAR_CROSSHAIR"
        const val ACTION_SET_STATS_VISIBLE = "com.gamespace.app.SET_STATS_VISIBLE"
        const val ACTION_SET_MONITOR_METRICS = "com.gamespace.app.SET_MONITOR_METRICS"
        const val ACTION_SET_COLOR_PRESET = "com.gamespace.app.SET_COLOR_PRESET"

        const val EXTRA_CROSSHAIR_STYLE_JSON = "crosshair_style_json"
        const val EXTRA_VISIBLE = "visible"
        const val EXTRA_PRESET_NAME = "preset_name"
        const val EXTRA_METRICS_JSON = "metrics_json"
    }

    private lateinit var crosshairOverlay: CrosshairOverlayView
    private lateinit var statsOverlay: StatsOverlayView
    private lateinit var controlBubble: FloatingControlBubble

    // Remembers the last crosshair style chosen, so the bubble's quick
    // toggle can turn it back on without needing the style resent.
    private var lastCrosshairStyle: CrosshairStyle? = null
    private var crosshairOn = false
    private var statsOn = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (tenths != Int.MIN_VALUE) {
                statsOverlay.onBatteryTempUpdate(tenths)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        crosshairOverlay = CrosshairOverlayView(this)
        statsOverlay = StatsOverlayView(this)
        controlBubble = FloatingControlBubble(
            context = this,
            isCrosshairOn = { crosshairOn },
            isStatsOn = { statsOn },
            onToggleCrosshair = { toggleCrosshairFromBubble() },
            onToggleStats = { toggleStatsFromBubble() }
        )
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        startForeground(NOTIF_ID, buildNotification())
        controlBubble.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_CROSSHAIR -> {
                val json = intent.getStringExtra(EXTRA_CROSSHAIR_STYLE_JSON)
                val style = if (json != null) {
                    try { CrosshairStyle.fromJson(JSONObject(json)) } catch (e: Exception) { CrosshairStyle() }
                } else CrosshairStyle()
                lastCrosshairStyle = style
                crosshairOverlay.show(style)
                crosshairOn = true
            }
            ACTION_CLEAR_CROSSHAIR -> {
                crosshairOverlay.hide()
                crosshairOn = false
            }
            ACTION_SET_STATS_VISIBLE -> {
                val visible = intent.getBooleanExtra(EXTRA_VISIBLE, false)
                if (visible) statsOverlay.show() else statsOverlay.hide()
                statsOn = visible
            }
            ACTION_SET_MONITOR_METRICS -> {
                val json = intent.getStringExtra(EXTRA_METRICS_JSON)
                if (json != null) {
                    try {
                        val obj = JSONObject(json)
                        statsOverlay.metrics = MonitorMetrics(
                            fps = obj.optBoolean("fps", true),
                            cpu = obj.optBoolean("cpu", true),
                            ram = obj.optBoolean("ram", true),
                            gpu = obj.optBoolean("gpu", true),
                            temperature = obj.optBoolean("temperature", true)
                        )
                    } catch (e: Exception) { /* ignore malformed */ }
                }
            }
            ACTION_SET_COLOR_PRESET -> {
                val preset = intent.getStringExtra(EXTRA_PRESET_NAME) ?: "off"
                ColorFilterManager.apply(this, preset)
            }
        }
        controlBubble.refreshToggleLabels()
        return START_STICKY
    }

    /** Quick toggle from the floating bubble: re-shows the last chosen crosshair style, or hides it. */
    private fun toggleCrosshairFromBubble() {
        if (crosshairOn) {
            crosshairOverlay.hide()
            crosshairOn = false
        } else {
            val style = lastCrosshairStyle ?: CrosshairStyle()
            crosshairOverlay.show(style)
            lastCrosshairStyle = style
            crosshairOn = true
        }
    }

    private fun toggleStatsFromBubble() {
        if (statsOn) {
            statsOverlay.hide()
            statsOn = false
        } else {
            statsOverlay.show()
            statsOn = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        crosshairOverlay.hide()
        statsOverlay.hide()
        controlBubble.hide()
        ColorFilterManager.clear(this)
        DisplayScaler.reset(this)
        unregisterReceiver(batteryReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val revertIntent = Intent(ResolutionRevertReceiver.ACTION_REVERT).apply {
            setPackage(packageName)
        }
        val revertPendingIntent = PendingIntent.getBroadcast(
            this, 0, revertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_running))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(0, "Revert resolution", revertPendingIntent)
            .build()
    }
}
