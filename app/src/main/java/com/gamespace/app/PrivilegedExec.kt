package com.gamespace.app

/**
 * Picks whichever privileged execution path is available, in order of
 * preference: Shizuku (no root needed, safer, user-revocable) then root
 * (`su`). Callers that just need "run this command with elevated
 * privilege, if any is available" go through here instead of picking
 * Shizuku or RootUtils directly.
 */
enum class PrivilegeMode { NONE, SHIZUKU, ROOT }

object PrivilegedExec {

    /** Which backend is actually usable right now. */
    fun activeMode(): PrivilegeMode = when {
        ShizukuManager.isServiceReady() -> PrivilegeMode.SHIZUKU
        RootUtils.isRootAvailable() -> PrivilegeMode.ROOT
        else -> PrivilegeMode.NONE
    }

    fun isAvailable(): Boolean = activeMode() != PrivilegeMode.NONE

    /** Runs [command] with whichever backend is active. Null if neither is available. */
    fun run(command: String): String? {
        return when (activeMode()) {
            PrivilegeMode.SHIZUKU -> ShizukuManager.exec(command)
            PrivilegeMode.ROOT -> RootUtils.runAsRoot(command)
            PrivilegeMode.NONE -> null
        }
    }

    fun foregroundPackage(): String? = extractForegroundPackage(run("dumpsys activity activities"))

    fun estimateFps(packageName: String): Int? {
        val safe = ShellSafety.sanitizedPackageNameOrNull(packageName) ?: return null
        return parseFpsFromFramestats(run("dumpsys gfxinfo $safe framestats"))
    }

    /** Writes a secure setting via whichever backend is active. */
    fun writeSecureSetting(key: String, value: String): Boolean {
        return run("settings put secure $key \"$value\"") != null
    }
}
