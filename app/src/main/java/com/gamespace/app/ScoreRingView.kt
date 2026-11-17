package com.gamespace.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Displays a 0-100 score as a filled ring. The score itself is computed by
 * the caller from a real measurement (see TouchTestActivity) and passed in
 * via [setScore] — this view only renders it, it never invents a number.
 */
class ScoreRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var score: Int? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#332A2D36")
        style = Paint.Style.STROKE
        strokeWidth = 18f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }

    fun setScore(value: Int?) {
        score = value?.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0) return
        val stroke = ringPaint.strokeWidth
        val rect = RectF(stroke, stroke, size - stroke, size - stroke)

        canvas.drawArc(rect, -90f, 360f, false, trackPaint)

        val s = score
        if (s != null) {
            ringPaint.color = when {
                s >= 80 -> Color.parseColor("#5CF2C0")
                s >= 50 -> Color.parseColor("#F2C85C")
                else -> Color.parseColor("#F25C5C")
            }
            canvas.drawArc(rect, -90f, 360f * (s / 100f), false, ringPaint)
            canvas.drawText("$s", size / 2f, size / 2f + 16f, textPaint)
        } else {
            canvas.drawText("--", size / 2f, size / 2f + 16f, textPaint)
        }
    }
}
