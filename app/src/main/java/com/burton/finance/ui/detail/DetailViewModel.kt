package com.burton.finance.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.finance.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FinanceRepository,
) : ViewModel() {
    val symbolId: String = savedStateHandle.get<String>("symbolId").orEmpty()
    val state = repository.state

    init {
        viewModelScope.launch {
            if (symbolId.isNotBlank()) repository.loadDetail(symbolId)
        }
    }

    fun toggleWatch() {
        val snap = state.value
        if (snap.watching(symbolId)) {
            repository.remove(symbolId)
        } else {
            val quote = snap.quote(symbolId) ?: return
            repository.add(quote)
        }
    }
}
