package com.sapiens.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.repository.NewsFeedType
import com.sapiens.app.data.repository.NewsRepository
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _domesticMarketNews = MutableStateFlow(MockData.NEWS_DOMESTIC_MARKET)
    val domesticMarketNews: StateFlow<List<Article>> = _domesticMarketNews.asStateFlow()

    private val selectedUsRssUrl = MutableStateFlow("")
    @OptIn(ExperimentalCoroutinesApi::class)
    val usRssNews: StateFlow<List<Article>> =
        selectedUsRssUrl
            .filter { it.isNotBlank() }
            .flatMapLatest { url ->
                repository.getRssFeed(url).catch { emit(emptyList()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setUsRssUrl(url: String) {
        selectedUsRssUrl.value = url
    }

    private val krRssUrls = listOf(
        "https://www.yna.co.kr/rss/market.xml",
        "https://www.mk.co.kr/rss/50200011/",
        "https://www.mk.co.kr/rss/30100041/",
        "https://www.hankyung.com/feed/finance",
        "https://www.hankyung.com/feed/economy",
        "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=02&plink=RSSREADER",
    )

    private val selectedKrKeywords = MutableStateFlow<List<String>>(emptyList())

    private val krFeed0: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[0]).catch { emit(emptyList()) }
    private val krFeed1: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[1]).catch { emit(emptyList()) }
    private val krFeed2: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[2]).catch { emit(emptyList()) }
    private val krFeed3: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[3]).catch { emit(emptyList()) }
    private val krFeed4: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[4]).catch { emit(emptyList()) }
    private val krFeed5: Flow<List<Article>> =
        repository.getRssFeed(krRssUrls[5]).catch { emit(emptyList()) }

    private val krAllRssNews: StateFlow<List<Article>> =
        combine(krFeed0, krFeed1, krFeed2, krFeed3, krFeed4, krFeed5) { lists: Array<List<Article>> ->
            lists.asList().flatten()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val krRssNews: StateFlow<List<Article>> =
        combine(krAllRssNews, selectedKrKeywords) { all, keywords ->
            if (keywords.isEmpty()) return@combine all
            val loweredKeywords = keywords.map { it.trim() }.filter { it.isNotBlank() }
            if (loweredKeywords.isEmpty()) return@combine all
            all.filter { article ->
                val haystack = "${article.headline}\n${article.summary}".lowercase()
                loweredKeywords.any { kw -> haystack.contains(kw.lowercase()) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setKrKeywords(keywords: List<String>) {
        selectedKrKeywords.value = keywords
    }

    init {
        viewModelScope.launch {
            repository.getNewsFeed(NewsFeedType.DOMESTIC_MARKET)
                .catch { e ->
                    Log.w(TAG, "domestic feed", e)
                    emit(emptyList())
                }
                .collect { domesticMarket ->
                    _domesticMarketNews.value = domesticMarket.takeIf { it.isNotEmpty() }
                        ?: _domesticMarketNews.value.takeIf { it.isNotEmpty() }
                        ?: MockData.NEWS_DOMESTIC_MARKET
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
