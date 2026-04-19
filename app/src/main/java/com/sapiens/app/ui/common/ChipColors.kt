package com.sapiens.app.ui.common

import androidx.compose.ui.graphics.Color
import com.sapiens.app.ui.theme.CategoryChipPalette

fun categoryChipColors(category: String): Pair<Color, Color> {
    val normalized = category.trim()
    return when (normalized) {
        "경제" -> CategoryChipPalette.economyBg to CategoryChipPalette.economyFg
        "IT", "IT·테크" -> CategoryChipPalette.itBg to CategoryChipPalette.itFg
        "정치" -> CategoryChipPalette.politicsBg to CategoryChipPalette.politicsFg
        "사회" -> CategoryChipPalette.societyBg to CategoryChipPalette.societyFg
        "국제" -> CategoryChipPalette.worldBg to CategoryChipPalette.worldFg
        "부동산" -> CategoryChipPalette.realestateBg to CategoryChipPalette.realestateFg
        "산업" -> CategoryChipPalette.industryBg to CategoryChipPalette.industryFg
        "금융" -> CategoryChipPalette.financeBg to CategoryChipPalette.financeFg
        "매크로" -> CategoryChipPalette.macroBg to CategoryChipPalette.macroFg
        "빅테크" -> CategoryChipPalette.itBg to CategoryChipPalette.itFg
        "금리" -> CategoryChipPalette.financeBg to CategoryChipPalette.financeFg
        else -> CategoryChipPalette.defaultBg to CategoryChipPalette.defaultFg
    }
}
