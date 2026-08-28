package com.kaamchor.ui.post

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.kaamchor.data.ApiClient
import com.kaamchor.databinding.ActivityCreatePostBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private var selectedImageUri: android.net.Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).into(binding.ivPreview)
            binding.ivPreview.visibility = View.VISIBLE
            binding.btnSelectPhoto.text = "Change Photo"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSelectPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnPost.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadPost()
        }
    }

    private fun uploadPost() {
        binding.btnPost.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    uriToFile(selectedImageUri!!)
                }

                if (file == null) {
                    Toast.makeText(this@CreatePostActivity, "Could not read file", Toast.LENGTH_SHORT).show()
                    binding.btnPost.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, reqFile)
                val caption = binding.etCaption.text.toString().trim()
                val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().createPost(
                        token = ApiClient.getAuthHeader(),
                        photo = photoPart,
                        caption = captionBody
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@CreatePostActivity, "Posted!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@CreatePostActivity, "Upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.net.ConnectException) {
                Toast.makeText(this@CreatePostActivity, "Cannot connect to server. Is it running?", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@CreatePostActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnPost.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun uriToFile(uri: android.net.Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
