package com.sapiens.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Figma — Frame `172:2` (sapiens, dev).
 * [디자인](https://www.figma.com/design/EaE3QXT7UV66RFZRXppOvi/sapiens?node-id=172-2&m=dev)
 *
 * MCP export는 `#F5F5F5` 베이스 + 방사형 워시 오버레이 조합이다.
 * Compose 단계에서는 동일한 톤으로 읽히도록 **세로 그라데이션**으로 근사한다.
 */
private val Figma172_2GradientTop = Color(0xFFFF9E7D)
private val Figma172_2GradientBottom = Color(0xFFFF6B6B)

/** 루트 화면 전체 배경 (1차 반영: 배경만). */
val figmaBackgroundBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(Figma172_2GradientTop, Figma172_2GradientBottom),
    )
