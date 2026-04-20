package com.sapiens.app.ui.common

import android.net.Uri

/**
 * `finance.naver.com/news/news_read.naver` 는 모바일에서 리다이렉트 이슈가 있어
 * `n.news.naver.com/article/{office_id}/{article_id}` 로 바꿔 연다.
 * 파라미터 추출 실패 시 [rawUrl] 그대로 반환.
 */
fun transformNaverFinanceNewsReadUrlForMobile(rawUrl: String): String {
    val marker = "finance.naver.com/news/news_read.naver"
    if (!rawUrl.contains(marker, ignoreCase = true)) return rawUrl
    return runCatching {
        val uri = Uri.parse(rawUrl)
        val articleId = uri.getQueryParameter("article_id")?.trim().orEmpty()
        val officeId = uri.getQueryParameter("office_id")?.trim().orEmpty()
        if (articleId.isNotEmpty() && officeId.isNotEmpty()) {
            "https://n.news.naver.com/article/$officeId/$articleId"
        } else {
            rawUrl
        }
    }.getOrElse { rawUrl }
}
