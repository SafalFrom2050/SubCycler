package com.subtrack.core.domain.model

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Curated Bold Color-Block launcher app icon colorways corresponding to pre-declared
 * `<activity-alias>` entries. Switching happens strictly on explicit user selection in Settings.
 */
enum class AppLauncherIcon(
    val displayName: String,
    val subtitle: String,
    val primaryHex: String,
    val lightPetalHex: String,
    val midPetalHex: String,
    val deepPetalHex: String,
    val backdropHex: String,
    val waveAccentHex: String,
    val aliasClassName: String
) {
    BOTANICAL(
        displayName = "Botanical",
        subtitle = "Emerald & lime",
        primaryHex = "#008256",
        lightPetalHex = "#D4FF5E",
        midPetalHex = "#22F09E",
        deepPetalHex = "#008A5A",
        backdropHex = "#03261A",
        waveAccentHex = "#D4FF5E",
        aliasClassName = "com.subtrack.app.MainActivityAliasBotanical"
    ),
    CINEMA(
        displayName = "Cinema",
        subtitle = "Ruby & gold",
        primaryHex = "#C4123B",
        lightPetalHex = "#FFD84D",
        midPetalHex = "#FF3B66",
        deepPetalHex = "#B50D36",
        backdropHex = "#240610",
        waveAccentHex = "#FFD84D",
        aliasClassName = "com.subtrack.app.MainActivityAliasCinema"
    ),
    OCEAN(
        displayName = "Ocean",
        subtitle = "Cobalt & lime",
        primaryHex = "#1D4ED8",
        lightPetalHex = "#4DF2FF",
        midPetalHex = "#2F78FF",
        deepPetalHex = "#1442C2",
        backdropHex = "#061336",
        waveAccentHex = "#D4FF5E",
        aliasClassName = "com.subtrack.app.MainActivityAliasOcean"
    ),
    TERRACOTTA(
        displayName = "Terracotta",
        subtitle = "Blaze & mint",
        primaryHex = "#B83E0A",
        lightPetalHex = "#FFD154",
        midPetalHex = "#FF6B26",
        deepPetalHex = "#B83406",
        backdropHex = "#260E04",
        waveAccentHex = "#5EFFC4",
        aliasClassName = "com.subtrack.app.MainActivityAliasTerracotta"
    ),
    AURORA(
        displayName = "Aurora",
        subtitle = "Iris & lime",
        primaryHex = "#5B21B6",
        lightPetalHex = "#D1A6FF",
        midPetalHex = "#8B5CF6",
        deepPetalHex = "#511AB8",
        backdropHex = "#130733",
        waveAccentHex = "#D4FF5E",
        aliasClassName = "com.subtrack.app.MainActivityAliasAurora"
    ),
    OBSIDIAN(
        displayName = "Obsidian",
        subtitle = "Carbon & lime",
        primaryHex = "#243038",
        lightPetalHex = "#D4FF5E",
        midPetalHex = "#DCE6EE",
        deepPetalHex = "#556673",
        backdropHex = "#0F1418",
        waveAccentHex = "#D4FF5E",
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
