package com.sapiens.app.data.repository

import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketIndicator
import com.sapiens.app.data.model.MarketIndexSnapshot
import com.sapiens.app.data.model.MarketTheme
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    /** Firestore `briefing/hankyung` 문서의 `articles`(파이프라인 풀 최대 5건). 앱에서 매경과 합쳐 정렬. */
    fun getBriefingHankyungArticles(): Flow<List<Article>>

    /** Firestore `briefing/maeil` 문서의 `articles`(파이프라인 풀 최대 5건). 앱에서 한경과 합쳐 정렬. */
    fun getBriefingMaeilArticles(): Flow<List<Article>>
    fun getUsArticles(): Flow<List<Article>>
    fun getMarketIndicators(): Flow<List<MarketIndicator>>
    fun getRepresentativeIndices(): Flow<MarketIndexSnapshot>
    fun getNewsFeed(type: NewsFeedType): Flow<List<Article>>

    /** Firestore `market/themes/by_no/{theme_no}` 문서들(파이프라인 `rank` 순). */
    fun getMarketThemes(): Flow<List<MarketTheme>>
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
