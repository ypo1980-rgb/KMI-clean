package il.kmi.app.screens.BeltQuestions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.SubjectTopic
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.app.screens.PracticeMenuDialog
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.prefs.KmiPrefs
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import il.kmi.app.ui.FloatingBeltQuickMenu
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import il.kmi.shared.domain.TopicsEngine
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

/* ------------------------------ Helpers מקומיים למסך ------------------------------ */

internal fun topicDetailsFor(belt: Belt, topicTitle: String): TopicDetails {
    val details = TopicsEngine.topicDetailsFor(belt, topicTitle.trim())

    val topicTrim = topicTitle.trim()
    val cleanSubs = details.subTitles
        .map { it.trim() }
        .filter { it.isNotBlank() && it != topicTrim }
        .distinct()

    return TopicDetails(
        itemCount = details.itemCount,
        subTitles = cleanSubs
    )
}

internal fun KmiPrefs.getStringCompat(key: String): String? = try {
    val c = this::class.java
    val m1 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 1 }
    val m2 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 2 }
    when {
        m1 != null -> m1.invoke(this, key) as? String
        m2 != null -> m2.invoke(this, key, null) as? String
        else -> null
    }
} catch (_: Exception) {
    null
}

internal fun findExplanationForHit(
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

internal fun Modifier.circleGlow(
    color: Color,
    radius: Dp,
    intensity: Float = 0.55f
) = this.drawBehind {
    val rPx = radius.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = intensity), Color.Transparent),
            center = this.center,
            radius = rPx
        ),
        radius = rPx,
        center = this.center
    )
}

internal fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = null
    ) { onClick() }
}

internal fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"
    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        append(before)
        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        append(after)
    }
}

internal fun formatCount(n: Int): String = when {
    n <= 0 -> "0 תרגילים"
    n == 1 -> "תרגיל 1"
    else -> "$n תרגילים"
}

/* ------------------------------ API ציבורי למסך ------------------------------ */

@Composable
fun BeltQuestionsByBeltScreen(
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    isCoach: Boolean,
    onNext: () -> Unit,
    onBackHome: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenSubject: (SubjectTopic) -> Unit,
    onOpenHardSubjectRoute: (Belt, String) -> Unit = { _, _ -> },
    onOpenSubTopic: (Belt, String, String) -> Unit = { _, _, _ -> },
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenRandomPractice: (Belt) -> Unit = {},
    onOpenFinalExam: (Belt) -> Unit = {},
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit,
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    BeltPangoLayout(
        vm = vm,
        kmiPrefs = kmiPrefs,
        isCoach = isCoach,        onNext = onNext,
        onBackHome = onBackHome,
        onOpenExercise = onOpenExercise,
        onOpenTopic = onOpenTopic,
        onOpenDefenseMenu = onOpenDefenseMenu,
        onOpenSubject = onOpenSubject,
        onOpenHardSubjectRoute = onOpenHardSubjectRoute,
        onOpenSubTopic = onOpenSubTopic,
        onOpenWeakPoints = onOpenWeakPoints,
        onOpenAllLists = onOpenAllLists,
        onOpenRandomPractice = onOpenRandomPractice,
        onOpenFinalExam = onOpenFinalExam,
        onPracticeByTopics = onPracticeByTopics,
        onOpenSummaryScreen = onOpenSummaryScreen,
        onOpenVoiceAssistant = onOpenVoiceAssistant,
        onOpenPdfMaterials = onOpenPdfMaterials
    )
}

/* -------------------------------- Layout ראשי -------------------------------- */

@Composable
internal fun BeltPangoLayout(
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    isCoach: Boolean,
    onNext: () -> Unit,
    onBackHome: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSubject: (SubjectTopic) -> Unit,
    onOpenHardSubjectRoute: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenWeakPoints: (Belt) -> Unit,    onOpenAllLists: (Belt) -> Unit,
    onOpenRandomPractice: (Belt) -> Unit,
    onOpenFinalExam: (Belt) -> Unit,
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit,
    onOpenSummaryScreen: (Belt) -> Unit,
    onOpenVoiceAssistant: (Belt) -> Unit,
    onOpenPdfMaterials: (Belt) -> Unit
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    val coachMode = remember { isCoach }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }

    // ✅ state לתפריט הצף (נשלט מהמסך)
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val belts = remember { Belt.order.filter { it != Belt.WHITE } }
    var topicsViewMode by rememberSaveable { mutableStateOf(TopicsViewMode.BY_BELT) }

    val initialBelt: Belt = remember(belts, kmiPrefs) {
        val regId =
            kmiPrefs.getStringCompat("current_belt")
                ?: kmiPrefs.getStringCompat("belt_current")

        val regBelt = regId?.let { Belt.fromId(it) }

        val base = when {
            regBelt != null && regBelt != Belt.WHITE -> regBelt
            else -> Belt.YELLOW
        }

        val next = when {
            regBelt == null || regBelt == Belt.WHITE -> Belt.ORANGE
            else -> {
                val idx = belts.indexOf(base).let { if (it < 0) 0 else it }
                if (idx < belts.lastIndex) belts[idx + 1] else belts.first()
            }
        }

        if (next in belts) next else Belt.ORANGE
    }

    var currentIndex by rememberSaveable {
        mutableIntStateOf(
            belts.indexOf(initialBelt).let { if (it >= 0) it else belts.indexOf(Belt.ORANGE).coerceAtLeast(0) }
        )
    }

    currentIndex = currentIndex.coerceIn(0, belts.lastIndex)

    val currentBelt = remember(currentIndex, belts, initialBelt) {
        belts.getOrNull(currentIndex) ?: initialBelt
    }

    LaunchedEffect(currentBelt) {
        vm.setSelectedBelt(currentBelt)
    }

    // ✅ shared-only: app SubjectTopic -> shared SubjectTopic
    fun SubjectTopic.toSharedSubject(): SharedSubjectTopic =
        SharedSubjectTopic(
            id = this.id,
            titleHeb = this.titleHeb,
            topicsByBelt = this.topicsByBelt,
            subTopicHint = this.subTopicHint,
            includeItemKeywords = this.includeItemKeywords.orEmpty(),
            requireAllItemKeywords = this.requireAllItemKeywords.orEmpty(),
            excludeItemKeywords = this.excludeItemKeywords.orEmpty()
        )

    // ✅ shared-only: items for subject via resolver (stable & cross-platform)
    fun itemsForSubject(belt: Belt, subject: SubjectTopic): List<String> =
        SubjectItemsResolver
            .resolveBySubject(belt = belt, subject = subject.toSharedSubject())
            .asSequence()
            .flatMap { it.items.asSequence() }
            .map { it.rawItem } // או it.canonicalId אם אתה רוצה מזהה יציב
            .toList()

    fun bestBeltForSubject(subject: SubjectTopic): Belt {
        // 1) קודם ננסה את החגורה הנוכחית במסך
        if (itemsForSubject(currentBelt, subject).isNotEmpty()) return currentBelt

        // 2) fallback: החגורה הראשונה של ה-subject שמחזירה items
        val fallback = subject.belts.firstOrNull { b -> itemsForSubject(b, subject).isNotEmpty() }
        return fallback ?: currentBelt
    }

    val backgroundBrush = remember(key1 = coachMode) {
        if (coachMode) {
            Brush.linearGradient(
                colors = listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF0EA5E9))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4))
            )
        }
    }

    var centerProgress by remember { mutableStateOf(currentIndex.toFloat()) }

    if (showPracticeMenu) {
        PracticeMenuDialog(
            canUseExtras = true,
            defaultBelt = currentBelt,
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
            }
        )
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = " ${currentBelt.heb}",
                onHome = onBackHome,
                onPickSearchResult = { key -> pickedKey = key },
                lockSearch = false,
                showBottomActions = true,
                centerTitle = true,
                showTopHome = false
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
                .statusBarsPadding()
        ) {
            // ✅ NEW: הגלילה רק לתוכן שמתחת לטאבים (בעיקר במצב BY_TOPIC)
            val contentScroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopicsViewModeToggle(
                    mode = topicsViewMode,
                    isCoach = coachMode,
                    onModeChange = {
                        topicsViewMode = it
                        clickSound()
                        haptic(true)
                    }
                )

                Spacer(Modifier.height(8.dp))

                // ✅ רק התוכן מתחת לטאבים יגלול (ולא הטאבים)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (topicsViewMode == TopicsViewMode.BY_TOPIC) {
                                base.verticalScroll(contentScroll)
                            } else base
                        }
                ) {
                    when (topicsViewMode) {
                        TopicsViewMode.BY_BELT -> {
                            TopicsCardForBelt(
                                belt = currentBelt,

                                // ✅ חובה: זה פותח את כל הנושא (במסך החדש/הנכון שלך)
                                onOpenTopic = onOpenTopic,

                                onOpenSubTopic = onOpenSubTopic,
                                onOpenDefenseMenu = onOpenDefenseMenu,
                                haptic = haptic,
                                clickSound = clickSound
                            )
                        }

                        TopicsViewMode.BY_TOPIC -> {
                            TopicsBySubjectCard(
                                currentBelt = currentBelt,

                                onSubjectClick = { belt, subject ->
                                    val best =
                                        if (itemsForSubject(belt, subject).isNotEmpty()) belt
                                        else bestBeltForSubject(subject)

                                    vm.setSelectedBelt(best)
                                    onOpenSubject(subject)
                                },

                                onOpenDefenseList = { belt, kind, pick ->
                                    onOpenDefenseMenu(belt, "$kind:$pick")
                                },

                                onOpenHardSubjectRoute = { belt, subjectId ->
                                    vm.setSelectedBelt(belt)
                                    onOpenHardSubjectRoute(belt, subjectId)
                                }
                            )
                        }
                    }
                }
            }

            // ✅ הקרוסלה רק במצב "לפי חגורה"
            if (topicsViewMode == TopicsViewMode.BY_BELT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .offset(y = 34.dp)
                        .padding(bottom = 0.dp)
                ) {
                    BeltArcPicker(
                        belts = belts,
                        currentIndex = currentIndex,
                        onIndexChange = { currentIndex = it },
                        onCenterTap = onNext,
                        onCenterProgress = { centerProgress = it },
                        haptic = haptic,
                        clickSound = clickSound,
                        inputEnabled = false
                    )
                }
            }

            // ✅ לחיצה מחוץ לתפריט סוגרת אותו (שכבה מעל המסך ומתחת לתפריט)
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

            // ✅ מרימים את הכפתור הצף כדי שלא יישב על העיגולים בתחתית
            val quickMenuBottomPad =
                if (topicsViewMode == TopicsViewMode.BY_BELT) 72.dp else 46.dp

            FloatingBeltQuickMenu(
                belt = currentBelt,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(
                        start = 18.dp,
                        bottom = quickMenuBottomPad
                    )
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                onWeakPoints = {
                    clickSound(); haptic(true)
                    onOpenWeakPoints(currentBelt)
                },
                onAllLists = {
                    clickSound(); haptic(true)
                    onOpenAllLists(currentBelt)
                },
                onPractice = {
                    clickSound(); haptic(true)
                    showPracticeMenu = true
                },
                onSummary = {
                    clickSound(); haptic(true)
                    onOpenSummaryScreen(currentBelt)
                },
                onVoice = {
                    clickSound(); haptic(true)
                    onOpenVoiceAssistant(currentBelt)
                },
                onPdf = {
                    clickSound(); haptic(true)
                    onOpenPdfMaterials(currentBelt)
                }
            )

            pickedKey?.let {
                // TODO: אם תרצה, ננתב מפה למסך תרגיל/נושא לפי parseSearchKey(...)
            }
        }
    }
}

/* ----------------------------- מתג "לפי חגורה / לפי נושא" ----------------------------- */

@Composable
internal fun TopicsViewModeToggle(
    mode: TopicsViewMode,
    isCoach: Boolean,
    onModeChange: (TopicsViewMode) -> Unit
) {
    val selectedIndex = if (mode == TopicsViewMode.BY_BELT) 0 else 1

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(bottom = 6.dp),
        color = Color.White.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-4).dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color.White.copy(alpha = 0.95f))
            )

            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {},
                indicator = { positions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedIndex]),
                        height = 3.dp,
                        color = Color.White
                    )
                },
                modifier = Modifier.matchParentSize()
            ) {
                Tab(
                    selected = (mode == TopicsViewMode.BY_BELT),
                    onClick = { if (mode != TopicsViewMode.BY_BELT) onModeChange(TopicsViewMode.BY_BELT) },
                    text = { Text("לפי חגורה", fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.7f)
                )

                Tab(
                    selected = (mode == TopicsViewMode.BY_TOPIC),
                    onClick = { if (mode != TopicsViewMode.BY_TOPIC) onModeChange(TopicsViewMode.BY_TOPIC) },
                    text = { Text("לפי נושא", fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/* ----------------------------- כרטיס “נושאים בחגורה” ---------------------------- */

@Composable
private fun TopicsCardForBelt(
    belt: Belt,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit
) {
    val topicTitles: List<String> = remember(belt) {
        TopicsEngine.topicTitlesFor(belt)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    val detailsByTitle: Map<String, TopicDetails> = remember(belt, topicTitles) {
        topicTitles.associateWith { title -> topicDetailsFor(belt, title) }
    }

    var expandedTopic by rememberSaveable(belt.id) { mutableStateOf<String?>(null) }

    val rowMinHeight = 76.dp
    val visibleRows = 4
    val listHeight = rowMinHeight * visibleRows + 8.dp

    val fabSize = 120.dp
    val desiredOverlap = fabSize / 2
    val fabClearance = desiredOverlap

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 0.dp)
            .padding(bottom = fabClearance + 12.dp)
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(
                text = "נושאים בחגורה",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF263238),
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            if (topicTitles.isEmpty()) {
                Text(
                    text = "אין נושאים להצגה",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF546E7A)
                )
            } else {
                val contentOnBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black
                val topicsScroll = rememberScrollState(0)

                Column(
                    modifier = Modifier
                        .heightIn(max = listHeight)
                        .verticalScroll(topicsScroll)
                ) {
                    topicTitles.forEach { title ->

                        val details = detailsByTitle[title]
                            ?: TopicDetails(itemCount = 0, subTitles = emptyList())

                        val subTitles: List<String> =
                            details.subTitles
                                .asSequence()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .filter { it != title.trim() }     // ✅ מסיר "תת־נושא פייק" ששווה לנושא
                                .distinct()
                                .toList()

                        val itemCount = details.itemCount
                        val subCount = subTitles.size

// ✅ יש תתי־נושאים "אמיתיים" רק אם נשאר משהו אחרי הסינון
                        val hasSubs = subTitles.isNotEmpty()

                        val countsLine = if (subCount > 0) {
                            "$subCount תתי נושאים  •  $itemCount תרגילים"
                        } else {
                            "$itemCount תרגילים"
                        }

                        val isExpanded = expandedTopic == title
                        val isDefenseTopic = title.trim().contains("הגנות")

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = rowMinHeight)
                                .padding(horizontal = 12.dp, vertical = 3.dp) // ✅ היה 6.dp
                                .clickable {
                                    clickSound()
                                    haptic(true)

                                    // ✅ FIX: אם יש תתי־נושאים (גם בהגנות) – פותחים/סוגרים רשימה
                                    if (hasSubs) {
                                        expandedTopic = if (isExpanded) null else title
                                    } else {
                                        // ✅ אין תתי־נושאים → ניווט
                                        if (isDefenseTopic) {
                                            onOpenDefenseMenu(belt, title)
                                        } else {
                                            onOpenTopic(belt, title)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(28.dp),
                            color = belt.color,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp), // ✅ היה 12.dp
                                horizontalAlignment = Alignment.End
                            ) {                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasSubs) {
                                        Icon(
                                            imageVector = if (isExpanded)
                                                Icons.Filled.KeyboardArrowUp
                                            else
                                                Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = contentOnBelt.copy(alpha = 0.95f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = contentOnBelt,
                                        textAlign = TextAlign.Right,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = countsLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentOnBelt.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Right,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (hasSubs && isExpanded) {
                                    Spacer(Modifier.height(6.dp)) // ✅ היה 10.dp

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(8.dp), // ✅ היה 10.dp
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        subTitles.forEach { sub ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp) // ✅ היה 4.dp
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)

                                                        // ✅ FIX: גם בהגנות מעבירים את שם תת־הנושא שנבחר
                                                        onOpenSubTopic(belt, title, sub)
                                                    },
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color.White.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = sub,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    textAlign = TextAlign.Right,
                                                    color = contentOnBelt,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = "פתח את כל הנושא",
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    clickSound()
                                                    haptic(true)

                                                    // ✅ FIX: גם כאן "הגנות" → MaterialsScreen
                                                    if (isDefenseTopic) {
                                                        onOpenDefenseMenu(belt, title)
                                                    } else {
                                                        onOpenTopic(belt, title)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = contentOnBelt.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/* ------------------------------- קרוסלת חגורות ------------------------------- */

@Composable
private fun BeltArcPicker(
    belts: List<Belt>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onCenterProgress: (Float) -> Unit = {},
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit,
    inputEnabled: Boolean = true
) {
    val big = 120.dp
    val small = 68.dp
    val step = small + 44.dp
    val arcDepth = 84.dp
    val pickerHeight = small + arcDepth

    val density = androidx.compose.ui.platform.LocalDensity.current
    val stepPx = with(density) { step.toPx() }

    val center = remember { Animatable(currentIndex.toFloat()) }
    LaunchedEffect(currentIndex) {
        if (currentIndex.toFloat() != center.targetValue) {
            center.animateTo(
                targetValue = currentIndex.toFloat(),
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        }
    }
    LaunchedEffect(center.value) { onCenterProgress(center.value) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        belts.forEachIndexed { index, belt ->
            val rel = index - center.value
            val dist = kotlin.math.abs(rel)
            val hide = dist > 2.6f
            val t = kotlin.math.min(1f, dist / 2f)

            val drop = arcDepth * (1f - cos(t * (PI / 2)).toFloat())
            val grow = (1f - kotlin.math.min(1f, dist)).coerceIn(0f, 1f)
            val targetSize = small + (big - small) * grow
            val size by animateDpAsState(targetValue = targetSize, label = "belt-size")

            val targetAlpha = if (hide) 0f else 0.75f + 0.25f * (1f - t)
            val alpha by animateFloatAsState(targetValue = targetAlpha, label = "belt-alpha")

            val xTarget = step * rel
            val xDp by animateDpAsState(targetValue = xTarget, label = "belt-x")

            val circleColor = belt.color.copy(alpha = 0.96f)

            val sideBoost = small
            val boostFactor = kotlin.math.min(1f, dist)
            val yDrop = drop + sideBoost * boostFactor

            val isCenter = dist < 0.25f

            val base = Modifier
                .offset(x = xDp, y = yDrop)
                .size(size)
                .alpha(alpha)
                .zIndex(if (isCenter) 2f else 1f)
                .then(
                    if (isCenter) {
                        Modifier.circleGlow(
                            color = circleColor,
                            radius = size * 0.95f,
                            intensity = 0.58f
                        )
                    } else Modifier
                )

            val gestures = Modifier
                .pointerInput(belts, index) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val prevIndex = currentIndex
                            val snap = center.value.roundToInt().coerceIn(0, belts.lastIndex)

                            scope.launch {
                                center.animateTo(
                                    targetValue = snap.toFloat(),
                                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                                )
                                onIndexChange(snap)

                                if (snap != prevIndex) {
                                    clickSound()
                                    haptic(true)
                                }
                            }
                        }
                    ) { _, drag ->
                        val delta = drag / stepPx
                        val next = (center.value + delta).coerceIn(0f, belts.lastIndex.toFloat())
                        scope.launch { center.snapTo(next) }
                    }
                }
                .then(
                    if (!inputEnabled) Modifier
                    else Modifier.noRippleClickable {
                        if (isCenter) {
                            clickSound(); haptic(true)
                            onCenterTap()
                        } else {
                            clickSound(); haptic(true)
                            val snap = index.coerceIn(0, belts.lastIndex)
                            scope.launch {
                                center.animateTo(
                                    targetValue = snap.toFloat(),
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                                onIndexChange(snap)
                            }
                        }
                    }
                )

            Box(
                modifier = base.then(gestures),
                contentAlignment = Alignment.Center
            ) {
                val forceWhiteOutline =
                    belt.heb.lowercase().let { it.contains("שחור") || it.contains("חום") }
                val outlineColor = when {
                    forceWhiteOutline -> Color.White
                    belt.color.luminance() < 0.5f -> Color.White
                    else -> Color.Black
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // ✅ רק לעיגול המרכזי: טבעת צבעונית מסתובבת מסביב
                    if (isCenter) {
                        RotatingOrbitRing(
                            modifier = Modifier.fillMaxSize(),
                            base = circleColor
                        )
                    }

                    // בתוך BeltArcPicker(...) באזור של ה-Box שמצייר את העיגול

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // ✅ רק לעיגול המרכזי: טבעת צבעונית מסתובבת מסביב
                        if (isCenter) {
                            RotatingOrbitRing(
                                modifier = Modifier.fillMaxSize(),
                                base = circleColor
                            )
                        }

                        // ✅ כדי שהטבעת לא “תכוסה” ע״י העיגול, מכניסים את העיגול פנימה קצת רק במרכז
                        val ringPad = if (isCenter) 8.dp else 0.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(ringPad)
                                .clip(CircleShape)
                                .border(BorderStroke(3.dp, outlineColor), CircleShape)
                                .background(circleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCenter) {
                                val clean = remember(belt.heb) {
                                    val s = belt.heb.trim()
                                    if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
                                }
                                Text(
                                    text = "חגורה\n$clean",
                                    color = if (belt.color.luminance() < 0.5f) Color.White else Color.Black,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                                    modifier = Modifier.padding(8.dp)
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
private fun RotatingOrbitRing(
    modifier: Modifier = Modifier,
    base: Color
) {
    val ringStroke = 7.dp
    val gapStroke = 7.dp

    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing)
        ),
        label = "ring-angle"
    )

    val sweep = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF22D3EE), // cyan
            Color(0xFFA78BFA), // purple
            Color(0xFFF472B6), // pink
            Color(0xFFFBBF24), // amber
            Color(0xFF22D3EE)
        )
    )

    Canvas(modifier = modifier) {
        val strokePx = ringStroke.toPx()
        val inset = strokePx / 2f

        // שכבת "צל" דקה כדי להיראות יותר מודרני
        drawArc(
            color = base.copy(alpha = 0.16f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(width = gapStroke.toPx())
        )

        // הטבעת המסתובבת (צבעונית)
        rotate(degrees = angle) {
            drawArc(
                brush = sweep,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}