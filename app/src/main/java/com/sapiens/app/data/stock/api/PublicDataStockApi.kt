package com.sapiens.app.data.stock.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface PublicDataStockApi {
    @GET("1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo")
    suspend fun getStockPriceInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("numOfRows") numOfRows: Int = 1,
        @Query("pageNo") pageNo: Int = 1,
        @Query("resultType") resultType: String = "json",
        @Query("likeSrtnCd") likeSrtnCd: String
    ): ResponseBody
}
