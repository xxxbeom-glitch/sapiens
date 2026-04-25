package com.sapiens.app.data.repository

import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketIndexSnapshot
import com.sapiens.app.data.model.MarketTheme
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getRepresentativeIndices(): Flow<MarketIndexSnapshot>
    fun getNewsFeed(type: NewsFeedType): Flow<List<Article>>
    /** 임의 RSS URL에서 기사 목록을 가져온다(예: Yahoo Finance RSS). */
    fun getRssFeed(url: String): Flow<List<Article>>

    /** Firestore `market/themes/by_no/{theme_no}` 문서들(파이프라인 `rank` 순). */
    fun getMarketThemes(): Flow<List<MarketTheme>>
}

/**
 * Firestore `news/{documentId}` 문서의 `articles` 필드와 1:1.
 * 뉴스 탭: domestic_market=국내 증시, global_market=미국·글로벌 증시, ai_issue=AI ISSUE.
 */
enum class NewsFeedType(val documentId: String) {
    DOMESTIC_MARKET("domestic_market"),
    GLOBAL_MARKET("global_market"),
    AI_ISSUE("ai_issue"),
}
