package com.gamespace.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class RuntimeCompilerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_compiler)

        val edtPackage = findViewById<EditText>(R.id.edtTargetPackage)
        val modeGroup = findViewById<RadioGroup>(R.id.modeGroup)
        val txtOutput = findViewById<TextView>(R.id.txtCompileOutput)

        findViewById<Button>(R.id.btnCheckStatus).setOnClickListener {
            val pkg = edtPackage.text.toString().trim()
            txtOutput.text = "Checking…"
            thread {
                val status = RuntimeCompilerManager.currentStatus(pkg) ?: "Tidak bisa membaca status (cek Shizuku/root, atau package salah)."
                runOnUiThread { txtOutput.text = status }
            }
        }

        findViewById<Button>(R.id.btnCompile).setOnClickListener {
            val pkg = edtPackage.text.toString().trim()
            if (pkg.isEmpty()) {
                txtOutput.text = "Isi package name dulu."
                return@setOnClickListener
            }
            val mode = when (modeGroup.checkedRadioButtonId) {
                R.id.radioSpeed -> RuntimeCompilerManager.CompileMode.SPEED
                R.id.radioSpeedProfile -> RuntimeCompilerManager.CompileMode.SPEED_PROFILE
                else -> RuntimeCompilerManager.CompileMode.VERIFY
            }

            AlertDialog.Builder(this)
                .setTitle("Konfirmasi kompilasi")
                .setMessage(
                    "Target: $pkg\nMode: ${mode.label}\n${mode.note}\n\n" +
                        "Ini TIDAK menjamin kenaikan FPS otomatis dan butuh akses privileged " +
                        "(Shizuku/root). Lanjutkan?"
                )
                .setPositiveButton("Jalankan") { _, _ ->
                    txtOutput.text = "Running…"
                    thread {
                        val result = RuntimeCompilerManager.compile(pkg, mode)
                        runOnUiThread {
                            txtOutput.text = "Success: ${result.success}\nOutput: ${result.rawOutput}"
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
