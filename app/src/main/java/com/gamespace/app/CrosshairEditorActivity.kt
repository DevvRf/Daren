package com.gamespace.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrosshairEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STYLE_JSON_IN = "style_json_in"
        const val EXTRA_STYLE_JSON_OUT = "style_json_out"

        private val PALETTE = listOf(
            "#39FF6A", "#FF4444", "#3AA7FF", "#F2C85C", "#FFFFFF", "#FF00FF"
        )
    }

    private lateinit var preview: CrosshairDrawView
    private var currentStyle = CrosshairStyle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crosshair_editor)

        val initialJson = intent.getStringExtra(EXTRA_STYLE_JSON_IN)
        currentStyle = if (initialJson != null) {
            try { CrosshairStyle.fromJson(org.json.JSONObject(initialJson)) } catch (e: Exception) { CrosshairStyle() }
        } else CrosshairStyle()

        preview = findViewById(R.id.previewView)
        preview.style = currentStyle

        val shapeGroup = findViewById<RadioGroup>(R.id.shapeGroup)
        val seekSize = findViewById<SeekBar>(R.id.seekSize)
        val seekThickness = findViewById<SeekBar>(R.id.seekThickness)
        val seekOpacity = findViewById<SeekBar>(R.id.seekOpacity)
        val labelSize = findViewById<TextView>(R.id.labelSize)
        val labelThickness = findViewById<TextView>(R.id.labelThickness)
        val labelOpacity = findViewById<TextView>(R.id.labelOpacity)
        val colorRow = findViewById<LinearLayout>(R.id.colorSwatchRow)

        shapeGroup.check(
            when (currentStyle.shape) {
                CrosshairShape.DOT -> R.id.radioShapeDot
                CrosshairShape.CROSS -> R.id.radioShapeCross
                CrosshairShape.CIRCLE -> R.id.radioShapeCircle
            }
        )
        seekSize.progress = (currentStyle.sizeDp - 6).coerceIn(0, 54)
        seekThickness.progress = (currentStyle.thicknessDp - 1).coerceIn(0, 9)
        seekOpacity.progress = currentStyle.opacityPercent.coerceIn(0, 100)
        labelSize.text = "Size: ${currentStyle.sizeDp}dp"
        labelThickness.text = "Thickness: ${currentStyle.thicknessDp}dp"
        labelOpacity.text = "Opacity: ${currentStyle.opacityPercent}%"

        PALETTE.forEach { hex ->
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginEnd = 8
                }
                setBackgroundColor(android.graphics.Color.parseColor(hex))
                setOnClickListener {
                    currentStyle = currentStyle.copy(colorHex = hex)
                    preview.style = currentStyle
                }
            }
            colorRow.addView(swatch)
        }

        shapeGroup.setOnCheckedChangeListener { _, checkedId ->
            val shape = when (checkedId) {
                R.id.radioShapeDot -> CrosshairShape.DOT
                R.id.radioShapeCircle -> CrosshairShape.CIRCLE
                else -> CrosshairShape.CROSS
            }
            currentStyle = currentStyle.copy(shape = shape)
            preview.style = currentStyle
        }

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 6
                labelSize.text = "Size: ${size}dp"
                currentStyle = currentStyle.copy(sizeDp = size)
                preview.style = currentStyle
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val thickness = progress + 1
                labelThickness.text = "Thickness: ${thickness}dp"
                currentStyle = currentStyle.copy(thicknessDp = thickness)
                preview.style = currentStyle
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelOpacity.text = "Opacity: $progress%"
                currentStyle = currentStyle.copy(opacityPercent = progress)
                preview.style = currentStyle
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnSaveCrosshair).setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_STYLE_JSON_OUT, currentStyle.toJson().toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
