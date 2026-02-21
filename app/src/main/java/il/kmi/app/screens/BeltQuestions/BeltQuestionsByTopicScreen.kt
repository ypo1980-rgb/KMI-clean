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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.DefenseKind
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.ui.FloatingBeltQuickMenu
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import il.kmi.shared.domain.content.SubjectItemsResolver.UiSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import il.kmi.app.navigation.defenses.defenseCount
import il.kmi.app.navigation.defenses.defenseRootCount

// ✅ FIX: פונקציית נרמול אחת בלבד, ברמת קובץ (נראית לכולם, אין forward-ref ואין אמביגיוטי)
private fun normText(raw: String): String = raw
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

    fun resolveSectionsForSubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<UiSection> {
        val raw = SubjectItemsResolver.resolveBySubject(
            belt = belt,
            subject = subject.toSharedSubject()
        )

        val hasHardKeywordFilters =
            subject.includeItemKeywords.isNotEmpty() ||
                    subject.requireAllItemKeywords.isNotEmpty() ||
                    subject.excludeItemKeywords.isNotEmpty()

        if (!hasHardKeywordFilters) return raw

        return raw
            .map { sec ->
                val filtered = sec.items.filter { item ->
                    val rawTitle = buildString {
                        append(item.canonicalId)
                        append("::")
                        append(item.displayName)
                    }

                    il.kmi.app.domain.TopicsBySubjectRegistry.run {
                        subject.matchesItem(
                            itemTitle = rawTitle,
                            subTopicTitle = sec.title ?: ""
                        )
                    }
                }
                sec.copy(items = filtered)
            }
            .filter { it.items.isNotEmpty() }
    }

    fun ensureDefenseRootTopic(subject: SubjectTopic): SubjectTopic {
        val isDefense =
            subject.titleHeb.contains("הגנות") ||
                    subject.defenseKind != il.kmi.app.domain.DefenseKind.NONE

        if (!isDefense) return subject

        val fixed = subject.topicsByBelt.mapValues { (_, list) ->
            val out = list.toMutableList()
            val hasRoot = out.any { normText(it) == normText("הגנות") }
            if (!hasRoot) out.add(0, "הגנות")
            out
        }

        return subject.copy(topicsByBelt = fixed)
    }

    fun matchesDefenseCategory(titleHeb: String, pick: String): Boolean {
        val t = titleHeb.trim()
        val isDefenseTitle = t.contains("הגנות") || t.contains("הגנה")
        if (!isDefenseTitle) return false

        val isKnifeLike = t.contains("סכין") || t.contains("דקיר") || t.contains("דקירה")

        return when (pick) {
            "הגנות נגד בעיטות" -> t.contains("בעיט") && !isKnifeLike
            "הגנות מסכין" -> isKnifeLike
            "הגנות מאיום אקדח" -> t.contains("אקדח")
            "הגנות נגד מקל" -> t.contains("מקל")
            else -> false
        }
    }

        // ✅ אם אתה עדיין צריך "רשימת שמות" בלבד (למקרים נקודתיים)
    fun getDisplayNamesForSubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<String> =
        resolveSectionsForSubject(belt, subject)
            .flatMap { it.items }
            .map { it.displayName }

        // =======================
// בתוך TopicsBySubjectCard
// =======================

        // ✅ NEW: חגורות שיש בהן תרגילים בפועל עבור subject (מסנן חגורות 0)
        // ❗️חשוב: משתמשים ב-topicsByBelt.keys (ולא subject.belts) כדי שלא נבחר חגורה “פיקטיבית”
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


        // ✅ NEW: אותו רעיון לעבודת ידיים – יש הרבה פריטים שנמצאים רק תחת root "עבודת ידיים"
        fun ensureHandsRootTopic(subject: SubjectTopic): SubjectTopic {
            val t = subject.titleHeb.trim()
            val isHands =
                t == "עבודת ידיים" ||
                        (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))

            if (!isHands) return subject

            val fixed = subject.topicsByBelt.mapValues { (_, list) ->
                val out = list.toMutableList()
                val hasRoot = out.any { normText(it) == normText("עבודת ידיים") }
                if (!hasRoot) out.add(0, "עבודת ידיים")
                out
            }

            return subject.copy(topicsByBelt = fixed)
        }

        // ✅ FIX: התאמת hint לשמות האמיתיים ב-ContentRepo
        fun fixHandsSubTopicHint(subject: SubjectTopic): SubjectTopic {
            val t = subject.titleHeb.trim()
            val isHands =
                t == "עבודת ידיים" ||
                        (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))

            if (!isHands) return subject

            val hintRaw = subject.subTopicHint?.trim().orEmpty()
            if (hintRaw.isBlank()) return subject

            val fixedHint = when {
                hintRaw.contains("מגל") && hintRaw.contains("סנוקרת") -> "מגל + סנוקרת"
                else -> hintRaw
            }

            return if (fixedHint == hintRaw) subject else subject.copy(subTopicHint = fixedHint)
        }

        fun openSubjectSmart(subject: SubjectTopic) {

            // ✅ FIX: גם הגנות וגם עבודת ידיים – מבטיחים root topic לפני resolver
            // ✅ FIX: גם root topic וגם hint נכון למגל+סנוקרת
            // ✅ FIX: גם root topic וגם hint נכון למגל+סנוקרת
            val fixedSubject =
                fixHandsSubTopicHint(
                    ensureHandsRootTopic(
                        ensureDefenseRootTopic(subject)
                    )
                )
            val nonEmptyBelts = beltsWithItemsForSubject(fixedSubject)

            val chosenBelt =
                nonEmptyBelts.firstOrNull()
                    ?: fixedSubject.topicsByBelt.keys.firstOrNull()
                    ?: Belt.GREEN

            android.util.Log.e(
                "KMI_DBG",
                "openSubjectSmart '${fixedSubject.titleHeb}' chosenBelt=$chosenBelt keys=${fixedSubject.topicsByBelt.keys}"
            )

            onSubjectClick(chosenBelt, fixedSubject)
        }

    // ✅ מאחדים "תיקונים" לפני ספירה/תצוגה
    fun normalizeSubjectForCounting(subject: SubjectTopic): SubjectTopic {
        return fixHandsSubTopicHint(
            ensureHandsRootTopic(
                ensureDefenseRootTopic(subject)
            )
        )
    }

    fun countUiTitlesForSubject(subject: SubjectTopic): Int {
        val fixed = normalizeSubjectForCounting(subject)

        val all = mutableSetOf<String>() // canonical IDs
        fixed.topicsByBelt.keys.forEach { belt ->
            resolveSectionsForSubject(belt, fixed)
                .asSequence()
                .flatMap { it.items.asSequence() }
                .map { it.canonicalId }
                .forEach { all += it }
        }
        return all.size
    }

    fun countUiTitlesForSubjects(list: List<SubjectTopic>): Int {
        val all = mutableSetOf<String>() // canonical IDs
        list.forEach { subject ->
            val fixed = normalizeSubjectForCounting(subject)

            fixed.topicsByBelt.keys.forEach { belt ->
                resolveSectionsForSubject(belt, fixed)
                    .asSequence()
                    .flatMap { it.items.asSequence() }
                    .map { it.canonicalId }
                    .forEach { all += it }
            }
        }
        return all.size
    }

        fun formatCount(n: Int): String = "$n תרגילים"

        // ✅ חסר אצלך: ספירה לדיאלוג “הגנות” (בעיטות/סכין/אקדח/מקל)
        fun countUiTitlesForDefensePick(pick: String): Int {
            val pickedSubjects = subjects.filter { s -> matchesDefenseCategory(s.titleHeb, pick) }
            return countUiTitlesForSubjects(pickedSubjects)
        }

        fun countUiTitlesForHandsPick(pick: String): Int {
            fun matchesHandsPick(titleHeb: String): Boolean {
                val t = titleHeb.trim()
                val isHands = t == "עבודת ידיים" || (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))
                if (!isHands) return false

                return when (pick) {
                    "מרפק" -> t.contains("מרפק")
                    "פיסת יד" -> t.contains("פיסת") || t.contains("פיסת יד") || t.contains("כף יד")
                    "אגרופים ישרים" -> t.contains("ישרים") || t.contains("אגרופים ישרים")
                    "מגל וסנוקרת" -> t.contains("מגל") || t.contains("סנוקרת")
                    else -> false
                }
            }

            val pickedSubjects = subjects.filter { s -> matchesHandsPick(s.titleHeb) }
            val all = mutableSetOf<String>()
            pickedSubjects.forEach { subject ->
                subject.topicsByBelt.keys.forEach { belt ->
                    resolveSectionsForSubject(belt, subject)
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .map { it.canonicalId }
                        .forEach { all += it }
                }
            }
            return all.size
        }

    fun displayName(raw: String): String {
        val t = raw.trim()
        if (!t.contains("::")) return t
        return t.substringAfterLast("::").trim().ifBlank { t }
    }

    // ✅ NEW: נרמול פריטי הגנות כדי לתמוך גם בפורמט הישן עם _ וגם בחדש עם :
    fun normalizeDefenseItem(raw: String): String {
        val t0 = raw.trim().trim('"')

        // פורמטים ישנים שהיו אצלך בשביל הסינונים
        val t1 = t0
            .replace("def_internal_punches::", "def:internal:punch::", ignoreCase = true)
            .replace("def_internal_kicks::", "def:internal:kick::", ignoreCase = true)
            .replace("def_external_punches::", "def:external:punch::", ignoreCase = true)
            .replace("def_external_kicks::", "def:external:kick::", ignoreCase = true)

        // אם נשארו וריאציות “חצי חדשות”
        val t2 = t1
            .replace("def:internal:punch:", "def:internal:punch::", ignoreCase = true)
            .replace("def:internal:kick:", "def:internal:kick::", ignoreCase = true)
            .replace("def:external:punch:", "def:external:punch::", ignoreCase = true)
            .replace("def:external:kick:", "def:external:kick::", ignoreCase = true)

        return t2
    }

// ----------------- חישובים כבדים ברקע -----------------

    var subjectCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var internalKindCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var externalKindCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var internalDefenseRootCount by remember(subjects) { mutableStateOf(0) }
    var externalDefenseRootCount by remember(subjects) { mutableStateOf(0) }
    var handsRootCount by remember(subjects) { mutableStateOf(0) }
    var totalDefenseCount by remember(subjects) { mutableStateOf(0) }
    var countsReady by remember(subjects) { mutableStateOf(false) }
    var handsPickCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var uiSectionCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) } // ✅ NEW

// ✅ NEW: איזה נושא “תתי נושאים” פתוח כרגע (id של subject)
    var askSubTopicsForId by rememberSaveable { mutableStateOf<String?>(null) }

// ✅ NEW: subjectId -> (pick -> count)
    var subTopicsPickCountsBySubjectId by remember(subjects) {
        mutableStateOf<Map<String, Map<String, Int>>>(emptyMap())
    }

// ✅ סטייטים לדיאלוגים הקיימים
    var askDefense by rememberSaveable { mutableStateOf(false) }
    var askKind by rememberSaveable { mutableStateOf<il.kmi.app.domain.DefenseKind?>(null) }
    var askHands by rememberSaveable { mutableStateOf(false) }

    // ✅ NEW: mapping של בחירה -> hint לסינון (צריך להתאים לשמות שקיימים אצלך בפריטים/תתי-נושאים)
    fun releasesHintForPick(pick: String): String {
        return when (pick) {
            "שחרור מתפיסות ידיים" -> "תפיס"   // יתפוס: תפיסה/תפיסות
            "שחרור מחניקות"       -> "חניק"   // יתפוס: חניקה/חניקות
            "שחרור מחביקות"       -> "חביק"   // יתפוס: חביקה/חביקות
            "שחרור חולצה / שיער"  -> "חולצה"  // וגם "שיער" אם צריך בהמשך
            else -> pick
        }
    }

    fun releasesSubject(): SubjectTopic? =
        subjects.firstOrNull { it.id == "releases" }

    // ✅ NEW: פילטרים מדויקים לפי תת־נושא בשחרורים (גם לספירה וגם לתצוגה בפועל)
    data class ReleasesPickFilter(
        val includeAny: List<String>,
        val requireAll: List<String>,
        val excludeAny: List<String>
    )

    fun releasesFilterForPick(pick: String): ReleasesPickFilter {
        return when (pick) {
            // ✅ קשוח: חייב גם "תפיסה/אחיזה" וגם "יד/פרק"
            "שחרור מתפיסות ידיים" -> ReleasesPickFilter(
                includeAny = listOf("תפיס", "אחיז", "אוחז"),           // אחד מהם מספיק
                requireAll = listOf("יד"),                              // חייב לכלול יד/ידיים/פרק יד (נבדק עם contains)
                excludeAny = listOf(
                    "חניק", "חניקה", "צוואר",
                    "חביק", "חביקה", "דוב", "מאחור",
                    "חולצ", "חולצה", "שיער",
                    "אקדח", "סכין", "מקל" // אם בטעות נכנסו גם הגנות
                )
            )

            "שחרור מחניקות" -> ReleasesPickFilter(
                includeAny = listOf("חניק", "חניקה"),
                requireAll = emptyList(),
                excludeAny = listOf("חביק", "חולצ", "שיער")
            )

            "שחרור מחביקות" -> ReleasesPickFilter(
                includeAny = listOf("חביק", "חביקה"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "חולצ", "שיער")
            )

            "שחרור חולצה / שיער" -> ReleasesPickFilter(
                includeAny = listOf("חולצ", "חולצה", "שיער"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "חביק")
            )

            else -> ReleasesPickFilter(
                includeAny = listOf(pick),
                requireAll = emptyList(),
                excludeAny = emptyList()
            )
        }
    }

    // ✅ NEW: יוצרים Subject זמני עם subTopicHint לפי הבחירה כדי שה-resolver יסנן
    fun releasesSubjectForPick(pick: String): SubjectTopic? {
        val base = releasesSubject() ?: return null
        val hint = releasesHintForPick(pick)
        return base.copy(
            titleHeb = "${base.titleHeb} - $pick",
            subTopicHint = hint
        )
    }

    fun countUiTitlesForSubjectWithHint(base: SubjectTopic, hint: String): Int {
        val tmp = base.copy(subTopicHint = hint)
        return countUiTitlesForSubject(tmp)
    }

    // ✅ mapping כללי: pick -> hint
    // releases נשאר מיוחד (כמו שעשינו), כל השאר: hint = pick
    fun hintForPick(base: SubjectTopic, pick: String): String {
        return if (base.id == "releases") {
            releasesHintForPick(pick) // כבר יש אצלך
        } else {
            pick
        }
    }

    // ✅ יוצר Subject זמני עם subTopicHint לתתי־נושאים כלליים
    fun subjectForPick(base: SubjectTopic, pick: String): SubjectTopic {
        // ✅ FIX: לשחרורים לא מסתמכים רק על subTopicHint — מוסיפים include/require/exclude אמיתי
        if (base.id == "releases") {
            val f = releasesFilterForPick(pick)
            return base.copy(
                titleHeb = "${base.titleHeb} - $pick",
                subTopicHint = releasesHintForPick(pick),
                includeItemKeywords = f.includeAny,
                requireAllItemKeywords = f.requireAll,
                excludeItemKeywords = f.excludeAny
            )
        }

        val hint = hintForPick(base, pick)
        return base.copy(
            titleHeb = "${base.titleHeb} - $pick",
            subTopicHint = hint
        )
    }

    LaunchedEffect(subjects) {
        countsReady = false

        data class Payload(
            val subjCounts: Map<String, Int>,
            val internalRoot: Int,
            val externalRoot: Int,
            val handsRoot: Int,
            val defenseTotal: Int,
            val internalKinds: Map<String, Int>,
            val externalKinds: Map<String, Int>,
            val handsPicks: Map<String, Int>,
            val sectionCounts: Map<String, Int>,
            val subTopicsCounts: Map<String, Map<String, Int>>
        )

        val p = withContext(Dispatchers.Default) {

            // ✅ NEW: ספירות לכל subject שיש לו subTopics (כולל "שחרור מחביקות גוף" כילד של releases)
            val subTopicsCounts: Map<String, Map<String, Int>> =
                subjects
                    .asSequence()
                    .filter { it.subTopics.isNotEmpty() }
                    .associate { base ->

                        val bodyHugsChild: SubjectTopic? =
                            if (base.id == "releases") subjects.firstOrNull { it.id == "releases_body_hugs" } else null

                        val picks: List<String> =
                            if (bodyHugsChild != null) base.subTopics + bodyHugsChild.titleHeb
                            else base.subTopics

                        val countsForBase: Map<String, Int> =
                            picks.associateWith { pick: String ->
                                // ✅ אם זה הילד "שחרור מחביקות גוף" — סופרים לפי ה-subject הילד עצמו
                                if (bodyHugsChild != null && pick == bodyHugsChild.titleHeb) {
                                    countUiTitlesForSubject(bodyHugsChild)
                                } else {
                                    val tmp = subjectForPick(base, pick) // ✅ עכשיו לשחרורים זה מסנן באמת
                                    countUiTitlesForSubject(tmp)
                                }
                            }

                        base.id to countsForBase
                    }

            val subjCounts = subjects.associate { s -> s.id to countUiTitlesForSubject(s) }

            val internalRoot = defenseRootCount(kind = "internal")
            val externalRoot = defenseRootCount(kind = "external")

            val handsRoot = run {
                val all = mutableSetOf<String>()

                listOf("מרפק", "פיסת יד", "אגרופים ישרים", "מגל וסנוקרת").forEach { pick ->
                    subjects
                        .asSequence()
                        .filter { s ->
                            val t = s.titleHeb.trim()
                            val isHands = t == "עבודת ידיים" || (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))
                            if (!isHands) return@filter false

                            when (pick) {
                                "מרפק" -> t.contains("מרפק")
                                "פיסת יד" -> t.contains("פיסת") || t.contains("פיסת יד") || t.contains("כף יד")
                                "אגרופים ישרים" -> t.contains("ישרים") || t.contains("אגרופים ישרים")
                                "מגל וסנוקרת" -> t.contains("מגל") || t.contains("סנוקרת")
                                else -> false
                            }
                        }
                        .forEach { subject ->
                            subject.topicsByBelt.keys.forEach { belt ->
                                resolveSectionsForSubject(belt = belt, subject = subject)
                                    .asSequence()
                                    .flatMap { it.items.asSequence() }
                                    .map { it.canonicalId }
                                    .forEach { all += it }
                            }
                        }
                }

                all.size
            }

            val defenseTotal = subjects
                .filter {
                    matchesDefenseCategory(it.titleHeb, "הגנות נגד בעיטות") ||
                            matchesDefenseCategory(it.titleHeb, "הגנות מסכין") ||
                            matchesDefenseCategory(it.titleHeb, "הגנות מאיום אקדח") ||
                            matchesDefenseCategory(it.titleHeb, "הגנות נגד מקל")
                }
                .sumOf { subjCounts[it.id] ?: 0 }

            val internalKinds = mapOf(
                "אגרופים" to defenseCount(kind = "internal", pick = "punch"),
                "בעיטות"  to defenseCount(kind = "internal", pick = "kick"),
            )

            val externalKinds = mapOf(
                "אגרופים" to defenseCount(kind = "external", pick = "punch"),
                "בעיטות"  to defenseCount(kind = "external", pick = "kick"),
            )

            val handsPicks = mapOf(
                "מרפק" to subjects
                    .filter { it.titleHeb.contains("עבודת ידיים") && it.titleHeb.contains("מרפק") }
                    .sumOf { subjCounts[it.id] ?: 0 },

                "פיסת יד" to subjects
                    .filter { it.titleHeb.contains("עבודת ידיים") && (it.titleHeb.contains("פיסת") || it.titleHeb.contains("כף יד")) }
                    .sumOf { subjCounts[it.id] ?: 0 },

                "אגרופים ישרים" to subjects
                    .filter { it.titleHeb.contains("עבודת ידיים") && it.titleHeb.contains("ישרים") }
                    .sumOf { subjCounts[it.id] ?: 0 },

                "מגל וסנוקרת" to subjects
                    .filter { it.titleHeb.contains("עבודת ידיים") && (it.titleHeb.contains("מגל") || it.titleHeb.contains("סנוקרת")) }
                    .sumOf { subjCounts[it.id] ?: 0 }
            )

            val sectionCounts: Map<String, Int> =
                subjects.associate { s ->
                    if (s.subTopics.isNotEmpty()) {
                        // אם זה releases — נספור גם את הילד כתת־נושא נוסף להצגה
                        val extra = if (s.id == "releases" && subjects.any { it.id == "releases_body_hugs" }) 1 else 0
                        s.id to (s.subTopics.size + extra)
                    } else {
                        val sec = mutableSetOf<String>()
                        s.topicsByBelt.keys.forEach { b ->
                            resolveSectionsForSubject(b, s)
                                .asSequence()
                                .filter { it.items.isNotEmpty() }
                                .mapNotNull { it.title?.trim() }
                                .filter { it.isNotBlank() }
                                .forEach { sec += it }
                        }
                        s.id to sec.size
                    }
                }

            Payload(
                subjCounts = subjCounts,
                internalRoot = internalRoot,
                externalRoot = externalRoot,
                handsRoot = handsRoot,
                defenseTotal = defenseTotal,
                internalKinds = internalKinds,
                externalKinds = externalKinds,
                handsPicks = handsPicks,
                sectionCounts = sectionCounts,
                subTopicsCounts = subTopicsCounts
            )
        }

        subjectCounts = p.subjCounts
        internalDefenseRootCount = p.internalRoot
        externalDefenseRootCount = p.externalRoot
        handsRootCount = p.handsRoot
        totalDefenseCount = p.defenseTotal
        internalKindCounts = p.internalKinds
        externalKindCounts = p.externalKinds
        handsPickCounts = p.handsPicks
        uiSectionCounts = p.sectionCounts
        subTopicsPickCountsBySubjectId = p.subTopicsCounts
        countsReady = true
    }

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

        // ✅ NEW: מציאת subject מדויק להגנות פנימיות/חיצוניות לפי kind + pick (גמיש: לא תלוי defenseKind בלבד)
        fun findDefenseSubject(
            kind: il.kmi.app.domain.DefenseKind,
            pick: String
        ): SubjectTopic? {
            fun n(s: String) = normText(s)

            val wantsInternal = kind == il.kmi.app.domain.DefenseKind.INTERNAL
            val pickKey = when {
                pick.contains("אגרופ") -> "אגרופ"   // יתפוס אגרוף/אגרופים
                pick.contains("בעיט")  -> "בעיט"    // יתפוס בעיטה/בעיטות
                else -> pick.trim()
            }

            return subjects.firstOrNull { s ->
                val t = n(s.titleHeb)

                val isDefense = t.contains("הגנות") || t.contains("הגנה")
                if (!isDefense) return@firstOrNull false

                val isKindOk =
                    if (wantsInternal) t.contains("פנימ") else t.contains("חיצונ")

                val isPickOk = t.contains(n(pickKey))

                // לפעמים הכותרת היא "הגנות פנימיות (אגרופים+בעיטות)" / "הגנות חיצוניות (אגרופים+בעיטות)"
                // אז אנחנו לא דורשים "contains(אגרופים)" בדיוק – מספיק "אגרופ"/"בעיט"
                isKindOk && isPickOk
            }
        }

        // ✅ NEW: מציאת subject מדויק ל"הגנות" לפי קטגוריה (בעיטות/סכין/אקדח/מקל)
        fun findDefenseCategorySubject(pick: String): SubjectTopic? {
            fun isKnifeLike(t: String) = t.contains("סכין") || t.contains("דקיר") || t.contains("דקירה")

            return subjects.firstOrNull { s ->
                val t = s.titleHeb.trim()
                val isDefense = t.contains("הגנות") || t.contains("הגנה")
                if (!isDefense) return@firstOrNull false

                when (pick) {
                    // ✅ FIX: "הגנות נגד בעיטות" = קטגוריה כללית, לא פנימיות/חיצוניות
                    "הגנות נגד בעיטות" -> {
                        val notKnife = !isKnifeLike(t)
                        val hasKick = t.contains("בעיט")
                        val notInternalExternal = !t.contains("פנימ") && !t.contains("חיצונ")
                        // מעדיפים Subject שהוא "הגנות - ..." או כללי
                        notKnife && hasKick && notInternalExternal
                    }

                    "הגנות מסכין" -> isKnifeLike(t)
                    "הגנות מאיום אקדח" -> t.contains("אקדח")
                    "הגנות נגד מקל" -> t.contains("מקל")
                    else -> false
                }
            } ?: run {
                // ✅ fallback אחרון (רק אם אין שום Subject קטגוריה):
                // נשאיר אותך עם משהו שמציג תרגילים במקום מסך ריק
                if (pick == "הגנות נגד בעיטות") {
                    subjects.firstOrNull { s ->
                        val t = s.titleHeb.trim()
                        !isKnifeLike(t) && t.contains("בעיט") && (s.defenseKind == DefenseKind.INTERNAL || s.defenseKind == DefenseKind.EXTERNAL)
                    }
                } else null
            }
        }

        // ✅ NEW: מציאת subject מדויק ל"עבודת ידיים" לפי תת־בחירה
        fun findHandsSubject(pick: String): SubjectTopic? {

            fun norm(s: String): String = s
                .replace("–", "-")
                .replace("—", "-")
                .replace("־", "-")
                .replace("+", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            fun isHands(t: String) =
                t == "עבודת ידיים" || (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))

            val pickN = norm(pick)

            return subjects.firstOrNull { s ->
                val t = norm(s.titleHeb)
                if (!isHands(t)) return@firstOrNull false

                when (pickN) {
                    "מרפק" -> t.contains("מרפק")
                    "פיסת יד" -> t.contains("פיסת") || t.contains("פיסת יד") || t.contains("כף יד")
                    "אגרופים ישרים" -> t.contains("ישרים") || t.contains("אגרופים ישרים")
                    "מגל וסנוקרת" -> t.contains("מגל") || t.contains("סנוקרת")
                    else -> false
                }
            }
        }

        val visibleSubjects = remember(subjects) {
            subjects.filter { !isDefenseChild(it) && !isHandsChild(it) }
        }

// ✅ NEW: שחרורים יופיע מתחת לעבודת ידיים (ולא יופיע שוב ברשימה)
        val releasesSubject = remember(visibleSubjects) {
            visibleSubjects.firstOrNull { it.id == "releases" }
        }
        val visibleSubjectsExceptReleases = remember(visibleSubjects) {
            visibleSubjects.filterNot { it.id == "releases" }
        }

        // ----------------- UI -----------------
    // (⚠️ החלק הזה חייב להישאר! אם מחקת אותו/סגרת } לפניו — המסך נהיה ריק)
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        modifier = Modifier
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

            DefenseRootCard(
                title = "הגנות פנימיות",
                subtitle = "אגרופים / בעיטות",
                kind = il.kmi.app.domain.DefenseKind.INTERNAL,
                countText = "2 נושאים\n${formatCount(n = internalDefenseRootCount)}",
                onClick = { askKind = il.kmi.app.domain.DefenseKind.INTERNAL }
            )

            DefenseRootCard(
                title = "הגנות חיצוניות",
                subtitle = "אגרופים / בעיטות",
                kind = il.kmi.app.domain.DefenseKind.EXTERNAL,
                countText = "2 נושאים\n${formatCount(n = externalDefenseRootCount)}",
                onClick = { askKind = il.kmi.app.domain.DefenseKind.EXTERNAL }
            )

            DefenseRootSingleCard(
                title = "הגנות",
                subtitle = "בעיטות / סכין / אקדח / מקל",
                countText = "4 תתי נושאים\n${formatCount(totalDefenseCount)}",
                onClick = { askDefense = true }
            )

            HandsRootCard(
                title = "עבודת ידיים",
                subtitle = "מרפק / פיסת יד / מגל+סנוקרת",
                countText = "4 נושאים\n${formatCount(n = handsRootCount)}",
                onClick = { askHands = true }
            )

            // ✅ "שחרורים" מיד אחרי "עבודת ידיים"
            releasesSubject?.let { subject ->
                val exercisesCount = subjectCounts[subject.id] ?: 0
                val subTopicsCount = uiSectionCounts[subject.id] ?: subject.subTopics.size

                val countTextUi = if (subTopicsCount > 0) {
                    "${subTopicsCount} תתי נושאים\n${exercisesCount} תרגילים"
                } else {
                    formatCount(exercisesCount)
                }

                val onClick = when {
                    subTopicsCount > 0 -> ({ askSubTopicsForId = subject.id })
                    else -> ({ openSubjectSmart(subject) })
                }

                SubjectRootCard(
                    title = subject.titleHeb,
                    subtitle = subject.description,
                    countText = countTextUi,
                    onClick = onClick
                )
            }

            // ✅ שאר הנושאים
            visibleSubjectsExceptReleases.forEach { subject ->
                val exercisesCount = subjectCounts[subject.id] ?: 0
                val subTopicsCount = uiSectionCounts[subject.id] ?: subject.subTopics.size

                val countTextUi = if (subTopicsCount > 0) {
                    "${subTopicsCount} תתי נושאים\n${exercisesCount} תרגילים"
                } else {
                    formatCount(exercisesCount)
                }

                val onClick = when {
                    subTopicsCount > 0 -> ({ askSubTopicsForId = subject.id })
                    else -> ({ openSubjectSmart(subject) })
                }

                SubjectRootCard(
                    title = subject.titleHeb,
                    subtitle = subject.description,
                    countText = countTextUi,
                    onClick = onClick
                )
            }
        }
    }



    // ----------------- דיאלוגים -----------------

    if (askDefense) {
        val kicksAll = remember(countsReady) {
            defenseCount(kind = "all", pick = "kick")
        }

        DefenseCategoryPickDialogModern(
            counts = remember(subjects, countsReady, kicksAll) {
                mapOf(
                    // ✅ בעיטות = מהגרף החדש (פנימיות+חיצוניות יחד)
                    "הגנות נגד בעיטות" to kicksAll,

                    // ✅ השאר נשאר אצלך כמו שהיה (Subjects)
                    "הגנות מסכין" to countUiTitlesForDefensePick("הגנות מסכין"),
                    "הגנות מאיום אקדח" to countUiTitlesForDefensePick("הגנות מאיום אקדח"),
                    "הגנות נגד מקל" to countUiTitlesForDefensePick("הגנות נגד מקל")
                )
            },
            onDismiss = { askDefense = false },
            onPick = { picked ->
                askDefense = false

                // ✅ “הגנות נגד בעיטות” נכנס למסך הרשימות החדש
                if (picked == "הגנות נגד בעיטות") {
                    onOpenDefenseList(currentBelt, "all", "kick")
                    return@DefenseCategoryPickDialogModern
                }

                // ✅ שאר הקטגוריות נשאר Subject-based
                val subject = findDefenseCategorySubject(picked)
                if (subject != null) {
                    openSubjectSmart(subject)
                }
            }
        )
    }

    askKind?.let { kind ->

        val countsForDialog = remember(kind, internalKindCounts, externalKindCounts) {
            when (kind) {
                il.kmi.app.domain.DefenseKind.INTERNAL -> internalKindCounts
                il.kmi.app.domain.DefenseKind.EXTERNAL -> externalKindCounts
                else -> emptyMap()
            }
        }

        DefensePickModeDialogModern(
            kind = kind,
            counts = countsForDialog,
            onDismiss = { askKind = null },
            onPick = { picked ->
                askKind = null

                val kindStr = if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) "internal" else "external"
                val pickStr = if (picked.contains("אגרופ")) "punch" else "kick"

                android.util.Log.e("KMI_DBG", "DEFENSE NAV -> belt=$currentBelt kind=$kindStr pick=$pickStr")

                onOpenDefenseList(currentBelt, kindStr, pickStr)
            }
        )
    }

    // ✅ עבודת ידיים – חייב להיות *בתוך* TopicsBySubjectCard
    if (askHands) {
        HandsPickModeDialogModern(
            counts = handsPickCounts,
            onDismiss = { askHands = false },
            onPick = { picked ->
                askHands = false
                val subject = findHandsSubject(pick = picked)
                if (subject != null) openSubjectSmart(subject)
            }
        )
    }

    // ✅ תתי-נושאים – להשאיר רק פעם אחת (אם יש לך עוד אחד למטה – למחוק אותו)
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
} // ✅ זה ה-} היחיד שסוגר את TopicsBySubjectCard

/* ----------------------------- helpers לשליפת תרגילים מתוך AppContentRepo ----------------------------- */
// ----------------------------- Adapter: app SubjectTopic -> shared SubjectTopic -----------------------------
private fun SubjectTopic.toSharedSubject(): SharedSubjectTopic =
    SharedSubjectTopic(
        id = this.id,
        titleHeb = this.titleHeb,
        topicsByBelt = this.topicsByBelt,
        subTopicHint = this.subTopicHint,
        includeItemKeywords = this.includeItemKeywords.orEmpty(),
        requireAllItemKeywords = this.requireAllItemKeywords.orEmpty(),
        excludeItemKeywords = this.excludeItemKeywords.orEmpty()
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
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Color(0x12000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.55f))
            )
            Spacer(Modifier.width(12.dp))

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

@Composable
private fun DefenseRootCard(
    title: String,
    subtitle: String,
    kind: il.kmi.app.domain.DefenseKind,
    countText: String? = null,
    onClick: () -> Unit
) {
    val accent = when (kind) {
        il.kmi.app.domain.DefenseKind.INTERNAL -> Color(0xFF4CAF50)
        il.kmi.app.domain.DefenseKind.EXTERNAL -> Color(0xFF2196F3)
        else -> Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.95f))
            )
            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!countText.isNullOrBlank()) {
                Spacer(Modifier.width(10.dp))
                CountTextBadge(text = countText, color = accent)
            }
        }
    }
}

@Composable
private fun DefenseRootSingleCard(
    title: String,
    subtitle: String,
    countText: String? = null,
    onClick: () -> Unit
) {
    val accent = Color(0xFF1565C0)

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.95f))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, fontWeight = FontWeight.Medium, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }

            if (!countText.isNullOrBlank()) {
                Spacer(Modifier.width(10.dp))
                CountTextBadge(text = countText, color = accent)
            }
        }
    }
}

@Composable
private fun HandsRootCard(
    title: String,
    subtitle: String,
    countText: String? = null,
    onClick: () -> Unit
) {
    val accent = Color(0xFF8E24AA)

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.95f))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, fontWeight = FontWeight.Medium, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }

            if (!countText.isNullOrBlank()) {
                Spacer(Modifier.width(10.dp))
                CountTextBadge(text = countText, color = accent)
            }
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
                ModernPickCard("הגנות נגד בעיטות", "הצג תרגילים של הגנות מול בעיטות", accent, "🦵",
                    countText = "${counts["הגנות נגד בעיטות"] ?: 0} תרגילים",
                    onClick = { onPick("הגנות נגד בעיטות") }
                )
                ModernPickCard("הגנות מסכין", "הצג תרגילים של הגנות מול סכין", accent, "🔪",
                    countText = "${counts["הגנות מסכין"] ?: 0} תרגילים",
                    onClick = { onPick("הגנות מסכין") }
                )
                ModernPickCard("הגנות מאיום אקדח", "הצג תרגילים של הגנות מאיום אקדח", accent, "🔫",
                    countText = "${counts["הגנות מאיום אקדח"] ?: 0} תרגילים",
                    onClick = { onPick("הגנות מאיום אקדח") }
                )
                ModernPickCard("הגנות נגד מקל", "הצג תרגילים של הגנות מול מקל", accent, "🦯",
                    countText = "${counts["הגנות נגד מקל"] ?: 0} תרגילים",
                    onClick = { onPick("הגנות נגד מקל") }
                )

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
                    "אגרופים",
                    "הצג תרגילים של הגנות מול אגרופים",
                    accent,
                    "👊",
                    countText = "${counts["אגרופים"] ?: 0} תרגילים"
                ) { onPick("אגרופים") }

                ModernPickCard(
                    "בעיטות",
                    "הצג תרגילים של הגנות מול בעיטות",
                    accent,
                    "🦵",
                    countText = "${counts["בעיטות"] ?: 0} תרגילים"
                ) { onPick("בעיטות") }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
}

@Composable
private fun HandsPickModeDialogModern(
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("בחר תת־נושא:", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

                ModernPickCard("מרפק", "תרגילים בתחום המרפק", accent, "💪",
                    countText = "${counts["מרפק"] ?: 0} תרגילים"
                ) { onPick("מרפק") }

                ModernPickCard("פיסת יד", "תרגילים בתחום פיסת היד", accent, "✋",
                    countText = "${counts["פיסת יד"] ?: 0} תרגילים"
                ) { onPick("פיסת יד") }

                ModernPickCard("אגרופים ישרים", "תרגילים באגרופים ישרים", accent, "👊",
                    countText = "${counts["אגרופים ישרים"] ?: 0} תרגילים"
                ) { onPick("אגרופים ישרים") }

                ModernPickCard("מגל וסנוקרת", "תרגילים במגל וסנוקרת", accent, "🪝",
                    countText = "${counts["מגל וסנוקרת"] ?: 0} תרגילים"
                ) { onPick("מגל וסנוקרת") }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = onDismiss) { Text("ביטול") }
                }
            }
        }
    )
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
                            subtitle = "כניסה ישירה לתרגילים של $pick",
                            accent = accent,
                            icon = "🧩",
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
                            subtitle = "כניסה ישירה לתרגילים של $pick",
                            accent = accent,
                            icon = "🧩",
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
private fun ModernPickCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: String,
    countText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

                countText?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.width(10.dp))
            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null, tint = accent.copy(alpha = 0.85f))
        }
    }
}

/* ----------------------------- dialogs המלאים (רשימות) ----------------------------- */
/* כרגע השארתי פה placeholders קצרים כדי שלא תקבל 600+ שורות.
   אם אתה רוצה — אני מחזיר לך פה את DefensePunchKickDialog + DefenseCategoryDialog + HandsTechDialog + UnifiedExerciseInfoDialog 1:1 כמו בקובץ הענק שלך.
   תגיד "תדביק את הדיאלוגים המלאים" ואני שם אותם פה בשלמות. */

@Composable
private fun DefenseCategoryDialog(
    belt: Belt,
    allSubjects: List<SubjectTopic>,
    onOpenSubject: (SubjectTopic) -> Unit,
    onDismiss: () -> Unit,
    forcedPick: String
) {
    fun matchesDefenseCategory(titleHeb: String, pick: String): Boolean {
        val t = titleHeb.trim()
        val isDefenseTitle = t.contains("הגנות") || t.contains("הגנה")
        if (!isDefenseTitle) return false
        val isKnifeLike = t.contains("סכין") || t.contains("דקיר") || t.contains("דקירה")
        return when (pick) {
            "הגנות נגד בעיטות" -> t.contains("בעיט") && !isKnifeLike
            "הגנות מסכין" -> isKnifeLike
            "הגנות מאיום אקדח" -> t.contains("אקדח")
            "הגנות נגד מקל" -> t.contains("מקל")
            else -> false
        }
    }

    val picked = remember(allSubjects, forcedPick) {
        allSubjects
            .filter { matchesDefenseCategory(it.titleHeb, forcedPick) }
            .sortedBy { it.titleHeb }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } },
        title = {
            Text(
                text = forcedPick,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            if (picked.isEmpty()) {
                Text("לא נמצאו תתי־נושאים", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    picked.forEach { subject ->
                        val c = uiCountForSubject(belt, subject)
                        ModernPickCard(
                            title = subject.titleHeb,
                            subtitle = subject.description,
                            accent = Color(0xFF1565C0),
                            icon = "🛡️",
                            countText = "$c תרגילים",
                            onClick = {
                                onDismiss()
                                onOpenSubject(subject)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DefensePunchKickDialog(
    belt: Belt,
    kind: il.kmi.app.domain.DefenseKind,
    allSubjects: List<SubjectTopic>,
    onOpenSubject: (SubjectTopic) -> Unit,
    onDismiss: () -> Unit,
    forcedPick: String? = null
) {
    val title = if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) "הגנות פנימיות" else "הגנות חיצוניות"
    val accent = if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) Color(0xFF4CAF50) else Color(0xFF2196F3)

    // ✅ DEBUG פעם אחת: נראה מה באמת יש ברשימה (כותרות + defenseKind)
    LaunchedEffect(allSubjects) {
        android.util.Log.e("KMI_DBG", "DefensePunchKickDialog subjects.size=${allSubjects.size}")
        allSubjects.take(120).forEach { s ->
            android.util.Log.e("KMI_DBG", "SUBJECT: '${s.titleHeb}'  defenseKind=${s.defenseKind}")
        }
    }

    fun normHeb(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun containsAny(t: String, vararg parts: String): Boolean =
        parts.any { p -> p.isNotBlank() && t.contains(p) }

    val picked = remember(allSubjects, kind, forcedPick) {
        val pick = forcedPick?.trim().orEmpty() // "אגרופים" / "בעיטות"
        val isInternal = kind == il.kmi.app.domain.DefenseKind.INTERNAL

        // 1) ניסיון “חזק” לפי defenseKind (אם אצלך זה באמת מוגדר)
        val pickClean = pick.trim()

        val primary = allSubjects
            .asSequence()
            .filter { s ->
                val title = s.titleHeb.trim()
                s.defenseKind == kind &&
                        (title.contains("הגנות") || title.contains("הגנה")) &&
                        (pickClean.isBlank() || title.contains(pickClean))
            }
            .toList()

        if (primary.isNotEmpty()) {
            primary.sortedBy { it.titleHeb }
        } else {
            // 2) ✅ FALLBACK גמיש (לא תלוי בשם המדויק של הכותרת)
            allSubjects
                .asSequence()
                .filter { s ->
                    val t = normHeb(s.titleHeb)

                    val wantsKind = if (isInternal) containsAny(t, "פנימ", "פנימי", "פנימיות")
                    else containsAny(t, "חיצונ", "חיצוני", "חיצוניות")

                    val wantsPick = when (pick) {
                        "אגרופים" -> containsAny(t, "אגרופ", "אגרופים", "Punch")
                        "בעיטות"  -> containsAny(t, "בעיט", "בעיטות", "Kick")
                        else      -> true
                    }

                    // שומר על עולם "הגנות", אבל מאפשר גם טקסטים בלי “הגנות פנימיות” בדיוק
                    val looksLikeDefense = containsAny(t, "הגנות", "הגנה")

                    looksLikeDefense && wantsKind && wantsPick
                }
                .sortedBy { it.titleHeb }
                .toList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } },
        title = {
            Text(
                text = if (forcedPick.isNullOrBlank()) title else "$title • $forcedPick",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            if (picked.isEmpty()) {
                Text("לא נמצאו תתי־נושאים", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    picked.forEach { subject ->
                        val c = uiCountForSubject(belt, subject)
                        ModernPickCard(
                            title = subject.titleHeb,
                            subtitle = subject.description,
                            accent = accent,
                            icon = if (forcedPick == "אגרופים") "👊" else "🦵",
                            countText = "$c תרגילים",
                            onClick = {
                                onDismiss()
                                onOpenSubject(subject)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun HandsTechDialog(
    belt: Belt,
    allSubjects: List<SubjectTopic>,
    onOpenSubject: (SubjectTopic) -> Unit,
    onDismiss: () -> Unit,
    forcedPick: String
) {
    fun matchesHandsPick(titleHeb: String, pick: String): Boolean {
        val t = titleHeb.trim()
        val isHands = t == "עבודת ידיים" || (t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–")))
        if (!isHands) return false

        return when (pick) {
            "מרפק" -> t.contains("מרפק")
            "פיסת יד" -> t.contains("פיסת") || t.contains("פיסת יד") || t.contains("כף יד")
            "אגרופים ישרים" -> t.contains("ישרים") || t.contains("אגרופים ישרים")
            "מגל וסנוקרת" -> t.contains("מגל") || t.contains("סנוקרת")
            else -> false
        }
    }

    val picked = remember(allSubjects, forcedPick) {
        allSubjects
            .filter { matchesHandsPick(it.titleHeb, forcedPick) }
            .sortedBy { it.titleHeb }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } },
        title = {
            Text(
                text = "עבודת ידיים • $forcedPick",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            if (picked.isEmpty()) {
                Text("לא נמצאו תתי־נושאים", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    picked.forEach { subject ->
                        val c = uiCountForSubject(belt, subject)
                        ModernPickCard(
                            title = subject.titleHeb,
                            subtitle = subject.description,
                            accent = Color(0xFF8E24AA),
                            icon = "✋",
                            countText = "$c תרגילים",
                            onClick = {
                                onDismiss()
                                onOpenSubject(subject)
                            }
                        )
                    }
                }
            }
        }
    )
}
