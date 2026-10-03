package com.burton.finance.ui.feed

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.burton.finance.data.parse.ArticleHtml
import com.burton.finance.data.parse.TinyJson
import com.burton.finance.data.parse.TinyJson.str
import com.burton.finance.di.NetworkModule
import com.burton.finance.domain.FeedItem
import com.burton.finance.ui.components.EmptyStatePanel
import com.burton.finance.ui.components.SegmentRow
import com.burton.finance.ui.theme.BurtonBlack
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonSand
import java.util.concurrent.atomic.AtomicBoolean

private enum class ArticleView(val label: String) {
    Read("Read"),
    Web("Web"),
}

@Composable
fun ArticleBrowserModal(
    item: FeedItem,
    startInReadMode: Boolean,
    onExit: () -> Unit,
) {
    var mode by remember {
        mutableStateOf(if (startInReadMode) ArticleView.Read else ArticleView.Web)
    }
    var generation by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val pendingRead = remember { AtomicBoolean(startInReadMode) }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BurtonBlack)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Rounded.Close, contentDescription = "Exit", tint = BurtonIvory)
                }
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BurtonIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        failed = false
                        loading = true
                        generation += 1
                    },
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh article", tint = BurtonIvory)
                }
            }
            SegmentRow(
                options = ArticleView.entries,
                selected = mode,
                label = { it.label },
                onSelect = { next ->
                    if (next != mode) {
                        failed = false
                        loading = true
                        mode = next
                        generation += 1
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                ArticleWebView(
                    url = item.url,
                    mode = mode,
                    generation = generation,
                    pendingRead = pendingRead,
                    item = item,
                    onLoading = { loading = it },
                    onFailed = {
                        failed = true
                        loading = false
                    },
                )
                if (loading && !failed) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurtonSand)
                    }
                }
                if (failed) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        EmptyStatePanel(
                            title = "Couldn't load article",
                            action = "Tap to retry",
                            onAction = {
                                failed = false
                                loading = true
                                generation += 1
                            },
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    url: String,
    mode: ArticleView,
    generation: Int,
    pendingRead: AtomicBoolean,
    item: FeedItem,
    onLoading: (Boolean) -> Unit,
    onFailed: () -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.userAgentString = NetworkModule.USER_AGENT
                isNestedScrollingEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val scheme = request.url.scheme?.lowercase()
                        return scheme != "http" && scheme != "https"
                    }

                    override fun onPageFinished(view: WebView, finishedUrl: String) {
                        if (view.progress < 100) return
                        if (finishedUrl != view.url) return
                        if (pendingRead.getAndSet(false) && finishedUrl.startsWith("http")) {
                            applyReadMode(view, item) { ok ->
                                if (!ok) onFailed()
                            }
                        } else {
                            onLoading(false)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        if (request.isForMainFrame) onFailed()
                    }
                }
            }
        },
        update = { view ->
            val key = "$generation|$mode|$url"
            if (view.tag != key) {
                view.tag = key
                pendingRead.set(mode == ArticleView.Read)
                onLoading(true)
                view.loadUrl(url)
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )
}

private fun applyReadMode(
    view: WebView,
    item: FeedItem,
    done: (Boolean) -> Unit,
) {
    view.evaluateJavascript(ArticleHtml.EXTRACT_SCRIPT) { raw ->
        val extracted = decodeExtracted(raw)
        val title = extracted.title.ifBlank { item.title }
        val html = when {
            extracted.text.length >= 80 -> ArticleHtml.sanitize(extracted.html)
            item.summary.isNotBlank() -> "<p>${ArticleHtml.escape(item.summary)}</p>"
            else -> ArticleHtml.sanitize(extracted.html)
        }
        if (title.isBlank() && ArticleHtml.textOf(html).isBlank()) {
            done(false)
            return@evaluateJavascript
        }
        val source = listOfNotNull(item.source, item.category.label).joinToString(" · ")
        val document = ArticleHtml.readerDocument(title, source, html)
        view.loadDataWithBaseURL(item.url, document, "text/html", "utf-8", item.url)
        done(true)
    }
}

private data class JsExtract(val title: String, val html: String, val text: String)

private fun decodeExtracted(raw: String?): JsExtract {
    if (raw.isNullOrBlank() || raw == "null") return JsExtract("", "", "")
    val inner = runCatching { TinyJson.parse(raw) as? String }.getOrNull() ?: raw.trim('"')
    if (inner.isBlank()) return JsExtract("", "", "")
    val obj = runCatching { TinyJson.parseObject(inner) }.getOrDefault(emptyMap())
    return JsExtract(
        title = obj.str("title"),
        html = obj.str("html"),
        text = obj.str("text"),
    )
}
