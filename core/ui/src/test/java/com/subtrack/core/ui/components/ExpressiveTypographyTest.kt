package com.subtrack.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressiveTypographyTest {

    @Test
    fun parsesUsdWithDecimals() {
        assertEquals(
            ParsedMoneyParts(prefix = "$", integerPart = "12", fractionalPart = ".16", suffix = ""),
            parseFormattedMoney("$12.16")
        )
    }

    @Test
    fun parsesUsdWholeThousandsWithoutMistakingCommaForDecimal() {
        assertEquals(
            ParsedMoneyParts(prefix = "$", integerPart = "1,440", fractionalPart = "", suffix = ""),
            parseFormattedMoney("$1,440")
        )
    }

    @Test
    fun parsesUsdThousandsAndDecimals() {
        assertEquals(
            ParsedMoneyParts(prefix = "$", integerPart = "1,234", fractionalPart = ".56", suffix = ""),
            parseFormattedMoney("$1,234.56")
        )
    }

    @Test
    fun parsesZeroDecimalYenAndWon() {
        assertEquals(
            ParsedMoneyParts(prefix = "¥", integerPart = "6,469", fractionalPart = "", suffix = ""),
            parseFormattedMoney("¥6,469")
        )
        assertEquals(
            ParsedMoneyParts(prefix = "₩", integerPart = "17,000", fractionalPart = "", suffix = ""),
            parseFormattedMoney("₩17,000")
        )
    }

    @Test
    fun parsesMultiCharCurrencyPrefix() {
        assertEquals(
            ParsedMoneyParts(prefix = "CA$", integerPart = "16", fractionalPart = ".49", suffix = ""),
            parseFormattedMoney("CA$16.49")
        )
    }

    @Test
    fun parsesEuropeanSuffixCurrencyWithCommaDecimal() {
        assertEquals(
            ParsedMoneyParts(prefix = "", integerPart = "12", fractionalPart = ",99", suffix = " €"),
            parseFormattedMoney("12,99 €")
        )
        assertEquals(
            ParsedMoneyParts(prefix = "", integerPart = "1.299", fractionalPart = ",50", suffix = " €"),
            parseFormattedMoney("1.299,50 €")
        )
    }

    @Test
    fun fallsBackGracefullyWhenNoDigitsPresent() {
        assertEquals(
            ParsedMoneyParts(prefix = "", integerPart = "Free", fractionalPart = "", suffix = ""),
            parseFormattedMoney("Free")
        )
    }
}
