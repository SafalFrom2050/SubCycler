package com.subtrack.core.domain.model

import java.util.Locale

data class SubscriptionPreset(
    val name: String,
    val defaultCost: Double,
    val category: SubscriptionCategory,
    val colorHex: String,
    val iconName: String,
    val suggestedStrategy: RotationStrategy = RotationStrategy.ROTATION_45_DAYS,
    val suggestedActiveDays: Int = 30,
    val suggestedBlackoutDays: Int = 15
)

/**
 * Curated offline Top 20 subscription preset library with authentic brand accent colors.
 * Brand colors are harmonized into soft Material 3 pastel palettes at render time so
 * third-party services feel instantly recognizable without bundling trademarked logos.
 */
object PresetLibrary {
    val presets = listOf(
        SubscriptionPreset(
            name = "Netflix",
            defaultCost = 17.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#E50914",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Disney+",
            defaultCost = 15.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#113CCF",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "YouTube Premium",
            defaultCost = 13.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#FF0033",
            iconName = "Play",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Amazon Prime Video",
            defaultCost = 8.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#00A8E1",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Max (HBO)",
            defaultCost = 16.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#5822B4",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Hulu",
            defaultCost = 9.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#1CE783",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Apple TV+",
            defaultCost = 9.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#2C2C2E",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Crunchyroll",
            defaultCost = 7.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#F47521",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Spotify",
            defaultCost = 11.99,
            category = SubscriptionCategory.MUSIC,
            colorHex = "#1DB954",
            iconName = "Music",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Apple Music",
            defaultCost = 10.99,
            category = SubscriptionCategory.MUSIC,
            colorHex = "#FA243C",
            iconName = "Music",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "ChatGPT Plus",
            defaultCost = 20.00,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#10A37F",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Claude Pro",
            defaultCost = 20.00,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#D97757",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Google AI (Gemini)",
            defaultCost = 19.99,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#6366F1",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Notion Plus",
            defaultCost = 12.00,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#5C544B",
            iconName = "Layers",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Microsoft 365",
            defaultCost = 9.99,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#0078D4",
            iconName = "Layers",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Xbox Game Pass",
            defaultCost = 19.99,
            category = SubscriptionCategory.GAMING,
            colorHex = "#107C10",
            iconName = "Gamepad",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "PlayStation Plus",
            defaultCost = 14.99,
            category = SubscriptionCategory.GAMING,
            colorHex = "#00439C",
            iconName = "Gamepad",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Nintendo Switch Online",
            defaultCost = 3.99,
            category = SubscriptionCategory.GAMING,
            colorHex = "#E60012",
            iconName = "Gamepad",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "iCloud+",
            defaultCost = 2.99,
            category = SubscriptionCategory.CLOUD,
            colorHex = "#5E5CE6",
            iconName = "Cloud",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Google One",
            defaultCost = 1.99,
            category = SubscriptionCategory.CLOUD,
            colorHex = "#1A73E8",
            iconName = "Cloud",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        )
    )

    private val keywordColorHints: List<Pair<String, String>> = listOf(
        "netflix" to "#E50914",
        "disney" to "#113CCF",
        "youtube" to "#FF0033",
        "prime video" to "#00A8E1",
        "amazon prime" to "#00A8E1",
        "hbo" to "#5822B4",
        "max" to "#5822B4",
        "hulu" to "#1CE783",
        "apple tv" to "#2C2C2E",
        "crunchyroll" to "#F47521",
        "spotify" to "#1DB954",
        "apple music" to "#FA243C",
        "chatgpt" to "#10A37F",
        "openai" to "#10A37F",
        "claude" to "#D97757",
        "anthropic" to "#D97757",
        "gemini" to "#6366F1",
        "google ai" to "#6366F1",
        "notion" to "#5C544B",
        "microsoft 365" to "#0078D4",
        "office 365" to "#0078D4",
        "xbox" to "#107C10",
        "playstation" to "#00439C",
        "ps plus" to "#00439C",
        "nintendo" to "#E60012",
        "icloud" to "#5E5CE6",
        "google one" to "#1A73E8",
        "discord" to "#5865F2",
        "duolingo" to "#58CC02",
        "audible" to "#F8991D",
        "adobe" to "#EB1000",
        "paramount" to "#0064FF",
        "twitch" to "#9146FF",
        "perplexity" to "#20808D",
        "cursor" to "#3D4451",
        "dazn" to "#C8E600",
        "u-next" to "#00A7E1"
    )

    private val keywordColorRegexes: List<Pair<Regex, String>> = keywordColorHints.map { (keyword, hex) ->
        Regex("""(?<![a-z0-9])${Regex.escape(keyword)}(?![a-z0-9])""") to hex
    }

    /**
     * Resolves an authentic brand hex color for a known preset or popular custom-typed service name
     * using word-boundary matching so substrings like "hbo" inside "Neighborhood" never false-match.
     * Returns `null` when the service name is unfamiliar so callers can fall back cleanly.
     */
    fun brandColorHexForName(serviceName: String): String? {
        val normalized = serviceName.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return null
        presets.firstOrNull { it.name.lowercase(Locale.ROOT) == normalized }?.let { return it.colorHex }
        return keywordColorRegexes.firstOrNull { (regex, _) -> regex.containsMatchIn(normalized) }?.second
    }

    /**
     * Curated swatch palette offered in the custom subscription color picker.
     */
    val customColorSwatches: List<Pair<String, String>> = listOf(
        "Emerald" to "#0D8F67",
        "Ruby" to "#E50914",
        "Berry" to "#FA243C",
        "Tangerine" to "#F47521",
        "Terracotta" to "#D97757",
        "Meadow" to "#1DB954",
        "Sky" to "#00A8E1",
        "Sapphire" to "#113CCF",
        "Iris" to "#6366F1",
        "Violet" to "#5822B4",
        "Stone" to "#5C544B",
        "Graphite" to "#2C2C2E"
    )
}
