package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetPortfolioSummaryUseCaseTest {

    private val useCase = GetPortfolioSummaryUseCase()

    @Test
    fun testPortfolioSummaryAggregatesCorrectly() {
        val today = LocalDate.of(2026, 8, 18)
        val s1 = Subscription(name = "Netflix", cost = 15.0, startDate = today, strategy = RotationStrategy.ROTATION_45_DAYS)
        val s2 = Subscription(name = "Disney+", cost = 15.0, startDate = today.minusDays(35), strategy = RotationStrategy.ROTATION_45_DAYS)
        val s3 = Subscription(name = "Spotify", cost = 10.0, startDate = today, strategy = RotationStrategy.CONTINUOUS)

        val statuses = listOf(s1, s2, s3).map { RotationEngine.calculateCycleStatus(it, today) }
        val summary = useCase(statuses)

        assertEquals(3, summary.totalSubscriptions)
        assertEquals(2, summary.activeCount)
        assertEquals(1, summary.blackoutCount)
        assertTrue(summary.totalAnnualSavings > 0.0)
        assertEquals(40.0, summary.totalStandardAnnualCost / 12.0, 0.01)
    }
}
