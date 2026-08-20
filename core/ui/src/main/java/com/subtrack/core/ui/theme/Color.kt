package com.subtrack.core.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// OLED Pitch Black & Obsidian Hierarchy
// ==========================================
val OledBlack = Color(0xFF000000)
val DarkBg = OledBlack
val DarkSurface = Color(0xFF0A0A0A)
val DarkSurfaceVariant = Color(0xFF121212)
val DarkBorder = Color(0xFF1C1C1E)          // Subtle non-contrasty 1dp hairline border
val DarkBorderSubtle = Color(0x14FFFFFF)    // 8% white hairline alpha


// ==========================================
// Primary Bright Accent: Material Sky Blue
// ==========================================
val Sky200 = Color(0xFFBAE6FD)
val Sky300 = Color(0xFF7DD3FC)
val Sky400 = Color(0xFF38BDF8)
val Sky500 = Color(0xFF0EA5E9)
val Sky600 = Color(0xFF0284C7)
val Sky700 = Color(0xFF0369A1)
val SkyContainer = Color(0xFF082F49)
val SkyBgSubtle = Color(0x1A38BDF8)

val PrimarySky = Sky400
val PrimarySkyBright = Sky300
val PrimarySkyContainer = SkyContainer

// Legacy compatibility aliases mapped to Sky Blue
val Emerald50 = Sky200
val Emerald100 = Sky300
val Emerald400 = Sky400
val Emerald500 = Sky400
val Emerald600 = Sky500
val Emerald700 = Sky600
val MintBright = Sky400
val MintPrimary = Sky400
val MintContainer = SkyContainer
val MintBgSubtle = SkyBgSubtle

// ==========================================
// Sparse Secondary Accent: Cyber Indigo
// ==========================================
val CyberIndigo = Color(0xFF6366F1)
val CyberIndigoLight = Color(0xFF818CF8)
val CyberIndigoContainer = Color(0xFF1E1B4B)
val BlackoutIndigo = CyberIndigo
val BlackoutIndigoBg = CyberIndigoContainer

// ==========================================
// Semantic Alert & Status Colors
// ==========================================
val ActiveSky = PrimarySky
val ActiveSkyBg = PrimarySkyContainer
val ActiveGreen = ActiveSky
val ActiveGreenBg = ActiveSkyBg
val AlertCrimson = Color(0xFFEF4444)
val AlertCrimsonBg = Color(0xFF450A0A)
val ErrorRose = AlertCrimson
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0xFF451A03)

// ==========================================
// Typography & Contrast Hierarchy
// ==========================================
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFA1A1AA)
val TextTertiaryDark = Color(0xFF71717A)

