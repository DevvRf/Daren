package com.gamespace.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.roundToInt

/** Futuristic Game Suite floating HUD. Existing callbacks/managers are preserved. */
class FloatingControlBubble(
    private val context: Context,
    private val isCrosshairOn: () -> Boolean,
    private val isStatsOn: () -> Boolean,
    private val onToggleCrosshair: () -> Unit,
    private val onToggleStats: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("floating_hud", Context.MODE_PRIVATE)
    private var rootView: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var keepAwake = false
    private lateinit var handle: TextView
    private lateinit var panel: LinearLayout
    private lateinit var crosshairToggle: TextView
    private lateinit var statsToggle: TextView
    private lateinit var dndToggle: TextView
    private lateinit var monitorText: TextView
    private lateinit var keepAwakeToggle: TextView

    private val red = Color.rgb(255, 23, 79)
    private val muted = Color.rgb(158, 158, 170)
    private val white = Color.WHITE
    private val panelBg = Color.argb(246, 17, 17, 25)

    fun show() {
        if (rootView != null) return
        val density = context.resources.displayMetrics.density
        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panelBg, red, 1.2f, 18f)
            clipToOutline = true
            elevation = dp(12).toFloat()
        }
        handle = TextView(context).apply {
            text = "GS  ❯❯"
            setTextColor(white)
            textSize = 14f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)
            letterSpacing = .08f
            setPadding(dp(15), dp(11), dp(15), dp(11))
            background = shape(Color.argb(235, 24, 12, 22), red, 1f, 18f)
        }
        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }

        buildExpandedPanel()
        outer.addView(handle)
        outer.addView(panel)
        rootView = outer

        val savedX = prefs.getInt("x", dp(12))
        val savedY = prefs.getInt("y", dp(100))
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        params = lp
        attachDragAndTapHandling(handle, outer, lp)
        windowManager.addView(outer, lp)
        refreshToggleLabels()
        refreshMonitor()
    }

    private fun buildExpandedPanel() {
        val title = TextView(context).apply {
            text = "G A M E   S U I T E     /     CONTROL CENTER"
            setTextColor(red)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .08f
            setPadding(dp(3), dp(2), dp(3), dp(7))
        }
        monitorText = TextView(context).apply {
            text = "FPS  N/A   •   CPU  N/A   •   RAM  --"
            setTextColor(white)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = shape(Color.rgb(28, 17, 27), red, .8f, 10f)
        }
        val monitorSub = TextView(context).apply {
            text = "Refresh rate / Battery / Thermal source"
            setTextColor(muted)
            textSize = 9f
            setPadding(dp(4), dp(5), dp(4), dp(8))
        }
        val columns = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val left = cardColumn()
        val right = cardColumn()

        crosshairToggle = controlRow("◎", "Crosshair") { onToggleCrosshair(); refreshToggleLabels() }
        statsToggle = controlRow("▥", "Stats Overlay") { onToggleStats(); refreshToggleLabels() }
        dndToggle = controlRow("◉", "Do Not Disturb") {
            if (!DndController.isGranted(context)) {
                toast("Aktifkan Notification Policy Access melalui Access Manager")
            } else {
                DndController.setDndOn(context, !DndController.isDndOn(context))
            }
            refreshToggleLabels()
        }
        keepAwakeToggle = controlRow("☼", "Keep Screen Awake") {
            keepAwake = !keepAwake
            updateKeepAwakeFlag()
            refreshToggleLabels()
        }
        left.addView(sectionLabel("QUICK CONTROLS"))
        left.addView(brightnessRow())
        left.addView(crosshairToggle)
        left.addView(statsToggle)
        left.addView(dndToggle)
        left.addView(keepAwakeToggle)

        right.addView(sectionLabel("GAMING TOOLS"))
        right.addView(volumeRow())
        right.addView(toolButton("▣  Screenshot") {
            launch(ScreenshotActivity::class.java)
        })
        right.addView(toolButton("◉  Voice Changer") {
            launch(VoiceChangerActivity::class.java)
        })
        right.addView(toolButton("↗  Auto Brightness Settings") {
            launchSettings(Settings.ACTION_DISPLAY_SETTINGS)
        })
        right.addView(toolButton("⌁  Refresh Monitor") { refreshMonitor() })

        columns.addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        columns.addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        val close = TextView(context).apply {
            text = "⌃   TAP TO COLLAPSE"
            gravity = Gravity.CENTER
            setTextColor(red)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(10), dp(8), dp(4))
            setOnClickListener { setExpanded(false) }
        }
        panel.addView(title)
        panel.addView(monitorText)
        panel.addView(monitorSub)
        panel.addView(columns)
        panel.addView(close)
    }

    private fun cardColumn() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(7), dp(7), dp(7), dp(7))
        background = shape(Color.argb(210, 27, 27, 37), Color.argb(150, 255, 23, 79), .6f, 12f)
    }

    private fun sectionLabel(label: String) = TextView(context).apply {
        text = label
        setTextColor(red)
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(3), dp(2), dp(3), dp(7))
    }

    private fun controlRow(icon: String, label: String, action: () -> Unit) = TextView(context).apply {
        text = "$icon  $label: OFF"
        setTextColor(white)
        textSize = 10f
        setPadding(dp(5), dp(9), dp(3), dp(9))
        setOnClickListener { action() }
    }

    private fun toolButton(label: String, action: () -> Unit) = TextView(context).apply {
        text = label
        setTextColor(white)
        textSize = 10f
        setPadding(dp(7), dp(11), dp(5), dp(11))
        background = shape(Color.rgb(35, 27, 37), Color.argb(90, 255, 23, 79), .5f, 8f)
        setOnClickListener { action() }
    }

    private fun brightnessRow(): LinearLayout {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(5), dp(4), dp(5)) }
        val label = smallLabel("☀  BRIGHTNESS")
        val seek = SeekBar(context).apply {
            max = 255
            progress = BrightnessController.getCurrent(context) ?: 128
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser && !BrightnessController.setCurrent(context, value)) toast("Izin WRITE_SETTINGS diperlukan")
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        box.addView(label); box.addView(seek)
        return box
    }

    private fun volumeRow(): LinearLayout {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(5), dp(4), dp(5)) }
        box.addView(smallLabel("♫  MEDIA VOLUME"))
        val seek = SeekBar(context).apply {
            max = VolumeController.getMax(context, VolumeController.Stream.MEDIA).coerceAtLeast(1)
            progress = VolumeController.getCurrent(context, VolumeController.Stream.MEDIA).coerceIn(0, max)
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) VolumeController.setVolume(context, VolumeController.Stream.MEDIA, value)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        box.addView(seek)
        return box
    }

    private fun smallLabel(value: String) = TextView(context).apply {
        text = value; setTextColor(muted); textSize = 9f; typeface = Typeface.DEFAULT_BOLD
    }

    fun refreshToggleLabels() {
        if (!::crosshairToggle.isInitialized) return
        crosshairToggle.text = "◎  Crosshair: ${if (isCrosshairOn()) "ON" else "OFF"}"
        statsToggle.text = "▥  Stats Overlay: ${if (isStatsOn()) "ON" else "OFF"}"
        dndToggle.text = "◉  Do Not Disturb: ${if (DndController.isDndOn(context)) "ON" else "OFF"}"
        keepAwakeToggle.text = "☼  Keep Screen Awake: ${if (keepAwake) "ON" else "OFF"}"
        listOf(crosshairToggle, statsToggle, dndToggle, keepAwakeToggle).forEach {
            it.setTextColor(if (it.text.toString().endsWith("ON")) red else white)
        }
    }

    private fun refreshMonitor() {
        if (!::monitorText.isInitialized) return
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memory = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memory)
            val totalGb = memory.totalMem / (1024f * 1024f * 1024f)
            val usedGb = (memory.totalMem - memory.availMem) / (1024f * 1024f * 1024f)
            val refresh = if (Build.VERSION.SDK_INT >= 30) {
                context.display?.refreshRate ?: 60f
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
            }
            val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else -1
            val tempTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
            val batteryTemp = if (tempTenths != Int.MIN_VALUE) "${tempTenths / 10f}°C BAT" else "BAT TEMP N/A"
            monitorText.text = "FPS N/A  •  CPU N/A\nRAM ${"%.1f".format(usedGb)}/${"%.1f".format(totalGb)} GB  •  ${"%.0f".format(refresh)} Hz\nBAT ${if (percent >= 0) "$percent%" else "N/A"}  •  $batteryTemp"
        } catch (_: Exception) {
            monitorText.text = "FPS N/A  •  CPU N/A  •  MONITOR UNAVAILABLE"
        }
    }

    private fun updateKeepAwakeFlag() {
        val lp = params ?: return
        lp.flags = if (keepAwake) lp.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        else lp.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        rootView?.let { try { windowManager.updateViewLayout(it, lp) } catch (_: Exception) {} }
        refreshToggleLabels()
    }

    private fun setExpanded(value: Boolean) {
        expanded = value
        panel.visibility = if (value) View.VISIBLE else View.GONE
        handle.text = if (value) "GS  HUD  ▾" else "GS  ❯❯"
        refreshMonitor()
    }

    fun hide() {
        rootView?.let { try { windowManager.removeView(it) } catch (_: IllegalArgumentException) {} }
        rootView = null; params = null
    }

    private fun attachDragAndTapHandling(target: TextView, windowView: LinearLayout, lp: WindowManager.LayoutParams) {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        target.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = lp.x; startY = lp.y; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) {
                        lp.x = (startX + dx).roundToInt(); lp.y = (startY + dy).roundToInt()
                        try { windowManager.updateViewLayout(windowView, lp) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) setExpanded(!expanded)
                    else prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    true
                }
                else -> true
            }
        }
    }

    private fun launch(clazz: Class<*>) {
        try { context.startActivity(Intent(context, clazz).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("Tidak dapat membuka fitur") }
    }

    private fun launchSettings(action: String) {
        try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("Pengaturan tidak tersedia") }
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private fun shape(color: Int, stroke: Int, strokeDp: Float, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        setCornerRadius(dp(radiusDp.roundToInt()).toFloat())
        setStroke(dp(strokeDp.roundToInt().coerceAtLeast(1)), stroke)
    }
}
