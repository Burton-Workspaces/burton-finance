package com.burton.finance.data.market

import com.burton.finance.data.parse.TinyJson
import com.burton.finance.data.parse.TinyJson.dbl
import com.burton.finance.data.parse.TinyJson.dblOrNull
import com.burton.finance.data.parse.TinyJson.long
import com.burton.finance.data.parse.TinyJson.longOrNull
import com.burton.finance.data.parse.TinyJson.numList
import com.burton.finance.data.parse.TinyJson.obj
import com.burton.finance.data.parse.TinyJson.objList
import com.burton.finance.data.parse.TinyJson.str
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.FeedCategory
import com.burton.finance.domain.FeedItem
import com.burton.finance.domain.Quote
import com.burton.finance.domain.SearchHit
import com.burton.finance.domain.assetKindFromYahoo
import com.burton.finance.domain.regionForSymbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YahooFinance @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun quotes(symbols: List<String>): List<Quote> = withContext(Dispatchers.IO) {
        val unique = symbols.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (unique.isEmpty()) return@withContext emptyList()
        unique.chunked(BATCH).flatMap { batch ->
            runCatching { parseQuotes(get(quoteUrl(batch))) }
                .getOrElse { parseCharts(batch) }
        }
    }

    suspend fun quoteWithSpark(symbol: String): Quote? = withContext(Dispatchers.IO) {
        parseChart(get(chartUrl(symbol)))
    }

    suspend fun search(query: String): List<SearchHit> = withContext(Dispatchers.IO) {
        parseSearch(get(searchUrl(query)))
    }

    suspend fun news(query: String, category: FeedCategory): List<FeedItem> = withContext(Dispatchers.IO) {
        parseNews(get(newsUrl(query)), category)
    }

    private fun parseCharts(symbols: List<String>): List<Quote> =
        symbols.mapNotNull { symbol ->
            runCatching { parseChart(get(chartUrl(symbol))) }.getOrNull()
        }

    private fun get(url: String): String {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Yahoo request failed (${response.code})")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val BATCH = 24

        fun quoteUrl(symbols: List<String>): String =
            "https://query1.finance.yahoo.com/v7/finance/quote".toHttpUrl().newBuilder()
                .addQueryParameter("symbols", symbols.joinToString(","))
                .addQueryParameter("fields", QUOTE_FIELDS)
                .build()
                .toString()

        fun chartUrl(symbol: String): String =
            "https://query1.finance.yahoo.com/v8/finance/chart/${symbol.trim()}".toHttpUrl().newBuilder()
                .addQueryParameter("interval", "1d")
                .addQueryParameter("range", "3mo")
                .build()
                .toString()

        fun searchUrl(query: String): String =
            "https://query1.finance.yahoo.com/v1/finance/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", query.trim())
                .addQueryParameter("quotesCount", "20")
                .addQueryParameter("newsCount", "0")
                .build()
                .toString()

        fun newsUrl(query: String): String =
            "https://query1.finance.yahoo.com/v1/finance/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", query.trim())
                .addQueryParameter("quotesCount", "0")
                .addQueryParameter("newsCount", "20")
                .build()
                .toString()

        fun parseQuotes(json: String): List<Quote> {
            val root = TinyJson.parseObject(json)
            val rows = root.obj("quoteResponse").objList("result")
            return rows.mapNotNull(::quoteFromRow)
        }

        fun parseChart(json: String): Quote? {
            val result = TinyJson.parseObject(json).obj("chart").objList("result").firstOrNull()
                ?: return null
            val meta = result.obj("meta")
            val symbol = meta.str("symbol")
            if (symbol.isBlank()) return null
            val closes = result.obj("indicators").objList("quote").firstOrNull()
                ?.numList("close")
                .orEmpty()
                .mapNotNull { it }
            val price = meta.dblOrNull("regularMarketPrice")
                ?: closes.lastOrNull()
                ?: return null
            val previous = meta.dblOrNull("chartPreviousClose")
                ?: meta.dblOrNull("previousClose")
                ?: closes.dropLast(1).lastOrNull()
                ?: price
            val change = price - previous
            val percent = if (previous == 0.0) 0.0 else (change / previous) * 100.0
            val kind = assetKindFromYahoo(meta.str("instrumentType"), symbol)
            val exchange = meta.str("fullExchangeName").ifBlank { meta.str("exchangeName") }
            return Quote(
                id = symbol,
                symbol = symbol,
                name = meta.str("shortName").ifBlank { meta.str("longName").ifBlank { symbol } },
                exchange = exchange,
                region = regionForSymbol(symbol, exchange),
                kind = kind,
                currency = meta.str("currency").ifBlank { "USD" },
                price = price,
                change = change,
                changePercent = percent,
                previousClose = previous,
                dayHigh = meta.dblOrNull("regularMarketDayHigh"),
                dayLow = meta.dblOrNull("regularMarketDayLow"),
                week52High = meta.dblOrNull("fiftyTwoWeekHigh"),
                week52Low = meta.dblOrNull("fiftyTwoWeekLow"),
                volume = meta.longOrNull("regularMarketVolume"),
                sparkline = downsample(closes),
                updatedAt = (meta.long("regularMarketTime") * 1000L).takeIf { it > 0 }
                    ?: System.currentTimeMillis(),
            )
        }

        fun parseSearch(json: String): List<SearchHit> {
            val quotes = TinyJson.parseObject(json).objList("quotes")
            return quotes.mapNotNull { row ->
                val symbol = row.str("symbol")
                if (symbol.isBlank()) return@mapNotNull null
                val exchange = row.str("exchDisp").ifBlank { row.str("exchange") }
                SearchHit(
                    id = symbol,
                    symbol = symbol,
                    name = row.str("shortname").ifBlank { row.str("longname").ifBlank { symbol } },
                    exchange = exchange,
                    region = regionForSymbol(symbol, exchange),
                    kind = assetKindFromYahoo(row.str("quoteType"), symbol),
                )
            }
        }

        fun parseNews(json: String, category: FeedCategory): List<FeedItem> {
            val news = TinyJson.parseObject(json).objList("news")
            return news.mapNotNull { row ->
                val title = row.str("title")
                val url = row.str("link")
                if (title.isBlank() || url.isBlank()) return@mapNotNull null
                val published = row.long("providerPublishTime").let { if (it < 10_000_000_000L) it * 1000L else it }
                FeedItem(
                    id = row.str("uuid").ifBlank { url },
                    title = title,
                    source = row.str("publisher").ifBlank { "Yahoo Finance" },
                    url = url,
                    publishedAt = published,
                    summary = "",
                    category = category,
                )
            }
        }

        private fun quoteFromRow(row: Map<String, Any?>): Quote? {
            val symbol = row.str("symbol")
            val price = row.dblOrNull("regularMarketPrice") ?: return null
            if (symbol.isBlank()) return null
            val previous = row.dblOrNull("regularMarketPreviousClose") ?: row.dbl("regularMarketPrice")
            val exchange = row.str("fullExchangeName").ifBlank { row.str("exchange") }
            return Quote(
                id = symbol,
                symbol = symbol,
                name = row.str("shortName").ifBlank { row.str("longName").ifBlank { symbol } },
                exchange = exchange,
                region = regionForSymbol(symbol, exchange),
                kind = assetKindFromYahoo(row.str("quoteType"), symbol),
                currency = row.str("currency").ifBlank { "USD" },
                price = price,
                change = row.dbl("regularMarketChange", price - previous),
                changePercent = row.dbl("regularMarketChangePercent"),
                previousClose = previous,
                dayHigh = row.dblOrNull("regularMarketDayHigh"),
                dayLow = row.dblOrNull("regularMarketDayLow"),
                week52High = row.dblOrNull("fiftyTwoWeekHigh"),
                week52Low = row.dblOrNull("fiftyTwoWeekLow"),
                volume = row.longOrNull("regularMarketVolume"),
                marketCap = row.dblOrNull("marketCap"),
                updatedAt = (row.long("regularMarketTime") * 1000L).takeIf { it > 0 }
                    ?: System.currentTimeMillis(),
            )
        }

        fun downsample(values: List<Double>, maxPoints: Int = 48): List<Double> {
            if (values.size <= maxPoints) return values
            val step = values.size / maxPoints.toDouble()
            return List(maxPoints) { index ->
                values[(index * step).toInt().coerceIn(0, values.lastIndex)]
            }
        }

        private const val QUOTE_FIELDS =
            "symbol,shortName,longName,regularMarketPrice,regularMarketChange,regularMarketChangePercent," +
                "regularMarketPreviousClose,regularMarketDayHigh,regularMarketDayLow,fiftyTwoWeekHigh," +
                "fiftyTwoWeekLow,regularMarketVolume,marketCap,currency,exchange,fullExchangeName," +
                "quoteType,regularMarketTime"
    }
}
