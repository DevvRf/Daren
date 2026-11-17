package com.gamespace.app

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager

/**
 * A small always-on-top CrosshairDrawView, centered by default and
 * draggable. Renders whatever CrosshairStyle it's given (shape, color,
 * size, thickness, opacity) dynamically — not a fixed set of preset
 * images, so the editor's sliders/colors actually take effect.
 */
class CrosshairOverlayView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: CrosshairDrawView? = null
    private var params: WindowManager.LayoutParams? = null

    fun show(style: CrosshairStyle) {
        hide() // avoid duplicate views if already shown

        val density = context.resources.displayMetrics.density
        // Give the view some margin beyond the crosshair's own size so
        // thick/large styles aren't clipped.
        val sizePx = ((style.sizeDp + 24) * density).toInt()

        val drawView = CrosshairDrawView(context).apply {
            this.style = style
        }

        val layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        attachDragHandling(drawView, layoutParams)

        windowManager.addView(drawView, layoutParams)
        view = drawView
        params = layoutParams
    }

    fun updateStyle(style: CrosshairStyle) {
        view?.style = style
    }

    fun isShowing(): Boolean = view != null

    fun hide() {
        view?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // already removed
            }
        }
        view = null
        params = null
    }

    private fun attachDragHandling(target: CrosshairDrawView, layoutParams: WindowManager.LayoutParams) {
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
