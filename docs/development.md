# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`). GitHub Actions signing is [build automation](build-automation.md).

Debug application id is `com.burton.finance.debug` so it can sit next to a signed install.

## Layout

```
app/src/main/java/com/burton/finance/
  MainActivity.kt              nav
  data/parse/                  tiny JSON + XML
  data/market/                 Yahoo Finance, CoinGecko
  data/feed/                   RSS/Atom
  data/repository/             FinanceRepository, DataStore
  domain/                      models, catalog, formatters
  ui/watchlist, markets, feed, detail, search, settings, components, theme
app/src/test/java/…            parser and codec tests (no device)
```

Parser tests cover Yahoo quote/chart/search JSON, CoinGecko markets, RSS/Atom, region suffixes, and watchlist catalog round-trip. Run those before changing network parsing.

## Network while debugging

Quotes, search, crypto, and the feed need the internet. The emulator is fine. All upstream APIs are HTTPS.

Yahoo Finance sometimes rate-limits unidentified clients. The OkHttp interceptor sends a mobile Chrome user agent. Empty search results usually mean a too-short query or a blocked request, not a UI bug.

CoinGecko’s public API is also rate-limited. The crypto board retries on the next refresh.

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).
