package com.sapiens.app.data.repository

import android.util.Log
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.MarketIndicator
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
        thumbnailUrl = thumbnailUrl
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
