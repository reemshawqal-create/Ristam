package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UsageStatEntity
import com.example.data.model.AppPreferencesState
import com.example.ui.theme.LocalPrismExtra
import com.example.ui.theme.PrismAmber
import com.example.ui.theme.PrismRose
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RistamViewModel
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    viewModel: RistamViewModel,
    usageStats: List<UsageStatEntity>,
    prefs: AppPreferencesState,
    sessionSeconds: Int,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val extra = LocalPrismExtra.current
    val today = usageStats.firstOrNull()
    val todayTotalMin = if (today != null) {
        (today.feedSeconds + today.exploreSeconds + today.reelsSeconds + today.messagesSeconds + today.studioSeconds) / 60
    } else {
        sessionSeconds / 60
    }
    val progress = (todayTotalMin.toFloat() / prefs.dailyUsageLimitMinutes.coerceAtLeast(10)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Text("تقارير البيانات والإنتاجية الدورية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            // Productivity Goal Card
            item {
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
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = PrismAmber)
                            Text("هدف الإنتاجية اليومي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "الاستخدام اليوم: $todayTotalMin دقيقة من الحد المحدد (${prefs.dailyUsageLimitMinutes} دقيقة)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            color = if (progress >= 0.9f) PrismRose else PrismViolet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                        Text(
                            text = "تعديل الحد اليومي للتنبيه: ${prefs.dailyUsageLimitMinutes} دقيقة",
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.subtleText
                        )
                        Slider(
                            value = prefs.dailyUsageLimitMinutes.toFloat(),
                            onValueChange = {
                                viewModel.setProductivitySettings(
                                    it.roundToInt(),
                                    prefs.productivityAlertsEnabled,
                                    prefs.systemNotificationsEnabled
                                )
                            },
                            valueRange = 10f..180f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تنبيهات الاستراحة والإنتاجية", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.productivityAlertsEnabled,
                                onCheckedChange = {
                                    viewModel.setProductivitySettings(
                                        prefs.dailyUsageLimitMinutes,
                                        it,
                                        prefs.systemNotificationsEnabled
                                    )
                                }
                            )
                        }
                        Button(
                            onClick = { viewModel.triggerTestProductivityCheck() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                            Text("  إرسال ملخص فوري الآن")
                        }
                    }
                }
            }

            // Custom Canvas Weekly Chart
            item {
                val ordered = usageStats.reversed().takeLast(7)
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("التقرير الأسبوعي للنشاط (دقائق يومياً)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val itemsToDraw = if (ordered.isEmpty()) listOf(UsageStatEntity("today", feedSeconds = 180)) else ordered
                            val maxTotal = itemsToDraw.maxOf {
                                (it.feedSeconds + it.exploreSeconds + it.reelsSeconds + it.messagesSeconds + it.studioSeconds).coerceAtLeast(60)
                            }.toFloat()

                            val barWidth = (size.width / (itemsToDraw.size * 2f)).coerceAtLeast(18f)
                            val spacing = size.width / itemsToDraw.size

                            itemsToDraw.forEachIndexed { idx, stat ->
                                val browseSec = stat.feedSeconds + stat.exploreSeconds + stat.reelsSeconds
                                val createSec = stat.studioSeconds + stat.messagesSeconds
                                val totalH = ((browseSec + createSec) / maxTotal) * (size.height * 0.85f)
                                val createH = (createSec / maxTotal) * (size.height * 0.85f)

                                val x = idx * spacing + (spacing - barWidth) / 2f
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(x, size.height - totalH),
                                    size = Size(barWidth, totalH),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                drawRoundRect(
                                    color = secondaryColor,
                                    topLeft = Offset(x, size.height - createH),
                                    size = Size(barWidth, createH),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ordered.forEach { stat ->
                                Text(
                                    text = stat.dateKey.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extra.subtleText
                                )
                            }
                        }
                    }
                }
            }

            // Data Usage & Zero-Telemetry Summary
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = SuccessGreen)
                            Text("تقرير استخدام البيانات والخصوصية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        ReportRow("استهلاك شبكة الإعلانات والمتتبعات", "0 KB (محظور بالكامل)")
                        ReportRow("استهلاك محرك الذكاء الاصطناعي السحابي", "0 KB (غير موجود)")
                        ReportRow("تخزين الوسائط والرسائل المحلي", "مشفر بالكامل داخل الجهاز")
                        ReportRow("عمليات النسخ الاحتياطي المسجلة", "${usageStats.sumOf { it.backupsCreated }} نسخة")
                        ReportRow("المنشورات المحررة محلياً", "${usageStats.sumOf { it.postsCreated }} منشور")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalPrismExtra.current.subtleText)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
