package com.gamespace.app

import android.content.Context
import android.view.WindowManager

/**
 * Refresh rate ≠ game FPS. This only reports/sets the DISPLAY's refresh
 * rate — the game can still render below that rate. A normal app can only
 * request a preferred mode for ITS OWN window (no privilege needed, but
 * has zero effect on another app like a game); forcing the rate
 * system-wide (so it actually affects the game you're about to open)
 * needs Settings.System `peak_refresh_rate`/`min_refresh_rate`, which
 * needs Shizuku or root like the other system-wide controls here.
 */
object DisplayController {

    /** Real Hz values this specific display actually reports supporting. */
    fun supportedRefreshRates(context: Context): List<Float> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        @Suppress("DEPRECATION")
        return display.supportedModes.map { it.refreshRate }.distinct().sorted()
    }

    @Suppress("DEPRECATION")
    fun currentRefreshRate(context: Context): Float {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.defaultDisplay.refreshRate
    }

    /** System-wide — affects every app, including the next game you open. Needs Shizuku/root. */
    fun setSystemWideRefreshRate(hz: Float): Boolean {
        val rounded = hz.toInt()
        val ok1 = PrivilegedExec.run("settings put system peak_refresh_rate $rounded") != null
        val ok2 = PrivilegedExec.run("settings put system min_refresh_rate $rounded") != null
        return ok1 && ok2
    }

    fun resetSystemWideRefreshRate(): Boolean {
        val ok1 = PrivilegedExec.run("settings delete system peak_refresh_rate") != null
        val ok2 = PrivilegedExec.run("settings delete system min_refresh_rate") != null
        return ok1 && ok2
    }
}
