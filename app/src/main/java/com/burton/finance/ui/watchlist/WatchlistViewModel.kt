package com.burton.finance.ui.watchlist

import androidx.lifecycle.ViewModel
import com.burton.finance.data.repository.FinanceRepository
import com.burton.finance.domain.SearchHit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {
    val state = repository.state

    fun refresh() = repository.refreshWatchlist()

    fun remove(id: String) = repository.remove(id)

    fun add(hit: SearchHit) = repository.add(hit)

    suspend fun search(query: String) = repository.search(query)
}
