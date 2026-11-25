package com.gamespace.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.abs

/**
 * Floating "GSR" bubble. Tap expands a full quick-panel: Crosshair/Stats
 * toggles, a real DND toggle, real Brightness/Volume sliders, and buttons
 * to launch Screenshot and Voice Changer. Every control here calls the
 * same real managers used elsewhere in the app — nothing here is a
 * decorative/non-functional button.
 */
class FloatingControlBubble(
    private val context: Context,
    private val isCrosshairOn: () -> Boolean,
    private val isStatsOn: () -> Boolean,
    private val onToggleCrosshair: () -> Unit,
    private val onToggleStats: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false

    private lateinit var bubbleLabel: TextView
    private lateinit var panelContainer: LinearLayout
    private lateinit var crosshairToggle: TextView
    private lateinit var statsToggle: TextView
    private lateinit var dndToggle: TextView

    fun show() {
        if (rootView != null) return
        val density = context.resources.displayMetrics.density

        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1A1C22"))
        }

        bubbleLabel = TextView(context).apply {
            text = "GSR"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
        }

        panelContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
        }

        crosshairToggle = rowButton { onToggleCrosshair(); refreshToggleLabels() }
        statsToggle = rowButton { onToggleStats(); refreshToggleLabels() }
        dndToggle = rowButton {
            if (!DndController.isGranted(context)) {
                android.widget.Toast.makeText(context, "Izinkan Notification Policy Access dulu (Access Manager)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                DndController.setDndOn(context, !DndController.isDndOn(context))
            }
            refreshToggleLabels()
        }

        panelContainer.addView(crosshairToggle)
        panelContainer.addView(statsToggle)
        panelContainer.addView(dndToggle)
        panelContainer.addView(brightnessRow(density))
        panelContainer.addView(volumeRow(density))
        panelContainer.addView(actionButton("Screenshot") {
            context.startActivity(Intent(context, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        })
        panelContainer.addView(actionButton("Voice Changer") {
            context.startActivity(Intent(context, VoiceChangerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        })

        outer.addView(bubbleLabel)
        outer.addView(panelContainer)
        rootView = outer

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (16 * density).toInt()
        }
        params = layoutParams

        attachDragAndTapHandling(bubbleLabel, outer, layoutParams)
        windowManager.addView(outer, layoutParams)
        refreshToggleLabels()
    }

    private fun rowButton(onClick: () -> Unit): TextView {
        val density = context.resources.displayMetrics.density
        return TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            setOnClickListener { onClick() }
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView {
        return rowButton(onClick).apply { text = label }
    }

    private fun brightnessRow(density: Float): LinearLayout {
        val row = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val label = TextView(context).apply {
            text = "Brightness"
            setTextColor(Color.WHITE)
            textSize = 11f
        }
        val seek = SeekBar(context).apply {
            max = 255
            progress = BrightnessController.getCurrent(context) ?: 128
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        if (!BrightnessController.setCurrent(context, progress)) {
                            android.widget.Toast.makeText(context, "Izinkan WRITE_SETTINGS dulu", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        row.addView(label)
        row.addView(seek)
        return row
    }

    private fun volumeRow(density: Float): LinearLayout {
        val row = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val label = TextView(context).apply {
            text = "Volume (Media)"
            setTextColor(Color.WHITE)
            textSize = 11f
        }
        val seek = SeekBar(context).apply {
            max = VolumeController.getMax(context, VolumeController.Stream.MEDIA)
            progress = VolumeController.getCurrent(context, VolumeController.Stream.MEDIA)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) VolumeController.setVolume(context, VolumeController.Stream.MEDIA, progress)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        row.addView(label)
        row.addView(seek)
        return row
    }

    fun refreshToggleLabels() {
        crosshairToggle.text = if (isCrosshairOn()) "Crosshair: ON" else "Crosshair: OFF"
        statsToggle.text = if (isStatsOn()) "Stats: ON" else "Stats: OFF"
        dndToggle.text = if (DndController.isDndOn(context)) "DND: ON" else "DND: OFF"
    }

    private fun setExpanded(value: Boolean) {
        expanded = value
        panelContainer.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE
    }

    fun hide() {
        rootView?.let {
            try { windowManager.removeView(it) } catch (e: IllegalArgumentException) { /* already removed */ }
        }
        rootView = null
        params = null
    }

    private fun attachDragAndTapHandling(
        tapTarget: TextView,
        windowView: LinearLayout,
        layoutParams: WindowManager.LayoutParams
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        tapTarget.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    layoutParams.x = initialX + dx.toInt()
                    layoutParams.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(windowView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) setExpanded(!expanded)
                    true
                }
                else -> false
            }
        }
    }
}
