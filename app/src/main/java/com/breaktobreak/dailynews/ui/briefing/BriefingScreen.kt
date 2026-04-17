package com.breaktobreak.dailynews.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.ui.common.ArticleBottomSheet
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.CardSpacing
import com.breaktobreak.dailynews.ui.theme.TextSecondary

@Composable
fun BriefingScreen() {
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val pagerState = rememberPagerState(pageCount = { MockData.morningArticles.size })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        item {
            SectionLabel(
                title = "아침 브리핑"
            )
        }

        item {
            MorningCardPager(
                articles = MockData.morningArticles,
                pagerState = pagerState,
                onClickArticle = { selectedArticle = it }
            )
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel(title = "미국 시황")
        }

        item {
            USMarketIndicatorsCard(
                indicators = MockData.marketIndicators,
            )
        }

        item {
            Spacer(modifier = Modifier.height(CardSpacing))
            USMajorArticlesCard(
                articles = MockData.usArticles,
                onClickArticle = { selectedArticle = it }
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

    selectedArticle?.let { article ->
        ArticleBottomSheet(
            article = article,
            onDismissRequest = { selectedArticle = null }
        )
    }
}
