package com.burton.finance.data.market

import com.burton.finance.data.parse.TinyJson
import com.burton.finance.data.parse.TinyJson.dbl
import com.burton.finance.data.parse.TinyJson.dblOrNull
import com.burton.finance.data.parse.TinyJson.numList
import com.burton.finance.data.parse.TinyJson.obj
import com.burton.finance.data.parse.TinyJson.objList
import com.burton.finance.data.parse.TinyJson.str
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.Quote
import com.burton.finance.domain.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGecko @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun markets(): List<Quote> = withContext(Dispatchers.IO) {
        parseMarkets(get(marketsUrl()))
    }

    suspend fun search(query: String): List<SearchHit> = withContext(Dispatchers.IO) {
        parseSearch(get(searchUrl(query)))
    }

    private fun get(url: String): String {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("CoinGecko request failed (${response.code})")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        fun cryptoId(coinId: String): String = "cg:$coinId"

        fun marketsUrl(): String =
            "https://api.coingecko.com/api/v3/coins/markets".toHttpUrl().newBuilder()
                .addQueryParameter("vs_currency", "usd")
                .addQueryParameter("order", "market_cap_desc")
                .addQueryParameter("per_page", "80")
                .addQueryParameter("page", "1")
                .addQueryParameter("sparkline", "true")
                .addQueryParameter("price_change_percentage", "24h")
                .build()
                .toString()

        fun searchUrl(query: String): String =
            "https://api.coingecko.com/api/v3/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", query.trim())
                .build()
                .toString()

        fun parseMarkets(json: String): List<Quote> {
            val now = System.currentTimeMillis()
            return TinyJson.parseArray(json).mapNotNull { item ->
                val row = item as? Map<*, *> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                quoteFromMarket(row as Map<String, Any?>, now)
            }
        }

        fun parseSearch(json: String): List<SearchHit> {
            val coins = TinyJson.parseObject(json).objList("coins")
            return coins.mapNotNull { row ->
                val id = row.str("id")
                val symbol = row.str("symbol").uppercase()
                val name = row.str("name")
                if (id.isBlank() || symbol.isBlank()) return@mapNotNull null
                SearchHit(
                    id = cryptoId(id),
                    symbol = symbol,
                    name = name.ifBlank { symbol },
                    exchange = "Crypto",
                    region = "Crypto",
                    kind = AssetKind.Crypto,
                )
            }.take(20)
        }

        private fun quoteFromMarket(row: Map<String, Any?>, now: Long): Quote? {
            val id = row.str("id")
            val symbol = row.str("symbol").uppercase()
            val price = row.dblOrNull("current_price") ?: return null
            if (id.isBlank() || symbol.isBlank()) return null
            val change = row.dbl("price_change_24h")
            val spark = row.obj("sparkline_in_7d").numList("price").mapNotNull { it }
            return Quote(
                id = cryptoId(id),
                symbol = symbol,
                name = row.str("name").ifBlank { symbol },
                exchange = "Crypto",
                region = "Crypto",
                kind = AssetKind.Crypto,
                currency = "USD",
                price = price,
                change = change,
                changePercent = row.dbl("price_change_percentage_24h"),
                previousClose = price - change,
                dayHigh = row.dblOrNull("high_24h"),
                dayLow = row.dblOrNull("low_24h"),
                week52High = row.dblOrNull("ath"),
                week52Low = row.dblOrNull("atl"),
                volume = row.dblOrNull("total_volume")?.toLong(),
                marketCap = row.dblOrNull("market_cap"),
                sparkline = YahooFinance.downsample(spark),
                updatedAt = now,
            )
        }
    }
}
