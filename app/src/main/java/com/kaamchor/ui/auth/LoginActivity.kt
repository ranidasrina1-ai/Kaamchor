package com.kaamchor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kaamchor.data.ApiClient
import com.kaamchor.data.LoginRequest
import com.kaamchor.data.SignupRequest
import com.kaamchor.databinding.ActivityLoginBinding
import com.kaamchor.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ApiClient.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        updateMode()

        binding.btnAuth.setOnClickListener {
            if (isLoginMode) doLogin() else doSignup()
        }

        binding.tvToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateMode()
        }
    }

    private fun updateMode() {
        if (isLoginMode) {
            binding.btnAuth.text = getString(com.kaamchor.R.string.btn_login)
            binding.tvToggleMode.text = getString(com.kaamchor.R.string.no_account)
            binding.tilEmail.visibility = View.GONE
            binding.tilDisplayName.visibility = View.GONE
        } else {
            binding.btnAuth.text = getString(com.kaamchor.R.string.btn_signup)
            binding.tvToggleMode.text = getString(com.kaamchor.R.string.have_account)
            binding.tilEmail.visibility = View.VISIBLE
            binding.tilDisplayName.visibility = View.VISIBLE
        }
    }

    private fun doLogin() {
        val identifier = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().login(LoginRequest(identifier, password))
                }

                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        ApiClient.saveToken(authResponse.token)
                        ApiClient.saveUserId(authResponse.user.id)
                        goToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = try {
                        val err = com.google.gson.Gson().fromJson(
                            response.errorBody()?.charStream(),
                            com.kaamchor.data.model.ErrorResponse::class.java
                        )
                        err?.error ?: "Login failed (${response.code()})"
                    } catch (_: Exception) {
                        "Login failed (${response.code()})"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!isFinishing) {
                    setLoading(false)
                }
            }
        }
    }

    private fun doSignup() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val displayName = binding.etDisplayName.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().signup(
                        SignupRequest(
                            username = username,
                            email = email,
                            password = password,
                            display_name = displayName.ifEmpty { null }
                        )
                    )
                }

                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        ApiClient.saveToken(authResponse.token)
                        ApiClient.saveUserId(authResponse.user.id)
                        goToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "Signup failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = try {
                        val err = com.google.gson.Gson().fromJson(
                            response.errorBody()?.charStream(),
                            com.kaamchor.data.model.ErrorResponse::class.java
                        )
                        err?.error ?: "Signup failed (${response.code()})"
                    } catch (_: Exception) {
                        "Signup failed (${response.code()})"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!isFinishing) {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnAuth.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
