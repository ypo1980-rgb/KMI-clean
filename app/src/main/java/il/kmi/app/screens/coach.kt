@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.app.Application
import android.content.Context
import android.util.Log
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.Locale
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.DemoTrainees
import android.app.Activity
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Assessment
import kotlin.math.roundToInt

//=========================================================================

data class TraineeProfile(
    val id: String,
    val fullName: String,
    val belt: String,
    val seniority: String,
    val age: Int,
    val attendancePct: Int = 0,
    val branch: String = "",
    val groupKey: String = ""
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
    onOpenDrawer: () -> Unit = { il.kmi.app.ui.DrawerBridge.open() }
) {
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    val role = sp.getString("user_role", "trainee").orEmpty()

    // --- branch / groupKey שנעשה בהם שימוש בפועל ---
    var effectiveBranch by remember { mutableStateOf(branch) }
    var effectiveGroupKey by remember { mutableStateOf(groupKey) }

    // מנסים קודם מה-SharedPreferences, ואם לא – מה-Firestore (users/{uid})
    LaunchedEffect(Unit) {
        // 1. SharedPreferences – גם מפתחות רגילים וגם coach_*
        if (effectiveBranch.isBlank()) {
            effectiveBranch =
                sp.getString("branch", null)
                    ?: sp.getString("coach_branch", null)
                            ?: sp.getString("selected_branch", null)
                            ?: sp.getString("current_branch", null)
                            ?: ""
        }
        if (effectiveGroupKey.isBlank()) {
            effectiveGroupKey =
                sp.getString("groupKey", null)
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
                        effectiveBranch =
                            snap.getString("branch")
                                ?: snap.getString("coachBranch")
                                        ?: ""
                    }

                    if (effectiveGroupKey.isBlank()) {
                        // קודם שדה groupKey / coachGroupKey, ואם אין – לוקחים את הקבוצה הראשונה מרשימת groups
                        effectiveGroupKey =
                            snap.getString("groupKey")
                                ?: snap.getString("coachGroupKey")
                                        ?: (snap.get("groups") as? List<*>)?.firstOrNull()?.toString()
                                        ?: ""
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

    // ✅ אם effectiveBranch מגיע כ-CSV ("סניף1, סניף2") – עובדים בפועל עם הסניף הראשון
    val effectiveBranchPrimary = remember(effectiveBranch) {
        effectiveBranch
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: effectiveBranch.trim()
    }

    LaunchedEffect(effectiveBranch, effectiveGroupKey) {
        isProfilesLoading = traineeProfiles.isEmpty()

        fun String.norm(): String = this
            .trim()
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        val groupName = effectiveGroupKey.norm()
        if (groupName.isBlank()) {
            isProfilesLoading = false
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- מאזינים ל-DB המקומי ובונים TraineeProfile "עשיר" (כולל Firestore) ---
        repo.members(branchDbKey, groupName).collectLatest { members: List<GroupMember> ->
            if (members.isEmpty()) {
                if (!serverHasPotentialMembers) {
                    traineeProfiles = emptyList()
                    isProfilesLoading = false
                    didFinishInitialProfilesLoad = true
                }

                return@collectLatest
            }

            isProfilesLoading = false
            didFinishInitialProfilesLoad = true

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

            val userInfoByName = mutableMapOf<String, Triple<Int, String, String>>() // nameKey -> (age, beltHeb, seniority)

            val userDocs = runCatching {
                // מנסה להביא לפי groups+role (אצלך groups[] קיים)
                Firebase.firestore.collection("users")
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()
                    .documents
            }.onFailure { e ->
                Log.e("COACH_TRAINEES", "fetch users for profiles FAILED", e)
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

                userInfoByName[n] = Triple(age, beltHeb(beltRaw), seniority)
            }

            traineeProfiles = members.map { m ->
                val stat = statsMap[m.id]
                val pct = if (stat != null && stat.second > 0) {
                    ((stat.first * 100.0) / stat.second).toInt()
                } else 0

                val key = m.displayName.normKey()
                val info = userInfoByName[key]
                val age = info?.first ?: 0
                val belt = info?.second.orEmpty()
                val seniority = info?.third.orEmpty()

                TraineeProfile(
                    id = m.id.toString(),
                    fullName = m.displayName,
                    belt = belt,
                    seniority = seniority,
                    age = age,
                    attendancePct = pct,
                    branch = branchDbKey,
                    groupKey = groupName
                )
            }
        }
    }


            // אם זה לא מאמן – עדיין רוצים טופ-בר עם אייקונים
    if (!role.equals("coach", ignoreCase = true)) {
        Scaffold(
            topBar = {
                val contextLang = LocalContext.current
                val langManager = remember { AppLanguageManager(contextLang) }

                KmiTopBar(
                    title = "רשימת המתאמנים",
                    onOpenDrawer = onOpenDrawer,
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
                Text("המסך זמין למאמנים בלבד", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    val uiProfiles = remember(traineeProfiles) {
        if (!DemoPrivacy.ENABLED) {
            traineeProfiles
        } else {
            traineeProfiles.mapIndexed { index, trainee ->
                val demo = DemoTrainees.trainees.getOrNull(index)

                trainee.copy(
                    fullName = demo?.name ?: "מתאמן ${index + 1}",
                    belt = demo?.belt?.heb ?: trainee.belt,
                    seniority = demo?.yearsTraining?.let { "$it שנים" } ?: trainee.seniority,
                    age = demo?.age ?: trainee.age,
                    attendancePct = demo?.attendancePercent ?: trainee.attendancePct
                )
            }
        }
    }

    // בחירה נוכחית
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected: TraineeProfile? = uiProfiles.firstOrNull { it.id == selectedId }
        ?: uiProfiles.firstOrNull()

    LaunchedEffect(uiProfiles) {
        if (selectedId == null && uiProfiles.isNotEmpty()) {
            selectedId = uiProfiles.first().id
        } else if (selectedId != null && uiProfiles.isNotEmpty() && uiProfiles.none { it.id == selectedId }) {
            selectedId = uiProfiles.firstOrNull()?.id
        }
    }

    // הערות מאמן לפי מתאמן
    val coachNotes = remember { mutableStateMapOf<String, String>() }
    var showStatsSheet by remember { mutableStateOf(false) }

    val groupStats = remember(uiProfiles) {
        buildGroupStats(uiProfiles, uiProfiles)
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (showStatsSheet) "סטטיסטיקת קבוצה" else "רשימת המתאמנים",
                onOpenDrawer = onOpenDrawer,
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
            if (!showStatsSheet) {
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
                                        text = "סטטיסטיקה לקבוצה",
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
                    bottom = if (showStatsSheet) 16.dp else 96.dp
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
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
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
                                                        if (trainee.belt.isNotBlank()) add(trainee.belt)
                                                        if (trainee.branch.isNotBlank()) add("סניף: ${trainee.branch}")
                                                        if (trainee.groupKey.isNotBlank()) add("קבוצה: ${trainee.groupKey}")
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
                                        "בחר מתאמן מהרשימה למעלה",
                                        color = Color(0xFF757575),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                            Text(
                                text = selected.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF212121)
                            )
                            Divider()
                            LabeledField(
                                "גיל",
                                if (selected.age > 0) "${selected.age}" else "—"
                            )
                            LabeledField(
                                "ותק",
                                selected.seniority.ifBlank { "—" }
                            )
                            LabeledField(
                                "דרגה",
                                selected.belt.ifBlank { "—" }
                            )
                            LabeledField(
                                "סניף",
                                selected.branch.ifBlank { "—" }
                            )
                            LabeledField(
                                "קבוצה",
                                selected.groupKey.ifBlank { "—" }
                            )
                            LabeledField(
                                "אחוז נוכחות (60 ימים אחרונים)",
                                if (selected.attendancePct > 0) "${selected.attendancePct}%" else "—"
                            )

                                    OutlinedTextField(
                                        value = coachNotes[selected.id] ?: "",
                                        onValueChange = { coachNotes[selected.id] = it },
                                        label = { Text("הערות מאמן") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
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
                    onClose = { showStatsSheet = false }
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF607D8B)
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF263238)
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
    onClose: () -> Unit
) {
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
            .ifEmpty { listOf(35, 48, 52, 61, 70, 76, 81, 88) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(30.dp),
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(glassTop, glassBottom)
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        statsAccent.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.55f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(30.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = glowShift.dp, y = (-22).dp)
                                    .size(180.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.42f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                                        shadowElevation = 4.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assessment,
                                                contentDescription = null,
                                                tint = statsAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "מידע על הקבוצה",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Premium analytics overview",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onClose,
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "סגור",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.44f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            AttendanceRing(
                                                percent = animatedAvgAttendance.toInt()
                                            )
                                            Text(
                                                text = "נוכחות ממוצעת",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = statsAccent.copy(alpha = 0.14f),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = statsAccent.copy(alpha = 0.22f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                Text(
                                                    text = "🏆",
                                                    style = MaterialTheme.typography.labelMedium
                                                )

                                                Spacer(Modifier.width(6.dp))

                                                Text(
                                                    text = "${stats.highAttendanceCount} מתאמנים מעל 80% נוכחות",
                                                    color = statsAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    AttendanceSparkline(
                                        values = sparklineData,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumStatChip(
                                    label = "גיל מינ'",
                                    value = minAge,
                                    icon = "🎂",
                                    gradient = listOf(
                                        Color(0xFF0F766E),
                                        Color(0xFF14B8A6),
                                        Color(0xFF5EEAD4)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumStatChip(
                                    label = "גיל מקס'",
                                    value = maxAge,
                                    icon = "📅",
                                    gradient = listOf(
                                        Color(0xFF1D4ED8),
                                        Color(0xFF3B82F6),
                                        Color(0xFF93C5FD)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumStatChip(
                                    label = "וותק מינ'",
                                    value = minSeniority,
                                    icon = "⏱",
                                    gradient = listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFFA855F7),
                                        Color(0xFFD8B4FE)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumStatChip(
                                    label = "וותק מקס'",
                                    value = maxSeniority,
                                    icon = "🔥",
                                    gradient = listOf(
                                        Color(0xFFBE185D),
                                        Color(0xFFEC4899),
                                        Color(0xFFF9A8D4)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PremiumMiniPill(
                                    title = "מתאמנים",
                                    value = stats.totalTrainees.toString(),
                                    icon = "👥",
                                    accent = Color(0xFF4F46E5),
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumMiniPill(
                                    title = "חגורות שונות",
                                    value = stats.beltCounts.size.toString(),
                                    icon = "🥋",
                                    accent = Color(0xFF0891B2),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        statsAccent.copy(alpha = 0.03f),
                                        Color.White
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                statsAccent.copy(alpha = 0.08f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "התפלגות חגורות בסניף",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (stats.beltCounts.isEmpty()) {
                            Text("אין נתונים להצגה", color = Color(0xFF64748B))
                        } else {
                            stats.beltCounts.forEach { (belt, count) ->
                                val progress =
                                    if (profiles.isNotEmpty()) count.toFloat() / profiles.size.toFloat() else 0f

                                val beltColor = beltColorForStats(belt)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(beltColor)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = belt,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (beltColor == Color(0xFFE5E7EB)) Color(0xFF475569) else beltColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        Surface(
                                            color = beltColor.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                color = if (beltColor == Color(0xFFE5E7EB)) Color(0xFF475569) else beltColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = beltColor,
                                        trackColor = beltColor.copy(alpha = 0.22f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        statsAccent.copy(alpha = 0.04f),
                                        Color.White
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .border(
                                1.dp,
                                statsAccent.copy(alpha = 0.10f),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "מתאמני הקבוצה",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (profiles.isEmpty()) {
                            Text("אין מתאמנים להצגה", color = Color(0xFF64748B))
                        } else {
                            profiles.forEach { trainee ->
                                Surface(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White,
                                                        statsAccent.copy(alpha = 0.025f),
                                                        Color.White
                                                    )
                                                ),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .border(
                                                1.dp,
                                                statsAccent.copy(alpha = 0.07f),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = trainee.fullName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0xFF263238)
                                        )
                                        Text(
                                            text = listOf(
                                                trainee.belt.ifBlank { "ללא דרגה" },
                                                if (trainee.age > 0) "גיל ${trainee.age}" else null,
                                                trainee.seniority.takeIf { it.isNotBlank() },
                                                if (trainee.attendancePct > 0) "נוכחות ${trainee.attendancePct}%" else null
                                            ).filterNotNull().joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B),
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
    }
}

@Composable
private fun PremiumStatChip(
    label: String,
    value: String,
    icon: String,
    gradient: List<Color>,
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
            durationMillis = 1400,
            easing = FastOutSlowInEasing
        ),
        label = "premiumChipValue"
    )

    val glowTransition = rememberInfiniteTransition(label = "statChipGlow")
    val glowShift by glowTransition.animateFloat(
        initialValue = -100f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "statChipGlowShift"
    )

    val ringScale by glowTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statChipRingScale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "statChipPressScale"
    )

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp,
        modifier = modifier
            .height(142.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
                .background(
                    brush = Brush.verticalGradient(colors = gradient),
                    shape = RoundedCornerShape(30.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.44f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.30f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .clip(RoundedCornerShape(30.dp))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = glowShift.dp, y = (-8).dp)
                    .size(128.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.93f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.58f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "pillScale"
    )

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .height(86.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {}
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            accent.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    accent,
                                    accent.copy(alpha = 0.7f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Text(
                        text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
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
