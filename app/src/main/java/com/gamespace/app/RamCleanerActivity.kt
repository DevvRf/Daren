package com.gamespace.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class RamCleanerActivity : AppCompatActivity() {

    private lateinit var adapter: BackgroundAppAdapter
    private lateinit var txtRamUsage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ram_cleaner)

        txtRamUsage = findViewById(R.id.txtRamUsage)
        val recycler = findViewById<RecyclerView>(R.id.recyclerBackgroundApps)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = BackgroundAppAdapter(emptyList(), packageManager) { app ->
            RamCleaner.killBackground(this, app.packageName)
            Toast.makeText(this, "${app.label} dihentikan", Toast.LENGTH_SHORT).show()
            refresh()
        }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnCleanAll).setOnClickListener {
            thread {
                val freedMb = RamCleaner.cleanAll(this)
                runOnUiThread {
                    Toast.makeText(this, "Selesai — ${freedMb} MB RAM terbebas (terukur nyata)", Toast.LENGTH_LONG).show()
                    refresh()
                }
            }
        }

        refresh()
    }

    private fun refresh() {
        val available = RamCleaner.availableMb(this)
        txtRamUsage.text = "$available MB tersedia"
        adapter.submitList(RamCleaner.listBackgroundApps(this))
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }
}
