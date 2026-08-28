package com.kaamchor.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaamchor.data.ApiClient
import com.kaamchor.databinding.FragmentChatListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatListAdapter
    private val groups = mutableListOf<com.kaamchor.data.model.ChatGroup>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatListAdapter(groups, ApiClient.getUserId()) { group ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("group_id", group.id)
            intent.putExtra("group_name", group.name ?: "")
            intent.putExtra("is_group", group.isGroup)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
        }
        loadGroups()
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getGroups(ApiClient.getAuthHeader())
                }
                if (_binding == null || !isAdded) return@launch
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        groups.clear()
                        groups.addAll(body)
                        adapter.notifyDataSetChanged()
                        binding.tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
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
            loadGroups()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
