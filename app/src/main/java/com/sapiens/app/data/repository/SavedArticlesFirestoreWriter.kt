package com.sapiens.app.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 북마크 시 `saved_articles/{article_id}`에 기사 전체 스냅샷 저장(파이프라인 삭제 보호용).
 * Firestore 보안 규칙에 맞게 인증된 사용자만 쓰기 가능해야 함.
 */
interface BookmarkSavedArticlesSync {
    suspend fun syncAfterAdded(article: Article)
    suspend fun syncAfterRemoved(articleId: String)
}

class SavedArticlesFirestoreWriter(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        DATABASE_ID
    )
) : BookmarkSavedArticlesSync {

    override suspend fun syncAfterAdded(article: Article) = withContext(Dispatchers.IO) {
        val id = article.stableId()
        if (id.isBlank()) return@withContext
        try {
            firestore.collection(COLLECTION).document(id).set(article.toSavedArticleMap(), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "saved_articles 저장 실패 id=$id", e)
        }
    }

    override suspend fun syncAfterRemoved(articleId: String) = withContext(Dispatchers.IO) {
        if (articleId.isBlank()) return@withContext
        try {
            firestore.collection(COLLECTION).document(articleId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "saved_articles 삭제 실패 id=$articleId", e)
        }
    }

    private fun Article.toSavedArticleMap(): Map<String, Any?> = mapOf(
        "source" to source,
        "headline" to headline,
        "headline_ko" to headline,
        "summary" to summary,
        "time" to time,
        "category" to category,
        "summaryPoints" to summaryPoints,
        "tag" to tag,
        "sourceColor" to sourceColor,
        "imageUrl" to imageUrl,
        "thumbnailUrl" to thumbnailUrl,
        "url" to url,
    )

    private companion object {
        private const val TAG = "SavedArticlesFS"
        private const val DATABASE_ID = "sapiens"
        private const val COLLECTION = "saved_articles"
    }
}
