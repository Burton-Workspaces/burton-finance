package com.burton.finance.domain

import java.util.Locale
import java.util.TimeZone

object TradingViewSymbol {
    fun from(
        id: String,
        symbol: String,
        kind: AssetKind,
        exchange: String = "",
    ): String {
        val raw = symbol.ifBlank { id.removePrefix("cg:") }.trim()
        if (raw.isBlank()) return ""
        val upper = raw.uppercase(Locale.US)
        INDICES[upper]?.let { return it }
        if (kind == AssetKind.Crypto || id.startsWith("cg:") || isYahooCrypto(upper)) {
            return crypto(upper)
        }
        val suffix = yahooSuffix(upper)
        if (suffix != null) {
            val ticker = upper.substringBeforeLast('.')
            return "${suffix.first}:${suffix.second(ticker)}"
        }
        if (upper.startsWith("^")) {
            return "TVC:${upper.trimStart('^')}"
        }
        val ticker = classShare(upper)
        val venue = usVenue(exchange)
        return if (venue != null) "$venue:$ticker" else ticker
    }

    fun html(tvSymbol: String, timezone: String = chartTimezone()): String {
        val symbolJson = jsonString(tvSymbol)
        val timezoneJson = jsonString(timezone.ifBlank { "Etc/UTC" })
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
              <style>
                html,body{margin:0;padding:0;height:100%;width:100%;background:#000;overflow:hidden}
                #tv_chart{height:100%;width:100%}
              </style>
            </head>
            <body>
              <div id="tv_chart"></div>
              <script src="https://s.tradingview.com/tv.js"></script>
              <script>
                new TradingView.widget({
                  autosize: true,
                  symbol: $symbolJson,
                  interval: "D",
                  timezone: $timezoneJson,
                  theme: "dark",
                  style: "1",
                  locale: "en",
                  backgroundColor: "#000000",
                  gridColor: "rgba(46, 46, 46, 0.35)",
                  hide_top_toolbar: false,
                  hide_side_toolbar: false,
                  allow_symbol_change: true,
                  save_image: false,
                  hide_legend: false,
                  withdateranges: true,
                  details: false,
                  hotlist: false,
                  calendar: false,
                  enable_publishing: false,
                  hide_volume: false,
                  studies: ["STD;SMA"],
                  container_id: "tv_chart",
                  support_host: "https://www.tradingview.com"
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun chartTimezone(zone: TimeZone = TimeZone.getDefault()): String {
        val id = zone.id
        return if (id.startsWith("GMT") && id.length > 3) "Etc/UTC" else id
    }

    private fun crypto(upper: String): String {
        val base = when {
            upper.endsWith("-USD") -> upper.removeSuffix("-USD")
            upper.endsWith("-USDT") -> upper.removeSuffix("-USDT")
            else -> upper.substringBefore("-")
        }.replace("-", "")
        return "CRYPTO:${base}USD"
    }

    private fun jsonString(value: String): String = buildString(value.length + 2) {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun isYahooCrypto(upper: String): Boolean =
        upper.endsWith("-USD") || upper.endsWith("-USDT")

    private fun classShare(symbol: String): String =
        if (symbol.contains('-') && !symbol.contains('.')) symbol.replace('-', '.') else symbol

    private fun yahooSuffix(upper: String): Pair<String, (String) -> String>? {
        val suffix = upper.substringAfterLast('.', missingDelimiterValue = "")
        if (suffix.isEmpty() || suffix == upper) return null
        val exchange = SUFFIX_EXCHANGE[suffix] ?: return null
        val normalize: (String) -> String =
            if (suffix == "HK") { ticker -> ticker.trimStart('0').ifBlank { "0" } }
            else { ticker -> ticker }
        return exchange to normalize
    }

    private fun usVenue(exchange: String): String? {
        val ex = exchange.uppercase(Locale.US)
        return when {
            ex.isBlank() -> null
            ex.contains("NASDAQ") || ex in setOf("NMS", "NGM", "NCM") -> "NASDAQ"
            ex.contains("NYSE") && (ex.contains("ARCA") || ex.contains("AMERICAN")) -> "AMEX"
            ex in setOf("NYSE", "NYQ") || (ex.contains("NYSE") && !ex.contains("ARCA")) -> "NYSE"
            ex in setOf("PCX", "ARCA", "AMEX", "ASE") || ex.contains("BZX") -> "AMEX"
            else -> null
        }
    }

    private val INDICES = mapOf(
        "^GSPC" to "TVC:SPX",
        "^DJI" to "TVC:DJI",
        "^IXIC" to "TVC:IXIC",
        "^RUT" to "TVC:RUT",
        "^VIX" to "TVC:VIX",
        "^FTSE" to "TVC:UKX",
        "^N225" to "TVC:NI225",
        "^HSI" to "TVC:HSI",
        "^GDAXI" to "TVC:DAX",
        "^FCHI" to "TVC:CAC40",
        "^GSPTSE" to "TVC:TSX",
        "^AXJO" to "TVC:XJO",
        "^NSEI" to "TVC:NIFTY",
        "^KS11" to "TVC:KOSPI",
        "^BVSP" to "TVC:IBOV",
    )

    private val SUFFIX_EXCHANGE = mapOf(
        "L" to "LSE",
        "T" to "TSE",
        "HK" to "HKEX",
        "DE" to "XETR",
        "F" to "FWB",
        "PA" to "EURONEXT",
        "AS" to "EURONEXT",
        "SW" to "SIX",
        "MI" to "MIL",
        "MC" to "BME",
        "TO" to "TSX",
        "V" to "TSXV",
        "AX" to "ASX",
        "NS" to "NSE",
        "BO" to "BSE",
        "KS" to "KRX",
        "KQ" to "KOSDAQ",
        "SA" to "BMFBOVESPA",
        "SS" to "SSE",
        "SZ" to "SZSE",
        "TW" to "TWSE",
        "SI" to "SGX",
    )
}
