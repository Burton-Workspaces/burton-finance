# Burton Finance

An Android markets client. Track stocks and indices worldwide, follow cryptocurrency, and read a markets feed. There is no cloud account and no brokerage login.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-finance/releases). Droidify / F-Droid: [burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid) (`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`).

## What it does

- **Watchlist** — saved symbols with last price, day change, and sparkline; tap a card for detail, tap the sparkline for a full TradingView chart; search Yahoo Finance and CoinGecko to add
- **Markets** — United States, international exchanges (UK, Japan, Hong Kong, Europe, Canada, Australia, India, Korea, China, Taiwan, Brazil, Singapore), and CoinGecko crypto
- **Feed** — Yahoo Finance, BBC Business, CoinDesk, Cointelegraph, plus watchlist headlines; tap an article for an in-app reader (read mode on by default)
- **Detail** — price, day range, 52-week range, volume, market cap, sparkline (tap for candlesticks, indicators, and drawings), add/remove from the watchlist
- **Settings** — read mode, saved count, feed sources, version

First launch hydrates the watchlist from local cache, then refreshes quotes and the feed.

## Requirements

- Android 8.0+ (API 26)
- Internet access for quotes, crypto markets, search, and feed articles

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Screens and what lives on-device |
| [Architecture](docs/architecture.md) | Packages, Yahoo Finance, CoinGecko, RSS |
| [Development](docs/development.md) | Build, run, test, layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Self-hosted repo, Fingerprint, Pages publish script |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.finance.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

## License and scope

This is a household market viewer. It does not place trades, hold balances, or sign in to a broker, exchange, or news account.
