package com.subtrack.core.ui.components

import android.graphics.Matrix
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure-math polygon rotation helper (works identically on Android and JVM unit tests).
 */
fun RoundedPolygon.rotated(degrees: Float): RoundedPolygon =
    ExpressiveMaterialPolygons.rotate(this, degrees)

/**
 * Material 3 Expressive v4.0 Normalized Cubic-Bézier Polygon Library backed by
 * Jetpack `androidx.graphics:graphics-shapes` (100% offline, ~35 KB math library).
 */
object ExpressiveMaterialPolygons {
    internal fun rotate(polygon: RoundedPolygon, degrees: Float): RoundedPolygon {
        if (degrees == 0f) return polygon
        val rad = Math.toRadians(degrees.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        return polygon.transformed { x, y ->
            TransformResult(x * cosA - y * sinA, x * sinA + y * cosA)
        }
    }

    val Circle: RoundedPolygon = RoundedPolygon.circle(numVertices = 10).normalized()

    val Square: RoundedPolygon = RoundedPolygon.rectangle(
        width = 1f,
        height = 1f,
        rounding = CornerRounding(0.18f, 0.25f)
    ).normalized()

    val Squircle: RoundedPolygon = RoundedPolygon.rectangle(
        width = 1f,
        height = 1f,
        rounding = CornerRounding(0.32f, 0.5f)
    ).normalized()

    val Cookie7: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 7,
        radius = 1f,
        innerRadius = 0.78f,
        rounding = CornerRounding(0.24f),
        innerRounding = CornerRounding(0.24f)
    ).normalized()

    val Cookie8: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        radius = 1f,
        innerRadius = 0.80f,
        rounding = CornerRounding(0.22f),
        innerRounding = CornerRounding(0.22f)
    ).normalized()

    val Cookie9: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        radius = 1f,
        innerRadius = 0.82f,
        rounding = CornerRounding(0.20f),
        innerRounding = CornerRounding(0.20f)
    ).normalized()

    val Cookie12: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        radius = 1f,
        innerRadius = 0.86f,
        rounding = CornerRounding(0.16f),
        innerRounding = CornerRounding(0.16f)
    ).normalized()

    val Clover4: RoundedPolygon = rotate(
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            radius = 1f,
            innerRadius = 0.56f,
            rounding = CornerRounding(0.34f),
            innerRounding = CornerRounding(0.22f)
        ),
        45f
    ).normalized()

    val Clover8: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        radius = 1f,
        innerRadius = 0.68f,
        rounding = CornerRounding(0.26f),
        innerRounding = CornerRounding(0.18f)
    ).normalized()

    val Sunny: RoundedPolygon = rotate(
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(0.16f),
            innerRounding = CornerRounding(0.16f)
        ),
        22.5f
    ).normalized()

    val SoftBurst: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 10,
        radius = 1f,
        innerRadius = 0.74f,
        rounding = CornerRounding(0.18f),
        innerRounding = CornerRounding(0.14f)
    ).normalized()

    // Vertex order of RoundedPolygon.rectangle: bottom-right, bottom-left, top-left, top-right
    val Arch: RoundedPolygon = RoundedPolygon.rectangle(
        width = 1f,
        height = 1f,
        perVertexRounding = listOf(
            CornerRounding(0.18f),
            CornerRounding(0.18f),
            CornerRounding(0.50f),
            CornerRounding(0.50f)
        )
    ).normalized()

    /** Dome facing Left (for LTR leading stadium snack icon chips). */
    val ArchLeft: RoundedPolygon = rotate(Arch, -90f).normalized()

    /** Dome facing Right (for RTL leading stadium snack icon chips). */
    val ArchRight: RoundedPolygon = rotate(Arch, 90f).normalized()

    val Pebble: RoundedPolygon = RoundedPolygon.rectangle(
        width = 1f,
        height = 1f,
        perVertexRounding = listOf(
            CornerRounding(0.46f),
            CornerRounding(0.22f),
            CornerRounding(0.46f),
            CornerRounding(0.22f)
        )
    ).normalized()

    val Gem: RoundedPolygon = rotate(
        RoundedPolygon(
            numVertices = 6,
            radius = 1f,
            rounding = CornerRounding(0.26f, 0.35f)
        ),
        -90f
    ).normalized()

    val Pentagon: RoundedPolygon = rotate(
        RoundedPolygon(
            numVertices = 5,
            radius = 1f,
            rounding = CornerRounding(0.24f, 0.30f)
        ),
        -90f
    ).normalized()

    val Flower: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        radius = 1f,
        innerRadius = 0.62f,
        rounding = CornerRounding(0.32f),
        innerRounding = CornerRounding(0.16f)
    ).normalized()

    val Bun: RoundedPolygon = RoundedPolygon.pillStar(
        width = 1f,
        height = 1.08f,
        numVerticesPerRadius = 6,
        innerRadiusRatio = 0.78f,
        rounding = CornerRounding(0.28f),
        innerRounding = CornerRounding(0.22f)
    ).normalized()

    val Puffy: RoundedPolygon = rotate(
        RoundedPolygon.star(
            numVerticesPerRadius = 5,
            radius = 1f,
            innerRadius = 0.76f,
            rounding = CornerRounding(0.32f),
            innerRounding = CornerRounding(0.24f)
        ),
        -90f
    ).normalized()

    /** 10 distinct silhouettes used by the Onboarding Vault Mosaic Wall. */
    val mosaicPalette: List<RoundedPolygon> = listOf(
        Cookie7,
        Clover4,
        Sunny,
        Cookie9,
        Gem,
        SoftBurst,
        Cookie12,
        Pentagon,
        Puffy,
        Flower
    )
}

/**
 * Compose [Shape] backed by a normalized [RoundedPolygon].
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private val rotationDegrees: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val androidPath = polygon.toPath()
        val matrix = Matrix()
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees, 0.5f, 0.5f)
        }
        matrix.postScale(size.width, size.height)
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

/**
 * Compose [Shape] interpolating topologically between two [RoundedPolygon] geometries
 * via Jetpack's [Morph] engine (`percentage` clamped into `0f..1f`).
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotationDegrees: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val androidPath = morph.toPath(progress = percentage.coerceIn(0f, 1f))
        val matrix = Matrix()
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees, 0.5f, 0.5f)
        }
        matrix.postScale(size.width, size.height)
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

/**
 * Renders any Material 3 Expressive [Shape] (including non-convex [RoundedPolygonShape] and
 * [MorphPolygonShape] cubic-Bézier paths) directly via [drawOutline] (`Canvas.drawPath`)
 * **without** calling `Modifier.clip(shape)`.
 *
 * Backward-compatibility rationale (API 26–28):
 * On Android 8.0–8.1 legacy HWUI GL pipelines (and pre-API 30 non-convex `Outline.Generic`),
 * `Modifier.clip(Outline.Generic(path))` falls back to hardware stencil `clipPath()` which drops
 * the entire clipped layer (0 pixels rendered). Painting fill and optional stroke directly via
 * [drawOutline] preserves pixel-identical Material 3 Expressive silhouettes and smooth live
 * morphing across every Android version from API 26 through API 36+.
 */
fun Modifier.expressivePolygonSurface(
    shape: Shape,
    color: Color,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline = outline, color = color)
    if (borderColor != null && borderWidth > 0.dp) {
        drawOutline(
            outline = outline,
            color = borderColor,
            style = Stroke(width = borderWidth.toPx())
        )
    }
}

/**
 * Smoothly morphs a Compose [Shape] whenever [targetPolygon] changes, driven by
 * [ExpressiveMotion.expressiveSpatialFastFloat] (or snapping instantly under reduced motion).
 */
@Composable
fun rememberMorphingPolygonShape(
    targetPolygon: RoundedPolygon,
    rotationDegrees: Float = 0f
): Shape {
    val reducedMotion = rememberReducedMotionEnabled()
    var fromPolygon by remember { mutableStateOf(targetPolygon) }
    var toPolygon by remember { mutableStateOf(targetPolygon) }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(targetPolygon, reducedMotion) {
        if (targetPolygon === toPolygon) return@LaunchedEffect
        fromPolygon = toPolygon
        toPolygon = targetPolygon
        if (reducedMotion) {
            morphProgress.snapTo(1f)
        } else {
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
    }

    val morph = remember(fromPolygon, toPolygon) {
        if (fromPolygon === toPolygon) null else Morph(fromPolygon, toPolygon)
    }
    return if (morph == null || morphProgress.value >= 0.999f) {
        remember(toPolygon, rotationDegrees) { RoundedPolygonShape(toPolygon, rotationDegrees) }
    } else {
        MorphPolygonShape(
            morph = morph,
            percentage = morphProgress.value.coerceIn(0f, 1f),
            rotationDegrees = rotationDegrees
        )
    }
}

/**
 * Legacy polar harmonic shape retained for backwards compatibility where direct groove
 * modulation is desired; v4.0 shapes prefer [RoundedPolygonShape] / [MorphPolygonShape].
 */
class ScallopedCookieShape(
    private val lobes: Int = 8,
    private val grooveDepthFraction: Float = 0.09f,
    private val phaseOffsetRadians: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = min(cx, cy)
        val steps = 120

        for (i in 0 until steps) {
            val theta = (2.0 * PI * i / steps).toFloat()
            val wave = (1f - cos(lobes * (theta - phaseOffsetRadians))) / 2f
            val r = maxRadius * (1f - grooveDepthFraction * wave)
            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

val ExpressiveCookie7Shape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Cookie7)
val ExpressiveCookie8Shape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Cookie8)
val ExpressiveCookie9Shape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Cookie9)
val ExpressiveCookie12Shape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Cookie12)
val ExpressiveClover4Shape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Clover4)
val ExpressiveSunnyShape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Sunny)
val ExpressiveGemShape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Gem)
val ExpressiveArchShape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Arch)
val ExpressivePebbleShape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Pebble)
val ExpressiveSquircleShape: Shape = RoundedPolygonShape(ExpressiveMaterialPolygons.Squircle)

enum class ExpressiveBadgeShapeKind {
    COOKIE,
    CLOVER,
    PEBBLE,
    ARCH,
    SQUIRCLE,
    CIRCLE
}

fun ExpressiveBadgeShapeKind.toMaterialPolygon(): RoundedPolygon = when (this) {
    ExpressiveBadgeShapeKind.COOKIE -> ExpressiveMaterialPolygons.Cookie9
    ExpressiveBadgeShapeKind.CLOVER -> ExpressiveMaterialPolygons.Clover4
    ExpressiveBadgeShapeKind.PEBBLE -> ExpressiveMaterialPolygons.Pebble
    ExpressiveBadgeShapeKind.ARCH -> ExpressiveMaterialPolygons.Arch
    ExpressiveBadgeShapeKind.SQUIRCLE -> ExpressiveMaterialPolygons.Squircle
    ExpressiveBadgeShapeKind.CIRCLE -> ExpressiveMaterialPolygons.Circle
}

fun ExpressiveBadgeShapeKind.toComposeShape(): Shape = when (this) {
    ExpressiveBadgeShapeKind.COOKIE -> ExpressiveCookie9Shape
    ExpressiveBadgeShapeKind.CLOVER -> ExpressiveClover4Shape
    ExpressiveBadgeShapeKind.PEBBLE -> ExpressivePebbleShape
    ExpressiveBadgeShapeKind.ARCH -> ExpressiveArchShape
    ExpressiveBadgeShapeKind.SQUIRCLE -> ExpressiveSquircleShape
    ExpressiveBadgeShapeKind.CIRCLE -> CircleShape
}

/**
 * Semantic lifecycle states that drive topological polygon morphing on [ExpressiveServiceBadge].
 */
enum class ServiceBadgeLifecycleState {
    ACTIVE,
    URGENT_DUE,
    COASTING_CANCELLED,
    BLACKOUT_REST,
    UNBANKED_READY,
    INCOMPLETE
}

/**
 * Architectural hairline border shared across Light & Dark cards so Dark sheets and cards
 * retain crisp Material 3 Expressive edge definition without introducing heavy strokes.
 */
@Composable
fun expressiveCardBorder(alphaOverride: Float? = null): BorderStroke {
    val isDark = LocalSubCyclerDarkTheme.current
    val alpha = alphaOverride ?: if (isDark) 0.36f else 0.55f
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
}

/**
 * Subtle 45-degree diagonal hatch pattern used consistently across the entire app
 * to visually distinguish "unbilled rest / break" windows from active billing spans.
 */
fun Modifier.diagonalHatchPattern(
    color: Color,
    spacing: Dp = 7.dp,
    strokeWidth: Dp = 1.dp
): Modifier = drawWithContent {
    clipRect {
        val spacingPx = spacing.toPx().coerceAtLeast(3f)
        val strokePx = strokeWidth.toPx().coerceAtLeast(0.8f)
        val span = size.width + size.height
        var offset = -size.height
        while (offset <= span) {
            drawLine(
                color = color,
                start = Offset(offset, size.height),
                end = Offset(offset + size.height, 0f),
                strokeWidth = strokePx
            )
            offset += spacingPx
        }
    }
    drawContent()
}

/**
 * Positional corner geometry helper for BunPod-inspired Connected Card Stacks (`MenuSection`):
 * Outer caps curve generously (`28dp`), inner seams sit crisp (`4dp`), and pressing a row
 * springs its inner seams outward (`14dp`).
 */
data class PositionalSlabRadii(
    val topRadius: Dp,
    val bottomRadius: Dp
)

fun computeSectionSlabRadii(
    index: Int,
    count: Int,
    isPressed: Boolean = false,
    outerRadius: Dp = 28.dp,
    innerRadius: Dp = 4.dp,
    pressedInnerRadius: Dp = 14.dp
): PositionalSlabRadii {
    val safeCount = count.coerceAtLeast(1)
    val safeIndex = index.coerceIn(0, safeCount - 1)
    val effectiveInner = if (isPressed) pressedInnerRadius else innerRadius
    val top = if (safeIndex == 0) outerRadius else effectiveInner
    val bottom = if (safeIndex == safeCount - 1) outerRadius else effectiveInner
    return PositionalSlabRadii(topRadius = top, bottomRadius = bottom)
}

/**
 * Material 3 Expressive v4.0 Connected Section Stack (`28dp` outer caps, `4dp` inner seams,
 * `2dp` architectural reveal gap). Replaces repetitive standalone cards in Settings,
 * Savings Breakdown, and Billing Destinations.
 */
@Composable
fun ExpressiveSectionStack(
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    tileGap: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tileGap)
    ) {
        if (!eyebrow.isNullOrBlank()) {
            ExpressiveEyebrow(
                text = eyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )
        }
        content()
    }
}

/**
 * Single positional tile inside an [ExpressiveSectionStack]. Automatically assigns `28dp` outer
 * corners and `4dp` inner seams that bloom to `14dp` when pressed (when [onClick] is supplied).
 */
@Composable
fun ExpressiveSectionRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    outerRadius: Dp = 28.dp,
    innerRadius: Dp = 4.dp,
    border: BorderStroke? = expressiveCardBorder(if (LocalSubCyclerDarkTheme.current) 0.28f else 0.48f),
    content: @Composable BoxScope.() -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val target = computeSectionSlabRadii(
        index = index,
        count = count,
        isPressed = isPressed && onClick != null,
        outerRadius = outerRadius,
        innerRadius = innerRadius
    )
    val topRadius by animateDpAsState(
        targetValue = target.topRadius,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "sectionRowTopRadius"
    )
    val bottomRadius by animateDpAsState(
        targetValue = target.bottomRadius,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "sectionRowBottomRadius"
    )
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth(), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/**
 * BunPod-inspired spring-stretching stadium progress dots:
 * active step stretches from `8dp` to `28dp` on [ExpressiveMotion.expressiveSpatialFastDp].
 */
@Composable
fun ExpressiveStepDots(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    passedColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    if (totalSteps <= 1) return
    val reducedMotion = rememberReducedMotionEnabled()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isCurrent = index == currentStep
            val isPassed = index < currentStep
            val width by animateDpAsState(
                targetValue = if (isCurrent) 28.dp else 8.dp,
                animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
                label = "stepDotWidth"
            )
            val color by animateColorAsState(
                targetValue = when {
                    isCurrent -> activeColor
                    isPassed -> passedColor
                    else -> inactiveColor
                },
                animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
                label = "stepDotColor"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Android 16 Material 3 Expressive Connected Segmented Button Group:
 * Outer ends hold generous pill radii (22dp), inner seams tighten to 6dp with a crisp 3dp gap,
 * and the selected (or pressed) segment smoothly morphs all 4 corners with spring physics!
 */
data class ExpressiveSegmentOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

@Composable
fun <T> ExpressiveSegmentedGroup(
    options: List<ExpressiveSegmentOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    fontSize: TextUnit = 12.5.sp
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val reducedMotion = rememberReducedMotionEnabled()
    val outerRadius = 22.dp
    val innerSeamRadius = 6.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option.value == selectedValue
            val isFirst = index == 0
            val isLast = index == options.lastIndex

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val targetStartRadius = when {
                isSelected -> outerRadius
                isPressed -> 13.dp
                isFirst -> outerRadius
                else -> innerSeamRadius
            }
            val targetEndRadius = when {
                isSelected -> outerRadius
                isPressed -> 13.dp
                isLast -> outerRadius
                else -> innerSeamRadius
            }

            val startRadius by animateDpAsState(
                targetValue = targetStartRadius,
                animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
                label = "segStartRadius"
            )
            val endRadius by animateDpAsState(
                targetValue = targetEndRadius,
                animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
                label = "segEndRadius"
            )
            val segScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
                label = "segScale"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
                label = "segBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
                label = "segContent"
            )

            val shape = RoundedCornerShape(
                topStart = startRadius,
                bottomStart = startRadius,
                topEnd = endRadius,
                bottomEnd = endRadius
            )
            val border = when {
                isSelected && isDark -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                !isSelected && !isDark -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                else -> null
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(option.value)
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = height)
                    .graphicsLayer {
                        scaleX = segScale
                        scaleY = segScale
                    },
                shape = shape,
                color = containerColor,
                border = border
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = option.label,
                        fontSize = fontSize,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Balanced 70/30 Editorial Tonal Palette:
 * blends calm Warm Sand and Cool Slate neutrals alongside signature Botanical Emerald
 * & Cool Jade shades so the emerald hero accent never overwhelms the screen.
 */
data class ExpressiveTonalPalette(
    val container: Color,
    val selectedContainer: Color,
    val border: Color,
    val content: Color,
    val accent: Color
)

object ExpressivePalettes {
    // Light Mode — Calm Editorial Neutrals + Botanical Emerald Shades
    private val LightFreshMint = ExpressiveTonalPalette(
        container = Color(0xFFD2F2E3),
        selectedContainer = Color(0xFFB7E9D2),
        border = Color(0x550C6B4E),
        content = Color(0xFF053324),
        accent = Color(0xFF0C6B4E)
    )
    private val LightCoolJade = ExpressiveTonalPalette(
        container = Color(0xFFD6F0ED),
        selectedContainer = Color(0xFFBCE6E1),
        border = Color(0x55196B64),
        content = Color(0xFF073633),
        accent = Color(0xFF196B64)
    )
    private val LightCoolSlate = ExpressiveTonalPalette(
        container = Color(0xFFE2E8EE),
        selectedContainer = Color(0xFFD2DCE6),
        border = Color(0x554C5D6B),
        content = Color(0xFF1E2B36),
        accent = Color(0xFF4C5D6B)
    )
    private val LightWarmSand = ExpressiveTonalPalette(
        container = Color(0xFFE7E0D0),
        selectedContainer = Color(0xFFD9CFBA),
        border = Color(0x666E6552),
        content = Color(0xFF332D22),
        accent = Color(0xFF6E6552)
    )
    private val LightDeepEmerald = ExpressiveTonalPalette(
        container = Color(0xFF0C6B4E),
        selectedContainer = Color(0xFF08573F),
        border = Color(0x66064A35),
        content = Color(0xFFF0FBF6),
        accent = Color(0xFF0C6B4E)
    )
    // Semantic Alert (strictly for destructive Delete / urgent 24h deadlines)
    private val LightAlertCoral = ExpressiveTonalPalette(
        container = Color(0xFFFFDAD6),
        selectedContainer = Color(0xFFFFC2BC),
        border = Color(0x55BA1A1A),
        content = Color(0xFF690005),
        accent = Color(0xFFBA1A1A)
    )
    private val LightWarmAmber = ExpressiveTonalPalette(
        container = Color(0xFFF6ECC8),
        selectedContainer = Color(0xFFECDCAA),
        border = Color(0x558B6914),
        content = Color(0xFF4F3A05),
        accent = Color(0xFF8B6914)
    )

    // Dark Mode — Refined Graphite Neutrals + Luminous Botanical Mint Shades
    private val DarkFreshMint = ExpressiveTonalPalette(
        container = Color(0xFF164636),
        selectedContainer = Color(0xFF1E5C47),
        border = Color(0x557CE6C1),
        content = Color(0xFFCCF7E7),
        accent = Color(0xFF7CE6C1)
    )
    private val DarkCoolJade = ExpressiveTonalPalette(
        container = Color(0xFF164341),
        selectedContainer = Color(0xFF1E5855),
        border = Color(0x5584DBD4),
        content = Color(0xFFC9F5F2),
        accent = Color(0xFF84DBD4)
    )
    private val DarkCoolSlate = ExpressiveTonalPalette(
        container = Color(0xFF273139),
        selectedContainer = Color(0xFF323F49),
        border = Color(0x55B3C3CF),
        content = Color(0xFFDBE5EC),
        accent = Color(0xFFB3C3CF)
    )
    private val DarkWarmSand = ExpressiveTonalPalette(
        container = Color(0xFF34312A),
        selectedContainer = Color(0xFF423F36),
        border = Color(0x55DDD5C3),
        content = Color(0xFFEBE4D6),
        accent = Color(0xFFDDD5C3)
    )
    private val DarkLuminousMint = ExpressiveTonalPalette(
        container = Color(0xFF7CE6C1),
        selectedContainer = Color(0xFF96EED0),
        border = Color(0x66A8F5DA),
        content = Color(0xFF042C1F),
        accent = Color(0xFF7CE6C1)
    )
    private val DarkAlertCoral = ExpressiveTonalPalette(
        container = Color(0xFF5E1D21),
        selectedContainer = Color(0xFF75262B),
        border = Color(0x55FFB4AB),
        content = Color(0xFFFFDAD6),
        accent = Color(0xFFFFB4AB)
    )
    private val DarkWarmAmber = ExpressiveTonalPalette(
        container = Color(0xFF473A1A),
        selectedContainer = Color(0xFF5C4B22),
        border = Color(0x55F2D28B),
        content = Color(0xFFF9E8BE),
        accent = Color(0xFFF2D28B)
    )

    @Composable
    fun softSky(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkFreshMint else LightFreshMint

    @Composable
    fun softOcean(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkCoolJade else LightCoolJade

    @Composable
    fun softSage(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkFreshMint else LightFreshMint

    @Composable
    fun softLavender(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkCoolSlate else LightCoolSlate

    @Composable
    fun softStone(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkWarmSand else LightWarmSand

    @Composable
    fun boldPrimary(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkLuminousMint else LightDeepEmerald

    @Composable
    fun softRose(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkAlertCoral else LightAlertCoral

    @Composable
    fun softAmber(): ExpressiveTonalPalette =
        if (LocalSubCyclerDarkTheme.current) DarkWarmAmber else LightWarmAmber

    private val curatedInitials = mapOf(
        "netflix" to "NF",
        "disney+" to "D+",
        "max (hbo)" to "MX",
        "youtube premium" to "YT",
        "spotify" to "SP",
        "apple music" to "AM",
        "apple tv+" to "TV",
        "amazon prime video" to "PV",
        "chatgpt plus" to "AI",
        "claude pro" to "CL",
        "google ai plus" to "G+",
        "google ai pro" to "GP",
        "google ai ultra" to "GU",
        "notion plus" to "NO",
        "xbox game pass ultimate" to "XB",
        "playstation plus" to "PS",
        "icloud+" to "iC",
        "google one" to "G1"
    )

    fun initialsForService(serviceName: String): String {
        val trimmed = serviceName.trim()
        if (trimmed.isEmpty()) return "+"
        val normalized = trimmed.lowercase(Locale.ROOT)
        curatedInitials[normalized]?.let { return it }
        return when {
            normalized.contains("netflix") -> "NF"
            normalized.contains("disney") -> "D+"
            normalized.contains("prime video") -> "PV"
            normalized.contains("youtube") -> "YT"
            normalized.contains("spotify") -> "SP"
            normalized.contains("chatgpt") -> "AI"
            normalized.contains("claude") -> "CL"
            normalized.contains("icloud") -> "iC"
            normalized.contains("playstation") -> "PS"
            normalized.contains("xbox") -> "XB"
            else -> {
                val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
                when {
                    words.isEmpty() -> "?"
                    words.size == 1 -> words.first().take(2).uppercase(Locale.US)
                    else -> words.take(2).joinToString("") { it.take(1) }.uppercase(Locale.US)
                }
            }
        }
    }

    @Composable
    fun styleForService(serviceName: String): Pair<ExpressiveBadgeShapeKind, ExpressiveTonalPalette> {
        val isDark = LocalSubCyclerDarkTheme.current
        val mint = if (isDark) DarkFreshMint else LightFreshMint
        val jade = if (isDark) DarkCoolJade else LightCoolJade
        val slate = if (isDark) DarkCoolSlate else LightCoolSlate
        val sand = if (isDark) DarkWarmSand else LightWarmSand
        val bold = if (isDark) DarkLuminousMint else LightDeepEmerald
        val allShades = listOf(slate, sand, mint, jade, bold)
        val allShapes = ExpressiveBadgeShapeKind.entries

        val normalized = serviceName.trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("netflix") -> ExpressiveBadgeShapeKind.COOKIE to bold
            normalized.contains("prime") -> ExpressiveBadgeShapeKind.PEBBLE to slate
            normalized.contains("chatgpt") -> ExpressiveBadgeShapeKind.SQUIRCLE to mint
            normalized.contains("disney") -> ExpressiveBadgeShapeKind.PEBBLE to sand
            normalized.contains("google ai") -> ExpressiveBadgeShapeKind.SQUIRCLE to jade
            normalized.contains("notion") -> ExpressiveBadgeShapeKind.CIRCLE to sand
            normalized.contains("spotify") -> ExpressiveBadgeShapeKind.CLOVER to bold
            normalized.contains("youtube") -> ExpressiveBadgeShapeKind.ARCH to slate
            normalized.contains("claude") -> ExpressiveBadgeShapeKind.COOKIE to sand
            normalized.contains("icloud") || normalized.contains("apple") -> ExpressiveBadgeShapeKind.CIRCLE to slate
            normalized.contains("playstation") -> ExpressiveBadgeShapeKind.CLOVER to jade
            normalized.contains("xbox") -> ExpressiveBadgeShapeKind.CLOVER to mint
            normalized.contains("max") || normalized.contains("hbo") -> ExpressiveBadgeShapeKind.ARCH to slate
            normalized.contains("google one") -> ExpressiveBadgeShapeKind.ARCH to jade
            else -> {
                val hash = abs(normalized.hashCode())
                val shape = allShapes[hash % allShapes.size]
                val shade = allShades[(hash / 7) % allShades.size]
                shape to shade
            }
        }
    }
}

/**
 * Resolves the target [RoundedPolygon] for a subscription badge based on its base shape,
 * lifecycle phase, selection state, and anticipatory press-preview state.
 */
fun resolveBadgeTargetPolygon(
    baseShapeKind: ExpressiveBadgeShapeKind,
    lifecycleState: ServiceBadgeLifecycleState,
    isSelected: Boolean,
    isPressed: Boolean
): RoundedPolygon = when {
    isPressed -> when (baseShapeKind) {
        ExpressiveBadgeShapeKind.COOKIE -> ExpressiveMaterialPolygons.Sunny
        ExpressiveBadgeShapeKind.CIRCLE -> ExpressiveMaterialPolygons.Gem
        else -> ExpressiveMaterialPolygons.Cookie7
    }
    lifecycleState == ServiceBadgeLifecycleState.INCOMPLETE -> ExpressiveMaterialPolygons.Square
    lifecycleState == ServiceBadgeLifecycleState.UNBANKED_READY -> ExpressiveMaterialPolygons.Cookie12
    lifecycleState == ServiceBadgeLifecycleState.URGENT_DUE -> ExpressiveMaterialPolygons.SoftBurst
    lifecycleState == ServiceBadgeLifecycleState.COASTING_CANCELLED -> ExpressiveMaterialPolygons.Circle
    lifecycleState == ServiceBadgeLifecycleState.BLACKOUT_REST -> ExpressiveMaterialPolygons.Bun
    isSelected -> ExpressiveMaterialPolygons.Cookie7
    else -> baseShapeKind.toMaterialPolygon()
}

/**
 * Material 3 Expressive v4.0 Topological Morphing Monogram Badge:
 * Morphs seamlessly across [RoundedPolygon] geometries via `androidx.graphics:graphics-shapes`
 * to communicate lifecycle state (*Active*, *Urgent Due*, *Coasting Cancelled*, *Blackout Rest*,
 * *Unbanked Ready*, *Incomplete*) and anticipatory press-preview silhouettes.
 */
@Composable
fun ExpressiveServiceBadge(
    serviceName: String,
    initials: String = ExpressivePalettes.initialsForService(serviceName),
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    fontSize: TextUnit = 13.sp,
    isSelected: Boolean = false,
    isPressed: Boolean = false,
    lifecycleState: ServiceBadgeLifecycleState = ServiceBadgeLifecycleState.ACTIVE
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val reducedMotion = rememberReducedMotionEnabled()
    val (baseShapeKind, basePalette) = ExpressivePalettes.styleForService(serviceName)
    val effectivePalette = when (lifecycleState) {
        ServiceBadgeLifecycleState.BLACKOUT_REST -> ExpressivePalettes.softLavender()
        ServiceBadgeLifecycleState.UNBANKED_READY -> ExpressivePalettes.boldPrimary()
        ServiceBadgeLifecycleState.INCOMPLETE -> ExpressivePalettes.softStone()
        else -> basePalette
    }

    val targetPolygon = resolveBadgeTargetPolygon(
        baseShapeKind = baseShapeKind,
        lifecycleState = lifecycleState,
        isSelected = isSelected,
        isPressed = isPressed
    )
    val activeShape = rememberMorphingPolygonShape(targetPolygon = targetPolygon)

    // Subtle slow breathing pulse on Urgent Due badges (disabled when reduced motion is active)
    val urgentPulse = if (lifecycleState == ServiceBadgeLifecycleState.URGENT_DUE && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "urgentBadgeBreath")
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.055f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "urgentBadgePulseScale"
        ).value
    } else {
        1f
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            isSelected -> 1.06f
            else -> 1.0f
        },
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "badgeScale"
    )
    val rotation by animateFloatAsState(
        targetValue = when {
            isPressed -> -8f
            isSelected -> -5f
            lifecycleState == ServiceBadgeLifecycleState.UNBANKED_READY -> -6f
            else -> 0f
        },
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "badgeRotation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) effectivePalette.selectedContainer else effectivePalette.container,
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "badgeContainer"
    )

    val badgeBorderColor: Color?
    val badgeBorderWidth: Dp
    when {
        lifecycleState == ServiceBadgeLifecycleState.INCOMPLETE -> {
            badgeBorderColor = null
            badgeBorderWidth = 0.dp
        }
        isSelected -> {
            badgeBorderColor = effectivePalette.accent.copy(alpha = 0.55f)
            badgeBorderWidth = 1.5.dp
        }
        !isDark -> {
            badgeBorderColor = effectivePalette.border.copy(alpha = 0.35f)
            badgeBorderWidth = 1.dp
        }
        else -> {
            badgeBorderColor = null
            badgeBorderWidth = 0.dp
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale * urgentPulse
                    scaleY = scale * urgentPulse
                    rotationZ = rotation
                }
                .expressivePolygonSurface(
                    shape = activeShape,
                    color = containerColor,
                    borderColor = badgeBorderColor,
                    borderWidth = badgeBorderWidth
                )
                .then(
                    if (lifecycleState == ServiceBadgeLifecycleState.INCOMPLETE) {
                        Modifier.drawWithContent {
                            drawContent()
                            val inset = 2.dp.toPx()
                            drawRoundRect(
                                color = effectivePalette.accent.copy(alpha = 0.55f),
                                topLeft = Offset(inset, inset),
                                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                style = Stroke(
                                    width = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = effectivePalette.content,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = (-0.5).sp
            )
        }

        // Concentric mini Arch protection seal when auto-renew is cancelled ("Coasting")
        if (lifecycleState == ServiceBadgeLifecycleState.COASTING_CANCELLED && size >= 36.dp) {
            val checkSize = (size.value * 0.36f).coerceIn(14f, 19f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(checkSize)
                    .expressivePolygonSurface(
                        shape = ExpressiveArchShape,
                        color = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.surfaceContainer,
                        borderWidth = 1.5.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Auto-renew cancelled",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((checkSize.value * 0.62f).dp)
                )
            }
        }
    }
}

/**
 * Compact 2-Zone Cycle Rhythm Track for Subscription Bento Cards:
 * Visually splits a rotating subscription's full cycle into [ Active window ] + [ Hatched rest break ]
 * with a clean minimum cap on Day 1 so the bar always looks intentional and informative.
 */
@Composable
fun CycleRhythmTrack(
    progress: Float,
    isActivePhase: Boolean,
    activeDays: Int,
    blackoutDays: Int,
    activeColor: Color,
    pausedColor: Color,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    hatchColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "cycleRhythmProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            }
    ) {
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)

        if (blackoutDays <= 0 || activeDays <= 0) {
            drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
            val fillW = (size.width * animatedProgress).coerceAtLeast(h * 1.4f).coerceAtMost(size.width)
            drawRoundRect(
                color = activeColor,
                size = Size(fillW, h),
                cornerRadius = radius
            )
            return@Canvas
        }

        val gapPx = 3.5.dp.toPx()
        val usableWidth = (size.width - gapPx).coerceAtLeast(1f)
        val totalDays = (activeDays + blackoutDays).toFloat()
        val rawActiveRatio = (activeDays / totalDays).coerceIn(0.32f, 0.78f)
        val activeSpanW = usableWidth * rawActiveRatio
        val breakSpanW = usableWidth - activeSpanW
        val breakStartX = activeSpanW + gapPx

        // 1. Active zone track + fill
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(activeSpanW, h),
            cornerRadius = radius
        )
        if (isActivePhase) {
            val fillW = (activeSpanW * animatedProgress).coerceAtLeast(h * 1.6f).coerceAtMost(activeSpanW)
            drawRoundRect(
                color = activeColor,
                topLeft = Offset.Zero,
                size = Size(fillW, h),
                cornerRadius = radius
            )
        } else {
            // Active window completed; show soft muted emerald fill
            drawRoundRect(
                color = activeColor.copy(alpha = 0.28f),
                topLeft = Offset.Zero,
                size = Size(activeSpanW, h),
                cornerRadius = radius
            )
        }

        // 2. Break / rest zone capsule with 45-degree diagonal hatch texture
        drawRoundRect(
            color = trackColor.copy(alpha = 0.85f),
            topLeft = Offset(breakStartX, 0f),
            size = Size(breakSpanW, h),
            cornerRadius = radius
        )
        clipRect(left = breakStartX, top = 0f, right = breakStartX + breakSpanW, bottom = h) {
            val step = 5.dp.toPx()
            var x = breakStartX - h
            while (x <= breakStartX + breakSpanW + h) {
                drawLine(
                    color = hatchColor,
                    start = Offset(x, h),
                    end = Offset(x + h, 0f),
                    strokeWidth = 1.1.dp.toPx()
                )
                x += step
            }
        }
        if (!isActivePhase) {
            val breakFillW = (breakSpanW * animatedProgress).coerceAtLeast(h * 1.5f).coerceAtMost(breakSpanW)
            drawRoundRect(
                color = pausedColor,
                topLeft = Offset(breakStartX, 0f),
                size = Size(breakFillW, h),
                cornerRadius = radius
            )
        }
    }
}

/**
 * Material 3 Expressive Wavy / Squiggly Progress Indicator with smooth entry ramp-in,
 * animated sinusoidal phase, rounded dot tip, optional proportional hatched break capsule,
 * and v4.0 finger-grab deformation ([grabFraction]: `0f` resting wave → `1f` flattened,
 * thickened track under finger during Cycle Time-Machine scrubbing).
 */
@Composable
fun WavyProgressIndicator(
    progress: Float,
    activeColor: Color,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    animateWave: Boolean = true,
    activeDays: Int = 0,
    blackoutDays: Int = 0,
    isActivePhase: Boolean = true,
    grabFraction: Float = 0f,
    scrubTotalProgress: Float? = null,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "wavyProgress"
    )
    val animatedGrab by animateFloatAsState(
        targetValue = grabFraction.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "wavyGrab"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveDrift")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val currentPhase = if (animateWave && !reducedMotion && animatedGrab < 0.95f) phaseShift else 0f
    val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val breakAccentColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            }
    ) {
        val grab = ExpressiveMotion.clampEffectProgress(animatedGrab)
        val strokeWidth = (3.5f + 2.4f * grab).dp.toPx()
        val amplitude = (3.2f * (1f - grab)).dp.toPx()
        val wavelength = 24.dp.toPx()
        val centerY = size.height / 2f
        val startX = strokeWidth
        val endX = (size.width - strokeWidth).coerceAtLeast(startX)
        val totalSpan = (endX - startX).coerceAtLeast(1f)

        val showBreakZone = activeDays > 0 && blackoutDays > 0
        val gapPx = if (showBreakZone) 8.dp.toPx() else 0f
        val activeRatio = if (showBreakZone) {
            (activeDays.toFloat() / (activeDays + blackoutDays)).coerceIn(0.45f, 0.78f)
        } else {
            1f
        }
        val waveZoneSpan = if (showBreakZone) (totalSpan - gapPx) * activeRatio else totalSpan
        val waveZoneEndX = startX + waveZoneSpan
        val breakStartX = waveZoneEndX + gapPx
        val breakWidth = (endX - breakStartX).coerceAtLeast(10.dp.toPx())

        // Resolve indicator X either from live Time-Machine scrub across full cycle or current phase
        val scrubbedX = scrubTotalProgress?.coerceIn(0f, 1f)?.let { frac ->
            if (!showBreakZone) {
                startX + totalSpan * frac
            } else if (frac <= activeRatio) {
                val localFrac = (frac / activeRatio).coerceIn(0f, 1f)
                startX + waveZoneSpan * localFrac
            } else {
                val breakLocalFrac = ((frac - activeRatio) / (1f - activeRatio).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                breakStartX + breakWidth * breakLocalFrac
            }
        }

        val minLeadPx = (waveZoneSpan * 0.12f).coerceAtLeast(18.dp.toPx()).coerceAtMost(waveZoneSpan)
        val activeEndX = when {
            scrubbedX != null -> scrubbedX.coerceAtMost(waveZoneEndX)
            !isActivePhase && showBreakZone -> waveZoneEndX
            animatedProgress <= 0f -> startX
            else -> (startX + waveZoneSpan * animatedProgress)
                .coerceAtLeast(startX + minLeadPx)
                .coerceAtMost(waveZoneEndX)
        }

        // Remaining active track
        if (activeEndX < waveZoneEndX) {
            drawLine(
                color = trackColor,
                start = Offset(activeEndX, centerY),
                end = Offset(waveZoneEndX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Active sinusoidal wave (flattens & thickens smoothly under finger grab!)
        val activeWaveColor = if (!isActivePhase && showBreakZone && scrubbedX == null) {
            activeColor.copy(alpha = 0.34f)
        } else {
            activeColor
        }
        if (activeEndX > startX + 1f) {
            val wavePath = Path()
            val stepPx = 2f
            val rampPx = 12.dp.toPx()
            var x = startX
            var lastY = centerY
            var first = true
            while (x <= activeEndX) {
                val distFromStart = x - startX
                val distFromEnd = activeEndX - x
                val edgeEnvelope = min(1f, min(distFromStart, distFromEnd) / rampPx).coerceIn(0.25f, 1f)
                val angle = (distFromStart / wavelength) * (2f * PI.toFloat()) - currentPhase
                val y = centerY + amplitude * edgeEnvelope * sin(angle)
                if (first) {
                    wavePath.moveTo(x, y)
                    first = false
                } else {
                    wavePath.lineTo(x, y)
                }
                lastY = y
                x += stepPx
            }
            drawPath(
                path = wavePath,
                color = activeWaveColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (scrubbedX == null && isActivePhase) {
                drawCircle(
                    color = activeColor,
                    radius = (4.4f + 1.8f * grab).dp.toPx(),
                    center = Offset(activeEndX, lastY)
                )
            }
        }

        // Break / Rest zone capsule with 45-degree diagonal hatch texture
        if (showBreakZone) {
            val pillHeight = (8.5f + 2.5f * grab).dp.toPx()
            val pillTop = centerY - pillHeight / 2f
            val corner = CornerRadius(pillHeight / 2f, pillHeight / 2f)
            drawRoundRect(
                color = trackColor.copy(alpha = 0.85f),
                topLeft = Offset(breakStartX, pillTop),
                size = Size(breakWidth, pillHeight),
                cornerRadius = corner
            )
            clipRect(
                left = breakStartX,
                top = pillTop,
                right = breakStartX + breakWidth,
                bottom = pillTop + pillHeight
            ) {
                val hatchStep = 5.dp.toPx()
                var hx = breakStartX - pillHeight
                while (hx <= breakStartX + breakWidth + pillHeight) {
                    drawLine(
                        color = hatchColor,
                        start = Offset(hx, pillTop + pillHeight),
                        end = Offset(hx + pillHeight, pillTop),
                        strokeWidth = 1.1.dp.toPx()
                    )
                    hx += hatchStep
                }
            }

            // Fill inside break capsule during active scrub into break OR while currently paused
            val breakFillWidth = when {
                scrubbedX != null && scrubbedX > breakStartX -> (scrubbedX - breakStartX).coerceIn(0f, breakWidth)
                scrubbedX == null && !isActivePhase -> (breakWidth * animatedProgress).coerceIn(pillHeight, breakWidth)
                else -> 0f
            }
            if (breakFillWidth > 0.5f) {
                drawRoundRect(
                    color = breakAccentColor.copy(alpha = 0.88f),
                    topLeft = Offset(breakStartX, pillTop),
                    size = Size(breakFillWidth, pillHeight),
                    cornerRadius = corner
                )
            }
        }

        // Tactile scrubber thumb handle when dragging Time-Machine across either zone
        if (scrubbedX != null) {
            val inBreak = showBreakZone && scrubbedX >= breakStartX
            val thumbColor = if (inBreak) breakAccentColor else activeColor
            drawCircle(
                color = thumbColor.copy(alpha = 0.24f),
                radius = 10.dp.toPx(),
                center = Offset(scrubbedX, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = 6.dp.toPx(),
                center = Offset(scrubbedX, centerY)
            )
        }
    }
}
