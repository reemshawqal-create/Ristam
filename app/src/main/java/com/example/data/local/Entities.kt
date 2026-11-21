package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AspectRatioOption
import com.example.data.model.FilterPreset
import com.example.data.model.MediaType

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val handle: String,
    val displayName: String,
    val bio: String,
    val avatarDrawable: String,
    val avatarUri: String? = null,
    val isVerified: Boolean = false,
    val isCurrentUser: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: String,
    val authorHandle: String,
    val authorDisplayName: String,
    val authorAvatar: String,
    val authorVerified: Boolean = false,
    val caption: String,
    val tagsCsv: String,
    val location: String = "",
    val mediaType: String = MediaType.PHOTO.name,
    val mediaUri: String? = null,
    val drawableName: String? = null,
    val aspectRatio: String = AspectRatioOption.PORTRAIT.name,
    val filterPreset: String = FilterPreset.ORIGINAL.name,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val rotationDegrees: Float = 0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 15000L,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isPinned: Boolean = false,
    val collectionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorHandle: String,
    val authorAvatar: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: String,
    val authorHandle: String,
    val authorAvatar: String,
    val mediaUri: String? = null,
    val drawableName: String? = null,
    val caption: String = "",
    val filterPreset: String = FilterPreset.ORIGINAL.name,
    val isViewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isPrivate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val threadId: String,
    val participantHandle: String,
    val participantName: String,
    val participantAvatar: String,
    val isVerified: Boolean = false,
    val isOnline: Boolean = true,
    val lastMessageCipher: String,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isEncrypted: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: String,
    val senderHandle: String,
    val isFromMe: Boolean,
    val encryptedBody: String,
    val attachmentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

@Entity(tableName = "notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val type: String, // LIKE, COMMENT, MESSAGE, PRODUCTIVITY, BACKUP
    val targetPostId: Long? = null,
    val targetThreadId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage_stats")
data class UsageStatEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val feedSeconds: Int = 0,
    val exploreSeconds: Int = 0,
    val reelsSeconds: Int = 0,
    val messagesSeconds: Int = 0,
    val studioSeconds: Int = 0,
    val postsCreated: Int = 0,
    val backupsCreated: Int = 0
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val subject: String,
    val details: String,
    val status: String = "تم الحفظ محلياً — جاهز للتصدير",
    val createdAt: Long = System.currentTimeMillis()
)
