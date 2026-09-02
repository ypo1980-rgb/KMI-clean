package il.kmi.app.attendance.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import java.time.YearMonth
import il.kmi.app.localization.rememberIsEnglish
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.material3.LocalContentColor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.time.LocalDate

//=========================================================================

@Composable
fun AttendanceGroupStatsScreen(
    repo: AttendanceRepository,
    branch: String,
    groupKey: String,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val isEnglish = rememberIsEnglish()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val screenTextAlign =
        if (isEnglish) {
            TextAlign.Start
        } else {
            TextAlign.Right
        }

    val screenTextDirection =
        if (isEnglish) {
            TextDirection.Ltr
        } else {
            TextDirection.Rtl
        }

    val screenTextStyle =
        TextStyle(
            textDirection = screenTextDirection
        )

    val reports by repo
        .reportsLastYear(branch, groupKey)
        .collectAsState(initial = emptyList())
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

    /*
     * כל כרטיסי החודשים מתחילים סגורים.
     *
     * רק לחיצה מפורשת על כרטיס החודש תפתח
     * את רשימת האימונים השייכת אליו.
     */
    val expandedByMonth =
        remember {
            mutableStateMapOf<YearMonth, Boolean>()
        }

    // פתיחה/סגירה של פירוט מתאמנים לכל דו"ח
    val expandedReportDetails =
        remember {
            mutableStateMapOf<Long, Boolean>()
        }

    LaunchedEffect(reportsByMonth) {
        reportsByMonth.forEach { (ym, _) ->
            expandedByMonth.putIfAbsent(
                ym,
                false
            )
        }
    }

    val hasRealReports = reports.isNotEmpty()

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

    @Composable
    fun AttendanceStatsTopTabs() {

        // סטטיסטיקה היא הטאב הפעיל במסך הזה
        val selectedIndex = 0

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            contentColor = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RectangleShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        brush = kmiSectionHeaderBrush()
                    )
            ) {

                // קו מפריד במרכז
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .height(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.65f)
                        )
                )

                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        divider = {},
                        indicator = { positions ->

                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(
                                        positions[selectedIndex]
                                    )
                                    .padding(horizontal = 58.dp)
                                    .offset(
                                        x = 19.dp,
                                        y = (-4).dp
                                    ),
                                height = 3.dp,
                                color = Color.White
                            )
                        },
                        modifier = Modifier.matchParentSize()
                    ) {

                        // שמאל — סטטיסטיקה
                        Tab(
                            selected = true,
                            onClick = {},
                            text = {
                                Text(
                                    text = tr(
                                        "סטטיסטיקה",
                                        "Statistics"
                                    ),
                                    style = KmiTypography.caption.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 38.dp)
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor =
                                Color.White.copy(alpha = 0.90f)
                        )

                        // ימין — בחירת אימון
                        Tab(
                            selected = false,
                            onClick = {
                                onBack()
                            },
                            text = {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Select\ntraining"
                                        } else {
                                            "בחירת\nאימון"
                                        },
                                    style = KmiTypography.caption.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 38.dp)
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor =
                                Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            KmiTopBar(
                title =
                    tr(
                        "סטטיסטיקת נוכחות",
                        "Attendance statistics"
                    ),
                onBack = onBack,

                /*
                 * אייקוני הבית והחיפוש מוצגים בשורת
                 * הפעולות התחתונה של KmiTopBar.
                 */
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                showTopShare = false,

                lockHome = false,
                lockSearch = false,

                centerTitle = true,

                onHome = onHome,

                onShare = {
                    shareAttendanceStatsPdf(
                        context = context,
                        branch = branch,
                        groupKey = groupKey,
                        avgPct = avgPct,
                        totalSessions = totalSessions,
                        avgPresent = avgPresent,
                        avgTotal = avgTotal,
                        reports = reports.map { report ->
                            AttendanceStatsPdfReport(
                                date = report.date.toString(),
                                total = report.totalMembers,
                                present = report.presentCount,
                                absent = report.absentCount,
                                pct = report.percentPresent
                            )
                        },
                        isEnglish = isEnglish
                    )
                }
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { p ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(p)
            ) {

                // פס הגראניט הגלובלי
                AttendanceStatsTopTabs()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                    contentPadding =
                        PaddingValues(bottom = 120.dp)
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
                            text =
                                if (deleteMode) {
                                    tr(
                                        "בחר דו\"חות למחיקה",
                                        "Select reports to delete"
                                    )
                                } else {
                                    tr(
                                        "דו\"חות אחרונים (שנה אחורה)",
                                        "Recent reports - last year"
                                    )
                                },
                            style = KmiTypography.cardTitle.merge(
                                screenTextStyle
                            ),
                            color =
                                MaterialTheme.colorScheme.onBackground,
                            textAlign = screenTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                        )
                    }

                    if (!hasRealReports) {
                        item {
                            EmptyAttendanceReportsCard(
                                branch = branch,
                                groupKey = groupKey,
                                isEnglish = isEnglish
                            )
                        }
                    }

                    reportsByMonth.forEach { (ym, monthReports) ->

                        // כותרת חודש (לחיצה => קיפול/פתיחה)
                        item(key = "month_${ym}") {
                            val monthTitle = remember(ym) {
                                ym.atDay(1).format(monthTitleFormatter)
                            }

                            /*
                             * חודש נחשב פתוח רק כאשר הערך שלו
                             * הוגדר במפורש כ-true.
                             */
                            val isExpanded =
                                expandedByMonth[ym] == true

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val current =
                                            expandedByMonth[ym] == true

                                        expandedByMonth[ym] =
                                            !current
                                    },
                                shape = RoundedCornerShape(18.dp),
                                color =
                                    MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 3.dp,
                                shadowElevation = 5.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.50f
                                        )
                                )
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
                                                imageVector =
                                                    if (isExpanded) {
                                                        Icons.Filled.ExpandLess
                                                    } else {
                                                        Icons.Filled.ExpandMore
                                                    },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(
                                                    KmiIconSize.medium
                                                )
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
                                                    style =
                                                        KmiTypography.cardTitle
                                                            .merge(
                                                                screenTextStyle
                                                            )
                                                            .copy(
                                                                fontWeight =
                                                                    FontWeight.Bold
                                                            ),
                                                    color =
                                                        MaterialTheme.colorScheme.onSurface,
                                                    textAlign =
                                                        if (isEnglish) {
                                                            TextAlign.Start
                                                        } else {
                                                            TextAlign.Right
                                                        },
                                                    modifier =
                                                        Modifier.fillMaxWidth(),
                                                    maxLines = 2,
                                                    overflow =
                                                        TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text =
                                                        if (isEnglish) {
                                                            "${monthReports.size} reports"
                                                        } else {
                                                            "${monthReports.size} דו\"חות"
                                                        },
                                                    style =
                                                        KmiTypography.caption.merge(
                                                            screenTextStyle
                                                        ),
                                                    color =
                                                        MaterialTheme.colorScheme.primary,
                                                    textAlign =
                                                        if (isEnglish) {
                                                            TextAlign.Start
                                                        } else {
                                                            TextAlign.Right
                                                        },
                                                    modifier =
                                                        Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        if (isEnglish) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        /*
                         * כל דיווחי החודש מוצגים בתוך משטח
                         * רציף אחד, עם קו מפריד בין האימונים.
                         */
                        val isExpandedNow =
                            expandedByMonth[ym] == true

                        if (isExpandedNow) {
                            item(
                                key = "month_reports_$ym"
                            ) {
                                Surface(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    shape =
                                        RoundedCornerShape(20.dp),
                                    color =
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 2.dp,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme.colorScheme.outline.copy(
                                                alpha = 0.45f
                                            )
                                    )
                                ) {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    ) {
                                        monthReports.forEachIndexed { reportIndex,
                                                                      report ->

                                            val checked =
                                                selected[report.id] == true

                                            val detailsExpanded =
                                                expandedReportDetails[
                                                    report.id
                                                ] == true

                                            /*
                                             * רקע מתחלף מדגיש את ההפרדה
                                             * בין הדוחות בלי ליצור כרטיס
                                             * נפרד סביב כל אימון.
                                             */
                                            val reportBackgroundColor =
                                                if (
                                                    reportIndex % 2 == 0
                                                ) {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .surface
                                                        .copy(alpha = 0.42f)
                                                } else {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primaryContainer
                                                        .copy(alpha = 0.34f)
                                                }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color =
                                                            reportBackgroundColor
                                                    )
                                                    .padding(
                                                        top = 4.dp,
                                                        bottom = 4.dp
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = 8.dp
                                                        ),
                                                    verticalAlignment =
                                                        Alignment.CenterVertically,
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                ) {
                                                    if (deleteMode) {
                                                        Checkbox(
                                                            checked =
                                                                checked,
                                                            onCheckedChange = { value ->

                                                                selected[
                                                                    report.id
                                                                ] = value
                                                            }
                                                        )
                                                    }

                                                    ReportRowCard(
                                                        dateText =
                                                            report.date
                                                                .toString(),
                                                        total =
                                                            report.totalMembers,
                                                        present =
                                                            report.presentCount,
                                                        absent =
                                                            report.absentCount,
                                                        pct =
                                                            report.percentPresent,
                                                        isEnglish =
                                                            isEnglish,
                                                        isDetailsExpanded =
                                                            detailsExpanded,
                                                        onToggleDetails = {
                                                            expandedReportDetails[
                                                                report.id
                                                            ] =
                                                                !detailsExpanded
                                                        },
                                                        modifier =
                                                            Modifier.weight(1f)
                                                    )
                                                }

                                                if (detailsExpanded) {
                                                    ReportAttendanceDetailsCard(
                                                        repo = repo,
                                                        branch = branch,
                                                        groupKey = groupKey,
                                                        date = report.date,
                                                        isEnglish = isEnglish
                                                    )
                                                }

                                                /*
                                                 * קו מפריד מוצג רק בין
                                                 * האימונים ולא אחרי האחרון.
                                                 */
                                                if (
                                                    reportIndex <
                                                    monthReports.lastIndex
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal =
                                                                    12.dp
                                                            )
                                                            .height(1.dp)
                                                            .background(
                                                                color =
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .primary
                                                                        .copy(
                                                                            alpha =
                                                                                0.42f
                                                                        )
                                                            )
                                                    )
                                                }
                                            }
                                        }
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
                                .heightIn(min = 52.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ "מחק דוחות" => מצב בחירה | במצב בחירה => "מחק נבחרים"
                            OutlinedButton(
                                onClick = {
                                    if (!hasRealReports) return@OutlinedButton

                                    if (!deleteMode) {
                                        deleteMode = true
                                        selected.clear()
                                    } else {
                                        if (selectedIds.isNotEmpty()) {
                                            confirmDeleteSelected = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor =
                                        MaterialTheme.colorScheme
                                            .surfaceVariant.copy(alpha = 0.55f),
                                    disabledContentColor =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant.copy(alpha = 0.55f)
                                ),
                                enabled = !busy && hasRealReports
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                )
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                val label = if (!deleteMode) {
                                    tr("מחק דוחות", "Delete reports")
                                } else {
                                    tr(
                                        "מחק נבחרים (${selectedIds.size})",
                                        "Delete selected (${selectedIds.size})"
                                    )
                                }

                                Text(
                                    text = label,
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = LocalContentColor.current,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme.colorScheme.primary
                                    ),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor =
                                                MaterialTheme.colorScheme
                                                    .onBackground
                                        ),
                                    enabled = !busy
                                ) {
                                    Text(
                                        text = tr("ביטול", "Cancel"),
                                        style = KmiTypography.action.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = LocalContentColor.current,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // ✅ איפוס הכל (כמו שעשית) – מוחק records/sessions/reports (לא מתאמנים)
                            Button(
                                onClick = {
                                    if (hasRealReports) {
                                        confirmResetAll = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.error,
                                        contentColor =
                                            MaterialTheme.colorScheme.onError,
                                        disabledContainerColor =
                                            MaterialTheme.colorScheme
                                                .surfaceVariant,
                                        disabledContentColor =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant.copy(
                                                    alpha = 0.55f
                                                )
                                    ),
                                enabled = !busy && hasRealReports
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                )

                                Spacer(
                                    Modifier.padding(horizontal = 4.dp)
                                )

                                Text(
                                    text = tr("איפוס", "Reset"),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                        onConfirm = {
                            confirmResetAll = false
                            busy = true
                            scope.launch {
                                runCatching {
                                    repo.resetAttendanceForGroup(branch, groupKey)
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
}

private data class AttendanceStatsPdfReport(
    val date: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val pct: Int
)

private fun shareAttendanceStatsPdf(
    context: Context,
    branch: String,
    groupKey: String,
    avgPct: Int,
    totalSessions: Int,
    avgPresent: Int,
    avgTotal: Int,
    reports: List<AttendanceStatsPdfReport>,
    isEnglish: Boolean
) {
    val pdfFile = createAttendanceStatsPdf(
        context = context,
        branch = branch,
        groupKey = groupKey,
        avgPct = avgPct,
        totalSessions = totalSessions,
        avgPresent = avgPresent,
        avgTotal = avgTotal,
        reports = reports,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) "KAMI attendance statistics" else "סטטיסטיקת נוכחות - KAMI"
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share PDF" else "שיתוף PDF"
        )
    )
}

private fun createAttendanceStatsPdf(
    context: Context,
    branch: String,
    groupKey: String,
    avgPct: Int,
    totalSessions: Int,
    avgPresent: Int,
    avgTotal: Int,
    reports: List<AttendanceStatsPdfReport>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val document = PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align = Paint.Align.RIGHT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)
    val smallPaint = paint(9f, textMuted, regular)

    fun drawRoundRect(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false,
        strokeWidth: Float = 1.2f
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            this.strokeWidth = strokeWidth
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, p)
    }

    fun drawHeader(
        canvas: android.graphics.Canvas
    ) {
        canvas.drawColor(
            android.graphics.Color.WHITE
        )

        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "סטטיסטיקת נוכחות",
            titleEnglish = "Attendance Statistics",
            subtitleHebrew =
                "$branch · $groupKey",
            subtitleEnglish =
                "$branch · $groupKey"
        )
    }

    fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNumber: Int,
        totalPages: Int
    ) {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = totalPages,
            isEnglish = isEnglish
        )
    }

    fun drawSummary(canvas: android.graphics.Canvas, top: Float): Float {
        drawRoundRect(canvas, margin, top, pageWidth - margin, top + 122f, lightBlue, 12f)
        drawRoundRect(
            canvas,
            margin,
            top,
            pageWidth - margin,
            top + 122f,
            borderBlue,
            12f,
            stroke = true
        )

        sectionPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$branch · $groupKey", pageWidth - margin - 22f, top + 30f, sectionPaint)

        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("ממוצע נוכחות שנה:", "Year attendance average:"),
            pageWidth - margin - 22f,
            top + 58f,
            labelPaint
        )

        boldValuePaint.textAlign = Paint.Align.LEFT
        boldValuePaint.textSize = 25f
        boldValuePaint.color = navy
        canvas.drawText("$avgPct%", margin + 28f, top + 58f, boldValuePaint)

        boldValuePaint.textSize = 13f
        boldValuePaint.color = textDark
        boldValuePaint.textAlign = Paint.Align.CENTER

        val boxTop = top + 74f
        val boxW = (pageWidth - margin * 2f - 30f) / 4f

        val stats = listOf(
            totalSessions.toString() to tr("שיעורים", "Sessions"),
            avgPresent.toString() to tr("ממוצע הגיעו", "Avg. present"),
            avgTotal.toString() to tr("ממוצע סה״כ", "Avg. total"),
            "$avgPct%" to tr("ממוצע נוכחות", "Avg. attendance")
        )

        stats.forEachIndexed { index, pair ->
            val left = margin + 15f + index * boxW
            val right = left + boxW - 8f

            drawRoundRect(canvas, left, boxTop, right, boxTop + 34f, softBlue, 10f)
            drawRoundRect(canvas, left, boxTop, right, boxTop + 34f, borderBlue, 10f, stroke = true)

            boldValuePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(pair.first, (left + right) / 2f, boxTop + 14f, boldValuePaint)

            smallPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(pair.second.take(16), (left + right) / 2f, boxTop + 28f, smallPaint)
        }

        return top + 146f
    }

    fun drawReportCard(
        canvas: android.graphics.Canvas,
        report: AttendanceStatsPdfReport,
        top: Float,
        index: Int
    ): Float {
        val right =
            pageWidth - margin

        val bottom =
            top + 82f

        val mid =
            pageWidth / 2f

        drawRoundRect(
            canvas,
            margin,
            top,
            right,
            bottom,
            if (index % 2 == 0) {
                lightBlue
            } else {
                softBlue
            },
            12f
        )

        drawRoundRect(
            canvas,
            margin,
            top,
            right,
            bottom,
            borderBlue,
            12f,
            stroke = true
        )

        canvas.drawLine(mid, top + 20f, mid, bottom - 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderBlue
            strokeWidth = 1f
        })

        sectionPaint.textAlign = Paint.Align.RIGHT
        sectionPaint.textSize = 13.5f
        canvas.drawText(report.date, right - 22f, top + 28f, sectionPaint)

        boldValuePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("נוכחות: ${report.pct}%", "Attendance: ${report.pct}%"),
            right - 22f,
            top + 52f,
            boldValuePaint
        )

        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr(
                "הגיעו ${report.present} · נעדרו ${report.absent}",
                "Present ${report.present} · Absent ${report.absent}"
            ),
            right - 22f,
            top + 70f,
            labelPaint
        )

        valuePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(
            tr(
                "סה״כ מתאמנים: ${report.total}",
                "Total trainees: ${report.total}"
            ),
            mid - 22f,
            top + 48f,
            valuePaint
        )

        return bottom + 8f
    }

    val firstPageCapacity = 5
    val nextPageCapacity = 6

    val totalPages = if (reports.size <= firstPageCapacity) {
        1
    } else {
        1 + kotlin.math.ceil((reports.size - firstPageCapacity) / nextPageCapacity.toDouble())
            .toInt()
    }

    var pageNumber = 1
    var reportIndex = 0

    while (pageNumber <= totalPages) {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        val canvas = page.canvas

        drawHeader(canvas)

        var y =
            KmiPdfHeader.CONTENT_TOP

        if (pageNumber == 1) {
            y = drawSummary(canvas, y)
        } else {
            sectionPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                tr("דו״חות נוכחות", "Attendance reports"),
                pageWidth / 2f,
                y,
                sectionPaint
            )
            y += 28f
        }

        val capacity = if (pageNumber == 1) firstPageCapacity else nextPageCapacity

        if (reports.isEmpty()) {
            drawRoundRect(canvas, margin, y, pageWidth - margin, y + 92f, softBlue, 12f)
            drawRoundRect(
                canvas,
                margin,
                y,
                pageWidth - margin,
                y + 92f,
                borderBlue,
                12f,
                stroke = true
            )

            sectionPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                tr("אין דוחות נוכחות שמורים", "No saved attendance reports"),
                pageWidth / 2f,
                y + 42f,
                sectionPaint
            )
        } else {
            repeat(capacity) {
                if (reportIndex >= reports.size) return@repeat

                val nextCardBottom =
                    y + 82f

                if (
                    nextCardBottom >
                    pageHeight -
                    KmiPdfFooter
                        .CONTENT_BOTTOM_PADDING
                ) {
                    return@repeat
                }

                y = drawReportCard(
                    canvas = canvas,
                    report = reports[reportIndex],
                    top = y,
                    index = reportIndex
                )

                reportIndex++
            }
        }

        drawFooter(canvas, pageNumber, totalPages)
        document.finishPage(page)

        pageNumber++
    }

    val dir =
        File(
            context.cacheDir,
            "pdfs"
        ).apply {
            mkdirs()
        }

    val reportDate =
        SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(
            Date()
        )

    val reportFileName =
        if (isEnglish) {
            "Attendance_Report_$reportDate.pdf"
        } else {
            "דוח_נוכחות_$reportDate.pdf"
        }

    val file =
        File(
            dir,
            reportFileName
        )

    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

@Composable
private fun EmptyAttendanceReportsCard(
    branch: String,
    groupKey: String,
    isEnglish: Boolean
) {

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    )
    val layoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 5.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.50f
                )
        )
    ) {

        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme
                                        .primaryContainer.copy(
                                            alpha = 0.42f
                                        )
                                )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(KmiIconSize.large)
                        .padding(
                            end = if (isEnglish) 4.dp else 0.dp
                        )
                )

                if (!isEnglish) {
                    Spacer(Modifier.padding(horizontal = 6.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = horizontalAlignment,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(
                            "אין עדיין דוחות נוכחות שמורים",
                            "No saved attendance reports yet"
                        ),
                        style = KmiTypography.cardTitle
                            .merge(textStyle)
                            .copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "המסך מחובר לשרת. לאחר שמירת דוח ממסך הנוכחות, הוא יופיע כאן לפי חודש עם פירוט מלא.",
                            "This screen is connected to the server. Once an attendance report is saved, it will appear here by month with full details."
                        ),
                        style = KmiTypography.secondary.merge(
                            textStyle
                        ),
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "סניף: ${branch.ifBlank { "—" }} · קבוצה: ${groupKey.ifBlank { "—" }}",
                            "Branch: ${branch.ifBlank { "—" }} · Group: ${groupKey.ifBlank { "—" }}"
                        ),
                        style = KmiTypography.caption
                            .merge(textStyle)
                            .copy(
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
                        color =
                            MaterialTheme.colorScheme.primary,
                        textAlign = align,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
        textDirection =
            if (isEnglish) {
                TextDirection.Ltr
            } else {
                TextDirection.Rtl
            }
    )

    val heroTitleColor =
        MaterialTheme.colorScheme.onSurface

    val heroSecondaryColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.96f
            ),
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.50f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
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
                                style = KmiTypography.body
                                    .merge(textStyle)
                                    .copy(
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                color = heroTitleColor,
                                textAlign = align,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = tr(
                                    "שיעורים עם דו\"ח: $totalSessions",
                                    "Reported sessions: $totalSessions"
                                ),
                                style = KmiTypography.secondary.merge(
                                    textStyle
                                ),
                                color = heroSecondaryColor,
                                textAlign = align,
                                maxLines = 2,
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
                text = tr(
                    "ממוצע נוכחות שנה: $avgPct%",
                    "Year attendance average: $avgPct%"
                ),
                style = KmiTypography.sectionTitle
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color =
                    MaterialTheme.colorScheme.primary,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsGlowIcon() {
    Box(
        modifier = Modifier
            .size(KmiIconSize.hero)
            .background(
                brush = Brush.radialGradient(
                    colors =
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint =
                MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(
                KmiIconSize.large
            )
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
        textDirection =
            if (isEnglish) {
                TextDirection.Ltr
            } else {
                TextDirection.Rtl
            }
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.50f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = tr(
                    "סיכום שנה אחורה",
                    "Last year summary"
                ),
                style = KmiTypography.sectionTitle
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color =
                    MaterialTheme.colorScheme.onSurface,
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

    /*
     * אין מסגרת נפרדת לכל אימון.
     *
     * המסגרת החיצונית עוטפת כעת את כל
     * רשימת האימונים של אותו חודש.
     */
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        val datePretty = remember(dateText, isEnglish) {
            runCatching {
                val d =
                    LocalDate.parse(dateText)
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
                style = KmiTypography.sectionTitle
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign = align,
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
                    label = tr("נעדרו", "Absent"),
                    value = absent.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = tr(
                    "נוכחות: $pct%",
                    "Attendance: $pct%"
                ),
                style = KmiTypography.body
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color =
                    MaterialTheme.colorScheme.primary,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onToggleDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.primary
                ),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.onSurface
                    )
            ) {
                Icon(
                    imageVector =
                        if (isDetailsExpanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                    contentDescription = null,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    )
                )

                Spacer(Modifier.padding(horizontal = 4.dp))

                Text(
                    text =
                        if (isDetailsExpanded) {
                            tr(
                                "סגור רשימת נוכחות",
                                "Hide attendance list"
                            )
                        } else {
                            tr(
                                "פתח רשימת נוכחות",
                                "Show attendance list"
                            )
                        },
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = LocalContentColor.current,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
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
    date: LocalDate,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val textStyle = TextStyle(
        textDirection =
            if (isEnglish) {
                TextDirection.Ltr
            } else {
                TextDirection.Rtl
            }
    )

    val members by repo
        .members(branch, groupKey)
        .collectAsState(initial = emptyList())
    val records by repo.attendanceForDay(branch, groupKey, date)
        .collectAsState(initial = emptyList())

    fun String.detailsNameKey(): String = this
        .trim()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""[."'\u05F3\u05F4,;:()\[\]{}]"""), "")
        .lowercase()

    fun String.isDemoOrPlaceholderDetailsName(): Boolean {
        val key = detailsNameKey()
        return key.isBlank() ||
                key == "מתאמן" ||
                key.startsWith("מתאמן ") ||
                key.startsWith("מתאמן_") ||
                key == "demo" ||
                key.startsWith("demo ") ||
                key == "trainee" ||
                key.startsWith("trainee ")
    }

    val realMembers = remember(members) {
        members
            .filterNot { it.displayName.isDemoOrPlaceholderDetailsName() }
            .distinctBy { it.displayName.detailsNameKey() }
    }

    val demoIndexByMemberId =
        remember(realMembers) {
            realMembers
                .mapIndexed { index, member ->
                    member.id to index
                }
                .toMap()
        }

    val statusByMemberId = remember(records) {
        records.associate { it.memberId to it.status }
    }

    val presentMembers = remember(
        realMembers,
        statusByMemberId
    ) {
        realMembers.filter { member ->
            statusByMemberId[member.id] ==
                    AttendanceStatus.PRESENT
        }
    }

    val absentMembers = remember(
        realMembers,
        statusByMemberId
    ) {
        realMembers.filter { member ->
            val status = statusByMemberId[member.id]

            status == AttendanceStatus.ABSENT ||
                    status == AttendanceStatus.EXCUSED ||
                    status == null
        }
    }

    fun demoSafeName(
        member: il.kmi.app.attendance.data.GroupMember
    ): String {
        return TraineeDisplayNameMapper.displayName(
            realName = member.displayName,
            stableKey = member.id.toString(),
            demoIndex =
                demoIndexByMemberId[
                    member.id
                ],
            isEnglish = isEnglish
        ).ifBlank {
            tr(
                "מתאמן ללא שם",
                "Unnamed trainee"
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color =
            MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.50f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = tr(
                    "פירוט נוכחות בדו״ח",
                    "Attendance details"
                ),
                style = KmiTypography.cardTitle
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            AttendanceStatusSection(
                title = tr("הגיעו", "Present"),
                names = presentMembers.map(::demoSafeName),
                emptyText = tr(
                    "אין מתאמנים שסומנו הגיעו",
                    "No trainees marked present"
                ),
                color =
                    MaterialTheme.colorScheme.tertiary,
                isEnglish = isEnglish
            )

            AttendanceStatusSection(
                title = tr("לא הגיעו", "Absent"),
                names = absentMembers.map(::demoSafeName),
                emptyText = tr(
                    "אין מתאמנים שלא הגיעו",
                    "No absent trainees"
                ),
                color =
                    MaterialTheme.colorScheme.error,
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
        textDirection =
            if (isEnglish) {
                TextDirection.Ltr
            } else {
                TextDirection.Rtl
            }
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.45f
                )
        )
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
                style = KmiTypography.cardTitle
                    .merge(textStyle)
                    .copy(
                        fontWeight = FontWeight.Black
                    ),
                color = color,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            if (names.isEmpty()) {
                Text(
                    text = emptyText,
                    style = KmiTypography.secondary.merge(
                        textStyle
                    ),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                names.forEach { name ->
                    Text(
                        text = "• $name",
                        style = KmiTypography.body.merge(
                            textStyle
                        ),
                        color =
                            MaterialTheme.colorScheme.onSurface,
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
        color =
            MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.80f
            ),
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.45f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = KmiTypography.metric,
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = label,
                style = KmiTypography.caption,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
        color =
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.45f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 6.dp,
                    vertical = 8.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.Black
                ),
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = label,
                style = KmiTypography.caption,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = KmiTypography.screenTitle.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Text(
                text = text,
                style = KmiTypography.body,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    text = confirmText,
                    style = KmiTypography.action,
                    color =
                        MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    text = dismissText,
                    style = KmiTypography.action,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}