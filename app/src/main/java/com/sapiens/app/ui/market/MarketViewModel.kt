package com.sapiens.app.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.repository.IndustryRepository
import com.sapiens.app.data.repository.IndustryRepositoryImpl
import com.sapiens.app.data.repository.NewsRepository
import com.sapiens.app.data.stock.ThemeDescriptionRepository
import com.sapiens.app.data.stock.ThemeDescriptionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MarketViewModel(
    private val repository: NewsRepository,
    private val industryRepository: IndustryRepository = IndustryRepositoryImpl(),
    private val themeDescriptions: ThemeDescriptionRepository = ThemeDescriptionRepositoryImpl(),
) : ViewModel() {

    private val _marketThemes = MutableStateFlow(MockData.marketThemes)
    val marketThemes: StateFlow<List<MarketTheme>> = _marketThemes.asStateFlow()

    private val _marketIndustries = MutableStateFlow(MockData.marketIndustries)
    val marketIndustries: StateFlow<List<MarketTheme>> = _marketIndustries.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMarketThemes().collectLatest { themes ->
                val base = themes.ifEmpty { MockData.marketThemes }
                _marketThemes.value = base
                if (base.any { it.themeNo != null }) {
                    _marketThemes.value = enrichThemesWithCategoryInfo(base)
                }
            }
        }
        viewModelScope.launch {
            industryRepository.observeIndustries().collectLatest { rows ->
                _marketIndustries.value = rows.ifEmpty { MockData.marketIndustries }
            }
        }
    }

    private suspend fun enrichThemesWithCategoryInfo(themes: List<MarketTheme>): List<MarketTheme> =
        coroutineScope {
            themes.map { theme ->
                async(Dispatchers.IO) {
                    val no = theme.themeNo ?: return@async theme
                    if (theme.categoryInfo.isNotBlank()) return@async theme
                    val info = themeDescriptions.getCategoryInfo(no)
                    theme.copy(categoryInfo = info.orEmpty())
                }
            }.awaitAll()
        }

    companion object {
        fun factory(
            repository: NewsRepository,
            industryRepository: IndustryRepository = IndustryRepositoryImpl(),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MarketViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return MarketViewModel(
                        repository = repository,
                        industryRepository = industryRepository,
                    ) as T
                }
            }
    }
}
