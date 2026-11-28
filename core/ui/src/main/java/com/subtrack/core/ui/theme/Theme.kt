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

/**
 * Curated Light/Dark focal accent bundle matching each [AppLauncherIcon].
 * Keeps SubCycler's 70% architectural neutral surfaces untouched while swapping
 * every 30% brand focal accent (FAB, wordmark, savings stamp, sliders, chips, waves).
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
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color
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
            heroGradientStart = Color(0xFFC8F1DF),
            heroGradientMid = Color(0xFFD7F5E8),
            heroGradientEnd = Color(0xFFE2F7EE)
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
            heroGradientStart = Color(0xFF154536),
            heroGradientMid = Color(0xFF12392D),
            heroGradientEnd = Color(0xFF102E25)
        )
    }

    AppLauncherIcon.CINEMA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFFB8253A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9DE),
            onPrimaryContainer = Color(0xFF41000E),
            secondary = Color(0xFF963B4D),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFBDCE2),
            onSecondaryContainer = Color(0xFF3D0E18),
            heroBase = Color(0xFFFCD8DE),
            heroGradientStart = Color(0xFFFBD1D8),
            heroGradientMid = Color(0xFFFCE0E5),
            heroGradientEnd = Color(0xFFFDECEF)
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFFF9AA8),
            onPrimary = Color(0xFF4D0614),
            primaryContainer = Color(0xFF5A1B27),
            onPrimaryContainer = Color(0xFFFFDCE1),
            secondary = Color(0xFFF2A8B4),
            onSecondary = Color(0xFF430C18),
            secondaryContainer = Color(0xFF4B1D26),
            onSecondaryContainer = Color(0xFFFBDDE2),
            heroBase = Color(0xFF3B151D),
            heroGradientStart = Color(0xFF491923),
            heroGradientMid = Color(0xFF3A141C),
            heroGradientEnd = Color(0xFF2D1016)
        )
    }

    AppLauncherIcon.OCEAN -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF1F54C4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD6E4FF),
            onPrimaryContainer = Color(0xFF06215C),
            secondary = Color(0xFF2B6299),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD6E8FA),
            onSecondaryContainer = Color(0xFF0A2847),
            heroBase = Color(0xFFD2E2FF),
            heroGradientStart = Color(0xFFCBDDFF),
            heroGradientMid = Color(0xFFDCE8FF),
            heroGradientEnd = Color(0xFFE7F0FF)
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFF9BC2FF),
            onPrimary = Color(0xFF072257),
            primaryContainer = Color(0xFF19376D),
            onPrimaryContainer = Color(0xFFD8E6FF),
            secondary = Color(0xFF94C7F5),
            onSecondary = Color(0xFF082644),
            secondaryContainer = Color(0xFF1A3A59),
            onSecondaryContainer = Color(0xFFD5EAFC),
            heroBase = Color(0xFF142B52),
            heroGradientStart = Color(0xFF183463),
            heroGradientMid = Color(0xFF132A4F),
            heroGradientEnd = Color(0xFF10213F)
        )
    }

    AppLauncherIcon.TERRACOTTA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFFB55424),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFE0CE),
            onPrimaryContainer = Color(0xFF421704),
            secondary = Color(0xFF995C32),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFAE2D2),
            onSecondaryContainer = Color(0xFF3B1C08),
            heroBase = Color(0xFFFDE0CF),
            heroGradientStart = Color(0xFFFCD8C3),
            heroGradientMid = Color(0xFFFDE4D5),
            heroGradientEnd = Color(0xFFFEEFE6)
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFFFB58A),
            onPrimary = Color(0xFF4B1D05),
            primaryContainer = Color(0xFF5B2B13),
            onPrimaryContainer = Color(0xFFFFE2D1),
            secondary = Color(0xFFF0BA95),
            onSecondary = Color(0xFF40200B),
            secondaryContainer = Color(0xFF4D2C18),
            onSecondaryContainer = Color(0xFFFCE4D4),
            heroBase = Color(0xFF3D1F10),
            heroGradientStart = Color(0xFF4A2513),
            heroGradientMid = Color(0xFF3B1E10),
            heroGradientEnd = Color(0xFF2E170D)
        )
    }

    AppLauncherIcon.AURORA -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF5846D6),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE4DFFF),
            onPrimaryContainer = Color(0xFF1B0D66),
            secondary = Color(0xFF6E52A8),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFEAE1FA),
            onSecondaryContainer = Color(0xFF27134D),
            heroBase = Color(0xFFE1DBFF),
            heroGradientStart = Color(0xFFDAD2FF),
            heroGradientMid = Color(0xFFE6E0FF),
            heroGradientEnd = Color(0xFFEFEBFF)
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFC3B8FF),
            onPrimary = Color(0xFF200F6E),
            primaryContainer = Color(0xFF34277A),
            onPrimaryContainer = Color(0xFFE6E1FF),
            secondary = Color(0xFFC9B5F5),
            onSecondary = Color(0xFF2A1454),
            secondaryContainer = Color(0xFF38285E),
            onSecondaryContainer = Color(0xFFECE3FC),
            heroBase = Color(0xFF251C54),
            heroGradientStart = Color(0xFF2E2269),
            heroGradientMid = Color(0xFF241B52),
            heroGradientEnd = Color(0xFF1C1540)
        )
    }

    AppLauncherIcon.OBSIDIAN -> if (!isDark) {
        LauncherAccentTokens(
            primary = Color(0xFF47545E),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDDE4E9),
            onPrimaryContainer = Color(0xFF172128),
            secondary = Color(0xFF54616B),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE1E7EC),
            onSecondaryContainer = Color(0xFF1D262D),
            heroBase = Color(0xFFDCE2E7),
            heroGradientStart = Color(0xFFD4DCE2),
            heroGradientMid = Color(0xFFE1E7EC),
            heroGradientEnd = Color(0xFFEAEFF2)
        )
    } else {
        LauncherAccentTokens(
            primary = Color(0xFFC2CFD8),
            onPrimary = Color(0xFF162027),
            primaryContainer = Color(0xFF2D3840),
            onPrimaryContainer = Color(0xFFE2E9EE),
            secondary = Color(0xFFB8C5CE),
            onSecondary = Color(0xFF1B242B),
            secondaryContainer = Color(0xFF2C353C),
            onSecondaryContainer = Color(0xFFDFE6EB),
            heroBase = Color(0xFF212A30),
            heroGradientStart = Color(0xFF29343C),
            heroGradientMid = Color(0xFF212A30),
            heroGradientEnd = Color(0xFF192025)
        )
    }
}

fun buildSubCyclerColorScheme(
    darkTheme: Boolean,
    accentIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL
): ColorScheme {
    val accent = launcherAccentTokensFor(accentIcon, darkTheme)
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
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val colorSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion)
    val baseScheme = buildSubCyclerColorScheme(darkTheme = darkTheme, accentIcon = accentIcon)

    val primary by animateColorAsState(baseScheme.primary, colorSpec, label = "themePrimary")
    val onPrimary by animateColorAsState(baseScheme.onPrimary, colorSpec, label = "themeOnPrimary")
    val primaryContainer by animateColorAsState(baseScheme.primaryContainer, colorSpec, label = "themePrimaryContainer")
    val onPrimaryContainer by animateColorAsState(baseScheme.onPrimaryContainer, colorSpec, label = "themeOnPrimaryContainer")
    val secondary by animateColorAsState(baseScheme.secondary, colorSpec, label = "themeSecondary")
    val onSecondary by animateColorAsState(baseScheme.onSecondary, colorSpec, label = "themeOnSecondary")
    val secondaryContainer by animateColorAsState(baseScheme.secondaryContainer, colorSpec, label = "themeSecondaryContainer")
    val onSecondaryContainer by animateColorAsState(baseScheme.onSecondaryContainer, colorSpec, label = "themeOnSecondaryContainer")

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
        LocalSubCyclerAccentIcon provides accentIcon
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
