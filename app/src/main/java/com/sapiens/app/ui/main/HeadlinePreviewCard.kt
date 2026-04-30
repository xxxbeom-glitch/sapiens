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

// ── Mock(테스트) 데이터: card == null일 때 표시 ─────────────────────────────
private const val MOCK_MARKET_STATUS =
    "미증시혼조실적연준변수속관망세지속"

private const val MOCK_BODY =
    "투자 분위기가 약해지며 시장은 방향 없이 움직이고 있다. 금리 불확실성과 실적 기대가 엇갈리면서 매수세가 제한된 모습이다. 당분간 선별적인 접근이 이어질 가능성이 크다."

private const val MOCK_INVEST_POINT =
    "AI반도체대장주수급지속여부확인필요"

private const val MOCK_CATEGORY = "미국증시"
private const val MOCK_MONEY_FLOW = "관망"

/** money_flow 방향에 따른 강조 색상 */
private fun moneyFlowColor(direction: MarketDirection): Color = when (direction) {
    MarketDirection.UP   -> MoneyFlowUpColor
    MarketDirection.DOWN -> MoneyFlowDownColor
    MarketDirection.FLAT -> MoneyFlowFlatColor
}

private val ALLOWED_CARD_CATEGORIES: Set<String> = setOf(
    "미국증시",
    "섹터·흐름",
    "대장주",
    "국내증시",
    "시장변수",
)

private fun normalizeCardCategory(raw: String?): String {
    val s = (raw ?: "").trim()
    if (s in ALLOWED_CARD_CATEGORIES) return s
    // 데이터가 다른 문자열로 들어오는 경우(공백/유사 표기) 최소한의 관용 처리
    return when {
        s.contains("미국") -> "미국증시"
        s.contains("국내") -> "국내증시"
        s.contains("섹터") || s.contains("흐름") -> "섹터·흐름"
        s.contains("대장") -> "대장주"
        s.contains("변수") || s.contains("금리") || s.contains("환율") || s.contains("연준") -> "시장변수"
        else -> MOCK_CATEGORY
    }
}

private fun categoryVariant(category: String): MarketCategoryLabelVariant = when (category) {
    "미국증시" -> MarketCategoryLabelVariant.UsMarket
    "시장변수" -> MarketCategoryLabelVariant.MarketVariable
    "섹터·흐름" -> MarketCategoryLabelVariant.SectorFlow
    "국내증시" -> MarketCategoryLabelVariant.KrMarket
    "대장주" -> MarketCategoryLabelVariant.Leader
    else -> MarketCategoryLabelVariant.UsMarket
}

private fun categoryDisplayText(category: String): String =
    if (category == "섹터·흐름") "섹터,흐름" else category

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

    // 카드 데이터 (card == null이면 테스트용 Mock 데이터 표시)
    val title = card?.marketStatus ?: MOCK_MARKET_STATUS
    val date = card?.generatedAt?.take(10).orEmpty() // "2026-04-30"
    // 본문: body 단일 문자열(서술형). 없으면 Mock.
    val bodyText = card?.body.orEmpty().ifBlank { MOCK_BODY }
    val category = normalizeCardCategory(card?.category)
    val direction = card?.moneyFlowDirection() ?: MarketDirection.FLAT
    val moneyFlow = card?.moneyFlow ?: MOCK_MONEY_FLOW

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
                    // 태그(칩)는 디자인 가이드의 5개 카테고리만 사용
                    MarketCategoryLabel(categoryDisplayText(category), categoryVariant(category))
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
                    // body 서술형
                    Text(
                        text = bodyText,
                        modifier = Modifier.fillMaxWidth(),
                        style = bodyStyle,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
