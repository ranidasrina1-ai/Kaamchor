package com.kaamchor.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaamchor.data.ApiClient
import com.kaamchor.data.CreateGroupRequest
import com.kaamchor.data.model.User
import com.kaamchor.databinding.ActivityCreateGroupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var adapter: UserSelectAdapter
    private val users = mutableListOf<User>()
    private val selectedIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCreate.setOnClickListener { createGroup() }

        adapter = UserSelectAdapter(users, selectedIds) { user, isSelected ->
            if (isSelected) {
                selectedIds.add(user.id)
            } else {
                selectedIds.remove(user.id)
            }
            updateCreateButton()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getUsers(ApiClient.getAuthHeader())
                }

                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        users.clear()
                        users.addAll(body.filter { it.id != ApiClient.getUserId() })
                        adapter.notifyDataSetChanged()
                        binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@CreateGroupActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCreateButton() {
        binding.btnCreate.isEnabled = selectedIds.isNotEmpty()
    }

    private fun createGroup() {
        val name = binding.etGroupName.text.toString().trim()
        val isGroup = selectedIds.size > 1

        if (isGroup && name.isEmpty()) {
            Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCreate.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().createGroup(
                        token = ApiClient.getAuthHeader(),
                        body = CreateGroupRequest(
                            name = name.ifEmpty { null },
                            member_ids = selectedIds.toList()
                        )
                    )
                }

                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateGroupActivity, "Chat created!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateGroupActivity, "Failed to create chat", Toast.LENGTH_SHORT).show()
                    binding.btnCreate.isEnabled = true
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@CreateGroupActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                binding.btnCreate.isEnabled = true
            }
        }
    }
}
