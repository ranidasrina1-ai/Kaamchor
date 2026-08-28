package com.kaamchor.ui.post

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kaamchor.data.ApiClient
import com.kaamchor.data.model.Post
import com.kaamchor.databinding.ActivityPostDetailBinding
import com.kaamchor.ui.feed.PostsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var postId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getIntExtra("post_id", -1)
        if (postId == -1) {
            finish()
            return
        }

        loadPost()
    }

    private fun loadPost() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getPost(
                        token = ApiClient.getAuthHeader(),
                        postId = postId
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    // Display single post — reuse the adapter with one item
                    // Implementation simplified for single post view
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
