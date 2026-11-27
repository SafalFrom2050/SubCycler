package com.subtrack.core.domain.model

import com.subtrack.core.domain.engine.RotationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RegionalPriceCatalogTest {

    @Test
    fun jpyCatalogProvidesCuratedMultiTierMonthlyAndYearlyPlans() {
        val netflixJpy = RegionalPriceCatalog.tiersFor("Netflix", "JPY")
        assertTrue(netflixJpy.size >= 3)
        assertEquals(1590.0, RegionalPriceCatalog.defaultTierFor("Netflix", "JPY")!!.price, 0.001)

        val primeJpy = RegionalPriceCatalog.tiersFor("Amazon Prime Video", "JPY")
        assertTrue(primeJpy.any { it.billingPeriod == BillingPeriod.MONTHLY && it.price == 600.0 })
        assertTrue(primeJpy.any { it.billingPeriod == BillingPeriod.YEARLY && it.price == 5900.0 })
    }

    @Test
    fun unlistedCurrencyReturnsEmptyTiersWithoutFabricatedConversion() {
        assertTrue(RegionalPriceCatalog.tiersFor("Netflix", "CHF").isEmpty())
        assertNull(RegionalPriceCatalog.defaultTierFor("Netflix", "CHF"))
    }

    @Test
    fun formatEditablePriceDropsDecimalsForZeroDecimalAndWholePrices() {
        assertEquals("1590", RegionalPriceCatalog.formatEditablePrice(1590.0, "JPY"))
        assertEquals("20", RegionalPriceCatalog.formatEditablePrice(20.0, "USD"))
        assertEquals("17.99", RegionalPriceCatalog.formatEditablePrice(17.99, "USD"))
    }

    @Test
    fun yearlySubscriptionNormalizesAnnualAndMonthlySpendCleanly() {
        val yearlyPrime = Subscription(
            name = "Amazon Prime Video",
            cost = 5900.0,
            currency = "JPY",
            billingPeriod = BillingPeriod.YEARLY,
            startDate = LocalDate.of(2026, 1, 1),
            strategy = RotationStrategy.CONTINUOUS
        )
        val status = RotationEngine.calculateCycleStatus(yearlyPrime, LocalDate.of(2026, 2, 1))
        assertEquals(5900.0, status.standardAnnualCost, 0.001)
        assertEquals(5900.0, status.optimizedAnnualCost, 0.001)
        assertEquals(0.0, status.annualSavings, 0.001)
        assertEquals(5900.0 / 12.0, yearlyPrime.monthlyEquivalentCost, 0.001)
        assertNotNull(RegionalPriceCatalog.defaultTierFor("Spotify", "EUR"))
    }

    @Test
    fun topTwentyPresetsAllHaveDistinctBrandColorsAndUsdTiers() {
        assertEquals(20, PresetLibrary.presets.size)
        val hexes = PresetLibrary.presets.map { it.colorHex.uppercase() }.toSet()
        assertEquals(20, hexes.size)
        PresetLibrary.presets.forEach { preset ->
            assertTrue("Invalid hex for ${preset.name}: ${preset.colorHex}", preset.colorHex.matches(Regex("#[0-9A-Fa-f]{6}")))
            assertNotNull("Missing USD default tier for ${preset.name}", RegionalPriceCatalog.defaultTierFor(preset.name, "USD"))
        }
        assertEquals("#E50914", PresetLibrary.brandColorHexForName("Netflix"))
        assertEquals("#F47521", PresetLibrary.brandColorHexForName("Crunchyroll Mega Fan"))
        assertEquals("#6366F1", PresetLibrary.brandColorHexForName("Google AI Pro"))
        assertEquals("#5865F2", PresetLibrary.brandColorHexForName("Discord Nitro"))
        assertNull(PresetLibrary.brandColorHexForName("Neighborhood Gym"))
    }
}
