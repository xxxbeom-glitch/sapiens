package com.sapiens.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.SapiensFontFamily
import com.sapiens.app.ui.theme.scaledDp

/** 피그마 185:2 — 카드 상단 카테고리 칩 (5종). */
enum class MarketCategoryLabelVariant {
    /** 미국증시 배경 #FF5D5D */
    UsMarket,
    /** 시장변수 배경 #7553F0 */
    MarketVariable,
    /** 섹터,흐름 배경 #238CE2 */
    SectorFlow,
    /** 국내증시 배경 #5B5B5B */
    KrMarket,
    /** 대장주 배경 #79C24C */
    Leader,
}

private val UsMarketLabelBackground = Color(0xFFFF5D5D)
private val MarketVariableLabelBackground = Color(0xFF7553F0)
private val SectorFlowLabelBackground = Color(0xFF238CE2)
private val KrMarketLabelBackground = Color(0xFF5B5B5B)
private val LeaderLabelBackground = Color(0xFF79C24C)
private val MarketCategoryLabelOnBackground = Color.White

/**
 * SUIT Medium 11px 근사, 라운드 4px, 패딩 상 4·하 5·좌우 6 (피그마).
 */
@Composable
fun MarketCategoryLabel(
    text: String,
    variant: MarketCategoryLabelVariant,
    modifier: Modifier = Modifier,
) {
    val background = when (variant) {
        MarketCategoryLabelVariant.UsMarket -> UsMarketLabelBackground
        MarketCategoryLabelVariant.MarketVariable -> MarketVariableLabelBackground
        MarketCategoryLabelVariant.SectorFlow -> SectorFlowLabelBackground
        MarketCategoryLabelVariant.KrMarket -> KrMarketLabelBackground
        MarketCategoryLabelVariant.Leader -> LeaderLabelBackground
    }
    val s = LocalFigmaFrameWidthScale.current
    val fontSize = remember(s) { (11f * s).sp }
    val lineHeight = remember(s) { (11f * s).sp }
    val letterSpacing = remember(s) { ((-0.015f) * s).sp }
    val padStart = remember(s) { scaledDp(6f, s) }
    val padTop = remember(s) { scaledDp(4f, s) }
    val padEnd = remember(s) { scaledDp(6f, s) }
    val padBottom = remember(s) { scaledDp(5f, s) }
    val chipShape = remember(s) { RoundedCornerShape(scaledDp(4f, s)) }
    Text(
        text = text,
        modifier = modifier
            .background(color = background, shape = chipShape)
            .padding(start = padStart, top = padTop, end = padEnd, bottom = padBottom),
        color = MarketCategoryLabelOnBackground,
        fontFamily = SapiensFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        maxLines = 1,
    )
}
