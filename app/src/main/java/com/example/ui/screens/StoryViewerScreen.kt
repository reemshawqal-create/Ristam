package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.StoryEntity
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.components.StoryRingAvatar
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    stories: List<StoryEntity>,
    initialStoryId: Long,
    onClose: () -> Unit,
    onMarkViewed: (Long) -> Unit,
    onSendReply: (authorId: String, authorHandle: String, replyText: String) -> Unit
) {
    BackHandler { onClose() }

    val startIdx = remember(stories, initialStoryId) {
        stories.indexOfFirst { it.id == initialStoryId }.coerceAtLeast(0)
    }
    var currentIndex by remember { mutableIntStateOf(startIdx) }
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val story = stories.getOrNull(currentIndex)
    if (story == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    LaunchedEffect(story.id) {
        onMarkViewed(story.id)
    }

    LaunchedEffect(currentIndex, isPaused, replyText.isNotEmpty()) {
        while (progress < 1f) {
            delay(50)
            if (!isPaused && replyText.isEmpty()) {
                progress += 0.01f
            }
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex, stories.size) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                        } else {
                            if (currentIndex > 0) currentIndex-- else progress = 0f
                        }
                    }
                )
            }
    ) {
        RistamMediaCanvas(
            mediaType = "PHOTO",
            mediaUri = story.mediaUri,
            drawableName = story.drawableName,
            filterPreset = story.filterPreset,
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
            warmth = 0f,
            vignette = 0.15f,
            rotationDegrees = 0f,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Segmented Story Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StoryRingAvatar(
                        drawableName = story.authorAvatar,
                        size = 36.dp,
                        showRing = false
                    )
                    Text(
                        text = story.authorHandle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق القصة", tint = Color.White)
                }
            }
        }

        // Caption + DM Reply Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (story.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("أرسل رداً مباشراً مشفراً...", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onSendReply(story.authorId, story.authorHandle, replyText)
                            replyText = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال رد مشفر",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
