package com.burton.finance.data.repository

import com.burton.finance.data.feed.NewsFeeds
import com.burton.finance.data.market.CoinGecko
import com.burton.finance.data.market.YahooFinance
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.CatalogListing
import com.burton.finance.domain.FeedCategory
import com.burton.finance.domain.FinanceSnapshot
import com.burton.finance.domain.MarketBoard
import com.burton.finance.domain.MarketCatalog
import com.burton.finance.domain.Quote
import com.burton.finance.domain.SearchHit
import com.burton.finance.domain.TrackedSymbol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val prefs: LocalPrefs,
    private val yahoo: YahooFinance,
    private val coinGecko: CoinGecko,
    private val newsFeeds: NewsFeeds,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistLock = Mutex()
    private val _state = MutableStateFlow(FinanceSnapshot())
    val state: StateFlow<FinanceSnapshot> = _state.asStateFlow()

    init {
        scope.launch { start() }
    }

    private suspend fun start() {
        val stored = runCatching { prefs.load() }.getOrDefault(StoredState())
        _state.update { it.copy(ready = true, watchlist = stored.watchlist) }
        refreshWatchlist()
        refreshFeed()
    }

    fun refreshWatchlist() {
        scope.launch { refreshWatchlistInternal() }
    }

    fun refreshMarkets(board: MarketBoard) {
        scope.launch {
            when (board) {
                MarketBoard.Crypto -> refreshCryptoInternal()
                MarketBoard.US, MarketBoard.International -> refreshYahooBoard(board)
            }
        }
    }

    fun refreshFeed() {
        scope.launch { refreshFeedInternal() }
    }

    fun add(hit: SearchHit) {
        add(
            TrackedSymbol(
                id = hit.id,
                symbol = hit.symbol,
                name = hit.name,
                exchange = hit.exchange,
                region = hit.region,
                kind = hit.kind,
            ),
        )
    }

    fun add(listing: CatalogListing) {
        add(
            TrackedSymbol(
                id = listing.id,
                symbol = listing.symbol,
                name = listing.name,
                exchange = listing.exchange,
                region = listing.region,
                kind = listing.kind,
            ),
        )
    }

    fun add(quote: Quote) {
        add(
            TrackedSymbol(
                id = quote.id,
                symbol = quote.symbol,
                name = quote.name,
                exchange = quote.exchange,
                region = quote.region,
                kind = quote.kind,
            ),
        )
        mergeQuotes(listOf(quote))
    }

    fun remove(id: String) {
        scope.launch {
            _state.update { snap ->
                snap.copy(watchlist = snap.watchlist.filterNot { it.id == id })
            }
            persist()
        }
    }

    suspend fun search(query: String): List<SearchHit> = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.length < 1) return@coroutineScope emptyList()
        val yahooJob = async { runCatching { yahoo.search(trimmed) }.getOrDefault(emptyList()) }
        val cryptoJob = async { runCatching { coinGecko.search(trimmed) }.getOrDefault(emptyList()) }
        (yahooJob.await() + cryptoJob.await()).distinctBy { it.id.uppercase() }
    }

    suspend fun loadDetail(id: String): Quote? {
        val existing = _state.value.quote(id)
        val tracked = _state.value.watchlist.firstOrNull { it.id == id }
        val listing = MarketCatalog.listing(id)
        return when {
            id.startsWith("cg:") -> {
                if (existing?.sparkline?.isNotEmpty() == true) existing
                else {
                    refreshCryptoInternal()
                    _state.value.quote(id)
                }
            }
            else -> {
                val symbol = tracked?.symbol ?: listing?.symbol ?: existing?.symbol ?: id
                val quote = runCatching { yahoo.quoteWithSpark(symbol) }.getOrNull()
                    ?: existing
                if (quote != null) mergeQuotes(listOf(quote))
                quote
            }
        }
    }

    private fun add(symbol: TrackedSymbol) {
        scope.launch {
            _state.update { snap ->
                if (snap.watchlist.any { it.id == symbol.id }) snap
                else snap.copy(watchlist = snap.watchlist + symbol, error = null)
            }
            persist()
            refreshOne(symbol)
        }
    }

    private suspend fun refreshWatchlistInternal() {
        val items = _state.value.watchlist
        if (items.isEmpty()) return
        _state.update { it.copy(refreshing = items.map { item -> item.id }.toSet(), error = null) }
        try {
            val yahooIds = items.filterNot { it.id.startsWith("cg:") }
            val cryptoIds = items.filter { it.id.startsWith("cg:") }
            if (yahooIds.isNotEmpty()) {
                val quotes = yahoo.quotes(yahooIds.map { it.symbol })
                mergeQuotes(quotes.map { quote ->
                    val tracked = yahooIds.firstOrNull { it.symbol.equals(quote.symbol, true) || it.id == quote.id }
                    if (tracked == null) quote else quote.copy(id = tracked.id, name = tracked.name.ifBlank { quote.name })
                })
                fillSparklines(yahooIds.take(12).map { it.symbol })
            }
            if (cryptoIds.isNotEmpty()) {
                refreshCryptoInternal()
            }
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Watchlist refresh failed") }
        } finally {
            _state.update { it.copy(refreshing = emptySet()) }
        }
    }

    private suspend fun refreshOne(symbol: TrackedSymbol) {
        _state.update { it.copy(refreshing = it.refreshing + symbol.id) }
        try {
            val quote = if (symbol.id.startsWith("cg:")) {
                refreshCryptoInternal()
                _state.value.quote(symbol.id)
            } else {
                yahoo.quoteWithSpark(symbol.symbol)?.copy(id = symbol.id, name = symbol.name.ifBlank { symbol.symbol })
            }
            if (quote != null) mergeQuotes(listOf(quote))
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Quote failed") }
        } finally {
            _state.update { it.copy(refreshing = it.refreshing - symbol.id) }
        }
    }

    private suspend fun refreshYahooBoard(board: MarketBoard) {
        val symbols = MarketCatalog.groupsFor(board).flatMap { it.listings }.map { it.symbol }.distinct()
        _state.update { it.copy(marketsLoading = true, error = null) }
        try {
            val quotes = yahoo.quotes(symbols)
            mergeMarketQuotes(quotes)
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Market refresh failed") }
        } finally {
            _state.update { it.copy(marketsLoading = false) }
        }
    }

    private suspend fun refreshCryptoInternal() {
        _state.update { it.copy(cryptoLoading = true, error = null) }
        try {
            val quotes = coinGecko.markets()
            _state.update { snap ->
                snap.copy(
                    cryptoListings = quotes,
                    quotes = snap.quotes + quotes.filter { coin -> snap.watching(coin.id) }.associateBy { it.id },
                    marketQuotes = snap.marketQuotes + quotes.associateBy { it.id },
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Crypto refresh failed") }
        } finally {
            _state.update { it.copy(cryptoLoading = false) }
        }
    }

    private suspend fun refreshFeedInternal() {
        _state.update { it.copy(feedLoading = true, error = null) }
        try {
            val rss = coroutineScope {
                NewsFeeds.sources.map { source ->
                    async { runCatching { newsFeeds.load(source) }.getOrDefault(emptyList()) }
                }.awaitAll().flatten()
            }
            val watchSymbols = _state.value.watchlist.filter { it.kind != AssetKind.Crypto }.take(6).map { it.symbol }
            val watchNews = if (watchSymbols.isEmpty()) {
                emptyList()
            } else {
                coroutineScope {
                    watchSymbols.map { symbol ->
                        async {
                            runCatching { yahoo.news(symbol, FeedCategory.Watchlist) }.getOrDefault(emptyList())
                        }
                    }.awaitAll().flatten()
                }
            }
            val combined = (rss + watchNews)
                .distinctBy { it.url }
                .sortedByDescending { it.publishedAt }
            _state.update { it.copy(feed = combined) }
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Feed refresh failed") }
        } finally {
            _state.update { it.copy(feedLoading = false) }
        }
    }

    private suspend fun fillSparklines(symbols: List<String>) {
        coroutineScope {
            symbols.map { symbol ->
                async {
                    runCatching { yahoo.quoteWithSpark(symbol) }.getOrNull()
                }
            }.awaitAll().filterNotNull().let { mergeQuotes(it) }
        }
    }

    private fun mergeQuotes(quotes: List<Quote>) {
        if (quotes.isEmpty()) return
        _state.update { snap ->
            val merged = snap.quotes.toMutableMap()
            quotes.forEach { quote ->
                val previous = merged[quote.id]
                merged[quote.id] = mergeQuote(previous, quote)
            }
            snap.copy(quotes = merged)
        }
    }

    private fun mergeMarketQuotes(quotes: List<Quote>) {
        if (quotes.isEmpty()) return
        _state.update { snap ->
            val market = snap.marketQuotes.toMutableMap()
            val watched = snap.quotes.toMutableMap()
            quotes.forEach { quote ->
                market[quote.id] = mergeQuote(market[quote.id], quote)
                if (snap.watching(quote.id)) {
                    watched[quote.id] = mergeQuote(watched[quote.id], quote)
                }
            }
            snap.copy(marketQuotes = market, quotes = watched)
        }
    }

    private fun mergeQuote(previous: Quote?, next: Quote): Quote {
        if (previous == null) return next
        return next.copy(
            sparkline = next.sparkline.ifEmpty { previous.sparkline },
            name = next.name.ifBlank { previous.name },
            marketCap = next.marketCap ?: previous.marketCap,
        )
    }

    private suspend fun persist() {
        persistLock.withLock {
            prefs.save(StoredState(watchlist = _state.value.watchlist))
        }
    }
}
