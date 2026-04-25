package com.sapiens.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.sapiens.app.ui.theme.AppRadius
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/** 2depth 탭 패딩 (Figma @1x). */
private val SecondDepthTabHorizontalPadding = Spacing.space14 + Spacing.space2
private val SecondDepthTabVerticalPadding = Spacing.space5 + Spacing.space2

/**
 * 뉴스 2depth 가로 탭. 라벨·선택 로직은 상위 [remember]로만 연동(데이터 미결합).
 */
@Composable
fun NewsSecondDepthTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma: 코너 곡률 99px (@1x → [AppRadius.radius99]).
    val pillShape = RoundedCornerShape(AppRadius.radius99)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = Spacing.space20),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(labels) { index, label ->
            val selected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(if (selected) Card else Color.Transparent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            onSelect(index)
                            // 선택한 탭이 화면 앞쪽으로 오도록, 한 칸 정도 앞에 여유를 두고 스크롤.
                            scope.launch { listState.animateScrollToItem(maxOf(index - 1, 0)) }
                        },
                    )
                    .padding(
                        horizontal = SecondDepthTabHorizontalPadding,
                        vertical = SecondDepthTabVerticalPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) TextPrimary else TextPrimary.copy(alpha = 0.3f),
                    style = SapiensTextStyles.newsSecondDepthTab,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
