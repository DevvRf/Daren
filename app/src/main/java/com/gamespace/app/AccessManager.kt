package com.gamespace.app

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Central place that checks every access path the app can use — root,
 * Shizuku, overlay, WRITE_SECURE_SETTINGS, Notification Policy Access,
 * usage-stats — and reports each as a [CapabilityResult]. Every feature
 * that needs one of these should ask AccessManager rather than assuming.
 */
object AccessManager {

    data class AccessItem(
        val name: String,
        val status: CapabilityStatus,
        val reason: String
    )

    fun rootStatus(): CapabilityResult {
        return if (RootUtils.isRootAvailable()) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "Root access detected (su available).")
        } else {
            CapabilityResult(CapabilityStatus.UNSUPPORTED, "No root detected on this device/ROM.")
        }
    }

    fun shizukuStatus(): CapabilityResult {
        if (!ShizukuManager.isShizukuInstalled()) {
            return CapabilityResult(
                CapabilityStatus.PERMISSION_REQUIRED,
                "Shizuku app isn't running. Install it and start it via wireless debugging or root."
            )
        }
        return if (ShizukuManager.hasPermission()) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "Shizuku running and permission granted.")
        } else {
            CapabilityResult(
                CapabilityStatus.PERMISSION_REQUIRED,
                "Shizuku is running but this app hasn't been granted permission yet."
            )
        }
    }

    fun overlayStatus(context: Context): CapabilityResult {
        return if (Settings.canDrawOverlays(context)) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "Draw-over-other-apps permission granted.")
        } else {
            CapabilityResult(
                CapabilityStatus.PERMISSION_REQUIRED,
                "Needed for crosshair/stats/bubble overlays. Grant via system settings."
            )
        }
    }

    /** Covers color presets, ANGLE driver selection, and anything else using Settings.Secure/Global. */
    fun writeSecureSettingsStatus(context: Context): CapabilityResult {
        val granted = context.packageManager.checkPermission(
            "android.permission.WRITE_SECURE_SETTINGS",
            context.packageName
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            granted -> CapabilityResult(CapabilityStatus.SUPPORTED, "WRITE_SECURE_SETTINGS granted directly.")
            PrivilegedExec.isAvailable() -> CapabilityResult(
                CapabilityStatus.PARTIALLY_SUPPORTED,
                "Not granted directly, but Shizuku/root is available as a fallback for these settings."
            )
            else -> CapabilityResult(
                CapabilityStatus.REQUIRES_SHIZUKU,
                "Needs either 'adb shell pm grant ... WRITE_SECURE_SETTINGS', or Shizuku, or root."
            )
        }
    }

    fun dndStatus(context: Context): CapabilityResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (nm.isNotificationPolicyAccessGranted) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "Notification Policy Access granted.")
        } else {
            CapabilityResult(
                CapabilityStatus.PERMISSION_REQUIRED,
                "Needed to toggle Do Not Disturb. Grant via Settings > Notification access."
            )
        }
    }

    fun writeSettingsStatus(context: Context): CapabilityResult {
        return if (Settings.System.canWrite(context)) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "WRITE_SETTINGS granted (needed for brightness control).")
        } else {
            CapabilityResult(
                CapabilityStatus.PERMISSION_REQUIRED,
                "Needed to change screen brightness. Grant via system settings."
            )
        }
    }

    fun usageStatsStatus(context: Context): CapabilityResult {
        val mode = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            appOps.unsafeCheckOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )
        } catch (e: Exception) {
            android.app.AppOpsManager.MODE_ERRORED
        }
        return if (mode == android.app.AppOpsManager.MODE_ALLOWED) {
            CapabilityResult(CapabilityStatus.SUPPORTED, "Usage access granted (foreground-app detection without root).")
        } else {
            CapabilityResult(
                CapabilityStatus.PARTIALLY_SUPPORTED,
                "Not granted — foreground-app detection falls back to Shizuku/root (dumpsys) if available."
            )
        }
    }

    /** Full list for the Access Manager screen. */
    fun allAccessItems(context: Context): List<AccessItem> {
        val items = mutableListOf<AccessItem>()
        rootStatus().let { items.add(AccessItem("Root", it.status, it.reason)) }
        shizukuStatus().let { items.add(AccessItem("Shizuku", it.status, it.reason)) }
        overlayStatus(context).let { items.add(AccessItem("Draw over other apps", it.status, it.reason)) }
        writeSecureSettingsStatus(context).let { items.add(AccessItem("WRITE_SECURE_SETTINGS (color/ANGLE)", it.status, it.reason)) }
        dndStatus(context).let { items.add(AccessItem("Notification Policy Access (DND)", it.status, it.reason)) }
        writeSettingsStatus(context).let { items.add(AccessItem("WRITE_SETTINGS (brightness)", it.status, it.reason)) }
        usageStatsStatus(context).let { items.add(AccessItem("Usage Access (game detection)", it.status, it.reason)) }
        return items
    }
}
