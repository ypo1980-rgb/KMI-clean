package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.training.TrainingData
import java.lang.reflect.AccessibleObject
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material3.Switch
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.assistant.AiAssistantDialog
// ⭐ Firebase – בשביל הודעות המאמן
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachInfoCard(
    coachName: String?,
    branchName: String?,
    city: String?,
    groupName: String?,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "מאמן: ${coachName.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val branchLine = buildString {
                    if (!city.isNullOrBlank()) append(city).append(" · ")
                    append(branchName.orEmpty())
                }
                Text(
                    text = branchLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "קבוצה: ${groupName.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    clickSound()
                    haptic(true)
                    onOpenProfile()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "פרופיל",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TrainingsWeekHeader(modifier: Modifier = Modifier) {
    val heb = java.util.Locale("he", "IL")
    val dateFmt = java.text.SimpleDateFormat("dd/MM", heb)
    val dayFmt  = java.text.SimpleDateFormat("EEEE", heb)   // שם היום בעברית

    val start = java.util.Calendar.getInstance()
    val end   = (start.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.DAY_OF_YEAR, 6)
    }

    // היום + תאריך: "יום חמישי 11/12"
    val startLabel = "${dayFmt.format(start.time)} ${dateFmt.format(start.time)}"
    val endLabel   = "${dayFmt.format(end.time)} ${dateFmt.format(end.time)}"

    Box(
        modifier = modifier
            .heightIn(min = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 9.dp)
        ) {
            Text(
                text = "אימונים לשבוע הקרוב",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "(תאריכים: $startLabel–$endLabel)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : AccessibleObject> T.makeAccessible(): T {
    try { isAccessible = true } catch (_: SecurityException) { /* ignore */ }
    return this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    trainings: List<TrainingData>,
    onOpenExercise: (String) -> Unit,
    onOpenFreeSessions: (String, String, String, String) -> Unit,
    onOpenMonthlyCalendar: () -> Unit,
    onOpenTrainingSummary: () -> Unit
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()
    // ✅ רוחב אחיד לבועות הטקסט במסך הבית
    val labelWidth = 150.dp

    // 🔵 מצב לדיאלוג העוזר האישי (AI)
    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    val ctxRole = LocalContext.current
    val userSpRole = remember { ctxRole.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    var userRole by remember { mutableStateOf(userSpRole.getString("user_role", "trainee")) }

    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

// ✅ Speed Dial FAB
    var fabExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ נשמור פרמטרים ל-FAB "אימונים חופשיים" מחוץ ל-Column (כדי שיהיו זמינים גם אחרי שה-Column נסגר)
    var freeBranchUi by rememberSaveable { mutableStateOf("") }
    var freeGroupKeyUi by rememberSaveable { mutableStateOf("") }
    var freeUidUi by rememberSaveable { mutableStateOf("") }
    var freeNameUi by rememberSaveable { mutableStateOf("") }

    DisposableEffect(userSpRole) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "user_role") userRole = userSpRole.getString("user_role", "trainee")
        }
        userSpRole.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSpRole.unregisterOnSharedPreferenceChangeListener(l) }
    }

    val isCoach = remember(userRole) {
        when (userRole?.trim()?.lowercase()) {
            "coach", "trainer", "מאמן" -> true
            else -> false
        }
    }

    val backgroundBrush = remember(isCoach) {
        if (isCoach) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141E30),
                    Color(0xFF243B55),
                    Color(0xFF0EA5E9)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4)),
                start = Offset(0f, 0f),
                end = Offset(1000f, 3000f)
            )
        }
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מסך הבית",
                onHome = { /* no-op במסך הבית */ },
                lockHome = true,
                homeDisabledToast = "אתה כבר במסך הבית 🙂",
                showTopHome = false,

                // חיפוש תרגיל מהסרגל התחתון
                onPickSearchResult = { key ->
                    clickSound()
                    haptic(true)
                    pickedKey = key
                },
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start  = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end    = padding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = padding.calculateBottomPadding()
                )
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                val ctx = LocalContext.current
                val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }

                // === KMI_MULTI_GROUPS (FIX) ===
                var groupsCsv by remember(userSp) {
                    mutableStateOf(
                        userSp.getString("age_groups", null)?.takeIf { it.isNotBlank() }
                            ?: userSp.getString("age_group", null)?.takeIf { it.isNotBlank() }
                            ?: userSp.getString("group", null).orEmpty()
                    )
                }

                var coachFromPrefs by remember(userSp) {
                    mutableStateOf(userSp.getString("coach_name", "") ?: "")
                }

                DisposableEffect(userSp) {
                    val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        when (key) {
                            "age_groups", "age_group", "group" -> {
                                groupsCsv =
                                    userSp.getString("age_groups", null)?.takeIf { it.isNotBlank() }
                                        ?: userSp.getString("age_group", null)?.takeIf { it.isNotBlank() }
                                                ?: userSp.getString("group", null).orEmpty()
                            }

                            "coach_name" -> coachFromPrefs =
                                userSp.getString("coach_name", "") ?: ""
                        }
                    }
                    userSp.registerOnSharedPreferenceChangeListener(l)
                    onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
                }

                val groupsEffective: List<String> = remember(groupsCsv) {
                    val raw = groupsCsv
                        .split(',', ';', '|', '\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val normalized = raw.map {
                        il.kmi.app.training.TrainingCatalog.normalizeGroupName(it).ifBlank { it }
                    }
                    (if (normalized.isEmpty()) listOf("בוגרים") else normalized).distinct()
                }
                // === KMI_MULTI_GROUPS (FIX) ===

                // =========================
                // ⭐ הודעות מהמאמן – Firestore (לפי UID)
                // =========================
                val currentUid = remember {
                    FirebaseAuth.getInstance().currentUser?.uid
                }

                var lastCoachMessage by remember { mutableStateOf<String?>(null) }
                var lastCoachFrom by remember { mutableStateOf<String?>(null) }
                var lastCoachSentAt by remember { mutableStateOf<java.util.Date?>(null) }

                DisposableEffect(currentUid) {
                    if (currentUid == null) {
                        lastCoachMessage = null
                        lastCoachFrom = null
                        lastCoachSentAt = null
                        onDispose { }
                    } else {
                        val db = FirebaseFirestore.getInstance()
                        val query = db.collection("coachBroadcasts")
                            .whereArrayContains("targetUids", currentUid)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(1)

                        val reg = query.addSnapshotListener { snap, e ->
                            if (e != null) {
                                // אם יש שגיאה – לא מוחקים את ההודעה האחרונה שכבר מוצגת
                                return@addSnapshotListener
                            }
                            if (snap != null && !snap.isEmpty) {
                                val doc = snap.documents.first()
                                lastCoachMessage = doc.getString("text")
                                lastCoachFrom = doc.getString("coachName")
                                    ?: doc.getString("coach_name")
                                lastCoachSentAt = doc.getTimestamp("createdAt")?.toDate()
                            } else {
                                lastCoachMessage = null
                                lastCoachFrom = null
                                lastCoachSentAt = null
                            }
                        }

                        onDispose { reg.remove() }
                    }
                }
                // =========================

                fun readSelectedBranches(sp: SharedPreferences): List<String> {
                    val fromJsonOrCsv = runCatching {
                        val js = sp.getString("branches_json", null) ?: sp.getString("branches", null)
                        if (!js.isNullOrBlank()) {
                            if (js.trim().startsWith("[")) {
                                val arr = org.json.JSONArray(js)
                                (0 until arr.length()).mapNotNull { arr.optString(it, null) }
                                    .filter { it.isNotBlank() }
                            } else {
                                js.split(',', ';', '|', '\n').map { it.trim() }.filter { it.isNotBlank() }
                            }
                        } else null
                    }.getOrNull()
                    if (!fromJsonOrCsv.isNullOrEmpty()) return fromJsonOrCsv

                    val b1Raw = sp.getString("branch", "")?.trim().orEmpty()
                    val fromBranchCsv =
                        if (b1Raw.contains(',') || b1Raw.contains(';') || b1Raw.contains('|') || b1Raw.contains('\n'))
                            b1Raw.split(',', ';', '|', '\n').map { it.trim() }.filter { it.isNotBlank() }
                        else listOf(b1Raw).filter { it.isNotBlank() }

                    val b2 = sp.getString("branch2", "")?.trim().orEmpty()
                    val b3 = sp.getString("branch3", "")?.trim().orEmpty()

                    return (fromBranchCsv + listOf(b2, b3)).filter { it.isNotBlank() }.distinct()
                }

                val selectedBranches: List<String> = remember(userSp) { readSelectedBranches(userSp) }
                val branchesEffective = if (selectedBranches.isEmpty())
                    listOf("נתניה – מרכז קהילתי אופק")
                else
                    selectedBranches.take(3)
                // ✅ name להצגה + פרמטרים לניווט אימונים חופשיים (נעדכן state כדי שה-FAB יוכל להשתמש גם מחוץ ל-Column)
                val freeName = remember(userSp) {
                    userSp.getString("full_name", null)
                        ?: userSp.getString("name", null)
                        ?: userSp.getString("user_name", null)
                        ?: ""
                }.orEmpty()

                LaunchedEffect(branchesEffective, groupsEffective, currentUid, freeName) {
                    freeBranchUi = branchesEffective.firstOrNull().orEmpty()
                    freeGroupKeyUi = groupsEffective.firstOrNull().orEmpty()
                    freeUidUi = currentUid.orEmpty()
                    freeNameUi = freeName
                }

                data class SlotLike(
                    val dayOfWeek: Int,
                    val startHour: Int,
                    val startMinute: Int,
                    val durationMinutes: Int
                )

                fun <T : java.lang.reflect.AccessibleObject> T.makeAccessibleSafe(): T {
                    try {
                        isAccessible = true
                    } catch (_: SecurityException) {
                        /* ignore */
                    }
                    return this
                }

                fun readSlot(slot: Any): SlotLike {
                    val cls = slot::class.java

                    val dayField = runCatching { cls.getDeclaredField("day").makeAccessibleSafe() }.getOrNull()
                    val startField =
                        runCatching { cls.getDeclaredField("start").makeAccessibleSafe() }.getOrNull()
                    val endField = runCatching { cls.getDeclaredField("end").makeAccessibleSafe() }.getOrNull()

                    if (dayField != null && startField != null && endField != null) {
                        val dayEnum = runCatching { dayField.get(slot) as? java.time.DayOfWeek }.getOrNull()
                        val startLt = runCatching { startField.get(slot) as? java.time.LocalTime }.getOrNull()
                        val endLt = runCatching { endField.get(slot) as? java.time.LocalTime }.getOrNull()

                        val calDay = when (dayEnum) {
                            java.time.DayOfWeek.SUNDAY -> java.util.Calendar.SUNDAY
                            java.time.DayOfWeek.MONDAY -> java.util.Calendar.MONDAY
                            java.time.DayOfWeek.TUESDAY -> java.util.Calendar.TUESDAY
                            java.time.DayOfWeek.WEDNESDAY -> java.util.Calendar.WEDNESDAY
                            java.time.DayOfWeek.THURSDAY -> java.util.Calendar.THURSDAY
                            java.time.DayOfWeek.FRIDAY -> java.util.Calendar.FRIDAY
                            java.time.DayOfWeek.SATURDAY -> java.util.Calendar.SATURDAY
                            else -> java.util.Calendar.MONDAY
                        }

                        val durMin = if (startLt != null && endLt != null)
                            java.time.Duration.between(startLt, endLt).toMinutes().toInt()
                        else 90

                        return SlotLike(
                            dayOfWeek = calDay,
                            startHour = startLt?.hour ?: 19,
                            startMinute = startLt?.minute ?: 0,
                            durationMinutes = durMin
                        )
                    }

                    fun intField(vararg names: String, fallback: Int): Int {
                        for (n in names) {
                            val v = runCatching {
                                val f = cls.getDeclaredField(n).makeAccessibleSafe()
                                (f.get(slot) as? Number)?.toInt()
                            }.getOrNull()
                            if (v != null) return v
                        }
                        return fallback
                    }

                    return SlotLike(
                        dayOfWeek = intField("dayOfWeek", "day", "dow", fallback = java.util.Calendar.MONDAY),
                        startHour = intField("startHour", "hour", "h", fallback = 19),
                        startMinute = intField("startMinute", "minute", "min", "startMin", fallback = 0),
                        durationMinutes = intField("durationMinutes", "duration", "dur", "length", fallback = 90)
                    )
                }

                Text(
                    text = "מסך הבית",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(56.dp))

                if (isCoach) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        TrainingsWeekHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7F00FF), Color(0xFF3F51B5))
                                )
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        TrainingsWeekHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCCFFFFFF),
                                    Color(0x66FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(Modifier.height(10.dp))

                fun rollForwardIfPast(src: java.util.Calendar, graceMinutes: Int = 60): java.util.Calendar {
                    val now = System.currentTimeMillis() - graceMinutes * 60_000L
                    val c = src.clone() as java.util.Calendar
                    while (c.timeInMillis <= now) c.add(java.util.Calendar.DAY_OF_YEAR, 7)
                    return c
                }

                fun datesRange(
                    from: java.time.LocalDate,
                    to: java.time.LocalDate
                ): Sequence<java.time.LocalDate> =
                    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }

                val HOLIDAYS_BY_YEAR: Map<Int, Set<java.time.LocalDate>> = remember {
                    buildMap {
                        put(
                            2025,
                            buildSet {
                                datesRange(
                                    java.time.LocalDate.of(2025, 9, 22),
                                    java.time.LocalDate.of(2025, 9, 24)
                                ).forEach { add(it) }
                                datesRange(
                                    java.time.LocalDate.of(2025, 10, 1),
                                    java.time.LocalDate.of(2025, 10, 2)
                                ).forEach { add(it) }
                                datesRange(
                                    java.time.LocalDate.of(2025, 10, 6),
                                    java.time.LocalDate.of(2025, 10, 14)
                                ).forEach { add(it) }
                            }
                        )
                        put(
                            2026,
                            buildSet {
                                datesRange(
                                    java.time.LocalDate.of(2026, 4, 1),
                                    java.time.LocalDate.of(2026, 4, 8)
                                ).forEach { add(it) }
                                add(java.time.LocalDate.of(2026, 4, 13))
                                add(java.time.LocalDate.of(2026, 4, 22))
                                datesRange(
                                    java.time.LocalDate.of(2026, 5, 21),
                                    java.time.LocalDate.of(2026, 5, 22)
                                ).forEach { add(it) }
                            }
                        )
                    }
                }

                fun java.util.Calendar.toLocalDate(): java.time.LocalDate =
                    this.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                fun isBlockedHolidayDate(cal: java.util.Calendar): Boolean {
                    val d = cal.toLocalDate()
                    val set = HOLIDAYS_BY_YEAR[d.year] ?: emptySet()
                    return d in set
                }

                val upcoming: List<TrainingData> =
                    remember(branchesEffective, groupsEffective, coachFromPrefs) {
                        val all = mutableListOf<TrainingData>()

                        branchesEffective.forEach { branchName ->
                            val parts = branchName.split('–', '-').map { it.trim() }
                            val city = parts.getOrNull(0) ?: branchName
                            val venue = parts.getOrNull(1) ?: ""

                            val addr =
                                il.kmi.app.training.TrainingCatalog.addressFor(branchName) ?: ""
                                    .ifBlank {
                                        if (city.isNotBlank() && venue.isNotBlank()) "$venue, $city" else branchName
                                    }
                            val place = il.kmi.app.training.TrainingCatalog.placeFor(branchName)

                            groupsEffective.forEach { grp ->
                                val sched =
                                    il.kmi.app.training.TrainingDirectory.getSchedule(branchName, grp)
                                val coach =
                                    sched?.coachName?.takeIf { it.isNotBlank() }
                                        ?: coachFromPrefs.takeIf { it.isNotBlank() }
                                        ?: "איציק ביטון"

                                val branchItems: List<TrainingData> =
                                    sched?.slots?.map { slotAny ->
                                        val s = readSlot(slotAny)
                                        TrainingData.nextWeekly(
                                            dayOfWeek = s.dayOfWeek,
                                            startHour = s.startHour,
                                            startMinute = s.startMinute,
                                            durationMinutes = s.durationMinutes,
                                            place = place,
                                            address = addr,
                                            coach = coach
                                        )
                                    } ?: emptyList()

                                all += branchItems
                                    .map { it.copy(cal = rollForwardIfPast(it.cal, 60)) }
                                    .filterNot { isBlockedHolidayDate(it.cal) }
                            }
                        }

                        all.distinctBy { it.cal.timeInMillis.toString() + "|" + it.address }
                            .sortedBy { it.cal.timeInMillis }
                            .take(4)
                    }

                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp)
                ) {

                    if (upcoming.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "אין אימונים קרובים",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        items(
                            items = upcoming,
                            key = { it.cal.timeInMillis }
                        ) { training ->
                            TrainingCardCompact(training = training)
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xCCFFFFFF),
                                            Color(0x66FFFFFF),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // ===== כרטיס הודעות מהמאמן – מעודכן להציג הודעה אחרונה =====
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("הודעות מהמאמן", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))

                                val msg = lastCoachMessage?.trim()
                                if (msg.isNullOrEmpty()) {
                                    Text(
                                        "אין הודעות חדשות כרגע",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val shortMsg = if (msg.length > 140) {
                                        msg.take(140) + "..."
                                    } else msg

                                    Text(
                                        text = shortMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    val metaLine = remember(lastCoachFrom, lastCoachSentAt) {
                                        val from =
                                            lastCoachFrom?.takeIf { it.isNotBlank() } ?: "המאמן"
                                        val timeStr = lastCoachSentAt?.let {
                                            val fmt = java.text.SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm",
                                                java.util.Locale("he", "IL")
                                            )
                                            fmt.format(it)
                                        } ?: ""
                                        if (timeStr.isNotBlank()) "$from · $timeStr" else from
                                    }

                                    Text(
                                        text = metaLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Button(
                        onClick = {
                            clickSound()
                            haptic(true)
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "מעבר לבחירת חגורה", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(2.dp))
            }

            // ===============================
// ✅ Speed-Dial FAB (עוזר קולי + אימונים חופשיים)
// ===============================
            val mainIcon = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add

// (אופציונלי) שכבת לחיצה לסגירה כשפתוח
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable { fabExpanded = false }
                )
            }

// כפתורים קטנים שנפתחים מעל הכפתור הראשי
            androidx.compose.animation.AnimatedVisibility(
                visible = fabExpanded,
                modifier = Modifier
                    // ✅ ב-RTL: Start = ימין, End = שמאל
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 150.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // ✅ ב-RTL: Alignment.Start = ימין
                    horizontalAlignment = Alignment.Start
                ) {

                    // =========================
                    // 🎤 עוזר קולי
                    // =========================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // ✅ ב-RTL: Arrangement.Start = ימין
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // ICON – צמוד לימין
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    fabExpanded = false
                                    showAiDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "עוזר קולי",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // LABEL – משמאל לאייקון
                        Surface(
                            onClick = {
                                clickSound()
                                haptic(true)
                                fabExpanded = false
                                showAiDialog = true
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.Black.copy(alpha = 0.06f)
                            )
                        ) {
                            Text(
                                text = "עוזר קולי",
                                modifier = Modifier
                                    .width(labelWidth)                 // ✅ NEW
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF0B1220),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,                          // ✅ מומלץ
                                overflow = TextOverflow.Ellipsis       // ✅ מומלץ
                            )
                        }
                    }

                    // =========================
                    // 📅 לוח אימונים חודשי
                    // =========================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    fabExpanded = false
                                    onOpenMonthlyCalendar()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = "לוח אימונים חודשי",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Surface(
                            onClick = {
                                clickSound()
                                haptic(true)
                                fabExpanded = false
                                onOpenMonthlyCalendar()
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.Black.copy(alpha = 0.06f)
                            )
                        ) {
                            Text(
                                text = "לוח אימונים חודשי",
                                modifier = Modifier
                                    .width(labelWidth)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF0B1220),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // =========================
                    // 📝 סיכום אימון
                    // =========================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    fabExpanded = false
                                    onOpenTrainingSummary()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditNote,
                                    contentDescription = "סיכום אימון",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Surface(
                            onClick = {
                                clickSound()
                                haptic(true)
                                fabExpanded = false
                                onOpenTrainingSummary()
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.Black.copy(alpha = 0.06f)
                            )
                        ) {
                            Text(
                                text = "סיכום אימון",
                                modifier = Modifier
                                    .width(labelWidth)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF0B1220),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // =========================
                    // ➕ אימונים חופשיים
                    // =========================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // ICON – צמוד לימין
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    fabExpanded = false
                                    onOpenFreeSessions(
                                        freeBranchUi,
                                        freeGroupKeyUi,
                                        freeUidUi,
                                        freeNameUi
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "אימונים חופשיים",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // LABEL – משמאל לאייקון
                        Surface(
                            onClick = {
                                clickSound()
                                haptic(true)
                                fabExpanded = false
                                onOpenFreeSessions(
                                    freeBranchUi,
                                    freeGroupKeyUi,
                                    freeUidUi,
                                    freeNameUi
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.Black.copy(alpha = 0.06f)
                            )
                        ) {
                            Text(
                                text = "אימונים חופשיים",
                                modifier = Modifier
                                    .width(labelWidth)                 // ✅ NEW
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color(0xFF0B1220),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,                          // ✅ מומלץ
                                overflow = TextOverflow.Ellipsis       // ✅ מומלץ
                            )
                        }
                    }
                }
            }

// הכפתור הראשי (הגדול) – תמיד מוצג
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 80.dp)
                    .size(58.dp)
                    .border(width = 3.dp, color = Color.White, shape = CircleShape)
            ) {
                IconButton(
                    onClick = {
                        clickSound()
                        haptic(true)
                        fabExpanded = !fabExpanded
                    }
                ) {
                    Icon(
                        imageVector = mainIcon,
                        contentDescription = "פעולות מהירות",
                        tint = Color.White
                    )
                }
            }

// ===== דיאלוג תרגיל מהחיפוש =====
            pickedKey?.let { key ->
                val (belt, topic, item) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

                val explanation = remember(belt, item) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic
                    )
                }

                var isFav by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { pickedKey = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "(${belt.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    isFav = !isFav
                                },
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "מועדף",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים",
                                    )
                                }
                            }
                        }
                    },
                    text = {
                        val annotated = buildExplanationWithStanceHighlight(
                            source = explanation,
                            stanceColor = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                clickSound()
                                haptic(true)
                                pickedKey = null
                            }
                        ) {
                            Text("סגור")
                        }
                    }
                )
            }
        }
    }

    // 🔊 דיאלוג העוזר הקולי – מחוץ ל-Box כדי להיות מעל כל המסך
    if (showAiDialog) {
        AiAssistantDialog(
            onDismiss = { showAiDialog = false },
            contextLabel = "מסך הבית"
        )
    }
}


/* ========= עזר: לפרק מפתח חיפוש "belt|topic|item" ========= */
private fun parseSearchKey(key: String): Triple<Belt, String, String> {
    val parts = when {
        "|" in key  -> key.split("|", limit = 3)
        "::" in key -> key.split("::", limit = 3)
        "/" in key  -> key.split("/", limit = 3)
        else        -> listOf("", "", "")
    }.let { (it + listOf("", "", "")).take(3) }

    val belt  = Belt.fromId(parts[0]) ?: Belt.WHITE
    val topic = parts[1]
    val item  = parts[2]
    return Triple(belt, topic, item)
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')
        .replace('־', '-')
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = il.kmi.app.domain.Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
}

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        append(before)

        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        append(after)
    }
}

/** כרטיס אימון קומפקטי – כמו לפני השינוי, עם סדר אייקונים ישן */
@Composable
private fun TrainingCardCompact(
    training: TrainingData
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showNavPicker by rememberSaveable(training.cal.timeInMillis) { mutableStateOf(false) }

    val navPref: NavAppPref = remember(training.cal.timeInMillis) {
        readNavPref(ctx)
    }

    fun onNavigateClick() {
        clickSound()
        haptic(true)

        val safeAddress = training.address?.trim().orEmpty()
        if (safeAddress.isBlank()) return

        when (navPref) {
            NavAppPref.ASK -> showNavPicker = true
            NavAppPref.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavAppPref.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    if (showNavPicker) {
        AlertDialog(
            onDismissRequest = { showNavPicker = false },
            title = {
                Text(
                    text = "פתיחה באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "בחר אפליקציה לניווט. אפשר גם לשמור כברירת מחדל.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var rememberChoice by rememberSaveable(training.cal.timeInMillis) { mutableStateOf(true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("זכור בחירה", modifier = Modifier.padding(end = 8.dp))
                        Switch(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.WAZE)
                                    openWaze(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("וייז", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.GOOGLE_MAPS)
                                    openGoogleMaps(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("גוגל מפות", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            writeNavPref(ctx, NavAppPref.ASK)
                            showNavPicker = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("כל פעם לשאול")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNavPicker = false }) { Text("סגור") }
            }
        )
    }

    val durationMin: Int = remember(training) {
        fun readIntField(vararg names: String, fallback: Int): Int {
            val cls = training::class.java
            for (n in names) {
                val v = runCatching {
                    val f = cls.getDeclaredField(n).apply { isAccessible = true }
                    (f.get(training) as? Number)?.toInt()
                }.getOrNull()
                if (v != null) return v
            }
            return fallback
        }
        readIntField("durationMinutes", "durationMinuets", "duration", "dur", fallback = 90)
    }

    val locale = java.util.Locale("he", "IL")
    val dayText = remember(training.cal.timeInMillis) {
        java.text.SimpleDateFormat("EEEE", locale).format(training.cal.time)
    }
    val dateText = remember(training.cal.timeInMillis) {
        java.text.SimpleDateFormat("dd/MM/yyyy", locale).format(training.cal.time)
    }
    val timeText = remember(training.cal.timeInMillis, durationMin) {
        val fmt = java.text.SimpleDateFormat("HH:mm", locale)
        val start = fmt.format(training.cal.time)
        val end   = fmt.format(java.util.Date(training.cal.timeInMillis + durationMin * 60_000L))
        "$start – $end"
    }

    @Composable
    fun MapChip(
        label: String,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 72.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val branchLine = remember(training.place, training.address) {
                training.place?.takeIf { it.isNotBlank() } ?: run {
                    val parts = training.address
                        .split('–', '-', ',', '·')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (parts.size >= 2) parts[1] else parts.firstOrNull().orEmpty()
                }
            }
            Text(
                text = branchLine,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationChip(
                    address = training.address,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
// ===============================
// ✅ ניווט: כפתור אחד + דיאלוג בחירה יפה
// ===============================

private enum class NavChoice { GOOGLE_MAPS, WAZE }

@Composable
private fun NavigationChip(
    address: String?,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showPicker by rememberSaveable(address) { mutableStateOf(false) }
    var rememberChoice by rememberSaveable(address) { mutableStateOf(true) }

    val safeAddress = address?.trim().orEmpty()

    fun open(choice: NavChoice) {
        if (safeAddress.isBlank()) return
        when (choice) {
            NavChoice.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavChoice.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    Surface(
        onClick = {
            clickSound()
            haptic(true)
            if (safeAddress.isNotBlank()) showPicker = true
        },
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
        ),
        modifier = modifier
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אייקון
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_maps),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ניווט",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1220),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (safeAddress.isBlank()) "אין כתובת" else safeAddress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.Person, // אם אתה רוצה חץ במקום, תגיד ואחליף
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showPicker) {
        NavPickerDialog(
            address = safeAddress,
            rememberChoice = rememberChoice,
            onRememberChoiceChange = { rememberChoice = it },
            onPick = { picked ->
                // אם בעתיד תרצה לשמור העדפה ב-SP — אפשר כאן.
                open(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun NavPickerDialog(
    address: String,
    rememberChoice: Boolean,
    onRememberChoiceChange: (Boolean) -> Unit,
    onPick: (NavChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "ניווט באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "זכור בחירה",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        androidx.compose.material3.Switch(
                            checked = rememberChoice,
                            onCheckedChange = onRememberChoiceChange
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    val btnHeight = 52.dp

                    // Waze
                    Surface(
                        onClick = { onPick(NavChoice.WAZE) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_waze),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Waze",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Google Maps
                    Surface(
                        onClick = { onPick(NavChoice.GOOGLE_MAPS) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_maps),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "גוגל מפות",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "אפשר לשמור בחירה כברירת מחדל (בהמשך נחבר את זה להגדרות).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("סגור") }
        }
    )
}

// ===== העדפת ניווט (Google Maps / Waze / Ask) =====

private enum class NavAppPref { ASK, GOOGLE_MAPS, WAZE }

private const val NAV_PREFS_FILE = "kmi_user"
private const val NAV_PREF_KEY = "nav_app_pref"

private fun readNavPref(ctx: Context): NavAppPref {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    return when (sp.getString(NAV_PREF_KEY, "ask")?.lowercase()) {
        "gmaps", "google", "google_maps" -> NavAppPref.GOOGLE_MAPS
        "waze" -> NavAppPref.WAZE
        else -> NavAppPref.ASK
    }
}

private fun writeNavPref(ctx: Context, pref: NavAppPref) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    val v = when (pref) {
        NavAppPref.ASK -> "ask"
        NavAppPref.GOOGLE_MAPS -> "gmaps"
        NavAppPref.WAZE -> "waze"
    }
    sp.edit().putString(NAV_PREF_KEY, v).apply()
}

// ===== עזרי ניווט מפות =====
private fun openGoogleMaps(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val mapsPkg = "com.google.android.apps.maps"

    // ✅ אם Maps מותקן – לפתוח ישר
    if (isPackageInstalled(ctx, mapsPkg)) {
        val navUri = Uri.parse("google.navigation:q=" + Uri.encode(safeAddress))
        val i = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage(mapsPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(i) }
        return
    }

    // ❗ Maps לא מותקן → לפתוח Play Store בלי chooser של חנויות
    openPlayStoreDirect(ctx, mapsPkg)
}

private fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
    return runCatching {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrElse { false }
}

private fun openPlayStoreDirect(ctx: Context, pkg: String) {
    val playPkg = "com.android.vending" // Google Play

    // ניסיון 1: לפתוח Play Store ישירות (בלי לשאול Galaxy Store)
    val marketUri = Uri.parse("market://details?id=$pkg")
    val playIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        setPackage(playPkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (playIntent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(playIntent)
        return
    }

    // ניסיון 2: fallback לדפדפן (אם אין Play Store)
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
    ctx.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun clearNavPref(ctx: Context) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    sp.edit().remove(NAV_PREF_KEY).apply()   // או putString(NAV_PREF_KEY, "ask")
}

private fun openWaze(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val wazeUri = Uri.parse("https://waze.com/ul?q=" + Uri.encode(safeAddress))
    val intent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
        setPackage("com.waze")
    }

    if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
    } else {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
    }
}
