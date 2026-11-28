package com.subtrack.app.widget

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.subtrack.app.notification.ExpressiveNotificationArt
import com.subtrack.app.notification.NotificationBadgeShape
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExpressiveArtRenderTest {

    @Test
    fun rendersAllExpressiveWidgetsAndNotificationsCleanly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val today = LocalDate.now()
        val subs = listOf(
            Subscription(
                id = 1L,
                name = "Disney+",
                cost = 1140.0,
                currency = "JPY",
                startDate = today.minusDays(28),
                strategy = RotationStrategy.ROTATION_45_DAYS,
                customActiveDays = 30,
                customBlackoutDays = 7,
                colorHex = "#113CCF",
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true
            ),
            Subscription(
                id = 2L,
                name = "Netflix",
                cost = 890.0,
                currency = "JPY",
                startDate = today.minusDays(22),
                strategy = RotationStrategy.ROTATION_45_DAYS,
                colorHex = "#E50914",
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true
            ),
            Subscription(
                id = 3L,
                name = "Spotify",
                cost = 980.0,
                currency = "JPY",
                startDate = today.minusDays(10),
                strategy = RotationStrategy.CONTINUOUS,
                colorHex = "#1DB954",
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true
            ),
            Subscription(
                id = 4L,
                name = "YouTube Premium",
                cost = 1280.0,
                currency = "JPY",
                startDate = today.minusDays(14),
                strategy = RotationStrategy.CONTINUOUS,
                colorHex = "#FF0000",
                isBillingDateConfirmed = true,
                isStrategyConfirmed = true
            )
        )
        val statuses = subs
            .map { RotationEngine.calculateCycleStatus(it, today) }
            .sortedBy { it.daysRemainingInPhase }
        val summary = RotationEngine.calculatePortfolioSavings(statuses)

        val previewDir = File(context.cacheDir, "expressive_previews").apply { mkdirs() }

        val w2x2 = ExpressiveWidgetArt.renderLivingStackPulseBitmap(
            context = context,
            summary = summary,
            upcomingStatuses = statuses,
            activeSubscriptionsForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.BOTANICAL,
            useStackMood = true,
            isDark = true,
            tier = LivingWidgetSizeTier.COMPACT_2X2
        )
        assertEquals(480, w2x2.width)
        assertEquals(480, w2x2.height)
        savePng(w2x2, File(previewDir, "widget_living_2x2_dark.png"))
        w2x2.recycle()

        val w4x2 = ExpressiveWidgetArt.renderLivingStackPulseBitmap(
            context = context,
            summary = summary,
            upcomingStatuses = statuses,
            activeSubscriptionsForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.OCEAN,
            useStackMood = true,
            isDark = true,
            tier = LivingWidgetSizeTier.WIDE_4X2
        )
        assertEquals(860, w4x2.width)
        assertEquals(400, w4x2.height)
        savePng(w4x2, File(previewDir, "widget_living_4x2_ocean_dark.png"))
        w4x2.recycle()

        val w4x3 = ExpressiveWidgetArt.renderLivingStackPulseBitmap(
            context = context,
            summary = summary,
            upcomingStatuses = statuses,
            activeSubscriptionsForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.BOTANICAL,
            useStackMood = true,
            isDark = false,
            tier = LivingWidgetSizeTier.TALL_4X3
        )
        assertEquals(860, w4x3.width)
        assertEquals(680, w4x3.height)
        savePng(w4x3, File(previewDir, "widget_living_4x3_light.png"))
        w4x3.recycle()

        val cookie = ExpressiveWidgetArt.renderNextUpCookieBitmap(
            context = context,
            soonest = statuses.first(),
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isDark = true
        )
        assertEquals(480, cookie.width)
        assertEquals(480, cookie.height)
        // Corner pixel outside the scalloped cookie must remain transparent so it floats over wallpaper
        assertEquals(0, Color.alpha(cookie.getPixel(2, 2)))
        assertTrue(Color.alpha(cookie.getPixel(240, 240)) > 200)
        savePng(cookie, File(previewDir, "widget_next_up_cookie_dark.png"))
        cookie.recycle()

        val notifBanner = ExpressiveNotificationArt.renderCycleRhythmBannerBitmap(
            context = context,
            cycleStatus = statuses.first(),
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isDark = true
        )
        assertEquals(960, notifBanner.width)
        assertEquals(340, notifBanner.height)
        savePng(notifBanner, File(previewDir, "notif_cycle_rhythm_dark.png"))
        notifBanner.recycle()

        val progressBanner = ExpressiveNotificationArt.renderActionProgressBannerBitmap(
            context = context,
            action = PendingSubscriptionAction(
                subscriptionId = 1L,
                subscriptionName = "Disney+",
                actionType = SubscriptionActionProgressType.UNSUBSCRIBE
            ),
            serviceColorHex = "#113CCF",
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isDark = true
        )
        assertEquals(960, progressBanner.width)
        assertEquals(230, progressBanner.height)
        savePng(progressBanner, File(previewDir, "notif_action_progress_dark.png"))
        progressBanner.recycle()

        val badge = ExpressiveNotificationArt.renderServiceBadgeBitmap(
            context = context,
            serviceName = "Disney+",
            colorHex = "#113CCF",
            shape = NotificationBadgeShape.COOKIE_9,
            isDark = true
        )
        assertEquals(192, badge.width)
        assertEquals(0, Color.alpha(badge.getPixel(0, 0)))
        badge.recycle()
    }

    private fun savePng(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
