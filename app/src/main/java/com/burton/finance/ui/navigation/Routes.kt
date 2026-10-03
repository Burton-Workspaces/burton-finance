package com.burton.finance.ui.navigation

import android.net.Uri

object Routes {
    const val WATCHLIST = "watchlist"
    const val MARKETS = "markets"
    const val FEED = "feed"
    const val SYMBOL = "symbol/{symbolId}"

    fun symbol(symbolId: String): String = "symbol/${Uri.encode(symbolId)}"
}
