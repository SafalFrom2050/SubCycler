package com.subtrack.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalSubCyclerDarkTheme = staticCompositionLocalOf { false }

private val SubCyclerLightColorScheme = lightColorScheme(
    primary = BrandLightPrimary,
    onPrimary = BrandLightOnPrimary,
    primaryContainer = BrandLightPrimaryContainer,
    onPrimaryContainer = BrandLightOnPrimaryContainer,
    secondary = BrandLightSecondary,
    onSecondary = BrandLightOnSecondary,
    secondaryContainer = BrandLightSecondaryContainer,
    onSecondaryContainer = BrandLightOnSecondaryContainer,
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

private val SubCyclerDarkColorScheme = darkColorScheme(
    primary = BrandDarkPrimary,
    onPrimary = BrandDarkOnPrimary,
    primaryContainer = BrandDarkPrimaryContainer,
    onPrimaryContainer = BrandDarkOnPrimaryContainer,
    secondary = BrandDarkSecondary,
    onSecondary = BrandDarkOnSecondary,
    secondaryContainer = BrandDarkSecondaryContainer,
    onSecondaryContainer = BrandDarkOnSecondaryContainer,
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

@Composable
fun SubTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SubCyclerDarkColorScheme else SubCyclerLightColorScheme

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

    CompositionLocalProvider(LocalSubCyclerDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
