package com.burton.finance.data.parse

data class ExtractedArticle(
    val title: String,
    val html: String,
    val text: String,
)

object ArticleHtml {
    private val scriptStyle = Regex("(?is)<(script|style|noscript|iframe)\\b[^>]*>.*?</\\1>")
    private val comments = Regex("(?is)<!--.*?-->")
    private val articleBlock = Regex("(?is)<article\\b[^>]*>(.*?)</article>")
    private val namedBody = Regex(
        "(?is)<(?:div|section)\\b[^>]*(?:itemprop\\s*=\\s*[\"']articleBody[\"']|class\\s*=\\s*[\"'][^\"']*" +
            "(?:article-body|story-body|post-content|entry-content|ArticleBody)[^\"']*[\"'])[^>]*>(.*?)</(?:div|section)>",
    )
    private val paragraph = Regex("(?is)<p\\b[^>]*>(.*?)</p>")
    private val titleTag = Regex("(?is)<title\\b[^>]*>(.*?)</title>")
    private val ogTitle = Regex("(?is)<meta\\b[^>]*property\\s*=\\s*[\"']og:title[\"'][^>]*>")
    private val ogContent = Regex("(?is)content\\s*=\\s*[\"'](.*?)[\"']")
    private val heading = Regex("(?is)<h1\\b[^>]*>(.*?)</h1>")

    fun extract(
        raw: String,
        fallbackTitle: String,
        fallbackHtml: String = "",
    ): ExtractedArticle {
        val cleaned = comments.replace(scriptStyle.replace(raw, " "), " ")
        val title = decode(
            metaTitle(cleaned)
                ?: first(titleTag, cleaned)
                ?: first(heading, cleaned)
                ?: fallbackTitle,
        ).ifBlank { fallbackTitle }
        val body = articleBlock.find(cleaned)?.groupValues?.getOrNull(1)
            ?: namedBody.find(cleaned)?.groupValues?.getOrNull(1)
            ?: paragraphs(cleaned)
        val html = sanitize(body.ifBlank { fallbackHtml }).ifBlank {
            fallbackHtml.ifBlank { "<p>${escape(fallbackTitle)}</p>" }
        }
        val text = textOf(html).ifBlank { textOf(fallbackHtml) }
        return ExtractedArticle(title = title, html = html, text = text)
    }

    fun readerDocument(title: String, source: String, bodyHtml: String): String {
        val safeTitle = escape(title)
        val safeSource = escape(source)
        val body = sanitize(bodyHtml).ifBlank { "<p></p>" }
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1"/>
              <style>
                html, body {
                  background: #000000;
                  color: #F5F2EC;
                  font-family: sans-serif;
                  margin: 0;
                }
                body {
                  font-size: 18px;
                  line-height: 1.55;
                  padding: 8px 4px 48px;
                  max-width: 40rem;
                  margin: 0 auto;
                }
                h1 {
                  font-size: 1.55rem;
                  font-weight: 500;
                  line-height: 1.25;
                  margin: 0 0 10px;
                }
                .meta {
                  color: #9A958C;
                  font-size: 0.92rem;
                  margin: 0 0 22px;
                }
                p { margin: 0 0 1em; }
                a { color: #D4C4A8; }
                img, video, figure { max-width: 100%; height: auto; }
                figure { margin: 0 0 1em; }
              </style>
            </head>
            <body>
              <h1>$safeTitle</h1>
              <p class="meta">$safeSource</p>
              $body
            </body>
            </html>
        """.trimIndent()
    }

    fun sanitize(html: String): String {
        if (html.isBlank()) return ""
        return html
            .replace(scriptStyle, "")
            .replace(Regex("(?i)\\son\\w+\\s*="), " data-dropped=")
            .replace(Regex("(?i)javascript:"), "")
            .trim()
    }

    fun textOf(html: String): String {
        if (html.isBlank()) return ""
        return decode(
            html
                .replace(scriptStyle, " ")
                .replace(Regex("(?s)<[^>]+>"), " ")
                .replace(Regex("\\s+"), " "),
        ).trim()
    }

    fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    const val EXTRACT_SCRIPT = """
        (function() {
          function drop(root, selector) {
            try { root.querySelectorAll(selector).forEach(function(node) { node.remove(); }); } catch (e) {}
          }
          var src = document.querySelector('article')
            || document.querySelector('[itemprop="articleBody"]')
            || document.querySelector('[class*="article-body"],[class*="story-body"],[class*="post-content"],[class*="entry-content"],[class*="ArticleBody"]')
            || document.querySelector('main')
            || document.body;
          if (!src) return JSON.stringify({title: document.title || '', html: '', text: ''});
          var node = src.cloneNode(true);
          drop(node, 'script,style,noscript,iframe,nav,footer,aside,form,button,svg,input,textarea,select,[role="navigation"],[role="banner"],[role="complementary"]');
          drop(node, '.ad,.ads,.advert,.share,.social,.related,.newsletter,.paywall,.comments,header');
          var heading = document.querySelector('h1');
          var title = ((heading && heading.innerText) || document.title || '').replace(/\s+/g, ' ').trim();
          var html = node.innerHTML || '';
          var text = (node.innerText || '').replace(/\s+/g, ' ').trim();
          return JSON.stringify({title: title, html: html, text: text});
        })();
    """

    private fun metaTitle(html: String): String? {
        val tag = ogTitle.find(html)?.value ?: return null
        return ogContent.find(tag)?.groupValues?.getOrNull(1)
    }

    private fun first(pattern: Regex, html: String): String? {
        val raw = pattern.find(html)?.groupValues?.getOrNull(1) ?: return null
        val text = textOf(raw)
        return text.ifBlank { null }
    }

    private fun paragraphs(html: String): String {
        val parts = paragraph.findAll(html).map { it.value }.toList()
        if (parts.size < 2) return ""
        return parts.joinToString("\n")
    }

    private fun decode(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .trim()
    }
}
