package com.subtrack.core.domain.model

import com.subtrack.core.common.currency.MoneyFormatter
import java.util.Locale

data class ServicePlanTier(
    val label: String,
    val price: Double,
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val isDefault: Boolean = false
)

/**
 * Offline, curated regional subscription price estimates across major markets.
 * SubCycler never fetches remote prices or guesses exchange-rate conversions:
 * unlisted currencies intentionally return an empty tier list so users enter exact local amounts.
 */
object RegionalPriceCatalog {
    const val LAST_VERIFIED_DATE = "2026-11"

    private val tiersByServiceAndCurrency: Map<String, Map<String, List<ServicePlanTier>>> = mapOf(
        "Netflix" to mapOf(
            "USD" to listOf(
                ServicePlanTier("With Ads", 7.99),
                ServicePlanTier("Standard", 17.99, isDefault = true),
                ServicePlanTier("Premium", 24.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("With Ads", 890.0),
                ServicePlanTier("Standard", 1590.0, isDefault = true),
                ServicePlanTier("Premium", 2290.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("With Ads", 5.99),
                ServicePlanTier("Standard", 13.99, isDefault = true),
                ServicePlanTier("Premium", 19.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("With Ads", 5.99),
                ServicePlanTier("Standard", 12.99, isDefault = true),
                ServicePlanTier("Premium", 18.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("With Ads", 7.99),
                ServicePlanTier("Standard", 18.99, isDefault = true),
                ServicePlanTier("Premium", 23.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("With Ads", 7.99),
                ServicePlanTier("Standard", 18.99, isDefault = true),
                ServicePlanTier("Premium", 25.99)
            ),
            "INR" to listOf(
                ServicePlanTier("Mobile", 149.0),
                ServicePlanTier("Basic", 199.0),
                ServicePlanTier("Standard", 499.0, isDefault = true),
                ServicePlanTier("Premium", 649.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("With Ads", 5500.0),
                ServicePlanTier("Standard", 13500.0, isDefault = true),
                ServicePlanTier("Premium", 17000.0)
            )
        ),
        "Disney+" to mapOf(
            "USD" to listOf(
                ServicePlanTier("With Ads", 9.99),
                ServicePlanTier("Premium", 15.99, isDefault = true),
                ServicePlanTier("Yearly", 159.99, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Standard", 1140.0, isDefault = true),
                ServicePlanTier("Standard Yearly", 11400.0, BillingPeriod.YEARLY),
                ServicePlanTier("Premium", 1520.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("With Ads", 5.99),
                ServicePlanTier("Standard", 9.99, isDefault = true),
                ServicePlanTier("Premium", 13.99),
                ServicePlanTier("Standard Yearly", 99.90, BillingPeriod.YEARLY)
            ),
            "GBP" to listOf(
                ServicePlanTier("With Ads", 4.99),
                ServicePlanTier("Standard", 8.99, isDefault = true),
                ServicePlanTier("Premium", 12.99),
                ServicePlanTier("Standard Yearly", 89.90, BillingPeriod.YEARLY)
            ),
            "CAD" to listOf(
                ServicePlanTier("With Ads", 8.99),
                ServicePlanTier("Standard", 12.99, isDefault = true),
                ServicePlanTier("Premium", 15.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("Standard", 13.99, isDefault = true),
                ServicePlanTier("Premium", 17.99),
                ServicePlanTier("Standard Yearly", 139.99, BillingPeriod.YEARLY)
            ),
            "KRW" to listOf(
                ServicePlanTier("Standard", 9900.0, isDefault = true),
                ServicePlanTier("Premium", 13900.0),
                ServicePlanTier("Standard Yearly", 99000.0, BillingPeriod.YEARLY)
            )
        ),
        "Max (HBO)" to mapOf(
            "USD" to listOf(
                ServicePlanTier("With Ads", 9.99),
                ServicePlanTier("Standard", 16.99, isDefault = true),
                ServicePlanTier("Ultimate", 20.99),
                ServicePlanTier("Standard Yearly", 169.99, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Basic Ads", 5.99),
                ServicePlanTier("Standard", 9.99, isDefault = true),
                ServicePlanTier("Premium", 13.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("Basic Ads", 11.99),
                ServicePlanTier("Standard", 15.99, isDefault = true),
                ServicePlanTier("Premium", 21.99)
            )
        ),
        "YouTube Premium" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Individual", 13.99, isDefault = true),
                ServicePlanTier("Yearly", 139.99, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 22.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("Individual", 1280.0, isDefault = true),
                ServicePlanTier("Yearly", 12800.0, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 2280.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("Individual", 12.99, isDefault = true),
                ServicePlanTier("Family", 23.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("Individual", 12.99, isDefault = true),
                ServicePlanTier("Family", 19.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("Individual", 12.99, isDefault = true),
                ServicePlanTier("Yearly", 129.99, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 22.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("Individual", 16.99, isDefault = true),
                ServicePlanTier("Family", 32.99)
            ),
            "INR" to listOf(
                ServicePlanTier("Individual", 149.0, isDefault = true),
                ServicePlanTier("Yearly", 1490.0, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 299.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("Individual", 14900.0, isDefault = true)
            )
        ),
        "Spotify" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Individual", 11.99, isDefault = true),
                ServicePlanTier("Duo", 16.99),
                ServicePlanTier("Family", 19.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("Individual", 980.0, isDefault = true),
                ServicePlanTier("Duo", 1280.0),
                ServicePlanTier("Family", 1580.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("Individual", 10.99, isDefault = true),
                ServicePlanTier("Duo", 14.99),
                ServicePlanTier("Family", 17.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("Individual", 11.99, isDefault = true),
                ServicePlanTier("Duo", 16.99),
                ServicePlanTier("Family", 19.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("Individual", 12.69, isDefault = true),
                ServicePlanTier("Duo", 17.89),
                ServicePlanTier("Family", 20.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("Individual", 13.99, isDefault = true),
                ServicePlanTier("Duo", 19.99),
                ServicePlanTier("Family", 23.99)
            ),
            "INR" to listOf(
                ServicePlanTier("Individual", 119.0, isDefault = true),
                ServicePlanTier("Duo", 149.0),
                ServicePlanTier("Family", 179.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("Individual", 10900.0, isDefault = true),
                ServicePlanTier("Duo", 16350.0)
            )
        ),
        "Apple Music" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Individual", 10.99, isDefault = true),
                ServicePlanTier("Yearly", 109.00, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 16.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("Individual", 1080.0, isDefault = true),
                ServicePlanTier("Yearly", 10800.0, BillingPeriod.YEARLY),
                ServicePlanTier("Family", 1680.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("Individual", 10.99, isDefault = true),
                ServicePlanTier("Family", 16.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("Individual", 10.99, isDefault = true),
                ServicePlanTier("Family", 16.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("Individual", 10.99, isDefault = true),
                ServicePlanTier("Family", 16.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("Individual", 12.99, isDefault = true),
                ServicePlanTier("Family", 19.99)
            ),
            "INR" to listOf(
                ServicePlanTier("Individual", 119.0, isDefault = true),
                ServicePlanTier("Family", 179.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("Individual", 8900.0, isDefault = true),
                ServicePlanTier("Family", 13500.0)
            )
        ),
        "Apple TV+" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 9.99, isDefault = true),
                ServicePlanTier("Yearly", 99.00, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 900.0, isDefault = true),
                ServicePlanTier("Yearly", 9000.0, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 9.99, isDefault = true),
                ServicePlanTier("Yearly", 99.00, BillingPeriod.YEARLY)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 8.99, isDefault = true),
                ServicePlanTier("Yearly", 89.00, BillingPeriod.YEARLY)
            ),
            "CAD" to listOf(
                ServicePlanTier("Monthly", 12.99, isDefault = true),
                ServicePlanTier("Yearly", 129.00, BillingPeriod.YEARLY)
            ),
            "AUD" to listOf(
                ServicePlanTier("Monthly", 12.99, isDefault = true),
                ServicePlanTier("Yearly", 129.00, BillingPeriod.YEARLY)
            ),
            "INR" to listOf(
                ServicePlanTier("Monthly", 99.0, isDefault = true)
            ),
            "KRW" to listOf(
                ServicePlanTier("Monthly", 6500.0, isDefault = true)
            )
        ),
        "Amazon Prime Video" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Video Only", 8.99, isDefault = true),
                ServicePlanTier("Prime Monthly", 14.99),
                ServicePlanTier("Prime Yearly", 139.00, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Prime Monthly", 600.0, isDefault = true),
                ServicePlanTier("Prime Yearly", 5900.0, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Prime Monthly", 8.99, isDefault = true),
                ServicePlanTier("Prime Yearly", 89.90, BillingPeriod.YEARLY)
            ),
            "GBP" to listOf(
                ServicePlanTier("Video Only", 5.99),
                ServicePlanTier("Prime Monthly", 8.99, isDefault = true),
                ServicePlanTier("Prime Yearly", 95.00, BillingPeriod.YEARLY)
            ),
            "CAD" to listOf(
                ServicePlanTier("Prime Monthly", 9.99, isDefault = true),
                ServicePlanTier("Prime Yearly", 99.00, BillingPeriod.YEARLY)
            ),
            "AUD" to listOf(
                ServicePlanTier("Prime Monthly", 9.99, isDefault = true),
                ServicePlanTier("Prime Yearly", 79.00, BillingPeriod.YEARLY)
            ),
            "INR" to listOf(
                ServicePlanTier("Monthly", 299.0),
                ServicePlanTier("Yearly", 1499.0, BillingPeriod.YEARLY, isDefault = true)
            )
        ),
        "ChatGPT Plus" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Plus", 20.00, isDefault = true)
            ),
            "JPY" to listOf(
                ServicePlanTier("Plus", 3000.0, isDefault = true)
            ),
            "EUR" to listOf(
                ServicePlanTier("Plus", 23.00, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Plus", 20.00, isDefault = true)
            ),
            "CAD" to listOf(
                ServicePlanTier("Plus", 29.00, isDefault = true)
            ),
            "AUD" to listOf(
                ServicePlanTier("Plus", 32.00, isDefault = true)
            ),
            "INR" to listOf(
                ServicePlanTier("Plus", 1999.0, isDefault = true)
            ),
            "KRW" to listOf(
                ServicePlanTier("Plus", 29000.0, isDefault = true)
            )
        ),
        "Claude Pro" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 20.00, isDefault = true),
                ServicePlanTier("Yearly", 216.00, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 3000.0, isDefault = true),
                ServicePlanTier("Yearly", 32400.0, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 21.60, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 18.00, isDefault = true)
            ),
            "CAD" to listOf(
                ServicePlanTier("Monthly", 28.00, isDefault = true)
            ),
            "AUD" to listOf(
                ServicePlanTier("Monthly", 31.00, isDefault = true)
            ),
            "INR" to listOf(
                ServicePlanTier("Monthly", 1999.0, isDefault = true)
            ),
            "KRW" to listOf(
                ServicePlanTier("Monthly", 29000.0, isDefault = true)
            )
        ),
        "Google AI Plus" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 9.99, isDefault = true)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 1200.0, isDefault = true)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 9.99, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 8.99, isDefault = true)
            )
        ),
        "Google AI Pro" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 19.99, isDefault = true),
                ServicePlanTier("Yearly", 199.99, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 2900.0, isDefault = true)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 21.99, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 18.99, isDefault = true)
            ),
            "CAD" to listOf(
                ServicePlanTier("Monthly", 26.99, isDefault = true)
            ),
            "AUD" to listOf(
                ServicePlanTier("Monthly", 32.99, isDefault = true)
            ),
            "INR" to listOf(
                ServicePlanTier("Monthly", 1950.0, isDefault = true)
            ),
            "KRW" to listOf(
                ServicePlanTier("Monthly", 29000.0, isDefault = true)
            )
        ),
        "Google AI Ultra" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 249.99, isDefault = true)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 36400.0, isDefault = true)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 274.99, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 234.99, isDefault = true)
            )
        ),
        "Notion Plus" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Monthly", 12.00, isDefault = true),
                ServicePlanTier("Yearly", 120.00, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Monthly", 1650.0, isDefault = true),
                ServicePlanTier("Yearly", 16500.0, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Monthly", 11.50, isDefault = true),
                ServicePlanTier("Yearly", 114.00, BillingPeriod.YEARLY)
            ),
            "GBP" to listOf(
                ServicePlanTier("Monthly", 10.00, isDefault = true),
                ServicePlanTier("Yearly", 96.00, BillingPeriod.YEARLY)
            ),
            "KRW" to listOf(
                ServicePlanTier("Monthly", 15000.0, isDefault = true),
                ServicePlanTier("Yearly", 150000.0, BillingPeriod.YEARLY)
            )
        ),
        "Xbox Game Pass Ultimate" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Standard", 14.99),
                ServicePlanTier("Ultimate", 19.99, isDefault = true)
            ),
            "JPY" to listOf(
                ServicePlanTier("Standard", 1100.0),
                ServicePlanTier("Ultimate", 1450.0, isDefault = true)
            ),
            "EUR" to listOf(
                ServicePlanTier("Standard", 12.99),
                ServicePlanTier("Ultimate", 17.99, isDefault = true)
            ),
            "GBP" to listOf(
                ServicePlanTier("Standard", 10.99),
                ServicePlanTier("Ultimate", 14.99, isDefault = true)
            ),
            "CAD" to listOf(
                ServicePlanTier("Standard", 16.99),
                ServicePlanTier("Ultimate", 22.99, isDefault = true)
            ),
            "AUD" to listOf(
                ServicePlanTier("Standard", 17.95),
                ServicePlanTier("Ultimate", 22.95, isDefault = true)
            ),
            "INR" to listOf(
                ServicePlanTier("Ultimate", 829.0, isDefault = true)
            ),
            "KRW" to listOf(
                ServicePlanTier("Ultimate", 16000.0, isDefault = true)
            )
        ),
        "PlayStation Plus" to mapOf(
            "USD" to listOf(
                ServicePlanTier("Essential", 9.99, isDefault = true),
                ServicePlanTier("Extra", 14.99),
                ServicePlanTier("Essential Yearly", 79.99, BillingPeriod.YEARLY),
                ServicePlanTier("Extra Yearly", 134.99, BillingPeriod.YEARLY)
            ),
            "JPY" to listOf(
                ServicePlanTier("Essential", 850.0, isDefault = true),
                ServicePlanTier("Extra", 1300.0),
                ServicePlanTier("Essential Yearly", 6800.0, BillingPeriod.YEARLY),
                ServicePlanTier("Extra Yearly", 11700.0, BillingPeriod.YEARLY)
            ),
            "EUR" to listOf(
                ServicePlanTier("Essential", 8.99, isDefault = true),
                ServicePlanTier("Extra", 13.99),
                ServicePlanTier("Essential Yearly", 71.99, BillingPeriod.YEARLY)
            ),
            "GBP" to listOf(
                ServicePlanTier("Essential", 6.99, isDefault = true),
                ServicePlanTier("Extra", 10.99),
                ServicePlanTier("Essential Yearly", 59.99, BillingPeriod.YEARLY)
            ),
            "CAD" to listOf(
                ServicePlanTier("Essential", 11.99, isDefault = true),
                ServicePlanTier("Extra", 17.99),
                ServicePlanTier("Essential Yearly", 94.99, BillingPeriod.YEARLY)
            ),
            "AUD" to listOf(
                ServicePlanTier("Essential", 11.95, isDefault = true),
                ServicePlanTier("Extra", 18.95),
                ServicePlanTier("Essential Yearly", 95.95, BillingPeriod.YEARLY)
            ),
            "INR" to listOf(
                ServicePlanTier("Essential", 499.0, isDefault = true),
                ServicePlanTier("Extra", 749.0),
                ServicePlanTier("Essential Yearly", 3949.0, BillingPeriod.YEARLY)
            ),
            "KRW" to listOf(
                ServicePlanTier("Essential", 7500.0, isDefault = true),
                ServicePlanTier("Extra", 11300.0),
                ServicePlanTier("Essential Yearly", 60000.0, BillingPeriod.YEARLY)
            )
        ),
        "iCloud+" to mapOf(
            "USD" to listOf(
                ServicePlanTier("50GB", 0.99),
                ServicePlanTier("200GB", 2.99, isDefault = true),
                ServicePlanTier("2TB", 9.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("50GB", 150.0),
                ServicePlanTier("200GB", 450.0, isDefault = true),
                ServicePlanTier("2TB", 1500.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("50GB", 0.99),
                ServicePlanTier("200GB", 2.99, isDefault = true),
                ServicePlanTier("2TB", 9.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("50GB", 0.99),
                ServicePlanTier("200GB", 2.99, isDefault = true),
                ServicePlanTier("2TB", 8.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("50GB", 1.29),
                ServicePlanTier("200GB", 3.99, isDefault = true),
                ServicePlanTier("2TB", 12.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("50GB", 1.49),
                ServicePlanTier("200GB", 4.49, isDefault = true),
                ServicePlanTier("2TB", 14.99)
            ),
            "INR" to listOf(
                ServicePlanTier("50GB", 75.0),
                ServicePlanTier("200GB", 219.0, isDefault = true),
                ServicePlanTier("2TB", 749.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("50GB", 1100.0),
                ServicePlanTier("200GB", 3300.0, isDefault = true),
                ServicePlanTier("2TB", 11100.0)
            )
        ),
        "Google One" to mapOf(
            "USD" to listOf(
                ServicePlanTier("100GB", 1.99, isDefault = true),
                ServicePlanTier("100GB Yearly", 19.99, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 9.99)
            ),
            "JPY" to listOf(
                ServicePlanTier("100GB", 290.0, isDefault = true),
                ServicePlanTier("100GB Yearly", 2900.0, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 1300.0)
            ),
            "EUR" to listOf(
                ServicePlanTier("100GB", 1.99, isDefault = true),
                ServicePlanTier("100GB Yearly", 19.99, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 9.99)
            ),
            "GBP" to listOf(
                ServicePlanTier("100GB", 1.59, isDefault = true),
                ServicePlanTier("100GB Yearly", 15.99, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 7.99)
            ),
            "CAD" to listOf(
                ServicePlanTier("100GB", 2.79, isDefault = true),
                ServicePlanTier("100GB Yearly", 27.99, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 13.99)
            ),
            "AUD" to listOf(
                ServicePlanTier("100GB", 2.49, isDefault = true),
                ServicePlanTier("100GB Yearly", 24.99, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 12.49)
            ),
            "INR" to listOf(
                ServicePlanTier("100GB", 130.0, isDefault = true),
                ServicePlanTier("100GB Yearly", 1300.0, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 650.0)
            ),
            "KRW" to listOf(
                ServicePlanTier("100GB", 2400.0, isDefault = true),
                ServicePlanTier("100GB Yearly", 24000.0, BillingPeriod.YEARLY),
                ServicePlanTier("2TB", 11900.0)
            )
        )
    )

    fun tiersFor(serviceName: String, currencyCode: String): List<ServicePlanTier> {
        val normalizedCurrency = currencyCode.trim().uppercase(Locale.US)
        return tiersByServiceAndCurrency[serviceName]?.get(normalizedCurrency).orEmpty()
    }

    fun defaultTierFor(serviceName: String, currencyCode: String): ServicePlanTier? {
        val tiers = tiersFor(serviceName, currencyCode)
        return tiers.firstOrNull { it.isDefault } ?: tiers.firstOrNull()
    }

    fun formatEditablePrice(price: Double, currencyCode: String): String {
        val isWholeNumber = price == price.toLong().toDouble()
        return if (MoneyFormatter.isZeroDecimal(currencyCode) || isWholeNumber) {
            price.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", price)
        }
    }
}
