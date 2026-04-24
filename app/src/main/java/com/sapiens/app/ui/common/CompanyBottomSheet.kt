package com.sapiens.app.ui.common

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Company
import com.sapiens.app.data.model.FinancialMetric
import com.sapiens.app.data.model.FinancialSeries
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.Elevated
import com.sapiens.app.ui.theme.MarketDown
import com.sapiens.app.ui.theme.MarketFlat
import com.sapiens.app.ui.theme.DividerOnMutedSurface
import com.sapiens.app.ui.theme.Error
import com.sapiens.app.ui.theme.ErrorContainer
import com.sapiens.app.ui.theme.MarketUp
import com.sapiens.app.ui.theme.BottomSheet
import com.sapiens.app.ui.theme.BottomSheetBottomPadding
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.SheetHorizontal
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.Success
import com.sapiens.app.ui.theme.SuccessContainer
import com.sapiens.app.ui.theme.SurfaceChartInactive
import com.sapiens.app.ui.theme.SurfaceHairlineOnDark
import com.sapiens.app.ui.theme.SurfaceMuted
import com.sapiens.app.ui.theme.SurfaceOpinion
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import com.sapiens.app.ui.theme.TextTertiary
import com.sapiens.app.ui.theme.Warning
import com.sapiens.app.ui.theme.WarningContainer
import kotlin.math.abs

private val detailTabs = listOf("기업 정보", "재무제표", "증권가 전망")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyBottomSheet(
    company: Company,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.PartiallyExpanded &&
                newValue != SheetValue.Hidden
        }
    )
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = available
        }
    }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val controller = activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        android.util.Log.d("StatusBar", "BottomSheet opened, isLight=${controller?.isAppearanceLightStatusBars}")
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            val controller = activity?.window?.let {
                WindowCompat.getInsetsController(it, it.decorView)
            }
            android.util.Log.d("StatusBar", "BottomSheet closed, isLight=${controller?.isAppearanceLightStatusBars}")
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = BottomSheet,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        properties = ModalBottomSheetProperties(
            securePolicy = SecureFlagPolicy.Inherit,
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true
        ),
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = Spacing.space6)
                    .height(Spacing.space64)
                    .padding(horizontal = SheetHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismissRequest,
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier.size(Spacing.space48)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.space24)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = TextPrimary,
                            modifier = Modifier.size(Spacing.space14)
                        )
                    }
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Card,
                contentColor = Accent,
                modifier = Modifier.fillMaxWidth()
            ) {
                detailTabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = SheetHorizontal)
                        .padding(bottom = BottomSheetBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space10)
                ) {
                    Spacer(modifier = Modifier.height(Spacing.space12))
                    when (selectedTab) {
                        0 -> CompanyInfoContent(company)
                        1 -> FinancialContent(company)
                        else -> AnalystAndOpinionContent(company)
                    }
                    Spacer(modifier = Modifier.height(Spacing.space12))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CompanyInfoContent(company: Company) {
    val info = mockCompanyInfo(company.ticker)
    val items = listOf(
        "티커" to company.ticker,
        "설립연도" to info.founded,
        "본사" to info.headquarters,
        "창립자" to info.founder,
        "현재 대표" to info.ceo,
        "주요 사업" to info.business,
        "수익 모델" to info.revenueModel,
        "2026년 전망" to info.outlook2026,
        "리스크 요인" to info.riskFactors,
        "자본력" to info.capital
    )

    Column {
        items.forEachIndexed { index, (label, value) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.space14)
            ) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.FinancialContent(company: Company) {
    val financial = MockData.companyFinancialSeries[company.ticker] ?: FinancialSeries(emptyList())
    val metricMap = financial.metrics.associateBy { it.title }
    val rows = listOf("매출", "영업이익", "순이익", "EPS", "PER", "ROE").chunked(2)
    val info = mockCompanyInfo(company.ticker)
    val health = info.financialHealth

    Text(
        text = "수익성",
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary
    )
    Column(
        modifier = Modifier.padding(top = Spacing.space8),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                rowItems.forEach { title ->
                    metricMap[title]?.let { metric ->
                        FinanceMetricCard(
                            metric = metric,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.space16))

    Text(
        text = "재무 건전성",
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary
    )

    FinancialHealthCard(
        health = health,
        modifier = Modifier.padding(top = Spacing.space8)
    )

    Spacer(modifier = Modifier.height(Spacing.space16))

    HealthSummaryCard(
        summary = info.healthSummary,
        score = info.healthScore
    )
}

@Composable
private fun FinanceMetricCard(
    metric: FinancialMetric,
    modifier: Modifier = Modifier
) {
    val y2023 = metric.values.find { it.year == "2023" }?.value ?: 0f
    val y2024 = metric.values.find { it.year == "2024" }?.value ?: 0f
    val y2025 = metric.values.find { it.year == "2025" }?.value ?: 0f
    val y2026Value = metric.values.find { it.year == "2026" }
    val y2026 = y2026Value?.value ?: 0f
    val latest = y2026Value?.displayValue ?: "-"
    val change = y2026 - y2025
    val changeColor = when {
        change > 0f -> MarketUp
        change < 0f -> MarketDown
        else -> MarketFlat
    }
    val sign = when {
        change > 0f -> "+"
        change < 0f -> "-"
        else -> ""
    }
    val changeText = if (y2025 != 0f) {
        "$sign${"%.1f".format((abs(change) / abs(y2025)) * 100f)}%"
    } else {
        "0.0%"
    }

    Column(
        modifier = modifier
            .clip(AppShapes.cardNested)
            .background(SurfaceMuted)
            .padding(start = Spacing.space12, end = Spacing.space12, top = Spacing.space14, bottom = Spacing.space10),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = metric.title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(Spacing.space4))
        Box(modifier = Modifier.height(Spacing.space36)) {
            Text(
                text = latest,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(Spacing.space4))
        Box(modifier = Modifier.height(Spacing.space20)) {
            Text(text = changeText, style = MaterialTheme.typography.labelSmall, color = changeColor)
        }
        Spacer(Modifier.height(Spacing.space8))
        VerticalBarChart(
            values = listOf(y2023, y2024, y2025, y2026),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VerticalBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val maxVal = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val years = listOf("23", "24", "25", "26")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.space48),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
        ) {
            values.forEachIndexed { index, value ->
                val ratio = (value / maxVal).coerceIn(0f, 1f)
                val barHeight = (40f * ratio).coerceAtLeast(2f).dp
                val barColor = if (index == 3) Accent else SurfaceChartInactive

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(AppShapes.barTop)
                            .background(barColor)
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.space1))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
        ) {
            years.forEachIndexed { index, year ->
                Text(
                    text = year,
                    modifier = Modifier.weight(1f),
                    style = SapiensTextStyles.statCaption9,
                    color = if (index == 3) Accent else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FinancialHealthCard(
    health: FinancialHealth,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            HealthCard(
                name = "부채비율",
                value = health.debtRatio,
                status = health.debtStatus,
                barRatio = health.debtBarRatio,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            HealthCard(
                name = "유동비율",
                value = health.currentRatio,
                status = health.currentStatus,
                barRatio = health.currentBarRatio,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            HealthCard(
                name = "자기자본비율",
                value = health.equityRatio,
                status = health.equityStatus,
                barRatio = health.equityBarRatio,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            HealthCard(
                name = "현금보유량",
                value = health.cashHolding,
                status = health.cashStatus,
                barRatio = health.cashBarRatio,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        HealthCard(
            name = "이자보상배율",
            value = health.interestCoverage,
            status = health.interestStatus,
            barRatio = health.interestBarRatio,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HealthCard(
    name: String,
    value: String,
    status: String,
    barRatio: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppShapes.cardNested)
            .background(SurfaceMuted)
            .padding(start = Spacing.space12, end = Spacing.space12, top = Spacing.space14, bottom = Spacing.space10)
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(Spacing.space4))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            HealthBadge(status)
        }
        Spacer(Modifier.height(Spacing.space6))
        Box(
            Modifier
                .fillMaxWidth()
                .height(Spacing.space4)
                .background(SurfaceHairlineOnDark, AppShapes.hairlineTrack)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(barRatio.coerceIn(0f, 1f))
                    .height(Spacing.space4)
                    .background(healthBarColor(status), AppShapes.hairlineTrack)
            )
        }
    }
}

@Composable
private fun HealthBadge(status: String) {
    val (bg, fg) = healthStatusColors(status)
    Box(
        modifier = Modifier
            .clip(AppShapes.healthCapsule)
            .background(bg)
            .padding(horizontal = Spacing.space8, vertical = Spacing.space3)
    ) {
        Text(text = status, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

private fun healthBarColor(status: String): Color = when (status) {
    "우량", "안정", "풍부" -> Success
    "주의" -> Warning
    "위험" -> Error
    else -> TextTertiary
}

@Composable
private fun HealthSummaryCard(
    summary: String,
    score: Int
) {
    val safeScore = score.coerceIn(0, 5)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.panel)
            .background(SurfaceOpinion)
            .border(Spacing.hairline, Accent.copy(alpha = 0.2f), AppShapes.panel)
            .padding(horizontal = Spacing.space16, vertical = Spacing.space18),
        verticalArrangement = Arrangement.spacedBy(Spacing.space10)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "재무 건전성 종합",
                style = MaterialTheme.typography.labelSmall,
                color = Accent
            )
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = (MaterialTheme.typography.bodySmall.fontSize.value * 1.75f).sp
            ),
            color = TextSecondary
        )
        HorizontalDivider(color = Accent.copy(alpha = 0.15f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("건전성 점수", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("$safeScore / 5", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

private fun healthStatusColors(status: String): Pair<Color, Color> = when (status) {
    "우량", "안정", "풍부" -> SuccessContainer to Success
    "주의" -> WarningContainer to Warning
    "위험" -> ErrorContainer to Error
    else -> WarningContainer to Warning
}

@Composable
private fun ColumnScope.AnalystAndOpinionContent(company: Company) {
    Text("증권가 전망", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Spacer(modifier = Modifier.height(Spacing.space8))
    val analystComments = mockAnalystComments(company.ticker)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.cardNested)
            .background(SurfaceMuted)
            .padding(Spacing.space14)
    ) {
        analystComments.forEachIndexed { index, comment ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.space10),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                Text(
                    text = "${comment.source} · ${comment.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = comment.comment,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = (MaterialTheme.typography.bodyMedium.fontSize.value * 1.5f).sp
                    ),
                    color = TextPrimary
                )
            }
            if (index != analystComments.lastIndex) {
                HorizontalDivider(color = DividerOnMutedSurface)
                Spacer(modifier = Modifier.height(Spacing.space10))
            }
        }
    }
    Spacer(modifier = Modifier.height(Spacing.space12))
    val opinion = mockOpinion(company.ticker)
    OpinionRatioBar(opinion)
}

@Composable
private fun OpinionRatioBar(opinion: Opinion) {
    val buyColor = MarketUp
    val holdColor = TextTertiary
    val sellColor = MarketDown
    val buy = opinion.buy.removeSuffix("%").toFloatOrNull() ?: 0f
    val hold = opinion.hold.removeSuffix("%").toFloatOrNull() ?: 0f
    val sell = opinion.sell.removeSuffix("%").toFloatOrNull() ?: 0f
    val total = (buy + hold + sell).takeIf { it > 0f } ?: 1f
    val hasPositive = buy > 0f || hold > 0f || sell > 0f
    val buyWeight = if (hasPositive) buy / total else 1f
    val holdWeight = if (hasPositive) hold / total else 1f
    val sellWeight = if (hasPositive) sell / total else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.panel)
            .background(SurfaceOpinion)
            .border(Spacing.hairline, Accent.copy(alpha = 0.2f), AppShapes.panel)
            .padding(horizontal = Spacing.space16, vertical = Spacing.space14),
        verticalArrangement = Arrangement.spacedBy(Spacing.space10)
    ) {
        Text("매수·매도 의견", style = MaterialTheme.typography.labelSmall, color = Accent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.space6)
                .clip(AppShapes.barSegment)
        ) {
            Box(Modifier.weight(buyWeight).fillMaxHeight().background(buyColor))
            Box(Modifier.weight(holdWeight).fillMaxHeight().background(holdColor))
            Box(Modifier.weight(sellWeight).fillMaxHeight().background(sellColor))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("매수 ${opinion.buy}", style = MaterialTheme.typography.labelSmall, color = buyColor)
            Text("중립 ${opinion.hold}", style = MaterialTheme.typography.labelSmall, color = holdColor)
            Text("매도 ${opinion.sell}", style = MaterialTheme.typography.labelSmall, color = sellColor)
        }
        HorizontalDivider(color = Accent.copy(alpha = 0.15f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("목표주가", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                opinion.targetPrice,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "AI 의견 요약: ${opinion.aiSummary}",
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = (MaterialTheme.typography.bodySmall.fontSize.value * 1.7f).sp
            ),
            color = TextSecondary
        )
    }
}

private data class CompanyInfo(
    val founded: String,
    val headquarters: String,
    val founder: String,
    val ceo: String,
    val business: String,
    val revenueModel: String,
    val outlook2026: String,
    val riskFactors: String,
    val capital: String,
    val financialHealth: FinancialHealth,
    val healthSummary: String,
    val healthScore: Int
)
private data class FinancialHealth(
    val debtRatio: String,
    val currentRatio: String,
    val equityRatio: String,
    val cashHolding: String,
    val interestCoverage: String,
    val debtStatus: String,
    val currentStatus: String,
    val equityStatus: String,
    val cashStatus: String,
    val interestStatus: String,
    val debtBarRatio: Float,
    val currentBarRatio: Float,
    val equityBarRatio: Float,
    val cashBarRatio: Float,
    val interestBarRatio: Float
)
private data class AnalystComment(val source: String, val date: String, val comment: String)
private data class Opinion(
    val buy: String,
    val hold: String,
    val sell: String,
    val targetPrice: String,
    val aiSummary: String
)

private fun mockCompanyInfo(ticker: String): CompanyInfo = when (ticker) {
    "005930" -> CompanyInfo(
        "1969년", "대한민국 수원", "이병철", "전영현",
        "메모리·시스템 반도체 중심 전자기업", "반도체와 MX·가전 판매",
        "HBM 비중 확대로 실적 개선 기대", "업황 변동성과 감가 부담",
        "시가총액 520조 · 부채 26% · 현금 115조",
        financialHealthByTicker("005930"),
        healthSummaryByTicker("005930"),
        healthScoreByTicker("005930")
    )
    "000660" -> CompanyInfo(
        "1983년", "대한민국 이천", "현대전자산업", "곽노정",
        "DRAM·NAND 중심 메모리 기업", "메모리 칩 판매",
        "AI 수요로 HBM 성장 기대", "메모리 가격 사이클 리스크",
        "시가총액 145조 · 부채 38% · 현금 16조",
        financialHealthByTicker("000660"),
        healthSummaryByTicker("000660"),
        healthScoreByTicker("000660")
    )
    "NVDA" -> CompanyInfo(
        "1993년", "미국 산타클라라", "젠슨 황 외", "젠슨 황",
        "데이터센터 GPU·AI 인프라", "GPU·시스템 판매",
        "AI 확산에 성장세 지속 전망", "고객 집중·규제 리스크",
        "시가총액 $3.2T · 부채 29% · 현금 $41B",
        financialHealthByTicker("NVDA"),
        healthSummaryByTicker("NVDA"),
        healthScoreByTicker("NVDA")
    )
    "035420" -> CompanyInfo(
        "1999년", "대한민국 성남", "이해진", "최수연",
        "검색·커머스·핀테크 플랫폼", "광고·커머스 수수료",
        "AI 상용화로 수익성 개선 기대", "광고경기·규제 리스크",
        "시가총액 34조 · 부채 42% · 현금 7.5조",
        financialHealthByTicker("035420"),
        healthSummaryByTicker("035420"),
        healthScoreByTicker("035420")
    )
    "AAPL" -> CompanyInfo(
        "1976년", "미국 쿠퍼티노", "스티브 잡스 외", "팀 쿡",
        "하드웨어+서비스 생태계", "기기 판매+서비스 구독",
        "온디바이스 AI로 교체 수요 기대", "중국판매·규제 리스크",
        "시가총액 $2.9T · 부채 161% · 현금 $67B",
        financialHealthByTicker("AAPL"),
        healthSummaryByTicker("AAPL"),
        healthScoreByTicker("AAPL")
    )
    "051910" -> CompanyInfo(
        "1947년", "대한민국 서울", "구인회", "신학철",
        "석유화학·첨단소재", "기초소재·소재 공급",
        "배터리 소재 회복 기대", "원재료 변동·경기 리스크",
        "시가총액 26조 · 부채 87% · 현금 3.2조",
        financialHealthByTicker("051910"),
        healthSummaryByTicker("051910"),
        healthScoreByTicker("051910")
    )
    else -> CompanyInfo(
        "2003년", "미국 오스틴", "마틴 에버하드 외", "일론 머스크",
        "전기차·에너지 통합", "차량 판매+소프트웨어",
        "저가형 모델·에너지 성장 기대", "가격경쟁·규제 리스크",
        "시가총액 $710B · 부채 18% · 현금 $29B",
        financialHealthByTicker("TSLA"),
        healthSummaryByTicker("TSLA"),
        healthScoreByTicker("TSLA")
    )
}

private fun financialHealthByTicker(ticker: String): FinancialHealth = when (ticker) {
    "005930" -> FinancialHealth(
        debtRatio = "26%", currentRatio = "198%", equityRatio = "79%",
        cashHolding = "115조", interestCoverage = "34.2배",
        debtStatus = "우량", currentStatus = "안정", equityStatus = "우량", cashStatus = "풍부", interestStatus = "우량",
        debtBarRatio = 0.82f, currentBarRatio = 0.78f, equityBarRatio = 0.85f, cashBarRatio = 0.90f, interestBarRatio = 0.88f
    )
    "000660" -> FinancialHealth(
        debtRatio = "38%", currentRatio = "152%", equityRatio = "68%",
        cashHolding = "16조", interestCoverage = "18.4배",
        debtStatus = "안정", currentStatus = "안정", equityStatus = "안정", cashStatus = "풍부", interestStatus = "안정",
        debtBarRatio = 0.70f, currentBarRatio = 0.69f, equityBarRatio = 0.71f, cashBarRatio = 0.74f, interestBarRatio = 0.72f
    )
    "NVDA" -> FinancialHealth(
        debtRatio = "29%", currentRatio = "275%", equityRatio = "72%",
        cashHolding = "$41B", interestCoverage = "41.8배",
        debtStatus = "우량", currentStatus = "풍부", equityStatus = "우량", cashStatus = "풍부", interestStatus = "우량",
        debtBarRatio = 0.80f, currentBarRatio = 0.92f, equityBarRatio = 0.80f, cashBarRatio = 0.86f, interestBarRatio = 0.91f
    )
    "035420" -> FinancialHealth(
        debtRatio = "42%", currentRatio = "131%", equityRatio = "64%",
        cashHolding = "7.5조", interestCoverage = "11.2배",
        debtStatus = "안정", currentStatus = "안정", equityStatus = "안정", cashStatus = "안정", interestStatus = "안정",
        debtBarRatio = 0.66f, currentBarRatio = 0.60f, equityBarRatio = 0.65f, cashBarRatio = 0.61f, interestBarRatio = 0.62f
    )
    "AAPL" -> FinancialHealth(
        debtRatio = "161%", currentRatio = "94%", equityRatio = "28%",
        cashHolding = "$67B", interestCoverage = "9.1배",
        debtStatus = "주의", currentStatus = "주의", equityStatus = "주의", cashStatus = "풍부", interestStatus = "안정",
        debtBarRatio = 0.34f, currentBarRatio = 0.45f, equityBarRatio = 0.38f, cashBarRatio = 0.82f, interestBarRatio = 0.56f
    )
    "051910" -> FinancialHealth(
        debtRatio = "87%", currentRatio = "118%", equityRatio = "53%",
        cashHolding = "3.2조", interestCoverage = "6.7배",
        debtStatus = "주의", currentStatus = "안정", equityStatus = "주의", cashStatus = "안정", interestStatus = "주의",
        debtBarRatio = 0.46f, currentBarRatio = 0.55f, equityBarRatio = 0.50f, cashBarRatio = 0.48f, interestBarRatio = 0.42f
    )
    else -> FinancialHealth(
        debtRatio = "18%", currentRatio = "166%", equityRatio = "81%",
        cashHolding = "$29B", interestCoverage = "21.4배",
        debtStatus = "우량", currentStatus = "안정", equityStatus = "우량", cashStatus = "풍부", interestStatus = "우량",
        debtBarRatio = 0.86f, currentBarRatio = 0.73f, equityBarRatio = 0.87f, cashBarRatio = 0.72f, interestBarRatio = 0.79f
    )
}

private fun healthSummaryByTicker(ticker: String): String = when (ticker) {
    "005930" -> "현금여력과 이자상환 능력이 모두 우수해 대규모 투자 사이클에서도 재무 안정성이 높습니다. 업황 변동성은 존재하지만 구조적 하방 방어력이 강한 편입니다."
    "000660" -> "부채 부담은 관리 가능한 수준이며 유동성과 상환능력도 안정 구간을 유지합니다. 메모리 경기 변동에 따라 단기 지표 흔들림은 가능하나 전반적 체력은 양호합니다."
    "NVDA" -> "현금흐름과 이자보상배율이 매우 우량해 성장 투자와 주주환원 병행 여력이 충분합니다. 단기 수요 변동에도 재무 건전성 훼손 가능성은 낮은 구조입니다."
    "035420" -> "전반적 지표는 안정 구간이지만 성장 투자 확대 시 비용 선반영 구간에서 수익성 변동이 나타날 수 있습니다. 보수적 현금 운용이 중기 안정성에 핵심입니다."
    "AAPL" -> "자본구조상 레버리지는 높은 편이지만 막대한 현금창출력으로 상환 리스크를 낮추고 있습니다. 규제 및 수요 둔화 국면에서 지표 점검이 필요한 구간입니다."
    "051910" -> "부채와 이자보상 지표는 주의 영역에 가까워 업황 회복 지연 시 부담이 커질 수 있습니다. 다만 현금흐름 개선이 확인되면 건전성 회복 속도는 빨라질 수 있습니다."
    else -> "낮은 부채와 안정적인 유동성으로 재무 안전판이 견고한 편입니다. 투자 집행이 커져도 단기 유동성 압박 가능성은 제한적이며, 건전성 점수는 상위 구간으로 평가됩니다."
}

private fun healthScoreByTicker(ticker: String): Int = when (ticker) {
    "005930" -> 5
    "000660" -> 4
    "NVDA" -> 5
    "035420" -> 4
    "AAPL" -> 3
    "051910" -> 3
    else -> 5
}

private fun mockAnalystComments(ticker: String): List<AnalystComment> = when (ticker) {
    "005930" -> listOf(
        AnalystComment("NH투자증권", "2026-04-18", "HBM 공급 확대 여부가 실적 상향의 핵심"),
        AnalystComment("한국투자증권", "2026-04-17", "메모리 가격 정상화가 하반기 이익 모멘텀")
    )
    "000660" -> listOf(
        AnalystComment("KB증권", "2026-04-18", "고대역폭 메모리 수익성 개선 구간"),
        AnalystComment("미래에셋증권", "2026-04-17", "AI 서버 증설 사이클의 직접 수혜")
    )
    "NVDA" -> listOf(
        AnalystComment("Morgan Stanley", "2026-04-18", "데이터센터 CAPEX 둔화 여부 점검 필요"),
        AnalystComment("Goldman Sachs", "2026-04-16", "소프트웨어 매출 비중 확대가 밸류 지지")
    )
    else -> listOf(
        AnalystComment("JP Morgan", "2026-04-18", "핵심 제품군 수요와 마진 추세 점검 필요"),
        AnalystComment("BofA", "2026-04-17", "투자 사이클 재가속 여부가 주가의 분기점")
    )
}

private fun mockOpinion(ticker: String): Opinion = when (ticker) {
    "005930" -> Opinion("67%", "26%", "7%", "92,000원", "단기 변동성은 있으나 중기 실적 개선 방향이 유효합니다.")
    "000660" -> Opinion("71%", "22%", "7%", "245,000원", "AI 수요 지속 시 업황 상단 테스트 가능성이 있습니다.")
    "NVDA" -> Opinion("63%", "28%", "9%", "$1,080", "밸류 부담 구간이지만 구조적 성장 스토리는 견고합니다.")
    "035420" -> Opinion("52%", "35%", "13%", "230,000원", "이익 체력은 안정적이나 성장 재가속 신호 확인이 필요합니다.")
    "AAPL" -> Opinion("58%", "33%", "9%", "$245", "서비스 매출이 하방을 방어하며 신제품 사이클이 변수입니다.")
    "051910" -> Opinion("49%", "38%", "13%", "480,000원", "소재 업황 반등 시 탄력적이지만 단기 실적 가시성은 제한적입니다.")
    else -> Opinion("46%", "34%", "20%", "$220", "변동성은 높지만 모빌리티 플랫폼 프리미엄이 일부 반영되어 있습니다.")
}
