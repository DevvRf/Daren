package com.gamespace.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import rikka.shizuku.Shizuku

class PermissionGateActivity : AppCompatActivity() {

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuManager.ensureServiceBound(this)
            }
            refreshAll()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_gate)
        ShizukuManager.addPermissionResultListener(shizukuPermissionListener)

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnShizuku).setOnClickListener {
            if (!ShizukuManager.isShizukuInstalled()) {
                android.widget.Toast.makeText(
                    this,
                    "Install app Shizuku dulu, jalankan via wireless debugging atau root.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else if (ShizukuManager.hasPermission()) {
                ShizukuManager.ensureServiceBound(this)
                refreshAll()
            } else {
                ShizukuManager.requestPermission()
            }
        }
        findViewById<Button>(R.id.btnUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        findViewById<Button>(R.id.btnDnd).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        findViewById<Button>(R.id.btnWriteSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnMic).setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 77)
        }
        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivity(Intent(this, GameListActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun micGranted(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun refreshAll() {
        val overlayOk = Settings.canDrawOverlays(this)
        val shizukuOk = ShizukuManager.hasPermission()
        val usageOk = ForegroundAppDetector.hasUsageAccess(this)
        val dndOk = DndController.isGranted(this)
        val writeSettingsOk = BrightnessController.isGranted(this)
        val micOk = micGranted()

        setStatus(R.id.txtOverlayStatus, "Overlay", overlayOk)
        setStatus(R.id.txtShizukuStatus, "Shizuku", shizukuOk)
        setStatus(R.id.txtUsageStatus, "Usage Access", usageOk)
        setStatus(R.id.txtDndStatus, "DND", dndOk)
        setStatus(R.id.txtWriteSettingsStatus, "Brightness", writeSettingsOk)
        setStatus(R.id.txtMicStatus, "Mikrofon", micOk)

        val allOk = overlayOk && shizukuOk && usageOk && dndOk && writeSettingsOk && micOk
        findViewById<Button>(R.id.btnContinue).isEnabled = allOk
    }

    private fun setStatus(viewId: Int, label: String, ok: Boolean) {
        findViewById<TextView>(viewId).apply {
            text = if (ok) "$label - OK" else "$label - belum"
            setTextColor(resources.getColor(if (ok) R.color.accent else R.color.text_primary, theme))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.removePermissionResultListener(shizukuPermissionListener)
    }
}
