package com.sapiens.app.ui.common

import androidx.compose.ui.graphics.Color
import com.sapiens.app.ui.theme.CategoryChipPalette
import com.sapiens.app.ui.theme.PublisherChipPalette

fun categoryChipColors(category: String): Pair<Color, Color> {
    val normalized = category.trim()
    return when (normalized) {
        "경제" -> CategoryChipPalette.economyBg to CategoryChipPalette.economyFg
        "테크&반도체" -> CategoryChipPalette.itBg to CategoryChipPalette.itFg
        "증시" -> CategoryChipPalette.financeBg to CategoryChipPalette.financeFg
        "정치" -> CategoryChipPalette.politicsBg to CategoryChipPalette.politicsFg
        "사회" -> CategoryChipPalette.societyBg to CategoryChipPalette.societyFg
        "국제" -> CategoryChipPalette.worldBg to CategoryChipPalette.worldFg
        "부동산" -> CategoryChipPalette.realestateBg to CategoryChipPalette.realestateFg
        "산업" -> CategoryChipPalette.industryBg to CategoryChipPalette.industryFg
        "빅테크" -> CategoryChipPalette.itBg to CategoryChipPalette.itFg
        "암호화폐" -> CategoryChipPalette.cryptoBg to CategoryChipPalette.cryptoFg
        // 레거시·별칭 (Firestore·구 파이프라인)
        "IT", "IT·테크", "테크·반도체" -> CategoryChipPalette.itBg to CategoryChipPalette.itFg
        "금융", "증권", "원자재", "채권" -> CategoryChipPalette.financeBg to CategoryChipPalette.financeFg
        "매크로", "금리", "환율" -> CategoryChipPalette.economyBg to CategoryChipPalette.economyFg
        else -> CategoryChipPalette.defaultBg to CategoryChipPalette.defaultFg
    }
}

/** 뉴스 출처 칩(매경·한경·조선·CNBC 등). [newsPublisherChipText] 결과 문자열에 맞춘다. */
fun publisherChipColors(displayLabel: String): Pair<Color, Color> {
    val t = displayLabel.trim()
    if (t.isEmpty()) {
        return PublisherChipPalette.defaultBg to PublisherChipPalette.defaultFg
    }
    val lower = t.lowercase()
    return when {
        lower.contains("cnbc") ->
            PublisherChipPalette.cnbcBg to PublisherChipPalette.cnbcFg
        t.contains("매일경제") ->
            PublisherChipPalette.mkBg to PublisherChipPalette.mkFg
        t.contains("한국경제") ->
            PublisherChipPalette.hankyungBg to PublisherChipPalette.hankyungFg
        t.contains("조선") ->
            PublisherChipPalette.chosunBg to PublisherChipPalette.chosunFg
        else ->
            PublisherChipPalette.defaultBg to PublisherChipPalette.defaultFg
    }
}
