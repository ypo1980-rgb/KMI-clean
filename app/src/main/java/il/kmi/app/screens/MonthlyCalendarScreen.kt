@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.graphics.graphicsLayer
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingData
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.prefs.KmiPrefs
import org.json.JSONObject
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
    onBack: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        val today = remember { LocalDate.now() }
        var ym by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
        var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(today) }

        // קלטים מהעדפות המשתמש
        val region   = kmiPrefs.region.orEmpty()
        val branchRaw = kmiPrefs.branch.orEmpty()
        val groupRaw  = kmiPrefs.ageGroup.orEmpty()

        var dialogDate by remember { mutableStateOf<LocalDate?>(null) }
        var dialogTrainings by remember { mutableStateOf<List<TrainingData>>(emptyList()) }

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
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val monthTitle = ym.month.getDisplayName(
                            java.time.format.TextStyle.FULL,
                            Locale("he")
                        )
                        Text(text = "$monthTitle ${ym.year}", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(22.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "סגור",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { ym = ym.minusMonths(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "חודש קודם")
                        }
                        IconButton(onClick = { ym = ym.plusMonths(1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "חודש הבא")
                        }
                    }
                )
            }
        ) { padding ->

            // סווייפ לשינוי חודש
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
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
                    Row(Modifier.fillMaxWidth()) {
                        days.forEach { dow ->
                            Text(
                                text = dow.getDisplayName(
                                    java.time.format.TextStyle.SHORT,
                                    Locale("he")
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // דיאגנוסטיקה
                    DebugBanner(
                        region = region,
                        normBranchKey = primaryBranch,
                        normGroupKey = primaryGroup,
                        ym = ym,
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
                    val firstOfMonth = ym.atDay(1)
                    val firstWeekdayIndex = (firstOfMonth.dayOfWeek.value % 7)
                    val daysInMonth = ym.lengthOfMonth()
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
                                        val date = ym.atDay(day)
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

                                                val dayTrainings = buildList {
                                                    for (b in normBranchKeys) {
                                                        for (g in normGroupKeys) {
                                                            val weekly = TrainingCatalog.trainingsFor(b, g)
                                                            weekly.forEach { td ->
                                                                val calDow =
                                                                    td.cal.get(java.util.Calendar.DAY_OF_WEEK)
                                                                val calIndex = when (calDow) {
                                                                    java.util.Calendar.SUNDAY    -> 0
                                                                    java.util.Calendar.MONDAY    -> 1
                                                                    java.util.Calendar.TUESDAY   -> 2
                                                                    java.util.Calendar.WEDNESDAY -> 3
                                                                    java.util.Calendar.THURSDAY  -> 4
                                                                    java.util.Calendar.FRIDAY    -> 5
                                                                    else                         -> 6
                                                                }
                                                                val dateIndex = date.dayOfWeek.value % 7
                                                                if (calIndex == dateIndex) {
                                                                    add(td)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                dialogDate = date
                                                dialogTrainings = dayTrainings
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
                                Locale("he")
                            )
                            val monthName = sel.month.getDisplayName(
                                java.time.format.TextStyle.FULL,
                                Locale("he")
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                tonalElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "יום נבחר: $dowName ${sel.dayOfMonth} $monthName ${sel.year}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val infoParts = buildList {
                                        if (selTrainings > 0) add("$selTrainings אימון/ים")
                                        if (!selHoliday.isNullOrBlank()) add(selHoliday!!)
                                    }

                                    Text(
                                        text = if (infoParts.isEmpty())
                                            "אין אירועים ביום זה."
                                        else
                                            infoParts.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // מקרא
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Dot(color = Color(0xFF1565C0), size = 10.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "אימון",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(24.dp))
                            Dot(color = Color(0xFF7B1FA2), size = 10.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "חג",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // דיאלוג פרטי אימון ליום שנבחר
                    dialogDate?.let { picked ->
                        val pickedHoliday = holidaysByDate[picked]

                        AlertDialog(
                            onDismissRequest = { dialogDate = null },
                            title = {
                                Text("אימונים ב-${picked.dayOfMonth}.${picked.monthValue}.${picked.year}")
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!pickedHoliday.isNullOrBlank()) {
                                        Text("🎉 $pickedHoliday", style = MaterialTheme.typography.bodyLarge)
                                    }

                                    if (dialogTrainings.isEmpty()) {
                                        Text("אין אימון ביום זה.")
                                    } else {
                                        dialogTrainings.forEach { td ->
                                            val h = td.cal.get(java.util.Calendar.HOUR_OF_DAY)
                                            val m = td.cal.get(java.util.Calendar.MINUTE)
                                            val timeStr = String.format("%02d:%02d", h, m)
                                            Text("• $timeStr – אימון קבוע")
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { dialogDate = null }) {
                                    Text("סגור")
                                }
                            }
                        )
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
    val trainingColor = Color(0xFF1565C0)
    val holidayColor = Color(0xFF7B1FA2)
    val todayRingColor = MaterialTheme.colorScheme.primary

    // ⭐ רטט גלובלי
    val haptics = rememberHapticsGlobal()
    val interactionSource = remember { MutableInteractionSource() }

    // רקע התא – גם כשהיום הנוכחי, הריבוע נשאר צבוע
    val cellBg: Color = when {
        holidayName != null -> holidayColor.copy(alpha = 0.18f)
        trainingCount > 0   -> trainingColor.copy(alpha = 0.18f)
        else                -> Color.Transparent
    }

    // ⭐ אנימציית scale ליום הנבחר
    val targetScale = if (isSelected) 0.96f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        label = "day-scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = cellBg,
                shape = RoundedCornerShape(12.dp)
            )
            .let { base ->
                if (onClick != null) {
                    base.clickable(
                        interactionSource = interactionSource
                    ) {
                        haptics(false)   // רטט קצר
                        onClick()
                    }
                } else base
            }
    ) {
        if (isToday) {
            // ✅ רק היום הנוכחי מקבל עיגול – אין עיגול נוסף ליום שנבחר
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .border(
                        width = 2.dp,
                        color = todayRingColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
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
