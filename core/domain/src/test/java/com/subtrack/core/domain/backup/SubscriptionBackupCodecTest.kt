package com.subtrack.core.domain.backup

import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubscriptionBackupCodecTest {

    @Test
    fun jsonRoundTripPreservesAllFieldsIncludingEarnedSavingsAndEscapedNotes() {
        val original = listOf(
            Subscription(
                name = "Netflix",
                cost = 17.99,
                currency = "USD",
                billingPeriod = BillingPeriod.MONTHLY,
                category = SubscriptionCategory.STREAMING,
                startDate = LocalDate.of(2026, 11, 21),
                strategy = RotationStrategy.ROTATION_45_DAYS,
                customActiveDays = 30,
                customBlackoutDays = 15,
                notes = "Watch \"Severance\"\nSeason 3",
                isAutoRenewCancelled = true,
                isBillingDateConfirmed = true,
                cancelledCycleIndex = 2,
                completedBreaksCount = 3,
                earnedSavingsTotal = 25.88,
                bonusActiveDays = 30
            ),
            Subscription(
                name = "Amazon Prime Video",
                cost = 5900.0,
                currency = "JPY",
                billingPeriod = BillingPeriod.YEARLY,
                category = SubscriptionCategory.STREAMING,
                startDate = LocalDate.of(2026, 1, 10),
                strategy = RotationStrategy.CONTINUOUS,
                notes = "",
                isBillingDateConfirmed = false
            )
        )

        val json = SubscriptionBackupCodec.toJson(original)
        val decoded = SubscriptionBackupCodec.fromJson(json).getOrThrow()

        assertEquals(2, decoded.size)
        assertEquals(original[0].copy(id = 0L), decoded[0])
        assertEquals(original[1].copy(id = 0L), decoded[1])
    }

    @Test
    fun csvExportProducesWellFormedHeaderAndRows() {
        val subs = listOf(
            Subscription(
                name = "Netflix",
                cost = 17.99,
                currency = "USD",
                startDate = LocalDate.of(2026, 11, 21),
                completedBreaksCount = 2,
                earnedSavingsTotal = 17.25,
                notes = "Show A, Show B"
            )
        )

        val csv = SubscriptionBackupCodec.toCsv(subs)
        val lines = csv.trim().lines()

        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("Name,Cost,Currency"))
        assertTrue(lines[1].contains("\"Netflix\",17.99,\"USD\""))
        assertTrue(lines[1].contains("\"Show A, Show B\""))
    }

    @Test
    fun malformedJsonReturnsFailure() {
        val result = SubscriptionBackupCodec.fromJson("not valid json")
        assertTrue(result.isFailure)
    }

    @Test
    fun jsonRoundTripPreservesIncompleteDraftSubscription() {
        val draft = Subscription(
            name = "Draft Service",
            cost = 0.0,
            currency = "JPY",
            startDate = LocalDate.of(2026, 11, 23),
            isBillingDateConfirmed = false,
            isStrategyConfirmed = false
        )
        val json = SubscriptionBackupCodec.toJson(listOf(draft))
        val decoded = SubscriptionBackupCodec.fromJson(json).getOrThrow().single()
        assertEquals(draft, decoded)
        assertTrue(decoded.isIncomplete)
    }
}
