package com.sapiens.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

/** 브리핑·뉴스 탭에서 공유하는 "첫 기사 강조 + 이후 한 줄" 혼합 카드. */
@Composable
fun ArticleMixedFeedCard(
    articles: List<Article>,
    onClickArticle: (Article) -> Unit,
    modifier: Modifier = Modifier,
    topChipForArticle: (Article) -> String = { a -> a.source.ifBlank { "국내" } },
    /** `null`이면 목록이 비었을 때 아무것도 그리지 않는다. 문자열이면 그 텍스트를 단일 문구로 표시한다. */
    emptyStateText: String? = null,
) {
    if (articles.isEmpty() && emptyStateText == null) return

    if (articles.isEmpty() && emptyStateText != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16),
            shape = AppShapes.card
        ) {
            Column(
                modifier = Modifier
                    .background(Card)
                    .padding(
                        start = Spacing.space24,
                        end = Spacing.space24,
                        top = Spacing.space26,
                        bottom = Spacing.space26
                    )
            ) {
                Text(
                    text = emptyStateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = RowVertical)
                )
            }
        }
        return
    }

    val list = articles
    val first = list.first()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16),
        shape = AppShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card)
                .padding(
                    start = Spacing.space24,
                    end = Spacing.space24,
                    top = Spacing.space26,
                    bottom = Spacing.space26
                )
        ) {
            FeedPublisherChip(label = topChipForArticle(first))
            Spacer(modifier = Modifier.height(Spacing.space8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClickArticle(first) },
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = first.headline,
                    modifier = Modifier.weight(1f),
                    style = SapiensTextStyles.briefingThemeHeadline,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (list.size > 1) {
                HorizontalDivider(
                    color = TextSecondary.copy(alpha = 0.2f),
                    thickness = Spacing.hairline,
                    modifier = Modifier.padding(
                        top = Spacing.space6 + Spacing.space2,
                        bottom = Spacing.space6
                    )
                )
            }

            list.drop(1).forEachIndexed { index, article ->
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
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index < list.drop(1).lastIndex) {
                    HorizontalDivider(
                        color = TextSecondary.copy(alpha = 0.2f),
                        thickness = Spacing.hairline,
                        modifier = Modifier.padding(vertical = Spacing.space6)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPublisherChip(label: String) {
    Surface(
        color = Accent.copy(alpha = 0.14f),
        shape = AppShapes.chipTight
    ) {
        Text(
            text = label,
            style = SapiensTextStyles.briefingPublisherChip,
            color = Accent,
            modifier = Modifier.padding(horizontal = Spacing.space6, vertical = Spacing.space2)
        )
    }
}

