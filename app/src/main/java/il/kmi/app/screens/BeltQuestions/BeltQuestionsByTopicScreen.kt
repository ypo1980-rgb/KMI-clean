package il.kmi.app.screens.BeltQuestions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.ui.FloatingBeltQuickMenu
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import il.kmi.shared.domain.content.SubjectItemsResolver.UiSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import il.kmi.app.navigation.defenses.defenseDialogCounts
import il.kmi.app.navigation.defenses.defensePickCounts
import il.kmi.app.navigation.defenses.kicksHardSubCounts
import il.kmi.app.navigation.defenses.totalDefenseCount

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeltQuestionsByTopicScreen(
    onOpenSubject: (Belt, SubjectTopic) -> Unit,
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onBackHome: () -> Unit,
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit = {},
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }
    var effectiveBelt by rememberSaveable { mutableStateOf(Belt.GREEN) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                ContentRepo.initIfNeeded()
            } catch (t: Throwable) {
                android.util.Log.e("KMI_DBG", "ContentRepo init failed", t)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "נושאים",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    onOpenTopic = { belt, topic ->
                        effectiveBelt = belt
                        onOpenTopic(belt, topic)
                    },
                    onOpenDefenseList = { belt, kind, pick ->
                        onOpenDefenseList(belt, kind, pick)
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
                onPractice = { showPracticeMenu = true },
                onSummary = { onOpenSummaryScreen(effectiveBelt) },
                onVoice = { onOpenVoiceAssistant(effectiveBelt) },
                onPdf = { onOpenPdfMaterials(effectiveBelt) }
            )
        }
    }
}

@Composable
internal fun TopicsBySubjectCard(
    modifier: Modifier = Modifier,
    currentBelt: Belt,
    onSubjectClick: (Belt, SubjectTopic) -> Unit = { _, _ -> },
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> }
) {
    val subjects = il.kmi.app.domain.TopicsBySubjectRegistry.allSubjects()

    // ✅ DEFENSE counts – source of truth מהקובץ defenses (לא state, לא LaunchedEffect)
    val defenseDialogCountsMap = remember { defenseDialogCounts() }
    val defensePickCountsMap = remember { defensePickCounts() }
    val kicksSubCountsMap = remember { kicksHardSubCounts() }
    val totalDefense = remember { totalDefenseCount() }

    // ✅ חייב להיות לפני כל שימוש (כדי שלא יהיה unresolved reference)
    fun resolveSectionsForSubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<UiSection> {
        val res = SubjectItemsResolver.resolveBySubject(
            belt = belt,
            subject = subject.toSharedSubject()
        )

        if (subject.id == "hands_all" || subject.titleHeb.contains("עבודת ידיים")) {
            android.util.Log.e(
                "KMI_RES",
                "resolveSectionsForSubject: belt=$belt subjectId='${subject.id}' title='${subject.titleHeb}' hint='${subject.subTopicHint}' " +
                        "topicsByBelt=${subject.topicsByBelt[belt]} sections=${res.size} items=${res.sumOf { it.items.size }}"
            )
        }

        return res
    }

    fun countUiTitlesForSubject(subject: SubjectTopic): Int {
        val all = mutableSetOf<String>()
        subject.topicsByBelt.keys.forEach { belt ->
            resolveSectionsForSubject(belt, subject)
                .asSequence()
                .flatMap { it.items.asSequence() }
                .map { it.canonicalId }
                .forEach { all += it }
        }
        return all.size
    }

    // ✅ NEW: base יחיד לעבודת ידיים (עם 2 תתי־נושאים)
    val handsBase: SubjectTopic? = remember(subjects) {
        subjects.firstOrNull { it.id == "hands_all" } // <- זה ה-id שהגדרת ב-TopicsBySubjectRegistry
    }

    // ----------------- STATE (רק מה שבאמת צריך לרינדור) -----------------

    var subjectCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var handsRootCount by remember(subjects) { mutableStateOf(0) }
    var countsReady by remember(subjects) { mutableStateOf(false) }
    var handsPickCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var uiSectionCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var askSubTopicsForId by rememberSaveable { mutableStateOf<String?>(null) }

    var subTopicsPickCountsBySubjectId by remember(subjects) {
        mutableStateOf<Map<String, Map<String, Int>>>(emptyMap())
    }

    // ✅ FIX: עבודת ידיים חייבת למפות לטופיקים הנכונים בכל חגורה (לפי pick)
    fun handsSubjectForPick(base: SubjectTopic, pick: String): SubjectTopic {
        val p = pick.trim()

        return when (p) {
            "מכות יד" -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = p,
                topicsByBelt = mapOf(
                    Belt.YELLOW to listOf("עבודת ידיים", "מכות ידיים", "מכות יד"),
                    Belt.ORANGE to listOf("עבודת ידיים", "מכות יד", "מכות ידיים")
                )
            )

            "מכות מרפק" -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = p,
                topicsByBelt = mapOf(
                    Belt.GREEN to listOf("מכות מרפק")
                )
            )

            else -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = p
            )
        }
    }

    // ----------------- background counts (רק Subjects + Hands) -----------------

    LaunchedEffect(subjects, currentBelt, handsBase) {
        data class Payload(
            val subjCounts: Map<String, Int>,
            val handsRoot: Int,
            val handsPicks: Map<String, Int>,
            val sectionCounts: Map<String, Int>,
            val subTopicsCounts: Map<String, Map<String, Int>>
        )

        val p = withContext(Dispatchers.Default) {

            val subjCounts = subjects.associate { s -> s.id to countUiTitlesForSubject(s) }

            // ✅ חשוב: משתמשים בדיוק ב-subTopics של handsBase אם קיימים (כדי למנוע mismatch)
            val handsPicksOrder: List<String> =
                handsBase?.subTopics?.takeIf { it.isNotEmpty() }
                    ?: listOf("מכות יד", "מכות מרפק")

            val handsPickCountsLocal: Map<String, Int> =
                handsPicksOrder.associateWith { pick ->
                    val base = handsBase
                    if (base == null) 0
                    else {
                        val tmp = handsSubjectForPick(base, pick)
                        countUiTitlesForSubject(tmp)
                    }
                }

            val handsRoot: Int = run {
                val base = handsBase ?: return@run 0
                val all = linkedSetOf<String>()

                handsPicksOrder.forEach { pick ->
                    val tmp = handsSubjectForPick(base, pick)

                    tmp.topicsByBelt.keys.forEach { b: Belt ->
                        resolveSectionsForSubject(b, tmp)
                            .asSequence()
                            .flatMap { it.items.asSequence() }
                            .map { it.canonicalId }
                            .forEach { all += it }
                    }
                }

                all.size
            }

            val sectionCounts: Map<String, Int> =
                subjects.associate { s -> s.id to s.subTopics.size }

            val subTopicsCounts: Map<String, Map<String, Int>> =
                subjects
                    .asSequence()
                    .filter { it.subTopics.isNotEmpty() }
                    .associate { base ->
                        val countsForBase: Map<String, Int> =
                            base.subTopics.associateWith { pick ->
                                val tmp =
                                    if (base.id == "hands_all") handsSubjectForPick(base, pick)
                                    else base.copy(
                                        titleHeb = "${base.titleHeb} - $pick",
                                        subTopicHint = pick
                                    )

                                countUiTitlesForSubject(tmp)
                            }
                        base.id to countsForBase
                    }

            Payload(
                subjCounts = subjCounts,
                handsRoot = handsRoot,
                handsPicks = handsPickCountsLocal,
                sectionCounts = sectionCounts,
                subTopicsCounts = subTopicsCounts
            )
        }

        subjectCounts = p.subjCounts
        handsRootCount = p.handsRoot
        handsPickCounts = p.handsPicks
        uiSectionCounts = p.sectionCounts
        subTopicsPickCountsBySubjectId = p.subTopicsCounts

        countsReady = true
    }

    // ----------------- UI -----------------

    fun formatCount(n: Int): String = "$n תרגילים"

    // ✅ NEW: פתיחה חכמה של subject – בוחר חגורה שיש בה תרגילים בפועל
    fun beltsWithItemsForSubject(subject: SubjectTopic): List<Belt> {
        return subject.topicsByBelt.keys
            .asSequence()
            .filter { b ->
                resolveSectionsForSubject(b, subject)
                    .asSequence()
                    .flatMap { it.items.asSequence() }
                    .any()
            }
            .toList()
    }

    fun openSubjectSmart(subject: SubjectTopic) {
        val nonEmptyBelts = beltsWithItemsForSubject(subject)

        val preferredOrder: List<Belt> = when {
            subject.id == "hands_all" && subject.subTopicHint == "מכות יד" -> listOf(Belt.YELLOW, Belt.ORANGE, currentBelt, Belt.GREEN)
            subject.id == "hands_all" && subject.subTopicHint == "מכות מרפק" -> listOf(Belt.GREEN, currentBelt, Belt.YELLOW, Belt.ORANGE)
            else -> emptyList()
        }

        val chosenBelt =
            preferredOrder.firstOrNull { it in nonEmptyBelts }
                ?: nonEmptyBelts.firstOrNull()
                ?: preferredOrder.firstOrNull { it in subject.topicsByBelt.keys }
                ?: subject.topicsByBelt.keys.firstOrNull()
                ?: currentBelt

        // ✅ לוג ניווט + בדיקה מה resolver מחזיר בפועל לפני הניווט
        val secs = resolveSectionsForSubject(chosenBelt, subject)
        val itemsCount = secs.sumOf { it.items.size }
        val sample = secs
            .asSequence()
            .flatMap { it.items.asSequence() }
            .take(8)
            .map { it.canonicalId }
            .toList()

        android.util.Log.e(
            "KMI_NAV",
            "openSubjectSmart: title='${subject.titleHeb}' id='${subject.id}' hint='${subject.subTopicHint}' " +
                    "chosenBelt=$chosenBelt nonEmptyBelts=$nonEmptyBelts topicsByBelt=${subject.topicsByBelt} " +
                    "resolverSections=${secs.size} resolverItems=$itemsCount sample=$sample"
        )

        onSubjectClick(chosenBelt, subject)
    }

    fun subjectForPick(base: SubjectTopic, pick: String): SubjectTopic {
        return base.copy(
            titleHeb = "${base.titleHeb} - $pick",
            subTopicHint = pick
        )
    }

    // ✅ דיאלוגים / מצבים (חזרו – היו חסרים ולכן unresolved)
    var askDefense by rememberSaveable { mutableStateOf(false) }
    var askHands by rememberSaveable { mutableStateOf(false) }
    var askKicksSubTopics by rememberSaveable { mutableStateOf(false) }
    var askKind by rememberSaveable { mutableStateOf<il.kmi.app.domain.DefenseKind?>(null) }

    fun isDefenseChild(s: SubjectTopic): Boolean {
        val t = s.titleHeb.trim()
        val isWeaponOrKicks =
            t.contains("בעיטות") || t.contains("סכין") || t.contains("אקדח") || t.contains("מקל")
        val isOldInternalExternal =
            (t.contains("הגנות פנימיות") || t.contains("הגנות חיצוניות")) &&
                    (t.contains("אגרופים") || t.contains("בעיטות"))
        return t.contains("הגנות") && (isWeaponOrKicks || isOldInternalExternal)
    }

    fun isHandsChild(s: SubjectTopic): Boolean {
        val t = s.titleHeb.trim()
        if (t == "עבודת ידיים") return true
        return t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–"))
    }

    val visibleSubjects = remember(subjects) {
        subjects.filter { !isDefenseChild(it) && !isHandsChild(it) }
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
                title = "הגנות",
                subtitle = "בעיטות / סכין / אקדח / מקל",
                countText = "6 תתי נושאים\n${formatCount(totalDefense)}",
                showLeftBadge = true,
                onClick = { askDefense = true }
            )

            SubjectRootCard(
                title = "עבודת ידיים",
                subtitle = "מכות יד / מכות מרפק",
                countText = "2 תתי נושאים\n${formatCount(handsRootCount)}",
                showLeftBadge = true,
                onClick = { askHands = true }
            )

            // שאר הנושאים (✅ עם תתי־נושאים קודם, בלי תתי־נושאים אחרונים)
            val (subjectsWithSubTopics, subjectsWithoutSubTopics) = remember(
                visibleSubjects, uiSectionCounts
            ) {
                visibleSubjects.partition { s ->
                    val subCount = uiSectionCounts[s.id] ?: s.subTopics.size
                    subCount > 0
                }
            }

            // ✅ מציגים נושאים עם תתי־נושאים
            subjectsWithSubTopics.forEach { s ->
                val subCount = uiSectionCounts[s.id] ?: s.subTopics.size
                val exCount = subjectCounts[s.id] ?: 0

                SubjectRootCard(
                    title = s.titleHeb,
                    subtitle = "בחר תת־נושא",
                    countText = "$subCount תתי נושאים\n${formatCount(exCount)}",
                    onClick = { askSubTopicsForId = s.id }
                )
            }

            // ✅ מציגים נושאים בלי תתי־נושאים (באותו סטייל של שאר הנושאים)
            subjectsWithoutSubTopics.forEach { s ->
                val exCount = subjectCounts[s.id] ?: 0

                SubjectRootCard(
                    title = s.titleHeb,
                    subtitle = "כניסה לתרגילים",
                    countText = formatCount(exCount), // ✅ אותו “כתב תרגילים” כמו כולם
                    onClick = { openSubjectSmart(s) }
                )
            }

            // ----------------- דיאלוגים -----------------

            if (askDefense) {
                DefenseCategoryPickDialogModern(
                    counts = defenseDialogCountsMap,
                    onDismiss = { askDefense = false },
                    onPick = { picked ->
                        askDefense = false
                        when (picked) {
                            "internal" -> askKind = il.kmi.app.domain.DefenseKind.INTERNAL
                            "external" -> askKind = il.kmi.app.domain.DefenseKind.EXTERNAL
                            "kicks" -> askKicksSubTopics = true
                            "knife" -> onOpenDefenseList(currentBelt, "knife_hard", "all")
                            "gun" -> onOpenDefenseList(currentBelt, "gun_hard", "all")
                            "stick" -> onOpenDefenseList(currentBelt, "stick_hard", "all")
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

                        val kindKey = when (kind) {
                            il.kmi.app.domain.DefenseKind.INTERNAL -> "internal"
                            il.kmi.app.domain.DefenseKind.EXTERNAL -> "external"
                            else -> "all"
                        }

                        val pickKey = when {
                            picked.contains("אגרופ") -> "punch"
                            picked.contains("בעיט") -> "kick"
                            else -> "punch"
                        }

                        onOpenDefenseList(currentBelt, kindKey, pickKey)
                    }
                )
            }

            if (askHands) {
                val handsPicks = remember(handsBase) {
                    handsBase?.subTopics?.takeIf { it.isNotEmpty() }
                        ?: listOf("מכות יד", "מכות מרפק")
                }

                HandsPickModeDialogModern(
                    picks = handsPicks,
                    counts = handsPickCounts,
                    onDismiss = { askHands = false },
                    onPick = { picked ->
                        askHands = false
                        val base = handsBase
                        if (base != null) {
                            val tmp = handsSubjectForPick(base, picked)

                            android.util.Log.e(
                                "KMI_NAV",
                                "Hands tmp subject: id='${tmp.id}' title='${tmp.titleHeb}' hint='${tmp.subTopicHint}' topicsByBelt=${tmp.topicsByBelt}"
                            )

                            openSubjectSmart(tmp)
                        } else {
                            android.util.Log.e("KMI_NAV", "Hands base is NULL -> cannot navigate")
                        }
                    }
                )
            }

            if (askKicksSubTopics) {
                val picks = remember {
                    listOf(
                        "הגנות נגד בעיטות ישרות / למפשעה",
                        "הגנות נגד מגל / מגל לאחור",
                        "הגנות נגד ברך",
                    )
                }

                KicksSubTopicsPickDialogModern(
                    picks = picks,
                    counts = kicksSubCountsMap,
                    onDismiss = { askKicksSubTopics = false },
                    onPick = { picked ->
                        android.util.Log.e("KMI_DBG", "KICKS_SUB pick='$picked' -> navigate")
                        askKicksSubTopics = false

                        val pickKey = when (picked) {
                            "הגנות נגד בעיטות ישרות / למפשעה" -> "straight_groin"
                            "הגנות נגד מגל / מגל לאחור" -> "hook_back"
                            "הגנות נגד ברך" -> "knee"
                            else -> "straight_groin"
                        }

                        onOpenDefenseList(currentBelt, "kicks_hard", pickKey)
                    }
                )
            }

            askSubTopicsForId?.let { id: String ->
                val base = remember(subjects, id) {
                    subjects.firstOrNull { it.id == id }
                }

                val bodyHugsChild: SubjectTopic? = remember(subjects, base) {
                    if (base?.id == "releases") subjects.firstOrNull { it.id == "releases_body_hugs" } else null
                }

                val picks = remember(base, bodyHugsChild) {
                    when {
                        base == null -> emptyList()
                        bodyHugsChild != null -> base.subTopics + bodyHugsChild.titleHeb
                        else -> base.subTopics
                    }
                }

                val counts = subTopicsPickCountsBySubjectId[id].orEmpty()

                SubTopicsPickModeDialogModern(
                    title = base?.titleHeb ?: "תתי נושאים",
                    picks = picks,
                    counts = counts,
                    onDismiss = { askSubTopicsForId = null },
                    onPick = { picked ->
                        askSubTopicsForId = null
                        if (base != null) {
                            if (bodyHugsChild != null && picked == bodyHugsChild.titleHeb) {
                                openSubjectSmart(bodyHugsChild)
                            } else {
                                val tmp = subjectForPick(base, picked)
                                openSubjectSmart(tmp)
                            }
                        }
                    }
                )
            }
        } // Column
    } // Surface
} // TopicsBySubjectCard

// ----------------------------- Cards (Root) - TOP LEVEL -----------------------------

@Composable
private fun BaseTopicCard(
    title: String,
    subtitle: String,
    accent: Color,
    countText: String? = null,
    onClick: () -> Unit
) {
    val parts = countText
        ?.split("\n")
        ?.map { it.trim() }
        .orEmpty()

    val badgeTop = when {
        parts.size >= 2 -> parts[0]
        parts.size == 1 -> "תרגילים"
        else -> null
    }
    val badgeBottom = when {
        parts.size >= 2 -> parts[1].replace("תרגילים", "").trim()
        parts.size == 1 -> parts[0].replace("תרגילים", "").trim()
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.9f))
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!badgeTop.isNullOrBlank() && !badgeBottom.isNullOrBlank()) {
                        CountBadge(
                            textTop = badgeTop,
                            textBottom = badgeBottom,
                            accent = accent
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountBadge(
    textTop: String,
    textBottom: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = textTop,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = textBottom,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DefenseRootCard(
    title: String,
    subtitle: String,
    kind: il.kmi.app.domain.DefenseKind,
    countText: String? = null,
    onClick: () -> Unit
) {
    val accent = when (kind) {
        il.kmi.app.domain.DefenseKind.INTERNAL -> Color(0xFF2E7D32)
        il.kmi.app.domain.DefenseKind.EXTERNAL -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.primary
    }

    BaseTopicCard(
        title = title,
        subtitle = subtitle,
        accent = accent,
        countText = countText,
        onClick = onClick
    )
}

@Composable
private fun DefenseRootSingleCard(
    title: String,
    subtitle: String,
    countText: String? = null,
    onClick: () -> Unit
) {
    BaseTopicCard(
        title = title,
        subtitle = subtitle,
        accent = Color(0xFF1565C0),
        countText = countText,
        onClick = onClick
    )
}

@Composable
private fun HandsRootCard(
    title: String,
    subtitle: String,
    countText: String? = null,
    onClick: () -> Unit
) {
    BaseTopicCard(
        title = title,
        subtitle = subtitle,
        accent = Color(0xFF8E24AA),
        countText = countText,
        onClick = onClick
    )
}
/* ----------------------------- helpers לשליפת תרגילים מתוך AppContentRepo ----------------------------- */
// ----------------------------- Adapter: app SubjectTopic -> shared SubjectTopic -----------------------------
private fun SubjectTopic.toSharedSubject(): SharedSubjectTopic =
    SharedSubjectTopic(
        id = this.id,
        titleHeb = this.titleHeb,
        topicsByBelt = this.topicsByBelt,
        subTopicHint = this.subTopicHint
    )

private fun uiCountForSubject(belt: Belt, subject: SubjectTopic): Int =
    SubjectItemsResolver
        .resolveBySubject(belt = belt, subject = subject.toSharedSubject())
        .asSequence()
        .flatMap { it.items.asSequence() }
        .map { it.canonicalId }
        .distinct()
        .count()

/* ----------------------------- כרטיסים/דיאלוגים (מהקוד שלך) ----------------------------- */
/*  כדי לקצר פה: הכנסתי בדיוק את הבלוקים העיקריים (Root cards + dialogs + rows).
    אם תרצה — אני אוסיף גם את countForHandsPick “האמת” בדיוק כמו שעשית קודם. */

// ✅ UI ONLY: תג ספירה אחיד לכל הכרטיסים (אותו פונט/גודל/משקל)
@Composable
private fun CountTextBadge(
    text: String,
    color: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        lineHeight = 16.sp
    )
}

@Composable
private fun SubjectRootCard(
    title: String,
    subtitle: String,
    countText: String? = null,
    showLeftBadge: Boolean = true,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Color(0x12000000))
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (showLeftBadge) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.55f))
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    Spacer(Modifier.width(4.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    Text(
                        text = subtitle.replace(Regex("^\\d+\\s+תרגילים\\s*•?\\s*"), ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .widthIn(min = 68.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!countText.isNullOrBlank()) {
                        CountTextBadge(text = countText, color = accent)
                        Spacer(Modifier.height(6.dp))
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectLeafCard(
    title: String,
    countText: String,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Color(0x0F000000)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.35f))
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


/* ----------------------------- דיאלוגים קטנים (Pick) ----------------------------- */

@Composable
private fun DefenseCategoryPickDialogModern(
    counts: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent = Color(0xFF1565C0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "הגנות",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                ModernPickCard(
                    title = "הגנות פנימיות",
                    accent = Color(0xFF2E7D32),
                    icon = "🛡️",
                    countText = "${counts["הגנות פנימיות"] ?: 0} תרגילים",
                    onClick = { onPick("internal") }
                )

                ModernPickCard(
                    title = "הגנות חיצוניות",
                    accent = Color(0xFF1565C0),
                    icon = "🛡️",
                    countText = "${counts["הגנות חיצוניות"] ?: 0} תרגילים",
                    onClick = { onPick("external") }
                )

                ModernPickCard(
                    title = "הגנות נגד בעיטות",
                    accent = accent,
                    icon = "🦵",
                    countText = "${counts["הגנות נגד בעיטות"] ?: 0} תרגילים",
                    onClick = { onPick("kicks") }
                )

                ModernPickCard(
                    title = "הגנות מסכין",
                    accent = accent,
                    icon = "🔪",
                    countText = "${counts["הגנות מסכין"] ?: 0} תרגילים",
                    onClick = { onPick("knife") }
                )

                ModernPickCard(
                    title = "הגנות מאיום אקדח",
                    accent = accent,
                    icon = "🔫",
                    countText = "${counts["הגנות מאיום אקדח"] ?: 0} תרגילים",
                    onClick = { onPick("gun") }
                )

                ModernPickCard(
                    title = "הגנות נגד מקל",
                    accent = accent,
                    icon = "🪵",
                    countText = "${counts["הגנות נגד מקל"] ?: 0} תרגילים",
                    onClick = { onPick("stick") }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}

@Composable
private fun KicksSubTopicsPickDialogModern(
    picks: List<String>,
    counts: Map<String, Int>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
){
    val accent = Color(0xFF1565C0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "הגנות נגד בעיטות",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text(
                    "בחר תת־נושא:",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                picks.forEach { pick ->
                    val c = counts[pick] ?: 0

                    ModernPickCard(
                        title = pick,
                        accent = accent,
                        icon = "🦵",
                        countText = "$c תרגילים",
                        onClick = { onPick(pick) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}

@Composable
private fun DefensePickModeDialogModern(
    kind: il.kmi.app.domain.DefenseKind,
    counts: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val title = if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) "הגנות פנימיות" else "הגנות חיצוניות"
    val accent = if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) Color(0xFF4CAF50) else Color(0xFF2196F3)

    val keyPunch = "${kind.name}:אגרופים"
    val keyKick  = "${kind.name}:בעיטות"

    val punchCount = counts[keyPunch] ?: 0
    val kickCount  = counts[keyKick] ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("מה לבחור?", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

                ModernPickCard(
                    title = "אגרופים",
                    accent = accent,
                    icon = "👊",
                    countText = "$punchCount תרגילים",
                    onClick = { onPick("אגרופים") }
                )

                ModernPickCard(
                    title = "בעיטות",
                    accent = accent,
                    icon = "🦵",
                    countText = "$kickCount תרגילים",
                    onClick = { onPick("בעיטות") }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}

@Composable
private fun HandsPickModeDialogModern(
    picks: List<String>,
    counts: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent = Color(0xFF8E24AA)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "עבודת ידיים",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "בחר תת־נושא:",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                ModernPickCard(
                    title = "מכות מרפק",
                    accent = accent,
                    icon = "💪",
                    countText = "${counts["מכות מרפק"] ?: 0} תרגילים",
                    onClick = { onPick("מכות מרפק") }
                )

                ModernPickCard(
                    title = "מכות יד",
                    accent = accent,
                    icon = "👊",
                    countText = "${counts["מכות יד"] ?: 0} תרגילים",
                    onClick = { onPick("מכות יד") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = onDismiss) { Text(text = "ביטול") }
                }
            }
        }
    )
}

@Composable
private fun ModernPickCard(
    title: String,
    accent: Color,
    icon: String? = null,
    countText: String? = null,
    onClick: () -> Unit
) {

    // ✅ כשיש icon – כופים LTR כדי שהאייקון יהיה בשמאל בצורה עקבית
    val dir = if (icon != null) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides dir) {

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 1.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ✅ icon בצד שמאל (רק כשיש icon)
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    countText?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ReleasesPickModeDialogModern(
    picks: List<String>,
    counts: Map<String, Int>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent = Color(0xFF455A64)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "שחרורים",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "בחר תת־נושא:",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (picks.isEmpty()) {
                    Text(
                        text = "לא הוגדרו תתי־נושאים לשחרורים.",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    picks.forEach { pick ->
                        val c = counts[pick] ?: 0
                        ModernPickCard(
                            title = pick,
                            accent = accent,
                            countText = "$c תרגילים",
                            onClick = { onPick(pick) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}

@Composable
private fun SubTopicsPickModeDialogModern(
    title: String,
    picks: List<String>,
    counts: Map<String, Int>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val accent = Color(0xFF455A64)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("בחר תת־נושא:", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

                if (picks.isEmpty()) {
                    Text("לא הוגדרו תתי־נושאים.", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                } else {
                    picks.forEach { pick ->
                        val c = counts[pick] ?: 0
                        ModernPickCard(
                            title = pick,
                            accent = accent,
                            countText = "$c תרגילים",
                            onClick = { onPick(pick) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}
