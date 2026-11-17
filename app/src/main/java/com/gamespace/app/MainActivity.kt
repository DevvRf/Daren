package com.gamespace.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

/**
 * Pure permissions/settings screen. Reached from the home screen
 * (GameListActivity) via the "Izin" button. Per-game crosshair, color
 * preset, downscale and the actual start/stop overlay controls live in
 * GameCustomizeActivity instead.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var txtPrivilegeStatus: TextView

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            refreshPrivilegeStatus()
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ShizukuManager.ensureServiceBound(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGrantOverlay = findViewById<Button>(R.id.btnGrantOverlay)
        val btnGrantSettings = findViewById<Button>(R.id.btnGrantSettings)
        val btnGrantShizuku = findViewById<Button>(R.id.btnGrantShizuku)
        txtPrivilegeStatus = findViewById(R.id.txtPrivilegeStatus)

        findViewById<Button>(R.id.btnAccessManager).setOnClickListener {
            startActivity(Intent(this, AccessManagerActivity::class.java))
        }

        ShizukuManager.addPermissionResultListener(shizukuPermissionListener)
        if (ShizukuManager.hasPermission()) ShizukuManager.ensureServiceBound(this)
        refreshPrivilegeStatus()

        btnGrantOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnGrantSettings.setOnClickListener {
            Toast.makeText(
                this,
                "Grant via: adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS" +
                    " — or it will fall back to root/Shizuku automatically if available.",
                Toast.LENGTH_LONG
            ).show()
        }

        btnGrantShizuku.setOnClickListener {
            if (!ShizukuManager.isShizukuInstalled()) {
                Toast.makeText(
                    this,
                    "Shizuku isn't running. Install the Shizuku app, start it via " +
                        "wireless debugging (Android 11+) or its root option, then come back here.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (ShizukuManager.hasPermission()) {
                ShizukuManager.ensureServiceBound(this)
                refreshPrivilegeStatus()
                Toast.makeText(this, "Shizuku already enabled", Toast.LENGTH_SHORT).show()
            } else {
                ShizukuManager.requestPermission()
            }
        }
    }

    private fun refreshPrivilegeStatus() {
        val mode = PrivilegedExec.activeMode()
        txtPrivilegeStatus.text = "Privileged mode: ${mode.name.lowercase()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.removePermissionResultListener(shizukuPermissionListener)
    }
}
