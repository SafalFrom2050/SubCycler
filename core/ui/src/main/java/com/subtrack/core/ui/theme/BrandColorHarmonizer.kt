package com.subtrack.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.components.ExpressiveTonalPalette
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Harmonized 3-petal color set for [com.subtrack.core.ui.components.SubCyclerSwirlLogo].
 * Preserves a consistent Light → Mid → Deep luminance ladder so the 3 overlapping swirl
 * petals always maintain crisp depth separation against the dark backdrop.
 */
data class SwirlLogoColors(
    val lightPetal: Color,
    val midPetal: Color,
    val deepPetal: Color,
    val backdrop: Color = Color(0xFF02151B),
    val centerGlyph: Color = Color(0xFFC8FDE8)
)

/**
 * Harmonized Living Hero Mood gradient palette for [com.subtrack.core.ui.components.ExpressiveHeroCardBackdrop].
 * Strictly bounded in lightness and chroma so hero typography maintains WCAG AAA contrast.
 */
data class HeroMoodPalette(
    val baseColor: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val watermarkTint: Color,
    val contentColor: Color,
    val accentColor: Color
)

/**
 * Pure-Kotlin offline Material 3 Tonal Harmonizer for subscription brand colors.
 *
 * Converts raw brand hex codes (e.g. Netflix `#E50914`, Spotify `#1DB954`, Disney+ `#113CCF`,
 * Crunchyroll `#F47521`) into calm, studio-cohesive soft pastel tonal palettes with mathematically
 * guaranteed WCAG AAA (≥ 7:1) monogram contrast in both Light and Dark themes.
 */
object BrandColorHarmonizer {

    private const val BOTANICAL_ANCHOR_HUE = 159f
    private const val HARMONIZE_PULL_FACTOR = 0.14f
    private const val MIN_CONTRAST_RATIO = 7.0

    private val LEGACY_GENERIC_HEXES = setOf(
        "#10B981",
        "#0C6B4E"
    )

    private val FALLBACK_CUSTOM_HEXES = listOf(
        "#0D8F67",
        "#00A8E1",
        "#6366F1",
        "#D97757",
        "#FA243C",
        "#1DB954",
        "#5822B4",
        "#5C544B"
    )

    val DefaultBotanicalSwirl = SwirlLogoColors(
        lightPetal = Color(0xFFB8FCE1),
        midPetal = Color(0xFF4FE3A4),
        deepPetal = Color(0xFF0D8F67),
        backdrop = Color(0xFF02151B),
        centerGlyph = Color(0xFFC8FDE8)
    )

    fun parseHexRgb(hex: String?): Int? {
        if (hex.isNullOrBlank()) return null
        val cleaned = hex.trim().removePrefix("#")
        val normalized = when (cleaned.length) {
            6 -> cleaned
            8 -> cleaned.substring(2) // strip alpha
            else -> return null
        }
        return normalized.toIntOrNull(16)
    }

    fun colorFromHex(hex: String?, fallback: Color = Color(0xFF0D8F67)): Color {
        val rgb = parseHexRgb(hex) ?: return fallback
        return Color(0xFF000000L or (rgb.toLong() and 0xFFFFFFL))
    }

    /**
     * Resolves the effective brand hex for a service:
     * 1. Explicit non-default user/preset [storedColorHex] wins.
     * 2. Known service names in [PresetLibrary.brandColorHexForName] upgrade legacy generic defaults.
     * 3. Unfamiliar custom services with legacy generic defaults receive a stable hash-picked hue.
     */
    fun resolveEffectiveBrandHex(
        serviceName: String,
        storedColorHex: String? = null
    ): String {
        val trimmedStored = storedColorHex?.trim()?.uppercase(Locale.US)
        val storedValid = trimmedStored != null && parseHexRgb(trimmedStored) != null
        if (storedValid && trimmedStored !in LEGACY_GENERIC_HEXES) {
            return trimmedStored
        }
        PresetLibrary.brandColorHexForName(serviceName)?.let { return it.uppercase(Locale.US) }
        if (storedValid) return trimmedStored
        val normalizedName = serviceName.trim().lowercase(Locale.ROOT)
        if (normalizedName.isEmpty()) return "#0D8F67"
        val idx = abs(normalizedName.hashCode()) % FALLBACK_CUSTOM_HEXES.size
        return FALLBACK_CUSTOM_HEXES[idx]
    }

    /**
     * Builds a soft harmonized Material 3 pastel [ExpressiveTonalPalette] from a service's name
     * and optional stored hex color, with guaranteed ≥ 7:1 contrast between container and content.
     */
    fun harmonizedTonalPalette(
        serviceName: String,
        storedColorHex: String? = null,
        isDark: Boolean
    ): ExpressiveTonalPalette {
        val hex = resolveEffectiveBrandHex(serviceName, storedColorHex)
        return harmonizedTonalPaletteFromHex(hex, isDark)
    }

    fun harmonizedTonalPaletteFromHex(
        hex: String,
        isDark: Boolean
    ): ExpressiveTonalPalette {
        val rgb = parseHexRgb(hex) ?: 0x0D8F67
        val (rawHue, rawSat, _) = rgbToHsl(rgb)
        val isNeutral = rawSat < 0.11f
        val harmonizedHue = if (isNeutral) rawHue else pullHueToward(rawHue, BOTANICAL_ANCHOR_HUE, HARMONIZE_PULL_FACTOR)

        return if (!isDark) {
            val containerSat = if (isNeutral) rawSat.coerceAtMost(0.10f) else (rawSat * 0.56f).coerceIn(0.28f, 0.52f)
            val containerLight = when {
                isNeutral -> 0.90f
                harmonizedHue in 42f..92f -> 0.91f // keep warm yellows/limes crisp & airy
                else -> 0.89f
            }
            val selectedSat = if (isNeutral) (containerSat + 0.03f).coerceAtMost(0.14f) else (containerSat + 0.08f).coerceAtMost(0.62f)
            val selectedLight = (containerLight - 0.06f).coerceAtLeast(0.80f)

            val containerRgb = hslToRgb(harmonizedHue, containerSat, containerLight)
            val selectedRgb = hslToRgb(harmonizedHue, selectedSat, selectedLight)

            val contentSat = if (isNeutral) rawSat.coerceAtMost(0.14f) else (rawSat * 0.68f).coerceIn(0.42f, 0.68f)
            var contentLight = if (isNeutral) 0.21f else 0.23f
            var contentRgb = hslToRgb(harmonizedHue, contentSat, contentLight)
            while (contrastRatio(containerRgb, contentRgb) < MIN_CONTRAST_RATIO && contentLight > 0.08f) {
                contentLight = (contentLight - 0.025f).coerceAtLeast(0.08f)
                contentRgb = hslToRgb(harmonizedHue, contentSat, contentLight)
            }

            val accentSat = if (isNeutral) rawSat.coerceAtMost(0.14f) else (rawSat * 0.62f).coerceIn(0.38f, 0.60f)
            val accentRgb = hslToRgb(harmonizedHue, accentSat, if (isNeutral) 0.36f else 0.39f)
            val borderRgb = hslToRgb(harmonizedHue, containerSat * 0.85f, if (isNeutral) 0.66f else 0.68f)

            ExpressiveTonalPalette(
                container = rgbToComposeColor(containerRgb),
                selectedContainer = rgbToComposeColor(selectedRgb),
                border = rgbToComposeColor(borderRgb),
                content = rgbToComposeColor(contentRgb),
                accent = rgbToComposeColor(accentRgb)
            )
        } else {
            val containerSat = if (isNeutral) rawSat.coerceAtMost(0.12f) else (rawSat * 0.44f).coerceIn(0.24f, 0.46f)
            val containerLight = if (isNeutral) 0.24f else 0.23f
            val selectedSat = if (isNeutral) (containerSat + 0.03f).coerceAtMost(0.16f) else (containerSat + 0.07f).coerceAtMost(0.54f)
            val selectedLight = 0.30f

            val containerRgb = hslToRgb(harmonizedHue, containerSat, containerLight)
            val selectedRgb = hslToRgb(harmonizedHue, selectedSat, selectedLight)

            val contentSat = if (isNeutral) rawSat.coerceAtMost(0.14f) else (rawSat * 0.66f).coerceIn(0.46f, 0.76f)
            var contentLight = 0.88f
            var contentRgb = hslToRgb(harmonizedHue, contentSat, contentLight)
            while (contrastRatio(containerRgb, contentRgb) < MIN_CONTRAST_RATIO && contentLight < 0.96f) {
                contentLight = (contentLight + 0.02f).coerceAtMost(0.96f)
                contentRgb = hslToRgb(harmonizedHue, contentSat, contentLight)
            }

            val accentSat = if (isNeutral) rawSat.coerceAtMost(0.16f) else (rawSat * 0.60f).coerceIn(0.44f, 0.70f)
            val accentRgb = hslToRgb(harmonizedHue, accentSat, 0.72f)
            val borderRgb = hslToRgb(harmonizedHue, containerSat * 0.85f, 0.44f)

            ExpressiveTonalPalette(
                container = rgbToComposeColor(containerRgb),
                selectedContainer = rgbToComposeColor(selectedRgb),
                border = rgbToComposeColor(borderRgb),
                content = rgbToComposeColor(contentRgb),
                accent = rgbToComposeColor(accentRgb)
            )
        }
    }

    /**
     * Resolves the 3 petal colors of SubCycler's dynamic swirl logo from the user's currently active
     * subscriptions (ordered by monthly spend, deduplicated by distinct hue so petals stay vivid).
     */
    fun resolveSwirlPetalColors(
        activeSubscriptions: List<Subscription>,
        useStackMood: Boolean = true
    ): SwirlLogoColors {
        if (!useStackMood || activeSubscriptions.isEmpty()) return DefaultBotanicalSwirl

        val distinctHues = selectDistinctActiveBrandRgb(activeSubscriptions, maxCount = 3)
        if (distinctHues.isEmpty()) return DefaultBotanicalSwirl

        return when (distinctHues.size) {
            1 -> {
                val (h, s, isNeutral) = harmonizedHueAndSat(distinctHues[0])
                SwirlLogoColors(
                    lightPetal = petalColor(h, s, isNeutral, targetLightness = 0.84f, satScale = 0.76f),
                    midPetal = petalColor(h, s, isNeutral, targetLightness = 0.62f, satScale = 0.72f),
                    deepPetal = petalColor(h, s, isNeutral, targetLightness = 0.39f, satScale = 0.70f),
                    centerGlyph = petalColor(h, s, isNeutral, targetLightness = 0.88f, satScale = 0.62f)
                )
            }
            2 -> {
                val (h1, s1, n1) = harmonizedHueAndSat(distinctHues[0])
                val (h2, s2, n2) = harmonizedHueAndSat(distinctHues[1])
                SwirlLogoColors(
                    lightPetal = petalColor(h1, s1, n1, targetLightness = 0.84f, satScale = 0.76f),
                    midPetal = petalColor(h2, s2, n2, targetLightness = 0.62f, satScale = 0.74f),
                    deepPetal = DefaultBotanicalSwirl.deepPetal,
                    centerGlyph = Color(0xFFD5F9EA)
                )
            }
            else -> {
                val (h1, s1, n1) = harmonizedHueAndSat(distinctHues[0])
                val (h2, s2, n2) = harmonizedHueAndSat(distinctHues[1])
                val (h3, s3, n3) = harmonizedHueAndSat(distinctHues[2])
                SwirlLogoColors(
                    lightPetal = petalColor(h1, s1, n1, targetLightness = 0.84f, satScale = 0.76f),
                    midPetal = petalColor(h2, s2, n2, targetLightness = 0.62f, satScale = 0.74f),
                    deepPetal = petalColor(h3, s3, n3, targetLightness = 0.41f, satScale = 0.72f),
                    centerGlyph = Color(0xFFE6F9F1)
                )
            }
        }
    }

    /**
     * Builds the soft Living Hero Mood backdrop palette from the user's active subscriptions.
     * Reverts cleanly to classic Botanical Emerald when [useStackMood] is `false` or no services
     * are currently active.
     */
    fun resolveHeroMoodPalette(
        activeSubscriptions: List<Subscription>,
        useStackMood: Boolean,
        isDark: Boolean
    ): HeroMoodPalette {
        val defaultBotanical = if (isDark) {
            HeroMoodPalette(
                baseColor = BrandDarkHeroContainer,
                gradientStart = Color(0xFF154536),
                gradientMid = Color(0xFF12392D),
                gradientEnd = Color(0xFF102E25),
                watermarkTint = BrandDarkOnPrimaryContainer,
                contentColor = BrandDarkOnPrimaryContainer,
                accentColor = BrandDarkPrimary
            )
        } else {
            HeroMoodPalette(
                baseColor = BrandLightHeroContainer,
                gradientStart = Color(0xFFC8F1DF),
                gradientMid = Color(0xFFD7F5E8),
                gradientEnd = Color(0xFFE2F7EE),
                watermarkTint = BrandLightOnPrimaryContainer,
                contentColor = BrandLightOnPrimaryContainer,
                accentColor = BrandLightPrimary
            )
        }
        if (!useStackMood || activeSubscriptions.isEmpty()) return defaultBotanical

        val activeRgbs = selectDistinctActiveBrandRgb(activeSubscriptions, maxCount = 3)
        if (activeRgbs.isEmpty()) return defaultBotanical

        val (h1, s1, n1) = harmonizedHueAndSat(activeRgbs[0])
        val (h2, s2, n2) = if (activeRgbs.size >= 2) {
            harmonizedHueAndSat(activeRgbs[1])
        } else {
            Triple(BOTANICAL_ANCHOR_HUE, 0.56f, false)
        }
        val (h3, s3, n3) = if (activeRgbs.size >= 3) {
            harmonizedHueAndSat(activeRgbs[2])
        } else {
            Triple(BOTANICAL_ANCHOR_HUE, 0.52f, false)
        }

        return if (!isDark) {
            val start = softHeroStop(h1, s1, n1, lightness = 0.875f, maxSat = 0.42f)
            val mid = softHeroStop(h2, s2, n2, lightness = 0.905f, maxSat = 0.36f)
            val end = softHeroStop(h3, s3, n3, lightness = 0.925f, maxSat = 0.34f)
            val inkRgb = ensureContrast(
                backgroundRgb = composeColorToRgb(start),
                hue = h1,
                sat = if (n1) 0.12f else 0.48f,
                initialLight = 0.17f,
                step = -0.02f,
                minLimit = 0.08f
            )
            HeroMoodPalette(
                baseColor = mid,
                gradientStart = start,
                gradientMid = mid,
                gradientEnd = end,
                watermarkTint = rgbToComposeColor(inkRgb),
                contentColor = rgbToComposeColor(inkRgb),
                accentColor = BrandLightPrimary
            )
        } else {
            val start = softHeroStop(h1, s1, n1, lightness = 0.215f, maxSat = 0.38f)
            val mid = softHeroStop(h2, s2, n2, lightness = 0.185f, maxSat = 0.34f)
            val end = softHeroStop(h3, s3, n3, lightness = 0.155f, maxSat = 0.32f)
            val inkRgb = ensureContrast(
                backgroundRgb = composeColorToRgb(start),
                hue = h1,
                sat = if (n1) 0.14f else 0.45f,
                initialLight = 0.90f,
                step = 0.02f,
                minLimit = 0.96f
            )
            HeroMoodPalette(
                baseColor = mid,
                gradientStart = start,
                gradientMid = mid,
                gradientEnd = end,
                watermarkTint = rgbToComposeColor(inkRgb),
                contentColor = rgbToComposeColor(inkRgb),
                accentColor = BrandDarkPrimary
            )
        }
    }

    /**
     * Recommends the closest curated [AppLauncherIcon] colorway for the user's dominant active
     * subscription so Settings can show a friendly "Matches your stack" hint.
     */
    fun recommendedLauncherIcon(activeSubscriptions: List<Subscription>): AppLauncherIcon {
        val lead = activeSubscriptions.maxByOrNull { it.monthlyEquivalentCost } ?: return AppLauncherIcon.BOTANICAL
        val rgb = parseHexRgb(resolveEffectiveBrandHex(lead.name, lead.colorHex)) ?: return AppLauncherIcon.BOTANICAL
        val (hue, sat, _) = rgbToHsl(rgb)
        if (sat < 0.12f) return AppLauncherIcon.OBSIDIAN
        return when {
            hue < 10f || hue >= 332f -> AppLauncherIcon.CINEMA
            hue in 10f..62f -> AppLauncherIcon.TERRACOTTA
            hue in 62f..182f -> AppLauncherIcon.BOTANICAL
            hue in 182f..242f -> AppLauncherIcon.OCEAN
            else -> AppLauncherIcon.AURORA
        }
    }

    /**
     * Computes the WCAG 2.1 relative contrast ratio (`1.0..21.0`) between two 24-bit RGB integers.
     */
    fun contrastRatio(rgbA: Int, rgbB: Int): Double {
        val lumA = relativeLuminance(rgbA)
        val lumB = relativeLuminance(rgbB)
        val lighter = max(lumA, lumB)
        val darker = min(lumA, lumB)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun relativeLuminance(rgb: Int): Double {
        fun channel(v: Int): Double {
            val s = (v and 0xFF) / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel(rgb shr 16)
        val g = channel(rgb shr 8)
        val b = channel(rgb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    internal fun selectDistinctActiveBrandRgb(
        activeSubscriptions: List<Subscription>,
        maxCount: Int
    ): List<Int> {
        val ranked = activeSubscriptions
            .sortedByDescending { it.monthlyEquivalentCost }
            .mapNotNull { sub -> parseHexRgb(resolveEffectiveBrandHex(sub.name, sub.colorHex)) }
        if (ranked.isEmpty()) return emptyList()

        val chosen = mutableListOf<Int>()
        val chosenHues = mutableListOf<Float>()
        for (rgb in ranked) {
            if (chosen.size >= maxCount) break
            val (h, s, _) = rgbToHsl(rgb)
            val tooClose = chosenHues.any { existingHue ->
                s >= 0.12f && circularHueDistance(h, existingHue) < 24f
            }
            if (!tooClose) {
                chosen.add(rgb)
                chosenHues.add(h)
            }
        }
        return chosen
    }

    private fun harmonizedHueAndSat(rgb: Int): Triple<Float, Float, Boolean> {
        val (h, s, _) = rgbToHsl(rgb)
        val neutral = s < 0.11f
        val harmonized = if (neutral) h else pullHueToward(h, BOTANICAL_ANCHOR_HUE, HARMONIZE_PULL_FACTOR)
        return Triple(harmonized, s, neutral)
    }

    private fun petalColor(
        hue: Float,
        sat: Float,
        isNeutral: Boolean,
        targetLightness: Float,
        satScale: Float
    ): Color {
        val effectiveSat = if (isNeutral) {
            sat.coerceAtMost(0.10f)
        } else {
            (sat * satScale).coerceIn(0.42f, 0.78f)
        }
        return rgbToComposeColor(hslToRgb(hue, effectiveSat, targetLightness))
    }

    private fun softHeroStop(
        hue: Float,
        sat: Float,
        isNeutral: Boolean,
        lightness: Float,
        maxSat: Float
    ): Color {
        val effectiveSat = if (isNeutral) {
            sat.coerceAtMost(0.10f)
        } else {
            (sat * 0.45f).coerceIn(0.20f, maxSat)
        }
        return rgbToComposeColor(hslToRgb(hue, effectiveSat, lightness))
    }

    private fun ensureContrast(
        backgroundRgb: Int,
        hue: Float,
        sat: Float,
        initialLight: Float,
        step: Float,
        minLimit: Float
    ): Int {
        var l = initialLight
        var candidate = hslToRgb(hue, sat, l)
        var guard = 0
        while (contrastRatio(backgroundRgb, candidate) < MIN_CONTRAST_RATIO && guard < 20) {
            l = if (step < 0f) (l + step).coerceAtLeast(minLimit) else (l + step).coerceAtMost(minLimit)
            candidate = hslToRgb(hue, sat, l)
            guard++
        }
        return candidate
    }

    internal fun pullHueToward(sourceHue: Float, targetHue: Float, fraction: Float): Float {
        var delta = (targetHue - sourceHue) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return ((sourceHue + delta * fraction) % 360f + 360f) % 360f
    }

    private fun circularHueDistance(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return min(diff, 360f - diff)
    }

    internal fun rgbToHsl(rgb: Int): Triple<Float, Float, Float> {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        val s = if (delta < 1e-5f) 0f else delta / (1f - abs(2f * l - 1f))
        val h = when {
            delta < 1e-5f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return Triple((h + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    internal fun hslToRgb(hue: Float, sat: Float, light: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = sat.coerceIn(0f, 1f)
        val l = light.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (r shl 16) or (g shl 8) or b
    }

    private fun rgbToComposeColor(rgb: Int): Color =
        Color(0xFF000000L or (rgb.toLong() and 0xFFFFFFL))

    fun composeColorToRgb(color: Color): Int {
        val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
        return (r shl 16) or (g shl 8) or b
    }
}
