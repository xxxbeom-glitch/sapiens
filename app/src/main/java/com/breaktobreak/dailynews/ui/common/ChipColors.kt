package com.breaktobreak.dailynews.ui.common

import androidx.compose.ui.graphics.Color

fun categoryChipColors(category: String): Pair<Color, Color> {
    val normalized = category.trim()
    return when (normalized) {
        "경제" -> Color(0xFF1A2F1A) to Color(0xFF4CAF50)
        "IT", "IT·테크" -> Color(0xFF1A1F2F) to Color(0xFF5B8DEF)
        "정치" -> Color(0xFF2F1A1A) to Color(0xFFEF5B5B)
        "사회" -> Color(0xFF2A2214) to Color(0xFFD4A843)
        "국제" -> Color(0xFF142228) to Color(0xFF43B8D4)
        "부동산" -> Color(0xFF2A1A2F) to Color(0xFFB05BEF)
        "산업" -> Color(0xFF2F1F1A) to Color(0xFFEF8C5B)
        "금융" -> Color(0xFF1A2525) to Color(0xFF5BCEBC)
        "매크로" -> Color(0xFF252015) to Color(0xFFD4943A)
        "빅테크" -> Color(0xFF1A1F2F) to Color(0xFF5B8DEF)
        "금리" -> Color(0xFF1A2525) to Color(0xFF5BCEBC)
        else -> Color(0xFF222222) to Color(0xFF888888)
    }
}
