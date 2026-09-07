package com.subtrack.core.common.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {

    @Test
    fun localeSuggestsItsIsoCurrency() {
        assertEquals("JPY", MoneyFormatter.currencyCodeForLocale(Locale.JAPAN))
        assertEquals("EUR", MoneyFormatter.currencyCodeForLocale(Locale.GERMANY))
    }

    @Test
    fun yenUsesZeroFractionDigits() {
        val formatted = MoneyFormatter.format(1234.56, "JPY", Locale.US)

        assertTrue(formatted.contains("1,235"))
        assertFalse(formatted.contains(".56"))
    }

    @Test
    fun currencyFractionDigitsWinOverTheDevicesCurrency() {
        val formatted = MoneyFormatter.format(1234.5, "USD", Locale.JAPAN)

        assertTrue(formatted.contains("1,234.50"))
    }

    @Test
    fun euroUsesLocaleSpecificSeparators() {
        val formatted = MoneyFormatter.format(1234.5, "EUR", Locale.GERMANY)

        assertTrue(formatted.contains("1.234,50"))
        assertTrue(formatted.contains("€"))
    }

    @Test
    fun pickerOffersCurrentCountryCurrenciesWithoutHistoricalCodes() {
        val currencies = MoneyFormatter.availableCurrencyCodes("JPY", Locale.JAPAN)

        assertEquals("JPY", currencies.first())
        assertTrue(currencies.contains("EUR"))
        assertFalse(currencies.contains("AFA"))
        assertFalse(currencies.contains("XUA"))
    }
}
