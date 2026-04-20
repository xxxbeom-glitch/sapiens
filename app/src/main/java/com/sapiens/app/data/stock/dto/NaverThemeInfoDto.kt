package com.sapiens.app.data.stock.dto

/** `GET .../theme/{theme_no}/info?marketType=ALL` 응답(필요 필드만). */
data class NaverThemeInfoDto(
    val categoryInfo: String? = null,
)
