package com.sapiens.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleMixedFeedCard
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val tabLabels = listOf("#국내 증시", "#미국 증시", "AI ISSUE")

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val domesticMarketNews by viewModel.domesticMarketNews.collectAsState()
    val globalMarketNews by viewModel.globalMarketNews.collectAsState()
    val aiIssueNews by viewModel.aiIssueNews.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { tabLabels.size })
    val scope = rememberCoroutineScope()

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
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Background,
                    contentColor = Accent
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val selected = pagerState.currentPage == index
                        Tab(
                            selected = selected,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = label,
                                    color = if (selected) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val pageItems = when (page) {
                        0 -> domesticMarketNews
                        1 -> globalMarketNews
                        else -> aiIssueNews
                    }
                    key(page) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(vertical = RowVertical)
                        ) {
                            ArticleMixedFeedCard(
                                articles = pageItems,
                                onClickArticle = { selectedArticle = it },
                                topChipForArticle = { article ->
                                    val chip = newsPublisherChipText(article.source)
                                    when {
                                        chip.isNotBlank() -> chip
                                        page == 1 -> "해외"
                                        else -> "국내"
                                    }
                                },
                                emptyStateText = "불러온 기사가 없습니다."
                            )
                        }
                    }
                }
            }
        }

        selectedArticle?.let { article ->
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { selectedArticle = null },
                onOpenOriginalArticle = {
                    val url = article.url.trim()
                    if (url.isNotBlank()) {
                        val toOpen = transformNaverFinanceNewsReadUrlForMobile(url)
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(toOpen))
                            )
                        }
                    }
                }
            )
        }
    }
}
