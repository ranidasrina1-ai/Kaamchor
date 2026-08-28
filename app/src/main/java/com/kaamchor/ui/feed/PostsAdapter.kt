package com.kaamchor.ui.feed

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.model.Post
import com.kaamchor.databinding.ItemPostBinding

class PostsAdapter(
    private val posts: MutableList<Post>,
    private val currentUserId: Int,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val b = holder.binding

        val author = post.author
        val username = author?.displayName ?: author?.username ?: "unknown"
        b.tvUsername.text = username
        b.tvCaptionUser.text = username

        if (author != null && !author.avatarUrl.isNullOrBlank()) {
            Glide.with(b.ivAvatar)
                .load(author.avatarUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .circleCrop()
                .into(b.ivAvatar)
        } else {
            b.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }

        Glide.with(b.ivPhoto)
            .load(post.photoUrl)
            .placeholder(android.R.color.darker_gray)
            .override(1080, 810)
            .centerCrop()
            .into(b.ivPhoto)

        b.tvCaption.text = post.caption ?: ""
        b.tvCaption.visibility = if (post.caption.isNullOrBlank()) View.GONE else View.VISIBLE
        b.tvCaptionUser.visibility = b.tvCaption.visibility

        // Like count next to heart icon
        b.tvLikes.text = formatCount(post.likeCount)

        val likeColor = if (post.likedByMe) R.color.like_red else R.color.text_primary
        b.btnLike.setColorFilter(androidx.core.content.ContextCompat.getColor(b.root.context, likeColor))

        // Comment count next to comment icon
        b.tvCommentCount.text = formatCount(post.commentCount)

        b.tvViewComments.text = formatCommentText(post.commentCount)
        b.tvViewComments.visibility = View.VISIBLE

        b.tvTime.text = formatTimeAgo(post.createdAt)

        b.btnLike.setOnClickListener { onLikeClick(post) }
        b.btnComment.setOnClickListener { onCommentClick(post) }
        b.tvViewComments.setOnClickListener { onCommentClick(post) }
    }

    override fun getItemCount(): Int = posts.size

    private fun formatCount(n: Int): String {
        return when {
            n >= 1000 -> String.format("%.1fK", n / 1000.0)
            else -> n.toString()
        }
    }

    private fun formatTimeAgo(isoTime: String?): String {
        if (isoTime.isNullOrBlank()) return ""
        return try {
            val cleaned = isoTime.replace("Z", "+00:00")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val time = sdf.parse(cleaned)
            if (time != null) {
                DateUtils.getRelativeTimeSpanString(
                    time.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatCommentText(count: Int): String {
        return when (count) {
            0 -> "Add a comment..."
            1 -> "View 1 comment"
            else -> "View all $count comments"
        }
    }
}
