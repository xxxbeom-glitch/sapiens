package com.breaktobreak.dailynews.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.MarketDirection
import com.breaktobreak.dailynews.data.model.MarketIndicator
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

@Composable
fun SectionLabel(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
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
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val article = articles[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClickArticle(article) },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = article.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

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
fun USMarketCard(
    indicators: List<MarketIndicator>,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "시장 지표",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "4/18 장 마감 기준",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                val leftIndicators = indicators.take(3)
                val rightIndicators = indicators.drop(3).take(3)

                IndicatorColumn(
                    indicators = leftIndicators,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(0.5.dp)
                        .height(120.dp)
                        .background(TextSecondary.copy(alpha = 0.08f))
                )

                IndicatorColumn(
                    indicators = rightIndicators,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = TextSecondary.copy(alpha = 0.24f)
            )

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
                            .clickable { onClickArticle(article) }
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
private fun IndicatorColumn(
    indicators: List<MarketIndicator>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        indicators.forEachIndexed { index, indicator ->
            MarketIndicatorRow(indicator = indicator)
            if (index < indicators.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = TextSecondary.copy(alpha = 0.06f)
                )
            }
        }
    }
}

@Composable
private fun MarketIndicatorRow(indicator: MarketIndicator) {
    val directionColor = when (indicator.direction) {
        MarketDirection.UP -> MarketUp
        MarketDirection.DOWN -> MarketDown
        MarketDirection.FLAT -> MarketFlat
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = indicator.name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = indicator.value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
            Text(
                text = indicator.change,
                style = MaterialTheme.typography.labelSmall,
                color = directionColor,
                textAlign = TextAlign.End
            )
        }
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
    Surface(
        color = TextSecondary.copy(alpha = 0.18f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
