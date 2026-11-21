package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AspectRatioOption
import com.example.data.model.FilterPreset
import com.example.data.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        PostEntity::class,
        CommentEntity::class,
        StoryEntity::class,
        CollectionEntity::class,
        ChatThreadEntity::class,
        ChatMessageEntity::class,
        AppNotificationEntity::class,
        UsageStatEntity::class,
        SupportTicketEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun dao(): PrismDao

    companion object {
        @Volatile
        private var INSTANCE: PrismDatabase? = null

        fun getInstance(context: Context): PrismDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrismDatabase::class.java,
                    "ristam_encrypted_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(SeedCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class SeedCallback(private val context: Context) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = getInstance(context).dao()
                    initializeRealUserSpace(dao)
                }
            }
        }

        /**
         * Initializes ONLY the user's own private workspace (zero fake people or fictional chats),
         * populated with the user's local image & video showcase posts so the LazyColumn dark feed
         * is immediately functional and ready to customize or replace.
         */
        suspend fun initializeRealUserSpace(dao: PrismDao) {
            if (dao.getCurrentUserOnce() != null) return

            val now = System.currentTimeMillis()
            val hour = 3_600_000L

            dao.upsertProfile(
                UserProfileEntity(
                    id = "user_me",
                    handle = "my.ristam",
                    displayName = "حسابي",
                    bio = "مساحة Ristam الخاصة • يعمل دون اتصال مع تشفير محلي AES-256",
                    avatarDrawable = "img_ristam_icon_fg",
                    isVerified = true,
                    isCurrentUser = true,
                    followersCount = 0,
                    followingCount = 0
                )
            )

            val visualFolderId = dao.insertCollection(
                CollectionEntity(
                    name = "إلهام بصري",
                    description = "مختاراتي من الصور والمشاهد",
                    isPrivate = true,
                    createdAt = now - 4 * hour
                )
            )
            val videoFolderId = dao.insertCollection(
                CollectionEntity(
                    name = "مقاطع مسائية",
                    description = "مقاطع الفيديو المحفوظة يدوياً",
                    isPrivate = true,
                    createdAt = now - 2 * hour
                )
            )

            dao.insertPosts(
                listOf(
                    PostEntity(
                        authorId = "user_me",
                        authorHandle = "my.ristam",
                        authorDisplayName = "حسابي",
                        authorAvatar = "img_ristam_icon_fg",
                        authorVerified = true,
                        caption = "لقطة معمارية بالساعة الذهبية — انعكاس الضوء والظل بتباين داكن هادئ.",
                        tagsCsv = "عمارة,تصوير,ضوء",
                        location = "الاستوديو الشخصي",
                        mediaType = MediaType.PHOTO.name,
                        drawableName = "img_post_architecture",
                        aspectRatio = AspectRatioOption.PORTRAIT.name,
                        filterPreset = FilterPreset.WARM_DUSK.name,
                        isBookmarked = true,
                        isPinned = true,
                        collectionId = visualFolderId,
                        createdAt = now - 1 * hour
                    ),
                    PostEntity(
                        authorId = "user_me",
                        authorHandle = "my.ristam",
                        authorDisplayName = "حسابي",
                        authorAvatar = "img_ristam_icon_fg",
                        authorVerified = true,
                        caption = "مقطع فيديو مسائي هادئ لانعكاس الإضاءة بعد المطر — تحكم كامل بالسرعة وكتم الصوت.",
                        tagsCsv = "فيديو,مطر,ليلي",
                        location = "مقاطعي",
                        mediaType = MediaType.VIDEO.name,
                        drawableName = "img_post_city_rain",
                        aspectRatio = AspectRatioOption.TALL.name,
                        filterPreset = FilterPreset.NOIR_CINEMA.name,
                        trimStartMs = 0L,
                        trimEndMs = 15000L,
                        playbackSpeed = 1.0f,
                        isBookmarked = true,
                        collectionId = videoFolderId,
                        createdAt = now - 2 * hour
                    ),
                    PostEntity(
                        authorId = "user_me",
                        authorHandle = "my.ristam",
                        authorDisplayName = "حسابي",
                        authorAvatar = "img_ristam_icon_fg",
                        authorVerified = true,
                        caption = "سماء الصحراء ليلاً تحت النجوم — محفوظة في الأرشيف المحلي المشفر.",
                        tagsCsv = "صحراء,نجوم,ليلي",
                        location = "رحلة ليلية",
                        mediaType = MediaType.PHOTO.name,
                        drawableName = "img_post_desert_night",
                        aspectRatio = AspectRatioOption.TALL.name,
                        filterPreset = FilterPreset.ORIGINAL.name,
                        collectionId = visualFolderId,
                        createdAt = now - 3 * hour
                    ),
                    PostEntity(
                        authorId = "user_me",
                        authorHandle = "my.ristam",
                        authorDisplayName = "حسابي",
                        authorAvatar = "img_ristam_icon_fg",
                        authorVerified = true,
                        caption = "جلسة عمل مركزة مع قهوة المساء في الوضع الليلي الخالي من التشتيت.",
                        tagsCsv = "قهوة,استوديو,تركيز",
                        location = "مكتبي",
                        mediaType = MediaType.PHOTO.name,
                        drawableName = "img_post_espresso_studio",
                        aspectRatio = AspectRatioOption.SQUARE.name,
                        filterPreset = FilterPreset.ANALOG_FILM.name,
                        createdAt = now - 5 * hour
                    )
                )
            )

            dao.insertNotification(
                AppNotificationEntity(
                    title = "مرحباً بك في Ristam",
                    body = "تم تجهيز مساحتك الخاصة بتشفير محلي AES-256 دون إعلانات أو ذكاء اصطناعي أو حسابات وهمية.",
                    type = "BACKUP",
                    isRead = false
                )
            )
        }
    }
}
