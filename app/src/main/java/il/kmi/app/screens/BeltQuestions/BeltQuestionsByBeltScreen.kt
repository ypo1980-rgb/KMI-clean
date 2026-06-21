package il.kmi.app.screens.BeltQuestions

import android.content.SharedPreferences
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import il.kmi.app.ui.FloatingQuickMenu
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import il.kmi.shared.domain.TopicsEngine
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.content.ExerciseTitlesEn
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.ui.QuickMenuTriggerMode
import il.kmi.app.subscription.AccessMode
import il.kmi.app.subscription.AccessModeResolver
import il.kmi.app.subscription.LockedContentPolicy
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.domain.ExerciseExplanationResolver


/* ------------------------------ Helpers מקומיים למסך ------------------------------ */

internal fun topicDetailsFor(belt: Belt, topicTitle: String): TopicDetails {
    val details = TopicsEngine.topicDetailsFor(belt, topicTitle.trim())

    val topicTrim = topicTitle.trim()
    val cleanSubs = details.subTitles
        .map { it.trim() }
        .filter { it.isNotBlank() && it != topicTrim }
        .distinct()

    return TopicDetails(
        // מספרי תרגילים כבר מגיעים רק מ-ExerciseCountProvider.
        // השדה הזה נשאר 0 כדי למנוע שימוש בטעות בספירה הישנה.
        itemCount = 0,
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
    topic: String,
    lang: AppLanguage
): String {
    val isEnglish = lang == AppLanguage.ENGLISH

    val display = ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = topic,
        item = display,
        isEnglish = isEnglish
    ).trim()

    val cleaned = if ("::" in resolved) {
        resolved
            .split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: resolved
    } else {
        resolved
    }.trim()

    val isFallback = if (isEnglish) {
        cleaned.isBlank() ||
                cleaned.startsWith("Detailed explanation for:") ||
                cleaned.startsWith("There is currently no explanation")
    } else {
        cleaned.isBlank() ||
                cleaned.startsWith("הסבר מפורט על") ||
                cleaned.startsWith("אין כרגע")
    }

    if (!isFallback) {
        return cleaned
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
}

private fun saveBeltQuestionByBeltNote(
    prefs: SharedPreferences,
    noteKey: String,
    text: String
) {
    val clean = text.trim()

    prefs.edit().apply {
        if (clean.isBlank()) {
            remove(noteKey)
        } else {
            putString(noteKey, clean)
        }
    }.apply()
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

internal fun formatCount(n: Int, lang: AppLanguage): String = when {
    lang == AppLanguage.ENGLISH -> {
        when {
            n <= 0 -> "0 exercises"
            n == 1 -> "1 exercise"
            else -> "$n exercises"
        }
    }

    else -> {
        when {
            n <= 0 -> "0 תרגילים"
            n == 1 -> "תרגיל 1"
            else -> "$n תרגילים"
        }
    }
}

private fun SharedContentRepo.SubTopic.totalExercisesCountDeep(): Int {
    val directCount = items.size
    val nestedCount = subTopics.sumOf { it.totalExercisesCountDeep() }
    return directCount + nestedCount
}

private fun topicExercisesCountForCurrentBelt(
    belt: Belt,
    topicTitle: String
): Int {
    val cleanTopicTitle = topicTitle.trim()

    return SharedContentRepo
        .getSubTopicsFor(
            belt = belt,
            topicTitle = cleanTopicTitle
        )
        .sumOf { subTopic ->
            subTopic.totalExercisesCountDeep()
        }
}

private fun subTopicStatsLineForUi(
    subTopic: SharedContentRepo.SubTopic,
    lang: AppLanguage
): String {
    val exercisesCount = subTopic.totalExercisesCountDeep()

    return if (lang == AppLanguage.ENGLISH) {
        if (exercisesCount == 1) "1 exercise" else "$exercisesCount exercises"
    } else {
        if (exercisesCount == 1) "תרגיל 1" else "$exercisesCount תרגילים"
    }
}

internal fun beltTitleForUi(belt: Belt, lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH) belt.en else belt.heb

internal fun beltShortNameForUi(belt: Belt, lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH) {
        belt.en.removeSuffix(" Belt")
    } else {
        belt.heb.removePrefix("חגורה").trim()
    }

internal fun topicTitleForUi(title: String, lang: AppLanguage): String {
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(title.trim())
    } else {
        title
    }
}

private fun beltTopicImageFor(belt: Belt, topicTitle: String): Int? {
    val clean = topicTitle.trim()

    return when {
        // הגנות
        clean.contains("הגנות") -> R.drawable.topic_defenses

        // שחרורים / חביקות
        clean.contains("שחרורים") ||
                clean.contains("שחרור") ||
                clean.contains("חביקות") ||
                clean.contains("חביקת") -> R.drawable.topic_body_hug_releases

        // מכות מרפק — רק חגורה ירוקה
        belt == Belt.GREEN &&
                clean.contains("מכות מרפק") -> R.drawable.topic_elbow_strikes

        // מכות ידיים / עבודת ידיים
        clean.contains("מכות ידיים") ||
                clean.contains("מכות יד") ||
                clean.contains("עבודת ידיים") ||
                clean.contains("עבודת יד") -> R.drawable.topic_hand_strikes

        // בלימות וגלגולים
        clean.contains("בלימות") ||
                clean.contains("גלגולים") ||
                clean.contains("גלגול") ||
                clean.contains("בלימה") -> R.drawable.topic_forward_roll

        // עמידת מוצא
        clean.contains("עמידת מוצא") -> R.drawable.topic_ready_stance

        // עבודת קרקע
        clean.contains("עבודת קרקע") -> R.drawable.topic_ground_fighting

        // בעיטות
        clean.contains("בעיטות") ||
                clean.contains("בעיטה") -> R.drawable.topic_kicks

        // קוואלר
        clean.contains("קוואלר") -> R.drawable.topic_kavaler

        // כללי
        clean.contains("כללי") -> R.drawable.topic_general

        // מקל / רובה — כרגע משתמשים בתמונת הגנות עם נשקים
        clean.contains("מקל") ||
                clean.contains("רובה") -> R.drawable.topic_defenses

        else -> null
    }
}

/* ------------------------------ API ציבורי למסך ------------------------------ */
@Composable
fun BeltQuestionsByBeltScreen(
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    isCoach: Boolean,
    onNext: () -> Unit,
    onBackHome: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenSubject: (SubjectTopic) -> Unit,
    onOpenHardSubjectRoute: (Belt, String) -> Unit = { _, _ -> },
    onOpenSubTopic: (Belt, String, String) -> Unit = { _, _, _ -> },
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenRandomPractice: (Belt) -> Unit = {},
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit = { _, _ -> },
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
        onOpenSubscription = onOpenSubscription,
        onOpenExercise = onOpenExercise,
        onOpenTopic = onOpenTopic,
        onOpenDefenseMenu = onOpenDefenseMenu,
        onOpenSubject = onOpenSubject,
        onOpenHardSubjectRoute = onOpenHardSubjectRoute,
        onOpenSubTopic = onOpenSubTopic,
        onOpenWeakPoints = onOpenWeakPoints,
        onOpenAllLists = onOpenAllLists,
        onOpenRandomPractice = onOpenRandomPractice,
        onOpenRandomPracticeByTopic = onOpenRandomPracticeByTopic,
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
    onOpenSubscription: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSubject: (SubjectTopic) -> Unit,
    onOpenHardSubjectRoute: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenWeakPoints: (Belt) -> Unit,
    onOpenAllLists: (Belt) -> Unit,
    onOpenRandomPractice: (Belt) -> Unit,
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit,
    onOpenFinalExam: (Belt) -> Unit,
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit,
    onOpenSummaryScreen: (Belt) -> Unit,
    onOpenVoiceAssistant: (Belt) -> Unit,
    onOpenPdfMaterials: (Belt) -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }

    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()
    // הערות תרגילים מנוהלות עכשיו בדיאלוג הגלובלי החדש דרך KmiTopBar
    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", android.content.Context.MODE_PRIVATE)
    }

    var accessRefreshTick by remember { mutableIntStateOf(0) }

    // מרענן מצב גישה גם בלי שינוי ב-SharedPreferences,
    // כדי שמנוי שפג יחזיר מנעולים גם כשהמשתמש נשאר במסך.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            accessRefreshTick++
        }
    }

    DisposableEffect(userSp, subsSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedSp, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_product" ||
                key == "sub_access_until" ||
                key == "access_changed_at"
            ) {
                accessRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val hasManagerAccess = remember(accessRefreshTick) {
        KmiAccess.hasFullAccess(userSp) ||
                KmiAccess.hasFullAccess(subsSp)
    }

    val accessMode = AccessModeResolver.resolve(
        hasManagerAccess = hasManagerAccess
    )

    val hasUnlockedAccess = accessMode == AccessMode.OPEN

    fun normalizeFavoriteId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    val coachMode = remember { isCoach }
    // החיפוש והסברי התרגילים עוברים דרך KmiTopBar + ExercisePremiumSearchDialog

    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }

    // ✅ state לתפריט הצף (נשלט מהמסך)
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val belts = remember { Belt.order.filter { it != Belt.WHITE } }
    var topicsViewMode by rememberSaveable { mutableStateOf(TopicsViewMode.BY_BELT) }

    val initialBelt: Belt = remember(belts, kmiPrefs, userSp) {
        val regId =
            kmiPrefs.getStringCompat("current_belt")
                ?: kmiPrefs.getStringCompat("belt_current")
                ?: userSp.getString("current_belt", null)
                ?: userSp.getString("belt_current", null)
                ?: userSp.getString("currentBelt", null)
                ?: userSp.getString("belt", null)

        val cleanRegId = regId?.trim().orEmpty()
        val regBelt = cleanRegId.takeIf { it.isNotBlank() }?.let { Belt.fromId(it) }

        val next = when {
            // אם המשתמש לא נרשם עם אף חגורה — ברירת מחדל כתומה
            cleanRegId.isBlank() || regBelt == null -> Belt.ORANGE

            // אם המשתמש נרשם עם לבנה — החגורה הבאה היא צהובה
            regBelt == Belt.WHITE -> Belt.YELLOW

            // בכל שאר החגורות — עוברים לחגורה הבאה בתור
            else -> {
                val idx = belts.indexOf(regBelt)
                if (idx >= 0 && idx < belts.lastIndex) {
                    belts[idx + 1]
                } else {
                    regBelt
                }
            }
        }

        if (next in belts) next else Belt.ORANGE
    }

    var currentIndex by rememberSaveable {
        mutableIntStateOf(
            belts.indexOf(initialBelt)
                .let { if (it >= 0) it else belts.indexOf(Belt.ORANGE).coerceAtLeast(0) }
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

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FBFF),
                Color(0xFFEAF4FF),
                Color(0xFFB7DDF7),
                Color(0xFF1F78B4),
                Color(0xFF062B4A)
            )
        )
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
            },
            onPracticeByTopicSelected = { beltArg, topicArg ->
                showPracticeMenu = false
                onOpenRandomPracticeByTopic(beltArg, topicArg)
            }
        )
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = if (topicsViewMode == TopicsViewMode.BY_TOPIC) {
                    if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) {
                        "Exercises by Topic"
                    } else {
                        "תרגילים לפי נושא"
                    }
                } else {
                    beltTitleForUi(currentBelt, langManager.getCurrentLanguage())
                },
                onHome = onBackHome,
                // החיפוש הגלובלי נפתח ומטופל פנימית בתוך KmiTopBar
                lockSearch = false,
                showBottomActions = true,
                centerTitle = true,
                showTopHome = false,
                showTopBeltIcon = topicsViewMode == TopicsViewMode.BY_BELT,
                topBeltIconRes = if (topicsViewMode == TopicsViewMode.BY_TOPIC) null else null,
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
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (topicsViewMode == TopicsViewMode.BY_TOPIC) {
                                base.verticalScroll(contentScroll)
                            } else base
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (topicsViewMode) {
                            TopicsViewMode.BY_BELT -> {
                                TopicsCardForBelt(
                                    belt = currentBelt,
                                    lang = langManager.getCurrentLanguage(),
                                    accessMode = accessMode,
                                    onOpenSubscription = onOpenSubscription,

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
                                    hasAccess = hasUnlockedAccess,
                                    onOpenSubscription = {
                                        clickSound()
                                        haptic(true)
                                        onOpenSubscription()
                                    },

                                    onSubjectClick = { belt, subject ->
                                        val best =
                                            if (itemsForSubject(belt, subject).isNotEmpty()) belt
                                            else bestBeltForSubject(subject)

                                        vm.setSelectedBelt(best)
                                        onOpenSubject(subject)
                                    },

                                    onOpenDefenseList = { belt, kind, pick ->
                                        vm.setSelectedBelt(belt)

                                        val cleanKind = kind.trim()
                                        val cleanPick = pick.trim()

                                        when {
                                            cleanKind.contains("knife", ignoreCase = true) ||
                                                    cleanPick.contains("סכין") ||
                                                    cleanPick.contains("knife", ignoreCase = true) -> {
                                                onOpenHardSubjectRoute(belt, "knife_defense")
                                            }

                                            cleanPick.contains("אקדח") ||
                                                    cleanPick.contains("gun", ignoreCase = true) -> {
                                                onOpenHardSubjectRoute(belt, "gun_threat_defense")
                                            }

                                            cleanPick.contains("מקל") ||
                                                    cleanPick.contains("stick", ignoreCase = true) -> {
                                                onOpenHardSubjectRoute(belt, "stick_defense")
                                            }

                                            cleanPick.contains("בעיטה") ||
                                                    cleanPick.contains("kick", ignoreCase = true) -> {
                                                onOpenHardSubjectRoute(belt, "kicks")
                                            }

                                            else -> {
                                                onOpenDefenseMenu(belt, "$cleanKind:$cleanPick")
                                            }
                                        }
                                    },

                                    onOpenHardSubjectRoute = { belt, subjectId ->
                                        vm.setSelectedBelt(belt)
                                        onOpenHardSubjectRoute(belt, subjectId)
                                    },

                                    onOpenKicksHardLocal = {
                                        android.util.Log.d(
                                            "KMI_TOPIC_CLICK",
                                            "OPEN_KICKS_HARD_FROM_BY_TOPIC | currentBelt=${currentBelt.id} | route=kicks_hard | accessMode=$accessMode | hasUnlockedAccess=$hasUnlockedAccess"
                                        )

                                        vm.setSelectedBelt(currentBelt)
                                        onOpenHardSubjectRoute(currentBelt, "kicks_hard")
                                    },

                                    accessMode = accessMode
                                )

                                // ✅ אין יותר כפתור תחתון של "מבט מהיר".
                                // התפריט המהיר נפתח עכשיו מהמלבן הצדדי כמו במצב "לפי חגורה".
                                Spacer(Modifier.height(24.dp))
                            }
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
                        inputEnabled = false,

                        // ✅ לא משתמשים יותר בהיפוך לפי שפה.
                        // הכיוון ייקבע רק מתוך הגרירה עצמה בתוך BeltArcPicker.
                        reverseSwipeDirection = true
                    )
                }
            }

            if (topicsViewMode == TopicsViewMode.BY_BELT) {
                FloatingQuickMenu(
                    belt = currentBelt,
                    modifier = Modifier
                        // ✅ מלבן צדדי בצד שמאל של המסך
                        .align(Alignment.CenterStart)
                        .zIndex(999f),
                    expanded = quickMenuExpanded,
                    onExpandedChange = { quickMenuExpanded = it },

                    // ✅ במקום הכפתור הצף העגול/מרובע הישן
                    triggerMode = QuickMenuTriggerMode.SideRail,

                    includePractice = true,
                    hasFullAccess = hasUnlockedAccess,
                    onLockedItemClick = {
                        clickSound(); haptic(true)
                        onOpenSubscription()
                    },
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
            }

            if (topicsViewMode == TopicsViewMode.BY_TOPIC) {
                FloatingQuickMenu(
                    belt = currentBelt,
                    modifier = Modifier
                        // ✅ אותו אייקון צדדי כמו במצב "לפי חגורה"
                        .align(Alignment.CenterStart)
                        .zIndex(999f),
                    expanded = quickMenuExpanded,
                    onExpandedChange = { quickMenuExpanded = it },
                    triggerMode = QuickMenuTriggerMode.SideRail,
                    includePractice = true,
                    includeAllLists = false,
                    includeSummary = false,
                    hasFullAccess = hasUnlockedAccess,
                    onLockedItemClick = {
                        clickSound(); haptic(true)
                        onOpenSubscription()
                    },
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
            }
            // אין כאן יותר דיאלוג חיפוש/הסבר מקומי.
            // כל החיפוש, ההסבר, המועדפים והערות המשתמש מטופלים דרך KmiTopBar.
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
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    // ✅ סדר טאבים לפי שפה:
    // עברית: שמאל = לפי נושא, ימין = לפי חגורה
    // אנגלית: שמאל = By Topic, ימין = By Belt
    //
    // שים לב:
    // ה-TabRow עצמו נשאר LTR כדי שהצדדים הפיזיים לא יתהפכו.
    // לכן הפריט הראשון ברשימה = שמאל, הפריט השני = ימין.
    val tabs = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                TopicsViewMode.BY_TOPIC to "By Topic",
                TopicsViewMode.BY_BELT to "By Belt"
            )
        } else {
            listOf(
                TopicsViewMode.BY_TOPIC to "לפי נושא",
                TopicsViewMode.BY_BELT to "לפי חגורה"
            )
        }
    }

    val selectedIndex = tabs
        .indexOfFirst { it.first == mode }
        .coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(bottom = 6.dp),
        color = Color(0xFF062B4A).copy(alpha = 0.78f),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.34f)
        ),
        shape = RoundedCornerShape(18.dp)
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
                    .background(Color.White.copy(alpha = 0.65f))
            )

            // ✅ חשוב:
            // מכריחים LTR רק לסידור הפיזי של שני הטאבים,
            // כדי שצד שמאל וצד ימין יהיו קבועים ולא יתהפכו בגלל RTL.
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
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
                    tabs.forEach { (tabMode, label) ->
                        Tab(
                            selected = (mode == tabMode),
                            onClick = {
                                if (mode != tabMode) {
                                    onModeChange(tabMode)
                                }
                            },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
    }
}

/* ----------------------------- כרטיס “נושאים בחגורה” ---------------------------- */

@Composable
private fun PremiumPulsingLockBadge(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val pulse = rememberInfiniteTransition(label = "topicLockPulse")

    val scale by pulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "topicLockScale"
    )

    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = Color(0xFFF59E0B),
        modifier = modifier
            .size(20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1f
            }
    )
}

@Composable
private fun TopicsCardForBelt(
    belt: Belt,
    lang: AppLanguage,
    accessMode: il.kmi.app.subscription.AccessMode,
    onOpenSubscription: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit
) {
    val isEnglish = lang == AppLanguage.ENGLISH
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // ✅ חשוב:
    // באנגלית משתמשים ב-Left פיזי ולא ב-Start,
    // כדי שלא יתהפך אם המסך עדיין מקבל RTL.
    val titleTextAlignByLang = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalByLang = if (isEnglish) Alignment.Start else Alignment.End
    val layoutByLang =
        if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
        else androidx.compose.ui.unit.LayoutDirection.Rtl

    val cardBg = if (isDarkTheme) Color(0xFF101827) else Color.White

    val cardBorder = if (isDarkTheme) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }

    val titleColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF263238)
    val rowTitleColor = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF1F2937)

    // ✅ צבע הספירה לפי צבע החגורה — יותר ברור, בלי לצבוע את כל שורת הנושא
    val rowSubColor = if (isDarkTheme) {
        belt.color.copy(alpha = 1f)
    } else {
        belt.color.copy(alpha = 1f)
    }

    // ✅ שורת הנושא עצמה נשארת נקייה, בלי ריבועים
    val rowBg = Color.Transparent

    val rowGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Transparent
        )
    )

    // ✅ כרטיס תתי־הנושאים יקבל צבע חגורה עדין אבל עם נוכחות
    val subTopicsCardBg = if (isDarkTheme) {
        belt.color.copy(alpha = 0.16f)
    } else {
        belt.color.copy(alpha = 0.13f)
    }

    val subTopicsCardBorder = if (isDarkTheme) {
        belt.color.copy(alpha = 0.34f)
    } else {
        belt.color.copy(alpha = 0.38f)
    }

    val subDividerColor = if (isDarkTheme) {
        belt.color.copy(alpha = 0.34f)
    } else {
        belt.color.copy(alpha = 0.42f)
    }

    val rawTopicTitles: List<String> = remember(belt) {
        TopicsEngine.topicTitlesFor(belt)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    val detailsByTitle: Map<String, TopicDetails> = remember(belt, rawTopicTitles) {
        rawTopicTitles.associateWith { title -> topicDetailsFor(belt, title) }
    }

    val exerciseCountByTitle = remember(belt, rawTopicTitles) {
        rawTopicTitles.associateWith { title ->
            topicExercisesCountForCurrentBelt(
                belt = belt,
                topicTitle = title
            )
        }
    }

    // ✅ נושאים שיש להם תתי־נושאים תמיד מופיעים למעלה.
    // ✅ בחגורה צהובה נותנים סדר מיוחד:
    // עבודת ידיים -> הגנות -> שחרורים.
    // ✅ בחגורה חומה "שחרורים" נשאר נושא רגיל,
    // כי שם יש רק תרגיל אחד והוא לא אמור להיות נעול/תת־נושאים.
    val topicTitles: List<String> = remember(belt, rawTopicTitles, detailsByTitle) {

        fun hasRealSubTopics(title: String): Boolean {
            val topicTrim = title.trim()

            return detailsByTitle[title]
                ?.subTitles
                .orEmpty()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { it != topicTrim }
                .distinct()
                .any()
        }

        fun priorityRank(title: String): Int {
            val clean = title.trim()

            return when {
                // ✅ בכל החגורות:
                // הגנות תמיד ראשון
                clean.contains("הגנות") -> 0

                // ✅ שחרורים תמיד שני
                clean.contains("שחרורים") -> 1

                // ✅ בצהובה עבודת ידיים מיד אחרי הגנות ושחרורים
                belt == Belt.YELLOW &&
                        clean.contains("עבודת ידיים") -> 2

                // ✅ שאר הנושאים עם תתי־נושאים
                hasRealSubTopics(title) -> 3

                // ✅ שאר הנושאים הרגילים
                else -> 10
            }
        }

        val sorted = rawTopicTitles
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<String>> { priorityRank(it.value) }
                    .thenBy { it.index }
            )
            .map { it.value }

        sorted
    }

    var expandedTopic by rememberSaveable(belt.id) { mutableStateOf<String?>(null) }
    val rowMinHeight = 54.dp

    // ✅ מגדיל את גובה כרטיסיית הנושאים,
    // כדי שתרד נמוך יותר לכיוון העיגול המרכזי.
    val visibleRows = 6
    val listHeight = rowMinHeight * visibleRows + 10.dp

    val fabSize = 120.dp

    // ✅ במקום להשאיר חצי עיגול רווח,
    // משאירים רק בערך שליש גובה כדי שהכרטיס יירד יותר למטה.
    val desiredOverlap = fabSize * 0.34f
    val fabClearance = desiredOverlap

    Surface(
        tonalElevation = if (isDarkTheme) 0.dp else 1.dp,
        shadowElevation = if (isDarkTheme) 0.dp else 6.dp,
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) Color.White.copy(alpha = 0.12f) else belt.color.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .padding(top = 0.dp)
            .padding(bottom = fabClearance + 2.dp)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                text = if (isEnglish) "Topics in Belt" else "נושאים בחגורה",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                textAlign = TextAlign.Center,
                color = titleColor,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            if (topicTitles.isEmpty()) {
                Text(
                    text = if (isEnglish) "No topics to display" else "אין נושאים להצגה",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center,
                    color = rowSubColor
                )
            } else {
                val topicsScroll = rememberScrollState(0)

                // ✅ חשוב:
                // כשעוברים חגורה, ScrollState עלול להישאר באמצע הרשימה מהחגורה הקודמת.
                // לכן מאפסים לראש הרשימה, כדי שבאמת נראה את הנושאים עם תתי־נושאים למעלה.
                LaunchedEffect(belt.id, topicTitles) {
                    topicsScroll.scrollTo(0)
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = listHeight)
                        .verticalScroll(topicsScroll)
                ) {
                    topicTitles.forEachIndexed { index, title ->

                        val details = detailsByTitle[title]
                            ?: TopicDetails(itemCount = 0, subTitles = emptyList())

                        val displayTitle = topicTitleForUi(title, lang)

                        val subTitles: List<String> = details.subTitles
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .filter { it != title.trim() }
                            .distinct()
                            .toList()

                        val itemCount = exerciseCountByTitle[title]
                            ?: topicExercisesCountForCurrentBelt(
                                belt = belt,
                                topicTitle = title
                            )

                        val subCount = subTitles.size
                        val hasSubs = subCount > 0

                        val countsLine = if (isEnglish) {
                            if (subCount > 0) {
                                "$subCount sub-topics · $itemCount exercises"
                            } else {
                                "$itemCount exercises"
                            }
                        } else {
                            if (subCount > 0) {
                                "\u200F$subCount\u00A0תתי נושאים · $itemCount\u00A0תרגילים\u200F"
                            } else {
                                "$itemCount תרגילים"
                            }
                        }

                        val isCombinedCountText =
                            subCount > 0

                        val topicTitleFontSize = 10.8.sp
                        val topicTitleLineHeight = 12.8.sp

                        val topicCountFontSize = 7.8.sp
                        val topicCountLineHeight = 9.2.sp

                        val isExpanded = expandedTopic == title
                        val isDefenseTopic = title.trim().contains("הגנות")

                        // ✅ שחרורים נשאר תוכן פרימיום גם אם בחגורה חומה
                        // הוא מופיע כנושא רגיל עם תרגיל אחד וללא תתי־נושאים.
                        val isBrownSingleReleaseTopic = false

                        val floatingTitleColor = rowTitleColor
                        val floatingSubColor = rowSubColor
                        val floatingAccent = Brush.verticalGradient(
                            colors = listOf(
                                belt.color.copy(alpha = 1f),
                                belt.color.copy(alpha = if (isDarkTheme) 1f else 0.95f)
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = rowMinHeight)
                                .padding(horizontal = 10.dp, vertical = 1.dp)
                                .clickable {
                                    clickSound()
                                    haptic(true)

                                    val canOpen =
                                        LockedContentPolicy.canOpenTopic(accessMode, title)

                                    if (!canOpen) {
                                        onOpenSubscription()
                                    } else if (hasSubs) {
                                        expandedTopic = if (isExpanded) null else title
                                    } else {
                                        if (isDefenseTopic) {
                                            onOpenDefenseMenu(belt, title)
                                        } else {
                                            onOpenTopic(belt, title)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                horizontalAlignment = horizontalByLang
                            ) {
                                val parentLocked =
                                    LockedContentPolicy.shouldShowLock(accessMode, title)

                                val topicImageRes = beltTopicImageFor(belt, title)

                                CompositionLocalProvider(
                                    androidx.compose.ui.platform.LocalLayoutDirection provides layoutByLang
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(floatingAccent)
                                        )

                                        Spacer(Modifier.width(5.dp))

                                        if (topicImageRes != null) {
                                            Box(
                                                modifier = Modifier
                                                    .width(38.dp)
                                                    .height(31.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = topicImageRes),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Spacer(Modifier.width(6.dp))
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = horizontalByLang
                                        ) {
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontSize = topicTitleFontSize,
                                                    lineHeight = topicTitleLineHeight
                                                ),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = floatingTitleColor,
                                                textAlign = titleTextAlignByLang,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(1.dp))

                                            Text(
                                                text = countsLine,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = topicCountFontSize,
                                                    lineHeight = topicCountLineHeight
                                                ),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = floatingSubColor,
                                                textAlign = titleTextAlignByLang,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (parentLocked) {
                                            Spacer(Modifier.width(5.dp))

                                            PremiumPulsingLockBadge(
                                                modifier = Modifier.size(16.dp),
                                                isDarkTheme = isDarkTheme
                                            )
                                        }

                                        if (hasSubs) {
                                            Spacer(Modifier.width(5.dp))

                                            Icon(
                                                imageVector = if (isExpanded) {
                                                    Icons.Filled.KeyboardArrowUp
                                                } else {
                                                    Icons.Filled.KeyboardArrowDown
                                                },
                                                contentDescription = null,
                                                tint = belt.color.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (parentLocked) {
                                            // ✅ כאשר נושא נעול בלי תתי־נושאים, למשל שחרורים בחגורה חומה,
                                            // שומרים מקום של חץ כדי שהמנעול יהיה מיושר מתחת למנעול של הגנות.
                                            Spacer(Modifier.width(5.dp))

                                            Box(
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (hasSubs && isExpanded) {
                                    Spacer(Modifier.height(4.dp))

                                    val parentLockedForSubTopics =
                                        LockedContentPolicy.shouldShowLock(accessMode, title)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 2.dp,
                                                bottom = 6.dp
                                            ),
                                        shape = RoundedCornerShape(18.dp),
                                        color = subTopicsCardBg,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = subTopicsCardBorder
                                        ),
                                        shadowElevation = 0.dp,
                                        tonalElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(0.dp),
                                            horizontalAlignment = horizontalByLang
                                        ) {
                                            subTitles.forEachIndexed { subIndex, sub ->
                                                val displaySub = topicTitleForUi(sub, lang)

                                                val subTopicStatsLine =
                                                    remember(belt, title, sub, lang) {
                                                        SharedContentRepo.getSubTopicsFor(
                                                            belt = belt,
                                                            topicTitle = title
                                                        )
                                                            .firstOrNull { it.title.trim() == sub.trim() }
                                                            ?.let { subTopicStatsLineForUi(it, lang) }
                                                            .orEmpty()
                                                    }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            clickSound()
                                                            haptic(true)

                                                            val canOpenSubTopic =
                                                                accessMode == AccessMode.OPEN ||
                                                                        LockedContentPolicy.canOpenTopic(
                                                                            accessMode,
                                                                            title
                                                                        )

                                                            if (!canOpenSubTopic) {
                                                                onOpenSubscription()
                                                            } else {
                                                                onOpenSubTopic(belt, title, sub)
                                                            }
                                                        }
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 5.dp
                                                        )
                                                        .heightIn(min = 48.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ChevronLeft,
                                                        contentDescription = null,
                                                        tint = belt.color.copy(alpha = 1f),
                                                        modifier = Modifier.size(15.dp)
                                                    )

                                                    Spacer(Modifier.width(6.dp))

                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = horizontalByLang
                                                    ) {
                                                        Text(
                                                            text = displaySub,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = titleTextAlignByLang,
                                                            color = rowTitleColor,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = 10.2.sp,
                                                                lineHeight = 11.8.sp
                                                            ),
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        if (subTopicStatsLine.isNotBlank()) {
                                                            Spacer(Modifier.height(1.dp))

                                                            Text(
                                                                text = subTopicStatsLine,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = titleTextAlignByLang,
                                                                color = belt.color.copy(alpha = 1f),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 8.6.sp,
                                                                    lineHeight = 10.sp
                                                                ),
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    if (parentLockedForSubTopics) {
                                                        Spacer(Modifier.width(8.dp))
                                                        PremiumPulsingLockBadge(
                                                            modifier = Modifier.size(16.dp),
                                                            isDarkTheme = isDarkTheme
                                                        )
                                                    }
                                                }

                                                if (subIndex != subTitles.lastIndex) {
                                                    HorizontalDivider(
                                                        color = subDividerColor,
                                                        thickness = 0.8.dp,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text = if (isEnglish) "Close topic" else "סגור נושא",
                                                modifier = Modifier
                                                    .align(horizontalByLang)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)
                                                        expandedTopic = null
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = belt.color.copy(alpha = 1f),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    lineHeight = 13.sp
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(Modifier.height(2.dp))

                                            Text(
                                                text = if (isEnglish) "Open full topic" else "פתח את כל הנושא",
                                                modifier = Modifier
                                                    .align(horizontalByLang)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)

                                                        if (isDefenseTopic) {
                                                            onOpenDefenseMenu(belt, title)
                                                        } else {
                                                            onOpenTopic(belt, title)
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = belt.color.copy(alpha = 1f),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    lineHeight = 13.sp
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (index != topicTitles.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = if (isDarkTheme) {
                                    Color.White.copy(alpha = 0.12f)
                                } else {
                                    Color(0x22000000)
                                },
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }

                    // ✅ רווח גלילה תחתון:
                    // מונע מצב שהסרגל הצף / הקרוסלה מסתירים את הנושא האחרון.
                    Spacer(Modifier.height(56.dp))
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
    inputEnabled: Boolean = true,
    reverseSwipeDirection: Boolean = false
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
                // ✅ חשוב:
                // absoluteOffset לא מושפע מ־RTL/LTR.
                // כך עיגול שנמצא פיזית מימין יישאר באמת מימין,
                // ועיגול שנמצא פיזית משמאל יישאר באמת משמאל.
                .absoluteOffset(x = xDp, y = yDrop)
                .size(size)
                .alpha(alpha)
                .zIndex(if (isCenter) 2f else 1f)

            val gestures = Modifier
                .pointerInput(belts, index, reverseSwipeDirection) {
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
                        val direction = if (reverseSwipeDirection) -1f else 1f
                        val rawDelta = (drag / stepPx) * direction

                        val next = (center.value + rawDelta)
                            .coerceIn(0f, belts.lastIndex.toFloat())

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
                            val ctx = LocalContext.current
                            val lang = remember { AppLanguageManager(ctx) }.getCurrentLanguage()
                            if (isCenter) {
                                val clean = remember(belt, lang) {
                                    beltShortNameForUi(belt, lang)
                                }
                                Text(
                                    text = if (lang == AppLanguage.ENGLISH) {
                                        "Belt\n$clean"
                                    } else {
                                        "חגורה\n$clean"
                                    },
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
            size = androidx.compose.ui.geometry.Size(
                size.width - inset * 2,
                size.height - inset * 2
            ),
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
                size = androidx.compose.ui.geometry.Size(
                    size.width - inset * 2,
                    size.height - inset * 2
                ),
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}