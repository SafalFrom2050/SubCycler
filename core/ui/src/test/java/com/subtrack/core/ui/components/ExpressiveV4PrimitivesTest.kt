package com.subtrack.core.ui.components

import androidx.compose.ui.unit.dp
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.theme.ExpressiveMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpressiveV4PrimitivesTest {

    @Test
    fun clampEffectProgressBoundsSpringOvershoot() {
        assertEquals(0f, ExpressiveMotion.clampEffectProgress(-0.18f), 0.0001f)
        assertEquals(0.45f, ExpressiveMotion.clampEffectProgress(0.45f), 0.0001f)
        assertEquals(1f, ExpressiveMotion.clampEffectProgress(1.14f), 0.0001f)
    }

    @Test
    fun overdragResistanceDampsProgressivelyForPositiveOvershoot() {
        assertEquals(1f, ExpressiveMotion.applyOverdragResistance(0f), 0.0001f)
        assertEquals(1f, ExpressiveMotion.applyOverdragResistance(-0.2f), 0.0001f)
        val slight = ExpressiveMotion.applyOverdragResistance(0.05f, resistance = 100f)
        val heavy = ExpressiveMotion.applyOverdragResistance(0.25f, resistance = 100f)
        assertTrue(slight < 1f)
        assertTrue(heavy < slight)
    }

    @Test
    fun comfortBandScrollDeltaLeavesCenteredChipsAloneAndNudgesEdgeChips() {
        // Viewport 0..1000 -> comfort band is 250..750
        assertNull(
            ExpressiveMotion.computeComfortBandScrollDelta(
                itemStartPx = 400,
                itemSizePx = 120,
                viewportStartPx = 0,
                viewportEndPx = 1000
            )
        )
        // Edge-clipped near leading edge (center = 80, comfortMin = 250 -> delta = -170)
        assertEquals(
            -170,
            ExpressiveMotion.computeComfortBandScrollDelta(
                itemStartPx = 20,
                itemSizePx = 120,
                viewportStartPx = 0,
                viewportEndPx = 1000
            )
        )
        // Edge-clipped near trailing edge (center = 900, comfortMax = 750 -> delta = +150)
        assertEquals(
            150,
            ExpressiveMotion.computeComfortBandScrollDelta(
                itemStartPx = 840,
                itemSizePx = 120,
                viewportStartPx = 0,
                viewportEndPx = 1000
            )
        )
    }

    @Test
    fun sectionSlabRadiiAssignOuterCapsAndInnerSeamsAndBloomOnPress() {
        val single = computeSectionSlabRadii(index = 0, count = 1)
        assertEquals(28.dp, single.topRadius)
        assertEquals(28.dp, single.bottomRadius)

        val first = computeSectionSlabRadii(index = 0, count = 3)
        assertEquals(28.dp, first.topRadius)
        assertEquals(4.dp, first.bottomRadius)

        val middle = computeSectionSlabRadii(index = 1, count = 3)
        assertEquals(4.dp, middle.topRadius)
        assertEquals(4.dp, middle.bottomRadius)

        val middlePressed = computeSectionSlabRadii(index = 1, count = 3, isPressed = true)
        assertEquals(14.dp, middlePressed.topRadius)
        assertEquals(14.dp, middlePressed.bottomRadius)

        val last = computeSectionSlabRadii(index = 2, count = 3)
        assertEquals(4.dp, last.topRadius)
        assertEquals(28.dp, last.bottomRadius)
    }

    @Test
    fun badgeLifecycleAndAnticipatoryPressResolveDistinctPolygons() {
        assertEquals(
            ExpressiveMaterialPolygons.Square,
            resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.COOKIE, ServiceBadgeLifecycleState.INCOMPLETE, false, false)
        )
        assertEquals(
            ExpressiveMaterialPolygons.SoftBurst,
            resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.COOKIE, ServiceBadgeLifecycleState.URGENT_DUE, false, false)
        )
        assertEquals(
            ExpressiveMaterialPolygons.Circle,
            resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.COOKIE, ServiceBadgeLifecycleState.COASTING_CANCELLED, false, false)
        )
        assertEquals(
            ExpressiveMaterialPolygons.Bun,
            resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.COOKIE, ServiceBadgeLifecycleState.BLACKOUT_REST, false, false)
        )
        assertEquals(
            ExpressiveMaterialPolygons.Cookie12,
            resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.COOKIE, ServiceBadgeLifecycleState.UNBANKED_READY, false, false)
        )
        // Anticipatory press preview morphs away from resting geometry
        val resting = resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.CIRCLE, ServiceBadgeLifecycleState.ACTIVE, false, false)
        val pressed = resolveBadgeTargetPolygon(ExpressiveBadgeShapeKind.CIRCLE, ServiceBadgeLifecycleState.ACTIVE, false, true)
        assertNotEquals(resting, pressed)
        assertEquals(10, ExpressiveMaterialPolygons.mosaicPalette.size)
    }

    @Test
    fun snackDepthVisualsStepPeekScaleAndShadeProgressively() {
        val front = computeSnackDepthVisuals(0f)
        assertEquals(0f, front.peekDp, 0.001f)
        assertEquals(1f, front.scale, 0.001f)
        assertEquals(0f, front.shadeAlpha, 0.001f)

        val back = computeSnackDepthVisuals(2f)
        assertEquals(24f, back.peekDp, 0.001f)
        assertEquals(0.90f, back.scale, 0.001f)
        assertEquals(0.30f, back.shadeAlpha, 0.001f)
    }

    @Test
    fun cycleScrubPreviewTransitionsCleanlyFromActiveToBreakZone() {
        val startDate = LocalDate.of(2026, 11, 1)
        val sub = Subscription(
            id = 1L,
            name = "Netflix",
            cost = 15.0,
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )
        val status = RotationEngine.calculateCycleStatus(sub, startDate.plusDays(5))

        val earlyActive = computeCycleScrubPreview(status, 0.2f)
        assertFalse(earlyActive.isInBreakZone)
        assertTrue(earlyActive.countdownLabel.endsWith("d left"))

        val midBreak = computeCycleScrubPreview(status, 0.85f)
        assertTrue(midBreak.isInBreakZone)
        assertTrue(midBreak.detailCaption.contains("break", ignoreCase = true))

        val endOfBreak = computeCycleScrubPreview(status, 1.0f)
        assertTrue(endOfBreak.isInBreakZone)
        assertTrue(endOfBreak.countdownLabel.startsWith("+"))
    }

    @Test
    fun checkInDeckPrioritizesUnbankedBreaksAheadOfIncompleteDrafts() {
        val today = LocalDate.of(2026, 11, 20)
        val incompleteSub = Subscription(
            id = 1L,
            name = "Spotify",
            cost = 0.0,
            currency = "USD",
            startDate = today,
            isBillingDateConfirmed = false,
            isStrategyConfirmed = false
        )
        val completedBreakSub = Subscription(
            id = 2L,
            name = "Netflix",
            cost = 15.0,
            currency = "USD",
            startDate = today.minusDays(46),
            strategy = RotationStrategy.ROTATION_45_DAYS,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )
        val statuses = listOf(
            RotationEngine.calculateCycleStatus(incompleteSub, today),
            RotationEngine.calculateCycleStatus(completedBreakSub, today)
        )
        val items = buildCycleCheckInItems(statuses)
        assertTrue(items.any { it is CheckInDeckItem.FinishIncompleteSetup })
        if (items.any { it is CheckInDeckItem.BankCompletedBreak }) {
            assertTrue(items.first() is CheckInDeckItem.BankCompletedBreak)
        }
    }

    @Test
    fun offscreenEdgeFadeDisabledOnLegacyHwuiApisAndEnabledFromApi29() {
        assertEquals(false, shouldUseOffscreenEdgeFade(26))
        assertEquals(false, shouldUseOffscreenEdgeFade(27))
        assertEquals(false, shouldUseOffscreenEdgeFade(28))
        assertEquals(true, shouldUseOffscreenEdgeFade(29))
        assertEquals(true, shouldUseOffscreenEdgeFade(35))
    }

    @Test
    fun importPreviewFlagsExistingAndDuplicateServicesCaseInsensitively() {
        val today = LocalDate.of(2026, 11, 27)
        val existing = listOf(
            Subscription(id = 1L, name = "Netflix", cost = 17.99, currency = "USD", startDate = today)
        )
        val incoming = listOf(
            Subscription(name = " netflix ", cost = 17.99, currency = "USD", startDate = today),
            Subscription(name = "Spotify", cost = 11.99, currency = "USD", startDate = today),
            Subscription(name = "SPOTIFY", cost = 11.99, currency = "USD", startDate = today)
        )
        val preview = buildImportPreviewItems(incoming = incoming, existing = existing)
        assertEquals(3, preview.size)
        assertEquals(true, preview[0].alreadyInStack)
        assertEquals(false, preview[1].alreadyInStack)
        assertEquals(true, preview[2].alreadyInStack)
    }

    @Test
    fun cycleVesselMetricsProgressMonotonicallyAcrossActiveAndBreakZones() {
        val startDate = LocalDate.of(2026, 11, 1)
        val rotating45 = Subscription(
            id = 10L,
            name = "Netflix",
            cost = 890.0,
            currency = "JPY",
            billingPeriod = BillingPeriod.MONTHLY,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )

        val day1 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(rotating45, startDate))
        val day15 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(rotating45, startDate.plusDays(14)))
        val day30 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(rotating45, startDate.plusDays(29)))
        val breakDay1 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(rotating45, startDate.plusDays(30)))
        val breakLast = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(rotating45, startDate.plusDays(44)))

        assertTrue(day1.hasBreakZone)
        assertTrue(day1.isActivePhase)
        assertEquals(30f / 45f, day1.activeZoneFraction, 0.001f)
        assertTrue(day1.overallCycleProgress < day15.overallCycleProgress)
        assertTrue(day15.overallCycleProgress < day30.overallCycleProgress)
        assertEquals(30f / 45f, day30.overallCycleProgress, 0.001f)

        // Break phase continues cleanly rightward past activeZoneFraction instead of snapping to 0
        assertFalse(breakDay1.isActivePhase)
        assertTrue(breakDay1.overallCycleProgress > day30.overallCycleProgress)
        assertTrue(breakDay1.overallCycleProgress < breakLast.overallCycleProgress)
        assertEquals(1f, breakLast.overallCycleProgress, 0.001f)
    }

    @Test
    fun cycleVesselMetricsHandleShortBreaksBonusDaysContinuousAndIncomplete() {
        val startDate = LocalDate.of(2026, 11, 1)
        val shortBreak37 = Subscription(
            id = 11L,
            name = "Disney+",
            cost = 1140.0,
            currency = "JPY",
            billingPeriod = BillingPeriod.MONTHLY,
            startDate = startDate,
            strategy = RotationStrategy.CUSTOM,
            customActiveDays = 30,
            customBlackoutDays = 7,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )
        val shortBreakMetrics = computeCycleVesselMetrics(
            RotationEngine.calculateCycleStatus(shortBreak37, startDate.plusDays(4))
        )
        assertTrue(shortBreakMetrics.hasBreakZone)
        // 30 / 37 = 0.8108 -> clamped to 0.80 so break zone stays clearly visible
        assertEquals(0.80f, shortBreakMetrics.activeZoneFraction, 0.001f)

        val bonusCycleSub = Subscription(
            id = 12L,
            name = "Crunchyroll",
            cost = 9.99,
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            startDate = startDate,
            strategy = RotationStrategy.ROTATION_45_DAYS,
            bonusActiveDays = 30,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )
        val cycle1 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(bonusCycleSub, startDate.plusDays(10)))
        val cycle2 = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(bonusCycleSub, startDate.plusDays(80)))
        assertEquals(0.80f, cycle1.activeZoneFraction, 0.001f)
        assertEquals(30f / 45f, cycle2.activeZoneFraction, 0.001f)

        val alwaysOn = Subscription(
            id = 13L,
            name = "Spotify",
            cost = 11.99,
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            startDate = startDate,
            strategy = RotationStrategy.CONTINUOUS,
            isBillingDateConfirmed = true,
            isStrategyConfirmed = true
        )
        val alwaysMetrics = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(alwaysOn, startDate.plusDays(14)))
        assertFalse(alwaysMetrics.hasBreakZone)
        assertEquals(1f, alwaysMetrics.activeZoneFraction, 0.001f)
        assertEquals(0.5f, alwaysMetrics.overallCycleProgress, 0.001f)

        val incomplete = Subscription(
            id = 14L,
            name = "Draft",
            cost = 0.0,
            currency = "USD",
            startDate = startDate,
            isBillingDateConfirmed = false,
            isStrategyConfirmed = false
        )
        val incompleteMetrics = computeCycleVesselMetrics(RotationEngine.calculateCycleStatus(incomplete, startDate))
        assertTrue(incompleteMetrics.isIncomplete)
        assertFalse(incompleteMetrics.hasBreakZone)
        assertEquals(incomplete.setupCompletionFraction, incompleteMetrics.overallCycleProgress, 0.001f)
    }
}
