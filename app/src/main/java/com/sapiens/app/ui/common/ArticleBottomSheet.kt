package com.sapiens.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.news.newsPublisherChipText
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.BottomSheet
import com.sapiens.app.ui.theme.BottomSheetBottomPadding
import com.sapiens.app.ui.theme.SheetDragHandleHeight
import com.sapiens.app.ui.theme.SheetHorizontal
import com.sapiens.app.ui.theme.SheetTop
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.SummaryPointSpacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleBottomSheet(
    article: Article,
    onDismissRequest: () -> Unit,
    onOpenOriginalArticle: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resolvedPoints = article.summaryPoints
        .takeIf { it.isNotEmpty() }
        ?: buildSummaryPoints(summary = article.summary, headline = article.headline)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = BottomSheet,
        dragHandle = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = SheetHorizontal)
                .padding(bottom = BottomSheetBottomPadding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Spacing.space8)
                    .height(SheetDragHandleHeight)
                    .fillMaxWidth(0.12f)
                    .clip(AppShapes.sheetHandle)
                    .background(TextSecondary.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SheetTop),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PublisherChip(
                    text = newsPublisherChipText(article.source).ifBlank { article.source.trim() }
                        .ifBlank { "출처" }
                )
            }

            val headlineStyle = MaterialTheme.typography.headlineMedium.run {
                copy(
                    fontSize = (fontSize.value - 1f).sp,
                    lineHeight = (lineHeight.value - 1f).sp
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.space14)
            ) {
                Text(
                    text = article.headline,
                    style = headlineStyle,
                    color = TextPrimary,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = articleTimeForDisplay(article.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = Spacing.space6)
                )
            }

            Column(
                modifier = Modifier.padding(top = Spacing.space14),
                verticalArrangement = Arrangement.spacedBy(SummaryPointSpacing)
            ) {
                resolvedPoints.take(4).forEachIndexed { index, point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
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

            val canOpenOriginal = article.url.isNotBlank()
            OutlinedButton(
                onClick = { if (canOpenOriginal) onOpenOriginalArticle?.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.space20)
                    .height(Spacing.space64),
                enabled = canOpenOriginal,
                shape = AppShapes.button,
                border = BorderStroke(Spacing.space1, TextSecondary.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    disabledContentColor = TextSecondary.copy(alpha = 0.45f)
                )
            ) {
                Text(
                    text = "기사 원문 보기",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PublisherChip(text: String) {
    val (backgroundColor, textColor) = categoryChipColors(text)
    Surface(
        color = backgroundColor,
        shape = AppShapes.chip
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space3)
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
