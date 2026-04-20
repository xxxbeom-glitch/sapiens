package com.sapiens.app.data.stock

data class FinancialYearRow(
    val yearLabel: String,
    val revenue: String,
    val operatingProfit: String,
)

data class StockReportItem(
    val brokerName: String,
    val title: String,
    val date: String,
    val goalPrice: String,
    val opinion: String,
    val url: String,
)

data class StockNewsItem(
    val officeName: String,
    val title: String,
    val date: String,
    val thumbnailUrl: String,
    val url: String,
)

data class StockDetailUi(
    val code: String,
    val stockName: String,
    val exchangeLabel: String,
    val closePrice: String,
    val fluctuationsRatio: String,
    /** null 이면 보합 등으로 색 구분 없음 */
    val isRising: Boolean?,
    val marketCap: String,
    val per: String,
    val pbr: String,
    val eps: String,
    val dividendYield: String,
    val dps: String,
    /** 연간 손익 차트 숫자 단위(예: 억원). 빈 문자열이면 부가 문구 생략. */
    val financialValueUnit: String,
    val financialYears: List<FinancialYearRow>,
    val reports: List<StockReportItem>,
    val news: List<StockNewsItem>,
)

sealed class StockDetailUiState {
    data object Idle : StockDetailUiState()
    data object Loading : StockDetailUiState()
    data class Success(val data: StockDetailUi) : StockDetailUiState()
    data class Error(val message: String) : StockDetailUiState()
}
