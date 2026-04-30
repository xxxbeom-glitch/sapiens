package com.sapiens.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleMixedFeedCard
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary

private data class UsCategory(
    val label: String,
    val documentId: String,
)

private val usCategories = listOf(
    UsCategory(
        label = "소프트웨어 및 인터넷",
        documentId = "us_software_internet"
    ),
    UsCategory(
        label = "반도체 및 하드웨어",
        documentId = "us_semiconductor_hw"
    ),
    UsCategory(
        label = "항공우주 및 모빌리티",
        documentId = "us_aerospace_mobility"
    ),
    UsCategory(
        label = "금융 및 자본 시장",
        documentId = "us_finance_capital"
    ),
)

private data class KrCategory(
    val label: String,
    val documentId: String,
)

private val krCategories = listOf(
    KrCategory(
        label = "국내증시",
        documentId = "kr_domestic_stock"
    ),
    KrCategory(
        label = "국내경제",
        documentId = "kr_domestic_economy"
    ),
)

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val headlineNews by viewModel.headlineNews.collectAsState()

    /** (기사, 뉴스 탭 페이지 0·1·2) — 바텀시트 등에서 탭 컨텍스트가 필요할 때 사용 */
    var sheetArticleAndPage by remember { mutableStateOf<Pair<Article, Int>?>(null) }
    val context = LocalContext.current
    var regionChipIndex by remember { mutableIntStateOf(0) }
    var secondDepthTabIndex by remember { mutableIntStateOf(0) }
    val secondDepthLabels = remember(regionChipIndex) {
        if (regionChipIndex == 1) {
            usCategories.map { it.label }
        } else {
            krCategories.map { it.label }
        }
    }

    androidx.compose.runtime.LaunchedEffect(regionChipIndex) {
        secondDepthTabIndex = 0
    }

    androidx.compose.runtime.LaunchedEffect(regionChipIndex, secondDepthTabIndex) {
        if (regionChipIndex == 1) {
            val idx = secondDepthTabIndex.coerceIn(0, usCategories.lastIndex)
            viewModel.setSelectedNewsDocId(usCategories[idx].documentId)
        } else {
            val idx = secondDepthTabIndex.coerceIn(0, krCategories.lastIndex)
            viewModel.setSelectedNewsDocId(krCategories[idx].documentId)
        }
    }

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
                    .padding(top = Spacing.space36)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.space20,
                            end = Spacing.space20,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#오늘의 헤드라인",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Spacing.space12),
                        color = TextPrimary,
                        style = SapiensTextStyles.todayHeadlineTitle(),
                        maxLines = 2,
                    )
                    KoreaUsRegionChips(
                        selectedIndex = regionChipIndex,
                        onSelect = { regionChipIndex = it },
                    )
                }

                Spacer(Modifier.height(Spacing.space28))

                NewsSecondDepthTabRow(
                    labels = secondDepthLabels,
                    selectedIndex = secondDepthTabIndex,
                    onSelect = { secondDepthTabIndex = it },
                    modifier = Modifier.padding(bottom = Spacing.space18),
                )
                ArticleMixedFeedCard(
                    articles = headlineNews,
                    onClickArticle = { sheetArticleAndPage = it to 0 },
                    modifier = Modifier
                        .fillMaxSize()
                        // 하단 앱바(Scaffold bottomBar) 위에서 16dp 정도 여유.
                        .padding(bottom = Spacing.space16),
                    topChipForArticle = { article ->
                        val chip = newsPublisherChipText(article.source)
                        when {
                            chip.isNotBlank() -> chip
                            regionChipIndex == 1 -> "해외"
                            else -> "국내"
                        }
                    },
                    emptyStateText = "불러온 기사가 없습니다."
                )
            }
        }

        sheetArticleAndPage?.let { (article, _) ->
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { sheetArticleAndPage = null },
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
