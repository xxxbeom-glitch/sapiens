package com.sapiens.app.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.repository.NewsRepository
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

    private val _domesticBriefingArticles = MutableStateFlow(
        mergeDomesticBriefingArticles(
            MockData.briefingHankyungArticles,
            MockData.briefingMaeilArticles
        )
    )
    val domesticBriefingArticles: StateFlow<List<Article>> = _domesticBriefingArticles.asStateFlow()

    private val _usArticles = MutableStateFlow(MockData.usArticles)
    val usArticles: StateFlow<List<Article>> = _usArticles.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getBriefingHankyungArticles(),
                repository.getBriefingMaeilArticles(),
                repository.getUsArticles(),
            ) { hankyung, maeil, us ->
                Triple(hankyung, maeil, us)
            }.collect { (hankyung, maeil, us) ->
                val h = hankyung.ifEmpty { MockData.briefingHankyungArticles }
                val m = maeil.ifEmpty { MockData.briefingMaeilArticles }
                _domesticBriefingArticles.value = mergeDomesticBriefingArticles(h, m)
                _usArticles.value = us.ifEmpty { MockData.usArticles }
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
