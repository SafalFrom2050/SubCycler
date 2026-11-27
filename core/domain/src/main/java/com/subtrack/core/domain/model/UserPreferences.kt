package com.subtrack.core.domain.model

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Curated launcher app icon colorways corresponding to pre-declared `<activity-alias>` entries.
 * Switching happens strictly on explicit user selection in Settings.
 */
enum class AppLauncherIcon(
    val displayName: String,
    val subtitle: String,
    val primaryHex: String,
    val lightPetalHex: String,
    val midPetalHex: String,
    val deepPetalHex: String,
    val backdropHex: String,
    val aliasClassName: String
) {
    BOTANICAL(
        displayName = "Botanical",
        subtitle = "Classic emerald",
        primaryHex = "#0D8F67",
        lightPetalHex = "#B8FCE1",
        midPetalHex = "#4FE3A4",
        deepPetalHex = "#0D8F67",
        backdropHex = "#02151B",
        aliasClassName = "com.subtrack.app.MainActivityAliasBotanical"
    ),
    CINEMA(
        displayName = "Cinema",
        subtitle = "Ruby & rose",
        primaryHex = "#D93848",
        lightPetalHex = "#FFD5DC",
        midPetalHex = "#F46B7D",
        deepPetalHex = "#B71C32",
        backdropHex = "#1A080C",
        aliasClassName = "com.subtrack.app.MainActivityAliasCinema"
    ),
    OCEAN(
        displayName = "Ocean",
        subtitle = "Sapphire & sky",
        primaryHex = "#2962D9",
        lightPetalHex = "#CDE6FF",
        midPetalHex = "#5CA8FF",
        deepPetalHex = "#1A4BB8",
        backdropHex = "#061124",
        aliasClassName = "com.subtrack.app.MainActivityAliasOcean"
    ),
    TERRACOTTA(
        displayName = "Terracotta",
        subtitle = "Warm amber",
        primaryHex = "#D97745",
        lightPetalHex = "#FFE3CA",
        midPetalHex = "#F5A26B",
        deepPetalHex = "#B85324",
        backdropHex = "#1C0F08",
        aliasClassName = "com.subtrack.app.MainActivityAliasTerracotta"
    ),
    AURORA(
        displayName = "Aurora",
        subtitle = "Iris & violet",
        primaryHex = "#6D5CE7",
        lightPetalHex = "#E4DCFF",
        midPetalHex = "#9B87FF",
        deepPetalHex = "#513CC7",
        backdropHex = "#0F0B21",
        aliasClassName = "com.subtrack.app.MainActivityAliasAurora"
    ),
    OBSIDIAN(
        displayName = "Obsidian",
        subtitle = "Silver slate",
        primaryHex = "#6C7880",
        lightPetalHex = "#E7EDF0",
        midPetalHex = "#A2B0B8",
        deepPetalHex = "#58656E",
        backdropHex = "#101417",
        aliasClassName = "com.subtrack.app.MainActivityAliasObsidian"
    )
}

data class UserPreferences(
    val currency: String = "USD",
    val notificationLeadDays: Int = 3,
    val isAutoCancelApprovalRequired: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val pendingSubscriptionAction: PendingSubscriptionAction? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useStackHeroMood: Boolean = true,
    val launcherIcon: AppLauncherIcon = AppLauncherIcon.BOTANICAL
)
