package com.rootgenie.app.ui

import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rootgenie.app.R

/**
 * Simple chat adapter — user messages right-aligned, AI messages left-aligned,
 * command output rendered in monospace.
 */
class ChatAdapter(
    private val items: List<ChatFragment.Msg>
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msg_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.text.text = m.text
        val ctx = holder.text.context
        if (m.isUser) {
            holder.text.gravity = Gravity.END
            holder.text.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_blue_dark))
            holder.text.typeface = Typeface.DEFAULT_BOLD
        } else if (m.isCode) {
            holder.text.gravity = Gravity.START
            holder.text.setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
            holder.text.typeface = Typeface.MONOSPACE
        } else {
            holder.text.gravity = Gravity.START
            holder.text.setTextColor(ContextCompat.getColor(ctx, android.R.color.primary_text_light))
            holder.text.typeface = Typeface.DEFAULT
        }
    }
}
