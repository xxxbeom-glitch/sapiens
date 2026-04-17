package com.breaktobreak.dailynews.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.CardPaddingHorizontal
import com.breaktobreak.dailynews.ui.theme.RowVertical
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

@Composable
fun NewsFeedList(
    items: List<Article>,
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
private fun NewsFeedRow(
    item: Article,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(item.tag)
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

@Composable
private fun CategoryChip(category: String) {
    Surface(
        color = Accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = Accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
