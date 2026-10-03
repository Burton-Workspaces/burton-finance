package com.burton.finance.ui.markets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.domain.MarketBoard
import com.burton.finance.domain.MarketCatalog
import com.burton.finance.ui.components.EmptyStatePanel
import com.burton.finance.ui.components.QuoteCard
import com.burton.finance.ui.components.RoomsSkeleton
import com.burton.finance.ui.components.SegmentRow
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonSand

@Composable
fun MarketsScreen(
    onOpenSymbol: (String) -> Unit,
    viewModel: MarketsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var board by remember { mutableStateOf(MarketBoard.US) }
    LaunchedEffect(board) {
        viewModel.refresh(board)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Markets",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.refresh(board) }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh markets", tint = BurtonIvory)
            }
        }
        Spacer(Modifier.height(8.dp))
        SegmentRow(
            options = MarketBoard.entries,
            selected = board,
            label = { it.label },
            onSelect = { board = it },
        )
        snapshot.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSand)
        }
        Spacer(Modifier.height(16.dp))
        when (board) {
            MarketBoard.Crypto -> {
                when {
                    snapshot.cryptoLoading && snapshot.cryptoListings.isEmpty() -> RoomsSkeleton(6)
                    snapshot.cryptoListings.isEmpty() -> EmptyStatePanel(title = "No crypto quotes")
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(snapshot.cryptoListings, key = { it.id }) { quote ->
                                QuoteCard(
                                    title = quote.name,
                                    subtitle = "${quote.symbol} · Crypto",
                                    quote = quote,
                                    refreshing = snapshot.cryptoLoading,
                                    onClick = { onOpenSymbol(quote.id) },
                                )
                            }
                        }
                    }
                }
            }
            MarketBoard.US, MarketBoard.International -> {
                val groups = MarketCatalog.groupsFor(board)
                val loading = snapshot.marketsLoading && groups.none { group ->
                    group.listings.any { listing -> snapshot.quote(listing.id) != null }
                }
                if (loading) {
                    RoomsSkeleton(6)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        groups.forEach { group ->
                            item(key = "h-${group.title}") {
                                Text(
                                    group.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BurtonIvory,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(group.listings, key = { it.id }) { listing ->
                                val quote = snapshot.quote(listing.id)
                                QuoteCard(
                                    title = listing.name,
                                    subtitle = listOf(listing.symbol, listing.region).joinToString(" · "),
                                    quote = quote,
                                    refreshing = snapshot.marketsLoading,
                                    onClick = { onOpenSymbol(listing.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
