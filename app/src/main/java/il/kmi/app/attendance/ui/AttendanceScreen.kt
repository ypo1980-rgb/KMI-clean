package il.kmi.app.attendance.ui

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.saveable.rememberSaveable
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.screens.parseSearchKey
import android.app.Activity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.time.YearMonth

//========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    vm: AttendanceViewModel,
    date: LocalDate,
    branch: String,
    groupKey: String,
    onOpenMemberStats: (memberId: Long?, name: String) -> Unit,
    onOpenGroupStats: (branch: String, groupKey: String) -> Unit,
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
    val screenTextAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val screenLayoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val isDarkMode =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    /*
     * true רק כאשר המאמן בחר לפתוח דוח קיים
     * לצורך עריכה.
     *
     * שינוי תאריך, סניף או קבוצה מחזיר את המסך
     * למצב הסגור שנקבע לפי הנתונים מהשרת.
     */
    var isEditingSavedReport by rememberSaveable(
        state.date,
        state.branch,
        state.groupKey
    ) {
        mutableStateOf(false)
    }

    /*
     * מאפשר סגירה מיידית לאחר שמירה, עוד לפני
     * שהקריאה החוזרת מ־Firestore הסתיימה.
     */
    var reportSavedLocally by rememberSaveable(
        state.date,
        state.branch,
        state.groupKey
    ) {
        mutableStateOf(false)
    }

    /*
     * דוח נשאר סגור כאשר הוא קיים בשרת או
     * נשמר בהצלחה במהלך הכניסה הנוכחית,
     * כל עוד המאמן לא לחץ על עריכת דיווח.
     */
    val isReportSaved =
        (
                state.hasSavedReport ||
                        reportSavedLocally
                ) &&
                !isEditingSavedReport

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.ReportSaved -> {
                    reportSavedLocally = true
                    isEditingSavedReport = false
                }

                is UiEvent.ReportSaveFailed -> {
                    /*
                     * בכשל שמירה משאירים את הרשימה
                     * פתוחה כדי שהסימונים לא ייעלמו.
                     */
                    reportSavedLocally = false
                }
            }
        }
    }

    // ✅ מקור אמת למסך: הבחירה הפעילה מתוך ה-ViewModel
// לא חותכים יותר CSV ולא לוקחים אוטומטית רק את הסניף הראשון.
    val effectiveBranchRaw = remember(
        state.branch,
        branch
    ) {
        (
                state.branch
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: branch
                ).trim()
    }

    val effectiveGroupRaw = remember(
        state.groupKey,
        groupKey
    ) {
        (
                state.groupKey
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: groupKey
                ).trim()
    }

    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
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

    LaunchedEffect(state.date, effectiveBranchRaw, effectiveGroupRaw) {

        fun String.norm(): String = trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")

        val branchBase = effectiveBranchRaw.norm()
        val groupBase  = effectiveGroupRaw.norm()

        if (branchBase.isBlank()) return@LaunchedEffect

        val key = "${state.date}|$branchBase|$groupBase"
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

    fun shareReport(s: AttendanceUiState) {
        val membersForPdf =
            s.members.filterNot {
                it.displayName
                    .isDemoOrPlaceholderTrainee()
            }

        val reportDate =
            s.date

        val reportDateText =
            reportDate.format(
                DateTimeFormatter.ofPattern(
                    "dd.MM.yyyy",
                    if (isEnglish) {
                        Locale.ENGLISH
                    } else {
                        Locale("he", "IL")
                    }
                )
            )

        val pdfFile =
            createAttendancePdf(
                context = context,
                state = s.copy(
                    members = membersForPdf
                ),
                date = reportDate,
                isEnglish = isEnglish
            )

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    if (isEnglish) {
                        "Attendance report - " +
                                "${s.branch}/${s.groupKey} - " +
                                reportDateText
                    } else {
                        "דו\"ח נוכחות – " +
                                "${s.branch}/${s.groupKey} – " +
                                reportDateText
                    }
                )

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        runCatching {
            context.startActivity(
                Intent.createChooser(
                    send,
                    tr(
                        "שיתוף דו\"ח נוכחות PDF",
                        "Share attendance PDF"
                    )
                )
            )
        }
    }

    // תרגיל שנבחר מהחיפוש (לפתיחת דיאלוג ההסבר)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ===== סטטיסטיקת נוכחות לשיעור הנוכחי =====

    fun String.phoneKey(): String = filter { it.isDigit() }
        .removePrefix("972")
        .removePrefix("0")

    fun GroupMember.attendanceUniqueKey(): String {
        val phoneKey = phone.orEmpty().phoneKey()
        val nameKey = displayName.nameKey()

        return when {
            phoneKey.isNotBlank() -> "phone:$phoneKey"
            nameKey.isNotBlank() -> "name:${nameKey.substringBefore(" ")}"
            else -> "member:$id"
        }
    }

    val displayMembers = remember(state.members) {
        state.members
            .filterNot { it.displayName.isDemoOrPlaceholderTrainee() }
            .distinctBy { it.attendanceUniqueKey() }
    }

    val hasRealMembers = displayMembers.isNotEmpty()

    /*
     * group = null בודק אם בסניף מתקיים אימון
     * כלשהו בתאריך הנבחר, ללא תלות בקבוצה.
     */
    val hasBranchTrainingOnSelectedDate =
        remember(
            state.date,
            effectiveBranchRaw
        ) {
            TrainingCatalog.hasTrainingOn(
                date = state.date,
                branch = effectiveBranchRaw,
                group = null
            )
        }

    val branchFieldText =
        if (hasBranchTrainingOnSelectedDate) {
            effectiveBranchRaw
        } else {
            tr(
                "אין סניף פעיל בתאריך הנבחר",
                "No active branch on the selected date"
            )
        }

    val groupFieldText =
        if (hasBranchTrainingOnSelectedDate) {
            effectiveGroupRaw
        } else {
            tr(
                "אין קבוצה פעילה בתאריך הנבחר",
                "No active group on the selected date"
            )
        }

    val totalMembers = displayMembers.size
    val presentCount = displayMembers.count {
        statusById[it.id] == AttendanceStatus.PRESENT
    }
    val absentCount = displayMembers.count {
        statusById[it.id] == AttendanceStatus.ABSENT
    }
    val attendancePct: Double =
        if (totalMembers > 0) {
            presentCount * 100.0 / totalMembers
        } else {
            0.0
        }

    Scaffold(
        topBar = {

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (isEnglish) "Attendance" else "נוכחות",
                showMenu = true,
                showRoleBadge = true,
                showModePill = true,
                showTopHome = false,
                showTopSearch = false,
                showTopShare =  false,
                showBottomActions = true,
                lockSearch = false,
                lockHome = false,
                centerTitle = true,
                onHome = onHomeClick,
                onShare = {
                    shareReport(
                        state.copy(
                            members = displayMembers
                        )
                    )
                },
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
            if (!isReportSaved) {
                FloatingActionButton(
                    onClick = {
                        addDialog = true
                    },
                    containerColor = Color(0xFF0EA5E9),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .offset(y = (-28).dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = tr(
                            "הוספת מתאמן",
                            "Add trainee"
                        ),
                        modifier = Modifier.size(
                            KmiIconSize.large
                        )
                    )
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { p ->

        /*
         * שימוש ישיר ברקע הגלובלי של האפליקציה.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(p)
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        top = 36.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    bottom = 120.dp
                )
            ) {
                item {
                    AttendanceSelectionCard(
                        selectedDate = state.date,
                        effectiveBranchRaw = effectiveBranchRaw,
                        branchDisplayText = branchFieldText,
                        effectiveGroupRaw = effectiveGroupRaw,
                        groupDisplayText = groupFieldText,
                        hasBranchTrainingOnSelectedDate =
                            hasBranchTrainingOnSelectedDate,
                        availableBranches = state.availableBranches,
                        availableGroups = state.availableGroups,
                        attendancePct = attendancePct,
                        isEnglish = isEnglish,
                        onDateClick = {
                            showDatePicker = true
                        },
                        onBranchSelected = { effectiveBranchRaw ->
                            vm.selectBranch(effectiveBranchRaw)
                        },
                        onGroupSelected = { effectiveGroupRaw ->
                            vm.selectGroup(effectiveGroupRaw)
                        }
                    )
                }

                /*
                 * כאשר נבחר אימון שכבר נשמר עבורו דוח,
                 * מציגים הודעה מיידית מתחת לבחירת האימון.
                 *
                 * בזמן עריכת הדוח ההודעה נעלמת, כדי שיהיה
                 * ברור שהמאמן נמצא שוב במצב סימון פעיל.
                 */
                if (isReportSaved) {
                    item {
                        SavedAttendanceNoticeCard(
                            isEnglish = isEnglish
                        )
                    }
                }

                item {
                    AttendanceSummaryCard(
                        totalMembers = totalMembers,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        attendancePct = attendancePct,
                        isEnglish = isEnglish
                    )
                }

                if (!isReportSaved) {
                    item {
                        Text(
                            text = tr(
                                "סימון נוכחות למתאמנים",
                                "Mark trainee attendance"
                            ),
                            style = KmiTypography.sectionTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color =
                                if (isDarkMode) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    Color(0xFF1E2A3D)
                                },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = screenTextAlign
                        )
                    }

                    if (!hasRealMembers) {
                        item {
                            EmptyAttendanceMembersCard(
                                isEnglish = isEnglish
                            )
                        }
                    }

                    itemsIndexed(
                        items = displayMembers,
                        key = { _, member ->
                            member.id
                        }
                    ) { index, m ->

                        CompositionLocalProvider(
                            LocalLayoutDirection provides
                                    screenLayoutDirection
                        ) {
                            Column(
                                Modifier.fillMaxWidth()
                            ) {

                                /*
                                 * קודם מקבלים את שם התצוגה מה-Mapper.
                                 *
                                 * אם ה-Mapper החליף את השם לשם פרטיות
                                 * ("מתאמן 2244" / "Trainee 2244"),
                                 * מחליפים את המספר למיקום האמיתי ברשימה.
                                 *
                                 * במצב רגיל השם האמיתי נשאר ללא שינוי.
                                 */
                                val mappedName =
                                    TraineeDisplayNameMapper
                                        .displayName(
                                            realName =
                                                m.displayName,
                                            stableKey =
                                                m.id.toString(),
                                            isEnglish =
                                                isEnglish
                                        )
                                        .trim()

                                val isDemoDisplayName =
                                    mappedName.startsWith(
                                        "מתאמן "
                                    ) ||
                                            mappedName.startsWith(
                                                "Trainee "
                                            )

                                val uiName =
                                    when {
                                        isDemoDisplayName -> {
                                            if (isEnglish) {
                                                "Trainee ${index + 1}"
                                            } else {
                                                "מתאמן ${index + 1}"
                                            }
                                        }

                                        mappedName.isNotBlank() ->
                                            mappedName

                                        else ->
                                            tr(
                                                "מתאמן ללא שם",
                                                "Unnamed trainee"
                                            )
                                    }

                                Text(
                                    text = uiName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    style = KmiTypography.cardTitle.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color =
                                        if (isDarkMode) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            Color(0xFF1E2A3D)
                                        },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                                        tonalElevation =
                                            if (selected) 2.dp else 0.dp,
                                        shadowElevation = 0.dp,
                                        border = brd,
                                        modifier = modifier
                                            .heightIn(min = 36.dp)
                                            .clickable {
                                                onClick()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 10.dp,
                                                    vertical = 8.dp
                                                ),
                                            horizontalArrangement =
                                                Arrangement.Center,
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(
                                                    KmiIconSize.tiny
                                                )
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(
                                                            KmiIconSize.tiny
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(4.dp))

                                            Text(
                                                text = text,
                                                style = KmiTypography.caption.copy(
                                                    fontWeight =
                                                        FontWeight.ExtraBold
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(12.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically
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
                                            IconButton(
                                                onClick = {
                                                    val mid: Long =
                                                        m.id

                                                    /*
                                                     * משתמשים באותו שם שכבר נבנה לשורה:
                                                     * מתאמן 1 / מתאמן 2 / ...
                                                     */
                                                    onOpenMemberStats(
                                                        mid,
                                                        uiName
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Filled.Assessment,
                                                    contentDescription = tr(
                                                        "סטטיסטיקה",
                                                        "Statistics"
                                                    ),
                                                    tint =
                                                        if (isDarkMode) {
                                                            Color(0xFFF0ABFC)
                                                        } else {
                                                            Color(0xFFA21CAF)
                                                        },
                                                    modifier = Modifier.size(
                                                        KmiIconSize.medium
                                                    )
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val id: Long =
                                                        m.id

                                                    pendingDelete =
                                                        id to uiName
                                                }
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Filled.Delete,
                                                    contentDescription = tr(
                                                        "הסר מתאמן",
                                                        "Remove trainee"
                                                    ),
                                                    tint = Color(0xFFF97316),
                                                    modifier = Modifier.size(
                                                        KmiIconSize.medium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        vertical = 10.dp
                                    ),
                                    color =
                                        if (isDarkMode) {
                                            MaterialTheme.colorScheme.outline
                                                .copy(alpha = 0.45f)
                                        } else {
                                            Color(0xFF1F2937)
                                        }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val compactPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)

                        @Composable
                        fun BtnText(text: String) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides
                                        screenLayoutDirection
                            ) {
                                Text(
                                    text = text,
                                    style = KmiTypography.action.copy(
                                        fontWeight =
                                            FontWeight.SemiBold
                                    ),
                                    color = LocalContentColor.current,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isReportSaved) {
                                    /*
                                     * פותחים את הדוח הקיים לעריכה.
                                     * הסימונים שכבר נטענו מהשרת
                                     * נשארים כפי שנשמרו.
                                     */
                                    isEditingSavedReport = true
                                } else if (hasRealMembers) {
                                    vm.saveTodayReport()
                                }
                            },
                            enabled =
                                isReportSaved ||
                                        hasRealMembers,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = compactPadding,
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                    if (isReportSaved) {
                                        Color(0xFF0284C7)
                                    } else {
                                        Color(0xFF16A34A)
                                    },
                                contentColor = Color.White,
                                disabledContainerColor =
                                    Color(0xFF475569),
                                disabledContentColor =
                                    Color(0xFFCBD5E1)
                            )
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides
                                        LayoutDirection.Rtl
                            ) {
                                Icon(
                                    imageVector =
                                        if (isReportSaved) {
                                            Icons.Filled.Check
                                        } else {
                                            Icons.Filled.Save
                                        },

                                    contentDescription = null,
                                    modifier = Modifier.size(
                                        KmiIconSize.small
                                    ),
                                    tint = LocalContentColor.current
                                )

                                Spacer(Modifier.width(6.dp))

                                BtnText(
                                    if (isReportSaved) {
                                        tr(
                                            "עריכת דיווח",
                                            "Edit report"
                                        )
                                    } else {
                                        tr(
                                            "שמור",
                                            "Save"
                                        )
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onOpenGroupStats(
                                    effectiveBranchRaw,
                                    effectiveGroupRaw
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
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

        // ===== בחירת תאריך אימון =====
        if (showDatePicker) {
            var visibleMonth by remember(state.date) {
                mutableStateOf(YearMonth.from(state.date))
            }

            val selectedDate = state.date

            val firstDayOfMonth = remember(visibleMonth) {
                visibleMonth.atDay(1)
            }

            // Sunday = 0, Monday = 1 ... Saturday = 6
            val leadingEmptyDays = remember(firstDayOfMonth) {
                firstDayOfMonth.dayOfWeek.value % 7
            }

            val daysInMonth = remember(visibleMonth) {
                visibleMonth.lengthOfMonth()
            }

            val monthLocale = if (isEnglish) Locale.ENGLISH else Locale("he", "IL")

            val monthTitle = remember(visibleMonth, isEnglish) {
                visibleMonth.atDay(1)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", monthLocale))
            }

            val selectedTitle = remember(selectedDate, isEnglish) {
                selectedDate.format(
                    DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy", monthLocale)
                )
            }

            Dialog(
                onDismissRequest = { showDatePicker = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF8FBFF),
                                        Color(0xFFEAF4FF),
                                        Color(0xFFB7DDF7),
                                        Color(0xFF1F78B4),
                                        Color(0xFF062B4A)
                                    )
                                )
                            )                            .padding(1.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(29.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.96f),
                            tonalElevation = 0.dp
                        ) {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isEnglish) {
                                    LayoutDirection.Ltr
                                } else {
                                    LayoutDirection.Rtl
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                        ) {
                                            Text(
                                                text = tr(
                                                    "בחירת תאריך אימון",
                                                    "Select training date"
                                                ),
                                                style =
                                                    KmiTypography.secondary.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color = Color(0xFFBFDBFE),
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text = selectedTitle,
                                                style =
                                                    KmiTypography.sectionTitle.copy(
                                                        fontWeight =
                                                            FontWeight.Black
                                                    ),
                                                color = Color.White,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                maxLines = 2,
                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                                        ) {
                                            Text(
                                                text = "📅",
                                                style = KmiTypography.metric,
                                                modifier = Modifier.padding(
                                                    10.dp
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color =
                                            Color.White.copy(
                                                alpha = 0.16f
                                            )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { visibleMonth = visibleMonth.minusMonths(1) }
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "‹"
                                                    } else {
                                                        "›"
                                                    },
                                                style = KmiTypography.metric.copy(
                                                    fontWeight =
                                                        FontWeight.Bold
                                                ),
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = monthTitle,
                                            style = KmiTypography.cardTitle.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val canMoveToNextMonth =
                                            visibleMonth <
                                                    YearMonth.from(
                                                        LocalDate.now()
                                                    )

                                        IconButton(
                                            onClick = {
                                                if (canMoveToNextMonth) {
                                                    visibleMonth =
                                                        visibleMonth.plusMonths(1)
                                                }
                                            },
                                            enabled =
                                                canMoveToNextMonth
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "›"
                                                    } else {
                                                        "‹"
                                                    },
                                                style =
                                                    KmiTypography.metric.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    if (canMoveToNextMonth) {
                                                        Color.White
                                                    } else {
                                                        Color.White.copy(
                                                            alpha = 0.25f
                                                        )
                                                    }
                                            )
                                        }
                                    }

                                    val weekDays = if (isEnglish) {
                                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                    } else {
                                        listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        weekDays.forEach { dayName ->
                                            Text(
                                                text = dayName,
                                                style =
                                                    KmiTypography.caption.copy(
                                                        fontWeight =
                                                            FontWeight.Black
                                                    ),
                                                color = Color(0xFF67E8F9),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    val cells =
                                        buildList {
                                            repeat(
                                                leadingEmptyDays
                                            ) {
                                                add(null)
                                            }

                                            for (
                                            day in 1..daysInMonth
                                            ) {
                                                add(day)
                                            }

                                            while (
                                                size % 7 != 0
                                            ) {
                                                add(null)
                                            }
                                        }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color.White.copy(alpha = 0.07f))
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        cells.chunked(7).forEach { week ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                week.forEach { day ->
                                                    val cellDate =
                                                        day?.let {
                                                            visibleMonth.atDay(it)
                                                        }

                                                    val today =
                                                        LocalDate.now()

                                                    val isSelected =
                                                        cellDate == selectedDate

                                                    val isToday =
                                                        cellDate == today

                                                    val isFuture =
                                                        cellDate?.isAfter(today) ==
                                                                true

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(
                                                                min = 40.dp
                                                            ),
                                                        contentAlignment =
                                                            Alignment.Center
                                                    ) {
                                                        if (day != null && cellDate != null) {
                                                            Surface(
                                                                modifier = Modifier
                                                                    .size(
                                                                        KmiIconSize.extraLarge
                                                                    )
                                                                    .clickable(
                                                                        enabled =
                                                                            !isFuture
                                                                    ) {
                                                                        vm.selectAttendanceDate(
                                                                            cellDate
                                                                        )
                                                                        showDatePicker =
                                                                            false
                                                                    },
                                                                shape = CircleShape,
                                                                color =
                                                                    when {
                                                                        isFuture ->
                                                                            Color.Transparent

                                                                        isSelected ->
                                                                            Color(0xFF22D3EE)

                                                                        isToday ->
                                                                            Color.White.copy(
                                                                                alpha = 0.14f
                                                                            )

                                                                        else ->
                                                                            Color.Transparent
                                                                    },
                                                                border = when {
                                                                    isSelected -> null
                                                                    isToday -> BorderStroke(1.dp, Color(0xFF22D3EE))
                                                                    else -> null
                                                                }
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text =
                                                                            day.toString(),
                                                                        style =
                                                                            KmiTypography.cardTitle.copy(
                                                                                fontWeight =
                                                                                    FontWeight.Black
                                                                            ),
                                                                        color =
                                                                            when {
                                                                                isFuture ->
                                                                                    Color.White.copy(
                                                                                        alpha = 0.25f
                                                                                    )

                                                                                isSelected ->
                                                                                    Color(0xFF020617)

                                                                                else ->
                                                                                    Color.White
                                                                            },
                                                                        textAlign =
                                                                            TextAlign.Center
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isEnglish) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { showDatePicker = false }
                                        ) {
                                            Text(
                                                text = tr("ביטול", "Cancel"),
                                                color = Color(0xFFBFDBFE),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                vm.selectAttendanceDate(LocalDate.now())
                                                showDatePicker = false
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF22D3EE),
                                                contentColor = Color(0xFF020617)
                                            )
                                        ) {
                                            Text(
                                                text = tr("היום", "Today"),
                                                fontWeight = FontWeight.Black
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

        // ===== דיאלוג תרגיל שנבחר מהחיפוש =====
        pickedKey?.let { key ->
            val (belt, _, item) =
                parseSearchKey(key)

            val displayName =
                ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

            val explanation =
                remember(
                    belt,
                    item,
                    isEnglish
                ) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceSelectionCard(
    selectedDate: LocalDate,
    effectiveBranchRaw: String,
    branchDisplayText: String,
    effectiveGroupRaw: String,
    groupDisplayText: String,
    hasBranchTrainingOnSelectedDate: Boolean,
    availableBranches: List<String>,
    availableGroups: List<String>,
    attendancePct: Double,
    isEnglish: Boolean,
    onDateClick: () -> Unit,
    onBranchSelected: (String) -> Unit,
    onGroupSelected: (String) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val align =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val horizontal =
        if (isEnglish) Alignment.Start else Alignment.End

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val fieldContainerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.White.copy(alpha = 0.74f)
        }

    val fieldBorderColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        } else {
            Color(0xFFC7D7EE)
        }

    val labelColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF64748B)
        }

    val valueColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF1E2A3D)
        }

    val dateText =
        remember(
            selectedDate,
            isEnglish
        ) {
            val locale =
                if (isEnglish) {
                    Locale.ENGLISH
                } else {
                    Locale("he", "IL")
                }

            selectedDate.format(
                DateTimeFormatter.ofPattern(
                    "EEEE · d.M.yy",
                    locale
                )
            )
        }

    @Composable
    fun CompactReadonlyRow(
        label: String,
        value: String,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable {
                    onClick()
                },
            shape = RoundedCornerShape(15.dp),
            color = fieldContainerColor,
            border = BorderStroke(
                width = 1.dp,
                color = fieldBorderColor
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment =
                        if (isEnglish) {
                            Alignment.Start
                        } else {
                            Alignment.End
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.caption.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = labelColor,
                        textAlign = align,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        Modifier.height(1.dp)
                    )

                    Text(
                        text = value.ifBlank { "—" },
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.caption.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = valueColor,
                        textAlign = align,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                Surface(
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    ),
                    shape = CircleShape,
                    color =
                        if (isDarkMode) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color(0xFFEAF2FF)
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color = fieldBorderColor
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = labelColor
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CompactDropdownRow(
        label: String,
        selected: String,
        options: List<String>,
        displayText: String = selected,
        onSelected: (String) -> Unit
    ) {
        val cleanOptions =
            remember(
                options,
                selected
            ) {
                (options + selected)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

        KmiPremiumDropdown(
            title = label,
            options = cleanOptions,
            selectedValue =
                displayText
                    .trim()
                    .ifBlank {
                        selected
                            .trim()
                    },
            isEnglish = isEnglish,
            placeholder = "—",
            enabled = cleanOptions.size > 1,
            onSelected = onSelected
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = fieldBorderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors =
                                if (isDarkMode) {
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF8FBFF),
                                        Color(0xFFE8F2FF),
                                        Color(0xFFD7E9FF)
                                    )
                                }
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalAlignment = horizontal,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = tr(
                        "בחירת אימון לנוכחות",
                        "Select attendance class"
                    ),
                    style = KmiTypography.cardTitle.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = valueColor,
                    textAlign = align,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    CompactReadonlyRow(
                        label = tr("תאריך", "Date"),
                        value = dateText,
                        onClick = onDateClick
                    )

                    CompactDropdownRow(
                        label = tr("סניף", "Branch"),
                        selected = effectiveBranchRaw,
                        options =
                            availableBranches.ifEmpty {
                                listOfNotNull(
                                    effectiveBranchRaw.takeIf {
                                        it.isNotBlank()
                                    }
                                )
                            },
                        displayText = branchDisplayText,
                        onSelected = onBranchSelected
                    )

                    CompactDropdownRow(
                        label = tr("קבוצה", "Group"),
                        selected = effectiveGroupRaw,
                        options =
                            if (hasBranchTrainingOnSelectedDate) {
                                availableGroups.ifEmpty {
                                    listOfNotNull(
                                        effectiveGroupRaw.takeIf {
                                            it.isNotBlank()
                                        }
                                    )
                                }
                            } else {
                                emptyList()
                            },
                        displayText = groupDisplayText,
                        onSelected = onGroupSelected
                    )
                }

                val formattedAttendancePct =
                    String.format(
                        if (isEnglish) {
                            Locale.ENGLISH
                        } else {
                            Locale("he", "IL")
                        },
                        "%.1f",
                        attendancePct
                    )

                Text(
                    text =
                        if (isEnglish) {
                            "Attendance: $formattedAttendancePct%"
                        } else {
                            "נוכחות: $formattedAttendancePct%"
                        },
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color =
                        if (isDarkMode) {
                            Color(0xFF67E8F9)
                        } else {
                            Color(0xFF0891B2)
                        },
                    textAlign = align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SavedAttendanceNoticeCard(
    isEnglish: Boolean
) {
    fun tr(
        he: String,
        en: String
    ): String =
        if (isEnglish) {
            en
        } else {
            he
        }

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() <
                0.5f

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color =
            if (isDarkMode) {
                Color(0xFF10243A)
            } else {
                Color(0xFFE8F3FF)
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (isDarkMode) {
                    Color(0xFF38BDF8).copy(
                        alpha = 0.55f
                    )
                } else {
                    Color(0xFF93C5FD)
                }
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    layoutDirection
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(
                        KmiIconSize.large
                    ),
                    shape = CircleShape,
                    color =
                        if (isDarkMode) {
                            Color(0xFF0284C7)
                                .copy(alpha = 0.28f)
                        } else {
                            Color(0xFFD7EAFF)
                        },
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF38BDF8)
                            .copy(alpha = 0.55f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.Info,
                            contentDescription = null,
                            tint =
                                if (isDarkMode) {
                                    Color(0xFF67E8F9)
                                } else {
                                    Color(0xFF0284C7)
                                },
                            modifier = Modifier.size(
                                KmiIconSize.medium
                            )
                        )
                    }
                }

                Spacer(
                    Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment =
                        if (isEnglish) {
                            Alignment.Start
                        } else {
                            Alignment.End
                        },
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tr(
                            "הנוכחות לאימון זה כבר סומנה ונשמרה",
                            "Attendance for this class has already been marked and saved"
                        ),
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography.cardTitle.copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                        color =
                            if (isDarkMode) {
                                Color(0xFF7DD3FC)
                            } else {
                                Color(0xFF0369A1)
                            },
                        textAlign = textAlign,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text = tr(
                            "ניתן לצפות בדיווח או לפתוח אותו שוב לעריכה במידת הצורך.",
                            "You can view the report or reopen it for editing if needed."
                        ),
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography.secondary,
                        color =
                            if (isDarkMode) {
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            } else {
                                Color(0xFF334155)
                            },
                        textAlign = textAlign,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAttendanceMembersCard(
    isEnglish: Boolean
) {
    fun tr(
        he: String,
        en: String
    ): String = if (isEnglish) en else he

    val align =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val horizontal =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    val direction =
        if (isEnglish) {
            TextDirection.Ltr
        } else {
            TextDirection.Rtl
        }

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() <
                0.5f

    val cardColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surface
        } else {
            Color(0xFFEAF2FF)
        }

    val cardBorderColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.outline.copy(
                alpha = 0.65f
            )
        } else {
            Color(0xFF93C5FD)
        }

    val titleColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF0F172A)
        }

    val bodyColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF334155)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = BorderStroke(
            width = 1.dp,
            color = cardBorderColor
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    layoutDirection
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors =
                                if (isDarkMode) {
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        Color(0xFF0C4A6E).copy(
                                            alpha = 0.38f
                                        )
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF8FBFF),
                                        Color(0xFFDCEEFF)
                                    )
                                }
                        )
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint =
                        if (isDarkMode) {
                            Color(0xFF38BDF8)
                        } else {
                            Color(0xFF0284C7)
                        },
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = horizontal,
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr(
                            "לא נמצאו מתאמנים בקבוצה שנבחרה",
                            "No trainees were found in the selected group"
                        ),
                        color = titleColor,
                        fontWeight = FontWeight.ExtraBold,
                        style =
                            KmiTypography.cardTitle.merge(
                                TextStyle(
                                    textDirection = direction
                                )
                            ),
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "רשימת המתאמנים נטענת אוטומטית לפי הסניף והקבוצה. אם חסר מתאמן, ניתן להוסיף אותו באמצעות כפתור הפלוס.",
                            "The trainee list loads automatically for the selected branch and group. If a trainee is missing, you can add them using the plus button."
                        ),
                        color = bodyColor,
                        style =
                            KmiTypography.body.merge(
                                TextStyle(
                                    textDirection = direction
                                )
                            ),
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(
    totalMembers: Int,
    presentCount: Int,
    absentCount: Int,
    attendancePct: Double,
    isEnglish: Boolean = false
) {
    val align =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    fun trLocal(
        he: String,
        en: String
    ): String = if (isEnglish) en else he

    val formattedAttendancePct = String.format(
        if (isEnglish) {
            Locale.ENGLISH
        } else {
            Locale("he", "IL")
        },
        "%.1f",
        attendancePct
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFEAF2FF),
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD8E3F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment =
                if (isEnglish) {
                    Alignment.Start
                } else {
                    Alignment.End
                }
        ) {
            Text(
                text = trLocal(
                    "נוכחות ממוצעת של הקבוצה בשיעור זה",
                    "Group attendance average for this class"
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E2A3D),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                AttendanceStatBox(
                    label = trLocal("סה״כ", "Total"),
                    value = totalMembers.toString()
                )

                AttendanceStatBox(
                    label = trLocal("הגיעו", "Present"),
                    value = presentCount.toString()
                )

                AttendanceStatBox(
                    label = trLocal("נעדרו", "Absent"),
                    value = absentCount.toString()
                )
            }

            Text(
                text = trLocal(
                    "נוכחות: $formattedAttendancePct%",
                    "Attendance: $formattedAttendancePct%"
                ),
                style = KmiTypography.secondary,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0891B2),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
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
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E2A3D)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5E6C80)
        )
    }
}

private fun createAttendancePdf(
    context: android.content.Context,
    state: AttendanceUiState,
    date: LocalDate,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f

    val document = PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(100, 116, 139)

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = regularTypeface
        textSize = 13f
        color = textDark
    }

    val titlePaint = Paint(paint).apply {
        typeface = boldTypeface
        textSize = 22f
        color = navy
        textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    }

    val headerPaint = Paint(paint).apply {
        typeface = boldTypeface
        textSize = 14f
        color = android.graphics.Color.WHITE
    }

    fun tr(he: String, en: String): String =
        if (isEnglish) en else he

    val total = state.members.size
    val present = state.members.count { state.statusByMemberId[it.id] == AttendanceStatus.PRESENT }
    val absent = state.members.count { state.statusByMemberId[it.id] == AttendanceStatus.ABSENT }
    val excused = state.members.count { state.statusByMemberId[it.id] == AttendanceStatus.EXCUSED }
    val pct = if (total > 0) present * 100.0 / total else 0.0

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = page.canvas
    var y = KmiPdfHeader.CONTENT_TOP

    fun drawHeader() {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "דו״ח נוכחות",
            titleEnglish = "Attendance Report",
            subtitleHebrew =
                "${state.branch} · ${state.groupKey}",
            subtitleEnglish =
                "${state.branch} · ${state.groupKey}"
        )

        y = KmiPdfHeader.CONTENT_TOP
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = null,
            isEnglish = isEnglish
        )
    }

    fun newPage() {
        drawFooter()
        document.finishPage(page)
        pageNumber++
        page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        canvas = page.canvas
        y = KmiPdfHeader.CONTENT_TOP
        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (
            y + height >
            pageHeight - KmiPdfFooter.CONTENT_BOTTOM_PADDING
        ) {
            newPage()
        }
    }

    drawHeader()

    titlePaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    canvas.drawText(
        tr("סיכום שיעור", "Class Summary"),
        if (isEnglish) margin else pageWidth - margin,
        y,
        titlePaint
    )
    y += 30f

    paint.textSize = 13f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

    val summaryLines = listOf(
        tr("סה\"כ מתאמנים: $total", "Total trainees: $total"),
        tr("הגיעו: $present", "Present: $present"),
        tr("לא הגיעו: $absent", "Absent: $absent"),
        tr("מוצדקים: $excused", "Excused: $excused"),
        tr("אחוז נוכחות: ${"%.1f".format(pct)}%", "Attendance: ${"%.1f".format(pct)}%")
    )

    summaryLines.forEach {
        canvas.drawText(
            it,
            if (isEnglish) margin else pageWidth - margin,
            y,
            paint
        )
        y += 22f
    }

    y += 12f

    val tableTop = y
    val tablePaint = Paint().apply {
        color = android.graphics.Color.rgb(2, 43, 74)
    }
    canvas.drawRoundRect(
        margin,
        tableTop,
        pageWidth - margin,
        tableTop + 30f,
        10f,
        10f,
        tablePaint
    )

    headerPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    canvas.drawText(
        tr("מתאמן", "Trainee"),
        if (isEnglish) margin + 16f else pageWidth - margin - 16f,
        tableTop + 20f,
        headerPaint
    )

    canvas.drawText(
        tr("סטטוס", "Status"),
        if (isEnglish) pageWidth - margin - 140f else margin + 140f,
        tableTop + 20f,
        headerPaint
    )

    y += 44f

    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.textSize = 12.5f

    state.members.forEachIndexed { index, member ->
        ensureSpace(34f)

        val rowBg = Paint().apply {
            color = if (index % 2 == 0) {
                android.graphics.Color.rgb(248, 251, 255)
            } else {
                android.graphics.Color.rgb(234, 244, 255)
            }
        }

        canvas.drawRoundRect(
            margin,
            y - 18f,
            pageWidth - margin,
            y + 10f,
            8f,
            8f,
            rowBg
        )

        val statusText = when (state.statusByMemberId[member.id]) {
            AttendanceStatus.PRESENT -> tr("הגיע", "Present")
            AttendanceStatus.ABSENT -> tr("לא הגיע", "Absent")
            AttendanceStatus.EXCUSED -> tr("מוצדק", "Excused")
            else -> tr("לא סומן", "Not marked")
        }

        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
        val pdfDisplayName =
            TraineeDisplayNameMapper.displayName(
                realName = member.displayName,
                stableKey = member.id.toString(),
                isEnglish = isEnglish
            ).ifBlank {
                tr(
                    "מתאמן ללא שם",
                    "Unnamed trainee"
                )
            }

        canvas.drawText(
            pdfDisplayName.take(38),
            if (isEnglish) margin + 16f else pageWidth - margin - 16f,
            y,
            paint
        )

        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText(
            statusText,
            if (isEnglish) pageWidth - margin - 140f else margin + 140f,
            y,
            paint
        )
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        y += 34f
    }

    drawFooter()
    document.finishPage(page)

    val dir =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * מנקים רק תווים שאינם חוקיים בשם קובץ.
     * שם הסניף והקבוצה נשארים בעברית / אנגלית
     * כפי שהם מופיעים בדוח.
     */
    fun safeFilePart(value: String): String =
        value
            .trim()
            .replace(
                Regex("""[\\/:*?"<>|]"""),
                "-"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .ifBlank {
                if (isEnglish) {
                    "Unknown"
                } else {
                    "לא ידוע"
                }
            }

    val safeBranch =
        safeFilePart(
            state.branch
        )

    val safeGroup =
        safeFilePart(
            state.groupKey
        )

    val fileName =
        if (isEnglish) {
            "Attendance report - $safeBranch - $safeGroup - $date.pdf"
        } else {
            "דוח נוכחות - $safeBranch - $safeGroup - $date.pdf"
        }

    val file =
        File(
            dir,
            fileName
        )

    /*
     * כתיבה עם append = false דורסת דוח קודם
     * בעל אותו שם ללא צורך במחיקה ידנית.
     */
    FileOutputStream(
        file,
        false
    ).use { out ->
        document.writeTo(out)
    }

    document.close()
    return file
}
