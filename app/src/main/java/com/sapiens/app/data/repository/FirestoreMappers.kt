package com.sapiens.app.data.repository

import android.util.Log
import java.util.Locale
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.MarketIndicator
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.model.ThemeStock
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.parseArticles(field: String = "articles"): List<Article> {
    if (!exists()) return emptyList()
    return try {
        get(field).asArticleList()
    } catch (e: Exception) {
        Log.e("FirestoreMappers", "parseArticles id=${id} field=$field", e)
        emptyList()
    }
}

internal fun DocumentSnapshot.parseIndicators(field: String = "indicators"): List<MarketIndicator> {
    if (!exists()) return emptyList()
    return try {
        get(field).asIndicatorList()
    } catch (e: Exception) {
        Log.e("FirestoreMappers", "parseIndicators id=${id} field=$field", e)
        emptyList()
    }
}

internal fun DocumentSnapshot.parseMarketThemes(field: String = "themes"): List<MarketTheme> {
    if (!exists()) return emptyList()
    return try {
        get(field).asThemeList()
    } catch (e: Exception) {
        Log.e("FirestoreMappers", "parseMarketThemes id=${id} field=$field", e)
        emptyList()
    }
}

/** `market/themes/by_no/{theme_no}` 및 `market/industries/by_no/{no}` 문서 → [MarketTheme]. */
internal fun DocumentSnapshot.toMarketThemeDoc(): MarketTheme? {
    if (!exists()) return null
    val m = data ?: return null
    val themeName = stringField(m, "theme_name", "themeName", "name")?.trim().orEmpty()
    if (themeName.isBlank()) return null
    val changeRate = stringField(m, "change_rate", "changeRate").orEmpty()
    val themeNo = longField(m, "no", "theme_no", "themeNo") ?: id.trim().toLongOrNull()
    val stocksRaw = m["stocks"] ?: m["stockList"] ?: m["items"]
    val stocks = when (stocksRaw) {
        is List<*> -> stocksRaw.mapNotNull { (it as? Map<*, *>)?.toThemeStock() }
        else -> {
            if (stocksRaw != null) {
                Log.w("FirestoreMappers", "toMarketThemeDoc: stocks 필드가 List가 아님 type=${stocksRaw::class.java.name}")
            }
            emptyList()
        }
    }
    val categoryInfo =
        stringField(m, "description", "categoryInfo", "category_info")?.trim().orEmpty()
    return MarketTheme(
        themeName = themeName,
        changeRate = changeRate,
        stocks = stocks,
        themeNo = themeNo,
        categoryInfo = categoryInfo,
    )
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asArticleList(): List<Article> {
    if (this !is List<*>) return emptyList()
    return this.mapNotNull { (it as? Map<*, *>)?.toArticle() }
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asIndicatorList(): List<MarketIndicator> {
    if (this !is List<*>) return emptyList()
    return this.mapNotNull { (it as? Map<*, *>)?.toMarketIndicator() }
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asThemeList(): List<MarketTheme> {
    if (this !is List<*>) return emptyList()
    return this.mapNotNull { (it as? Map<*, *>)?.toMarketTheme() }
}

private fun Map<*, *>.toArticle(): Article? {
    val source = (this["source"] as? String).orEmpty()
    val headlineKo = this["headline_ko"] as? String
    val headline = (headlineKo ?: this["headline"] as? String)?.trim().orEmpty()
    if (headline.isBlank()) return null
    val summary = this["summary"] as? String ?: return null
    val time = this["time"] as? String ?: return null
    val category = this["category"] as? String ?: ""
    val tag = this["tag"] as? String ?: ""
    val sourceColor = this["sourceColor"] as? String
    val imageUrl = (
        this["imageUrl"] as? String
            ?: this["image_url"] as? String
            ?: this["thumbnailUrl"] as? String
            ?: this["thumbnail_url"] as? String
            ?: this["thumbnail"] as? String
    ).orEmpty()
    val thumbnailUrl = (
        this["thumbnailUrl"] as? String
            ?: this["thumbnail_url"] as? String
            ?: this["imageUrl"] as? String
            ?: this["image_url"] as? String
            ?: this["thumbnail"] as? String
    ).orEmpty()
    Log.d(
        "FirestoreMappers",
        "article image fields imageUrl='${this["imageUrl"]}' thumbnailUrl='${this["thumbnailUrl"]}' " +
            "thumbnail_url='${this["thumbnail_url"]}' image_url='${this["image_url"]}' mapped='$imageUrl/$thumbnailUrl'"
    )
    val summaryPoints = (this["summaryPoints"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    val url = (
        this["url"] as? String
            ?: this["link"] as? String
            ?: this["articleUrl"] as? String
    ).orEmpty().trim()
    return Article(
        source = source,
        headline = headline,
        summary = summary,
        time = time,
        category = category,
        summaryPoints = summaryPoints,
        tag = tag,
        sourceColor = sourceColor,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
        url = url
    )
}

private fun Map<*, *>.toMarketIndicator(): MarketIndicator? {
    val name = this["name"] as? String ?: return null
    val value = this["value"] as? String ?: return null
    val change = this["change"] as? String ?: return null
    val direction = parseDirection(this["direction"])
    return MarketIndicator(name = name, value = value, change = change, direction = direction)
}

private fun parseDirection(raw: Any?): MarketDirection = when (raw) {
    is String -> when (raw.uppercase()) {
        "UP" -> MarketDirection.UP
        "DOWN" -> MarketDirection.DOWN
        "FLAT" -> MarketDirection.FLAT
        else -> MarketDirection.FLAT
    }
    else -> MarketDirection.FLAT
}

private fun Map<*, *>.toMarketTheme(): MarketTheme? {
    val themeName = stringField(this, "theme_name", "themeName", "name")?.trim().orEmpty()
    if (themeName.isBlank()) return null
    val changeRate = stringField(this, "change_rate", "changeRate").orEmpty()
    val themeNo = longField(this, "no", "theme_no", "themeNo")
    val stocksRaw = this["stocks"] ?: this["stockList"] ?: this["items"]
    val stocks = when (stocksRaw) {
        is List<*> -> stocksRaw.mapNotNull { (it as? Map<*, *>)?.toThemeStock() }
        else -> {
            if (stocksRaw != null) {
                Log.w("FirestoreMappers", "parseMarketThemes: stocks 필드가 List가 아님 type=${stocksRaw::class.java.name}")
            }
            emptyList()
        }
    }
    val categoryInfo =
        stringField(this, "description", "categoryInfo", "category_info")?.trim().orEmpty()
    return MarketTheme(
        themeName = themeName,
        changeRate = changeRate,
        stocks = stocks,
        themeNo = themeNo,
        categoryInfo = categoryInfo,
    )
}

/** Firestore/파이프라인 snake·camel 혼용 및 숫자 price 대응. code 없어도 종목명이 있으면 행 유지(로고만 생략). */
private fun Map<*, *>.toThemeStock(): ThemeStock? {
    val name = stringField(this, "name", "itemname", "itemName", "stockName", "stockNm", "nm")?.trim().orEmpty()
    if (name.isBlank()) return null
    val code = normalizeKrxStockCode(
        stringField(this, "code", "itemCode", "itemcode", "stockCd", "stockCode", "isuCd", "isuSrtCd", "ticker", "symbol")
            ?: numberFieldAsString(this, "code", "itemCode", "stockCd", "stockCode")
    )
    val price = stringField(this, "price", "dealPrice", "closePrice", "prpr", "now")
        ?: numberFieldAsString(this, "price", "closePrice", "prpr", "now", "dealPrice")
        ?: "—"
    val priceDisplay = price.trim().ifBlank { "—" }
    val change = stringField(this, "change", "chg", "fluctuationsRatio")
        ?: numberFieldAsString(this, "change", "fluctuationsRatio", "prdyCtrt")
        ?: "0%"
    val changeDisplay = change.trim().ifBlank { "0%" }
    return ThemeStock(name = name, price = priceDisplay, change = changeDisplay, code = code)
}

private fun longField(m: Map<*, *>, vararg keys: String): Long? {
    for (k in keys) {
        when (val v = m[k]) {
            is Number -> return v.toLong()
            is String -> v.trim().toLongOrNull()?.let { return it }
            else -> {}
        }
    }
    return null
}

private fun stringField(m: Map<*, *>, vararg keys: String): String? {
    for (k in keys) {
        when (val v = m[k]) {
            is String -> if (v.isNotBlank()) return v
            else -> {}
        }
    }
    return null
}

private fun numberFieldAsString(m: Map<*, *>, vararg keys: String): String? {
    for (k in keys) {
        when (val v = m[k]) {
            is Number -> return formatThemeNumberForDisplay(v)
            is String -> {
                val t = v.trim()
                if (t.isNotEmpty() && t.any { it.isDigit() }) return t
            }
            else -> {}
        }
    }
    return null
}

private fun formatThemeNumberForDisplay(n: Number): String {
    val d = n.toDouble()
    if (d.isNaN()) return n.toString()
    val longVal = d.toLong()
    if (kotlin.math.abs(d - longVal.toDouble()) < 1e-6) {
        return String.format(Locale.KOREA, "%,d", longVal)
    }
    return String.format(Locale.KOREA, "%.2f", d)
}

/** 6자리 내 숫자면 앞에 0 패딩, 그 외는 trim 그대로. */
private fun normalizeKrxStockCode(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val t = raw.trim()
    val digits = t.filter { it.isDigit() }
    if (digits.isEmpty()) return t
    if (digits.length in 1..6) return digits.padStart(6, '0')
    return t
}
