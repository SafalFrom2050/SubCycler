package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single floating pill notification dealt into [ExpressiveSnackHost].
 */
data class ExpressiveSnackMessage(
    val id: Long,
    val message: String,
    val actionLabel: String? = null,
    val serviceNameForBadge: String? = null,
    val icon: ImageVector? = Icons.Default.Check,
    val durationMillis: Long = 3800L,
    val dedupeKey: String = message,
    val onAction: (() -> Unit)? = null
)

/**
 * Computes the 3D card-stack transform metrics (peek offset, scale step-down, and
 * progressive shadow shade) for a pill at [depth] (`0` = frontmost pill).
 */
data class SnackDepthVisuals(
    val peekDp: Float,
    val scale: Float,
    val shadeAlpha: Float
)

fun computeSnackDepthVisuals(depth: Float): SnackDepthVisuals {
    val safeDepth = depth.coerceAtLeast(0f)
    return SnackDepthVisuals(
        peekDp = safeDepth * 12f,
        scale = (1f - safeDepth * 0.05f).coerceIn(0.80f, 1f),
        shadeAlpha = (safeDepth * 0.15f).coerceIn(0f, 0.45f)
    )
}

/**
 * State holder for BunPod-inspired 3D stacked floating stadium pill snacks (`expressive_snack`):
 * - Deals rapid messages into a 3D depth stack (up to 4 simultaneous pills).
 * - Deduplicates repeated messages by firing a real spring-velocity horizontal wobble instead of spam.
 * - Supports finger-hold timer pause and velocity-aware swipe-down dismissal.
 */
@Stable
class ExpressiveSnackHostState {
    private var nextId by mutableLongStateOf(1L)
    val snacks = mutableStateListOf<ExpressiveSnackMessage>()
    internal val dismissingIds = mutableStateMapOf<Long, Boolean>()
    internal val shakeTriggers = mutableStateMapOf<Long, Int>()

    fun show(
        message: String,
        actionLabel: String? = null,
        serviceNameForBadge: String? = null,
        icon: ImageVector? = Icons.Default.Check,
        durationMillis: Long = 3800L,
        dedupeKey: String = message,
        onAction: (() -> Unit)? = null
    ): Long {
        val existing = snacks.lastOrNull {
            it.dedupeKey == dedupeKey && dismissingIds[it.id] != true
        }
        if (existing != null) {
            shakeTriggers[existing.id] = (shakeTriggers[existing.id] ?: 0) + 1
            return existing.id
        }

        val id = nextId++
        if (snacks.size >= 4) {
            val oldest = snacks.firstOrNull { dismissingIds[it.id] != true }
            if (oldest != null) beginDismiss(oldest.id)
        }
        snacks.add(
            ExpressiveSnackMessage(
                id = id,
                message = message,
                actionLabel = actionLabel,
                serviceNameForBadge = serviceNameForBadge,
                icon = icon,
                durationMillis = durationMillis,
                dedupeKey = dedupeKey,
                onAction = onAction
            )
        )
        return id
    }

    fun beginDismiss(id: Long) {
        if (dismissingIds[id] == true) return
        dismissingIds[id] = true
    }

    internal fun finalizeRemove(id: Long) {
        snacks.removeAll { it.id == id }
        dismissingIds.remove(id)
        shakeTriggers.remove(id)
    }
}

@Composable
fun rememberExpressiveSnackHostState(): ExpressiveSnackHostState =
    remember { ExpressiveSnackHostState() }

/**
 * 3D Stacked Floating Stadium Snack Host hovering cleanly above SubCycler's floating dock.
 * Inspired by BunPod's `expressive_snack` package.
 */
@Composable
fun ExpressiveSnackHost(
    hostState: ExpressiveSnackHostState,
    modifier: Modifier = Modifier
) {
    if (hostState.snacks.isEmpty()) return

    // Deal depths newest-first; dismissing pills don't claim a depth slot so pills behind step forward.
    var nextDepth = 0
    val depths = HashMap<Long, Int>(hostState.snacks.size)
    for (i in hostState.snacks.lastIndex downTo 0) {
        val snack = hostState.snacks[i]
        val isDismissing = hostState.dismissingIds[snack.id] == true
        depths[snack.id] = if (isDismissing) nextDepth else nextDepth++
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        hostState.snacks.forEach { snack ->
            androidx.compose.runtime.key(snack.id) {
                ExpressiveSnackPillView(
                    snack = snack,
                    depth = depths[snack.id] ?: 0,
                    isDismissing = hostState.dismissingIds[snack.id] == true,
                    shakeTrigger = hostState.shakeTriggers[snack.id] ?: 0,
                    onBeginDismiss = { hostState.beginDismiss(snack.id) },
                    onFinalizeRemove = { hostState.finalizeRemove(snack.id) }
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSnackPillView(
    snack: ExpressiveSnackMessage,
    depth: Int,
    isDismissing: Boolean,
    shakeTrigger: Int,
    onBeginDismiss: () -> Unit,
    onFinalizeRemove: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val reducedMotion = rememberReducedMotionEnabled()
    val scope = rememberCoroutineScope()

    val travelPx = with(density) { 140.dp.toPx() }
    val dismissThresholdPx = with(density) { 24.dp.toPx() }
    val shakeVelocityPx = with(density) { 600.dp.toPx() }

    var isHolding by remember { mutableStateOf(false) }
    val entryAnim = remember { Animatable(if (reducedMotion) 0f else 1f) }
    val dragOffset = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(0f) }

    val animatedDepth by animateFloatAsState(
        targetValue = depth.toFloat(),
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "snackDepth"
    )

    // Entry spring
    LaunchedEffect(snack.id, reducedMotion) {
        if (reducedMotion) {
            entryAnim.snapTo(0f)
        } else {
            entryAnim.animateTo(
                targetValue = 0f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
    }

    // Auto-dismiss countdown (pauses while finger holds the pill; resets on duplicate shake!)
    LaunchedEffect(snack.id, isHolding, shakeTrigger, isDismissing) {
        if (isDismissing || isHolding) return@LaunchedEffect
        delay(snack.durationMillis)
        onBeginDismiss()
    }

    // Duplicate message physics-impulse wobble
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger <= 0 || isDismissing) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (!reducedMotion) {
            shakeOffset.animateTo(
                targetValue = 0f,
                initialVelocity = shakeVelocityPx,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
    }

    // Exit spring -> remove from state once settled
    LaunchedEffect(isDismissing, reducedMotion) {
        if (!isDismissing) return@LaunchedEffect
        if (reducedMotion) {
            entryAnim.snapTo(1f)
        } else {
            entryAnim.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.standardSpatialFastFloat()
            )
        }
        onFinalizeRemove()
    }

    val depthVisuals = computeSnackDepthVisuals(animatedDepth)
    val peekPx = with(density) { depthVisuals.peekDp.dp.toPx() }
    val clampedExit = ExpressiveMotion.clampEffectProgress(entryAnim.value)
    val shadowColor = MaterialTheme.colorScheme.scrim

    val archShape = remember(layoutDirection) {
        RoundedPolygonShape(
            if (layoutDirection == LayoutDirection.Ltr) {
                ExpressiveMaterialPolygons.ArchLeft
            } else {
                ExpressiveMaterialPolygons.ArchRight
            }
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = shakeOffset.value
                translationY = entryAnim.value * travelPx + dragOffset.value - peekPx
                scaleX = depthVisuals.scale
                scaleY = depthVisuals.scale
                transformOrigin = TransformOrigin(0.5f, 1f)
                alpha = if (isDismissing) 1f - clampedExit else 1f
            }
            .pointerInput(snack.id, isDismissing) {
                if (isDismissing) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        isHolding = true
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val next = (dragOffset.value + dragAmount).coerceIn(0f, travelPx * 1.6f)
                            dragOffset.snapTo(next)
                        }
                    },
                    onDragCancel = {
                        isHolding = false
                        scope.launch {
                            dragOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = ExpressiveMotion.standardSpatialFastFloat()
                                    .orSnapIfReduced(reducedMotion)
                            )
                        }
                    },
                    onDragEnd = {
                        isHolding = false
                        if (dragOffset.value > dismissThresholdPx) {
                            onBeginDismiss()
                        } else {
                            scope.launch {
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = ExpressiveMotion.standardSpatialFastFloat()
                                        .orSnapIfReduced(reducedMotion)
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onBeginDismiss()
                }
        ) {
            Box {
                Row(
                    modifier = Modifier.padding(
                        start = if (snack.icon != null || snack.serviceNameForBadge != null) 8.dp else 20.dp,
                        top = 8.dp,
                        end = if (snack.actionLabel != null) 8.dp else 20.dp,
                        bottom = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        snack.serviceNameForBadge != null -> {
                            ExpressiveServiceBadge(
                                serviceName = snack.serviceNameForBadge,
                                size = 32.dp,
                                fontSize = 11.sp
                            )
                        }
                        snack.icon != null -> {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .expressivePolygonSurface(
                                        shape = archShape,
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = snack.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.inverseSurface,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = snack.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )

                    if (snack.actionLabel != null && snack.onAction != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                snack.onAction.invoke()
                                onBeginDismiss()
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.inversePrimary
                        ) {
                            Text(
                                text = snack.actionLabel,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Progressive depth darkening veil so receding stacked pills stay legible & separated
                if (depthVisuals.shadeAlpha > 0.005f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                shadowColor.copy(alpha = depthVisuals.shadeAlpha),
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
    }
}
