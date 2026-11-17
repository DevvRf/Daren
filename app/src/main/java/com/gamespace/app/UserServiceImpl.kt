package com.gamespace.app

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * This class is not instantiated by our own app process — Shizuku starts a
 * separate process (running as the shell UID, or root if the device is
 * rooted and the user allowed Shizuku to run as root) and instantiates this
 * class there via the no-arg constructor Shizuku requires. Everything in
 * here runs with whatever privilege Shizuku granted, not our app's own.
 */
class UserServiceImpl : IUserService.Stub() {

    // Required no-arg constructor — Shizuku instantiates this reflectively.
    constructor()

    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
