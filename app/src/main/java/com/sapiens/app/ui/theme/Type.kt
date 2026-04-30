package com.sapiens.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Material [SapiensTypography] 외에 화면별로 자주 쓰는 문단 스타일.
 * 스타일 값은 피그마 360 기준이며, [SapiensTheme]에서 너비 비율·시스템 글자 크기 고정이 적용된다.
 */
object SapiensTextStyles {
    private val todayHeadlineTitleBase =
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.018f).em,
        )

    @Composable
    fun todayHeadlineTitle(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) { todayHeadlineTitleBase.scaleForFigmaFrame(s) }
    }

    private val newsSecondDepthTabBase =
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.018f).em,
        )

    @Composable
    fun newsSecondDepthTab(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) { newsSecondDepthTabBase.scaleForFigmaFrame(s) }
    }

    private val newsSubTabBase =
        TextStyle(
            fontFamily = SapiensFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        )

    @Composable
    fun newsSubTab(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) { newsSubTabBase.scaleForFigmaFrame(s) }
    }

    @Composable
    fun newsSubTabUnselected(): TextStyle = newsSubTab()

    @Composable
    fun briefingPublisherChip(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun morningCardHeadline(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun morningListRow(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.bodyMedium.copy(fontSize = 13.sp).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun briefingThemeHeadline(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 34.sp,
            ).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun marketIndexGroup(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.labelSmall.copy(fontSize = 9.sp).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun statCaption9(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.labelSmall.copy(fontSize = 9.sp).scaleForFigmaFrame(s)
        }
    }

    @Composable
    fun toggleLabel12(): TextStyle {
        val s = LocalFigmaFrameWidthScale.current
        return remember(s) {
            SapiensTypography.labelMedium.copy(fontSize = 12.sp).scaleForFigmaFrame(s)
        }
    }
}
