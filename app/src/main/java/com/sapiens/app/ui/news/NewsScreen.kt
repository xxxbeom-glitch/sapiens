package com.sapiens.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

private val newsTabs = listOf("실시간 속보", "많이 본 뉴스", "주요 뉴스")

@Composable
fun NewsScreen(viewModel: NewsViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val realtimeNews by viewModel.realtimeNews.collectAsState()
    val popularNews by viewModel.popularNews.collectAsState()
    val mainNews by viewModel.mainNews.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val currentItems = when (selectedTabIndex) {
        0 -> realtimeNews
        1 -> popularNews
        else -> mainNews
    }
    val showRank = selectedTabIndex == 1

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
                    newsTabs.forEachIndexed { index, label ->
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
                        items = currentItems,
                        key = { index, article -> "$index-${article.headline}" }
                    ) { index, article ->
                        NewsFeedRow(
                            item = article,
                            rank = if (showRank) index + 1 else null,
                            onClick = { selectedArticle = article }
                        )
                        if (index < currentItems.lastIndex) {
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
