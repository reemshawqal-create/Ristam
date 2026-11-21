package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.DecryptedThread
import com.example.ui.components.StoryRingAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismRose
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RistamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectMessagesScreen(
    viewModel: RistamViewModel,
    threads: List<DecryptedThread>,
    onBack: () -> Unit
) {
    var activeThread by remember { mutableStateOf<DecryptedThread?>(null) }

    if (activeThread != null) {
        ChatThreadScreen(
            viewModel = viewModel,
            thread = activeThread!!,
            onBack = { activeThread = null }
        )
        return
    }

    BackHandler { onBack() }

    val extra = LocalPrismExtra.current
    var search by remember { mutableStateOf("") }
    var showNewThreadDialog by remember { mutableStateOf(false) }
    val filteredThreads = remember(threads, search) {
        if (search.isBlank()) threads
        else threads.filter {
            it.participantName.contains(search, ignoreCase = true) ||
                it.participantHandle.contains(search, ignoreCase = true) ||
                it.lastMessagePreview.contains(search, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Text(
                    text = "الرسائل المباشرة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Text("AES-256", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    }
                }
                androidx.compose.material3.TextButton(onClick = { showNewThreadDialog = true }) {
                    Text("+ جديد")
                }
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("بحث في المحادثات المشفرة...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        if (filteredThreads.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "لا توجد محادثات بعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ابدأ محادثة مشفرة أو أنشئ دفتر ملاحظات شخصي محمي بتشفير AES-256.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.subtleText
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(14.dp))
                androidx.compose.material3.Button(onClick = { showNewThreadDialog = true }) {
                    Text("إنشاء محادثة / ملاحظة مشفرة")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredThreads, key = { it.threadId }) { thread ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markThreadRead(thread.threadId)
                                activeThread = thread
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("dm_thread_${thread.threadId}"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            StoryRingAvatar(
                                drawableName = thread.participantAvatar,
                                size = 54.dp,
                                showRing = false
                            )
                            if (thread.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = thread.participantName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (thread.isVerified) VerifiedBadge(size = 14.dp)
                            }
                            Text(
                                text = thread.lastMessagePreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.onSurface else extra.subtleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (thread.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(PrismRose),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${thread.unreadCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    HorizontalDivider(color = extra.divider.copy(alpha = 0.35f), thickness = 0.5.dp)
                }
            }
        }
    }

    if (showNewThreadDialog) {
        var contactName by remember { mutableStateOf("") }
        var contactHandle by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewThreadDialog = false },
            title = { Text("إنشاء محادثة / خزنة رسائل مشفرة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("الاسم (أو ملاحظات خاصة)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactHandle,
                        onValueChange = { contactHandle = it },
                        label = { Text("المعرف @ (اختياري)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    viewModel.createCustomThread(contactName, contactHandle)
                    showNewThreadDialog = false
                }) { Text("إنشاء") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNewThreadDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun ChatThreadScreen(
    viewModel: RistamViewModel,
    thread: DecryptedThread,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val messages by viewModel.observeMessages(thread.threadId).collectAsStateWithLifecycle(initialValue = emptyList())
    var inputText by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(thread.threadId) {
        viewModel.markThreadRead(thread.threadId)
    }

    val mediaAttachLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            viewModel.sendMessage(thread.threadId, inputText, uri.toString())
            inputText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع للمحادثات")
            }
            StoryRingAvatar(drawableName = thread.participantAvatar, size = 38.dp, showRing = false)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(thread.participantName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (thread.isVerified) VerifiedBadge(size = 13.dp)
                }
                Text(
                    text = "مشفر محلياً AES-256-GCM",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
        }

        HorizontalDivider(color = LocalPrismExtra.current.divider, thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isFromMe) 16.dp else 4.dp,
                            bottomEnd = if (msg.isFromMe) 4.dp else 16.dp
                        ),
                        color = if (msg.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!msg.attachmentUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = Uri.parse(msg.attachmentUri),
                                    contentDescription = "مرفق وسائط",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Text(
                                text = msg.body,
                                color = if (msg.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${timeFormat.format(Date(msg.createdAt))} • 🔒",
                                color = if (msg.isFromMe) Color.White.copy(alpha = 0.75f) else LocalPrismExtra.current.subtleText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = {
                mediaAttachLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Filled.AttachFile, contentDescription = "إرفاق صورة")
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("اكتب رسالة مشفرة...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("dm_message_input")
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(thread.threadId, inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.testTag("dm_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
