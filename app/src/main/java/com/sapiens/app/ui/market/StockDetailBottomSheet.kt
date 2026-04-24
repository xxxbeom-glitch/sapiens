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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.sapiens.app.data.stock.FinancialYearRow
import com.sapiens.app.data.stock.StockDetailUi
import com.sapiens.app.data.stock.StockDetailUiState
import com.sapiens.app.data.stock.StockNewsItem
import com.sapiens.app.data.stock.StockReportItem
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.SectionLabel
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

    LaunchedEffect(stockCode) {
        viewModel.load(stockCode)
    }

    fun openUrl(url: String) {
        val u = url.trim()
        if (u.isEmpty()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(Background) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val previousColor = window.statusBarColor
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                val previousLightIcons = controller.isAppearanceLightStatusBars
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Background.toArgb()
                controller.isAppearanceLightStatusBars =
                    ColorUtils.calculateLuminance(Background.toArgb()) > 0.5
                onDispose {
                    window.statusBarColor = previousColor
                    controller.isAppearanceLightStatusBars = previousLightIcons
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                }
            } else {
                onDispose { }
            }
        }
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
                                    .padding(top = Spacing.space28)
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
                                    .padding(top = Spacing.space28)
                                    .padding(horizontal = SheetHorizontal)
                            )
                        }
                    }
                    is StockDetailUiState.Success -> {
                        StockDetailContent(
                            modifier = Modifier.weight(1f),
                            data = s.data,
                            onOpenUrl = ::openUrl,
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
    Box(
        modifier = modifier
            .size(Spacing.space24)
            .clip(CircleShape)
            .background(TextSecondary.copy(alpha = ContentAlpha.iconGhost))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
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
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val svgLoader = rememberSvgImageLoader()
    val logoUrl =
        "https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${data.code}.svg"
    val dividerColor = TextSecondary.copy(alpha = ContentAlpha.hairlineOnSecondary)
    val reports3 = data.reports.take(3)
    val news3 = data.news.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SheetHorizontal)
                .padding(top = Spacing.space28, bottom = Spacing.space16)
        ) {
            StockDetailCloseIconButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Spacing.space32)
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
            }
        }

        HorizontalDivider(
            thickness = Spacing.hairline,
            color = dividerColor,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                top = Spacing.space8,
                bottom = BottomSheetBottomPadding + Spacing.space8
            )
        ) {
            item {
                SectionLabel(title = "투자 정보")
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space16)
                        .padding(bottom = Spacing.space8)
                ) {
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
            }
            if (data.financialYears.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.space18))
                    SectionLabel(title = "매출·영업이익 (연간)")
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.space16)
                            .padding(bottom = Spacing.space8)
                    ) {
                        FinancialYearCardGrid(rows = data.financialYears)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(Spacing.space18))
                SectionLabel(title = "리포트")
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space16)
                        .padding(bottom = Spacing.space8)
                ) {
                    if (reports3.isEmpty()) {
                        Text(
                            text = "리포트가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        StockDetailMetaHeadlineCardFeed(
                            items = reports3.map { it.toArticle() },
                            metaFor = { it.reportMetaLine() },
                            onClickItem = { onOpenUrl(it.url) }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(Spacing.space18))
                SectionLabel(title = "뉴스")
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space16)
                        .padding(bottom = Spacing.space8)
                ) {
                    if (news3.isEmpty()) {
                        Text(
                            text = "뉴스가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        StockDetailMetaHeadlineCardFeed(
                            items = news3.map { it.toArticle() },
                            metaFor = { it.newsMetaLine() },
                            onClickItem = { onOpenUrl(it.url) }
                        )
                    }
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

/** [NewsFeedList]과 동일한 카드·구분선; 메타(증권사·날짜 등)는 타이틀 위 한 줄. */
@Composable
private fun StockDetailMetaHeadlineCardFeed(
    items: List<Article>,
    metaFor: (Article) -> String,
    onClickItem: (Article) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card, AppShapes.card)
    ) {
        items.forEachIndexed { index, article ->
            StockDetailMetaHeadlineRow(
                metaLine = metaFor(article),
                headline = article.headline,
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

@Composable
private fun StockDetailMetaHeadlineRow(
    metaLine: String,
    headline: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = CardPaddingHorizontal, vertical = RowVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        if (metaLine.isNotBlank()) {
            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
    }
}

private fun Article.reportMetaLine(): String = time.trim()

private fun Article.newsMetaLine(): String {
    val s = source.trim()
    val t = time.trim()
    return when {
        s.isNotBlank() && t.isNotBlank() -> "$s · $t"
        else -> s.ifBlank { t }
    }
}

private fun StockReportItem.toArticle(): Article {
    val broker = brokerName.trim().ifBlank { "증권" }
    val d = date.trim().ifBlank { "-" }
    return Article(
        source = "",
        headline = title,
        summary = "",
        time = "$broker · $d",
        category = "",
        tag = broker,
        url = url
    )
}

private fun StockNewsItem.toArticle(): Article = Article(
    source = officeName.trim().ifBlank { "-" },
    headline = title,
    summary = "",
    time = date.trim().ifBlank { "-" },
    category = "",
    tag = "",
    url = url
)
