package com.sapiens.app.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.MarketIndex
import com.sapiens.app.data.model.MarketIndicator
import com.sapiens.app.ui.common.MarketIndexStyleChangeText
import com.sapiens.app.ui.common.categoryChipColors
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingBottom
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.CardPaddingVertical
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.MarketDown
import com.sapiens.app.ui.theme.MarketFlat
import com.sapiens.app.ui.theme.MarketUp
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.SurfaceMuted
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay

@Composable
fun SectionLabel(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.space20, vertical = Spacing.space12)
    ) {
        SectionTitleText(title = title)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = Spacing.space4)
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
fun MorningSourceCard(
    source: String,
    articles: List<Article>,
    onClickArticle: (Article) -> Unit,
    modifier: Modifier = Modifier
) {
    if (articles.isEmpty()) return
    val topArticles = articles.take(10)
    val first = topArticles.first()
    val publishedAt = first.time.ifBlank { "-" }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = AppShapes.button,
        border = BorderStroke(Spacing.hairline, TextSecondary.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(
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
                SourceChip(label = source)
                Text(
                    text = publishedAt,
                    style = SapiensTextStyles.briefingPublisherChip,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space10))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClickArticle(first) },
                horizontalArrangement = Arrangement.spacedBy(Spacing.space10),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = first.headline,
                    modifier = Modifier.weight(1f),
                    style = SapiensTextStyles.morningCardHeadline,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                HeadlineThumbnail(
                    imageUrl = first.imageUrl,
                    modifier = Modifier.size(Spacing.space52)
                )
            }

            if (topArticles.size > 1) {
                HorizontalDivider(
                    color = TextSecondary.copy(alpha = 0.2f),
                    thickness = Spacing.hairline,
                    modifier = Modifier.padding(top = Spacing.space10)
                )
            }

            topArticles.drop(1).forEachIndexed { index, article ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClickArticle(article) }
                        .padding(vertical = Spacing.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.headline,
                        style = SapiensTextStyles.morningListRow,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < topArticles.drop(1).lastIndex) {
                    HorizontalDivider(
                        color = TextSecondary.copy(alpha = 0.2f),
                        thickness = Spacing.hairline,
                        modifier = Modifier.padding(vertical = RowVertical)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadlineThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val shape = AppShapes.thumbnail
    if (imageUrl.isBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(TextSecondary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(Spacing.space22)
            )
        }
        return
    }
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        placeholder = painterResource(id = android.R.drawable.ic_menu_report_image),
        error = painterResource(id = android.R.drawable.ic_menu_report_image),
        fallback = painterResource(id = android.R.drawable.ic_menu_report_image),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(TextSecondary.copy(alpha = 0.12f))
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
                .padding(horizontal = Spacing.space16),
            shape = AppShapes.card
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
                verticalArrangement = Arrangement.Top
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
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Spacing.space56)
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

                        Spacer(modifier = Modifier.height(RowVertical))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Spacing.space44)
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

                        HorizontalDivider(
                            color = TextSecondary.copy(alpha = 0.24f),
                            thickness = Spacing.hairline,
                            modifier = Modifier.padding(vertical = RowVertical)
                        )

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
                .padding(top = Spacing.space12),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(articles.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.space3)
                        .height(Spacing.space6)
                        .width(if (selected) Spacing.space20 else Spacing.space6)
                        .clip(CircleShape)
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
            .padding(horizontal = Spacing.space16, vertical = Spacing.space8)
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
                        .height(Spacing.space28)
                        .width(Spacing.hairline)
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
                .padding(top = Spacing.space8),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.space2)
                        .height(Spacing.space4)
                        .width(if (selected) Spacing.space12 else Spacing.space4)
                        .clip(AppShapes.sheetHandle)
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
        MarketDirection.UP -> com.sapiens.app.ui.theme.MarketUp
        MarketDirection.DOWN -> MarketDown
        MarketDirection.FLAT -> com.sapiens.app.ui.theme.MarketFlat
    }

    Row(
        modifier = modifier.padding(horizontal = Spacing.space10),
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
fun MarketIndexGrid(
    indices: List<MarketIndex>
) {
    val rows = indices.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
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
            text = index.group.uppercase(),
            style = SapiensTextStyles.marketIndexGroup,
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
        MarketIndexStyleChangeText(
            change = index.change,
            direction = index.direction
        )
    }
}

@Composable
fun SourceChip(label: String) {
    Surface(
        color = Accent.copy(alpha = 0.14f),
        shape = AppShapes.chip
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space5)
        ) {
            Box(
                modifier = Modifier
                    .width(Spacing.space5)
                    .height(Spacing.space5)
                    .clip(CircleShape)
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
