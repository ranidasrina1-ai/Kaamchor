package com.kaamchor.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kaamchor.data.ApiClient
import com.kaamchor.data.model.Post
import com.kaamchor.databinding.FragmentFeedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostsAdapter
    private val posts = mutableListOf<Post>()
    private var cursor: Int? = null
    private var isLoading = false
    private var hasMore = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostsAdapter(
            posts = posts,
            currentUserId = ApiClient.getUserId(),
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post -> openComments(post) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val visibleItemCount = lm.childCount
                val totalItemCount = lm.itemCount
                val firstVisibleItemPosition = lm.findFirstVisibleItemPosition()

                if (!isLoading && hasMore) {
                    if (visibleItemCount + firstVisibleItemPosition + 3 >= totalItemCount) {
                        loadMore()
                    }
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            refresh()
        }

        loadFirstPage()
    }

    private fun loadFirstPage() {
        posts.clear()
        cursor = null
        hasMore = true
        adapter.notifyDataSetChanged()
        loadMore()
    }

    private fun refresh() {
        posts.clear()
        cursor = null
        hasMore = true
        adapter.notifyDataSetChanged()
        loadMore()
    }

    private fun loadMore() {
        if (isLoading) return
        isLoading = true

        if (posts.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getFeed(
                        token = ApiClient.getAuthHeader(),
                        cursor = cursor,
                        limit = 20
                    )
                }

                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        val startPos = posts.size
                        posts.addAll(data.posts)
                        adapter.notifyItemRangeInserted(startPos, data.posts.size)

                        cursor = data.nextCursor
                        hasMore = data.hasMore

                        if (posts.isEmpty()) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            binding.tvEmpty.visibility = View.GONE
                        }
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to load feed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Check your connection", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun toggleLike(post: Post) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().toggleLike(
                        token = ApiClient.getAuthHeader(),
                        postId = post.id
                    )
                }

                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val likeResp = response.body()
                    if (likeResp != null) {
                        val index = posts.indexOfFirst { it.id == post.id }
                        if (index >= 0) {
                            val updated = posts[index].copy(
                                likedByMe = likeResp.liked,
                                likeCount = likeResp.likeCount
                            )
                            posts[index] = updated
                            adapter.notifyItemChanged(index)
                        }
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Like failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openComments(post: Post) {
        val dialog = CommentBottomSheet.newInstance(post.id)
        dialog.show(parentFragmentManager, "CommentBottomSheet")

        parentFragmentManager.setFragmentResultListener("comment_updated", viewLifecycleOwner) { _, _ ->
            refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
