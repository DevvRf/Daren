package com.gamespace.app

import android.app.ActivityManager
import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Reads device performance stats from the public /proc and /sys filesystems.
 *
 * Everything here reads files any app can read on stock Android (no root
 * needed) except where noted. Thermal-zone and GPU-clock paths vary a lot
 * between chipsets (Qualcomm/MediaTek/Exynos all differ), so this scans a
 * list of common paths and returns the first one that parses. On some
 * devices GPU load/clock simply isn't exposed without root or a
 * vendor-specific driver node — in that case we report "n/a" rather than a
 * fake number.
 */
class SystemStatsReader(private val context: Context) {

    data class Snapshot(
        val cpuUsagePercent: Float?,
        val ramUsedMb: Long,
        val ramTotalMb: Long,
        val batteryTempC: Float?,
        val cpuTempC: Float?,
        val gpuClockMhz: Int?,
        val fps: Int?
    )

    // ---- CPU usage, from /proc/stat (delta between two reads) ----
    private var lastTotal = 0L
    private var lastIdle = 0L

    private fun readCpuDelta(): Float? {
        return try {
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val line = reader.readLine() ?: return null
                // cpu  user nice system idle iowait irq softirq steal
                val parts = line.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
                if (parts.size < 4) return null
                val idle = parts[3] + (parts.getOrElse(4) { 0L })
                val total = parts.sum()

                val deltaTotal = total - lastTotal
                val deltaIdle = idle - lastIdle
                lastTotal = total
                lastIdle = idle

                if (deltaTotal <= 0) return null
                val usage = (1f - deltaIdle.toFloat() / deltaTotal.toFloat()) * 100f
                usage.coerceIn(0f, 100f)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---- RAM, from ActivityManager (always available, no permission) ----
    private fun readRam(): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024 * 1024)
        val usedMb = (info.totalMem - info.availMem) / (1024 * 1024)
        return usedMb to totalMb
    }

    // ---- Battery temperature, from the sticky battery broadcast (set by MainActivity) ----
    @Volatile var lastBatteryTempTenthsC: Int? = null

    // ---- CPU temperature, scanning common thermal-zone nodes ----
    private val cpuThermalCandidates = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp"
    )

    private fun readCpuTemp(): Float? {
        for (path in cpuThermalCandidates) {
            val value = readFirstLongFromFile(path) ?: continue
            // Most kernels report millidegrees (e.g. 38200 = 38.2C);
            // a few report whole degrees. Normalize by magnitude.
            return if (value > 1000) value / 1000f else value.toFloat()
        }
        return null
    }

    // ---- GPU clock, scanning common vendor nodes (best-effort, often root-only) ----
    // Honest note: there is no universal path. These are grouped by GPU
    // vendor/chipset family; a given device only has the node(s) matching
    // its own chipset. For a MediaTek + Mali device (e.g. Helio G88 /
    // Mali-G52, the GSR reference test device) the Mali/MediaTek entries
    // are the ones expected to work, if any — many MediaTek builds still
    // don't expose GPU clock without root. If none of these read, the app
    // correctly reports "n/a" rather than guessing.
    private val gpuClockCandidates = listOf(
        // Qualcomm Adreno
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        // Mali (Bifrost/Valhall; common on MediaTek + some Exynos)
        "/sys/class/misc/mali0/device/cur_freq",
        "/sys/class/misc/mali0/device/clock",
        "/sys/devices/platform/mali/cur_freq",
        "/sys/kernel/debug/mali0/dvfs/curr_freq",
        // Generic devfreq node some MediaTek/Exynos builds expose
        "/sys/class/devfreq/gpufreq/cur_freq",
        "/sys/kernel/gpu/gpu_clock"
    )

    private fun readGpuClockMhz(): Int? {
        for (path in gpuClockCandidates) {
            val hz = readFirstLongFromFile(path) ?: continue
            return (hz / 1_000_000).toInt().takeIf { it > 0 }
        }
        return null
    }

    private fun readFirstLongFromFile(path: String): Long? {
        return try {
            val f = File(path)
            if (!f.exists() || !f.canRead()) return null
            f.readText().trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun snapshot(fps: Int?): Snapshot {
        val (usedMb, totalMb) = readRam()
        return Snapshot(
            cpuUsagePercent = readCpuDelta(),
            ramUsedMb = usedMb,
            ramTotalMb = totalMb,
            batteryTempC = lastBatteryTempTenthsC?.let { it / 10f },
            cpuTempC = readCpuTemp(),
            gpuClockMhz = readGpuClockMhz(),
            fps = fps
        )
    }
}
