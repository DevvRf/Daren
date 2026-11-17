package com.gamespace.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Renders a CrosshairStyle directly — real dynamic drawing, not a fixed set of preset images. */
class CrosshairDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var style: CrosshairStyle = CrosshairStyle()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val sizePx = style.sizeDp * density
        val thicknessPx = style.thicknessDp * density
        val cx = width / 2f
        val cy = height / 2f

        val baseColor = try {
            Color.parseColor(style.colorHex)
        } catch (e: Exception) {
            Color.parseColor("#39FF6A")
        }
        val alpha = (style.opacityPercent.coerceIn(0, 100) * 255 / 100)
        paint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

        when (style.shape) {
            CrosshairShape.DOT -> {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, sizePx / 4f, paint)
            }
            CrosshairShape.CROSS -> {
                paint.style = Paint.Style.FILL
                paint.strokeWidth = thicknessPx
                canvas.drawRect(cx - thicknessPx / 2f, cy - sizePx / 2f, cx + thicknessPx / 2f, cy + sizePx / 2f, paint)
                canvas.drawRect(cx - sizePx / 2f, cy - thicknessPx / 2f, cx + sizePx / 2f, cy + thicknessPx / 2f, paint)
            }
            CrosshairShape.CIRCLE -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = thicknessPx
                canvas.drawCircle(cx, cy, sizePx / 2f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, thicknessPx, paint)
            }
        }
    }
}
