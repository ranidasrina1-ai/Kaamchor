package com.kaamchor.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.data.model.Post
import com.kaamchor.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfilePostsAdapter
    private val posts = mutableListOf<Post>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ProfilePostsAdapter(posts)
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            com.kaamchor.service.KaamchorNotificationService.stop(requireContext())
            ApiClient.clearToken()
            val intent = Intent(requireContext(), com.kaamchor.ui.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        loadProfile()
        loadPosts()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getMyProfile(ApiClient.getAuthHeader())
                }
                if (_binding == null || !isAdded) return@launch
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        binding.tvUsername.text = user.username
                        binding.tvDisplayName.text = user.displayName ?: user.username
                        binding.tvBio.text = user.bio ?: ""
                        binding.tvBio.visibility = if (user.bio.isNullOrBlank()) View.GONE else View.VISIBLE
                        if (!user.avatarUrl.isNullOrBlank()) {
                            Glide.with(binding.ivAvatar)
                                .load(user.avatarUrl)
                                .placeholder(R.drawable.bg_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivAvatar)
                        }
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Check your connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getUserPosts(
                        token = ApiClient.getAuthHeader(),
                        userId = ApiClient.getUserId(),
                        limit = 50
                    )
                }
                if (_binding == null || !isAdded) return@launch
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        posts.clear()
                        posts.addAll(body.posts)
                        adapter.notifyDataSetChanged()
                        binding.tvPostCount.text = posts.size.toString()
                        binding.tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Check your connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadProfile()
            loadPosts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
