package com.gamespace.app

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AccessManagerActivity : AppCompatActivity() {

    private lateinit var adapter: AccessAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_manager)

        val recycler = findViewById<RecyclerView>(R.id.recyclerAccess)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AccessAdapter(AccessManager.allAccessItems(this))
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            adapter.submitList(AccessManager.allAccessItems(this))
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.submitList(AccessManager.allAccessItems(this))
    }
}
