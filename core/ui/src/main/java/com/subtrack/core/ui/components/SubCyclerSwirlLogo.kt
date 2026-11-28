package com.subtrack.core.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.SwirlLogoColors
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled

/**
 * Dynamic Bold Color-Block 3-Petal SubCycler Swirl Logo:
 * Renders 3 interlocking organic teardrop petals with crisp cut-line separation around a deep
 * center medallion and calendar-cycle mark featuring a contrasting Triadic Pop inner wave.
 *
 * Pure Compose Canvas drawing guarantees razor-sharp vector geometry on Android 8.0–16 (API 26–36)
 * without `Outline.Generic` clipping issues.
 */
@Composable
fun SubCyclerSwirlLogo(
    modifier: Modifier = Modifier,
    colors: SwirlLogoColors = BrandColorHarmonizer.DefaultBotanicalSwirl,
    size: Dp = 34.dp,
    cornerRadius: Dp = 10.dp,
    rotationDegrees: Float = 0f,
    contentDescriptionText: String? = "SubCycler logo"
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion)

    val lightPetal by animateColorAsState(colors.lightPetal, colorSpec, label = "swirlLightPetal")
    val midPetal by animateColorAsState(colors.midPetal, colorSpec, label = "swirlMidPetal")
    val deepPetal by animateColorAsState(colors.deepPetal, colorSpec, label = "swirlDeepPetal")
    val backdrop by animateColorAsState(colors.backdrop, colorSpec, label = "swirlBackdrop")
    val centerGlyph by animateColorAsState(colors.centerGlyph, colorSpec, label = "swirlGlyph")
    val waveAccent by animateColorAsState(colors.waveAccent, colorSpec, label = "swirlWaveAccent")

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backdrop)
            .then(
                if (contentDescriptionText != null) {
                    Modifier.semantics { contentDescription = contentDescriptionText }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees }
        ) {
            drawSwirlPetalsAndMark(
                lightPetal = lightPetal,
                midPetal = midPetal,
                deepPetal = deepPetal,
                backdrop = backdrop,
                centerGlyph = centerGlyph,
                waveAccent = waveAccent
            )
        }
    }
}

private fun DrawScope.drawSwirlPetalsAndMark(
    lightPetal: Color,
    midPetal: Color,
    deepPetal: Color,
    backdrop: Color,
    centerGlyph: Color,
    waveAccent: Color
) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val center = Offset(cx, cy)

    fun buildPetalPath(): Path = Path().apply {
        moveTo(cx - s * 0.14f, cy - s * 0.16f)
        cubicTo(
            cx - s * 0.11f, cy - s * 0.37f,
            cx + s * 0.04f, cy - s * 0.42f,
            cx + s * 0.16f, cy - s * 0.33f
        )
        cubicTo(
            cx + s * 0.31f, cy - s * 0.22f,
            cx + s * 0.34f, cy + s * 0.02f,
            cx + s * 0.21f, cy + s * 0.14f
        )
        cubicTo(
            cx + s * 0.11f, cy + s * 0.22f,
            cx - s * 0.05f, cy + s * 0.16f,
            cx - s * 0.12f, cy + s * 0.04f
        )
        cubicTo(
            cx - s * 0.17f, cy - s * 0.04f,
            cx - s * 0.17f, cy - s * 0.11f,
            cx - s * 0.14f, cy - s * 0.16f
        )
        close()
    }

    val petalPath = buildPetalPath()
    val seamStroke = Stroke(width = (s * 0.022f).coerceAtLeast(0.9f), join = StrokeJoin.Round)

    // Back-to-front 120° rotational symmetry with crisp cut-line separation for Bold Color-Block petals
    rotate(degrees = 120f, pivot = center) {
        drawPath(path = petalPath, color = deepPetal)
    }
    rotate(degrees = 240f, pivot = center) {
        drawPath(path = petalPath, color = backdrop, style = seamStroke)
        drawPath(path = petalPath, color = midPetal)
    }
    drawPath(path = petalPath, color = backdrop, style = seamStroke)
    drawPath(path = petalPath, color = lightPetal)

    // Center dark circular medallion
    val discRadius = s * 0.20f
    drawCircle(
        color = backdrop,
        radius = discRadius,
        center = center
    )

    // Crisp center calendar-cycle rounded mark (2 top tabs + rounded body + Triadic wavy cycle bar)
    val frameW = s * 0.215f
    val frameH = s * 0.158f
    val frameLeft = cx - frameW / 2f
    val frameTop = cy - frameH / 2f + s * 0.014f
    val strokeW = (s * 0.038f).coerceAtLeast(1.4f)

    val lugRadius = (s * 0.023f).coerceAtLeast(1.0f)
    val lugY = frameTop - s * 0.021f
    drawCircle(
        color = centerGlyph,
        radius = lugRadius,
        center = Offset(cx - frameW * 0.24f, lugY)
    )
    drawCircle(
        color = centerGlyph,
        radius = lugRadius,
        center = Offset(cx + frameW * 0.24f, lugY)
    )

    drawRoundRect(
        color = centerGlyph,
        topLeft = Offset(frameLeft, frameTop),
        size = Size(frameW, frameH),
        cornerRadius = CornerRadius(s * 0.052f, s * 0.052f),
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val waveY = frameTop + frameH * 0.52f
    val waveHalfW = frameW * 0.25f
    val waveAmp = frameH * 0.12f
    val wavePath = Path().apply {
        moveTo(cx - waveHalfW, waveY + waveAmp * 0.4f)
        cubicTo(
            cx - waveHalfW * 0.35f, waveY - waveAmp,
            cx + waveHalfW * 0.35f, waveY + waveAmp,
            cx + waveHalfW, waveY - waveAmp * 0.4f
        )
    }
    drawPath(
        path = wavePath,
        color = waveAccent,
        style = Stroke(width = strokeW * 0.88f, cap = StrokeCap.Round)
    )
}

/**
 * Android [android.graphics.Canvas] renderer mirroring [SubCyclerSwirlLogo] so exported
 * `1080×1350` QR Stack Card PNGs and home-screen widgets showcase the user's personalized swirl.
 */
fun drawSubCyclerSwirlLogoOnCanvas(
    canvas: android.graphics.Canvas,
    bounds: RectF,
    colors: SwirlLogoColors = BrandColorHarmonizer.DefaultBotanicalSwirl,
    cornerRadiusPx: Float = bounds.width() * 0.28f
) {
    val s = minOf(bounds.width(), bounds.height())
    val cx = bounds.centerX()
    val cy = bounds.centerY()

    fun toArgb(color: Color): Int =
        0xFF000000.toInt() or BrandColorHarmonizer.composeColorToRgb(color)

    val clipPath = android.graphics.Path().apply {
        addRoundRect(bounds, cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(clipPath)

    val backdropArgb = toArgb(colors.backdrop)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = backdropArgb
        strokeWidth = (s * 0.022f).coerceAtLeast(1.2f)
        strokeJoin = Paint.Join.ROUND
    }
    fillPaint.color = backdropArgb
    canvas.drawRect(bounds, fillPaint)

    val petalPath = android.graphics.Path().apply {
        moveTo(cx - s * 0.14f, cy - s * 0.16f)
        cubicTo(
            cx - s * 0.11f, cy - s * 0.37f,
            cx + s * 0.04f, cy - s * 0.42f,
            cx + s * 0.16f, cy - s * 0.33f
        )
        cubicTo(
            cx + s * 0.31f, cy - s * 0.22f,
            cx + s * 0.34f, cy + s * 0.02f,
            cx + s * 0.21f, cy + s * 0.14f
        )
        cubicTo(
            cx + s * 0.11f, cy + s * 0.22f,
            cx - s * 0.05f, cy + s * 0.16f,
            cx - s * 0.12f, cy + s * 0.04f
        )
        cubicTo(
            cx - s * 0.17f, cy - s * 0.04f,
            cx - s * 0.17f, cy - s * 0.11f,
            cx - s * 0.14f, cy - s * 0.16f
        )
        close()
    }

    // 120° deep petal
    canvas.save()
    canvas.rotate(120f, cx, cy)
    fillPaint.color = toArgb(colors.deepPetal)
    canvas.drawPath(petalPath, fillPaint)
    canvas.restore()

    // 240° mid petal + cut-line
    canvas.save()
    canvas.rotate(240f, cx, cy)
    canvas.drawPath(petalPath, seamPaint)
    fillPaint.color = toArgb(colors.midPetal)
    canvas.drawPath(petalPath, fillPaint)
    canvas.restore()

    // 0° light petal + cut-line
    canvas.drawPath(petalPath, seamPaint)
    fillPaint.color = toArgb(colors.lightPetal)
    canvas.drawPath(petalPath, fillPaint)

    // Center medallion
    fillPaint.color = backdropArgb
    canvas.drawCircle(cx, cy, s * 0.20f, fillPaint)

    // Center calendar-cycle mark
    val glyphArgb = toArgb(colors.centerGlyph)
    val waveArgb = toArgb(colors.waveAccent)
    val frameW = s * 0.215f
    val frameH = s * 0.158f
    val frameLeft = cx - frameW / 2f
    val frameTop = cy - frameH / 2f + s * 0.014f
    val strokeW = (s * 0.038f).coerceAtLeast(1.8f)

    fillPaint.color = glyphArgb
    val lugRadius = (s * 0.023f).coerceAtLeast(1.2f)
    val lugY = frameTop - s * 0.021f
    canvas.drawCircle(cx - frameW * 0.24f, lugY, lugRadius, fillPaint)
    canvas.drawCircle(cx + frameW * 0.24f, lugY, lugRadius, fillPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = glyphArgb
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawRoundRect(
        RectF(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH),
        s * 0.052f,
        s * 0.052f,
        strokePaint
    )

    val waveY = frameTop + frameH * 0.52f
    val waveHalfW = frameW * 0.25f
    val waveAmp = frameH * 0.12f
    val wavePath = android.graphics.Path().apply {
        moveTo(cx - waveHalfW, waveY + waveAmp * 0.4f)
        cubicTo(
            cx - waveHalfW * 0.35f, waveY - waveAmp,
            cx + waveHalfW * 0.35f, waveY + waveAmp,
            cx + waveHalfW, waveY - waveAmp * 0.4f
        )
    }
    strokePaint.color = waveArgb
    strokePaint.strokeWidth = strokeW * 0.88f
    canvas.drawPath(wavePath, strokePaint)

    canvas.restore()
}
