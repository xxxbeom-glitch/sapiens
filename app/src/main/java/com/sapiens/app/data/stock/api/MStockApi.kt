package com.sapiens.app.data.stock.api

import com.sapiens.app.data.stock.dto.FinanceSummaryDto
import com.sapiens.app.data.stock.dto.NaverStockBasicDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MStockApi {
    @GET("api/stock/{code}/basic")
    suspend fun getBasic(@Path("code") code: String): NaverStockBasicDto

    /** 시총·PER·EPS 등은 [totalInfos]에 있음( basic 단독 응답에는 없는 경우가 많음). */
    @GET("api/stock/{code}/integration")
    suspend fun getIntegration(@Path("code") code: String): ResponseBody

    @GET("api/stock/{code}/finance/summary")
    suspend fun getFinanceSummary(@Path("code") code: String): FinanceSummaryDto

    /** `stock.naver.com` 뉴스 API 404 등일 때 폴백. */
    @GET("api/news/stock/{code}")
    suspend fun getNewsByStock(
        @Path("code") code: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 16
    ): ResponseBody
}
