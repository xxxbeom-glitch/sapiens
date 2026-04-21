package com.sapiens.app.ui.briefing

import com.sapiens.app.data.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BriefingHeadlineDedupeTest {

    @Test
    fun normalize_strips_punctuation_and_space() {
        assertEquals(
            "openaichatgptimage20",
            normalizeBriefingHeadlineForSimilarity("OpenAI, ChatGPT Image 2.0 — 출시")
        )
    }

    @Test
    fun similar_openai_headlines_merge_to_one_representative() {
        val a = article(
            "한경",
            "오픈AI, 챗GPT 이미지 2.0 출시…광고·슬라이드에 활용",
            "2026-04-21 09:00",
            "https://h.example/1"
        )
        val b = article(
            "매경",
            "오픈AI 챗GPT 이미지 2.0 vs 어도비 CX 엔터프라이즈",
            "2026-04-21 08:55",
            "https://m.example/2"
        )
        val c = article(
            "매경",
            "하림그룹, 홈플러스 익스프레스 인수 우선협상대상자",
            "2026-04-21 08:50",
            "https://m.example/3"
        )
        val merged = mergeDomesticBriefingArticles(listOf(a), listOf(b, c))
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.headline.contains("오픈AI") })
        assertTrue(merged.any { it.headline.contains("하림") })
    }

    @Test
    fun selectTop_fills_slot_after_skipping_similar() {
        val articles = listOf(
            article("A", "Same topic one", "2026-04-21 10:00", "u1"),
            article("B", "Same topic two extra words", "2026-04-21 09:00", "u2"),
            article("C", "완전히 다른 이슈 헤드라인", "2026-04-21 08:00", "u3"),
            article("D", "또 다른 고유 주제 뉴스", "2026-04-21 07:00", "u4"),
        )
        val top = selectTopBriefingArticlesDedupedByHeadline(articles, maxCount = 3)
        assertEquals(3, top.size)
        assertEquals("Same topic one", top[0].headline)
        assertEquals("완전히 다른 이슈 헤드라인", top[1].headline)
        assertEquals("또 다른 고유 주제 뉴스", top[2].headline)
    }

    @Test
    fun us_list_deduped_preserves_order() {
        val list = listOf(
            article("R", "Nvidia AI chip rally continues", "06:00", "r1"),
            article("B", "Nvidia AI chips rally extended on demand", "05:00", "b1"),
            article("W", "Fed minutes hint at cuts", "04:00", "w1"),
        )
        val out = dedupeBriefingArticlesBySimilarHeadline(list)
        assertEquals(2, out.size)
        assertEquals("Nvidia AI chip rally continues", out[0].headline)
        assertEquals("Fed minutes hint at cuts", out[1].headline)
    }

    private fun article(
        source: String,
        headline: String,
        time: String,
        url: String,
    ): Article = Article(
        source = source,
        headline = headline,
        summary = "",
        time = time,
        url = url
    )
}
