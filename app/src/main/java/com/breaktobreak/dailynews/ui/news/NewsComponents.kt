package com.breaktobreak.dailynews.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.ui.common.categoryChipColors
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.CardPaddingHorizontal
import com.breaktobreak.dailynews.ui.theme.RowVertical
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

@Composable
fun NewsFeedList(
    items: List<Article>,
    showRank: Boolean = false,
    onClickItem: (Article) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Card, RoundedCornerShape(18.dp))
    ) {
        items.forEachIndexed { index, item ->
            NewsFeedRow(
                item = item,
                rank = if (showRank) index + 1 else null,
                onClick = { onClickItem(item) }
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    color = TextSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun NewsFeedRow(
    item: Article,
    rank: Int? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = CardPaddingHorizontal,
                vertical = RowVertical
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (rank != null) {
                val rankColor = if (rank <= 3) Accent else TextSecondary
                val rankWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Medium
                Text(
                    text = rank.toString(),
                    modifier = Modifier
                        .width(28.dp)
                        .padding(top = 2.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = rankColor,
                    fontWeight = rankWeight
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(item.category.ifBlank { item.tag })
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = item.headline,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val (backgroundColor, textColor) = categoryChipColors(category)
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
