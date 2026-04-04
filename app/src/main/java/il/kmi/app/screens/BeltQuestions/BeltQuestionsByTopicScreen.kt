package il.kmi.app.screens.BeltQuestions

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.LocalTextStyle
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.FloatingBeltQuickMenu
import il.kmi.app.screens.parseSearchKey
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import il.kmi.app.domain.ExerciseCountsRegistry



// ✅ FIX: פונקציית נרמול אחת בלבד, ברמת קובץ (נראית לכולם, אין forward-ref ואין אמביגיוטי)
private fun normText(raw: String): String =
    raw
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

private fun releasesSectionIdFor(raw: String): String? {
    val t = normText(raw)

    return when (t) {
        "שחרור מתפיסות ידיים / שיער / חולצה" -> "releases_hands_hair_shirt"
        "שחרור מחניקות" -> "releases_chokes"
        "שחרור מחביקות" -> "releases_hugs"
        "חביקות גוף" -> "releases_hugs_body"
        "חביקות צוואר" -> "releases_hugs_neck"
        "חביקות זרוע" -> "releases_hugs_arm"
        else -> null
    }
}

private fun handsSectionIdFor(raw: String): String? {
    val t = normText(raw)

    return when (t) {
        "מכות יד" -> "hands_strikes"
        "מכות מרפק" -> "hands_elbows"
        "מכות במקל / רובה" -> "hands_stick_rifle"
        else -> null
    }
}

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

private fun findExplanationForHitLocal(
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
        if (
            got.isNotBlank() &&
            !got.startsWith("הסבר מפורט על") &&
            !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
}

@Composable
private fun rememberEnsureContentRepoInitialized() {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                ContentRepo.initIfNeeded()
            } catch (t: Throwable) {
                android.util.Log.e("KMI_DBG", "ContentRepo init failed", t)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeltQuestionsByTopicScreen(
    onOpenSubject: (Belt, SubjectTopic) -> Unit,
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit = { _, _ -> },
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit = {},
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    rememberEnsureContentRepoInitialized()

    val ctx = LocalContext.current
    val notePrefs = remember(ctx) {
        ctx.getSharedPreferences("kmi_exercise_notes", Context.MODE_PRIVATE)
    }

    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var effectiveBelt by rememberSaveable { mutableStateOf(Belt.GREEN) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "נושאים",
                onHome = { },
                lockHome = false,
                showTopHome = false,
                onPickSearchResult = { key ->
                    pickedKey = key
                },
                lockSearch = false,
                showBottomActions = true,
                centerTitle = true
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TopicsBySubjectCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    currentBelt = effectiveBelt,
                    onSubjectClick = { belt, subject ->
                        effectiveBelt = belt
                        onOpenSubject(belt, subject)
                    },
                    onOpenTopicWithSub = { belt, topic, sub ->
                        android.util.Log.e(
                            "KMI_NAV",
                            "SCREEN onOpenTopicWithSub belt=${belt.id} topic='$topic' sub='$sub'"
                        )
                        effectiveBelt = belt
                        onOpenTopicWithSub(belt, topic, sub)
                    },
                    onOpenDefenseList = { belt, kind, pick ->
                        onOpenDefenseList(belt, kind, pick)
                    },
                    onOpenHardSubjectRoute = { belt, subjectId ->
                        android.util.Log.e(
                            "KMI_NAV",
                            "SCREEN onOpenHardSubjectRoute belt=${belt.id} subjectId='$subjectId'"
                        )
                        effectiveBelt = belt
                        onOpenHardSubjectRoute(belt, subjectId)
                    }
                )

                Spacer(Modifier.height(90.dp))
            }

            if (quickMenuExpanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(998f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { quickMenuExpanded = false }
                )
            }

            FloatingBeltQuickMenu(
                belt = effectiveBelt,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, bottom = 22.dp)
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                onWeakPoints = { onOpenWeakPoints(effectiveBelt) },
                onAllLists = { onOpenAllLists(effectiveBelt) },
                onPractice = { },
                onSummary = { onOpenSummaryScreen(effectiveBelt) },
                onVoice = { onOpenVoiceAssistant(effectiveBelt) },
                onPdf = { onOpenPdfMaterials(effectiveBelt) }
            )

            pickedKey?.let { key ->
                val (belt, topic, item) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

                val favoriteId = remember(item) { normalizeFavoriteId(item) }
                val favorites: Set<String> by FavoritesStore
                    .favoritesFlow
                    .collectAsState(initial = emptySet())
                val isFavorite = favorites.contains(favoriteId)

                val noteKey = remember(belt, topic, favoriteId) {
                    "note_${belt.id}_${topic.trim()}_${favoriteId}"
                }

                var noteText by remember(noteKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }

                var showNoteEditor by remember { mutableStateOf(false) }

                val explanation = remember(belt, item) {
                    findExplanationForHitLocal(
                        belt = belt,
                        rawItem = item,
                        topic = topic
                    )
                }

                AlertDialog(
                    onDismissRequest = { pickedKey = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
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
                                    text = "${topic} • ${belt.heb}",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = {
                                    showNoteEditor = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "הערה",
                                    tint = Color(0xFF42A5F5)
                                )
                            }

                            IconButton(
                                onClick = {
                                    FavoritesStore.toggle(favoriteId)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "מועדפים",
                                    tint = if (isFavorite) Color(0xFFFFC107) else Color.Gray
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (noteText.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))

                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.LightGray.copy(alpha = 0.65f)
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "הערה של המתאמן:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pickedKey = null }) {
                            Text("סגור")
                        }
                    }
                )

                if (showNoteEditor) {
                    AlertDialog(
                        onDismissRequest = { showNoteEditor = false },
                        title = {
                            Text(
                                text = "הערה לתרגיל",
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                label = {
                                    Text("כתוב הערה")
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    notePrefs.edit()
                                        .putString(noteKey, noteText.trim())
                                        .apply()
                                    showNoteEditor = false
                                }
                            ) {
                                Text("שמור")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showNoteEditor = false }
                            ) {
                                Text("ביטול")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun TopicsBySubjectCard(
    modifier: Modifier = Modifier,
    currentBelt: Belt,
    onSubjectClick: (Belt, SubjectTopic) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit = { _, _ -> },
) {
    val subjects = il.kmi.app.domain.TopicsBySubjectRegistry.allSubjects()

// ✅ DEFENSE counts – source of truth מ-ExerciseCountsRegistry
    val defenseDialogCountsMap = remember {
        ExerciseCountsRegistry.defenseDialogCounts()
    }

    val defensePickCountsMap = remember {
        ExerciseCountsRegistry.defensePickCounts()
    }

    val totalDefense = remember {
        ExerciseCountsRegistry.totalDefenseCount()
    }

    val handsBase: SubjectTopic? = remember(subjects) {
        subjects.firstOrNull { it.id == "hands_all" }
    }

    var subjectCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var handsRootCount by remember(subjects) { mutableStateOf(0) }
    var countsReady by remember(subjects) { mutableStateOf(false) }
    var handsPickCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var uiSectionCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var askSubTopicsForId by rememberSaveable { mutableStateOf<String?>(null) }

    var subTopicsPickCountsBySubjectId by remember(subjects) {
        mutableStateOf<Map<String, Map<String, Int>>>(emptyMap())
    }

    var askDefense by rememberSaveable { mutableStateOf(false) }
    var askHands by rememberSaveable { mutableStateOf(false) }
    var askKind by rememberSaveable { mutableStateOf<il.kmi.app.domain.DefenseKind?>(null) }

    fun applyPayload(payload: SubjectTopicsUiLogic.TopicsUiCountsPayload) {
        subjectCounts = payload.subjectCounts
        handsRootCount = payload.handsRootCount
        handsPickCounts = payload.handsPickCounts
        uiSectionCounts = payload.uiSectionCounts
        subTopicsPickCountsBySubjectId = payload.subTopicsPickCountsBySubjectId
        countsReady = true
    }

    LaunchedEffect(subjects, handsBase) {
        // ✅ אם כבר יש cache בזיכרון — משתמשים בו מיידית בלי חישוב מחדש
        SubjectTopicsUiLogic.getCachedTopicsUiCountsPayload()?.let { cached ->
            applyPayload(cached)
            return@LaunchedEffect
        }

        val payload = withContext(Dispatchers.Default) {
            SubjectTopicsUiLogic.buildTopicsUiCountsPayload(
                subjects = subjects,
                handsBase = handsBase
            )
        }

        SubjectTopicsUiLogic.cacheTopicsUiCountsPayload(payload)
        applyPayload(payload)
    }

    // ----------------- UI -----------------

    fun formatCount(n: Int): String = "$n תרגילים"

    fun openSubjectSmart(subject: SubjectTopic) {
        val action = SubjectTopicsUiLogic.buildOpenSubjectUiAction(
            subject = subject,
            currentBelt = currentBelt
        )

        android.util.Log.e("KMI_NAV", action.logMessage)

        onSubjectClick(action.chosenBelt, subject)
    }

    val visibleSubjectsSplit = remember(subjects, uiSectionCounts) {
        SubjectTopicsUiLogic.splitVisibleSubjects(
            subjects = subjects,
            sectionCounts = uiSectionCounts
        )
    }

    val subjectsWithSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        )
    }

    val subjectsWithoutSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withoutSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        )
    }

    val defenseRootCard = remember(totalDefense) {
        SubjectTopicsUiLogic.buildDefenseRootCard(
            totalDefense = totalDefense,
            formatCount = ::formatCount
        )
    }

    val handsRootCard = remember(handsRootCount) {
        SubjectTopicsUiLogic.buildHandsRootCard(
            handsRootCount = handsRootCount,
            formatCount = ::formatCount
        )
    }

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "נושאים (קטגוריות)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF263238)
            )

            if (!countsReady) {
                Text(
                    text = "טוען נתונים…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SubjectRootCard(
                title = defenseRootCard.title,
                subtitle = defenseRootCard.subtitle,
                countText = defenseRootCard.countText,
                showLeftBadge = true,
                onClick = { askDefense = true }
            )

            SubjectRootCard(
                title = handsRootCard.title,
                subtitle = handsRootCard.subtitle,
                countText = handsRootCard.countText,
                showLeftBadge = true,
                onClick = { askHands = true }
            )

            // ✅ מציגים נושאים עם תתי־נושאים
            subjectsWithSubTopicsCards.forEach { card ->
                SubjectRootCard(
                    title = card.title,
                    subtitle = card.subtitle,
                    countText = card.countText,
                    onClick = {
                        if (card.id == "releases_hugs") {
                            onOpenHardSubjectRoute(currentBelt, "releases_hugs")
                        } else {
                            askSubTopicsForId = card.id
                        }
                    }
                )
            }

            // ✅ מציגים נושאים בלי תתי־נושאים (באותו סטייל של שאר הנושאים)
            subjectsWithoutSubTopicsCards.forEach { card ->
                val subject = subjects.firstOrNull { it.id == card.id } ?: return@forEach

                SubjectRootCard(
                    title = card.title,
                    subtitle = card.subtitle,
                    countText = card.countText,
                    onClick = { openSubjectSmart(subject) }
                )
            }

            // ----------------- דיאלוגים -----------------

            if (askDefense) {
                DefenseCategoryPickDialogModern(
                    counts = defenseDialogCountsMap,
                    onDismiss = { askDefense = false },
                    onPick = { picked ->
                        askDefense = false

                        when (val decision = SubjectTopicsUiLogic.resolveDefenseDialogPick(picked)) {
                            is SubjectTopicsUiLogic.DefenseDialogDecision.AskKind -> {
                                askKind = decision.kind
                            }

                            is SubjectTopicsUiLogic.DefenseDialogDecision.OpenHardSubject -> {
                                android.util.Log.e(
                                    "KMI_NAV",
                                    "Defense dialog -> hard subject '${decision.subjectId}'"
                                )
                                android.util.Log.e(
                                    "KMI_NAV",
                                    "TOPICS_CARD before onOpenHardSubjectRoute belt=${currentBelt.id} subjectId='${decision.subjectId}'"
                                )
                                onOpenHardSubjectRoute(currentBelt, decision.subjectId)
                            }

                            SubjectTopicsUiLogic.DefenseDialogDecision.None -> Unit
                        }
                    }
                )
            }

            // ✅ FIX: זה היה חסר ולכן לחיצה על "הגנות פנימיות/חיצוניות" לא הובילה לשום מקום
            askKind?.let { kind ->
                DefensePickModeDialogModern(
                    kind = kind,
                    counts = defensePickCountsMap,
                    onDismiss = { askKind = null },
                    onPick = { picked ->
                        askKind = null

                        when (val decision = SubjectTopicsUiLogic.resolveDefenseKindPick(kind, picked)) {
                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenLegacyDefenses -> {
                                onOpenDefenseList(
                                    currentBelt,
                                    decision.kind,
                                    decision.pick
                                )
                            }

                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenHardSubject -> {
                                android.util.Log.e(
                                    "KMI_NAV",
                                    "TOPICS_CARD kind before onOpenHardSubjectRoute belt=${currentBelt.id} subjectId='${decision.subjectId}'"
                                )
                                onOpenHardSubjectRoute(currentBelt, decision.subjectId)
                            }

                            SubjectTopicsUiLogic.DefenseKindPickDecision.None -> Unit
                        }
                    }
                )
            }

            if (askHands) {
                val handsPicks = remember(handsBase) {
                    SubjectTopicsUiLogic.handsPicks(handsBase)
                }

                HandsPickModeDialogModern(
                    picks = handsPicks,
                    counts = handsPickCounts,
                    onDismiss = { askHands = false },
                    onPick = { picked ->
                        askHands = false

                        val hardSubjectId = handsSectionIdFor(picked)

                        if (hardSubjectId != null) {
                            android.util.Log.e(
                                "KMI_NAV",
                                "Hands dialog -> hard subject '$hardSubjectId' picked='$picked'"
                            )

                            onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                        } else {
                            val subject = SubjectTopicsUiLogic.resolveHandsPick(
                                base = handsBase,
                                picked = picked
                            )

                            if (subject != null) {
                                android.util.Log.e(
                                    "KMI_NAV",
                                    "Hands tmp subject: id='${subject.id}' title='${subject.titleHeb}' hint='${subject.subTopicHint}' topicsByBelt=${subject.topicsByBelt}"
                                )

                                openSubjectSmart(subject)
                            } else {
                                android.util.Log.e("KMI_NAV", "Hands base is NULL -> cannot navigate")
                            }
                        }
                    }
                )
            }

            // ✅ SubTopics dialog for regular subjects (חייב להיות בתוך ה-Column כאן)
            askSubTopicsForId?.let { id ->

                val dialogData = remember(subjects, id) {
                    SubjectTopicsUiLogic.buildSubTopicsDialogData(
                        subjects = subjects,
                        id = id
                    )
                }

                val counts = subTopicsPickCountsBySubjectId[id].orEmpty()

                SubTopicsPickModeDialogModern(
                    title = dialogData.base?.titleHeb ?: "תתי נושאים",
                    picks = dialogData.picks,
                    counts = counts,
                    onDismiss = { askSubTopicsForId = null },
                    onPick = { picked ->
                        askSubTopicsForId = null

                        val decision = SubjectTopicsUiLogic.resolveSubTopicPick(
                            base = dialogData.base,
                            bodyHugsChild = dialogData.bodyHugsChild,
                            picked = picked,
                            norm = ::normText
                        )

                        when (decision) {
                            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenTopicWithSub -> {
                                android.util.Log.e(
                                    "KMI_NAV",
                                    "SubTopic decision -> onOpenTopicWithSub(${decision.topic}, ${decision.subTopic})"
                                )

                                onOpenTopicWithSub(
                                    currentBelt,
                                    decision.topic,
                                    decision.subTopic
                                )
                            }

                            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenSubject -> {
                                val hardSubjectId = when (decision.subject.id) {
                                    "releases" -> releasesSectionIdFor(picked)
                                    else -> null
                                }

                                if (hardSubjectId != null) {
                                    android.util.Log.e(
                                        "KMI_NAV",
                                        "SubTopic decision DIRECT_HARD_SUBJECT -> subjectId='$hardSubjectId' picked='$picked'"
                                    )

                                    onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                                } else {
                                    android.util.Log.e(
                                        "KMI_NAV",
                                        "SubTopic decision -> openSubjectSmart id='${decision.subject.id}' title='${decision.subject.titleHeb}'"
                                    )

                                    openSubjectSmart(decision.subject)
                                }
                            }

                            SubjectTopicsUiLogic.SubTopicPickDecision.None -> Unit
                        }
                    }
                )
            }
        }
    }
}
