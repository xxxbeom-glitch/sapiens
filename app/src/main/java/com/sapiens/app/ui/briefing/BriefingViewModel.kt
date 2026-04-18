package com.sapiens.app.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.MarketIndicator
import com.sapiens.app.data.model.MarketIndex
import com.sapiens.app.data.repository.NewsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private val _marketIndices = MutableStateFlow(MockData.MARKET_INDEX_LIST)
    val marketIndices: StateFlow<List<MarketIndex>> = _marketIndices.asStateFlow()

    private val _marketUpdatedLabel = MutableStateFlow("AI 요약 · 06:00 업데이트")
    val marketUpdatedLabel: StateFlow<String> = _marketUpdatedLabel.asStateFlow()

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
        viewModelScope.launch {
            repository.getRepresentativeIndices().collect { snapshot ->
                _marketIndices.value = snapshot.indices.ifEmpty { MockData.MARKET_INDEX_LIST }
                _marketUpdatedLabel.value = buildUpdatedLabel(snapshot.updatedAtMillis)
            }
        }
    }

    private fun buildUpdatedLabel(updatedAtMillis: Long): String {
        if (updatedAtMillis <= 0L) return "AI 요약 · 업데이트 시간 없음"
        val fmt = SimpleDateFormat("HH:mm", Locale.KOREA)
        return "AI 요약 · ${fmt.format(Date(updatedAtMillis))} 업데이트"
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
