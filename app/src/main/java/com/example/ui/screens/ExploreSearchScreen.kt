package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PostEntity
import com.example.data.model.AppPreferencesState
import com.example.data.model.MediaType
import com.example.data.model.RankedPost
import com.example.data.model.SearchFilterType
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.viewmodel.RistamViewModel

@Composable
fun ExploreSearchScreen(
    viewModel: RistamViewModel,
    rankedPosts: List<RankedPost>,
    prefs: AppPreferencesState,
    onOpenSettingsAlgorithm: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(SearchFilterType.ALL) }
    var inspectedPost by remember { mutableStateOf<RankedPost?>(null) }

    val displayedPosts = remember(query, filterType, rankedPosts) {
        if (query.isBlank() && filterType == SearchFilterType.ALL) {
            rankedPosts
        } else {
            val matched = viewModel.searchPosts(query, filterType)
            val rankLookup = rankedPosts.associateBy { it.post.id }
            matched.map { rankLookup[it.id] ?: RankedPost(it, 1.0, listOf("نتيجة بحث ذكي محلي")) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Smart Search Bar (100% Local Non-AI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("بحث ذكي بالوسوم # أو الحسابات @ أو النص...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("smart_search_input")
            )
            IconButton(
                onClick = onOpenSettingsAlgorithm,
                modifier = Modifier.testTag("tune_algorithm_btn")
            ) {
                Icon(Icons.Filled.Tune, contentDescription = "ضبط الخوارزمية اليدوية")
            }
        }

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilterType.entries) { filter ->
                FilterChip(
                    selected = filterType == filter,
                    onClick = {
                        filterType = filter
                        if (query.isNotBlank()) viewModel.recordSearchQuery(query)
                    },
                    label = { Text(filter.labelAr, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // Recent Searches
        if (prefs.recentSearches.isNotEmpty() && query.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("عمليات البحث الأخيرة", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { viewModel.clearRecentSearches() }) {
                    Text("مسح", style = MaterialTheme.typography.labelSmall)
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prefs.recentSearches) { recent ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { query = recent }
                    ) {
                        Text(
                            text = recent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3-Column Instagram Explore Grid
        if (displayedPosts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = if (query.isBlank()) "لا توجد منشورات للاستكشاف بعد" else "لا توجد نتائج مطابقة لبحثك",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "أضف صورك أو مقاطعك من تبويب الإنشاء لتظهر هنا مصنفة بالخوارزمية اليدوية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 115.dp),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                items(displayedPosts, key = { it.post.id }) { ranked ->
                    val post = ranked.post
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (query.isNotBlank()) viewModel.recordSearchQuery(query)
                                inspectedPost = ranked
                            }
                            .testTag("explore_item_${post.id}")
                    ) {
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
                        if (post.mediaType == MediaType.VIDEO.name) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = "مقطع فيديو",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    inspectedPost?.let { ranked ->
        val post = ranked.post
        AlertDialog(
            onDismissRequest = { inspectedPost = null },
            title = {
                Text(
                    text = "@${post.authorHandle}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        playbackSpeed = post.playbackSpeed,
                        isMuted = post.isMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                    Text(post.caption, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "شفافية الخوارزمية اليدوية (بدون ذكاء اصطناعي):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${ranked.matchReasons.joinToString(" • ")} (الدرجة: ${"%.2f".format(ranked.score)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleBookmark(post)
                    inspectedPost = null
                }) {
                    Text(if (post.isBookmarked) "إزالة من المحفوظات" else "حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedPost = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
