package il.kmi.app.attendance

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.attendance.data.AttendanceReport
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.flow.firstOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import il.kmi.app.localization.rememberIsEnglish

@Composable
fun AttendanceStatsScreen(
    branch: String,
    groupKey: String,
    memberName: String?,
    memberId: Long?,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repo = remember(app) { AttendanceRepository.get(app) }
    val scope = rememberCoroutineScope()

    val isEnglish = rememberIsEnglish()
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    var monthlyPercent by remember { mutableStateOf(0) }
    var yearlyPercent by remember { mutableStateOf(0) }
    var streakDays by remember { mutableStateOf(0) }
    var bestDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSessions by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasRealAttendanceData by remember { mutableStateOf(false) }

    // קארוסל דו"חות שמורים
    var reports by remember { mutableStateOf<List<AttendanceReport>>(emptyList()) }

    // תאריך האימון הנוכחי (היום)
    val today = remember { LocalDate.now() }

    // חישוב נתונים אמיתיים מה־DB (סטטיסטיקה דינמית לפי נוכחות)
    LaunchedEffect(branch, groupKey, memberId) {
        monthlyPercent = 0
        yearlyPercent = 0
        streakDays = 0
        bestDays = emptyList()
        lastSessions = emptyList()
        hasRealAttendanceData = false

        if (branch.isBlank() || groupKey.isBlank()) return@LaunchedEffect

        val monthStart = today.withDayOfMonth(1)
        val yearBack = today.minusYears(1)

        var monthPresent = 0
        var monthTotal = 0
        var yearPresent = 0
        var yearTotal = 0

        var currentStreak = 0
        var streakOngoing = true

        val tmpLastSessions = mutableListOf<String>()
        val dayCounts = mutableMapOf<DayOfWeek, Int>()

        var d = today
        // נסרוק עד שנה אחורה או עד שיש לנו מספיק מידע
        while (d.isAfter(yearBack) || d.isEqual(yearBack)) {
            val records = repo.attendanceForDay(branch, groupKey, d).firstOrNull().orEmpty()

            // מקור אמת: AttendanceRecord typed fields, בלי reflection
            val relevant = if (memberId != null) {
                records.filter { it.memberId == memberId }
            } else {
                records
            }

            if (relevant.isNotEmpty()) {
                hasRealAttendanceData = true

                val statuses = relevant.map { it.status }
                val presentToday = statuses.count { it == AttendanceStatus.PRESENT }
                val totalToday = statuses.size

                if (d >= monthStart) {
                    monthPresent += presentToday
                    monthTotal += totalToday
                }
                yearPresent += presentToday
                yearTotal += totalToday

                // רצף: נספר מהיום הנוכחי אחורה עד אימון ראשון שלא הוגדר "הגיע"
                if (streakOngoing) {
                    if (presentToday > 0) {
                        currentStreak++
                    } else {
                        streakOngoing = false
                    }
                }

                // ימים חזקים – לפי ימי השבוע שבהם הגיע
                if (presentToday > 0) {
                    val dow = d.dayOfWeek
                    dayCounts[dow] = (dayCounts[dow] ?: 0) + presentToday
                }

                // 5 אימונים אחרונים
                if (tmpLastSessions.size < 5) {
                    val statusLabel = when {
                        statuses.all { it == AttendanceStatus.PRESENT } -> tr("הגיע", "Present")
                        statuses.any { it == AttendanceStatus.PRESENT } &&
                                statuses.any { it == AttendanceStatus.EXCUSED } -> tr("הגיע חלקית / מוצדק", "Partial / excused")
                        statuses.any { it == AttendanceStatus.EXCUSED } -> tr("מוצדק", "Excused")
                        else -> tr("לא הגיע", "Absent")
                    }

                    val dateText = if (isEnglish) {
                        d.toString()
                    } else {
                        formatDateHeb(d)
                    }

                    tmpLastSessions.add("$dateText · $statusLabel")
                }

            } else {
                // אין רשומה לאותו יום – שובר רצף
                if (streakOngoing) {
                    streakOngoing = false
                }
            }

            d = d.minusDays(1)
        }

        monthlyPercent = if (monthTotal > 0) ((monthPresent * 100.0) / monthTotal).toInt() else 0
        yearlyPercent = if (yearTotal > 0) ((yearPresent * 100.0) / yearTotal).toInt() else 0
        streakDays = currentStreak
        bestDays = dayCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { hebDay(it.key) }

        lastSessions = tmpLastSessions
    }

    // טעינת 5 הדו"חות האחרונים מה־DB
    LaunchedEffect(branch, groupKey) {
        if (branch.isBlank() || groupKey.isBlank()) return@LaunchedEffect
        repo.lastReports(branch, groupKey, limit = 5).collect { reports = it }
    }

    val name = memberName?.trim()?.takeIf { it.isNotBlank() }
        ?: tr("מתאמן לא נבחר", "No trainee selected")

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr("סטטיסטיקת נוכחות", "Attendance statistics"),
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true, // סרגל האייקונים למטה
                lockSearch = true,
                centerTitle = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        // רקע מודרני – גרדיאנט עמוק
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // ───── Hero Card – סיכום עליון ─────
                HeroAttendanceHeader(
                    name = name,
                    branch = branch,
                    groupKey = groupKey,
                    today = today,
                    monthlyPercent = monthlyPercent,
                    yearlyPercent = yearlyPercent,
                    isEnglish = isEnglish
                )

                // ───── כרטיסי אחוזים (חודש / שנה) ─────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AttendanceMetricCard(
                        title = tr("נוכחות חודשית", "Monthly attendance"),
                        percent = monthlyPercent,
                        gradient = Brush.verticalGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricCard(
                        title = tr("נוכחות שנתית", "Yearly attendance"),
                        percent = yearlyPercent,
                        gradient = Brush.verticalGradient(
                            listOf(Color(0xFF22C55E), Color(0xFF14B8A6))
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!hasRealAttendanceData) {
                    EmptyMemberAttendanceStatsCard(
                        branch = branch,
                        groupKey = groupKey,
                        memberName = name,
                        isEnglish = isEnglish
                    )
                }

                // ───── רצף נוכחות ─────
                Surface(
                    color = Color.White.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF6366F1)
                            )
                            Text(
                                "רצף נוכחות",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Text(
                            "$streakDays אימונים ברצף 👏",
                            color = Color(0xFF4B5563),
                            fontSize = 14.sp
                        )

                        LinearProgressIndicator(
                            progress = (streakDays / 10f).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF6366F1),
                            trackColor = Color(0xFFE5E7EB)
                        )

                        Text(
                            text = "יעד חודשי: 10 אימונים",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6366F1)
                        )
                    }
                }

                // ───── ימים חזקים ─────
                Surface(
                    color = Color.White.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = Color(0xFF0EA5E9)
                            )
                            Text(
                                "ימים חזקים",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            "הימים שבהם $name מגיע הכי הרבה",
                            color = Color(0xFF4B5563),
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("א", "ב", "ג", "ד", "ה", "ו").forEach { day ->
                                val selected = day in bestDays
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (selected)
                                                Color(0xFF4F46E5).copy(alpha = 0.14f)
                                            else
                                                Color(0xFFF3F4F6)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day,
                                        color = if (selected) Color(0xFF4F46E5) else Color(0xFF4B5563),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // ───── 5 אימונים אחרונים (דינמי לפי רשומות נוכחות) ─────
                Surface(
                    color = Color.White.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFF97316)
                            )
                            Text(
                                "5 אימונים אחרונים",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        lastSessions.forEach { row ->
                            val wasPresent = row.contains("הגיע")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (wasPresent) Color(0xFF22C55E)
                                            else Color(0xFFEF4444)
                                        )
                                )
                                Text(
                                    row,
                                    color = Color(0xFF374151),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        if (lastSessions.isEmpty()) {
                            Text(
                                "אין נתוני נוכחות מוצגים עדיין.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // ───── קארוסל דו"חות אחרונים ─────
                if (reports.isNotEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "דו\"חות נוכחות אחרונים",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                reports.forEach { report ->
                                    AttendanceReportChip(report = report)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private fun rtlLine(s: String): String = "\u200F" + s + "\u200F"

/* ───── Hero: כרטיס עליון גדול לסיכום ───── */

@Composable
private fun EmptyMemberAttendanceStatsCard(
    branch: String,
    groupKey: String,
    memberName: String,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Left else TextAlign.Right
    val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    val direction = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color(0xFF1D4ED8).copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = tr(
                        "אין עדיין נתוני נוכחות למתאמן",
                        "No attendance data for this trainee yet"
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall.merge(
                        TextStyle(textDirection = direction)
                    ),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr(
                        "המסך מחובר לשרת. לאחר סימון ושמירת נוכחות במסך הנוכחות, הנתונים של $memberName יופיעו כאן.",
                        "This screen is connected to the server. After attendance is marked and saved, $memberName's data will appear here."
                    ),
                    color = Color(0xFFBFDBFE),
                    style = MaterialTheme.typography.bodySmall.merge(
                        TextStyle(textDirection = direction)
                    ),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr(
                        "סניף: ${branch.ifBlank { "—" }} · קבוצה: ${groupKey.ifBlank { "—" }}",
                        "Branch: ${branch.ifBlank { "—" }} · Group: ${groupKey.ifBlank { "—" }}"
                    ),
                    color = Color(0xFFE0F2FE),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall.merge(
                        TextStyle(textDirection = direction)
                    ),
                    textAlign = align,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeroAttendanceHeader(
    name: String,
    branch: String,
    groupKey: String,
    today: LocalDate,
    monthlyPercent: Int,
    yearlyPercent: Int,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        // ✅ משאירים פריסה LTR כדי ש-End יהיה ימין פיזית
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

            val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // שורת כותרת: טקסט בימין + אווטאר מימין
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.merge(rtlStyle),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Right,     // ✅ ימין פיזית
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $today"
                            } else {
                                "יום ${hebDay(today.dayOfWeek)}, ${formatDateHeb(today)}"
                            },
                            style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                            color = Color(0xFFE5E7EB),
                            textAlign = TextAlign.Right,     // ✅
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF312E81))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // אחוזים – מיושר לימין פיזית
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("חודשי: $monthlyPercent%", "Monthly: $monthlyPercent%"),
                        style = MaterialTheme.typography.labelLarge.merge(rtlStyle),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE),
                        textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = tr("שנתי: $yearlyPercent%", "Yearly: $yearlyPercent%"),
                        style = MaterialTheme.typography.labelLarge.merge(rtlStyle),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Right
                    )
                }

                @Composable
                fun InfoRow(label: String, lines: List<String>) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFBFDBFE),
                            style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        lines.filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = line,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.merge(rtlStyle),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val branchLines = remember(branch) {
                    branch
                        .replace(" • ", "\n")
                        .split('\n', ',', ';', '；')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                val groupLines = remember(groupKey) {
                    groupKey
                        .replace(" • ", "\n")
                        .split('\n', ',', ';', '；')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                InfoRow(tr("סניף", "Branch"), branchLines)
                InfoRow(tr("קבוצה", "Group"), groupLines)
            }
        }
    }
}

/** כרטיס אחוז יפה עם מעגל וצבעים (Next-Gen) */
@Composable
private fun AttendanceMetricCard(
    title: String,
    percent: Int,
    gradient: Brush,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ✅ גובה קבוע לכותרת כדי ששני הכרטיסים יהיו באותו גודל
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(brush = gradient, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "$percent%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF111827)
                        )
                    }
                }
            }

            Text(
                text = when {
                    percent >= 85 -> tr("מצוין 💜", "Excellent 💜")
                    percent >= 70 -> tr("טוב מאוד", "Very good")
                    else -> tr("אפשר לשפר", "Can improve")
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ───── Chip של דו"ח ב"קארוסל" ───── */

@Composable
private fun AttendanceReportChip(
    report: AttendanceReport,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 180.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF1D4ED8))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDateHeb(report.date),
                color = Color(0xFFBFDBFE),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "נוכחות: ${report.percentPresent}%",
                color = Color(0xFF22C55E),
                fontSize = 13.sp
            )
            Text(
                text = "סה\"כ ${report.totalMembers} • הגיעו ${report.presentCount} • מוצדקים ${report.excusedCount}",
                color = Color(0xFFE5E7EB),
                fontSize = 11.sp
            )
        }
    }
}

/* ===== עזר לפורמט תאריך ויום בשבוע ===== */

private fun hebDay(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.SUNDAY -> "א"
    DayOfWeek.MONDAY -> "ב"
    DayOfWeek.TUESDAY -> "ג"
    DayOfWeek.WEDNESDAY -> "ד"
    DayOfWeek.THURSDAY -> "ה"
    DayOfWeek.FRIDAY -> "ו"
    DayOfWeek.SATURDAY -> "ש"
}

private fun formatDateHeb(date: LocalDate): String {
    return "${date.dayOfMonth}.${date.monthValue}.${date.year}"
}
