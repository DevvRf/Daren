package com.gamespace.app

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Transparent, no-UI activity: its only job is to show Android's own
 * screen-capture consent dialog (MediaProjectionManager) — this is the
 * sanctioned way to take a screenshot programmatically; there is no
 * "silent screenshot" path here or anywhere else in the app. The user
 * sees and must approve the system dialog every time, since Android
 * requires a fresh grant per capture session.
 */
class ScreenshotActivity : AppCompatActivity() {

    private val requestProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenshotService::class.java).apply {
                putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenshotService.EXTRA_RESULT_DATA, result.data)
            }
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } else {
            Toast.makeText(this, "Screenshot dibatalkan — izin tidak diberikan", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        requestProjection.launch(projectionManager.createScreenCaptureIntent())
    }
}
