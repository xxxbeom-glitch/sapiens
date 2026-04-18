package com.breaktobreak.dailynews.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.MarketIndicator
import com.breaktobreak.dailynews.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BriefingViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _morningArticles = MutableStateFlow(MockData.morningArticles)
    val morningArticles: StateFlow<List<Article>> = _morningArticles.asStateFlow()

    private val _usArticles = MutableStateFlow(MockData.usArticles)
    val usArticles: StateFlow<List<Article>> = _usArticles.asStateFlow()

    private val _marketIndicators = MutableStateFlow(MockData.marketIndicators)
    val marketIndicators: StateFlow<List<MarketIndicator>> = _marketIndicators.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getMorningArticles(),
                repository.getUsArticles(),
                repository.getMarketIndicators()
            ) { morning, us, indicators ->
                Triple(morning, us, indicators)
            }.collect { (morning, us, indicators) ->
                _morningArticles.value = morning.ifEmpty { MockData.morningArticles }
                _usArticles.value = us.ifEmpty { MockData.usArticles }
                _marketIndicators.value = indicators.ifEmpty { MockData.marketIndicators }
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(BriefingViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return BriefingViewModel(repository) as T
                }
            }
    }
}
