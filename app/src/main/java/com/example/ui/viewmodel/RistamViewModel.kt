package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppNotificationEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CommentEntity
import com.example.data.local.PostEntity
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
import com.example.data.model.FeedSortMode
import com.example.data.model.RankedPost
import com.example.data.model.SearchFilterType
import com.example.data.model.ThemePreset
import com.example.data.repository.RistamRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RistamViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RistamRepository(application)

    val preferences: StateFlow<AppPreferencesState> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferencesState()
    )

    val currentUser: StateFlow<UserProfileEntity?> = repository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allProfiles: StateFlow<List<UserProfileEntity>> = repository.allProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPosts: StateFlow<List<PostEntity>> = repository.allPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stories: StateFlow<List<StoryEntity>> = repository.allStories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val collections: StateFlow<List<CollectionEntity>> = repository.allCollections.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatThreads: StateFlow<List<DecryptedThread>> = repository.chatThreads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notifications: StateFlow<List<AppNotificationEntity>> = repository.notifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val usageStats: StateFlow<List<UsageStatEntity>> = repository.usageStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val supportTickets: StateFlow<List<SupportTicketEntity>> = repository.supportTickets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _feedSortMode = MutableStateFlow(FeedSortMode.ALL)
    val feedSortMode: StateFlow<FeedSortMode> = _feedSortMode.asStateFlow()

    private val _selectedCollectionFilter = MutableStateFlow<Long?>(null)
    val selectedCollectionFilter: StateFlow<Long?> = _selectedCollectionFilter.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _productivityAlert = MutableStateFlow<String?>(null)
    val productivityAlert: StateFlow<String?> = _productivityAlert.asStateFlow()

    private val _sessionSeconds = MutableStateFlow(0)
    val sessionSeconds: StateFlow<Int> = _sessionSeconds.asStateFlow()

    private val _currentTabRoute = MutableStateFlow("feed")

    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _lastAlertMinuteTriggered = MutableStateFlow(0)

    val rankedFeedPosts: StateFlow<List<RankedPost>> = combine(
        allPosts,
        preferences,
        _feedSortMode,
        _selectedCollectionFilter
    ) { posts, prefs, sortMode, colFilter ->
        val base = if (colFilter != null) {
            posts.filter { it.collectionId == colFilter }
        } else {
            posts
        }
        val ranked = repository.rankPosts(base, prefs.algorithmWeights, prefs.blockedTags)
        when (sortMode) {
            FeedSortMode.ALL -> ranked.sortedByDescending { it.post.isPinned }
            FeedSortMode.CHRONOLOGICAL -> base
                .filter { p ->
                    val tags = com.example.data.algorithm.RecommendationEngine.parseTags(p.tagsCsv)
                    tags.none { t -> t in prefs.blockedTags.map { it.lowercase() } }
                }
                .sortedByDescending { it.createdAt }
                .map { RankedPost(it, 1.0, listOf("ترتيب زمني مباشر")) }
            FeedSortMode.ALGORITHM -> ranked
            FeedSortMode.FAVORITES -> ranked.filter { it.post.isBookmarked || it.post.isLiked }
            FeedSortMode.VIDEOS_ONLY -> ranked.filter { it.post.mediaType == "VIDEO" }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            val initialPrefs = repository.preferences.first()
            if (initialPrefs.masterPasswordEnabled && initialPrefs.lockOnLaunch) {
                _isLocked.value = true
            }
        }
        startProductivityTimer()
    }

    private fun startProductivityTimer() {
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                if (!_isLocked.value) {
                    _sessionSeconds.value += 15
                    repository.recordActiveSeconds(_currentTabRoute.value, 15)
                    val prefs = preferences.value
                    val limitSec = prefs.dailyUsageLimitMinutes * 60
                    val mins = _sessionSeconds.value / 60
                    if (prefs.productivityAlertsEnabled &&
                        _sessionSeconds.value >= limitSec &&
                        mins > _lastAlertMinuteTriggered.value
                    ) {
                        _lastAlertMinuteTriggered.value = mins
                        val msg = "تنبيه الإنتاجية: لقد أمضيت $mins دقيقة نشطة — خذ استراحة قصيرة للحفاظ على تركيزك."
                        _productivityAlert.value = msg
                        repository.emitInstantNotification(
                            title = "تنبيه الإنتاجية الذكي",
                            body = msg,
                            type = "PRODUCTIVITY"
                        )
                    }
                }
            }
        }
    }

    fun onActiveTabChanged(route: String) {
        _currentTabRoute.value = route
    }

    fun dismissProductivityAlert() {
        _productivityAlert.value = null
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatus(msg: String) {
        _statusBanner.value = msg
    }

    // Security & Master Password
    fun unlockWithMasterPassword(password: String) {
        viewModelScope.launch {
            val ok = repository.verifyAndUnlockMasterPassword(password)
            if (ok) {
                _isLocked.value = false
                _authError.value = null
            } else {
                _authError.value = "كلمة المرور الرئيسية غير صحيحة"
            }
        }
    }

    fun lockVaultNow() {
        if (preferences.value.masterPasswordEnabled) {
            _isLocked.value = true
        }
    }

    fun setupMasterPassword(password: String) {
        viewModelScope.launch {
            val ok = repository.setupMasterPassword(password)
            _statusBanner.value = if (ok) {
                "تم تفعيل كلمة المرور الرئيسية وتشفير الخزنة بنجاح"
            } else {
                "كلمة المرور يجب أن تكون 4 أحرف على الأقل"
            }
        }
    }

    fun disableMasterPassword() {
        viewModelScope.launch {
            repository.disableMasterPassword()
            _isLocked.value = false
            _statusBanner.value = "تم إلغاء القفل بكلمة المرور الرئيسية"
        }
    }

    // Feed & Posts
    fun setFeedSortMode(mode: FeedSortMode) {
        _feedSortMode.value = mode
    }

    fun setCollectionFilter(collectionId: Long?) {
        _selectedCollectionFilter.value = collectionId
    }

    fun toggleLike(post: PostEntity) {
        viewModelScope.launch { repository.toggleLike(post) }
    }

    fun toggleBookmark(post: PostEntity, collectionId: Long? = post.collectionId) {
        viewModelScope.launch { repository.toggleBookmark(post, collectionId) }
    }

    fun assignToCollection(post: PostEntity, collectionId: Long?) {
        viewModelScope.launch {
            repository.assignPostToCollection(post, collectionId)
            _statusBanner.value = "تم تحديث المجلد اليدوي للمنشور"
        }
    }

    fun togglePin(post: PostEntity) {
        viewModelScope.launch {
            repository.togglePin(post)
            _statusBanner.value = if (!post.isPinned) "تم تثبيت المنشور في الأعلى" else "تم إلغاء تثبيت المنشور"
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            repository.deletePost(postId)
            _statusBanner.value = "تم حذف المنشور محلياً"
        }
    }

    fun observeComments(postId: Long): Flow<List<CommentEntity>> = repository.observeComments(postId)

    fun addComment(post: PostEntity, text: String) {
        viewModelScope.launch { repository.addComment(post, text) }
    }

    fun createPost(post: PostEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createPost(post)
            _statusBanner.value = "تم نشر المحتوى وحفظه محلياً"
            onDone()
        }
    }

    // Stories
    fun createStory(mediaUri: String?, drawableName: String?, caption: String, filterPreset: String) {
        viewModelScope.launch {
            repository.createStory(mediaUri, drawableName, caption, filterPreset)
            _statusBanner.value = "تمت إضافة قصتك لمدة 24 ساعة"
        }
    }

    fun markStoryViewed(storyId: Long) {
        viewModelScope.launch { repository.markStoryViewed(storyId) }
    }

    // Collections
    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            repository.createCollection(name, description)
            _statusBanner.value = "تم إنشاء مجلد يدوي جديد: $name"
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            if (_selectedCollectionFilter.value == collectionId) {
                _selectedCollectionFilter.value = null
            }
            _statusBanner.value = "تم حذف المجلد"
        }
    }

    // Direct Messages
    fun observeMessages(threadId: String): Flow<List<DecryptedMessage>> = repository.observeMessages(threadId)

    fun sendMessage(threadId: String, text: String, attachmentUri: String? = null) {
        viewModelScope.launch { repository.sendEncryptedMessage(threadId, text, attachmentUri) }
    }

    fun markThreadRead(threadId: String) {
        viewModelScope.launch { repository.markThreadRead(threadId) }
    }

    fun createCustomThread(name: String, handle: String) {
        viewModelScope.launch {
            repository.createCustomThread(name, handle)
            _statusBanner.value = "تم إنشاء قناة رسائل مشفرة جديدة"
        }
    }

    fun replyToStoryAsDm(authorId: String, authorHandle: String, replyText: String) {
        viewModelScope.launch {
            val profiles = allProfiles.value
            val target = profiles.firstOrNull { it.id == authorId || it.handle == authorHandle }
            if (target != null) {
                val threadId = repository.startOrOpenThreadForProfile(target)
                repository.sendEncryptedMessage(threadId, "رد على القصة: $replyText")
                _statusBanner.value = "تم إرسال الرد المشفر إلى @$authorHandle"
            }
        }
    }

    // Search
    fun searchPosts(query: String, filter: SearchFilterType): List<PostEntity> {
        val prefs = preferences.value
        return repository.searchPosts(query, allPosts.value, filter, prefs.blockedTags)
    }

    fun recordSearchQuery(query: String) {
        viewModelScope.launch { repository.addRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { repository.clearRecentSearches() }
    }

    // Notifications
    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun triggerTestProductivityCheck() {
        viewModelScope.launch {
            val mins = (_sessionSeconds.value / 60).coerceAtLeast(1)
            repository.emitInstantNotification(
                title = "تقرير النشاط الفوري",
                body = "الوقت النشط في الجلسة الحالية: $mins دقيقة • وضع عدم الاتصال مؤمن بالكامل.",
                type = "PRODUCTIVITY"
            )
            _statusBanner.value = "تم إرسال إشعار فوري"
        }
    }

    // Profile & Settings
    fun updateProfile(displayName: String, handle: String, bio: String, isVerified: Boolean) {
        viewModelScope.launch {
            repository.updateProfile(displayName, handle, bio, isVerified)
            _statusBanner.value = "تم تحديث الملف الشخصي"
        }
    }

    fun setTheme(preset: ThemePreset) { viewModelScope.launch { repository.setTheme(preset) } }
    fun setAccent(preset: AccentPreset) { viewModelScope.launch { repository.setAccent(preset) } }
    fun setCornerDensity(density: CornerDensity) { viewModelScope.launch { repository.setCornerDensity(density) } }
    fun setDistractionFree(enabled: Boolean) { viewModelScope.launch { repository.setDistractionFree(enabled) } }
    fun setLockOnLaunch(enabled: Boolean) { viewModelScope.launch { repository.setLockOnLaunch(enabled) } }

    fun setOfflineAndSync(offline: Boolean, optionalSync: Boolean, note: String) {
        viewModelScope.launch {
            repository.setOfflineAndSync(offline, optionalSync, note)
            _statusBanner.value = if (offline) "الوضع الدائم دون اتصال مفعل" else "تم تحديث خيار المزامنة الاختيارية"
        }
    }

    fun setProductivitySettings(limitMinutes: Int, alertsEnabled: Boolean, sysNotifs: Boolean) {
        viewModelScope.launch {
            repository.setProductivity(limitMinutes, alertsEnabled, sysNotifs)
            _statusBanner.value = "تم حفظ إعدادات الإنتاجية والتنبيهات"
        }
    }

    fun setAlgorithmWeights(weights: AlgorithmWeights) {
        viewModelScope.launch { repository.setAlgorithmWeights(weights) }
    }

    fun addBlockedTag(tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val updated = (preferences.value.blockedTags + clean).distinct()
            repository.updateBlockedTags(updated)
        }
    }

    fun removeBlockedTag(tag: String) {
        viewModelScope.launch {
            val updated = preferences.value.blockedTags.filterNot { it == tag }
            repository.updateBlockedTags(updated)
        }
    }

    // Encrypted Local Backup & Readable Export
    fun createEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            val res = repository.createEncryptedBackup(uri, passphrase)
            _statusBanner.value = res.getOrElse { it.message ?: "فشل إنشاء النسخة الاحتياطية" }
        }
    }

    fun restoreEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            val res = repository.restoreEncryptedBackup(uri, passphrase)
            _statusBanner.value = res.getOrElse { it.message ?: "فشل استعادة النسخة الاحتياطية" }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportJson(uri)
            _statusBanner.value = res.getOrElse { it.message ?: "فشل تصدير JSON" }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportCsv(uri)
            _statusBanner.value = res.getOrElse { it.message ?: "فشل تصدير CSV" }
        }
    }

    // 24/7 Support & Diagnostics
    fun runDiagnostics() {
        viewModelScope.launch {
            _diagnosticReport.value = repository.runDiagnostics()
        }
    }

    fun submitSupportTicket(category: String, subject: String, details: String) {
        viewModelScope.launch {
            repository.submitSupportTicket(category, subject, details)
            _statusBanner.value = "تم تسجيل تذكرة الدعم التقني محلياً بنجاح"
        }
    }
}
