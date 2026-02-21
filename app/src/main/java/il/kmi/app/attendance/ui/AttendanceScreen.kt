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
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    vm: AttendanceViewModel,
    date: LocalDate,
    branch: String,
    groupKey: String,
    onOpenMemberStats: (memberId: Long?, name: String) -> Unit,
    onOpenGroupStats: (branch: String, groupKey: String) -> Unit,   // ✅ חדש
    onHomeClick: () -> Unit = {}
) {
    // הקשר למסך
    LaunchedEffect(branch, groupKey, date) {
        vm.setContext(date, branch.trim(), groupKey.trim())
        vm.ensureSession()
    }

    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current   // לשיתוף דו"ח

    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is UiEvent.ReportSaved -> onOpenGroupStats(ev.branch, ev.groupKey)
                is UiEvent.ReportSaveFailed -> Log.e("ATT_SAVE", "save failed: ${ev.message}")
            }
        }
    }



// ✅ סניף נבחר למסך (לא רשימת סניפים)
// אם מגיע CSV – ניקח את הראשון (עד שתוסיף UI בחירה מסודר)
    // ✅ סניף נבחר למסך (לא רשימת סניפים)
    // אם ה-state עדיין ריק (בהתחלה), נשתמש בפרמטרים שהגיעו למסך
    val effectiveBranchRaw = remember(state.branch, branch) {
        (state.branch.takeIf { it.isNotBlank() } ?: branch).trim()
    }
    val effectiveGroupRaw = remember(state.groupKey, groupKey) {
        (state.groupKey.takeIf { it.isNotBlank() } ?: groupKey).trim()
    }

    val selectedBranch = remember(effectiveBranchRaw) {
        effectiveBranchRaw
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: effectiveBranchRaw.trim()
    }

    // ===== טעינה אוטומטית של מתאמנים מה־users לפי סניף + קבוצה =====
    var bootstrapKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(effectiveBranchRaw, effectiveGroupRaw) {

        fun String.norm(): String = trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")

        val branchBase = selectedBranch.norm()
        val groupBase  = effectiveGroupRaw.norm()

        if (branchBase.isBlank()) return@LaunchedEffect

        val key = "$branchBase|$groupBase"
        if (bootstrapKey == key) return@LaunchedEffect

        val hasOnlyPlaceholder =
            state.members.size == 1 && state.members.first().displayName.trim().startsWith("מתאמן")

        if (state.members.isNotEmpty() && !hasOnlyPlaceholder) {
            bootstrapKey = key
            return@LaunchedEffect
        }

        vm.bootstrapMembersFromUsers(branchBase = branchBase, groupBase = groupBase)

        bootstrapKey = key
    }

    var addDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Pair<Long, String>?>(null) } // (memberId, displayName)


    // ✅ מקור אמת: הסטטוסים מגיעים מה-ViewModel (Live from Firestore)
    // נדרש שב-UiState יהיה Map<Long, AttendanceStatus> בשם statusByMemberId (או דומה)
    val statusById = state.statusByMemberId


    // דו"ח טקסט / CSV
    fun buildReportText(s: AttendanceUiState): String {
        val total   = s.members.size
        val present = s.members.count { s.statusByMemberId[it.id] == AttendanceStatus.PRESENT }
        val absent  = s.members.count { s.statusByMemberId[it.id] == AttendanceStatus.ABSENT }
        val excused = s.members.count { s.statusByMemberId[it.id] == AttendanceStatus.EXCUSED }
        val pct     = if (total > 0) (present * 100.0 / total) else 0.0

        val header = "דו\"ח נוכחות – ${s.branch} / ${s.groupKey} – $date\n"
        val stats  = "סה\"כ: $total | הגיעו: $present | לא הגיעו: $absent | מוצדקים: $excused | נוכחות: ${"%.1f".format(pct)}%\n"
        val lines  = s.members.joinToString("\n") { m ->
            val st = when (s.statusByMemberId[m.id]) {
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

    fun String.nameKey(): String = this
        .trim()
        .replace('־', '-')   // maqaf
        .replace('–', '-')   // en-dash
        .replace('—', '-')   // em-dash
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""[."'\u05F3\u05F4,;:()\\[\\]{}]"""), "") // גרש/גרשיים/פיסוק נפוץ
        .lowercase()

    val displayMembers = remember(state.members) {
        state.members.distinctBy { it.displayName.nameKey() }
    }

    val totalMembers = displayMembers.size
    val presentCount = displayMembers.count { statusById[it.id] == AttendanceStatus.PRESENT }
    val absentCount  = displayMembers.count { statusById[it.id] == AttendanceStatus.ABSENT }
    val excusedCount = displayMembers.count { statusById[it.id] == AttendanceStatus.EXCUSED }
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

                items(displayMembers, key = { it.id }) { m ->
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

                            val curr = statusById[m.id]

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
                                            val mid = m.id
                                            scope.launch { vm.mark(mid, AttendanceStatus.PRESENT) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    StatusPill(
                                        text = "לא הגיע",
                                        selected = curr == AttendanceStatus.ABSENT,
                                        selectedColor = Color(0xFFEF4444),
                                        onClick = {
                                            val mid = m.id
                                            scope.launch { vm.mark(mid, AttendanceStatus.ABSENT) }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val compactPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)

                        @Composable
                        fun BtnText(text: String) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = text,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip, // ✅ לא להפוך ל-...
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalContentColor.current
                                )
                            }
                        }

                        Button(
                            onClick = { vm.saveTodayReport() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Icon(
                                    Icons.Filled.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = LocalContentColor.current
                                )
                                Spacer(Modifier.width(6.dp))
                                BtnText("שמור")
                            }
                        }

                        Button(
                            onClick = { shareReport(state) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                                contentColor = Color.White
                            )
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Icon(
                                    Icons.Filled.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = LocalContentColor.current
                                )
                                Spacer(Modifier.width(6.dp))
                                BtnText("שתף")
                            }
                        }

                        OutlinedButton(
                            onClick = { onOpenGroupStats(state.branch, state.groupKey) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            // ✅ טקסט קצר יותר אם עדיין צפוף: "נתונים"
                            BtnText("סטטיסטיקה")
                        }
                    }
                }
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
