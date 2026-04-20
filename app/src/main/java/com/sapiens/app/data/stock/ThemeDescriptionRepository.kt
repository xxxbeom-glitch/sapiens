package com.sapiens.app.data.stock

import com.sapiens.app.data.stock.api.StockNaverThemeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface ThemeDescriptionRepository {
    /** 네이버 테마 상세의 `categoryInfo` 텍스트. 실패 시 null. */
    suspend fun getCategoryInfo(themeNo: Long): String?
}

class ThemeDescriptionRepositoryImpl(
    private val api: StockNaverThemeApi = StockRetrofitProvider.stockNaverTheme,
) : ThemeDescriptionRepository {

    override suspend fun getCategoryInfo(themeNo: Long): String? = withContext(Dispatchers.IO) {
        runCatching {
            api.getThemeInfo(themeNo).categoryInfo?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
