package com.subtrack.core.common.currency

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {

    fun currencyCodeForLocale(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(locale).currencyCode }
            .getOrDefault("USD")

    fun format(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
        maximumFractionDigits: Int? = null
    ): String {
        val currency = currency(currencyCode)
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            val currencyFractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
            val digits = maximumFractionDigits ?: currencyFractionDigits
            this.maximumFractionDigits = digits
            minimumFractionDigits = minOf(currencyFractionDigits, digits)
        }.format(amount)
    }

    fun symbol(currencyCode: String, locale: Locale = Locale.getDefault()): String =
        currency(currencyCode).getSymbol(locale)

    fun displayName(currencyCode: String, locale: Locale = Locale.getDefault()): String =
        currency(currencyCode).getDisplayName(locale)

    fun availableCurrencyCodes(
        preferredCurrencyCode: String,
        locale: Locale = Locale.getDefault()
    ): List<String> = Locale.getAvailableLocales()
        .asSequence()
        .mapNotNull { availableLocale ->
            runCatching { Currency.getInstance(availableLocale).currencyCode }.getOrNull()
        }
        .filterNot { it == "XXX" }
        .distinct()
        .sortedWith(
            compareBy<String> { it != preferredCurrencyCode }
                .thenBy { displayName(it, locale) }
        )
        .toList()

    private fun currency(currencyCode: String): Currency =
        runCatching { Currency.getInstance(currencyCode.uppercase(Locale.US)) }
            .getOrElse { Currency.getInstance("USD") }
}
