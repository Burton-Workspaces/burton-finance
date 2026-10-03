package com.burton.finance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.BuildConfig
import com.burton.finance.data.repository.FinanceRepository
import com.burton.finance.ui.components.FullScreenModal
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: FinanceRepository,
) : ViewModel() {
    val state = repository.state
}

@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    FullScreenModal(
        onDismiss = onDismiss,
        title = "Settings",
    ) {
        Spacer(Modifier.height(20.dp))
        SettingsRow(
            title = "Watchlist",
            subtitle = if (snapshot.watchlist.isEmpty()) "No symbols saved" else "${snapshot.watchlist.size} saved",
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Markets",
            subtitle = "US equities, international exchanges, and crypto",
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Feed",
            subtitle = "Yahoo Finance, BBC Business, CoinDesk, Cointelegraph",
            trailing = snapshot.feed.size.takeIf { it > 0 }?.toString(),
        )
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Burton Finance",
            subtitle = "About",
            trailing = BuildConfig.VERSION_NAME,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyLarge, color = BurtonMute)
        }
    }
}
