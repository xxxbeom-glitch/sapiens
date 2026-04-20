package com.sapiens.app.data.stock.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockNaverNewsApi {
    @GET("api/domestic/news/{code}/news")
    suspend fun getDomesticNews(
        @Path("code") code: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 16
    ): ResponseBody
}
