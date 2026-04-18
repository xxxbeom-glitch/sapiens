package com.breaktobreak.dailynews.data.repository

import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.MarketIndicator
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NewsRepositoryImpl(
    private val firestore: FirebaseFirestore
) : NewsRepository {

    constructor() : this(
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
    )

    override fun getMorningArticles(): Flow<List<Article>> = callbackFlow {
        val reg = firestore.collection(COLLECTION_BRIEFING).document(DOC_MORNING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.parseArticles("articles"))
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override fun getUsArticles(): Flow<List<Article>> = callbackFlow {
        val reg = firestore.collection(COLLECTION_BRIEFING).document(DOC_US_MARKET)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.parseArticles("articles"))
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override fun getMarketIndicators(): Flow<List<MarketIndicator>> = callbackFlow {
        val reg = firestore.collection(COLLECTION_BRIEFING).document(DOC_US_MARKET)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.parseIndicators("indicators"))
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override fun getNewsFeed(type: String): Flow<List<Article>> = callbackFlow {
        val docId = when (type) {
            NewsFeedType.REALTIME -> "realtime"
            NewsFeedType.POPULAR -> "popular"
            NewsFeedType.MAIN -> "main"
            else -> "main"
        }
        val reg = firestore.collection(COLLECTION_NEWS).document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.parseArticles("articles"))
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    companion object {
        private const val DATABASE_ID = "daily-brief"
        private const val COLLECTION_BRIEFING = "briefing"
        private const val COLLECTION_NEWS = "news"
        private const val DOC_MORNING = "morning"
        private const val DOC_US_MARKET = "us_market"
    }
}
