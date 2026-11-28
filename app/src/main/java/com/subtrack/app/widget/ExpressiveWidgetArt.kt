package com.subtrack.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.subtrack.app.notification.ExpressiveNotificationArt
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.components.drawSubCyclerSwirlLogoOnCanvas
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class LivingWidgetSizeTier {
    COMPACT_2X2,
    WIDE_4X2,
    TALL_4X3
}

/**
 * Shape-only & flat color-block backdrop renderer for SubCycler's **Native `RemoteViews` Home-Screen Widgets**.
 *
 * All typography, pills, row containers, touch ripples, and Android 12+ dynamic widget corner radii
 * are rendered natively by Android `RemoteViews`. This helper supplies strictly non-text accents:
 * - Pure Flat Color-Block Living Stack Mood backdrop with tone-on-tone M3E polygon geometry
 * - Dynamic 3-petal swirl emblem
 * - Transparent curly spend/savings efficiency wave & 2-zone cycle rhythm strips
 * - Shape-only scalloped cookie shell + sweeping perimeter ring for `Next Up Cookie`
 */
object ExpressiveWidgetArt {

    fun classifySizeTier(minWidthDp: Int, minHeightDp: Int): LivingWidgetSizeTier = when {
        minHeightDp >= 185 -> LivingWidgetSizeTier.TALL_4X3
        minWidthDp >= 220 -> LivingWidgetSizeTier.WIDE_4X2
        else -> LivingWidgetSizeTier.COMPACT_2X2
    }

    /**
     * Renders a **Pure Flat Color-Block** Living Stack Mood backdrop matching the in-app Hero HUD:
     * one confident solid jewel color + crisp tone-on-tone geometric M3 Expressive shapes
     * (top-right white `Clover8` watermark + subtle bottom-left graphic depth block; zero rainbow,
     * zero silk-photo haze, zero rasterized text).
     */
    fun renderHeroMoodBackdropBitmap(
        context: Context,
        activeSubscriptionsForMood: List<Subscription>,
        useStackMood: Boolean,
        isDark: Boolean,
        launcherIcon: AppLauncherIcon,
        widthPx: Int = 560,
        heightPx: Int = 300
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val heroMood = BrandColorHarmonizer.resolveHeroMoodPalette(
            activeSubscriptions = activeSubscriptionsForMood,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )
        canvas.drawColor(ExpressiveNotificationArt.toArgb(heroMood.baseColor))

        // Subtle bottom-left dark graphic depth block
        val lowerShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0x26000000 else 0x17000000
        }
        canvas.save()
        canvas.rotate(20f, 36f, heightPx - 12f)
        val blockSize = min(widthPx, heightPx) * 0.62f
        canvas.drawRoundRect(
            RectF(-blockSize * 0.35f, heightPx - blockSize * 0.65f, blockSize * 0.65f, heightPx + blockSize * 0.35f),
            blockSize * 0.34f,
            blockSize * 0.34f,
            lowerShade
        )
        canvas.restore()

        // Crisp tone-on-tone top-right Clover8 polygon watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ExpressiveNotificationArt.toArgb(heroMood.watermarkTint, if (isDark) 26 else 34)
        }
        canvas.drawPath(
            ExpressiveNotificationArt.buildScallopedPath(
                cx = widthPx - 36f,
                cy = 34f,
                radius = min(widthPx, heightPx) * 0.44f,
                petals = 8,
                amplitudeFraction = 0.09f
            ),
            wmPaint
        )

        return bitmap
    }

    /**
     * Renders ONLY the crisp dynamic 3-petal SubCycler swirl emblem (`96×96`).
     */
    fun renderSwirlBadgeBitmap(
        activeSubscriptionsForMood: List<Subscription>,
        useStackMood: Boolean,
        launcherIcon: AppLauncherIcon,
        sizePx: Int = 96
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val swirlColors = BrandColorHarmonizer.resolveSwirlPetalColors(
            activeSubscriptions = activeSubscriptionsForMood,
            useStackMood = useStackMood,
            fallbackIcon = launcherIcon
        )
        drawSubCyclerSwirlLogoOnCanvas(
            canvas = canvas,
            bounds = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
            colors = swirlColors,
            cornerRadiusPx = sizePx * 0.30f
        )
        return bitmap
    }

    /**
     * Renders a **100% transparent-background** Material 3 Expressive curly Spend wave + chunky
     * theme-accent Saved pill matching `FinancialEfficiencyHud` on the home screen.
     */
    fun renderWavyEfficiencyStripBitmap(
        spendFraction: Float,
        waveArgb: Int,
        savedArgb: Int,
        showSavedPill: Boolean = true,
        widthPx: Int = 420,
        heightPx: Int = 36
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerY = heightPx / 2f
        val strokeW = 10.5f
        val halfStroke = strokeW / 2f
        val savedPillH = 12.5f
        val visualGap = 15f
        val waveStartX = halfStroke
        val waveVisualEndX = if (showSavedPill) {
            (widthPx - visualGap) * spendFraction.coerceIn(0.22f, 0.88f)
        } else {
            widthPx - savedPillH - visualGap
        }
        val waveEndX = (waveVisualEndX - halfStroke).coerceAtLeast(waveStartX + 1f)

        val amp = 6.8f
        val wavelength = 48f
        val rampPx = 18f
        val wavePath = Path()
        var x = waveStartX
        var first = true
        while (x <= waveEndX) {
            val distStart = x - waveStartX
            val distEnd = waveEndX - x
            val env = min(1f, min(distStart, distEnd) / rampPx).coerceIn(0f, 1f)
            val y = centerY + amp * env * sin((distStart / wavelength) * (2.0 * PI).toFloat())
            if (first) {
                wavePath.moveTo(x, y)
                first = false
            } else {
                wavePath.lineTo(x, y)
            }
            x += 3f
        }
        wavePath.lineTo(waveEndX, centerY)

        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = waveArgb
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(wavePath, wavePaint)

        val savedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = savedArgb }
        if (showSavedPill) {
            val savedStartX = (waveVisualEndX + visualGap).coerceAtMost(widthPx - savedPillH)
            val savedRect = RectF(savedStartX, centerY - savedPillH / 2f, widthPx.toFloat(), centerY + savedPillH / 2f)
            canvas.drawRoundRect(savedRect, savedPillH / 2f, savedPillH / 2f, savedPaint)
        } else {
            canvas.drawCircle(widthPx - savedPillH / 2f, centerY, savedPillH * 0.52f, savedPaint)
        }

        return bitmap
    }

    /**
     * Renders a **100% transparent-background** slim 2-zone Active + `45°` Hatched Break strip
     * (`260×28`) for native widget next-up cards.
     */
    fun renderMiniRhythmStripBitmap(
        status: CycleStatus,
        accentArgb: Int,
        isDark: Boolean,
        widthPx: Int = 260,
        heightPx: Int = 28
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sub = status.subscription
        val centerY = heightPx / 2f
        val barH = 12f
        val r = barH / 2f
        val activeDays = sub.activeDays.coerceAtLeast(1)
        val breakDays = if (sub.isRotating) sub.blackoutDays.coerceAtLeast(0) else 0
        val gap = if (breakDays > 0) 10f else 0f
        val total = (activeDays + breakDays).coerceAtLeast(1)
        val activeW = if (breakDays > 0) {
            ((widthPx - gap) * (activeDays.toFloat() / total)).coerceIn(widthPx * 0.44f, widthPx * 0.76f)
        } else {
            widthPx.toFloat()
        }
        val isBlackout = status.phase == CyclePhase.BLACKOUT

        val trackBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0x3DFFFFFF else 0x26151918
        }
        canvas.drawRoundRect(RectF(0f, centerY - r, activeW, centerY + r), r, r, trackBg)

        val activeProgress = if (isBlackout) 1f else status.progressFraction.coerceIn(0.14f, 1f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlackout) ((accentArgb and 0x00FFFFFF) or (0x66 shl 24)) else accentArgb
        }
        canvas.drawRoundRect(
            RectF(0f, centerY - r, (activeW * activeProgress).coerceAtLeast(barH), centerY + r),
            r,
            r,
            fillPaint
        )

        if (breakDays > 0) {
            val breakLeft = activeW + gap
            val breakRect = RectF(breakLeft, centerY - r, widthPx.toFloat(), centerY + r)
            val breakPath = Path().apply { addRoundRect(breakRect, r, r, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(breakPath)
            val bBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) 0x30B3C3CF else 0x2A4C5D6B
            }
            canvas.drawRect(breakRect, bBg)
            val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBlackout) ((accentArgb and 0x00FFFFFF) or (0xC0 shl 24)) else (if (isDark) 0x66B3C3CF else 0x554C5D6B)
                strokeWidth = 3f
            }
            var hx = breakRect.left - 18f
            while (hx < breakRect.right + 18f) {
                canvas.drawLine(hx, breakRect.bottom + 3f, hx + 14f, breakRect.top - 3f, hatch)
                hx += 11f
            }
            canvas.restore()
        }

        return bitmap
    }

    /**
     * Renders **shape-only** transparent-corner organic silhouette + perimeter progress ring
     * (`480×480`, ZERO rasterized text!) behind real native `TextView`s in `Next Up Cookie`.
     */
    fun renderNextUpCookieShellBitmap(
        soonest: CycleStatus?,
        launcherIcon: AppLauncherIcon,
        isDark: Boolean,
        useStackMood: Boolean = true,
        activeSubscriptionsForMood: List<Subscription> = emptyList(),
        sizePx: Int = 480
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val moodPool = activeSubscriptionsForMood.ifEmpty { listOfNotNull(soonest?.subscription) }
        val accent = BrandColorHarmonizer.resolveEffectiveAccentTokens(
            activeSubscriptions = moodPool,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )

        val containerArgb: Int
        val inkArgb: Int
        val ringAccentArgb: Int
        val silhouette: Path

        if (soonest == null) {
            containerArgb = ExpressiveNotificationArt.toArgb(accent.primaryContainer)
            inkArgb = ExpressiveNotificationArt.toArgb(accent.onPrimaryContainer)
            ringAccentArgb = ExpressiveNotificationArt.toArgb(accent.primary)
            silhouette = ExpressiveNotificationArt.buildScallopedPath(cx, cy, sizePx * 0.46f, petals = 12, amplitudeFraction = 0.065f)
        } else {
            val sub = soonest.subscription
            val palette = BrandColorHarmonizer.harmonizedTonalPalette(sub.name, sub.colorHex, isDark)
            val isBreakReady = soonest.phase == CyclePhase.BLACKOUT && soonest.daysRemainingInPhase <= 1
            if (isBreakReady) {
                containerArgb = ExpressiveNotificationArt.toArgb(accent.heroPop)
                inkArgb = ExpressiveNotificationArt.toArgb(accent.onHeroPop)
                ringAccentArgb = ExpressiveNotificationArt.toArgb(accent.onHeroPop)
                silhouette = ExpressiveNotificationArt.buildScallopedPath(cx, cy, sizePx * 0.47f, petals = 12, amplitudeFraction = 0.075f)
            } else {
                containerArgb = ExpressiveNotificationArt.toArgb(
                    if (useStackMood) palette.container else accent.heroBase
                )
                inkArgb = ExpressiveNotificationArt.toArgb(
                    if (useStackMood) palette.content else accent.heroContent
                )
                ringAccentArgb = ExpressiveNotificationArt.toArgb(
                    if (useStackMood) palette.content else accent.heroPop
                )
                silhouette = if (sub.isRotating) {
                    ExpressiveNotificationArt.buildScallopedPath(cx, cy, sizePx * 0.46f, petals = 10, amplitudeFraction = 0.068f)
                } else {
                    Path().apply {
                        addRoundRect(RectF(28f, 28f, sizePx - 28f, sizePx - 28f), 116f, 116f, Path.Direction.CW)
                    }
                }
            }
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = containerArgb
        }
        canvas.drawPath(silhouette, fillPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (inkArgb and 0x00FFFFFF) or (0x2C shl 24)
            strokeWidth = 3f
        }
        canvas.drawPath(silhouette, rimPaint)

        val arcR = sizePx * 0.365f
        val arcRect = RectF(cx - arcR, cy - arcR, cx + arcR, cy + arcR)
        val trackArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (inkArgb and 0x00FFFFFF) or (0x2E shl 24)
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(14f, 14f), 0f)
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackArcPaint)

        if (soonest != null) {
            val progress = soonest.progressFraction.coerceIn(0.06f, 1f)
            val sweep = 360f * progress
            val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = ringAccentArgb
                strokeWidth = 11f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(arcRect, -90f, sweep, false, activeArcPaint)

            val tipRad = ((-90.0 + sweep) * PI / 180.0)
            val tipX = cx + arcR * cos(tipRad).toFloat()
            val tipY = cy + arcR * sin(tipRad).toFloat()
            canvas.drawCircle(tipX, tipY, 9.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringAccentArgb })
        }

        return bitmap
    }
}
