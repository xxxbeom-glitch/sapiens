package com.sapiens.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleBottomSheetKind
import com.sapiens.app.ui.common.ArticleMixedFeedCard
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.OnPrimaryFixed
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

private val domesticTabLabels = listOf("국내 증시", "해외 증시", "AI ISSUE")
private val overseasTabLabels = listOf("Stocks", "Technology")

@Composable
fun NewsRegionToggle(
    isOverseas: Boolean,
    onIsOverseasChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillSurface = Card.copy(alpha = 0.5f)
    val innerH = Spacing.space24
    Row(
        modifier = modifier
            .height(Spacing.space30)
            .clip(AppShapes.pill)
            .background(pillSurface)
            .padding(Spacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(innerH)
                .clip(AppShapes.pillInner)
                .background(if (!isOverseas) Accent else Color.Transparent)
                .clickable { onIsOverseasChange(false) }
                .padding(horizontal = Spacing.space12)
        ) {
            Text(
                text = "국내",
                color = if (!isOverseas) OnPrimaryFixed else TextSecondary,
                style = SapiensTextStyles.toggleLabel12
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(innerH)
                .clip(AppShapes.pillInner)
                .background(if (isOverseas) Accent else Color.Transparent)
                .clickable { onIsOverseasChange(true) }
                .padding(horizontal = Spacing.space12)
        ) {
            Text(
                text = "해외",
                color = if (isOverseas) OnPrimaryFixed else TextSecondary,
                style = SapiensTextStyles.toggleLabel12
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
    val domesticMarketNews by viewModel.domesticMarketNews.collectAsState()
    val globalMarketNews by viewModel.globalMarketNews.collectAsState()
    val aiIssueNews by viewModel.aiIssueNews.collectAsState()
    val overseasStocks by viewModel.overseasStocks.collectAsState()
    val overseasTech by viewModel.overseasTech.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val bookmarkedIds by viewModel.bookmarkedArticleIds.collectAsState()
    val context = LocalContext.current
    // 국내(3탭: 국내 증시·해외 증시·AI ISSUE)·해외(2탭) 인덱스를 분리해 두지 않으면, 국내 3번째 탭에서 해외로 바꿀 때
    // 한 프레임이라도 selectedTabIndex(2) >= 해외 tabCount(2)가 되어 PrimaryTabRow가 크래시난다.
    var domesticTabIndex by remember { mutableIntStateOf(0) }
    var overseasTabIndex by remember { mutableIntStateOf(0) }

    val tabLabels = if (isOverseas) overseasTabLabels else domesticTabLabels
    val selectedTabIndex = if (isOverseas) overseasTabIndex else domesticTabIndex

    val currentItems = when {
        isOverseas -> when (overseasTabIndex) {
            0 -> overseasStocks
            1 -> overseasTech
            else -> overseasStocks
        }
        else -> when (domesticTabIndex) {
            0 -> domesticMarketNews
            1 -> globalMarketNews
            else -> aiIssueNews
        }
    }
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
                            onClick = {
                                if (isOverseas) overseasTabIndex = index else domesticTabIndex = index
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = RowVertical)
                ) {
                    ArticleMixedFeedCard(
                        articles = displayItems,
                        onClickArticle = { selectedArticle = it },
                        topChipForArticle = { article ->
                            if (isOverseas) {
                                formatOverseasNewsSource(article.source).ifBlank { "해외" }
                            } else {
                                article.source.ifBlank { "국내" }
                            }
                        },
                        emptyStateText = "불러온 기사가 없습니다."
                    )
                }
            }
        }

        selectedArticle?.let { article ->
            val bookmarked = article.stableId() in bookmarkedIds
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { selectedArticle = null },
                isBookmarked = bookmarked,
                onBookmarkToggle = { viewModel.toggleNewsBookmark(article) },
                kind = ArticleBottomSheetKind.News,
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
