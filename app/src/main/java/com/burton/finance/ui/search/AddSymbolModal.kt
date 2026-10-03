package com.burton.finance.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.domain.SearchHit
import com.burton.finance.ui.components.FullScreenModal
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonLine
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.theme.BurtonSand
import com.burton.finance.ui.watchlist.WatchlistViewModel
import kotlinx.coroutines.delay

@Composable
fun AddSymbolModal(
    onDismiss: () -> Unit,
    viewModel: WatchlistViewModel,
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val saved = snapshot.watchlist.map { it.id }.toSet()
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 1) {
            hits = emptyList()
            searched = false
            error = null
            return@LaunchedEffect
        }
        delay(280)
        loading = true
        error = null
        runCatching { viewModel.search(trimmed) }
            .onSuccess {
                hits = it
                searched = true
            }
            .onFailure {
                error = it.message ?: "Search failed"
                hits = emptyList()
                searched = true
            }
        loading = false
    }

    FullScreenModal(
        onDismiss = onDismiss,
        title = "Add symbol",
    ) {
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search stocks, indices, crypto") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BurtonIvory,
                unfocusedTextColor = BurtonIvory,
                focusedBorderColor = BurtonSand,
                unfocusedBorderColor = BurtonLine,
                cursorColor = BurtonIvory,
                focusedPlaceholderColor = BurtonMute,
                unfocusedPlaceholderColor = BurtonMute,
                focusedLeadingIconColor = BurtonSand,
                unfocusedLeadingIconColor = BurtonMute,
                focusedTrailingIconColor = BurtonIvory,
                unfocusedTrailingIconColor = BurtonMute,
            ),
        )
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurtonSand)
            }
            error != null -> Text(error ?: "", color = BurtonIvory)
            query.isBlank() -> Text(
                "Type a ticker or name. Yahoo Finance covers global exchanges; CoinGecko covers crypto.",
                color = BurtonMute,
            )
            searched && hits.isEmpty() -> Text("No matching symbols.", color = BurtonMute)
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                hits.forEach { hit ->
                    SearchHitRow(
                        hit = hit,
                        saved = hit.id in saved,
                        onAdd = {
                            viewModel.add(hit)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    hit: SearchHit,
    saved: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                hit.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(hit.name, hit.exchange.ifBlank { null }, hit.region).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onAdd, enabled = !saved) {
            Text(if (saved) "Added" else "Add", color = if (saved) BurtonMute else BurtonSand)
        }
    }
}
