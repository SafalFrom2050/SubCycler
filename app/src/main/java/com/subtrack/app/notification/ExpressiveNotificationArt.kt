package com.subtrack.app.notification

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.ui.R as CoreUiR
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.launcherAccentTokensFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class NotificationBadgeShape {
    COOKIE_9,
    SUNNY_12,
    SQUIRCLE
}

/**
 * Pure offline Android Canvas art renderer for SubCycler's Material 3 Expressive v4.0 notifications.
 * Produces organic harmonized monogram LargeIcons, expanded 2-zone Wavy+Hatched Cycle Rhythm Cards,
 * and 2-step Subscribe/Unsubscribe Progress Banners with bundled OFL variable typography.
 */
object ExpressiveNotificationArt {

    fun resolveDarkTheme(context: Context, themeMode: AppThemeMode): Boolean = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> {
            val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMask == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun toArgb(color: androidx.compose.ui.graphics.Color, alphaOverride: Int = 255): Int {
        val rgb = BrandColorHarmonizer.composeColorToRgb(color)
        return ((alphaOverride.coerceIn(0, 255)) shl 24) or (rgb and 0x00FFFFFF)
    }

    fun loadDisplayTypeface(context: Context): Typeface =
        runCatching { ResourcesCompat.getFont(context, CoreUiR.font.bricolage_grotesque_variable) }
            .getOrNull()
            ?.let { Typeface.create(it, Typeface.BOLD) }
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun loadBodyTypeface(context: Context, bold: Boolean = false): Typeface =
        runCatching { ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans_variable) }
            .getOrNull()
            ?.let { Typeface.create(it, if (bold) Typeface.BOLD else Typeface.NORMAL) }
            ?: Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)

    fun buildScallopedPath(
        cx: Float,
        cy: Float,
        radius: Float,
        petals: Int,
        amplitudeFraction: Float = 0.075f
    ): Path {
        val path = Path()
        val steps = petals * 20
        for (i in 0..steps) {
            val angle = (2.0 * PI * i / steps) - (PI / 2.0)
            val wave = cos(petals * angle).toFloat()
            val r = radius * (1f - amplitudeFraction + amplitudeFraction * wave)
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /**
     * Renders a crisp `192×192` transparent organic service monogram badge for `NotificationCompat.setLargeIcon`.
     */
    fun renderServiceBadgeBitmap(
        context: Context,
        serviceName: String,
        colorHex: String?,
        shape: NotificationBadgeShape,
        isDark: Boolean,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        sizePx: Int = 192
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = serviceName,
            storedColorHex = colorHex,
            isDark = isDark
        )
        val accent = launcherAccentTokensFor(launcherIcon, isDark)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        val containerArgb: Int
        val contentArgb: Int
        val badgePath = when (shape) {
            NotificationBadgeShape.COOKIE_9 -> {
                containerArgb = toArgb(palette.container)
                contentArgb = toArgb(palette.content)
                buildScallopedPath(cx, cy, sizePx * 0.46f, petals = 9, amplitudeFraction = 0.085f)
            }
            NotificationBadgeShape.SUNNY_12 -> {
                containerArgb = toArgb(accent.primary)
                contentArgb = toArgb(accent.onPrimary)
                buildScallopedPath(cx, cy, sizePx * 0.47f, petals = 12, amplitudeFraction = 0.07f)
            }
            NotificationBadgeShape.SQUIRCLE -> {
                containerArgb = toArgb(palette.container)
                contentArgb = toArgb(palette.content)
                Path().apply {
                    val inset = sizePx * 0.08f
                    addRoundRect(
                        RectF(inset, inset, sizePx - inset, sizePx - inset),
                        sizePx * 0.30f,
                        sizePx * 0.30f,
                        Path.Direction.CW
                    )
                }
            }
        }

        fillPaint.color = containerArgb
        canvas.drawPath(badgePath, fillPaint)

        // Subtle inner rim for crisp definition on both Light and Dark notification shades
        strokePaint.color = if (shape == NotificationBadgeShape.SUNNY_12) {
            toArgb(accent.onPrimary, 72)
        } else {
            toArgb(palette.border, 140)
        }
        strokePaint.strokeWidth = sizePx * 0.022f
        if (shape == NotificationBadgeShape.SUNNY_12) {
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(sizePx * 0.05f, sizePx * 0.04f), 0f)
            canvas.drawCircle(cx, cy, sizePx * 0.36f, strokePaint)
        } else {
            canvas.drawPath(badgePath, strokePaint)
        }

        val initials = ExpressivePalettes.initialsForService(serviceName)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = contentArgb
            typeface = loadDisplayTypeface(context)
            textSize = sizePx * 0.34f
            textAlign = Paint.Align.CENTER
        }
        val fm = textPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(initials, cx, baseline, textPaint)

        return bitmap
    }

    /**
     * Renders an expanded `960×340` Material 3 Expressive Cycle Rhythm Card for `BigPictureStyle`.
     */
    fun renderCycleRhythmBannerBitmap(
        context: Context,
        cycleStatus: CycleStatus,
        launcherIcon: AppLauncherIcon,
        isDark: Boolean
    ): Bitmap {
        val w = 960
        val h = 340
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sub = cycleStatus.subscription
        val servicePalette = BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = sub.name,
            storedColorHex = sub.colorHex,
            isDark = isDark
        )
        val accent = launcherAccentTokensFor(launcherIcon, isDark)
        val displayTf = loadDisplayTypeface(context)
        val bodyBoldTf = loadBodyTypeface(context, bold = true)
        val bodyTf = loadBodyTypeface(context, bold = false)

        val surfaceBase = if (isDark) 0xFF171B1D.toInt() else 0xFFFAFBFA.toInt()
        val textPrimary = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
        val textSecondary = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()
        val hairline = if (isDark) 0x33FFFFFF else 0x1F151918

        val cardRect = RectF(6f, 6f, w - 6f, h - 6f)
        val cardRadius = 44f

        // 1. Card backdrop + soft harmonized brand pastel wash
        val cardPath = Path().apply {
            addRoundRect(cardRect, cardRadius, cardRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(cardPath)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = surfaceBase
        canvas.drawRect(cardRect, bgPaint)

        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(
                    toArgb(servicePalette.container, if (isDark) 115 else 155),
                    toArgb(accent.primaryContainer, if (isDark) 42 else 78),
                    surfaceBase
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(cardRect, washPaint)

        // Subtle off-corner scalloped watermark for expressive depth
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = toArgb(accent.primary, if (isDark) 26 else 22)
        }
        canvas.drawPath(
            buildScallopedPath(w - 58f, 44f, 148f, petals = 8, amplitudeFraction = 0.08f),
            wmPaint
        )

        // 2. Top-left organic badge + service name + phase pill
        val isBlackout = cycleStatus.phase == CyclePhase.BLACKOUT
        val badgeBmp = renderServiceBadgeBitmap(
            context = context,
            serviceName = sub.name,
            colorHex = sub.colorHex,
            shape = if (isBlackout) NotificationBadgeShape.SUNNY_12 else NotificationBadgeShape.COOKIE_9,
            isDark = isDark,
            launcherIcon = launcherIcon,
            sizePx = 112
        )
        canvas.drawBitmap(badgeBmp, 38f, 38f, null)
        badgeBmp.recycle()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = displayTf
            textSize = 44f
        }
        val clippedName = if (sub.name.length > 18) sub.name.take(17) + "…" else sub.name
        canvas.drawText(clippedName, 174f, 88f, titlePaint)

        val phasePillText = when {
            isBlackout -> "BREAK COMPLETE · READY"
            cycleStatus.daysRemainingInPhase <= 0 -> "CANCEL TODAY"
            cycleStatus.daysRemainingInPhase == 1 -> "CANCEL IN 1 DAY"
            else -> "ACTIVE · ${cycleStatus.daysRemainingInPhase}d LEFT"
        }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.onPrimaryContainer)
            typeface = bodyBoldTf
            textSize = 22f
            letterSpacing = 0.05f
        }
        val pillTextW = pillTextPaint.measureText(phasePillText)
        val pillRect = RectF(174f, 106f, 174f + pillTextW + 34f, 148f)
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.primaryContainer, if (isDark) 225 else 240)
        }
        canvas.drawRoundRect(pillRect, 22f, 22f, pillBgPaint)
        val pillFm = pillTextPaint.fontMetrics
        canvas.drawText(
            phasePillText,
            pillRect.left + 17f,
            pillRect.centerY() - (pillFm.ascent + pillFm.descent) / 2f,
            pillTextPaint
        )

        val dateLabel = RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            typeface = bodyTf
            textSize = 23f
        }
        canvas.drawText("Target $dateLabel", pillRect.right + 16f, pillRect.centerY() + 8f, datePaint)

        // 3. Top-right tilted 12-petal scalloped savings seal
        val sealAmount = if (isBlackout && cycleStatus.perBreakSavings > 0.5) {
            MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)
        } else {
            MoneyFormatter.format(cycleStatus.annualSavings, sub.currency, maximumFractionDigits = 0)
        }
        val sealTopLabel = if (isBlackout) "BANK" else "SAVE"
        val sealBottomLabel = if (isBlackout) "this break" else "per year"

        val sealCx = w - 122f
        val sealCy = 122f
        val sealR = 78f
        canvas.save()
        canvas.rotate(-6f, sealCx, sealCy)
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = toArgb(accent.primary)
        }
        canvas.drawPath(buildScallopedPath(sealCx, sealCy, sealR, petals = 12, amplitudeFraction = 0.065f), sealPaint)

        val dashRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = toArgb(accent.onPrimary, 95)
            strokeWidth = 2.4f
            pathEffect = DashPathEffect(floatArrayOf(9f, 8f), 0f)
        }
        canvas.drawCircle(sealCx, sealCy, sealR * 0.78f, dashRing)

        val sealSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.onPrimary, 210)
            typeface = bodyBoldTf
            textSize = 17f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        val sealBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.onPrimary)
            typeface = displayTf
            textSize = if (sealAmount.length >= 6) 25f else 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(sealTopLabel, sealCx, sealCy - 22f, sealSmallPaint)
        canvas.drawText(sealAmount, sealCx, sealCy + 10f, sealBigPaint)
        canvas.drawText(sealBottomLabel, sealCx, sealCy + 34f, sealSmallPaint.apply { letterSpacing = 0.02f; textSize = 16f })
        canvas.restore()

        // 4. Bottom 2-zone Wavy Active + 45° Hatched Break Rhythm Track
        val trackLeft = 42f
        val trackRight = w - 42f
        val trackY = 244f
        val trackW = trackRight - trackLeft
        val activeDays = sub.activeDays.coerceAtLeast(1)
        val breakDays = sub.blackoutDays.coerceAtLeast(0)
        val totalDays = (activeDays + breakDays).coerceAtLeast(1)
        val gapPx = if (breakDays > 0) 18f else 0f
        val activeZoneW = if (breakDays > 0) {
            ((trackW - gapPx) * (activeDays.toFloat() / totalDays)).coerceIn(trackW * 0.45f, trackW * 0.78f)
        } else {
            trackW
        }
        val breakLeft = trackLeft + activeZoneW + gapPx
        val breakW = (trackRight - breakLeft).coerceAtLeast(0f)

        // Inactive active-zone rail
        val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF2D3337.toInt() else 0xFFDCE2DF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(trackLeft + 7f, trackY, trackLeft + activeZoneW - 7f, trackY, railPaint)

        // Filled wavy active progress
        val progress = if (isBlackout) 1f else cycleStatus.progressFraction.coerceIn(0.08f, 1f)
        val filledEndX = trackLeft + 7f + (activeZoneW - 14f) * progress
        val wavePath = Path().apply {
            val startX = trackLeft + 7f
            val span = (filledEndX - startX).coerceAtLeast(1f)
            val segments = 36
            val amp = 5.5f
            for (i in 0..segments) {
                val t = i.toFloat() / segments
                val x = startX + span * t
                val y = trackY + sin(x * 0.085f) * amp
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.primary)
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(wavePath, wavePaint)

        // Glowing bead at active progress tip
        val beadX = if (isBlackout) breakLeft + breakW * cycleStatus.progressFraction.coerceIn(0.15f, 0.95f) else filledEndX
        val beadY = if (isBlackout) trackY else trackY + sin(filledEndX * 0.085f) * 5.5f
        if (!isBlackout) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary, 75) }
            canvas.drawCircle(beadX, beadY, 17f, haloPaint)
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary) }
            canvas.drawCircle(beadX, beadY, 9.5f, dotPaint)
        }

        // 45° diagonal hatched break pill
        if (breakW > 20f) {
            val breakRect = RectF(breakLeft, trackY - 12f, trackRight, trackY + 12f)
            val breakPath = Path().apply { addRoundRect(breakRect, 12f, 12f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(breakPath)
            val breakBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) 0xFF252D33.toInt() else 0xFFE3E9EE.toInt()
            }
            canvas.drawRect(breakRect, breakBg)
            val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBlackout) toArgb(accent.primary, 160) else (if (isDark) 0x55B3C3CF else 0x554C5D6B)
                strokeWidth = 4.2f
            }
            var hx = breakRect.left - 30f
            while (hx < breakRect.right + 30f) {
                canvas.drawLine(hx, breakRect.bottom + 6f, hx + 26f, breakRect.top - 6f, hatchPaint)
                hx += 16f
            }
            canvas.restore()
            if (isBlackout) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary) }
                canvas.drawCircle(beadX, trackY, 10f, dotPaint)
            }
        }

        // Track captions
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            typeface = bodyBoldTf
            textSize = 21f
        }
        canvas.drawText("${activeDays}d active", trackLeft, 298f, capPaint)
        if (breakDays > 0) {
            capPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${breakDays}d break (not billed)", trackRight, 298f, capPaint)
        }

        canvas.restore()

        // Outer hairline border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = hairline
            strokeWidth = 2f
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        return bitmap
    }

    /**
     * Renders a crisp `960×230` 2-step progress banner for the ongoing Subscribe/Unsubscribe notification.
     */
    fun renderActionProgressBannerBitmap(
        context: Context,
        action: PendingSubscriptionAction,
        serviceColorHex: String?,
        launcherIcon: AppLauncherIcon,
        isDark: Boolean
    ): Bitmap {
        val w = 960
        val h = 230
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accent = launcherAccentTokensFor(launcherIcon, isDark)
        val displayTf = loadDisplayTypeface(context)
        val bodyBoldTf = loadBodyTypeface(context, bold = true)
        val isUnsub = action.actionType == SubscriptionActionProgressType.UNSUBSCRIBE

        val surfaceBase = if (isDark) 0xFF171B1D.toInt() else 0xFFFAFBFA.toInt()
        val textPrimary = if (isDark) 0xFFECEFEE.toInt() else 0xFF151918.toInt()
        val textSecondary = if (isDark) 0xFF9AA4A0.toInt() else 0xFF5A6360.toInt()
        val cardRect = RectF(6f, 6f, w - 6f, h - 6f)
        val cardPath = Path().apply { addRoundRect(cardRect, 40f, 40f, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(cardPath)
        canvas.drawColor(surfaceBase)

        val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                toArgb(accent.primaryContainer, if (isDark) 90 else 135),
                surfaceBase,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(cardRect, wash)

        // Left organic badge
        val badge = renderServiceBadgeBitmap(
            context = context,
            serviceName = action.subscriptionName,
            colorHex = serviceColorHex,
            shape = NotificationBadgeShape.SQUIRCLE,
            isDark = isDark,
            launcherIcon = launcherIcon,
            sizePx = 112
        )
        canvas.drawBitmap(badge, 36f, (h - 112f) / 2f, null)
        badge.recycle()

        // Step 1 active pill
        val step1Rect = RectF(176f, 46f, 548f, 184f)
        val step1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary) }
        canvas.drawRoundRect(step1Rect, 30f, 30f, step1Paint)

        val step1LabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.onPrimary, 200)
            typeface = bodyBoldTf
            textSize = 20f
            letterSpacing = 0.08f
        }
        val step1TitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.onPrimary)
            typeface = displayTf
            textSize = 28f
        }
        canvas.drawText("STEP 1 · IN PROGRESS", step1Rect.left + 26f, step1Rect.top + 48f, step1LabelPaint)
        val verb = if (isUnsub) "Cancel on" else "Subscribe on"
        val shortName = if (action.subscriptionName.length > 12) action.subscriptionName.take(11) + "…" else action.subscriptionName
        canvas.drawText("$verb $shortName", step1Rect.left + 26f, step1Rect.top + 96f, step1TitlePaint)

        // Dotted connector between Step 1 & Step 2
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(accent.primary, 180)
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
        }
        canvas.drawLine(step1Rect.right + 12f, h / 2f, 608f, h / 2f, dashPaint)

        // Step 2 waiting pill
        val step2Rect = RectF(616f, 46f, w - 36f, 184f)
        val step2Fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF23292D.toInt() else 0xFFECEFED.toInt()
        }
        canvas.drawRoundRect(step2Rect, 30f, 30f, step2Fill)
        val step2Border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = toArgb(accent.primary, 110)
            strokeWidth = 3f
        }
        canvas.drawRoundRect(step2Rect, 30f, 30f, step2Border)

        val step2Eyebrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            typeface = bodyBoldTf
            textSize = 20f
            letterSpacing = 0.08f
        }
        val step2Title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = displayTf
            textSize = 28f
        }
        canvas.drawText("STEP 2 · CONFIRM", step2Rect.left + 26f, step2Rect.top + 48f, step2Eyebrow)
        canvas.drawText("Tap Completed", step2Rect.left + 26f, step2Rect.top + 96f, step2Title)

        canvas.restore()
        return bitmap
    }
}
