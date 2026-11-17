package com.gamespace.app

import android.content.Context
import android.provider.Settings

/**
 * Applies a system-wide color grading preset.
 *
 * Android has a built-in, public mechanism for this: the display
 * color-correction matrix used by the Accessibility "color correction"
 * feature (Settings.Secure ACCESSIBILITY_DISPLAY_DALTONIZER /
 * ACCESSIBILITY_DISPLAY_COLOR_MATRIX on devices that expose it, surfaced
 * through android.provider.Settings.Secure). Writing it requires
 * WRITE_SECURE_SETTINGS, which a normal app can't self-grant — the user
 * grants it once via adb, or we write it through `su` on a rooted device.
 *
 * This changes the whole screen's color grading (including games running
 * underneath the overlay), not just our own UI.
 */
object ColorFilterManager {

    // 4x4 column-major color matrices, as Android's DISPLAY_COLOR_MATRIX expects.
    private val PRESETS: Map<String, FloatArray> = mapOf(
        "off" to identity(),
        "vivid" to floatArrayOf(
            1.25f, 0f, 0f, 0f,
            0f, 1.2f, 0f, 0f,
            0f, 0f, 1.25f, 0f,
            0f, 0f, 0f, 1f
        ),
        "ocean" to floatArrayOf(
            0.9f, 0f, 0f, 0f,
            0f, 1.05f, 0f, 0f,
            0f, 0f, 1.35f, 0f,
            0f, 0f, 0f, 1f
        ),
        "warm" to floatArrayOf(
            1.15f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f,
            0f, 0f, 0.85f, 0f,
            0f, 0f, 0f, 1f
        ),
        "grayscale" to floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f,
            0.59f, 0.59f, 0.59f, 0f,
            0.11f, 0.11f, 0.11f, 0f,
            0f, 0f, 0f, 1f
        )
    )

    fun presetNames(): List<String> = PRESETS.keys.toList()

    private fun identity() = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    /**
     * Returns true if it applied the preset successfully, false if neither
     * direct settings access nor root worked.
     */
    fun apply(context: Context, presetName: String): Boolean {
        val matrix = PRESETS[presetName] ?: return false
        val matrixString = matrix.joinToString(",")

        // Preferred path: the app already has WRITE_SECURE_SETTINGS
        // (granted ahead of time via `adb shell pm grant`).
        val direct = try {
            Settings.Secure.putString(
                context.contentResolver,
                "accessibility_display_color_matrix",
                matrixString
            )
        } catch (e: SecurityException) {
            false
        }
        if (direct) return true

        // Fallback: write it via Shizuku (preferred, no root needed) or root.
        return PrivilegedExec.writeSecureSetting("accessibility_display_color_matrix", matrixString)
    }

    fun clear(context: Context): Boolean = apply(context, "off")
}
