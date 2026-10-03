package com.burton.finance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonMute

@Composable
fun <T> SegmentRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Text(
                text = label(option),
                style = MaterialTheme.typography.titleMedium,
                color = if (active) BurtonIvory else BurtonMute,
                modifier = Modifier.clickable { onSelect(option) },
            )
        }
    }
}
