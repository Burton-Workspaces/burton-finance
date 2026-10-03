package com.burton.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burton.finance.domain.Quote
import com.burton.finance.domain.TrackedSymbol
import com.burton.finance.domain.formatPercent
import com.burton.finance.domain.formatPrice
import com.burton.finance.ui.theme.BurtonCharcoal
import com.burton.finance.ui.theme.BurtonDanger
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute
import com.burton.finance.ui.theme.BurtonSand

@Composable
fun QuoteCard(
    title: String,
    subtitle: String,
    quote: Quote?,
    refreshing: Boolean,
    onClick: () -> Unit,
) {
    val changeColor = when {
        quote == null -> BurtonMute
        quote.changePercent > 0 -> BurtonSand
        quote.changePercent < 0 -> BurtonDanger
        else -> BurtonMute
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if ((quote?.sparkline?.size ?: 0) >= 2) {
            Sparkline(
                values = quote?.sparkline.orEmpty(),
                color = changeColor,
                modifier = Modifier
                    .width(72.dp)
                    .height(36.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = quote?.let { formatPrice(it.price, it.currency) } ?: if (refreshing) "…" else "—",
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = quote?.let { formatPercent(it.changePercent) } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = changeColor,
            )
        }
    }
}

fun TrackedSymbol.cardSubtitle(quote: Quote?): String {
    val place = quote?.exchange?.ifBlank { region } ?: region
    val kind = kind.name
    return listOfNotNull(symbol, place.ifBlank { null }, kind).joinToString(" · ")
}
