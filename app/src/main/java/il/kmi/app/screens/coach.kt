@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

data class TraineeProfile(
    val id: String,
    val fullName: String,
    val belt: String,
    val seniority: String,
    val age: Int,
    val attendancePct: Int = 0
)

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
    // ✅ אם effectiveBranch מגיע כ-CSV ("סניף1, סניף2") – עובדים בפועל עם הסניף הראשון
    val effectiveBranchPrimary = remember(effectiveBranch) {
        effectiveBranch
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: effectiveBranch.trim()
    }

    LaunchedEffect(effectiveBranch, effectiveGroupKey) {
        traineeProfiles = emptyList()

        fun String.norm(): String = this
            .trim()
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        val groupName = effectiveGroupKey.norm()
        if (groupName.isBlank()) return@LaunchedEffect

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
        if (branchDbKey.isBlank()) return@LaunchedEffect

        // --- סנכרון אוטומטי מ-Firestore (users) לטבלת group_members ---
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
                traineeProfiles = emptyList()
                return@collectLatest
            }

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

            val userInfoByName = mutableMapOf<String, Pair<Int, String>>() // nameKey -> (age, beltHeb)

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

                userInfoByName[n] = age to beltHeb(beltRaw)
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

                TraineeProfile(
                    id = m.id.toString(),
                    fullName = m.displayName,
                    belt = belt,
                    seniority = "",        // אין אצלך שדה כזה כרגע
                    age = age,
                    attendancePct = pct
                )
            }
        }
    }


            // אם זה לא מאמן – עדיין רוצים טופ-בר עם אייקונים
    if (!role.equals("coach", ignoreCase = true)) {
        Scaffold(
            topBar = {
                KmiTopBar(
                    title = "אודות מתאמנים",
                    onOpenDrawer = onOpenDrawer,
                    showRoleStatus = false,
                    lockSearch = true,
                    showBottomActions = true
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

    // בחירה נוכחית
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected: TraineeProfile? = traineeProfiles.firstOrNull { it.id == selectedId }
        ?: traineeProfiles.firstOrNull()

    LaunchedEffect(traineeProfiles) {
        if (selectedId == null && traineeProfiles.isNotEmpty()) {
            selectedId = traineeProfiles.first().id
        } else if (traineeProfiles.none { it.id == selectedId }) {
            selectedId = traineeProfiles.firstOrNull()?.id
        }
    }

    // הערות מאמן לפי מתאמן
    val coachNotes = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = "אודות מתאמנים",
                onOpenDrawer = onOpenDrawer,
                showRoleStatus = false,
                lockSearch = true,
                showBottomActions = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // שורת דיבאג – שתראה איזה סניף/קבוצה המסך מצא בפועל
                val branchLabel = if (effectiveBranch.isBlank()) "(לא ידוע)" else effectiveBranch
                val groupLabel = if (effectiveGroupKey.isBlank()) "(לא ידוע)" else effectiveGroupKey

                val branchPrimaryLabel = if (effectiveBranchPrimary.isBlank()) "(לא ידוע)" else effectiveBranchPrimary

                Text(
                    text = "סניף: $branchLabel | בפועל: $branchPrimaryLabel | קבוצה: $groupLabel",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )

                // כרטיס רשימת מתאמנים
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Divider()

                        if (effectiveBranch.isBlank() || effectiveGroupKey.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "לא נבחרו סניף/קבוצה.\nלא ניתן להציג מתאמנים.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF616161)
                                )
                            }
                        } else if (traineeProfiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "עדיין לא הוגדרו מתאמנים לקבוצה זו.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF616161)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 210.dp)
                            ) {
                                items(traineeProfiles, key = { it.id }) { trainee ->
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
                                                trainee.fullName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (trainee.belt.isNotBlank()) {
                                                Text(
                                                    trainee.belt,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF616161)
                                                )
                                            }
                                        }
                                        if (selectedId == trainee.id) {
                                            Text("נבחר", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }
                    }
                }

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
}

@Composable
private fun LabeledField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF616161))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
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
