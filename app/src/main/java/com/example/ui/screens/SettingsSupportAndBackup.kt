package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.SupportTicketEntity
import com.example.data.model.DiagnosticReport
import com.example.security.CipherVault
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RistamViewModel

@Composable
fun BackupAndExportSection(viewModel: RistamViewModel) {
    var pendingBackupPassphrase by remember { mutableStateOf("") }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && pendingBackupPassphrase.isNotBlank()) {
            viewModel.createEncryptedBackup(uri, pendingBackupPassphrase)
            pendingBackupPassphrase = ""
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && pendingBackupPassphrase.isNotBlank()) {
            viewModel.restoreEncryptedBackup(uri, pendingBackupPassphrase)
            pendingBackupPassphrase = ""
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportJson(uri)
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportCsv(uri)
    }

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
                Icon(Icons.Filled.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("النسخ الاحتياطي المحلي المشفر والتصدير", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "النسخ الاحتياطي محلي ومشفر حصرياً بصيغة .ristambak (AES-256-GCM)، مع دعم تصدير قابل للقراءة للتطبيقات الخارجية.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalPrismExtra.current.subtleText
            )

            Button(
                onClick = { showBackupDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_encrypted_backup_btn")
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Text("  إنشاء نسخة احتياطية محلية مشفرة (.ristambak)")
            }

            OutlinedButton(
                onClick = { showRestoreDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restore_encrypted_backup_btn")
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Text("  استعادة نسخة احتياطية مشفرة")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportJsonLauncher.launch("ristam_export.json") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_json_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  تصدير JSON")
                }
                OutlinedButton(
                    onClick = { exportCsvLauncher.launch("ristam_posts.csv") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text("  تصدير CSV")
                }
            }
        }
    }

    if (showBackupDialog) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("تشفير النسخة الاحتياطية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة مرور لتشفير ملف النسخة الاحتياطية المحلي AES-256:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("كلمة مرور النسخة") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = pass.length >= 4,
                    onClick = {
                        pendingBackupPassphrase = pass
                        showBackupDialog = false
                        createBackupLauncher.launch("ristam_backup.ristambak")
                    }
                ) { Text("اختيار مكان الحفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showRestoreDialog) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("استعادة نسخة مشفرة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور لفك تشفير واستعادة ملف .ristambak:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("كلمة مرور فك التشفير") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = pass.length >= 4,
                    onClick = {
                        pendingBackupPassphrase = pass
                        showRestoreDialog = false
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    }
                ) { Text("اختيار الملف") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun Support24x7Section(
    viewModel: RistamViewModel,
    diagnostics: DiagnosticReport?,
    tickets: List<SupportTicketEntity>
) {
    var ticketSubject by remember { mutableStateOf("") }
    var ticketDetails by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الخصوصية والتشفير") }

    LaunchedEffect(Unit) {
        viewModel.runDiagnostics()
    }

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
                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = PrismAmber)
                Text("مركز الدعم التقني المتاح 24/7", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "فحص تلقائي مباشر لسلامة التشفير وقاعدة البيانات + إرسال تذاكر الدعم التقني وحفظها محلياً دون إرسال بيانات شخصية.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalPrismExtra.current.subtleText
            )

            diagnostics?.let { diag ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "تشخيص النظام المباشر",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AES-256 Self-Test: ${if (diag.aesCipherSelfTestPassed) "PASS" else "FAIL"} | FP: ${CipherVault.shortFingerprint()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                        )
                        Text("الرسائل المشفرة: ${diag.totalEncryptedMessages} • المنشورات: ${diag.totalPostsCached} • الحجم: ${diag.estimatedStorageKb} KB", style = MaterialTheme.typography.bodySmall)
                        Text("الإعلانات والتتبع: ${diag.adsTrackersBlocked}", style = MaterialTheme.typography.bodySmall)
                        Text("الذكاء الاصطناعي: ${diag.aiEngineStatus}", style = MaterialTheme.typography.bodySmall)
                        Text("وضع عدم الاتصال: ${diag.offlineReadiness}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.runDiagnostics() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Text("  إعادة فحص سلامة النظام الآن")
            }

            OutlinedTextField(
                value = ticketSubject,
                onValueChange = { ticketSubject = it },
                label = { Text("عنوان طلب الدعم التقني ($selectedCategory)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ticketDetails,
                onValueChange = { ticketDetails = it },
                label = { Text("تفاصيل المشكلة أو الاستفسار") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = ticketSubject.isNotBlank() && ticketDetails.isNotBlank(),
                onClick = {
                    viewModel.submitSupportTicket(selectedCategory, ticketSubject, ticketDetails)
                    ticketSubject = ""
                    ticketDetails = ""
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل تذكرة دعم تقني 24/7")
            }

            if (tickets.isNotEmpty()) {
                Text("تذاكر الدعم المسجلة (${tickets.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                tickets.take(3).forEach { t ->
                    Text("• ${t.subject} — ${t.status}", style = MaterialTheme.typography.bodySmall, color = LocalPrismExtra.current.subtleText)
                }
            }
        }
    }
}
