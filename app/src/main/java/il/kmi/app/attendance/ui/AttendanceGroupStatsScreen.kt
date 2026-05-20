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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
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
import il.kmi.app.localization.rememberIsEnglish

@Composable
fun AttendanceGroupStatsScreen(
    repo: AttendanceRepository,
    branch: String,
    groupKey: String,
    onBack: () -> Unit
) {
    val isEnglish = rememberIsEnglish()
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val screenTextAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val screenHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val screenTextDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    val screenTextStyle = TextStyle(textDirection = screenTextDirection)

    val reports by repo.reportsLastYear(branch, groupKey).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val monthTitleFormatter = remember(isEnglish) {
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
        )
    }

    val reportsByMonth = remember(reports) {
        reports
            .groupBy { YearMonth.from(it.date) }
            .toList()
            .sortedByDescending { (ym, _) -> ym } // חודשים מהחדש לישן
    }

    // Accordion per month: ברירת מחדל = פתוח
    val expandedByMonth = remember { mutableStateMapOf<YearMonth, Boolean>() }

    // פתיחה/סגירה של פירוט מתאמנים לכל דו"ח
    val expandedReportDetails = remember { mutableStateMapOf<Long, Boolean>() }

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
                title = tr("סטטיסטיקת נוכחות", "Attendance statistics"),
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = false,
                lockSearch = true,
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
                        totalSessions = totalSessions,
                        isEnglish = isEnglish
                    )
                }

                item {
                    StatsSummaryCard(
                        avgPct = avgPct,
                        totalSessions = totalSessions,
                        avgPresent = avgPresent,
                        avgTotal = avgTotal,
                        isEnglish = isEnglish
                    )
                }

                item {
                    Text(
                        text = if (deleteMode) {
                            tr("בחר דו\"חות למחיקה", "Select reports to delete")
                        } else {
                            tr("דו\"חות אחרונים (שנה אחורה)", "Recent reports - last year")
                        },
                        style = MaterialTheme.typography.titleSmall.merge(screenTextStyle),
                        color = Color(0xFFECFEFF),
                        textAlign = screenTextAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
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
                                    val cur = expandedByMonth[ym] != false
                                    expandedByMonth[ym] = !cur
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (!isEnglish) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFF93C5FD)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                        ) {
                                            Text(
                                                text = monthTitle,
                                                style = MaterialTheme.typography.titleMedium.merge(screenTextStyle),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFECFEFF),
                                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Text(
                                                text = if (isEnglish) {
                                                    "${monthReports.size} reports"
                                                } else {
                                                    "${monthReports.size} דו\"חות"
                                                },
                                                style = MaterialTheme.typography.labelSmall.merge(screenTextStyle),
                                                color = Color(0xFFBFDBFE),
                                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    if (isEnglish) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFF93C5FD)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ✅ כאן (מחוץ ל-item) לקרוא שוב מצב נוכחי בצורה “טהורה”
                    val isExpandedNow = expandedByMonth[ym] != false
                    if (isExpandedNow) {
                        items(monthReports, key = { it.id }) { r ->
                            val checked = selected[r.id] == true
                            val detailsExpanded = expandedReportDetails[r.id] == true

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                        pct = r.percentPresent,
                                        isEnglish = isEnglish,
                                        isDetailsExpanded = detailsExpanded,
                                        onToggleDetails = {
                                            expandedReportDetails[r.id] = !detailsExpanded
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (detailsExpanded) {
                                    ReportAttendanceDetailsCard(
                                        repo = repo,
                                        branch = branch,
                                        groupKey = groupKey,
                                        date = r.date,
                                        isEnglish = isEnglish
                                    )
                                }
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
                            val label = if (!deleteMode) {
                                tr("מחק דוחות", "Delete reports")
                            } else {
                                tr("מחק נבחרים (${selectedIds.size})", "Delete selected (${selectedIds.size})")
                            }

                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                                Text(
                                    text = tr("ביטול", "Cancel"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
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
                            Text(
                                text = tr("איפוס", "Reset"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ===== אישור איפוס הכל =====
            if (confirmResetAll) {
                ConfirmDialog(
                    title = tr("איפוס נוכחות לקבוצה", "Reset group attendance"),
                    text = tr(
                        "אזהרה: פעולה זו תמחק את כל נתוני הנוכחות לקבוצה:\n• סימונים\n• שיעורים\n• דו\"חות\n\nמתאמנים ברשימה נשארים.",
                        "Warning: this action will delete all attendance data for this group:\n• Attendance marks\n• Sessions\n• Reports\n\nThe trainee list will remain."
                    ),
                    confirmText = tr("אפס הכל", "Reset all"),
                    dismissText = tr("ביטול", "Cancel"),
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
                    title = tr("מחיקת דו\"חות", "Delete reports"),
                    text = tr(
                        "למחוק ${selectedIds.size} דו\"חות מסומנים?",
                        "Delete ${selectedIds.size} selected reports?"
                    ),
                    confirmText = tr("מחק", "Delete"),
                    dismissText = tr("ביטול", "Cancel"),
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
    totalSessions: Int,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFF1D4ED8).copy(alpha = 0.22f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEnglish) {
                        StatsGlowIcon()
                        Spacer(Modifier.padding(horizontal = 6.dp))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = horizontalAlignment
                        ) {
                            Text(
                                text = "$branch · $groupKey",
                                style = MaterialTheme.typography.labelMedium.merge(textStyle),
                                color = Color(0xFFE5E7EB),
                                textAlign = align,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = tr("שיעורים עם דו\"ח: $totalSessions", "Reported sessions: $totalSessions"),
                                style = MaterialTheme.typography.labelMedium.merge(textStyle),
                                color = Color(0xFFBFDBFE),
                                textAlign = align,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (isEnglish) {
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        StatsGlowIcon()
                    }
                }
            }

            Text(
                text = tr("ממוצע נוכחות שנה: $avgPct%", "Year attendance average: $avgPct%"),
                style = MaterialTheme.typography.titleMedium.merge(textStyle),
                fontWeight = FontWeight.Black,
                color = Color(0xFF22D3EE),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsGlowIcon() {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF1E40AF))
                ),
                shape = CircleShape
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun StatsSummaryCard(
    avgPct: Int,
    totalSessions: Int,
    avgPresent: Int,
    avgTotal: Int,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1D4ED8).copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = tr("סיכום שנה אחורה", "Last year summary"),
                style = MaterialTheme.typography.titleMedium.merge(textStyle),
                fontWeight = FontWeight.Black,
                color = Color(0xFFECFEFF),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        label = tr("שיעורים", "Sessions"),
                        value = totalSessions.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = tr("ממוצע הגיעו", "Avg. present"),
                        value = avgPresent.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        label = tr("ממוצע סה״כ", "Avg. total"),
                        value = avgTotal.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = tr("ממוצע נוכחות", "Avg. attendance"),
                        value = "$avgPct%",
                        modifier = Modifier.weight(1f)
                    )
                }
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
    pct: Int,
    isEnglish: Boolean,
    isDetailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        val datePretty = remember(dateText, isEnglish) {
            runCatching {
                val d = java.time.LocalDate.parse(dateText)
                val fmt = DateTimeFormatter.ofPattern(
                    "dd.MM.yyyy",
                    if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
                )
                d.format(fmt)
            }.getOrElse { dateText }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = datePretty,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = align,
                style = MaterialTheme.typography.titleMedium.merge(textStyle),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniReportStat(
                    label = tr("סה״כ", "Total"),
                    value = total.toString(),
                    modifier = Modifier.weight(1f)
                )
                MiniReportStat(
                    label = tr("הגיעו", "Present"),
                    value = present.toString(),
                    modifier = Modifier.weight(1f)
                )
                MiniReportStat(
                    label = tr("מוצדקים", "Excused"),
                    value = excused.toString(),
                    modifier = Modifier.weight(1f)
                )
                MiniReportStat(
                    label = tr("נעדרו", "Absent"),
                    value = absent.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = tr("נוכחות: $pct%", "Attendance: $pct%"),
                color = Color(0xFF22D3EE),
                fontWeight = FontWeight.Black,
                textAlign = align,
                style = MaterialTheme.typography.bodyMedium.merge(textStyle),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onToggleDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Icon(
                    imageVector = if (isDetailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(Modifier.padding(horizontal = 4.dp))

                Text(
                    text = if (isDetailsExpanded) {
                        tr("סגור רשימת נוכחות", "Hide attendance list")
                    } else {
                        tr("פתח רשימת נוכחות", "Show attendance list")
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReportAttendanceDetailsCard(
    repo: AttendanceRepository,
    branch: String,
    groupKey: String,
    date: java.time.LocalDate,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )

    val members by repo.members(branch, groupKey).collectAsState(initial = emptyList())
    val records by repo.attendanceForDay(branch, groupKey, date).collectAsState(initial = emptyList())

    val statusByMemberId = remember(records) {
        records.associate { it.memberId to it.status }
    }

    val presentMembers = remember(members, statusByMemberId) {
        members.filter { statusByMemberId[it.id] == AttendanceStatus.PRESENT }
    }

    val excusedMembers = remember(members, statusByMemberId) {
        members.filter { statusByMemberId[it.id] == AttendanceStatus.EXCUSED }
    }

    val absentMembers = remember(members, statusByMemberId) {
        members.filter { member ->
            val status = statusByMemberId[member.id]
            status == AttendanceStatus.ABSENT || status == null
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF020617).copy(alpha = 0.36f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = tr("פירוט נוכחות בדו״ח", "Attendance details"),
                style = MaterialTheme.typography.titleSmall.merge(textStyle),
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            AttendanceStatusSection(
                title = tr("הגיעו", "Present"),
                names = presentMembers.map { it.displayName },
                emptyText = tr("אין מתאמנים שסומנו הגיעו", "No trainees marked present"),
                color = Color(0xFF22C55E),
                isEnglish = isEnglish
            )

            AttendanceStatusSection(
                title = tr("מוצדקים", "Excused"),
                names = excusedMembers.map { it.displayName },
                emptyText = tr("אין מתאמנים שסומנו מוצדקים", "No trainees marked excused"),
                color = Color(0xFFF59E0B),
                isEnglish = isEnglish
            )

            AttendanceStatusSection(
                title = tr("לא הגיעו", "Absent"),
                names = absentMembers.map { it.displayName },
                emptyText = tr("אין מתאמנים שלא הגיעו", "No absent trainees"),
                color = Color(0xFFEF4444),
                isEnglish = isEnglish
            )
        }
    }
}

@Composable
private fun AttendanceStatusSection(
    title: String,
    names: List<String>,
    emptyText: String,
    color: Color,
    isEnglish: Boolean
) {
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = "$title (${names.size})",
                style = MaterialTheme.typography.labelLarge.merge(textStyle),
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            if (names.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall.merge(textStyle),
                    color = Color(0xFFCBD5E1),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                names.forEach { name ->
                    Text(
                        text = "• $name",
                        style = MaterialTheme.typography.bodyMedium.merge(textStyle),
                        color = Color.White,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5F5),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
        }
    }
}

@Composable
private fun MiniReportStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Text(
                text = label,
                color = Color(0xFFCBD5F5),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    dismissText: String,
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
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
