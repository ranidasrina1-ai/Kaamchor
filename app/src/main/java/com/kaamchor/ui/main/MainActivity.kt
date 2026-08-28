package com.kaamchor.ui.main

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.data.SocketClient
import com.kaamchor.databinding.ActivityMainBinding
import com.kaamchor.ui.chat.ChatListFragment
import com.kaamchor.ui.feed.FeedFragment
import com.kaamchor.ui.post.CreatePostActivity
import com.kaamchor.ui.profile.ProfileFragment
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ApiClient.isLoggedIn()) {
            redirectToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SocketClient.connect(
            token = ApiClient.getToken() ?: "",
            serverUrl = ApiClient.getServerUrl()
        )

        // Start the SSE notification service
        try {
            com.kaamchor.service.KaamchorNotificationService.start(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Could not start notification service: ${e.message}")
        }

        setupBottomNav()
        applyZeyadaFontToBottomNav()

        if (savedInstanceState == null) {
            loadFragment(FeedFragment())
        }
    }

    private fun applyZeyadaFontToBottomNav() {
        try {
            val zeyadaTypeface = resources.getFont(R.font.zeyada)
            val menu = binding.bottomNav.menu
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                val title = menuItem.title
                if (title != null) {
                    val spannable = SpannableStringBuilder(title)
                    val typefaceSpan = CustomTypefaceSpan(zeyadaTypeface)
                    spannable.setSpan(typefaceSpan, 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    menuItem.title = spannable
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(FeedFragment()); true }
                R.id.nav_upload -> {
                    startActivity(Intent(this, CreatePostActivity::class.java))
                    false
                }
                R.id.nav_chat -> { loadFragment(ChatListFragment()); true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, com.kaamchor.ui.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

class CustomTypefaceSpan(private val typeface: Typeface) : android.text.style.TypefaceSpan("") {
    override fun updateDrawState(ds: TextPaint) {
        ds.typeface = typeface
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }
}
