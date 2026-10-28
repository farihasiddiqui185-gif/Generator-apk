package com.example.robloxshorts.data.model

enum class ShortsCategory(
    val title: String,
    val subtitle: String,
    val iconName: String,
    val primaryColorHex: Long
) {
    HORROR("Roblox Horror", "Doors, The Mimic & 3AM Creepy Elevators", "warning", 0xFFFF0055),
    FUNNY("Funny Moments", "Epic obby fails & troll admin chaos", "mood", 0xFF00F0FF),
    EMOTIONAL("Emotional Stories", "Noob Bacon Hair redemption & friendship", "favorite", 0xFFEC4899),
    RICH_VS_POOR("Rich vs Poor", "0 Robux vs 100,000,000 Dominus flex", "attach_money", 0xFFFACC15),
    SCHOOL("School Stories", "Strict teacher escapes & detention drama", "school", 0xFFA855F7),
    MYSTERY("Mysteries", "Brookhaven secret codes & glitched players", "search", 0xFF3B82F6),
    SURVIVAL("Survival", "Surviving 99 days of meteor floods & lava", "shield", 0xFF10B981),
    PLOT_TWIST("Plot Twists", "Unbelievable betrayals & hidden impostors", "bolt", 0xFFFF5722)
}

enum class VideoDuration(val seconds: Int, val label: String) {
    SEC_15(15, "15s (Fast Short)"),
    SEC_30(30, "30s (Viral Standard)"),
    SEC_45(45, "45s (Deep Story)"),
    SEC_60(60, "60s (Full Episode)")
}

enum class VideoLanguage(val code: String, val displayName: String) {
    EN("en", "English 🇺🇸"),
    HI("hi", "Hindi 🇮🇳"),
    ES("es", "Spanish 🇪🇸"),
    PT("pt", "Portuguese 🇧🇷"),
    DE("de", "German 🇩🇪"),
    FR("fr", "French 🇫🇷"),
    JA("ja", "Japanese 🇯🇵")
}

data class BulkGenerationParams(
    val quantity: Int = 1,
    val category: ShortsCategory = ShortsCategory.HORROR,
    val duration: VideoDuration = VideoDuration.SEC_30,
    val language: VideoLanguage = VideoLanguage.EN,
    val customPromptPrompt: String = "",
    val soundEffectsEnabled: Boolean = true,
    val bgMusicEnabled: Boolean = true,
    val animatedSubtitlesEnabled: Boolean = true
)
