package il.kmi.app.screens.BeltQuestions

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.ExerciseCountsRegistry
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.localization.rememberIsEnglish
import il.kmi.app.screens.parseSearchKey
import il.kmi.app.ui.FloatingQuickMenu
import il.kmi.app.ui.QuickMenuTriggerMode
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.app.screens.PracticeMenuDialog
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import il.kmi.app.domain.color


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

private fun subTopicTitleForUi(title: String, isEnglish: Boolean): String {
    return if (isEnglish) {
        ExerciseTitlesEn.getOrSame(title.trim())
    } else {
        title
    }
}

private fun subjectTitleForUi(
    subjectId: String,
    fallbackHeb: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return fallbackHeb

    val sharedTranslated = ExerciseTitlesEn.get(fallbackHeb.trim())
    if (!sharedTranslated.isNullOrBlank()) return sharedTranslated

    return when (subjectId) {
        "general" -> "General"
        "hands_all" -> "Hand Techniques"
        "hands_root" -> "Hand Techniques"
        "kicks" -> "Kicks"
        "releases" -> "Releases"
        "releases_hugs" -> "Hugs"
        "defense_root" -> "Defenses"
        "knife_defense" -> "Knife Defense"
        "gun_threat_defense" -> "Gun Threat Defense"
        "stick_defense" -> "Stick Defense"
        "hands_strikes" -> "Hand Strikes"
        "hands_elbows" -> "Elbow Strikes"
        "hands_stick_rifle" -> "Stick / Rifle Strikes"
        "rolls_breakfalls" -> "Breakfalls and Rolls"
        "topic_ready_stance" -> "Ready Stance"
        "topic_ground_prep" -> "Groundwork Preparation"
        "topic_kavaler" -> "Kavaler"
        "def_internal_punches" -> "Internal Defenses - Punches"
        "def_internal_kicks" -> "Internal Defenses - Kicks"
        "def_external_punches" -> "External Defenses - Punches"
        "def_external_kicks" -> "External Defenses - Kicks"
        "releases_hands_hair_shirt" -> "Releases from Hand / Hair / Shirt Grabs"
        "releases_chokes" -> "Choke Releases"
        else -> fallbackHeb
    }
}

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

private fun beltTitleForUi(belt: Belt, isEnglish: Boolean): String =
    if (isEnglish) belt.en else belt.heb

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
    android.util.Log.e("KMI_TOPIC_SCREEN", "BeltQuestionsByTopicScreen LOADED")
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
    onOpenRandomPractice: (Belt) -> Unit = {},
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenFinalExam: (Belt) -> Unit = {},
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit = {},
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    rememberEnsureContentRepoInitialized()
    val isEnglish = rememberIsEnglish()
    val ctx = LocalContext.current
    val notePrefs = remember(ctx) { ctx.getSharedPreferences("kmi_exercise_notes", Context.MODE_PRIVATE) }

    // State לניהול התפריט המהיר
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }
    var effectiveBelt by rememberSaveable { mutableStateOf(Belt.GREEN) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    if (showPracticeMenu) {
        PracticeMenuDialog(
            canUseExtras = true,
            defaultBelt = effectiveBelt,
            onDismiss = { showPracticeMenu = false },
            onRandomPractice = { beltArg ->
                showPracticeMenu = false
                onOpenRandomPractice(beltArg)
            },
            onFinalExam = { beltArg ->
                showPracticeMenu = false
                onOpenFinalExam(beltArg)
            },
            onPracticeByTopics = { selection ->
                showPracticeMenu = false
                onPracticeByTopics(selection)
            },
            onPracticeByTopicSelected = { beltArg, topicArg ->
                showPracticeMenu = false
                onOpenRandomPracticeByTopic(beltArg, topicArg)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = beltTitleForUi(effectiveBelt, isEnglish),
                onHome = { },
                lockHome = false,
                showTopHome = false,
                onPickSearchResult = { key -> pickedKey = key },
                lockSearch = false,
                showBottomActions = true,
                centerTitle = true
            )
        },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. תוכן המסך (נגלל)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                TopicsBySubjectCard(
                    currentBelt = effectiveBelt,
                    onSubjectClick = { belt, subject ->
                        effectiveBelt = belt
                        onOpenSubject(belt, subject)
                    },
                    onOpenTopic = onOpenTopic,
                    onOpenTopicWithSub = onOpenTopicWithSub,
                    onOpenDefenseList = onOpenDefenseList,
                    onOpenHardSubjectRoute = onOpenHardSubjectRoute,
                    onQuickViewClick = {
                        quickMenuExpanded = true
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = { quickMenuExpanded = true },
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 10.dp,
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        effectiveBelt.color.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        effectiveBelt.color.copy(alpha = 0.10f),
                                        Color.White,
                                        effectiveBelt.color.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = null,
                            tint = effectiveBelt.color,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isEnglish) "Quick View" else "מבט מהיר",
                            fontWeight = FontWeight.Bold,
                            color = effectiveBelt.color,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(88.dp))
            }

            FloatingQuickMenu(
                belt = effectiveBelt,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 84.dp)
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                triggerMode = QuickMenuTriggerMode.BottomBar,
                includePractice = true,
                onWeakPoints = { onOpenWeakPoints(effectiveBelt) },
                onAllLists = { onOpenAllLists(effectiveBelt) },
                onPractice = { showPracticeMenu = true },
                onSummary = { onOpenSummaryScreen(effectiveBelt) },
                onVoice = { onOpenVoiceAssistant(effectiveBelt) },
                onPdf = { onOpenPdfMaterials(effectiveBelt) }
            )
        }

        // --- ניהול דיאלוגים (AlertDialog) ---
        pickedKey?.let { key ->
            val (belt, topic, item) = parseSearchKey(key)
            val baseDisplayName = ExerciseTitleFormatter.displayName(item).ifBlank { item }
            val displayName = if (isEnglish) {
                ExerciseTitlesEn.getOrSame(baseDisplayName)
            } else {
                baseDisplayName
            }
            val favoriteId = remember(item) { normalizeFavoriteId(item) }
            val favorites by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())
            val isFavorite = favorites.contains(favoriteId)
            val noteKey = "note_${belt.id}_${topic.trim()}_${favoriteId}"
            var noteText by remember { mutableStateOf(notePrefs.getString(noteKey, "").orEmpty()) }
            var showNoteEditor by remember { mutableStateOf(false) }
            val explanation = remember(belt, item) { findExplanationForHitLocal(belt, item, topic) }

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
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )

                            Text(
                                text = if (isEnglish) {
                                    "${ExerciseTitlesEn.getOrSame(topic)} • ${belt.en}"
                                } else {
                                    "$topic • ${belt.heb}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right
                            )
                        }

                        IconButton(
                            onClick = { showNoteEditor = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF42A5F5)
                            )
                        }

                        IconButton(
                            onClick = { FavoritesStore.toggle(favoriteId) }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Text(text = explanation, textAlign = TextAlign.Right)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pickedKey = null }) { Text(if (isEnglish) "Close" else "סגור") }
                }
            )
        }
    }
}

private fun subjectAccentColor(subjectId: String): Color =
    when (subjectId) {
        "defense_root" -> Color(0xFF5E35B1)      // סגול
        "hands_root" -> Color(0xFF00897B)        // טורקיז
        "releases" -> Color(0xFF1E88E5)          // כחול
        "releases_hugs" -> Color(0xFF3949AB)     // אינדיגו
        "rolls_breakfalls" -> Color(0xFF6D4C41)  // חום
        "topic_ready_stance" -> Color(0xFF43A047)// ירוק
        "topic_ground_prep" -> Color(0xFF8E24AA) // סגול בהיר
        "topic_kavaler" -> Color(0xFFFB8C00)     // כתום
        else -> Color(0xFF7E57C2)
    }

private fun subjectIconFor(subjectId: String): ImageVector =
    when (subjectId) {
        "defense_root" -> Icons.Filled.Security
        "hands_root" -> Icons.Filled.PanTool
        "releases" -> Icons.Filled.PanTool
        "releases_hugs" -> Icons.Filled.PanTool
        "rolls_breakfalls" -> Icons.Filled.Build
        "topic_ready_stance" -> Icons.Filled.CheckCircle
        "topic_ground_prep" -> Icons.Filled.Info
        "topic_kavaler" -> Icons.Filled.Build
        else -> Icons.Filled.Info
    }

@Composable
private fun SubjectRootCardPremium(
    title: String,
    subtitle: String = "",
    subjectId: String,
    countText: String,
    showLeftBadge: Boolean = false,
    onClick: () -> Unit
) {
    val isEnglish = rememberIsEnglish()
    val accent = remember(subjectId) { subjectAccentColor(subjectId) }
    val icon = remember(subjectId) { subjectIconFor(subjectId) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnglish) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (showLeftBadge) 34.dp else 26.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = countText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = accent,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Right,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = countText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            color = accent,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (showLeftBadge) 34.dp else 26.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
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
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit = { _, _ -> },
    onQuickViewClick: () -> Unit = {}
) {

    val isEnglish = rememberIsEnglish()

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

    fun formatCount(n: Int): String =
        if (isEnglish) "exercises $n" else "$n תרגילים"

    fun translateCardCountText(raw: String): String {
        if (!isEnglish) return raw

        return raw
            .replace(Regex("""(\d+)\s+תתי\s+נושאים"""), "sub-topics $1")
            .replace(Regex("""(\d+)\s+תרגילים"""), "exercises $1")
    }

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
        subjectCounts,
        isEnglish
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        ).map { card ->
            card.copy(
                title = subjectTitleForUi(
                    subjectId = card.id,
                    fallbackHeb = card.title,
                    isEnglish = isEnglish
                ),
                countText = translateCardCountText(card.countText)
            )
        }
    }

    val subjectsWithoutSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts,
        isEnglish
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withoutSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        ).map { card ->
            card.copy(
                title = subjectTitleForUi(
                    subjectId = card.id,
                    fallbackHeb = card.title,
                    isEnglish = isEnglish
                ),
                countText = translateCardCountText(card.countText)
            )
        }
    }

    val defenseRootCard = remember(totalDefense, defenseDialogCountsMap, isEnglish) {
        SubjectTopicsUiLogic.buildDefenseRootCard(
            totalDefense = totalDefense,
            formatCount = ::formatCount,
            subTopicsCount = defenseDialogCountsMap.size
        ).copy(
            title = subjectTitleForUi(
                subjectId = "defense_root",
                fallbackHeb = SubjectTopicsUiLogic.buildDefenseRootCard(
                    totalDefense = totalDefense,
                    formatCount = ::formatCount,
                    subTopicsCount = defenseDialogCountsMap.size
                ).title,
                isEnglish = isEnglish
            )
        )
    }

    val handsRootCard = remember(handsRootCount, handsPickCounts, isEnglish) {
        SubjectTopicsUiLogic.buildHandsRootCard(
            handsRootCount = handsRootCount,
            formatCount = ::formatCount,
            subTopicsCount = handsPickCounts.size
        ).copy(
            title = subjectTitleForUi(
                subjectId = "hands_root",
                fallbackHeb = SubjectTopicsUiLogic.buildHandsRootCard(
                    handsRootCount = handsRootCount,
                    formatCount = ::formatCount,
                    subTopicsCount = handsPickCounts.size
                ).title,
                isEnglish = isEnglish
            )
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val scrollState = rememberScrollState()

                val showScrollHint by remember {
                    derivedStateOf {
                        scrollState.value < scrollState.maxValue
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .padding(bottom = if (showScrollHint) 46.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Subjects (Categories)" else "נושאים (קטגוריות)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        color = Color(0xFF263238)
                    )

                if (!countsReady) {
                    Text(
                        text = if (isEnglish) "Loading data..." else "טוען נתונים…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))

                SubjectRootCardPremium(
                    title = defenseRootCard.title,
                    subtitle = "",
                    subjectId = "defense_root",
                    countText = defenseRootCard.countText,
                    showLeftBadge = true,
                    onClick = {
                        android.util.Log.e(
                            "KMI_NAV",
                            "TOPICS_CARD defense root -> open defense picker belt=${currentBelt.id}"
                        )
                        askDefense = true
                    }
                )

                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color(0x14000000),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                SubjectRootCardPremium(
                    title = handsRootCard.title,
                    subtitle = "",
                    subjectId = "hands_root",
                    countText = handsRootCard.countText,
                    showLeftBadge = true,
                    onClick = { askHands = true }
                )

                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = Color(0x14000000),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // ✅ מציגים נושאים עם תתי־נושאים
                subjectsWithSubTopicsCards.forEachIndexed { index, card ->
                    SubjectRootCardPremium(
                        title = card.title,
                        subtitle = "",
                        subjectId = card.id,
                        countText = card.countText,
                        onClick = {
                            if (card.id == "releases_hugs") {
                                onOpenHardSubjectRoute(currentBelt, "releases_hugs")
                            } else {
                                askSubTopicsForId = card.id
                            }
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = Color(0x14000000),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                // ✅ מציגים נושאים בלי תתי־נושאים
                    subjectsWithoutSubTopicsCards
                        .filter { it.id != "defenses" }
                        .forEachIndexed { index, card ->

                            val subject = subjects.firstOrNull { it.id == card.id } ?: return@forEachIndexed
                    SubjectRootCardPremium(
                        title = card.title,
                        subtitle = "",
                        subjectId = card.id,
                        countText = card.countText,
                        onClick = {
                            when (card.id) {
                                "topic_kavaler" -> {
                                    val action = SubjectTopicsUiLogic.buildOpenSubjectUiAction(
                                        subject = subject,
                                        currentBelt = currentBelt
                                    )

                                    val chosenBelt = action.chosenBelt

                                    android.util.Log.e(
                                        "KMI_NAV",
                                        "KAVALER HARD_ROUTE -> currentBelt=${currentBelt.id} chosenBelt=${chosenBelt.id} subjectId='topic_kavaler'"
                                    )

                                    onOpenHardSubjectRoute(chosenBelt, "topic_kavaler")
                                }

                                else -> {
                                    android.util.Log.e(
                                        "KMI_NAV",
                                        "NO_SUBTOPIC click -> title='${subject.titleHeb}' id='${subject.id}' currentBelt=${currentBelt.id}"
                                    )

                                    openSubjectSmart(subject)
                                }
                            }
                        }
                    )

                    if (index != subjectsWithoutSubTopicsCards.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = Color(0x14000000),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }

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
                    val handsDisplayPicks = remember(handsPicks, isEnglish) {
                        handsPicks.map { subTopicTitleForUi(it, isEnglish) }
                    }

                    HandsPickModeDialogModern(
                        picks = handsDisplayPicks,
                        counts = handsPickCounts,
                        onDismiss = { askHands = false },
                        onPick = { pickedDisplay: String ->
                            askHands = false

                            val picked = handsPicks.firstOrNull {
                                subTopicTitleForUi(it, isEnglish) == pickedDisplay
                            } ?: pickedDisplay

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

                    askSubTopicsForId?.let { id ->

                        val dialogData = remember(subjects, id) {
                            SubjectTopicsUiLogic.buildSubTopicsDialogData(
                                subjects = subjects,
                                id = id
                            )
                        }

                        val counts = subTopicsPickCountsBySubjectId[id].orEmpty()
                        val displayPicks = remember(dialogData.picks, isEnglish) {
                            dialogData.picks.map { subTopicTitleForUi(it.trim(), isEnglish) }
                        }


                        SubTopicsPickModeDialogModern(
                            title = dialogData.base?.titleHeb ?: if (isEnglish) "Sub topics" else "תתי נושאים",
                            picks = displayPicks,
                            counts = counts,
                            onDismiss = { askSubTopicsForId = null },
                            onPick = { pickedDisplay: String ->
                                askSubTopicsForId = null

                                val picked = dialogData.picks.firstOrNull {
                                    subTopicTitleForUi(it, isEnglish) == pickedDisplay
                                } ?: pickedDisplay

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
                if (showScrollHint) {


                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .zIndex(5f),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC6A1B9A),
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isEnglish) "Scroll down" else "גלול למטה",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            }
        }
    }
}