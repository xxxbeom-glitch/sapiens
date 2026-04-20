package com.sapiens.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 코너 반경 토큰 (dp). */
object AppRadius {
    val radius2 = 2.dp
    val radius3 = 3.dp
    val radius4 = 4.dp
    val radius6 = 6.dp
    val radius8 = 8.dp
    val radius10 = 10.dp
    val radius12 = 12.dp
    val radius14 = 14.dp
    val radius15 = 15.dp
    val radius16 = 16.dp
    val radius18 = 18.dp
    val radius20 = 20.dp
    val radius99 = 99.dp
}

/** 자주 쓰는 `RoundedCornerShape` 프리셋. */
object AppShapes {
    val chip = RoundedCornerShape(AppRadius.radius6)
    val chipTight = RoundedCornerShape(AppRadius.radius4)
    val button = RoundedCornerShape(AppRadius.radius12)
    /** 시트·패널 내부 중간 카드 (12dp). */
    val cardMedium = RoundedCornerShape(AppRadius.radius12)
    val card = RoundedCornerShape(AppRadius.radius18)
    val cardNested = RoundedCornerShape(AppRadius.radius14)
    val sheetHandle = RoundedCornerShape(AppRadius.radius99)
    val pill = RoundedCornerShape(AppRadius.radius15)
    val pillInner = RoundedCornerShape(AppRadius.radius10)
    val thumbnail = RoundedCornerShape(AppRadius.radius6)
    val barTop = RoundedCornerShape(
        topStart = AppRadius.radius3,
        topEnd = AppRadius.radius3,
        bottomStart = Spacing.space0,
        bottomEnd = Spacing.space0
    )
    val barSegment = RoundedCornerShape(AppRadius.radius3)
    val hairlineTrack = RoundedCornerShape(AppRadius.radius2)
    val healthCapsule = RoundedCornerShape(AppRadius.radius20)
    val panel = RoundedCornerShape(AppRadius.radius16)
    val searchField = RoundedCornerShape(AppRadius.radius8)
}
