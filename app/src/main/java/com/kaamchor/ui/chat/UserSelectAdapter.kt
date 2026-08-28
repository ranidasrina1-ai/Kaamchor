package com.kaamchor.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.model.User
import com.kaamchor.databinding.ItemUserSelectBinding

class UserSelectAdapter(
    private val users: List<User>,
    private val selectedIds: MutableSet<Int>,
    private val onToggle: (User, Boolean) -> Unit
) : RecyclerView.Adapter<UserSelectAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUserSelectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemUserSelectBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val b = holder.binding

        b.tvName.text = user.displayName ?: user.username
        b.tvUsername.text = "@${user.username}"

        if (!user.avatarUrl.isNullOrBlank()) {
            Glide.with(b.ivAvatar).load(user.avatarUrl).circleCrop()
                .placeholder(R.drawable.bg_avatar_placeholder).into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }

        val isSelected = selectedIds.contains(user.id)
        b.checkbox.isChecked = isSelected

        b.root.setOnClickListener {
            val newState = !b.checkbox.isChecked
            b.checkbox.isChecked = newState
            onToggle(user, newState)
        }
    }

    override fun getItemCount(): Int = users.size
}
