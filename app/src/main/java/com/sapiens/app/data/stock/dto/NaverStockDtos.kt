package com.sapiens.app.data.stock.dto

import com.google.gson.annotations.SerializedName

data class NaverStockBasicDto(
    @SerializedName("stockName") val stockName: String? = null,
    @SerializedName("itemCode") val itemCode: String? = null,
    @SerializedName("closePrice") val closePrice: String? = null,
    @SerializedName("fluctuationsRatio") val fluctuationsRatio: String? = null,
    @SerializedName("compareToPreviousPrice") val compareToPreviousPrice: CompareToPreviousDto? = null,
    @SerializedName("stockExchangeType") val stockExchangeType: StockExchangeTypeDto? = null,
    @SerializedName("stockExchangeName") val stockExchangeName: String? = null,
    /** 네이버 종목 basic — 투자 지표(숫자/문자 혼합 응답은 Gson이 문자열로 매핑) */
    @SerializedName(value = "per", alternate = ["PER"]) val per: String? = null,
    @SerializedName(value = "pbr", alternate = ["PBR"]) val pbr: String? = null,
    @SerializedName(value = "eps", alternate = ["EPS"]) val eps: String? = null,
    @SerializedName(
        value = "dividendRate",
        alternate = ["dividend_rate", "dvdYld", "divYld"]
    ) val dividendRate: String? = null,
    @SerializedName(
        value = "dividend",
        alternate = ["dps", "DPS", "stckDps", "stck_dvd_amt"]
    ) val dividend: String? = null,
    @SerializedName(
        value = "marketSum",
        alternate = ["market_sum", "mrktTotAmt", "hts_avls"]
    ) val marketSum: String? = null,
)

data class CompareToPreviousDto(
    @SerializedName("name") val name: String? = null,
)

data class StockExchangeTypeDto(
    @SerializedName("nameKor") val nameKor: String? = null,
)

data class FinanceSummaryDto(
    @SerializedName("chartIncomeStatement") val chartIncomeStatement: ChartIncomeStatementDto? = null,
)

data class ChartIncomeStatementDto(
    @SerializedName("annual") val annual: ChartBlockDto? = null,
)

data class ChartBlockDto(
    @SerializedName("columns") val columns: List<List<String>>? = null,
)

data class ResearchItemDto(
    @SerializedName("brokerName") val brokerName: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("writeDate") val writeDate: String? = null,
    @SerializedName("goalPrice") val goalPrice: String? = null,
    @SerializedName("opinion") val opinion: String? = null,
    @SerializedName("attachUrl") val attachUrl: String? = null,
)
