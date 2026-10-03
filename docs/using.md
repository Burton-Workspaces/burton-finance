# Using the app

Burton Finance keeps the watchlist on this phone. Quotes, crypto markets, and feed articles come from public HTTPS APIs. There is no Burton account.

## Screens

### Watchlist

The home list. Each card shows the name, ticker, last price, day change, and a sparkline once a quote has loaded.

- **Add** opens a full-screen search. Type a ticker or name; pick **Add**. Already-saved rows show **Added**. Yahoo Finance covers global exchanges; CoinGecko covers cryptocurrency.
- **Refresh** reloads every saved symbol.
- Tap a card for the quote detail.

An empty watchlist uses the Burton empty-state card: **Nothing watched yet**, with **Tap to browse Markets**.

### Markets

Curated lists under three boards:

- **United States** — indices, large caps, ETFs
- **International** — UK, Japan, Hong Kong, Europe, Canada, Australia, India, Korea, Greater China, Brazil, Singapore
- **Crypto** — CoinGecko market-cap list (price, 24h change, 7-day sparkline)

Tap a row for detail. Add from the detail screen.

### Feed

A combined article list from Yahoo Finance, BBC Business, CoinDesk, and Cointelegraph. Watchlist symbols contribute extra Yahoo headlines when you have names saved.

Chips filter **All**, **Markets**, **Crypto**, and **Watchlist**. Tap a card to open the article in the system browser.

An empty feed uses the Burton empty-state card: **Nothing in the feed**.

### Detail

Last price, signed change, sparkline, previous close, day high/low, 52-week high/low, volume, market cap, currency, and region. **Add to watchlist** / **Remove from watchlist** persist immediately.

### Settings

Opened from the Watchlist gear.

| Row | What it does |
| --- | --- |
| Watchlist | How many symbols are saved |
| Markets | Boards covered |
| Feed | Source names and current article count |
| Burton Finance | App version from `version.txt`. Long-press files an issue. |

### File an issue

Shake the phone, or long-press **About** in Settings. Burton Issues opens on New issue with this app already selected. Nothing is posted until you submit; Back cancels.

## What lives on-device

Saved watchlist symbols (id, ticker, name, exchange, region, kind) in DataStore. Quotes and feed items are memory-only and refresh from the network.
