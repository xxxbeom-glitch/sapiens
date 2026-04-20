package com.sapiens.app

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * 포그라운드 수신 시 로컬 알림 표시. 백그라운드는 시스템이 notification 페이로드로 표시.
 */
class SapiensFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.notification == null && remoteMessage.data.isEmpty()) return

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val section = remoteMessage.data["section"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("section", section)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            section.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ico_news)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify((section + title).hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "sapiens_news"
    }
}
