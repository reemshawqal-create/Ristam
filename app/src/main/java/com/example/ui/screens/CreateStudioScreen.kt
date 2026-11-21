package com.example.ui.screens

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CollectionEntity
import com.example.data.local.PostEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.AspectRatioOption
import com.example.data.model.FilterPreset
import com.example.data.model.MediaType
import com.example.ui.components.RistamMediaCanvas
import com.example.ui.viewmodel.RistamViewModel

@Composable
fun CreateStudioScreen(
    viewModel: RistamViewModel,
    currentUser: UserProfileEntity?,
    collections: List<CollectionEntity>,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    var mediaType by remember { mutableStateOf(MediaType.PHOTO) }
    var mediaUri by remember { mutableStateOf<String?>(null) }
    var drawableName by remember { mutableStateOf<String?>("img_post_architecture") }
    var aspectRatio by remember { mutableStateOf(AspectRatioOption.PORTRAIT) }
    var filterPreset by remember { mutableStateOf(FilterPreset.ORIGINAL) }

    // Advanced manual image/video tuning
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    // Video trim & playback controls
    var videoDurationMs by remember { mutableLongStateOf(20_000L) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    // Post metadata
    var caption by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("تصوير,Ristam") }
    var locationInput by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val mime = context.contentResolver.getType(uri) ?: ""
            mediaUri = uri.toString()
            drawableName = null
            if (mime.startsWith("video")) {
                mediaType = MediaType.VIDEO
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 15_000L
                    videoDurationMs = dur.coerceAtLeast(2000L)
                    retriever.release()
                } catch (_: Exception) {
                    videoDurationMs = 15_000L
                }
                trimRange = 0f..1f
            } else {
                mediaType = MediaType.PHOTO
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "استوديو التعديل والنشر المتقدم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    filterPreset = FilterPreset.ORIGINAL
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                    warmth = 0f
                    vignette = 0f
                    rotationDegrees = 0f
                    playbackSpeed = 1f
                    isMuted = false
                    trimRange = 0f..1f
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط التعديلات")
                }
            }

            // Pickers Row (Zero-Permission Photo Picker + Quick Creative Assets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_media_btn")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  اختيار صورة أو فيديو", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        mediaType = if (mediaType == MediaType.PHOTO) MediaType.VIDEO else MediaType.PHOTO
                        if (mediaUri == null && mediaType == MediaType.VIDEO) {
                            drawableName = "img_post_city_rain"
                        }
                    },
                    modifier = Modifier.testTag("toggle_media_mode_btn")
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  ${mediaType.labelAr}", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Bundled sample selector for instant offline testing
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val samples = listOf(
                    "img_post_architecture" to "عمارة ذهبية",
                    "img_post_desert_night" to "ليل الصحراء",
                    "img_post_espresso_studio" to "مكتب القهوة",
                    "img_post_city_rain" to "مقطع المطر"
                )
                items(samples) { (resName, label) ->
                    FilterChip(
                        selected = mediaUri == null && drawableName == resName,
                        onClick = {
                            mediaUri = null
                            drawableName = resName
                            mediaType = if (resName == "img_post_city_rain") MediaType.VIDEO else MediaType.PHOTO
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Live Interactive Preview
            val trimStart = (trimRange.start * videoDurationMs).toLong()
            val trimEnd = (trimRange.endInclusive * videoDurationMs).toLong().coerceAtLeast(trimStart + 1000L)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                RistamMediaCanvas(
                    mediaType = mediaType.name,
                    mediaUri = mediaUri,
                    drawableName = drawableName,
                    filterPreset = filterPreset.name,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    warmth = warmth,
                    vignette = vignette,
                    rotationDegrees = rotationDegrees,
                    trimStartMs = trimStart,
                    trimEndMs = trimEnd,
                    playbackSpeed = playbackSpeed,
                    isMuted = isMuted,
                    autoPlayVideo = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio.ratio.coerceIn(0.7f, 1.8f))
                )
            }

            // Aspect Ratio & Rotate Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AspectRatioOption.entries.forEach { opt ->
                        FilterChip(
                            selected = aspectRatio == opt,
                            onClick = { aspectRatio = opt },
                            label = { Text(opt.labelAr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "تدوير 90 درجة")
                }
            }

            // Filter Presets
            Text("الفلاتر اللونية الفورية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(
                contentPadding = PaddingValues(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FilterPreset.entries) { preset ->
                    FilterChip(
                        selected = filterPreset == preset,
                        onClick = { filterPreset = preset },
                        label = { Text(preset.labelAr, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Precision Color Adjustments
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("أدوات التعديل اليدوية الدقيقة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    AdjustmentSlider("السطوع", brightness, -0.5f..0.5f) { brightness = it }
                    AdjustmentSlider("التباين", contrast, 0.5f..1.8f) { contrast = it }
                    AdjustmentSlider("التشبع اللوني", saturation, 0f..1.8f) { saturation = it }
                    AdjustmentSlider("الدفء اللوني", warmth, -0.4f..0.4f) { warmth = it }
                    AdjustmentSlider("تعتيم الحواف (Vignette)", vignette, 0f..1f) { vignette = it }
                }
            }

            // Video Editing Tools
            if (mediaType == MediaType.VIDEO) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("أدوات تحرير الفيديو", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "قص المقطع: ${trimStart / 1000} ث  —  ${trimEnd / 1000} ث",
                            style = MaterialTheme.typography.bodySmall
                        )
                        RangeSlider(
                            value = trimRange,
                            onValueChange = { if (it.endInclusive - it.start >= 0.1f) trimRange = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("سرعة التشغيل", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                    FilterChip(
                                        selected = playbackSpeed == spd,
                                        onClick = { playbackSpeed = spd },
                                        label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("كتم الصوت الأصلي", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isMuted, onCheckedChange = { isMuted = it })
                        }
                    }
                }
            }

            // Caption, Tags, Location, Manual Folder
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("الوصف") },
                placeholder = { Text("اكتب وصفاً ملهماً لمنشورك...") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_caption_input")
            )
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("الوسوم اليدوية (مفصولة بفاصلة)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text("الموقع (اختياري)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (collections.isNotEmpty()) {
                Text("حفظ في مجلد يدوي (اختياري)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(collections, key = { it.id }) { col ->
                        FilterChip(
                            selected = selectedCollectionId == col.id,
                            onClick = {
                                selectedCollectionId = if (selectedCollectionId == col.id) null else col.id
                            },
                            label = { Text(col.name) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val user = currentUser
                    val post = PostEntity(
                        authorId = user?.id ?: "user_me",
                        authorHandle = user?.handle ?: "ristam.studio",
                        authorDisplayName = user?.displayName ?: "حسابي",
                        authorAvatar = user?.avatarDrawable ?: "img_post_espresso_studio",
                        authorVerified = user?.isVerified ?: true,
                        caption = caption.ifBlank { "منشور جديد معدل في استوديو Ristam" },
                        tagsCsv = tagsInput,
                        location = locationInput,
                        mediaType = mediaType.name,
                        mediaUri = mediaUri,
                        drawableName = drawableName,
                        aspectRatio = aspectRatio.name,
                        filterPreset = filterPreset.name,
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        warmth = warmth,
                        vignette = vignette,
                        rotationDegrees = rotationDegrees,
                        trimStartMs = trimStart,
                        trimEndMs = trimEnd,
                        playbackSpeed = playbackSpeed,
                        isMuted = isMuted,
                        isBookmarked = selectedCollectionId != null,
                        collectionId = selectedCollectionId
                    )
                    viewModel.createPost(post, onDone = onPublished)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 8.dp)
                    .testTag("publish_post_btn")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  نشر في المخزن المحلي المشفر", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${"%.2f".format(value)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
