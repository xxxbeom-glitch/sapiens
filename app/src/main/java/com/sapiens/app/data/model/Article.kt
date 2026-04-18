package com.sapiens.app.data.model

data class Article(
    val source: String,
    val headline: String,
    val summary: String,
    val time: String,
    val category: String = "",
    val summaryPoints: List<String> = emptyList(),
    val tag: String = "",
    val sourceColor: String? = null
)

enum class MarketDirection {
    UP,
    DOWN,
    FLAT
}

data class MarketIndicator(
    val name: String,
    val value: String,
    val change: String,
    val direction: MarketDirection
)

data class MarketIndex(
    val group: String,
    val name: String,
    val value: String,
    val change: String,
    val prevValue: String,
    val direction: MarketDirection
)

data class MarketIndexSnapshot(
    val indices: List<MarketIndex>,
    val updatedAtMillis: Long
)

data class USReport(
    val date: String,
    val body: String
)
