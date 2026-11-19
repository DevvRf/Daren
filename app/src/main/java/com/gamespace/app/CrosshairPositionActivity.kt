package com.gamespace.app

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrosshairPositionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME_ID = "game_id"
    }

    private lateinit var overlay: CrosshairOverlayView
    private lateinit var store: GameProfileStore
    private lateinit var profile: GameProfile
    private var currentOffsetX = 0
    private var currentOffsetY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crosshair_position)

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan overlay dulu di layar Izin", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        store = GameProfileStore(this)
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        profile = store.loadAll().firstOrNull { it.id == gameId } ?: run { finish(); return }

        overlay = CrosshairOverlayView(this)
        currentOffsetX = profile.crosshairStyle.offsetXDp
        currentOffsetY = profile.crosshairStyle.offsetYDp

        overlay.show(profile.crosshairStyle, editable = true) { x, y ->
            currentOffsetX = x
            currentOffsetY = y
        }

        findViewById<Button>(R.id.btnSavePosition).setOnClickListener {
            val newStyle = profile.crosshairStyle.copy(offsetXDp = currentOffsetX, offsetYDp = currentOffsetY)
            profile = profile.copy(crosshairStyle = newStyle)
            store.upsert(profile)
            overlay.hide()
            Toast.makeText(this, "Posisi disimpan dan dikunci", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnCancelPosition).setOnClickListener {
            overlay.hide()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlay.hide()
    }
}
