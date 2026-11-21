package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.SupportTicketEntity
import com.example.data.model.AccentPreset
import com.example.data.model.AppPreferencesState
import com.example.data.model.CornerDensity
import com.example.data.model.DiagnosticReport
import com.example.data.model.ThemePreset
import com.example.ui.components.RistamWordmark
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RistamViewModel

@Composable
fun SettingsScreen(
    viewModel: RistamViewModel,
    prefs: AppPreferencesState,
    diagnostics: DiagnosticReport?,
    supportTickets: List<SupportTicketEntity>,
    onOpenReports: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val extra = LocalPrismExtra.current
    var showMasterPasswordDialog by remember { mutableStateOf(false) }
    var blockedTagInput by remember { mutableStateOf("") }
    var syncNoteInput by remember(prefs.cloudSyncProfileNote) { mutableStateOf(prefs.cloudSyncProfileNote) }

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
                Text("الإعدادات والخصوصية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            RistamWordmark()
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            // 1. Account, Master Password & Encryption
            item {
                SettingsCard(title = "التوثيق وكلمة المرور الرئيسية والتشفير", icon = Icons.Filled.Lock) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حماية بكلمة مرور رئيسية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (prefs.masterPasswordEnabled) "مفعلة — يتم تشفير الرسائل والخزنة بمفتاح مشتق PBKDF2" else "غير مفعلة — أضف كلمة مرور رئيسية لقفل التطبيق",
                                style = MaterialTheme.typography.bodySmall,
                                color = extra.subtleText
                            )
                        }
                        Button(
                            onClick = { showMasterPasswordDialog = true },
                            modifier = Modifier.testTag("setup_master_password_btn")
                        ) {
                            Text(if (prefs.masterPasswordEnabled) "تغيير / إيقاف" else "تفعيل")
                        }
                    }

                    if (prefs.masterPasswordEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("القفل التلقائي عند فتح التطبيق", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.lockOnLaunch,
                                onCheckedChange = { viewModel.setLockOnLaunch(it) }
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.lockVaultNow() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("قفل التطبيق فوراً")
                        }
                    }
                }
            }

            // 2. Appearance & Dark Mode Customization
            item {
                SettingsCard(title = "المظهر والوضع الليلي والتخصيص", icon = Icons.Filled.Palette) {
                    Text("نمط الإضاءة الداكنة / النهارية", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ThemePreset.entries) { preset ->
                            FilterChip(
                                selected = prefs.themePreset == preset,
                                onClick = { viewModel.setTheme(preset) },
                                label = { Text(preset.labelAr, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("لون التمييز الأساسي", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccentPreset.entries) { accent ->
                            FilterChip(
                                selected = prefs.accentPreset == accent,
                                onClick = { viewModel.setAccent(accent) },
                                label = { Text(accent.labelAr, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("كثافة وتدوير الواجهة", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CornerDensity.entries.forEach { density ->
                            FilterChip(
                                selected = prefs.cornerDensity == density,
                                onClick = { viewModel.setCornerDensity(density) },
                                label = { Text(density.labelAr, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع التركيز الخالي من التشتيت", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("يخفي عدادات الإعجاب والقصص لتجربة هادئة 100%", style = MaterialTheme.typography.bodySmall, color = extra.subtleText)
                        }
                        Switch(
                            checked = prefs.distractionFreeMode,
                            onCheckedChange = { viewModel.setDistractionFree(it) },
                            modifier = Modifier.testTag("distraction_free_switch")
                        )
                    }
                }
            }

            // 3. Deterministic Non-AI Algorithm & Manual Organization
            item {
                SettingsCard(title = "التنظيم اليدوي وخوارزمية المحتوى (بدون AI)", icon = Icons.Filled.Tune) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "لا يحتوي Ristam على أي ذكاء اصطناعي. ترتيب المحتوى يعتمد فقط على أوزان حسابية شفافة تضبطها أنت يدوياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrismViolet,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    val w = prefs.algorithmWeights
                    WeightSlider("وزن الحداثة الزمنية", w.recencyWeight) {
                        viewModel.setAlgorithmWeights(w.copy(recencyWeight = it))
                    }
                    WeightSlider("وزن تطابق الوسوم اليدوية", w.tagAffinityWeight) {
                        viewModel.setAlgorithmWeights(w.copy(tagAffinityWeight = it))
                    }
                    WeightSlider("وزن أولوية المبدعين المفضلين", w.creatorPriorityWeight) {
                        viewModel.setAlgorithmWeights(w.copy(creatorPriorityWeight = it))
                    }
                    WeightSlider("تفضيل المقاطع المرئية", w.videoPreferenceBoost) {
                        viewModel.setAlgorithmWeights(w.copy(videoPreferenceBoost = it))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = blockedTagInput,
                            onValueChange = { blockedTagInput = it },
                            label = { Text("إخفاء وسم يدوياً") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            if (blockedTagInput.isNotBlank()) {
                                viewModel.addBlockedTag(blockedTagInput)
                                blockedTagInput = ""
                            }
                        }) { Text("إضافة") }
                    }
                    if (prefs.blockedTags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(prefs.blockedTags) { tag ->
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.removeBlockedTag(tag) },
                                    label = { Text("#$tag ✕", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Permanent Offline Mode & Optional Cloud Sync
            item {
                SettingsCard(title = "العمل الدائم دون اتصال والمزامنة الاختيارية", icon = Icons.Filled.CloudOff) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع العمل دون اتصال بالإنترنت بشكل دائم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("كل الميزات والتعديل والتشفير تعمل محلياً 100%", style = MaterialTheme.typography.bodySmall, color = extra.subtleText)
                        }
                        Switch(
                            checked = prefs.permanentOfflineMode,
                            onCheckedChange = {
                                viewModel.setOfflineAndSync(it, if (it) false else prefs.optionalCloudSyncEnabled, syncNoteInput)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مزامنة الملف الشخصي الاختيارية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "للمزامنة السحابية الآمنة عبر السحابة، فعّل إعداد Firebase؛ أو استخدم ملفات .ristambak المشفرة للنقل اليدوي بين الأجهزة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extra.subtleText
                            )
                        }
                        Switch(
                            checked = prefs.optionalCloudSyncEnabled,
                            onCheckedChange = {
                                viewModel.setOfflineAndSync(prefs.permanentOfflineMode, it, syncNoteInput)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = syncNoteInput,
                        onValueChange = { syncNoteInput = it },
                        label = { Text("معرّف المزامنة الاختياري / اسم الجهاز") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Encrypted Local Backup & Readable Export
            item {
                BackupAndExportSection(viewModel = viewModel)
            }

            // 6. Reports & Productivity Shortcut
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenReports)
                        .testTag("settings_open_reports_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("تقارير البيانات والإنتاجية والتنبيهات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("عرض الرسوم البيانية وساعات النشاط وحدود التنبيه", style = MaterialTheme.typography.bodySmall, color = extra.subtleText)
                            }
                        }
                        Text("فتح ←", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 7. Privacy & Zero-Ads Guarantee
            item {
                SettingsCard(title = "الخصوصية التامة ومنع الإعلانات", icon = Icons.Filled.PrivacyTip) {
                    Text("• 0 إعلانات مدمجة نهائياً في أي شاشة.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    Text("• 0 أدوات تتبع أو جمع بيانات شخصية خارج نطاق الاستخدام.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    Text("• النسخ الاحتياطي محلي مشفر فقط، مع إمكانية التصدير بصيغ JSON/CSV.", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 8. 24/7 Technical Support Center
            item {
                Support24x7Section(
                    viewModel = viewModel,
                    diagnostics = diagnostics,
                    tickets = supportTickets
                )
            }
        }
    }

    if (showMasterPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMasterPasswordDialog = false },
            title = { Text("كلمة المرور الرئيسية للخزنة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تحمي كلمة المرور الرئيسية دخولك إلى التطبيق وتُستخدم لاشتقاق مفتاح التشفير PBKDF2.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("كلمة المرور الجديدة (4 أحرف فأكثر)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPass.length >= 4,
                    onClick = {
                        viewModel.setupMasterPassword(newPass)
                        showMasterPasswordDialog = false
                    }
                ) { Text("حفظ وتفعيل") }
            },
            dismissButton = {
                Row {
                    if (prefs.masterPasswordEnabled) {
                        TextButton(onClick = {
                            viewModel.disableMasterPassword()
                            showMasterPasswordDialog = false
                        }) {
                            Text("إلغاء القفل", color = PrismRose)
                        }
                    }
                    TextButton(onClick = { showMasterPasswordDialog = false }) { Text("إغلاق") }
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun WeightSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}
