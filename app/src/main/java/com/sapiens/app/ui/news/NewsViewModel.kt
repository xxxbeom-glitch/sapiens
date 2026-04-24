package com.sapiens.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.repository.NewsFeedType
import com.sapiens.app.data.repository.NewsRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _domesticMarketNews = MutableStateFlow(MockData.NEWS_DOMESTIC_MARKET)
    val domesticMarketNews: StateFlow<List<Article>> = _domesticMarketNews.asStateFlow()

    private val _globalMarketNews = MutableStateFlow(MockData.NEWS_GLOBAL_MARKET)
    val globalMarketNews: StateFlow<List<Article>> = _globalMarketNews.asStateFlow()

    private val _aiIssueNews = MutableStateFlow(emptyList<Article>())
    val aiIssueNews: StateFlow<List<Article>> = _aiIssueNews.asStateFlow()

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
                    _domesticMarketNews.value = domesticMarket.takeIf { it.isNotEmpty() }
                        ?: _domesticMarketNews.value.takeIf { it.isNotEmpty() }
                        ?: MockData.NEWS_DOMESTIC_MARKET
                    _globalMarketNews.value = globalMarket.takeIf { it.isNotEmpty() }
                        ?: _globalMarketNews.value.takeIf { it.isNotEmpty() }
                        ?: MockData.NEWS_GLOBAL_MARKET
                    _aiIssueNews.value = aiIssue.takeIf { it.isNotEmpty() }
                        ?: _aiIssueNews.value
                    _isLoading.value = false
                }
        }
    }

    companion object {
        private const val TAG = "NewsViewModel"

        fun factory(
            repository: NewsRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return NewsViewModel(
                        repository
                    ) as T
                }
            }
    }
}
