package com.gamespace.app

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BackgroundAppAdapter(
    private var items: List<RamCleaner.BackgroundApp>,
    private val packageManager: PackageManager,
    private val onStop: (RamCleaner.BackgroundApp) -> Unit
) : RecyclerView.Adapter<BackgroundAppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgAppIcon)
        val label: TextView = view.findViewById(R.id.txtAppLabel)
        val mb: TextView = view.findViewById(R.id.txtAppMb)
        val stopBtn: Button = view.findViewById(R.id.btnStopApp)
    }

    fun submitList(newItems: List<RamCleaner.BackgroundApp>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_background_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.label.text = app.label
        holder.mb.text = "${app.estimatedMb} MB"
        try {
            holder.icon.setImageDrawable(packageManager.getApplicationIcon(app.packageName))
        } catch (e: PackageManager.NameNotFoundException) {
            holder.icon.setImageResource(R.drawable.ic_logo_z)
        }
        holder.stopBtn.setOnClickListener { onStop(app) }
    }

    override fun getItemCount(): Int = items.size
}
