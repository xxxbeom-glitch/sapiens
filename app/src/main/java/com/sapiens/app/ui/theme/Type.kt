package com.sapiens.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material [SapiensTypography] 외에 화면별로 자주 쓰는 문단 스타일.
 * (브리핑·뉴스 등에서 `copy(fontSize = …)` 하드코딩을 줄이기 위함.)
 */
object SapiensTextStyles {
    val briefingPublisherChip: TextStyle = SapiensTypography.labelSmall.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
    val morningCardHeadline: TextStyle = SapiensTypography.bodyMedium.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
    val morningListRow: TextStyle = SapiensTypography.bodyMedium.copy(fontSize = 13.sp)
    val briefingThemeHeadline: TextStyle = SapiensTypography.headlineMedium.copy(
        fontSize = 24.sp,
        lineHeight = 34.sp
    )
    val marketIndexGroup: TextStyle = SapiensTypography.labelSmall.copy(fontSize = 9.sp)
    val statCaption9: TextStyle = SapiensTypography.labelSmall.copy(fontSize = 9.sp)
    val toggleLabel12: TextStyle = SapiensTypography.labelMedium.copy(fontSize = 12.sp)
}
