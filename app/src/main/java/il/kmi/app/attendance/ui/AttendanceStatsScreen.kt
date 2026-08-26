package il.kmi.app.attendance

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
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
import il.kmi.app.attendance.data.AttendanceReport
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.MemberAttendanceSession
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.KmiTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import il.kmi.app.localization.rememberIsEnglish
import il.kmi.app.ui.KmiTypography
import java.time.YearMonth

/**
 * נתון חודשי אחד בגרף הנוכחות.
 *
 * attendedDates מכיל את התאריכים הייחודיים שבהם
 * המתאמן סומן כנוכח לפחות פעם אחת.
 *
 * attendedTrainings הוא מספר ימי הנוכחות בחודש.
 */
private data class MonthlyAttendancePoint(
    val yearMonth: YearMonth,
    val attendedTrainings: Int,
    val attendedDates: List<LocalDate>
)

@Composable
fun AttendanceStatsScreen(
    branch: String,
    groupKey: String,
    memberName: String?,
    memberId: Long?,
    onBack: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repo = remember(app) {
        AttendanceRepository.get(app)
    }

    val isEnglish = rememberIsEnglish()
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    var monthlyPercent by remember {
        mutableStateOf(0)
    }

    var yearlyPercent by remember {
        mutableStateOf(0)
    }

    /*
     * נתוני המונה והמכנה נשמרים גם לצורך
     * הפקת דו״ח ה־PDF.
     */
    var monthlyPresentCount by remember {
        mutableStateOf(0)
    }

    var monthlyScheduledCount by remember {
        mutableStateOf(0)
    }

    var yearlyPresentCount by remember {
        mutableStateOf(0)
    }

    var yearlyScheduledCount by remember {
        mutableStateOf(0)
    }

    /*
     * כל עוד הנתונים מחושבים מהמסד, לא מציגים
     * אחוזי 0 או כרטיסים המבוססים על מידע חלקי.
     */
    var isAttendanceLoading by remember(
        branch,
        groupKey,
        memberId
    ) {
        mutableStateOf(true)
    }

    /*
     * מכיל רק חודשים שבהם הייתה לפחות נוכחות אחת.
     * חודשים ריקים אינם נכנסים לרשימה ולכן אינם
     * תופסים מקום בגרף.
     */
    var monthlyAttendance by remember {
        mutableStateOf<List<MonthlyAttendancePoint>>(
            emptyList()
        )
    }

    var lastSessions by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasRealAttendanceData by remember { mutableStateOf(false) }

    // קארוסל דו"חות שמורים
    var reports by remember { mutableStateOf<List<AttendanceReport>>(emptyList()) }

    // תאריך האימון הנוכחי (היום)
    val today = remember { LocalDate.now() }

    /*
     * חישוב נתוני המתאמן לפי לוח האימונים השבועי
     * ומסמכי הנוכחות האישיים.
     *
     * המכנה כולל את כל האימונים המתוכננים שכבר
     * התקיימו ממועד תחילת המתאמן ועד היום.
     */
    LaunchedEffect(
        branch,
        groupKey,
        memberId,
        isEnglish
    ) {
        isAttendanceLoading = true

        monthlyPercent = 0
        yearlyPercent = 0

        monthlyPresentCount = 0
        monthlyScheduledCount = 0
        yearlyPresentCount = 0
        yearlyScheduledCount = 0

        monthlyAttendance = emptyList()
        lastSessions = emptyList()
        hasRealAttendanceData = false

        if (
            branch.isBlank() ||
            groupKey.isBlank() ||
            memberId == null ||
            memberId <= 0L
        ) {
            isAttendanceLoading = false
            return@LaunchedEffect
        }

        try {
            val currentMonth =
                YearMonth.from(today)

            val firstDisplayedMonth =
                currentMonth.minusMonths(11)

            val requestedFrom =
                firstDisplayedMonth.atDay(1)

            val history =
                repo.memberAttendanceHistory(
                    branch = branch,
                    groupKey = groupKey,
                    memberId = memberId,
                    requestedFrom = requestedFrom,
                    to = today
                )

            /*
             * קוראים את ימי האימון השבועיים של הקבוצה
             * ממקור האמת הגלובלי של לוח האימונים.
             */
            val exactGroupTrainings =
                TrainingCatalog.trainingsFor(
                    branch = branch,
                    group = groupKey,
                    isEnglish = false
                )

            /*
             * אם שם הקבוצה שנשמר בנוכחות אינו תואם
             * בדיוק לשם בקטלוג, מבצעים חיפוש גיבוי
             * בכל קבוצות הסניף.
             *
             * בסניף אופק כל הקבוצות הרלוונטיות
             * מתאמנות בימי שני וחמישי.
             */
            val catalogTrainings =
                exactGroupTrainings.ifEmpty {
                    TrainingCatalog.trainingsFor(
                        branch = branch,
                        group = null,
                        isEnglish = false
                    )
                }

            val scheduledTrainingDays =
                catalogTrainings
                    .mapNotNull { training ->
                        when (
                            training.cal.get(
                                Calendar.DAY_OF_WEEK
                            )
                        ) {
                            Calendar.SUNDAY ->
                                DayOfWeek.SUNDAY

                            Calendar.MONDAY ->
                                DayOfWeek.MONDAY

                            Calendar.TUESDAY ->
                                DayOfWeek.TUESDAY

                            Calendar.WEDNESDAY ->
                                DayOfWeek.WEDNESDAY

                            Calendar.THURSDAY ->
                                DayOfWeek.THURSDAY

                            Calendar.FRIDAY ->
                                DayOfWeek.FRIDAY

                            Calendar.SATURDAY ->
                                DayOfWeek.SATURDAY

                            else -> null
                        }
                    }
                    .toSet()

            /*
      * רשומות הנוכחות הן מקור האמת למונה.
      * מועדי הלו״ז השבועי הם מקור האמת למכנה.
      */
            val currentMonthStart =
                today.withDayOfMonth(1)

            /*
             * createdAtMillis עשוי להיות מועד הכנסת
             * המתאמן למסד ולא מועד תחילת האימונים.
             *
             * memberStartDate שמוחזר מה־Repository כבר
             * מתחשב גם ברשומת הנוכחות ההיסטורית הראשונה.
             */
            val scheduleCalculationStart =
                if (
                    history.memberStartDate.isAfter(
                        currentMonthStart
                    ) &&
                    history.sessions.any { session ->
                        YearMonth.from(session.date) ==
                                YearMonth.from(today)
                    }
                ) {
                    currentMonthStart
                } else {
                    history.memberStartDate
                }

            /*
             * כל מועדי האימון המתוכננים שכבר התקיימו.
             * תאריכים עתידיים אינם נכנסים למכנה.
             */
            val scheduledDates =
                if (scheduledTrainingDays.isEmpty()) {
                    emptyList()
                } else {
                    generateSequence(
                        scheduleCalculationStart
                    ) { date ->
                        date.plusDays(1)
                    }
                        .takeWhile { date ->
                            !date.isAfter(today)
                        }
                        .filter { date ->
                            date.dayOfWeek in
                                    scheduledTrainingDays
                        }
                        .distinct()
                        .toList()
                }

            val monthlyScheduledDates =
                scheduledDates.filter { date ->
                    !date.isBefore(
                        currentMonthStart
                    ) &&
                            !date.isAfter(today)
                }

            /*
             * סופרים כל סימון PRESENT פעם אחת בלבד
             * בכל תאריך.
             *
             * גם אם הסימון נשמר ביום פתיחת המסך ולא
             * בדיוק בתאריך הלו״ז, הוא נשאר במונה אך
             * אינו יוצר אימון נוסף במכנה.
             */
            val presentDates =
                history.sessions
                    .asSequence()
                    .filter { session ->
                        session.status ==
                                AttendanceStatus.PRESENT
                    }
                    .map { session ->
                        session.date
                    }
                    .distinct()
                    .toList()

            val monthlyPresent =
                presentDates.count { date ->
                    !date.isBefore(
                        currentMonthStart
                    ) &&
                            !date.isAfter(today)
                }

            val yearlyPresent =
                presentDates.count { date ->
                    !date.isBefore(
                        scheduleCalculationStart
                    ) &&
                            !date.isAfter(today)
                }

            monthlyPresentCount =
                monthlyPresent

            monthlyScheduledCount =
                monthlyScheduledDates.size

            yearlyPresentCount =
                yearlyPresent

            yearlyScheduledCount =
                scheduledDates.size

            monthlyPercent =
                if (
                    monthlyScheduledDates.isNotEmpty()
                ) {
                    (
                            monthlyPresent *
                                    100.0 /
                                    monthlyScheduledDates.size
                            )
                        .toInt()
                        .coerceIn(
                            minimumValue = 0,
                            maximumValue = 100
                        )
                } else {
                    0
                }

            yearlyPercent =
                if (scheduledDates.isNotEmpty()) {
                    (
                            yearlyPresent *
                                    100.0 /
                                    scheduledDates.size
                            )
                        .toInt()
                        .coerceIn(
                            minimumValue = 0,
                            maximumValue = 100
                        )
                } else {
                    0
                }

            /*
             * הרשומות האישיות משמשות לגרף ולרשימת
             * האימונים האחרונים, ולא כמכנה האחוזים.
             */
            val allSessions =
                history.sessions
                    .filter { session ->
                        !session.date.isAfter(today)
                    }

            hasRealAttendanceData =
                allSessions.isNotEmpty() ||
                        scheduledDates.isNotEmpty()

            /*
             * בגרף מציגים רק חודשים שבהם המתאמן
             * נכח לפחות באימון אחד.
             */
            monthlyAttendance =
                allSessions
                    .asSequence()
                    .filter { session ->
                        session.status ==
                                AttendanceStatus.PRESENT
                    }
                    .groupBy { session ->
                        YearMonth.from(
                            session.date
                        )
                    }
                    .map { (yearMonth, sessions) ->

                        val attendedDates =
                            sessions
                                .map { session ->
                                    session.date
                                }
                                .distinct()
                                .sorted()

                        MonthlyAttendancePoint(
                            yearMonth = yearMonth,
                            attendedTrainings =
                                attendedDates.size,
                            attendedDates =
                                attendedDates
                        )
                    }
                    .filter { point ->
                        point.attendedTrainings > 0
                    }
                    .sortedBy { point ->
                        point.yearMonth
                    }

            lastSessions =
                allSessions
                    .sortedByDescending { session ->
                        session.date
                    }
                    .take(5)
                    .map { session ->
                        val dateText =
                            if (isEnglish) {
                                session.date.toString()
                            } else {
                                formatDateHeb(
                                    session.date
                                )
                            }

                        val statusText =
                            when (session.status) {
                                AttendanceStatus.PRESENT ->
                                    tr(
                                        "הגיע",
                                        "Present"
                                    )

                                AttendanceStatus.EXCUSED ->
                                    tr(
                                        "מוצדק",
                                        "Excused"
                                    )

                                AttendanceStatus.ABSENT ->
                                    tr(
                                        "לא הגיע",
                                        "Absent"
                                    )
                            }

                        "$dateText · $statusText"
                    }
        } finally {
            isAttendanceLoading = false
        }
    }

    // טעינת 5 הדו"חות האחרונים מה־DB

    // טעינת 5 הדו"חות האחרונים מה־DB
    LaunchedEffect(branch, groupKey) {
        if (branch.isBlank() || groupKey.isBlank()) return@LaunchedEffect
        repo.lastReports(branch, groupKey, limit = 5).collect { reports = it }
    }

    val name =
        memberName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: tr(
                "מתאמן לא נבחר",
                "No trainee selected"
            )

    /*
     * אותו רקע בדיוק שבו משתמש מסך הבית.
     */
    val backgroundBrush =
        remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FBFF),
                    Color(0xFFEAF4FF),
                    Color(0xFFB7DDF7),
                    Color(0xFF1F78B4),
                    Color(0xFF062B4A)
                )
            )
        }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr(
                    "סטטיסטיקת נוכחות",
                    "Attendance statistics"
                ),

                /*
                 * חזרה ובית הן פעולות נפרדות:
                 * חזרה מחזירה למסך הקודם,
                 * בית עובר ישירות למסך הבית.
                 */
                onBack = onBack,
                onHome = onHome,

                /*
                 * החיפוש הגלובלי מנוהל פנימית על ידי
                 * KmiTopBar, ולכן צריך רק להסיר את הנעילה.
                 */
                lockSearch = false,

                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                showSettings = true,
                showBottomHelp = true,
                showBottomShare = true,
                centerTitle = true,
                onShare = {
                    shareAttendanceStatsPdf(
                        context = context,
                        data = AttendanceStatsPdfData(
                            memberName = name,
                            branch = branch,
                            groupKey = groupKey,
                            monthlyPercent =
                                monthlyPercent,
                            yearlyPercent =
                                yearlyPercent,
                            monthlyPresent =
                                monthlyPresentCount,
                            monthlyScheduled =
                                monthlyScheduledCount,
                            yearlyPresent =
                                yearlyPresentCount,
                            yearlyScheduled =
                                yearlyScheduledCount,
                            monthlyPoints =
                                monthlyAttendance,
                            recentSessions =
                                lastSessions
                        ),
                        isEnglish = isEnglish
                    )
                }
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    backgroundBrush
                )
        ) {
            if (isAttendanceLoading) {
                AttendanceLoadingRings(
                    isEnglish = isEnglish,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(18.dp)
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

                // ───── גרף נוכחות חודשי בשנה האחרונה ─────
                if (monthlyAttendance.isNotEmpty()) {
                    MonthlyAttendanceChart(
                        points = monthlyAttendance,
                        isEnglish = isEnglish
                    )
                }

                // ───── 5 אימונים אחרונים (דינמי לפי רשומות נוכחות) ─────
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.96f
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.55f
                        )
                    )
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
                                text =
                                    if (isEnglish) {
                                        "5 recent trainings"
                                    } else {
                                        "5 אימונים אחרונים"
                                    },
                                style = KmiTypography.sectionTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        lastSessions.forEach { row ->
                            val wasPresent =
                                (
                                        row.contains("· הגיע") ||
                                                row.contains(
                                                    "· Present",
                                                    ignoreCase = true
                                                )
                                        ) &&
                                        !row.contains("לא הגיע") &&
                                        !row.contains(
                                            "Absent",
                                            ignoreCase = true
                                        )

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
                                    text = row,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = KmiTypography.body,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (lastSessions.isEmpty()) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "No attendance data is available yet."
                                    } else {
                                        "אין נתוני נוכחות מוצגים עדיין."
                                    },
                                style = KmiTypography.caption,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                    /*
                     * דו"חות הנוכחות מכילים סיכום של כל הקבוצה,
                     * ולכן אינם מוצגים כאשר נבחר מתאמן בודד.
                     */
                    if (
                        memberId == null &&
                        reports.isNotEmpty()
                    ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.96f
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 6.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.55f
                                )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Recent attendance reports"
                                    } else {
                                        "דו״חות נוכחות אחרונים"
                                    },
                                style = KmiTypography.sectionTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
}

private fun rtlLine(s: String): String = "\u200F" + s + "\u200F"


/**
 * אנימציית טעינה של שלוש טבעות מסתובבות.
 *
 * משתמשת ב־CircularProgressIndicator הגלובלי של
 * Material 3 ולכן ממשיכה להסתובב עד שהנתונים מוכנים.
 */
@Composable
private fun AttendanceLoadingRings(
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(124.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(124.dp),
                color = Color(0xFF22D3EE),
                trackColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.35f
                    ),
                strokeWidth = 6.dp
            )

            CircularProgressIndicator(
                modifier = Modifier.size(88.dp),
                color = Color(0xFFA78BFA),
                trackColor = Color.Transparent,
                strokeWidth = 6.dp
            )

            CircularProgressIndicator(
                modifier = Modifier.size(52.dp),
                color = Color(0xFFF472B6),
                trackColor = Color.Transparent,
                strokeWidth = 5.dp
            )
        }

        Text(
            text =
                if (isEnglish) {
                    "Loading attendance data…"
                } else {
                    "טוען נתוני נוכחות…"
                },
            style = KmiTypography.cardTitle,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

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
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = 0.94f
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.42f
                                ),
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.14f
                                )
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
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = KmiTypography.cardTitle.merge(
                        TextStyle(
                            textDirection = direction
                        )
                    ),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr(
                        "המסך מחובר לשרת. לאחר סימון ושמירת נוכחות במסך הנוכחות, הנתונים של $memberName יופיעו כאן.",
                        "This screen is connected to the server. After attendance is marked and saved, $memberName's data will appear here."
                    ),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.body.merge(
                        TextStyle(
                            textDirection = direction
                        )
                    ),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr(
                        "סניף: ${branch.ifBlank { "—" }} · קבוצה: ${groupKey.ifBlank { "—" }}",
                        "Branch: ${branch.ifBlank { "—" }} · Group: ${groupKey.ifBlank { "—" }}"
                    ),
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = KmiTypography.caption.merge(
                        TextStyle(
                            textDirection = direction
                        )
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
                            style = KmiTypography.cardTitle.merge(rtlStyle),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
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
                            style = KmiTypography.caption.merge(rtlStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        style = KmiTypography.secondary.merge(rtlStyle),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE),
                        textAlign = TextAlign.Right
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = tr("שנתי: $yearlyPercent%", "Yearly: $yearlyPercent%"),
                        style = KmiTypography.secondary.merge(rtlStyle),
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
                            color = MaterialTheme.colorScheme.primary,
                            style = KmiTypography.caption.merge(rtlStyle),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        lines
                            .filter { it.isNotBlank() }
                            .forEach { line ->
                                Text(
                                    text = line,
                                    color =
                                        MaterialTheme.colorScheme.onSurface,
                                    style =
                                        KmiTypography.body.merge(
                                            rtlStyle
                                        ),
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    textAlign = TextAlign.Right,
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow =
                                        TextOverflow.Ellipsis
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
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = 0.94f
        ),
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
                    .heightIn(min = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = KmiTypography.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            text = "$percent%",
                            style = KmiTypography.sectionTitle,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,

                            /*
                             * העיגול הפנימי לבן בשני המצבים,
                             * ולכן הטקסט חייב להיות כהה וקבוע.
                             */
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
                style = KmiTypography.secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * גרף עמודות של נוכחות חודשית.
 *
 * הרשימה שמתקבלת כבר מסוננת ולכן מוצגים רק חודשים
 * שבהם המתאמן נכח לפחות באימון אחד.
 */
@Composable
private fun MonthlyAttendanceChart(
    points: List<MonthlyAttendancePoint>,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        return
    }

    var selectedPoint by remember {
        mutableStateOf<MonthlyAttendancePoint?>(null)
    }

    val maxAttendance =
        points
            .maxOfOrNull {
                it.attendedTrainings
            }
            ?.coerceAtLeast(1)
            ?: 1

    val monthNamesHe =
        listOf(
            "ינו׳",
            "פבר׳",
            "מרץ",
            "אפר׳",
            "מאי",
            "יוני",
            "יולי",
            "אוג׳",
            "ספט׳",
            "אוק׳",
            "נוב׳",
            "דצמ׳"
        )

    val monthNamesEn =
        listOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = 0.96f
        ),
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.20f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 18.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "Attendance over the last 12 months"
                    } else {
                        "נוכחות ב־12 החודשים האחרונים"
                    },
                style =
                    KmiTypography.sectionTitle,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text =
                    if (isEnglish) {
                        "Only months with at least one attended training are shown"
                    } else {
                        "מוצגים רק חודשים שבהם המתאמן נכח לפחות באימון אחד"
                    },
                style =
                    KmiTypography.secondary,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val barFraction =
                        (
                                point.attendedTrainings.toFloat() /
                                        maxAttendance.toFloat()
                                )
                            .coerceIn(
                                0f,
                                1f
                            )

                    val barHeight =
                        (
                                24f +
                                        96f * barFraction
                                ).dp

                    val monthIndex =
                        point.yearMonth.monthValue - 1

                    val monthLabel =
                        if (isEnglish) {
                            monthNamesEn[
                                monthIndex
                            ]
                        } else {
                            monthNamesHe[
                                monthIndex
                            ]
                        }

                    Column(
                        modifier = Modifier.widthIn(
                            min = 56.dp
                        ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text =
                                point.attendedTrainings
                                    .toString(),
                            style =
                                KmiTypography.secondary,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(124.dp)
                                .pointerInput(point.yearMonth) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event =
                                                awaitPointerEvent()

                                            when (event.type) {
                                                PointerEventType.Enter,
                                                PointerEventType.Move -> {
                                                    selectedPoint = point
                                                }

                                                PointerEventType.Exit -> {
                                                    if (
                                                        selectedPoint?.yearMonth ==
                                                        point.yearMonth
                                                    ) {
                                                        selectedPoint = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .clickable {
                                    selectedPoint =
                                        if (
                                            selectedPoint?.yearMonth ==
                                            point.yearMonth
                                        ) {
                                            null
                                        } else {
                                            point
                                        }
                                },
                            contentAlignment =
                                Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(barHeight)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 10.dp,
                                            topEnd = 10.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFF2563EB),
                                                Color(0xFF06B6D4)
                                            )
                                        )
                                    )
                            )
                        }

                        Text(
                            text = monthLabel,
                            style =
                                KmiTypography.caption,
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Text(
                            text =
                                point.yearMonth.year
                                    .toString(),
                            style =
                                KmiTypography.caption,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            selectedPoint?.let { point ->

                val selectedMonthIndex =
                    point.yearMonth.monthValue - 1

                val selectedMonthName =
                    if (isEnglish) {
                        monthNamesEn[selectedMonthIndex]
                    } else {
                        monthNamesHe[selectedMonthIndex]
                    }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color =
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.55f
                        ),
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.25f
                            )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text =
                                if (isEnglish) {
                                    "$selectedMonthName ${point.yearMonth.year} · " +
                                            "${point.attendedTrainings} attended"
                                } else {
                                    "$selectedMonthName ${point.yearMonth.year} · " +
                                            "${point.attendedTrainings} נוכחויות"
                                },
                            style = KmiTypography.secondary,
                            fontWeight = FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            point.attendedDates.forEach { date ->
                                Text(
                                    text =
                                        if (isEnglish) {
                                            date.toString()
                                        } else {
                                            formatDateHeb(date)
                                        },
                                    style = KmiTypography.caption,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign =
                                        if (isEnglish) {
                                            TextAlign.Left
                                        } else {
                                            TextAlign.Right
                                        },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
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
        modifier = modifier.widthIn(min = 190.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.45f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDateHeb(report.date),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = KmiTypography.body
            )

            Text(
                text = "נוכחות: ${report.percentPresent}%",
                color = Color(0xFF22C55E),
                style = KmiTypography.secondary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "סה״כ ${report.totalMembers} • " +
                            "הגיעו ${report.presentCount} • " +
                            "מוצדקים ${report.excusedCount}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = KmiTypography.caption
            )
        }
    }
}

private fun hebDay(
    dayOfWeek: DayOfWeek
): String {
    return when (dayOfWeek) {
        DayOfWeek.SUNDAY -> "א׳"
        DayOfWeek.MONDAY -> "ב׳"
        DayOfWeek.TUESDAY -> "ג׳"
        DayOfWeek.WEDNESDAY -> "ד׳"
        DayOfWeek.THURSDAY -> "ה׳"
        DayOfWeek.FRIDAY -> "ו׳"
        DayOfWeek.SATURDAY -> "שבת"
    }
}

private fun formatDateHeb(date: LocalDate): String {
    return "${date.dayOfMonth}.${date.monthValue}.${date.year}"
}

private data class AttendanceStatsPdfData(
    val memberName: String,
    val branch: String,
    val groupKey: String,
    val monthlyPercent: Int,
    val yearlyPercent: Int,
    val monthlyPresent: Int,
    val monthlyScheduled: Int,
    val yearlyPresent: Int,
    val yearlyScheduled: Int,
    val monthlyPoints: List<MonthlyAttendancePoint>,
    val recentSessions: List<String>
)

private fun shareAttendanceStatsPdf(
    context: Context,
    data: AttendanceStatsPdfData,
    isEnglish: Boolean
) {
    val pdfFile =
        createAttendanceStatsPdf(
            context = context,
            data = data,
            isEnglish = isEnglish
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "KAMI attendance report - " +
                            data.memberName
                } else {
                    "דו״ח נוכחות ק.מ.י - " +
                            data.memberName
                }
            )

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) {
                "Share attendance PDF"
            } else {
                "שיתוף דו״ח נוכחות PDF"
            }
        )
    )
}

private fun createAttendanceStatsPdf(
    context: Context,
    data: AttendanceStatsPdfData,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val document =
        PdfDocument()

    val page =
        document.startPage(
            PdfDocument.PageInfo
                .Builder(
                    pageWidth,
                    pageHeight,
                    1
                )
                .create()
        )

    val canvas =
        page.canvas

    val navy =
        android.graphics.Color.rgb(
            2,
            43,
            74
        )

    val blue =
        android.graphics.Color.rgb(
            12,
            78,
            130
        )

    val lightBlue =
        android.graphics.Color.rgb(
            234,
            246,
            255
        )

    val softBlue =
        android.graphics.Color.rgb(
            244,
            250,
            255
        )

    val borderBlue =
        android.graphics.Color.rgb(
            191,
            213,
            232
        )

    val textDark =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val textMuted =
        android.graphics.Color.rgb(
            80,
            100,
            120
        )

    val green =
        android.graphics.Color.rgb(
            22,
            163,
            74
        )

    val regular =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val bold =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            textAlign = align
        }
    }

    val titlePaint =
        paint(
            size = 27f,
            color = android.graphics.Color.WHITE,
            typeface = bold
        )

    val subtitlePaint =
        paint(
            size = 13f,
            color = android.graphics.Color.WHITE
        )

    val sectionPaint =
        paint(
            size = 17f,
            color = blue,
            typeface = bold
        )

    val labelPaint =
        paint(
            size = 10.5f,
            color = blue,
            typeface = bold
        )

    val valuePaint =
        paint(
            size = 12f,
            color = textDark
        )

    val boldValuePaint =
        paint(
            size = 14f,
            color = textDark,
            typeface = bold
        )

    val smallPaint =
        paint(
            size = 9f,
            color = textMuted
        )

    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false
    ) {
        val rectanglePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                this.color = color
                style =
                    if (stroke) {
                        Paint.Style.STROKE
                    } else {
                        Paint.Style.FILL
                    }
                strokeWidth = 1.2f
            }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectanglePaint
        )
    }

    fun drawKmiLogo(
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val outerPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
            }

        val innerPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
            }

        val logoTextPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
                typeface = bold
                textSize = radius * 0.62f
                textAlign = Paint.Align.CENTER
            }

        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            outerPaint
        )

        canvas.drawCircle(
            centerX,
            centerY,
            radius - 4f,
            innerPaint
        )

        canvas.drawText(
            "KAMI",
            centerX,
            centerY + radius * 0.22f,
            logoTextPaint
        )
    }

    /*
     * כותרת זהה לסגנון דו״ח מסך הבית.
     */
    canvas.drawColor(
        android.graphics.Color.WHITE
    )

    val headerPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
        }

    val firstAccentPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.rgb(
                    36,
                    103,
                    158
                )
        }

    val secondAccentPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.rgb(
                    128,
                    183,
                    220
                )
        }

    canvas.drawPath(
        Path().apply {
            moveTo(
                pageWidth.toFloat(),
                0f
            )
            lineTo(
                pageWidth.toFloat(),
                122f
            )
            lineTo(
                178f,
                122f
            )
            lineTo(
                238f,
                0f
            )
            close()
        },
        headerPaint
    )

    canvas.drawPath(
        Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        },
        firstAccentPaint
    )

    canvas.drawPath(
        Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        },
        secondAccentPaint
    )

    drawKmiLogo(
        centerX = 78f,
        centerY = 58f,
        radius = 42f
    )

    titlePaint.textAlign =
        Paint.Align.RIGHT

    subtitlePaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr(
            "דו״ח סטטיסטיקת נוכחות",
            "Attendance statistics report"
        ),
        pageWidth - 34f,
        50f,
        titlePaint
    )

    canvas.drawText(
        data.memberName.take(34),
        pageWidth - 34f,
        77f,
        subtitlePaint
    )

    smallPaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr(
            "תאריך הפקה:",
            "Generated:"
        ) + " " +
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(Date()),
        pageWidth - 34f,
        142f,
        smallPaint
    )

    /*
     * פרטי מתאמן.
     */
    drawRoundRect(
        margin,
        160f,
        pageWidth - margin,
        232f,
        lightBlue
    )

    drawRoundRect(
        margin,
        160f,
        pageWidth - margin,
        232f,
        borderBlue,
        stroke = true
    )

    sectionPaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr(
            "פרטי המתאמן",
            "Trainee details"
        ),
        pageWidth - margin - 18f,
        184f,
        sectionPaint
    )

    valuePaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr("סניף:", "Branch:") +
                " " +
                data.branch.take(42),
        pageWidth - margin - 18f,
        205f,
        valuePaint
    )

    canvas.drawText(
        tr("קבוצה:", "Group:") +
                " " +
                data.groupKey.take(42),
        pageWidth - margin - 18f,
        222f,
        valuePaint
    )

    /*
     * ארבעה נתוני סיכום.
     */
    val cardTop = 250f
    val cardBottom = 334f
    val cardWidth = 126f
    val cardGap = 10f

    data class SummaryItem(
        val title: String,
        val value: String
    )

    val summaryItems =
        listOf(
            SummaryItem(
                title = tr(
                    "נוכחות חודשית",
                    "Monthly attendance"
                ),
                value =
                    "${data.monthlyPercent}%"
            ),
            SummaryItem(
                title = tr(
                    "אימוני החודש",
                    "Monthly trainings"
                ),
                value =
                    "${data.monthlyPresent}" +
                            "/" +
                            "${data.monthlyScheduled}"
            ),
            SummaryItem(
                title = tr(
                    "נוכחות שנתית",
                    "Yearly attendance"
                ),
                value =
                    "${data.yearlyPercent}%"
            ),
            SummaryItem(
                title = tr(
                    "אימונים שנתיים",
                    "Yearly trainings"
                ),
                value =
                    "${data.yearlyPresent}" +
                            "/" +
                            "${data.yearlyScheduled}"
            )
        )

    summaryItems.forEachIndexed {
            index,
            item ->

        val left =
            margin +
                    index *
                    (
                            cardWidth +
                                    cardGap
                            )

        val right =
            left + cardWidth

        drawRoundRect(
            left,
            cardTop,
            right,
            cardBottom,
            if (index % 2 == 0) {
                lightBlue
            } else {
                softBlue
            }
        )

        drawRoundRect(
            left,
            cardTop,
            right,
            cardBottom,
            borderBlue,
            stroke = true
        )

        labelPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            item.title,
            (left + right) / 2f,
            cardTop + 27f,
            labelPaint
        )

        boldValuePaint.textAlign =
            Paint.Align.CENTER

        boldValuePaint.textSize = 22f

        boldValuePaint.color =
            if (
                index == 0 ||
                index == 2
            ) {
                green
            } else {
                navy
            }

        canvas.drawText(
            item.value,
            (left + right) / 2f,
            cardTop + 61f,
            boldValuePaint
        )
    }

    boldValuePaint.textSize = 14f
    boldValuePaint.color = textDark

    /*
     * פירוט חודשי.
     */
    sectionPaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr(
            "נוכחות לפי חודשים",
            "Attendance by month"
        ),
        pageWidth - margin,
        370f,
        sectionPaint
    )

    var rowY = 397f

    if (data.monthlyPoints.isEmpty()) {
        valuePaint.textAlign =
            Paint.Align.RIGHT

        canvas.drawText(
            tr(
                "אין עדיין נוכחות חודשית להצגה",
                "No monthly attendance data yet"
            ),
            pageWidth - margin,
            rowY,
            valuePaint
        )

        rowY += 28f
    } else {
        data.monthlyPoints
            .takeLast(6)
            .forEachIndexed {
                    index,
                    point ->

                val rowTop =
                    rowY - 17f

                drawRoundRect(
                    margin,
                    rowTop,
                    pageWidth - margin,
                    rowTop + 29f,
                    if (index % 2 == 0) {
                        lightBlue
                    } else {
                        softBlue
                    },
                    radius = 7f
                )

                val monthText =
                    "${point.yearMonth.monthValue}" +
                            "/" +
                            "${point.yearMonth.year}"

                valuePaint.textAlign =
                    Paint.Align.RIGHT

                canvas.drawText(
                    monthText,
                    pageWidth - margin - 14f,
                    rowY + 2f,
                    valuePaint
                )

                boldValuePaint.textAlign =
                    Paint.Align.LEFT

                canvas.drawText(
                    tr(
                        "${point.attendedTrainings} נוכחויות",
                        "${point.attendedTrainings} attended"
                    ),
                    margin + 14f,
                    rowY + 2f,
                    boldValuePaint
                )

                rowY += 34f
            }
    }

    /*
     * חמש רשומות אחרונות.
     */
    rowY += 15f

    sectionPaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        tr(
            "5 אימונים אחרונים",
            "5 recent trainings"
        ),
        pageWidth - margin,
        rowY,
        sectionPaint
    )

    rowY += 26f

    if (data.recentSessions.isEmpty()) {
        valuePaint.textAlign =
            Paint.Align.RIGHT

        canvas.drawText(
            tr(
                "אין רשומות נוכחות להצגה",
                "No attendance records to display"
            ),
            pageWidth - margin,
            rowY,
            valuePaint
        )
    } else {
        data.recentSessions
            .take(5)
            .forEachIndexed {
                    index,
                    session ->

                if (rowY < 785f) {
                    drawRoundRect(
                        margin,
                        rowY - 17f,
                        pageWidth - margin,
                        rowY + 12f,
                        if (index % 2 == 0) {
                            lightBlue
                        } else {
                            softBlue
                        },
                        radius = 7f
                    )

                    valuePaint.textAlign =
                        Paint.Align.RIGHT

                    canvas.drawText(
                        session.take(52),
                        pageWidth - margin - 14f,
                        rowY + 2f,
                        valuePaint
                    )

                    rowY += 34f
                }
            }
    }

    /*
     * תחתית זהה לדו״ח מסך הבית.
     */
    val footerY = 804f

    val footerLine =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            strokeWidth = 2f
        }

    canvas.drawLine(
        0f,
        footerY,
        pageWidth.toFloat(),
        footerY,
        footerLine
    )

    drawKmiLogo(
        centerX = 38f,
        centerY = footerY + 22f,
        radius = 13f
    )

    smallPaint.textAlign =
        Paint.Align.LEFT

    canvas.drawText(
        "Together We Protect",
        62f,
        footerY + 25f,
        smallPaint
    )

    smallPaint.textAlign =
        Paint.Align.CENTER

    canvas.drawText(
        tr(
            "עמוד 1 מתוך 1",
            "Page 1 of 1"
        ),
        pageWidth / 2f,
        footerY + 25f,
        smallPaint
    )

    smallPaint.textAlign =
        Paint.Align.RIGHT

    canvas.drawText(
        "Krav Maga Israel",
        pageWidth - 66f,
        footerY + 18f,
        smallPaint
    )

    canvas.drawText(
        "www.kmi.org.il",
        pageWidth - 66f,
        footerY + 31f,
        smallPaint
    )

    document.finishPage(page)

    val directory =
        File(
            context.cacheDir,
            "pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * מנקה רק תווים שאינם חוקיים בשם קובץ.
     * עברית ואנגלית נשמרות ללא שינוי.
     */
    fun safeFilePart(value: String): String =
        value
            .trim()
            .replace(
                Regex("""[\\/:*?"<>|]"""),
                "-"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .ifBlank {
                if (isEnglish) {
                    "Unknown"
                } else {
                    "לא ידוע"
                }
            }

    val safeMemberName =
        safeFilePart(
            data.memberName
        )

    val safeBranch =
        safeFilePart(
            data.branch
        )

    val safeGroup =
        safeFilePart(
            data.groupKey
        )

    val fileName =
        if (isEnglish) {
            "Attendance statistics - $safeMemberName - $safeBranch - $safeGroup.pdf"
        } else {
            "סטטיסטיקת נוכחות - $safeMemberName - $safeBranch - $safeGroup.pdf"
        }

    val file =
        File(
            directory,
            fileName
        )

    /*
     * מונע הצטברות של עותקים לאותו דוח
     * בתיקיית המטמון.
     */
    if (file.exists()) {
        file.delete()
    }

    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}