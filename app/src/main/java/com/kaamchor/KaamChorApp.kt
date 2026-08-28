package com.kaamchor

import android.app.Application
import com.kaamchor.data.ApiClient
import com.kaamchor.service.NtfyMessagingService

class KaamChorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        NtfyMessagingService.createNotificationChannel(this)
    }
}
