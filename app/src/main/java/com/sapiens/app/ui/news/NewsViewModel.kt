package com.sapiens.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.repository.FeedbackRepository
import com.sapiens.app.data.repository.NewsFeedType
import com.sapiens.app.data.repository.NewsRepository
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.data.store.BookmarkToggleResult
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository,
    private val bookmarksRepository: ArticleBookmarksRepository,
    private val feedbackRepository: FeedbackRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _domesticMarketNews = MutableStateFlow(MockData.NEWS_DOMESTIC_MARKET)
    val domesticMarketNews: StateFlow<List<Article>> = _domesticMarketNews.asStateFlow()

    private val _globalMarketNews = MutableStateFlow(MockData.NEWS_GLOBAL_MARKET)
    val globalMarketNews: StateFlow<List<Article>> = _globalMarketNews.asStateFlow()

    private val _aiIssueNews = MutableStateFlow(MockData.NEWS_AI_ISSUE)
    val aiIssueNews: StateFlow<List<Article>> = _aiIssueNews.asStateFlow()

    private val _overseasStocks = MutableStateFlow(emptyList<Article>())
    val overseasStocks: StateFlow<List<Article>> = _overseasStocks.asStateFlow()

    private val _overseasTech = MutableStateFlow(emptyList<Article>())
    val overseasTech: StateFlow<List<Article>> = _overseasTech.asStateFlow()

    val bookmarkedArticleIds: StateFlow<Set<String>> = bookmarksRepository.bookmarksFlow
        .map { entries -> entries.map { it.article.stableId() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            combine(
                repository.getNewsFeed(NewsFeedType.DOMESTIC_MARKET),
                repository.getNewsFeed(NewsFeedType.GLOBAL_MARKET),
                repository.getNewsFeed(NewsFeedType.AI_ISSUE)
            ) { domesticMarket, globalMarket, aiIssue ->
                Triple(domesticMarket, globalMarket, aiIssue)
            }
                .catch { e ->
                    Log.w(TAG, "domestic feeds combine", e)
                    emit(Triple(emptyList(), emptyList(), emptyList()))
                }
                .collect { (domesticMarket, globalMarket, aiIssue) ->
                    _domesticMarketNews.value =
                        domesticMarket.ifEmpty { MockData.NEWS_DOMESTIC_MARKET }
                    _globalMarketNews.value = globalMarket.ifEmpty { MockData.NEWS_GLOBAL_MARKET }
                    _aiIssueNews.value = aiIssue.ifEmpty { MockData.NEWS_AI_ISSUE }
                    _isLoading.value = false
                }
        }
        viewModelScope.launch {
            repository.getNewsFeed(NewsFeedType.OVERSEAS_STOCKS)
                .catch { e ->
                    Log.w(TAG, "overseas_stocks", e)
                    emit(emptyList())
                }
                .collect { list ->
                    _overseasStocks.value = list
                }
        }
        viewModelScope.launch {
            repository.getNewsFeed(NewsFeedType.OVERSEAS_TECH)
                .catch { e ->
                    Log.w(TAG, "overseas_tech", e)
                    emit(emptyList())
                }
                .collect { list ->
                    _overseasTech.value = list
                }
        }
    }

    fun toggleNewsBookmark(article: Article) {
        viewModelScope.launch {
            val id = article.stableId()
            try {
                when (
                    val r = bookmarksRepository.toggleBookmark(
                        article,
                        withFeedbackWhenAdding = true
                    )
                ) {
                    is BookmarkToggleResult.Added ->
                        if (r.withFeedbackSync) {
                            feedbackRepository.saveArticleLike(id, article.category)
                        }
                    is BookmarkToggleResult.Removed ->
                        if (r.hadFeedbackSync) {
                            feedbackRepository.deleteFeedback(id)
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "bookmark/feedback", e)
            }
        }
    }

    companion object {
        private const val TAG = "NewsViewModel"

        fun factory(
            repository: NewsRepository,
            bookmarksRepository: ArticleBookmarksRepository,
            feedbackRepository: FeedbackRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return NewsViewModel(
                        repository,
                        bookmarksRepository,
                        feedbackRepository
                    ) as T
                }
            }
    }
}
