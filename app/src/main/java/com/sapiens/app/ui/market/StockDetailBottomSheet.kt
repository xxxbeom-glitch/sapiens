package com.sapiens.app.ui.market

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.sapiens.app.data.stock.FinancialYearRow
import com.sapiens.app.data.stock.StockDetailUi
import com.sapiens.app.data.stock.StockDetailUiState
import com.sapiens.app.data.stock.StockNewsItem
import com.sapiens.app.data.stock.StockReportItem
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.PdfViewerDialog
import com.sapiens.app.ui.news.NewsFeedRow
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.BottomSheetBottomPadding
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingBottom
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.CardPaddingVertical
import com.sapiens.app.ui.theme.CardSpacing
import com.sapiens.app.ui.theme.ContentAlpha
import com.sapiens.app.ui.theme.MarketDown
import com.sapiens.app.ui.theme.MarketFlat
import com.sapiens.app.ui.theme.MarketUp
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.SheetHorizontal
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.SurfaceMuted
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

@Composable
fun StockDetailBottomSheet(
    stockCode: String,
    viewModel: StockDetailViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pdfViewerState by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(stockCode) {
        viewModel.load(stockCode)
    }

    pdfViewerState?.let { (pdfUrl, pdfTitle) ->
        PdfViewerDialog(
            url = pdfUrl,
            title = pdfTitle,
            onDismiss = { pdfViewerState = null },
        )
    }

    fun openUrl(url: String) {
        val u = url.trim()
        if (u.isEmpty()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        }
    }

    Dialog(
        onDismissRequest = {
            pdfViewerState = null
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                when (val s = uiState) {
                    StockDetailUiState.Idle,
                    StockDetailUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = Accent,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            StockDetailCloseIconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = RowVertical)
                                    .padding(horizontal = SheetHorizontal)
                            )
                        }
                    }
                    is StockDetailUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = s.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = SheetHorizontal)
                            )
                            StockDetailCloseIconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = RowVertical)
                                    .padding(horizontal = SheetHorizontal)
                            )
                        }
                    }
                    is StockDetailUiState.Success -> {
                        StockDetailContent(
                            modifier = Modifier.weight(1f),
                            data = s.data,
                            onOpenUrl = ::openUrl,
                            onOpenReportPdf = { url, title ->
                                pdfViewerState = url to title
                            },
                            onDismissRequest = onDismissRequest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDetailCloseIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.space40)
                .clip(CircleShape)
                .background(TextSecondary.copy(alpha = ContentAlpha.iconGhost)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = TextPrimary,
                modifier = Modifier.size(Spacing.space18)
            )
        }
    }
}

@Composable
private fun rememberSvgImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}

@Composable
private fun StockDetailContent(
    data: StockDetailUi,
    onOpenUrl: (String) -> Unit,
    onOpenReportPdf: (String, String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val svgLoader = rememberSvgImageLoader()
    val logoUrl =
        "https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${data.code}.svg"
    val changeColor = when (data.isRising) {
        true -> MarketUp
        false -> MarketDown
        null -> MarketFlat
    }
    val dividerColor = TextSecondary.copy(alpha = ContentAlpha.hairlineOnSecondary)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SheetHorizontal)
                .padding(top = RowVertical)
        ) {
            StockDetailCloseIconButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Spacing.space48)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = data.stockName,
                        imageLoader = svgLoader,
                        modifier = Modifier
                            .size(Spacing.space52)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = data.stockName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = data.exchangeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = Spacing.space2)
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = CardSpacing),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space10)
                ) {
                    Text(
                        text = data.closePrice,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = data.fluctuationsRatio,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = changeColor
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = Spacing.hairline,
            color = dividerColor,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.space24))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SheetHorizontal),
            contentPadding = PaddingValues(bottom = BottomSheetBottomPadding)
        ) {
            item {
                Spacer(Modifier.height(CardSpacing))
                Text(
                    text = "투자 정보",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(CardSpacing))
                InvestmentMetricCardGrid(
                    metrics = listOf(
                        "시가총액" to data.marketCap,
                        "PER" to data.per,
                        "PBR" to data.pbr,
                        "EPS" to data.eps,
                        "배당수익률" to data.dividendYield,
                        "주당배당금" to data.dps,
                    )
                )
            }
            if (data.financialYears.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(CardSpacing))
                    Text(
                        text = "매출·영업이익 (연간)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (data.financialValueUnit.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.space4))
                        Text(
                            text = "단위: ${data.financialValueUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(CardSpacing))
                    FinancialYearCardGrid(rows = data.financialYears)
                }
            }
            item {
                val reports3 = data.reports.take(3)
                val news3 = data.news.take(3)
                Spacer(Modifier.height(CardSpacing))
                Text(
                    text = "리포트",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(CardSpacing))
                if (reports3.isEmpty()) {
                    Text(
                        "리포트가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    StockDetailNewsStyleFeed(
                        items = reports3.map { it.toArticle() },
                        onClickItem = { article ->
                            onOpenReportPdf(article.url, article.headline)
                        }
                    )
                }
                Spacer(Modifier.height(CardSpacing))
                Text(
                    text = "뉴스",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(CardSpacing))
                if (news3.isEmpty()) {
                    Text(
                        "뉴스가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    StockDetailNewsStyleFeed(
                        items = news3.map { it.toArticle() },
                        onClickItem = { onOpenUrl(it.url) }
                    )
                }
            }
        }
    }
}

/** 브리핑「대표 지수」[MarketIndexCard]와 동일한 서피스·타이포로 단일 지표 표시 */
@Composable
private fun BriefingStyleSingleMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceMuted, AppShapes.cardNested)
            .padding(
                start = CardPaddingHorizontal,
                end = CardPaddingHorizontal,
                top = CardPaddingVertical,
                bottom = CardPaddingBottom
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.space6)
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = SapiensTextStyles.marketIndexGroup,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InvestmentMetricCardGrid(metrics: List<Pair<String, String>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        metrics.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                rowItems.forEach { (label, value) ->
                    BriefingStyleSingleMetricCard(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BriefingStyleFinancialYearCard(row: FinancialYearRow, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SurfaceMuted, AppShapes.cardNested)
            .padding(
                start = CardPaddingHorizontal,
                end = CardPaddingHorizontal,
                top = CardPaddingVertical,
                bottom = CardPaddingBottom
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.space6)
    ) {
        Text(
            text = row.yearLabel,
            style = SapiensTextStyles.marketIndexGroup,
            color = TextSecondary
        )
        Text(
            text = "매출액",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = row.revenue,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "영업이익",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = row.operatingProfit,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinancialYearCardGrid(rows: List<FinancialYearRow>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        rows.chunked(2).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                chunk.forEach { row ->
                    BriefingStyleFinancialYearCard(
                        row = row,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (chunk.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 뉴스 탭 [NewsScreen] `LazyColumn`과 동일: Background 위 Card + RowVertical·구분선 */
@Composable
private fun StockDetailNewsStyleFeed(
    items: List<Article>,
    onClickItem: (Article) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RowVertical)
            .background(Card, AppShapes.card)
    ) {
        items.forEachIndexed { index, article ->
            NewsFeedRow(
                item = article,
                rank = null,
                onClick = { onClickItem(article) }
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    color = TextSecondary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.space16)
                )
            }
        }
    }
}

private fun StockReportItem.toArticle(): Article {
    val broker = brokerName.trim().ifBlank { "증권" }
    val d = date.trim().ifBlank { "-" }
    return Article(
        source = "목표가 ${goalPrice.trim().ifBlank { "-" }} · ${opinion.trim().ifBlank { "-" }}",
        headline = title,
        summary = "",
        time = "$broker · $d",
        category = "리포트",
        tag = broker,
        url = url
    )
}

private fun StockNewsItem.toArticle(): Article = Article(
    source = officeName.trim().ifBlank { "-" },
    headline = title,
    summary = "",
    time = date.trim().ifBlank { "-" },
    category = "뉴스",
    tag = "",
    url = url
)
