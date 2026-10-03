package com.burton.finance.ui.markets

import androidx.lifecycle.ViewModel
import com.burton.finance.data.repository.FinanceRepository
import com.burton.finance.domain.MarketBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MarketsViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {
    val state = repository.state

    fun refresh(board: MarketBoard) = repository.refreshMarkets(board)
}
