package com.sapiens.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppRadius
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.OnPrimaryFixed
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.SapiensFontFamily
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextSecondary

/** 바깥 컨테이너 86×35 (@1x). 안쪽 동일 패딩 후 두 칩이 같은 너비. */
private val RegionToggleOuterWidth = 86.dp
private val RegionToggleOuterHeight = 35.dp

/** 네 면 동일 패딩 → 내부 칩 영역 (86−8)×(35−8) = 78×27, 칩 각 39×27. */
private val RegionToggleInnerPadding = Spacing.space4

/**
 * 한국·미국 세그먼트. 비활성 칩 배경은 투명, 바깥은 [Card] 트랙.
 */
@Composable
fun KoreaUsRegionChips(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(RegionToggleOuterWidth, RegionToggleOuterHeight)
            .clip(RoundedCornerShape(AppRadius.radius8))
            .background(Card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(RegionToggleInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KoreaUsRegionSegment(
                label = "한국",
                selected = selectedIndex == 0,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f),
            )
            KoreaUsRegionSegment(
                label = "미국",
                selected = selectedIndex == 1,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KoreaUsRegionSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val s = LocalFigmaFrameWidthScale.current
    val fontSize = remember(s) { (13f * s).sp }
    val lineHeight = remember(s) { (15f * s).sp }
    val innerShape = RoundedCornerShape(AppRadius.radius6)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(innerShape)
            .background(if (selected) Accent else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) OnPrimaryFixed else TextSecondary.copy(alpha = 0.3f),
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            lineHeight = lineHeight,
            letterSpacing = (-0.018f).em,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}
