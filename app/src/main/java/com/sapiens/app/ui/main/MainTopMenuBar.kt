package com.sapiens.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.SapiensFontFamily
import com.sapiens.app.ui.theme.scaleForFigmaFrame
import com.sapiens.app.ui.theme.scaledDp

/** 상단 메뉴 항목 (TODAY / BOOKMARK / SETTING). */
enum class MainTopMenuItem {
    Today,
    Bookmark,
    Setting,
}

private val MainMenuTextStyle =
    TextStyle(
        fontFamily = SapiensFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 34.sp,
    )

private const val DesignMainMenuItemSpacingDp = 12f
private val MenuActiveColor = Color(0xFFFFFFFF)
private val MenuInactiveColor = Color(0xFFFFFFFF).copy(alpha = 0.2f)

/**
 * 메인 상단 메뉴 (가이드: SUIT Bold 18 / line 34, 항목 간 12, 그룹 가로 중앙).
 * 활성 #ffffff 100%, 비활성 #ffffff 20%.
 */
@Composable
fun MainTopMenuBar(
    selected: MainTopMenuItem,
    onSelect: (MainTopMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalFigmaFrameWidthScale.current
    val menuItemSpacing = remember(s) { scaledDp(DesignMainMenuItemSpacingDp, s) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTopMenuLabel(
            label = "TODAY",
            selected = selected == MainTopMenuItem.Today,
            onClick = { onSelect(MainTopMenuItem.Today) },
        )
        Spacer(modifier = Modifier.width(menuItemSpacing))
        MainTopMenuLabel(
            label = "BOOKMARK",
            selected = selected == MainTopMenuItem.Bookmark,
            onClick = { onSelect(MainTopMenuItem.Bookmark) },
        )
        Spacer(modifier = Modifier.width(menuItemSpacing))
        MainTopMenuLabel(
            label = "SETTING",
            selected = selected == MainTopMenuItem.Setting,
            onClick = { onSelect(MainTopMenuItem.Setting) },
        )
    }
}

@Composable
private fun MainTopMenuLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val s = LocalFigmaFrameWidthScale.current
    val style =
        remember(s, selected) {
            MainMenuTextStyle.scaleForFigmaFrame(s).copy(
                color = if (selected) MenuActiveColor else MenuInactiveColor,
            )
        }
    Text(
        text = label,
        style = style,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}
