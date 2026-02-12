package il.kmi.app.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.openBeltPdf
import android.content.SharedPreferences
import android.util.Log
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.shared.search.KmiSearch
import il.kmi.app.ui.KmiLightTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import il.kmi.app.subscription.KmiAccess   // 👈 מצב גישה + ניסיון/מנוי
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Mic
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.assistant.AiAssistantDialog      // 👈 העוזר הקולי
import androidx.compose.foundation.shape.CircleShape
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.material.icons.filled.FitnessCenter
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.KmiSpeedDialFab
import il.kmi.app.ui.KmiFabAction

//==================================================================

private const val TAG_DEPRECATED = "KMI_DEPRECATED_FILE"

@Composable
private fun DeprecatedFileMarker() {
    LaunchedEffect(Unit) {
        Log.e(TAG_DEPRECATED, "⚠️ DEPRECATED FILE LOADED: ${"TopicsScreen.kt"} (should be deleted)")
    }
}


// ───────────── עזר: שליפה שטוחה של תרגילים לנושא (Bridge-first) ─────────────
private fun itemsForTopicFlatten(belt: Belt, topicTitle: String): List<String> {

    // 1) Bridge (המקור הרשמי עכשיו)
    val fromBridge: List<String> = runCatching {
        val direct = ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )

        val subs = ContentRepo.listSubTopicTitles(belt, topicTitle)
        val viaSubs = subs.flatMap { stTitle ->
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = stTitle
            )
        }

        (direct + viaSubs)
    }.getOrDefault(emptyList())

    if (fromBridge.isNotEmpty()) {
        return fromBridge
            .map { ExerciseTitleFormatter.displayName(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // 2) Bridge אחר (אם נשאר אצלך מסיבות היסטוריות)
    val viaSearchBridge = runCatching {
        il.kmi.app.search.KmiSearchBridge.itemsFor(belt, topicTitle)
    }.getOrDefault(emptyList())

    if (viaSearchBridge.isNotEmpty()) {
        return viaSearchBridge
            .map { ExerciseTitleFormatter.displayName(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    return emptyList()
}

private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val stancePrefix = "עמידת מוצא"

    // אם אין "עמידת מוצא" – מחזירים טקסט רגיל
    val idx = source.indexOf(stancePrefix)
    if (idx < 0) return AnnotatedString(source)

    val before = source.substring(0, idx)

    // מחפשים פסיק או נקודה אחרי "עמידת מוצא"
    val endPunctIndex = listOf(',', '.')
        .map { ch -> source.indexOf(ch, idx + stancePrefix.length) }
        .filter { it >= 0 }
        .minOrNull()

    // נסיים או אחרי סימן הפיסוק (כולל) או בסוף שורה/טקסט
    val stanceEndIndexExclusive = if (endPunctIndex != null) {
        endPunctIndex + 1   // כולל הפסיק / הנקודה
    } else {
        // אם אין פסיק/נקודה – עד סוף שורה או סוף הטקסט
        source.indexOf('\n', idx + stancePrefix.length).takeIf { it >= 0 }
            ?: source.length
    }

    val stanceText = source.substring(idx, stanceEndIndexExclusive)
    val after = source.substring(stanceEndIndexExclusive)

    return buildAnnotatedString {
        // לפני ההדגשה
        append(before)

        // החלק המודגש – "עמידת מוצא .....," או "עמידת מוצא ..... ."
        val stanceStart = length
        append(stanceText)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        // שאר ההסבר
        append(after)
    }
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun parseSearchKey(key: String): Triple<Belt, String, String> {
    fun dec(s: String): String =
        runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    val parts0 = when {
        '|' in key  -> key.split('|', limit = 3)
        "::" in key -> key.split("::", limit = 3)
        '/' in key  -> key.split('/', limit = 3)
        else        -> listOf("", "", "")
    }
    val parts = (parts0 + listOf("", "", "")).take(3)
    val belt  = Belt.fromId(parts[0]) ?: Belt.WHITE
    val topic = dec(parts[1])
    val item  = dec(parts[2])
    return Triple(belt, topic, item)
}

private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')    // en dash
        .replace('־', '-')    // maqaf
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
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return got.split("::")
                .map { it.trim() }
                .lastOrNull { it.isNotBlank() }
                ?: got.trim()
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
}

private fun findSubTopicTitleForItem(belt: Belt, topic: String, item: String): String? {

    fun norm(s: String): String = s
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('\u05BE','-').replace('\u2010','-').replace('\u2011','-')
        .replace('\u2012','-').replace('\u2013','-').replace('\u2014','-')
        .replace('\u2015','-').replace('\u2212','-')
        .replace(Regex("\\s*-\\s*"), "-")
        .trim().replace(Regex("\\s+"), " ").lowercase()

    val wanted = norm(item)

    val subTitles = runCatching { ContentRepo.listSubTopicTitles(belt, topic) }
        .getOrDefault(emptyList())

    if (subTitles.isEmpty()) return null

    // ניסיון 1: התאמה ישירה (item כפי שהוא)
    for (stTitle in subTitles) {
        val items = runCatching {
            ContentRepo.listItemTitles(belt, topic, subTopicTitle = stTitle)
        }.getOrDefault(emptyList())

        if (items.any { it == item }) return stTitle
    }

    // ניסיון 2: התאמה מנורמלת
    for (stTitle in subTitles) {
        val items = runCatching {
            ContentRepo.listItemTitles(belt, topic, subTopicTitle = stTitle)
        }.getOrDefault(emptyList())

        if (items.any { norm(it) == wanted }) return stTitle
    }

    return null
}

// ───────── SectionCard ─────────
@Composable
private fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    headerTopPadding: Dp = 8.dp,
    headerBottomSpacing: Dp = 6.dp,
    innerHorizontalPadding: Dp = 10.dp,
    innerVerticalPadding: Dp = 6.dp,
    titleInsetFromRight: Dp = 16.dp,
    footerBottomPadding: Dp = 14.dp,
    borderWidth: Dp = 0.5.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(24.dp)

    val outline = remember(container) {
        if (container.luminance() > 0.6f) Color(0x803A3A3A) else Color.White.copy(alpha = 0.85f)
    }

    val shadow = if (container.alpha <= 0.01f) 0.dp else 1.dp

    Surface(
        modifier = modifier.clip(cardShape),
        shape = cardShape,
        color = container,
        tonalElevation = 0.dp,
        shadowElevation = shadow,
        border = BorderStroke(borderWidth, outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(
                start = innerHorizontalPadding,
                end = innerHorizontalPadding,
                top = innerVerticalPadding,
                bottom = innerVerticalPadding
            )
        ) {
            if (!title.isNullOrBlank()) {
                Spacer(Modifier.height(headerTopPadding))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = titleInsetFromRight),
                    textAlign = TextAlign.Right
                )
                Spacer(Modifier.height(headerBottomSpacing))
                Divider(thickness = 1.dp, color = outline.copy(alpha = 0.35f))
                Spacer(Modifier.height(6.dp))
            }

            Column(Modifier.fillMaxWidth(), content = content)

            Spacer(Modifier.height(footerBottomPadding))
        }
    }
}

@Composable
private fun ModernDivider(
    modifier: Modifier = Modifier,
    height: Dp = 2.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(999.dp)
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    vm: KmiViewModel,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onSummary: (Belt) -> Unit,
    onRandomPractice: (Belt) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLists: (Belt) -> Unit,
    onExam: (Belt) -> Unit,
    onOpenHome: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenWeakPoints: () -> Unit = {},   // ✅ חדש: לא מחובר עדיין (no-op)

    // ✅ חשוב: בלי default כדי שלא “יתקע” אם שכחת להעביר מה-NavGraph
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit
) {
    val belt = vm.selectedBelt.collectAsState().value ?: Belt.WHITE

    // ✅ לעולם לא עובדים עם WHITE במסך הזה
    val effectiveBelt: Belt = belt.takeUnless { it == Belt.WHITE } ?: Belt.GREEN

    val topicTitles: List<String> = remember(effectiveBelt) {
    val viaBridge = runCatching {
            il.kmi.app.search.KmiSearchBridge.topicTitlesFor(effectiveBelt)
        }.getOrDefault(emptyList())
        if (viaBridge.isNotEmpty()) viaBridge
        else {
            runCatching {
                val sharedBelt: il.kmi.shared.domain.Belt =
                    il.kmi.shared.domain.Belt.fromId(effectiveBelt.id)
                        ?: il.kmi.shared.domain.Belt.WHITE

                il.kmi.shared.domain.SubTopicRegistry
                    .allForBelt(sharedBelt)
                    .keys
                    .toList()
            }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(effectiveBelt) {
        val sharedRepo = ContentRepo.asSharedRepo()
        val hits = KmiSearch.search(
            repo = sharedRepo,
            query = "הגנה",
            belt = effectiveBelt.toShared()
        )
        Log.d(
            "KMI-SEARCH",
            if (hits.isEmpty()) "no results"
            else hits.joinToString(" | ") { h ->
                val where = h.item ?: "(topic)"
                "${h.belt.name}/${h.topic}/$where"
            }
        )
    }

    LaunchedEffect(Unit) {
        try {
            val dump = il.kmi.app.search.KmiSearchBridge.debugDumpRepo()
            android.util.Log.d("KMI-DEBUG", "repoDump: $dump")
        } catch (t: Throwable) {
            android.util.Log.e("KMI-DEBUG", "repoDump failed", t)
        }
    }

    val beltLabel = remember(effectiveBelt) {
        if (effectiveBelt.heb.contains("חגורה")) effectiveBelt.heb else "חגורה ${effectiveBelt.heb}"
    }

    val onBeltColor = if (effectiveBelt.color.luminance() < 0.5f) Color.White else Color.Black
    val bottomButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFE0E0E0),
        contentColor = Color.Black
    )
    val bottomButtonShape = RoundedCornerShape(24.dp)
    val ctx = LocalContext.current
    val userSp = remember { ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE) }

    // 👇 מצב גישה לפי ניסיון/מנוי + עקיפת מנהל (מתעדכן בזמן אמת)
    var isManagerOverride by remember {
        mutableStateOf(
            userSp.getBoolean("is_manager", false) ||
                    userSp.getBoolean("manager_mode", false) ||
                    userSp.getBoolean("dev_override", false)
        )
    }
    DisposableEffect(userSp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "is_manager" || key == "manager_mode" || key == "dev_override") {
                isManagerOverride =
                    sp.getBoolean("is_manager", false) ||
                            sp.getBoolean("manager_mode", false) ||
                            sp.getBoolean("dev_override", false)
            }
        }
        userSp.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    val canUseTraining = KmiAccess.canUseTraining(userSp) || isManagerOverride
    val canUseExtras   = KmiAccess.canUseExtras(userSp)   || isManagerOverride
    val isTrial        = KmiAccess.isTrialActive(userSp) && !KmiAccess.hasFullAccess(userSp)
    val trialDaysLeft  = KmiAccess.trialDaysLeft(userSp)

    // ✅ סנכרון חגורה אפקטיבית חזרה ל-VM (מונע מצב שהמסך הבא רואה WHITE/ישן)
    LaunchedEffect(effectiveBelt) {
        val cur = vm.selectedBelt.value
        if (cur == null || cur != effectiveBelt) {
            vm.setSelectedBelt(effectiveBelt)
        }
    }

    // עוזר לדיבוג אם צריך
    LaunchedEffect(canUseTraining, canUseExtras, isManagerOverride) {
        android.util.Log.d(
            "KMI-ACCESS",
            "canUseTraining=$canUseTraining canUseExtras=$canUseExtras isManagerOverride=$isManagerOverride"
        )
    }

    // ===== ROLE (ללא vm.selectedRole) =====
    var userRole by remember { mutableStateOf(userSp.getString("user_role", null)) }
    DisposableEffect(userSp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences, key: String? ->
            if (key == "user_role") userRole = userSp.getString("user_role", null)
        }
        userSp.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    // ✅ במקום vm.selectedRole.collectAsState(...)
    val isCoach = remember(userRole) {
        val role = userRole?.trim().orEmpty()
        role.equals("coach", ignoreCase = true) ||
                role.contains("coach", ignoreCase = true) ||
                role.contains("מאמן") ||
                role.contains("מדריך")
    }

    // 🔹 רקע לפי מאמן/מתאמן – כמו במסכים האחרים
    val backgroundBrush = remember(isCoach) {
        if (isCoach) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141E30),
                    Color(0xFF243B55),
                    Color(0xFF0EA5E9)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7F00FF),
                    Color(0xFF3F51B5),
                    Color(0xFF03A9F4)
                )
            )
        }
    }

    LaunchedEffect(isCoach) {
        if (isCoach) {
            val cur = vm.selectedBelt.value
            if (cur == null || cur == Belt.WHITE) {
                vm.setSelectedBelt(Belt.GREEN)
            }
        }
    }

    val settingsSp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }

    KmiLightTheme(useGreenAccent = false) {
        // ⭐ helpers – רטט וצליל גלובליים למסך
        val haptic = rememberHapticsGlobal()
        val clickSound = rememberClickSound()

        var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
        var showAssistant by rememberSaveable { mutableStateOf(false) }

        // ✅ השאר רק אחד (Saveable)
        var fabMenuOpen by rememberSaveable { mutableStateOf(false) }

        // ✅ חדש: דיאלוג תרגול (3 אפשרויות)
        var showPracticeMenu by rememberSaveable { mutableStateOf(false) }

        // ✅ NEW: דיאלוג עוזר קולי (כפתור צף "עוזר קולי")
        if (showAssistant) {
            AiAssistantDialog(
                onDismiss = { showAssistant = false }
                // אם ל-AiAssistantDialog יש עוד פרמטרים אצלך (vm / ctx / belt וכו') –
                // תוסיף אותם כאן לפי החתימה בקובץ שלו.
            )
        }

        // ✅ חדש: דיאלוג "תרגול" (3 אפשרויות + בחירת נושאים)
        if (showPracticeMenu) {
            PracticeMenuDialog(
                canUseExtras = canUseExtras,

                // ✅ לא מעבירים WHITE כדיפולט (גם אם לא מסומן בפועל)
                defaultBelt = effectiveBelt.takeUnless { it == Belt.WHITE } ?: Belt.GREEN,

                onDismiss = { showPracticeMenu = false },

                onRandomPractice = { beltArg ->
                    clickSound()
                    haptic(true)
                    showPracticeMenu = false
                    onRandomPractice(beltArg)
                },
                onFinalExam = { beltArg ->
                    clickSound()
                    haptic(true)
                    showPracticeMenu = false
                    onExam(beltArg)
                },
                onPracticeByTopics = { selection ->
                    clickSound()
                    haptic(true)

                    // ✅ חשוב: קודם סוגרים, ואז מזניקים ניווט (כדי שהדיאלוג לא יישאר מעל המסך)
                    showPracticeMenu = false
                    onPracticeByTopics(selection)
                }
            )
        }

        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title = effectiveBelt.heb,
                    onHome = onOpenHome,
                    centerTitle = true,
                    showTopHome = false,
                    showBottomActions = true,
                    lockSearch = false,
                    showTopSearch = false,
                    onPickSearchResult = { key -> pickedKey = key },
                    extraActions = {}
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { padding ->

        // ----- דיאלוג הסבר מהחיפוש -----
            pickedKey?.let { key ->
                val (hitBelt, hitTopic, hitItem) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter.displayName(hitItem)
                val explanation = remember(hitBelt, hitItem) {
                    findExplanationForHit(
                        belt = hitBelt,
                        rawItem = hitItem,
                        topic = hitTopic
                    )
                }

                val favKey = "fav_${hitBelt.id}_${hitTopic.ifBlank { "general" }}"
                val favoritesState = remember(favKey) {
                    mutableStateOf(
                        settingsSp.getStringSet(favKey, emptySet()) ?: emptySet()
                    )
                }
                val favorites = favoritesState.value

                fun toggleFavorite(id: String) {
                    val newSet = favorites.toMutableSet()
                    if (!newSet.add(id)) newSet.remove(id)
                    favoritesState.value = newSet
                    settingsSp.edit().putStringSet(favKey, newSet).apply()
                }

                AlertDialog(
                    onDismissRequest = { pickedKey = null },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    toggleFavorite(hitItem)
                                },
                                modifier = Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterLeft)
                            ) {
                                if (favorites.contains(hitItem)) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "הסר ממועדפים",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים"
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(androidx.compose.ui.AbsoluteAlignment.CenterRight)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${hitBelt.heb})",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    text = {
                        val stanceColor = MaterialTheme.colorScheme.primary
                        val annotated = remember(explanation, stanceColor) {
                            buildExplanationWithStanceHighlight(
                                source = explanation,
                                stanceColor = stanceColor
                            )
                        }

                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right,
                            color = Color.Black
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                clickSound()
                                haptic(true)
                                pickedKey = null
                            }
                        ) {
                            Text("סגור")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundBrush)
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 0.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.End
                    ) {

                        // 🔹 פס תקופת ניסיון – ריבוע כחול
                        if (isTrial && canUseTraining && trialDaysLeft > 0) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                color = Color(0xFF0EA5E9).copy(alpha = 0.98f),
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 4.dp,
                                shadowElevation = 8.dp
                            ) {
                                Text(
                                    text = "נשארו $trialDaysLeft ימים לניסיון החינמי 🎯",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 18.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        val contentScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .verticalScroll(contentScroll)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {

                            val buttonSpacing = 10.dp

                            // === מסגרת עליונה: נושאים ===
                            SectionCard(
                                title = "נושאים",
                                modifier = Modifier.fillMaxWidth(),
                                container = MaterialTheme.colorScheme.surface
                            ) {
                                if (!canUseTraining) {
                                    Text(
                                        text = "תקופת הניסיון הסתיימה.\nכדי להמשיך להשתמש בתכני האימונים יש לרכוש מנוי.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFB71C1C),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    )
                                } else if (topicTitles.isEmpty()) {
                                    Text(
                                        text = "אין נושאים להצגה לחגורה זו",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    topicTitles.forEachIndexed { idx: Int, title: String ->

                                        val displayTitle by remember(title) {
                                            mutableStateOf(
                                                title.replace(Regex("\\s*\\n\\s*"), " ")
                                                    .replace(Regex("\\s{2,}"), " ")
                                                    .trim()
                                            )
                                        }

                                        val realSubsCount by remember(effectiveBelt, title) {
                                            mutableStateOf(
                                                runCatching {
                                                    ContentRepo.listSubTopicTitles(effectiveBelt, title)
                                                        .map { it.trim() }
                                                        .filter { it.isNotEmpty() && !it.equals(title.trim(), ignoreCase = true) }
                                                        .distinct()
                                                        .size
                                                }.getOrDefault(0)
                                            )
                                        }

                                        val itemCount by remember(effectiveBelt, title) {
                                            mutableStateOf(itemsForTopicFlatten(effectiveBelt, title).size)
                                        }

                                        val hasRealSubs = realSubsCount > 0

                                        Button(
                                            onClick = {
                                                if (!canUseTraining) return@Button
                                                clickSound()
                                                haptic(true)

                                                // ✅ חשוב: תמיד לסנכרן חגורה ל-VM לפני ניווט
                                                vm.setSelectedBelt(effectiveBelt)

                                                if (hasRealSubs) onOpenDefenseMenu(effectiveBelt, title)
                                                else onOpenTopic(effectiveBelt, title)
                                            },
                                            enabled = canUseTraining,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 88.dp)
                                                .padding(top = if (idx == 0) 0.dp else 10.dp)
                                                .border(
                                                    BorderStroke(
                                                        width = 2.dp,
                                                        color =
                                                            if (effectiveBelt.color.luminance() > 0.6f)
                                                                Color(0x803A3A3A)
                                                            else
                                                                Color.White.copy(alpha = 0.85f)
                                                    ),
                                                    shape = bottomButtonShape
                                                )
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = bottomButtonShape,
                                                    clip = false
                                                ),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = effectiveBelt.color,
                                                contentColor = onBeltColor
                                            ),
                                            shape = bottomButtonShape,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                                        ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = displayTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Right
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                val counts = if (hasRealSubs) {
                                                    "${realSubsCount} תתי נושאים  •  ${itemCount} תרגילים"
                                                } else {
                                                    "${itemCount} תרגילים"
                                                }
                                                Text(
                                                    text = counts,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = onBeltColor.copy(alpha = 0.90f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            ModernDivider(modifier = Modifier.padding(horizontal = 24.dp))
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // ✅ Speed-Dial FAB — משותף לכל המסכים ובאותו מיקום
                il.kmi.app.ui.KmiFloatingMenuOverlay(
                    effectiveBelt = effectiveBelt,
                    canUseExtras = canUseExtras,
                    onOpenWeakPoints = { onOpenWeakPoints() },
                    onOpenLists = { b -> onOpenLists(b) },
                    onOpenPracticeMenu = { showPracticeMenu = true },
                    onOpenSummary = { b ->
                        vm.setSelectedBelt(effectiveBelt)
                        onSummary(b)
                    },
                    onOpenAssistant = { showAssistant = true },
                    onOpenPdf = { b -> openBeltPdf(ctx, b) },
                    onHaptic = { haptic(true) },
                    onClickSound = { clickSound() }
                )
            } // ✅ סוף Box (הרקע)
        }     // ✅ סוף padding lambda של Scaffold
    }             // ✅ סוף KmiLightTheme
} // ✅ סוף TopicsScreen

    // ✅ SpeedDialRow חייב להיות ברמת קובץ (לא בתוך TopicsScreen)
@Composable
private fun SpeedDialRow(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    height: Dp = 52.dp,
    labelWidth: Dp = 170.dp,
    iconButtonSize: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    gap: Dp = 10.dp
) {
    val alphaDisabled = 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()         // ✅ חשוב: נמדד לפי רוחב ה-Column הקבוע
            .height(height),
        horizontalArrangement = Arrangement.End, // ✅ מצמיד את כל התוכן לאותו קו ימין
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LABEL
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = if (enabled) 0.92f else 0.80f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
            modifier = Modifier
                .width(labelWidth)
                .height(height)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF0B1220).copy(alpha = if (enabled) 1f else alphaDisabled),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.width(gap))

        // ICON
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = if (enabled) 0.92f else 0.80f),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
            modifier = Modifier.size(iconButtonSize)
        ) {
            IconButton(
                enabled = enabled,
                onClick = onClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(iconSize),
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
