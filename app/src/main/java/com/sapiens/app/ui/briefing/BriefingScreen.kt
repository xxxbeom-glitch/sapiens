package com.sapiens.app.ui.briefing

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleBottomSheetKind
import com.sapiens.app.ui.common.ArticleMixedFeedCard
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import kotlinx.coroutines.launch
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Spacing

@Composable
fun BriefingScreen(
    viewModel: BriefingViewModel,
    bookmarksRepository: ArticleBookmarksRepository
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val domesticBriefingArticles by viewModel.domesticBriefingArticles.collectAsState()
    val usArticles by viewModel.usArticles.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val bookmarkEntries by bookmarksRepository.bookmarksFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.space8)
            ) {
                item {
                    SectionLabel(
                        title = "국내 주요뉴스"
                    )
                }

                item {
                    ArticleMixedFeedCard(
                        articles = domesticBriefingArticles,
                        onClickArticle = { selectedArticle = it },
                        topChipForArticle = { it.source.ifBlank { "국내" } },
                        emptyStateText = null,
                        modifier = Modifier.padding(bottom = Spacing.space8)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.space18))
                    SectionLabel(title = "해외 주요뉴스")
                }

                item {
                    ArticleMixedFeedCard(
                        articles = usArticles,
                        onClickArticle = { selectedArticle = it },
                        topChipForArticle = { it.source.ifBlank { "해외" } },
                        emptyStateText = "불러온 기사가 없습니다."
                    )
                }
            }
        }

        selectedArticle?.let { article ->
            val bookmarked = bookmarkEntries.any { it.article.stableId() == article.stableId() }
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { selectedArticle = null },
                isBookmarked = bookmarked,
                onBookmarkToggle = {
                    scope.launch {
                        try {
                            bookmarksRepository.toggleBookmark(
                                article,
                                withFeedbackWhenAdding = false
                            )
                        } catch (e: Exception) {
                            Log.w("BriefingScreen", "bookmark", e)
                        }
                    }
                },
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
