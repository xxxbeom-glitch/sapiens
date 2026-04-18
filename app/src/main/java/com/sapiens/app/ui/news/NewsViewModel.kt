package com.sapiens.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.repository.NewsFeedType
import com.sapiens.app.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _realtimeNews = MutableStateFlow(MockData.NEWS_REALTIME)
    val realtimeNews: StateFlow<List<Article>> = _realtimeNews.asStateFlow()

    private val _popularNews = MutableStateFlow(MockData.NEWS_POPULAR)
    val popularNews: StateFlow<List<Article>> = _popularNews.asStateFlow()

    private val _mainNews = MutableStateFlow(MockData.NEWS_MAIN)
    val mainNews: StateFlow<List<Article>> = _mainNews.asStateFlow()

    private val _overseasStocks = MutableStateFlow(emptyList<Article>())
    val overseasStocks: StateFlow<List<Article>> = _overseasStocks.asStateFlow()

    private val _overseasTech = MutableStateFlow(emptyList<Article>())
    val overseasTech: StateFlow<List<Article>> = _overseasTech.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getNewsFeed(NewsFeedType.REALTIME),
                repository.getNewsFeed(NewsFeedType.POPULAR),
                repository.getNewsFeed(NewsFeedType.MAIN)
            ) { realtime, popular, main ->
                Triple(realtime, popular, main)
            }.collect { (realtime, popular, main) ->
                _realtimeNews.value = realtime.ifEmpty { MockData.NEWS_REALTIME }
                _popularNews.value = popular.ifEmpty { MockData.NEWS_POPULAR }
                _mainNews.value = main.ifEmpty { MockData.NEWS_MAIN }
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.getNewsFeed(NewsFeedType.OVERSEAS_STOCKS).collect { list ->
                _overseasStocks.value = list
            }
        }
        viewModelScope.launch {
            repository.getNewsFeed(NewsFeedType.OVERSEAS_TECH).collect { list ->
                _overseasTech.value = list
            }
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return NewsViewModel(repository) as T
                }
            }
    }
}
