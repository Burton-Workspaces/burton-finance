package com.burton.finance.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun formatPrice(value: Double, currency: String): String {
    if (!value.isFinite()) return "—"
    val code = currency.ifBlank { "USD" }.uppercase(Locale.US)
    val decimals = when {
        abs(value) >= 1000 && code in ZERO_DECIMAL -> 0
        abs(value) >= 1000 -> 2
        abs(value) >= 1 -> 2
        abs(value) >= 0.01 -> 4
        else -> 6
    }
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = decimals
            minimumFractionDigits = if (decimals <= 2) decimals else 2
            this.currency = Currency.getInstance(code)
        }
        format.format(value)
    } catch (_: Exception) {
        val pattern = if (decimals == 0) "%,.0f" else "%,.${decimals}f"
        "$code ${pattern.format(Locale.US, value)}"
    }
}

fun formatCompact(value: Double): String {
    if (!value.isFinite()) return "—"
    val absValue = abs(value)
    val (scaled, suffix) = when {
        absValue >= 1_000_000_000_000 -> value / 1_000_000_000_000.0 to "T"
        absValue >= 1_000_000_000 -> value / 1_000_000_000.0 to "B"
        absValue >= 1_000_000 -> value / 1_000_000.0 to "M"
        absValue >= 1_000 -> value / 1_000.0 to "K"
        else -> return NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }.format(value.roundToInt())
    }
    return "%.1f%s".format(Locale.US, scaled, suffix)
}

fun formatPercent(changePercent: Double): String {
    if (!changePercent.isFinite()) return "—"
    val sign = if (changePercent > 0) "+" else ""
    return "%s%.2f%%".format(Locale.US, sign, changePercent)
}

fun formatSignedPrice(change: Double, currency: String): String {
    val body = formatPrice(abs(change), currency)
    return when {
        change > 0 -> "+$body"
        change < 0 -> "-$body"
        else -> body
    }
}

fun assetKindFromYahoo(quoteType: String, symbol: String): AssetKind {
    val type = quoteType.uppercase(Locale.US)
    val upper = symbol.uppercase(Locale.US)
    return when {
        type.contains("CRYPTO") || upper.endsWith("-USD") || upper.endsWith("-USDT") -> AssetKind.Crypto
        type.contains("ETF") -> AssetKind.Etf
        type.contains("INDEX") || upper.startsWith("^") -> AssetKind.Index
        else -> AssetKind.Equity
    }
}

fun regionForSymbol(symbol: String, exchange: String = ""): String {
    val upper = symbol.uppercase(Locale.US)
    val ex = exchange.uppercase(Locale.US)
    return when {
        upper.endsWith("-USD") || upper.endsWith("-USDT") || ex.contains("CCC") || ex.contains("CRYPTO") -> "Crypto"
        upper.endsWith(".L") || ex in setOf("LSE", "LON") -> "United Kingdom"
        upper.endsWith(".T") || ex in setOf("JPX", "TYO", "OSE") -> "Japan"
        upper.endsWith(".HK") || ex in setOf("HKG", "HKSE") -> "Hong Kong"
        upper.endsWith(".TO") || upper.endsWith(".V") || ex in setOf("TOR", "TSX", "VAN") -> "Canada"
        upper.endsWith(".AX") || ex in setOf("ASX", "AUS") -> "Australia"
        upper.endsWith(".NS") || upper.endsWith(".BO") || ex in setOf("NSI", "BSE", "NSE") -> "India"
        upper.endsWith(".KS") || upper.endsWith(".KQ") || ex in setOf("KSC", "KOE") -> "Korea"
        upper.endsWith(".SA") || ex in setOf("SAO", "BVMF") -> "Brazil"
        upper.endsWith(".SS") || upper.endsWith(".SZ") -> "China"
        upper.endsWith(".TW") || ex in setOf("TAI", "TPE") -> "Taiwan"
        upper.endsWith(".SI") || ex in setOf("SES", "SGX") -> "Singapore"
        upper.endsWith(".SW") || ex in setOf("EBS", "SWX") -> "Switzerland"
        upper.endsWith(".DE") || upper.endsWith(".F") || ex in setOf("GER", "FRA") -> "Germany"
        upper.endsWith(".PA") || ex in setOf("PAR", "EPA") -> "France"
        upper.endsWith(".AS") || ex in setOf("AMS", "AEX") -> "Netherlands"
        upper.endsWith(".MI") || ex == "MIL" -> "Italy"
        upper.endsWith(".MC") || ex == "MCE" -> "Spain"
        else -> "United States"
    }
}

fun marketBoardFor(region: String, kind: AssetKind): MarketBoard = when {
    kind == AssetKind.Crypto || region.equals("Crypto", ignoreCase = true) -> MarketBoard.Crypto
    region.equals("United States", ignoreCase = true) -> MarketBoard.US
    else -> MarketBoard.International
}

private val ZERO_DECIMAL = setOf("JPY", "KRW", "VND", "IDR")
