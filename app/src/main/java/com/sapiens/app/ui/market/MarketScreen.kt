package com.sapiens.app.ui.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.model.ThemeStock
import com.sapiens.app.data.model.logoUrl
import com.sapiens.app.data.model.stocksForDisplay
import com.sapiens.app.data.model.themeStockChangeDisplay
import com.sapiens.app.data.model.toMarketCardChangeDisplay
import com.sapiens.app.ui.common.MarketIndexStyleChangeText
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingBottom
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.CardPaddingVertical
import com.sapiens.app.ui.theme.CardSpacing
import com.sapiens.app.ui.theme.ContentAlpha
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.SectorChipPalette
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import com.sapiens.app.ui.theme.TextTertiary
import com.sapiens.app.R
import kotlinx.coroutines.launch

/** 테마 종목 행: 로고 + 텍스트 간격(피그마식 인셋 구분선 정렬용) */
private val ThemeStockLogoSize = Spacing.space40
private val ThemeStockLogoNameSpacing = Spacing.space12

@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
) {
    val marketThemes by viewModel.marketThemes.collectAsState()
    val marketIndustries by viewModel.marketIndustries.collectAsState()
    val sectionTabs = listOf(
        stringResource(R.string.market_tab_theme),
        stringResource(R.string.market_tab_industry),
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { sectionTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Background,
            contentColor = Accent
        ) {
            sectionTabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = tab,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> MarketThemesTabBody(themes = marketThemes)
                1 -> MarketIndustriesTabBody(industries = marketIndustries)
            }
        }
    }
}

@Composable
private fun MarketThemesTabBody(
    themes: List<MarketTheme>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = RowVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.space16)
    ) {
        items(
            items = themes,
            key = { "${it.themeNo}_${it.themeName}" }
        ) { theme ->
            MarketThemeCard(
                theme = theme,
                modifier = Modifier.padding(horizontal = Spacing.space16),
            )
        }
    }
}

@Composable
private fun MarketIndustriesTabBody(
    industries: List<MarketTheme>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = RowVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.space16)
    ) {
        items(
            items = industries,
            key = { "${it.themeNo}_${it.themeName}" }
        ) { industry ->
            MarketThemeCard(
                theme = industry,
                modifier = Modifier.padding(horizontal = Spacing.space16),
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
private fun ThemeCardChangeText(
    raw: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val (label, dir) = remember(raw) { raw.toMarketCardChangeDisplay() }
    MarketIndexStyleChangeText(
        change = label,
        direction = dir,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
private fun MarketThemeCard(
    theme: MarketTheme,
    modifier: Modifier = Modifier,
) {
    val svgLoader = rememberSvgImageLoader()
    val figmaScale = LocalFigmaFrameWidthScale.current
    val themeCardHashtagFontSize = remember(figmaScale) { (25f * figmaScale).sp }
    val themeCardHashtagLineHeight = remember(figmaScale) { (34f * figmaScale).sp }
    val displayStocks = theme.stocksForDisplay()
    val descriptionText = theme.categoryInfo.trim()
    val cardKey = "${theme.themeNo}_${theme.themeName}"
    var descExpanded by remember(cardKey) { mutableStateOf(false) }
    var descOverflow by remember(cardKey) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        color = Card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    PaddingValues(
                        start = CardPaddingHorizontal,
                        end = CardPaddingHorizontal,
                        top = CardPaddingVertical,
                        bottom = CardPaddingBottom
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "#${theme.themeName}",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = themeCardHashtagFontSize,
                        lineHeight = themeCardHashtagLineHeight,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ThemeCardChangeText(
                    raw = theme.changeRate,
                    modifier = Modifier.padding(top = Spacing.space6),
                )
                if (descriptionText.isNotEmpty()) {
                    Text(
                        text = descriptionText,
                        modifier = Modifier.padding(top = Spacing.space6),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { descOverflow = it.hasVisualOverflow }
                    )
                    if (descOverflow || descExpanded) {
                        Text(
                            text = if (descExpanded) "접기" else "더보기",
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { descExpanded = !descExpanded },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            if (displayStocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.space32))
                displayStocks.forEachIndexed { index, stock ->
                    ThemeStockRow(
                        stock = stock,
                        imageLoader = svgLoader,
                    )
                    if (index < displayStocks.lastIndex) {
                        HorizontalDivider(
                            thickness = Spacing.hairline,
                            modifier = Modifier.padding(
                                start = ThemeStockLogoSize + ThemeStockLogoNameSpacing,
                                top = RowVertical,
                                bottom = RowVertical
                            ),
                            color = TextSecondary.copy(alpha = ContentAlpha.hairlineOnSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeStockRow(
    stock: ThemeStock,
    imageLoader: ImageLoader,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ThemeStockLogoNameSpacing),
            modifier = Modifier.weight(1f)
        ) {
            val logo = stock.logoUrl()
            if (logo.isNotEmpty()) {
                AsyncImage(
                    model = logo,
                    contentDescription = stock.name,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .size(ThemeStockLogoSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(ThemeStockLogoSize)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stock.name.firstOrNull()?.toString().orEmpty().ifBlank { "·" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(min = Spacing.space76)
        ) {
            Text(
                text = stock.price,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val (stockChangeLabel, stockChangeDir) = remember(stock.change, stock.upDownGb) {
                stock.themeStockChangeDisplay()
            }
            MarketIndexStyleChangeText(
                change = stockChangeLabel,
                direction = stockChangeDir,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectorChip(label: String) {
    val (backgroundColor, textColor) = SectorChipPalette.colors(label)
    Surface(
        color = backgroundColor,
        shape = AppShapes.chip
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space3)
        )
    }
}
