package com.burton.finance.ui.watchlist

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.ui.components.EmptyStatePanel
import com.burton.finance.ui.components.QuoteCard
import com.burton.finance.ui.components.RoomsSkeleton
import com.burton.finance.ui.components.cardSubtitle
import com.burton.finance.ui.search.AddSymbolModal
import com.burton.finance.ui.settings.SettingsModal
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonSand

@Composable
fun WatchlistScreen(
    onOpenSymbol: (String) -> Unit,
    onOpenMarkets: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
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
                text = "Watchlist",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add symbol", tint = BurtonIvory)
            }
            IconButton(onClick = viewModel::refresh, enabled = snapshot.watchlist.isNotEmpty()) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh quotes", tint = BurtonIvory)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = BurtonIvory)
            }
        }
        snapshot.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSand)
        }
        Spacer(Modifier.height(16.dp))
        when {
            !snapshot.ready -> RoomsSkeleton()
            snapshot.watchlist.isEmpty() -> EmptyStatePanel(
                title = "Nothing watched yet",
                action = "Tap to browse Markets",
                onAction = onOpenMarkets,
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(snapshot.watchlist, key = { it.id }) { item ->
                        val quote = snapshot.quote(item.id)
                        QuoteCard(
                            title = item.name,
                            subtitle = item.cardSubtitle(quote),
                            quote = quote,
                            refreshing = item.id in snapshot.refreshing,
                            onClick = { onOpenSymbol(item.id) },
                        )
                    }
                }
            }
        }
    }
    if (showSettings) {
        SettingsModal(onDismiss = { showSettings = false })
    }
    if (showAdd) {
        AddSymbolModal(
            onDismiss = { showAdd = false },
            viewModel = viewModel,
        )
    }
}
