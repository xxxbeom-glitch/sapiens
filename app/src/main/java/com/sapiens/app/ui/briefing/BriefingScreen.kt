package com.sapiens.app.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.CardSpacing
import com.sapiens.app.ui.theme.TextSecondary

@Composable
fun BriefingScreen(viewModel: BriefingViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val morningArticles by viewModel.morningArticles.collectAsState()
    val usArticles by viewModel.usArticles.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { morningArticles.size.coerceAtLeast(1) }
    )

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                item {
                    SectionLabel(
                        title = "국내 주요뉴스"
                    )
                }

                item {
                    MorningCardPager(
                        articles = morningArticles,
                        pagerState = pagerState,
                        onClickArticle = { selectedArticle = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionLabel(title = "해외 주요뉴스")
                }

                item {
                    USMajorArticlesCard(
                        articles = usArticles,
                        onClickArticle = { selectedArticle = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(CardSpacing))
                    SectionLabel(title = "대표 지수")
                }

                item {
                    MarketIndexGrid(
                        indices = MockData.MARKET_INDEX_LIST
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "AI 요약 · 06:00 업데이트",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
