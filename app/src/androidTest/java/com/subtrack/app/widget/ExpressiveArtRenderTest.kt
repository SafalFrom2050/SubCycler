package com.subtrack.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.subtrack.app.notification.ExpressiveNotificationArt
import com.subtrack.app.notification.NotificationBadgeShape
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
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
    fun inflatesNativeRemoteViewsWidgetsAndRendersTransparentAccentsCleanly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val today = LocalDate.now()
        val subs = listOf(
            Subscription(
                id = 1L,
                name = "Disney+",
                cost = 1140.0,
                currency = "JPY",
                startDate = today.minusDays(28),
                strategy = RotationStrategy.CUSTOM,
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

        val previewDir = (context.getExternalFilesDir("expressive_previews")
            ?: File(context.cacheDir, "expressive_previews")).apply { mkdirs() }

        // 1. Verify transparent-background hybrid notification strips (zero opaque card rectangle!)
        val rhythmStrip = ExpressiveNotificationArt.renderTransparentCycleRhythmStripBitmap(
            cycleStatus = statuses.first(),
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isSystemDark = true
        )
        assertEquals(720, rhythmStrip.width)
        assertEquals(44, rhythmStrip.height)
        assertEquals(0, Color.alpha(rhythmStrip.getPixel(0, 0)))
        savePng(rhythmStrip, File(previewDir, "notif_rhythm_strip_dark.png"))
        rhythmStrip.recycle()

        val progressStrip = ExpressiveNotificationArt.renderTransparentActionProgressStripBitmap(
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isSystemDark = true
        )
        assertEquals(720, progressStrip.width)
        assertEquals(40, progressStrip.height)
        assertEquals(0, Color.alpha(progressStrip.getPixel(0, 0)))
        savePng(progressStrip, File(previewDir, "notif_progress_strip_dark.png"))
        progressStrip.recycle()

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

        // 2. Verify shape-only Next Up Cookie shell retains transparent outer corners
        val shell = ExpressiveWidgetArt.renderNextUpCookieShellBitmap(
            soonest = statuses.first(),
            launcherIcon = AppLauncherIcon.BOTANICAL,
            isDark = true
        )
        assertEquals(480, shell.width)
        assertEquals(480, shell.height)
        assertEquals(0, Color.alpha(shell.getPixel(2, 2)))
        assertTrue(Color.alpha(shell.getPixel(240, 240)) > 200)
        shell.recycle()

        // 3. Inflate and render REAL native RemoteViews widgets at true launcher dp sizes!
        val livingProvider = SubCyclerWidgetProvider()
        val cookieProvider = NextUpCookieWidgetProvider()

        val rvCompact = livingProvider.buildNativeLivingViews(
            context = context,
            widgetId = 1,
            tier = LivingWidgetSizeTier.COMPACT_2X2,
            summary = summary,
            upcoming = statuses,
            activeForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.BOTANICAL,
            useStackMood = false,
            isDark = true
        )
        val rvWide = livingProvider.buildNativeLivingViews(
            context = context,
            widgetId = 2,
            tier = LivingWidgetSizeTier.WIDE_4X2,
            summary = summary,
            upcoming = statuses,
            activeForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.OCEAN,
            useStackMood = false,
            isDark = true
        )
        val rvTallLight = livingProvider.buildNativeLivingViews(
            context = context,
            widgetId = 3,
            tier = LivingWidgetSizeTier.TALL_4X3,
            summary = summary,
            upcoming = statuses,
            activeForMood = subs,
            currencyCode = "JPY",
            launcherIcon = AppLauncherIcon.BOTANICAL,
            useStackMood = false,
            isDark = false
        )
        val rvCookie = cookieProvider.buildNativeCookieViews(
            context = context,
            soonest = statuses.first(),
            launcherIcon = AppLauncherIcon.BOTANICAL,
            useStackMood = true,
            activeForMood = subs,
            isDark = true
        )

        instrumentation.runOnMainSync {
            renderRemoteViewsToPng(context, rvCompact, 172, 172, File(previewDir, "native_widget_2x2_dark.png"))
            renderRemoteViewsToPng(context, rvWide, 344, 172, File(previewDir, "native_widget_4x2_ocean_dark.png"))
            renderRemoteViewsToPng(context, rvTallLight, 344, 286, File(previewDir, "native_widget_4x3_light.png"))
            renderRemoteViewsToPng(context, rvCookie, 172, 172, File(previewDir, "native_widget_cookie_dark.png"))
        }
    }

    private fun renderRemoteViewsToPng(
        context: android.content.Context,
        remoteViews: RemoteViews,
        widthDp: Int,
        heightDp: Int,
        outFile: File
    ) {
        val density = context.resources.displayMetrics.density
        val wPx = (widthDp * density).toInt()
        val hPx = (heightDp * density).toInt()
        val container = FrameLayout(context)
        val inflated = remoteViews.apply(context, container)
        inflated.measure(
            View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY)
        )
        inflated.layout(0, 0, wPx, hPx)
        val bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        inflated.draw(canvas)
        savePng(bmp, outFile)
        bmp.recycle()
    }

    private fun savePng(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
