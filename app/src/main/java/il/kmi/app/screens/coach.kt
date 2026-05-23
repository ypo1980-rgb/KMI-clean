@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.app.Application
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import android.app.Activity
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.material3.rememberDatePickerState

//=========================================================================

data class CoachDateEntry(
    val date: String = "",
    val description: String = ""
)

private fun nextCoachDateItemName(
    sectionTitle: String,
    index: Int
): String {
    return when (sectionTitle) {
        "השתלמויות" -> "השתלמות $index"
        "מחנות אימונים" -> "מחנה אימונים $index"
        "הסמכות" -> "הסמכה $index"
        else -> "פריט $index"
    }
}

data class TraineeProfile(
    val id: String,
    val fullName: String,
    val belt: String,
    val seniority: String,
    val age: Int,
    val attendancePct: Int = 0,
    val branch: String = "",
    val groupKey: String = "",

    // ✅ מזהה מסמך אמיתי ב-Firestore: users/{userDocId}
    // כל שמירה של תאריכים / הערות תתבצע לפיו ולא לפי שם.
    val userDocId: String = "",

    val beltAwardDates: Map<String, String> = emptyMap(),

    // ✅ הערות מאמן — נשמרות בשרת תחת users/{docId}.coachNotes
    val coachNotes: String = "",

    // ✅ שדות נוספים למילוי ע"י המאמן ונשמרים בשרת: תאריך + תיאור
    val seminarDates: Map<String, CoachDateEntry> = emptyMap(),
    val campDates: Map<String, CoachDateEntry> = emptyMap(),
    val certificationDates: Map<String, CoachDateEntry> = emptyMap()
)

data class GroupStatsUi(
    val totalTrainees: Int,
    val filteredTrainees: Int,
    val avgAge: Int,
    val avgAttendance: Int,
    val beltCounts: Map<String, Int>,
    val highAttendanceCount: Int
)

private fun parseYearsFromSeniority(value: String): Int? {
    val digits = Regex("""\d+""").find(value)?.value ?: return null
    return digits.toIntOrNull()
}

private fun beltColorForStats(belt: String): Color {
    val normalized = belt.trim()

    return when {
        normalized.contains("לבנ") -> Color(0xFFE5E7EB)
        normalized.contains("צהוב") -> Color(0xFFFACC15)
        normalized.contains("כתומ") -> Color(0xFFF97316)
        normalized.contains("ירוק") -> Color(0xFF22C55E)
        normalized.contains("כחול") -> Color(0xFF3B82F6)
        normalized.contains("חומ") -> Color(0xFF8B5A2B)
        normalized.contains("שחור") -> Color(0xFF111111)
        else -> Color(0xFF7C3AED)
    }
}

private fun coachTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun coachTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private fun coachHorizontalAlignment(isEnglish: Boolean): Alignment.Horizontal =
    if (isEnglish) Alignment.Start else Alignment.End

private fun coachBeltNameForUi(
    beltName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return beltName

    return when (beltName.trim()) {
        "לבנה" -> "White"
        "צהובה" -> "Yellow"
        "כתומה" -> "Orange"
        "ירוקה" -> "Green"
        "כחולה" -> "Blue"
        "חומה" -> "Brown"
        "שחורה" -> "Black"
        "ללא דרגה" -> "No rank"
        "חגורה לבנה" -> "White"
        "חגורה צהובה" -> "Yellow"
        "חגורה כתומה" -> "Orange"
        "חגורה ירוקה" -> "Green"
        "חגורה כחולה" -> "Blue"
        "חגורה חומה" -> "Brown"
        "חגורה שחורה" -> "Black"
        else -> beltName
    }
}

private fun coachSectionTitleForUi(
    title: String,
    isEnglish: Boolean
): String {
    return when (title) {
        "השתלמויות" -> coachTr(isEnglish, "השתלמויות", "Seminars")
        "מחנות אימונים" -> coachTr(isEnglish, "מחנות אימונים", "Training camps")
        "הסמכות" -> coachTr(isEnglish, "הסמכות", "Certifications")
        else -> title
    }
}

private fun coachDateItemNameForUi(
    itemName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return itemName

    val number = Regex("""\d+""").find(itemName)?.value.orEmpty()

    return when {
        itemName.startsWith("השתלמות") -> "Seminar $number"
        itemName.startsWith("מחנה אימונים") -> "Training camp $number"
        itemName.startsWith("הסמכה") -> "Certification $number"
        else -> itemName
    }
}

private fun coachDateSectionIcon(title: String): String {
    return when (title) {
        "השתלמויות" -> "🎓"
        "מחנות אימונים" -> "👥"
        "הסמכות" -> "🏅"
        else -> "⌄"
    }
}

private fun coachDateSectionAccent(title: String): Color {
    return when (title) {
        "השתלמויות" -> Color(0xFF7C3AED)
        "מחנות אימונים" -> Color(0xFF2563EB)
        "הסמכות" -> Color(0xFF0891B2)
        else -> Color(0xFF6D56B8)
    }
}

private fun coachDateToMillis(dateText: String): Long? {
    return runCatching {
        val date = LocalDate.parse(dateText.trim())
        date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun coachMillisToDateText(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private fun buildGroupStats(profiles: List<TraineeProfile>, filtered: List<TraineeProfile>): GroupStatsUi {
    val avgAge = filtered
        .map { it.age }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.roundToInt()
        ?: 0

    val avgAttendance = filtered
        .map { it.attendancePct }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.roundToInt()
        ?: 0

    val beltCounts = filtered
        .groupingBy { it.belt.ifBlank { "ללא דרגה" } }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .toMap()

    val highAttendanceCount = filtered.count { it.attendancePct >= 80 }

    return GroupStatsUi(
        totalTrainees = profiles.size,
        filteredTrainees = filtered.size,
        avgAge = avgAge,
        avgAttendance = avgAttendance,
        beltCounts = beltCounts,
        highAttendanceCount = highAttendanceCount
    )
}

@Composable
fun CoachTraineesScreen(
    branch: String = "",
    groupKey: String = "",
    onBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = { il.kmi.app.ui.DrawerBridge.open() },
    onOpenHome: () -> Unit = onBack
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    val sp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    val role = sp.getString("user_role", "trainee").orEmpty()

    val isCoachRole = remember(role) {
        val cleanRole = role.trim().lowercase()
        cleanRole == "coach" ||
                cleanRole.contains("coach") ||
                cleanRole.contains("trainer") ||
                cleanRole.contains("instructor") ||
                cleanRole.contains("מאמן") ||
                cleanRole.contains("מדריך")
    }

    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenTextAlign = coachTextAlign(isEnglish)
    val screenHorizontalAlignment = coachHorizontalAlignment(isEnglish)

    // --- branch / groupKey שנעשה בהם שימוש בפועל ---
    var effectiveBranch by remember { mutableStateOf(branch) }
    var effectiveGroupKey by remember { mutableStateOf(groupKey) }

    // מנסים קודם מה-SharedPreferences, ואם לא – מה-Firestore (users/{uid})
    LaunchedEffect(Unit) {
        // 1. SharedPreferences – תומך במפתחות הישנים והחדשים
        if (effectiveBranch.isBlank()) {
            effectiveBranch =
                sp.getString("active_branch", null)
                    ?: sp.getString("activeBranch", null)
                            ?: sp.getString("branch", null)
                            ?: sp.getString("branchesCsv", null)
                            ?: sp.getString("coach_branch", null)
                            ?: sp.getString("selected_branch", null)
                            ?: sp.getString("current_branch", null)
                            ?: ""
        }

        if (effectiveGroupKey.isBlank()) {
            effectiveGroupKey =
                sp.getString("active_group", null)
                    ?: sp.getString("activeGroup", null)
                            ?: sp.getString("primaryGroup", null)
                            ?: sp.getString("groupKey", null)
                            ?: sp.getString("group_key", null)
                            ?: sp.getString("age_group", null)
                            ?: sp.getString("group", null)
                            ?: sp.getString("coach_groupKey", null)
                            ?: sp.getString("selected_groupKey", null)
                            ?: sp.getString("current_groupKey", null)
                            ?: ""
        }

        // 2. אם עדיין חסר – שליפה מ-Firestore users/{uid}
        if (effectiveBranch.isBlank() || effectiveGroupKey.isBlank()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    val snap = Firebase.firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()

                    if (effectiveBranch.isBlank()) {
                        val branchesList = snap.get("branches") as? List<*>
                        val branchesFromList = branchesList
                            ?.mapNotNull { it?.toString()?.trim() }
                            ?.firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        effectiveBranch =
                            snap.getString("activeBranch")
                                ?: snap.getString("active_branch")
                                        ?: snap.getString("branch")
                                        ?: snap.getString("branchesCsv")
                                        ?: snap.getString("coachBranch")
                                        ?: branchesFromList
                    }

                    if (effectiveGroupKey.isBlank()) {
                        val groupsList = snap.get("groups") as? List<*>
                        val groupFromList = groupsList
                            ?.mapNotNull { it?.toString()?.trim() }
                            ?.firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        effectiveGroupKey =
                            snap.getString("activeGroup")
                                ?: snap.getString("active_group")
                                        ?: snap.getString("primaryGroup")
                                        ?: snap.getString("groupKey")
                                        ?: snap.getString("group_key")
                                        ?: snap.getString("age_group")
                                        ?: snap.getString("group")
                                        ?: snap.getString("coachGroupKey")
                                        ?: groupFromList
                    }
                } catch (_: Exception) {
                    // במקרה של שגיאה לא מפילים את האפליקציה – פשוט נשארים עם מה שיש
                }
            }
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF141E30),
                Color(0xFF243B55),
                Color(0xFF0EA5E9)
            )
        )
    }

    // --- טעינת מתאמנים אמיתיים + חישוב אחוז נוכחות לכל מתאמן ---
    val app = ctx.applicationContext as Application
    val repo = remember(app) { AttendanceRepository.get(app) }

    var traineeProfiles by remember { mutableStateOf<List<TraineeProfile>>(emptyList()) }
    var isProfilesLoading by remember { mutableStateOf(true) }
    var didFinishInitialProfilesLoad by remember { mutableStateOf(false) }

    // מונע הצגת "לא נמצאו מתאמנים" לפני שהסנכרון הראשוני באמת הסתיים
    var isInitialServerSyncRunning by remember { mutableStateOf(true) }

    // ✅ אם effectiveBranch מגיע כ-CSV ("סניף1, סניף2") – עובדים בפועל עם הסניף הראשון
    val effectiveBranchPrimary = remember(effectiveBranch) {
        effectiveBranch
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: effectiveBranch.trim()
    }

    LaunchedEffect(effectiveBranch, effectiveGroupKey) {
        isProfilesLoading = true
        isInitialServerSyncRunning = true
        didFinishInitialProfilesLoad = false

        fun String.norm(): String = this
            .trim()
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        val groupName = effectiveGroupKey.norm()
        if (groupName.isBlank()) {
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
            return@LaunchedEffect
        }

        // candidate keys (כולל CSV + סניף ראשון + dash/en-dash)
        val fullBranch = effectiveBranch.norm()
        val primaryBranch = effectiveBranchPrimary.norm()

        val branchKeys = listOf(fullBranch, primaryBranch)
            .flatMap { b ->
                listOf(
                    b,
                    b.replace("-", "–"),
                    b.replace("–", "-")
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // ✅ בוחרים מפתח DB שבאמת יש בו נתונים (כדי ש-% נוכחות לא יהיה 0 סתם)
        suspend fun pickDbBranchKey(): String {
            val today = LocalDate.now()
            for (k in branchKeys) {
                val members = repo.members(k, groupName).firstOrNull().orEmpty()
                if (members.isNotEmpty()) return k

                val rec = repo.attendanceForDay(k, groupName, today).firstOrNull().orEmpty()
                if (rec.isNotEmpty()) return k
            }
            return branchKeys.firstOrNull().orEmpty()
        }

        val branchDbKey = pickDbBranchKey()
        if (branchDbKey.isBlank()) {
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
            return@LaunchedEffect
        }

        // --- סנכרון אוטומטי מ-Firestore (users) לטבלת group_members ---
        var serverHasPotentialMembers = false

        try {
            val existingMembers = repo.members(branchDbKey, groupName).firstOrNull().orEmpty()
            val existingNames = existingMembers.map { it.displayName.trim() }.toSet()

            suspend fun fetchUsersByBranchesArray(cand: String) =
                Firebase.firestore.collection("users")
                    .whereArrayContains("branches", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            suspend fun fetchUsersByBranchesCsv(cand: String) =
                Firebase.firestore.collection("users")
                    .whereEqualTo("branchesCsv", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            suspend fun fetchUsersBySingleBranch(cand: String) =
                Firebase.firestore.collection("users")
                    .whereEqualTo("branch", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            var usersSnap =
                runCatching { fetchUsersByBranchesArray(branchKeys.first()) }.getOrNull()

            if (usersSnap == null || usersSnap.isEmpty) {
                for (cand in branchKeys.drop(1)) {
                    val tmp = runCatching { fetchUsersByBranchesArray(cand) }.getOrNull()
                    if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersByBranchesCsv(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersByBranchesCsv(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
                    }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersBySingleBranch(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersBySingleBranch(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
                    }
                }
            }

            val docs = usersSnap?.documents.orEmpty()
            serverHasPotentialMembers = docs.isNotEmpty()

            for (doc in docs) {
                val fullName = doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: continue

                if (fullName.trim() in existingNames) continue

                repo.addMember(
                    branch = branchDbKey,
                    groupKey = groupName,
                    displayName = fullName.trim()
                )
            }
        } catch (_: Exception) {
        } finally {
            isInitialServerSyncRunning = false
        }

        // --- מאזינים ל-DB המקומי ובונים TraineeProfile "עשיר" (כולל Firestore) ---
        repo.members(branchDbKey, groupName).collectLatest { members: List<GroupMember> ->
            if (members.isEmpty()) {
                if (serverHasPotentialMembers || isInitialServerSyncRunning) {
                    isProfilesLoading = true
                    didFinishInitialProfilesLoad = false
                    return@collectLatest
                }

                traineeProfiles = emptyList()
                isProfilesLoading = false
                didFinishInitialProfilesLoad = true
                return@collectLatest
            }

            // יש members, אבל עדיין לא בנינו את traineeProfiles.
            // לכן ממשיכים להציג טעינה ולא מציגים "לא נמצאו מתאמנים".
            isProfilesLoading = true
            didFinishInitialProfilesLoad = false

            // 1) אחוז נוכחות מה-DB המקומי
            val today = LocalDate.now()
            val from = today.minusDays(59)
            val statsMap = mutableMapOf<Long, Pair<Int, Int>>() // mid -> (present,total)

            var d = from
            while (!d.isAfter(today)) {
                val records = repo.attendanceForDay(branchDbKey, groupName, d).firstOrNull().orEmpty()
                for (record in records) {
                    val mid = extractMemberId(record) ?: continue
                    val status = recordStatus(record) ?: continue
                    val (p, t) = statsMap[mid] ?: (0 to 0)
                    statsMap[mid] = (p + if (status == AttendanceStatus.PRESENT) 1 else 0) to (t + 1)
                }
                d = d.plusDays(1)
            }

            // 2) פרטים מ-Firestore (belt + birthDate -> age) לפי שם, עם נרמול קשוח
            fun String.normKey(): String = this
                .trim()
                .replace('־', '-')   // maqaf
                .replace('–', '-')   // en-dash
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

            fun beltHeb(b: String): String {
                return when (b.trim().lowercase(Locale.US)) {
                    "white" -> "לבנה"
                    "yellow" -> "צהובה"
                    "orange" -> "כתומה"
                    "green" -> "ירוקה"
                    "blue" -> "כחולה"
                    "brown" -> "חומה"
                    "black" -> "שחורה"
                    else -> b // אם תרצה להציג באנגלית כמו שהוא
                }
            }

            fun ageFromBirthDate(birthDate: String?): Int {
                // birthDate אצלך: "1980-10-13"
                val s = birthDate?.trim().orEmpty()
                if (s.isBlank()) return 0
                return runCatching {
                    val dob = java.time.LocalDate.parse(s) // ISO-8601
                    val now = java.time.LocalDate.now()
                    java.time.Period.between(dob, now).years
                }.getOrDefault(0)
            }

            data class FireUserInfo(
                val userDocId: String,
                val age: Int,
                val beltHeb: String,
                val seniority: String,
                val beltAwardDates: Map<String, String>,
                val coachNotes: String,
                val seminarDates: Map<String, CoachDateEntry>,
                val campDates: Map<String, CoachDateEntry>,
                val certificationDates: Map<String, CoachDateEntry>
            )

            val userInfoByName = mutableMapOf<String, FireUserInfo>() // nameKey -> full user info

            fun String.normProfileKey(): String = this
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

            fun userDocMatchesBranchAndGroup(
                doc: com.google.firebase.firestore.DocumentSnapshot,
                branchCandidates: List<String>,
                groupCandidate: String
            ): Boolean {
                val branchSet = branchCandidates
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val docBranches = buildList {
                    doc.getString("branch")?.let { add(it) }
                    doc.getString("activeBranch")?.let { add(it) }
                    doc.getString("active_branch")?.let { add(it) }
                    doc.getString("branchesCsv")?.split(",")?.forEach { add(it) }
                    (doc.get("branches") as? List<*>)?.forEach { item ->
                        item?.toString()?.let { add(it) }
                    }
                }
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }

                val groupNorm = groupCandidate.normProfileKey()

                val docGroups = buildList {
                    doc.getString("primaryGroup")?.let { add(it) }
                    doc.getString("activeGroup")?.let { add(it) }
                    doc.getString("active_group")?.let { add(it) }
                    doc.getString("groupKey")?.let { add(it) }
                    doc.getString("group_key")?.let { add(it) }
                    doc.getString("group")?.let { add(it) }
                    doc.getString("age_group")?.let { add(it) }
                    (doc.get("groups") as? List<*>)?.forEach { item ->
                        item?.toString()?.let { add(it) }
                    }
                }
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }

                val branchMatches =
                    branchSet.isEmpty() ||
                            docBranches.any { docBranch ->
                                docBranch in branchSet ||
                                        branchSet.any { candidate ->
                                            candidate.length >= 3 &&
                                                    docBranch.length >= 3 &&
                                                    (docBranch.contains(candidate) || candidate.contains(docBranch))
                                        }
                            }

                val groupMatches =
                    groupNorm.isBlank() ||
                            docGroups.any { docGroup ->
                                docGroup == groupNorm ||
                                        docGroup.contains(groupNorm) ||
                                        groupNorm.contains(docGroup)
                            }

                return branchMatches && groupMatches
            }

            val userDocs = runCatching {
                val directDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

                for (branchCandidate in branchKeys) {
                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereArrayContains("branches", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }

                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereEqualTo("branch", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }

                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereEqualTo("branchesCsv", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }
                }

                val distinctDirect = directDocs.distinctBy { it.id }

                if (distinctDirect.isNotEmpty()) {
                    distinctDirect
                } else {
                    Firebase.firestore.collection("users")
                        .whereEqualTo("role", "trainee")
                        .get()
                        .await()
                        .documents
                        .filter { doc ->
                            userDocMatchesBranchAndGroup(
                                doc = doc,
                                branchCandidates = branchKeys,
                                groupCandidate = groupName
                            )
                        }
                }
            }.getOrNull().orEmpty()

            for (doc in userDocs) {
                val n = (doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: continue).normKey()

                val beltRaw = doc.getString("belt").orEmpty()
                val age = ageFromBirthDate(doc.getString("birthDate"))
                val seniority =
                    doc.getString("seniority")
                        ?: doc.getString("trainingSeniority")
                        ?: doc.getString("yearsTraining")
                        ?: ""

                fun readStringMap(fieldName: String): Map<String, String> {
                    val raw = doc.get(fieldName) as? Map<*, *> ?: emptyMap<Any, Any>()
                    return raw.entries.associate { entry ->
                        entry.key.toString() to entry.value.toString()
                    }
                }

                fun readCoachEntryMap(fieldName: String): Map<String, CoachDateEntry> {
                    val raw = doc.get(fieldName) as? Map<*, *> ?: emptyMap<Any, Any>()

                    return raw.entries.associate { entry ->
                        val key = entry.key.toString()
                        val value = entry.value

                        val parsed = when (value) {
                            is Map<*, *> -> CoachDateEntry(
                                date = value["date"]?.toString().orEmpty(),
                                description = value["description"]?.toString().orEmpty()
                            )

                            // ✅ תאימות לאחור: אם בעבר נשמר רק תאריך כמחרוזת
                            is String -> CoachDateEntry(
                                date = value,
                                description = ""
                            )

                            else -> CoachDateEntry()
                        }

                        key to parsed
                    }
                }

                val beltAwardDates = readStringMap("beltAwardDates")
                val coachNotes = doc.getString("coachNotes").orEmpty()
                val seminarDates = readCoachEntryMap("seminarDates")
                val campDates = readCoachEntryMap("campDates")
                val certificationDates = readCoachEntryMap("certificationDates")

                userInfoByName[n] = FireUserInfo(
                    userDocId = doc.id,
                    age = age,
                    beltHeb = beltHeb(beltRaw),
                    seniority = seniority,
                    beltAwardDates = beltAwardDates,
                    coachNotes = coachNotes,
                    seminarDates = seminarDates,
                    campDates = campDates,
                    certificationDates = certificationDates
                )
            }

            val builtProfiles = members.map { m ->
                val stat = statsMap[m.id]
                val pct = if (stat != null && stat.second > 0) {
                    ((stat.first * 100.0) / stat.second).toInt()
                } else 0

                val key = m.displayName.normKey()
                val info = userInfoByName[key]
                val userDocId = info?.userDocId.orEmpty()
                val age = info?.age ?: 0
                val belt = info?.beltHeb.orEmpty()
                val seniority = info?.seniority.orEmpty()
                val beltAwardDates = info?.beltAwardDates ?: emptyMap()
                val coachNotes = info?.coachNotes.orEmpty()
                val seminarDates = info?.seminarDates ?: emptyMap()
                val campDates = info?.campDates ?: emptyMap()
                val certificationDates = info?.certificationDates ?: emptyMap()

                TraineeProfile(
                    id = m.id.toString(),
                    fullName = m.displayName,
                    belt = belt,
                    seniority = seniority,
                    age = age,
                    attendancePct = pct,
                    branch = branchDbKey,
                    groupKey = groupName,
                    userDocId = userDocId,
                    beltAwardDates = beltAwardDates,
                    coachNotes = coachNotes,
                    seminarDates = seminarDates,
                    campDates = campDates,
                    certificationDates = certificationDates
                )
            }

            traineeProfiles = builtProfiles
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
        }
    }


    // אם זה לא מאמן – עדיין רוצים טופ-בר עם אייקונים
    if (!isCoachRole) {

        Scaffold(
            topBar = {
                val contextLang = LocalContext.current
                val langManager = remember { AppLanguageManager(contextLang) }

                KmiTopBar(
                    title = coachTr(isEnglish, "רשימת המתאמנים", "Trainees list"),
                    onOpenDrawer = onOpenDrawer,
                    onHome = onOpenHome,
                    showTopHome = false,
                    showRoleStatus = false,
                    lockSearch = true,
                    showBottomActions = true,
                    currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                    onToggleLanguage = {
                        val newLang =
                            if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        langManager.setLanguage(newLang)
                        (contextLang as? Activity)?.recreate()
                    }
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coachTr(isEnglish, "המסך זמין למאמנים בלבד", "This screen is available for coaches only"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
        return
    }

    val uiProfiles = remember(traineeProfiles) {
        traineeProfiles
    }

    // בחירה נוכחית
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected: TraineeProfile? = uiProfiles.firstOrNull { it.id == selectedId }
        ?: uiProfiles.firstOrNull()

    // הערות מאמן לפי מתאמן
    val coachNotes = remember { mutableStateMapOf<String, String>() }

    // תאריכי קבלת חגורות לפי מתאמן
    val beltAwardDatesState = remember { mutableStateMapOf<String, Map<String, String>>() }

    // ✅ שדות נוספים למילוי ע"י המאמן לפי מתאמן: תאריך + תיאור
    val seminarDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val campDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val certificationDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }

    var showStatsSheet by remember { mutableStateOf(false) }

    val groupStats = remember(uiProfiles) {
        buildGroupStats(uiProfiles, uiProfiles)
    }

    val screenScope = rememberCoroutineScope()

    suspend fun resolveUserDocIdForSelected(
        selectedProfile: TraineeProfile
    ): String {
        val directDocId = selectedProfile.userDocId.trim()
        if (directDocId.isNotBlank()) return directDocId

        fun String.normSaveKey(): String = this
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale("he", "IL"))

        val targetName = selectedProfile.fullName.normSaveKey()
        val targetBranch = selectedProfile.branch.normSaveKey()
        val targetGroup = selectedProfile.groupKey.normSaveKey()

        val docs = Firebase.firestore.collection("users")
            .whereEqualTo("role", "trainee")
            .get()
            .await()
            .documents

        val matched = docs.firstOrNull { doc ->
            val docName = (
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: ""
                    ).normSaveKey()

            val docGroups = (doc.get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.normSaveKey() }
                .orEmpty()

            val docBranches = buildList {
                doc.getString("branch")?.let { add(it.normSaveKey()) }
                doc.getString("branchesCsv")?.split(",")?.forEach { add(it.normSaveKey()) }
                (doc.get("branches") as? List<*>)?.forEach { item ->
                    item?.toString()?.let { add(it.normSaveKey()) }
                }
            }

            val nameMatches = docName == targetName
            val groupMatches = targetGroup.isBlank() || targetGroup in docGroups
            val branchMatches = targetBranch.isBlank() || docBranches.any { branch ->
                branch == targetBranch ||
                        branch.contains(targetBranch) ||
                        targetBranch.contains(branch)
            }

            nameMatches && groupMatches && branchMatches
        } ?: docs.firstOrNull { doc ->
            val docName = (
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: ""
                    ).normSaveKey()

            docName == targetName
        }

        val resolvedDocId = matched?.id.orEmpty()

        if (resolvedDocId.isBlank()) {
            error("Missing userDocId for trainee: ${selectedProfile.fullName}")
        }

        return resolvedDocId
    }

    suspend fun saveBeltAwardDatesForSelected(
        selectedProfile: TraineeProfile,
        dates: Map<String, String>
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanedDates = dates
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }

        if (cleanedDates.isEmpty()) return

        val updates = cleanedDates.entries.associate { (beltName, dateValue) ->
            "beltAwardDates.$beltName" to dateValue
        }

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(updates)
            .await()
    }

    suspend fun saveCoachDateSectionForSelected(
        selectedProfile: TraineeProfile,
        firestoreFieldName: String,
        entries: Map<String, CoachDateEntry>
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanedEntries = entries
            .mapValues { (_, value) ->
                mapOf(
                    "date" to value.date.trim(),
                    "description" to value.description.trim()
                )
            }
            .filterValues { value ->
                value["date"].orEmpty().isNotBlank() ||
                        value["description"].orEmpty().isNotBlank()
            }

        if (cleanedEntries.isEmpty()) return

        val updates = cleanedEntries.entries.associate { (itemName, value) ->
            "$firestoreFieldName.$itemName" to value
        }

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(updates)
            .await()
    }

    suspend fun saveCoachNotesForSelected(
        selectedProfile: TraineeProfile,
        note: String
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanNote = note.trim()

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(
                mapOf(
                    "coachNotes" to cleanNote,
                    "coachNotesUpdatedAtMillis" to System.currentTimeMillis()
                )
            )
            .await()
    }

    LaunchedEffect(uiProfiles) {
        if (selectedId == null && uiProfiles.isNotEmpty()) {
            selectedId = uiProfiles.first().id
        } else if (selectedId != null && uiProfiles.isNotEmpty() && uiProfiles.none { it.id == selectedId }) {
            selectedId = uiProfiles.firstOrNull()?.id
        }

        uiProfiles.forEach { trainee ->
            if (beltAwardDatesState[trainee.id] == null) {
                beltAwardDatesState[trainee.id] = trainee.beltAwardDates
            }

            if (coachNotes[trainee.id] == null) {
                coachNotes[trainee.id] = trainee.coachNotes
            }

            if (seminarDatesState[trainee.id] == null) {
                seminarDatesState[trainee.id] = trainee.seminarDates
            }

            if (campDatesState[trainee.id] == null) {
                campDatesState[trainee.id] = trainee.campDates
            }

            if (certificationDatesState[trainee.id] == null) {
                certificationDatesState[trainee.id] = trainee.certificationDates
            }
        }
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (showStatsSheet) {
                    coachTr(isEnglish, "סטטיסטיקת קבוצה", "Group statistics")
                } else {
                    coachTr(isEnglish, "רשימת המתאמנים", "Trainees list")
                },
                onOpenDrawer = onOpenDrawer,
                onHome = onOpenHome,
                showTopHome = false,
                showRoleStatus = false,
                lockSearch = true,
                showBottomActions = true,
                currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        bottomBar = {
            if (!showStatsSheet && !isKeyboardVisible) {
                var statsBubbleOffset by remember { mutableStateOf(0f) }

                LaunchedEffect(Unit) {
                    while (true) {
                        androidx.compose.animation.core.animate(
                            initialValue = -120f,
                            targetValue = 320f,
                            animationSpec = androidx.compose.animation.core.tween(2600)
                        ) { value, _ ->
                            statsBubbleOffset = value
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Surface(
                        onClick = { showStatsSheet = true },
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                            .graphicsLayer {
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF7F00FF),
                                            Color(0xFF3F51B5),
                                            Color(0xFF03A9F4)
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = statsBubbleOffset.dp)
                                    .size(140.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Assessment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = coachTr(isEnglish, "סטטיסטיקה לקבוצה", "Group statistics"),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = if (showStatsSheet || isKeyboardVisible) 16.dp else 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    // כרטיס רשימת מתאמנים
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Divider()

                            if (effectiveBranch.isBlank() || effectiveGroupKey.isBlank()) {
                                Text(
                                    text = coachTr(
                                        isEnglish,
                                        "לא אותרו סניף או קבוצה עבור המאמן.",
                                        "No branch or group was found for this coach."
                                    ),
                                    color = Color(0xFFB91C1C),
                                    textAlign = screenTextAlign,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            } else if (isProfilesLoading || isInitialServerSyncRunning || !didFinishInitialProfilesLoad) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = coachTr(
                                            isEnglish,
                                            "טוען מתאמנים מהשרת...",
                                            "Loading trainees from the server..."
                                        ),
                                        color = Color(0xFF0369A1),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else if (uiProfiles.isEmpty() && didFinishInitialProfilesLoad && !isProfilesLoading && !isInitialServerSyncRunning) {
                                Text(
                                    text = coachTr(
                                        isEnglish,
                                        "לא נמצאו מתאמנים פעילים לסניף ולקבוצה שנבחרו.",
                                        "No active trainees were found for the selected branch and group."
                                    ),
                                    color = Color(0xFF64748B),
                                    textAlign = screenTextAlign,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            } else if (uiProfiles.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 210.dp)
                                ) {
                                    items(uiProfiles, key = { it.id }) { trainee ->
                                        val isSelected = selectedId == trainee.id

                                        Surface(
                                            color = if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
                                            tonalElevation = 0.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF7DD3FC)) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedId = trainee.id }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = trainee.fullName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (isSelected) Color(0xFF0C4A6E) else Color.Unspecified
                                                    )

                                                    val metaLine = buildList {
                                                        if (trainee.belt.isNotBlank()) {
                                                            add(coachBeltNameForUi(trainee.belt, isEnglish))
                                                        }
                                                        if (trainee.branch.isNotBlank()) {
                                                            add(coachTr(isEnglish, "סניף: ${trainee.branch}", "Branch: ${trainee.branch}"))
                                                        }
                                                        if (trainee.groupKey.isNotBlank()) {
                                                            add(coachTr(isEnglish, "קבוצה: ${trainee.groupKey}", "Group: ${trainee.groupKey}"))
                                                        }
                                                    }.joinToString(" • ")

                                                    if (metaLine.isNotBlank()) {
                                                        Text(
                                                            text = metaLine,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (isSelected) Color(0xFF0369A1) else Color(0xFF616161)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiProfiles.isNotEmpty()) {
                    item {
                        // כרטיס פרטי מתאמן
                        Surface(
                            color = Color.White,
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selected == null) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        coachTr(isEnglish, "בחר מתאמן מהרשימה למעלה", "Select a trainee from the list above"),
                                        color = Color(0xFF757575),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = screenHorizontalAlignment
                                ) {
                                    Text(
                                        text = selected.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color(0xFF212121),
                                        textAlign = screenTextAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Divider()

                                    LabeledField(
                                        label = coachTr(isEnglish, "גיל", "Age"),
                                        value = if (selected.age > 0) "${selected.age}" else "—",
                                        isEnglish = isEnglish
                                    )
                                    LabeledField(
                                        label = coachTr(isEnglish, "ותק", "Seniority"),
                                        value = selected.seniority.ifBlank { "—" },
                                        isEnglish = isEnglish
                                    )
                                    LabeledField(
                                        label = coachTr(isEnglish, "דרגה", "Rank"),
                                        value = coachBeltNameForUi(selected.belt.ifBlank { "—" }, isEnglish),
                                        isEnglish = isEnglish
                                    )
                                    LabeledField(
                                        label = coachTr(isEnglish, "סניף", "Branch"),
                                        value = selected.branch.ifBlank { "—" },
                                        isEnglish = isEnglish
                                    )
                                    LabeledField(
                                        label = coachTr(isEnglish, "קבוצה", "Group"),
                                        value = selected.groupKey.ifBlank { "—" },
                                        isEnglish = isEnglish
                                    )
                                    LabeledField(
                                        label = coachTr(isEnglish, "אחוז נוכחות (60 ימים אחרונים)", "Attendance rate — last 60 days"),
                                        value = if (selected.attendancePct > 0) "${selected.attendancePct}%" else "—",
                                        isEnglish = isEnglish
                                    )

                                    Divider()

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        var isBeltDatesSectionExpanded by remember(selected.id) {
                                            mutableStateOf(false)
                                        }

                                        PremiumCoachCompactSectionHeader(
                                            title = coachTr(isEnglish, "תאריכי קבלת חגורות", "Belt award dates"),
                                            subtitle = if (isBeltDatesSectionExpanded) {
                                                coachTr(isEnglish, "עדכון תאריכים לפי חגורה", "Update dates by belt")
                                            } else {
                                                coachTr(isEnglish, "לחצו לפתיחת רשימת החגורות", "Tap to open the belt list")
                                            },
                                            iconText = "📅",
                                            isExpanded = isBeltDatesSectionExpanded,
                                            accent = Color(0xFF6D56B8),
                                            isEnglish = isEnglish,
                                            onClick = {
                                                    isBeltDatesSectionExpanded = !isBeltDatesSectionExpanded
                                                }
                                            )

                                            if (isBeltDatesSectionExpanded) {
                                            val beltOrder = listOf(
                                                "צהובה",
                                                "כתומה",
                                                "ירוקה",
                                                "כחולה",
                                                "חומה",
                                                "שחורה"
                                            )

                                            val beltAccentMap = mapOf(
                                                "צהובה" to Color(0xFFFACC15),
                                                "כתומה" to Color(0xFFF97316),
                                                "ירוקה" to Color(0xFF22C55E),
                                                "כחולה" to Color(0xFF3B82F6),
                                                "חומה" to Color(0xFF8B5A2B),
                                                "שחורה" to Color(0xFF111111)
                                            )

                                            val selectedDates = beltAwardDatesState[selected.id] ?: emptyMap()
                                            var expandedBelt by remember(selected.id) { mutableStateOf<String?>(null) }

                                            beltOrder.forEach { beltName ->
                                                val beltAccent = beltAccentMap[beltName] ?: Color(0xFF6366F1)
                                                val currentDate = selectedDates[beltName].orEmpty()
                                                val hasDate = currentDate.isNotBlank()
                                                val isExpanded = expandedBelt == beltName

                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(20.dp),
                                                    shadowElevation = 3.dp,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        beltAccent.copy(alpha = 0.18f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            expandedBelt = if (isExpanded) null else beltName
                                                        }
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .clip(CircleShape)
                                                                    .background(beltAccent)
                                                            )

                                                            Spacer(Modifier.width(8.dp))

                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isEnglish) {
                                                                        coachBeltNameForUi(beltName, true)
                                                                    } else {
                                                                        "חגורה $beltName"
                                                                    },
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF1F2937)
                                                                )

                                                                Text(
                                                                    text = if (hasDate) {
                                                                        coachTr(isEnglish, "תאריך קבלה: $currentDate", "Award date: $currentDate")
                                                                    } else {
                                                                        coachTr(isEnglish, "אין תאריך קבלה", "No award date")
                                                                    },
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = if (hasDate) Color(0xFF0F766E) else Color(0xFF94A3B8)
                                                                )
                                                            }

                                                            Icon(
                                                                imageVector = if (hasDate) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                                contentDescription = null,
                                                                tint = if (hasDate) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                            )

                                                            Spacer(Modifier.width(6.dp))

                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = null,
                                                                tint = Color(0xFF64748B)
                                                            )
                                                        }

                                                        if (isExpanded) {
                                                            var showBeltDatePicker by remember(selected.id, beltName) {
                                                                mutableStateOf(false)
                                                            }

                                                            val datePickerState = rememberDatePickerState(
                                                                initialSelectedDateMillis = coachDateToMillis(currentDate)
                                                                    ?: System.currentTimeMillis()
                                                            )

                                                            Surface(
                                                                onClick = {
                                                                    showBeltDatePicker = true
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = Color(0xFFF8FAFC),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    beltAccent.copy(alpha = 0.75f)
                                                                ),
                                                                shadowElevation = 2.dp
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                    horizontalAlignment = coachHorizontalAlignment(isEnglish)
                                                                ) {
                                                                    Text(
                                                                        text = coachTr(isEnglish, "תאריך קבלה", "Award date"),
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = beltAccent,
                                                                        fontWeight = FontWeight.Bold,
                                                                        textAlign = coachTextAlign(isEnglish),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )

                                                                    Text(
                                                                        text = currentDate.ifBlank {
                                                                            coachTr(
                                                                                isEnglish,
                                                                                "בחר תאריך מלוח השנה",
                                                                                "Choose a date from calendar"
                                                                            )
                                                                        },
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        color = if (currentDate.isBlank()) {
                                                                            Color(0xFF64748B)
                                                                        } else {
                                                                            Color(0xFF0F172A)
                                                                        },
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        textAlign = coachTextAlign(isEnglish),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                }
                                                            }

                                                            if (showBeltDatePicker) {
                                                                DatePickerDialog(
                                                                    onDismissRequest = {
                                                                        showBeltDatePicker = false
                                                                    },
                                                                    confirmButton = {
                                                                        TextButton(
                                                                            onClick = {
                                                                                val selectedMillis =
                                                                                    datePickerState.selectedDateMillis

                                                                                if (selectedMillis != null) {
                                                                                    val newDate =
                                                                                        coachMillisToDateText(selectedMillis)

                                                                                    val current = beltAwardDatesState[selected.id]
                                                                                        .orEmpty()
                                                                                        .toMutableMap()

                                                                                    current[beltName] = newDate
                                                                                    beltAwardDatesState[selected.id] = current
                                                                                }

                                                                                showBeltDatePicker = false
                                                                            }
                                                                        ) {
                                                                            Text(coachTr(isEnglish, "אישור", "OK"))
                                                                        }
                                                                    },
                                                                    dismissButton = {
                                                                        TextButton(
                                                                            onClick = {
                                                                                showBeltDatePicker = false
                                                                            }
                                                                        ) {
                                                                            Text(coachTr(isEnglish, "ביטול", "Cancel"))
                                                                        }
                                                                    }
                                                                ) {
                                                                    DatePicker(
                                                                        state = datePickerState,
                                                                        title = {
                                                                            Text(
                                                                                text = coachTr(
                                                                                    isEnglish,
                                                                                    "בחר תאריך קבלת חגורה",
                                                                                    "Choose belt award date"
                                                                                ),
                                                                                modifier = Modifier.padding(
                                                                                    start = 24.dp,
                                                                                    end = 24.dp,
                                                                                    top = 16.dp
                                                                                )
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            var isSavingBeltDates by remember(selected.id) { mutableStateOf(false) }
                                            var beltDatesSaveMessage by remember(selected.id) { mutableStateOf<String?>(null) }

                                            Surface(
                                                onClick = {
                                                    val selectedProfile = selected
                                                    if (selectedProfile != null && !isSavingBeltDates) {
                                                        val datesToSave = beltAwardDatesState[selectedProfile.id]
                                                            .orEmpty()
                                                            .filterValues { it.isNotBlank() }

                                                        if (datesToSave.isEmpty()) {
                                                            beltDatesSaveMessage = coachTr(isEnglish, "אין תאריכים לשמירה", "No dates to save")
                                                            return@Surface
                                                        }

                                                        screenScope.launch {
                                                            isSavingBeltDates = true
                                                            beltDatesSaveMessage = null

                                                            runCatching {
                                                                saveBeltAwardDatesForSelected(
                                                                    selectedProfile = selectedProfile,
                                                                    dates = datesToSave
                                                                )
                                                            }.onSuccess {
                                                                beltDatesSaveMessage = coachTr(isEnglish, "תאריכי החגורות נשמרו", "Belt dates saved")
                                                            }.onFailure {
                                                                beltDatesSaveMessage = coachTr(
                                                                    isEnglish,
                                                                    "שמירת תאריכי החגורות נכשלה",
                                                                    "Failed to save belt dates"
                                                                )
                                                            }

                                                            isSavingBeltDates = false
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color.Transparent,
                                                shadowElevation = 6.dp,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            brush = Brush.horizontalGradient(
                                                                colors = listOf(
                                                                    Color(0xFF7C3AED),
                                                                    Color(0xFF6366F1),
                                                                    Color(0xFF0EA5E9)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .padding(vertical = 14.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (isSavingBeltDates) {
                                                            coachTr(isEnglish, "שומר...", "Saving...")
                                                        } else {
                                                            coachTr(isEnglish, "שמור תאריכי חגורות", "Save belt dates")
                                                        },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                                beltDatesSaveMessage?.let { msg ->
                                                    Surface(
                                                        color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) {
                                                            Color(0xFFDCFCE7)
                                                        } else {
                                                            Color(0xFFFEE2E2)
                                                        },
                                                        shape = RoundedCornerShape(14.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = msg,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) Color(0xFF166534) else Color(0xFF991B1B),
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                    }

                                    CoachDateSectionCard(
                                        title = "השתלמויות",
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת השתלמויות", "Tap to open the seminar list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור להשתלמויות שהמתאמן עבר", "Add a date and description for seminars completed by the trainee"),
                                        defaultItems = listOf(
                                            "השתלמות 1",
                                            "השתלמות 2",
                                            "השתלמות 3"
                                        ),
                                        selectedId = selected.id,
                                        stateMap = seminarDatesState,
                                        firestoreFieldName = "seminarDates",
                                        selectedProfile = selected,
                                        screenScope = screenScope,
                                        isEnglish = isEnglish,
                                        onSave = ::saveCoachDateSectionForSelected
                                    )

                                    CoachDateSectionCard(
                                        title = "מחנות אימונים",
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת מחנות אימונים", "Tap to open the training camp list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור למחנות אימונים שבהם המתאמן השתתף", "Add a date and description for training camps attended by the trainee"),
                                        defaultItems = listOf(
                                            "מחנה אימונים 1",
                                            "מחנה אימונים 2",
                                            "מחנה אימונים 3"
                                        ),
                                        selectedId = selected.id,
                                        stateMap = campDatesState,
                                        firestoreFieldName = "campDates",
                                        selectedProfile = selected,
                                        screenScope = screenScope,
                                        isEnglish = isEnglish,
                                        onSave = ::saveCoachDateSectionForSelected
                                    )

                                    CoachDateSectionCard(
                                        title = "הסמכות",
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת הסמכות", "Tap to open the certification list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור להסמכות שהמתאמן קיבל", "Add a date and description for certifications received by the trainee"),
                                        defaultItems = listOf(
                                            "הסמכה 1",
                                            "הסמכה 2",
                                            "הסמכה 3"
                                        ),
                                        selectedId = selected.id,
                                        stateMap = certificationDatesState,
                                        firestoreFieldName = "certificationDates",
                                        selectedProfile = selected,
                                        screenScope = screenScope,
                                        isEnglish = isEnglish,
                                        onSave = ::saveCoachDateSectionForSelected
                                    )

                                    var isSavingCoachNotes by remember(selected.id) {
                                        mutableStateOf(false)
                                    }

                                    var coachNotesSaveMessage by remember(selected.id) {
                                        mutableStateOf<String?>(null)
                                    }

                                    OutlinedTextField(
                                        value = coachNotes[selected.id] ?: "",
                                        onValueChange = {
                                            coachNotes[selected.id] = it
                                            coachNotesSaveMessage = null
                                        },
                                        label = {
                                            Text(coachTr(isEnglish, "הערות מאמן", "Coach notes"))
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            textAlign = screenTextAlign
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    Surface(
                                        onClick = {
                                            val selectedProfile = selected
                                            if (selectedProfile != null && !isSavingCoachNotes) {
                                                val noteToSave = coachNotes[selectedProfile.id].orEmpty()

                                                screenScope.launch {
                                                    isSavingCoachNotes = true
                                                    coachNotesSaveMessage = null

                                                    runCatching {
                                                        saveCoachNotesForSelected(
                                                            selectedProfile = selectedProfile,
                                                            note = noteToSave
                                                        )
                                                    }.onSuccess {
                                                        coachNotesSaveMessage = coachTr(
                                                            isEnglish,
                                                            "הערות המאמן נשמרו",
                                                            "Coach notes saved"
                                                        )
                                                    }.onFailure {
                                                        coachNotesSaveMessage = coachTr(
                                                            isEnglish,
                                                            "שמירת הערות המאמן נכשלה",
                                                            "Failed to save coach notes"
                                                        )
                                                    }

                                                    isSavingCoachNotes = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.Transparent,
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF0F766E),
                                                            Color(0xFF0891B2),
                                                            Color(0xFF2563EB)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isSavingCoachNotes) {
                                                    coachTr(isEnglish, "שומר...", "Saving...")
                                                } else {
                                                    coachTr(isEnglish, "שמור הערות מאמן", "Save coach notes")
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    coachNotesSaveMessage?.let { msg ->
                                        Surface(
                                            color = if (
                                                msg.contains("נשמרו") ||
                                                msg.contains("saved", ignoreCase = true)
                                            ) {
                                                Color(0xFFDCFCE7)
                                            } else {
                                                Color(0xFFFEE2E2)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (
                                                    msg.contains("נשמרו") ||
                                                    msg.contains("saved", ignoreCase = true)
                                                ) {
                                                    Color(0xFF166534)
                                                } else {
                                                    Color(0xFF991B1B)
                                                },
                                                textAlign = screenTextAlign,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showStatsSheet) {
                CoachGroupStatsPremiumScreen(
                    stats = groupStats,
                    profiles = uiProfiles,
                    isEnglish = isEnglish,
                    onClose = { showStatsSheet = false }
                )
            }
        }
    }
}

@Composable
private fun PremiumCoachCompactSectionHeader(
    title: String,
    subtitle: String,
    iconText: String,
    isExpanded: Boolean,
    accent: Color,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFFCFDFF),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFE7ECF7)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.035f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.08f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                )

                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7A879A)
                    )
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CoachDateSectionCard(
    title: String,
    collapsedSubtitle: String,
    expandedSubtitle: String,
    defaultItems: List<String>,
    selectedId: String,
    stateMap: MutableMap<String, Map<String, CoachDateEntry>>,
    firestoreFieldName: String,
    selectedProfile: TraineeProfile,
    screenScope: kotlinx.coroutines.CoroutineScope,
    isEnglish: Boolean,
    onSave: suspend (
        selectedProfile: TraineeProfile,
        firestoreFieldName: String,
        entries: Map<String, CoachDateEntry>
    ) -> Unit
) {
    var isExpanded by remember(selectedId, title) { mutableStateOf(false) }
    var expandedItem by remember(selectedId, title) { mutableStateOf<String?>(null) }
    var isSaving by remember(selectedId, title) { mutableStateOf(false) }
    var saveMessage by remember(selectedId, title) { mutableStateOf<String?>(null) }

    val selectedEntries = stateMap[selectedId].orEmpty()

    val dynamicItems = remember(selectedEntries, defaultItems, title) {
        val mergedItems = (defaultItems + selectedEntries.keys)
            .distinct()
            .toMutableList()

        val lastItemName = mergedItems.lastOrNull()
        val lastEntry = lastItemName?.let { selectedEntries[it] }

        val shouldAddNextRow =
            lastItemName != null &&
                    lastEntry != null &&
                    (
                            lastEntry.date.isNotBlank() ||
                                    lastEntry.description.isNotBlank()
                            )

        if (shouldAddNextRow) {
            mergedItems += nextCoachDateItemName(title, mergedItems.size + 1)
        }

        mergedItems
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val sectionIcon = coachDateSectionIcon(title)
        val sectionAccent = coachDateSectionAccent(title)
        val sectionTitleUi = coachSectionTitleForUi(title, isEnglish)

            PremiumCoachCompactSectionHeader(
                title = sectionTitleUi,
                subtitle = if (isExpanded) expandedSubtitle else collapsedSubtitle,
                iconText = sectionIcon,
                isExpanded = isExpanded,
                accent = sectionAccent,
                isEnglish = isEnglish,
                onClick = {
                    isExpanded = !isExpanded
                }
            )

            if (isExpanded) {
                dynamicItems.forEach { itemName ->
                    val currentEntry = selectedEntries[itemName] ?: CoachDateEntry()
                    val hasContent =
                        currentEntry.date.isNotBlank() ||
                                currentEntry.description.isNotBlank()

                    val isItemExpanded = expandedItem == itemName
                    val accent = Color(0xFF6366F1)

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 3.dp,
                        border = BorderStroke(
                            1.dp,
                            accent.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedItem = if (isItemExpanded) null else itemName
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = coachDateItemNameForUi(itemName, isEnglish),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = when {
                                            currentEntry.date.isNotBlank() &&
                                                    currentEntry.description.isNotBlank() ->
                                                coachTr(
                                                    isEnglish,
                                                    "תאריך: ${currentEntry.date} • ${currentEntry.description}",
                                                    "Date: ${currentEntry.date} • ${currentEntry.description}"
                                                )

                                            currentEntry.date.isNotBlank() ->
                                                coachTr(
                                                    isEnglish,
                                                    "תאריך: ${currentEntry.date}",
                                                    "Date: ${currentEntry.date}"
                                                )

                                            currentEntry.description.isNotBlank() ->
                                                currentEntry.description

                                            else -> coachTr(isEnglish, "אין מידע", "No information")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasContent) Color(0xFF0F766E) else Color(0xFF94A3B8),
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Icon(
                                    imageVector = if (hasContent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (hasContent) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )

                                Spacer(Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isItemExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }

                            if (isItemExpanded) {
                                OutlinedTextField(
                                    value = currentEntry.date,
                                    onValueChange = { newValue ->
                                        val current = stateMap[selectedId]
                                            .orEmpty()
                                            .toMutableMap()

                                        val oldEntry = current[itemName] ?: CoachDateEntry()
                                        current[itemName] = oldEntry.copy(date = newValue)
                                        stateMap[selectedId] = current
                                    },
                                    label = { Text(coachTr(isEnglish, "תאריך", "Date")) },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = coachTextAlign(isEnglish)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )

                                OutlinedTextField(
                                    value = currentEntry.description,
                                    onValueChange = { newValue ->
                                        val current = stateMap[selectedId]
                                            .orEmpty()
                                            .toMutableMap()

                                        val oldEntry = current[itemName] ?: CoachDateEntry()
                                        current[itemName] = oldEntry.copy(description = newValue)
                                        stateMap[selectedId] = current
                                    },
                                    label = { Text(coachTr(isEnglish, "תיאור", "Description")) },
                                    placeholder = {
                                        Text(
                                            coachTr(
                                                isEnglish,
                                                "לדוגמה: השתלמות מדריכים / מחנה קיץ / הסמכת עוזר מדריך",
                                                "Example: instructor seminar / summer camp / assistant instructor certification"
                                            ),
                                            textAlign = coachTextAlign(isEnglish),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = coachTextAlign(isEnglish)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = {
                        if (!isSaving) {
                            val entriesToSave = stateMap[selectedId].orEmpty()
                                .filterValues { entry ->
                                    entry.date.isNotBlank() || entry.description.isNotBlank()
                                }

                            if (entriesToSave.isEmpty()) {
                                saveMessage = coachTr(isEnglish, "אין נתונים לשמירה", "No data to save")
                                return@Surface
                            }

                            screenScope.launch {
                                isSaving = true
                                saveMessage = null

                                runCatching {
                                    onSave(
                                        selectedProfile,
                                        firestoreFieldName,
                                        entriesToSave
                                    )
                                }.onSuccess {
                                    saveMessage = coachTr(
                                        isEnglish,
                                        "${coachSectionTitleForUi(title, false)} נשמרו",
                                        "${coachSectionTitleForUi(title, true)} saved"
                                    )
                                }.onFailure {
                                    saveMessage = coachTr(
                                        isEnglish,
                                        "שמירת ${coachSectionTitleForUi(title, false)} נכשלה",
                                        "Failed to save ${coachSectionTitleForUi(title, true)}"
                                    )
                                }

                                isSaving = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFF6366F1),
                                        Color(0xFF0EA5E9)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSaving) {
                                coachTr(isEnglish, "שומר...", "Saving...")
                            } else {
                                coachTr(
                                    isEnglish,
                                    "שמור ${coachSectionTitleForUi(title, false)}",
                                    "Save ${coachSectionTitleForUi(title, true)}"
                                )
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                saveMessage?.let { msg ->
                    Surface(
                        color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) {
                            Color(0xFFDCFCE7)
                        } else {
                            Color(0xFFFEE2E2)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) Color(0xFF166534) else Color(0xFF991B1B),
                            textAlign = coachTextAlign(isEnglish),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun LabeledField(
    label: String,
    value: String,
    isEnglish: Boolean
) {
    val textAlign = coachTextAlign(isEnglish)
    val horizontalAlignment = coachHorizontalAlignment(isEnglish)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF607D8B),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF263238),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF607D8B)
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CoachGroupStatsPremiumScreen(
    stats: GroupStatsUi,
    profiles: List<TraineeProfile>,
    isEnglish: Boolean,
    onClose: () -> Unit
) {
    val statsTextAlign = coachTextAlign(isEnglish)
    val statsHorizontalAlignment = coachHorizontalAlignment(isEnglish)

    val statsAccent = Color(0xFF4F46E5)
    val glassTop = Color.White.copy(alpha = 0.72f)
    val glassBottom = Color.White.copy(alpha = 0.38f)

    val statsBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEAF1FF),
            Color(0xFFF7FAFF),
            Color(0xFFEFF4FB)
        )
    )

    val minAge = profiles.map { it.age }.filter { it > 0 }.minOrNull()?.toString() ?: "—"
    val maxAge = profiles.map { it.age }.filter { it > 0 }.maxOrNull()?.toString() ?: "—"
    val minSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.minOrNull()?.toString() ?: "—"
    val maxSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.maxOrNull()?.toString() ?: "—"

    val avgAttendanceValue = profiles
        .map { it.attendancePct }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.toInt()
        ?: 0

    var startAvgAttendanceAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAvgAttendanceAnimation = true
    }

    val animatedAvgAttendance by animateFloatAsState(
        targetValue = if (startAvgAttendanceAnimation) avgAttendanceValue.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 1400,
            easing = FastOutSlowInEasing
        ),
        label = "avgAttendanceAnim"
    )

    val glowTransition = rememberInfiniteTransition(label = "glassGlow")
    val glowShift by glowTransition.animateFloat(
        initialValue = -160f,
        targetValue = 460f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassGlowShift"
    )

    val sparklineData = remember(profiles) {
        profiles
            .map { it.attendancePct.coerceIn(0, 100) }
            .filter { it > 0 }
            .sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(statsBgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color(0xFFFCFDFF),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 7.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE7ECF7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFAFBFF),
                                            Color(0xFFF7FAFF)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = statsHorizontalAlignment
                                ) {
                                    Text(
                                        text = coachTr(isEnglish, "מידע על הקבוצה", "Group information"),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 22.sp,
                                            lineHeight = 25.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                    )

                                    Text(
                                        text = coachTr(
                                            isEnglish,
                                            "סיכום נוכחות, גיל, ותק וחגורות",
                                            "Attendance, age, seniority and belt summary"
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Surface(
                                    onClick = onClose,
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = coachTr(isEnglish, "סגור", "Close"),
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFFCFDFF),
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 5.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color(0xFFFAFBFF),
                                                        Color(0xFF4F46E5).copy(alpha = 0.07f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalAlignment = statsHorizontalAlignment,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stats.highAttendanceCount.toString(),
                                                textAlign = statsTextAlign,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontSize = 32.sp,
                                                    lineHeight = 34.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF4F46E5)
                                                )
                                            )

                                            Text(
                                                text = "🏆",
                                                fontSize = 22.sp,
                                                textAlign = TextAlign.Left
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = coachTr(isEnglish, "מעל 80%", "Above 80%"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF4F46E5)
                                            )
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        Text(
                                            text = coachTr(isEnglish, "נוכחות", "Attendance"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFCFDFF),
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 5.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color(0xFFFAFBFF),
                                                        Color(0xFF22C55E).copy(alpha = 0.07f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        AttendanceRing(
                                            percent = animatedAvgAttendance.toInt(),
                                            modifier = Modifier.size(82.dp)
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            text = coachTr(isEnglish, "נוכחות ממוצעת", "Average attendance"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                    }
                                }
                            }

                            AttendanceSparkline(
                                values = sparklineData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumStatChip(
                            label = coachTr(isEnglish, "גיל מינ׳", "Min age"),
                            value = minAge,
                            icon = "📅",
                            gradient = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF3B82F6),
                                Color(0xFF93C5FD)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumStatChip(
                            label = coachTr(isEnglish, "גיל מקס׳", "Max age"),
                            value = maxAge,
                            icon = "🎂",
                            gradient = listOf(
                                Color(0xFF0F766E),
                                Color(0xFF14B8A6),
                                Color(0xFF5EEAD4)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumStatChip(
                            label = coachTr(isEnglish, "ותק מינ׳", "Min seniority"),
                            value = minSeniority,
                            icon = "⏱",
                            gradient = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFFA855F7),
                                Color(0xFFD8B4FE)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumStatChip(
                            label = coachTr(isEnglish, "ותק מקס׳", "Max seniority"),
                            value = maxSeniority,
                            icon = "🔥",
                            gradient = listOf(
                                Color(0xFFBE185D),
                                Color(0xFFEC4899),
                                Color(0xFFF9A8D4)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumMiniPill(
                            title = coachTr(isEnglish, "מתאמנים", "Trainees"),
                            value = stats.totalTrainees.toString(),
                            icon = "👥",
                            accent = Color(0xFF4F46E5),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumMiniPill(
                            title = coachTr(isEnglish, "חגורות שונות", "Belt types"),
                            value = stats.beltCounts.size.toString(),
                            icon = "🥋",
                            accent = Color(0xFF0891B2),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFCFDFF),
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 7.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE7ECF7)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFFAFBFF),
                                        Color(0xFFF7FAFF)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = coachTr(isEnglish, "התפלגות חגורות בסניף", "Belt distribution"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = statsTextAlign,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        )

                        if (stats.beltCounts.isEmpty()) {
                            Text(
                                text = coachTr(isEnglish, "אין נתונים להצגה", "No data to display"),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = statsTextAlign,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        } else {
                            stats.beltCounts.forEach { (belt, count) ->
                                val progress =
                                    if (profiles.isNotEmpty()) count.toFloat() / profiles.size.toFloat() else 0f

                                val beltColor = beltColorForStats(belt)
                                val labelColor =
                                    if (beltColor == Color(0xFFE5E7EB)) Color(0xFF475569) else beltColor

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = beltColor.copy(alpha = 0.14f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    color = labelColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = coachBeltNameForUi(belt, isEnglish),
                                            modifier = Modifier.weight(1f),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = 18.sp,
                                                lineHeight = 21.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = labelColor
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(beltColor)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = beltColor,
                                        trackColor = beltColor.copy(alpha = 0.18f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(999.dp))
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

@Composable
private fun PremiumStatChip(
    label: String,
    value: String,
    icon: String,
    gradient: List<Color>,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "premiumChipValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "statChipPressScale"
    )

    val accent = gradient.getOrNull(1) ?: Color(0xFF4F46E5)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFCFDFF),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(96.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(start = 6.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.13f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(34.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 28.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceSparkline(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val lineAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "sparklineReveal"
    )

    Canvas(modifier = modifier) {
        val maxValue = 100f
        val minValue = 0f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width

        val points = values.mapIndexed { index, value ->
            val normalized = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            Offset(
                x = index * stepX * lineAnim,
                y = size.height - (normalized * size.height)
            )
        }

        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f
            )
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = 0.24f),
                    Color(0xFF6366F1).copy(alpha = 0.03f)
                )
            )
        )

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = linePath,
            color = Color(0xFF4F46E5),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            if (index == points.lastIndex) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = Color(0xFF4F46E5),
                    radius = 5f,
                    center = point
                )
            }
        }
    }
}

@Composable
private fun PremiumMiniPill(
    title: String,
    value: String,
    icon: String,
    accent: Color,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "miniPillValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "pillScale"
    )

    Surface(
        color = Color(0xFFFCFDFF),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 7.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(78.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {}
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                )

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(
    percent: Int,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "attendanceRing"
    )

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            drawCircle(
                color = Color(0xFFE5E7EB),
                style = Stroke(width = 12f)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF22C55E),
                        Color(0xFF4ADE80),
                        Color(0xFF16A34A)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(
                    width = 12f,
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}

/* ===== עזר רפלקטיבי ל-AttendanceRecord ===== */

private fun extractMemberId(record: Any): Long? {
    val cls = record.javaClass
    // קודם מחפשים שדה בשם שמרמז על memberId
    cls.declaredFields.forEach { f ->
        try {
            if (!f.name.contains("member", ignoreCase = true)) return@forEach
            f.isAccessible = true
            val v = f.get(record)
            when (v) {
                is Long -> return v
                is Int -> return v.toLong()
                is String -> return v.toLongOrNull()
            }
        } catch (_: Throwable) {
        }
    }
    // אם לא נמצא – מחפשים סתם Long יחיד
    cls.declaredFields.forEach { f ->
        try {
            f.isAccessible = true
            val v = f.get(record)
            if (v is Long) return v
        } catch (_: Throwable) {
        }
    }
    return null
}

private fun recordStatus(record: Any): AttendanceStatus? {
    val cls = record.javaClass
    // קודם מנסים שדה מסוג AttendanceStatus
    cls.declaredFields.forEach { f ->
        try {
            f.isAccessible = true
            val v = f.get(record)
            if (v is AttendanceStatus) return v
        } catch (_: Throwable) {
        }
    }
    // אחר כך מנסים String בשם status
    cls.declaredFields.forEach { f ->
        try {
            if (!f.name.contains("status", ignoreCase = true)) return@forEach
            f.isAccessible = true
            val v = f.get(record) as? String ?: return@forEach
            return runCatching { AttendanceStatus.valueOf(v) }.getOrNull()
        } catch (_: Throwable) {
        }
    }
    return null
}
