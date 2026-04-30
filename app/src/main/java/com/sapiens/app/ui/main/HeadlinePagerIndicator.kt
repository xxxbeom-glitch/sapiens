package com.sapiens.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sapiens.app.ui.theme.AppRadius
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.scaledDp

/** 트랙 46×6 @360, 라운드 99. */
private const val DesignPagerTrackWidth = 46f
private const val DesignPagerTrackHeight = 6f

private val HeadlinePagerTrackColor = Color(0xFFFFFFFF).copy(alpha = 0.30f)
/** 스펙상 Fill #ffffff 0% — 비강조는 트랙만; 현재 페이지 구간은 100%로 표시. */
private val HeadlinePagerFillColor = Color(0xFFFFFFFF).copy(alpha = 1f)

/**
 * 헤드라인 카드용 페이지 인디케이터.
 * Track #ffffff 30%, 활성 Fill #ffffff 100%.
 */
@Composable
fun HeadlinePagerIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    require(pageCount >= 1) { "pageCount >= 1" }
    val safePage = currentPage.coerceIn(0, pageCount - 1)
    val s = LocalFigmaFrameWidthScale.current
    val trackW = remember(s) { scaledDp(DesignPagerTrackWidth, s) }
    val trackH = remember(s) { scaledDp(DesignPagerTrackHeight, s) }
    val capsule =
        remember(s) {
            RoundedCornerShape(scaledDp(AppRadius.radius99.value, s))
        }
    val segmentWidth = trackW / pageCount

    Box(
        modifier = modifier
            .width(trackW)
            .height(trackH)
            .background(color = HeadlinePagerTrackColor, shape = capsule),
    ) {
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * safePage)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(color = HeadlinePagerFillColor, shape = capsule),
        )
    }
}
