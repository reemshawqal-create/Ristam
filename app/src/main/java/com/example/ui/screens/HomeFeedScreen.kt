package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CollectionEntity
import com.example.data.local.PostEntity
import com.example.data.local.StoryEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.AppPreferencesState
import com.example.data.model.AspectRatioOption
import com.example.data.model.FeedSortMode
import com.example.data.model.RankedPost
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.components.RistamWordmark
import com.example.ui.components.StoryRingAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismViolet
import com.example.ui.viewmodel.RistamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    viewModel: RistamViewModel,
    currentUser: UserProfileEntity?,
    rankedPosts: List<RankedPost>,
    stories: List<StoryEntity>,
    collections: List<CollectionEntity>,
    prefs: AppPreferencesState,
    sortMode: FeedSortMode,
    selectedCollectionId: Long?,
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenStory: (StoryEntity) -> Unit
) {
    val extra = LocalPrismExtra.current
    var activeCommentsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeOptionsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeCollectionPost by remember { mutableStateOf<PostEntity?>(null) }
    var showCreateStorySheet by remember { mutableStateOf(false) }

    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createStory(
                mediaUri = uri.toString(),
                drawableName = null,
                caption = "قصة جديدة من لحظتي",
                filterPreset = "ORIGINAL"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Instagram-style Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RistamWordmark(fontSize = 26.sp)
                if (prefs.permanentOfflineMode) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "محمي محلياً",
                                tint = PrismViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "محلي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setDistractionFree(!prefs.distractionFreeMode) },
                    modifier = Modifier.testTag("toggle_distraction_free_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = "وضع التركيز الخالي من التشتيت",
                        tint = if (prefs.distractionFreeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenCreate,
                    modifier = Modifier.testTag("topbar_create_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddBox,
                        contentDescription = "إنشاء منشور"
                    )
                }
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("topbar_notifications_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(containerColor = PrismRose) {
                                    Text("$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (unreadNotificationsCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "الإشعارات الفورية"
                        )
                    }
                }
                IconButton(
                    onClick = onOpenMessages,
                    modifier = Modifier.testTag("topbar_dm_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadMessagesCount > 0) {
                                Badge(containerColor = PrismRose) {
                                    Text("$unreadMessagesCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "الرسائل المباشرة"
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = extra.divider.copy(alpha = 0.5f), thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Stories Tray (hidden in Distraction-Free mode)
            if (!prefs.distractionFreeMode) {
                item(key = "stories_tray") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "my_story_add") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clickable { showCreateStorySheet = true }
                                    .testTag("add_story_btn")
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    StoryRingAvatar(
                                        drawableName = currentUser?.avatarDrawable ?: "img_post_espresso_studio",
                                        imageUri = currentUser?.avatarUri,
                                        size = 68.dp,
                                        hasUnseenStory = false,
                                        showRing = false
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "أضف قصة",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "قصتك",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(stories, key = { it.id }) { story ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.markStoryViewed(story.id)
                                        onOpenStory(story)
                                    }
                                    .testTag("story_item_${story.id}")
                            ) {
                                StoryRingAvatar(
                                    drawableName = story.authorAvatar,
                                    size = 68.dp,
                                    hasUnseenStory = !story.isViewed
                                )
                                Text(
                                    text = story.authorHandle,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(68.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = extra.divider.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }

            // Manual Feed Organization & Sorting Bar
            item(key = "manual_sort_bar") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FeedSortMode.entries) { mode ->
                        FilterChip(
                            selected = sortMode == mode && selectedCollectionId == null,
                            onClick = {
                                viewModel.setCollectionFilter(null)
                                viewModel.setFeedSortMode(mode)
                            },
                            label = { Text(mode.labelAr, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    items(collections, key = { "col_${it.id}" }) { col ->
                        FilterChip(
                            selected = selectedCollectionId == col.id,
                            onClick = {
                                viewModel.setCollectionFilter(
                                    if (selectedCollectionId == col.id) null else col.id
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(col.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            if (rankedPosts.isEmpty()) {
                item(key = "empty_feed") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (selectedCollectionId != null || sortMode != FeedSortMode.ALL) {
                                "لا توجد منشورات مطابقة للتصفية الحالية"
                            } else {
                                "مساحتك في Ristam جاهزة ونظيفة"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لا توجد حسابات وهمية أو إعلانات. ابدأ بنشر أول صورة أو مقطع فيديو من جهازك وتنظيمه يدوياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.subtleText
                        )
                        Button(onClick = onOpenCreate) {
                            Text("إضافة أول صورة أو فيديو")
                        }
                        if (selectedCollectionId != null || sortMode != FeedSortMode.ALL) {
                            TextButton(onClick = {
                                viewModel.setCollectionFilter(null)
                                viewModel.setFeedSortMode(FeedSortMode.ALL)
                            }) {
                                Text("عرض كل المنشورات")
                            }
                        }
                    }
                }
            }

            items(rankedPosts, key = { it.post.id }) { ranked ->
                InstagramPostCard(
                    rankedPost = ranked,
                    distractionFree = prefs.distractionFreeMode,
                    showAlgorithmReason = sortMode == FeedSortMode.ALGORITHM && !prefs.distractionFreeMode,
                    onLike = { viewModel.toggleLike(ranked.post) },
                    onBookmark = { viewModel.toggleBookmark(ranked.post) },
                    onOpenComments = { activeCommentsPost = ranked.post },
                    onOpenCollectionPicker = { activeCollectionPost = ranked.post },
                    onOpenOptions = { activeOptionsPost = ranked.post }
                )
            }
        }
    }

    // Comments Sheet
    activeCommentsPost?.let { post ->
        val comments by viewModel.observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { activeCommentsPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "التعليقات (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = extra.divider)
                if (comments.isEmpty()) {
                    Text(
                        text = "لا توجد تعليقات بعد. كن أول من يضيف تعليقاً!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    comments.forEach { comment ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            StoryRingAvatar(
                                drawableName = comment.authorAvatar,
                                size = 34.dp,
                                showRing = false
                            )
                            Column {
                                Text(
                                    text = comment.authorHandle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("أضف تعليقاً...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(post, commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Text("نشر")
                    }
                }
            }
        }
    }

    // Post Options Sheet (Manual Organization)
    activeOptionsPost?.let { post ->
        ModalBottomSheet(onDismissRequest = { activeOptionsPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "أدوات التنظيم اليدوية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.togglePin(post)
                            activeOptionsPost = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PushPin, contentDescription = null)
                        Text(if (post.isPinned) "إلغاء التثبيت من الأعلى" else "تثبيت المنشور في الأعلى")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeOptionsPost = null
                            activeCollectionPost = post
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Text("نقل إلى مجلد يدوي")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.deletePost(post.id)
                            activeOptionsPost = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("حذف المنشور محلياً", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Save to Manual Collection Sheet
    activeCollectionPost?.let { post ->
        var newFolderName by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { activeCollectionPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حفظ وتنظيم في مجلد يدوي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                collections.forEach { col ->
                    val selected = post.collectionId == col.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.assignToCollection(post, col.id)
                                activeCollectionPost = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Filled.Folder, contentDescription = null)
                                Text(col.name, fontWeight = FontWeight.SemiBold)
                            }
                            if (selected) Text("محدد", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("اسم مجلد جديد...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createCollection(newFolderName, "مجلد شخصي")
                            newFolderName = ""
                        }
                    }) {
                        Text("إنشاء")
                    }
                }
            }
        }
    }

    // Create Story Sheet
    if (showCreateStorySheet) {
        var storyCaption by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showCreateStorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("إضافة قصة جديدة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = storyCaption,
                    onValueChange = { storyCaption = it },
                    label = { Text("نص القصة (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        showCreateStorySheet = false
                        storyPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اختيار صورة من الجهاز (Photo Picker)")
                }
                TextButton(
                    onClick = {
                        viewModel.createStory(
                            mediaUri = null,
                            drawableName = "img_post_espresso_studio",
                            caption = storyCaption.ifBlank { "لحظة تركيز من استوديو Ristam" },
                            filterPreset = "WARM_DUSK"
                        )
                        showCreateStorySheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("نشر قصة سريعة بصورة الاستوديو")
                }
            }
        }
    }
}

@Composable
fun InstagramPostCard(
    rankedPost: RankedPost,
    distractionFree: Boolean,
    showAlgorithmReason: Boolean,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenCollectionPicker: () -> Unit,
    onOpenOptions: () -> Unit
) {
    val post = rankedPost.post
    val extra = LocalPrismExtra.current
    val ratioValue = AspectRatioOption.entries.firstOrNull { it.name == post.aspectRatio }?.ratio ?: 0.85f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .testTag("post_card_${post.id}")
    ) {
        // Post Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryRingAvatar(
                    drawableName = post.authorAvatar,
                    size = 38.dp,
                    showRing = !distractionFree,
                    hasUnseenStory = true
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.authorHandle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (post.authorVerified) {
                            VerifiedBadge(size = 14.dp)
                        }
                        if (post.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "مثبت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (post.location.isNotBlank()) {
                        Text(
                            text = post.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.subtleText
                        )
                    }
                }
            }

            IconButton(
                onClick = onOpenOptions,
                modifier = Modifier.testTag("post_options_${post.id}")
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "خيارات المنشور")
            }
        }

        // Transparent non-AI algorithm reason badge
        AnimatedVisibility(visible = showAlgorithmReason && rankedPost.matchReasons.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "سبب الاقتراح اليدوي: ${rankedPost.matchReasons.joinToString(" • ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Full-bleed Media Canvas (Images & Videos)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratioValue.coerceIn(0.65f, 1.8f))
        ) {
            RistamMediaCanvas(
                mediaType = post.mediaType,
                mediaUri = post.mediaUri,
                drawableName = post.drawableName,
                filterPreset = post.filterPreset,
                brightness = post.brightness,
                contrast = post.contrast,
                saturation = post.saturation,
                warmth = post.warmth,
                vignette = post.vignette,
                rotationDegrees = post.rotationDegrees,
                trimStartMs = post.trimStartMs,
                trimEndMs = post.trimEndMs,
                playbackSpeed = post.playbackSpeed,
                isMuted = post.isMuted,
                autoPlayVideo = true,
                onDoubleTapLike = { if (!post.isLiked) onLike() },
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.62f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = if (post.mediaType == "VIDEO") "فيديو" else "صورة",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        // Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.testTag("like_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "إعجاب",
                        tint = if (post.isLiked) PrismRose else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("comment_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "تعليقات"
                    )
                }
                IconButton(onClick = onOpenCollectionPicker) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "تنظيم في مجلد"
                    )
                }
            }
            IconButton(
                onClick = onBookmark,
                modifier = Modifier.testTag("bookmark_btn_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "حفظ",
                    tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Likes + Caption + Tags
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!distractionFree) {
                Text(
                    text = "${post.likesCount} إعجاب",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${post.authorHandle}  ${post.caption}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (post.tagsCsv.isNotBlank()) {
                val tagsText = post.tagsCsv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("  ") { "#$it" }
                Text(
                    text = tagsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!distractionFree && post.commentsCount > 0) {
                Text(
                    text = "عرض جميع التعليقات (${post.commentsCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.subtleText,
                    modifier = Modifier.clickable(onClick = onOpenComments)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = extra.divider.copy(alpha = 0.35f), thickness = 0.5.dp)
    }
}
