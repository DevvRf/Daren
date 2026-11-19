package com.gamespace.app

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager

class CrosshairOverlayView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: CrosshairDrawView? = null
    private var params: WindowManager.LayoutParams? = null
    private var onDragEnd: ((offsetXDp: Int, offsetYDp: Int) -> Unit)? = null

    fun show(style: CrosshairStyle, editable: Boolean = false, onDragEnd: ((Int, Int) -> Unit)? = null) {
        hide()
        this.onDragEnd = onDragEnd

        val density = context.resources.displayMetrics.density
        val sizePx = ((style.sizeDp + 24) * density).toInt()

        val drawView = CrosshairDrawView(context).apply {
            this.style = style
        }

        val touchFlag = if (editable) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        val layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            touchFlag or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = (style.offsetXDp * density).toInt()
            y = (style.offsetYDp * density).toInt()
        }

        if (editable) {
            attachDragHandling(drawView, layoutParams, density)
        }

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
            }
        }
        view = null
        params = null
    }

    private fun attachDragHandling(target: CrosshairDrawView, layoutParams: WindowManager.LayoutParams, density: Float) {
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
                MotionEvent.ACTION_UP -> {
                    val offsetXDp = (layoutParams.x / density).toInt()
                    val offsetYDp = (layoutParams.y / density).toInt()
                    onDragEnd?.invoke(offsetXDp, offsetYDp)
                    true
                }
                else -> false
            }
        }
    }
}
