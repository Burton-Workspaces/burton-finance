package com.burton.finance.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
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
import com.burton.finance.di.NetworkModule
import com.burton.finance.domain.AssetKind
import com.burton.finance.domain.Quote
import com.burton.finance.domain.TradingViewSymbol
import com.burton.finance.ui.theme.BurtonBlack
import com.burton.finance.ui.theme.BurtonIvory
import com.burton.finance.ui.theme.BurtonSand

data class ChartLaunch(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val kind: AssetKind,
)

fun Quote.toChartLaunch(): ChartLaunch = ChartLaunch(
    id = id,
    symbol = symbol,
    name = name,
    exchange = exchange,
    kind = kind,
)

@Composable
fun ChartModal(
    launch: ChartLaunch,
    onDismiss: () -> Unit,
) {
    var generation by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val tvSymbol = remember(launch) {
        TradingViewSymbol.from(launch.id, launch.symbol, launch.kind, launch.exchange)
    }
    val html = remember(tvSymbol) { TradingViewSymbol.html(tvSymbol) }

    Dialog(
        onDismissRequest = onDismiss,
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
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Exit", tint = BurtonIvory)
                }
                Text(
                    launch.name.ifBlank { launch.symbol },
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
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh chart", tint = BurtonIvory)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                ChartWebView(
                    html = html,
                    generation = generation,
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
                            title = "Couldn't load chart",
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
private fun ChartWebView(
    html: String,
    generation: Int,
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
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                isNestedScrollingEnabled = true
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        if (newProgress >= 100) onLoading(false)
                    }
                }
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
                        onLoading(false)
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
            val key = "$generation|$html"
            if (view.tag != key) {
                view.tag = key
                onLoading(true)
                view.loadDataWithBaseURL(
                    "https://www.tradingview.com",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )
}
