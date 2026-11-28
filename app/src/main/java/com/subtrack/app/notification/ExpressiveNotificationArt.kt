package com.subtrack.app.notification

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.ui.R as CoreUiR
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.LauncherAccentTokens
import com.subtrack.core.ui.theme.launcherAccentTokensFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class NotificationBadgeShape {
    COOKIE_9,
    SUNNY_12,
    SQUIRCLE
}

/**
 * Pure offline Android Canvas accent renderer for SubCycler's Hybrid Native notifications:
 * - Organic harmonized monogram `LargeIcon` avatars (`COOKIE_9`, `SUNNY_12`, `SQUIRCLE`).
 * - Transparent-background slim Material 3 Expressive curly rhythm & 2-step progress strips
 *   embedded directly inside `NotificationCompat.DecoratedCustomViewStyle` so all text, card
 *   surfaces, and inline actions remain 100% native Android UI.
 */
object ExpressiveNotificationArt {

    fun resolveDarkTheme(context: Context, themeMode: AppThemeMode): Boolean = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> resolveSystemDarkTheme(context)
    }

    /**
     * Resolves Android's live system shade Dark/Light mode (independent of any in-app theme override)
     * so transparent notification strips always maintain high contrast on the system shade.
     */
    fun resolveSystemDarkTheme(context: Context): Boolean {
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMask == Configuration.UI_MODE_NIGHT_YES
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
     * Renders a crisp transparent organic service monogram badge for `NotificationCompat.setLargeIcon`
     * and compact native widget rows.
     */
    fun renderServiceBadgeBitmap(
        context: Context,
        serviceName: String,
        colorHex: String?,
        shape: NotificationBadgeShape,
        isDark: Boolean,
        launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
        accentOverride: LauncherAccentTokens? = null,
        sizePx: Int = 192
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = serviceName,
            storedColorHex = colorHex,
            isDark = isDark
        )
        val accent = accentOverride ?: launcherAccentTokensFor(launcherIcon, isDark)
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
     * Renders a **100% transparent-background** Material 3 Expressive 2-zone curly rhythm strip
     * (`720×44`) for expanded hybrid cycle notifications — zero card frame, zero rasterized text.
     */
    fun renderTransparentCycleRhythmStripBitmap(
        cycleStatus: CycleStatus,
        launcherIcon: AppLauncherIcon,
        isSystemDark: Boolean,
        accentOverride: LauncherAccentTokens? = null,
        widthPx: Int = 720,
        heightPx: Int = 44
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val sub = cycleStatus.subscription
        val accent = accentOverride ?: launcherAccentTokensFor(launcherIcon, isSystemDark)
        val accentArgb = toArgb(accent.primary)
        val isBlackout = cycleStatus.phase == CyclePhase.BLACKOUT

        val centerY = heightPx / 2f
        val strokeW = 10f
        val halfStroke = strokeW / 2f
        val leftPad = halfStroke + 2f
        val rightPad = widthPx - halfStroke - 2f
        val totalSpan = (rightPad - leftPad).coerceAtLeast(1f)

        val activeDays = sub.activeDays.coerceAtLeast(1)
        val breakDays = if (sub.isRotating) sub.blackoutDays.coerceAtLeast(0) else 0
        val hasBreak = breakDays > 0
        val gapPx = if (hasBreak) 18f else 0f
        val activeZoneW = if (hasBreak) {
            ((totalSpan - gapPx) * (activeDays.toFloat() / (activeDays + breakDays)))
                .coerceIn(totalSpan * 0.44f, totalSpan * 0.78f)
        } else {
            totalSpan
        }
        val activeZoneEnd = leftPad + activeZoneW
        val breakLeft = activeZoneEnd + gapPx
        val breakW = (rightPad - breakLeft).coerceAtLeast(0f)

        val railColor = if (isSystemDark) 0x38FFFFFF else 0x24151918
        val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = railColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(leftPad, centerY, activeZoneEnd, centerY, railPaint)

        // Curly Material 3 Expressive active wave
        val activeProgress = if (isBlackout) 1f else cycleStatus.progressFraction.coerceIn(0.10f, 1f)
        val filledEndX = leftPad + (activeZoneEnd - leftPad) * activeProgress
        val amp = 7.2f
        val wavelength = 48f
        val rampPx = 18f
        val wavePath = Path()
        var x = leftPad
        var first = true
        while (x <= filledEndX) {
            val distStart = x - leftPad
            val distEnd = filledEndX - x
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
        wavePath.lineTo(filledEndX, centerY)

        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlackout) toArgb(accent.primary, 130) else accentArgb
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(wavePath, wavePaint)

        if (!isBlackout) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary, 65) }
            canvas.drawCircle(filledEndX, centerY, 15f, haloPaint)
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
            canvas.drawCircle(filledEndX, centerY, 8.5f, dotPaint)
        }

        // 45° hatched Break capsule
        if (hasBreak && breakW > 18f) {
            val pillHalfH = 7.5f
            val breakRect = RectF(breakLeft, centerY - pillHalfH, rightPad, centerY + pillHalfH)
            val breakPath = Path().apply { addRoundRect(breakRect, pillHalfH, pillHalfH, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(breakPath)
            val breakBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSystemDark) 0x29B3C3CF else 0x264C5D6B
            }
            canvas.drawRect(breakRect, breakBg)
            val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBlackout) toArgb(accent.primary, 185) else (if (isSystemDark) 0x66B3C3CF else 0x554C5D6B)
                strokeWidth = 3.2f
            }
            var hx = breakRect.left - 24f
            while (hx < breakRect.right + 24f) {
                canvas.drawLine(hx, breakRect.bottom + 4f, hx + 20f, breakRect.top - 4f, hatchPaint)
                hx += 13f
            }
            canvas.restore()

            if (isBlackout) {
                val beadX = breakRect.left + breakRect.width() * cycleStatus.progressFraction.coerceIn(0.14f, 0.94f)
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = toArgb(accent.primary, 65) }
                canvas.drawCircle(beadX, centerY, 14f, haloPaint)
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
                canvas.drawCircle(beadX, centerY, 8f, dotPaint)
            }
        }

        return bitmap
    }

    /**
     * Renders a **100% transparent-background** Material 3 Expressive 2-step curly progress strip
     * (`720×40`) for the ongoing Subscribe / Unsubscribe notification — zero card box, zero text.
     */
    fun renderTransparentActionProgressStripBitmap(
        launcherIcon: AppLauncherIcon,
        isSystemDark: Boolean,
        accentOverride: LauncherAccentTokens? = null,
        stepFraction: Float = 0.5f,
        widthPx: Int = 720,
        heightPx: Int = 40
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accent = accentOverride ?: launcherAccentTokensFor(launcherIcon, isSystemDark)
        val accentArgb = toArgb(accent.primary)
        val trackColor = if (isSystemDark) 0x38FFFFFF else 0x26151918

        val centerY = heightPx / 2f
        val strokeW = 9.5f
        val halfStroke = strokeW / 2f
        val startX = halfStroke + 2f
        val endX = widthPx - halfStroke - 2f
        val gap = 16f
        val clamped = stepFraction.coerceIn(0.18f, 0.85f)
        val waveEndX = startX + (endX - startX - gap) * clamped
        val trackStartX = waveEndX + gap

        // Step 1 active curly wave
        val amp = 6.8f
        val wavelength = 46f
        val rampPx = 16f
        val wavePath = Path()
        var x = startX
        var first = true
        while (x <= waveEndX) {
            val distStart = x - startX
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
            color = accentArgb
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(wavePath, wavePaint)

        // Step 2 calm track + stop dot
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(trackStartX, centerY, endX - 6f, centerY, trackPaint)

        val endDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
        canvas.drawCircle(endX - 4f, centerY, 4.5f, endDotPaint)

        return bitmap
    }
}
