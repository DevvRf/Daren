package com.gamespace.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DeviceInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        val info = DeviceInfoProvider.collect(this)
        findViewById<TextView>(R.id.txtDeviceInfo).text = buildString {
            append("Manufacturer: ${info.manufacturer}\n")
            append("Model: ${info.model}\n")
            append("Device/Board/Hardware: ${info.device} / ${info.board} / ${info.hardware}\n\n")
            append("Android: ${info.androidVersion} (API ${info.sdkInt})\n")
            append("ABIs: ${info.abis.joinToString(", ")}\n")
            append("CPU cores: ${info.cpuCores}\n")
            append("RAM total: ${info.ramTotalMb} MB\n\n")
            append("Screen: ${info.screenWidthPx}x${info.screenHeightPx} px, ${info.densityDpi} dpi\n")
            append("Refresh rate (current): ${info.refreshRateHz} Hz\n")
            append("Reported GLES version: ${info.glEsVersion}\n\n")
            append("Catatan: data di atas diambil langsung dari Build/ActivityManager/DisplayMetrics — ")
            append("bukan hasil benchmark atau estimasi.")
        }
    }
}
