package com.kaamchor.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaamchor.data.ApiClient
import com.kaamchor.data.SocketClient
import com.kaamchor.data.TextMessageRequest
import com.kaamchor.data.model.Message
import com.kaamchor.databinding.ActivityChatBinding
import com.kaamchor.ui.call.CallActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter

    private val messages = mutableListOf<Message>()
    private var groupId: Int = -1
    private var isGroup: Boolean = false
    private var groupName: String = ""
    private var cursor: Int? = null
    private var isLoading = false

    private val handler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null
    private var isCurrentlyTyping = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sendMediaMessage(uri, "photo")
        }
    }

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            sendMediaMessage(uri, "video")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getIntExtra("group_id", -1)
        groupName = intent.getStringExtra("group_name") ?: ""
        isGroup = intent.getBooleanExtra("is_group", false)

        if (groupId == -1) {
            finish()
            return
        }

        setupUI()
        setupSocketListeners()
        loadMessages()

        SocketClient.emitMarkDelivered(groupId)
    }

    private fun setupUI() {
        binding.tvTitle.text = groupName.ifEmpty { if (isGroup) "Group Chat" else "Chat" }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCall.setOnClickListener {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("group_id", groupId)
            intent.putExtra("is_group", isGroup)
            intent.putExtra("is_caller", true)
            startActivity(intent)
        }

        adapter = MessagesAdapter(messages, ApiClient.getUserId())
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        binding.recyclerView.layoutManager = lm
        binding.recyclerView.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendTextMessage(text)
            }
        }

        binding.btnPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnVideo.setOnClickListener {
            pickVideo.launch("video/*")
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    if (isCurrentlyTyping) {
                        isCurrentlyTyping = false
                        SocketClient.emitTyping(groupId, false)
                    }
                    return
                }

                if (!isCurrentlyTyping) {
                    isCurrentlyTyping = true
                    SocketClient.emitTyping(groupId, true)
                }

                typingRunnable?.let { handler.removeCallbacks(it) }
                typingRunnable = Runnable {
                    isCurrentlyTyping = false
                    SocketClient.emitTyping(groupId, false)
                }
                handler.postDelayed(typingRunnable!!, 2000)
            }
        })
    }

    private fun setupSocketListeners() {
        SocketClient.onNewMessage { msg ->
            runOnUiThread {
                if (msg.groupId == groupId) {
                    messages.add(msg)
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.recyclerView.scrollToPosition(messages.size - 1)
                    SocketClient.emitMarkRead(groupId, listOf(msg.id))
                }
            }
        }

        SocketClient.onTyping { gid, userId, username, typing ->
            runOnUiThread {
                if (gid == groupId && userId != ApiClient.getUserId()) {
                    if (typing) {
                        binding.tvTyping.text = "$username is typing…"
                        binding.tvTyping.visibility = View.VISIBLE
                    } else {
                        binding.tvTyping.visibility = View.GONE
                    }
                }
            }
        }

        SocketClient.onMessagesRead { gid, messageIds, readBy ->
            runOnUiThread {
                if (gid == groupId) {
                    for (mid in messageIds) {
                        val index = messages.indexOfFirst { it.id == mid }
                        if (index >= 0) {
                            adapter.notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }

    private fun loadMessages() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getMessages(
                        token = ApiClient.getAuthHeader(),
                        groupId = groupId,
                        cursor = null,
                        limit = 30
                    )
                }

                if (isFinishing) return@launch

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        messages.clear()
                        messages.addAll(data.messages)
                        cursor = data.nextCursor
                        adapter.notifyDataSetChanged()
                        if (messages.isNotEmpty()) {
                            binding.recyclerView.scrollToPosition(messages.size - 1)
                        }
                        binding.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE

                        SocketClient.emitMarkRead(groupId, messages.filter { it.senderId != ApiClient.getUserId() }.map { it.id })
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Could not load messages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@ChatActivity, "Check your connection", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
                if (!isFinishing) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun sendTextMessage(text: String) {
        binding.etMessage.text?.clear()

        val tempMsg = Message(
            id = System.currentTimeMillis().toInt().unaryMinus(),
            groupId = groupId,
            senderId = ApiClient.getUserId(),
            text = text,
            mediaType = "text",
            createdAt = null,
            sender = null
        )
        messages.add(tempMsg)
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerView.scrollToPosition(messages.size - 1)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().sendMessageText(
                        token = ApiClient.getAuthHeader(),
                        groupId = groupId,
                        body = TextMessageRequest(text)
                    )
                }

                if (isFinishing) return@launch

                if (response.isSuccessful) {
                    val realMsg = response.body()
                    if (realMsg != null) {
                        val index = messages.indexOfFirst { it.id == tempMsg.id }
                        if (index >= 0) {
                            messages[index] = realMsg
                            adapter.notifyItemChanged(index)
                        }
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send", Toast.LENGTH_SHORT).show()
                    val index = messages.indexOfFirst { it.id == tempMsg.id }
                    if (index >= 0) {
                        messages.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@ChatActivity, "Check your connection", Toast.LENGTH_SHORT).show()
                    val index = messages.indexOfFirst { it.id == tempMsg.id }
                    if (index >= 0) {
                        messages.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
            }
        }
    }

    private fun sendMediaMessage(uri: Uri, mediaType: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { uriToFile(uri) }
                if (file == null) {
                    Toast.makeText(this@ChatActivity, "Could not read file", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val mimeType = if (mediaType == "photo") "image/*" else "video/*"
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, reqFile)
                val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaTypeOrNull())
                val textBody = "".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().sendMessageMedia(
                        token = ApiClient.getAuthHeader(),
                        groupId = groupId,
                        file = filePart,
                        text = textBody,
                        mediaType = mediaTypeBody,
                        duration = null
                    )
                }

                if (isFinishing) return@launch

                if (response.isSuccessful) {
                    val msg = response.body()
                    if (msg != null) {
                        messages.add(msg)
                        adapter.notifyItemInserted(messages.size - 1)
                        binding.recyclerView.scrollToPosition(messages.size - 1)
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send media", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@ChatActivity, "Check your connection", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!isFinishing) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val ext = if (uri.toString().contains("video")) ".mp4" else ".jpg"
            val tempFile = File.createTempFile("chat_media_", ext, cacheDir)
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isCurrentlyTyping) {
            SocketClient.emitTyping(groupId, false)
        }
        SocketClient.removeAllListeners()
    }
}
