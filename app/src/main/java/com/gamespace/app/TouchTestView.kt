package com.gamespace.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Measures two real things from actual touch events, not fake numbers:
 *  - maxSimultaneousPointers: the highest pointer count ACTION_POINTER_DOWN
 *    ever reported during the test (true multi-touch capability of this
 *    screen/digitizer).
 *  - average inter-sample interval: time between consecutive MotionEvent
 *    deliveries while a finger is down, which reflects the digitizer's
 *    actual sampling/report rate on this device.
 * Call [reset] to start a fresh run, then read the two getters once the
 * test window (driven by the Activity) ends.
 */
class TouchTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Touch(var x: Float, var y: Float)
    private val activeTouches = mutableMapOf<Int, Touch>()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5CF2C0")
        style = Paint.Style.FILL
    }

    var maxSimultaneousPointers = 0
        private set

    private var lastEventNanos: Long = 0L
    private var intervalSum: Long = 0L
    private var intervalCount: Int = 0

    fun reset() {
        maxSimultaneousPointers = 0
        lastEventNanos = 0L
        intervalSum = 0L
        intervalCount = 0
        activeTouches.clear()
        invalidate()
    }

    /** Average ms between touch samples recorded during the test; null if too little data. */
    fun averageSampleIntervalMs(): Double? {
        if (intervalCount == 0) return null
        return (intervalSum.toDouble() / intervalCount) / 1_000_000.0
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val now = System.nanoTime()
        if (lastEventNanos != 0L) {
            intervalSum += (now - lastEventNanos)
            intervalCount++
        }
        lastEventNanos = now

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                activeTouches[id] = Touch(event.getX(index), event.getY(index))
                if (activeTouches.size > maxSimultaneousPointers) {
                    maxSimultaneousPointers = activeTouches.size
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activeTouches[id]?.let {
                        it.x = event.getX(i)
                        it.y = event.getY(i)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                activeTouches.remove(event.getPointerId(index))
            }
            MotionEvent.ACTION_CANCEL -> activeTouches.clear()
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (touch in activeTouches.values) {
            canvas.drawCircle(touch.x, touch.y, 48f, dotPaint)
        }
    }
}
