package com.kaamchor

import android.app.Application
import android.os.Build
import com.kaamchor.data.ApiClient
import com.kaamchor.service.KaamchorNotificationService
import com.kaamchor.service.NtfyMessagingService

class KaamChorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        NtfyMessagingService.createNotificationChannel(this)

        // Start the SSE-based push notification service if the user is logged in
        if (ApiClient.isLoggedIn()) {
            KaamchorNotificationService.start(this)
        }
    }
}
