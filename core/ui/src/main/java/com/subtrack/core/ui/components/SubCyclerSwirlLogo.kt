package com.subtrack.core.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
 * Pure Compose draw-phase caching guarantees razor-sharp vector geometry at locked 90/120Hz
 * with zero per-frame `Path` allocations or recompositions during rotation and color morphs
 * on Android 8.0–16 (API 26–36).
 */
@Composable
fun SubCyclerSwirlLogo(
    modifier: Modifier = Modifier,
    colors: SwirlLogoColors = BrandColorHarmonizer.DefaultBotanicalSwirl,
    size: Dp = 34.dp,
    cornerRadius: Dp = 10.dp,
    rotationDegrees: Float = 0f,
    rotationDegreesProvider: (() -> Float)? = null,
    contentDescriptionText: String? = "SubCycler logo"
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion)

    // Keep animated colors as State<Color> and read exclusively in draw phase so color morphs
    // trigger zero recompositions.
    val lightPetalState = animateColorAsState(colors.lightPetal, colorSpec, label = "swirlLightPetal")
    val midPetalState = animateColorAsState(colors.midPetal, colorSpec, label = "swirlMidPetal")
    val deepPetalState = animateColorAsState(colors.deepPetal, colorSpec, label = "swirlDeepPetal")
    val backdropState = animateColorAsState(colors.backdrop, colorSpec, label = "swirlBackdrop")
    val centerGlyphState = animateColorAsState(colors.centerGlyph, colorSpec, label = "swirlGlyph")
    val waveAccentState = animateColorAsState(colors.waveAccent, colorSpec, label = "swirlWaveAccent")

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind { drawRect(backdropState.value) }
            .then(
                if (contentDescriptionText != null) {
                    Modifier.semantics { contentDescription = contentDescriptionText }
                } else {
                    Modifier
                }
            )
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotationDegreesProvider?.invoke() ?: rotationDegrees
                }
                .drawWithCache {
                    val s = this.size.minDimension
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val center = Offset(cx, cy)

                    val petalPath = Path().apply {
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
                    val seamStroke = Stroke(
                        width = (s * 0.022f).coerceAtLeast(0.9f),
                        join = StrokeJoin.Round
                    )
                    val discRadius = s * 0.20f

                    val frameW = s * 0.215f
                    val frameH = s * 0.158f
                    val frameTopLeft = Offset(cx - frameW / 2f, cy - frameH / 2f + s * 0.014f)
                    val frameSize = Size(frameW, frameH)
                    val frameCorner = CornerRadius(s * 0.052f, s * 0.052f)
                    val strokeW = (s * 0.038f).coerceAtLeast(1.4f)
                    val frameStroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val waveStroke = Stroke(width = strokeW * 0.88f, cap = StrokeCap.Round)

                    val lugRadius = (s * 0.023f).coerceAtLeast(1.0f)
                    val lugY = frameTopLeft.y - s * 0.021f
                    val leftLug = Offset(cx - frameW * 0.24f, lugY)
                    val rightLug = Offset(cx + frameW * 0.24f, lugY)

                    val waveY = frameTopLeft.y + frameH * 0.52f
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

                    onDrawBehind {
                        val backdropColor = backdropState.value
                        val centerGlyphColor = centerGlyphState.value

                        // Back-to-front 120° rotational symmetry with crisp cut-line separation
                        rotate(degrees = 120f, pivot = center) {
                            drawPath(path = petalPath, color = deepPetalState.value)
                        }
                        rotate(degrees = 240f, pivot = center) {
                            drawPath(path = petalPath, color = backdropColor, style = seamStroke)
                            drawPath(path = petalPath, color = midPetalState.value)
                        }
                        drawPath(path = petalPath, color = backdropColor, style = seamStroke)
                        drawPath(path = petalPath, color = lightPetalState.value)

                        // Center dark circular medallion
                        drawCircle(
                            color = backdropColor,
                            radius = discRadius,
                            center = center
                        )

                        // Crisp center calendar-cycle rounded mark + Triadic wavy cycle bar
                        drawCircle(color = centerGlyphColor, radius = lugRadius, center = leftLug)
                        drawCircle(color = centerGlyphColor, radius = lugRadius, center = rightLug)
                        drawRoundRect(
                            color = centerGlyphColor,
                            topLeft = frameTopLeft,
                            size = frameSize,
                            cornerRadius = frameCorner,
                            style = frameStroke
                        )
                        drawPath(
                            path = wavePath,
                            color = waveAccentState.value,
                            style = waveStroke
                        )
                    }
                }
        )
    }
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
