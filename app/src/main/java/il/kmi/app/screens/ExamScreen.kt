package il.kmi.app.screens

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import il.kmi.shared.domain.Belt
import il.kmi.app.R
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.ext.color
import il.kmi.shared.localization.AppLanguage
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.kmi.app.ui.practice.PracticeBottomControls
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiSectionHeaderBackground
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import il.kmi.app.ui.practice.PracticeExerciseCenterCard

//===============================================================

// ✅ קבוע אחד למבחן (לא דיאלוג, לא שינוי, לא כפילויות)
private const val EXAM_SECONDS_PER_EXERCISE = 20

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

private fun exerciseNoteIdFor(raw: String): String {
    return normalizeFavoriteId(toDisplayItem(raw))
        .ifBlank { normalizeFavoriteId(raw) }
        .trim()
}

private fun readExerciseNote(
    prefs: SharedPreferences,
    primaryKey: String,
    vararg legacyKeys: String
): String {
    val keys = buildList {
        add(primaryKey)
        addAll(legacyKeys)
    }.distinct()

    return keys
        .asSequence()
        .map { key -> prefs.getString(key, "").orEmpty().trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

private fun saveExerciseNote(
    prefs: SharedPreferences,
    text: String,
    primaryKey: String,
    vararg legacyKeys: String
) {
    val clean = text.trim()

    prefs.edit {
        val keys = buildList {
            add(primaryKey)
            addAll(legacyKeys)
        }.distinct()

        keys.forEach { key ->
            if (clean.isBlank()) {
                remove(key)
            } else {
                putString(key, clean)
            }
        }
    }
}

private fun findExplanationForExam(
    belt: Belt,
    rawItem: String,
    isEnglish: Boolean,
    topic: String = ""
): String {
    val display = il.kmi.shared.questions.model.util.ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved =
        ExerciseExplanationResolver.get(
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

    val isFallback =
        cleaned.isBlank() ||
                cleaned.startsWith("הסבר מפורט על") ||
                cleaned.startsWith("אין כרגע") ||
                cleaned.startsWith("Detailed explanation for:") ||
                cleaned.startsWith("There is currently no explanation")

    if (!isFallback) {
        return cleaned
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
}

/** מזהה אם טקסט נראה כמו tag (לטיני/מספרים/_,:) ולא כמו עברית */
private fun looksLikeTag(s: String): Boolean {
    val t = s.trim()
    if (t.isBlank()) return false
    val hasHebrew = t.any { it in '\u0590'..'\u05FF' }
    if (hasHebrew) return false
    return t.any { it.isLetterOrDigit() } && t.all { it.isLetterOrDigit() || it in "_:-" }
}

/** מחזיר שם תרגיל “נקי” להצגה/הקראה */
private fun toDisplayItem(raw: String): String {
    val s = raw.trim()

    // מקרה נפוץ: tag::שם או שם::tag
    if ("::" in s) {
        val left = s.substringBefore("::").trim()
        val right = s.substringAfterLast("::").trim()
        return when {
            looksLikeTag(left) && !looksLikeTag(right) -> right
            !looksLikeTag(left) && looksLikeTag(right) -> left
            else -> right
        }.ifBlank { s }
    }

    // לפעמים מגיע כ-2 שורות: שורה 1 tag, שורה 2 שם
    val lines = s.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size >= 2 && looksLikeTag(lines.first())) {
        val rest = lines.drop(1).joinToString(" ").trim()
        if (rest.isNotBlank()) return rest
    }

    // ניקוי קל: אם מתחיל ב-tag ואז רווח
    return s.replace(Regex("^[a-zA-Z0-9:_-]{2,}\\s+"), "").ifBlank { s }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    belt: Belt,
    onBack: () -> Unit,
    excludedItems: Set<String> = emptySet(),
    onHome: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER")
    onSearch: () -> Unit = {}
) {
    val context = LocalContext.current

    val languageManager =
        remember(context) {
            AppLanguageManager(context)
        }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val notePrefs = remember(context) {
        context.getSharedPreferences("kmi_exercise_notes", android.content.Context.MODE_PRIVATE)
    }
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

    // ✅ רב-פלטפורמי (כרגע No-Op כדי לקמפל בלי תלות ב-R)
    val soundPlayer = remember { il.kmi.shared.platform.PlatformSoundPlayer(context) }

    var examStarted by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var pickedSearchKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ----- שליפת פריטי המבחן (ללא רפלקציה / ללא JVM) -----
    val baseItems: List<String> = remember(belt) {
        il.kmi.shared.exam.ExamFacade.buildExamItems(
            beltId = belt.id,
            topicTitlesProvider = { beltId ->
                val appBelt =
                    Belt.fromId(beltId) ?: belt

                runCatching {
                    il.kmi.app.search.KmiSearchBridge
                        .topicTitlesFor(appBelt)
                }.getOrDefault(emptyList())
            },
            itemsProvider = { beltId, topicTitle ->
                val appBelt =
                    Belt.fromId(beltId) ?: belt

                runCatching {
                    il.kmi.app.search.KmiSearchBridge
                        .itemsFor(
                            appBelt,
                            topicTitle
                        )
                }.getOrDefault(emptyList())
            }
        )
    }

    val items: List<String> = remember(baseItems, excludedItems) {
        baseItems
            .filterNot { it in excludedItems }
            .shuffled()
    }

    val displayItems: List<String> = remember(items) {
        items.map(::toDisplayItem)
    }

    var currentIndex by remember {
        mutableIntStateOf(0)
    }

    var timeLeft by remember {
        mutableIntStateOf(
            EXAM_SECONDS_PER_EXERCISE
        )
    }

    var isRunning by remember {
        mutableStateOf(false)
    }
    var isMuted by rememberSaveable { mutableStateOf(false) }

    // ✅ Guard: אם items השתנתה והאינדקס יצא מהטווח – מתקנים
    LaunchedEffect(items.size) {
        currentIndex = when {
            items.isEmpty() -> 0
            currentIndex in items.indices -> currentIndex
            else -> 0
        }
    }

    // אתחול TTS + (אופציונלי) letsgo ואז תחילת המבחן
    LaunchedEffect(Unit) {
        // ✅ רב-פלטפורמי (Android/iOS)
        il.kmi.shared.tts.KmiTtsManager.init(
            il.kmi.shared.tts.PlatformContext(context)
        )

        runCatching { soundPlayer.play("letsgo") }

        examStarted = true
        isRunning = true
    }

    // מקריא אחרי תחילת המבחן ובכל מעבר לתרגיל חדש
    LaunchedEffect(currentIndex, items, isMuted, examStarted) {
        if (examStarted && !isMuted && items.isNotEmpty() && currentIndex in items.indices) {
            delay(300.milliseconds)
            KmiTtsManager.speak(displayItems[currentIndex])
        }
    }

    // ✅ טיימר: תמיד 20 שניות לתרגיל, ובסוף המבחן עוצר
    LaunchedEffect(currentIndex, isRunning, items, examStarted) {
        if (examStarted && isRunning && items.isNotEmpty() && currentIndex in items.indices) {
            timeLeft = EXAM_SECONDS_PER_EXERCISE

            while (timeLeft > 0 && isRunning) {
                delay(1.seconds)
                timeLeft--
            }

            if (!isRunning) return@LaunchedEffect

            if (timeLeft == 0) {
                if (currentIndex < items.lastIndex) {
                    currentIndex++
                } else {
                    // ✅ נגמר המבחן (אין עוד תרגילים) — עוצרים
                    isRunning = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KmiTtsManager.stop()
            runCatching { soundPlayer.release() }
        }
    }

    val total = items.size.coerceAtLeast(1)
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode =
        colorScheme.background.luminance() < 0.5f

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title =
                    tr(
                        "מבחן מסכם",
                        "Final Exam"
                    ),
                onBack = null,
                showBackNavigation = false,
                topBeltIconRes =
                    when (belt) {
                        Belt.WHITE ->
                            R.drawable.intro_belt_white

                        Belt.YELLOW ->
                            R.drawable.intro_belt_yellow

                        Belt.ORANGE ->
                            R.drawable.intro_belt_orange

                        Belt.GREEN ->
                            R.drawable.intro_belt_green

                        Belt.BLUE ->
                            R.drawable.intro_belt_blue

                        Belt.BROWN ->
                            R.drawable.intro_belt_brown

                        Belt.BLACK ->
                            R.drawable.intro_belt_black
                    },
                topBeltIconDescription =
                    if (isEnglish) {
                        belt.en
                    } else {
                        belt.heb
                    },
                showTopBeltIcon = true,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                onHome = onHome,
                onPickSearchResult = { key ->
                    pickedSearchKey = key
                },
                lockSearch = false
            )
        }
    ) { padding ->

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        tr(
                            "אין תרגילים זמינים",
                            "No exercises available"
                        ),
                    style = KmiTypography.body,
                    color =
                        colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
                .padding(padding)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .kmiSectionHeaderBackground()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text =
                            tr(
                                "בדקו את הידע וההתקדמות שלכם",
                                "Test your knowledge and progress"
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        color = kmiSectionHeaderContentColor(),
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text =
                            tr(
                                "20 שניות לכל תרגיל",
                                "20 seconds per exercise"
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        color =
                            kmiSectionHeaderContentColor()
                                .copy(alpha = 0.92f),
                        style = KmiTypography.caption,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 150.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    PracticeExerciseCenterCard(
                        belt = belt,
                        exerciseTitle =
                            displayItems[currentIndex],
                        exerciseSubtitle = null,
                        timeText =
                            String.format(
                                Locale.getDefault(),
                                "00:%02d",
                                timeLeft
                            ),
                        currentIndex =
                            currentIndex,
                        totalCount =
                            total,
                        centerLabel = null,
                        isRunning =
                            isRunning,
                        isMuted =
                            isMuted,
                        onToggleRunning = {
                            isRunning =
                                !isRunning

                            if (!isRunning) {
                                KmiTtsManager.stop()
                            } else if (
                                currentIndex in
                                items.indices
                            ) {
                                KmiTtsManager.speak(
                                    displayItems[currentIndex]
                                )
                            }
                        },
                        onToggleMute = {
                            isMuted =
                                !isMuted

                            if (isMuted) {
                                KmiTtsManager.stop()
                            } else if (
                                currentIndex in
                                items.indices
                            ) {
                                KmiTtsManager.speak(
                                    displayItems[currentIndex]
                                )
                            }
                        },
                        onCardClick = {
                            showHelp = true
                        }
                    )
                }

                PracticeBottomControls(
                    isEnglish = isEnglish,
                    showSkip =
                        currentIndex <
                                items.lastIndex,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 12.dp
                        ),
                    onHelp = {
                        showHelp = true
                    },
                    onSkip = {
                        KmiTtsManager.stop()

                        if (
                            currentIndex <
                            items.lastIndex
                        ) {
                            currentIndex++
                        }
                    },
                    onFinish = {
                        isRunning = false
                        examStarted = false

                        KmiTtsManager.stop()
                        onBack()
                    }
                )
            }
        }
    }

    pickedSearchKey?.let { key ->

        val (b, topic, item) =
            parseSearchKey(key)

        val explanation =
            remember(
                b,
                item,
                topic,
                isEnglish
            ) {
                findExplanationForExam(
                    belt = b,
                    rawItem = item,
                    isEnglish = isEnglish,
                    topic = topic
                )
            }

        val favId = remember(item) { normalizeFavoriteId(item) }
        val isFav = favorites.contains(favId)

        val noteId = remember(item) { exerciseNoteIdFor(item) }

        val noteKey = remember(b, noteId) {
            "note_${b.id}_${noteId}"
        }

        val legacyTopicNoteKey = remember(b, topic, favId) {
            "note_${b.id}_${topic.trim()}_${favId}"
        }

        var noteText by remember(noteKey, legacyTopicNoteKey) {
            mutableStateOf(
                readExerciseNote(
                    prefs = notePrefs,
                    primaryKey = noteKey,
                    legacyTopicNoteKey
                )
            )
        }

        var showNoteEditor by remember { mutableStateOf(false) }

        fun toggleFav() {
            if (item.isBlank()) return
            FavoritesStore.toggle(favId)
        }

        ExerciseExplanationDialog(
            title = toDisplayItem(item),
            beltLabel =
                if (isEnglish) {
                    "$topic • ${b.en}"
                } else {
                    "$topic • ${b.heb}"
                },
            explanation = explanation,
            noteText = noteText,
            isFavorite = isFav,
            accentColor = b.color,
            isEnglish = isEnglish,
            onDismiss = { pickedSearchKey = null },
            onEditNote = { showNoteEditor = true },
            onDeleteNote = {
                noteText = ""

                saveExerciseNote(
                    prefs = notePrefs,
                    text = "",
                    primaryKey = noteKey,
                    legacyTopicNoteKey
                )
            },
            onToggleFavorite = { toggleFav() }
        )

        if (showNoteEditor) {
            ExerciseNoteEditorDialog(
                exerciseTitle = toDisplayItem(item),
                noteText = noteText,
                isEnglish = isEnglish,
                accentColor = b.color,
                onNoteChange = { noteText = it },
                onDismiss = { showNoteEditor = false },
                onSave = {
                    val cleanNote = noteText.trim()
                    noteText = cleanNote

                    saveExerciseNote(
                        prefs = notePrefs,
                        text = cleanNote,
                        primaryKey = noteKey,
                        legacyTopicNoteKey
                    )

                    showNoteEditor = false
                }
            )
        }
    }

    if (showHelp && currentIndex in items.indices) {
        val rawItem = items[currentIndex]
        val displayItem = displayItems[currentIndex]

        val explanation =
            remember(
                belt,
                rawItem,
                isEnglish
            ) {
                findExplanationForExam(
                    belt = belt,
                    rawItem = rawItem,
                    isEnglish = isEnglish,
                    topic = ""
                )
            }

        val favId = remember(rawItem) { normalizeFavoriteId(rawItem) }
        val isFav = favorites.contains(favId)

        val noteId = remember(rawItem) { exerciseNoteIdFor(rawItem) }

        val noteKey = remember(belt, noteId) {
            "note_${belt.id}_${noteId}"
        }

        val legacyExamNoteKey = remember(belt, favId) {
            "note_${belt.id}_exam_${favId}"
        }

        var noteText by remember(noteKey, legacyExamNoteKey) {
            mutableStateOf(
                readExerciseNote(
                    prefs = notePrefs,
                    primaryKey = noteKey,
                    legacyExamNoteKey
                )
            )
        }

        var showNoteEditor by remember { mutableStateOf(false) }

        fun toggleFav() {
            if (rawItem.isBlank()) return
            FavoritesStore.toggle(favId)
        }

        ExerciseExplanationDialog(
            title = displayItem,
            beltLabel =
                tr(
                    "מבחן מסכם • ${belt.heb}",
                    "Final Exam • ${belt.en}"
                ),
            explanation = explanation,
            noteText = noteText,
            isFavorite = isFav,
            accentColor = belt.color,
            isEnglish = isEnglish,
            onDismiss = { showHelp = false },
            onEditNote = { showNoteEditor = true },
            onDeleteNote = {
                noteText = ""

                saveExerciseNote(
                    prefs = notePrefs,
                    text = "",
                    primaryKey = noteKey,
                    legacyExamNoteKey
                )
            },
            onToggleFavorite = { toggleFav() }
        )

        if (showNoteEditor) {
            ExerciseNoteEditorDialog(
                exerciseTitle = displayItem,
                noteText = noteText,
                isEnglish = isEnglish,
                accentColor = belt.color,
                onNoteChange = { noteText = it },
                onDismiss = { showNoteEditor = false },
                onSave = {
                    val cleanNote = noteText.trim()
                    noteText = cleanNote

                    saveExerciseNote(
                        prefs = notePrefs,
                        text = cleanNote,
                        primaryKey = noteKey,
                        legacyExamNoteKey
                    )

                    showNoteEditor = false
                }
            )
        }
    }
}

