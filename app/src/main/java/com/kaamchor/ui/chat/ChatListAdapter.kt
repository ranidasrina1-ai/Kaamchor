package com.kaamchor.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.model.ChatGroup
import com.kaamchor.databinding.ItemChatListBinding

class ChatListAdapter(
    private val groups: MutableList<ChatGroup>,
    private val currentUserId: Int,
    private val onClick: (ChatGroup) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChatListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemChatListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        val b = holder.binding

        // Chat title: group name or other person's name (for 1-on-1)
        if (group.isGroup) {
            b.tvName.text = group.name ?: "Group"
        } else {
            val other = group.members.firstOrNull { it.id != currentUserId }
            b.tvName.text = other?.displayName ?: other?.username ?: "Unknown"
        }

        // Last message
        val lastMsg = group.lastMessage
        if (lastMsg != null) {
            val senderPrefix = if (lastMsg.senderId == currentUserId) "You: " else ""
            val content = when {
                !lastMsg.text.isNullOrBlank() -> lastMsg.text
                lastMsg.mediaType == "photo" -> "📷 Photo"
                lastMsg.mediaType == "video" -> "🎥 Video"
                else -> ""
            }
            b.tvLastMessage.text = senderPrefix + content
            b.tvLastMessage.visibility = android.view.View.VISIBLE
        } else {
            b.tvLastMessage.text = "No messages yet"
        }

        // Unread badge
        if (group.unreadCount > 0) {
            b.tvUnread.visibility = android.view.View.VISIBLE
            b.tvUnread.text = if (group.unreadCount > 99) "99+" else group.unreadCount.toString()
        } else {
            b.tvUnread.visibility = android.view.View.GONE
        }

        // Avatar
        val other = group.members.firstOrNull { it.id != currentUserId }
        val avatarUrl = if (group.isGroup) null else other?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(b.ivAvatar).load(avatarUrl).circleCrop()
                .placeholder(R.drawable.bg_avatar_placeholder).into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }

        b.root.setOnClickListener { onClick(group) }
    }

    override fun getItemCount(): Int = groups.size
}
