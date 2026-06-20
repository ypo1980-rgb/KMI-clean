package il.kmi.app.screens.BeltQuestions

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import il.kmi.app.R
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.ExerciseCountsRegistry
import il.kmi.app.domain.ExerciseCountProvider
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.localization.rememberIsEnglish
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
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import il.kmi.app.domain.color
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.subscription.AccessMode
import il.kmi.app.subscription.AccessModeResolver
import il.kmi.app.subscription.LockedContentPolicy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

//==================================================================

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

        "שחרור מחביקות",
        "חביקות גוף",
        "חביקות צואר",
        "חביקות זרוע" -> "releases_hugs"

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
    val clean = title
        .trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")

    if (!isEnglish) return clean

    val translated = ExerciseTitlesEn.getOrSame(clean).trim()
    if (translated.isNotBlank() && translated != clean) {
        return translated
    }

    return when (clean) {
        "מכות יד" -> "Hand Strikes"
        "מכות מרפק" -> "Elbow Strikes"
        "מכות במקל / רובה" -> "Stick / Rifle Strikes"

        "שחרור מתפיסות ידיים / שיער / חולצה" ->
            "Releases from Hand / Hair / Shirt Grabs"

        "שחרור מחניקות" ->
            "Choke Releases"

        "שחרור מחביקות" ->
            "Hug Releases"

        "חביקות גוף" ->
            "Body Hugs"

        "חביקות צואר" ->
            "Neck Hugs"

        "חביקות זרוע" ->
            "Arm Hugs"

        "הגנות עם רובה נגד דקירות סכין" ->
            "Rifle Defenses Against Knife Stabs"

        "הגנות נגד מספר תוקפים" ->
            "Multiple Attackers Defense"

        else -> translated.ifBlank { clean }
    }
}

private fun subjectTitleForUi(
    subjectId: String,
    fallbackHeb: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return fallbackHeb

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

private fun isDefenseRootSubjectId(subjectId: String): Boolean {
    return when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses",
        "הגנות" -> true

        else -> false
    }
}

private fun isPremiumRootSubject(subjectId: String): Boolean {
    return when {
        subjectId.trim().lowercase() == "releases" -> true
        isDefenseRootSubjectId(subjectId) -> true
        else -> false
    }
}

private fun defenseCombinedSectionIdFor(kind: il.kmi.app.domain.DefenseKind): String? {
    val clean = kind.name.lowercase()

    return when {
        clean.contains("internal") || clean.contains("inside") -> "def_internal"
        clean.contains("external") || clean.contains("outside") -> "def_external"
        else -> null
    }
}

private fun withLockSuffix(text: String, locked: Boolean): String {
    return if (locked) "$text 🔒" else text
}

private fun stripLockSuffix(text: String): String {
    return text.removeSuffix(" 🔒").trim()
}

@Composable
private fun PremiumTopicLockIcon(
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "subjectTopicLockPulse")

    val scale by pulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subjectTopicLockScale"
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

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

private fun beltTitleForUi(belt: Belt, isEnglish: Boolean): String =
    if (isEnglish) belt.en else belt.heb

private fun hardSubjectLoadingTitle(
    subjectId: String,
    isEnglish: Boolean
): String {
    return if (isEnglish) {
        when (subjectId.trim()) {
            "def_internal" -> "Internal defenses"
            "def_external" -> "External defenses"
            "releases_hugs" -> "Hug releases"
            "kicks_hard" -> "Defenses against kicks"
            else -> "Loading exercises"
        }
    } else {
        when (subjectId.trim()) {
            "def_internal" -> "הגנות פנימיות"
            "def_external" -> "הגנות חיצוניות"
            "releases_hugs" -> "שחרור מחביקות"
            "kicks_hard" -> "הגנות נגד בעיטות"
            else -> "טוען תרגילים"
        }
    }
}

@Composable
private fun HardSubjectLoadingScreen(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFF37B7E8).copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1F78B4),
                    modifier = Modifier.size(34.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = title,
                    color = Color(0xFF102033),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "טוען תרגילים...",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun rememberEnsureContentRepoInitialized() {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                ContentRepo.initIfNeeded()
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
    onOpenSubscription: () -> Unit,
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
    // הערות תרגילים מנוהלות עכשיו בדיאלוג הגלובלי החדש דרך KmiTopBar

    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
    }

    val legacySp = remember(ctx) {
        ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE)
    }

    var accessRefreshTick by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ מרענן גישה מיד כשחוזרים ממסך רכישה / מנוי
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessRefreshTick++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // מרענן מצב גישה גם בלי שינוי ב-SharedPreferences.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            accessRefreshTick++
        }
    }

    DisposableEffect(userSp, subsSp, legacySp) {
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
        legacySp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
            legacySp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val hasManagerAccess = remember(accessRefreshTick) {
        KmiAccess.hasFullAccess(userSp) ||
                KmiAccess.hasFullAccess(subsSp) ||
                KmiAccess.hasFullAccess(legacySp)
    }

    val accessMode = AccessModeResolver.resolve(
        hasManagerAccess = hasManagerAccess
    )

    val hasAccess = accessMode == AccessMode.OPEN

    var pendingHardSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var localHardSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var localHardSectionId by rememberSaveable { mutableStateOf<String?>(null) }

    pendingHardSubjectId?.let { pendingSubjectId ->
        HardSubjectLoadingScreen(
            title = hardSubjectLoadingTitle(pendingSubjectId, isEnglish)
        )

        LaunchedEffect(pendingSubjectId) {
            kotlinx.coroutines.yield()
            localHardSectionId = null
            localHardSubjectId = pendingSubjectId
            pendingHardSubjectId = null
        }

        return
    }

    localHardSubjectId?.let { subjectId ->
        il.kmi.app.screens.UnifiedSubjectExercisesScreen(
            subjectId = subjectId,
            sectionId = localHardSectionId,
            onOpenSection = { nextSubjectId, nextSectionId ->
                localHardSubjectId = nextSubjectId
                localHardSectionId = nextSectionId
            },
            onBack = {
                if (localHardSectionId != null) {
                    localHardSectionId = null
                } else {
                    localHardSubjectId = null
                }
            }
        )
        return
    }

    // State לניהול התפריט המהיר
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }
    var effectiveBelt by rememberSaveable { mutableStateOf(Belt.GREEN) }
    // החיפוש והסברי התרגילים עוברים דרך KmiTopBar + ExercisePremiumSearchDialog

    if (showPracticeMenu) {
        PracticeMenuDialog(
            canUseExtras = hasAccess,
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
                title = if (isEnglish) "Exercises by Topic" else "תרגילים לפי נושא",
                onHome = { },
                lockHome = false,
                showTopHome = false,
                showTopBeltIcon = false,
                topBeltIconRes = null,
                // החיפוש הגלובלי נפתח ומטופל פנימית בתוך KmiTopBar
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
                )
        ) {

            // 1. תוכן המסך (נגלל)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopicsBySubjectCard(
                    currentBelt = effectiveBelt,
                    accessMode = accessMode,
                    hasAccess = hasAccess,
                    onOpenSubscription = {
                        onOpenSubscription()
                    },
                    onSubjectClick = { belt, subject ->
                        effectiveBelt = belt
                        onOpenSubject(belt, subject)
                    },
                    onOpenTopic = onOpenTopic,
                    onOpenTopicWithSub = onOpenTopicWithSub,
                    onOpenDefenseList = onOpenDefenseList,
                    onOpenHardSubjectRoute = { belt, subjectId ->
                        effectiveBelt = belt

                        val cleanSubjectId = subjectId.trim()

                        when (cleanSubjectId) {
                            "def_internal",
                            "def_external",
                            "releases_hugs",
                            "kicks_hard" -> {
                                pendingHardSubjectId = cleanSubjectId
                                localHardSubjectId = null
                                localHardSectionId = null
                            }

                            else -> {
                                onOpenHardSubjectRoute(belt, cleanSubjectId)
                            }
                        }
                    },
                    onOpenKicksHardLocal = {
                        effectiveBelt = Belt.GREEN
                        pendingHardSubjectId = "kicks_hard"
                        localHardSubjectId = null
                        localHardSectionId = null
                    },
                    onQuickViewClick = {
                        quickMenuExpanded = true
                    }
                )

                // ✅ אין יותר כפתור תחתון של "מבט מהיר".
                // התפריט המהיר נפתח עכשיו מהמלבן הצדדי כמו במסך לפי חגורה.
                Spacer(modifier = Modifier.height(14.dp))
            }

            FloatingQuickMenu(
                belt = effectiveBelt,
                modifier = Modifier
                    // ✅ אותו מלבן צדדי כמו במסך תרגילים לפי חגורה
                    .align(Alignment.CenterStart)
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                triggerMode = QuickMenuTriggerMode.SideRail,
                includePractice = true,
                hasFullAccess = hasAccess,
                onLockedItemClick = { onOpenSubscription() },
                onWeakPoints = { onOpenWeakPoints(effectiveBelt) },
                onAllLists = { onOpenAllLists(effectiveBelt) },
                onPractice = { showPracticeMenu = true },
                onSummary = { onOpenSummaryScreen(effectiveBelt) },
                onVoice = { onOpenVoiceAssistant(effectiveBelt) },
                onPdf = { onOpenPdfMaterials(effectiveBelt) }
            )

            // החץ הוסר: כרטיס הנושאים נפרס לגובה מלא כדי להציג את כל הנושאים
        }

        // אין כאן יותר דיאלוג חיפוש/הסבר מקומי.
        // כל החיפוש, ההסבר, המועדפים והערות המשתמש מטופלים דרך KmiTopBar.
    }
}

private fun subjectAccentColor(subjectId: String): Color =
    when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses" -> Color(0xFF7C5CFF)          // violet premium

        "releases" -> Color(0xFF19A7E0)          // aqua premium

        "hands_root",
        "hands_all" -> Color(0xFF16B39A)         // emerald premium

        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> Color(0xFFB88746) // bronze premium

        "topic_ready_stance" -> Color(0xFF59B96F) // green premium

        "topic_ground_prep" -> Color(0xFFD05AA0) // pink premium

        "topic_kavaler",
        "kavaler" -> Color(0xFFF09A2A)           // amber premium

        "kicks",
        "topic_kicks" -> Color(0xFF3B82F6)       // blue premium

        "releases_hugs" -> Color(0xFF5B6CFF)

        else -> Color(0xFF7C5CFF)
    }

private fun subjectPremiumBackgroundColors(subjectId: String): List<Color> =
    when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses" -> listOf(
            Color(0xFFF3EEFF),
            Color(0xFFE9E0FF)
        )

        "releases" -> listOf(
            Color(0xFFEAF7FF),
            Color(0xFFDCEFFF)
        )

        "hands_root",
        "hands_all" -> listOf(
            Color(0xFFEAF9F5),
            Color(0xFFDDF4ED)
        )

        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> listOf(
            Color(0xFFF8F1E7),
            Color(0xFFF0E4D2)
        )

        "topic_ready_stance" -> listOf(
            Color(0xFFEEF9EE),
            Color(0xFFE1F3E2)
        )

        "topic_ground_prep" -> listOf(
            Color(0xFFF9EDF4),
            Color(0xFFF2DFEA)
        )

        "topic_kavaler",
        "kavaler" -> listOf(
            Color(0xFFFFF3E5),
            Color(0xFFFFE6C8)
        )

        "kicks",
        "topic_kicks" -> listOf(
            Color(0xFFEEF5FF),
            Color(0xFFDDEBFF)
        )

        "releases_hugs" -> listOf(
            Color(0xFFEEF0FF),
            Color(0xFFE2E6FF)
        )

        else -> listOf(
            Color(0xFFF4F0FF),
            Color(0xFFE9E3FF)
        )
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

private fun subjectImageFor(subjectId: String): Int? =
    when (subjectId.trim().lowercase()) {
        // הגנות
        "defense_root",
        "defenses_root",
        "defenses" -> R.drawable.topic_defenses

        // שחרורים / חביקות גוף
        "releases",
        "releases_hugs",
        "releases_hugs_body" -> R.drawable.topic_body_hug_releases

        // עבודות ידיים
        "hands_root",
        "hands_all",
        "hands_strikes" -> R.drawable.topic_hand_strikes

        // בלימות וגלגולים
        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> R.drawable.topic_forward_roll

        // עמידת מוצא
        "topic_ready_stance" -> R.drawable.topic_ready_stance

        // עבודת קרקע
        "topic_ground_prep" -> R.drawable.topic_ground_fighting

        // קוואלר
        "topic_kavaler",
        "kavaler" -> R.drawable.topic_kavaler

        // בעיטות
        "kicks",
        "topic_kicks" -> R.drawable.topic_kicks

        else -> null
    }

@Composable
private fun InlineSubTopicsExpansionCard(
    picks: List<String>,
    counts: Map<String, Int>,
    isEnglish: Boolean,
    accent: Color,
    onPick: (String) -> Unit
) {
    fun countLabel(n: Int): String =
        if (isEnglish) "exercises $n" else "$n תרגילים"

    fun countForDisplay(pick: String): Int {
        counts[pick]?.let { return it }

        val clean = stripLockSuffix(pick).trim()

        return counts[clean]
            ?: when (clean) {
                // שחרורים
                "Release from Hand / Hair / Shirt Grabs" ->
                    counts["שחרור מתפיסות ידיים / שיער / חולצה"] ?: 0

                "Choke Releases" ->
                    counts["שחרור מחניקות"] ?: 0

                "Bear Hug Releases",
                "Hug Releases" ->
                    counts["שחרור מחביקות"] ?: 0

                // עבודת ידיים
                "Hand Strikes" ->
                    counts["מכות יד"] ?: 0

                "Elbow Strikes" ->
                    counts["מכות מרפק"] ?: 0

                "Stick / Rifle Strikes" ->
                    counts["מכות במקל / רובה"] ?: 0

                else -> 0
            }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.20f),
                            accent.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            picks.forEachIndexed { index, pick ->
                val cleanTitle = stripLockSuffix(pick)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(pick) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        Text(
                            text = cleanTitle,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(1.dp))

                        Text(
                            text = countLabel(countForDisplay(pick)),
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (index != picks.lastIndex) {
                    HorizontalDivider(
                        color = accent.copy(alpha = 0.22f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectRootCardPremium(
    title: String,
    subtitle: String = "",
    subjectId: String,
    countText: String,
    showLeftBadge: Boolean = false,
    isDarkMode: Boolean = false,
    showExpandArrow: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    val isEnglish = rememberIsEnglish()
    val accent = remember(subjectId) { subjectAccentColor(subjectId) }
    val icon = remember(subjectId) { subjectIconFor(subjectId) }
    val imageRes = remember(subjectId) { subjectImageFor(subjectId) }
    val bgColors = remember(subjectId) { subjectPremiumBackgroundColors(subjectId) }

    val titleColor = if (isDarkMode) Color.White else Color(0xFF18212F)
    val subtitleColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.80f)
    } else {
        Color(0xFF5F6C7B)
    }
    val countColor = if (isDarkMode) Color.White.copy(alpha = 0.92f) else Color(0xFF2F3B4A)

    val isTitleLocked = title.endsWith(" 🔒")
    val displayTitle = stripLockSuffix(title)

    val isRollsBreakfallsCard = subjectId.trim().lowercase() == "rolls_breakfalls" ||
            subjectId.trim().lowercase() == "topic_breakfalls_rolls"

    val titleFontSize = if (isRollsBreakfallsCard) 9.8.sp else 10.8.sp
    val titleLineHeight = if (isRollsBreakfallsCard) 11.4.sp else 12.8.sp
    val titleMaxLines = if (isRollsBreakfallsCard) 1 else 2

    val isCombinedCountText =
        countText.contains("תתי נושאים") ||
                countText.contains("sub-topics", ignoreCase = true)

    val countFontSize = if (isCombinedCountText) 8.6.sp else 10.sp
    val countLineHeight = if (isCombinedCountText) 10.sp else 12.sp

    @Composable
    fun SubjectVisual() {
        if (imageRes != null) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(31.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(bgColors),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(
                                    when {
                                        imageRes != null -> 44.dp
                                        showLeftBadge -> 34.dp
                                        else -> 26.dp
                                    }
                                )
                                .background(
                                    color = accent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(10.dp))
                        SubjectVisual()
                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = titleFontSize,
                                        lineHeight = titleLineHeight
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Start,
                                    color = titleColor,
                                    maxLines = titleMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isTitleLocked) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    PremiumTopicLockIcon(
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                if (showExpandArrow) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isExpanded) "⌃" else "⌄",
                                        color = accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            if (subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    color = subtitleColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = countFontSize,
                                    lineHeight = countLineHeight
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Start,
                                color = countColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.width(34.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showExpandArrow) {
                                    Text(
                                        text = if (isExpanded) "⌃" else "⌄",
                                        color = accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTitleLocked) {
                                    PremiumTopicLockIcon(
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = titleFontSize,
                                    lineHeight = titleLineHeight
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Right,
                                color = titleColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Right,
                                    color = subtitleColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = countFontSize,
                                    lineHeight = countLineHeight
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Right,
                                color = countColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))
                        SubjectVisual()
                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(
                                    when {
                                        imageRes != null -> 44.dp
                                        showLeftBadge -> 34.dp
                                        else -> 26.dp
                                    }
                                )
                                .background(
                                    color = accent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
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
    accessMode: AccessMode,
    hasAccess: Boolean = true,
    onOpenSubscription: () -> Unit,
    onSubjectClick: (Belt, SubjectTopic) -> Unit = { _, _ -> },
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit,
    onOpenKicksHardLocal: () -> Unit,
    onQuickViewClick: () -> Unit = {}
) {

    val isEnglish = rememberIsEnglish()

    // בודקים את ה-Theme של האפליקציה בפועל, לא את מצב המכשיר.
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
    var expandedSubTopicsForId by rememberSaveable { mutableStateOf<String?>(null) }

    var subTopicsPickCountsBySubjectId by remember(subjects) {
        mutableStateOf<Map<String, Map<String, Int>>>(emptyMap())
    }

    var askDefense by rememberSaveable { mutableStateOf(false) }
    var askKind by rememberSaveable { mutableStateOf<il.kmi.app.domain.DefenseKind?>(null) }

    fun applyPayload(payload: SubjectTopicsUiLogic.TopicsUiCountsPayload) {
        subjectCounts = payload.subjectCounts
        handsRootCount = payload.handsRootCount
        handsPickCounts = payload.handsPickCounts
        uiSectionCounts = payload.uiSectionCounts
        subTopicsPickCountsBySubjectId = payload.subTopicsPickCountsBySubjectId
        countsReady = true
    }

    LaunchedEffect(subjects, handsBase, currentBelt) {
        // ✅ אם כבר יש cache בזיכרון לחגורה הנוכחית — משתמשים בו מיידית
        SubjectTopicsUiLogic.getCachedTopicsUiCountsPayload(currentBelt)?.let { cached ->
            applyPayload(cached)
            return@LaunchedEffect
        }

        val payload = withContext(Dispatchers.Default) {
            SubjectTopicsUiLogic.buildTopicsUiCountsPayload(
                subjects = subjects,
                handsBase = handsBase,
                currentBelt = currentBelt
            )
        }

        SubjectTopicsUiLogic.cacheTopicsUiCountsPayload(
            currentBelt = currentBelt,
            value = payload
        )

        applyPayload(payload)
    }

    // ----------------- UI -----------------

    fun formatCount(n: Int): String =
        if (isEnglish) "exercises $n" else "$n תרגילים"

    fun formatSubTopicsAndExercises(subTopics: Int, exercises: Int): String =
        if (isEnglish) {
            "$subTopics sub-topics · $exercises exercises"
        } else {
            "\u200F$subTopics\u00A0תתי נושאים · $exercises\u00A0תרגילים\u200F"
        }

    fun translateCardCountText(raw: String): String {
        if (!isEnglish) return raw

        return raw
            .replace(Regex("""(\d+)\s+תתי\s+נושאים"""), "sub-topics $1")
            .replace(Regex("""(\d+)\s+תרגילים"""), "exercises $1")
    }

    fun countTextFromSubTopicTotalsOrFallback(
        counts: Map<String, Int>,
        fallback: String,
        subTopicsCount: Int
    ): String {
        val totalExercises = counts
            .filterKeys { it.trim().isNotBlank() }
            .filterValues { it > 0 }
            .values
            .sum()

        if (totalExercises > 0) {
            return if (subTopicsCount > 0) {
                formatSubTopicsAndExercises(subTopicsCount, totalExercises)
            } else {
                formatCount(totalExercises)
            }
        }

        val fallbackExerciseCount = Regex("""\d+""")
            .findAll(fallback)
            .mapNotNull { it.value.toIntOrNull() }
            .lastOrNull()
            ?: 0

        return if (subTopicsCount > 0 && fallbackExerciseCount > 0) {
            formatSubTopicsAndExercises(subTopicsCount, fallbackExerciseCount)
        } else {
            formatCount(fallbackExerciseCount)
        }
    }

    fun normalizeCountPart(value: String): String =
        value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun subjectTopicCandidatesForCount(
        subjectId: String,
        title: String
    ): List<String> {
        val cleanTitle = normalizeCountPart(stripLockSuffix(title))

        return when (subjectId.trim().lowercase()) {
            "rolls_breakfalls",
            "topic_breakfalls_rolls" -> listOf(
                "בלימות וגלגולים",
                "גלגולים ובלימות",
                "topic_breakfalls_rolls"
            )

            "topic_ready_stance" -> listOf(
                "עמידת מוצא",
                "topic_ready_stance"
            )

            "topic_ground_prep" -> listOf(
                "עבודת קרקע",
                "topic_ground_prep"
            )

            "topic_kavaler",
            "kavaler" -> listOf(
                "קוואלר",
                "topic_kavaler",
                "kavaler"
            )

            "kicks",
            "topic_kicks" -> listOf(
                "בעיטות",
                "topic_kicks",
                "kicks"
            )

            else -> listOf(cleanTitle, subjectId)
        }
            .map { normalizeCountPart(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun fallbackLastNumberFromText(text: String): Int {
        return Regex("""\d+""")
            .findAll(text)
            .mapNotNull { it.value.toIntOrNull() }
            .lastOrNull()
            ?: 0
    }

    fun countTextForSubjectCard(
        card: SubjectTopicsUiLogic.SubjectCardModel
    ): String {
        val globalTopicTitle = when (card.id.trim().lowercase()) {
            "rolls_breakfalls",
            "topic_breakfalls_rolls" -> "בלימות וגלגולים"

            "topic_ready_stance" -> "עמידת מוצא"

            "topic_ground_prep" -> "עבודת קרקע"

            "topic_kavaler",
            "kavaler" -> "קוואלר"

            "kicks",
            "topic_kicks" -> "בעיטות"

            else -> null
        }

        if (globalTopicTitle != null) {
            val stats = ExerciseCountProvider.topicStats(
                belt = currentBelt,
                topicTitle = globalTopicTitle
            )

            if (stats.exerciseCount > 0) {
                return formatCount(stats.exerciseCount)
            }
        }

        val candidates = subjectTopicCandidatesForCount(
            subjectId = card.id,
            title = card.title
        )

        val subTitles = candidates
            .flatMap { candidate ->
                runCatching {
                    ContentRepo.listSubTopicTitles(currentBelt, candidate)
                }.getOrDefault(emptyList())
            }
            .map { normalizeCountPart(it) }
            .filter { it.isNotBlank() }
            .filter { subTitle ->
                candidates.none { candidate ->
                    normalizeCountPart(candidate).equals(
                        normalizeCountPart(subTitle),
                        ignoreCase = true
                    )
                }
            }
            .distinct()

        val directItems = candidates
            .flatMap { candidate ->
                runCatching {
                    ContentRepo.listItemTitles(
                        belt = currentBelt,
                        topicTitle = candidate,
                        subTopicTitle = null
                    )
                }.getOrDefault(emptyList())
            }

        val subItems = candidates
            .flatMap { candidate ->
                subTitles.flatMap { subTitle ->
                    runCatching {
                        ContentRepo.listItemTitles(
                            belt = currentBelt,
                            topicTitle = candidate,
                            subTopicTitle = subTitle
                        )
                    }.getOrDefault(emptyList())
                }
            }

        val realItemCount = (directItems + subItems)
            .map { ExerciseTitleFormatter.displayName(it).ifBlank { it } }
            .map { normalizeCountPart(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .size

        val cachedSubTopicCounts = subTopicsPickCountsBySubjectId[card.id].orEmpty()

        val finalSubTopicCount = maxOf(
            subTitles.size,
            cachedSubTopicCounts.size
        )

        val finalItemCount = when {
            realItemCount > 0 -> realItemCount
            cachedSubTopicCounts.values.sum() > 0 -> cachedSubTopicCounts.values.sum()
            else -> fallbackLastNumberFromText(card.countText)
        }

        if (finalItemCount <= 0) {
            return translateCardCountText(card.countText)
        }

        return if (finalSubTopicCount > 0) {
            formatSubTopicsAndExercises(finalSubTopicCount, finalItemCount)
        } else {
            formatCount(finalItemCount)
        }
    }

    fun openSubjectSmart(subject: SubjectTopic) {
        val action = SubjectTopicsUiLogic.buildOpenSubjectUiAction(
            subject = subject,
            currentBelt = currentBelt
        )

        onSubjectClick(action.chosenBelt, subject)
    }

    fun openPickedSubTopic(
        id: String,
        pickedDisplay: String
    ) {
        val dialogData = SubjectTopicsUiLogic.buildSubTopicsDialogData(
            subjects = subjects,
            id = id
        )

        val pickedDisplayClean = stripLockSuffix(pickedDisplay)

        val picked = dialogData.picks.firstOrNull {
            subTopicTitleForUi(it, isEnglish) == pickedDisplayClean
        } ?: pickedDisplayClean

        val isLockedPick =
            accessMode != AccessMode.OPEN &&
                    LockedContentPolicy.shouldShowLock(
                        accessMode,
                        dialogData.base?.titleHeb.orEmpty()
                    )

        if (isLockedPick) {
            onOpenSubscription()
            return
        }

        val decision = SubjectTopicsUiLogic.resolveSubTopicPick(
            base = dialogData.base,
            bodyHugsChild = dialogData.bodyHugsChild,
            picked = picked,
            norm = ::normText
        )

        when (decision) {
            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenTopicWithSub -> {
                onOpenTopicWithSub(
                    currentBelt,
                    decision.topic,
                    decision.subTopic
                )
            }

            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenSubject -> {
                val hardSubjectId = when (decision.subject.id) {
                    "releases",
                    "releases_hugs" -> releasesSectionIdFor(picked) ?: "releases_hugs"

                    else -> null
                }

                if (hardSubjectId != null) {
                    onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                } else {
                    openSubjectSmart(decision.subject)
                }
            }

            SubjectTopicsUiLogic.SubTopicPickDecision.None -> Unit
        }
    }

    fun isReleasesRootCard(card: SubjectTopicsUiLogic.SubjectCardModel): Boolean {
        val cleanId = card.id.trim().lowercase()
        val cleanTitle = normText(stripLockSuffix(card.title))

        return cleanId == "releases" ||
                cleanTitle == "שחרורים" ||
                cleanTitle == "releases"
    }

    fun isDefenseRootCard(card: SubjectTopicsUiLogic.SubjectCardModel): Boolean {
        val cleanId = card.id.trim().lowercase()
        val cleanTitle = normText(stripLockSuffix(card.title))

        return isDefenseRootSubjectId(cleanId) ||
                cleanTitle == "הגנות" ||
                cleanTitle == "defenses"
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
        isEnglish,
        hasAccess
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            subTopicsPickCountsBySubjectId = subTopicsPickCountsBySubjectId,
            formatCount = ::formatCount
        ).map { card ->

            val baseTitle = subjectTitleForUi(
                subjectId = card.id,
                fallbackHeb = card.title,
                isEnglish = isEnglish
            )

            val titleWithLock =
                if (
                    card.id == "releases" &&
                    accessMode != AccessMode.OPEN
                ) {
                    "$baseTitle 🔒"
                } else if (LockedContentPolicy.shouldShowLock(accessMode, baseTitle)) {
                    "$baseTitle 🔒"
                } else {
                    baseTitle
                }

            card.copy(
                title = titleWithLock,
                countText = translateCardCountText(card.countText)
            )
        }.sortedWith(
            compareByDescending<SubjectTopicsUiLogic.SubjectCardModel> {
                isPremiumRootSubject(it.id)
            }.thenBy {
                when (it.id.trim().lowercase()) {
                    "defense_root", "defenses_root" -> 0
                    "releases" -> 1
                    else -> 2
                }
            }
        )
    }

    val subjectsWithoutSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts,
        isEnglish,
        hasAccess
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withoutSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            subTopicsPickCountsBySubjectId = subTopicsPickCountsBySubjectId,
            formatCount = ::formatCount
        ).map { card ->

            val baseTitle = subjectTitleForUi(
                subjectId = card.id,
                fallbackHeb = card.title,
                isEnglish = isEnglish
            )

            val titleWithLock =
                if (
                    card.id == "releases" &&
                    accessMode != AccessMode.OPEN
                ) {
                    "$baseTitle 🔒"
                } else if (LockedContentPolicy.shouldShowLock(accessMode, baseTitle)) {
                    "$baseTitle 🔒"
                } else {
                    baseTitle
                }

            card.copy(
                title = titleWithLock,
                countText = translateCardCountText(card.countText)
            )
        }
    }

    // ✅ Releases יכול להגיע בשתי צורות:
    // 1. כנושא עם תתי־נושאים — ברוב החגורות
    // 2. כנושא ישיר בלי תתי־נושאים — למשל חגורה חומה
    // לכן מחפשים אותו גם ב-withSubTopics וגם ב-withoutSubTopics.
    val releasesRootCard = remember(
        subjectsWithSubTopicsCards,
        subjectsWithoutSubTopicsCards,
        subTopicsPickCountsBySubjectId,
        isEnglish,
        hasAccess
    ) {
        val card = subjectsWithSubTopicsCards.firstOrNull { isReleasesRootCard(it) }
            ?: subjectsWithoutSubTopicsCards.firstOrNull { isReleasesRootCard(it) }

        card?.let {
            val baseTitle = subjectTitleForUi(
                subjectId = "releases",
                fallbackHeb = stripLockSuffix(it.title),
                isEnglish = isEnglish
            )

            val releaseCounts = subTopicsPickCountsBySubjectId["releases"].orEmpty()

            it.copy(
                id = "releases",
                title = if (hasAccess) baseTitle else "$baseTitle 🔒",
                countText = countTextFromSubTopicTotalsOrFallback(
                    counts = releaseCounts,
                    fallback = it.countText,
                    subTopicsCount = releaseCounts.size
                )
            )
        }
    }

    val otherSubjectsWithSubTopicsCards = remember(subjectsWithSubTopicsCards) {
        subjectsWithSubTopicsCards.filterNot {
            isReleasesRootCard(it) || isDefenseRootCard(it)
        }
    }

    val otherSubjectsWithoutSubTopicsCards = remember(subjectsWithoutSubTopicsCards) {
        subjectsWithoutSubTopicsCards.filterNot {
            isReleasesRootCard(it) || isDefenseRootCard(it)
        }
    }

    val defenseRootCard = remember(totalDefense, defenseDialogCountsMap, isEnglish, hasAccess) {
        val base = SubjectTopicsUiLogic.buildDefenseRootCard(
            totalDefense = totalDefense,
            formatCount = ::formatCount,
            subTopicsCount = defenseDialogCountsMap.size
        )

        val baseTitle = subjectTitleForUi(
            subjectId = "defense_root",
            fallbackHeb = base.title,
            isEnglish = isEnglish
        )

        base.copy(
            title = if (hasAccess) baseTitle else "$baseTitle 🔒",
            countText = countTextFromSubTopicTotalsOrFallback(
                counts = defenseDialogCountsMap,
                fallback = base.countText,
                subTopicsCount = defenseDialogCountsMap.size
            )
        )
    }

    val handsRootCard = remember(handsRootCount, handsPickCounts, isEnglish) {
        val totalHandsExercises =
            handsPickCounts.values.sum().takeIf { it > 0 } ?: handsRootCount

        val base = SubjectTopicsUiLogic.buildHandsRootCard(
            handsRootCount = totalHandsExercises,
            formatCount = ::formatCount,
            subTopicsCount = handsPickCounts.size
        )

        base.copy(
            title = subjectTitleForUi(
                subjectId = "hands_root",
                fallbackHeb = base.title,
                isEnglish = isEnglish
            ),
            countText = countTextFromSubTopicTotalsOrFallback(
                counts = handsPickCounts,
                fallback = base.countText,
                subTopicsCount = handsPickCounts.size
            )
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        val scrollState = rememberScrollState()

        val showScrollHint by remember {
            derivedStateOf {
                scrollState.value < scrollState.maxValue
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
                shadowElevation = if (isDarkMode) 8.dp else 6.dp,
                color = if (isDarkMode) {
                    Color(0xFF101827).copy(alpha = 0.98f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEnglish) "Subjects (Categories)" else "נושאים (קטגוריות)",
                        style = if (isDarkMode) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp),
                        color = if (isDarkMode) Color.White else Color(0xFF263238)
                    )

                    if (!countsReady) {
                        Text(
                            text = if (isEnglish) "Loading data..." else "טוען נתונים…",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.78f) else Color(
                                0xFF475569
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(440.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 8.dp)
                                .padding(top = 0.dp, bottom = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val pinnedLockCards = buildList {
                                add(
                                    Triple(
                                        "defense_root",
                                        defenseRootCard.title,
                                        defenseRootCard.countText
                                    )
                                )

                                releasesRootCard?.let { releasesCard ->
                                    add(
                                        Triple(
                                            "releases",
                                            releasesCard.title,
                                            releasesCard.countText
                                        )
                                    )
                                }
                            }

                            pinnedLockCards.forEachIndexed { index, card ->
                                SubjectRootCardPremium(
                                    title = card.second,
                                    subtitle = "",
                                    subjectId = card.first,
                                    countText = card.third,
                                    showLeftBadge = true,
                                    isDarkMode = isDarkMode,
                                    showExpandArrow = true,
                                    isExpanded = expandedSubTopicsForId == card.first,
                                    onClick = {
                                        when (card.first) {
                                            "defense_root",
                                            "defenses_root",
                                            "defenses" -> {
                                                expandedSubTopicsForId =
                                                    if (expandedSubTopicsForId == "defense_root") null else "defense_root"
                                            }

                                            "releases" -> {
                                                if (!hasAccess) {
                                                    onOpenSubscription()
                                                } else {
                                                    expandedSubTopicsForId =
                                                        if (expandedSubTopicsForId == "releases") null else "releases"
                                                }
                                            }
                                        }
                                    }
                                )

                                if (card.first == "defense_root" && expandedSubTopicsForId == "defense_root") {
                                    val defensePicks = remember(isEnglish, hasAccess) {
                                        listOf(
                                            if (isEnglish) "Internal Defenses" else "הגנות פנימיות",
                                            if (isEnglish) "External Defenses" else "הגנות חיצוניות",
                                            if (isEnglish) "Defenses Against Kicks" else "הגנות נגד בעיטות",
                                            if (isEnglish) "Knife Defenses" else "הגנות מסכין",
                                            if (isEnglish) "Rifle Defenses Against Knife Stabs" else "הגנות עם רובה נגד דקירות סכין",
                                            if (isEnglish) "Gun Threat Defenses" else "הגנות מאיום אקדח",
                                            if (isEnglish) "Defenses Against Multiple Attackers" else "הגנות נגד מספר תוקפים",
                                            if (isEnglish) "Stick Defenses" else "הגנות נגד מקל"
                                        ).map { title ->
                                            val cleanTitle = stripLockSuffix(title).trim()

                                            val isFreeKickDefense =
                                                cleanTitle == "הגנות נגד בעיטות" ||
                                                        cleanTitle == "Defenses Against Kicks"

                                            withLockSuffix(title, !hasAccess && !isFreeKickDefense)
                                        }
                                    }

                                    InlineSubTopicsExpansionCard(
                                        picks = defensePicks,
                                        counts = defenseDialogCountsMap,
                                        isEnglish = isEnglish,
                                        accent = subjectAccentColor("defense_root"),
                                        onPick = { pickedDisplay ->
                                            val pickedClean = stripLockSuffix(pickedDisplay)

                                            val pickedForLogic = when (pickedClean) {
                                                "Internal Defenses" -> "הגנות פנימיות"
                                                "External Defenses" -> "הגנות חיצוניות"
                                                "Defenses Against Kicks" -> "הגנות נגד בעיטות"
                                                "Knife Defenses" -> "הגנות מסכין"
                                                "Rifle Defenses Against Knife Stabs" -> "הגנות עם רובה נגד דקירות סכין"
                                                "Gun Threat Defenses" -> "הגנות מאיום אקדח"
                                                "Defenses Against Multiple Attackers" -> "הגנות נגד מספר תוקפים"
                                                "Stick Defenses" -> "הגנות נגד מקל"
                                                else -> pickedClean
                                            }

                                            if (pickedForLogic == "הגנות נגד בעיטות") {
                                                onOpenKicksHardLocal()
                                            } else {
                                                when (val decision =
                                                    SubjectTopicsUiLogic.resolveDefenseDialogPick(
                                                        pickedForLogic
                                                    )) {

                                                    is SubjectTopicsUiLogic.DefenseDialogDecision.AskKind -> {
                                                        val combinedId =
                                                            defenseCombinedSectionIdFor(decision.kind)

                                                        if (combinedId != null) {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                combinedId
                                                            )
                                                        } else {
                                                            askKind = decision.kind
                                                        }
                                                    }

                                                    is SubjectTopicsUiLogic.DefenseDialogDecision.OpenHardSubject -> {
                                                        when (decision.subjectId) {
                                                            "kicks",
                                                            "kicks_hard" -> {
                                                                onOpenKicksHardLocal()
                                                            }

                                                            else -> {
                                                                onOpenHardSubjectRoute(
                                                                    currentBelt,
                                                                    decision.subjectId
                                                                )
                                                            }
                                                        }
                                                    }

                                                    SubjectTopicsUiLogic.DefenseDialogDecision.None -> Unit
                                                }
                                            }
                                        }
                                    )
                                }

                                if (card.first == "releases" && expandedSubTopicsForId == "releases") {
                                    val dialogData = remember(subjects) {
                                        SubjectTopicsUiLogic.buildSubTopicsDialogData(
                                            subjects = subjects,
                                            id = "releases"
                                        )
                                    }

                                    val counts =
                                        subTopicsPickCountsBySubjectId["releases"].orEmpty()

                                    val displayPicks = remember(
                                        dialogData.picks,
                                        dialogData.base,
                                        isEnglish,
                                        hasAccess,
                                        accessMode
                                    ) {
                                        dialogData.picks.map { rawPick ->
                                            val uiTitle =
                                                subTopicTitleForUi(rawPick.trim(), isEnglish)

                                            withLockSuffix(
                                                uiTitle,
                                                accessMode != AccessMode.OPEN &&
                                                        LockedContentPolicy.shouldShowLock(
                                                            accessMode,
                                                            dialogData.base?.titleHeb.orEmpty()
                                                        )
                                            )
                                        }
                                    }

                                    InlineSubTopicsExpansionCard(
                                        picks = displayPicks,
                                        counts = counts,
                                        isEnglish = isEnglish,
                                        accent = subjectAccentColor("releases"),
                                        onPick = { pickedDisplay ->
                                            openPickedSubTopic("releases", pickedDisplay)
                                        }
                                    )
                                }

                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = if (isDarkMode) {
                                        Color.White.copy(alpha = 0.08f)
                                    } else {
                                        Color(0x14000000)
                                    },
                                    modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                                )
                            }

                            SubjectRootCardPremium(
                                title = handsRootCard.title,
                                subtitle = "",
                                subjectId = "hands_root",
                                countText = handsRootCard.countText,
                                showLeftBadge = true,
                                isDarkMode = isDarkMode,
                                showExpandArrow = true,
                                isExpanded = expandedSubTopicsForId == "hands_root",
                                onClick = {
                                    expandedSubTopicsForId =
                                        if (expandedSubTopicsForId == "hands_root") null else "hands_root"
                                }
                            )

                            if (expandedSubTopicsForId == "hands_root") {
                                val handsPicks = remember(handsBase) {
                                    SubjectTopicsUiLogic.handsPicks(handsBase)
                                }

                                val handsDisplayPicks = remember(handsPicks, isEnglish) {
                                    handsPicks.map { subTopicTitleForUi(it, isEnglish) }
                                }

                                InlineSubTopicsExpansionCard(
                                    picks = handsDisplayPicks,
                                    counts = handsPickCounts,
                                    isEnglish = isEnglish,
                                    accent = subjectAccentColor("hands_root"),
                                    onPick = { pickedDisplay ->
                                        val picked = handsPicks.firstOrNull {
                                            subTopicTitleForUi(it, isEnglish) == pickedDisplay
                                        } ?: pickedDisplay

                                        val hardSubjectId = handsSectionIdFor(picked)

                                        if (hardSubjectId != null) {
                                            onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                                        } else {
                                            val subject = SubjectTopicsUiLogic.resolveHandsPick(
                                                base = handsBase,
                                                picked = picked
                                            )

                                            if (subject != null) {
                                                openSubjectSmart(subject)
                                            }
                                        }
                                    }
                                )
                            }


                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = if (isDarkMode) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color(0x14000000)
                                },
                                modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                            )

                            // ✅ שאר הנושאים עם תתי־נושאים (בלי releases ובלי defenses שכבר הוצגו למעלה)
                            otherSubjectsWithSubTopicsCards
                                .filter { it.id != "defense_root" && it.id != "defenses_root" }
                                .forEachIndexed { index, card ->
                                    SubjectRootCardPremium(
                                        title = card.title,
                                        subtitle = "",
                                        subjectId = card.id,
                                        countText = countTextForSubjectCard(card),
                                        isDarkMode = isDarkMode,
                                        showExpandArrow = card.id != "releases_hugs",
                                        isExpanded = expandedSubTopicsForId == card.id,
                                        onClick = {
                                            if (card.id == "releases_hugs") {
                                                onOpenHardSubjectRoute(currentBelt, "releases_hugs")
                                            } else {
                                                expandedSubTopicsForId =
                                                    if (expandedSubTopicsForId == card.id) null else card.id
                                            }
                                        }
                                    )

                                    if (expandedSubTopicsForId == card.id) {
                                        val dialogData = remember(subjects, card.id) {
                                            SubjectTopicsUiLogic.buildSubTopicsDialogData(
                                                subjects = subjects,
                                                id = card.id
                                            )
                                        }

                                        val counts =
                                            subTopicsPickCountsBySubjectId[card.id].orEmpty()

                                        val displayPicks = remember(
                                            dialogData.picks,
                                            dialogData.base,
                                            isEnglish,
                                            hasAccess,
                                            accessMode
                                        ) {
                                            dialogData.picks.map { rawPick ->
                                                val uiTitle =
                                                    subTopicTitleForUi(rawPick.trim(), isEnglish)

                                                withLockSuffix(
                                                    uiTitle,
                                                    accessMode != AccessMode.OPEN &&
                                                            LockedContentPolicy.shouldShowLock(
                                                                accessMode,
                                                                dialogData.base?.titleHeb.orEmpty()
                                                            )
                                                )
                                            }
                                        }

                                        InlineSubTopicsExpansionCard(
                                            picks = displayPicks,
                                            counts = counts,
                                            isEnglish = isEnglish,
                                            accent = subjectAccentColor(card.id),
                                            onPick = { pickedDisplay ->
                                                openPickedSubTopic(card.id, pickedDisplay)
                                            }
                                        )
                                    }

                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = if (isDarkMode) {
                                            Color.White.copy(alpha = 0.08f)
                                        } else {
                                            Color(0x14000000)
                                        },
                                        modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                                    )
                                }

                            // ✅ מציגים נושאים בלי תתי־נושאים
                            // releases כבר מוצג למעלה יחד עם Defenses כדי לקבל מנעול כמו נושא פרימיום.
                            otherSubjectsWithoutSubTopicsCards
                                .filter { it.id != "defenses" }
                                .forEachIndexed { index, card ->

                                    val subject = subjects.firstOrNull { it.id == card.id }
                                        ?: return@forEachIndexed
                                    SubjectRootCardPremium(
                                        title = card.title,
                                        subtitle = "",
                                        subjectId = card.id,
                                        countText = countTextForSubjectCard(card),
                                        isDarkMode = isDarkMode,
                                        onClick = {
                                            when (card.id) {
                                                "topic_kavaler" -> {
                                                    val action =
                                                        SubjectTopicsUiLogic.buildOpenSubjectUiAction(
                                                            subject = subject,
                                                            currentBelt = currentBelt
                                                        )

                                                    val chosenBelt = action.chosenBelt

                                                    onOpenHardSubjectRoute(
                                                        chosenBelt,
                                                        "topic_kavaler"
                                                    )
                                                }

                                                else -> {
                                                    openSubjectSmart(subject)
                                                }
                                            }
                                        }
                                    )

                                    if (index != otherSubjectsWithoutSubTopicsCards.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 0.8.dp,
                                            color = if (isDarkMode) {
                                                Color.White.copy(alpha = 0.08f)
                                            } else {
                                                Color(0x14000000)
                                            },
                                            modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                                        )
                                    }
                                }

                            if (askDefense) {
                                DefenseCategoryPickDialogModern(
                                    counts = defenseDialogCountsMap,
                                    hasAccess = hasAccess,
                                    onDismiss = { askDefense = false },
                                    onPick = { picked ->
                                        askDefense = false

                                        val pickedClean = picked
                                            .replace("🔒", "")
                                            .trim()

                                        if (
                                            pickedClean == "הגנות נגד בעיטות" ||
                                            pickedClean == "Defenses Against Kicks"
                                        ) {
                                            onOpenHardSubjectRoute(
                                                currentBelt,
                                                "kicks_hard"
                                            )
                                        } else if (!hasAccess) {
                                            onOpenSubscription()
                                        } else {
                                            when (val decision =
                                                SubjectTopicsUiLogic.resolveDefenseDialogPick(
                                                    pickedClean
                                                )
                                            ) {
                                                is SubjectTopicsUiLogic.DefenseDialogDecision.AskKind -> {
                                                    askKind = decision.kind
                                                }

                                                is SubjectTopicsUiLogic.DefenseDialogDecision.OpenHardSubject -> {
                                                    when (decision.subjectId) {
                                                        "kicks",
                                                        "kicks_hard" -> {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                "kicks_hard"
                                                            )
                                                        }

                                                        else -> {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                decision.subjectId
                                                            )
                                                        }
                                                    }
                                                }

                                                SubjectTopicsUiLogic.DefenseDialogDecision.None -> Unit
                                            }
                                        }
                                    }
                                )
                            }

                            askKind?.let { kind ->
                                DefensePickModeDialogModern(
                                    kind = kind,
                                    counts = defensePickCountsMap,
                                    hasAccess = hasAccess,
                                    onDismiss = { askKind = null },
                                    onPick = { picked ->
                                        askKind = null

                                        val pickedClean = stripLockSuffix(picked)

                                        when (val decision =
                                            SubjectTopicsUiLogic.resolveDefenseKindPick(
                                                kind,
                                                pickedClean
                                            )) {
                                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenLegacyDefenses -> {
                                                val canOpen = hasAccess

                                                if (!canOpen) {
                                                    onOpenSubscription()
                                                } else {
                                                    onOpenDefenseList(
                                                        currentBelt,
                                                        decision.kind,
                                                        decision.pick
                                                    )
                                                }
                                            }

                                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenHardSubject -> {
                                                when (decision.subjectId) {
                                                    "kicks",
                                                    "kicks_hard" -> {
                                                        onOpenKicksHardLocal()
                                                    }

                                                    else -> {
                                                        onOpenHardSubjectRoute(
                                                            currentBelt,
                                                            decision.subjectId
                                                        )
                                                    }
                                                }
                                            }

                                            SubjectTopicsUiLogic.DefenseKindPickDecision.None -> Unit
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // החץ הועבר החוצה למסך האב כדי שיופיע מתחת לכרטיס הנושאים
            }
        }
    }
}

@Composable
private fun PremiumScrollDownHint(
    currentBelt: Belt,
    isDarkMode: Boolean,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollHintPulse = rememberInfiniteTransition(label = "premiumScrollHintPulse")

    val arrowOffsetY by scrollHintPulse.animateFloat(
        initialValue = -1.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumScrollHintOffset"
    )

    val arrowAlpha by scrollHintPulse.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumScrollHintAlpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (isDarkMode) {
            Color(0xFF162033).copy(alpha = 0.98f)
        } else {
            Color.White.copy(alpha = 0.98f)
        },
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkMode) {
                Color.White.copy(alpha = 0.14f)
            } else {
                currentBelt.color.copy(alpha = 0.28f)
            }
        )
    ) {
        Box(
            modifier = Modifier.size(width = 58.dp, height = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                contentDescription = if (isEnglish) {
                    "More items below"
                } else {
                    "יש עוד פריטים למטה"
                },
                tint = if (isDarkMode) {
                    Color.White.copy(alpha = arrowAlpha)
                } else {
                    currentBelt.color.copy(alpha = arrowAlpha)
                },
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        translationY = arrowOffsetY
                    }
            )
        }
    }
}
