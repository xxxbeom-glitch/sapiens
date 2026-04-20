package com.sapiens.app.data.stock.api

import com.sapiens.app.data.stock.dto.NaverThemeInfoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockNaverThemeApi {
    @GET("api/domestic/market/theme/{themeNo}/info")
    suspend fun getThemeInfo(
        @Path("themeNo") themeNo: Long,
        @Query("marketType") marketType: String = "ALL",
    ): NaverThemeInfoDto
}
