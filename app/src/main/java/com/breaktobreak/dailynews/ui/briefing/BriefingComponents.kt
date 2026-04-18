package com.breaktobreak.dailynews.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.MarketDirection
import com.breaktobreak.dailynews.data.model.MarketIndex
import com.breaktobreak.dailynews.data.model.MarketIndicator
import com.breaktobreak.dailynews.ui.common.categoryChipColors
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.CardPaddingBottom
import com.breaktobreak.dailynews.ui.theme.CardPaddingHorizontal
import com.breaktobreak.dailynews.ui.theme.CardPaddingVertical
import com.breaktobreak.dailynews.ui.theme.MarketDown
import com.breaktobreak.dailynews.ui.theme.MarketFlat
import com.breaktobreak.dailynews.ui.theme.MarketUp
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SectionLabel(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SectionTitleText(title = title)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionTitleText(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary
    )
}

@Composable
fun MorningCardPager(
    articles: List<Article>,
    pagerState: PagerState,
    onClickArticle: (Article) -> Unit
) {
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Card)
                    .padding(
                        start = CardPaddingHorizontal,
                        end = CardPaddingHorizontal,
                        top = CardPaddingVertical,
                        bottom = CardPaddingBottom
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { page ->
                    val article = articles[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onClickArticle(article) },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Text(
                                text = article.headline,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = article.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.24f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${article.source} · ${article.time}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            CategoryChip(label = article.category.ifBlank { "경제" })
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(articles.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (selected) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Accent else TextSecondary.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
fun MarketTickerBanner(
    pages: List<List<MarketIndicator>>
) {
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState, pages.size) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % pages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val pageItems = pages[it]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarketTickerItem(
                    indicator = pageItems.getOrNull(0),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(0.5.dp)
                        .background(TextSecondary.copy(alpha = 0.2f))
                )

                MarketTickerItem(
                    indicator = pageItems.getOrNull(1),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .height(4.dp)
                        .width(if (selected) 12.dp else 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (selected) Accent else TextSecondary.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun MarketTickerItem(
    indicator: MarketIndicator?,
    modifier: Modifier = Modifier
) {
    if (indicator == null) {
        Spacer(modifier = modifier)
        return
    }

    val directionColor = when (indicator.direction) {
        MarketDirection.UP -> com.breaktobreak.dailynews.ui.theme.MarketUp
        MarketDirection.DOWN -> MarketDown
        MarketDirection.FLAT -> com.breaktobreak.dailynews.ui.theme.MarketFlat
    }

    Row(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = indicator.name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = indicator.value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = indicator.change,
            style = MaterialTheme.typography.labelSmall,
            color = directionColor
        )
    }
}

@Composable
fun USMajorArticlesCard(
    articles: List<Article>,
    onClickArticle: (Article) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Card)
                .padding(
                    start = CardPaddingHorizontal,
                    end = CardPaddingHorizontal,
                    top = CardPaddingVertical,
                    bottom = CardPaddingBottom
                )
        ) {
            Text(
                text = "주요 기사",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )

            Column(modifier = Modifier.padding(top = 4.dp)) {
                articles.forEachIndexed { index, article ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onClickArticle(article) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index < articles.lastIndex) {
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun MarketIndexGrid(
    indices: List<MarketIndex>
) {
    val rows = indices.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { index ->
                    MarketIndexCard(
                        index = index,
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
private fun MarketIndexCard(
    index: MarketIndex,
    modifier: Modifier = Modifier
) {
    val directionColor = when (index.direction) {
        MarketDirection.UP -> MarketUp
        MarketDirection.DOWN -> MarketDown
        MarketDirection.FLAT -> MarketFlat
    }

    Column(
        modifier = modifier
            .background(Color(0xFF28282A), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = index.group.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextSecondary
        )
        Text(
            text = index.name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = index.value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = index.change,
            style = MaterialTheme.typography.labelSmall,
            color = directionColor
        )
    }
}

@Composable
fun SourceChip(label: String) {
    Surface(
        color = Accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Accent
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String) {
    val (backgroundColor, textColor) = categoryChipColors(label)
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
