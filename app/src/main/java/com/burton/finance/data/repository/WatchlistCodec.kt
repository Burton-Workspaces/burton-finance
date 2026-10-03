package com.burton.finance.data.repository

import com.burton.finance.data.parse.TinyJson
import com.burton.finance.data.parse.TinyJson.bool
import com.burton.finance.data.parse.TinyJson.objList
import com.burton.finance.data.parse.TinyJson.str
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.TrackedSymbol

data class StoredState(
    val watchlist: List<TrackedSymbol> = emptyList(),
    val openArticlesInReadMode: Boolean = true,
)

object WatchlistCodec {
    fun encode(state: StoredState): String = TinyJson.stringify(
        mapOf(
            "watchlist" to state.watchlist.map { item ->
                mapOf(
                    "id" to item.id,
                    "symbol" to item.symbol,
                    "name" to item.name,
                    "exchange" to item.exchange,
                    "region" to item.region,
                    "kind" to item.kind.name,
                )
            },
            "openArticlesInReadMode" to state.openArticlesInReadMode,
        ),
    )

    fun decode(json: String): StoredState {
        if (json.isBlank()) return StoredState()
        val root = TinyJson.parseObject(json)
        val watchlist = root.objList("watchlist").mapNotNull { row ->
            val id = row.str("id")
            val symbol = row.str("symbol")
            if (id.isBlank() || symbol.isBlank()) return@mapNotNull null
            TrackedSymbol(
                id = id,
                symbol = symbol,
                name = row.str("name").ifBlank { symbol },
                exchange = row.str("exchange"),
                region = row.str("region").ifBlank { "United States" },
                kind = runCatching { AssetKind.valueOf(row.str("kind")) }.getOrDefault(AssetKind.Equity),
            )
        }
        val openArticlesInReadMode = if (root.containsKey("openArticlesInReadMode")) {
            root.bool("openArticlesInReadMode", true)
        } else {
            true
        }
        return StoredState(
            watchlist = watchlist,
            openArticlesInReadMode = openArticlesInReadMode,
        )
    }
}
