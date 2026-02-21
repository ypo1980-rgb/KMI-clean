package il.kmi.app.attendance.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import java.time.YearMonth

@Composable
fun AttendanceGroupStatsScreen(
    repo: AttendanceRepository,
    branch: String,
    groupKey: String,
    onBack: () -> Unit
) {
    val reports by repo.reportsLastYear(branch, groupKey).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val monthTitleFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("he", "IL"))
    }

    val reportsByMonth = remember(reports) {
        reports
            .groupBy { YearMonth.from(it.date) }
            .toList()
            .sortedByDescending { (ym, _) -> ym } // חודשים מהחדש לישן
    }

    // Accordion per month: ברירת מחדל = פתוח
    val expandedByMonth = remember { mutableStateMapOf<YearMonth, Boolean>() }

    LaunchedEffect(reportsByMonth) {
        reportsByMonth.forEach { (ym, _) ->
            expandedByMonth.putIfAbsent(ym, true)
        }
    }

    val avgPct = remember(reports) {
        if (reports.isEmpty()) 0
        else reports.map { it.percentPresent }.average().toInt()
    }

    val totalSessions = reports.size
    val avgPresent = remember(reports) {
        if (reports.isEmpty()) 0 else reports.map { it.presentCount }.average().toInt()
    }
    val avgTotal = remember(reports) {
        if (reports.isEmpty()) 0 else reports.map { it.totalMembers }.average().toInt()
    }

    var deleteMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }

    val selectedIds by remember {
        derivedStateOf { selected.filterValues { it }.keys.toList() }
    }

    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var confirmResetAll by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = "סטטיסטיקת נוכחות",
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = false,
                lockSearch = true,
                onBack = onBack,
                centerTitle = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { p ->

        // רקע כמו מסך הנוכחות
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF020617),
                            Color(0xFF111827),
                            Color(0xFF1D4ED8),
                            Color(0xFF22D3EE)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(p)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    StatsHeroCard(
                        branch = branch,
                        groupKey = groupKey,
                        avgPct = avgPct,
                        totalSessions = totalSessions
                    )
                }

                item {
                    StatsSummaryCard(
                        avgPct = avgPct,
                        totalSessions = totalSessions,
                        avgPresent = avgPresent,
                        avgTotal = avgTotal
                    )
                }

                item {
                    Text(
                        text = if (deleteMode) "בחר דו\"חות למחיקה" else "דו\"חות אחרונים (שנה אחורה)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFECFEFF),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                reportsByMonth.forEach { (ym, monthReports) ->

                    // כותרת חודש (לחיצה => קיפול/פתיחה)
                    item(key = "month_${ym}") {
                        val monthTitle = remember(ym) {
                            ym.atDay(1).format(monthTitleFormatter)
                        }

                        // ✅ לקרוא מצב “פתוח/סגור” בתוך קומפוזיציה
                        val isExpanded = expandedByMonth[ym] != false

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ✅ תמיד לפי הערך הנוכחי במפה (לא value שתפסנו)
                                    val cur = expandedByMonth[ym] != false
                                    expandedByMonth[ym] = !cur
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = monthTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFECFEFF),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "${monthReports.size} דו\"חות",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFBFDBFE),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ✅ כאן (מחוץ ל-item) לקרוא שוב מצב נוכחי בצורה “טהורה”
                    val isExpandedNow = expandedByMonth[ym] != false
                    if (isExpandedNow) {
                        items(monthReports, key = { it.id }) { r ->
                            val checked = selected[r.id] == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (deleteMode) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { v -> selected[r.id] = v }
                                    )
                                }

                                ReportRowCard(
                                    dateText = r.date.toString(),
                                    total = r.totalMembers,
                                    present = r.presentCount,
                                    excused = r.excusedCount,
                                    absent = r.absentCount,
                                    pct = r.percentPresent
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ "מחק דוחות" => מצב בחירה | במצב בחירה => "מחק נבחרים"
                        OutlinedButton(
                            onClick = {
                                if (!deleteMode) {
                                    deleteMode = true
                                    selected.clear()
                                } else {
                                    if (selectedIds.isNotEmpty()) {
                                        confirmDeleteSelected = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                            enabled = !busy
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            val label = if (!deleteMode) "מחק דוחות"
                            else "מחק נבחרים (${selectedIds.size})"
                            Text(label, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        // ✅ ביטול מצב בחירה
                        if (deleteMode) {
                            OutlinedButton(
                                onClick = {
                                    deleteMode = false
                                    selected.clear()
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                enabled = !busy
                            ) {
                                Text("ביטול", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        // ✅ איפוס הכל (כמו שעשית) – מוחק records/sessions/reports (לא מתאמנים)
                        Button(
                            onClick = { confirmResetAll = true },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            enabled = !busy
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("איפוס נוכחות", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ===== אישור איפוס הכל =====
            if (confirmResetAll) {
                ConfirmDialog(
                    title = "איפוס נוכחות לקבוצה",
                    text = "אזהרה: פעולה זו תמחק את כל נתוני הנוכחות לקבוצה:\n• סימונים (attendance_records)\n• שיעורים (training_sessions)\n• דו\"חות (attendance_reports)\n\nמתאמנים ברשימה נשארים.",
                    confirmText = "אפס הכל",
                    danger = true,
                    onConfirm = {
                        confirmResetAll = false
                        busy = true
                        scope.launch {
                            runCatching {
                                repo.resetAttendanceForGroup(branch, groupKey)
                            }.onFailure {
                                Log.e("ATT_STATS", "resetAttendanceForGroup failed", it)
                            }
                            busy = false
                            deleteMode = false
                            selected.clear()
                        }
                    },
                    onDismiss = { confirmResetAll = false }
                )
            }

            // ===== אישור מחיקת דו"חות נבחרים =====
            if (confirmDeleteSelected) {
                ConfirmDialog(
                    title = "מחיקת דו\"חות",
                    text = "למחוק ${selectedIds.size} דו\"חות מסומנים?",
                    confirmText = "מחק",
                    danger = true,
                    onConfirm = {
                        confirmDeleteSelected = false
                        busy = true
                        scope.launch {
                            runCatching {
                                // ✅ כאן המחיקה האמיתית לפי createdAtMillis
                                repo.deleteReportsByIds(
                                    branch = branch,
                                    groupKey = groupKey,
                                    reportIds = selectedIds
                                )
                            }.onFailure {
                                Log.e("ATT_STATS", "deleteReports(selected) failed", it)
                            }
                            busy = false
                            deleteMode = false
                            selected.clear()
                        }
                    },
                    onDismiss = { confirmDeleteSelected = false }
                )
            }
        }
    }
}

@Composable
private fun StatsHeroCard(
    branch: String,
    groupKey: String,
    avgPct: Int,
    totalSessions: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$branch · $groupKey",
                        style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                        color = Color(0xFFE5E7EB),
                        textAlign = TextAlign.Right,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "שיעורים עם דו\"ח: $totalSessions",
                        style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                        color = Color(0xFFBFDBFE),
                        textAlign = TextAlign.Right,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF1E40AF))
                            ),
                            shape = CircleShape
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "ממוצע נוכחות (שנה): $avgPct%",
                style = MaterialTheme.typography.titleMedium.merge(rtlStyle),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22D3EE),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsSummaryCard(
    avgPct: Int,
    totalSessions: Int,
    avgPresent: Int,
    avgTotal: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "סיכום שנה אחורה",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFECFEFF),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBox(label = "שיעורים", value = totalSessions.toString())
                StatBox(label = "ממוצע הגיעו", value = avgPresent.toString())
                StatBox(label = "ממוצע סה\"כ", value = avgTotal.toString())
                StatBox(label = "% ממוצע", value = "$avgPct%")
            }
        }
    }
}

@Composable
private fun ReportRowCard(
    dateText: String,
    total: Int,
    present: Int,
    excused: Int,
    absent: Int,
    pct: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)
        val datePretty = remember(dateText) {
            runCatching {
                val d = java.time.LocalDate.parse(dateText)
                val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("he", "IL"))
                d.format(fmt)
            }.getOrElse { dateText }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = datePretty,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.titleSmall.merge(rtlStyle),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "סה\"כ: $total | הגיעו: $present | מוצדקים: $excused | לא הגיעו: $absent",
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodySmall.merge(rtlStyle),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "נוכחות: $pct%",
                color = Color(0xFF22D3EE),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodySmall.merge(rtlStyle),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5F5)
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    danger: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
