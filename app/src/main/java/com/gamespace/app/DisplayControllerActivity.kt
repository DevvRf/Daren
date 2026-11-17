package com.gamespace.app

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DisplayControllerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_controller)

        findViewById<TextView>(R.id.txtCurrentRate).text =
            "Refresh rate saat ini: ${DisplayController.currentRefreshRate(this)} Hz"

        val rateGroup = findViewById<RadioGroup>(R.id.rateGroup)
        val rates = DisplayController.supportedRefreshRates(this)
        rates.forEachIndexed { index, hz ->
            val rb = RadioButton(this).apply {
                text = "${hz} Hz"
                id = index + 1000
                setTextColor(resources.getColor(R.color.text_primary, theme))
            }
            rateGroup.addView(rb)
        }
        if (rates.isNotEmpty()) (rateGroup.getChildAt(0) as RadioButton).isChecked = true

        findViewById<Button>(R.id.btnApplyRate).setOnClickListener {
            val checkedIndex = rateGroup.checkedRadioButtonId - 1000
            if (checkedIndex !in rates.indices) return@setOnClickListener
            if (!PrivilegedExec.isAvailable()) {
                Toast.makeText(this, "Butuh Shizuku atau root", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ok = DisplayController.setSystemWideRefreshRate(rates[checkedIndex])
            Toast.makeText(this, if (ok) "Diterapkan" else "Gagal", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnResetRate).setOnClickListener {
            val ok = DisplayController.resetSystemWideRefreshRate()
            Toast.makeText(this, if (ok) "Direset" else "Gagal", Toast.LENGTH_SHORT).show()
        }
    }
}
