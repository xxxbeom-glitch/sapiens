package com.sapiens.app.ui.briefing

import com.sapiens.app.data.model.Article
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val BriefingPublishedTimeFormat =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
private val SeoulZone = ZoneId.of("Asia/Seoul")

/** Firestore `time`(= 파이프라인 `published_at`) 문자열을 정렬용 epoch ms로 변환. */
internal fun Article.briefingPublishedAtMillis(): Long {
    val t = time.trim()
    if (t.isBlank()) return 0L
    return try {
        LocalDateTime.parse(t, BriefingPublishedTimeFormat)
            .atZone(SeoulZone)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        t.toLongOrNull() ?: 0L
    }
}

/**
 * `briefing/hankyung`·`briefing/maeil` 각 최대 5건을 합친 뒤 발행 시각 내림차순으로 상위 10건.
 * (파이프라인의 briefing_hankyung_pool / briefing_maeil_pool과 동일 출처.)
 * 동일·유사 헤드라인(다른 URL)은 한 주제당 한 건만 남긴다.
 */
internal fun mergeDomesticBriefingArticles(
    hankyung: List<Article>,
    maeil: List<Article>,
): List<Article> {
    val sorted = (hankyung.take(5) + maeil.take(5))
        .distinctBy { a -> a.url.ifBlank { "${a.source}|${a.headline}" } }
        .sortedByDescending { it.briefingPublishedAtMillis() }
    return selectTopBriefingArticlesDedupedByHeadline(sorted, maxCount = 10)
}
