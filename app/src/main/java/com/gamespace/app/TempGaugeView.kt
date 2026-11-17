package com.gamespace.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A 270-degree arc gauge for a temperature reading (°C). Color shifts
 * green -> yellow -> red as [currentTemp] rises through [warnTemp] and
 * [hotTemp]. The needle angle is computed directly from the real reading
 * passed in via [setTemp] — nothing here is animated independently of
 * actual data.
 */
class TempGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var currentTemp: Float? = null
    private val minTemp = 20f
    private val maxTemp = 90f
    private val warnTemp = 45f
    private val hotTemp = 60f

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#332A2D36")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 32f
    }

    fun setTemp(celsius: Float?) {
        currentTemp = celsius
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0) return
        val stroke = arcPaint.strokeWidth
        val rect = RectF(stroke, stroke, size - stroke, size - stroke)

        val startAngle = 135f
        val sweepTotal = 270f

        canvas.drawArc(rect, startAngle, sweepTotal, false, trackPaint)

        val temp = currentTemp
        if (temp != null) {
            val fraction = ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
            arcPaint.color = when {
                temp < warnTemp -> Color.parseColor("#5CF2C0")
                temp < hotTemp -> Color.parseColor("#F2C85C")
                else -> Color.parseColor("#F25C5C")
            }
            canvas.drawArc(rect, startAngle, sweepTotal * fraction, false, arcPaint)
            canvas.drawText("${temp.toInt()}°C", size / 2f, size / 2f + 10f, textPaint)
        } else {
            canvas.drawText("n/a", size / 2f, size / 2f + 10f, textPaint)
        }
    }
}
