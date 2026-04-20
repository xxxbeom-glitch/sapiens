package com.sapiens.app.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sapiens.app.data.model.MarketTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Firestore `market/industries/by_no/{no}` — 파이프라인 업종 스냅샷.
 * 문서 필드는 테마와 동일(`name`, `change_rate`, `stocks`, `rank`)하며 [toMarketThemeDoc]으로 파싱.
 */
fun interface IndustryRepository {
    fun observeIndustries(): Flow<List<MarketTheme>>
}

class IndustryRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        DATABASE_ID,
    ),
) : IndustryRepository {

    override fun observeIndustries(): Flow<List<MarketTheme>> = callbackFlow {
        val coll = firestore.collection(COLLECTION_MARKET)
            .document(DOC_MARKET_INDUSTRIES_ROOT)
            .collection(SUB_MARKET_INDUSTRIES_BY_NO)
        val reg = coll.addSnapshotListener { qs, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (qs == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val sorted = qs.documents.sortedWith(
                compareBy<DocumentSnapshot> { it.getLong("rank") ?: Long.MAX_VALUE }
                    .thenBy { it.id }
            )
            trySend(sorted.mapNotNull { it.toMarketThemeDoc() })
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    private companion object {
        private const val DATABASE_ID = "sapiens"
        private const val COLLECTION_MARKET = "market"
        private const val DOC_MARKET_INDUSTRIES_ROOT = "industries"
        private const val SUB_MARKET_INDUSTRIES_BY_NO = "by_no"
    }
}
