package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatThreadEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.PostEntity
import com.example.data.local.PrismDao
import com.example.data.local.UserProfileEntity
import com.example.security.CipherVault
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class BackupExportManager(
    private val context: Context,
    private val dao: PrismDao
) {

    /**
     * Writes an AES-256-GCM encrypted `.ristambak` local backup file via Storage Access Framework.
     */
    suspend fun createEncryptedLocalBackup(targetUri: Uri, passphrase: String): Result<String> {
        return try {
            if (passphrase.length < 4) {
                return Result.failure(IllegalArgumentException("يرجى إدخال كلمة مرور لا تقل عن 4 أحرف لحماية النسخة الاحتياطية"))
            }
            val json = buildFullExportJson(includeDecryptedMessages = true)
            val encryptedBlob = CipherVault.encryptBackupPayload(json.toString(), passphrase)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(encryptedBlob.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return Result.failure(IllegalStateException("تعذر فتح مسار الحفظ المختار"))
            Result.success("تم إنشاء نسخة احتياطية محلية مشفرة AES-256 بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts and restores a `.ristambak` encrypted local backup file.
     */
    suspend fun restoreEncryptedLocalBackup(sourceUri: Uri, passphrase: String): Result<String> {
        return try {
            val rawBlob = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            } ?: return Result.failure(IllegalStateException("تعذر قراءة الملف المختار"))

            val decrypted = CipherVault.decryptBackupPayload(rawBlob, passphrase)
            val plainJson = decrypted.getOrElse { return Result.failure(it) }
            val root = JSONObject(plainJson)

            // Restore Collections
            val collectionsArr = root.optJSONArray("collections") ?: JSONArray()
            dao.clearCollections()
            val colIdMap = mutableMapOf<Long, Long>()
            for (i in 0 until collectionsArr.length()) {
                val obj = collectionsArr.getJSONObject(i)
                val oldId = obj.optLong("id")
                val newId = dao.insertCollection(
                    CollectionEntity(
                        name = obj.optString("name", "مجلد مستعاد"),
                        description = obj.optString("description", ""),
                        isPrivate = obj.optBoolean("isPrivate", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                colIdMap[oldId] = newId
            }

            // Restore Posts
            val postsArr = root.optJSONArray("posts") ?: JSONArray()
            dao.clearPosts()
            for (i in 0 until postsArr.length()) {
                val obj = postsArr.getJSONObject(i)
                val oldCol = if (obj.isNull("collectionId")) null else obj.optLong("collectionId")
                dao.insertPost(
                    PostEntity(
                        authorId = obj.optString("authorId", "user_me"),
                        authorHandle = obj.optString("authorHandle", "ristam.studio"),
                        authorDisplayName = obj.optString("authorDisplayName", "حسابي"),
                        authorAvatar = obj.optString("authorAvatar", "img_post_espresso_studio"),
                        authorVerified = obj.optBoolean("authorVerified", false),
                        caption = obj.optString("caption", ""),
                        tagsCsv = obj.optString("tagsCsv", ""),
                        location = obj.optString("location", ""),
                        mediaType = obj.optString("mediaType", "PHOTO"),
                        mediaUri = obj.optString("mediaUri").takeIf { it.isNotEmpty() && it != "null" },
                        drawableName = obj.optString("drawableName").takeIf { it.isNotEmpty() && it != "null" },
                        aspectRatio = obj.optString("aspectRatio", "PORTRAIT"),
                        filterPreset = obj.optString("filterPreset", "ORIGINAL"),
                        brightness = obj.optDouble("brightness", 0.0).toFloat(),
                        contrast = obj.optDouble("contrast", 1.0).toFloat(),
                        saturation = obj.optDouble("saturation", 1.0).toFloat(),
                        warmth = obj.optDouble("warmth", 0.0).toFloat(),
                        vignette = obj.optDouble("vignette", 0.0).toFloat(),
                        rotationDegrees = obj.optDouble("rotationDegrees", 0.0).toFloat(),
                        trimStartMs = obj.optLong("trimStartMs", 0L),
                        trimEndMs = obj.optLong("trimEndMs", 15000L),
                        playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat(),
                        isMuted = obj.optBoolean("isMuted", false),
                        likesCount = obj.optInt("likesCount", 0),
                        commentsCount = obj.optInt("commentsCount", 0),
                        isLiked = obj.optBoolean("isLiked", false),
                        isBookmarked = obj.optBoolean("isBookmarked", false),
                        isPinned = obj.optBoolean("isPinned", false),
                        collectionId = oldCol?.let { colIdMap[it] },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore Threads & re-encrypt messages locally
            val threadsArr = root.optJSONArray("threads") ?: JSONArray()
            dao.clearThreads()
            for (i in 0 until threadsArr.length()) {
                val obj = threadsArr.getJSONObject(i)
                dao.upsertThread(
                    ChatThreadEntity(
                        threadId = obj.getString("threadId"),
                        participantHandle = obj.getString("participantHandle"),
                        participantName = obj.getString("participantName"),
                        participantAvatar = obj.getString("participantAvatar"),
                        isVerified = obj.optBoolean("isVerified", false),
                        isOnline = obj.optBoolean("isOnline", false),
                        lastMessageCipher = CipherVault.encryptText(obj.optString("lastMessagePlain", "")),
                        lastMessageTime = obj.optLong("lastMessageTime", System.currentTimeMillis()),
                        unreadCount = obj.optInt("unreadCount", 0),
                        isEncrypted = true
                    )
                )
            }

            val messagesArr = root.optJSONArray("messages") ?: JSONArray()
            dao.clearMessages()
            for (i in 0 until messagesArr.length()) {
                val obj = messagesArr.getJSONObject(i)
                dao.insertMessage(
                    ChatMessageEntity(
                        threadId = obj.getString("threadId"),
                        senderHandle = obj.getString("senderHandle"),
                        isFromMe = obj.optBoolean("isFromMe", true),
                        encryptedBody = CipherVault.encryptText(obj.optString("bodyPlain", "")),
                        attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotEmpty() && it != "null" },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", true)
                    )
                )
            }

            // Restore Profile
            root.optJSONObject("profile")?.let { prof ->
                dao.upsertProfile(
                    UserProfileEntity(
                        id = prof.optString("id", "user_me"),
                        handle = prof.optString("handle", "ristam.studio"),
                        displayName = prof.optString("displayName", "حسابي"),
                        bio = prof.optString("bio", ""),
                        avatarDrawable = prof.optString("avatarDrawable", "img_post_espresso_studio"),
                        avatarUri = prof.optString("avatarUri").takeIf { it.isNotEmpty() && it != "null" },
                        isVerified = prof.optBoolean("isVerified", true),
                        isCurrentUser = true,
                        followersCount = prof.optInt("followersCount", 0),
                        followingCount = prof.optInt("followingCount", 0)
                    )
                )
            }

            Result.success("تمت استعادة البيانات المشفرة بنجاح (${postsArr.length()} منشورات)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports readable JSON for external apps (note: readable plaintext format).
     */
    suspend fun exportReadableJson(targetUri: Uri): Result<String> {
        return try {
            val json = buildFullExportJson(includeDecryptedMessages = true).toString(2)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(json.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return Result.failure(IllegalStateException("تعذر الكتابة إلى الملف"))
            Result.success("تم تصدير البيانات بصيغة JSON قابلة للقراءة")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports readable CSV (UTF-8 with BOM for Excel/external spreadsheet compatibility).
     */
    suspend fun exportReadableCsv(targetUri: Uri): Result<String> {
        return try {
            val posts = dao.getAllPostsOnce()
            val sb = StringBuilder()
            // UTF-8 BOM for proper Arabic rendering in Excel/Sheets
            sb.append("\uFEFF")
            sb.append("ID,AuthorHandle,MediaType,Caption,Tags,Location,Filter,Likes,Bookmarked,Pinned,CreatedAt\n")
            for (p in posts) {
                sb.append(
                    listOf(
                        p.id.toString(),
                        csvEscape(p.authorHandle),
                        csvEscape(p.mediaType),
                        csvEscape(p.caption),
                        csvEscape(p.tagsCsv),
                        csvEscape(p.location),
                        csvEscape(p.filterPreset),
                        p.likesCount.toString(),
                        p.isBookmarked.toString(),
                        p.isPinned.toString(),
                        p.createdAt.toString()
                    ).joinToString(",")
                ).append("\n")
            }
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: return Result.failure(IllegalStateException("تعذر الكتابة إلى ملف CSV"))
            Result.success("تم تصدير ${posts.size} منشوراً بصيغة CSV قابلة للقراءة")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$clean\""
    }

    private suspend fun buildFullExportJson(includeDecryptedMessages: Boolean): JSONObject {
        val root = JSONObject()
        root.put("app", "Ristam")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        dao.getCurrentUserOnce()?.let { user ->
            root.put(
                "profile",
                JSONObject().apply {
                    put("id", user.id)
                    put("handle", user.handle)
                    put("displayName", user.displayName)
                    put("bio", user.bio)
                    put("avatarDrawable", user.avatarDrawable)
                    put("avatarUri", user.avatarUri)
                    put("isVerified", user.isVerified)
                    put("followersCount", user.followersCount)
                    put("followingCount", user.followingCount)
                }
            )
        }

        val cols = JSONArray()
        dao.getAllCollectionsOnce().forEach { c ->
            cols.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("description", c.description)
                    put("isPrivate", c.isPrivate)
                    put("createdAt", c.createdAt)
                }
            )
        }
        root.put("collections", cols)

        val postsArr = JSONArray()
        dao.getAllPostsOnce().forEach { p ->
            postsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("authorId", p.authorId)
                    put("authorHandle", p.authorHandle)
                    put("authorDisplayName", p.authorDisplayName)
                    put("authorAvatar", p.authorAvatar)
                    put("authorVerified", p.authorVerified)
                    put("caption", p.caption)
                    put("tagsCsv", p.tagsCsv)
                    put("location", p.location)
                    put("mediaType", p.mediaType)
                    put("mediaUri", p.mediaUri)
                    put("drawableName", p.drawableName)
                    put("aspectRatio", p.aspectRatio)
                    put("filterPreset", p.filterPreset)
                    put("brightness", p.brightness.toDouble())
                    put("contrast", p.contrast.toDouble())
                    put("saturation", p.saturation.toDouble())
                    put("warmth", p.warmth.toDouble())
                    put("vignette", p.vignette.toDouble())
                    put("rotationDegrees", p.rotationDegrees.toDouble())
                    put("trimStartMs", p.trimStartMs)
                    put("trimEndMs", p.trimEndMs)
                    put("playbackSpeed", p.playbackSpeed.toDouble())
                    put("isMuted", p.isMuted)
                    put("likesCount", p.likesCount)
                    put("commentsCount", p.commentsCount)
                    put("isLiked", p.isLiked)
                    put("isBookmarked", p.isBookmarked)
                    put("isPinned", p.isPinned)
                    put("collectionId", p.collectionId)
                    put("createdAt", p.createdAt)
                }
            )
        }
        root.put("posts", postsArr)

        if (includeDecryptedMessages) {
            val threadsArr = JSONArray()
            dao.getAllThreadsOnce().forEach { t ->
                threadsArr.put(
                    JSONObject().apply {
                        put("threadId", t.threadId)
                        put("participantHandle", t.participantHandle)
                        put("participantName", t.participantName)
                        put("participantAvatar", t.participantAvatar)
                        put("isVerified", t.isVerified)
                        put("isOnline", t.isOnline)
                        put("lastMessagePlain", CipherVault.decryptText(t.lastMessageCipher))
                        put("lastMessageTime", t.lastMessageTime)
                        put("unreadCount", t.unreadCount)
                    }
                )
            }
            root.put("threads", threadsArr)

            val msgsArr = JSONArray()
            dao.getAllMessagesOnce().forEach { m ->
                msgsArr.put(
                    JSONObject().apply {
                        put("threadId", m.threadId)
                        put("senderHandle", m.senderHandle)
                        put("isFromMe", m.isFromMe)
                        put("bodyPlain", CipherVault.decryptText(m.encryptedBody))
                        put("attachmentUri", m.attachmentUri)
                        put("createdAt", m.createdAt)
                        put("isRead", m.isRead)
                    }
                )
            }
            root.put("messages", msgsArr)
        }

        return root
    }
}
