package com.sapiens.app.data.repository

import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketIndicator
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getMorningArticles(): Flow<List<Article>>
    fun getUsArticles(): Flow<List<Article>>
    fun getMarketIndicators(): Flow<List<MarketIndicator>>
    fun getNewsFeed(type: NewsFeedType): Flow<List<Article>>
}

/**
 * Firestore `news/{documentId}` 문서의 `articles` 필드와 1:1.
 * 국내: realtime / popular / main, 해외: overseas_stocks / overseas_tech
 */
enum class NewsFeedType(val documentId: String) {
    REALTIME("realtime"),
    POPULAR("popular"),
    MAIN("main"),
    OVERSEAS_STOCKS("overseas_stocks"),
    OVERSEAS_TECH("overseas_tech"),
}
