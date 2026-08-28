package com.kaamchor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class KaamchorNotificationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: OkHttpClient? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildForegroundNotification())

        scope.launch {
            client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            while (isActive) {
                try {
                    connectToSSE()
                } catch (e: Exception) {
                    Log.e(TAG, "SSE connection failed: ${e.message}")
                }
                delay(5000)
            }
        }

        return START_STICKY
    }

    private fun connectToSSE() {
        val token = ApiClient.getToken() ?: return
        val serverUrl = ApiClient.getServerUrl().trimEnd('/')
        val url = "$serverUrl/api/notifications/stream"
        Log.d(TAG, "Connecting to SSE: $url")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val response = client!!.newCall(request).execute()

        if (!response.isSuccessful) {
            Log.e(TAG, "SSE returned ${response.code}")
            response.close()
            return
        }

        val source = response.body?.source() ?: return

        while (scope.isActive) {
            val line = source.readUtf8Line() ?: break
            if (line.isBlank()) continue
            if (line.startsWith(":")) continue  // keepalive comment

            if (line.startsWith("data:")) {
                val data = line.removePrefix("data:").trim()
                try {
                    val json = JSONObject(data)
                    val title = json.optString("title", "Kaam Chor")
                    val message = json.optString("message", "")
                    val type = json.optString("type", "message")
                    Log.d(TAG, "Got notification: title=$title, msg=$message, type=$type")
                    showNotification(title, message, type)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: ${e.message}")
                }
            }
        }

        source.close()
        response.close()
    }

    private fun showNotification(title: String, message: String, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColorized(true)
            .setColor(0xFF6C5CE7.toInt())
            .setVibrate(longArrayOf(0, 250, 200, 250))

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FG_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle("Kaam Chor")
            .setContentText("Listening for notifications...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kaam Chor Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat messages, likes, and comments"
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val fgChannel = NotificationChannel(
                FG_CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(fgChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "KaamchorNotifService"
        private const val CHANNEL_ID = "kaamchor_notifications"
        private const val FG_CHANNEL_ID = "kaamchor_foreground"
        private const val NOTIF_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, KaamchorNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KaamchorNotificationService::class.java))
        }
    }
}
