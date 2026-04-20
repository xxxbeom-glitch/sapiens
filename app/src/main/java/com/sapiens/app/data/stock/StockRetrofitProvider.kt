package com.sapiens.app.data.stock

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sapiens.app.data.stock.api.KrxDataSystemApi
import com.sapiens.app.data.stock.api.MStockApi
import com.sapiens.app.data.stock.api.PublicDataStockApi
import com.sapiens.app.data.stock.api.StockNaverNewsApi
import com.sapiens.app.data.stock.api.StockNaverResearchApi
import com.sapiens.app.data.stock.api.StockNaverThemeApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object StockRetrofitProvider {

    private val gson: Gson = GsonBuilder().create()

    private val okHttp: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(log)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val krxOkHttp: OkHttpClient by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Referer", "https://data.krx.co.kr/")
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(log)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val mStock: MStockApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://m.stock.naver.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MStockApi::class.java)
    }

    private val stockNaverRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://stock.naver.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val stockNaverResearch: StockNaverResearchApi by lazy {
        stockNaverRetrofit.create(StockNaverResearchApi::class.java)
    }

    val stockNaverNews: StockNaverNewsApi by lazy {
        stockNaverRetrofit.create(StockNaverNewsApi::class.java)
    }

    val stockNaverTheme: StockNaverThemeApi by lazy {
        stockNaverRetrofit.create(StockNaverThemeApi::class.java)
    }

    val publicData: PublicDataStockApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PublicDataStockApi::class.java)
    }

    val krxDataSystem: KrxDataSystemApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.krx.co.kr/")
            .client(krxOkHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(KrxDataSystemApi::class.java)
    }
}
