package com.gamespace.app

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A real-time HUD panel: a scrolling CPU/RAM line graph (PerfGraphView), a
 * temperature arc gauge (TempGaugeView), and exact-value text for FPS/CPU/
 * RAM/temp/GPU. Every number and every graph point comes from a reading
 * taken this tick via SystemStatsReader / PrivilegedExec — nothing here is
 * simulated or randomized.
 */
/** Which individual metrics are enabled — each toggled independently, per the master prompt's requirement. */
data class MonitorMetrics(
    val fps: Boolean = true,
    val cpu: Boolean = true,
    val ram: Boolean = true,
    val gpu: Boolean = true,
    val temperature: Boolean = true
)

class StatsOverlayView(private val context: Context) {

    var metrics: MonitorMetrics = MonitorMetrics()

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val statsReader = SystemStatsReader(context)
    private val handler = Handler(Looper.getMainLooper())

    private var panel: LinearLayout? = null
    private var textView: TextView? = null
    private var graphView: PerfGraphView? = null
    private var tempGauge: TempGaugeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var updateRunnable: Runnable? = null
    private var usePrivilegedFps = false

    /** 500ms gives a visibly live-updating graph without hammering /proc reads. */
    fun show(updateIntervalMs: Long = 500L) {
        hide()
        usePrivilegedFps = PrivilegedExec.isAvailable()

        val density = context.resources.displayMetrics.density
        val gaugeSizePx = (56 * density).toInt()
        val graphWidthPx = (140 * density).toInt()
        val graphHeightPx = (48 * density).toInt()

        val label = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
            text = "Game Space\nstarting…"
        }
        textView = label

        val graph = PerfGraphView(context).apply {
            layoutParams = LinearLayout.LayoutParams(graphWidthPx, graphHeightPx)
        }
        graphView = graph

        val gauge = TempGaugeView(context).apply {
            layoutParams = LinearLayout.LayoutParams(gaugeSizePx, gaugeSizePx).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = (6 * density).toInt()
            }
        }
        tempGauge = gauge

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(graph)
            addView(gauge)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1A1C22"))
            addView(topRow)
            addView(label)
        }
        panel = container

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (64 * density).toInt()
        }
        params = layoutParams

        attachDragHandling(container, layoutParams)
        windowManager.addView(container, layoutParams)

        startUpdates(updateIntervalMs)
    }

    private fun startUpdates(intervalMs: Long) {
        val runnable = object : Runnable {
            override fun run() {
                val fps = if (usePrivilegedFps) {
                    PrivilegedExec.foregroundPackage()?.let { PrivilegedExec.estimateFps(it) }
                } else null

                val snap = statsReader.snapshot(fps)

                val ramPercent = if (snap.ramTotalMb > 0) {
                    (snap.ramUsedMb.toFloat() / snap.ramTotalMb.toFloat()) * 100f
                } else null

                graphView?.pushSample(
                    if (metrics.cpu) snap.cpuUsagePercent else null,
                    if (metrics.ram) ramPercent else null
                )
                if (metrics.temperature) tempGauge?.setTemp(snap.cpuTempC ?: snap.batteryTempC)
                tempGauge?.visibility = if (metrics.temperature) android.view.View.VISIBLE else android.view.View.GONE
                graphView?.visibility = if (metrics.cpu || metrics.ram) android.view.View.VISIBLE else android.view.View.GONE

                textView?.text = buildString {
                    // Labeled "(est.)" because this FPS comes from sampling
                    // dumpsys gfxinfo's frame timestamps over ~1s, not a
                    // frame-accurate counter — accuracy varies by game and
                    // isn't guaranteed to match the game's own FPS exactly.
                    if (metrics.fps) append("FPS (est.): ${snap.fps?.toString() ?: "n/a"}\n")
                    if (metrics.cpu) append("CPU: ${snap.cpuUsagePercent?.let { "%.0f%%".format(it) } ?: "n/a"}\n")
                    if (metrics.ram) append("RAM: ${snap.ramUsedMb}/${snap.ramTotalMb} MB\n")
                    if (metrics.temperature) append("Battery: ${snap.batteryTempC?.let { "%.1f°C".format(it) } ?: "n/a"}\n")
                    if (metrics.gpu) append("GPU: ${snap.gpuClockMhz?.let { "$it MHz" } ?: "n/a"}")
                }
                handler.postDelayed(this, intervalMs)
            }
        }
        updateRunnable = runnable
        handler.post(runnable)
    }

    /** Feed in the latest battery temperature read from ACTION_BATTERY_CHANGED. */
    fun onBatteryTempUpdate(tenthsOfDegreeC: Int) {
        statsReader.lastBatteryTempTenthsC = tenthsOfDegreeC
    }

    fun hide() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
        panel?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // already removed
            }
        }
        panel = null
        textView = null
        graphView = null
        tempGauge = null
        params = null
    }

    private fun attachDragHandling(target: LinearLayout, layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        target.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - touchX).toInt()
                    layoutParams.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(v, layoutParams)
                    true
                }
                else -> false
            }
        }
    }
}
