package com.kaamchor.data

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "kaamchor_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_SOCKET_URL = "socket_url"
    private const val PREFS_VERSION = "v7"

    const val DEFAULT_SERVER_URL = "https://kaamchorz.dpdns.org"
    const val DEFAULT_SOCKET_URL = "https://kaamchorz.dpdns.org"

    private lateinit var prefs: SharedPreferences
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val storedVersion = prefs.getString("prefs_version", "") ?: ""
        if (storedVersion != PREFS_VERSION) {
            prefs.edit()
                .putString(KEY_SERVER_URL, DEFAULT_SERVER_URL)
                .putString(KEY_SOCKET_URL, DEFAULT_SOCKET_URL)
                .putString("prefs_version", PREFS_VERSION)
                .apply()
        }

        rebuildClient()
    }

    private fun rebuildClient() {
        val baseUrl = getServerUrl().trimEnd('/') + "/api/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(ApiService::class.java)
    }

    fun getApi(): ApiService {
        return apiService ?: throw IllegalStateException("ApiClient not initialized. Call init() first.")
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getAuthHeader(): String {
        val token = getToken() ?: return ""
        return "Bearer $token"
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).remove(KEY_USERNAME).apply()
    }

    fun saveUserId(id: Int) {
        prefs.edit().putInt(KEY_USER_ID, id).apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
        rebuildClient()
    }

    fun getSocketUrl(): String {
        return prefs.getString(KEY_SOCKET_URL, DEFAULT_SOCKET_URL) ?: DEFAULT_SOCKET_URL
    }

    fun setSocketUrl(url: String) {
        prefs.edit().putString(KEY_SOCKET_URL, url).apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }
}
