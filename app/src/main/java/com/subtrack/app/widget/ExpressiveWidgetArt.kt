package com.subtrack.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.subtrack.app.notification.ExpressiveNotificationArt
import com.subtrack.app.notification.NotificationBadgeShape
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.ui.R as CoreUiR
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.drawSubCyclerSwirlLogoOnCanvas
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.launcherAccentTokensFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LivingWidgetSizeTier {
    COMPACT_2X2,
    WIDE_4X2,
    TALL_4X3
}

/**
 * Pure offline Material 3 Expressive v4.0 Canvas renderer for SubCycler's Home-Screen Widget Suite:
 * 1. **Living Stack Pulse** (responsive 2x2 / 4x2 / 4x3 synced to App Icon accent & Living Stack Mood)
 * 2. **Next Up Cookie** (2x2 sculptural non-rectangular countdown widget in the soonest service's pastel)
 */
object ExpressiveWidgetArt {

    fun classifySizeTier(minWidthDp: Int, minHeightDp: Int): LivingWidgetSizeTier = when {
        minHeightDp >= 185 -> LivingWidgetSizeTier.TALL_4X3
        minWidthDp >= 220 -> LivingWidgetSizeTier.WIDE_4X2
        else -> LivingWidgetSizeTier.COMPACT_2X2
    }

    private fun decodeSilkWavesScaled(context: Context): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeResource(context.resources, CoreUiR.drawable.bg_expressive_silk_waves, opts)
    }.getOrNull()

    /**
     * Renders the responsive **Living Stack Pulse** widget bitmap matching the user's
     * active [AppLauncherIcon] accent, Living Stack Mood toggle, and Light/Dark theme.
     */
    fun renderLivingStackPulseBitmap(
        context: Context,
        summary: PortfolioSummary,
        upcomingStatuses: List<CycleStatus>,
        activeSubscriptionsForMood: List<com.subtrack.core.domain.model.Subscription>,
        currencyCode: String,
        launcherIcon: AppLauncherIcon,
        useStackMood: Boolean,
        isDark: Boolean,
        tier: LivingWidgetSizeTier
    ): Bitmap {
        val (w, h) = when (tier) {
            LivingWidgetSizeTier.COMPACT_2X2 -> 480 to 480
            LivingWidgetSizeTier.WIDE_4X2 -> 860 to 400
            LivingWidgetSizeTier.TALL_4X3 -> 860 to 680
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val displayTf = ExpressiveNotificationArt.loadDisplayTypeface(context)
        val bodyBoldTf = ExpressiveNotificationArt.loadBodyTypeface(context, bold = true)
        val bodyTf = ExpressiveNotificationArt.loadBodyTypeface(context, bold = false)

        val accent = launcherAccentTokensFor(launcherIcon, isDark)
        val heroMood = BrandColorHarmonizer.resolveHeroMoodPalette(
            activeSubscriptions = activeSubscriptionsForMood,
            useStackMood = useStackMood,
            isDark = isDark,
            fallbackIcon = launcherIcon
        )
        val swirlColors = BrandColorHarmonizer.resolveSwirlPetalColors(
            activeSubscriptions = activeSubscriptionsForMood,
            useStackMood = useStackMood,
            fallbackIcon = launcherIcon
        )

        val cardRadius = if (tier == LivingWidgetSizeTier.COMPACT_2X2) 64f else 56f
        val fullRect = RectF(4f, 4f, w - 4f, h - 4f)
        val clipPath = Path().apply { addRoundRect(fullRect, cardRadius, cardRadius, Path.Direction.CW) }

        val surfaceBase = if (isDark) 0xFF14181A.toInt() else 0xFFF7F9F8.toInt()
        val inkPrimary = ExpressiveNotificationArt.toArgb(heroMood.contentColor)
        val inkSecondary = ExpressiveNotificationArt.toArgb(heroMood.contentColor, 178)
        val rimColor = if (isDark) 0x2EFFFFFF else 0x1F151918

        canvas.save()
        canvas.clipPath(clipPath)

        // Base canvas (for TALL_4X3, Hero occupies top 316px and Stack Deck sits on calm neutral below)
        canvas.drawColor(surfaceBase)
        val heroBottom = if (tier == LivingWidgetSizeTier.TALL_4X3) 318f else h.toFloat()
        val heroRect = RectF(0f, 0f, w.toFloat(), heroBottom)

        // Draw silk-waves texture + Living Stack Mood gradient inside Hero area
        canvas.save()
        val heroClip = if (tier == LivingWidgetSizeTier.TALL_4X3) {
            Path().apply {
                addRoundRect(
                    RectF(4f, 4f, w - 4f, heroBottom),
                    floatArrayOf(cardRadius, cardRadius, cardRadius, cardRadius, 36f, 36f, 36f, 36f),
                    Path.Direction.CW
                )
            }
        } else {
            clipPath
        }
        canvas.clipPath(heroClip)
        canvas.drawColor(ExpressiveNotificationArt.toArgb(heroMood.baseColor))

        decodeSilkWavesScaled(context)?.let { silk ->
            val silkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = if (isDark) 86 else 112
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
            val src = Rect(0, 0, silk.width, silk.height)
            canvas.drawBitmap(silk, src, heroRect, silkPaint)
            silk.recycle()
        }

        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), heroBottom,
                intArrayOf(
                    ExpressiveNotificationArt.toArgb(heroMood.gradientStart, if (isDark) 205 else 188),
                    ExpressiveNotificationArt.toArgb(heroMood.gradientMid, if (isDark) 216 else 206),
                    ExpressiveNotificationArt.toArgb(heroMood.gradientEnd, if (isDark) 232 else 224)
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(heroRect, gradPaint)

        // Off-corner Clover8 watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ExpressiveNotificationArt.toArgb(heroMood.watermarkTint, if (isDark) 26 else 24)
        }
        canvas.drawPath(
            ExpressiveNotificationArt.buildScallopedPath(w - 44f, 44f, 152f, petals = 8, amplitudeFraction = 0.09f),
            wmPaint
        )
        canvas.restore()

        val soonest = upcomingStatuses.firstOrNull()
        val hasSavings = summary.totalAnnualSavings > 0.5
        val savingsAmount = MoneyFormatter.format(summary.totalAnnualSavings, currencyCode, maximumFractionDigits = 0)
        val monthlyFormatted = if (summary.totalSubscriptions == 0) {
            "Add stack"
        } else {
            MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode)
        }

        when (tier) {
            LivingWidgetSizeTier.COMPACT_2X2 -> {
                // Top row: Dynamic 3-petal swirl + active status pill
                drawSubCyclerSwirlLogoOnCanvas(
                    canvas = canvas,
                    bounds = RectF(34f, 34f, 92f, 92f),
                    colors = swirlColors,
                    cornerRadiusPx = 18f
                )
                val pillText = when {
                    summary.totalSubscriptions == 0 -> "Empty"
                    summary.blackoutCount > 0 -> "${summary.activeCount} on · ${summary.blackoutCount} off"
                    else -> "${summary.activeCount} active"
                }
                drawCompactPill(
                    canvas = canvas,
                    text = pillText,
                    left = 108f,
                    centerY = 63f,
                    bgArgb = if (isDark) 0x99101417.toInt() else 0xDDF7F9F8.toInt(),
                    fgArgb = inkPrimary,
                    typeface = bodyBoldTf,
                    textSize = 22f
                )

                // Effective Monthly spend
                val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkSecondary
                    typeface = bodyBoldTf
                    textSize = 19f
                    letterSpacing = 0.11f
                }
                canvas.drawText("EFFECTIVE MONTHLY", 34f, 146f, eyebrowPaint)

                val moneyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkPrimary
                    typeface = displayTf
                    textSize = if (monthlyFormatted.length > 7) 54f else 64f
                }
                canvas.drawText(monthlyFormatted, 34f, 214f, moneyPaint)

                // Tilted 12-petal savings seal bottom-right
                if (hasSavings) {
                    drawSavingsSeal(
                        canvas = canvas,
                        cx = w - 102f,
                        cy = 326f,
                        radius = 74f,
                        amount = savingsAmount,
                        accentBg = ExpressiveNotificationArt.toArgb(accent.primary),
                        accentInk = ExpressiveNotificationArt.toArgb(accent.onPrimary),
                        displayTf = displayTf,
                        bodyBoldTf = bodyBoldTf
                    )
                }

                // Bottom-left next-up summary + slim rhythm bar
                val nextCaption = when {
                    soonest == null -> "100% offline"
                    soonest.phase == CyclePhase.BLACKOUT -> "${soonest.subscription.name} · ${soonest.daysRemainingInPhase}d break"
                    else -> "${soonest.subscription.name} · ${soonest.daysRemainingInPhase}d"
                }
                val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkPrimary
                    typeface = bodyBoldTf
                    textSize = 23f
                }
                val maxCapW = if (hasSavings) w - 220f else w - 68f
                canvas.drawText(truncateText(nextCaption, capPaint, maxCapW), 34f, 348f, capPaint)

                if (soonest != null) {
                    drawMiniRhythmBar(
                        canvas = canvas,
                        left = 34f,
                        right = if (hasSavings) w - 200f else w - 34f,
                        centerY = 392f,
                        status = soonest,
                        accentArgb = ExpressiveNotificationArt.toArgb(accent.primary),
                        isDark = isDark
                    )
                }
            }

            LivingWidgetSizeTier.WIDE_4X2 -> {
                drawHeroHeaderBlock(
                    canvas = canvas,
                    width = w.toFloat(),
                    summary = summary,
                    monthlyFormatted = monthlyFormatted,
                    savingsAmount = savingsAmount,
                    hasSavings = hasSavings,
                    currencyCode = currencyCode,
                    swirlColors = swirlColors,
                    accent = accent,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    isDark = isDark,
                    displayTf = displayTf,
                    bodyBoldTf = bodyBoldTf,
                    bodyTf = bodyTf
                )

                // Bottom frosted Next-Up ribbon with organic badge + wavy/hatched rhythm bar
                val barRect = RectF(28f, 274f, w - 28f, h - 24f)
                val barFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) 0xB814181A.toInt() else 0xDEFFFFFF.toInt()
                }
                canvas.drawRoundRect(barRect, 30f, 30f, barFill)

                if (soonest != null) {
                    val sub = soonest.subscription
                    val badge = ExpressiveNotificationArt.renderServiceBadgeBitmap(
                        context = context,
                        serviceName = sub.name,
                        colorHex = sub.colorHex,
                        shape = if (soonest.phase == CyclePhase.BLACKOUT) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
                        isDark = isDark,
                        launcherIcon = launcherIcon,
                        sizePx = 68
                    )
                    canvas.drawBitmap(badge, barRect.left + 16f, barRect.centerY() - 34f, null)
                    badge.recycle()

                    val rowPrimary = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
                    val rowSecondary = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()
                    val nextLabel = when {
                        soonest.phase == CyclePhase.ACTIVE && sub.isRotating && !soonest.isAutoRenewCancelledForCurrentCycle ->
                            "Cancel ${sub.name} in ${soonest.daysRemainingInPhase}d"
                        soonest.phase == CyclePhase.BLACKOUT ->
                            "Resume ${sub.name} in ${soonest.daysRemainingInPhase}d"
                        else ->
                            "${sub.name} · ${soonest.daysRemainingInPhase}d left"
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rowPrimary
                        typeface = bodyBoldTf
                        textSize = 25f
                    }
                    canvas.drawText(
                        truncateText(nextLabel, labelPaint, barRect.width() - 330f),
                        barRect.left + 100f,
                        barRect.centerY() - 4f,
                        labelPaint
                    )
                    val subCaption = if (sub.isRotating) {
                        "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                    } else {
                        "Continuous · ${sub.billingPeriod.name.lowercase()}"
                    }
                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rowSecondary
                        typeface = bodyTf
                        textSize = 20f
                    }
                    canvas.drawText(subCaption, barRect.left + 100f, barRect.centerY() + 26f, subPaint)

                    drawMiniRhythmBar(
                        canvas = canvas,
                        left = barRect.right - 220f,
                        right = barRect.right - 22f,
                        centerY = barRect.centerY(),
                        status = soonest,
                        accentArgb = ExpressiveNotificationArt.toArgb(accent.primary),
                        isDark = isDark
                    )
                } else {
                    val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()
                        typeface = bodyBoldTf
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Tap to add your first subscription offline", barRect.centerX(), barRect.centerY() + 8f, emptyPaint)
                }
            }

            LivingWidgetSizeTier.TALL_4X3 -> {
                drawHeroHeaderBlock(
                    canvas = canvas,
                    width = w.toFloat(),
                    summary = summary,
                    monthlyFormatted = monthlyFormatted,
                    savingsAmount = savingsAmount,
                    hasSavings = hasSavings,
                    currencyCode = currencyCode,
                    swirlColors = swirlColors,
                    accent = accent,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    isDark = isDark,
                    displayTf = displayTf,
                    bodyBoldTf = bodyBoldTf,
                    bodyTf = bodyTf
                )

                // Bottom Stack Deck: up to 3 chromatic Cycle Vessel mini-rows aligned to clickable RemoteViews bands
                val rowsTop = 336f
                val rowHeight = 100f
                val rowGap = 12f
                val visible = upcomingStatuses.take(3)
                if (visible.isEmpty()) {
                    val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()
                        typeface = bodyBoldTf
                        textSize = 26f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Tap anywhere to build your offline stack", w / 2f, 505f, emptyPaint)
                } else {
                    visible.forEachIndexed { idx, status ->
                        val top = rowsTop + idx * (rowHeight + rowGap)
                        val rowRect = RectF(24f, top, w - 24f, top + rowHeight)
                        drawTallServiceRow(
                            canvas = canvas,
                            context = context,
                            rect = rowRect,
                            status = status,
                            index = idx,
                            total = visible.size,
                            launcherIcon = launcherIcon,
                            isDark = isDark,
                            displayTf = displayTf,
                            bodyBoldTf = bodyBoldTf,
                            bodyTf = bodyTf
                        )
                    }
                }
            }
        }

        canvas.restore()

        // Crisp outer hairline frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = rimColor
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(fullRect, cardRadius, cardRadius, borderPaint)

        return bitmap
    }

    /**
     * Renders the sculptural `2×2` **Next Up Cookie** widget bitmap (`480×480`, transparent outer canvas)
     * so the scalloped cookie / squircle / sunny burst silhouette floats directly over the user's wallpaper.
     */
    fun renderNextUpCookieBitmap(
        context: Context,
        soonest: CycleStatus?,
        launcherIcon: AppLauncherIcon,
        isDark: Boolean
    ): Bitmap {
        val size = 480
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val displayTf = ExpressiveNotificationArt.loadDisplayTypeface(context)
        val bodyBoldTf = ExpressiveNotificationArt.loadBodyTypeface(context, bold = true)
        val bodyTf = ExpressiveNotificationArt.loadBodyTypeface(context, bold = false)

        val cx = size / 2f
        val cy = size / 2f
        val accent = launcherAccentTokensFor(launcherIcon, isDark)

        val containerArgb: Int
        val inkArgb: Int
        val ringAccentArgb: Int
        val silhouette: Path

        if (soonest == null) {
            containerArgb = ExpressiveNotificationArt.toArgb(accent.primaryContainer)
            inkArgb = ExpressiveNotificationArt.toArgb(accent.onPrimaryContainer)
            ringAccentArgb = ExpressiveNotificationArt.toArgb(accent.primary)
            silhouette = ExpressiveNotificationArt.buildScallopedPath(cx, cy, size * 0.46f, petals = 12, amplitudeFraction = 0.065f)
        } else {
            val sub = soonest.subscription
            val palette = BrandColorHarmonizer.harmonizedTonalPalette(sub.name, sub.colorHex, isDark)
            val isBreakReady = soonest.phase == CyclePhase.BLACKOUT && soonest.daysRemainingInPhase <= 1
            if (isBreakReady) {
                containerArgb = ExpressiveNotificationArt.toArgb(accent.primary)
                inkArgb = ExpressiveNotificationArt.toArgb(accent.onPrimary)
                ringAccentArgb = ExpressiveNotificationArt.toArgb(accent.onPrimary)
                silhouette = ExpressiveNotificationArt.buildScallopedPath(cx, cy, size * 0.47f, petals = 12, amplitudeFraction = 0.075f)
            } else if (sub.isRotating) {
                containerArgb = ExpressiveNotificationArt.toArgb(palette.container)
                inkArgb = ExpressiveNotificationArt.toArgb(palette.content)
                ringAccentArgb = ExpressiveNotificationArt.toArgb(palette.accent)
                silhouette = ExpressiveNotificationArt.buildScallopedPath(cx, cy, size * 0.46f, petals = 10, amplitudeFraction = 0.068f)
            } else {
                containerArgb = ExpressiveNotificationArt.toArgb(palette.container)
                inkArgb = ExpressiveNotificationArt.toArgb(palette.content)
                ringAccentArgb = ExpressiveNotificationArt.toArgb(palette.accent)
                silhouette = Path().apply {
                    addRoundRect(RectF(28f, 28f, size - 28f, size - 28f), 116f, 116f, Path.Direction.CW)
                }
            }
        }

        // Fill sculptural silhouette
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = containerArgb
        }
        canvas.drawPath(silhouette, fillPaint)

        // Subtle tactile inner border along silhouette
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (inkArgb and 0x00FFFFFF) or (0x2E shl 24)
            strokeWidth = 3f
        }
        canvas.drawPath(silhouette, rimPaint)

        // Sweeping perimeter progress arc inside the silhouette
        val arcR = size * 0.355f
        val arcRect = RectF(cx - arcR, cy - arcR, cx + arcR, cy + arcR)
        val trackArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (inkArgb and 0x00FFFFFF) or (0x30 shl 24)
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

            // Glowing progress tip dot
            val tipRad = ((-90.0 + sweep) * PI / 180.0)
            val tipX = cx + arcR * cos(tipRad).toFloat()
            val tipY = cy + arcR * sin(tipRad).toFloat()
            canvas.drawCircle(tipX, tipY, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringAccentArgb })

            val sub = soonest.subscription
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (inkArgb and 0x00FFFFFF) or (0xCC shl 24)
                typeface = bodyBoldTf
                textSize = 25f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(truncateText(sub.name, namePaint, 240f), cx, cy - 64f, namePaint)

            val countdownText = when {
                soonest.daysRemainingInPhase <= 0 -> "Today"
                else -> "${soonest.daysRemainingInPhase}d"
            }
            val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkArgb
                typeface = displayTf
                textSize = when {
                    countdownText == "Today" -> 64f
                    countdownText.length >= 4 -> 76f
                    else -> 96f
                }
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(countdownText, cx, cy + 34f, countPaint)

            val phaseCaption = when {
                soonest.phase == CyclePhase.BLACKOUT && soonest.perBreakSavings > 0.5 -> {
                    val saved = MoneyFormatter.format(soonest.perBreakSavings, sub.currency, maximumFractionDigits = 0)
                    "Bank $saved"
                }
                soonest.phase == CyclePhase.BLACKOUT -> "of break"
                sub.isRotating && !soonest.isAutoRenewCancelledForCurrentCycle -> "until cancel"
                else -> "remaining"
            }
            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (inkArgb and 0x00FFFFFF) or (0xBF shl 24)
                typeface = bodyTf
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(phaseCaption, cx, cy + 84f, captionPaint)
        } else {
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkArgb
                typeface = displayTf
                textSize = 38f
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (inkArgb and 0x00FFFFFF) or (0xB8 shl 24)
                typeface = bodyBoldTf
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("SubCycler", cx, cy + 6f, brandPaint)
            canvas.drawText("Tap to add", cx, cy + 48f, subPaint)
        }

        return bitmap
    }

    private fun drawHeroHeaderBlock(
        canvas: Canvas,
        width: Float,
        summary: PortfolioSummary,
        monthlyFormatted: String,
        savingsAmount: String,
        hasSavings: Boolean,
        currencyCode: String,
        swirlColors: com.subtrack.core.ui.theme.SwirlLogoColors,
        accent: com.subtrack.core.ui.theme.LauncherAccentTokens,
        inkPrimary: Int,
        inkSecondary: Int,
        isDark: Boolean,
        displayTf: Typeface,
        bodyBoldTf: Typeface,
        bodyTf: Typeface
    ) {
        drawSubCyclerSwirlLogoOnCanvas(
            canvas = canvas,
            bounds = RectF(32f, 28f, 82f, 78f),
            colors = swirlColors,
            cornerRadiusPx = 15f
        )

        val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkSecondary
            typeface = bodyBoldTf
            textSize = 20f
            letterSpacing = 0.12f
        }
        canvas.drawText("EFFECTIVE MONTHLY", 98f, 60f, eyebrowPaint)

        val moneyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkPrimary
            typeface = displayTf
            textSize = 72f
        }
        canvas.drawText(monthlyFormatted, 34f, 158f, moneyPaint)

        val pctOff = summary.overallSavingsPercentage.roundToInt()
        val subLine = if (summary.totalSubscriptions == 0) {
            "100% offline subscription rotation"
        } else if (hasSavings && pctOff > 0) {
            val std = MoneyFormatter.format(summary.totalMonthlyStandardCost, currencyCode)
            "Down from $std/mo · -$pctOff%"
        } else {
            "${summary.activeCount} active service${if (summary.activeCount == 1) "" else "s"}"
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkSecondary
            typeface = bodyTf
            textSize = 24f
        }
        val maxSubW = if (hasSavings) width - 252f else width - 68f
        canvas.drawText(truncateText(subLine, subPaint, maxSubW), 34f, 200f, subPaint)

        val pillText = when {
            summary.totalSubscriptions == 0 -> "Empty"
            summary.blackoutCount > 0 -> "${summary.activeCount} active · ${summary.blackoutCount} paused"
            else -> "${summary.activeCount} active services"
        }
        drawCompactPill(
            canvas = canvas,
            text = pillText,
            left = 34f,
            centerY = 238f,
            bgArgb = if (isDark) 0x99101417.toInt() else 0xDDF7F9F8.toInt(),
            fgArgb = inkPrimary,
            typeface = bodyBoldTf,
            textSize = 21f
        )

        if (hasSavings) {
            drawSavingsSeal(
                canvas = canvas,
                cx = width - 118f,
                cy = 144f,
                radius = 90f,
                amount = savingsAmount,
                accentBg = ExpressiveNotificationArt.toArgb(accent.primary),
                accentInk = ExpressiveNotificationArt.toArgb(accent.onPrimary),
                displayTf = displayTf,
                bodyBoldTf = bodyBoldTf
            )
        }
    }

    private fun drawTallServiceRow(
        canvas: Canvas,
        context: Context,
        rect: RectF,
        status: CycleStatus,
        index: Int,
        total: Int,
        launcherIcon: AppLauncherIcon,
        isDark: Boolean,
        displayTf: Typeface,
        bodyBoldTf: Typeface,
        bodyTf: Typeface
    ) {
        val sub = status.subscription
        val palette = BrandColorHarmonizer.harmonizedTonalPalette(sub.name, sub.colorHex, isDark)
        val topR = if (index == 0) 36f else 12f
        val botR = if (index == total - 1) 36f else 12f
        val rowPath = Path().apply {
            addRoundRect(
                rect,
                floatArrayOf(topR, topR, topR, topR, botR, botR, botR, botR),
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(rowPath)

        val rowBg = if (isDark) 0xFF1C2124.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawColor(rowBg)

        // Subtle left-side Cycle Vessel tonal wash proportional to progress!
        val progressFillW = (rect.width() * status.progressFraction.coerceIn(0.10f, 1f))
        val vesselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ExpressiveNotificationArt.toArgb(palette.container, if (isDark) 62 else 82)
        }
        canvas.drawRect(rect.left, rect.top, rect.left + progressFillW, rect.bottom, vesselPaint)

        val badge = ExpressiveNotificationArt.renderServiceBadgeBitmap(
            context = context,
            serviceName = sub.name,
            colorHex = sub.colorHex,
            shape = if (status.phase == CyclePhase.BLACKOUT) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
            isDark = isDark,
            launcherIcon = launcherIcon,
            sizePx = 64
        )
        canvas.drawBitmap(badge, rect.left + 18f, rect.centerY() - 32f, null)
        badge.recycle()

        val inkPrimary = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
        val inkSecondary = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkPrimary
            typeface = displayTf
            textSize = 27f
        }
        canvas.drawText(truncateText(sub.name, namePaint, 340f), rect.left + 98f, rect.centerY() - 4f, namePaint)

        val priceStr = MoneyFormatter.format(sub.cost, sub.currency)
        val cadence = if (sub.isRotating) "${sub.totalCycleDays}d cycle" else "Continuous"
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkSecondary
            typeface = bodyTf
            textSize = 20f
        }
        canvas.drawText("$priceStr · $cadence", rect.left + 98f, rect.centerY() + 25f, subPaint)

        // Right countdown pill
        val countText = when {
            status.phase == CyclePhase.BLACKOUT -> "Paused · ${status.daysRemainingInPhase}d"
            status.daysRemainingInPhase <= 0 -> "Due today"
            else -> "${status.daysRemainingInPhase}d left"
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bodyBoldTf
            textSize = 21f
        }
        val pillW = pillPaint.measureText(countText) + 34f
        drawCompactPill(
            canvas = canvas,
            text = countText,
            left = rect.right - pillW - 18f,
            centerY = rect.centerY(),
            bgArgb = ExpressiveNotificationArt.toArgb(palette.container, if (isDark) 215 else 235),
            fgArgb = ExpressiveNotificationArt.toArgb(palette.content),
            typeface = bodyBoldTf,
            textSize = 21f
        )

        canvas.restore()
    }

    private fun drawSavingsSeal(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        amount: String,
        accentBg: Int,
        accentInk: Int,
        displayTf: Typeface,
        bodyBoldTf: Typeface
    ) {
        canvas.save()
        canvas.rotate(-6f, cx, cy)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentBg }
        canvas.drawPath(
            ExpressiveNotificationArt.buildScallopedPath(cx, cy, radius, petals = 12, amplitudeFraction = 0.068f),
            fillPaint
        )
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (accentInk and 0x00FFFFFF) or (0x62 shl 24)
            strokeWidth = 2.4f
            pathEffect = DashPathEffect(floatArrayOf(8f, 7f), 0f)
        }
        canvas.drawCircle(cx, cy, radius * 0.78f, dashPaint)

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accentInk and 0x00FFFFFF) or (0xCC shl 24)
            typeface = bodyBoldTf
            textSize = radius * 0.21f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentInk
            typeface = displayTf
            textSize = if (amount.length >= 6) radius * 0.31f else radius * 0.37f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SAVE", cx, cy - radius * 0.27f, smallPaint)
        canvas.drawText(amount, cx, cy + radius * 0.13f, amountPaint)
        smallPaint.letterSpacing = 0.02f
        canvas.drawText("per year", cx, cy + radius * 0.42f, smallPaint)
        canvas.restore()
    }

    private fun drawMiniRhythmBar(
        canvas: Canvas,
        left: Float,
        right: Float,
        centerY: Float,
        status: CycleStatus,
        accentArgb: Int,
        isDark: Boolean
    ) {
        val totalW = (right - left).coerceAtLeast(60f)
        val sub = status.subscription
        val hasBreak = sub.blackoutDays > 0
        val gap = if (hasBreak) 10f else 0f
        val activeW = if (hasBreak) {
            ((totalW - gap) * (sub.activeDays.toFloat() / sub.totalCycleDays.coerceAtLeast(1)))
                .coerceIn(totalW * 0.50f, totalW * 0.76f)
        } else {
            totalW
        }
        val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF2D3438.toInt() else 0xFFD9DFDC.toInt()
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(left + 5f, centerY, left + activeW - 5f, centerY, railPaint)

        val progress = if (status.phase == CyclePhase.BLACKOUT) 1f else status.progressFraction.coerceIn(0.10f, 1f)
        val fillEnd = left + 5f + (activeW - 10f) * progress
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(left + 5f, centerY, fillEnd, centerY, activePaint)

        if (hasBreak) {
            val breakRect = RectF(left + activeW + gap, centerY - 6f, right, centerY + 6f)
            val breakPath = Path().apply { addRoundRect(breakRect, 6f, 6f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(breakPath)
            canvas.drawRect(
                breakRect,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDark) 0xFF262F35.toInt() else 0xFFE2E8EE.toInt()
                }
            )
            val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) 0x66B3C3CF else 0x664C5D6B
                strokeWidth = 3f
            }
            var hx = breakRect.left - 16f
            while (hx < breakRect.right + 16f) {
                canvas.drawLine(hx, breakRect.bottom + 4f, hx + 14f, breakRect.top - 4f, hatch)
                hx += 10f
            }
            canvas.restore()
        }
    }

    private fun drawCompactPill(
        canvas: Canvas,
        text: String,
        left: Float,
        centerY: Float,
        bgArgb: Int,
        fgArgb: Int,
        typeface: Typeface,
        textSize: Float
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgArgb
            this.typeface = typeface
            this.textSize = textSize
        }
        val tw = textPaint.measureText(text)
        val h = textSize * 1.75f
        val rect = RectF(left, centerY - h / 2f, left + tw + 34f, centerY + h / 2f)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgArgb })
        val fm = textPaint.fontMetrics
        canvas.drawText(text, rect.left + 17f, rect.centerY() - (fm.ascent + fm.descent) / 2f, textPaint)
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
