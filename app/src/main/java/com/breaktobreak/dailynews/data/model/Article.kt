package com.breaktobreak.dailynews.data.model

data class Article(
    val source: String,
    val headline: String,
    val summary: String,
    val time: String,
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

data class USReport(
    val date: String,
    val body: String
)
