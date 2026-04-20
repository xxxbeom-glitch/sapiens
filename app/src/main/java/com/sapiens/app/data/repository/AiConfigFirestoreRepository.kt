package com.sapiens.app.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sapiens.app.data.model.AiSelectedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 파이프라인과 동일 스키마: `settings/ai_config`
 * 필드: [selected_model], [updatedAt]
 */
class AiConfigFirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        DATABASE_ID,
    )
) {

    suspend fun save(selectedModel: String) = withContext(Dispatchers.IO) {
        val normalized = AiSelectedModel.normalize(selectedModel)
        try {
            firestore.collection(COLLECTION_SETTINGS).document(DOC_AI_CONFIG).set(
                mapOf(
                    FIELD_SELECTED_MODEL to normalized,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "settings/ai_config 저장 실패", e)
        }
    }

    private companion object {
        private const val TAG = "AiConfigFS"
        private const val DATABASE_ID = "sapiens"
        private const val COLLECTION_SETTINGS = "settings"
        private const val DOC_AI_CONFIG = "ai_config"
        private const val FIELD_SELECTED_MODEL = "selected_model"
        private const val FIELD_UPDATED_AT = "updatedAt"
    }
}
