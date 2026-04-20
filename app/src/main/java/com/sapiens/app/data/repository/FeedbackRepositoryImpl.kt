package com.sapiens.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedbackRepositoryImpl(
    private val firestore: FirebaseFirestore
) : FeedbackRepository {

    constructor() : this(
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
    )

    override suspend fun saveArticleLike(articleId: String, category: String) {
        withContext(Dispatchers.IO) {
            val doc = firestore.collection(COLLECTION_FEEDBACK).document(articleId)
            val payload = hashMapOf<String, Any>(
                "type" to "like",
                "category" to category.ifBlank { "" },
                "keywords" to emptyList<String>(),
                "timestamp" to FieldValue.serverTimestamp()
            )
            Tasks.await(doc.set(payload))
        }
    }

    override suspend fun deleteFeedback(articleId: String) {
        withContext(Dispatchers.IO) {
            Tasks.await(
                firestore.collection(COLLECTION_FEEDBACK).document(articleId).delete()
            )
        }
    }

    override suspend fun getFeedbackDocument(articleId: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            val snap = Tasks.await(
                firestore.collection(COLLECTION_FEEDBACK).document(articleId).get()
            )
            if (!snap.exists()) return@withContext null
            snap.data
        }
    }

    override suspend fun restoreFeedbackDocument(articleId: String, data: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            Tasks.await(
                firestore.collection(COLLECTION_FEEDBACK).document(articleId).set(data)
            )
        }
    }

    private companion object {
        const val DATABASE_ID = "sapiens"
        const val COLLECTION_FEEDBACK = "feedback"
    }
}
