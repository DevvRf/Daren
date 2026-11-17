package com.gamespace.app

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

/** Do Not Disturb — real toggle via NotificationManager, requires Notification Policy Access. */
object DndController {
    fun isGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun isDndOn(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    /** Returns false (no-op) if permission isn't granted — never silently pretends to succeed. */
    fun setDndOn(context: Context, on: Boolean): Boolean {
        if (!isGranted(context)) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }

    fun requestAccessIntent(): android.content.Intent =
        android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}

/** Screen brightness — real read/write via Settings.System, requires WRITE_SETTINGS. */
object BrightnessController {
    fun isGranted(context: Context): Boolean = Settings.System.canWrite(context)

    /** 0-255 raw Android brightness scale. Null if it couldn't be read. */
    fun getCurrent(context: Context): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (e: Settings.SettingNotFoundException) {
        null
    }

    fun setCurrent(context: Context, value0to255: Int): Boolean {
        if (!isGranted(context)) return false
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value0to255.coerceIn(0, 255)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun requestAccessIntent(context: Context): android.content.Intent =
        android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
}

/**
 * Volume — only the streams the master prompt explicitly allows: media,
 * notification, alarm. Ringtone/call volume are intentionally NOT exposed
 * here (changing those has different system implications and wasn't
 * asked for).
 */
object VolumeController {
    enum class Stream(val androidStream: Int, val label: String) {
        MEDIA(AudioManager.STREAM_MUSIC, "Media"),
        NOTIFICATION(AudioManager.STREAM_NOTIFICATION, "Notification"),
        ALARM(AudioManager.STREAM_ALARM, "Alarm")
    }

    fun getMax(context: Context, stream: Stream): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getStreamMaxVolume(stream.androidStream)
    }

    fun getCurrent(context: Context, stream: Stream): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getStreamVolume(stream.androidStream)
    }

    fun setVolume(context: Context, stream: Stream, value: Int): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(stream.androidStream, value.coerceIn(0, getMax(context, stream)), 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
