package com.gamespace.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Pure detection — reports what the device/driver actually exposes via
 * public Android APIs. Does NOT claim any game can be forced onto a
 * different backend; that depends on the game, Android version, and
 * driver support, which this class cannot know for a specific game.
 */
object GraphicsManager {

    data class GraphicsInfo(
        val glEsVersionReported: String,
        val vulkanSupported: Boolean,
        val vulkanVersion: Int,
        val angleLikelyAvailable: CapabilityResult,
        val currentAngleOverridePkgs: String?
    )

    fun detect(context: Context): GraphicsInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val glEs = am.deviceConfigurationInfo.glEsVersion ?: "unknown"

        val pm = context.packageManager
        val vulkanSupported = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersion = try {
            pm.getSystemAvailableFeatures()
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version ?: 0
        } catch (e: Exception) {
            0
        }

        // ANGLE as a selectable driver was introduced around Android 10 (API 29).
        // This is a necessary-but-not-sufficient check — actual per-app ANGLE
        // support still depends on the OEM's driver stack, which this app
        // cannot verify without you actually testing a specific game.
        val angleStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CapabilityResult(
                CapabilityStatus.PARTIALLY_SUPPORTED,
                "Android ${Build.VERSION.RELEASE} supports the ANGLE driver-selection mechanism in principle. Whether a specific game actually renders correctly under ANGLE is untested here and varies by game."
            )
        } else {
            CapabilityResult(
                CapabilityStatus.UNSUPPORTED,
                "ANGLE driver selection needs Android 10+ (API 29). This device reports API ${Build.VERSION.SDK_INT}."
            )
        }

        val currentPkgs = PrivilegedExec.run("settings get global angle_gl_driver_selection_pkgs")?.trim()
            ?.takeIf { it.isNotEmpty() && it != "null" }

        return GraphicsInfo(glEs, vulkanSupported, vulkanVersion, angleStatus, currentPkgs)
    }
}
