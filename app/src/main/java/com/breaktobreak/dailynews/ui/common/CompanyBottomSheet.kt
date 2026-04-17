package com.breaktobreak.dailynews.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.model.Company
import com.breaktobreak.dailynews.ui.company.SectorChip
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.Elevated
import com.breaktobreak.dailynews.ui.theme.MarketDown
import com.breaktobreak.dailynews.ui.theme.MarketFlat
import com.breaktobreak.dailynews.ui.theme.MarketUp
import com.breaktobreak.dailynews.ui.theme.SheetHorizontal
import com.breaktobreak.dailynews.ui.theme.SheetTop
import com.breaktobreak.dailynews.ui.theme.RowVertical
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

private val detailTabs = listOf("기업 정보", "재무제표", "증권가 전망")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyBottomSheet(
    company: Company,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = Card,
        dragHandle = null,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextSecondary.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetHorizontal, vertical = SheetTop),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = company.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = company.ticker,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                SectorChip(label = company.sector)
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

            Box(modifier = Modifier.weight(1f)) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = SheetHorizontal)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(SheetTop))
                    when (selectedTab) {
                        0 -> CompanyInfoContent(company)
                        1 -> FinancialContent(company)
                        else -> AnalystAndOpinionContent(company)
                    }
                    Spacer(modifier = Modifier.height(SheetTop))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CompanyInfoContent(company: Company) {
    val info = mockCompanyInfo(company.ticker)
    val rows = listOf(
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

    Column(verticalArrangement = Arrangement.spacedBy(RowVertical)) {
        rows.forEachIndexed { index, (label, value) ->
            CompanyInfoRow(label = label, value = value)
            if (index < rows.lastIndex) {
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
private fun CompanyInfoRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun ColumnScope.FinancialContent(company: Company) {
    val financial = mockFinancialSeries(company.ticker)
    financial.metrics.forEachIndexed { index, metric ->
        Text(
            text = metric.title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val maxValue = metric.values.maxOfOrNull { it.value } ?: 0f
            metric.values.forEach { item ->
                FinancialBarRow(
                    item = item,
                    maxValue = maxValue,
                    barColor = if (item.year == "2026") Accent else Elevated
                )
            }
        }

        if (index < financial.metrics.lastIndex) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextSecondary.copy(alpha = 0.25f)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
        color = TextSecondary.copy(alpha = 0.25f)
    )
    FinancialMetricCaption()
}

@Composable
private fun FinancialBarRow(
    item: YearFinancialValue,
    maxValue: Float,
    barColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.year,
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
        ) {
            val ratio = if (maxValue <= 0f) 0f else (item.value / maxValue).coerceIn(0f, 1f)
            drawRoundRect(
                color = barColor,
                size = Size(width = size.width * ratio, height = size.height),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.displayValue,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun FinancialMetricCaption() {
    val captions = listOf(
        "매출" to "기업이 벌어들인 총 수익",
        "영업이익" to "본업으로 번 돈 (매출 - 영업비용)",
        "순이익" to "세금·이자 제외 후 최종 남은 돈",
        "EPS" to "주식 1주당 순이익",
        "PER" to "주가 ÷ EPS, 고평가·저평가 판단 지표",
        "ROE" to "자기자본 대비 순이익률, 경영 효율성"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        captions.forEach { (title, description) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent
                )
                Text(
                    text = "· $description",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.AnalystAndOpinionContent(company: Company) {
    Text("증권가 전망", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    mockAnalystComments(company.ticker).forEach { comment ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${comment.source} · ${comment.date}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = comment.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = TextSecondary.copy(alpha = 0.25f)
    )

    Text("매수·매도 의견", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    val opinion = mockOpinion(company.ticker)
    OpinionRatioBar(opinion = opinion)
    Text("목표주가: ${opinion.targetPrice}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    Text(
        text = "AI 의견 요약: ${opinion.aiSummary}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
}

@Composable
private fun OpinionRatioBar(opinion: Opinion) {
    val buy = opinion.buy.removeSuffix("%").toFloatOrNull() ?: 0f
    val hold = opinion.hold.removeSuffix("%").toFloatOrNull() ?: 0f
    val sell = opinion.sell.removeSuffix("%").toFloatOrNull() ?: 0f
    val total = (buy + hold + sell).takeIf { it > 0f } ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(99.dp))
    ) {
        Box(
            modifier = Modifier
                .weight(buy / total)
                .fillMaxWidth()
                .background(MarketUp)
        )
        Box(
            modifier = Modifier
                .weight(hold / total)
                .fillMaxWidth()
                .background(MarketFlat)
        )
        Box(
            modifier = Modifier
                .weight(sell / total)
                .fillMaxWidth()
                .background(MarketDown)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("매수 ${opinion.buy}", style = MaterialTheme.typography.labelSmall, color = MarketUp)
        Text("중립 ${opinion.hold}", style = MaterialTheme.typography.labelSmall, color = MarketFlat)
        Text("매도 ${opinion.sell}", style = MaterialTheme.typography.labelSmall, color = MarketDown)
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
    val capital: String
)
private data class YearFinancialValue(val year: String, val value: Float, val displayValue: String)
private data class FinancialMetric(val title: String, val values: List<YearFinancialValue>)
private data class FinancialSeries(val metrics: List<FinancialMetric>)
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
        founded = "1969년",
        headquarters = "대한민국 수원",
        founder = "이병철",
        ceo = "전영현 (DX·DS 총괄)",
        business = "메모리·시스템 반도체, 스마트폰, TV·가전, 파운드리까지 아우르는 종합 전자 기업입니다.",
        revenueModel = "반도체와 MX·가전 판매가 매출의 축이며, 프리미엄 제품 믹스와 B2B 부품 공급으로 수익성을 확보합니다.",
        outlook2026 = "HBM과 고부가 메모리 비중 확대, 첨단 공정 전환이 동반되면 이익 회복 속도가 빨라질 가능성이 큽니다.",
        riskFactors = "메모리 업황 변동성, 지정학 리스크, 대규모 CAPEX에 따른 감가 부담이 단기 실적 변동 요인입니다.",
        capital = "시가총액 약 520조원 · 부채비율 약 26% · 현금보유량 약 115조원"
    )
    "000660" -> CompanyInfo(
        founded = "1983년",
        headquarters = "대한민국 이천",
        founder = "현대전자산업 (현대그룹 계열로 출범)",
        ceo = "곽노정",
        business = "DRAM·NAND 중심 메모리 반도체를 개발·생산하며 AI 서버용 고대역폭 메모리 공급을 확대하고 있습니다.",
        revenueModel = "메모리 칩 판매가 핵심이며, 고성능 제품 ASP 상승과 장기 공급계약이 수익성 개선을 견인합니다.",
        outlook2026 = "AI 인프라 투자 지속 시 HBM 판매 증가와 제품 믹스 고도화로 영업 레버리지 확대가 기대됩니다.",
        riskFactors = "메모리 가격 하락 사이클 재진입, 고객사 CAPEX 조정, 공정 전환 수율 리스크가 주요 변수입니다.",
        capital = "시가총액 약 145조원 · 부채비율 약 38% · 현금보유량 약 16조원"
    )
    "NVDA" -> CompanyInfo(
        founded = "1993년",
        headquarters = "미국 캘리포니아 산타클라라",
        founder = "젠슨 황, 크리스 말라초스키, 커티스 프리엠",
        ceo = "젠슨 황",
        business = "데이터센터 GPU, AI 가속기, 네트워킹, 소프트웨어 플랫폼(CUDA)을 결합한 AI 인프라 생태계를 운영합니다.",
        revenueModel = "GPU·시스템 판매가 중심이며, 소프트웨어 생태계 락인과 하이엔드 제품 프리미엄으로 높은 마진을 유지합니다.",
        outlook2026 = "차세대 아키텍처 출시와 엔터프라이즈 AI 확산이 이어지면 매출 성장세가 추가 연장될 가능성이 있습니다.",
        riskFactors = "대형 고객 의존도, 수출 규제, 경쟁사 ASIC 확산이 성장률 둔화 압력으로 작용할 수 있습니다.",
        capital = "시가총액 약 $3.2T · 부채비율 약 29% · 현금보유량 약 $41B"
    )
    "035420" -> CompanyInfo(
        founded = "1999년",
        headquarters = "대한민국 성남",
        founder = "이해진",
        ceo = "최수연",
        business = "검색·광고, 커머스, 핀테크, 콘텐츠, 클라우드 및 AI 서비스를 중심으로 플랫폼 사업을 전개합니다.",
        revenueModel = "광고와 커머스 수수료, 핀테크 결제 수익, 구독·콘텐츠 매출이 결합된 다각화된 수익 구조입니다.",
        outlook2026 = "생성형 AI 서비스의 상용화와 커머스 효율 개선이 맞물리면 이익률 개선 여지가 존재합니다.",
        riskFactors = "국내 광고 경기 둔화, 플랫폼 규제 강화, 신사업 투자비 증가가 단기 수익성에 부담이 될 수 있습니다.",
        capital = "시가총액 약 34조원 · 부채비율 약 42% · 현금보유량 약 7.5조원"
    )
    "AAPL" -> CompanyInfo(
        founded = "1976년",
        headquarters = "미국 캘리포니아 쿠퍼티노",
        founder = "스티브 잡스, 스티브 워즈니악, 로널드 웨인",
        ceo = "팀 쿡",
        business = "iPhone·Mac·iPad·Wearable 하드웨어와 App Store, iCloud, Music 등 서비스 생태계를 통합 운영합니다.",
        revenueModel = "하드웨어 판매와 고마진 서비스 구독 매출이 결합되어 안정적인 현금창출 구조를 형성합니다.",
        outlook2026 = "온디바이스 AI 경험 강화와 서비스 ARPU 상승이 동반되면 완만한 이익 성장 경로가 유지될 가능성이 높습니다.",
        riskFactors = "중국 판매 변동성, 반독점 규제, 공급망 리스크가 밸류에이션 프리미엄 축소 요인이 될 수 있습니다.",
        capital = "시가총액 약 $2.9T · 부채비율 약 161% · 현금보유량 약 $67B"
    )
    "051910" -> CompanyInfo(
        founded = "1947년",
        headquarters = "대한민국 서울",
        founder = "락희화학공업사 (구인회 창업)",
        ceo = "신학철",
        business = "석유화학, 첨단소재, 배터리 소재(양극재·분리막) 중심으로 포트폴리오를 전환하고 있습니다.",
        revenueModel = "기초소재 판매와 장기 공급 계약 기반 첨단소재 매출이 결합되며, 제품 스프레드가 수익성의 핵심입니다.",
        outlook2026 = "배터리 소재 수요 회복과 고부가 제품 비중 확대 시 실적 반등 폭이 커질 수 있습니다.",
        riskFactors = "원재료 가격 변동, 글로벌 경기 둔화, 신규 설비 가동 초기 비용 부담이 주요 리스크입니다.",
        capital = "시가총액 약 26조원 · 부채비율 약 87% · 현금보유량 약 3.2조원"
    )
    else -> CompanyInfo(
        founded = "2003년",
        headquarters = "미국 텍사스 오스틴",
        founder = "마틴 에버하드, 마크 타페닝 (이후 일론 머스크 합류)",
        ceo = "일론 머스크",
        business = "전기차, 에너지 저장장치, 충전 인프라, 자율주행 소프트웨어를 통합한 모빌리티·에너지 기업입니다.",
        revenueModel = "차량 판매가 핵심이며, 에너지 저장·소프트웨어 옵션(FSD)·크레딧 판매가 부가 수익원으로 작동합니다.",
        outlook2026 = "저가형 모델 확대와 에너지 사업 성장, 소프트웨어 매출 기여 확대가 실적 레버리지 요인으로 기대됩니다.",
        riskFactors = "가격 인하 경쟁, 자율주행 규제, 생산 차질 및 원가 변동성이 마진 압박으로 이어질 수 있습니다.",
        capital = "시가총액 약 $710B · 부채비율 약 18% · 현금보유량 약 $29B"
    )
}

private fun mockFinancialSeries(ticker: String): FinancialSeries = when (ticker) {
    "005930" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 300f to "300조원", 320f to "320조원", 340f to "340조원"),
            metric("영업이익", 2.8f to "2.8조원", 3.0f to "3조원", 3.4f to "3.4조원"),
            metric("순이익", 2.1f to "2.1조원", 2.4f to "2.4조원", 2.7f to "2.7조원"),
            metric("EPS", 3200f to "3,200원", 3600f to "3,600원", 4100f to "4,100원"),
            metric("PER", 16.9f to "16.9배", 17.6f to "17.6배", 18.4f to "18.4배"),
            metric("ROE", 9.6f to "9.6%", 10.8f to "10.8%", 12.1f to "12.1%")
        )
    )
    "NVDA" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 61f to "$61B", 130f to "$130B", 195f to "$195B"),
            metric("영업이익", 33f to "$33B", 82f to "$82B", 124f to "$124B"),
            metric("순이익", 30f to "$30B", 73f to "$73B", 112f to "$112B"),
            metric("EPS", 1.2f to "$1.2", 2.9f to "$2.9", 4.5f to "$4.5"),
            metric("PER", 44.5f to "44.5배", 48.2f to "48.2배", 52.1f to "52.1배"),
            metric("ROE", 51.0f to "51.0%", 57.4f to "57.4%", 61.8f to "61.8%")
        )
    )
    "000660" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 46f to "46조원", 52f to "52조원", 58f to "58조원"),
            metric("영업이익", 8.4f to "8.4조원", 10.1f to "10.1조원", 12.2f to "12.2조원"),
            metric("순이익", 6.8f to "6.8조원", 8.0f to "8조원", 9.6f to "9.6조원"),
            metric("EPS", 12000f to "12,000원", 14200f to "14,200원", 16800f to "16,800원"),
            metric("PER", 11.8f to "11.8배", 13.2f to "13.2배", 14.6f to "14.6배"),
            metric("ROE", 14.2f to "14.2%", 15.9f to "15.9%", 17.3f to "17.3%")
        )
    )
    "035420" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 10.8f to "10.8조원", 11.5f to "11.5조원", 12.6f to "12.6조원"),
            metric("영업이익", 1.6f to "1.6조원", 1.8f to "1.8조원", 2.1f to "2.1조원"),
            metric("순이익", 1.2f to "1.2조원", 1.35f to "1.35조원", 1.55f to "1.55조원"),
            metric("EPS", 5600f to "5,600원", 6300f to "6,300원", 7100f to "7,100원"),
            metric("PER", 20.6f to "20.6배", 21.4f to "21.4배", 22.7f to "22.7배"),
            metric("ROE", 7.9f to "7.9%", 8.6f to "8.6%", 9.4f to "9.4%")
        )
    )
    "AAPL" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 391f to "$391B", 408f to "$408B", 427f to "$427B"),
            metric("영업이익", 121f to "$121B", 126f to "$126B", 132f to "$132B"),
            metric("순이익", 97f to "$97B", 101f to "$101B", 106f to "$106B"),
            metric("EPS", 6.4f to "$6.4", 6.8f to "$6.8", 7.2f to "$7.2"),
            metric("PER", 27.5f to "27.5배", 28.1f to "28.1배", 29.0f to "29.0배"),
            metric("ROE", 152f to "152%", 156f to "156%", 161f to "161%")
        )
    )
    "051910" -> FinancialSeries(
        metrics = listOf(
            metric("매출", 49f to "49조원", 53f to "53조원", 58f to "58조원"),
            metric("영업이익", 2.9f to "2.9조원", 3.4f to "3.4조원", 4.1f to "4.1조원"),
            metric("순이익", 2.0f to "2조원", 2.5f to "2.5조원", 3.1f to "3.1조원"),
            metric("EPS", 9800f to "9,800원", 11300f to "11,300원", 12900f to "12,900원"),
            metric("PER", 15.6f to "15.6배", 16.8f to "16.8배", 18.1f to "18.1배"),
            metric("ROE", 6.2f to "6.2%", 7.5f to "7.5%", 8.9f to "8.9%")
        )
    )
    else -> FinancialSeries(
        metrics = listOf(
            metric("매출", 97f to "$97B", 112f to "$112B", 128f to "$128B"),
            metric("영업이익", 8.1f to "$8.1B", 10.4f to "$10.4B", 13.0f to "$13.0B"),
            metric("순이익", 6.7f to "$6.7B", 8.6f to "$8.6B", 10.9f to "$10.9B"),
            metric("EPS", 3.2f to "$3.2", 4.0f to "$4.0", 4.9f to "$4.9"),
            metric("PER", 42.7f to "42.7배", 49.3f to "49.3배", 56.1f to "56.1배"),
            metric("ROE", 14.3f to "14.3%", 16.7f to "16.7%", 19.8f to "19.8%")
        )
    )
}

private fun metric(
    title: String,
    y2024: Pair<Float, String>,
    y2025: Pair<Float, String>,
    y2026: Pair<Float, String>
): FinancialMetric = FinancialMetric(
    title = title,
    values = listOf(
        YearFinancialValue("2024", y2024.first, y2024.second),
        YearFinancialValue("2025", y2025.first, y2025.second),
        YearFinancialValue("2026", y2026.first, y2026.second)
    )
)

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
    "035420" -> listOf(
        AnalystComment("삼성증권", "2026-04-18", "광고·커머스 회복이 단기 주가 변수"),
        AnalystComment("신한투자증권", "2026-04-15", "AI 서비스의 수익화 속도 관찰 필요")
    )
    "AAPL" -> listOf(
        AnalystComment("JP Morgan", "2026-04-18", "서비스 매출 성장률이 프리미엄 유지 포인트"),
        AnalystComment("Wedbush", "2026-04-17", "온디바이스 AI 전략이 교체 수요를 자극할 가능성")
    )
    "051910" -> listOf(
        AnalystComment("메리츠증권", "2026-04-18", "배터리 소재 스프레드 개선이 관건"),
        AnalystComment("하나증권", "2026-04-16", "CAPEX 집행 강도와 수익성 균형 중요")
    )
    else -> listOf(
        AnalystComment("Barclays", "2026-04-18", "가격 정책이 마진에 미치는 영향 주시"),
        AnalystComment("BofA", "2026-04-17", "자율주행 소프트웨어 상용화 일정이 핵심")
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
