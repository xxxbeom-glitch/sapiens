package com.sapiens.app.data.model

import java.util.Locale

data class ThemeStock(
    val name: String,
    val price: String,
    val change: String,
    val code: String,
    /** 네이버 stocklist: 1=하락 2=상승 3=보합 4=하한가 5=상한가. 없으면 [change]만으로 추정. */
    val upDownGb: String? = null,
)

data class MarketTheme(
    val themeName: String,
    val changeRate: String,
    val stocks: List<ThemeStock>,
    /** 네이버 테마 API 경로용 (`no` 등). 없으면 설명 API 미호출. */
    val themeNo: Long? = null,
    /** Firestore `description`(파이프라인 categoryInfo) 또는 [ThemeDescriptionRepository] 폴백. */
    val categoryInfo: String = "",
)

fun ThemeStock.changeSignedPercent(): Double = change.toSignedChangePercent()

fun MarketTheme.stocksForDisplay(maxCount: Int = 4): List<ThemeStock> =
    stocks
        .sortedByDescending { it.changeSignedPercent() }
        .take(maxCount)

fun ThemeStock.logoUrl(): String =
    if (code.isBlank()) "" else "https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock$code.svg"

/** `+1,23%`, `−0.5%` 등 Firestore/표시용 등락률 문자열을 정렬용 실수로 변환. */
internal fun String.toSignedChangePercent(): Double {
    if (isBlank()) return 0.0
    val normalized = trim()
        .replace("％", "%")
        .replace("−", "-")
        .replace("–", "-")
        .replace("%", "")
        .replace(",", "")
        .trim()
    val isNeg = normalized.startsWith("-")
    val isPos = normalized.startsWith("+")
    val core = normalized.removePrefix("+").removePrefix("-").trim()
    val num = core.toDoubleOrNull() ?: return 0.0
    return when {
        isNeg -> -kotlin.math.abs(num)
        isPos -> kotlin.math.abs(num)
        else -> num
    }
}

private val CHANGE_DISPLAY_NOISE = listOf(
    "전일대비",
    "전일비",
    "상한가",
    "하한가",
    "보합",
    "상승",
    "하락",
    "등락",
)

private fun String.stripChangeDisplayNoise(): String {
    var s = trim()
    for (m in CHANGE_DISPLAY_NOISE) {
        s = s.replace(m, "")
    }
    return s.replace(" ", "")
}

private fun formatPercentDigitsOnly(signedMag: Double, direction: MarketDirection): String {
    val body = String.format(Locale.US, "%.2f", kotlin.math.abs(signedMag))
    return when (direction) {
        MarketDirection.UP -> "+${body}%"
        MarketDirection.DOWN -> "-${body}%"
        MarketDirection.FLAT -> "0%"
    }
}

/**
 * 마켓 테마·업종 카드 등락: 수치만(또는 `상한가`/`하한가`/`0%`), 색은 [MarketDirection].
 *
 * [upDownGb] 네이버 API: `1` 하락, `2` 상승, `3` 보합, `4` 하한가, `5` 상한가.
 * 없으면 [raw] 문자열만으로 부호·한가 추정(레거시 Firestore).
 */
fun marketChangeDisplay(raw: String, upDownGb: String? = null): Pair<String, MarketDirection> {
    val gb = upDownGb?.trim().orEmpty()
    if (gb == "5") return "상한가" to MarketDirection.UP
    if (gb == "4") return "하한가" to MarketDirection.DOWN
    if (gb == "3") return "0%" to MarketDirection.FLAT

    val t = raw.trim()
    if (t.contains("상한가")) return "상한가" to MarketDirection.UP
    if (t.contains("하한가")) return "하한가" to MarketDirection.DOWN

    val v = t.stripChangeDisplayNoise().toSignedChangePercent()
    if (kotlin.math.abs(v) < 1e-9) return "0%" to MarketDirection.FLAT

    return when (gb) {
        "2" -> formatPercentDigitsOnly(v, MarketDirection.UP) to MarketDirection.UP
        "1" -> formatPercentDigitsOnly(v, MarketDirection.DOWN) to MarketDirection.DOWN
        else -> when {
            v > 0.0 -> formatPercentDigitsOnly(v, MarketDirection.UP) to MarketDirection.UP
            v < 0.0 -> formatPercentDigitsOnly(v, MarketDirection.DOWN) to MarketDirection.DOWN
            else -> "0%" to MarketDirection.FLAT
        }
    }
}

fun ThemeStock.themeStockChangeDisplay(): Pair<String, MarketDirection> =
    marketChangeDisplay(change, upDownGb)

/** 테마·업종 카드 헤더용(change_rate 문자열만). */
fun String.toMarketCardChangeDisplay(): Pair<String, MarketDirection> =
    marketChangeDisplay(this, null)
