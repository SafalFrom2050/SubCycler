package com.subtrack.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressiveWidgetArtTest {

    @Test
    fun classifySizeTierMapsCompactWideAndTallBreakpointsAccurately() {
        assertEquals(LivingWidgetSizeTier.COMPACT_2X2, ExpressiveWidgetArt.classifySizeTier(130, 110))
        assertEquals(LivingWidgetSizeTier.COMPACT_2X2, ExpressiveWidgetArt.classifySizeTier(210, 170))
        assertEquals(LivingWidgetSizeTier.WIDE_4X2, ExpressiveWidgetArt.classifySizeTier(250, 120))
        assertEquals(LivingWidgetSizeTier.WIDE_4X2, ExpressiveWidgetArt.classifySizeTier(320, 175))
        assertEquals(LivingWidgetSizeTier.TALL_4X3, ExpressiveWidgetArt.classifySizeTier(250, 195))
        assertEquals(LivingWidgetSizeTier.TALL_4X3, ExpressiveWidgetArt.classifySizeTier(340, 260))
    }
}
