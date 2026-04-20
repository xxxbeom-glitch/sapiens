package com.sapiens.app.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sapiens.app.data.stock.StockDetailRepository
import com.sapiens.app.data.stock.StockDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockDetailViewModel(
    private val repository: StockDetailRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Idle)
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    fun load(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading
            _uiState.value = runCatching {
                StockDetailUiState.Success(repository.loadStockDetail(trimmed))
            }.getOrElse { e ->
                StockDetailUiState.Error(e.message ?: "데이터를 불러오지 못했습니다.")
            }
        }
    }

    fun reset() {
        _uiState.value = StockDetailUiState.Idle
    }

    companion object {
        fun factory(repository: StockDetailRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(StockDetailViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return StockDetailViewModel(repository) as T
                }
            }
    }
}
