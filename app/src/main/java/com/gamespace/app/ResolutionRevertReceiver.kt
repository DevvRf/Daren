package com.gamespace.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Handles the "Revert resolution" action button on the overlay's
 * persistent notification, for a one-tap way to undo a downscale without
 * reopening the app — useful if a game looks wrong and you want native
 * resolution back immediately.
 */
class ResolutionRevertReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REVERT = "com.gamespace.app.ACTION_REVERT_RESOLUTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REVERT) {
            val ok = DisplayScaler.reset(context)
            Toast.makeText(
                context,
                if (ok) "Resolution reverted to native" else "Couldn't revert — check Shizuku/root",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
