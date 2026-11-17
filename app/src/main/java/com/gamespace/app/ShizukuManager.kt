package com.gamespace.app

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/**
 * Wraps the dev.rikka.shizuku:api SDK.
 *
 * Shizuku lets an app run privileged commands (shell UID, or root if the
 * user's Shizuku is running as root) without the app itself needing root
 * access or any dangerous permission — the user installs the separate
 * Shizuku app once and starts it via ADB over Wi-Fi/USB, or via root if
 * they have it. This app never bundles or downloads Shizuku itself.
 *
 * Reference: https://github.com/RikkaApps/Shizuku-API
 */
object ShizukuManager {

    private const val REQUEST_CODE = 9100
    private var userService: IUserService? = null
    private var bound = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfigPackage.PACKAGE_NAME, UserServiceImpl::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("privileged")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = service?.let { IUserService.Stub.asInterface(it) }
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            bound = false
        }
    }

    fun isShizukuInstalled(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /** Call from an Activity. Result arrives via the listener registered in MainActivity. */
    fun requestPermission() {
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    fun ensureServiceBound(context: Context) {
        if (bound || !hasPermission()) return
        Shizuku.bindUserService(userServiceArgs, connection)
    }

    fun unbindService() {
        if (bound) {
            try {
                Shizuku.unbindUserService(userServiceArgs, connection, true)
            } catch (e: Exception) {
                // ignore — process may already be gone
            }
        }
        bound = false
        userService = null
    }

    fun isServiceReady(): Boolean = bound && userService != null

    /** Runs a shell command with Shizuku's privilege. Returns null if the service isn't bound. */
    fun exec(command: String): String? {
        return try {
            userService?.exec(command)
        } catch (e: Exception) {
            null
        }
    }
}

/** Small holder so ShizukuManager doesn't need a generated BuildConfig import at file scope. */
object BuildConfigPackage {
    const val PACKAGE_NAME = "com.gamespace.app"
}
