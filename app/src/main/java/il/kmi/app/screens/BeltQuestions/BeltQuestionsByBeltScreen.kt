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
        isCoach = isCoach,
        onNext = onNext,
        onBackHome = onBackHome,
        onOpenExercise = onOpenExercise,
        onOpenTopic = onOpenTopic,
        onOpenDefenseMenu = onOpenDefenseMenu,
        onOpenSubject = onOpenSubject,
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
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenWeakPoints: (Belt) -> Unit,
    onOpenAllLists: (Belt) -> Unit,
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

    val startBelt: Belt = remember {
        val fromVm = try { vm.selectedBelt.value } catch (_: Exception) { null }
        if (fromVm != null) {
            fromVm
        } else {
            val regId = kmiPrefs.getStringCompat("current_belt")
                ?: kmiPrefs.getStringCompat("belt_current")
            val regBelt = regId?.let { Belt.fromId(it) }

            val base = when {
                regBelt == null || regBelt == Belt.WHITE -> belts.firstOrNull() ?: Belt.YELLOW
                else -> regBelt
            }

            val idx = belts.indexOf(base).coerceAtLeast(0)
            if (idx < belts.lastIndex) belts[idx + 1] else belts[idx]
        }
    }

    var currentIndex by remember(startBelt, belts) {
        mutableStateOf(belts.indexOf(startBelt).coerceAtLeast(0))
    }
    currentIndex = currentIndex.coerceIn(0, belts.lastIndex)

    val currentBelt = remember(currentIndex, belts, startBelt) {
        belts.getOrNull(currentIndex) ?: startBelt
    }

    LaunchedEffect(currentBelt) { vm.setSelectedBelt(currentBelt) }

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
                .statusBarsPaddingCompat()
        ) {
            val topColumnModifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .let { base ->
                    if (topicsViewMode == TopicsViewMode.BY_TOPIC) {
                        base.verticalScroll(rememberScrollState())
                    } else base
                }

            Column(
                modifier = topColumnModifier,
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

                when (topicsViewMode) {
                    TopicsViewMode.BY_BELT -> {
                        TopicsCardForBelt(
                            belt = currentBelt,
                            onOpenTopic = onOpenTopic,
                            onOpenSubTopicsScreen = { b, topic -> onOpenDefenseMenu(b, topic) },
                            onOpenSubTopic = onOpenSubTopic,
                            onOpenDefenseMenu = onOpenDefenseMenu,
                            haptic = haptic,
                            clickSound = clickSound
                        )
                    }

                    TopicsViewMode.BY_TOPIC -> {
                        TopicsBySubjectCard(
                            onSubjectClick = { belt, subject ->
                                val best =
                                    if (itemsForSubject(belt, subject).isNotEmpty()) belt
                                    else bestBeltForSubject(subject)

                                vm.setSelectedBelt(best)
                                onOpenSubject(subject)
                            },
                            onOpenTopic = { b, topicTitle ->
                                vm.setSelectedBelt(b)
                                onOpenTopic(b, topicTitle)
                            }
                        )
                    }
                }
            }

            // ✅ הקרוסלה רק במצב "לפי חגורה"
            if (topicsViewMode == TopicsViewMode.BY_BELT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPaddingCompat()
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
// במצב "לפי חגורה" צריך יותר מרווח בגלל הקרוסלה למטה
            val quickMenuBottomPad =
                if (topicsViewMode == TopicsViewMode.BY_BELT) 72.dp else 46.dp

            FloatingBeltQuickMenu(
                belt = currentBelt,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPaddingCompat()
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
    onOpenSubTopicsScreen: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit
) {
    val topicTitles: List<String> = remember(belt) { topicTitlesForCompat(belt) }

    val detailsByTitle: Map<String, TopicDetails> = remember(belt, topicTitles) {
        topicTitles.associateWith { title -> topicDetailsFor(belt, title) }
    }

    var expandedTopic by rememberSaveable(belt.id) { mutableStateOf<String?>(null) }

    val rowMinHeight = 88.dp
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
                                .map { it.trim() }
                                .filter { it.isNotBlank() && it != title.trim() }
                                .distinct()

                        val itemCount = details.itemCount
                        val subCount = subTitles.size
                        val hasSubs = subCount > 0

                        val countsLine = if (subCount > 0) {
                            "$subCount תתי נושאים  •  $itemCount תרגילים"
                        } else {
                            "$itemCount תרגילים"
                        }

                        val isExpanded = expandedTopic == title

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = rowMinHeight)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable {
                                    clickSound()
                                    haptic(true)
                                    if (hasSubs) {
                                        expandedTopic = if (isExpanded) null else title
                                    } else {
                                        onOpenTopic(belt, title)
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
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
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
                                    Spacer(Modifier.height(10.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        subTitles.forEach { sub ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)
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
                                            text = "פתח מסך תתי נושאים",
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    clickSound()
                                                    haptic(true)
                                                    onOpenSubTopicsScreen(belt, title)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = contentOnBelt.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            text = "פתח את כל הנושא",
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    clickSound()
                                                    haptic(true)
                                                    onOpenTopic(belt, title)
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
                        .fillMaxSize()
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
