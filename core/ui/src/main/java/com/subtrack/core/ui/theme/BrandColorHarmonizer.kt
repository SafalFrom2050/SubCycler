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
 * Pure Flat Color-Block Living Hero Mood palette for [com.subtrack.core.ui.components.ExpressiveHeroCardBackdrop].
 *
 * Uses a single confident jewel hue from the dominant active service (zero multi-brand rainbow gradient),
 * paired with a high-contrast contrasting triadic [sealColor]/[onSealColor] pop and up to 3 vivid
 * [stackAccentDots] representing active stack variety cleanly inside the status pill.
 */
data class HeroMoodPalette(
    val baseColor: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val watermarkTint: Color,
    val contentColor: Color,
    val accentColor: Color,
    val sealColor: Color = BrandTriadicLime,
    val onSealColor: Color = BrandOnTriadicLime,
    val stackAccentDots: List<Color> = emptyList()
)

/**
 * Pure-Kotlin offline Material 3 Expressive Bold Color-Block Engine for subscription brand colors.
 *
 * Preserves true brand hues (zero muddy green-pull on reds/blues/oranges) while generating bold,
 * vivid jewel badges, high-contrast flat color-block Hero moods, and WCAG-verified ink pairs
 * across both Light and Dark themes.
 */
object BrandColorHarmonizer {

    private const val BOTANICAL_ANCHOR_HUE = 159f
    private const val HARMONIZE_PULL_FACTOR = 0.0f
    private const val MIN_BOLD_CONTRAST_RATIO = 5.0
    private const val MIN_AAA_CONTRAST_RATIO = 7.0

    private val LEGACY_GENERIC_HEXES = setOf(
        "#10B981",
        "#0C6B4E"
    )

    private val FALLBACK_CUSTOM_HEXES = listOf(
        "#00A86B",
        "#0095E8",
        "#6366F1",
        "#EA580C",
        "#E11D48",
        "#1DB954",
        "#7C3AED",
        "#3B4752"
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

    fun colorFromHex(hex: String?, fallback: Color = Color(0xFF008256)): Color {
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
        if (normalizedName.isEmpty()) return "#008256"
        val idx = abs(normalizedName.hashCode()) % FALLBACK_CUSTOM_HEXES.size
        return FALLBACK_CUSTOM_HEXES[idx]
    }

    /**
     * Builds a Bold Color-Block true-hue [ExpressiveTonalPalette] from a service's name
     * and optional stored hex color, guaranteeing crisp WCAG AA+/AAA legibility (`>= 5.0:1`)
     * on saturated solid badges and providing a luminous [ExpressiveTonalPalette.accent] for
     * 2-zone wave vessels and seam strokes.
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
        val rgb = parseHexRgb(hex) ?: 0x008256
        val (rawHue, rawSat, rawLight) = rgbToHsl(rgb)
        val isNeutral = rawSat < 0.11f
        val trueHue = if (isNeutral) rawHue else pullHueToward(rawHue, BOTANICAL_ANCHOR_HUE, HARMONIZE_PULL_FACTOR)

        // Naturally luminous hues (tangerine, amber, lime, emerald, electric cyan) pop best with
        // bright vivid fills + deep dark ink in Dark mode; deep jewel hues (crimson, sapphire, violet)
        // pop best as solid saturated jewel badges with crisp pure-white ink.
        val prefersDarkInkOnBrightFill = !isNeutral && trueHue in 26f..198f && (isDark || trueHue in 44f..178f)

        if (isNeutral) {
            val containerRgb = if (isDark) hslToRgb(trueHue, rawSat.coerceAtMost(0.10f), 0.28f) else hslToRgb(trueHue, rawSat.coerceAtMost(0.10f), 0.18f)
            val selectedRgb = if (isDark) hslToRgb(trueHue, rawSat.coerceAtMost(0.12f), 0.34f) else hslToRgb(trueHue, rawSat.coerceAtMost(0.12f), 0.24f)
            val contentRgb = 0xFFFFFF
            val accentRgb = if (isDark) hslToRgb(trueHue, 0.12f, 0.68f) else hslToRgb(trueHue, 0.12f, 0.36f)
            val borderRgb = if (isDark) hslToRgb(trueHue, 0.10f, 0.48f) else hslToRgb(trueHue, 0.10f, 0.34f)
            return ExpressiveTonalPalette(
                container = rgbToComposeColor(containerRgb),
                selectedContainer = rgbToComposeColor(selectedRgb),
                border = rgbToComposeColor(borderRgb),
                content = rgbToComposeColor(contentRgb),
                accent = rgbToComposeColor(accentRgb)
            )
        }

        val vividSat = (rawSat * 0.96f).coerceIn(0.76f, 0.94f)
        return if (prefersDarkInkOnBrightFill) {
            val startLight = if (isDark) {
                when {
                    trueHue in 46f..100f -> 0.56f
                    trueHue in 100f..170f -> 0.50f
                    else -> 0.54f
                }
            } else {
                when {
                    trueHue in 46f..95f -> 0.54f
                    trueHue in 100f..170f -> 0.48f
                    else -> 0.50f
                }
            }
            val deepInkRgb = hslToRgb(trueHue, 0.72f, 0.08f)
            var light = startLight
            var containerRgb = hslToRgb(trueHue, vividSat, light)
            while (contrastRatio(containerRgb, deepInkRgb) < MIN_AAA_CONTRAST_RATIO && light < 0.72f) {
                light = (light + 0.025f).coerceAtMost(0.72f)
                containerRgb = hslToRgb(trueHue, vividSat, light)
            }
            val selectedRgb = hslToRgb(trueHue, (vividSat + 0.04f).coerceAtMost(0.98f), (light + 0.05f).coerceAtMost(0.76f))
            val accentRgb = hslToRgb(trueHue, vividSat, if (isDark) (light + 0.06f).coerceAtMost(0.66f) else (light - 0.10f).coerceAtLeast(0.36f))
            val borderRgb = hslToRgb(trueHue, vividSat * 0.85f, if (isDark) 0.66f else 0.36f)
            ExpressiveTonalPalette(
                container = rgbToComposeColor(containerRgb),
                selectedContainer = rgbToComposeColor(selectedRgb),
                border = rgbToComposeColor(borderRgb),
                content = rgbToComposeColor(deepInkRgb),
                accent = rgbToComposeColor(accentRgb)
            )
        } else {
            // Bold jewel fill (crimson, ruby, royal blue, indigo, violet, deep orange in Light) + crisp pure white ink
            val whiteInkRgb = 0xFFFFFF
            val initialLight = when {
                trueHue in 205f..265f -> if (isDark) 0.50f else 0.45f // royal blue stays punchy & luminous
                trueHue in 265f..315f -> if (isDark) 0.49f else 0.44f // vivid violet
                else -> if (isDark) 0.45f else 0.42f                  // vivid crimson / scarlet / tangerine
            }
            var light = initialLight
            var containerRgb = hslToRgb(trueHue, vividSat, light)
            while (contrastRatio(containerRgb, whiteInkRgb) < MIN_BOLD_CONTRAST_RATIO && light > 0.24f) {
                light = (light - 0.02f).coerceAtLeast(0.24f)
                containerRgb = hslToRgb(trueHue, vividSat, light)
            }
            val selectedRgb = hslToRgb(trueHue, (vividSat + 0.04f).coerceAtMost(0.98f), (light - 0.05f).coerceAtLeast(0.20f))
            val accentRgb = hslToRgb(
                trueHue,
                vividSat,
                if (isDark) (light + 0.14f).coerceIn(0.56f, 0.68f) else light.coerceIn(0.42f, 0.50f)
            )
            val borderRgb = hslToRgb(trueHue, vividSat * 0.85f, if (isDark) 0.62f else 0.42f)
            ExpressiveTonalPalette(
                container = rgbToComposeColor(containerRgb),
                selectedContainer = rgbToComposeColor(selectedRgb),
                border = rgbToComposeColor(borderRgb),
                content = rgbToComposeColor(whiteInkRgb),
                accent = rgbToComposeColor(accentRgb)
            )
        }
    }

    /**
     * Converts any curated [AppLauncherIcon] colorway directly into its matching [SwirlLogoColors].
     */
    fun swirlColorsForLauncherIcon(icon: AppLauncherIcon): SwirlLogoColors =
        if (icon == AppLauncherIcon.BOTANICAL) {
            DefaultBotanicalSwirl
        } else {
            SwirlLogoColors(
                lightPetal = colorFromHex(icon.lightPetalHex),
                midPetal = colorFromHex(icon.midPetalHex),
                deepPetal = colorFromHex(icon.deepPetalHex),
                backdrop = colorFromHex(icon.backdropHex),
                centerGlyph = colorFromHex(icon.lightPetalHex)
            )
        }

    /**
     * Resolves the 3 petal colors of SubCycler's dynamic swirl logo from the user's currently active
     * subscriptions (ordered by monthly spend, deduplicated by distinct hue so petals stay vivid).
     * Falls back to [fallbackIcon]'s curated swirl colorway when [useStackMood] is off or empty.
     */
    fun resolveSwirlPetalColors(
        activeSubscriptions: List<Subscription>,
        useStackMood: Boolean = true,
        fallbackIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL
    ): SwirlLogoColors {
        val fallbackSwirl = swirlColorsForLauncherIcon(fallbackIcon)
        if (!useStackMood || activeSubscriptions.isEmpty()) return fallbackSwirl

        val distinctHues = selectDistinctActiveBrandRgb(activeSubscriptions, maxCount = 3)
        if (distinctHues.isEmpty()) return fallbackSwirl

        return when (distinctHues.size) {
            1 -> {
                val (h, s, isNeutral) = trueHueAndSat(distinctHues[0])
                SwirlLogoColors(
                    lightPetal = petalColor(h, s, isNeutral, targetLightness = 0.82f, satScale = 0.88f),
                    midPetal = petalColor(h, s, isNeutral, targetLightness = 0.60f, satScale = 0.88f),
                    deepPetal = petalColor(h, s, isNeutral, targetLightness = 0.40f, satScale = 0.86f),
                    backdrop = fallbackSwirl.backdrop,
                    centerGlyph = petalColor(h, s, isNeutral, targetLightness = 0.88f, satScale = 0.72f)
                )
            }
            2 -> {
                val (h1, s1, n1) = trueHueAndSat(distinctHues[0])
                val (h2, s2, n2) = trueHueAndSat(distinctHues[1])
                SwirlLogoColors(
                    lightPetal = petalColor(h1, s1, n1, targetLightness = 0.80f, satScale = 0.88f),
                    midPetal = petalColor(h2, s2, n2, targetLightness = 0.60f, satScale = 0.88f),
                    deepPetal = fallbackSwirl.deepPetal,
                    backdrop = fallbackSwirl.backdrop,
                    centerGlyph = fallbackSwirl.centerGlyph
                )
            }
            else -> {
                val (h1, s1, n1) = trueHueAndSat(distinctHues[0])
                val (h2, s2, n2) = trueHueAndSat(distinctHues[1])
                val (h3, s3, n3) = trueHueAndSat(distinctHues[2])
                SwirlLogoColors(
                    lightPetal = petalColor(h1, s1, n1, targetLightness = 0.80f, satScale = 0.88f),
                    midPetal = petalColor(h2, s2, n2, targetLightness = 0.60f, satScale = 0.88f),
                    deepPetal = petalColor(h3, s3, n3, targetLightness = 0.42f, satScale = 0.86f),
                    backdrop = fallbackSwirl.backdrop,
                    centerGlyph = Color(0xFFECFFF6)
                )
            }
        }
    }

    /**
     * Resolves the active focal accent token set (`primary`, `onPrimary`, `primaryContainer`,
     * `onPrimaryContainer`, flat `heroBase`, `heroContent`, and triadic `heroPop`) linked to
     * **Living Stack Mood** when [useStackMood] is enabled and active subscriptions exist, or
     * falling back cleanly to [fallbackIcon]'s curated Bold Color-Block colorway.
     */
    fun resolveEffectiveAccentTokens(
        activeSubscriptions: List<Subscription>,
        useStackMood: Boolean,
        isDark: Boolean,
        fallbackIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL
    ): LauncherAccentTokens {
        val fallback = launcherAccentTokensFor(fallbackIcon, isDark)
        if (!useStackMood || activeSubscriptions.isEmpty()) return fallback

        val activeRgbs = selectDistinctActiveBrandRgb(activeSubscriptions, maxCount = 3)
        if (activeRgbs.isEmpty()) return fallback

        val (h1, s1, n1) = trueHueAndSat(activeRgbs[0])
        val (h2, s2, n2) = if (activeRgbs.size >= 2) {
            trueHueAndSat(activeRgbs[1])
        } else {
            Triple(h1, s1, n1)
        }
        val (popColor, onPopColor) = triadicPopForHeroHue(h1, n1)
        val flatHeroRgb = resolveFlatHeroBlockRgb(h1, s1, n1, isDark)
        val flatHeroColor = rgbToComposeColor(flatHeroRgb)
        val flatHeroInk = Color(0xFFFFFFFF)

        return if (!isDark) {
            val primarySat = if (n1) 0.14f else (s1 * 0.90f).coerceIn(0.68f, 0.90f)
            val primaryRgb = ensureContrast(
                backgroundRgb = 0xFFFFFF,
                hue = h1,
                sat = primarySat,
                initialLight = 0.40f,
                step = -0.02f,
                minLimit = 0.16f,
                targetRatio = MIN_BOLD_CONTRAST_RATIO
            )
            val onPrimaryRgb = 0xFFFFFF

            val containerSat = if (n1) 0.10f else (s1 * 0.78f).coerceIn(0.56f, 0.84f)
            val containerRgb = hslToRgb(h1, containerSat, 0.87f)
            val onContainerRgb = ensureContrast(
                backgroundRgb = containerRgb,
                hue = h1,
                sat = if (n1) 0.14f else 0.74f,
                initialLight = 0.18f,
                step = -0.02f,
                minLimit = 0.06f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )

            val secSat = if (n2) 0.14f else (s2 * 0.84f).coerceIn(0.60f, 0.86f)
            val secRgb = ensureContrast(
                backgroundRgb = 0xFFFFFF,
                hue = h2,
                sat = secSat,
                initialLight = 0.40f,
                step = -0.02f,
                minLimit = 0.18f,
                targetRatio = MIN_BOLD_CONTRAST_RATIO
            )
            val secContainerRgb = hslToRgb(h2, if (n2) 0.10f else (s2 * 0.72f).coerceIn(0.50f, 0.78f), 0.88f)
            val onSecContainerRgb = ensureContrast(
                backgroundRgb = secContainerRgb,
                hue = h2,
                sat = if (n2) 0.14f else 0.70f,
                initialLight = 0.18f,
                step = -0.02f,
                minLimit = 0.06f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )

            LauncherAccentTokens(
                primary = rgbToComposeColor(primaryRgb),
                onPrimary = rgbToComposeColor(onPrimaryRgb),
                primaryContainer = rgbToComposeColor(containerRgb),
                onPrimaryContainer = rgbToComposeColor(onContainerRgb),
                secondary = rgbToComposeColor(secRgb),
                onSecondary = rgbToComposeColor(0xFFFFFF),
                secondaryContainer = rgbToComposeColor(secContainerRgb),
                onSecondaryContainer = rgbToComposeColor(onSecContainerRgb),
                heroBase = flatHeroColor,
                heroContent = flatHeroInk,
                heroPop = popColor,
                onHeroPop = onPopColor
            )
        } else {
            val primarySat = if (n1) 0.16f else (s1 * 0.90f).coerceIn(0.68f, 0.94f)
            val primaryRgb = hslToRgb(h1, primarySat, 0.70f)
            val onPrimaryRgb = ensureContrast(
                backgroundRgb = primaryRgb,
                hue = h1,
                sat = if (n1) 0.14f else 0.68f,
                initialLight = 0.12f,
                step = -0.02f,
                minLimit = 0.05f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )

            val containerSat = if (n1) 0.14f else (s1 * 0.78f).coerceIn(0.58f, 0.84f)
            val containerRgb = ensureContrast(
                backgroundRgb = 0xF5F7F6,
                hue = h1,
                sat = containerSat,
                initialLight = 0.28f,
                step = -0.02f,
                minLimit = 0.16f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )
            val onContainerRgb = ensureContrast(
                backgroundRgb = containerRgb,
                hue = h1,
                sat = if (n1) 0.12f else 0.42f,
                initialLight = 0.94f,
                step = 0.02f,
                minLimit = 0.98f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )

            val secSat = if (n2) 0.16f else (s2 * 0.84f).coerceIn(0.60f, 0.88f)
            val secRgb = hslToRgb(h2, secSat, 0.68f)
            val onSecRgb = ensureContrast(
                backgroundRgb = secRgb,
                hue = h2,
                sat = if (n2) 0.14f else 0.64f,
                initialLight = 0.12f,
                step = -0.02f,
                minLimit = 0.05f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )
            val secContainerRgb = ensureContrast(
                backgroundRgb = 0xF5F7F6,
                hue = h2,
                sat = if (n2) 0.14f else (s2 * 0.72f).coerceIn(0.52f, 0.78f),
                initialLight = 0.27f,
                step = -0.02f,
                minLimit = 0.16f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )
            val onSecContainerRgb = ensureContrast(
                backgroundRgb = secContainerRgb,
                hue = h2,
                sat = if (n2) 0.12f else 0.40f,
                initialLight = 0.94f,
                step = 0.02f,
                minLimit = 0.98f,
                targetRatio = MIN_AAA_CONTRAST_RATIO
            )

            LauncherAccentTokens(
                primary = rgbToComposeColor(primaryRgb),
                onPrimary = rgbToComposeColor(onPrimaryRgb),
                primaryContainer = rgbToComposeColor(containerRgb),
                onPrimaryContainer = rgbToComposeColor(onContainerRgb),
                secondary = rgbToComposeColor(secRgb),
                onSecondary = rgbToComposeColor(onSecRgb),
                secondaryContainer = rgbToComposeColor(secContainerRgb),
                onSecondaryContainer = rgbToComposeColor(onSecContainerRgb),
                heroBase = flatHeroColor,
                heroContent = flatHeroInk,
                heroPop = popColor,
                onHeroPop = onPopColor
            )
        }
    }

    /**
     * Builds the **Pure Flat Color-Block** Living Hero Mood palette from the user's active stack.
     * Uses ONE clean dominant jewel hue (zero multi-brand rainbow gradient), paired with a smart
     * contrasting triadic `SAVE` seal pop and up to 3 vivid [HeroMoodPalette.stackAccentDots].
     */
    fun resolveHeroMoodPalette(
        activeSubscriptions: List<Subscription>,
        useStackMood: Boolean,
        isDark: Boolean,
        fallbackIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL
    ): HeroMoodPalette {
        val fallbackAccent = launcherAccentTokensFor(fallbackIcon, isDark)
        val defaultPalette = HeroMoodPalette(
            baseColor = fallbackAccent.heroBase,
            gradientStart = fallbackAccent.heroBase,
            gradientMid = fallbackAccent.heroBase,
            gradientEnd = fallbackAccent.heroBase,
            watermarkTint = Color.White,
            contentColor = fallbackAccent.heroContent,
            accentColor = fallbackAccent.heroPop,
            sealColor = fallbackAccent.heroPop,
            onSealColor = fallbackAccent.onHeroPop,
            stackAccentDots = emptyList()
        )
        if (!useStackMood || activeSubscriptions.isEmpty()) return defaultPalette

        val activeRgbs = selectDistinctActiveBrandRgb(activeSubscriptions, maxCount = 3)
        if (activeRgbs.isEmpty()) return defaultPalette

        val (h1, s1, n1) = trueHueAndSat(activeRgbs[0])
        val flatHeroRgb = resolveFlatHeroBlockRgb(h1, s1, n1, isDark)
        val flatHeroColor = rgbToComposeColor(flatHeroRgb)
        val (sealColor, onSealColor) = triadicPopForHeroHue(h1, n1)
        val stackDots = activeRgbs.map { dotRgb ->
            val (dh, ds, dn) = trueHueAndSat(dotRgb)
            if (dn) Color(0xFFE2E8F0) else rgbToComposeColor(hslToRgb(dh, (ds * 0.96f).coerceIn(0.78f, 0.96f), 0.58f))
        }

        return HeroMoodPalette(
            baseColor = flatHeroColor,
            gradientStart = flatHeroColor,
            gradientMid = flatHeroColor,
            gradientEnd = flatHeroColor,
            watermarkTint = Color.White,
            contentColor = Color(0xFFFFFFFF),
            accentColor = sealColor,
            sealColor = sealColor,
            onSealColor = onSealColor,
            stackAccentDots = stackDots
        )
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

    private fun trueHueAndSat(rgb: Int): Triple<Float, Float, Boolean> {
        val (h, s, _) = rgbToHsl(rgb)
        val neutral = s < 0.11f
        val hue = if (neutral) h else pullHueToward(h, BOTANICAL_ANCHOR_HUE, HARMONIZE_PULL_FACTOR)
        return Triple(hue, s, neutral)
    }

    /**
     * Picks a single rich, bold flat color-block jewel surface for the Hero card that guarantees
     * crisp white typography (`>= 5.5:1` Light, `>= 6.8:1` Dark) across every brand hue.
     */
    private fun resolveFlatHeroBlockRgb(
        hue: Float,
        sat: Float,
        isNeutral: Boolean,
        isDark: Boolean
    ): Int {
        if (isNeutral) {
            return if (isDark) 0x222C34 else 0x1E272E
        }
        val effectiveSat = (sat * 0.90f).coerceIn(0.68f, 0.88f)
        val targetContrast = if (isDark) 6.8 else 5.6
        val startLight = when {
            hue in 210f..265f -> if (isDark) 0.38f else 0.44f
            hue in 265f..315f -> if (isDark) 0.36f else 0.42f
            else -> if (isDark) 0.34f else 0.40f
        }
        return ensureContrast(
            backgroundRgb = 0xFFFFFF,
            hue = hue,
            sat = effectiveSat,
            initialLight = startLight,
            step = -0.02f,
            minLimit = 0.16f,
            targetRatio = targetContrast
        )
    }

    /**
     * Picks a joyful contrasting Material 3 Expressive triadic pop accent for the `SAVE` cookie seal
     * and savings wave pill that always clashes constructively against the flat Hero hue.
     */
    private fun triadicPopForHeroHue(heroHue: Float, isNeutral: Boolean): Pair<Color, Color> {
        if (isNeutral) return BrandTriadicLime to BrandOnTriadicLime
        return when {
            heroHue in 52f..108f -> BrandTriadicMint to BrandOnTriadicMint   // yellow-lime lead -> electric mint pop
            heroHue in 14f..52f -> BrandTriadicMint to BrandOnTriadicMint    // terracotta/orange lead -> electric mint pop
            else -> BrandTriadicLime to BrandOnTriadicLime                   // ruby, emerald, sapphire, violet -> electric acid-lime pop!
        }
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
            (sat * satScale).coerceIn(0.58f, 0.92f)
        }
        return rgbToComposeColor(hslToRgb(hue, effectiveSat, targetLightness))
    }

    private fun ensureContrast(
        backgroundRgb: Int,
        hue: Float,
        sat: Float,
        initialLight: Float,
        step: Float,
        minLimit: Float,
        targetRatio: Double = MIN_AAA_CONTRAST_RATIO
    ): Int {
        var l = initialLight
        var candidate = hslToRgb(hue, sat, l)
        var guard = 0
        while (contrastRatio(backgroundRgb, candidate) < targetRatio && guard < 24) {
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
