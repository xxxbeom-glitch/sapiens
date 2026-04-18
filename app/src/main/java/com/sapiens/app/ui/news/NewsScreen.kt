package com.sapiens.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

private val domesticTabLabels = listOf("실시간 속보", "많이 본 뉴스", "주요 뉴스")
private val overseasTabLabels = listOf("Stocks", "Technology")

@Composable
fun NewsRegionToggle(
    isOverseas: Boolean,
    onIsOverseasChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillSurface = Card.copy(alpha = 0.5f)
    val innerH = 24.dp
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(pillSurface)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(innerH)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!isOverseas) Accent else Color.Transparent)
                .clickable { onIsOverseasChange(false) }
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "국내",
                color = if (!isOverseas) Color.White else TextSecondary,
                fontSize = 12.sp
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(innerH)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isOverseas) Accent else Color.Transparent)
                .clickable { onIsOverseasChange(true) }
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "해외",
                color = if (isOverseas) Color.White else TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    isOverseas: Boolean
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val realtimeNews by viewModel.realtimeNews.collectAsState()
    val popularNews by viewModel.popularNews.collectAsState()
    val mainNews by viewModel.mainNews.collectAsState()
    val overseasStocks by viewModel.overseasStocks.collectAsState()
    val overseasTech by viewModel.overseasTech.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isOverseas) {
        selectedTabIndex = 0
    }

    val tabLabels = if (isOverseas) overseasTabLabels else domesticTabLabels

    val currentItems = when {
        isOverseas -> when (selectedTabIndex) {
            0 -> overseasStocks
            1 -> overseasTech
            else -> overseasStocks
        }
        else -> when (selectedTabIndex) {
            0 -> realtimeNews
            1 -> popularNews
            else -> mainNews
        }
    }
    val showRank = !isOverseas && selectedTabIndex == 1
    val displayItems = if (isOverseas) currentItems.map { it.withKoreanHeadlineFallback() } else currentItems

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Background,
                    contentColor = Accent
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val selected = selectedTabIndex == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = label,
                                    color = if (selected) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = RowVertical)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Card),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    itemsIndexed(
                        items = displayItems,
                        key = { index, article -> "$index-${article.headline}" }
                    ) { index, article ->
                        NewsFeedRow(
                            item = article,
                            rank = if (showRank) index + 1 else null,
                            onClick = { selectedArticle = article }
                        )
                        if (index < displayItems.lastIndex) {
                            HorizontalDivider(
                                color = TextSecondary.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        selectedArticle?.let { article ->
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { selectedArticle = null }
            )
        }
    }
}

private fun Article.withKoreanHeadlineFallback(): Article {
    val headlineTrimmed = headline.trim()
    if (!headlineTrimmed.looksEnglishOnlyHeadline()) return this
    val translatedFallback = summaryPoints.firstOrNull { it.containsHangul() }?.trim()
    if (translatedFallback.isNullOrBlank()) return this
    return copy(headline = translatedFallback)
}

private fun String.looksEnglishOnlyHeadline(): Boolean {
    if (isBlank()) return false
    val hasLatin = any { it in 'a'..'z' || it in 'A'..'Z' }
    val hasHangul = containsHangul()
    return hasLatin && !hasHangul
}

private fun String.containsHangul(): Boolean = any { it in '\uAC00'..'\uD7A3' }
