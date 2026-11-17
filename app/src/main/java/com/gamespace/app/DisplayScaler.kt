package com.gamespace.app

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Lowers the system's rendering resolution to trade visual sharpness for
 * frame rate, using the same `wm size` command the `adb shell wm size`
 * tool uses. This is a system-wide display change (it affects everything
 * on screen, not just one app), which is why it goes through the same
 * privileged backend (Shizuku or root) as the color presets — a normal
 * app has no public API to resize the display, only the shell/root user
 * does.
 */
object DisplayScaler {

    private var nativeWidth: Int = 0
    private var nativeHeight: Int = 0
    private const val PREFS = "display_scaler_state"
    private const val KEY_ACTIVE_PERCENT = "active_percent"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The downscale percent this app last applied (100 or missing = none active). */
    fun lastAppliedPercent(context: Context): Int = prefs(context).getInt(KEY_ACTIVE_PERCENT, 100)

    private fun recordApplied(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_ACTIVE_PERCENT, percent).apply()
    }

    private fun readNativeSizeIfNeeded(context: Context) {
        if (nativeWidth > 0 && nativeHeight > 0) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        nativeWidth = metrics.widthPixels
        nativeHeight = metrics.heightPixels
    }

    /**
     * Applies [percent] (50-100) of native resolution. 100 is a no-op
     * (and also resets any previous downscale, so switching a profile
     * back to 100% cleanly restores native resolution).
     */
    fun apply(context: Context, percent: Int): Boolean {
        val clamped = percent.coerceIn(50, 100)
        if (clamped >= 100) return reset(context)

        readNativeSizeIfNeeded(context)
        if (nativeWidth == 0 || nativeHeight == 0) return false

        val targetW = (nativeWidth * clamped / 100).let { it - (it % 2) } // keep even
        val targetH = (nativeHeight * clamped / 100).let { it - (it % 2) }

        val ok = PrivilegedExec.run("wm size ${targetW}x${targetH}") != null
        if (ok) recordApplied(context, clamped)
        return ok
    }

    /** Restores native resolution. Always safe to call, even if nothing was changed. */
    fun reset(context: Context): Boolean {
        val ok = PrivilegedExec.run("wm size reset") != null
        if (ok) recordApplied(context, 100)
        return ok
    }

    /**
     * Real Android already resets `wm size` on reboot by itself — this is a
     * belt-and-suspenders check for the rare case a privileged write didn't
     * take effect as a full reset (e.g. a partial `su` failure right before
     * shutdown). Called from ResolutionBootReceiver.
     */
    fun verifyResetAfterBoot(context: Context) {
        if (lastAppliedPercent(context) != 100) {
            reset(context)
        }
    }
}
