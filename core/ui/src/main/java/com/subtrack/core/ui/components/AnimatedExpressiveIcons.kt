package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Tactile Material 3 Expressive icon motion archetypes that react to touch,
 * presses, selection changes, and user actions using spring physics.
 */
enum class ReactiveMotionType {
    GEAR_SPIN,
    PLUS_TWIST,
    CYCLE_SPIN,
    SWAP_FLIP,
    BELL_RING,
    NUDGE_LEFT,
    NUDGE_RIGHT,
    PIGGY_BOUNCE,
    POP_BOUNCE
}

/**
 * GPU-accelerated reactive vector icon that squashes smoothly while held down
 * and plays a tailored spring choreography whenever [triggerKey] changes.
 */
@Composable
fun ReactiveIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    motion: ReactiveMotionType = ReactiveMotionType.POP_BOUNCE,
    isPressed: Boolean = false,
    triggerKey: Any? = null,
    animateOnFirstComposition: Boolean = false,
    extraRotationDegrees: Float = 0f
) {
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "reactiveIconPressScale"
    )
    val pressTilt by animateFloatAsState(
        targetValue = when {
            !isPressed -> 0f
            motion == ReactiveMotionType.GEAR_SPIN -> -18f
            motion == ReactiveMotionType.PLUS_TWIST -> 22f
            motion == ReactiveMotionType.NUDGE_LEFT -> -6f
            motion == ReactiveMotionType.NUDGE_RIGHT -> 6f
            motion == ReactiveMotionType.BELL_RING -> -12f
            else -> -6f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "reactiveIconPressTilt"
    )

    val animScale = remember { Animatable(1f) }
    val animRotation = remember { Animatable(0f) }
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    var hasInitialized by remember { mutableIntStateOf(0) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == null) return@LaunchedEffect
        val isFirst = hasInitialized == 0
        hasInitialized++
        if (isFirst && !animateOnFirstComposition) return@LaunchedEffect

        val bouncySpring = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        val livelySpring = spring<Float>(
            dampingRatio = 0.42f,
            stiffness = Spring.StiffnessMediumLow
        )

        when (motion) {
            ReactiveMotionType.GEAR_SPIN -> {
                launch {
                    animScale.snapTo(0.86f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    val target = animRotation.value + 120f
                    animRotation.animateTo(target, livelySpring)
                }
            }

            ReactiveMotionType.PLUS_TWIST -> {
                launch {
                    animScale.snapTo(0.80f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    val target = animRotation.value + 90f
                    animRotation.animateTo(target, livelySpring)
                }
            }

            ReactiveMotionType.CYCLE_SPIN -> {
                launch {
                    animScale.snapTo(0.86f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    val target = animRotation.value + 360f
                    animRotation.animateTo(
                        target,
                        spring(
                            dampingRatio = 0.52f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }

            ReactiveMotionType.SWAP_FLIP -> {
                launch {
                    animScale.snapTo(0.84f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    val target = animRotation.value + 180f
                    animRotation.animateTo(target, livelySpring)
                }
            }

            ReactiveMotionType.BELL_RING -> {
                launch {
                    animScale.snapTo(1.16f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    animRotation.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
                    animRotation.animateTo(18f, spring(stiffness = Spring.StiffnessHigh))
                    animRotation.animateTo(-12f, spring(stiffness = Spring.StiffnessMedium))
                    animRotation.animateTo(6f, spring(stiffness = Spring.StiffnessMedium))
                    animRotation.animateTo(0f, bouncySpring)
                }
            }

            ReactiveMotionType.NUDGE_LEFT -> {
                launch {
                    animScale.snapTo(0.88f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    animOffsetX.snapTo(-14f)
                    animOffsetX.animateTo(0f, bouncySpring)
                }
            }

            ReactiveMotionType.NUDGE_RIGHT -> {
                launch {
                    animScale.snapTo(0.88f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    animOffsetX.snapTo(14f)
                    animOffsetX.animateTo(0f, bouncySpring)
                }
            }

            ReactiveMotionType.PIGGY_BOUNCE -> {
                launch {
                    animScale.snapTo(1.18f)
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    animOffsetY.animateTo(-12f, spring(stiffness = Spring.StiffnessHigh))
                    animOffsetY.animateTo(0f, livelySpring)
                }
                launch {
                    animRotation.animateTo(-10f, spring(stiffness = Spring.StiffnessHigh))
                    animRotation.animateTo(7f, spring(stiffness = Spring.StiffnessMedium))
                    animRotation.animateTo(0f, bouncySpring)
                }
            }

            ReactiveMotionType.POP_BOUNCE -> {
                launch {
                    animScale.snapTo(0.78f)
                    animScale.animateTo(1.14f, spring(stiffness = Spring.StiffnessHigh))
                    animScale.animateTo(1f, bouncySpring)
                }
                launch {
                    animRotation.snapTo(-10f)
                    animRotation.animateTo(0f, bouncySpring)
                }
            }
        }
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val combinedScale = pressScale * animScale.value
                scaleX = combinedScale
                scaleY = combinedScale
                rotationZ = animRotation.value + pressTilt + extraRotationDegrees
                translationX = animOffsetX.value
                translationY = animOffsetY.value
            }
    )
}

/**
 * Organic-shaped icon button (scalloped cookie, clover, or circle) whose container
 * squashes & tilts on touch while its inner [ReactiveIcon] fires spring physics on tap.
 */
@Composable
fun ExpressiveReactiveIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveCookie12Shape,
    containerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
    borderColor: Color? = null,
    iconTint: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 19.dp,
    motion: ReactiveMotionType = ReactiveMotionType.POP_BOUNCE,
    externalTrigger: Int = 0
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var tapCount by remember { mutableIntStateOf(0) }

    val containerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "reactiveBtnScale"
    )
    val containerRotation by animateFloatAsState(
        targetValue = if (isPressed) 10f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "reactiveBtnRot"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(buttonSize)
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                rotationZ = containerRotation
            }
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button
            ) {
                tapCount++
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        ReactiveIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint,
            size = iconSize,
            motion = motion,
            isPressed = isPressed,
            triggerKey = tapCount + externalTrigger
        )
    }
}
