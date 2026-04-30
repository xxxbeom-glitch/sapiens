package com.sapiens.app.ui.main

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapiens.app.R
import com.sapiens.app.data.model.BriefingCard
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.moneyFlowDirection
import com.sapiens.app.ui.common.MarketCategoryLabel
import com.sapiens.app.ui.common.MarketCategoryLabelVariant
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.SapiensFontFamily
import com.sapiens.app.ui.theme.scaleForFigmaFrame
import com.sapiens.app.ui.theme.scaledDp

private const val DesignHeadlineCardWidth = 290f
private const val DesignHeadlineCardHeight = 443f
private const val DesignHeadlineTitleWidth = 246f
private const val DesignHeadlineTitleHeight = 72f
private const val DesignHeadlineBodyWidth = 246f
private const val DesignHeadlineBodyHeight = 220f
private const val DesignCardCornerDp = 26f
private const val DesignCardPaddingH = 22f
private const val DesignCardPaddingTop = 40f
private const val DesignCardPaddingBottom = 34f
private const val DesignCardChipSpacing = 10f
private const val DesignBookmarkIcon = 24f
private const val DesignSectionSpacerSm = 6f
private const val DesignSectionSpacerMd = 13f
private const val DesignShadowBlur = 12f
private const val DesignShadowOffsetY = -2f

private val HeadlineCardBackground = Color(0xFFF8F8F8)
private val HeadlineTitleColor = Color(0xFF222222)
private val HeadlineDateColor = Color(0xFF565656)
private val HeadlineBodyColor = Color(0xFF333333)
private val MoneyFlowUpColor = Color(0xFFE53935)
private val MoneyFlowDownColor = Color(0xFF1565C0)
private val MoneyFlowFlatColor = Color(0xFF757575)

/** money_flow 방향에 따른 강조 색상 */
private fun moneyFlowColor(direction: MarketDirection): Color = when (direction) {
    MarketDirection.UP   -> MoneyFlowUpColor
    MarketDirection.DOWN -> MoneyFlowDownColor
    MarketDirection.FLAT -> MoneyFlowFlatColor
}

/**
 * 브리핑 카드 1장.
 * [card]가 null이면 로딩 스켈레톤(빈 카드) 표시.
 * [onBookmarkToggle] 북마크 토글 콜백 (card_id 전달).
 * [isBookmarked] 현재 북마크 상태.
 */
@Composable
fun HeadlinePreviewCard(
    card: BriefingCard?,
    isBookmarked: Boolean = false,
    onBookmarkToggle: (cardId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val figmaScale = LocalFigmaFrameWidthScale.current

    val headlineTitleStyle = remember(figmaScale) {
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = HeadlineTitleColor,
        ).scaleForFigmaFrame(figmaScale)
    }
    val dateStyle = remember(figmaScale) {
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = HeadlineDateColor,
        ).scaleForFigmaFrame(figmaScale)
    }
    val bodyStyle = remember(figmaScale) {
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            color = HeadlineBodyColor,
        ).scaleForFigmaFrame(figmaScale)
    }
    val reasonStyle = remember(figmaScale) {
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = HeadlineBodyColor,
        ).scaleForFigmaFrame(figmaScale)
    }

    val cardWidth    = remember(figmaScale) { scaledDp(DesignHeadlineCardWidth, figmaScale) }
    val cardHeight   = remember(figmaScale) { scaledDp(DesignHeadlineCardHeight, figmaScale) }
    val titleBlockW  = remember(figmaScale) { scaledDp(DesignHeadlineTitleWidth, figmaScale) }
    val titleBlockH  = remember(figmaScale) { scaledDp(DesignHeadlineTitleHeight, figmaScale) }
    val bodyBlockW   = remember(figmaScale) { scaledDp(DesignHeadlineBodyWidth, figmaScale) }
    val bodyBlockH   = remember(figmaScale) { scaledDp(DesignHeadlineBodyHeight, figmaScale) }
    val padH         = remember(figmaScale) { scaledDp(DesignCardPaddingH, figmaScale) }
    val padTop       = remember(figmaScale) { scaledDp(DesignCardPaddingTop, figmaScale) }
    val padBottom    = remember(figmaScale) { scaledDp(DesignCardPaddingBottom, figmaScale) }
    val chipGap      = remember(figmaScale) { scaledDp(DesignCardChipSpacing, figmaScale) }
    val bookmarkSize = remember(figmaScale) {
        scaledDp(DesignBookmarkIcon, figmaScale).coerceAtLeast(DesignBookmarkIcon.dp)
    }
    val spacerSm     = remember(figmaScale) { scaledDp(DesignSectionSpacerSm, figmaScale) }
    val spacerMd     = remember(figmaScale) { scaledDp(DesignSectionSpacerMd, figmaScale) }
    val cardCornerShape = remember(figmaScale) {
        RoundedCornerShape(scaledDp(DesignCardCornerDp, figmaScale))
    }

    // 카드 데이터 (null이면 빈 값으로 폴백)
    val title       = card?.marketStatus.orEmpty()
    val date        = card?.generatedAt?.take(10).orEmpty()  // "2026-04-30"
    val bodyText    = card?.keyReasons?.joinToString("\n• ", prefix = "• ").orEmpty()
    val investPoint = card?.investPoint.orEmpty()
    val tags        = card?.tags ?: emptyList()
    val direction   = card?.moneyFlowDirection() ?: MarketDirection.FLAT
    val moneyFlow   = card?.moneyFlow.orEmpty()

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val radiusPx     = scaledDp(DesignCardCornerDp, figmaScale).toPx()
                    val blurPx       = scaledDp(DesignShadowBlur, figmaScale).toPx()
                    val shadowOffset = scaledDp(DesignShadowOffsetY, figmaScale).toPx()
                    drawIntoCanvas { canvas ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = HeadlineCardBackground.toArgb()
                            setShadowLayer(blurPx, 0f, shadowOffset,
                                Color.Black.copy(alpha = 0.12f).toArgb())
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            RectF(0f, 0f, size.width, size.height),
                            radiusPx, radiusPx, paint,
                        )
                    }
                },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = HeadlineCardBackground, shape = cardCornerShape)
                .padding(start = padH, end = padH, top = padTop, bottom = padBottom),
        ) {
            // ── 상단: 태그 칩 + 북마크 ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(chipGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags.take(2).forEach { tag ->
                        val variant = when {
                            tag.contains("미국") -> MarketCategoryLabelVariant.UsMarket
                            tag.contains("AI") || tag.contains("반도체") ->
                                MarketCategoryLabelVariant.UsMarket
                            else -> MarketCategoryLabelVariant.KrMarket
                        }
                        MarketCategoryLabel(tag, variant)
                    }
                    // 태그가 없을 때 카테고리로 대체
                    if (tags.isEmpty() && card != null) {
                        MarketCategoryLabel(card.category, MarketCategoryLabelVariant.KrMarket)
                    }
                }
                Image(
                    painter = painterResource(
                        if (isBookmarked) R.drawable.ico_bookmark_fill else R.drawable.ico_bookmark
                    ),
                    contentDescription = if (isBookmarked) "북마크됨" else "북마크",
                    modifier = Modifier
                        .size(bookmarkSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { card?.cardId?.let { onBookmarkToggle(it) } },
                )
            }

            Spacer(modifier = Modifier.height(spacerSm))

            // ── 시장 상태 (헤드라인) ────────────────────────────
            Text(
                text = title,
                modifier = Modifier
                    .width(titleBlockW)
                    .height(titleBlockH),
                style = headlineTitleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(spacerSm))

            // ── 날짜 + money_flow ───────────────────────────────
            Row(
                modifier = Modifier.width(titleBlockW),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = date, style = dateStyle)
                if (moneyFlow.isNotBlank()) {
                    Text(
                        text = moneyFlow,
                        style = dateStyle.copy(color = moneyFlowColor(direction)),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacerMd))

            // ── 핵심 이유 + 투자 포인트 ────────────────────────
            Box(
                modifier = Modifier
                    .width(bodyBlockW)
                    .height(bodyBlockH)
                    .clipToBounds(),
            ) {
                Column {
                    // key_reasons 불릿 리스트
                    Text(
                        text = bodyText,
                        modifier = Modifier.fillMaxWidth(),
                        style = bodyStyle,
                        overflow = TextOverflow.Clip,
                    )
                    if (investPoint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📌 $investPoint",
                            modifier = Modifier.fillMaxWidth(),
                            style = reasonStyle.copy(color = Color(0xFF6C54DD)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
