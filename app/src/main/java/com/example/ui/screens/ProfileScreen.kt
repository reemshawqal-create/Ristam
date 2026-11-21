package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CollectionEntity
import com.example.data.local.PostEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.AppPreferencesState
import com.example.data.model.MediaType
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.components.StoryRingAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.viewmodel.RistamViewModel

@Composable
fun ProfileScreen(
    viewModel: RistamViewModel,
    currentUser: UserProfileEntity?,
    allPosts: List<PostEntity>,
    collections: List<CollectionEntity>,
    prefs: AppPreferencesState,
    onOpenSettings: () -> Unit,
    onOpenReports: () -> Unit
) {
    val extra = LocalPrismExtra.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var activeFolderFilterId by remember { mutableStateOf<Long?>(null) }

    val myPosts = remember(allPosts, currentUser) {
        allPosts.filter { it.authorId == (currentUser?.id ?: "user_me") || it.isPinned }
    }
    val myVideos = remember(allPosts) {
        allPosts.filter { it.mediaType == MediaType.VIDEO.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "ملف مشفر", modifier = Modifier.size(16.dp))
                Text(
                    text = currentUser?.handle ?: "my.ristam",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (currentUser?.isVerified == true) VerifiedBadge(size = 18.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenReports,
                    modifier = Modifier.testTag("profile_reports_btn")
                ) {
                    Icon(Icons.Filled.Analytics, contentDescription = "تقارير البيانات والإنتاجية")
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("profile_settings_btn")
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "إعدادات Ristam")
                }
            }
        }

        // Avatar + Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StoryRingAvatar(
                drawableName = currentUser?.avatarDrawable ?: "img_ristam_icon_fg",
                imageUri = currentUser?.avatarUri,
                size = 82.dp,
                showRing = !prefs.distractionFreeMode
            )

            if (!prefs.distractionFreeMode) {
                ProfileStatColumn("${allPosts.size}", "المنشورات")
                ProfileStatColumn("${currentUser?.followersCount ?: 0}", "المتابعون")
                ProfileStatColumn("${currentUser?.followingCount ?: 0}", "أتابع")
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "وضع التركيز مفعل — الإحصائيات مخفية لتقليل التشتيت",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Name & Bio
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = currentUser?.displayName ?: "حسابي الشخصي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentUser?.bio ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = extra.subtleText
            )
        }

        // Profile Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showEditProfileDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_profile_btn")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  تعديل الملف الشخصي", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_settings_btn")
            ) {
                Text("الإعدادات والخصوصية", style = MaterialTheme.typography.labelMedium)
            }
        }

        // 3 Profile Tabs (Posts, Videos, Manual Collections)
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Filled.GridOn, contentDescription = "المنشورات") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "المقاطع") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Filled.Folder, contentDescription = "المجلدات اليدوية") }
            )
        }

        when (selectedTab) {
            0 -> {
                val gridPosts = if (myPosts.isNotEmpty()) myPosts else allPosts
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 115.dp),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(gridPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.aspectRatio(1f)) {
                            RistamMediaCanvas(
                                mediaType = MediaType.PHOTO.name,
                                mediaUri = post.mediaUri,
                                drawableName = post.drawableName,
                                filterPreset = post.filterPreset,
                                brightness = post.brightness,
                                contrast = post.contrast,
                                saturation = post.saturation,
                                warmth = post.warmth,
                                vignette = post.vignette,
                                rotationDegrees = post.rotationDegrees,
                                autoPlayVideo = false,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (post.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "منشور مثبت",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 115.dp),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(myVideos, key = { it.id }) { post ->
                        Box(modifier = Modifier.aspectRatio(0.75f)) {
                            RistamMediaCanvas(
                                mediaType = MediaType.PHOTO.name,
                                mediaUri = post.mediaUri,
                                drawableName = post.drawableName,
                                filterPreset = post.filterPreset,
                                brightness = post.brightness,
                                contrast = post.contrast,
                                saturation = post.saturation,
                                warmth = post.warmth,
                                vignette = post.vignette,
                                rotationDegrees = post.rotationDegrees,
                                autoPlayVideo = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Button(
                            onClick = { showNewFolderDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("  إنشاء مجلد تنظيم يدوي جديد")
                        }
                    }
                    items(collections, key = { it.id }) { col ->
                        val count = allPosts.count { it.collectionId == col.id }
                        val expanded = activeFolderFilterId == col.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, extra.divider, RoundedCornerShape(14.dp))
                                .clickable {
                                    activeFolderFilterId = if (expanded) null else col.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(col.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("${col.description} • $count عناصر", style = MaterialTheme.typography.labelSmall, color = extra.subtleText)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteCollection(col.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "حذف المجلد", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (expanded) {
                                    Spacer(Modifier.height(8.dp))
                                    val folderPosts = allPosts.filter { it.collectionId == col.id }
                                    if (folderPosts.isEmpty()) {
                                        Text("لا توجد عناصر داخل هذا المجلد بعد.", style = MaterialTheme.typography.bodySmall, color = extra.subtleText)
                                    } else {
                                        folderPosts.forEach { p ->
                                            Text("• ${p.authorHandle}: ${p.caption}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(currentUser?.displayName ?: "") }
        var handle by remember { mutableStateOf(currentUser?.handle ?: "") }
        var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
        var verified by remember { mutableStateOf(currentUser?.isVerified ?: true) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل الملف الشخصي والتوثيق") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم المعروض") })
                    OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("اسم المستخدم") })
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("النبذة التعريفية") }, minLines = 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شارة التوثيق الأمني", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = verified, onCheckedChange = { verified = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile(name, handle, bio, verified)
                    showEditProfileDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("مجلد تنظيم يدوي جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("اسم المجلد") })
                    OutlinedTextField(value = folderDesc, onValueChange = { folderDesc = it }, label = { Text("الوصف (اختياري)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (folderName.isNotBlank()) {
                        viewModel.createCollection(folderName, folderDesc)
                        showNewFolderDialog = false
                    }
                }) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun ProfileStatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalPrismExtra.current.subtleText)
    }
}
