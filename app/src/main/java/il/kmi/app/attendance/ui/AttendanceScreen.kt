package il.kmi.app.attendance.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.Info
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
import il.kmi.app.screens.parseSearchKey
import android.app.Activity
import androidx.compose.ui.unit.sp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//========================================================================

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

    val languageManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val screenTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val screenLayoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is UiEvent.ReportSaved -> onOpenGroupStats(ev.branch, ev.groupKey)
                is UiEvent.ReportSaveFailed -> Unit
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

    fun String.nameKey(): String = this
        .trim()
        .replace('־', '-')   // maqaf
        .replace('–', '-')   // en-dash
        .replace('—', '-')   // em-dash
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""[."'\u05F3\u05F4,;:()\[\]{}]"""), "")
        .lowercase()

    fun String.isDemoOrPlaceholderTrainee(): Boolean {
        val clean = trim()
        if (clean.isBlank()) return true

        val key = clean.nameKey()

        return key == "מתאמן" ||
                key.startsWith("מתאמן ") ||
                key.startsWith("מתאמן_") ||
                key == "demo" ||
                key.startsWith("demo ") ||
                key.startsWith("test trainee") ||
                key == "trainee" ||
                key.startsWith("trainee ")
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

        val hasRealServerMembers = state.members.any {
            !it.displayName.isDemoOrPlaceholderTrainee()
        }

        if (hasRealServerMembers) {
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

        val header = if (isEnglish) {
            "Attendance report - ${s.branch} / ${s.groupKey} - $date\n"
        } else {
            "דו\"ח נוכחות – ${s.branch} / ${s.groupKey} – $date\n"
        }

        val stats = if (isEnglish) {
            "Total: $total | Present: $present | Absent: $absent | Excused: $excused | Attendance: ${"%.1f".format(pct)}%\n"
        } else {
            "סה\"כ: $total | הגיעו: $present | לא הגיעו: $absent | מוצדקים: $excused | נוכחות: ${"%.1f".format(pct)}%\n"
        }

        val lines = s.members.joinToString("\n") { m ->
            val st = when (s.statusByMemberId[m.id]) {
                AttendanceStatus.PRESENT -> tr("הגיע", "Present")
                AttendanceStatus.ABSENT  -> tr("לא הגיע", "Absent")
                AttendanceStatus.EXCUSED -> tr("מוצדק", "Excused")
                else                     -> tr("לא סומן", "Not marked")
            }
            "• ${m.displayName} - $st"
        }

        return header + stats + "\n" + lines
    }

    fun shareReport(s: AttendanceUiState) {
        val text = buildReportText(s)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "Attendance report - ${s.branch}/${s.groupKey} - $date"
                } else {
                    "דו\"ח נוכחות – ${s.branch}/${s.groupKey} – $date"
                }
            )
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    send,
                    tr("שליחת דו\"ח", "Send report")
                )
            )
        }
    }

    val hebDate = remember(date, isEnglish) {
        val locale = if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
        date.format(
            DateTimeFormatter.ofPattern("EEEE · d.M.yyyy", locale)
        )
    }

    // תרגיל שנבחר מהחיפוש (לפתיחת דיאלוג ההסבר)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ===== סטטיסטיקת נוכחות לשיעור הנוכחי =====

    val displayMembers = remember(state.members) {
        state.members
            .filterNot { it.displayName.isDemoOrPlaceholderTrainee() }
            .distinctBy { it.displayName.nameKey() }
    }

    val hasRealMembers = displayMembers.isNotEmpty()

    val totalMembers = displayMembers.size
    val presentCount = displayMembers.count { statusById[it.id] == AttendanceStatus.PRESENT }
    val absentCount  = displayMembers.count { statusById[it.id] == AttendanceStatus.ABSENT }
    val excusedCount = displayMembers.count { statusById[it.id] == AttendanceStatus.EXCUSED }
    val attendancePct: Double =
        if (totalMembers > 0) presentCount * 100.0 / totalMembers else 0.0

    Scaffold(
        topBar = {

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (isEnglish) "Attendance" else "נוכחות",
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                lockHome = false,
                centerTitle = true,
                onHome = onHomeClick,
                onPickSearchResult = { key -> pickedKey = key },

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
                Icon(
                    Icons.Filled.Add,
                    contentDescription = tr("הוספת מתאמן", "Add trainee")
                )
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
                        attendancePct = attendancePct,
                        isEnglish = isEnglish
                    )
                }

                item {
                    AttendanceSummaryCard(
                        totalMembers = totalMembers,
                        presentCount = presentCount,
                        excusedCount = excusedCount,
                        absentCount = absentCount,
                        attendancePct = attendancePct,
                        isEnglish = isEnglish
                    )
                }

                item {
                    Text(
                        text = tr("סימון נוכחות למתאמנים", "Mark trainee attendance"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECFEFF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = screenTextAlign
                    )
                }

                if (!hasRealMembers) {
                    item {
                        EmptyAttendanceMembersCard(
                            branch = selectedBranch,
                            groupKey = effectiveGroupRaw,
                            isEnglish = isEnglish
                        )
                    }
                }

                items(displayMembers, key = { it.id }) { m ->
                    CompositionLocalProvider(LocalLayoutDirection provides screenLayoutDirection) {
                        Column(Modifier.fillMaxWidth()) {

                            val uiName = m.displayName.ifBlank {
                                tr("מתאמן ללא שם", "Unnamed trainee")
                            }

                            Text(
                                text = uiName,
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
                                        text = tr("הגיע", "Present"),
                                        selected = curr == AttendanceStatus.PRESENT,
                                        selectedColor = Color(0xFF22C55E),
                                        onClick = {
                                            val mid = m.id
                                            scope.launch { vm.mark(mid, AttendanceStatus.PRESENT) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    StatusPill(
                                        text = tr("לא הגיע", "Absent"),
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
                                            val uiName = m.displayName.ifBlank {
                                                tr("מתאמן ללא שם", "Unnamed trainee")
                                            }
                                            onOpenMemberStats(mid, uiName)
                                        }) {
                                            Icon(
                                                Icons.Filled.Assessment,
                                                contentDescription = tr("סטטיסטיקה", "Statistics"),
                                                tint = Color(0xFF38BDF8)
                                            )
                                        }

                                        IconButton(onClick = {
                                            val id = (m.id as? Long) ?: (m.id as? String)?.toLongOrNull() ?: return@IconButton
                                            val uiName = m.displayName.ifBlank {
                                                tr("מתאמן ללא שם", "Unnamed trainee")
                                            }
                                            pendingDelete = id to uiName
                                        }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = tr("הסר מתאמן", "Remove trainee"),
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
                            CompositionLocalProvider(LocalLayoutDirection provides screenLayoutDirection) {
                                Text(
                                    text = text,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalContentColor.current,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (hasRealMembers) {
                                    vm.saveTodayReport()
                                }
                            },
                            enabled = hasRealMembers,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF475569),
                                disabledContentColor = Color(0xFFCBD5E1)
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
                                BtnText(tr("שמור", "Save"))
                            }
                        }

                        Button(
                            onClick = {
                                if (hasRealMembers) {
                                    shareReport(
                                        state.copy(
                                            members = displayMembers
                                        )
                                    )
                                }
                            },
                            enabled = hasRealMembers,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF475569),
                                disabledContentColor = Color(0xFFCBD5E1)
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
                                BtnText(tr("שתף", "Share"))
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
                            BtnText(tr("סטטיסטיקה", "Stats"))
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

            val explanation = remember(belt, item, isEnglish) {
                findExplanationForHit(
                    belt = belt,
                    rawItem = item,
                    topic = topic,
                    isEnglish = isEnglish
                )
            }

            val beltLabel = if (isEnglish) {
                when (belt) {
                    Belt.WHITE -> "(White belt)"
                    Belt.YELLOW -> "(Yellow belt)"
                    Belt.ORANGE -> "(Orange belt)"
                    Belt.GREEN -> "(Green belt)"
                    Belt.BLUE -> "(Blue belt)"
                    Belt.BROWN -> "(Brown belt)"
                    Belt.BLACK -> "(Black belt)"
                }
            } else {
                "(${belt.heb})"
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
                            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = beltLabel,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = screenTextAlign,
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
                                    contentDescription = tr("מועדף", "Favorite"),
                                    tint = Color(0xFFFFC107)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = tr("הוסף למועדפים", "Add to favorites"),
                                )
                            }
                        }
                    }
                },
                text = {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { pickedKey = null }) {
                        Text(tr("סגור", "Close"))
                    }
                }
            )
        }

        // ====== דיאלוג אישור מחיקה ======
        pendingDelete?.let { (memberId, displayName) ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = {
                    Text(
                        text = tr("הסרת מתאמן", "Remove trainee"),
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = if (isEnglish) {
                            "Remove \"$displayName\" from the list?"
                        } else {
                            "להסיר את \"$displayName\" מהרשימה?"
                        },
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.removeMember(memberId)
                        pendingDelete = null
                    }) {
                        Text(tr("הסר", "Remove"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(tr("ביטול", "Cancel"))
                    }
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
                    }) {
                        Text(tr("הוספה", "Add"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { addDialog = false }) {
                        Text(tr("ביטול", "Cancel"))
                    }
                },
                title = {
                    Text(
                        text = tr("הוספת מתאמן לקבוצה", "Add trainee to group"),
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text(tr("שם מלא", "Full name")) },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = screenTextAlign,
                            textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyAttendanceMembersCard(
    branch: String,
    groupKey: String,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End
    val direction = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color(0xFF1D4ED8).copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = horizontal,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr(
                            "אין מתאמנים אמיתיים בקבוצה הזו עדיין",
                            "No real trainees in this group yet"
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall.merge(
                            TextStyle(textDirection = direction)
                        ),
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "המסך מחובר לשרת. אם קיימים משתמשים רשומים בסניף ובקבוצה, הם ייטענו אוטומטית. אפשר גם להוסיף מתאמן ידנית עם כפתור הפלוס.",
                            "This screen is connected to the server. Registered users in this branch and group will load automatically. You can also add a trainee manually using the plus button."
                        ),
                        color = Color(0xFFBFDBFE),
                        style = MaterialTheme.typography.bodySmall.merge(
                            TextStyle(textDirection = direction)
                        ),
                        textAlign = align,
                        lineHeight = 17.dp.value.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "סניף: ${branch.ifBlank { "—" }} · קבוצה: ${groupKey.ifBlank { "—" }}",
                            "Branch: ${branch.ifBlank { "—" }} · Group: ${groupKey.ifBlank { "—" }}"
                        ),
                        color = Color(0xFFE0F2FE),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall.merge(
                            TextStyle(textDirection = direction)
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
}

@Composable
private fun AttendanceHeroCard(
    branch: String,
    groupKey: String,
    hebDate: String,
    totalMembers: Int,
    attendancePct: Double,
    isEnglish: Boolean = false
) {
    val align = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End
    val textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    val styleDirection = TextStyle(textDirection = textDirection)

    fun trLocal(he: String, en: String): String = if (isEnglish) en else he

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = horizontal
                    ) {
                        Text(
                            text = hebDate,
                            style = MaterialTheme.typography.labelSmall.merge(styleDirection),
                            color = Color(0xFFE5E7EB),
                            textAlign = align,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = trLocal(
                                "מתאמנים בשיעור: $totalMembers",
                                "Trainees in class: $totalMembers"
                            ),
                            style = MaterialTheme.typography.labelSmall.merge(styleDirection),
                            color = Color(0xFFBFDBFE),
                            textAlign = align,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trLocal(
                            "נוכחות: ${"%.0f".format(attendancePct)}%",
                            "Attendance: ${"%.0f".format(attendancePct)}%"
                        ),
                        style = MaterialTheme.typography.labelLarge.merge(styleDirection),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22D3EE),
                        textAlign = align
                    )
                }

                @Composable
                fun InfoRow(label: String, lines: List<String>) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = horizontal,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFFBFDBFE),
                            style = MaterialTheme.typography.labelSmall.merge(styleDirection),
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        lines.filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = line,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.merge(styleDirection),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = align,
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

                InfoRow(trLocal("סניף", "Branch"), branchLines)
                InfoRow(trLocal("קבוצה", "Group"), groupLines)
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
    attendancePct: Double,
    isEnglish: Boolean = false
) {
    val align = if (isEnglish) TextAlign.Left else TextAlign.Right
    fun trLocal(he: String, en: String): String = if (isEnglish) en else he

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
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                text = trLocal(
                    "נוכחות ממוצעת של הקבוצה בשיעור זה",
                    "Group attendance average for this class"
                ),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFECFEFF),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceStatBox(
                    label = trLocal("סה\"כ", "Total"),
                    value = totalMembers.toString()
                )
                AttendanceStatBox(
                    label = trLocal("הגיעו", "Present"),
                    value = presentCount.toString()
                )
                AttendanceStatBox(
                    label = trLocal("נוכחות %", "Attendance %"),
                    value = String.format(
                        if (isEnglish) Locale.ENGLISH else Locale("he", "IL"),
                        "%.1f",
                        attendancePct
                    )
                )
            }
        }
    }
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean = false
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

    if (isEnglish) {
        for (candidate in candidates) {
            val got = il.kmi.shared.domain.content.English.ExerciseExplanationsEn
                .get(belt, candidate)
                .trim()

            if (got.isNotBlank() && !got.startsWith("Detailed explanation for:")) {
                return if ("::" in got) got.substringAfter("::").trim() else got
            }
        }

        return "No explanation is currently available for this exercise."
    }

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
