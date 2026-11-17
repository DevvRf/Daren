package com.gamespace.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccessAdapter(private var items: List<AccessManager.AccessItem>) :
    RecyclerView.Adapter<AccessAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtAccessName)
        val status: TextView = view.findViewById(R.id.txtAccessStatus)
        val reason: TextView = view.findViewById(R.id.txtAccessReason)
    }

    fun submitList(newItems: List<AccessManager.AccessItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_access, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.status.text = item.status.label
        holder.status.setTextColor(colorFor(item.status))
        holder.reason.text = item.reason
    }

    private fun colorFor(status: CapabilityStatus): Int = when (status) {
        CapabilityStatus.SUPPORTED -> Color.parseColor("#5CF2C0")
        CapabilityStatus.PARTIALLY_SUPPORTED -> Color.parseColor("#C8F25C")
        CapabilityStatus.REQUIRES_ROOT, CapabilityStatus.REQUIRES_SHIZUKU -> Color.parseColor("#3AA7FF")
        CapabilityStatus.PERMISSION_REQUIRED -> Color.parseColor("#F2C85C")
        CapabilityStatus.UNSUPPORTED -> Color.parseColor("#F25C5C")
    }

    override fun getItemCount(): Int = items.size
}
