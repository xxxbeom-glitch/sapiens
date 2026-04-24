package com.sapiens.app.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Cloud Functions / 파이프라인에서 발송하는 FCM 토픽과 동일한 이름. */
object FcmTopicSync {
    /** 통합 예약 푸시(구 `news_update` / `market_update` 대체). */
    val topics: List<String> = listOf("sapiens_feed")

    private val legacyTopics: List<String> = listOf("news_update", "market_update")

    private const val TAG = "FcmTopicSync"

    suspend fun subscribeAll() {
        val messaging = FirebaseMessaging.getInstance()
        for (topic in legacyTopics) {
            try {
                messaging.unsubscribeFromTopic(topic).await()
            } catch (e: Exception) {
                Log.w(TAG, "레거시 토픽 구독 해제 실패: $topic", e)
            }
        }
        for (topic in topics) {
            try {
                messaging.subscribeToTopic(topic).await()
            } catch (e: Exception) {
                Log.w(TAG, "토픽 구독 실패: $topic", e)
            }
        }
    }

    suspend fun unsubscribeAll() {
        val messaging = FirebaseMessaging.getInstance()
        for (topic in legacyTopics + topics) {
            try {
                messaging.unsubscribeFromTopic(topic).await()
            } catch (e: Exception) {
                Log.w(TAG, "토픽 구독 해제 실패: $topic", e)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * DataStore 푸시 설정과 OS 알림 권한에 맞춰 토픽 구독을 맞춤.
     * 꺼짐이거나(API 33+) 권한 없으면 구독 해제.
     */
    suspend fun syncFromPreference(context: Context, pushEnabled: Boolean) {
        if (!pushEnabled || !hasNotificationPermission(context)) {
            unsubscribeAll()
            return
        }
        subscribeAll()
    }
}
