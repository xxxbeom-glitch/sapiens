package com.sapiens.app.data.stock.api

import com.sapiens.app.data.stock.dto.ResearchItemDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockNaverResearchApi {
    @GET("api/domestic/research/{code}/research")
    suspend fun getResearch(
        @Path("code") code: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 16
    ): List<ResearchItemDto>
}
