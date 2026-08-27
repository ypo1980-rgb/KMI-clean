@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.halacha.HolidayCalendarRepository
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.calendar.KmiCalendarMarkers
import il.kmi.app.ui.calendar.KmiCalendarMonth
import il.kmi.shared.prefs.KmiPrefs
import java.time.*
import java.util.Locale

private data class CalendarTrainingItem(
    val branch: String,
    val group: String,
    val timeText: String,
    val branchEn: String = "",
    val groupEn: String = ""
) {
    fun displayBranch(isEnglish: Boolean): String {
        return if (isEnglish && branchEn.isNotBlank()) branchEn else branch
    }

    fun displayGroup(isEnglish: Boolean): String {
        return if (isEnglish && groupEn.isNotBlank()) groupEn else group
    }
}

/* -------------------------------------------------------------------------- */
/*                             Screen itself                                  */
/* -------------------------------------------------------------------------- */

@Composable
fun MonthlyCalendarScreen(
    kmiPrefs: KmiPrefs,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onDateClick: (
        date: LocalDate,
        branch: String,
        group: String,
        timeText: String
    ) -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenLayoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    val screenLocale = if (isEnglish) Locale.ENGLISH else Locale("he")

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val screenBackgroundBrush =
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF071126),
                    Color(0xFF0D1E43),
                    Color(0xFF183A7A),
                    Color(0xFF3F78F2)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    colorScheme.background,
                    colorScheme.surface,
                    colorScheme.primaryContainer.copy(alpha = 0.30f),
                    colorScheme.background
                )
            )
        }

    val secondaryTextColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.92f)
        } else {
            colorScheme.onSurfaceVariant
        }

    val informationCardColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.10f)
        } else {
            colorScheme.surface
        }

    val informationCardBorder =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.12f)
        } else {
            colorScheme.outline.copy(alpha = 0.22f)
        }

    val selectedDayBrush =
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6A8FE8).copy(alpha = 0.78f),
                    Color(0xFF5D84E4).copy(alpha = 0.72f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    colorScheme.primaryContainer,
                    colorScheme.secondaryContainer
                )
            )
        }

    val selectedDayTextColor =
        if (isDarkTheme) {
            Color.White
        } else {
            colorScheme.onPrimaryContainer
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides screenLayoutDirection
    ) {

        val today = remember { LocalDate.now() }
        var ym by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
        var selectedDate by rememberSaveable { mutableStateOf<LocalDate?>(today) }

        var trainingChoiceDate by remember {
            mutableStateOf<LocalDate?>(null)
        }

        var trainingChoices by remember {
            mutableStateOf<List<CalendarTrainingItem>>(
                emptyList()
            )
        }

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

        // נרמול סניפים – קודם מתוך branches.json, ואם אין התאמה אז fallback ל־TrainingCatalog הישן
        val normBranchKeys: List<String> = remember(ctx, region, branchListRaw) {
            val dbBranches = KmiDatabaseProvider.branches(ctx)
            val dbRegionBranches = dbBranches.filter { branch ->
                branch.regionHe == region ||
                        branch.regionEn.equals(region, ignoreCase = true) ||
                        branch.regionId.equals(region, ignoreCase = true)
            }

            val pickedFromDb = branchListRaw.mapNotNull { wanted ->
                dbRegionBranches.firstOrNull { branch ->
                    branch.nameHe == wanted ||
                            branch.nameEn.equals(wanted, ignoreCase = true) ||
                            branch.placeHe == wanted ||
                            branch.placeEn.equals(wanted, ignoreCase = true)
                }?.nameHe
            }

            if (pickedFromDb.isNotEmpty()) {
                pickedFromDb.distinct()
            } else {
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
        }

        val ctx = LocalContext.current

        /*
         * מקור אמת יחיד לכל נתוני החגים בחודש:
         * שמות להצגה והחלטה אם החג מבטל אימון.
         */
        val holidayInfoByDate = remember(
            ym,
            ctx
        ) {
            HolidayCalendarRepository.holidaysForMonth(
                context = ctx,
                yearMonth = ym
            )
        }

        val holidaysByDate: Map<LocalDate, String> =
            remember(
                holidayInfoByDate,
                isEnglish
            ) {
                holidayInfoByDate.mapValues { (_, holidays) ->
                    holidays
                        .map { holiday ->
                            holiday.displayName(isEnglish)
                        }
                        .filter { name ->
                            name.isNotBlank()
                        }
                        .distinct()
                        .joinToString(" · ")
                }
            }

        val cancelledTrainingDates: Set<LocalDate> =
            remember(holidayInfoByDate) {
                holidayInfoByDate
                    .filterValues { holidays ->
                        holidays.any { holiday ->
                            holiday.cancelsTraining
                        }
                    }
                    .keys
            }

        // ✅ SharedPreferences של הסיכומים
        val summarySp = remember(ctx) {
            ctx.getSharedPreferences("kmi_training_summary", Context.MODE_PRIVATE)
        }

        // ✅ גרסת רענון כדי שהלוח יתעדכן מיד אחרי שמירה / חזרה מהמסך
        var summaryVersion by remember { mutableStateOf(0) }

        DisposableEffect(summarySp) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "training_summary_days") {
                    summaryVersion++
                }
            }
            summarySp.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                summarySp.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        // ✅ ימים שיש להם כבר סיכום שמור
        val summaryDatesThisMonth: Set<LocalDate> = remember(ym, summaryVersion, summarySp, ctx) {
            val legacyUserSp = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

            val allMarks =
                summarySp.getStringSet("training_summary_days", emptySet()).orEmpty() +
                        legacyUserSp.getStringSet("training_summary_days", emptySet()).orEmpty()

            allMarks
                .mapNotNull { raw ->
                    runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()
                }
                .filter { YearMonth.from(it) == ym }
                .toSet()
        }

// אימונים מאוחדים לחודש — קודם branches.json, ואם אין נתונים אז fallback ל־TrainingCatalog
        val trainingsCountByDate: Map<LocalDate, Int> = remember(
            ym,
            ctx,
            region,
            normBranchKeys,
            normGroupKeys,
            cancelledTrainingDates
        ) {
            if (region.isBlank() || normBranchKeys.isEmpty() || normGroupKeys.isEmpty()) {
                emptyMap()
            } else {
                val fromDatabase = mergeMonthlyTrainingCountsFromDatabase(
                    ctx = ctx,
                    ym = ym,
                    branches = normBranchKeys,
                    groups = normGroupKeys,
                    skipDates = cancelledTrainingDates
                )

                if (fromDatabase.isNotEmpty()) {
                    fromDatabase
                } else if (!TrainingCatalog.isRegionActive(region)) {
                    emptyMap()
                } else {
                    mergeMonthlyTrainingCounts(
                        ym = ym,
                        branches = normBranchKeys,
                        groups = normGroupKeys,
                        skipDates = cancelledTrainingDates
                    )
                }
            }
        }

        // פירוט אימונים לפי תאריך — קודם branches.json, ואם אין נתונים אז fallback ל־TrainingCatalog
        val trainingsByDate: Map<LocalDate, List<CalendarTrainingItem>> = remember(
            ym,
            ctx,
            region,
            normBranchKeys,
            normGroupKeys,
            cancelledTrainingDates
        ) {
            if (region.isBlank() || normBranchKeys.isEmpty() || normGroupKeys.isEmpty()) {
                emptyMap()
            } else {
                val fromDatabase = mergeMonthlyTrainingItemsFromDatabase(
                    ctx = ctx,
                    ym = ym,
                    branches = normBranchKeys,
                    groups = normGroupKeys,
                    skipDates = cancelledTrainingDates
                )

                if (fromDatabase.isNotEmpty()) {
                    fromDatabase
                } else if (!TrainingCatalog.isRegionActive(region)) {
                    emptyMap()
                } else {
                    mergeMonthlyTrainingItems(
                        ym = ym,
                        branches = normBranchKeys,
                        groups = normGroupKeys,
                        skipDates = cancelledTrainingDates
                    )
                }
            }
        }

        // לערכי תצוגה
        val primaryBranch = normBranchKeys.firstOrNull().orEmpty()
        val primaryGroup  = normGroupKeys.firstOrNull().orEmpty()

        val databaseRegionActive = remember(ctx, region) {
            KmiDatabaseProvider.regions(ctx).any { dbRegion ->
                dbRegion.nameHe == region ||
                        dbRegion.nameEn.equals(region, ignoreCase = true) ||
                        dbRegion.id.equals(region, ignoreCase = true)
            }
        }

        val databaseBranchExists = remember(ctx, primaryBranch) {
            KmiDatabaseProvider.branchByName(ctx, primaryBranch) != null
        }

        // סיבת חסר — מכיר גם את branches.json וגם את TrainingCatalog הישן
        val missingReason = when {
            region.isBlank() -> "לא נבחר אזור (region) בהגדרות"
            primaryBranch.isBlank() -> "לא נבחר סניף (branch) בהגדרות"
            primaryGroup.isBlank() -> "לא נבחרה קבוצה / קבוצת גיל"
            !databaseRegionActive && !TrainingCatalog.isRegionActive(region) ->
                "האזור \"$region\" לא פעיל ב־Database או ב־TrainingCatalog"
            !databaseBranchExists && !TrainingCatalog.branchesFor(region).contains(primaryBranch) ->
                "הסניף \"$primaryBranch\" לא שייך לאזור \"$region\""
            else -> null
        }

        Scaffold(
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                val contextLang =
                    LocalContext.current

                val topBarLanguageManager =
                    remember(contextLang) {
                        AppLanguageManager(contextLang)
                    }

                KmiTopBar(
                    title = tr(
                        "לוח אימונים חודשי",
                        "Monthly calendar"
                    ),

                    /*
                     * במסך לוח השנה אין צורך בכפתור X.
                     * החזרה זמינה דרך מערכת הניווט של המכשיר.
                     */
                    onBack = null,
                    onHome = onHome,
                    useCloseIcon = false,

                    /*
                     * תפריט הצד מוצג דרך DrawerBridge או
                     * דרך DrawerState שמסופק ברמת האפליקציה.
                     */
                    showMenu = true,

                    /*
                     * KmiTopBar קורא את user_role ומציג
                     * אוטומטית מאמן או מתאמן.
                     */
                    showRoleStatus = true,
                    showRoleBadge = true,
                    showModePill = true,

                    /*
                     * מפעיל את סרגל הפעולות הגלובלי,
                     * ובתוכו גם הפקודות הקוליות.
                     */
                    showBottomActions = true,
                    showBottomHelp = true,

                    centerTitle = true,
                    showTopHome = false,
                    showTopShare = false,

                    currentLang =
                        if (
                            topBarLanguageManager
                                .getCurrentLanguage() ==
                            AppLanguage.ENGLISH
                        ) {
                            "en"
                        } else {
                            "he"
                        },

                    onToggleLanguage = {
                        val newLanguage =
                            if (
                                topBarLanguageManager
                                    .getCurrentLanguage() ==
                                AppLanguage.HEBREW
                            ) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        topBarLanguageManager.setLanguage(
                            newLanguage
                        )

                        (contextLang as? Activity)
                            ?.recreate()
                    }
                )
            }
        ) { padding ->

            // סווייפ לשינוי חודש
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(screenBackgroundBrush)
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
                    val calendarScrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(calendarScrollState)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .padding(bottom = 28.dp)
                    ) {

                        // הודעה נקייה למשתמש אם חסרים פרטי סניף / אזור / קבוצה.
                        // לא מציגים יותר באנר דיאגנוסטיקה במסך עצמו.
                        if (missingReason != null) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = informationCardColor,
                                tonalElevation = 0.dp,
                                shadowElevation =
                                    if (isDarkTheme) 0.dp else 3.dp,
                                border = BorderStroke(
                                    1.dp,
                                    informationCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "לא נמצאו אימונים להצגה עבור האזור, הסניף והקבוצה שנבחרו בפרופיל.",
                                        "No trainings were found for the region, branch, and group selected in your profile."
                                    ),
                                    color = secondaryTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // לוח השנה המרכזי המשותף לכל מסכי האפליקציה.
                        val calendarMarkers = remember(
                            trainingsCountByDate,
                            holidaysByDate,
                            summaryDatesThisMonth
                        ) {
                            KmiCalendarMarkers(
                                trainingDates =
                                    trainingsCountByDate
                                        .filterValues { count ->
                                            count > 0
                                        }
                                        .keys,
                                holidayDates =
                                    holidaysByDate.keys,
                                summaryDates =
                                    summaryDatesThisMonth
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KmiCalendarMonth(
                                visibleMonth = animatedYm,
                                selectedDate = selectedDate,
                                isEnglish = isEnglish,
                                onVisibleMonthChange = { newMonth ->
                                    ym = newMonth
                                },
                                onDateSelected = { date ->
                                    selectedDate = date

                                    val dayTrainings =
                                        trainingsByDate[date]
                                            .orEmpty()
                                            .sortedBy {
                                                it.timeText
                                            }

                                    when {
                                        dayTrainings.size == 1 -> {
                                            val training =
                                                dayTrainings.first()

                                            onDateClick(
                                                date,
                                                training.branch,
                                                training.group,
                                                training.timeText
                                            )
                                        }

                                        dayTrainings.size > 1 -> {
                                            trainingChoiceDate =
                                                date

                                            trainingChoices =
                                                dayTrainings
                                        }

                                        else -> {
                                            trainingChoiceDate =
                                                null

                                            trainingChoices =
                                                emptyList()
                                        }
                                    }
                                },
                                markers = calendarMarkers
                            )

                            Spacer(Modifier.height(4.dp))

                            // אינדיקציה ל"יום הנבחר"
                            selectedDate?.let { sel ->
                                val selTrainings = trainingsCountByDate[sel] ?: 0
                                val selectedTrainingItems = trainingsByDate[sel].orEmpty()
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
                                        .padding(
                                            horizontal = 2.dp,
                                            vertical = 8.dp
                                        ),
                                    shape = RoundedCornerShape(26.dp),
                                    color = informationCardColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation =
                                        if (isDarkTheme) 14.dp else 5.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        informationCardBorder
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
                                            .heightIn(min = 156.dp)
                                            .background(selectedDayBrush)
                                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.TopStart)
                                                .padding(bottom = 76.dp)
                                        ) {
                                            Text(
                                                text = tr(
                                                    "יום נבחר: $dowName ${sel.dayOfMonth} $monthName ${sel.year}",
                                                    "Selected day: $dowName ${sel.dayOfMonth} $monthName ${sel.year}"
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = selectedDayTextColor,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Start
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            Text(
                                                text = when {
                                                    selectedTrainingItems.isNotEmpty() -> {
                                                        val title = tr("פירוט אימונים:", "Training details:")
                                                        val rows = selectedTrainingItems
                                                            .sortedBy { it.timeText }
                                                            .joinToString("\n") { item ->
                                                                val branchLabel = item.displayBranch(isEnglish)
                                                                val groupLabel = item.displayGroup(isEnglish)

                                                                tr(
                                                                    "• ${item.timeText} · $branchLabel · $groupLabel",
                                                                    "• ${item.timeText} · $branchLabel · $groupLabel"
                                                                )
                                                            }

                                                        buildString {
                                                            append(title)
                                                            append("\n")
                                                            append(rows)

                                                            if (!selHoliday.isNullOrBlank()) {
                                                                append("\n")
                                                                append(tr("חג / מועד: $selHoliday", "Holiday: $selHoliday"))
                                                            }
                                                        }
                                                    }

                                                    infoParts.isEmpty() -> {
                                                        tr("אין אירועים ביום זה.", "No events on this day.")
                                                    }

                                                    else -> {
                                                        infoParts.joinToString(" • ")
                                                    }
                                                },
                                                style =
                                                    MaterialTheme.typography.bodyMedium,
                                                color =
                                                    selectedDayTextColor.copy(
                                                        alpha = 0.92f
                                                    ),
                                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(if (isEnglish) Alignment.BottomStart else Alignment.BottomEnd)
                                                .padding(top = 8.dp),
                                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                                        ) {
                                            val hasSummaryForSelectedDate = sel in summaryDatesThisMonth

                                            Button(
                                                onClick = {
                                                    val dayTrainings =
                                                        selectedTrainingItems
                                                            .sortedBy {
                                                                it.timeText
                                                            }

                                                    when {
                                                        dayTrainings.size == 1 -> {
                                                            val training =
                                                                dayTrainings.first()

                                                            onDateClick(
                                                                sel,
                                                                training.branch,
                                                                training.group,
                                                                training.timeText
                                                            )
                                                        }

                                                        dayTrainings.size > 1 -> {
                                                            trainingChoiceDate =
                                                                sel

                                                            trainingChoices =
                                                                dayTrainings
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor =
                                                            colorScheme.primary,
                                                        contentColor =
                                                            colorScheme.onPrimary
                                                    ),
                                                elevation = ButtonDefaults.buttonElevation(
                                                    defaultElevation = 6.dp,
                                                    pressedElevation = 10.dp
                                                )
                                            ) {
                                                Text(
                                                    text = if (hasSummaryForSelectedDate) {
                                                        tr("קריאת סיכום", "Read training summary")
                                                    } else {
                                                        tr("הוספת סיכום", "Add training summary")
                                                    },
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

            val choiceDate =
                trainingChoiceDate

            if (
                choiceDate != null &&
                trainingChoices.size > 1
            ) {
                AlertDialog(
                    onDismissRequest = {
                        trainingChoiceDate = null
                        trainingChoices = emptyList()
                    },
                    title = {
                        Text(
                            text =
                                tr(
                                    "בחר אימון לסיכום",
                                    "Choose training for summary"
                                ),
                            fontWeight =
                                FontWeight.ExtraBold,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                },
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            trainingChoices
                                .sortedBy {
                                    it.timeText
                                }
                                .forEach { training ->

                                    val branchLabel =
                                        training.displayBranch(
                                            isEnglish
                                        )

                                    val groupLabel =
                                        training.displayGroup(
                                            isEnglish
                                        )

                                    Surface(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDateClick(
                                                        choiceDate,
                                                        training.branch,
                                                        training.group,
                                                        training.timeText
                                                    )

                                                    trainingChoiceDate =
                                                        null

                                                    trainingChoices =
                                                        emptyList()
                                                },
                                        shape =
                                            RoundedCornerShape(16.dp),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,
                                        border =
                                            BorderStroke(
                                                1.dp,
                                                MaterialTheme
                                                    .colorScheme
                                                    .outlineVariant
                                            )
                                    ) {
                                        Column(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 14.dp,
                                                        vertical = 12.dp
                                                    )
                                        ) {
                                            Text(
                                                text =
                                                    "${training.timeText} · $branchLabel",
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .titleSmall,
                                                fontWeight =
                                                    FontWeight.ExtraBold,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            )

                                            Spacer(
                                                Modifier.height(3.dp)
                                            )

                                            Text(
                                                text = groupLabel,
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyMedium,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(
                            onClick = {
                                trainingChoiceDate = null
                                trainingChoices = emptyList()
                            }
                        ) {
                            Text(
                                tr(
                                    "ביטול",
                                    "Cancel"
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                             helpers                                         */
/* -------------------------------------------------------------------------- */

private fun databaseGroupMatchesCalendar(
    selectedGroup: String,
    databaseGroupHe: String,
    databaseGroupEn: String
): Boolean {
    val wanted = TrainingCatalog
        .normalizeGroupName(selectedGroup)
        .ifBlank { selectedGroup }
        .trim()

    val dbHe = TrainingCatalog
        .normalizeGroupName(databaseGroupHe)
        .ifBlank { databaseGroupHe }
        .trim()

    val dbEn = databaseGroupEn.trim()

    if (wanted.equals(dbHe, ignoreCase = true)) return true
    if (selectedGroup.trim().equals(databaseGroupHe.trim(), ignoreCase = true)) return true
    if (selectedGroup.trim().equals(dbEn, ignoreCase = true)) return true

    if (wanted == "נוער" && dbHe == "נוער + בוגרים") return true
    if (wanted == "בוגרים" && dbHe == "נוער + בוגרים") return true

    return false
}

private fun calendarDowFromDatabase(dayOfWeek: String): DayOfWeek {
    return when (dayOfWeek.trim().uppercase(Locale.US)) {
        "SUNDAY" -> DayOfWeek.SUNDAY
        "MONDAY" -> DayOfWeek.MONDAY
        "TUESDAY" -> DayOfWeek.TUESDAY
        "WEDNESDAY" -> DayOfWeek.WEDNESDAY
        "THURSDAY" -> DayOfWeek.THURSDAY
        "FRIDAY" -> DayOfWeek.FRIDAY
        "SATURDAY" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.MONDAY
    }
}

private fun firstDateInMonthForJavaDow(startOfMonth: LocalDate, wanted: DayOfWeek): LocalDate {
    var d = startOfMonth
    while (d.dayOfWeek != wanted) d = d.plusDays(1)
    return d
}

private fun buildMonthlyTrainingItemsFromDatabase(
    ctx: Context,
    ym: YearMonth,
    branch: String,
    group: String,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, List<CalendarTrainingItem>> {
    val dbBranch = KmiDatabaseProvider.branchByName(ctx, branch) ?: return emptyMap()

    val matchingDays = dbBranch.trainingDays.filter { day ->
        databaseGroupMatchesCalendar(
            selectedGroup = group,
            databaseGroupHe = day.groupHe,
            databaseGroupEn = day.groupEn
        )
    }

    if (matchingDays.isEmpty()) return emptyMap()

    val startOfMonth = ym.atDay(1)
    val endOfMonth = ym.atEndOfMonth()
    val out = linkedMapOf<LocalDate, MutableList<CalendarTrainingItem>>()

    matchingDays.forEach { trainingDay ->
        val dow = calendarDowFromDatabase(trainingDay.dayOfWeek)
        var d = firstDateInMonthForJavaDow(startOfMonth, dow)

        while (!d.isAfter(endOfMonth)) {
            if (d !in skipDates) {
                out.getOrPut(d) { mutableListOf() }
                    .add(
                        CalendarTrainingItem(
                            branch = dbBranch.nameHe.ifBlank { branch },
                            group = trainingDay.groupHe.ifBlank { group },
                            timeText = trainingDay.startTime,
                            branchEn = dbBranch.nameEn.ifBlank { branch },
                            groupEn = trainingDay.groupEn.ifBlank { group }
                        )
                    )
            }

            d = d.plusDays(7)
        }
    }

    return out.mapValues { (_, items) ->
        items.sortedBy { it.timeText }
    }
}

private fun buildMonthlyTrainingCountFromDatabase(
    ctx: Context,
    ym: YearMonth,
    branch: String,
    group: String,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, Int> {
    return buildMonthlyTrainingItemsFromDatabase(
        ctx = ctx,
        ym = ym,
        branch = branch,
        group = group,
        skipDates = skipDates
    ).mapValues { (_, items) ->
        items.size
    }
}

private fun mergeMonthlyTrainingItemsFromDatabase(
    ctx: Context,
    ym: YearMonth,
    branches: List<String>,
    groups: List<String>,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, List<CalendarTrainingItem>> {
    val out = linkedMapOf<LocalDate, MutableList<CalendarTrainingItem>>()

    for (b in branches) {
        for (g in groups) {
            val m = buildMonthlyTrainingItemsFromDatabase(
                ctx = ctx,
                ym = ym,
                branch = b,
                group = g,
                skipDates = skipDates
            )

            m.forEach { (date, items) ->
                out.getOrPut(date) { mutableListOf() }.addAll(items)
            }
        }
    }

    return out.mapValues { (_, items) ->
        items.sortedBy { it.timeText }
    }
}

private fun mergeMonthlyTrainingCountsFromDatabase(
    ctx: Context,
    ym: YearMonth,
    branches: List<String>,
    groups: List<String>,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, Int> {
    val out = mutableMapOf<LocalDate, Int>()

    for (b in branches) {
        for (g in groups) {
            val m = buildMonthlyTrainingCountFromDatabase(
                ctx = ctx,
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

/** יוצר מיפוי לכל המופעים החודשיים לפי לו״ז שבועי */
private fun buildMonthlyTrainingItems(
    ym: YearMonth,
    branch: String,
    group: String,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, List<CalendarTrainingItem>> {
    val base = TrainingCatalog.trainingsFor(branch, group)
    if (base.isEmpty()) return emptyMap()

    val startOfMonth = ym.atDay(1)
    val endOfMonth = ym.atEndOfMonth()

    val out = linkedMapOf<LocalDate, MutableList<CalendarTrainingItem>>()

    base.forEach { td ->
        val cal = td.cal
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val first = firstDateInMonthForDow(startOfMonth, dow)

        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val timeText = "%02d:%02d".format(hour, minute)

        var d = first
        while (!d.isAfter(endOfMonth)) {
            if (d !in skipDates) {
                out.getOrPut(d) { mutableListOf() }
                    .add(
                        CalendarTrainingItem(
                            branch = branch,
                            group = group,
                            timeText = timeText
                        )
                    )
            }

            d = d.plusDays(7)
        }
    }

    return out.mapValues { (_, items) ->
        items.sortedBy { it.timeText }
    }
}

/** יוצר ספירה חודשית של אימונים לפי תאריך */
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

private fun mergeMonthlyTrainingItems(
    ym: YearMonth,
    branches: List<String>,
    groups: List<String>,
    skipDates: Set<LocalDate> = emptySet()
): Map<LocalDate, List<CalendarTrainingItem>> {
    val out = linkedMapOf<LocalDate, MutableList<CalendarTrainingItem>>()

    for (b in branches) {
        for (g in groups) {
            val m = buildMonthlyTrainingItems(
                ym = ym,
                branch = b,
                group = g,
                skipDates = skipDates
            )

            m.forEach { (date, items) ->
                out.getOrPut(date) { mutableListOf() }.addAll(items)
            }
        }
    }

    return out.mapValues { (_, items) ->
        items.sortedBy { it.timeText }
    }
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