package com.kaamchor.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.model.Comment
import com.kaamchor.databinding.ItemCommentBinding

class CommentsAdapter(
    private val comments: List<Comment>,
    private val currentUserId: Int
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(
            ItemCommentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val b = holder.binding

        val author = comment.author
        b.tvUsername.text = author?.displayName ?: author?.username ?: "unknown"
        b.tvComment.text = comment.text

        if (author != null && !author.avatarUrl.isNullOrBlank()) {
            Glide.with(b.ivAvatar)
                .load(author.avatarUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .circleCrop()
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }
    }

    override fun getItemCount(): Int = comments.size
}
