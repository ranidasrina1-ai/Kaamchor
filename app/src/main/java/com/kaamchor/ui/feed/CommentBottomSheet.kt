package com.kaamchor.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.data.CommentRequest
import com.kaamchor.data.model.Comment
import com.kaamchor.databinding.BottomSheetCommentsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()
    private var postId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getInt(ARG_POST_ID, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CommentsAdapter(comments, ApiClient.getUserId())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text)
            }
        }

        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getComments(
                        token = ApiClient.getAuthHeader(),
                        postId = postId
                    )
                }

                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        comments.clear()
                        comments.addAll(body)
                        adapter.notifyDataSetChanged()
                        binding.tvEmpty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Check your connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun postComment(text: String) {
        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().addComment(
                        token = ApiClient.getAuthHeader(),
                        postId = postId,
                        body = CommentRequest(text)
                    )
                }

                if (_binding == null) return@launch

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        comments.add(body)
                        adapter.notifyItemInserted(comments.size - 1)
                        binding.recyclerView.scrollToPosition(comments.size - 1)
                        binding.etComment.text?.clear()
                        binding.tvEmpty.visibility = View.GONE

                        parentFragmentManager.setFragmentResult("comment_updated", Bundle.EMPTY)
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to comment", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (_binding != null && isAdded) {
                    Toast.makeText(requireContext(), "Check your connection", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (_binding != null) {
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        fun newInstance(postId: Int): CommentBottomSheet {
            return CommentBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POST_ID, postId)
                }
            }
        }
    }
}
