package com.gamespace.app

import android.app.ActivityManager
import android.content.Context

/**
 * Real RAM cleanup: uses the public ActivityManager API to end killable
 * background processes, and (if Shizuku/root is available) a stronger
 * `am force-stop` for individually selected apps. "Freed MB" is computed
 * from an actual before/after memory read, never guessed.
 */
object RamCleaner {

    data class BackgroundApp(
        val packageName: String,
        val label: String,
        val estimatedMb: Long
    )

    private fun memInfo(context: Context): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info
    }

    fun availableMb(context: Context): Long = memInfo(context).availMem / (1024 * 1024)

    /**
     * Lists background apps using real per-process memory reads
     * (Debug.MemoryInfo via ActivityManager), sorted by actual usage.
     * Only processes Android itself reports as backgrounded are included.
     */
    fun listBackgroundApps(context: Context): List<BackgroundApp> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val processes = am.runningAppProcesses ?: return emptyList()

        val backgroundProcesses = processes.filter {
            it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED &&
                it.pkgList.isNotEmpty()
        }
        if (backgroundProcesses.isEmpty()) return emptyList()

        val pids = backgroundProcesses.map { it.pid }.toIntArray()
        val memInfos = try {
            am.getProcessMemoryInfo(pids)
        } catch (e: Exception) {
            null
        }

        return backgroundProcesses.mapIndexedNotNull { index, proc ->
            val pkg = proc.pkgList.firstOrNull() ?: return@mapIndexedNotNull null
            if (pkg == context.packageName) return@mapIndexedNotNull null // never list ourselves
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
            val mb = memInfos?.getOrNull(index)?.totalPss?.div(1024L) ?: 0L
            BackgroundApp(pkg, label, mb)
        }.sortedByDescending { it.estimatedMb }
    }

    /** Kills one app via the public killBackgroundProcesses API — safe, no privilege needed. */
    fun killBackground(context: Context, packageName: String) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.killBackgroundProcesses(packageName)
    }

    /** Stronger stop via privileged shell — stops services too, not just cached processes. */
    fun forceStop(packageName: String): Boolean {
        val safe = ShellSafety.sanitizedPackageNameOrNull(packageName) ?: return false
        return PrivilegedExec.run("am force-stop $safe") != null
    }

    /**
     * Cleans every listed background app via the public API, and returns the
     * REAL before/after available memory difference — never a fabricated number.
     */
    fun cleanAll(context: Context): Long {
        val before = availableMb(context)
        listBackgroundApps(context).forEach { killBackground(context, it.packageName) }
        // Give the system a brief moment to actually reclaim memory before re-reading.
        Thread.sleep(300)
        val after = availableMb(context)
        return (after - before).coerceAtLeast(0)
    }
}
