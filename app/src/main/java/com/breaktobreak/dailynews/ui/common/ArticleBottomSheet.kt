package com.breaktobreak.dailynews.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.SheetBottom
import com.breaktobreak.dailynews.ui.theme.SheetHorizontal
import com.breaktobreak.dailynews.ui.theme.SheetTop
import com.breaktobreak.dailynews.ui.theme.SummaryPointSpacing
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleBottomSheet(
    article: Article,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resolvedPoints = article.summaryPoints
        .takeIf { it.isNotEmpty() }
        ?: buildSummaryPoints(summary = article.summary, headline = article.headline)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = Card,
        dragHandle = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = SheetHorizontal)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .height(5.dp)
                    .fillMaxWidth(0.12f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextSecondary.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SheetTop),
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

            Text(
                text = article.headline,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 3,
                modifier = Modifier.padding(top = 14.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp),
                color = TextSecondary.copy(alpha = 0.24f)
            )

            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(SummaryPointSpacing)
            ) {
                resolvedPoints.take(4).forEachIndexed { index, point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.labelLarge,
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 14.dp),
                color = TextSecondary.copy(alpha = 0.24f)
            )

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = SheetBottom),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = TextPrimary
                )
            ) {
                Text("저장")
            }
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

fun buildSummaryPoints(
    summary: String,
    headline: String
): List<String> {
    val normalized = summary
        .replace('\n', ' ')
        .replace("  ", " ")
        .trim()

    val parsed = normalized
        .split("다.", ".", "!", "?")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { sentence ->
            if (sentence.endsWith("다")) sentence else "${sentence}다"
        }

    return when {
        parsed.size >= 4 -> parsed.take(4)
        parsed.size >= 3 -> parsed.take(3)
        parsed.isNotEmpty() -> parsed
        else -> listOf(
            "$headline 관련 핵심 내용을 요약한 기사입니다.",
            "시장 반응과 산업 파급효과를 중심으로 확인할 필요가 있습니다.",
            "추가 업데이트와 원문 확인을 통해 세부 수치를 점검해보세요."
        )
    }
}
