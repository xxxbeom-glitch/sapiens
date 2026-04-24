package com.sapiens.app.data.repository

/**
 * 사용자 선호도 피드백 — Firestore `feedback/{article_id}`.
 * 뉴스 탭 북마크 시에만 기록; 브리핑 북마크는 사용하지 않음.
 */
interface FeedbackRepository {
    suspend fun saveArticleLike(articleId: String, category: String)
    suspend fun deleteFeedback(articleId: String)

    /** `feedback/{articleId}` 문서 전체(백업용). 없으면 null. */
    suspend fun getFeedbackDocument(articleId: String): Map<String, Any>?

    /** 백업 복원 시 `feedback/{articleId}` 덮어쓰기. */
    suspend fun restoreFeedbackDocument(articleId: String, data: Map<String, Any>)
}
