package com.gamespace.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.ArrayDeque

/**
 * A real-time scrolling line chart. Call [pushSample] on every stats tick
 * with the actual CPU% and RAM% just measured — it keeps a rolling buffer
 * and redraws, so the line always reflects genuinely sampled data, never
 * synthetic/randomized values.
 */
class PerfGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val maxSamples = 60 // ~1 minute at 1s/sample, or ~30s at 500ms
    private val cpuHistory = ArrayDeque<Float>()
    private val ramHistory = ArrayDeque<Float>()

    private val cpuPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5CF2C0")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val ramPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3AA7FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        strokeWidth = 1f
    }

    /** Feed one real sample (0-100 scale for both). Null means "no reading this tick". */
    fun pushSample(cpuPercent: Float?, ramPercent: Float?) {
        if (cpuHistory.size >= maxSamples) cpuHistory.poll()
        if (ramHistory.size >= maxSamples) ramHistory.poll()
        cpuHistory.offer(cpuPercent ?: (cpuHistory.lastOrNull() ?: 0f))
        ramHistory.offer(ramPercent ?: (ramHistory.lastOrNull() ?: 0f))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Light horizontal gridlines at 25/50/75%
        for (frac in listOf(0.25f, 0.5f, 0.75f)) {
            val y = h * (1 - frac)
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        drawSeries(canvas, cpuHistory, w, h, cpuPaint)
        drawSeries(canvas, ramHistory, w, h, ramPaint)
    }

    private fun drawSeries(canvas: Canvas, data: ArrayDeque<Float>, w: Float, h: Float, paint: Paint) {
        if (data.size < 2) return
        val list = data.toList()
        val stepX = w / (maxSamples - 1).toFloat()
        val startX = w - (list.size - 1) * stepX
        var prevX = startX
        var prevY = h * (1 - (list[0].coerceIn(0f, 100f) / 100f))
        for (i in 1 until list.size) {
            val x = startX + i * stepX
            val y = h * (1 - (list[i].coerceIn(0f, 100f) / 100f))
            canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
        }
    }
}
