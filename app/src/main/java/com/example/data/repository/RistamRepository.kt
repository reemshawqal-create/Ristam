package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.algorithm.RecommendationEngine
import com.example.data.backup.BackupExportManager
import com.example.data.local.AppNotificationEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatThreadEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CommentEntity
import com.example.data.local.PostEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.PrismDatabase
import com.example.data.local.StoryEntity
import com.example.data.local.SupportTicketEntity
import com.example.data.local.UsageStatEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.AccentPreset
import com.example.data.model.AlgorithmWeights
import com.example.data.model.AppPreferencesState
import com.example.data.model.CornerDensity
import com.example.data.model.DecryptedMessage
import com.example.data.model.DecryptedThread
import com.example.data.model.DiagnosticReport
import com.example.data.model.RankedPost
import com.example.data.model.SearchFilterType
import com.example.data.model.ThemePreset
import com.example.notifications.InstantNotificationHelper
import com.example.security.CipherVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RistamRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = PrismDatabase.getInstance(appContext)
    private val dao = db.dao()
    val prefs = PreferencesManager(appContext)
    private val backupManager = BackupExportManager(appContext, dao)
    val notificationHelper = InstantNotificationHelper(appContext)

    val currentUser: Flow<UserProfileEntity?> = dao.observeCurrentUser()
    val allProfiles: Flow<List<UserProfileEntity>> = dao.observeAllProfiles()
    val allPosts: Flow<List<PostEntity>> = dao.observeAllPosts()
    val allStories: Flow<List<StoryEntity>> = dao.observeAllStories()
    val allCollections: Flow<List<CollectionEntity>> = dao.observeAllCollections()
    val notifications: Flow<List<AppNotificationEntity>> = dao.observeNotifications()
    val usageStats: Flow<List<UsageStatEntity>> = dao.observeUsageStats()
    val supportTickets: Flow<List<SupportTicketEntity>> = dao.observeSupportTickets()
    val preferences: Flow<AppPreferencesState> = prefs.preferencesFlow

    val chatThreads: Flow<List<DecryptedThread>> = dao.observeChatThreads().map { list ->
        list.map { entity ->
            DecryptedThread(
                threadId = entity.threadId,
                participantHandle = entity.participantHandle,
                participantName = entity.participantName,
                participantAvatar = entity.participantAvatar,
                isVerified = entity.isVerified,
                isOnline = entity.isOnline,
                lastMessagePreview = CipherVault.decryptText(entity.lastMessageCipher),
                lastMessageTime = entity.lastMessageTime,
                unreadCount = entity.unreadCount,
                isEncrypted = entity.isEncrypted
            )
        }
    }

    fun observeMessages(threadId: String): Flow<List<DecryptedMessage>> =
        dao.observeMessages(threadId).map { msgs ->
            msgs.map { m ->
                DecryptedMessage(
                    id = m.id,
                    threadId = m.threadId,
                    senderHandle = m.senderHandle,
                    isFromMe = m.isFromMe,
                    body = CipherVault.decryptText(m.encryptedBody),
                    attachmentUri = m.attachmentUri,
                    createdAt = m.createdAt,
                    isRead = m.isRead,
                    isEncryptedAtRest = m.encryptedBody.startsWith("ENC_V1:")
                )
            }
        }

    fun observeComments(postId: Long): Flow<List<CommentEntity>> = dao.observeCommentsForPost(postId)

    suspend fun toggleLike(post: PostEntity) {
        val nextLiked = !post.isLiked
        val nextCount = (post.likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
        dao.updatePost(post.copy(isLiked = nextLiked, likesCount = nextCount))
    }

    suspend fun toggleBookmark(post: PostEntity, collectionId: Long? = post.collectionId) {
        val nextSaved = if (collectionId != post.collectionId) true else !post.isBookmarked
        dao.updatePost(
            post.copy(
                isBookmarked = nextSaved,
                collectionId = if (nextSaved) collectionId else null
            )
        )
    }

    suspend fun assignPostToCollection(post: PostEntity, collectionId: Long?) {
        dao.updatePost(
            post.copy(
                isBookmarked = collectionId != null || post.isBookmarked,
                collectionId = collectionId
            )
        )
    }

    suspend fun togglePin(post: PostEntity) {
        dao.updatePost(post.copy(isPinned = !post.isPinned))
    }

    suspend fun deletePost(postId: Long) {
        dao.deletePost(postId)
    }

    suspend fun addComment(post: PostEntity, text: String) {
        val me = dao.getCurrentUserOnce() ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        dao.insertComment(
            CommentEntity(
                postId = post.id,
                authorHandle = me.handle,
                authorAvatar = me.avatarDrawable,
                text = clean
            )
        )
        dao.updatePost(post.copy(commentsCount = post.commentsCount + 1))
    }

    suspend fun createPost(post: PostEntity): Long {
        val id = dao.insertPost(post)
        incrementUsageCounter(postsCreatedDelta = 1)
        emitInstantNotification(
            title = "تم نشر المحتوى بنجاح",
            body = "تم حفظ منشورك في المخزن المحلي المشفر بنجاح.",
            type = "BACKUP",
            targetPostId = id
        )
        return id
    }

    suspend fun createStory(mediaUri: String?, drawableName: String?, caption: String, filterPreset: String) {
        val me = dao.getCurrentUserOnce() ?: return
        dao.insertStory(
            StoryEntity(
                authorId = me.id,
                authorHandle = me.handle,
                authorAvatar = me.avatarDrawable,
                mediaUri = mediaUri,
                drawableName = drawableName,
                caption = caption,
                filterPreset = filterPreset
            )
        )
    }

    suspend fun markStoryViewed(storyId: Long) = dao.markStoryViewed(storyId)

    suspend fun createCollection(name: String, description: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        dao.insertCollection(CollectionEntity(name = clean, description = description.trim()))
    }

    suspend fun deleteCollection(collectionId: Long) {
        dao.clearCollectionFromPosts(collectionId)
        dao.deleteCollection(collectionId)
    }

    suspend fun sendEncryptedMessage(threadId: String, text: String, attachmentUri: String? = null) {
        val me = dao.getCurrentUserOnce() ?: return
        val clean = text.trim()
        if (clean.isEmpty() && attachmentUri == null) return
        val displayBody = if (clean.isEmpty()) "📎 مرفق وسائط مشفر" else clean
        val encrypted = CipherVault.encryptText(displayBody)
        val now = System.currentTimeMillis()
        dao.insertMessage(
            ChatMessageEntity(
                threadId = threadId,
                senderHandle = me.handle,
                isFromMe = true,
                encryptedBody = encrypted,
                attachmentUri = attachmentUri,
                createdAt = now
            )
        )
        val threads = dao.getAllThreadsOnce()
        val thread = threads.firstOrNull { it.threadId == threadId }
        if (thread != null) {
            dao.upsertThread(
                thread.copy(
                    lastMessageCipher = encrypted,
                    lastMessageTime = now,
                    unreadCount = 0
                )
            )
        }
    }

    suspend fun startOrOpenThreadForProfile(profile: UserProfileEntity): String {
        val threadId = "thread_${profile.id}"
        val existing = dao.getAllThreadsOnce().firstOrNull { it.threadId == threadId }
        if (existing == null) {
            dao.upsertThread(
                ChatThreadEntity(
                    threadId = threadId,
                    participantHandle = profile.handle,
                    participantName = profile.displayName,
                    participantAvatar = profile.avatarDrawable,
                    isVerified = profile.isVerified,
                    isOnline = true,
                    lastMessageCipher = CipherVault.encryptText("بدء محادثة مشفرة محلياً"),
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0,
                    isEncrypted = true
                )
            )
        }
        return threadId
    }

    suspend fun createCustomThread(name: String, handle: String): String {
        val cleanName = name.trim().ifEmpty { "ملاحظاتي المشفرة" }
        val cleanHandle = handle.trim().removePrefix("@").ifEmpty { "private.vault" }
        val threadId = "thread_${System.currentTimeMillis()}"
        dao.upsertThread(
            ChatThreadEntity(
                threadId = threadId,
                participantHandle = cleanHandle,
                participantName = cleanName,
                participantAvatar = "img_ristam_icon_fg",
                isVerified = false,
                isOnline = false,
                lastMessageCipher = CipherVault.encryptText("قناة محلية مشفرة AES-256"),
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = 0,
                isEncrypted = true
            )
        )
        return threadId
    }

    suspend fun markThreadRead(threadId: String) = dao.markThreadRead(threadId)

    suspend fun updateProfile(displayName: String, handle: String, bio: String, isVerified: Boolean) {
        val current = dao.getCurrentUserOnce() ?: return
        dao.upsertProfile(
            current.copy(
                displayName = displayName.trim().ifEmpty { current.displayName },
                handle = handle.trim().ifEmpty { current.handle },
                bio = bio.trim(),
                isVerified = isVerified
            )
        )
    }

    suspend fun emitInstantNotification(
        title: String,
        body: String,
        type: String,
        targetPostId: Long? = null,
        targetThreadId: String? = null
    ) {
        val id = dao.insertNotification(
            AppNotificationEntity(
                title = title,
                body = body,
                type = type,
                targetPostId = targetPostId,
                targetThreadId = targetThreadId
            )
        )
        val currentPrefs = prefs.preferencesFlow.first()
        if (currentPrefs.systemNotificationsEnabled) {
            notificationHelper.postSystemNotification(title, body, id.toInt())
        }
    }

    suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead()

    // Master password security
    suspend fun setupMasterPassword(password: String): Boolean {
        if (password.length < 4) return false
        val salt = CipherVault.generateSaltBase64()
        val verifier = CipherVault.hashPasswordVerifier(password, salt)
        prefs.setMasterPassword(salt, verifier, lockOnLaunch = true)
        CipherVault.unlockWithMasterPassword(password, salt)
        return true
    }

    suspend fun verifyAndUnlockMasterPassword(password: String): Boolean {
        val (salt, verifier) = prefs.getMasterPasswordCredentials()
        val ok = CipherVault.verifyPassword(password, salt, verifier)
        if (ok) {
            CipherVault.unlockWithMasterPassword(password, salt)
        }
        return ok
    }

    suspend fun disableMasterPassword() {
        CipherVault.clearSessionKey()
        prefs.disableMasterPassword()
    }

    // Usage tracking & reports
    suspend fun recordActiveSeconds(tabRoute: String, seconds: Int) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val current = dao.getUsageStatForDate(key) ?: UsageStatEntity(dateKey = key)
        val updated = when (tabRoute) {
            "feed" -> current.copy(feedSeconds = current.feedSeconds + seconds)
            "explore" -> current.copy(exploreSeconds = current.exploreSeconds + seconds)
            "reels" -> current.copy(reelsSeconds = current.reelsSeconds + seconds)
            "messages" -> current.copy(messagesSeconds = current.messagesSeconds + seconds)
            "create" -> current.copy(studioSeconds = current.studioSeconds + seconds)
            else -> current.copy(feedSeconds = current.feedSeconds + seconds)
        }
        dao.upsertUsageStat(updated)
    }

    private suspend fun incrementUsageCounter(postsCreatedDelta: Int = 0, backupsDelta: Int = 0) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val current = dao.getUsageStatForDate(key) ?: UsageStatEntity(dateKey = key)
        dao.upsertUsageStat(
            current.copy(
                postsCreated = current.postsCreated + postsCreatedDelta,
                backupsCreated = current.backupsCreated + backupsDelta
            )
        )
    }

    // Backup & Export
    suspend fun createEncryptedBackup(uri: Uri, passphrase: String): Result<String> {
        val res = backupManager.createEncryptedLocalBackup(uri, passphrase)
        if (res.isSuccess) {
            incrementUsageCounter(backupsDelta = 1)
            emitInstantNotification(
                title = "اكتمل النسخ الاحتياطي المشفر",
                body = "تم حفظ نسخة .ristambak محلية مشفرة بنجاح.",
                type = "BACKUP"
            )
        }
        return res
    }

    suspend fun restoreEncryptedBackup(uri: Uri, passphrase: String): Result<String> {
        return backupManager.restoreEncryptedLocalBackup(uri, passphrase)
    }

    suspend fun exportJson(uri: Uri): Result<String> = backupManager.exportReadableJson(uri)
    suspend fun exportCsv(uri: Uri): Result<String> = backupManager.exportReadableCsv(uri)

    // 24/7 Tech Support & Diagnostics
    suspend fun submitSupportTicket(category: String, subject: String, details: String) {
        dao.insertSupportTicket(
            SupportTicketEntity(
                category = category,
                subject = subject.trim(),
                details = details.trim()
            )
        )
    }

    suspend fun runDiagnostics(): DiagnosticReport {
        val posts = dao.getAllPostsOnce()
        val msgCount = dao.getMessageCount()
        val dbFile = appContext.getDatabasePath("ristam_encrypted_vault.db")
        val sizeKb = if (dbFile.exists()) (dbFile.length() / 1024L).coerceAtLeast(1L) else 128L
        return DiagnosticReport(
            aesCipherSelfTestPassed = CipherVault.runSelfTest(),
            databaseAccessible = posts.isNotEmpty() || dbFile.exists(),
            totalEncryptedMessages = msgCount,
            totalPostsCached = posts.size,
            estimatedStorageKb = sizeKb
        )
    }

    // Pure algorithmic ranking & search helpers
    fun rankPosts(posts: List<PostEntity>, weights: AlgorithmWeights, blocked: List<String>): List<RankedPost> =
        RecommendationEngine.rankPosts(posts, weights, blocked)

    fun searchPosts(query: String, posts: List<PostEntity>, filter: SearchFilterType, blocked: List<String>): List<PostEntity> =
        RecommendationEngine.smartSearch(query, posts, filter, blocked)

    // Preferences pass-throughs
    suspend fun setTheme(preset: ThemePreset) = prefs.setTheme(preset)
    suspend fun setAccent(preset: AccentPreset) = prefs.setAccent(preset)
    suspend fun setCornerDensity(density: CornerDensity) = prefs.setCornerDensity(density)
    suspend fun setDistractionFree(enabled: Boolean) = prefs.setDistractionFree(enabled)
    suspend fun setOfflineAndSync(offline: Boolean, sync: Boolean, note: String) = prefs.setOfflineAndSync(offline, sync, note)
    suspend fun setProductivity(limitMin: Int, alerts: Boolean, sysNotifs: Boolean) = prefs.setProductivitySettings(limitMin, alerts, sysNotifs)
    suspend fun setAlgorithmWeights(weights: AlgorithmWeights) = prefs.setAlgorithmWeights(weights)
    suspend fun updateBlockedTags(tags: List<String>) = prefs.updateBlockedTags(tags)
    suspend fun addRecentSearch(q: String) = prefs.addRecentSearch(q)
    suspend fun clearRecentSearches() = prefs.clearRecentSearches()
    suspend fun setLockOnLaunch(enabled: Boolean) = prefs.setLockOnLaunch(enabled)
}
