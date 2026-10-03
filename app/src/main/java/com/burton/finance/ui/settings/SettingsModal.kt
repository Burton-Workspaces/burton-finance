package com.burton.finance.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.BuildConfig
import com.burton.finance.report.BurtonIssues
import com.burton.finance.data.repository.FinanceRepository
import com.burton.finance.ui.components.FullScreenModal
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonGraphite
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.theme.BurtonSand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {
    val state = repository.state

    fun setOpenArticlesInReadMode(enabled: Boolean) {
        repository.setOpenArticlesInReadMode(enabled)
    }
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
            title = "Read mode",
            subtitle = if (snapshot.openArticlesInReadMode) {
                "Articles open in read mode"
            } else {
                "Articles open as the original page"
            },
            trailingContent = {
                Switch(
                    checked = snapshot.openArticlesInReadMode,
                    onCheckedChange = viewModel::setOpenArticlesInReadMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BurtonIvory,
                        checkedTrackColor = BurtonSand,
                        uncheckedThumbColor = BurtonMute,
                        uncheckedTrackColor = BurtonGraphite,
                    ),
                )
            },
            onClick = { viewModel.setOpenArticlesInReadMode(!snapshot.openArticlesInReadMode) },
        )
        Spacer(Modifier.height(10.dp))
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
        val context = LocalContext.current
        SettingsRow(
            title = "Burton Finance",
            subtitle = "About",
            trailing = BuildConfig.VERSION_NAME,
            onLongClick = { BurtonIssues.openNewIssue(context) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .then(
                when {
                    onClick != null && onLongClick != null -> {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                    onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
                    onLongClick != null -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                    else -> Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyLarge, color = BurtonMute)
        }
    }
}
