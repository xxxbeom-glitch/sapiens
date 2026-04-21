package com.sapiens.app.ui.briefing

import com.sapiens.app.data.model.Article
import kotlin.math.min

/**
 * 브리핑 국내·해외 주요뉴스: 서로 다른 URL이어도 헤드라인이 사실상 같은 사건이면 한 건만 남긴다.
 * 정렬이 최신 우선이면 앞쪽(최신) 기사를 대표로 유지한다.
 */

private const val DEFAULT_SIMILARITY_THRESHOLD = 0.52

/** 헤드라인 비교용: 공백·구두점 제거, 라틴 문자만 소문자. */
internal fun normalizeBriefingHeadlineForSimilarity(headline: String): String {
    val sb = StringBuilder(headline.length)
    for (ch in headline) {
        when {
            ch.isWhitespace() -> Unit
            ch.isLetterOrDigit() -> {
                val c = if (ch in 'A'..'Z') ch.lowercaseChar() else ch
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

private fun bigramMultisetDice(a: String, b: String): Double {
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    if (a == b) return 1.0
    val countsA = HashMap<Long, Int>(min(a.length, 64))
    val countsB = HashMap<Long, Int>(min(b.length, 64))
    var sumA = 0
    var sumB = 0
    for (i in 0 until a.lastIndex) {
        val key = a[i].code.toLong() shl 32 or (a[i + 1].code.toLong() and 0xffffffffL)
        countsA[key] = (countsA[key] ?: 0) + 1
        sumA++
    }
    for (i in 0 until b.lastIndex) {
        val key = b[i].code.toLong() shl 32 or (b[i + 1].code.toLong() and 0xffffffffL)
        countsB[key] = (countsB[key] ?: 0) + 1
        sumB++
    }
    if (sumA == 0 && sumB == 0) return if (a == b) 1.0 else 0.0
    if (sumA == 0 || sumB == 0) return 0.0
    var inter = 0
    for ((k, ca) in countsA) {
        val cb = countsB[k] ?: 0
        if (cb > 0) inter += min(ca, cb)
    }
    val denom = sumA + sumB
    return if (denom == 0) 0.0 else (2.0 * inter) / denom
}

/** 짧은 헤드라인은 문자 단위 Dice로 보조(바이그램이 비어 있는 경우). */
private fun unigramMultisetDice(a: String, b: String): Double {
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    if (a == b) return 1.0
    val ca = HashMap<Int, Int>(min(a.length, 32))
    val cb = HashMap<Int, Int>(min(b.length, 32))
    var sumA = 0
    var sumB = 0
    for (ch in a) {
        val c = ch.code
        ca[c] = (ca[c] ?: 0) + 1
        sumA++
    }
    for (ch in b) {
        val c = ch.code
        cb[c] = (cb[c] ?: 0) + 1
        sumB++
    }
    if (sumA == 0 || sumB == 0) return 0.0
    var inter = 0
    for ((k, va) in ca) {
        val vb = cb[k] ?: 0
        if (vb > 0) inter += min(va, vb)
    }
    val denom = sumA + sumB
    return if (denom == 0) 0.0 else (2.0 * inter) / denom
}

internal fun headlineSimilarityScore(a: String, b: String): Double {
    val na = normalizeBriefingHeadlineForSimilarity(a)
    val nb = normalizeBriefingHeadlineForSimilarity(b)
    if (na.isEmpty() && nb.isEmpty()) return 1.0
    if (na.isEmpty() || nb.isEmpty()) return 0.0
    if (na == nb) return 1.0
    val dice = if (na.length >= 2 && nb.length >= 2) {
        bigramMultisetDice(na, nb)
    } else {
        unigramMultisetDice(na, nb)
    }
    return dice
}

internal fun articlesSimilarByHeadline(
    a: Article,
    b: Article,
    threshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
): Boolean =
    headlineSimilarityScore(a.headline, b.headline) >= threshold

/**
 * 입력 순서를 유지한 채, 앞에 이미 넣은 기사와 헤드라인이 [threshold] 이상 유사하면 제외한다.
 */
internal fun dedupeBriefingArticlesBySimilarHeadline(
    articles: List<Article>,
    threshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
): List<Article> {
    if (articles.size <= 1) return articles
    val out = ArrayList<Article>(articles.size)
    for (article in articles) {
        val duplicate = out.any { kept ->
            articlesSimilarByHeadline(kept, article, threshold)
        }
        if (!duplicate) out.add(article)
    }
    return out
}

/**
 * [articles]는 보통 최신 우선 정렬. 앞에서부터 고르되, 이미 고른 기사와 헤드라인이 유사하면 건너뛰어
 * 최대 [maxCount]건의 주제별 대표만 남긴다.
 */
internal fun selectTopBriefingArticlesDedupedByHeadline(
    articles: List<Article>,
    maxCount: Int,
    threshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
): List<Article> {
    if (maxCount <= 0 || articles.isEmpty()) return emptyList()
    val out = ArrayList<Article>(min(maxCount, articles.size))
    for (article in articles) {
        if (out.size >= maxCount) break
        val duplicate = out.any { kept ->
            articlesSimilarByHeadline(kept, article, threshold)
        }
        if (!duplicate) out.add(article)
    }
    return out
}
