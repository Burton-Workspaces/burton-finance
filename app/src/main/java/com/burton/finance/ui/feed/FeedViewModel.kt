package com.burton.finance.ui.feed

import androidx.lifecycle.ViewModel
import com.burton.finance.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {
    val state = repository.state

    fun refresh() = repository.refreshFeed()
}
