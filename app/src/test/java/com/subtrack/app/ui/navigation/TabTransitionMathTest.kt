package com.subtrack.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTransitionMathTest {

    @Test
    fun frameZeroStartsWithOutgoingFullyVisibleAndIncomingHidden() {
        assertEquals(1f, outgoingTabAlpha(0f), 0.0001f)
        assertEquals(0f, incomingTabAlpha(0f), 0.0001f)
        assertEquals(0f, incomingTabSpatial(0f), 0.0001f)
    }

    @Test
    fun outgoingClearsBeforeMidpointAndIncomingSettlesCleanlyToOne() {
        assertEquals(0f, outgoingTabAlpha(0.35f), 0.0001f)
        assertEquals(0f, outgoingTabAlpha(1f), 0.0001f)

        assertTrue(incomingTabAlpha(0.5f) in 0.4f..0.9f)
        assertEquals(1f, incomingTabAlpha(1f), 0.0001f)
        assertEquals(1f, incomingTabSpatial(1f), 0.0001f)
    }

    @Test
    fun curvesAreMonotonicAndClampedOutsideUnitInterval() {
        assertEquals(1f, outgoingTabAlpha(-0.5f), 0.0001f)
        assertEquals(0f, incomingTabAlpha(-0.5f), 0.0001f)
        assertEquals(1f, incomingTabAlpha(1.5f), 0.0001f)
        assertEquals(1f, incomingTabSpatial(1.5f), 0.0001f)

        var prevOut = 1f
        var prevIn = 0f
        var prevSpatial = 0f
        for (step in 1..20) {
            val p = step / 20f
            val out = outgoingTabAlpha(p)
            val incoming = incomingTabAlpha(p)
            val spatial = incomingTabSpatial(p)
            assertTrue("outgoing alpha should never increase at p=$p", out <= prevOut + 0.0001f)
            assertTrue("incoming alpha should never decrease at p=$p", incoming >= prevIn - 0.0001f)
            assertTrue("incoming spatial should never decrease at p=$p", spatial >= prevSpatial - 0.0001f)
            prevOut = out
            prevIn = incoming
            prevSpatial = spatial
        }
    }
}
