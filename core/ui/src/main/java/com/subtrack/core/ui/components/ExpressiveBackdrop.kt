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

private val GrayscaleSilkColorFilter: ColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

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
    val accentTokens = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
    val imageAlpha = when {
        isDark && emphasized -> 0.24f
        emphasized -> 0.28f
        else -> 0.0f
    }
    val maskBrush = if (emphasized) {
        Brush.verticalGradient(
            colors = listOf(
                accentTokens.heroBase.copy(alpha = if (isDark) 0.34f else 0.16f),
                baseColor.copy(alpha = 0.90f),
                baseColor
            )
        )
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        if (imageAlpha > 0.01f) {
            Image(
                painter = painterResource(id = R.drawable.bg_expressive_silk_waves),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = imageAlpha,
                colorFilter = GrayscaleSilkColorFilter,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (maskBrush != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(maskBrush)
            )
        }
        content()
    }
}

/**
 * Pure Flat Color-Block Material 3 Expressive Hero Card backdrop:
 * uses a single confident flat jewel color (zero multi-brand rainbow gradient, zero photo haze),
 * accented purely by crisp tone-on-tone geometric M3 Expressive polygon silhouettes (`Clover8`
 * top-right with scroll-parallax rotation + subtle bottom-left `Pebble` depth block).
 */
@Composable
fun ExpressiveHeroCardBackdrop(
    modifier: Modifier = Modifier,
    watermarkPolygon: RoundedPolygon? = ExpressiveMaterialPolygons.Clover8,
    watermarkRotationDegrees: Float = 0f,
    watermarkAlpha: Float = 0.11f,
    heroMood: HeroMoodPalette? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val accentDefaults = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion)

    val targetBase = heroMood?.baseColor ?: accentDefaults.heroBase
    val targetWatermark = heroMood?.watermarkTint ?: Color.White

    val baseColor by animateColorAsState(targetBase, colorSpec, label = "heroFlatBlockBase")
    val watermarkTint by animateColorAsState(targetWatermark, colorSpec, label = "heroFlatBlockWatermark")
    val lowerShape = remember { RoundedPolygonShape(ExpressiveMaterialPolygons.Pebble) }
    val effectiveWatermarkAlpha = if (isDark) {
        watermarkAlpha.coerceIn(0f, 0.11f)
    } else {
        (watermarkAlpha * 1.18f).coerceIn(0f, 0.15f)
    }

    Box(
        modifier = modifier.background(baseColor)
    ) {
        // Subtle bottom-left dark graphic depth block (tone-on-tone flat geometry)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-42).dp, y = 46.dp)
                .size(134.dp)
                .graphicsLayer {
                    rotationZ = 20f
                }
                .expressivePolygonSurface(
                    shape = lowerShape,
                    color = Color.Black.copy(alpha = if (isDark) 0.15f else 0.09f)
                )
        )
        if (watermarkPolygon != null && effectiveWatermarkAlpha > 0.001f) {
            val watermarkShape = remember(watermarkPolygon) { RoundedPolygonShape(watermarkPolygon) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 42.dp, y = (-40).dp)
                    .size(176.dp)
                    .graphicsLayer {
                        rotationZ = watermarkRotationDegrees
                    }
                    .expressivePolygonSurface(
                        shape = watermarkShape,
                        color = watermarkTint.copy(
                            alpha = ExpressiveMotion.clampEffectProgress(effectiveWatermarkAlpha)
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
