package com.subtrack.core.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.subtrack.core.ui.R
import com.subtrack.core.ui.theme.BrandDarkBackground
import com.subtrack.core.ui.theme.BrandDarkHeroContainer
import com.subtrack.core.ui.theme.BrandLightBackground
import com.subtrack.core.ui.theme.BrandLightHeroContainer
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.HeroMoodPalette
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay

/**
 * Full-screen Material 3 Expressive editorial backdrop:
 * keeps main surfaces cleanly neutral (~70%) with subtle sculptural silk waves
 * peeking through the top mask, while giving Welcome / Reveal screens an emphasized
 * Botanical Emerald atmospheric wash.
 */
@Composable
fun ExpressiveScreenBackdrop(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val baseColor = if (isDark) BrandDarkBackground else BrandLightBackground
    val imageAlpha = when {
        isDark && emphasized -> 0.40f
        isDark -> 0.23f
        emphasized -> 0.48f
        else -> 0.24f
    }
    val maskBrush = if (isDark) {
        if (emphasized) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0E2920).copy(alpha = 0.70f),
                    BrandDarkBackground.copy(alpha = 0.86f),
                    BrandDarkBackground.copy(alpha = 0.95f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF131B19).copy(alpha = 0.74f),
                    BrandDarkBackground.copy(alpha = 0.89f),
                    BrandDarkBackground.copy(alpha = 0.96f)
                )
            )
        }
    } else {
        if (emphasized) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFDDF5E9).copy(alpha = 0.72f),
                    BrandLightBackground.copy(alpha = 0.88f),
                    BrandLightBackground.copy(alpha = 0.96f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEFF3F1).copy(alpha = 0.82f),
                    BrandLightBackground.copy(alpha = 0.92f),
                    BrandLightBackground.copy(alpha = 0.98f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_expressive_silk_waves),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = imageAlpha,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(maskBrush)
        )
        content()
    }
}

/**
 * Editorial duotone Hero Card backdrop:
 * concentrates the signature Botanical Emerald + sculptural silk waves texture
 * inside the Hero Bento centerpiece, optionally accented by an oversized off-corner
 * M3 Expressive polygon watermark silhouette that responds smoothly to scroll parallax.
 */
@Composable
fun ExpressiveHeroCardBackdrop(
    modifier: Modifier = Modifier,
    watermarkPolygon: RoundedPolygon? = ExpressiveMaterialPolygons.Clover8,
    watermarkRotationDegrees: Float = 0f,
    watermarkAlpha: Float = 0.085f,
    heroMood: HeroMoodPalette? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val accentIcon = com.subtrack.core.ui.theme.LocalSubCyclerAccentIcon.current
    val accentDefaults = remember(accentIcon, isDark) {
        com.subtrack.core.ui.theme.launcherAccentTokensFor(accentIcon, isDark)
    }
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion)

    val targetBase = heroMood?.baseColor ?: accentDefaults.heroBase
    val targetStart = heroMood?.gradientStart ?: accentDefaults.heroGradientStart
    val targetMid = heroMood?.gradientMid ?: accentDefaults.heroGradientMid
    val targetEnd = heroMood?.gradientEnd ?: accentDefaults.heroGradientEnd
    val targetWatermark = heroMood?.watermarkTint ?: MaterialTheme.colorScheme.onPrimaryContainer

    val baseColor by animateColorAsState(targetBase, colorSpec, label = "heroMoodBase")
    val gradStart by animateColorAsState(targetStart, colorSpec, label = "heroMoodStart")
    val gradMid by animateColorAsState(targetMid, colorSpec, label = "heroMoodMid")
    val gradEnd by animateColorAsState(targetEnd, colorSpec, label = "heroMoodEnd")
    val watermarkTint by animateColorAsState(targetWatermark, colorSpec, label = "heroMoodWatermark")

    val maskBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                gradStart.copy(alpha = 0.78f),
                gradMid.copy(alpha = 0.84f),
                gradEnd.copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                gradStart.copy(alpha = 0.72f),
                gradMid.copy(alpha = 0.80f),
                gradEnd.copy(alpha = 0.88f)
            )
        )
    }

    Box(
        modifier = modifier.background(baseColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_expressive_silk_waves),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.34f else 0.46f,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(maskBrush)
        )
        if (watermarkPolygon != null && watermarkAlpha > 0.001f) {
            val watermarkShape = remember(watermarkPolygon) { RoundedPolygonShape(watermarkPolygon) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 46.dp, y = (-42).dp)
                    .size(176.dp)
                    .graphicsLayer {
                        rotationZ = watermarkRotationDegrees
                    }
                    .expressivePolygonSurface(
                        shape = watermarkShape,
                        color = watermarkTint.copy(
                            alpha = ExpressiveMotion.clampEffectProgress(watermarkAlpha)
                        )
                    )
            )
        }
        content()
    }
}

/**
 * Subliminal off-corner breathing M3 Expressive polygon backdrop (inspired by BunPod's
 * `ShapeBloomBackdrop`). Kept intentionally low-contrast (`0.055f` Light / `0.075f` Dark)
 * so foreground cards remain effortless to scan, and freezes automatically under reduced motion.
 */
@Composable
fun ShapeBloomBackdrop(
    modifier: Modifier = Modifier,
    from: RoundedPolygon = ExpressiveMaterialPolygons.Clover4,
    to: RoundedPolygon = ExpressiveMaterialPolygons.Cookie7,
    size: Dp = 360.dp,
    restMillis: Long = 3000L,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val reducedMotion = rememberReducedMotionEnabled()
    val alpha = if (isDark) 0.075f else 0.055f
    val morph = remember(from, to) { Morph(from, to) }
    val progress = remember { Animatable(0f) }
    var forward by remember { mutableStateOf(true) }

    LaunchedEffect(reducedMotion, from, to) {
        if (reducedMotion) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            delay(restMillis)
            progress.animateTo(
                targetValue = if (forward) 1f else 0f,
                animationSpec = ExpressiveMotion.expressiveSpatialSlowFloat()
            )
            forward = !forward
        }
    }

    val shape = MorphPolygonShape(morph = morph, percentage = progress.value.coerceIn(0f, 1f))
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-140).dp, y = 170.dp)
                .size(size)
                .graphicsLayer {
                    rotationZ = progress.value * 14f
                }
                .expressivePolygonSurface(
                    shape = shape,
                    color = color.copy(alpha = alpha)
                )
        )
    }
}

/**
 * Returns `true` when the runtime Android graphics pipeline supports low-overhead offscreen
 * `BlendMode.DstIn` alpha masking cleanly (Android 10 / API 29+ SkiaGL/Vulkan).
 * On Android 8.0–9 (API 26–28) legacy HWUI GL pipelines, a zero-FBO gradient scrim avoids
 * full-screen `saveLayer` frame drops and nested `LazyRow` offscreen clipping artifacts.
 */
internal fun shouldUseOffscreenEdgeFade(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.Q

/**
 * 6-stop `BlendMode.DstIn` progressive edge fade on Android 10+ (API 29+), with an automatic
 * zero-offscreen gradient scrim fallback on Android 8.0–9 (API 26–28) for smooth scrolling on
 * legacy HWUI GL pipelines.
 */
@Composable
fun Modifier.progressiveEdgeFade(
    topFadeHeight: Dp = 0.dp,
    bottomFadeHeight: Dp = 0.dp,
    fallbackScrimColor: Color = MaterialTheme.colorScheme.background
): Modifier {
    if (!shouldUseOffscreenEdgeFade(Build.VERSION.SDK_INT)) {
        return this.drawWithContent {
            drawContent()
            val topPx = topFadeHeight.toPx().coerceIn(0f, size.height * 0.45f)
            if (topPx > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to fallbackScrimColor.copy(alpha = 0.92f),
                        0.55f to fallbackScrimColor.copy(alpha = 0.42f),
                        1.0f to Color.Transparent,
                        startY = 0f,
                        endY = topPx
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, topPx)
                )
            }
            val bottomPx = bottomFadeHeight.toPx().coerceIn(0f, size.height * 0.45f)
            if (bottomPx > 1f) {
                val startY = size.height - bottomPx
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to fallbackScrimColor.copy(alpha = 0.42f),
                        1.0f to fallbackScrimColor.copy(alpha = 0.92f),
                        startY = startY,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, startY),
                    size = Size(size.width, bottomPx)
                )
            }
        }
    }

    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val topPx = topFadeHeight.toPx().coerceIn(0f, size.height * 0.45f)
            if (topPx > 1f) {
                val endStop = (topPx / size.height).coerceIn(0.01f, 0.45f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        endStop * 0.25f to Color.Black.copy(alpha = 0.15f),
                        endStop * 0.50f to Color.Black.copy(alpha = 0.40f),
                        endStop * 0.75f to Color.Black.copy(alpha = 0.72f),
                        endStop to Color.Black,
                        1.0f to Color.Black
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            val bottomPx = bottomFadeHeight.toPx().coerceIn(0f, size.height * 0.45f)
            if (bottomPx > 1f) {
                val startStop = (1f - bottomPx / size.height).coerceIn(0.55f, 0.99f)
                val span = 1f - startStop
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black,
                        startStop to Color.Black,
                        startStop + span * 0.25f to Color.Black.copy(alpha = 0.72f),
                        startStop + span * 0.50f to Color.Black.copy(alpha = 0.40f),
                        startStop + span * 0.75f to Color.Black.copy(alpha = 0.15f),
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
