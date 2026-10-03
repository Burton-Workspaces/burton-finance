package com.burton.finance.data.feed

import com.burton.finance.data.parse.Xml
import com.burton.finance.data.parse.attr
import com.burton.finance.data.parse.child
import com.burton.finance.data.parse.childText
import com.burton.finance.data.parse.children
import com.burton.finance.data.parse.descendants
import com.burton.finance.data.parse.matchesName
import com.burton.finance.data.parse.rootElement
import com.burton.finance.domain.FeedCategory
import com.burton.finance.domain.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class FeedSource(
    val name: String,
    val url: String,
    val category: FeedCategory,
)

@Singleton
class NewsFeeds @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun load(source: FeedSource): List<FeedItem> = withContext(Dispatchers.IO) {
        parse(get(source.url), source)
    }

    private fun get(url: String): String {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Feed request failed (${response.code})")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        val sources: List<FeedSource> = listOf(
            FeedSource("Yahoo Finance", "https://finance.yahoo.com/news/rssindex", FeedCategory.Markets),
            FeedSource("BBC Business", "https://feeds.bbci.co.uk/news/business/rss.xml", FeedCategory.Markets),
            FeedSource("CoinDesk", "https://www.coindesk.com/arc/outboundfeeds/rss/", FeedCategory.Crypto),
            FeedSource("Cointelegraph", "https://cointelegraph.com/rss", FeedCategory.Crypto),
        )

        fun parse(xml: String, source: FeedSource): List<FeedItem> {
            val root = Xml.parse(xml).rootElement()
            return if (root.matchesName("feed")) {
                parseAtom(root, source)
            } else {
                val channel = root.descendants("channel").firstOrNull() ?: root
                parseRss(channel, source)
            }
        }

        private fun parseRss(channel: Element, source: FeedSource): List<FeedItem> =
            channel.children().filter { it.matchesName("item") }.mapNotNull { item ->
                val title = item.childText("title")
                val url = item.childText("link").ifBlank {
                    item.child("guid")?.textContent?.trim().orEmpty()
                }
                if (title.isBlank() || !url.startsWith("http")) return@mapNotNull null
                FeedItem(
                    id = item.childText("guid").ifBlank { url },
                    title = stripTags(title),
                    source = source.name,
                    url = url,
                    publishedAt = parseDate(item.childText("pubDate").ifBlank { item.childText("date") }),
                    summary = stripTags(
                        item.childText("description").ifBlank { item.childText("encoded") },
                    ).take(220),
                    category = source.category,
                )
            }

        private fun parseAtom(feed: Element, source: FeedSource): List<FeedItem> =
            feed.children().filter { it.matchesName("entry") }.mapNotNull { entry ->
                val title = entry.childText("title")
                val url = entry.atomLink() ?: entry.childText("id")
                if (title.isBlank() || !url.startsWith("http")) return@mapNotNull null
                FeedItem(
                    id = entry.childText("id").ifBlank { url },
                    title = stripTags(title),
                    source = source.name,
                    url = url,
                    publishedAt = parseDate(
                        entry.childText("published").ifBlank { entry.childText("updated") },
                    ),
                    summary = stripTags(
                        entry.childText("summary").ifBlank { entry.childText("content") },
                    ).take(220),
                    category = source.category,
                )
            }

        fun parseDate(raw: String): Long {
            if (raw.isBlank()) return 0L
            val patterns = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd",
            )
            val normalized = raw.trim()
            patterns.forEach { pattern ->
                try {
                    val format = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = true
                    }
                    val parsed = format.parse(normalized.replace("Z", "+0000").let {
                        if (pattern.contains("'Z'")) normalized else it
                    })
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
            return 0L
        }

        fun stripTags(raw: String): String {
            if (raw.isBlank()) return ""
            return raw
                .replace(Regex("(?is)<script.*?>.*?</script>"), " ")
                .replace(Regex("(?is)<style.*?>.*?</style>"), " ")
                .replace(Regex("(?s)<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun Element.atomLink(): String? {
            children().filter { it.matchesName("link") }.forEach { link ->
                val rel = link.attr("rel").ifBlank { "alternate" }
                if (rel.equals("alternate", ignoreCase = true) || rel.equals("self", ignoreCase = true)) {
                    val href = link.attr("href")
                    if (href.startsWith("http")) return href
                }
            }
            children().filter { it.matchesName("link") }.forEach { link ->
                val href = link.attr("href")
                if (href.startsWith("http")) return href
            }
            return null
        }
    }
}
