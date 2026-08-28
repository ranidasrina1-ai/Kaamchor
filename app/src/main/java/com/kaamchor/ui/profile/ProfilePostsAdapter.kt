package com.kaamchor.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kaamchor.data.model.Post
import com.kaamchor.databinding.ItemProfilePostBinding

class ProfilePostsAdapter(
    private val posts: List<Post>
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProfilePostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProfilePostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.binding.ivPhoto)
            .load(post.photoUrl)
            .centerCrop()
            .into(holder.binding.ivPhoto)
    }

    override fun getItemCount(): Int = posts.size
}
