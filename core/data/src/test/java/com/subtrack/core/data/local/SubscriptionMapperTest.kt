package com.subtrack.core.data.local

import com.subtrack.core.data.local.entity.SubscriptionEntity
import com.subtrack.core.data.local.mapper.toDomain
import com.subtrack.core.data.local.mapper.toEntity
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubscriptionMapperTest {

    @Test
    fun testEntityToDomainMapping() {
        val entity = SubscriptionEntity(
            id = 10,
            name = "Netflix",
            cost = 15.49,
            currency = "USD",
            category = SubscriptionCategory.STREAMING,
            startDate = LocalDate.of(2026, 1, 1),
            strategy = RotationStrategy.ROTATION_45_DAYS,
            customActiveDays = 30,
            customBlackoutDays = 15,
            colorHex = "#E50914",
            iconName = "Tv",
            notes = "Test Note",
            isAutoRenewCancelled = true,
            pairedWithSubscriptionId = 20L
        )

        val domain = entity.toDomain()

        assertEquals(10L, domain.id)
        assertEquals("Netflix", domain.name)
        assertEquals(15.49, domain.cost, 0.001)
        assertEquals("USD", domain.currency)
        assertEquals(SubscriptionCategory.STREAMING, domain.category)
        assertEquals(LocalDate.of(2026, 1, 1), domain.startDate)
        assertEquals(RotationStrategy.ROTATION_45_DAYS, domain.strategy)
        assertEquals(30, domain.customActiveDays)
        assertEquals(15, domain.customBlackoutDays)
        assertEquals("#E50914", domain.colorHex)
        assertEquals("Tv", domain.iconName)
        assertEquals("Test Note", domain.notes)
        assertEquals(true, domain.isAutoRenewCancelled)
        assertEquals(20L, domain.pairedWithSubscriptionId)
    }

    @Test
    fun testDomainToEntityMapping() {
        val domain = Subscription(
            id = 5,
            name = "Spotify",
            cost = 11.99,
            currency = "EUR",
            category = SubscriptionCategory.MUSIC,
            startDate = LocalDate.of(2026, 3, 15),
            strategy = RotationStrategy.CONTINUOUS,
            customActiveDays = 30,
            customBlackoutDays = 0,
            colorHex = "#1DB954",
            iconName = "Music",
            notes = "Music Note",
            isAutoRenewCancelled = false,
            pairedWithSubscriptionId = null
        )

        val entity = domain.toEntity()

        assertEquals(5L, entity.id)
        assertEquals("Spotify", entity.name)
        assertEquals(11.99, entity.cost, 0.001)
        assertEquals("EUR", entity.currency)
        assertEquals(SubscriptionCategory.MUSIC, entity.category)
        assertEquals(LocalDate.of(2026, 3, 15), entity.startDate)
        assertEquals(RotationStrategy.CONTINUOUS, entity.strategy)
        assertEquals(30, entity.customActiveDays)
        assertEquals(0, entity.customBlackoutDays)
        assertEquals("#1DB954", entity.colorHex)
        assertEquals("Music", entity.iconName)
        assertEquals("Music Note", entity.notes)
        assertEquals(false, entity.isAutoRenewCancelled)
        assertEquals(null, entity.pairedWithSubscriptionId)
    }
}
