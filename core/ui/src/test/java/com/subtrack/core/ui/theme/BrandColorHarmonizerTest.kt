package com.subtrack.core.ui.theme

import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BrandColorHarmonizerTest {

    @Test
    fun allTopTwentyPresetsMeetHighContrastInBothLightAndDarkModes() {
        PresetLibrary.presets.forEach { preset ->
            listOf(false, true).forEach { isDark ->
                val palette = BrandColorHarmonizer.harmonizedTonalPalette(
                    serviceName = preset.name,
                    storedColorHex = preset.colorHex,
                    isDark = isDark
                )
                val bgRgb = BrandColorHarmonizer.composeColorToRgb(palette.container)
                val fgRgb = BrandColorHarmonizer.composeColorToRgb(palette.content)
                val ratio = BrandColorHarmonizer.contrastRatio(bgRgb, fgRgb)
                assertTrue(
                    "Expected >= 5.0 Bold Expressive contrast for ${preset.name} (dark=$isDark), got $ratio",
                    ratio >= 5.0
                )
            }
        }
    }

    @Test
    fun legacyGenericDefaultsUpgradeToPresetBrandColorAutomatically() {
        assertEquals("#E50914", BrandColorHarmonizer.resolveEffectiveBrandHex("Netflix", "#10B981"))
        assertEquals("#F47521", BrandColorHarmonizer.resolveEffectiveBrandHex("Crunchyroll", "#0C6B4E"))
        // Explicit custom user color is preserved even for known services
        assertEquals("#5822B4", BrandColorHarmonizer.resolveEffectiveBrandHex("Netflix", "#5822B4"))
    }

    @Test
    fun swirlLogoAdaptsAcrossZeroOneTwoAndThreeActiveServices() {
        val empty = BrandColorHarmonizer.resolveSwirlPetalColors(emptyList(), useStackMood = true)
        assertEquals(BrandColorHarmonizer.DefaultBotanicalSwirl, empty)

        val netflix = Subscription(name = "Netflix", cost = 17.99, colorHex = "#E50914", startDate = LocalDate.of(2026, 11, 27))
        val disney = Subscription(name = "Disney+", cost = 15.99, colorHex = "#113CCF", startDate = LocalDate.of(2026, 11, 27))
        val crunchy = Subscription(name = "Crunchyroll", cost = 7.99, colorHex = "#F47521", startDate = LocalDate.of(2026, 11, 27))

        val disabled = BrandColorHarmonizer.resolveSwirlPetalColors(listOf(netflix, disney, crunchy), useStackMood = false)
        assertEquals(BrandColorHarmonizer.DefaultBotanicalSwirl, disabled)

        val one = BrandColorHarmonizer.resolveSwirlPetalColors(listOf(netflix), useStackMood = true)
        assertNotEquals(BrandColorHarmonizer.DefaultBotanicalSwirl.lightPetal, one.lightPetal)

        val two = BrandColorHarmonizer.resolveSwirlPetalColors(listOf(netflix, disney), useStackMood = true)
        assertEquals(BrandColorHarmonizer.DefaultBotanicalSwirl.deepPetal, two.deepPetal)
        assertNotEquals(two.lightPetal, two.midPetal)

        val three = BrandColorHarmonizer.resolveSwirlPetalColors(listOf(netflix, disney, crunchy), useStackMood = true)
        assertNotEquals(BrandColorHarmonizer.DefaultBotanicalSwirl.deepPetal, three.deepPetal)
    }

    @Test
    fun recommendedLauncherIconMatchesDominantActiveBrandHue() {
        val netflix = Subscription(name = "Netflix", cost = 17.99, colorHex = "#E50914", startDate = LocalDate.of(2026, 11, 27))
        val disney = Subscription(name = "Disney+", cost = 15.99, colorHex = "#113CCF", startDate = LocalDate.of(2026, 11, 27))
        val claude = Subscription(name = "Claude Pro", cost = 20.00, colorHex = "#D97757", startDate = LocalDate.of(2026, 11, 27))
        val appleTv = Subscription(name = "Apple TV+", cost = 9.99, colorHex = "#2C2C2E", startDate = LocalDate.of(2026, 11, 27))
        val max = Subscription(name = "Max (HBO)", cost = 16.99, colorHex = "#5822B4", startDate = LocalDate.of(2026, 11, 27))

        assertEquals(AppLauncherIcon.BOTANICAL, BrandColorHarmonizer.recommendedLauncherIcon(emptyList()))
        assertEquals(AppLauncherIcon.CINEMA, BrandColorHarmonizer.recommendedLauncherIcon(listOf(netflix)))
        assertEquals(AppLauncherIcon.OCEAN, BrandColorHarmonizer.recommendedLauncherIcon(listOf(disney)))
        assertEquals(AppLauncherIcon.TERRACOTTA, BrandColorHarmonizer.recommendedLauncherIcon(listOf(claude)))
        assertEquals(AppLauncherIcon.OBSIDIAN, BrandColorHarmonizer.recommendedLauncherIcon(listOf(appleTv)))
        assertEquals(AppLauncherIcon.AURORA, BrandColorHarmonizer.recommendedLauncherIcon(listOf(max)))
    }

    @Test
    fun heroMoodIsFlatSingleHueAndMaintainsHighContrastInLightAndDark() {
        val stack = listOf(
            Subscription(name = "Netflix", cost = 17.99, colorHex = "#E50914", startDate = LocalDate.of(2026, 11, 27)),
            Subscription(name = "Disney+", cost = 15.99, colorHex = "#113CCF", startDate = LocalDate.of(2026, 11, 27))
        )
        listOf(false, true).forEach { isDark ->
            val mood = BrandColorHarmonizer.resolveHeroMoodPalette(stack, useStackMood = true, isDark = isDark)
            // Pure Flat Color-Block: no multi-color rainbow gradient stops
            assertEquals(mood.baseColor, mood.gradientStart)
            assertEquals(mood.baseColor, mood.gradientMid)
            assertEquals(mood.baseColor, mood.gradientEnd)
            assertEquals(2, mood.stackAccentDots.size)

            val ratio = BrandColorHarmonizer.contrastRatio(
                BrandColorHarmonizer.composeColorToRgb(mood.baseColor),
                BrandColorHarmonizer.composeColorToRgb(mood.contentColor)
            )
            assertTrue("Hero mood contrast too low (dark=$isDark): $ratio", ratio >= 5.5)

            val sealRatio = BrandColorHarmonizer.contrastRatio(
                BrandColorHarmonizer.composeColorToRgb(mood.sealColor),
                BrandColorHarmonizer.composeColorToRgb(mood.onSealColor)
            )
            assertTrue("Hero seal contrast too low (dark=$isDark): $sealRatio", sealRatio >= 10.0)
        }
    }

    @Test
    fun allLauncherIconsProvideDistinctHighContrastAccentTokens() {
        val lightPrimaries = AppLauncherIcon.entries.map { icon ->
            launcherAccentTokensFor(icon, isDark = false).primary
        }.toSet()
        val darkPrimaries = AppLauncherIcon.entries.map { icon ->
            launcherAccentTokensFor(icon, isDark = true).primary
        }.toSet()

        assertEquals(AppLauncherIcon.entries.size, lightPrimaries.size)
        assertEquals(AppLauncherIcon.entries.size, darkPrimaries.size)

        AppLauncherIcon.entries.forEach { icon ->
            listOf(false, true).forEach { isDark ->
                val tokens = launcherAccentTokensFor(icon, isDark)
                val primaryContrast = BrandColorHarmonizer.contrastRatio(
                    BrandColorHarmonizer.composeColorToRgb(tokens.primary),
                    BrandColorHarmonizer.composeColorToRgb(tokens.onPrimary)
                )
                val containerContrast = BrandColorHarmonizer.contrastRatio(
                    BrandColorHarmonizer.composeColorToRgb(tokens.primaryContainer),
                    BrandColorHarmonizer.composeColorToRgb(tokens.onPrimaryContainer)
                )
                assertTrue(
                    "Low primary contrast for ${icon.name} (dark=$isDark): $primaryContrast",
                    primaryContrast >= 4.5
                )
                assertTrue(
                    "Low container contrast for ${icon.name} (dark=$isDark): $containerContrast",
                    containerContrast >= 7.0
                )
            }
        }
    }

    @Test
    fun effectiveAccentTokensFollowLivingMoodWhenEnabledAndLauncherIconWhenDisabled() {
        val disney = Subscription(
            name = "Disney+",
            cost = 15.99,
            colorHex = "#113CCF",
            startDate = LocalDate.of(2026, 11, 27)
        )
        val staticOcean = BrandColorHarmonizer.resolveEffectiveAccentTokens(
            activeSubscriptions = listOf(disney),
            useStackMood = false,
            isDark = true,
            fallbackIcon = AppLauncherIcon.OCEAN
        )
        assertEquals(launcherAccentTokensFor(AppLauncherIcon.OCEAN, isDark = true), staticOcean)

        val liveMood = BrandColorHarmonizer.resolveEffectiveAccentTokens(
            activeSubscriptions = listOf(disney),
            useStackMood = true,
            isDark = true,
            fallbackIcon = AppLauncherIcon.BOTANICAL
        )
        assertNotEquals(launcherAccentTokensFor(AppLauncherIcon.BOTANICAL, isDark = true).primary, liveMood.primary)

        // Verify high contrast across all top 20 presets in both Light & Dark modes
        PresetLibrary.presets.forEach { preset ->
            val sub = Subscription(
                name = preset.name,
                cost = 12.99,
                colorHex = preset.colorHex,
                startDate = LocalDate.of(2026, 11, 27)
            )
            listOf(false, true).forEach { isDark ->
                val tokens = BrandColorHarmonizer.resolveEffectiveAccentTokens(
                    activeSubscriptions = listOf(sub),
                    useStackMood = true,
                    isDark = isDark,
                    fallbackIcon = AppLauncherIcon.BOTANICAL
                )
                val primaryContrast = BrandColorHarmonizer.contrastRatio(
                    BrandColorHarmonizer.composeColorToRgb(tokens.primary),
                    BrandColorHarmonizer.composeColorToRgb(tokens.onPrimary)
                )
                val containerContrast = BrandColorHarmonizer.contrastRatio(
                    BrandColorHarmonizer.composeColorToRgb(tokens.primaryContainer),
                    BrandColorHarmonizer.composeColorToRgb(tokens.onPrimaryContainer)
                )
                assertTrue(
                    "Low mood primary contrast for ${preset.name} (dark=$isDark): $primaryContrast",
                    primaryContrast >= 5.0
                )
                assertTrue(
                    "Low mood container contrast for ${preset.name} (dark=$isDark): $containerContrast",
                    containerContrast >= 7.0
                )
            }
        }
    }
}
