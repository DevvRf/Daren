package com.gamespace.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
        adapter = GameListAdapter(store.loadAll()) { profile ->
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
    }

    override fun onResume() {
        super.onResume()
        adapter.submitList(store.loadAll())
    }

    private fun showAddGameDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(this).apply { hint = getString(R.string.hint_game_name) }
        val pkgInput = EditText(this).apply { hint = getString(R.string.hint_package_name) }
        container.addView(nameInput)
        container.addView(pkgInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_game_title)
            .setView(container)
            .setPositiveButton("Tambah") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val pkg = pkgInput.text.toString().trim().ifEmpty { null }
                val profile = GameProfile(
                    id = UUID.randomUUID().toString(),
                    displayName = name,
                    packageName = pkg
                )
                store.upsert(profile)
                adapter.submitList(store.loadAll())
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
