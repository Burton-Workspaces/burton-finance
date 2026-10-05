package com.burton.finance.ui.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.MarketCatalog
import com.burton.finance.domain.Quote
import com.burton.finance.domain.formatCompact
import com.burton.finance.domain.formatPercent
import com.burton.finance.domain.formatPrice
import com.burton.finance.domain.formatSignedPrice
import com.burton.finance.ui.components.ChartLaunch
import com.burton.finance.ui.components.ChartModal
import com.burton.finance.ui.components.EmptyStatePanel
import com.burton.finance.ui.components.Sparkline
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonDanger
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.theme.BurtonSand
import com.burton.finance.ui.theme.BurtonVoid

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val id = viewModel.symbolId
    val decoded = runCatching { Uri.decode(id) }.getOrDefault(id)
    val quote = snapshot.quote(decoded) ?: snapshot.quote(id)
    val tracked = snapshot.watchlist.firstOrNull { it.id == decoded || it.id == id }
    val listing = MarketCatalog.listing(decoded) ?: MarketCatalog.listing(id)
    val name = quote?.name ?: tracked?.name ?: listing?.name ?: decoded
    val symbol = quote?.symbol ?: tracked?.symbol ?: listing?.symbol ?: decoded
    val watching = snapshot.watching(decoded) || snapshot.watching(id)
    var chart by remember { mutableStateOf<ChartLaunch?>(null) }
    val chartLaunch = ChartLaunch(
        id = quote?.id ?: tracked?.id ?: listing?.id ?: decoded,
        symbol = symbol,
        name = name,
        exchange = quote?.exchange ?: tracked?.exchange ?: listing?.exchange.orEmpty(),
        kind = quote?.kind ?: tracked?.kind ?: listing?.kind ?: AssetKind.Equity,
    )
    val changeColor = when {
        quote == null -> BurtonMute
        quote.changePercent > 0 -> BurtonSand
        quote.changePercent < 0 -> BurtonDanger
        else -> BurtonMute
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = BurtonIvory,
            )
        }
        Text(name, style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(symbol, quote?.exchange ?: listing?.region, quote?.kind?.name ?: listing?.kind?.name)
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = BurtonMute,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            quote?.let { formatPrice(it.price, it.currency) } ?: "—",
            style = MaterialTheme.typography.displayLarge,
            color = BurtonIvory,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            quote?.let { "${formatSignedPrice(it.change, it.currency)}  ${formatPercent(it.changePercent)}" } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = changeColor,
        )
        if ((quote?.sparkline?.size ?: 0) >= 2) {
            Spacer(Modifier.height(20.dp))
            Sparkline(
                values = quote?.sparkline.orEmpty(),
                color = changeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BurtonCharcoal)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Open full chart",
                        onClick = { chart = chartLaunch },
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            )
        } else if (symbol.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            EmptyStatePanel(
                title = "Open chart",
                action = "Candles, indicators, and drawings",
                onAction = { chart = chartLaunch },
            )
        }
        Spacer(Modifier.height(20.dp))
        if (quote != null) {
            MetricGrid(quote)
            Spacer(Modifier.height(20.dp))
        }
        Button(
            onClick = viewModel::toggleWatch,
            colors = ButtonDefaults.buttonColors(
                containerColor = BurtonIvory,
                contentColor = BurtonVoid,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (watching) "Remove from watchlist" else "Add to watchlist")
        }
        Spacer(Modifier.height(28.dp))
    }
    chart?.let {
        ChartModal(launch = it, onDismiss = { chart = null })
    }
}

@Composable
private fun MetricGrid(quote: Quote) {
    val rows = listOfNotNull(
        "Previous close" to formatPrice(quote.previousClose, quote.currency),
        quote.dayHigh?.let { "Day high" to formatPrice(it, quote.currency) },
        quote.dayLow?.let { "Day low" to formatPrice(it, quote.currency) },
        quote.week52High?.let { "52-week high" to formatPrice(it, quote.currency) },
        quote.week52Low?.let { "52-week low" to formatPrice(it, quote.currency) },
        quote.volume?.let { "Volume" to formatCompact(it.toDouble()) },
        quote.marketCap?.let { "Market cap" to formatCompact(it) },
        "Currency" to quote.currency,
        "Region" to quote.region,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(BurtonCharcoal, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = BurtonMute)
                        Spacer(Modifier.height(6.dp))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
