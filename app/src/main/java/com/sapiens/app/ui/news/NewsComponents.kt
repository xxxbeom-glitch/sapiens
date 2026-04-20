package com.sapiens.app.ui.news

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.categoryChipColors
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

@Composable
fun NewsFeedList(
    items: List<Article>,
    showRank: Boolean = false,
    onClickItem: (Article) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16)
            .background(Card, AppShapes.card)
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
                    modifier = Modifier.padding(start = Spacing.space16)
                )
            }
        }
    }
}

/** 해외 뉴스 카드: ISO 8601 등 → KST `MM.dd HH:mm`. 파싱 실패 시 원문. */
internal fun formatOverseasNewsTimeKst(raw: String): String {
    val t = raw.trim()
    if (t.isBlank()) return raw
    val seoul = ZoneId.of("Asia/Seoul")
    val outFmt = DateTimeFormatter.ofPattern("MM.dd HH:mm", Locale.ROOT)
    return runCatching {
        val instant = try {
            Instant.parse(t)
        } catch (_: Exception) {
            OffsetDateTime.parse(t).toInstant()
        }
        instant.atZone(seoul).format(outFmt)
    }.getOrDefault(raw)
}

/** `섹션 | 언론사` → 마지막 구간만. `|` 없으면 원문. */
internal fun formatOverseasNewsSource(raw: String): String {
    val s = raw.trim()
    if (!s.contains('|')) return s
    return s.substringAfterLast('|').trim().ifBlank { s }
}

@Composable
fun NewsFeedRow(
    item: Article,
    rank: Int? = null,
    onClick: () -> Unit,
    /** null 이면 [Article.time] 사용. 해외 탭에서 KST 포맷 문자열 전달. */
    timeDisplayOverride: String? = null,
    /** null 이면 [Article.source] 사용. 해외 탭에서 언론사만 전달. */
    sourceDisplayOverride: String? = null,
) {
    val timeText = timeDisplayOverride ?: item.time
    val sourceText = sourceDisplayOverride ?: item.source
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(
                horizontal = CardPaddingHorizontal,
                vertical = RowVertical
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (rank != null) {
                val rankColor = if (rank <= 3) Accent else TextSecondary
                val rankWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Medium
                Text(
                    text = rank.toString(),
                    modifier = Modifier
                        .width(Spacing.space28)
                        .padding(top = Spacing.space2),
                    style = MaterialTheme.typography.titleSmall,
                    color = rankColor,
                    fontWeight = rankWeight
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.space8),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    CategoryChip(item.category.ifBlank { item.tag })
                    Text(
                        text = timeText,
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
                    text = sourceText,
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
        shape = AppShapes.chip
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space3)
        )
    }
}
