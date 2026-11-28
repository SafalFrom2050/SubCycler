package com.subtrack.core.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.subtrack.core.domain.model.AppLauncherIcon

val LocalSubCyclerDarkTheme = staticCompositionLocalOf { false }
val LocalSubCyclerAccentIcon = staticCompositionLocalOf { AppLauncherIcon.BOTANICAL }
val LocalSubCyclerAccentTokens = staticCompositionLocalOf {
    launcherAccentTokensFor(AppLauncherIcon.BOTANICAL, isDark = false)
}
val LocalSubCyclerSwirlColors = staticCompositionLocalOf {
    BrandColorHarmonizer.DefaultBotanicalSwirl
}

/**
 * Curated Bold Color-Block Material 3 Expressive focal accent bundle matching each [AppLauncherIcon].
 *
 * Keeps SubCycler's crisp neutral canvas untouched while driving:
 * - Bold saturated `primary` & rich jewel `primaryContainer` across tabs, chips, waves, and widgets
 * - Pure flat single-hue color-block `heroBase` + crisp `heroContent` ink (zero multi-hue rainbow)
 * - Contrasting triadic `heroPop` / `onHeroPop` accents for the `SAVE` cookie seal, active chip, and `+` FAB
 */
data class LauncherAccentTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val heroBase: Color,
    val heroGradientStart: Color = heroBase,
    val heroGradientMid: Color = heroBase,
    val heroGradientEnd: Color = heroBase,
    val heroContent: Color = Color.White,
    val heroPop: Color = BrandTriadicLime,
    val onHeroPop: Color = BrandOnTriadicLime
)

fun launcherAccentTokensFor(
    icon: AppLauncherIcon,
    isDark: Boolean
): LauncherAccentTokens = when (icon) {
    AppLauncherIcon.BOTANICAL -> if (!isDark) {
        LauncherAccentTokens(
            primary = BrandLightPrimary,
            onPrimary = BrandLightOnPrimary,
            primaryContainer = BrandLightPrimaryContainer,
            onPrimaryContainer = BrandLightOnPrimaryContainer,
            secondary = BrandLightSecondary,
            onSecondary = BrandLightOnSecondary,
            secondaryContainer = BrandLightSecondaryContainer,
            onSecondaryContainer = BrandLightOnSecondaryContainer,
            heroBase = BrandLightHeroContainer,
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    } else {
        LauncherAccentTokens(
            primary = BrandDarkPrimary,
            onPrimary = BrandDarkOnPrimary,
            primaryContainer = BrandDarkPrimaryContainer,
            onPrimaryContainer = BrandDarkOnPrimaryContainer,
            secondary = BrandDarkSecondary,
            onSecondary = BrandDarkOnSecondary,
            secondaryContainer = BrandDarkSecondaryContainer,
            onSecondaryContainer = BrandDarkOnSecondaryContainer,
            heroBase = BrandDarkHeroContainer,
            heroContent = Color(0xFFF2FFF9),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    }

    AppLauncherIcon.CINEMA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFFC4123B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFCDD7),
            onPrimaryContainer = Color(0xFF470011),
            secondary = Color(0xFFA61E4D),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFD1E1),
            onSecondaryContainer = Color(0xFF42041B),
            heroBase = Color(0xFFBA1138),
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicGold,
            onHeroPop = BrandOnTriadicGold
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFFF6B86),
            onPrimary = Color(0xFF4A0012),
            primaryContainer = Color(0xFF7D112C),
            onPrimaryContainer = Color(0xFFFFE3E8),
            secondary = Color(0xFFFF7AA8),
            onSecondary = Color(0xFF45031B),
            secondaryContainer = Color(0xFF6B1435),
            onSecondaryContainer = Color(0xFFFFE3EE),
            heroBase = Color(0xFF941233),
            heroContent = Color(0xFFFFF5F7),
            heroPop = BrandTriadicGold,
            onHeroPop = BrandOnTriadicGold
        )
    }

    AppLauncherIcon.OCEAN -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF1D4ED8),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFC8DCFF),
            onPrimaryContainer = Color(0xFF041C5C),
            secondary = Color(0xFF026AA2),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFC3E8FF),
            onSecondaryContainer = Color(0xFF03253D),
            heroBase = Color(0xFF1A47C7),
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFF6EA8FF),
            onPrimary = Color(0xFF041C54),
            primaryContainer = Color(0xFF143C8A),
            onPrimaryContainer = Color(0xFFE0ECFF),
            secondary = Color(0xFF56CCF2),
            onSecondary = Color(0xFF032436),
            secondaryContainer = Color(0xFF0E4769),
            onSecondaryContainer = Color(0xFFD9F3FF),
            heroBase = Color(0xFF163B9E),
            heroContent = Color(0xFFF2F7FF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    }

    AppLauncherIcon.TERRACOTTA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFFB83E0A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD6BF),
            onPrimaryContainer = Color(0xFF421200),
            secondary = Color(0xFF9E4B0A),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDFC2),
            onSecondaryContainer = Color(0xFF3A1800),
            heroBase = Color(0xFFB03808),
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicMint,
            onHeroPop = BrandOnTriadicMint
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFFF965A),
            onPrimary = Color(0xFF421400),
            primaryContainer = Color(0xFF7A2B0A),
            onPrimaryContainer = Color(0xFFFFE5D6),
            secondary = Color(0xFFFFB266),
            onSecondary = Color(0xFF3D1A00),
            secondaryContainer = Color(0xFF69330A),
            onSecondaryContainer = Color(0xFFFFE9D4),
            heroBase = Color(0xFF8F2E08),
            heroContent = Color(0xFFFFF6F2),
            heroPop = BrandTriadicMint,
            onHeroPop = BrandOnTriadicMint
        )
    }

    AppLauncherIcon.AURORA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF5B21B6),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDFD1FF),
            onPrimaryContainer = Color(0xFF1F0654),
            secondary = Color(0xFF7E22CE),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFEBD2FF),
            onSecondaryContainer = Color(0xFF2E0554),
            heroBase = Color(0xFF551EB0),
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFB69EFF),
            onPrimary = Color(0xFF21075E),
            primaryContainer = Color(0xFF3E1D8A),
            onPrimaryContainer = Color(0xFFEDE6FF),
            secondary = Color(0xFFD09EFF),
            onSecondary = Color(0xFF2D0554),
            secondaryContainer = Color(0xFF4F1A80),
            onSecondaryContainer = Color(0xFFF3E6FF),
            heroBase = Color(0xFF461A8C),
            heroContent = Color(0xFFF6F2FF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    }

    AppLauncherIcon.OBSIDIAN -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF243038),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD5DFE6),
            onPrimaryContainer = Color(0xFF10181E),
            secondary = Color(0xFF3A4954),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFDCE4EA),
            onSecondaryContainer = Color(0xFF162027),
            heroBase = Color(0xFF1F2930),
            heroContent = Color(0xFFFFFFFF),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFD0DEE8),
            onPrimary = Color(0xFF101920),
            primaryContainer = Color(0xFF2F3D47),
            onPrimaryContainer = Color(0xFFEAF1F6),
            secondary = Color(0xFFBACAD6),
            onSecondary = Color(0xFF152028),
            secondaryContainer = Color(0xFF2A3640),
            onSecondaryContainer = Color(0xFFE4ECF2),
            heroBase = Color(0xFF232D35),
            heroContent = Color(0xFFF2F6F9),
            heroPop = BrandTriadicLime,
            onHeroPop = BrandOnTriadicLime
        )
    }
}

fun buildSubCyclerColorScheme(
    darkTheme: Boolean,
    accentIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
    accentTokensOverride: LauncherAccentTokens? = null
): ColorScheme {
    val accent = accentTokensOverride ?: launcherAccentTokensFor(accentIcon, darkTheme)
    return if (darkTheme) {
        darkColorScheme(
            primary = accent.primary,
            onPrimary = accent.onPrimary,
            primaryContainer = accent.primaryContainer,
            onPrimaryContainer = accent.onPrimaryContainer,
            secondary = accent.secondary,
            onSecondary = accent.onSecondary,
            secondaryContainer = accent.secondaryContainer,
            onSecondaryContainer = accent.onSecondaryContainer,
            tertiary = BrandDarkTertiary,
            onTertiary = BrandDarkOnTertiary,
            tertiaryContainer = BrandDarkTertiaryContainer,
            onTertiaryContainer = BrandDarkOnTertiaryContainer,
            error = BrandDarkError,
            onError = BrandDarkOnError,
            errorContainer = BrandDarkErrorContainer,
            onErrorContainer = BrandDarkOnErrorContainer,
            background = BrandDarkBackground,
            onBackground = BrandDarkOnSurface,
            surface = BrandDarkSurface,
            onSurface = BrandDarkOnSurface,
            surfaceVariant = BrandDarkSurfaceContainer,
            onSurfaceVariant = BrandDarkOnSurfaceVariant,
            surfaceContainerLowest = BrandDarkSurfaceContainerLowest,
            surfaceContainerLow = BrandDarkSurfaceContainerLow,
            surfaceContainer = BrandDarkSurfaceContainer,
            surfaceContainerHigh = BrandDarkSurfaceContainerHigh,
            surfaceContainerHighest = BrandDarkSurfaceContainerHighest,
            outline = BrandDarkOutline,
            outlineVariant = BrandDarkOutlineVariant
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = accent.onPrimary,
            primaryContainer = accent.primaryContainer,
            onPrimaryContainer = accent.onPrimaryContainer,
            secondary = accent.secondary,
            onSecondary = accent.onSecondary,
            secondaryContainer = accent.secondaryContainer,
            onSecondaryContainer = accent.onSecondaryContainer,
            tertiary = BrandLightTertiary,
            onTertiary = BrandLightOnTertiary,
            tertiaryContainer = BrandLightTertiaryContainer,
            onTertiaryContainer = BrandLightOnTertiaryContainer,
            error = BrandLightError,
            onError = BrandLightOnError,
            errorContainer = BrandLightErrorContainer,
            onErrorContainer = BrandLightOnErrorContainer,
            background = BrandLightBackground,
            onBackground = BrandLightOnSurface,
            surface = BrandLightSurface,
            onSurface = BrandLightOnSurface,
            surfaceVariant = BrandLightSurfaceContainer,
            onSurfaceVariant = BrandLightOnSurfaceVariant,
            surfaceContainerLowest = BrandLightSurfaceContainerLowest,
            surfaceContainerLow = BrandLightSurfaceContainerLow,
            surfaceContainer = BrandLightSurfaceContainer,
            surfaceContainerHigh = BrandLightSurfaceContainerHigh,
            surfaceContainerHighest = BrandLightSurfaceContainerHighest,
            outline = BrandLightOutline,
            outlineVariant = BrandLightOutlineVariant
        )
    }
}

@Composable
fun SubTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL,
    accentTokensOverride: LauncherAccentTokens? = null,
    swirlColorsOverride: SwirlLogoColors? = null,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion)
    val baseAccent = accentTokensOverride ?: launcherAccentTokensFor(accentIcon, darkTheme)
    val effectiveSwirl = swirlColorsOverride ?: BrandColorHarmonizer.swirlColorsForLauncherIcon(accentIcon)
    val baseScheme = buildSubCyclerColorScheme(
        darkTheme = darkTheme,
        accentIcon = accentIcon,
        accentTokensOverride = accentTokensOverride
    )

    val primary by animateColorAsState(baseScheme.primary, colorSpec, label = "themePrimary")
    val onPrimary by animateColorAsState(baseScheme.onPrimary, colorSpec, label = "themeOnPrimary")
    val primaryContainer by animateColorAsState(baseScheme.primaryContainer, colorSpec, label = "themePrimaryContainer")
    val onPrimaryContainer by animateColorAsState(baseScheme.onPrimaryContainer, colorSpec, label = "themeOnPrimaryContainer")
    val secondary by animateColorAsState(baseScheme.secondary, colorSpec, label = "themeSecondary")
    val onSecondary by animateColorAsState(baseScheme.onSecondary, colorSpec, label = "themeOnSecondary")
    val secondaryContainer by animateColorAsState(baseScheme.secondaryContainer, colorSpec, label = "themeSecondaryContainer")
    val onSecondaryContainer by animateColorAsState(baseScheme.onSecondaryContainer, colorSpec, label = "themeOnSecondaryContainer")
    val heroBase by animateColorAsState(baseAccent.heroBase, colorSpec, label = "themeHeroBase")
    val heroContent by animateColorAsState(baseAccent.heroContent, colorSpec, label = "themeHeroContent")
    val heroPop by animateColorAsState(baseAccent.heroPop, colorSpec, label = "themeHeroPop")
    val onHeroPop by animateColorAsState(baseAccent.onHeroPop, colorSpec, label = "themeOnHeroPop")

    val animatedAccentTokens = baseAccent.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        heroBase = heroBase,
        heroGradientStart = heroBase,
        heroGradientMid = heroBase,
        heroGradientEnd = heroBase,
        heroContent = heroContent,
        heroPop = heroPop,
        onHeroPop = onHeroPop
    )

    val colorScheme = baseScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSubCyclerDarkTheme provides darkTheme,
        LocalSubCyclerAccentIcon provides accentIcon,
        LocalSubCyclerAccentTokens provides animatedAccentTokens,
        LocalSubCyclerSwirlColors provides effectiveSwirl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
