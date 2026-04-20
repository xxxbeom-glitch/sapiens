package com.sapiens.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SapiensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureNewsNotificationChannel()
    }

    private fun ensureNewsNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            "sapiens_news",
            "Sapiens 뉴스 알림",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "브리핑·뉴스·마켓 업데이트 알림"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
