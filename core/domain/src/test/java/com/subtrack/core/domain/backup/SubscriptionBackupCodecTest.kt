package com.subtrack.core.domain.backup

import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubscriptionBackupCodecTest {

    private val richSample = listOf(
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

    @Test
    fun transferLinkRoundTripPreservesAllFieldsWhenPersonalHistoryIncluded() {
        val link = SubscriptionBackupCodec.toTransferLink(richSample, includePersonalHistory = true)

        assertTrue(link.startsWith(SubscriptionBackupCodec.TRANSFER_LINK_PREFIX))
        assertTrue(SubscriptionBackupCodec.containsTransferLink(link))

        val decoded = SubscriptionBackupCodec.decodeTransferPayload(link).getOrThrow()
        assertEquals(2, decoded.size)
        assertEquals(richSample[0].copy(id = 0L), decoded[0])
        assertEquals(richSample[1].copy(id = 0L), decoded[1])
    }

    @Test
    fun transferLinkStripsPersonalNotesAndSavingsHistoryWhenDisabled() {
        val link = SubscriptionBackupCodec.toTransferLink(richSample, includePersonalHistory = false)
        val decoded = SubscriptionBackupCodec.decodeTransferPayload(link).getOrThrow()

        val first = decoded[0]
        assertEquals("Netflix", first.name)
        assertEquals(17.99, first.cost, 0.001)
        assertEquals("", first.notes)
        assertFalse(first.isAutoRenewCancelled)
        assertNull(first.cancelledCycleIndex)
        assertEquals(0, first.completedBreaksCount)
        assertEquals(0.0, first.earnedSavingsTotal, 0.001)
        assertEquals(0, first.bonusActiveDays)
    }

    @Test
    fun shareMessageEmbedsLinkCleanlyWithoutTechnicalJargonAndDecodesFromChatText() {
        val message = SubscriptionBackupCodec.toShareMessage(richSample, includePersonalHistory = false)

        assertFalse(message.contains("json", ignoreCase = true))
        assertFalse(message.contains("csv", ignoreCase = true))
        assertTrue(message.contains("Netflix"))
        assertTrue(message.contains("Amazon Prime Video"))
        assertTrue(SubscriptionBackupCodec.containsTransferLink(message))

        val wrappedInChat = "Hey! Here's my streaming rotation:\n\n$message\n\nLet me know what you think!"
        val decoded = SubscriptionBackupCodec.decodeTransferPayload(wrappedInChat).getOrThrow()
        assertEquals(2, decoded.size)
        assertEquals("Netflix", decoded[0].name)
    }

    @Test
    fun backupFilePayloadRoundTripUsesUnifiedFormat() {
        val filePayload = SubscriptionBackupCodec.toBackupFilePayload(richSample, includePersonalHistory = true)

        assertTrue(filePayload.startsWith("# SubCycler Backup"))
        assertTrue(SubscriptionBackupCodec.containsTransferLink(filePayload))

        val decoded = SubscriptionBackupCodec.decodeTransferPayload(filePayload).getOrThrow()
        assertEquals(richSample[0].copy(id = 0L), decoded[0])
        assertEquals(richSample[1].copy(id = 0L), decoded[1])
    }

    @Test
    fun legacyBackupEnvelopeRemainsDecodableForBackwardCompatibility() {
        val legacyJson = SubscriptionBackupCodec.toJson(richSample)
        val decoded = SubscriptionBackupCodec.decodeTransferPayload(legacyJson).getOrThrow()

        assertEquals(2, decoded.size)
        assertEquals(richSample[0].copy(id = 0L), decoded[0])
        assertEquals(richSample[1].copy(id = 0L), decoded[1])
    }

    @Test
    fun malformedTransferInputReturnsFailure() {
        assertTrue(SubscriptionBackupCodec.decodeTransferPayload("not a subcycler link").isFailure)
        assertTrue(SubscriptionBackupCodec.decodeTransferPayload("subcycler://import?d=!!!invalid!!!").isFailure)
        assertTrue(SubscriptionBackupCodec.decodeTransferPayload("").isFailure)
    }

    @Test
    fun transferLinkIsCompactAndShortForTypicalThreeServiceStack() {
        val threeSubs = listOf(
            Subscription(name = "Netflix", cost = 17.99, currency = "USD", startDate = LocalDate.of(2026, 11, 27)),
            Subscription(name = "Disney+", cost = 15.99, currency = "USD", startDate = LocalDate.of(2026, 11, 27)),
            Subscription(
                name = "Spotify",
                cost = 11.99,
                currency = "USD",
                category = SubscriptionCategory.MUSIC,
                strategy = RotationStrategy.CONTINUOUS,
                startDate = LocalDate.of(2026, 11, 27)
            )
        )
        val link = SubscriptionBackupCodec.toTransferLink(threeSubs, includePersonalHistory = false)
        assertTrue(link.startsWith(SubscriptionBackupCodec.SHORT_LINK_PREFIX))
        assertFalse(link.contains("H4sI"))
        assertTrue("Expected compact link under 150 chars, got ${link.length}: $link", link.length < 150)

        val decoded = SubscriptionBackupCodec.decodeTransferPayload(link).getOrThrow()
        assertEquals(threeSubs, decoded)
    }

    @Test
    fun humanShareCaptionContainsZeroEncodedTokensOrJargon() {
        val caption = SubscriptionBackupCodec.toHumanShareCaption(richSample, includePersonalHistory = false)
        assertFalse(caption.contains("subcycler://"))
        assertFalse(caption.contains("json", ignoreCase = true))
        assertFalse(caption.contains("csv", ignoreCase = true))
        assertTrue(caption.contains("Netflix"))
        assertTrue(caption.contains("Amazon Prime Video"))
    }

    @Test
    fun transferLinkPreservesIncompleteDraftSubscription() {
        val draft = Subscription(
            name = "Draft Service",
            cost = 0.0,
            currency = "JPY",
            startDate = LocalDate.of(2026, 11, 23),
            isBillingDateConfirmed = false,
            isStrategyConfirmed = false
        )
        val link = SubscriptionBackupCodec.toTransferLink(listOf(draft))
        val decoded = SubscriptionBackupCodec.decodeTransferPayload(link).getOrThrow().single()
        assertEquals(draft, decoded)
        assertTrue(decoded.isIncomplete)
    }
}
