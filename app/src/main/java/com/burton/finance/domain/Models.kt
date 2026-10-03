package com.burton.finance.domain

enum class AssetKind {
    Equity,
    Index,
    Etf,
    Crypto,
}

enum class MarketBoard(val label: String) {
    US("United States"),
    International("International"),
    Crypto("Crypto"),
}

enum class FeedCategory(val label: String) {
    Markets("Markets"),
    Crypto("Crypto"),
    Watchlist("Watchlist"),
}

data class TrackedSymbol(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val region: String,
    val kind: AssetKind,
)

data class Quote(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val region: String,
    val kind: AssetKind,
    val currency: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val previousClose: Double,
    val dayHigh: Double? = null,
    val dayLow: Double? = null,
    val week52High: Double? = null,
    val week52Low: Double? = null,
    val volume: Long? = null,
    val marketCap: Double? = null,
    val sparkline: List<Double> = emptyList(),
    val updatedAt: Long = 0L,
)

data class SearchHit(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val region: String,
    val kind: AssetKind,
)

data class CatalogListing(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val region: String,
    val kind: AssetKind,
)

data class MarketGroup(
    val title: String,
    val region: String,
    val board: MarketBoard,
    val listings: List<CatalogListing>,
)

data class FeedItem(
    val id: String,
    val title: String,
    val source: String,
    val url: String,
    val publishedAt: Long,
    val summary: String,
    val category: FeedCategory,
)

data class FinanceSnapshot(
    val ready: Boolean = false,
    val watchlist: List<TrackedSymbol> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val marketQuotes: Map<String, Quote> = emptyMap(),
    val cryptoListings: List<Quote> = emptyList(),
    val feed: List<FeedItem> = emptyList(),
    val refreshing: Set<String> = emptySet(),
    val marketsLoading: Boolean = false,
    val cryptoLoading: Boolean = false,
    val feedLoading: Boolean = false,
    val openArticlesInReadMode: Boolean = true,
    val error: String? = null,
) {
    fun quote(id: String): Quote? = quotes[id] ?: marketQuotes[id]
        ?: cryptoListings.firstOrNull { it.id == id }

    fun watching(id: String): Boolean = watchlist.any { it.id == id }
}
