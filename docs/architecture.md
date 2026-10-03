# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, and OkHttp. UI never talks HTTP directly; screens collect `FinanceRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      TrackedSymbol, Quote, FeedItem, MarketCatalog, formatters
data/
  parse      tiny JSON + XML helpers
  market     Yahoo Finance quotes/search, CoinGecko crypto
  feed       RSS/Atom sources
  repository FinanceRepository + LocalPrefs (DataStore)
di/          OkHttp client
```

## Quotes

`YahooFinance` loads batches from `https://query1.finance.yahoo.com/v7/finance/quote`. If a batch fails, it falls back to `v8/finance/chart` per symbol (also used for 3-month sparklines). Search and watchlist news use `v1/finance/search`.

International tickers keep Yahoo suffixes (`.L`, `.T`, `.HK`, `.DE`, `.PA`, `.TO`, `.AX`, `.NS`, `.KS`, `.SA`, `.SS`, `.TW`, `.SI`, `.SW`, `.AS`). Indices use `^` prefixes.

`CoinGecko` loads `https://api.coingecko.com/api/v3/coins/markets` (USD, sparkline) and ` /search`. Crypto watchlist ids are `cg:{id}` so they never collide with Yahoo symbols.

## Feed

`NewsFeeds` pulls RSS/Atom:

- Yahoo Finance news index
- BBC Business
- CoinDesk
- Cointelegraph

Items are parsed with the same XML helpers as Burton Pod. Watchlist headlines come from Yahoo search `newsCount`. Tapping an item opens **ArticleBrowserModal**: a full-screen in-app WebView. **Web** loads the article URL. **Read** injects an extractor after load and replaces the page with a Burton-styled document (`ArticleHtml`). HTTPS links stay in the WebView; other schemes are ignored.

Tapping a sparkline opens **ChartModal**, another full-screen WebView. It embeds TradingView’s free Advanced Real-Time Chart (`s.tradingview.com/tv.js`) for the mapped symbol (Yahoo suffixes, US venues, indices, and CoinGecko `cg:` ids). The widget stays in-app; non-HTTPS schemes are ignored.

## Snapshot and cache

`FinanceRepository` is a process singleton. `start()`:

1. Hydrates DataStore (`burton_finance`) so Watchlist is not an empty spinner (also restores whether articles open in read mode)
2. Refreshes saved quotes
3. Loads the feed

Markets quotes load when that tab is opened. Removing a symbol drops it from the list, then persists.

## UI shell

`MainActivity` hosts a `NavHost` and a persistent bottom bar. Tab order is Watchlist → Markets → Feed. Symbol detail is a nested destination (bar hidden). Add-symbol and Settings are **FullScreenModal** overlays from Watchlist. Feed articles use **ArticleBrowserModal** (Exit, Read/Web, Refresh).
