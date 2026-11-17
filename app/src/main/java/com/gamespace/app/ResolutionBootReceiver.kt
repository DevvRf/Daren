package com.gamespace.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Safety net registered for BOOT_COMPLETED: Android already resets `wm
 * size` to native on every reboot on its own, but if our last recorded
 * state says a downscale was still "active" (e.g. the device lost power
 * mid-write), this verifies and force-resets rather than trusting that
 * record blindly.
 */
class ResolutionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DisplayScaler.verifyResetAfterBoot(context)
        }
    }
}
