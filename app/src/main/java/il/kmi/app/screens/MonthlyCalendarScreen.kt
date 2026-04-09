@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.prefs.KmiPrefs
import java.io.BufferedReader
import java.time.*
import java.util.Locale

/* -------------------------------------------------------------------------- */
/*                              Debug banner                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun DebugBanner(
    region: String,
    normBranchKey: String,
    normGroupKey: String,
    ym: YearMonth,
    holidaysByDate: Map<LocalDate, String>,
    trainingsCountByDate: Map<LocalDate, Int>,
    missingReason: String?
) {
    // ✅ "בעיה" רק אם יש סיבה אמיתית
    val hasIssue = (missingReason != null) || trainingsCountByDate.isEmpty()

    // ✅ אם אין בעיה – לא מציגים באנר בכלל (גם אם holidays=0)
    if (!hasIssue) return

    Surface(
        color = Color(0x33FF0000),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("דיאגנוסטיקה", fontWeight = FontWeight.Bold)
            if (missingReason != null) Text("• $missingReason", color = Color.Red)

            Text("• YM: $ym")
            Text("• region='$region'")
            Text("• branchKey='$normBranchKey'  (branches=${TrainingCatalog.branchesFor(region)})")
            Text("• groupKey='$normGroupKey'")
            Text("• holidays.thisMonth=${holidaysByDate.size}")
            Text("• trainings.thisMonth=${trainingsCountByDate.size}")
        }
    }
}


/* -------------------------------------------------------------------------- */
/*                             Screen itself                                  */
/* -------------------------------------------------------------------------- */

@Composable
fun MonthlyCalendarScreen(
    kmiPrefs: KmiPrefs,
    onBack: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenLayoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    val screenLocale = if (isEnglish) Locale.ENGLISH else Locale("he")

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    CompositionLocalProvider(LocalLayoutDirection provides screenLayoutDirection) {

        val today = remember { LocalDate.now() }
        var ym by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
        var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(today) }

        // קלטים מהעדפות המשתמש
        val region   = kmiPrefs.region.orEmpty()
        val branchRaw = kmiPrefs.branch.orEmpty()
        val groupRaw  = kmiPrefs.ageGroup.orEmpty()

        // ❌ הוסר: דיאלוג אימונים מקומי
        // var dialogDate by remember { mutableStateOf<LocalDate?>(null) }
        // var dialogTrainings by remember { mutableStateOf<List<TrainingData>>(emptyList()) }

        // פירוק ערכים מרובים
        fun splitMulti(src: String): List<String> =
            src.split(',', ';', '|', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val branchListRaw = splitMulti(branchRaw)
        val groupListRaw  = splitMulti(groupRaw)

        // נרמול קבוצות – כל קבוצה בנפרד
        val normGroupKeys: List<String> = remember(groupListRaw) {
            groupListRaw
                .map { g -> TrainingCatalog.normalizeGroupName(g) }
                .filter { it.isNotEmpty() }
        }

        // נרמול סניפים – לפי האזור
        val normBranchKeys: List<String> = remember(region, branchListRaw) {
            val regionBranches = TrainingCatalog.branchesFor(region)
            if (regionBranches.isEmpty()) {
                emptyList()
            } else {
                val picked = branchListRaw.mapNotNull { wanted ->
                    regionBranches.firstOrNull { it == wanted }
                        ?: regionBranches.firstOrNull { it.equals(wanted, true) }
                }
                if (picked.isNotEmpty()) picked else listOf(regionBranches.first())
            }
        }

        // חגים לחודש הנבחר
        val ctx = LocalContext.current
        val holidaysByDate: Map<LocalDate, String> = remember(ym, ctx) {
            JewishHolidays.forMonth(ym, ctx = ctx)
        }
        val holidayDates: Set<LocalDate> = remember(holidaysByDate) { holidaysByDate.keys }

// ✅ האם יש חגים בחודש הזה?
        val hasHolidaysThisMonth = remember(holidaysByDate) { holidaysByDate.isNotEmpty() }

// אימונים מאוחדים לחודש
        val trainingsCountByDate: Map<LocalDate, Int> = remember(
            ym, region, normBranchKeys, normGroupKeys, holidayDates
        ) {
            if (region.isBlank() || normBranchKeys.isEmpty() || normGroupKeys.isEmpty()) {
                emptyMap()
            } else if (!TrainingCatalog.isRegionActive(region)) {
                emptyMap()
            } else {
                mergeMonthlyTrainingCounts(
                    ym = ym,
                    branches = normBranchKeys,
                    groups   = normGroupKeys,
                    skipDates = holidayDates
                )
            }
        }

        // לערכי תצוגה
        val primaryBranch = normBranchKeys.firstOrNull().orEmpty()
        val primaryGroup  = normGroupKeys.firstOrNull().orEmpty()

        // סיבת חסר
        val missingReason = when {
            region.isBlank() -> "לא נבחר אזור (region) בהגדרות"
            primaryBranch.isBlank() -> "לא נבחר סניף (branch) בהגדרות"
            primaryGroup.isBlank() -> "לא נבחרה קבוצה / קבוצת גיל"
            !TrainingCatalog.isRegionActive(region) -> "האזור \"$region\" לא פעיל ב־TrainingCatalog"
            !TrainingCatalog.branchesFor(region).contains(primaryBranch) ->
                "הסניף \"$primaryBranch\" לא שייך לאזור \"$region\""
            else -> null
        }

        // לוגים
        LaunchedEffect(
            ym,
            holidaysByDate,
            trainingsCountByDate,
            region,
            primaryBranch,
            primaryGroup
        ) {
            if (missingReason != null) {
                android.util.Log.w("CalendarDebug", "NO TRAININGS: $missingReason")
            } else {
                android.util.Log.d(
                    "CalendarDebug",
                    "ym=$ym holidays=${holidaysByDate.size} trainings=${trainingsCountByDate.size} " +
                            "region=$region branchKey=$primaryBranch groupKey=$primaryGroup"
                )
            }
        }

        Scaffold(
            containerColor = Color(0xFF08142C),
            topBar = {
                val monthTitle = ym.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    screenLocale
                ).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(screenLocale) else ch.toString()
                }

                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF09152F),
                                        Color(0xFF0D1D40),
                                        Color(0xFF122856)
                                    )
                                )
                            )
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            shadowElevation = 14.dp,
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.White.copy(alpha = 0.03f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .clickable { onBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = tr("סגור", "Close"),
                                        tint = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tr("לוח אימונים חודשי", "Monthly calendar"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFC7BAFF),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = { ym = ym.minusMonths(1) },
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.09f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                        ) {
                                            Box(
                                                modifier = Modifier.size(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isEnglish) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                                                    contentDescription = tr("חודש קודם", "Previous month"),
                                                    tint = Color.White
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = Color(0xFF8C74FF).copy(alpha = 0.18f),
                                            border = BorderStroke(
                                                1.dp,
                                                Color.White.copy(alpha = 0.10f)
                                            ),
                                            shadowElevation = 2.dp
                                        ) {
                                            Text(
                                                text = "$monthTitle ${ym.year}",
                                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Surface(
                                            onClick = { ym = ym.plusMonths(1) },
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.09f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                        ) {
                                            Box(
                                                modifier = Modifier.size(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isEnglish) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft,
                                                    contentDescription = tr("חודש הבא", "Next month"),
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->

            // סווייפ לשינוי חודש
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF071126),
                                Color(0xFF0D1E43),
                                Color(0xFF183A7A),
                                Color(0xFF3F78F2)
                            )
                        )
                    )
                    .pointerInput(ym) {
                        val threshold = 48f
                        detectHorizontalDragGestures { _, dragAmount ->
                            when {
                                dragAmount > threshold -> ym = ym.minusMonths(1)
                                dragAmount < -threshold -> ym = ym.plusMonths(1)
                            }
                        }
                    }
            ) {

                AnimatedContent(
                    targetState = ym,
                    transitionSpec = {
                        slideInHorizontally { width -> width } togetherWith
                                slideOutHorizontally { width -> -width }
                    },
                    label = "month-transition"
                ) { animatedYm ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {

                        // כותרות ימי השבוע
                        val days = listOf(
                            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
                        )

                        fun shortWeekdayLabel(dow: DayOfWeek): String {
                            return if (isEnglish) {
                                when (dow) {
                                    DayOfWeek.SUNDAY -> "Sun"
                                    DayOfWeek.MONDAY -> "Mon"
                                    DayOfWeek.TUESDAY -> "Tue"
                                    DayOfWeek.WEDNESDAY -> "Wed"
                                    DayOfWeek.THURSDAY -> "Thu"
                                    DayOfWeek.FRIDAY -> "Fri"
                                    DayOfWeek.SATURDAY -> "Sat"
                                }
                            } else {
                                when (dow) {
                                    DayOfWeek.SUNDAY -> "א׳"
                                    DayOfWeek.MONDAY -> "ב׳"
                                    DayOfWeek.TUESDAY -> "ג׳"
                                    DayOfWeek.WEDNESDAY -> "ד׳"
                                    DayOfWeek.THURSDAY -> "ה׳"
                                    DayOfWeek.FRIDAY -> "ו׳"
                                    DayOfWeek.SATURDAY -> "שבת"
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.06f),
                                                Color.White.copy(alpha = 0.12f),
                                                Color.White.copy(alpha = 0.06f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                days.forEach { dow ->
                                    Text(
                                        text = shortWeekdayLabel(dow),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // דיאגנוסטיקה
                        DebugBanner(
                            region = region,
                            normBranchKey = primaryBranch,
                            normGroupKey = primaryGroup,
                            ym = animatedYm,
                            holidaysByDate = holidaysByDate,
                            trainingsCountByDate = trainingsCountByDate,
                            missingReason = missingReason
                        )

                        if (missingReason != null) {
                            Text(
                                text = missingReason,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        // גריד החודש
                        val firstOfMonth = animatedYm.atDay(1)
                        val firstWeekdayIndex = (firstOfMonth.dayOfWeek.value % 7)
                        val daysInMonth = animatedYm.lengthOfMonth()
                        val totalCells = firstWeekdayIndex + daysInMonth
                        val rows = (totalCells + 6) / 7

                        Column(Modifier.fillMaxWidth()) {
                            var day = 1
                            repeat(rows) { rowIdx ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(7) { col ->
                                        val idx = rowIdx * 7 + col
                                        if (idx < firstWeekdayIndex || day > daysInMonth) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                            )
                                        } else {
                                            val date = animatedYm.atDay(day)
                                            val trainingCount = trainingsCountByDate[date] ?: 0
                                            val holidayName = holidaysByDate[date]

                                            DayCell(
                                                date = date,
                                                isToday = date == today,
                                                isSelected = (selectedDate == date),
                                                trainingCount = trainingCount,
                                                holidayName = holidayName,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    selectedDate = date
                                                }
                                            )
                                            day++
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // אינדיקציה ל"יום הנבחר"
                            selectedDate?.let { sel ->
                                val selTrainings = trainingsCountByDate[sel] ?: 0
                                val selHoliday = holidaysByDate[sel]
                                val dowName = sel.dayOfWeek.getDisplayName(
                                    java.time.format.TextStyle.FULL,
                                    screenLocale
                                )
                                val monthName = sel.month.getDisplayName(
                                    java.time.format.TextStyle.FULL,
                                    screenLocale
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(26.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    shadowElevation = 14.dp,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                ) {
                                    val infoParts = buildList {
                                        if (selTrainings > 0) {
                                            add(tr("$selTrainings אימון/ים", "$selTrainings training(s)"))
                                        }
                                        if (!selHoliday.isNullOrBlank()) {
                                            add(selHoliday)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF6A8FE8).copy(alpha = 0.78f),
                                                        Color(0xFF5D84E4).copy(alpha = 0.72f)
                                                    )
                                                )
                                            )
                                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = tr(
                                                    "יום נבחר: $dowName ${sel.dayOfMonth} $monthName ${sel.year}",
                                                    "Selected day: $dowName ${sel.dayOfMonth} $monthName ${sel.year}"
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            Text(
                                                text = if (infoParts.isEmpty()) {
                                                    tr("אין אירועים ביום זה.", "No events on this day.")
                                                } else {
                                                    infoParts.joinToString(" • ")
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.90f),
                                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(if (isEnglish) Alignment.BottomStart else Alignment.BottomEnd),
                                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                                        ) {
                                            Button(
                                                onClick = { onDateClick(sel) },
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF7C4DFF),
                                                    contentColor = Color.White
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(
                                                    defaultElevation = 6.dp,
                                                    pressedElevation = 10.dp
                                                )
                                            ) {
                                                Text(
                                                    text = tr("+ סיכום", "+ Summary"),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                             helpers                                         */
/* -------------------------------------------------------------------------- */
/** יוצר מיפוי לכל המופעים החודשיים לפי לו״ז שבועי */
private fun buildMonthlyTrainingCount(
    ym: YearMonth,
    branch: String,
    group: String,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, Int> {
    val base = TrainingCatalog.trainingsFor(branch, group)
    if (base.isEmpty()) return emptyMap()

    val startOfMonth = ym.atDay(1)
    val endOfMonth = ym.atEndOfMonth()

    val counts = HashMap<LocalDate, Int>()
    base.forEach { td ->
        val cal = td.cal
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val first = firstDateInMonthForDow(startOfMonth, dow)

        var d = first
        while (!d.isAfter(endOfMonth)) {
            if (d !in skipDates) {
                counts[d] = (counts[d] ?: 0) + 1
            }
            d = d.plusDays(7)
        }
    }
    return counts
}

/** איחוד של כמה סניפים * כמה קבוצות */
private fun mergeMonthlyTrainingCounts(
    ym: YearMonth,
    branches: List<String>,
    groups: List<String>,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, Int> {
    val out = mutableMapOf<LocalDate, Int>()
    for (b in branches) {
        for (g in groups) {
            val m = buildMonthlyTrainingCount(
                ym = ym,
                branch = b,
                group = g,
                skipDates = skipDates
            )
            m.forEach { (date, count) ->
                out[date] = (out[date] ?: 0) + count
            }
        }
    }
    return out
}

/** מחזיר את התאריך הראשון בחודש בעל DayOfWeek מסוים (Calendar) */
private fun firstDateInMonthForDow(startOfMonth: LocalDate, calendarDow: Int): LocalDate {
    fun calendarToJavaDow(calDow: Int): DayOfWeek = when (calDow) {
        java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
        java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
        java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
        java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
        java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
        java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    val wanted = calendarToJavaDow(calendarDow)
    var d = startOfMonth
    while (d.dayOfWeek != wanted) d = d.plusDays(1)
    return d
}

private fun anyToLocalDate(v: Any?): LocalDate? {
    return when (v) {
        null -> null
        is LocalDate -> v
        is LocalDateTime -> v.toLocalDate()
        is OffsetDateTime -> v.toLocalDate()
        is ZonedDateTime -> v.toLocalDate()
        is java.util.Date ->
            v.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        is Long -> {
            val millis = if (v < 3_000_000_000L) v * 1000L else v
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        is Int -> {
            val millis = if (v.toLong() < 3_000_000_000L) v.toLong() * 1000L else v.toLong()
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        is String -> {
            runCatching { LocalDate.parse(v.take(10)) }.getOrNull()
                ?: runCatching {
                    Instant.parse(v).atZone(ZoneId.systemDefault()).toLocalDate()
                }.getOrNull()
                ?: runCatching {
                    val num = v.trim().toLong()
                    val millis = if (num < 3_000_000_000L) num * 1000L else num
                    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                }.getOrNull()
        }
        else -> null
    }
}

/* -------------------------------------------------------------------------- */
/*                             UI bits                                        */
/* -------------------------------------------------------------------------- */

@Composable
private fun Dot(color: Color, size: Dp = 6.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    trainingCount: Int,
    holidayName: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val trainingColor = Color(0xFF3FA7FF)
    val holidayColor = Color(0xFFFF4D6D)
    val todayRingColor = Color(0xFF9A7BFF)

    val haptics = rememberHapticsGlobal()
    val interactionSource = remember { MutableInteractionSource() }

    val cellBgTop: Color
    val cellBgBottom: Color
    when {
        holidayName != null && trainingCount > 0 -> {
            cellBgTop = Color(0xFF6D63D9).copy(alpha = 0.96f)
            cellBgBottom = Color(0xFF4B7EF6).copy(alpha = 0.96f)
        }
        holidayName != null -> {
            cellBgTop = Color(0xFF5B5FCF).copy(alpha = 0.94f)
            cellBgBottom = Color(0xFF415FC4).copy(alpha = 0.94f)
        }
        trainingCount > 0 -> {
            cellBgTop = Color(0xFF5E96FF)
            cellBgBottom = Color(0xFF3F6EE8)
        }
        else -> {
            cellBgTop = Color.White.copy(alpha = 0.14f)
            cellBgBottom = Color.White.copy(alpha = 0.06f)
        }
    }

    val borderColor = when {
        isSelected -> Color.White.copy(alpha = 0.90f)
        holidayName != null || trainingCount > 0 -> Color.White.copy(alpha = 0.14f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    val dayTextColor = Color.White
    val targetScale = when {
        isSelected -> 0.92f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "day-scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .padding(3.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = when {
                    isSelected -> 12.dp
                    trainingCount > 0 -> 8.dp
                    else -> 4.dp
                },
                shape = RoundedCornerShape(14.dp),
                clip = false
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        cellBgTop,
                        cellBgBottom,
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )

            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isSelected) 1.4.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .let { base ->
                if (onClick != null) {
                    base.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptics(false)
                        onClick()
                    }
                } else base
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
        )

        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .background(
                        color = todayRingColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = dayTextColor,
            modifier = Modifier.align(Alignment.Center)
        )

        if (holidayName != null || trainingCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(trainingColor, CircleShape)
                    )
                }

                if (trainingCount > 0 && holidayName != null) {
                    Spacer(Modifier.width(4.dp))
                }

                if (holidayName != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(holidayColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillBadge(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    // אפשר להשאיר לפיצ'רים עתידיים – כרגע לא משתמשים יותר
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = tint
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                         Holidays provider                                  */
/* -------------------------------------------------------------------------- */

object JewishHolidays {
    private const val DEFAULT_ASSET = "holidays_hebrew_2024_2026.json"

    @Volatile private var cache: Map<LocalDate, String>? = null
    @Volatile private var loadedFrom: String? = null

    // ✅ חדש: מידע דיאגנוסטי לבאנר
    data class HolidaysDebugInfo(
        val loadedFrom: String?,
        val cacheSize: Int,
        val range: String
    )

    fun debugInfo(): HolidaysDebugInfo {
        val m = cache.orEmpty()
        val min = m.keys.minOrNull()
        val max = m.keys.maxOrNull()
        val range = if (min != null && max != null) "$min..$max" else "n/a"
        return HolidaysDebugInfo(
            loadedFrom = loadedFrom,
            cacheSize = m.size,
            range = range
        )
    }

    fun forMonth(
        ym: YearMonth,
        ctx: Context,
        assetFile: String = DEFAULT_ASSET
    ): Map<LocalDate, String> {
        cache?.takeIf { loadedFrom == assetFile }?.let { m ->
            return m.filterKeys { YearMonth.from(it) == ym }
        }

        val all = runCatching {
            val json = ctx.assets.open(assetFile)
                .bufferedReader()
                .use(BufferedReader::readText)
            parse(json)
        }.getOrElse { e ->
            val assetList = runCatching { ctx.assets.list("")?.toList().orEmpty() }.getOrElse { emptyList() }
            android.util.Log.e(
                "CalendarDebug",
                "Failed to open/parse asset '$assetFile'. assets(root)=$assetList error=${e::class.java.simpleName}: ${e.message}"
            )
            emptyMap()
        }

        cache = all
        loadedFrom = assetFile

        // ✅ חדש: לוג טווח אמיתי בקובץ
        val min = all.keys.minOrNull()
        val max = all.keys.maxOrNull()
        android.util.Log.d(
            "CalendarDebug",
            "Holidays loaded: asset='$assetFile' items=${all.size} range=${min ?: "n/a"}..${max ?: "n/a"}"
        )

        return all.filterKeys { YearMonth.from(it) == ym }
    }

    private fun parse(json: String): Map<LocalDate, String> {
        val tok = org.json.JSONTokener(json).nextValue()
        val items = when (tok) {
            is org.json.JSONObject -> when {
                tok.has("items") -> tok.optJSONArray("items")
                tok.has("data")  -> tok.optJSONArray("data")
                else             -> null
            }
            is org.json.JSONArray -> tok
            else -> null
        } ?: return emptyMap()

        val out = HashMap<LocalDate, String>(items.length())

        fun putRange(start: LocalDate, end: LocalDate, name: String) {
            var d = start
            while (!d.isAfter(end)) {
                out.putIfAbsent(d, name)
                d = d.plusDays(1)
            }
        }

        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            val name = it.optString("name").trim()
            if (name.isBlank()) continue

            val hasRange = it.has("start_iso") && it.has("end_iso")
            if (hasRange) {
                val s = it.optString("start_iso").trim()
                val e = it.optString("end_iso").trim()
                val start = runCatching { LocalDate.parse(s.take(10)) }.getOrNull()
                val end = runCatching { LocalDate.parse(e.take(10)) }.getOrNull()
                if (start != null && end != null && !end.isBefore(start)) {
                    putRange(start, end, name)
                    continue
                }
            }

            val dateStr = it.optString("date_iso").trim()
            val single = runCatching { LocalDate.parse(dateStr.take(10)) }.getOrNull()
            if (single != null) out.putIfAbsent(single, name)
        }

        return out
    }
}
