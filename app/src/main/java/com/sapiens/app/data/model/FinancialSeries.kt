package com.sapiens.app.data.model

data class YearFinancialValue(
    val year: String,
    val value: Float,
    val displayValue: String
)

data class FinancialMetric(
    val title: String,
    val values: List<YearFinancialValue>
)

data class FinancialSeries(
    val metrics: List<FinancialMetric>
)
