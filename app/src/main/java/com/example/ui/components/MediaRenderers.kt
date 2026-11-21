package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FilterPreset
import com.example.ui.theme.CinzelDecorativeFontFamily
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismStoryBrush
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.VerifiedBlue
import kotlinx.coroutines.delay

fun resolveDrawableId(name: String?): Int {
    return when (name) {
        "img_post_architecture" -> R.drawable.img_post_architecture
        "img_post_desert_night" -> R.drawable.img_post_desert_night
        "img_post_espresso_studio" -> R.drawable.img_post_espresso_studio
        "img_post_city_rain" -> R.drawable.img_post_city_rain
        "img_ristam_icon_fg" -> R.drawable.img_ristam_icon_fg
        else -> R.drawable.img_post_architecture
    }
}

@Composable
fun RistamWordmark(
    fontSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Ristam",
        style = TextStyle(
            fontFamily = CinzelDecorativeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            brush = Brush.horizontalGradient(
                colors = listOf(PrismRose, PrismViolet, PrismAmber)
            )
        ),
        modifier = modifier
    )
}

@Composable
fun VerifiedBadge(size: Dp = 15.dp) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "حساب موثق",
        tint = VerifiedBlue,
        modifier = Modifier.size(size)
    )
}

@Composable
fun StoryRingAvatar(
    drawableName: String?,
    imageUri: String? = null,
    size: Dp = 64.dp,
    hasUnseenStory: Boolean = true,
    showRing: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val ringModifier = if (showRing) {
        if (hasUnseenStory) {
            Modifier
                .size(size)
                .border(2.5.dp, PrismStoryBrush, CircleShape)
                .padding(4.dp)
        } else {
            Modifier
                .size(size)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(4.dp)
        }
    } else {
        Modifier.size(size)
    }

    Box(
        modifier = ringModifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "صورة الملف الشخصي",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = resolveDrawableId(drawableName)),
                contentDescription = "صورة الملف الشخصي",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

fun buildAdjustmentColorMatrix(
    filterPresetName: String,
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f,
    warmth: Float = 0f
): ColorMatrix {
    val preset = FilterPreset.entries.firstOrNull { it.name == filterPresetName } ?: FilterPreset.ORIGINAL
    val totalContrast = (contrast * preset.contrast).coerceIn(0.4f, 2.2f)
    val totalSat = (saturation * preset.saturation).coerceIn(0f, 2.2f)
    val totalBright = ((brightness + preset.brightness) * 140f).coerceIn(-120f, 120f)
    val totalWarmth = (warmth + preset.warmth).coerceIn(-0.6f, 0.6f)

    val satMatrix = ColorMatrix().apply { setToSaturation(totalSat) }

    val t = (1f - totalContrast) / 2f * 255f + totalBright
    val rBoost = 1f + (totalWarmth * 0.25f)
    val bBoost = 1f - (totalWarmth * 0.25f)
    val contrastWarmMatrix = ColorMatrix(
        floatArrayOf(
            totalContrast * rBoost, 0f, 0f, 0f, t,
            0f, totalContrast, 0f, 0f, t,
            0f, 0f, totalContrast * bBoost, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    contrastWarmMatrix.timesAssign(satMatrix)
    return contrastWarmMatrix
}

@Composable
fun RistamMediaCanvas(
    mediaType: String,
    mediaUri: String?,
    drawableName: String?,
    filterPreset: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    vignette: Float,
    rotationDegrees: Float,
    trimStartMs: Long = 0L,
    trimEndMs: Long = 15000L,
    playbackSpeed: Float = 1.0f,
    isMuted: Boolean = false,
    autoPlayVideo: Boolean = true,
    onDoubleTapLike: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    var mutedState by remember(isMuted) { mutableStateOf(isMuted) }
    var isPlaying by remember(autoPlayVideo) { mutableStateOf(autoPlayVideo) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750)
            showHeartBurst = false
        }
    }

    val colorFilter = remember(filterPreset, brightness, contrast, saturation, warmth) {
        ColorFilter.colorMatrix(
            buildAdjustmentColorMatrix(filterPreset, brightness, contrast, saturation, warmth)
        )
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(onDoubleTapLike) {
                if (onDoubleTapLike != null) {
                    detectTapGestures(
                        onDoubleTap = {
                            showHeartBurst = true
                            onDoubleTapLike()
                        },
                        onTap = {
                            if (mediaType == "VIDEO") {
                                isPlaying = !isPlaying
                            }
                        }
                    )
                }
            }
    ) {
        if (mediaType == "VIDEO" && !mediaUri.isNullOrBlank()) {
            NativeVideoPlayerView(
                videoUri = Uri.parse(mediaUri),
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                playbackSpeed = playbackSpeed,
                isMuted = mutedState,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationDegrees)
            )
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "reel_motion")
            val zoom by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (mediaType == "VIDEO" && isPlaying) 1.08f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = (6000 / playbackSpeed.coerceAtLeast(0.5f)).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ken_burns"
            )
            val motionModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    rotationZ = rotationDegrees
                }

            if (!mediaUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(mediaUri),
                    contentDescription = "محتوى مرئي",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = motionModifier
                )
            } else {
                Image(
                    painter = painterResource(id = resolveDrawableId(drawableName)),
                    contentDescription = "محتوى مرئي",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = motionModifier
                )
            }
        }

        // Vignette overlay
        if (vignette > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = (vignette * 0.78f).coerceIn(0f, 0.85f))
                            )
                        )
                    )
            )
        }

        // Video controls badge
        if (mediaType == "VIDEO") {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { isPlaying = !isPlaying }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { mutedState = !mutedState }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mutedState) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (mutedState) "إلغاء كتم الصوت" else "كتم الصوت",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Double tap heart burst
        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "إعجاب",
                tint = PrismRose,
                modifier = Modifier.size(88.dp)
            )
        }
    }
}

@Composable
fun NativeVideoPlayerView(
    videoUri: Uri,
    trimStartMs: Long,
    trimEndMs: Long,
    playbackSpeed: Float,
    isMuted: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember(videoUri) { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(mediaPlayer, isPlaying, isMuted, playbackSpeed) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        try {
            val vol = if (isMuted) 0f else 1f
            mp.setVolume(vol, vol)
            val params = mp.playbackParams
            params.speed = playbackSpeed.coerceIn(0.5f, 2.0f)
            mp.playbackParams = params
            if (isPlaying && !mp.isPlaying) mp.start()
            else if (!isPlaying && mp.isPlaying) mp.pause()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(mediaPlayer, trimStartMs, trimEndMs, isPlaying) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        while (true) {
            delay(250)
            try {
                if (mp.isPlaying && trimEndMs > trimStartMs && mp.currentPosition >= trimEndMs) {
                    mp.seekTo(trimStartMs.toInt())
                }
            } catch (_: Exception) {
                break
            }
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(context, videoUri)
                                setSurface(Surface(st))
                                isLooping = true
                                setOnPreparedListener { prepared ->
                                    val vol = if (isMuted) 0f else 1f
                                    prepared.setVolume(vol, vol)
                                    if (trimStartMs > 0) prepared.seekTo(trimStartMs.toInt())
                                    try {
                                        val params = prepared.playbackParams
                                        params.speed = playbackSpeed.coerceIn(0.5f, 2.0f)
                                        prepared.playbackParams = params
                                    } catch (_: Exception) {
                                    }
                                    if (isPlaying) prepared.start()
                                }
                                prepareAsync()
                            }
                            mediaPlayer = mp
                        } catch (_: Exception) {
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        }
    )
}
