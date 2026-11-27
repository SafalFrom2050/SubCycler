package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Two-Beat Physical Card Removal (inspired by BunPod's `RemovalCollapse`):
 * - **Beat 1 (Implode)**: Shrinks & fades the card inside its slot on `StandardSpatialFast`.
 * - **Beat 2 (Neighbor Collision)**: Snaps the empty slot shut on `ExpressiveSpatialDefault`;
 *   negative overshoot clamps to `0` (collision impact) while positive rebound briefly
 *   bounces neighboring rows apart by a few pixels before firing [onRemoved].
 */
@Composable
fun ExpressiveRemovalCollapse(
    removing: Boolean,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val popAnim = remember { Animatable(1f) }
    val slotAnim = remember { Animatable(1f) }
    var callbackFired by remember { mutableStateOf(false) }

    LaunchedEffect(removing, reducedMotion) {
        if (!removing) {
            callbackFired = false
            if (reducedMotion) {
                slotAnim.snapTo(1f)
                popAnim.snapTo(1f)
            } else {
                if (slotAnim.value < 1f) {
                    slotAnim.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
                }
                if (popAnim.value < 1f) {
                    popAnim.animateTo(1f, ExpressiveMotion.expressiveSpatialFastFloat())
                }
            }
            return@LaunchedEffect
        }

        if (reducedMotion) {
            popAnim.snapTo(0f)
            slotAnim.snapTo(0f)
            if (!callbackFired) {
                callbackFired = true
                onRemoved()
            }
            return@LaunchedEffect
        }

        // Beat 1: Implode in place on calm fast spring
        popAnim.animateTo(
            targetValue = 0f,
            animationSpec = ExpressiveMotion.standardSpatialFastFloat()
        )
        // Beat 2: Snap slot shut on expressive spatial default spring (with rebound!)
        slotAnim.animateTo(
            targetValue = 0f,
            animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
        )
        if (!callbackFired) {
            callbackFired = true
            onRemoved()
        }
    }

    val clampedPop = ExpressiveMotion.clampEffectProgress(popAnim.value)
    val heightFactor = slotAnim.value.coerceAtLeast(0f)

    Box(
        modifier = modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val targetHeight = (placeable.height * heightFactor).roundToInt().coerceAtLeast(0)
                layout(placeable.width, targetHeight) {
                    val yOffset = (targetHeight - placeable.height) / 2
                    placeable.placeRelative(0, yOffset)
                }
            }
            .graphicsLayer {
                scaleX = clampedPop
                scaleY = clampedPop
                alpha = clampedPop
            }
    ) {
        content()
    }
}

/**
 * Staggered Spring Row Arrival (inspired by BunPod's `FeedArrival`):
 * Springs newly arrived rows into the feed with a `120ms` cascade and light selection haptic.
 */
@Composable
fun ExpressiveStaggeredArrival(
    order: Int?,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = false,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val shouldAnimate = order != null && !reducedMotion
    val progress = remember { Animatable(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(order, reducedMotion) {
        if (!shouldAnimate) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        val cappedOrder = (order ?: 0).coerceIn(0, 6)
        delay(120L + 110L * cappedOrder)
        if (enableHaptic && cappedOrder <= 3) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
        )
    }

    val raw = progress.value
    val clamped = ExpressiveMotion.clampEffectProgress(raw)
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = clamped
                val s = 0.88f + 0.12f * raw
                scaleX = s
                scaleY = s
                translationY = (1f - raw) * 28f
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
    ) {
        content()
    }
}

/**
 * Spring-Fanned Overlapping Ringed Badge Deck (inspired by BunPod's `OpmlShapeStack`):
 * Displays up to [maxVisible] overlapping organic service monograms framed in crisp surface
 * cutout rings that fan out horizontally and untilt smoothly on entry!
 */
@Composable
fun FannedBadgeStack(
    serviceNames: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 5,
    badgeSize: Dp = 48.dp,
    step: Dp = 32.dp,
    ringWidth: Dp = 3.dp,
    ringColor: Color = MaterialTheme.colorScheme.background
) {
    if (serviceNames.isEmpty()) return
    val reducedMotion = rememberReducedMotionEnabled()
    val visible = serviceNames.take(maxVisible)
    val overflow = (serviceNames.size - visible.size).coerceAtLeast(0)
    val totalSlots = visible.size + if (overflow > 0) 1 else 0

    val fanProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(serviceNames.size, reducedMotion) {
        if (reducedMotion) {
            fanProgress.snapTo(1f)
        } else {
            delay(100L)
            fanProgress.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
            )
        }
    }

    val t = fanProgress.value
    val clampedT = ExpressiveMotion.clampEffectProgress(t)
    val totalWidth = badgeSize + step * (totalSlots - 1).coerceAtLeast(0)
    val outerSize = badgeSize + ringWidth * 2

    Box(
        modifier = modifier
            .width(totalWidth + ringWidth * 2)
            .height(outerSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // Draw back-to-front so index 0 rests cleanly on top (or overflow badge on trailing edge)
        if (overflow > 0) {
            val idx = visible.size
            val xOffset = step * idx * t
            val tilt = (1f - clampedT) * 9f
            Box(
                modifier = Modifier
                    .offset(x = xOffset)
                    .size(outerSize)
                    .graphicsLayer { rotationZ = tilt }
                    .clip(ExpressiveCookie7Shape)
                    .background(ringColor)
                    .padding(ringWidth)
                    .clip(ExpressiveCookie7Shape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        for (i in visible.lastIndex downTo 0) {
            val serviceName = visible[i]
            val (shapeKind, _) = ExpressivePalettes.styleForService(serviceName)
            val ringShape = remember(shapeKind) { RoundedPolygonShape(shapeKind.toMaterialPolygon()) }
            val xOffset = step * i * t
            val tilt = (1f - clampedT) * ((i % 5) - 2) * 7.5f

            Box(
                modifier = Modifier
                    .offset(x = xOffset)
                    .size(outerSize)
                    .graphicsLayer { rotationZ = tilt }
                    .clip(ringShape)
                    .background(ringColor)
                    .padding(ringWidth),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveServiceBadge(
                    serviceName = serviceName,
                    size = badgeSize,
                    fontSize = (badgeSize.value * 0.30f).coerceIn(10f, 15f).sp
                )
            }
        }
    }
}
