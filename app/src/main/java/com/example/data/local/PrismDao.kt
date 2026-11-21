package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrismDao {
    // Profiles
    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserOnce(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY isCurrentUser DESC, followersCount DESC")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    // Posts
    @Query("SELECT * FROM posts ORDER BY isPinned DESC, createdAt DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPostsOnce(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: Long): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: Long)

    // Comments
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeCommentsForPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments")
    suspend fun getAllCommentsOnce(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    // Stories
    @Query("SELECT * FROM stories ORDER BY isViewed ASC, createdAt DESC")
    fun observeAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Query("UPDATE stories SET isViewed = 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: Long)

    // Manual Collections / Folders
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    suspend fun getAllCollectionsOnce(): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: Long)

    @Query("UPDATE posts SET collectionId = NULL WHERE collectionId = :collectionId")
    suspend fun clearCollectionFromPosts(collectionId: Long)

    // Encrypted Direct Messages
    @Query("SELECT * FROM chat_threads ORDER BY lastMessageTime DESC")
    fun observeChatThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads")
    suspend fun getAllThreadsOnce(): List<ChatThreadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: ChatThreadEntity)

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun getAllMessagesOnce(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_threads SET unreadCount = 0 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: String)

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int

    // Instant Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeNotifications(): Flow<List<AppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    // Usage Stats & Productivity Reports
    @Query("SELECT * FROM usage_stats ORDER BY dateKey DESC LIMIT 14")
    fun observeUsageStats(): Flow<List<UsageStatEntity>>

    @Query("SELECT * FROM usage_stats WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getUsageStatForDate(dateKey: String): UsageStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsageStat(stat: UsageStatEntity)

    // 24/7 Support Tickets
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun observeSupportTickets(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupportTicket(ticket: SupportTicketEntity): Long

    // Clear / Restore helpers
    @Query("DELETE FROM posts")
    suspend fun clearPosts()

    @Query("DELETE FROM collections")
    suspend fun clearCollections()

    @Query("DELETE FROM chat_threads")
    suspend fun clearThreads()

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}
