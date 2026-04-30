package com.sapiens.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 피그마 기준 프레임 너비(@1x). 화면 너비와의 비율로 텍스트를 스케일한다. */
const val FigmaFrameWidthDp: Float = 360f

/**
 * 피그마 360 프레임에서의 dp 값을 `LocalFigmaFrameWidthScale`과 동일 비율로 변환.
 * 카드·간격 등 레이아웃 고정값에 사용한다.
 */
fun scaledDp(rawDpAtFigma360: Float, scale: Float): Dp =
    (rawDpAtFigma360 * scale).dp

/**
 * 피그마 360 프레임 대비 현재 단말 가로(dp) 비율.
 * [SapiensTheme]에서 주입; 미사용 시 1f.
 */
val LocalFigmaFrameWidthScale = compositionLocalOf { 1f }

/**
 * 기준 프레임 대비 너비 비율. 회전·멀티 윈도 시 [LocalConfiguration] 변화에 맞춰 갱신된다.
 */
@Composable
fun rememberFigmaFrameWidthScale(): Float {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    return remember(screenWidthDp) { screenWidthDp / FigmaFrameWidthDp }
}

/** 피그마 @1x 숫자(예: 18)를 현재 기기의 프레임 비율에 맞게 `sp`로 변환. */
@Composable
fun designSp(pxOrSpValue: Float): TextUnit {
    val s = LocalFigmaFrameWidthScale.current
    return remember(pxOrSpValue, s) { (pxOrSpValue * s).sp }
}

private fun TextUnit.scaleSpOrKeep(scale: Float): TextUnit =
    when {
        scale == 1f -> this
        this == TextUnit.Unspecified -> this
        this.isSp -> (this.value * scale).sp
        else -> this
    }

private fun TextUnit.scaleLetterSpacing(scale: Float): TextUnit =
    when {
        scale == 1f -> this
        this == TextUnit.Unspecified -> this
        this.isSp -> (this.value * scale).sp
        this.isEm -> this
        else -> this
    }

private fun TextUnit.scaleLineHeight(scale: Float): TextUnit =
    when {
        scale == 1f -> this
        this == TextUnit.Unspecified -> this
        this.isSp -> (this.value * scale).sp
        this.isEm -> this
        else -> this
    }

/**
 * 피그마(@360 너비)에서 정의한 [TextStyle]을 현재 기기 너비 비율로 스케일한다.
 * (시스템 글자 크기는 루트 [androidx.compose.ui.platform.LocalDensity]에서 별도 고정.)
 */
fun TextStyle.scaleForFigmaFrame(scale: Float): TextStyle {
    if (scale == 1f) return this
    return copy(
        fontSize = fontSize.scaleSpOrKeep(scale),
        lineHeight = lineHeight.scaleLineHeight(scale),
        letterSpacing = letterSpacing.scaleLetterSpacing(scale),
    )
}

fun Typography.scaleForFigmaFrame(scale: Float): Typography {
    if (scale == 1f) return this
    return copy(
        displayLarge = displayLarge.scaleForFigmaFrame(scale),
        displayMedium = displayMedium.scaleForFigmaFrame(scale),
        displaySmall = displaySmall.scaleForFigmaFrame(scale),
        headlineLarge = headlineLarge.scaleForFigmaFrame(scale),
        headlineMedium = headlineMedium.scaleForFigmaFrame(scale),
        headlineSmall = headlineSmall.scaleForFigmaFrame(scale),
        titleLarge = titleLarge.scaleForFigmaFrame(scale),
        titleMedium = titleMedium.scaleForFigmaFrame(scale),
        titleSmall = titleSmall.scaleForFigmaFrame(scale),
        bodyLarge = bodyLarge.scaleForFigmaFrame(scale),
        bodyMedium = bodyMedium.scaleForFigmaFrame(scale),
        bodySmall = bodySmall.scaleForFigmaFrame(scale),
        labelLarge = labelLarge.scaleForFigmaFrame(scale),
        labelMedium = labelMedium.scaleForFigmaFrame(scale),
        labelSmall = labelSmall.scaleForFigmaFrame(scale),
    )
}
