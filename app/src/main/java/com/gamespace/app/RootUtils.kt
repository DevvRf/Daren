package com.gamespace.app

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Thin wrapper around `su -c`. Used only for the handful of things that
 * genuinely need root on stock Android:
 *  - reading another app's frame timing (for the FPS overlay)
 *  - writing the system color-matrix settings if WRITE_SECURE_SETTINGS
 *    wasn't granted via adb
 *
 * Every call is a single, explicit shell command — nothing is hidden from
 * the user and nothing touches any app other than reading public
 * `dumpsys` output.
 */
object RootUtils {

    private var rootAvailable: Boolean? = null

    fun isRootAvailable(): Boolean {
        rootAvailable?.let { return it }
        val result = runAsRoot("id")?.contains("uid=0") == true
        rootAvailable = result
        return result
    }

    fun runAsRoot(command: String): String? {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }

    /** Returns the package name currently in the foreground, or null. */
    fun foregroundPackage(): String? = extractForegroundPackage(runAsRoot("dumpsys activity activities"))
}

/**
 * Parsing helpers shared by both privileged backends (root and Shizuku),
 * since the underlying shell commands are identical — only how the
 * command gets run (su vs. the Shizuku user service) differs.
 */
fun extractForegroundPackage(dump: String?): String? {
    dump ?: return null
    val regex = Regex("mResumedActivity.*?\\{.*?\\s([a-zA-Z0-9_.]+)/")
    return regex.find(dump)?.groupValues?.get(1)
}

/**
 * Rough FPS estimate from `dumpsys gfxinfo <pkg> framestats` output, over
 * the last ~1 second window. This is an approximation — Android doesn't
 * expose a clean "current FPS of another app" API even with elevated
 * privilege, so we count rendered frames reported in the profile and
 * divide by the window.
 */
fun parseFpsFromFramestats(dump: String?): Int? {
    dump ?: return null
    val frameLines = dump.lineSequence()
        .filter { it.trim().firstOrNull()?.isDigit() == true && it.contains(",") }
        .toList()
    if (frameLines.size < 2) return null

    val timestamps = frameLines.mapNotNull { it.split(",").firstOrNull()?.trim()?.toLongOrNull() }
    if (timestamps.size < 2) return null

    val sorted = timestamps.sorted()
    val windowNs = sorted.last() - sorted.first()
    if (windowNs <= 0) return null
    val fps = (sorted.size - 1) * 1_000_000_000.0 / windowNs
    return fps.toInt().coerceIn(0, 240)
}
