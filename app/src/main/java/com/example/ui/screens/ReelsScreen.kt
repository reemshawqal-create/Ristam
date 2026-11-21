package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PostEntity
import com.example.data.model.MediaType
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.components.StoryRingAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.PrismRose
import com.example.ui.viewmodel.RistamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    viewModel: RistamViewModel,
    allPosts: List<PostEntity>,
    distractionFree: Boolean
) {
    val reelPosts = remember(allPosts) {
        val videos = allPosts.filter { it.mediaType == MediaType.VIDEO.name }
        if (videos.isNotEmpty()) videos else allPosts
    }
    var commentsPost by remember { mutableStateOf<PostEntity?>(null) }

    if (reelPosts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("لا توجد مقاطع متاحة حالياً", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reelPosts.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val post = reelPosts[page]
        val isCurrentPage = pagerState.currentPage == page
        Box(modifier = Modifier.fillMaxSize()) {
            RistamMediaCanvas(
                mediaType = MediaType.VIDEO.name,
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
                autoPlayVideo = isCurrentPage,
                onDoubleTapLike = { if (!post.isLiked) viewModel.toggleLike(post) },
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient scrim for readability
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StoryRingAvatar(
                                drawableName = post.authorAvatar,
                                size = 38.dp,
                                showRing = false
                            )
                            Text(
                                text = post.authorHandle,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (post.authorVerified) VerifiedBadge(size = 14.dp)
                        }
                        Text(
                            text = post.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (post.tagsCsv.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = post.tagsCsv.split(",").take(3).joinToString(" ") { "#${it.trim()}" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Side Actions Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.toggleLike(post) }) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "إعجاب",
                                    tint = if (post.isLiked) PrismRose else Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            if (!distractionFree) {
                                Text("${post.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { commentsPost = post }) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "تعليقات",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            if (!distractionFree) {
                                Text("${post.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        IconButton(onClick = { viewModel.toggleBookmark(post) }) {
                            Icon(
                                imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "حفظ",
                                tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    commentsPost?.let { post ->
        val comments by viewModel.observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var input by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { commentsPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("تعليقات المقطع (${comments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                comments.forEach { c ->
                    Text("${c.authorHandle}: ${c.text}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("اكتب تعليقاً...") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            viewModel.addComment(post, input)
                            input = ""
                        }
                    }) { Text("إرسال") }
                }
            }
        }
    }
}
