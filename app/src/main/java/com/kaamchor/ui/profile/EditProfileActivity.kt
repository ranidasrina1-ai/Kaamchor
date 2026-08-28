package com.kaamchor.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.data.EditProfileJsonRequest
import com.kaamchor.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedAvatarUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            Glide.with(this).load(uri).circleCrop().into(binding.ivAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.ivAvatar.setOnClickListener { pickImage.launch("image/*") }

        loadCurrentProfile()

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadCurrentProfile() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().getMyProfile(ApiClient.getAuthHeader())
                }
                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        binding.etDisplayName.setText(user.displayName ?: user.username)
                        binding.etBio.setText(user.bio ?: "")
                        if (!user.avatarUrl.isNullOrBlank()) {
                            Glide.with(this@EditProfileActivity)
                                .load(user.avatarUrl)
                                .placeholder(R.drawable.bg_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivAvatar)
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfile() {
        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val displayName = binding.etDisplayName.text.toString().trim()
                val bio = binding.etBio.text.toString().trim()

                if (selectedAvatarUri != null) {
                    val file = withContext(Dispatchers.IO) { uriToFile(selectedAvatarUri!!) }
                    if (file == null) {
                        if (!isFinishing) {
                            Toast.makeText(this@EditProfileActivity, "Could not read image", Toast.LENGTH_SHORT).show()
                        }
                        binding.btnSave.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        return@launch
                    }

                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, reqFile)
                    val displayNameBody = displayName.toRequestBody("text/plain".toMediaTypeOrNull())
                    val bioBody = bio.toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = withContext(Dispatchers.IO) {
                        ApiClient.getApi().editProfile(
                            token = ApiClient.getAuthHeader(),
                            avatar = avatarPart,
                            displayName = displayNameBody,
                            bio = bioBody
                        )
                    }

                    if (isFinishing) return@launch
                    if (response.isSuccessful) {
                        if (!isFinishing) {
                            Toast.makeText(this@EditProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    } else {
                        if (!isFinishing) {
                            Toast.makeText(this@EditProfileActivity, "Update failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val editRequest = EditProfileJsonRequest(
                        display_name = displayName,
                        bio = bio
                    )
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.getApi().editProfileJson(
                            token = ApiClient.getAuthHeader(),
                            body = editRequest
                        )
                    }

                    if (isFinishing) return@launch
                    if (response.isSuccessful) {
                        if (!isFinishing) {
                            Toast.makeText(this@EditProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    } else {
                        if (!isFinishing) {
                            Toast.makeText(this@EditProfileActivity, "Update failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!isFinishing) {
                    binding.btnSave.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("avatar_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
