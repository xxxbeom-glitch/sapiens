package com.sapiens.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.BriefingCard
import com.sapiens.app.data.repository.NewsRepository
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedNewsDocId = MutableStateFlow("kr_domestic_stock")
    val selectedNewsDocId: StateFlow<String> = _selectedNewsDocId.asStateFlow()

    // ── 브리핑 카드 ───────────────────────────────────────
    val briefingCards: StateFlow<List<BriefingCard>> =
        repository.getBriefingCards()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 북마크 (로컬 in-memory, 필요시 DataStore로 교체) ──
    private val _savedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val savedCardIds: StateFlow<Set<String>> = _savedCardIds.asStateFlow()

    fun toggleBookmark(cardId: String) {
        _savedCardIds.value = _savedCardIds.value.toMutableSet().also { set ->
            if (cardId in set) set.remove(cardId) else set.add(cardId)
        }
    }

    // ── 헤드라인 뉴스 ─────────────────────────────────────
        selectedNewsDocId
            .flatMapLatest { docId ->
                repository.getNewsFeedDocument(docId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSelectedNewsDocId(documentId: String) {
        _selectedNewsDocId.value = documentId
    }

    init {
        viewModelScope.launch {
            headlineNews.collect { items ->
                    // 첫 응답이 오면 로딩만 해제한다.
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
