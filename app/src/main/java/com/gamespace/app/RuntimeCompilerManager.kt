package com.gamespace.app

/**
 * HONEST SCOPE: this controls Android's own ART (Android Runtime)
 * ahead-of-time/JIT compilation filter for an already-installed app, via
 * the real `cmd package compile` / `pm compile` shell command (the same
 * thing `adb shell pm compile` does). It is NOT a compiler for a game's
 * source code, cannot modify an APK, and does not guarantee any FPS
 * improvement — re-compiling with a different filter can make an app's
 * *cold start* and *first-run jank* better or worse depending on the app
 * and device; it does not change a game's actual rendering workload.
 *
 * Available filters vary by Android version/OEM; this exposes the
 * filters that are broadly documented in AOSP (verify, speed,
 * speed-profile). It does not claim support for anything it hasn't
 * actually tried to run, and surfaces the raw shell output back to the
 * user rather than inventing a "success" message.
 */
object RuntimeCompilerManager {

    enum class CompileMode(val flag: String, val label: String, val note: String) {
        VERIFY("verify", "Verify", "Fastest, minimal optimization. Safe default."),
        SPEED("speed", "Speed", "Full AOT compile. Larger storage use, faster first launches, no runtime JIT warm-up."),
        SPEED_PROFILE("speed-profile", "Speed Profile", "AOT-compiles only methods seen as 'hot' in a collected profile. Needs the app to have run enough to build a profile.")
    }

    data class CompileResult(val success: Boolean, val rawOutput: String)

    /**
     * Runs the real compile command for [packageName] with [mode].
     * Returns the actual shell output — never fabricated — so the caller
     * can show the user exactly what Android reported.
     */
    fun compile(packageName: String, mode: CompileMode): CompileResult {
        val safePkg = ShellSafety.sanitizedPackageNameOrNull(packageName)
            ?: return CompileResult(false, "Invalid package name — refused to run.")

        if (!PrivilegedExec.isAvailable()) {
            return CompileResult(false, "Requires Shizuku or root — 'pm compile' needs shell-level access.")
        }

        val output = PrivilegedExec.run("cmd package compile -m ${mode.flag} -f $safePkg")
            ?: return CompileResult(false, "Command failed to run (no output from privileged backend).")

        // Real Android success output for this command is literally "Success"
        val success = output.trim().equals("Success", ignoreCase = true)
        return CompileResult(success, output.trim())
    }

    /** Reads the package's current compile status via the real `dumpsys package` dump, no guessing. */
    fun currentStatus(packageName: String): String? {
        val safePkg = ShellSafety.sanitizedPackageNameOrNull(packageName) ?: return null
        val dump = PrivilegedExec.run("dumpsys package $safePkg") ?: return null
        val line = dump.lineSequence().firstOrNull { it.contains("status=") }
        return line?.trim()
    }
}
