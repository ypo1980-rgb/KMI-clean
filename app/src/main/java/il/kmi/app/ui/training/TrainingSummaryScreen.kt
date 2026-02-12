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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableLongStateOf
import java.time.Instant
import java.time.ZoneId
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.graphics.Color

/**
 * פריט תרגיל "לבחירה" שמגיע מהקטלוג (ContentRepo).
 * אתה תבנה את הרשימה הזו מה-ContentRepo אצלך בנקודת החיבור (Route/NavGraph).
 */
data class ExercisePickItem(
    val exerciseId: String,
    val name: String,
    val topic: String
)

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
    onBack: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val scrollState = rememberLazyListState()

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
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->

        // ✅ רקע “גרניט” יוקרתי (טקסטורה עדינה + גרדיאנט)
        val granite = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.surface
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 1300f)
        )

        // שכבת "גרעיניות" עדינה באמצעות diagonal stripes שקופים
        val graniteNoise = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.00f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.030f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.00f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 1200f)
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

// חיפוש + בחירה מרובה
// -----------------------------
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("תרגילים שבוצעו", fontWeight = FontWeight.ExtraBold)

                            // =========================
                        // ✅ בחירת חגורה (קומבו)
                        // =========================
                        var beltOpen by remember { mutableStateOf(false) }

                        // התחל מה-belt שנשלח ל-Screen, אבל תן למשתמש לבחור
                        var selectedBelt by remember { mutableStateOf(belt) }

                        ExposedDropdownMenuBox(
                            expanded = beltOpen,
                            onExpandedChange = { beltOpen = !beltOpen }
                        ) {
                            OutlinedTextField(
                                value = beltHebLabel(selectedBelt),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("חגורה") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = beltOpen) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = beltOpen,
                                onDismissRequest = { beltOpen = false }
                            ) {
                                // ✅ Belt הוא enum אצלך
                                Belt.values().forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(beltHebLabel(b), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            selectedBelt = b
                                            beltOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // =========================
                        // ✅ Topics לפי חגורה
                        // =========================
                        val topics: List<String> = remember(selectedBelt) {
                            val viaBridge = runCatching {
                                il.kmi.app.search.KmiSearchBridge.topicTitlesFor(selectedBelt)
                            }.getOrDefault(emptyList())

                            if (viaBridge.isNotEmpty()) viaBridge
                            else {
                                runCatching {
                                    val sharedBelt: il.kmi.shared.domain.Belt =
                                        il.kmi.shared.domain.Belt.fromId(selectedBelt.id)
                                            ?: il.kmi.shared.domain.Belt.WHITE

                                    il.kmi.shared.domain.SubTopicRegistry
                                        .allForBelt(sharedBelt)
                                        .keys
                                        .toList()
                                }.getOrDefault(emptyList())
                            }
                        }

                        var topic by rememberSaveable { mutableStateOf(topics.firstOrNull().orEmpty()) }
                        var subTopic by rememberSaveable { mutableStateOf("") }

                        // אם החליפו חגורה וה-topic לא קיים יותר – נאפס
                        LaunchedEffect(selectedBelt, topics) {
                            if (topic.isBlank() || topic !in topics) {
                                topic = topics.firstOrNull().orEmpty()
                                subTopic = ""
                            }
                        }

                        // =========================
                        // ✅ SubTopics לפי נושא
                        // =========================
                            val subTopics: List<String> = remember(selectedBelt, topic) {
                                runCatching {
                                    il.kmi.app.domain.ContentRepo
                                        .listSubTopicTitles(selectedBelt, topic)
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                }.getOrDefault(emptyList())
                            }

                            LaunchedEffect(topic, subTopics) {
                            if (subTopic.isNotBlank() && subTopic !in subTopics) subTopic = ""
                        }

                        // ===== Topic dropdown =====
                        var topicOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = topicOpen,
                            onExpandedChange = { topicOpen = !topicOpen }
                        ) {
                            OutlinedTextField(
                                value = topic,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("נושא") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicOpen) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = topicOpen,
                                onDismissRequest = { topicOpen = false }
                            ) {
                                topics.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            topic = t
                                            subTopic = ""
                                            topicOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // ===== SubTopic dropdown (אם יש) =====
                        if (subTopics.isNotEmpty()) {
                            var subOpen by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = subOpen,
                                onExpandedChange = { subOpen = !subOpen }
                            ) {
                                OutlinedTextField(
                                    value = if (subTopic.isBlank()) "כל תתי הנושאים" else subTopic,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("תת-נושא") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subOpen) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = subOpen,
                                    onDismissRequest = { subOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("כל תתי הנושאים") },
                                        onClick = {
                                            subTopic = ""
                                            subOpen = false
                                        }
                                    )
                                    subTopics.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            onClick = {
                                                subTopic = st
                                                subOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ✅ חיפוש (מסנן בתוך הנושא/תת-נושא שנבחרו)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.searchQuery,
                            onValueChange = { vm.setSearchQuery(it) },
                            label = { Text("חיפוש תרגיל (בתוך הנושא)") },
                            singleLine = true
                        )

                        // ✅ שליפת תרגילים מהמאגר לפי חגורה/נושא/תת-נושא
                            val rawItems: List<String> = remember(selectedBelt, topic, subTopic) {
                                runCatching {
                                    il.kmi.app.domain.ContentRepo.listItemTitles(
                                        belt = selectedBelt,
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

                            // =========================
                            // ✅ בחירה מרובה עם “אשר”
                            // =========================
                            var pendingPicks by remember {
                                mutableStateOf<LinkedHashMap<String, ExercisePickItem>>(linkedMapOf())
                            }

                            Text(
                                text = "נמצאו ${filteredItems.size} · מסומנים ${state.selected.size} · להוספה ${pendingPicks.size}",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ניקוי בחירות זמניות
                                if (pendingPicks.isNotEmpty()) {
                                    TextButton(
                                        onClick = { pendingPicks = linkedMapOf() }
                                    ) {
                                        Text("נקה בחירה")
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }

                                FilledTonalButton(
                                    onClick = {
                                        // מוסיפים את כל מה שסומן זמנית, בלי לשכפל
                                        pendingPicks.values.forEach { p ->
                                            if (!state.selected.containsKey(p.exerciseId)) {
                                                vm.toggleExercise(p)
                                            }
                                        }
                                        pendingPicks = linkedMapOf()
                                    },
                                    enabled = pendingPicks.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("אשר", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Divider()

                            // ✅ הרשימה מוצגת כבחירה (ללא הגבלה)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                userScrollEnabled = true
                            ) {
                                items(filteredItems, key = { it }) { name ->
                                    val id = "${selectedBelt.id}|$topic|$subTopic|$name"

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
                                            // אם כבר נבחר קבוע - משנים דרך ה-VM (הסרה/הוספה)
                                            if (alreadySelected) {
                                                vm.toggleExercise(
                                                    ExercisePickItem(
                                                        exerciseId = id,
                                                        name = name,
                                                        topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                                                    )
                                                )
                                            } else {
                                                // אחרת - רק מסמנים/מסירים זמני
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
                        }

                    }
                }

            item {
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
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
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Header יפה
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "התרגילים שתורגלו באימון היום",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "סה\"כ ${state.selected.size} תרגילים",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // קו הפרדה עדין
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(999.dp)
                            ) { Spacer(Modifier.height(2.dp)) }

                            val selectedList = state.selected.values.toList()
                                .sortedBy { it.name.lowercase() }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 520.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
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
            }

            // -----------------------------
            // סיכום חופשי (מאמן/מתאמן לפי role)
            // -----------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (state.isCoach) "סיכום השיעור" else "סיכום השיעור",
                            fontWeight = FontWeight.ExtraBold
                        )

                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            value = state.notes,
                            onValueChange = { vm.setNotes(it) },
                            label = {
                                Text(
                                    if (state.isCoach) "דגשים מקצועיים, ביצוע, מה לשפר…"
                                    else "איך היה האימון? מה הרגשת? מה לשפר…"
                                )
                            },
                            minLines = 4
                        )
                    }
                }
            }

                // -----------------------------
                // שמירה
                // -----------------------------
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            FilledTonalButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                onClick = {
                                    // ✅ שמירה רגילה
                                    vm.save()

                                    // ✅ סימון היום כ"יש סיכום"
                                    val key = "training_summary_days"
                                    val cur = sp.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
                                    cur.add(state.dateIso.trim())
                                    sp.edit().putStringSet(key, cur).apply()
                                },
                                enabled = !state.isSaving
                            ) {
                                Text(
                                    text = if (state.isSaving) "שומר..." else "שמירת סיכום",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(10.dp)) }
            } // LazyColumn
        } // Box
    } // Scaffold
} // TrainingSummaryScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingInfoCard(
    dateIso: String,
    onDateChange: (String) -> Unit,
    branchName: String,
    coachName: String,
    groupKey: String,
    errorText: String?,

    // ✅ חדש
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "אימון",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = prettyDate(dateIso),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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

            fun millisToIso(millis: Long): String {
                val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                return d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
            }

            var showDatePicker by rememberSaveable { mutableStateOf(false) }
            var initialMillis by rememberSaveable {
                mutableLongStateOf(isoToMillis(dateIso) ?: System.currentTimeMillis())
            }

            LaunchedEffect(dateIso) {
                isoToMillis(dateIso)?.let { initialMillis = it }
            }

            // ✅ כל שורת התאריך לחיצה -> פותח לוח שנה
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        },
                        indication = null
                    ) { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = dateIso,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = { Text("תאריך") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    trailingIcon = {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "בחר תאריך")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // ✅ חדש: כפתור מפורש לפתיחת היומן עם הסימונים (קריאת סיכומים)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                onClick = { showDatePicker = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("קריאת סיכומים (יומן)")
            }

            if (showDatePicker) {

                // ✅ נתחיל מהתאריך הנוכחי של המסך
                val initDate = remember(dateIso) {
                    runCatching { LocalDate.parse(dateIso.trim()) }.getOrNull() ?: LocalDate.now()
                }

                var viewYear  by rememberSaveable(initDate) { mutableStateOf(initDate.year) }
                var viewMonth by rememberSaveable(initDate) { mutableStateOf(initDate.monthValue) } // 1..12
                var pickedDay by rememberSaveable(initDate) { mutableStateOf(initDate.dayOfMonth) }

                // ✅ בכל פתיחה/מעבר חודש – נבקש מה-VM להביא ימים עם סיכום לחודש הזה
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

                                // Header חודש + ניווט
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

                                // ימי שבוע
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

                                // Grid ימים (עם סימונים)
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
                                onDateChange(picked.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)))
                                showDatePicker = false
                            }
                        ) { Text("בחר") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("ביטול") }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(branchName.ifBlank { "סניף: —" }) },
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(coachName.ifBlank { "מאמן: —" }) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(groupKey.ifBlank { "קבוצה: —" }) },
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                )
            }

            if (!errorText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.onErrorContainer,
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
            .clip(RoundedCornerShape(14.dp)),
        color = if (checked)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // ✅ טקסטים מימין (RTL)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = item.topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(10.dp))

            // ✅ צ'קבוקס בצד שמאל-ויזואלית אבל בקצה (RTL)
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
    // ✅ UX: שדה הערות נפתח רק כשצריך
    var notesOpen by rememberSaveable(item.exerciseId) {
        mutableStateOf(item.highlight.isNotBlank())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {

            // ===== Header (מודרני) =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {

                // טקסטים מימין (RTL)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = item.topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(10.dp))

                // ✅ כפתור "+"/הערות – מודרני
                FilledTonalIconButton(
                    onClick = { notesOpen = !notesOpen }
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = if (notesOpen) "סגור הערות" else "הוסף הערה"
                    )
                }

                Spacer(Modifier.width(8.dp))

                // ✅ מחיקה – אייקון פח במקום X
                OutlinedIconButton(
                    onClick = onRemove,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "מחק תרגיל",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ===== Preview when closed (אם יש הערה קיימת) =====
            if (!notesOpen && item.highlight.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = item.highlight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ===== Notes editor when open =====
            if (notesOpen) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = item.highlight,
                    onValueChange = { onHighlight(it) },
                    label = { Text("דגשים והערות לתרגיל") },
                    minLines = 2
                )
            }
        }
    }
}

@Composable
private fun rememberFilteredExercises(
    allExercises: List<ExercisePickItem>,
    query: String
): List<ExercisePickItem> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return allExercises
    return allExercises.filter { ex ->
        ex.name.lowercase().contains(q) || ex.topic.lowercase().contains(q)
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
