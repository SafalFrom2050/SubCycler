package com.subtrack.core.domain.model

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

object PresetLibrary {
    val presets = listOf(
        SubscriptionPreset(
            name = "Netflix",
            defaultCost = 15.49,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#E50914",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Disney+",
            defaultCost = 13.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#113CCF",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Max (HBO)",
            defaultCost = 16.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#002BE7",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "YouTube Premium",
            defaultCost = 13.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#FF0000",
            iconName = "Play",
            suggestedStrategy = RotationStrategy.CONTINUOUS
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
            name = "Apple TV+",
            defaultCost = 9.99,
            category = SubscriptionCategory.STREAMING,
            colorHex = "#333333",
            iconName = "Tv",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
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
            colorHex = "#D97706",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Google AI Plus",
            defaultCost = 9.99,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#4285F4",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Google AI Pro",
            defaultCost = 19.99,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#4285F4",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Google AI Ultra",
            defaultCost = 249.99,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#4285F4",
            iconName = "Cpu",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
        ),
        SubscriptionPreset(
            name = "Notion Plus",
            defaultCost = 10.00,
            category = SubscriptionCategory.AI_TOOLS,
            colorHex = "#333333",
            iconName = "Layers",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        ),
        SubscriptionPreset(
            name = "Xbox Game Pass Ultimate",
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
            colorHex = "#003791",
            iconName = "Gamepad",
            suggestedStrategy = RotationStrategy.ROTATION_45_DAYS
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
            colorHex = "#4285F4",
            iconName = "Cloud",
            suggestedStrategy = RotationStrategy.CONTINUOUS
        )
    )
}
