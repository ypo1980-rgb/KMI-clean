package il.kmi.app.ui.training

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingDirectory
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.prefs.KmiPrefs
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableLongStateOf
import java.time.ZoneId
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection


// ===========================
// Training Summary Palette
// ===========================

private val SummaryBgTop = Color(0xFF08101F)
private val SummaryBgBottom = Color(0xFF1A315C)

private val SummaryCard = Color(0xFF223454)
private val SummaryCardInner = Color(0xFF2B3D5F)

private val SummaryBorder = Color(0xFF39527F)
private val SummaryDivider = Color(0xFF364A72)

private val SummaryChip = Color(0xFF32486F)
private val SummaryChipSelected = Color(0xFF47649A)

/**
 * פריט תרגיל "לבחירה" שמגיע מהקטלוג (ContentRepo).
 * אתה תבנה את הרשימה הזו מה-ContentRepo אצלך בנקודת החיבור (Route/NavGraph).
 */
data class ExercisePickItem(
    val exerciseId: String,
    val name: String,
    val topic: String
)

@Composable
private fun SummarySectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.merge(rtlStyle),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.merge(rtlStyle),
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumSummaryCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = SummaryCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            SummaryBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
            content = content
        )
    }
}

/**
 * ✅ שים לב: השם V2 כדי למנוע Conflicting overloads אם כבר קיים אצלך TrainingSummaryScreen אחר בפרויקט.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSummaryScreen(
    vm: TrainingSummaryViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    belt: Belt,
    pickedDateIso: String? = null,
    onBack: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val scrollState = rememberLazyListState()
    var showAddExercisesSheet by rememberSaveable { mutableStateOf(false) }

    // ✅ אם המסך נפתח מלוח השנה עם תאריך נבחר — נכניס אותו ל-VM מיד בכניסה
    LaunchedEffect(pickedDateIso) {
        val iso = pickedDateIso?.trim().orEmpty()
        if (iso.isNotBlank() && state.dateIso != iso) {
            vm.setDateIso(iso)
        }
    }

    // ✅ מקור אמת לסניף/קבוצה/מאמן לפי תאריך (כמו במסך הבית)
    val truth = remember(sp) { HomeScheduleTruth(sp) }
    var branchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.dateIso) {
        val t = runCatching { truth.trainingForDate(state.dateIso) }.getOrNull()

        if (t == null) {
            branchError = "לא נמצא אימון בתאריך הזה לפי הלוז שלך במסך הבית."
            if (state.branchName.isNotBlank()) vm.setBranchName("")
            // לא מאפסים מאמן/קבוצה כדי לא למחוק ידני
            return@LaunchedEffect
        }

        branchError = null

        if (state.branchName != t.branchName) vm.setBranchName(t.branchName)
        if (state.groupKey != t.groupKey) vm.setGroupKey(t.groupKey)

        if (t.coachName.isNotBlank() && state.coachName != t.coachName) {
            vm.setCoachName(t.coachName)
        }
    }

    fun beltHebLabel(b: Belt): String {
        // ב-shared כבר יש לך heb מלא כמו: "חגורה צהובה"
        // אם אתה רוצה להציג בדיוק את זה:
        return b.heb

        // אם היית רוצה רק "צהובה" ולהוסיף "חגורה " — תוכל לעשות:
        // val only = b.heb.removePrefix("חגורה ").trim()
        // return "חגורה $only"
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF0B1020)) {
                if (onBack == null) {
                    KmiTopBar(
                        title = "סיכום אימון",
                        showTopHome = false,
                        lockSearch = true
                    )
                } else {
                    KmiTopBar(
                        title = "סיכום אימון",
                        showTopHome = false,
                        onBack = onBack,
                        lockSearch = true
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color(0xFF0B1020)
    ) { padding ->

        val granite = Brush.verticalGradient(
            colors = listOf(
                SummaryBgTop,
                Color(0xFF0E1A33),
                Color(0xFF15284D),
                SummaryBgBottom
            )
        )

        val graniteNoise = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.030f),
                Color.Transparent,
                Color.White.copy(alpha = 0.018f),
                Color.Transparent,
                Color.White.copy(alpha = 0.014f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1400f, 1400f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(granite)
                .background(graniteNoise)
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {

                // -----------------------------
                // ✅ כרטיס אימון קומפקטי (במקום "פרטי אימון" הגדול)
                // -----------------------------
                item {
                    TrainingInfoCard(
                        dateIso = state.dateIso,
                        onDateChange = { vm.setDateIso(it) },
                        branchName = state.branchName,
                        coachName = state.coachName,
                        groupKey = state.groupKey,
                        errorText = branchError,

                        // ✅ חדש
                        markedDateIsos = state.summaryDaysInCalendarMonth,
                        onRequestMonthMarks = { y, m -> vm.loadSummaryDaysForMonth(y, m) }
                    )
                }

// הוספת תרגילים – כרטיס קומפקטי + פתיחת Bottom Sheet
// -----------------------------
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title = "הוספת תרגילים",
                            subtitle = "בחר אם להוסיף תרגילים שבוצעו באימון",
                            icon = Icons.Filled.PlaylistAddCheck
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SummaryDivider,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Spacer(Modifier.height(2.dp))
                        }

                        Text(
                            text = if (state.selected.isEmpty()) {
                                "עדיין לא נוספו תרגילים לאימון הזה"
                            } else {
                                "נוספו כבר ${state.selected.size} תרגילים לאימון הזה"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        onClick = { showAddExercisesSheet = true },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAddCheck,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "הוסף תרגילים",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SummaryDivider,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(6.dp))
            }

// -----------------------------
// תרגילים שנבחרו + עריכה (כרטיס מודרני)
// -----------------------------
            if (state.selected.isNotEmpty()) {
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title = "התרגילים שנוספו לאימון",
                            subtitle = "ניהול, עריכה והוספת דגשים לכל תרגיל",
                            icon = Icons.Filled.FitnessCenter
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SummaryDivider,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Spacer(Modifier.height(2.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { },
                                label = { Text("סה\"כ ${state.selected.size} תרגילים") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                )
                            )
                        }

                        val selectedList = state.selected.values.toList()
                            .sortedBy { it.name.lowercase() }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 560.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = true
                        ) {
                            items(selectedList, key = { it.exerciseId }) { ex ->
                                SelectedExerciseEditor(
                                    item = ex,
                                    onRemove = { vm.removeExercise(ex.exerciseId) },
                                    onDifficulty = { vm.setDifficulty(ex.exerciseId, it) },
                                    onHighlight = { vm.setHighlight(ex.exerciseId, it) },
                                    onHomePractice = { vm.setHomePractice(ex.exerciseId, it) }
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------
            // סיכום חופשי (מאמן/מתאמן לפי role)
            // -----------------------------
            item {
                PremiumSummaryCard {
                    SummarySectionHeader(
                        title = "סיכום כללי",
                        subtitle = "סיכום חופשי של האימון, תחושות, דגשים ומה לשפר",
                        icon = Icons.Filled.Notes
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SummaryDivider,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Spacer(Modifier.height(2.dp))
                    }

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        value = state.notes,
                        onValueChange = { vm.setNotes(it) },
                        label = {
                            Text(
                                if (state.isCoach) "דגשים מקצועיים, ביצוע, מה לשפר…"
                                else "איך היה האימון? מה הרגשת? מה לשפר…"
                            )
                        },
                        minLines = 6,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SummaryBorder,
                            unfocusedBorderColor = SummaryDivider,
                            focusedLabelColor = Color.White.copy(alpha = 0.90f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.72f),
                            cursorColor = Color.White,
                            focusedContainerColor = SummaryCardInner,
                            unfocusedContainerColor = SummaryCardInner
                        )
                    )
                }
            }

                // -----------------------------
                // שמירה
                // -----------------------------
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title = "שמירה",
                            subtitle = "שמור את הסיכום והתרגילים שנוספו לאימון הזה",
                            icon = Icons.Filled.Check
                        )

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        onClick = {
                            vm.save()

                            val key = "training_summary_days"
                            val cur = sp.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
                            cur.add(state.dateIso.trim())
                            sp.edit().putStringSet(key, cur).apply()
                        },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF2563EB).copy(alpha = 0.90f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (state.isSaving) "שומר..." else "שמירת סיכום האימון",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

                item { Spacer(Modifier.height(10.dp)) }
        } // LazyColumn

            if (showAddExercisesSheet) {
                AddExercisesBottomSheet(
                    vm = vm,
                    state = state,
                    initialBelt = belt,
                    beltHebLabel = ::beltHebLabel,
                    onDismiss = { showAddExercisesSheet = false }
                )
            }

        } // Box
    } // Scaffold
} // TrainingSummaryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExercisesBottomSheet(
    vm: TrainingSummaryViewModel,
    state: TrainingSummaryUiState,
    initialBelt: Belt,
    beltHebLabel: (Belt) -> String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var beltOpen by remember { mutableStateOf(false) }
    var selectedBelt by rememberSaveable { mutableStateOf<Belt?>(null) }
    var topic by rememberSaveable { mutableStateOf("") }
    var subTopic by rememberSaveable { mutableStateOf("") }

    var pendingPicks by remember {
        mutableStateOf<LinkedHashMap<String, ExercisePickItem>>(linkedMapOf())
    }

    val darkFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = SummaryBorder,
        unfocusedBorderColor = SummaryDivider,
        focusedLabelColor = Color.White.copy(alpha = 0.88f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.68f),
        focusedTrailingIconColor = Color.White.copy(alpha = 0.90f),
        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.68f),
        focusedLeadingIconColor = Color.White.copy(alpha = 0.90f),
        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.68f),
        cursorColor = Color.White,
        focusedContainerColor = SummaryCardInner,
        unfocusedContainerColor = SummaryCardInner
    )

    val topics: List<String> = remember(selectedBelt) {
        val belt = selectedBelt ?: return@remember emptyList()

        val viaBridge = runCatching {
            il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt)
        }.getOrDefault(emptyList())

        if (viaBridge.isNotEmpty()) {
            viaBridge
        } else {
            runCatching {
                val sharedBelt =
                    il.kmi.shared.domain.Belt.fromId(belt.id)
                        ?: il.kmi.shared.domain.Belt.WHITE

                il.kmi.shared.domain.SubTopicRegistry
                    .allForBelt(sharedBelt)
                    .keys
                    .toList()
            }.getOrDefault(emptyList())
        }
    }

    val subTopics: List<String> = remember(selectedBelt, topic) {
        val belt = selectedBelt
        if (belt == null || topic.isBlank()) return@remember emptyList()

        runCatching {
            il.kmi.app.domain.ContentRepo
                .listSubTopicTitles(belt, topic)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filterNot { it == topic.trim() }
                .filterNot { it == "כל תתי הנושאים" }
                .distinct()
        }.getOrDefault(emptyList())
    }

    val rawItems: List<String> = remember(selectedBelt, topic, subTopic, subTopics) {
        val belt = selectedBelt
        if (belt == null || topic.isBlank()) return@remember emptyList()
        if (subTopics.isNotEmpty() && subTopic.isBlank()) return@remember emptyList()

        runCatching {
            il.kmi.app.domain.ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topic,
                subTopicTitle = subTopic.ifBlank { null }
            )
        }.getOrDefault(emptyList())
    }

    val displayItems: List<String> = remember(rawItems) {
        rawItems
            .map {
                il.kmi.shared.questions.model.util.ExerciseTitleFormatter
                    .displayName(it)
                    .ifBlank { it }
                    .trim()
            }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredItems: List<String> = remember(displayItems, state.searchQuery) {
        val q = state.searchQuery.trim()
        if (q.isBlank()) displayItems
        else displayItems.filter { it.contains(q, ignoreCase = true) }
    }

    val showTopicField = selectedBelt != null
    val showSubTopicField = showTopicField && topic.isNotBlank() && subTopics.isNotEmpty()
    val showSearchAndItems = showTopicField && topic.isNotBlank() &&
            (subTopics.isEmpty() || subTopic.isNotBlank())

    LaunchedEffect(selectedBelt) {
        topic = ""
        subTopic = ""
        pendingPicks = linkedMapOf()
        vm.setSearchQuery("")
    }

    LaunchedEffect(topic) {
        subTopic = ""
        pendingPicks = linkedMapOf()
        vm.setSearchQuery("")
    }

    LaunchedEffect(subTopic) {
        pendingPicks = linkedMapOf()
        vm.setSearchQuery("")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10182D),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(54.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF10182D),
                            Color(0xFF16213F),
                            Color(0xFF1B2C56)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                item {
                    SummarySectionHeader(
                        title = "הוספת תרגילים",
                        subtitle = "בחר חגורה, נושא ותת־נושא והוסף תרגילים לאימון",
                        icon = Icons.Filled.PlaylistAddCheck
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = beltOpen,
                        onExpandedChange = { beltOpen = !beltOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedBelt?.let(beltHebLabel).orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("בחר חגורה") },
                            label = { Text("חגורה") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = beltOpen)
                            },
                            colors = darkFieldColors,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = beltOpen,
                            onDismissRequest = { beltOpen = false },
                            containerColor = Color(0xFF182545)
                        ) {
                            Belt.values().forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = beltHebLabel(b),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        selectedBelt = b
                                        beltOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (showTopicField) {
                    item {
                        var topicOpen by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = topicOpen,
                            onExpandedChange = { topicOpen = !topicOpen }
                        ) {
                            OutlinedTextField(
                                value = topic,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("בחר נושא") },
                                label = { Text("נושא") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicOpen)
                                },
                                colors = darkFieldColors,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = topicOpen,
                                onDismissRequest = { topicOpen = false },
                                containerColor = Color(0xFF182545)
                            ) {
                                topics.forEach { t ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = t,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            topic = t
                                            topicOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSubTopicField) {
                    item {
                        var subOpen by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = subOpen,
                            onExpandedChange = { subOpen = !subOpen }
                        ) {
                            OutlinedTextField(
                                value = subTopic,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("בחר תת-נושא") },
                                label = { Text("תת-נושא") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = subOpen)
                                },
                                colors = darkFieldColors,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = subOpen,
                                onDismissRequest = { subOpen = false },
                                containerColor = Color(0xFF182545)
                            ) {
                                subTopics.forEach { st ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = st,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            subTopic = st
                                            subOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSearchAndItems) {
                    item {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.searchQuery,
                            onValueChange = { vm.setSearchQuery(it) },
                            label = { Text("חיפוש תרגיל") },
                            singleLine = true,
                            colors = darkFieldColors
                        )
                    }

                    item {
                        Text(
                            text = "נמצאו ${filteredItems.size} · כבר נוספו ${state.selected.size} · ממתינים לאישור ${pendingPicks.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.78f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pendingPicks.isNotEmpty()) {
                                TextButton(onClick = { pendingPicks = linkedMapOf() }) {
                                    Text("נקה בחירה", color = Color.White.copy(alpha = 0.82f))
                                }
                                Spacer(Modifier.width(10.dp))
                            }

                            FilledTonalButton(
                                onClick = {
                                    pendingPicks.values.forEach { p ->
                                        if (!state.selected.containsKey(p.exerciseId)) {
                                            vm.toggleExercise(p)
                                        }
                                    }
                                    pendingPicks = linkedMapOf()
                                    onDismiss()
                                },
                                enabled = pendingPicks.isNotEmpty(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF2563EB).copy(alpha = 0.90f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("אשר והוסף", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    items(filteredItems, key = { it }) { name ->
                        val belt = selectedBelt ?: return@items
                        val id = "${belt.id}|$topic|$subTopic|$name"
                        val alreadySelected = state.selected.containsKey(id)
                        val isPending = pendingPicks.containsKey(id)

                        ExercisePickRow(
                            item = ExercisePickItem(
                                exerciseId = id,
                                name = name,
                                topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                            ),
                            checked = alreadySelected || isPending,
                            onToggle = {
                                if (alreadySelected) {
                                    vm.toggleExercise(
                                        ExercisePickItem(
                                            exerciseId = id,
                                            name = name,
                                            topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                                        )
                                    )
                                } else {
                                    val next = LinkedHashMap(pendingPicks)
                                    if (next.containsKey(id)) next.remove(id)
                                    else next[id] = ExercisePickItem(
                                        exerciseId = id,
                                        name = name,
                                        topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                                    )
                                    pendingPicks = next
                                }
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingInfoCard(
    dateIso: String,
    onDateChange: (String) -> Unit,
    branchName: String,
    coachName: String,
    groupKey: String,
    errorText: String?,
    markedDateIsos: Set<String>,
    onRequestMonthMarks: (year: Int, month1to12: Int) -> Unit
) {
    fun prettyDate(iso: String): String {
        return runCatching {
            val d = LocalDate.parse(iso.trim())
            val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("he", "IL"))
            d.format(fmt)
        }.getOrDefault(iso)
    }

    val zone = remember { ZoneId.systemDefault() }

    fun isoToMillis(iso: String): Long? {
        return runCatching {
            val d = LocalDate.parse(
                iso.trim(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            )
            d.atStartOfDay(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var initialMillis by rememberSaveable {
        mutableLongStateOf(isoToMillis(dateIso) ?: System.currentTimeMillis())
    }

    LaunchedEffect(dateIso) {
        isoToMillis(dateIso)?.let { initialMillis = it }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SummaryCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, SummaryBorder)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "פרטי האימון",
                            style = MaterialTheme.typography.titleLarge.merge(rtlStyle),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = prettyDate(dateIso),
                            style = MaterialTheme.typography.bodySmall.merge(rtlStyle),
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF312E81))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SummaryCardInner
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        PremiumInfoRow(
                            label = "סניף",
                            value = branchName.ifBlank { "לא נמצא סניף" }
                        )

                        PremiumInfoRow(
                            label = "מאמן",
                            value = coachName.ifBlank { "מאמן לא ידוע" }
                        )

                        if (groupKey.isNotBlank()) {
                            PremiumInfoRow(
                                label = "קבוצה",
                                value = groupKey
                            )
                        }
                    }
                }

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "קריאת סיכומים",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = errorText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initDate = remember(dateIso) {
            runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull() ?: LocalDate.now()
        }

        var viewYear by rememberSaveable(initDate) { mutableStateOf(initDate.year) }
        var viewMonth by rememberSaveable(initDate) { mutableStateOf(initDate.monthValue) }
        var pickedDay by rememberSaveable(initDate) { mutableStateOf(initDate.dayOfMonth) }

        LaunchedEffect(viewYear, viewMonth, showDatePicker) {
            if (showDatePicker) onRequestMonthMarks(viewYear, viewMonth)
        }

        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = {
                Text(
                    text = "בחירת תאריך",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val next = LocalDate.of(viewYear, viewMonth, 1).plusMonths(1)
                                    viewYear = next.year
                                    viewMonth = next.monthValue
                                    pickedDay = 1
                                }
                            ) { Text("הבא") }

                            Spacer(Modifier.weight(1f))

                            val monthTitle = remember(viewYear, viewMonth) {
                                val d = LocalDate.of(viewYear, viewMonth, 1)
                                val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("he", "IL"))
                                d.format(fmt)
                            }

                            Text(
                                text = monthTitle,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.weight(1f))

                            TextButton(
                                onClick = {
                                    val prev = LocalDate.of(viewYear, viewMonth, 1).minusMonths(1)
                                    viewYear = prev.year
                                    viewMonth = prev.monthValue
                                    pickedDay = 1
                                }
                            ) { Text("הקודם") }
                        }

                        val week = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { wd ->
                                Text(
                                    text = wd,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        MonthGridWithMarks(
                            year = viewYear,
                            month1to12 = viewMonth,
                            pickedDay = pickedDay,
                            markedDateIsos = markedDateIsos,
                            onPickDay = { pickedDay = it }
                        )

                        val chosenIso = remember(viewYear, viewMonth, pickedDay) {
                            LocalDate.of(viewYear, viewMonth, pickedDay)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        }

                        Text(
                            text = "נבחר: $chosenIso",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = LocalDate.of(viewYear, viewMonth, pickedDay)
                        onDateChange(
                            picked.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        )
                        showDatePicker = false
                    }
                ) { Text("בחר") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ביטול") }
            }
        )
    }
}

@Composable
private fun MonthGridWithMarks(
    year: Int,
    month1to12: Int,
    pickedDay: Int,
    markedDateIsos: Set<String>,
    onPickDay: (Int) -> Unit
) {
    val first = remember(year, month1to12) { LocalDate.of(year, month1to12, 1) }
    val daysInMonth = remember(year, month1to12) { first.lengthOfMonth() }

    // אנחנו מציגים שבוע: א ב ג ד ה ו ש (Sunday..Saturday)
    // LocalDate.dayOfWeek: Monday..Sunday
    fun dayOfWeekIndexSundayStart(dow: java.time.DayOfWeek): Int {
        return when (dow) {
            java.time.DayOfWeek.SUNDAY -> 0
            java.time.DayOfWeek.MONDAY -> 1
            java.time.DayOfWeek.TUESDAY -> 2
            java.time.DayOfWeek.WEDNESDAY -> 3
            java.time.DayOfWeek.THURSDAY -> 4
            java.time.DayOfWeek.FRIDAY -> 5
            java.time.DayOfWeek.SATURDAY -> 6
        }
    }

    val offset = remember(first) { dayOfWeekIndexSundayStart(first.dayOfWeek) }
    val totalCells = ((offset + daysInMonth + 6) / 7) * 7
    val cells = remember(year, month1to12) { (0 until totalCells).toList() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { idx ->
                    val dayNum = idx - offset + 1
                    val enabled = dayNum in 1..daysInMonth

                    val iso = if (enabled) {
                        LocalDate.of(year, month1to12, dayNum)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                    } else ""

                    val isPicked = enabled && dayNum == pickedDay
                    val isMarked = enabled && iso in markedDateIsos

                    val bg = when {
                        isPicked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        isMarked -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable(enabled = enabled) { onPickDay(dayNum) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (enabled) dayNum.toString() else "",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isPicked) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
                            )

                            // ✅ נקודה קטנה = יש סיכום באותו יום
                            if (isMarked) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: java.time.YearMonth,
    selected: LocalDate,
    markedIsoDays: Set<String>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val isoFmt = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) }

    // ראשון..שבת
    val weekLabels = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")

    // DayOfWeek: MONDAY=1..SUNDAY=7  → אנחנו רוצים Sunday=0
    fun sundayIndex(dow: java.time.DayOfWeek): Int = when (dow) {
        java.time.DayOfWeek.SUNDAY -> 0
        java.time.DayOfWeek.MONDAY -> 1
        java.time.DayOfWeek.TUESDAY -> 2
        java.time.DayOfWeek.WEDNESDAY -> 3
        java.time.DayOfWeek.THURSDAY -> 4
        java.time.DayOfWeek.FRIDAY -> 5
        java.time.DayOfWeek.SATURDAY -> 6
    }

    val firstDay = remember(month) { month.atDay(1) }
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val startOffset = remember(month) { sundayIndex(firstDay.dayOfWeek) }

    val monthTitle = remember(month) {
        // "ינואר 2026" בעברית (פשוט)
        val mName = month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("he", "IL"))
        "$mName ${month.year}"
    }

    Column(modifier = Modifier.padding(12.dp)) {

        // Header עם חצים
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "חודש קודם")
            }

            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onNext) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "חודש הבא")
            }
        }

        Spacer(Modifier.height(8.dp))

        // שמות ימים
        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { w ->
                Text(
                    text = w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // גריד 6 שורות מקסימום
        val totalCells = 42
        val cells = (0 until totalCells).map { idx ->
            val dayNum = idx - startOffset + 1
            if (dayNum in 1..daysInMonth) month.atDay(dayNum) else null
        }

        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val date = cells[row * 7 + col]
                    val isSelected = date != null && date == selected
                    val isMarked = date != null && markedIsoDays.contains(date.format(isoFmt))

                    val bg = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isMarked   -> MaterialTheme.colorScheme.secondaryContainer
                        else       -> Color.Transparent
                    }

                    val fg = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isMarked   -> MaterialTheme.colorScheme.onSecondaryContainer
                        else       -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable(enabled = date != null) {
                                date?.let(onPick)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date?.dayOfMonth?.toString() ?: "",
                            textAlign = TextAlign.Center,
                            color = fg,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // מקרא קטן
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) { Spacer(Modifier.size(12.dp)) }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "יש סיכום",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumInfoRow(
    label: String,
    value: String
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val rtlStyle = TextStyle(textDirection = TextDirection.Rtl)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.merge(rtlStyle),
                color = Color(0xFFBFDBFE),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.merge(rtlStyle),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Label מימין
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Right
        )

        Spacer(Modifier.width(10.dp))

        // ✅ Value משמאל ל-Label, אבל מיושר לימין בתוך השטח
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Right,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExercisePickRow(
    item: ExercisePickItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = if (checked)
            Color(0xFF3B82F6).copy(alpha = 0.22f)
        else
            SummaryCardInner.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFF60A5FA).copy(alpha = 0.45f)
            else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = item.topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(10.dp))

            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun SelectedExerciseEditor(
    item: SelectedExerciseUi,
    onRemove: () -> Unit,
    onDifficulty: (Int?) -> Unit,
    onHighlight: (String) -> Unit,
    onHomePractice: (Boolean) -> Unit
) {
    var notesOpen by rememberSaveable(item.exerciseId) {
        mutableStateOf(item.highlight.isNotBlank())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = SummaryCardInner,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            SummaryBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = item.topic.ifBlank { "ללא נושא" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SummaryChip,
                                labelColor = Color.White,
                                leadingIconContentColor = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    FilledTonalIconButton(
                        onClick = { notesOpen = !notesOpen },                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = if (notesOpen) "סגור הערות" else "פתח הערות"
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedIconButton(
                        onClick = onRemove,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.60f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "מחק תרגיל",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SummaryDivider,
                shape = RoundedCornerShape(999.dp)
            ) {
                Spacer(Modifier.height(1.5.dp))
            }

            Text(
                text = "רמת קושי",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = { onHomePractice(!item.homePractice) },
                    label = {
                        Text(
                            if (item.homePractice) "סומן לעבודה בבית" else "סמן לעבודה בבית"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (item.homePractice)
                            SummaryChipSelected
                        else
                            SummaryChip,
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    )
                )
            }

            if (!notesOpen && item.highlight.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = SummaryChip
                ) {
                        Text(
                            text = item.highlight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

            if (notesOpen) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = item.highlight,
                    onValueChange = { onHighlight(it) },
                    label = { Text("דגשים והערות לתרגיל") },
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SummaryBorder,
                        unfocusedBorderColor = SummaryDivider,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.78f),
                        cursorColor = Color.White,
                        focusedContainerColor = SummaryCardInner,
                        unfocusedContainerColor = SummaryCardInner
                    )
                )
            }
        }
    }
}

/* =========================
   Home “source of truth”
   ========================= */

private class HomeScheduleTruth(
    private val sp: SharedPreferences
) {
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private fun readSelectedBranches(): List<String> {
        val fromJsonOrCsv = runCatching {
            val js = sp.getString("branches_json", null) ?: sp.getString("branches", null)
            if (!js.isNullOrBlank()) {
                if (js.trim().startsWith("[")) {
                    val arr = JSONArray(js)
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

    data class TrainingTruth(
        val branchName: String,
        val groupKey: String,
        val coachName: String
    )

    // ✅ מקור אמת מלא לפי תאריך: סניף + קבוצה + מאמן
    fun trainingForDate(dateIso: String): TrainingTruth? {
        val date = runCatching { LocalDate.parse(dateIso.trim(), isoFmt) }.getOrNull() ?: return null
        val wantedDow = date.toCalendarDow()

        val branches = readSelectedBranches().ifEmpty { listOf("נתניה – מרכז קהילתי אופק") }.take(3)
        val groups = groupsEffective()

        for (branchName in branches) {
            for (grp in groups) {
                val sched = TrainingDirectory.getSchedule(branchName, grp) ?: continue
                val slots = sched.slots ?: emptyList()

                for (slotAny in slots) {
                    val s = readSlot(slotAny)
                    if (s.dayOfWeek == wantedDow) {
                        val coach = (sched.coachName ?: "").trim()
                            .ifBlank { defaultCoachName().orEmpty() }

                        return TrainingTruth(
                            branchName = branchName,
                            groupKey = grp,
                            coachName = coach
                        )
                    }
                }
            }
        }
        return null
    }

    private fun groupsEffective(): List<String> {
        val groupsCsv =
            sp.getString("age_groups", null)?.takeIf { it.isNotBlank() }
                ?: sp.getString("age_group", null)?.takeIf { it.isNotBlank() }
                ?: sp.getString("group", null).orEmpty()

        val raw = groupsCsv
            .split(',', ';', '|', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val normalized = raw.map {
            TrainingCatalog.normalizeGroupName(it).ifBlank { it }
        }
        return (if (normalized.isEmpty()) listOf("בוגרים") else normalized).distinct()
    }

    // ✅ ברירת מחדל לקבוצה (ללא קשר ליום) – הקבוצה הראשונה מההגדרות
    fun defaultGroup(): String? {
        val groups = groupsEffective()
        return groups.firstOrNull()
    }

    // ✅ שם מאמן ברירת מחדל מתוך SharedPreferences (מפתחות נפוצים)
    fun defaultCoachName(): String? {
        val candidates = listOf(
            "coach_name",
            "coachName",
            "trainer_name",
            "trainerName",
            "coach",
            "trainer"
        )
        for (k in candidates) {
            val v = sp.getString(k, null)?.trim()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    // ✅ קבוצה לפי תאריך: מחזיר את הקבוצה הראשונה שמתאימה ליום בשבוע שיש לה slot
    fun groupForDate(dateIso: String): String? {
        val date = runCatching { LocalDate.parse(dateIso.trim(), isoFmt) }.getOrNull() ?: return null
        val wantedDow = date.toCalendarDow()

        val branches = readSelectedBranches().ifEmpty { listOf("נתניה – מרכז קהילתי אופק") }.take(3)
        val groups = groupsEffective()

        for (branchName in branches) {
            for (grp in groups) {
                val sched = TrainingDirectory.getSchedule(branchName, grp) ?: continue
                val slots = sched.slots ?: emptyList()
                for (slotAny in slots) {
                    val s = readSlot(slotAny)
                    if (s.dayOfWeek == wantedDow) {
                        return grp
                    }
                }
            }
        }
        return null
    }

    private fun LocalDate.toCalendarDow(): Int {
        return when (this.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> java.util.Calendar.SUNDAY
            java.time.DayOfWeek.MONDAY -> java.util.Calendar.MONDAY
            java.time.DayOfWeek.TUESDAY -> java.util.Calendar.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> java.util.Calendar.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> java.util.Calendar.THURSDAY
            java.time.DayOfWeek.FRIDAY -> java.util.Calendar.FRIDAY
            java.time.DayOfWeek.SATURDAY -> java.util.Calendar.SATURDAY
        }
    }

    data class SlotLike(
        val dayOfWeek: Int,
        val startHour: Int,
        val startMinute: Int,
        val durationMinutes: Int
    )

    private fun readSlot(slot: Any): SlotLike {
        val cls = slot::class.java

        fun <T : java.lang.reflect.AccessibleObject> T.acc(): T {
            runCatching { isAccessible = true }
            return this
        }

        val dayField = runCatching { cls.getDeclaredField("day").acc() }.getOrNull()
        val startField = runCatching { cls.getDeclaredField("start").acc() }.getOrNull()
        val endField = runCatching { cls.getDeclaredField("end").acc() }.getOrNull()

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

            val durMin =
                if (startLt != null && endLt != null)
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
                    val f = cls.getDeclaredField(n).acc()
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

    fun branchForDate(dateIso: String): String? {
        val date = runCatching { LocalDate.parse(dateIso.trim(), isoFmt) }.getOrNull() ?: return null
        val wantedDow = date.toCalendarDow()

        val branches = readSelectedBranches().ifEmpty { listOf("נתניה – מרכז קהילתי אופק") }.take(3)
        val groups = groupsEffective()

        for (branchName in branches) {
            for (grp in groups) {
                val sched = TrainingDirectory.getSchedule(branchName, grp) ?: continue
                val slots = sched.slots ?: emptyList()
                for (slotAny in slots) {
                    val s = readSlot(slotAny)
                    if (s.dayOfWeek == wantedDow) {
                        return branchName
                    }
                }
            }
        }
        return null
    }
}
