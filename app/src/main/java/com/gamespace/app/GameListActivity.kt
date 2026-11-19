package com.gamespace.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class GameListActivity : AppCompatActivity() {

    private lateinit var store: GameProfileStore
    private lateinit var adapter: GameListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)

        store = GameProfileStore(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerGames)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = GameListAdapter(store.loadAll(), packageManager) { profile ->
            val intent = Intent(this, GameCustomizeActivity::class.java)
            intent.putExtra(GameCustomizeActivity.EXTRA_GAME_ID, profile.id)
            startActivity(intent)
        }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnOpenSettings).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.btnGameLab).setOnClickListener {
            startActivity(Intent(this, TouchTestActivity::class.java))
        }

        findViewById<Button>(R.id.btnDeviceInfo).setOnClickListener {
            startActivity(Intent(this, DeviceInfoActivity::class.java))
        }
        findViewById<Button>(R.id.btnRuntimeCompiler).setOnClickListener {
            startActivity(Intent(this, RuntimeCompilerActivity::class.java))
        }
        findViewById<Button>(R.id.btnDisplayController).setOnClickListener {
            startActivity(Intent(this, DisplayControllerActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddGame).setOnClickListener {
            showAddGameDialog()
        }

        ensureMonitorServiceRunning()
    }

    override fun onResume() {
        super.onResume()
        adapter.submitList(store.loadAll())
    }

    private fun ensureMonitorServiceRunning() {
        val intent = Intent(this, GameMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun showAddGameDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(this).apply { hint = getString(R.string.hint_game_name) }
        val pkgInput = EditText(this).apply { hint = "Package name (wajib)" }
        container.addView(nameInput)
        container.addView(pkgInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_game_title)
            .setView(container)
            .setPositiveButton("Tambah") { _, _ ->
                val name = nameInput.text.toString().trim()
                val pkg = pkgInput.text.toString().trim()

                if (name.isEmpty() || pkg.isEmpty()) {
                    Toast.makeText(this, "Nama dan package name wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val installedAppLabel = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                if (installedAppLabel == null) {
                    Toast.makeText(
                        this,
                        "Package '$pkg' tidak ditemukan. Pastikan nama benar dan app sudah terinstall.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                val profile = GameProfile(
                    id = UUID.randomUUID().toString(),
                    displayName = name,
                    packageName = pkg
                )
                store.upsert(profile)
                adapter.submitList(store.loadAll())
                Toast.makeText(this, "Ditambahkan: $installedAppLabel terdeteksi", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
