package com.gamespace.app

import android.content.Context
import android.content.Intent

/**
 * Applies or reverts a GameProfile's full settings (crosshair, color,
 * downscale, renderer, monitor metrics) in one place, so both the manual
 * "Start overlay" button and the automatic game-detection path
 * (GameMonitorService) use exactly the same logic.
 */
object GameSessionManager {

    fun applyProfile(context: Context, profile: GameProfile) {
        val crosshairIntent = Intent(context, OverlayService::class.java)
        if (profile.crosshairEnabled) {
            crosshairIntent.action = OverlayService.ACTION_SET_CROSSHAIR
            crosshairIntent.putExtra(
                OverlayService.EXTRA_CROSSHAIR_STYLE_JSON,
                profile.crosshairStyle.toJson().toString()
            )
        } else {
            crosshairIntent.action = OverlayService.ACTION_CLEAR_CROSSHAIR
        }
        context.startService(crosshairIntent)

        context.startService(Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SET_COLOR_PRESET
            putExtra(OverlayService.EXTRA_PRESET_NAME, profile.colorPreset)
        })

        context.startService(Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SET_MONITOR_METRICS
            putExtra(OverlayService.EXTRA_METRICS_JSON, metricsToJson(profile.monitorMetrics))
        })

        DisplayScaler.apply(context, profile.downscalePercent)

        if (profile.rendererKey.isNotEmpty() && PrivilegedExec.isAvailable()) {
            RenderingManager.applyRenderer(RenderingManager.rendererByKey(profile.rendererKey))
        }
    }

    /** Reverts everything to defaults — called when the detected foreground game changes away. */
    fun revertAll(context: Context) {
        context.startService(Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_CLEAR_CROSSHAIR
        })
        context.startService(Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SET_COLOR_PRESET
            putExtra(OverlayService.EXTRA_PRESET_NAME, "off")
        })
        DisplayScaler.reset(context)
        if (PrivilegedExec.isAvailable()) {
            RenderingManager.resetAll()
        }
    }

    private fun metricsToJson(m: MonitorMetrics): String {
        return org.json.JSONObject().apply {
            put("fps", m.fps)
            put("cpu", m.cpu)
            put("ram", m.ram)
            put("gpu", m.gpu)
            put("temperature", m.temperature)
        }.toString()
    }
}
