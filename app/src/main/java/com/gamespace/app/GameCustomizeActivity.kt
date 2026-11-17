package com.gamespace.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class GameCustomizeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME_ID = "game_id"
        private const val REQ_EDIT_CROSSHAIR = 501
    }

    private lateinit var store: GameProfileStore
    private lateinit var profile: GameProfile
    private var overlayRunning = false

    private lateinit var switchCrosshair: SwitchCompat
    private lateinit var colorGroup: RadioGroup
    private lateinit var rendererGroup: RadioGroup
    private lateinit var switchStats: SwitchCompat
    private lateinit var chkFps: CheckBox
    private lateinit var chkCpu: CheckBox
    private lateinit var chkRam: CheckBox
    private lateinit var chkGpu: CheckBox
    private lateinit var chkTemp: CheckBox
    private lateinit var switchAutoApply: SwitchCompat
    private lateinit var seekDownscale: SeekBar
    private lateinit var txtDownscaleValue: TextView
    private lateinit var btnStartStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_customize)

        store = GameProfileStore(this)
        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        profile = store.loadAll().firstOrNull { it.id == gameId } ?: run {
            finish(); return
        }

        findViewById<TextView>(R.id.txtGameTitle).text = profile.displayName

        switchCrosshair = findViewById(R.id.switchCrosshair)
        colorGroup = findViewById(R.id.colorGroup)
        rendererGroup = findViewById(R.id.rendererGroup)
        switchStats = findViewById(R.id.switchStats)
        chkFps = findViewById(R.id.chkFps)
        chkCpu = findViewById(R.id.chkCpu)
        chkRam = findViewById(R.id.chkRam)
        chkGpu = findViewById(R.id.chkGpu)
        chkTemp = findViewById(R.id.chkTemp)
        switchAutoApply = findViewById(R.id.switchAutoApply)
        seekDownscale = findViewById(R.id.seekDownscale)
        txtDownscaleValue = findViewById(R.id.txtDownscaleValue)
        btnStartStop = findViewById(R.id.btnStartStop)

        restoreSelections()

        switchCrosshair.setOnCheckedChangeListener { _, _ -> persistAndMaybeApply() }
        colorGroup.setOnCheckedChangeListener { _, _ -> persistAndMaybeApply() }
        rendererGroup.setOnCheckedChangeListener { _, _ -> persistAndMaybeApply() }
        switchAutoApply.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && profile.packageName == null) {
                Toast.makeText(this, "Auto-apply butuh package name. Isi dulu lewat Tambah game.", Toast.LENGTH_LONG).show()
                switchAutoApply.isChecked = false
                return@setOnCheckedChangeListener
            }
            persistAndMaybeApply()
            if (isChecked) ensureMonitorServiceRunning()
        }
        for (chk in listOf(chkFps, chkCpu, chkRam, chkGpu, chkTemp)) {
            chk.setOnCheckedChangeListener { _, _ -> persistAndMaybeApply() }
        }
        switchStats.setOnCheckedChangeListener { _, isChecked ->
            if (overlayRunning) {
                startService(Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SET_STATS_VISIBLE
                    putExtra(OverlayService.EXTRA_VISIBLE, isChecked)
                })
            }
        }

        seekDownscale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = 50 + progress
                txtDownscaleValue.text = if (percent >= 100) "100% (resolusi asli)" else "$percent%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { persistAndMaybeApply() }
        })

        findViewById<Button>(R.id.btnEditCrosshair).setOnClickListener {
            val i = Intent(this, CrosshairEditorActivity::class.java)
            i.putExtra(CrosshairEditorActivity.EXTRA_STYLE_JSON_IN, profile.crosshairStyle.toJson().toString())
            startActivityForResult(i, REQ_EDIT_CROSSHAIR)
        }

        findViewById<Button>(R.id.btnLaunchGame).setOnClickListener { launchGame() }
        findViewById<Button>(R.id.btnDeleteGame).setOnClickListener {
            store.delete(profile.id)
            finish()
        }

        btnStartStop.setOnClickListener { toggleOverlay() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_EDIT_CROSSHAIR && resultCode == RESULT_OK) {
            val json = data?.getStringExtra(CrosshairEditorActivity.EXTRA_STYLE_JSON_OUT)
            if (json != null) {
                val style = try { CrosshairStyle.fromJson(org.json.JSONObject(json)) } catch (e: Exception) { CrosshairStyle() }
                profile = profile.copy(crosshairStyle = style)
                store.upsert(profile)
                if (overlayRunning && profile.crosshairEnabled) applyToRunningOverlay()
            }
        }
    }

    private fun currentDownscalePercent(): Int = 50 + seekDownscale.progress

    private fun currentMetrics(): MonitorMetrics = MonitorMetrics(
        fps = chkFps.isChecked, cpu = chkCpu.isChecked, ram = chkRam.isChecked,
        gpu = chkGpu.isChecked, temperature = chkTemp.isChecked
    )

    private fun restoreSelections() {
        switchCrosshair.isChecked = profile.crosshairEnabled

        val colorRadioId = when (profile.colorPreset) {
            "vivid" -> R.id.radioColorVivid
            "ocean" -> R.id.radioColorOcean
            "warm" -> R.id.radioColorWarm
            "grayscale" -> R.id.radioColorGrayscale
            else -> R.id.radioColorOff
        }
        colorGroup.check(colorRadioId)

        val rendererRadioId = when (profile.rendererKey) {
            "skiagl" -> R.id.radioRendererSkiaGl
            "skiavk" -> R.id.radioRendererSkiaVk
            "vulkan" -> R.id.radioRendererVulkan
            "opengles" -> R.id.radioRendererGles
            else -> R.id.radioRendererDefault
        }
        rendererGroup.check(rendererRadioId)

        seekDownscale.progress = (profile.downscalePercent - 50).coerceIn(0, 50)
        txtDownscaleValue.text = if (profile.downscalePercent >= 100) "100% (resolusi asli)" else "${profile.downscalePercent}%"

        chkFps.isChecked = profile.monitorMetrics.fps
        chkCpu.isChecked = profile.monitorMetrics.cpu
        chkRam.isChecked = profile.monitorMetrics.ram
        chkGpu.isChecked = profile.monitorMetrics.gpu
        chkTemp.isChecked = profile.monitorMetrics.temperature

        switchAutoApply.isChecked = profile.autoApply
    }

    private fun selectedColorPreset(): String = when (colorGroup.checkedRadioButtonId) {
        R.id.radioColorVivid -> "vivid"
        R.id.radioColorOcean -> "ocean"
        R.id.radioColorWarm -> "warm"
        R.id.radioColorGrayscale -> "grayscale"
        else -> "off"
    }

    private fun selectedRendererKey(): String = when (rendererGroup.checkedRadioButtonId) {
        R.id.radioRendererSkiaGl -> "skiagl"
        R.id.radioRendererSkiaVk -> "skiavk"
        R.id.radioRendererVulkan -> "vulkan"
        R.id.radioRendererGles -> "opengles"
        else -> ""
    }

    private fun persistAndMaybeApply() {
        profile = profile.copy(
            crosshairEnabled = switchCrosshair.isChecked,
            colorPreset = selectedColorPreset(),
            rendererKey = selectedRendererKey(),
            downscalePercent = currentDownscalePercent(),
            monitorMetrics = currentMetrics(),
            autoApply = switchAutoApply.isChecked
        )
        store.upsert(profile)
        if (overlayRunning) applyToRunningOverlay()
    }

    private fun applyToRunningOverlay() {
        GameSessionManager.applyProfile(this, profile)
    }

    private fun toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan overlay dulu di layar Izin", Toast.LENGTH_SHORT).show()
            return
        }

        if (!overlayRunning) {
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            overlayRunning = true
            btnStartStop.text = getString(R.string.btn_stop_overlay)
            applyToRunningOverlay()
            if (switchStats.isChecked) {
                startService(Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SET_STATS_VISIBLE
                    putExtra(OverlayService.EXTRA_VISIBLE, true)
                })
            }
        } else {
            GameSessionManager.revertAll(this)
            stopService(Intent(this, OverlayService::class.java))
            overlayRunning = false
            btnStartStop.text = getString(R.string.btn_start_overlay)
        }
    }

    private fun ensureMonitorServiceRunning() {
        val intent = Intent(this, GameMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun launchGame() {
        val pkg = profile.packageName
        if (pkg == null) {
            Toast.makeText(this, "Game ini belum punya package name tersimpan", Toast.LENGTH_SHORT).show()
            return
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Toast.makeText(this, "$pkg belum terinstall di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(launchIntent)
    }
}
