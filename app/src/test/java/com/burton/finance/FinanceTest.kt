package com.burton.finance

import com.burton.finance.data.feed.FeedSource
import com.burton.finance.data.feed.NewsFeeds
import com.burton.finance.data.market.CoinGecko
import com.burton.finance.data.market.YahooFinance
import com.burton.finance.data.parse.ArticleHtml
import com.burton.finance.data.parse.TinyJson
import com.burton.finance.data.repository.WatchlistCodec
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.FeedCategory
import com.burton.finance.domain.MarketBoard
import com.burton.finance.domain.MarketCatalog
import com.burton.finance.domain.TrackedSymbol
import com.burton.finance.domain.TradingViewSymbol
import com.burton.finance.domain.assetKindFromYahoo
import com.burton.finance.domain.formatPercent
import com.burton.finance.domain.formatPrice
import com.burton.finance.domain.regionForSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TinyJsonTest {
    @Test
    fun roundTripObject() {
        val json = TinyJson.stringify(mapOf("name" to "AAPL", "n" to 12, "ok" to true))
        val parsed = TinyJson.parseObject(json)
        assertEquals("AAPL", parsed["name"])
        assertEquals(12L, parsed["n"])
        assertEquals(true, parsed["ok"])
    }
}

class FormatTest {
    @Test
    fun formatsPricesAndPercents() {
        assertTrue(formatPrice(227.52, "USD").contains("227.52"))
        assertEquals("+1.25%", formatPercent(1.25))
        assertEquals("-0.50%", formatPercent(-0.5))
        assertEquals(AssetKind.Crypto, assetKindFromYahoo("CRYPTOCURRENCY", "BTC-USD"))
        assertEquals(AssetKind.Index, assetKindFromYahoo("INDEX", "^N225"))
        assertEquals("Japan", regionForSymbol("7203.T"))
        assertEquals("United Kingdom", regionForSymbol("SHEL.L"))
        assertEquals("Crypto", regionForSymbol("ETH-USD"))
        assertEquals("Hong Kong", regionForSymbol("0700.HK"))
    }
}

class TradingViewSymbolTest {
    @Test
    fun mapsYahooAndCryptoSymbols() {
        assertEquals(
            "NASDAQ:AAPL",
            TradingViewSymbol.from("AAPL", "AAPL", AssetKind.Equity, "NasdaqGS"),
        )
        assertEquals(
            "NYSE:IBM",
            TradingViewSymbol.from("IBM", "IBM", AssetKind.Equity, "NYSE"),
        )
        assertEquals(
            "BRK.B",
            TradingViewSymbol.from("BRK-B", "BRK-B", AssetKind.Equity, "United States"),
        )
        assertEquals("TVC:SPX", TradingViewSymbol.from("^GSPC", "^GSPC", AssetKind.Index))
        assertEquals("TVC:UKX", TradingViewSymbol.from("^FTSE", "^FTSE", AssetKind.Index))
        assertEquals("TVC:NI225", TradingViewSymbol.from("^N225", "^N225", AssetKind.Index))
        assertEquals("LSE:SHEL", TradingViewSymbol.from("SHEL.L", "SHEL.L", AssetKind.Equity))
        assertEquals("TSE:7203", TradingViewSymbol.from("7203.T", "7203.T", AssetKind.Equity))
        assertEquals("HKEX:700", TradingViewSymbol.from("0700.HK", "0700.HK", AssetKind.Equity))
        assertEquals("XETR:SAP", TradingViewSymbol.from("SAP.DE", "SAP.DE", AssetKind.Equity))
        assertEquals("EURONEXT:MC", TradingViewSymbol.from("MC.PA", "MC.PA", AssetKind.Equity))
        assertEquals("EURONEXT:ASML", TradingViewSymbol.from("ASML.AS", "ASML.AS", AssetKind.Equity))
        assertEquals("SIX:NESN", TradingViewSymbol.from("NESN.SW", "NESN.SW", AssetKind.Equity))
        assertEquals("TSX:SHOP", TradingViewSymbol.from("SHOP.TO", "SHOP.TO", AssetKind.Equity))
        assertEquals("ASX:BHP", TradingViewSymbol.from("BHP.AX", "BHP.AX", AssetKind.Equity))
        assertEquals("NSE:RELIANCE", TradingViewSymbol.from("RELIANCE.NS", "RELIANCE.NS", AssetKind.Equity))
        assertEquals("KRX:005930", TradingViewSymbol.from("005930.KS", "005930.KS", AssetKind.Equity))
        assertEquals("BMFBOVESPA:PETR4", TradingViewSymbol.from("PETR4.SA", "PETR4.SA", AssetKind.Equity))
        assertEquals("SSE:600519", TradingViewSymbol.from("600519.SS", "600519.SS", AssetKind.Equity))
        assertEquals("TWSE:2330", TradingViewSymbol.from("2330.TW", "2330.TW", AssetKind.Equity))
        assertEquals("SGX:D05", TradingViewSymbol.from("D05.SI", "D05.SI", AssetKind.Equity))
        assertEquals(
            "CRYPTO:BTCUSD",
            TradingViewSymbol.from("BTC-USD", "BTC-USD", AssetKind.Crypto),
        )
        assertEquals(
            "CRYPTO:ETHUSD",
            TradingViewSymbol.from("cg:ethereum", "ETH", AssetKind.Crypto),
        )
    }

    @Test
    fun chartPageEmbedsSymbol() {
        val html = TradingViewSymbol.html("NASDAQ:AAPL", "Etc/UTC")
        assertTrue(html.contains("\"NASDAQ:AAPL\""))
        assertTrue(html.contains("s.tradingview.com/tv.js"))
        assertTrue(html.contains("hide_side_toolbar: false"))
        assertTrue(!html.contains("<script src=\"javascript:"))
    }
}

class MarketCatalogTest {
    @Test
    fun coversUsInternationalAndCryptoBoards() {
        assertTrue(MarketCatalog.groupsFor(MarketBoard.US).isNotEmpty())
        assertTrue(MarketCatalog.groupsFor(MarketBoard.International).any { it.region == "Japan" })
        assertTrue(MarketCatalog.groupsFor(MarketBoard.International).any { it.region == "United Kingdom" })
        assertTrue(MarketCatalog.yahooSymbols.contains("AAPL"))
        assertTrue(MarketCatalog.yahooSymbols.contains("7203.T"))
        assertTrue(MarketCatalog.yahooSymbols.contains("SAP.DE"))
        assertTrue(MarketCatalog.listing("^FTSE")?.kind == AssetKind.Index)
    }
}

class YahooFinanceTest {
    @Test
    fun parseQuoteBatch() {
        val json = """
            {"quoteResponse":{"result":[{
              "symbol":"AAPL",
              "shortName":"Apple Inc.",
              "regularMarketPrice":227.52,
              "regularMarketChange":1.4,
              "regularMarketChangePercent":0.62,
              "regularMarketPreviousClose":226.12,
              "currency":"USD",
              "exchange":"NMS",
              "fullExchangeName":"NasdaqGS",
              "quoteType":"EQUITY",
              "regularMarketVolume":51234567,
              "marketCap":3400000000000
            }]}}
        """.trimIndent()
        val quotes = YahooFinance.parseQuotes(json)
        assertEquals(1, quotes.size)
        assertEquals("AAPL", quotes[0].symbol)
        assertEquals(227.52, quotes[0].price, 0.001)
        assertEquals(AssetKind.Equity, quotes[0].kind)
        assertEquals("United States", quotes[0].region)
    }

    @Test
    fun parseChartSparkline() {
        val json = """
            {"chart":{"result":[{
              "meta":{
                "currency":"USD",
                "symbol":"7203.T",
                "exchangeName":"JPX",
                "instrumentType":"EQUITY",
                "regularMarketPrice":2850.0,
                "chartPreviousClose":2800.0,
                "shortName":"Toyota"
              },
              "indicators":{"quote":[{"close":[2800.0,2820.0,2850.0]}]}
            }]}}
        """.trimIndent()
        val quote = YahooFinance.parseChart(json)!!
        assertEquals("7203.T", quote.symbol)
        assertEquals(2850.0, quote.price, 0.01)
        assertEquals(50.0, quote.change, 0.01)
        assertEquals(3, quote.sparkline.size)
        assertEquals("Japan", quote.region)
    }

    @Test
    fun parseSearchAndNews() {
        val search = """
            {"quotes":[{"symbol":"BTC-USD","shortname":"Bitcoin USD","quoteType":"CRYPTOCURRENCY","exchDisp":"CCC"}]}
        """.trimIndent()
        val hits = YahooFinance.parseSearch(search)
        assertEquals(1, hits.size)
        assertEquals(AssetKind.Crypto, hits[0].kind)
        val news = """
            {"news":[{"uuid":"n1","title":"Markets open higher","link":"https://example.com/a","publisher":"Yahoo Finance","providerPublishTime":1710000000}]}
        """.trimIndent()
        val items = YahooFinance.parseNews(news, FeedCategory.Markets)
        assertEquals("Markets open higher", items[0].title)
        assertEquals(1710000000L * 1000L, items[0].publishedAt)
    }
}

class CoinGeckoTest {
    @Test
    fun parseMarketsAndSearch() {
        val markets = """
            [{"id":"bitcoin","symbol":"btc","name":"Bitcoin","current_price":67000.5,
              "price_change_24h":1200.0,"price_change_percentage_24h":1.82,
              "high_24h":68000,"low_24h":65000,"market_cap":1300000000000,
              "total_volume":32000000000,"ath":73750,"atl":67.81,
              "sparkline_in_7d":{"price":[65000.0,66000.0,67000.5]}}]
        """.trimIndent()
        val quotes = CoinGecko.parseMarkets(markets)
        assertEquals(1, quotes.size)
        assertEquals("cg:bitcoin", quotes[0].id)
        assertEquals("BTC", quotes[0].symbol)
        assertEquals(AssetKind.Crypto, quotes[0].kind)
        assertEquals(3, quotes[0].sparkline.size)
        val search = """
            {"coins":[{"id":"ethereum","symbol":"eth","name":"Ethereum"}]}
        """.trimIndent()
        val hits = CoinGecko.parseSearch(search)
        assertEquals("cg:ethereum", hits[0].id)
        assertEquals("ETH", hits[0].symbol)
    }
}

class NewsFeedsTest {
    @Test
    fun parseRssItems() {
        val xml = """
            <rss><channel>
              <item>
                <title>Oil slips on inventories</title>
                <link>https://example.com/oil</link>
                <guid>oil-1</guid>
                <pubDate>Wed, 01 Oct 2025 12:00:00 GMT</pubDate>
                <description>Crude futures fell after a build.</description>
              </item>
            </channel></rss>
        """.trimIndent()
        val items = NewsFeeds.parse(xml, FeedSource("BBC Business", "https://example.com/rss", FeedCategory.Markets))
        assertEquals(1, items.size)
        assertEquals("Oil slips on inventories", items[0].title)
        assertEquals("BBC Business", items[0].source)
        assertTrue(items[0].publishedAt > 0L)
    }

    @Test
    fun parseAtomAndStripTags() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>c1</id>
                <title>Bitcoin holds &lt;b&gt;range&lt;/b&gt;</title>
                <link rel="alternate" href="https://example.com/btc"/>
                <updated>2025-10-01T12:00:00Z</updated>
                <summary>&lt;p&gt;Spot stays firm.&lt;/p&gt;</summary>
              </entry>
            </feed>
        """.trimIndent()
        val items = NewsFeeds.parse(xml, FeedSource("CoinDesk", "https://example.com/atom", FeedCategory.Crypto))
        assertEquals(1, items.size)
        assertEquals("Bitcoin holds range", items[0].title)
        assertEquals("Spot stays firm.", items[0].summary)
        assertEquals(FeedCategory.Crypto, items[0].category)
    }
}

class WatchlistCodecTest {
    @Test
    fun roundTrip() {
        val encoded = WatchlistCodec.encode(
            com.burton.finance.data.repository.StoredState(
                watchlist = listOf(
                    TrackedSymbol("AAPL", "AAPL", "Apple", "NasdaqGS", "United States", AssetKind.Equity),
                    TrackedSymbol("cg:bitcoin", "BTC", "Bitcoin", "Crypto", "Crypto", AssetKind.Crypto),
                ),
            ),
        )
        val decoded = WatchlistCodec.decode(encoded)
        assertEquals(2, decoded.watchlist.size)
        assertEquals("Apple", decoded.watchlist[0].name)
        assertEquals(AssetKind.Crypto, decoded.watchlist[1].kind)
        assertTrue(decoded.openArticlesInReadMode)
    }

    @Test
    fun emptyJson() {
        val decoded = WatchlistCodec.decode("")
        assertTrue(decoded.watchlist.isEmpty())
        assertTrue(decoded.openArticlesInReadMode)
    }

    @Test
    fun missingReadModeDefaultsOn() {
        val decoded = WatchlistCodec.decode("""{"watchlist":[]}""")
        assertTrue(decoded.openArticlesInReadMode)
    }

    @Test
    fun persistsReadModeOff() {
        val encoded = WatchlistCodec.encode(
            com.burton.finance.data.repository.StoredState(openArticlesInReadMode = false),
        )
        assertEquals(false, WatchlistCodec.decode(encoded).openArticlesInReadMode)
    }
}

class ArticleHtmlTest {
    @Test
    fun extractsArticleAndStripsChrome() {
        val html = """
            <html><head><title>Markets rally &amp; yields ease</title></head>
            <body>
              <nav>Home</nav>
              <script>alert(1)</script>
              <article>
                <p>Stocks rose on Friday as Treasury yields eased.</p>
                <p>Energy lagged while banks led the session.</p>
              </article>
            </body></html>
        """.trimIndent()
        val article = ArticleHtml.extract(html, fallbackTitle = "Fallback")
        assertEquals("Markets rally & yields ease", article.title)
        assertTrue(article.text.contains("Stocks rose on Friday"))
        assertTrue(article.html.contains("Energy lagged"))
        assertTrue(!article.html.contains("<script"))
        assertTrue(!article.html.contains("<nav"))
    }

    @Test
    fun usesFallbackWhenPageIsEmpty() {
        val article = ArticleHtml.extract(
            "<html><body></body></html>",
            fallbackTitle = "Oil slips",
            fallbackHtml = "<p>Crude futures fell after a build.</p>",
        )
        assertEquals("Oil slips", article.title)
        assertTrue(article.text.contains("Crude futures fell"))
    }

    @Test
    fun readerDocumentIsDarkAndEscapesTitle() {
        val page = ArticleHtml.readerDocument("A & B", "BBC Business", "<p onclick=\"x\">Hello</p>")
        assertTrue(page.contains("A &amp; B"))
        assertTrue(page.contains("BBC Business"))
        assertTrue(page.contains("#000000"))
        assertTrue(page.contains("data-dropped="))
        assertTrue(!page.contains("onclick="))
    }
}
