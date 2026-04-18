package com.breaktobreak.dailynews.data.repository

import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.MarketIndicator
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getMorningArticles(): Flow<List<Article>>
    fun getUsArticles(): Flow<List<Article>>
    fun getMarketIndicators(): Flow<List<MarketIndicator>>
    fun getNewsFeed(type: String): Flow<List<Article>>
}

/** `getNewsFeed(type)` 인자 — Firestore `news/feed` 문서 필드명과 동일 */
object NewsFeedType {
    const val REALTIME = "realtime"
    const val POPULAR = "popular"
    const val MAIN = "main"
}
