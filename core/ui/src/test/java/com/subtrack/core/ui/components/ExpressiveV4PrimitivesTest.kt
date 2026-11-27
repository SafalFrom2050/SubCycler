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
}
