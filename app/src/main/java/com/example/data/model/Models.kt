package com.example.data.model

enum class ThemePreset(val labelAr: String) {
    AMOLED_DARK("ليلي عميق (AMOLED)"),
    SLATE_DARK("داكن أردوازي"),
    MIDNIGHT_WARM("ليلي دافئ"),
    CLEAN_LIGHT("نهاري خفيف")
}

enum class AccentPreset(val labelAr: String) {
    ROSE_VIOLET("وردي بنفسجي"),
    AMBER_GOLD("عنبر ذهبي"),
    EMERALD_MINT("زمردي"),
    SKY_CYAN("أزرق سماوي")
}

enum class CornerDensity(val labelAr: String) {
    COMPACT("حواف مدمجة"),
    STANDARD("حواف قياسية"),
    COMFORTABLE("حواف ناعمة")
}

enum class MediaType(val labelAr: String) {
    PHOTO("صورة"),
    VIDEO("فيديو")
}

enum class FilterPreset(
    val labelAr: String,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val brightness: Float
) {
    ORIGINAL("أصلي", 1.0f, 1.0f, 0.0f, 0.0f),
    NOIR_CINEMA("سينما داكن", 1.25f, 0.15f, -0.05f, -0.04f),
    WARM_DUSK("غروب دافئ", 1.08f, 1.18f, 0.28f, 0.03f),
    CRISP_SILVER("فضي حاد", 1.20f, 0.0f, 0.0f, 0.02f),
    NEON_COOL("أزرق ليلي", 1.12f, 1.25f, -0.25f, -0.02f),
    ANALOG_FILM("فيلم كلاسيكي", 0.94f, 0.82f, 0.14f, 0.04f)
}

enum class FeedSortMode(val labelAr: String) {
    ALL("الكل"),
    CHRONOLOGICAL("الأحدث زمنياً"),
    ALGORITHM("الخوارزمية المخصصة"),
    FAVORITES("المفضلة"),
    VIDEOS_ONLY("المقاطع فقط")
}

enum class SearchFilterType(val labelAr: String) {
    ALL("الكل"),
    PHOTOS("صور"),
    VIDEOS("فيديو"),
    TAGS("وسوم"),
    ACCOUNTS("حسابات")
}

enum class AspectRatioOption(val labelAr: String, val ratio: Float) {
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 0.8f),
    TALL("3:4", 0.75f),
    LANDSCAPE("16:9", 1.777f)
}

data class AlgorithmWeights(
    val recencyWeight: Float = 0.35f,
    val tagAffinityWeight: Float = 0.40f,
    val creatorPriorityWeight: Float = 0.25f,
    val videoPreferenceBoost: Float = 0.15f
)

data class AppPreferencesState(
    val themePreset: ThemePreset = ThemePreset.AMOLED_DARK,
    val accentPreset: AccentPreset = AccentPreset.ROSE_VIOLET,
    val cornerDensity: CornerDensity = CornerDensity.STANDARD,
    val distractionFreeMode: Boolean = false,
    val permanentOfflineMode: Boolean = true,
    val optionalCloudSyncEnabled: Boolean = false,
    val cloudSyncProfileNote: String = "",
    val lastSyncTimestamp: Long = 0L,
    val masterPasswordEnabled: Boolean = false,
    val lockOnLaunch: Boolean = false,
    val dailyUsageLimitMinutes: Int = 45,
    val productivityAlertsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = false,
    val algorithmWeights: AlgorithmWeights = AlgorithmWeights(),
    val blockedTags: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList()
)

data class RankedPost(
    val post: com.example.data.local.PostEntity,
    val score: Double,
    val matchReasons: List<String>
)

data class DecryptedMessage(
    val id: Long,
    val threadId: String,
    val senderHandle: String,
    val isFromMe: Boolean,
    val body: String,
    val attachmentUri: String?,
    val createdAt: Long,
    val isRead: Boolean,
    val isEncryptedAtRest: Boolean
)

data class DecryptedThread(
    val threadId: String,
    val participantHandle: String,
    val participantName: String,
    val participantAvatar: String,
    val isVerified: Boolean,
    val isOnline: Boolean,
    val lastMessagePreview: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isEncrypted: Boolean
)

data class DiagnosticReport(
    val aesCipherSelfTestPassed: Boolean,
    val databaseAccessible: Boolean,
    val totalEncryptedMessages: Int,
    val totalPostsCached: Int,
    val estimatedStorageKb: Long,
    val adsTrackersBlocked: String = "0 إعلان / 0 متتبع خارجي",
    val aiEngineStatus: String = "معطل بالكامل (0% ذكاء اصطناعي)",
    val offlineReadiness: String = "جاهز 100% دون اتصال"
)
