package com.gamespace.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager

/** Every value here comes from a real public Android API — nothing guessed. */
object DeviceInfoProvider {

    data class Info(
        val manufacturer: String,
        val model: String,
        val device: String,
        val board: String,
        val hardware: String,
        val androidVersion: String,
        val sdkInt: Int,
        val abis: List<String>,
        val cpuCores: Int,
        val ramTotalMb: Long,
        val screenWidthPx: Int,
        val screenHeightPx: Int,
        val densityDpi: Int,
        val refreshRateHz: Float,
        val glEsVersion: String
    )

    fun collect(context: Context): Info {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = context.resources.displayMetrics
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay

        return Info(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            abis = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            ramTotalMb = memInfo.totalMem / (1024 * 1024),
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            @Suppress("DEPRECATION")
            refreshRateHz = display.refreshRate,
            glEsVersion = am.deviceConfigurationInfo.glEsVersion ?: "unknown"
        )
    }
}
