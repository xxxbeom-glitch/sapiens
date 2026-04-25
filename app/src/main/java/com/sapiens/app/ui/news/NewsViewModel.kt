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
            }.filterNot(::looksLikeAdOrOpinion)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setKrKeywords(keywords: List<String>) {
        selectedKrKeywords.value = keywords
    }

    /**
     * 키워드로 잡히더라도 광고성(협찬/PR/보도자료) 혹은 칼럼/기자수첩 등 주관성 글로 보이면 제외.
     * RSS만으로 완벽히 판별은 어렵기 때문에, 문구 패턴 기반의 보수적 필터로 시작한다.
     */
    private fun looksLikeAdOrOpinion(article: Article): Boolean {
        val text = ("${article.headline}\n${article.summary}").lowercase()

        // --- 광고/홍보성 ---
        val adMarkers = listOf(
            "광고", "협찬", "제휴", "후원", "sponsored", "sponsor",
            "프로모션", "promotion", "이벤트", "event", "할인", "특가", "쿠폰",
            "보도자료", "자료제공", "pr", "press release",
            "신제품", "출시", "론칭", "런칭",
            "문의", "상담", "예약", "신청", "구매", "구독",
        )
        if (adMarkers.any(text::contains)) return true

        // --- 칼럼/오피니언/기자 주관 ---
        val opinionMarkers = listOf(
            "칼럼", "기고", "사설", "논설", "오피니언", "opinion",
            "기자수첩", "데스크칼럼", "기자의 시선", "기자 생각",
            "에디터", "editorial",
        )
        if (opinionMarkers.any(text::contains)) return true

        // 과도한 클릭 유도/선정성 패턴(가벼운 광고성/주관성에 자주 등장)
        val clickbaitMarkers = listOf(
            "지금 사야", "무조건", "대박", "초대박", "충격", "반전", "비밀", "단독",
            "…", "!!", "!!!"
        )
        if (clickbaitMarkers.any(text::contains)) return true

        return false
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
