package il.kmi.app.attendance.ui

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.attendance.data.AttendanceStatus
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import il.kmi.app.ui.KmiTopBar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.saveable.rememberSaveable
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.ktx.Firebase
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    vm: AttendanceViewModel,
    date: LocalDate,
    branch: String,
    groupKey: String,
    // חייב להיות תואם ל־NavGraph: מזהה יכול להיות גם null
    onOpenMemberStats: (memberId: Long?, name: String) -> Unit,
    onHomeClick: () -> Unit = {}          // 👈 חדש
) {
    // הקשר למסך
    LaunchedEffect(branch, groupKey, date) {
        vm.setContext(date, branch.trim(), groupKey.trim())
        vm.ensureSession()
    }

    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current   // לשיתוף דו"ח
// ✅ סניף נבחר למסך (לא רשימת סניפים)
// אם מגיע CSV – ניקח את הראשון (עד שתוסיף UI בחירה מסודר)
    val selectedBranch = remember(state.branch) {
        state.branch
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: state.branch.trim()
    }

    // ===== טעינה אוטומטית של מתאמנים מה־users לפי branches + groups =====
    var bootstrapKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.branch, state.groupKey, state.members.size) {
        fun String.norm(): String = this
            .trim()
            // ❗ לא מחליפים כאן – ל־- כדי לא לשבור התאמה למסמכים
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        val branchRaw = selectedBranch.norm()
        val groupName = state.groupKey.norm()

        // אם מגיעים כמה סניפים באותה מחרוזת (מופרדים בפסיק) – נבחר את הראשון
        val branchBase = branchRaw
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: branchRaw

        // ננסה גם עם '-' וגם עם '–' כי במסמכים שלך branches[] שמור עם '–'
        val branchCandidates = listOf(
            branchBase,
            branchBase.replace("-", "–"),
            branchBase.replace("–", "-"),
        ).map { it.trim() }.distinct()

        val branchName = branchBase

        android.util.Log.w(
            "ATT_BOOT",
            "ENTER LaunchedEffect: branchRaw='${state.branch}' -> branch='$branchName' group='${state.groupKey}' members=${state.members.size}"
        )

        if (branchName.isBlank() || groupName.isBlank()) {
            android.util.Log.e("ATT_BOOT", "ABORT: blank branch/group after trim. branch='$branchName' group='$groupName'")
            return@LaunchedEffect
        }

        val key = "$branchName|$groupName"
        if (bootstrapKey == key) {
            android.util.Log.i("ATT_BOOT", "SKIP: bootstrapKey already '$key'")
            return@LaunchedEffect
        }
        if (state.members.isNotEmpty()) {
            android.util.Log.i("ATT_BOOT", "SKIP: members already present (${state.members.size})")
            bootstrapKey = key
            return@LaunchedEffect
        }

        android.util.Log.i("ATT_BOOT", "bootstrap start: branch='$branchName' group='$groupName'")

        runCatching {
            // 1) ניסיון ראשי: branches[] + groups[] + role (מנסה גם '-' וגם '–')
            var snap1 = Firebase.firestore.collection("users")
                .whereArrayContains("branches", branchCandidates.first())
                .whereArrayContains("groups", groupName)
                .whereEqualTo("role", "trainee")
                .get()
                .await()

            android.util.Log.i("ATT_BOOT", "snap1 size=${snap1.size()} using branch='${branchCandidates.first()}'")

            // אם יצא 0 – ננסה עוד וריאציות של הסניף (dash/en-dash)
            if (snap1.isEmpty) {
                for (cand in branchCandidates.drop(1)) {
                    val tmp = Firebase.firestore.collection("users")
                        .whereArrayContains("branches", cand)
                        .whereArrayContains("groups", groupName)
                        .whereEqualTo("role", "trainee")
                        .get()
                        .await()

                    android.util.Log.i("ATT_BOOT", "snap1 retry size=${tmp.size()} using branch='$cand'")
                    if (!tmp.isEmpty) {
                        snap1 = tmp
                        break
                    }
                }
            }

            val names1 = snap1.documents
                .mapNotNull { d ->
                    d.getString("fullName")
                        ?: d.getString("name")
                        ?: d.getString("displayName")
                }
                .map { it.norm() }
                .filter { it.isNotBlank() }

            // 2) Fallback נכון למסמכים שלך: branchesCsv (string) + groups[] + role
            val names = if (names1.isNotEmpty()) {
                names1
            } else {
                android.util.Log.w("ATT_BOOT", "snap1 returned 0 -> trying fallback branchesCsv")

                var snap2 = Firebase.firestore.collection("users")
                    .whereEqualTo("branchesCsv", branchCandidates.first())
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

                android.util.Log.i("ATT_BOOT", "snap2 size=${snap2.size()} using branchesCsv='${branchCandidates.first()}'")

                if (snap2.isEmpty) {
                    for (cand in branchCandidates.drop(1)) {
                        val tmp = Firebase.firestore.collection("users")
                            .whereEqualTo("branchesCsv", cand)
                            .whereArrayContains("groups", groupName)
                            .whereEqualTo("role", "trainee")
                            .get()
                            .await()

                        android.util.Log.i("ATT_BOOT", "snap2 retry size=${tmp.size()} using branchesCsv='$cand'")
                        if (!tmp.isEmpty) {
                            snap2 = tmp
                            break
                        }
                    }
                }

                snap2.documents
                    .mapNotNull { d ->
                        d.getString("fullName")
                            ?: d.getString("name")
                            ?: d.getString("displayName")
                    }
                    .map { it.norm() }
                    .filter { it.isNotBlank() }
            }

            android.util.Log.i("ATT_BOOT", "bootstrap names=${names.size} -> $names")

            names
                .distinctBy { it.lowercase() }
                .forEach { n -> vm.addMember(n) }

        }.onFailure { t ->
            android.util.Log.e("ATT_BOOT", "bootstrap failed", t)
            return@LaunchedEffect
        }

        bootstrapKey = key
    }

    var addDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Pair<Long, String>?>(null) } // (memberId, displayName)

    // --- Helper: שליפת סטטוס לפי memberId (תומך במפתחות Long או String) ---
    fun memberStatusOf(s: AttendanceUiState, memberId: Any): AttendanceStatus? {
        fun lookup(map: Map<*, *>, key: Any): AttendanceStatus? {
            (map[key] as? AttendanceStatus)?.let { return it }
            if (key is Long) (map[key.toString()] as? AttendanceStatus)?.let { return it }
            if (key is String) key.toLongOrNull()
                ?.let { (map[it] as? AttendanceStatus)?.let { st -> return st } }
            return null
        }
        val candidates = listOf("marks", "statuses", "statusById", "attendance", "recordsMap")
        for (name in candidates) {
            runCatching {
                val f = s::class.java.getDeclaredField(name).apply { isAccessible = true }
                val map = f.get(s) as? Map<*, *>
                if (map != null) lookup(map, memberId)?.let { return it }
            }
        }
        return null
    }

    LaunchedEffect(branch, groupKey) {
        fun String.norm(): String = trim()
            .replace('–', '-')
            .replace('־', '-')
            .replace(Regex("\\s+"), " ")

        val bRaw = branch.norm()
        val g    = groupKey.norm()

        // אם branch מגיע כרשימת סניפים במחרוזת אחת – נבחר את הראשון
        val b = bRaw
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: bRaw

        runCatching {
            val sample = Firebase.firestore.collection("users").limit(1).get().await()
            Log.d("USERS_TEST", "users collection readable, size sample=${sample.size()}")
        }.onFailure {
            Log.e("USERS_TEST", "users collection FAILED", it)
            return@LaunchedEffect
        }

        Log.d("USERS_TEST", "query users: branchRaw='$bRaw' -> branch='$b' groupKey='$g' role=trainee")

        // הערה: במסמכים אצלך branch נשמר בתוך branches[] ולעיתים עם en-dash (–)
        val bAlt = b.replace("-", "–")

        runCatching {
            // ניסיון A: עם b (dash רגיל)
            val snapA = Firebase.firestore.collection("users")
                .whereArrayContains("branches", b)
                .whereArrayContains("groups", g)
                .whereEqualTo("role", "trainee")
                .get()
                .await()

            // ניסיון B: עם bAlt (en-dash) אם A יצא ריק
            val snap = if (snapA.isEmpty && bAlt != b) {
                Firebase.firestore.collection("users")
                    .whereArrayContains("branches", bAlt)
                    .whereArrayContains("groups", g)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()
            } else snapA

            Log.d("USERS_TEST", "users query( branches[]+groups[] ) OK, size=${snap.size()}")
            snap.documents.take(10).forEach { d ->
                Log.d(
                    "USERS_TEST",
                    "uid=${d.id} fullName=${d.getString("fullName")} " +
                            "branches=${d.get("branches")} groups=${d.get("groups")} primaryGroup=${d.getString("primaryGroup")} role=${d.getString("role")}"
                )
            }
        }.onFailure { e ->
            // כאן יופיע גם מצב של "index required" במקום קריסה
            Log.e("USERS_TEST", "users query( branches[]+groups[] ) FAILED", e)
        }
    }

    // מפה מקומית ל־UI אופטימיסטי
    val localStatuses = remember { mutableStateMapOf<Any, AttendanceStatus>() }
    LaunchedEffect(state.members, state.hashCode()) {
        state.members.forEach { m ->
            memberStatusOf(state, m.id)?.let { st -> localStatuses[m.id] = st }
        }
    }


    // דו"ח טקסט / CSV
    fun buildReportText(s: AttendanceUiState): String {
        val total   = s.members.size
        val present = s.members.count { localStatuses[it.id] == AttendanceStatus.PRESENT }
        val absent  = s.members.count { localStatuses[it.id] == AttendanceStatus.ABSENT }
        val excused = s.members.count { localStatuses[it.id] == AttendanceStatus.EXCUSED }
        val pct     = if (total > 0) (present * 100.0 / total) else 0.0

        val header = "דו\"ח נוכחות – ${s.branch} / ${s.groupKey} – $date\n"
        val stats  = "סה\"כ: $total | הגיעו: $present | לא הגיעו: $absent | מוצדקים: $excused | נוכחות: ${"%.1f".format(pct)}%\n"
        val lines  = s.members.joinToString("\n") { m ->
            val st = when (localStatuses[m.id]) {
                AttendanceStatus.PRESENT -> "הגיע"
                AttendanceStatus.ABSENT  -> "לא הגיע"
                AttendanceStatus.EXCUSED -> "מוצדק"
                else                     -> "לא סומן"
            }
            "• ${m.displayName} – $st"
        }
        return header + stats + "\n" + lines
    }

    fun shareReport(s: AttendanceUiState) {
        val text = buildReportText(s)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "דו\"ח נוכחות – ${s.branch}/${s.groupKey} – $date")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, "שליחת דו\"ח"))
        }.onFailure { e ->
            Log.e("ATT_SHARE", "shareReport failed", e)
        }
    }

    val hebDate = remember(date) {
        val day = date.format(
            DateTimeFormatter.ofPattern("EEEE", Locale("he", "IL"))
        )
        val dmy = date.format(
            DateTimeFormatter.ofPattern("d.M.yyyy", Locale("he", "IL"))
        )
        "$day · $dmy"
    }

    // תרגיל שנבחר מהחיפוש (לפתיחת דיאלוג ההסבר)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ===== סטטיסטיקת נוכחות לשיעור הנוכחי =====
    val totalMembers = state.members.size
    val presentCount = state.members.count { localStatuses[it.id] == AttendanceStatus.PRESENT }
    val absentCount  = state.members.count { localStatuses[it.id] == AttendanceStatus.ABSENT }
    val excusedCount = state.members.count { localStatuses[it.id] == AttendanceStatus.EXCUSED }
    val attendancePct: Double =
        if (totalMembers > 0) presentCount * 100.0 / totalMembers else 0.0

    Scaffold(
        topBar = {
            KmiTopBar(
                title = "נוכחות",
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = false,
                lockSearch = false,
                lockHome = false,
                centerTitle = true,
                onHome = onHomeClick,
                onPickSearchResult = { key -> pickedKey = key }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addDialog = true },
                containerColor = Color(0xFF0EA5E9),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .offset(y = (-28).dp) // ✅ מרים עוד למעלה (אפשר לשחק עם -24/-32)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "הוספת מתאמן")
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { p ->

        // רקע גרדיאנט מודרני
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
            LazyColumn(
                modifier = Modifier
                    .padding(p)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp) // ✅ מקום ל-FAB + כפתור שמירה
            ) {
                item {
                    AttendanceHeroCard(
                        branch = state.branch,
                        groupKey = state.groupKey,
                        hebDate = hebDate,
                        totalMembers = totalMembers,
                        attendancePct = attendancePct
                    )
                }

                item {
                    AttendanceSummaryCard(
                        totalMembers = totalMembers,
                        presentCount = presentCount,
                        excusedCount = excusedCount,
                        absentCount = absentCount,
                        attendancePct = attendancePct
                    )
                }

                item {
                    Text(
                        text = "סימון נוכחות למתאמנים",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECFEFF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }

                items(state.members, key = { it.id }) { m ->
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                text = m.displayName,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            Spacer(Modifier.height(6.dp))

                            val curr = localStatuses[m.id]

                            @Composable
                            fun StatusPill(
                                text: String,
                                selected: Boolean,
                                selectedColor: Color,
                                onClick: () -> Unit,
                                modifier: Modifier = Modifier
                            ) {
                                val bg = if (selected) selectedColor else Color(0xFF0B1220)
                                val fg = if (selected) Color.White else Color(0xFFE5E7EB)
                                val brd = if (selected) null else BorderStroke(1.dp, Color(0xFF334155))

                                Surface(
                                    color = bg,
                                    contentColor = fg,
                                    shape = RoundedCornerShape(999.dp),
                                    tonalElevation = if (selected) 2.dp else 0.dp,
                                    shadowElevation = 0.dp,
                                    border = brd,
                                    modifier = modifier
                                        .height(44.dp)
                                        .clickable { onClick() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(18.dp)) {
                                            if (selected) {
                                                Icon(Icons.Filled.Check, contentDescription = null)
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(text)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    StatusPill(
                                        text = "הגיע",
                                        selected = curr == AttendanceStatus.PRESENT,
                                        selectedColor = Color(0xFF22C55E),
                                        onClick = {
                                            localStatuses[m.id] = AttendanceStatus.PRESENT
                                            val mid = (m.id as? Long) ?: (m.id as? String)?.toLongOrNull()
                                            if (mid != null) scope.launch { vm.mark(mid, AttendanceStatus.PRESENT) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    StatusPill(
                                        text = "לא הגיע",
                                        selected = curr == AttendanceStatus.ABSENT,
                                        selectedColor = Color(0xFFEF4444),
                                        onClick = {
                                            localStatuses[m.id] = AttendanceStatus.ABSENT
                                            val mid = (m.id as? Long) ?: (m.id as? String)?.toLongOrNull()
                                            if (mid != null) scope.launch { vm.mark(mid, AttendanceStatus.ABSENT) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = {
                                            val mid: Long? = (m.id as? Long) ?: (m.id as? String)?.toLongOrNull()
                                            onOpenMemberStats(mid, m.displayName)
                                        }) {
                                            Icon(
                                                Icons.Filled.Assessment,
                                                contentDescription = "סטטיסטיקה",
                                                tint = Color(0xFF38BDF8)
                                            )
                                        }

                                        IconButton(onClick = {
                                            val id = (m.id as? Long) ?: (m.id as? String)?.toLongOrNull() ?: return@IconButton
                                            pendingDelete = id to m.displayName
                                        }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "הסר מתאמן",
                                                tint = Color(0xFFF97316)
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { shareReport(state) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0EA5E9),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "שמור דו\"ח נוכחות של האימון היום",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        // ===== דיאלוג תרגיל שנבחר מהחיפוש =====
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
                            onClick = { isFav = !isFav },
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
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { pickedKey = null }) {
                        Text("סגור")
                    }
                }
            )
        }

        // ====== דיאלוג אישור מחיקה ======
        pendingDelete?.let { (memberId, displayName) ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("הסרת מתאמן") },
                text = { Text("להסיר את \"$displayName\" מהרשימה?") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.removeMember(memberId)
                        pendingDelete = null
                    }) { Text("הסר") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("ביטול") }
                }
            )
        }

        // ====== דיאלוג הוספת מתאמן ======
        if (addDialog) {
            AlertDialog(
                onDismissRequest = { addDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            vm.addMember(name.trim())
                            name = ""
                        }
                        addDialog = false
                    }) { Text("הוספה") }
                },
                dismissButton = {
                    TextButton(onClick = { addDialog = false }) { Text("ביטול") }
                },
                title = { Text("הוספת מתאמן לקבוצה") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("שם מלא") }
                    )
                }
            )
        }
    }
}

@Composable
private fun AttendanceHeroCard(
    branch: String,
    groupKey: String,
    hebDate: String,
    totalMembers: Int,
    attendancePct: Double
) {
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

                // ─── שורת כותרת קומפקטית ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = hebDate,
                            style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                            color = Color(0xFFE5E7EB),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "מתאמנים בשיעור: $totalMembers",
                            style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                            color = Color(0xFFBFDBFE),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF1E40AF))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ─── אחוז נוכחות – מיושר לימין ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "נוכחות: ${"%.0f".format(attendancePct)}%",
                        style = MaterialTheme.typography.labelLarge.merge(rtlStyle),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE),
                        textAlign = TextAlign.Right
                    )
                }

                // ─── פרטים: סניפים/קבוצה – ימין + שורות נפרדות ───
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

                InfoRow("סניף", branchLines)
                InfoRow("קבוצה", groupLines)
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(
    totalMembers: Int,
    presentCount: Int,
    excusedCount: Int,
    absentCount: Int,
    attendancePct: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "נוכחות ממוצעת של הקבוצה בשיעור זה",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFECFEFF),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceStatBox(label = "סה\"כ", value = totalMembers.toString())
                AttendanceStatBox(label = "הגיעו", value = presentCount.toString())
                AttendanceStatBox(label = "מוצדקים", value = excusedCount.toString())
                AttendanceStatBox(
                    label = "נוכחות %",
                    value = String.format(Locale("he", "IL"), "%.1f", attendancePct)
                )
            }
        }
    }
}

/* ========= עזר: לפרק מפתח חיפוש "belt|topic|item" ========= */
private fun parseSearchKey(key: String): Triple<Belt, String, String> {
    val parts = when {
        "|" in key  -> key.split("|",  limit = 3)
        "::" in key -> key.split("::", limit = 3)
        "/" in key  -> key.split("/",  limit = 3)
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
        .replace('–', '-')    // en dash
        .replace('־', '-')    // maqaf
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
}

@Composable
private fun AttendanceStatBox(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5F5)
        )
    }
}
