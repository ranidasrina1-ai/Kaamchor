package com.kaamchor.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.data.model.Message
import com.kaamchor.databinding.ItemMessageMineBinding
import com.kaamchor.databinding.ItemMessageTheirsBinding

class MessagesAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MINE = 1
        private const val TYPE_THEIRS = 2
    }

    inner class MineViewHolder(val binding: ItemMessageMineBinding) : RecyclerView.ViewHolder(binding.root)
    inner class TheirsViewHolder(val binding: ItemMessageTheirsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) TYPE_MINE else TYPE_THEIRS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_MINE) {
            MineViewHolder(
                ItemMessageMineBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else {
            TheirsViewHolder(
                ItemMessageTheirsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]

        when (holder) {
            is MineViewHolder -> bindMine(holder.binding, msg)
            is TheirsViewHolder -> bindTheirs(holder.binding, msg)
        }
    }

    private fun bindMine(b: ItemMessageMineBinding, msg: Message) {
        // Text
        if (!msg.text.isNullOrBlank()) {
            b.tvText.text = msg.text
            b.tvText.visibility = View.VISIBLE
        } else {
            b.tvText.visibility = View.GONE
        }

        // Media
        if (!msg.mediaUrl.isNullOrBlank()) {
            b.ivMedia.visibility = View.VISIBLE
            Glide.with(b.ivMedia).load(msg.mediaUrl)
                .placeholder(android.R.color.darker_gray)
                .into(b.ivMedia)
        } else {
            b.ivMedia.visibility = View.GONE
        }

        b.tvTime.text = formatTime(msg.createdAt)
    }

    private fun bindTheirs(b: ItemMessageTheirsBinding, msg: Message) {
        val sender = msg.sender
        b.tvSender.text = sender?.displayName ?: sender?.username ?: ""
        b.tvSender.visibility = View.VISIBLE

        // Text
        if (!msg.text.isNullOrBlank()) {
            b.tvText.text = msg.text
            b.tvText.visibility = View.VISIBLE
        } else {
            b.tvText.visibility = View.GONE
        }

        // Media
        if (!msg.mediaUrl.isNullOrBlank()) {
            b.ivMedia.visibility = View.VISIBLE
            Glide.with(b.ivMedia).load(msg.mediaUrl)
                .placeholder(android.R.color.darker_gray)
                .into(b.ivMedia)
        } else {
            b.ivMedia.visibility = View.GONE
        }

        b.tvTime.text = formatTime(msg.createdAt)
    }

    override fun getItemCount(): Int = messages.size

    private fun formatTime(isoTime: String?): String {
        if (isoTime.isNullOrBlank()) return ""
        return try {
            val cleaned = isoTime.replace("Z", "+00:00")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val time = sdf.parse(cleaned)
            if (time != null) {
                val outFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                outFormat.timeZone = java.util.TimeZone.getDefault()
                outFormat.format(time)
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
