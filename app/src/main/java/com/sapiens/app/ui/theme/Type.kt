package com.sapiens.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Material [SapiensTypography] 외에 화면별로 자주 쓰는 문단 스타일.
 * (브리핑·뉴스 등에서 `copy(fontSize = …)` 하드코딩을 줄이기 위함.)
 */
object SapiensTextStyles {
    /**
     * 뉴스 상단 `#오늘의 헤드라인` (Bold, 28px / line 32px, 자간 -1.8%).
     */
    val todayHeadlineTitle: TextStyle = TextStyle(
        fontFamily = SapiensFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.018f).em
    )
    /**
     * 뉴스 2depth 탭 (Medium, 14px / line 22px, 자간 -1.8%).
     * 박스 세로 22dp·좌우 패딩 14dp는 [NewsSecondDepthTabRow]에서 적용.
     */
    val newsSecondDepthTab: TextStyle = TextStyle(
        fontFamily = SapiensFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.018f).em
    )
    /**
     * 뉴스 > 국내·미국·AI 이슈 가로 스크롤 탭 (SUIT Semibold, 28sp / line 36, 자간 0).
     * 선택/비선택 모두 weight 동일, 색만 [NewsScreen]에서 구분.
     * 폰트: [SapiensFontFamily] (SUIT TTF 연동 시 [Typography]와 동일 경로로 갱신).
     */
    val newsSubTab: TextStyle = TextStyle(
        fontFamily = SapiensFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    val newsSubTabUnselected: TextStyle = newsSubTab
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
