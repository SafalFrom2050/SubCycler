package com.subtrack.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Material 3 Expressive Scalloped Cookie / Clover shape using polar harmonic modulation:
 * r(theta) = R * (1 - grooveDepth * (1 - cos(lobes * (theta - phaseOffset))) / 2)
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

val ExpressiveCookie8Shape: Shape = ScallopedCookieShape(lobes = 8, grooveDepthFraction = 0.125f)
val ExpressiveCookie12Shape: Shape = ScallopedCookieShape(lobes = 12, grooveDepthFraction = 0.075f)
val ExpressiveClover4Shape: Shape = ScallopedCookieShape(
    lobes = 4,
    grooveDepthFraction = 0.24f,
    phaseOffsetRadians = (PI / 4.0).toFloat()
)
val ExpressiveArchShape: Shape = RoundedCornerShape(
    topStartPercent = 50,
    topEndPercent = 50,
    bottomStartPercent = 24,
    bottomEndPercent = 24
)
val ExpressivePebbleShape: Shape = RoundedCornerShape(
    topStartPercent = 48,
    topEndPercent = 30,
    bottomEndPercent = 48,
    bottomStartPercent = 30
)
val ExpressiveSquircleShape: Shape = RoundedCornerShape(32)

enum class ExpressiveBadgeShapeKind {
    COOKIE,
    CLOVER,
    PEBBLE,
    ARCH,
    SQUIRCLE,
    CIRCLE
}

fun ExpressiveBadgeShapeKind.toComposeShape(): Shape = when (this) {
    ExpressiveBadgeShapeKind.COOKIE -> ExpressiveCookie8Shape
    ExpressiveBadgeShapeKind.CLOVER -> ExpressiveClover4Shape
    ExpressiveBadgeShapeKind.PEBBLE -> ExpressivePebbleShape
    ExpressiveBadgeShapeKind.ARCH -> ExpressiveArchShape
    ExpressiveBadgeShapeKind.SQUIRCLE -> ExpressiveSquircleShape
    ExpressiveBadgeShapeKind.CIRCLE -> CircleShape
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
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "segStartRadius"
            )
            val endRadius by animateDpAsState(
                targetValue = targetEndRadius,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "segEndRadius"
            )
            val segScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "segScale"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(160),
                label = "segBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(160),
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
        container = Color(0xFFE7E0D0),          // Deepened editorial sand so badges pop crisply on white & mist surfaces
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
 * Material 3 Expressive Monogram Badge using balanced neutral & Botanical Emerald shades
 * and organic shapes, smoothly blooming its groove depth & spring bounce when selected/pressed.
 */
@Composable
fun ExpressiveServiceBadge(
    serviceName: String,
    initials: String = ExpressivePalettes.initialsForService(serviceName),
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    fontSize: TextUnit = 13.sp,
    isSelected: Boolean = false,
    isPressed: Boolean = false
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val (baseShapeKind, palette) = ExpressivePalettes.styleForService(serviceName)

    val animatedGroove by animateFloatAsState(
        targetValue = when {
            isSelected -> 0.125f
            isPressed && baseShapeKind == ExpressiveBadgeShapeKind.COOKIE -> 0.16f
            baseShapeKind == ExpressiveBadgeShapeKind.COOKIE -> 0.125f
            baseShapeKind == ExpressiveBadgeShapeKind.CLOVER -> if (isPressed) 0.18f else 0.24f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badgeGrooveMorph"
    )

    val activeShape = when {
        isSelected -> ScallopedCookieShape(lobes = 8, grooveDepthFraction = animatedGroove)
        baseShapeKind == ExpressiveBadgeShapeKind.COOKIE ->
            ScallopedCookieShape(lobes = 8, grooveDepthFraction = animatedGroove)
        baseShapeKind == ExpressiveBadgeShapeKind.CLOVER ->
            ScallopedCookieShape(lobes = 4, grooveDepthFraction = animatedGroove, phaseOffsetRadians = (PI / 4.0).toFloat())
        else -> baseShapeKind.toComposeShape()
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            isSelected -> 1.07f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badgeScale"
    )
    val rotation by animateFloatAsState(
        targetValue = when {
            isPressed -> -9f
            isSelected -> -6f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "badgeRotation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) palette.selectedContainer else palette.container,
        animationSpec = tween(180),
        label = "badgeContainer"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .clip(activeShape)
            .background(containerColor, activeShape)
            .then(
                when {
                    isSelected -> Modifier.border(1.5.dp, palette.accent.copy(alpha = 0.55f), activeShape)
                    !isDark -> Modifier.border(1.dp, palette.border.copy(alpha = 0.35f), activeShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = palette.content,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = (-0.5).sp
        )
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
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
 * animated sinusoidal phase, rounded dot tip, and optional proportional hatched break capsule
 * so Detail Sheets visualize both active progress and the upcoming rest break!
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
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wavyProgress"
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
    val currentPhase = if (animateWave) phaseShift else 0f
    val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            }
    ) {
        val strokeWidth = 3.4.dp.toPx()
        val amplitude = 3.2.dp.toPx()
        val wavelength = 24.dp.toPx()
        val centerY = size.height / 2f
        val startX = strokeWidth
        val endX = (size.width - strokeWidth).coerceAtLeast(startX)
        val totalSpan = endX - startX

        val showBreakZone = activeDays > 0 && blackoutDays > 0 && isActivePhase
        val gapPx = if (showBreakZone) 8.dp.toPx() else 0f
        val waveZoneSpan = if (showBreakZone) {
            val ratio = (activeDays.toFloat() / (activeDays + blackoutDays)).coerceIn(0.45f, 0.78f)
            (totalSpan - gapPx) * ratio
        } else {
            totalSpan
        }
        val waveZoneEndX = startX + waveZoneSpan

        // Ensure Day 1 has an expressive minimum visible wave span (~12% of zone) so it never looks like a stray dot
        val minLeadPx = (waveZoneSpan * 0.12f).coerceAtLeast(18.dp.toPx()).coerceAtMost(waveZoneSpan)
        val activeEndX = if (animatedProgress <= 0f) {
            startX
        } else {
            (startX + waveZoneSpan * animatedProgress).coerceAtLeast(startX + minLeadPx).coerceAtMost(waveZoneEndX)
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

        // Smooth sinusoidal wave with gentle ramp-in at edges
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
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawCircle(
                color = activeColor,
                radius = 4.4.dp.toPx(),
                center = Offset(activeEndX, lastY)
            )
        }

        // Upcoming Break Capsule (proportional hatched pill showing the rest break right ahead!)
        if (showBreakZone) {
            val breakStartX = waveZoneEndX + gapPx
            val breakWidth = (endX - breakStartX).coerceAtLeast(10.dp.toPx())
            val pillHeight = 8.dp.toPx()
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
        }
    }
}
