package com.subtrack.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.subtrack.core.ui.R
import com.subtrack.core.ui.theme.BrandDarkBackground
import com.subtrack.core.ui.theme.BrandDarkHeroContainer
import com.subtrack.core.ui.theme.BrandLightBackground
import com.subtrack.core.ui.theme.BrandLightHeroContainer
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme

/**
 * Full-screen Material 3 Expressive editorial backdrop:
 * keeps main surfaces cleanly neutral (~70%) with subtle sculptural silk waves
 * peeking through the top mask, while giving Welcome / Reveal screens an emphasized
 * Botanical Emerald atmospheric wash.
 */
@Composable
fun ExpressiveScreenBackdrop(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val baseColor = if (isDark) BrandDarkBackground else BrandLightBackground
    val imageAlpha = when {
        isDark && emphasized -> 0.40f
        isDark -> 0.23f
        emphasized -> 0.48f
        else -> 0.24f
    }
    val maskBrush = if (isDark) {
        if (emphasized) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0E2920).copy(alpha = 0.70f),
                    BrandDarkBackground.copy(alpha = 0.86f),
                    BrandDarkBackground.copy(alpha = 0.95f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF131B19).copy(alpha = 0.74f),
                    BrandDarkBackground.copy(alpha = 0.89f),
                    BrandDarkBackground.copy(alpha = 0.96f)
                )
            )
        }
    } else {
        if (emphasized) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFDDF5E9).copy(alpha = 0.72f),
                    BrandLightBackground.copy(alpha = 0.88f),
                    BrandLightBackground.copy(alpha = 0.96f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEFF3F1).copy(alpha = 0.82f),
                    BrandLightBackground.copy(alpha = 0.92f),
                    BrandLightBackground.copy(alpha = 0.98f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_expressive_silk_waves),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = imageAlpha,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(maskBrush)
        )
        content()
    }
}

/**
 * Editorial duotone Hero Card backdrop:
 * concentrates the signature Botanical Emerald + sculptural silk waves texture
 * inside the Hero Bento centerpiece so the rest of the screen breathes calmly in neutral tones.
 */
@Composable
fun ExpressiveHeroCardBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val baseColor = if (isDark) BrandDarkHeroContainer else BrandLightHeroContainer
    val maskBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF154536).copy(alpha = 0.76f),
                Color(0xFF102E25).copy(alpha = 0.88f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFC8F1DF).copy(alpha = 0.72f),
                Color(0xFFE2F7EE).copy(alpha = 0.86f)
            )
        )
    }

    Box(
        modifier = modifier.background(baseColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_expressive_silk_waves),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.34f else 0.46f,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(maskBrush)
        )
        content()
    }
}
