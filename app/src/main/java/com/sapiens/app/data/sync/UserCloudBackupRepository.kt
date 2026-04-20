package com.sapiens.app.data.sync

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.repository.FeedbackRepository
import com.sapiens.app.data.repository.FeedbackRepositoryImpl
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.data.store.BookmarkEntry
import com.sapiens.app.data.store.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 로그인 사용자 설정·북마크·피드백을 Firestore `users/{uid}` 하위에 백업/복원.
 *
 * 구조:
 * - `users/{uid}` — 필드 `lastBackupAt`
 * - `users/{uid}/settings/data` — 설정 맵
 * - `users/{uid}/bookmarks/data` — `bookmarksJson`
 * - `users/{uid}/feedback/data` — `entries` (articleId + feedback 필드)
 */
class UserCloudBackupRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val articleBookmarksRepository: ArticleBookmarksRepository,
    private val feedbackRepository: FeedbackRepository,
) {

    suspend fun restoreFromCloudIfNeeded(uid: String) = withContext(Dispatchers.IO) {
        val userRef = firestore.collection(COLLECTION_USERS).document(uid)
        val root = userRef.get().await()
        if (!root.exists()) return@withContext

        val settingsSnap = userRef.collection(SUB_SETTINGS).document(DOC_DATA).get().await()
        val bookmarksSnap = userRef.collection(SUB_BOOKMARKS).document(DOC_DATA).get().await()
        val feedbackSnap = userRef.collection(SUB_FEEDBACK).document(DOC_DATA).get().await()

        val hasCloudPayload =
            settingsSnap.exists() || bookmarksSnap.exists() || feedbackSnap.exists()
        if (!hasCloudPayload) return@withContext

        val lastCloudMs = root.getTimestamp(FIELD_LAST_BACKUP_AT)?.toDate()?.time ?: 0L
        val lastRestored = userPreferencesRepository.getLastRestoredCloudBackupMs()
        if (lastCloudMs > 0L && lastRestored > 0L && lastCloudMs <= lastRestored) {
            return@withContext
        }

        val localHasBookmarks = articleBookmarksRepository.hasBookmarks()
        if (localHasBookmarks) {
            return@withContext
        }

        if (settingsSnap.exists()) {
            val data = settingsSnap.data
            if (!data.isNullOrEmpty()) {
                userPreferencesRepository.applyCloudSettingsSnapshot(data)
            }
        }
        if (bookmarksSnap.exists()) {
            val json = bookmarksSnap.getString(FIELD_BOOKMARKS_JSON).orEmpty()
            if (json.isNotBlank()) {
                articleBookmarksRepository.setRawBookmarksJson(json)
            }
        }
        if (feedbackSnap.exists()) {
            @Suppress("UNCHECKED_CAST")
            val entries = feedbackSnap.get(FIELD_ENTRIES) as? List<Map<String, Any>> ?: emptyList()
            for (raw in entries) {
                val articleId = raw["articleId"] as? String ?: continue
                val payload = raw.filterKeys { it != "articleId" }.toMutableMap()
                if (payload.isNotEmpty()) {
                    feedbackRepository.restoreFeedbackDocument(articleId, payload)
                }
            }
        }

        val appliedMs = if (lastCloudMs > 0L) lastCloudMs else System.currentTimeMillis()
        userPreferencesRepository.setLastRestoredCloudBackupMs(appliedMs)
    }

    suspend fun performBackup(uid: String) = withContext(Dispatchers.IO) {
        val userRef = firestore.collection(COLLECTION_USERS).document(uid)

        val settings = userPreferencesRepository.exportSettingsSnapshot()
        userRef.collection(SUB_SETTINGS).document(DOC_DATA).set(settings, SetOptions.merge()).await()

        val bookmarksJson = articleBookmarksRepository.getRawBookmarksJson()
        userRef.collection(SUB_BOOKMARKS).document(DOC_DATA)
            .set(mapOf(FIELD_BOOKMARKS_JSON to bookmarksJson), SetOptions.merge()).await()

        val bookmarkEntries: List<BookmarkEntry> =
            articleBookmarksRepository.bookmarksFlow.first()
        val feedbackEntries = buildList<Map<String, Any>> {
            for (e in bookmarkEntries) {
                if (!e.withFeedbackSync) continue
                val id = e.article.stableId()
                val doc = feedbackRepository.getFeedbackDocument(id) ?: continue
                add(linkedMapOf<String, Any>("articleId" to id).apply { putAll(doc) })
            }
        }
        userRef.collection(SUB_FEEDBACK).document(DOC_DATA)
            .set(mapOf(FIELD_ENTRIES to feedbackEntries), SetOptions.merge()).await()

        userRef.set(
            mapOf(FIELD_LAST_BACKUP_AT to FieldValue.serverTimestamp()),
            SetOptions.merge()
        ).await()
    }

    companion object {
        private const val DATABASE_ID = "sapiens"
        private const val COLLECTION_USERS = "users"
        private const val SUB_SETTINGS = "settings"
        private const val SUB_BOOKMARKS = "bookmarks"
        private const val SUB_FEEDBACK = "feedback"
        private const val DOC_DATA = "data"
        private const val FIELD_LAST_BACKUP_AT = "lastBackupAt"
        private const val FIELD_BOOKMARKS_JSON = "bookmarksJson"
        private const val FIELD_ENTRIES = "entries"

        fun create(application: Application): UserCloudBackupRepository {
            val fs = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
            return UserCloudBackupRepository(
                context = application.applicationContext,
                firestore = fs,
                userPreferencesRepository = UserPreferencesRepository(application),
                articleBookmarksRepository = ArticleBookmarksRepository(application),
                feedbackRepository = FeedbackRepositoryImpl()
            )
        }
    }
}
