package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppNotificationEntity
import com.example.data.model.AppPreferencesState
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismViolet
import com.example.ui.viewmodel.RistamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    viewModel: RistamViewModel,
    notifications: List<AppNotificationEntity>,
    prefs: AppPreferencesState,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val extra = LocalPrismExtra.current
    val timeFmt = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setProductivitySettings(
            prefs.dailyUsageLimitMinutes,
            prefs.productivityAlertsEnabled,
            granted
        )
        if (granted) {
            viewModel.triggerTestProductivityCheck()
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Text("الإشعارات الفورية والنشاط", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                Text("تحديد الكل كمقروء")
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("إشعارات النظام الفورية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (prefs.systemNotificationsEnabled) "مفعلة لإشعارات الإنتاجية والنسخ الاحتياطي" else "تفعيل تلقي تنبيهات فورية على النظام",
                        style = MaterialTheme.typography.bodySmall,
                        color = extra.subtleText
                    )
                }
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !prefs.systemNotificationsEnabled) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setProductivitySettings(
                            prefs.dailyUsageLimitMinutes,
                            prefs.productivityAlertsEnabled,
                            !prefs.systemNotificationsEnabled
                        )
                        viewModel.triggerTestProductivityCheck()
                    }
                }) {
                    Text(if (prefs.systemNotificationsEnabled) "اختبار إشعار" else "تفعيل")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notifications, key = { it.id }) { notif ->
                val icon = when (notif.type) {
                    "MESSAGE" -> Icons.Filled.MarkEmailUnread
                    "PRODUCTIVITY" -> Icons.Filled.Timer
                    "BACKUP" -> Icons.Filled.Backup
                    "LIKE" -> Icons.Filled.Favorite
                    else -> Icons.Filled.NotificationsActive
                }
                val tint = when (notif.type) {
                    "MESSAGE" -> PrismRose
                    "PRODUCTIVITY" -> PrismAmber
                    else -> PrismViolet
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(notif.body, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = timeFmt.format(Date(notif.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = extra.subtleText
                            )
                        }
                    }
                }
            }
        }
    }
}
