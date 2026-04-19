package com.sapiens.app.ui.briefing

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.ui.common.ArticleBottomSheet
import kotlinx.coroutines.launch
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.CardSpacing
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextSecondary

@Composable
fun BriefingScreen(
    viewModel: BriefingViewModel,
    bookmarksRepository: ArticleBookmarksRepository
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val hankyungBriefingArticles by viewModel.hankyungBriefingArticles.collectAsState()
    val maeilBriefingArticles by viewModel.maeilBriefingArticles.collectAsState()
    val usArticles by viewModel.usArticles.collectAsState()
    val marketIndices by viewModel.marketIndices.collectAsState()
    val marketUpdatedLabel by viewModel.marketUpdatedLabel.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val bookmarkEntries by bookmarksRepository.bookmarksFlow.collectAsState(initial = emptyList())
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
                    DomesticNewspapersBriefingCard(
                        hankyungArticles = hankyungBriefingArticles,
                        maeilArticles = maeilBriefingArticles,
                        onClickArticle = { selectedArticle = it },
                        modifier = Modifier.padding(bottom = Spacing.space8)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.space18))
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
                        indices = marketIndices
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.space24))
                    Text(
                        text = marketUpdatedLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.space20)
                    )
                    Spacer(modifier = Modifier.height(Spacing.space24))
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
                }
            )
        }
    }
}
