package com.sapiens.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.BottomSheetBottomPadding
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.OnPrimaryFixed
import com.sapiens.app.ui.theme.SheetDragHandleHeight
import com.sapiens.app.ui.theme.SheetHorizontal
import com.sapiens.app.ui.theme.SheetTop
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.SummaryPointSpacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

/** 포인트 컬러 대비 눌림 시 약 6% 어둡게 */
private fun Color.darkenTowardsBlack(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - f),
        green = green * (1f - f),
        blue = blue * (1f - f),
        alpha = alpha
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleBottomSheet(
    article: Article,
    onDismissRequest: () -> Unit,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    kind: ArticleBottomSheetKind = ArticleBottomSheetKind.Standard,
    onOpenOriginalArticle: (() -> Unit)? = null
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

            when (kind) {
                ArticleBottomSheetKind.Standard -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.space14),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(Spacing.space12))
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "저장됨" else "저장",
                            tint = if (isBookmarked) Accent else TextSecondary,
                            modifier = Modifier
                                .size(Spacing.space24)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onBookmarkToggle() }
                        )
                    }
                }
                ArticleBottomSheetKind.News -> {
                    val headlineStyle = MaterialTheme.typography.headlineMedium.run {
                        copy(
                            fontSize = (fontSize.value - 1f).sp,
                            lineHeight = (lineHeight.value - 1f).sp
                        )
                    }
                    Text(
                        text = article.headline,
                        style = headlineStyle,
                        color = TextPrimary,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.space14)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = Spacing.space14),
                color = TextSecondary.copy(alpha = 0.24f)
            )

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

            if (kind == ArticleBottomSheetKind.News) {
                val canOpenOriginal = article.url.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.space20),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space6),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (canOpenOriginal) onOpenOriginalArticle?.invoke() },
                        modifier = Modifier
                            .weight(1f)
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
                    NewsSaveBookmarkButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(Spacing.space64)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsSaveBookmarkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = if (pressed) Accent.darkenTowardsBlack(0.06f) else Accent
    Button(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = AppShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = OnPrimaryFixed
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Spacing.space0,
            pressedElevation = Spacing.space0
        )
    ) {
        Text(
            text = "저장",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
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
