package com.gamespace.app

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameListAdapter(
    private var items: List<GameProfile>,
    private val packageManager: PackageManager,
    private val onClick: (GameProfile) -> Unit
) : RecyclerView.Adapter<GameListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgGameIcon)
        val name: TextView = view.findViewById(R.id.txtGameName)
        val summary: TextView = view.findViewById(R.id.txtGameSummary)
    }

    fun submitList(newItems: List<GameProfile>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = items[position]
        holder.name.text = profile.displayName
        val presetLabel = profile.colorPreset.replaceFirstChar { it.uppercase() }
        holder.summary.text = "$presetLabel ${profile.downscalePercent}%"

        val pkg = profile.packageName
        val realIcon = if (pkg != null) {
            try { packageManager.getApplicationIcon(pkg) } catch (e: PackageManager.NameNotFoundException) { null }
        } else null

        if (realIcon != null) {
            holder.icon.setImageDrawable(realIcon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_logo_z)
        }

        holder.itemView.setOnClickListener { onClick(profile) }
    }

    override fun getItemCount(): Int = items.size
}
