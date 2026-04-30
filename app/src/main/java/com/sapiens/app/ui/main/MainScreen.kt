package com.sapiens.app.ui.main

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.scaledDp
import kotlinx.coroutines.launch

import com.sapiens.app.data.model.BriefingCard

@Composable
fun MainScreen(
    navigateToSectionKey: String? = null,
    onNavigateToSectionConsumed: () -> Unit = {},
    briefingCards: List<BriefingCard> = emptyList(),
    savedCardIds: Set<String> = emptySet(),
    onBookmarkToggle: (cardId: String) -> Unit = {},
) {
    LaunchedEffect(navigateToSectionKey) {
        if (navigateToSectionKey != null) {
            onNavigateToSectionConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        var selectedMenu by remember { mutableStateOf(MainTopMenuItem.Today) }
        var headlinePagerPage by remember { mutableStateOf(0) }
        val figmaScale = LocalFigmaFrameWidthScale.current
        val mainMenuTop = remember(figmaScale) { scaledDp(100f, figmaScale) }
        val mainMenuToCardGap = remember(figmaScale) { scaledDp(42f, figmaScale) }
        val headlineCardWidth = remember(figmaScale) { scaledDp(290f, figmaScale) }
        val headlineCardPeekWidth = remember(figmaScale) { scaledDp(14f, figmaScale) }
        val headlineCardHeight = remember(figmaScale) { scaledDp(443f, figmaScale) }
        val cardToIndicatorGap = remember(figmaScale) { scaledDp(20f, figmaScale) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {
            MainTopMenuBar(
                selected = selectedMenu,
                onSelect = { selectedMenu = it },
                modifier = Modifier.padding(top = mainMenuTop),
            )
            Spacer(modifier = Modifier.height(mainMenuToCardGap))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val firstCardLeft = (maxWidth - headlineCardWidth) / 2f
                val secondCardLeft = (maxWidth - headlineCardPeekWidth).coerceAtLeast(0.dp)
                val density = LocalDensity.current

                // page 0: 첫 카드 중앙 + 다음 카드 우측 14dp 노출
                // page 1: 두 번째 카드 중앙(첫 카드는 왼쪽으로 일부만 노출)
                val snapToPage1Px =
                    remember(maxWidth, headlineCardWidth, headlineCardPeekWidth, density) {
                        with(density) {
                            // page1의 left가 중앙 정렬 위치(firstCardLeft)로 오도록 전체를 이동
                            (firstCardLeft - secondCardLeft).toPx()
                        }
                    }

                val minOffsetPx = snapToPage1Px // 음수
                val maxOffsetPx = 0f
                val offsetAnim = remember(minOffsetPx) { Animatable(0f) }
                LaunchedEffect(minOffsetPx, headlinePagerPage) {
                    offsetAnim.snapTo(if (headlinePagerPage == 1) minOffsetPx else 0f)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headlineCardHeight),
                    ) {
                        val draggableState = rememberDraggableState { delta ->
                            val next = (offsetAnim.value + delta).coerceIn(minOffsetPx, maxOffsetPx)
                            scope.launch { offsetAnim.snapTo(next) }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .draggable(
                                    state = draggableState,
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = { velocity ->
                                        val velocityThresholdPxPerSec = 900f
                                        val byVelocity = when {
                                            velocity <= -velocityThresholdPxPerSec -> true
                                            velocity >= velocityThresholdPxPerSec -> false
                                            else -> null
                                        }
                                        val distanceThreshold = minOffsetPx * 0.25f
                                        val shouldGoPage1 = byVelocity ?: (offsetAnim.value < distanceThreshold)
                                        val target = if (shouldGoPage1) minOffsetPx else 0f
                                        headlinePagerPage = if (shouldGoPage1) 1 else 0
                                        scope.launch {
                                            offsetAnim.animateTo(
                                                targetValue = target,
                                                animationSpec = tween(
                                                    durationMillis = 260,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                            )
                                        }
                                    },
                                ),
                        ) {
                            val dragOffsetDp = with(density) { offsetAnim.value.toDp() }
                            // 첫 번째 카드
                            HeadlinePreviewCard(
                                card = briefingCards.getOrNull(headlinePagerPage * 2),
                                isBookmarked = briefingCards.getOrNull(headlinePagerPage * 2)
                                    ?.cardId?.let { it in savedCardIds } == true,
                                onBookmarkToggle = onBookmarkToggle,
                                modifier = Modifier.offset(x = firstCardLeft + dragOffsetDp),
                            )
                            // 두 번째 카드 (우측 peek)
                            HeadlinePreviewCard(
                                card = briefingCards.getOrNull(headlinePagerPage * 2 + 1),
                                isBookmarked = briefingCards.getOrNull(headlinePagerPage * 2 + 1)
                                    ?.cardId?.let { it in savedCardIds } == true,
                                onBookmarkToggle = onBookmarkToggle,
                                modifier = Modifier.offset(x = secondCardLeft + dragOffsetDp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(cardToIndicatorGap))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val pageCount = ((briefingCards.size + 1) / 2).coerceAtLeast(1)
                        HeadlinePagerIndicator(
                            currentPage = headlinePagerPage,
                            pageCount = pageCount,
                        )
                    }
                }
            }
        }
    }
}
