package com.burton.finance.ui.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.domain.FeedCategory
import com.burton.finance.domain.FeedItem
import com.burton.finance.ui.components.EmptyStatePanel
import com.burton.finance.ui.components.RoomsSkeleton
import com.burton.finance.ui.components.SegmentRow
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.theme.BurtonSand
import java.text.DateFormat
import java.util.Date

private enum class FeedFilter(val label: String) {
    All("All"),
    Markets("Markets"),
    Crypto("Crypto"),
    Watchlist("Watchlist"),
}

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(FeedFilter.All) }
    val items = snapshot.feed.filter { item ->
        when (filter) {
            FeedFilter.All -> true
            FeedFilter.Markets -> item.category == FeedCategory.Markets
            FeedFilter.Crypto -> item.category == FeedCategory.Crypto
            FeedFilter.Watchlist -> item.category == FeedCategory.Watchlist
        }
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
                text = "Feed",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh feed", tint = BurtonIvory)
            }
        }
        Spacer(Modifier.height(8.dp))
        SegmentRow(
            options = FeedFilter.entries,
            selected = filter,
            label = { it.label },
            onSelect = { filter = it },
        )
        snapshot.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = BurtonSand)
        }
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.feedLoading && snapshot.feed.isEmpty() -> RoomsSkeleton(5)
            items.isEmpty() -> EmptyStatePanel(title = "Nothing in the feed")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        FeedCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(20.dp))
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                }
            }
            .padding(16.dp),
    ) {
        Text(
            listOfNotNull(item.source, item.category.label, formatWhen(item.publishedAt)).joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = BurtonMute,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.titleLarge,
            color = BurtonIvory,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.summary.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatWhen(epochMs: Long): String? {
    if (epochMs <= 0L) return null
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))
}
