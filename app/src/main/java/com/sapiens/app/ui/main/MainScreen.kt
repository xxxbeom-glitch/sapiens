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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sapiens.app.ui.theme.LocalFigmaFrameWidthScale
import com.sapiens.app.ui.theme.scaledDp
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        var targetPage by remember { mutableStateOf(0) }
        val figmaScale = LocalFigmaFrameWidthScale.current
        val mainMenuTop = remember(figmaScale) { scaledDp(100f, figmaScale) }
        val mainMenuToCardGap = remember(figmaScale) { scaledDp(42f, figmaScale) }
        val headlineCardWidth = remember(figmaScale) { scaledDp(290f, figmaScale) }
        val headlineCardPeekWidth = remember(figmaScale) { scaledDp(14f, figmaScale) }
        val headlineCardHeight = remember(figmaScale) { scaledDp(443f, figmaScale) }
        val cardToIndicatorGap = remember(figmaScale) { scaledDp(20f, figmaScale) }
        val scope = rememberCoroutineScope()
        val pageCount = remember(briefingCards.size) { briefingCards.size }

        // 페이지 수 변경 시 현재 페이지를 유효 범위로 제한
        LaunchedEffect(pageCount) {
            if (pageCount <= 0) {
                headlinePagerPage = 0
                targetPage = 0
                return@LaunchedEffect
            }
            headlinePagerPage = headlinePagerPage.coerceIn(0, pageCount - 1)
            targetPage = targetPage.coerceIn(0, pageCount - 1)
        }

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
                val peekShiftDp = secondCardLeft - firstCardLeft

                // 카드 1장 이동량(현재 카드 ↔ 다음 카드) — 페이지 수와 무관
                val snapToNextPx =
                    remember(maxWidth, headlineCardWidth, headlineCardPeekWidth, density) {
                        with(density) {
                            (firstCardLeft - secondCardLeft).toPx()
                        }
                    }

                // snapToNextPx는 음수이므로, shiftPx는 양수.
                val shiftPx = -snapToNextPx
                val minDragPx = -shiftPx // 다음 카드로 (왼쪽 드래그)
                val maxDragPx = shiftPx // 이전 카드로 (오른쪽 드래그)
                val offsetAnim = remember(shiftPx) { Animatable(0f) }

                fun normalizeOngoingTransition() {
                    if (pageCount <= 1 || shiftPx == 0f) return
                    val maxIndex = pageCount - 1
                    val v = offsetAnim.value
                    // 진행 중이던 전환이 반 이상 넘어갔다면, 그 방향으로 페이지를 먼저 확정하고
                    // 오프셋을 새 페이지 기준으로 보정해 드래그를 자연스럽게 이어가게 한다.
                    if (v <= -shiftPx * 0.5f && headlinePagerPage < maxIndex) {
                        headlinePagerPage = (headlinePagerPage + 1).coerceIn(0, maxIndex)
                        targetPage = headlinePagerPage
                        scope.launch { offsetAnim.snapTo(v + shiftPx) }
                    } else if (v >= shiftPx * 0.5f && headlinePagerPage > 0) {
                        headlinePagerPage = (headlinePagerPage - 1).coerceIn(0, maxIndex)
                        targetPage = headlinePagerPage
                        scope.launch { offsetAnim.snapTo(v - shiftPx) }
                    }
                }

                fun animateToPage(page: Int) {
                    if (pageCount <= 0) return
                    val maxIndex = (pageCount - 1).coerceAtLeast(0)
                    val clamped = page.coerceIn(0, maxIndex)
                    targetPage = clamped

                    val pageDelta = clamped - headlinePagerPage
                    val targetOffsetPx =
                        when {
                            pageDelta > 0 -> minDragPx
                            pageDelta < 0 -> maxDragPx
                            else -> 0f
                        }

                    scope.launch {
                        offsetAnim.stop() // 기존 애니메이션 중단
                        offsetAnim.animateTo(
                            targetValue = targetOffsetPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                        // 애니메이션 종료 시 페이지 확정 후 오프셋을 0으로 정규화
                        headlinePagerPage = clamped
                        offsetAnim.snapTo(0f)
                    }
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
                            val next = (offsetAnim.value + delta).coerceIn(minDragPx, maxDragPx)
                            scope.launch { offsetAnim.snapTo(next) }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .draggable(
                                    state = draggableState,
                                    orientation = Orientation.Horizontal,
                                    onDragStarted = { _: Offset ->
                                        // 애니메이션 중에도 연속 스와이프가 가능하도록 즉시 중단
                                        scope.launch { offsetAnim.stop() }
                                        normalizeOngoingTransition()
                                    },
                                    onDragStopped = { velocity ->
                                        if (pageCount <= 1) return@draggable

                                        val velocityThresholdPxPerSec = 200f
                                        val distanceThresholdPx = shiftPx * 0.15f

                                        val pageDelta = when {
                                            velocity <= -velocityThresholdPxPerSec -> +1
                                            velocity >= velocityThresholdPxPerSec -> -1
                                            offsetAnim.value <= -distanceThresholdPx -> +1
                                            offsetAnim.value >= distanceThresholdPx -> -1
                                            else -> 0
                                        }

                                        val maxPageIndex = (pageCount - 1).coerceAtLeast(0)
                                        val nextPage = (headlinePagerPage + pageDelta).coerceIn(0, maxPageIndex)
                                        animateToPage(nextPage)
                                    },
                                ),
                        ) {
                            val dragOffsetDp = with(density) { offsetAnim.value.toDp() }
                            val progress =
                                remember(offsetAnim.value, shiftPx) {
                                    if (shiftPx == 0f) 0f else (abs(offsetAnim.value) / shiftPx).coerceIn(0f, 1f)
                                }
                            fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

                            val neighborScale = 0.95f
                            val isDraggingToNext = offsetAnim.value < 0f
                            val isDraggingToPrev = offsetAnim.value > 0f
                            val currentScale =
                                if (isDraggingToNext || isDraggingToPrev) lerp(1f, neighborScale, progress) else 1f
                            val nextScale = if (isDraggingToNext) lerp(neighborScale, 1f, progress) else neighborScale
                            val prevScale = if (isDraggingToPrev) lerp(neighborScale, 1f, progress) else neighborScale

                            val currentIndex = headlinePagerPage
                            val prevIndex = currentIndex - 1
                            val nextIndex = currentIndex + 1

                            // 이전 카드(좌측) — 스와이프 back용
                            briefingCards.getOrNull(prevIndex)?.let { prevCard ->
                                HeadlinePreviewCard(
                                    card = prevCard,
                                    isBookmarked = prevCard.cardId in savedCardIds,
                                    onBookmarkToggle = onBookmarkToggle,
                                    modifier = Modifier
                                        .offset(x = (firstCardLeft - peekShiftDp) + dragOffsetDp)
                                        .graphicsLayer {
                                            scaleX = prevScale
                                            scaleY = prevScale
                                        },
                                )
                            }

                            // 현재 카드(중앙)
                            briefingCards.getOrNull(currentIndex)?.let { currentCard ->
                                HeadlinePreviewCard(
                                    card = currentCard,
                                    isBookmarked = currentCard.cardId in savedCardIds,
                                    onBookmarkToggle = onBookmarkToggle,
                                    modifier = Modifier
                                        .offset(x = firstCardLeft + dragOffsetDp)
                                        .graphicsLayer {
                                            scaleX = currentScale
                                            scaleY = currentScale
                                        },
                                )
                            }

                            // 다음 카드(우측 peek 유지)
                            val nextCard = briefingCards.getOrNull(nextIndex)
                            if (nextCard != null) {
                                HeadlinePreviewCard(
                                    card = nextCard,
                                    isBookmarked = nextCard.cardId in savedCardIds,
                                    onBookmarkToggle = onBookmarkToggle,
                                    modifier = Modifier
                                        .offset(x = secondCardLeft + dragOffsetDp)
                                        .graphicsLayer {
                                            scaleX = nextScale
                                            scaleY = nextScale
                                        },
                                )
                            } else {
                                // 마지막 페이지에서 peek용 카드가 없으면 아예 렌더링하지 않음
                                Box(modifier = Modifier.width(0.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(cardToIndicatorGap))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (pageCount > 1) {
                            HeadlinePagerIndicator(
                                currentPage = headlinePagerPage.coerceIn(0, pageCount - 1),
                                pageCount = briefingCards.size,
                            )
                        }
                    }
                }
            }
        }
    }
}
