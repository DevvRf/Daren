package com.gamespace.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Detects the current foreground app. Prefers UsageStatsManager (needs
 * only the user-grantable "Usage access" permission, no root/Shizuku) and
 * falls back to the privileged dumpsys path only if that's unavailable.
 * Reports which method it used so callers/UI can be honest about it.
 */
object ForegroundAppDetector {

    enum class Method { USAGE_STATS, PRIVILEGED_SHELL, NONE }

    data class Result(val packageName: String?, val method: Method)

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.unsafeCheckOpNoThrow(
                "android:get_usage_stats", Process.myUid(), context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun detect(context: Context): Result {
        if (hasUsageAccess(context)) {
            val pkg = detectViaUsageStats(context)
            if (pkg != null) return Result(pkg, Method.USAGE_STATS)
        }
        if (PrivilegedExec.isAvailable()) {
            val pkg = PrivilegedExec.foregroundPackage()
            if (pkg != null) return Result(pkg, Method.PRIVILEGED_SHELL)
        }
        return Result(null, Method.NONE)
    }

    private fun detectViaUsageStats(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 10_000 // last 10 seconds is enough to catch the latest foreground event
        val events = usm.queryEvents(start, end)
        var lastForegroundPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPkg = event.packageName
            }
        }
        return lastForegroundPkg
    }
}
