package il.kmi.app.screens

import il.kmi.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.shared.domain.Belt
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.AbsoluteAlignment
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTtsManager.speak
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.platform.PlatformSoundPlayer
import java.net.URLDecoder   // ✅ נשאר רק זה
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.domain.ContentRepo as SharedContentRepo


// ✅ NEW: טוקן לתרגול לפי נושאים (חגורות+נושאים) – חייב להתאים ל-HomeNavGraph/PracticeFabMenu
private const val TOPICS_PICK_TOKEN = "__TOPICS_PICK__"

private fun decTokenPart(s: String): String =
    runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

/**
 * פורמט הטוקן:
 * __TOPICS_PICK__:<beltId>|<topicEnc>,<topicEnc>;<beltId>|<topicEnc>...
 *
 * מחזיר: Map<Belt, List<String>>
 */
private fun parseTopicsPickToken(token: String): Map<Belt, List<String>> {
    if (!token.startsWith("$TOPICS_PICK_TOKEN:")) return emptyMap()
    val payload = token.removePrefix("$TOPICS_PICK_TOKEN:").trim()
    if (payload.isBlank()) return emptyMap()

    return payload
        .split(';')
        .mapNotNull { seg ->
            val parts = seg.split('|', limit = 2)
            if (parts.size != 2) return@mapNotNull null

            val beltId = parts[0].trim()
            val belt = Belt.fromId(beltId) ?: return@mapNotNull null

            val topics = parts[1]
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { decTokenPart(it) }

            if (topics.isEmpty()) return@mapNotNull null
            belt to topics
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, lists) -> lists.flatten().distinct() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPracticeScreen(
    belt: Belt,
    topicFilter: String?,
    onBack: () -> Unit,
    practiceDurationMinutes: Int = 1,
    beepLast10: Boolean = true,
    onOpenSettings: () -> Unit = {},
    onHome: () -> Unit = {},
    onSearch: () -> Unit = {}
) {

    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    // ✅ Favorites – source of truth אחד לכל האפליקציה
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

    // ===================== NEW: canonical/display helpers (יציב ל-SP) =====================

    // ✅ נרמול עברי (כדי לייצר מפתח עקבי בין מסכים)
    fun String.normHeb(): String = this
        .replace("\u200F", "") // RLM
        .replace("\u200E", "") // LRM
        .replace("\u00A0", " ") // NBSP -> space
        .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
        .replace('\u05BE', '-') // מקאף עברי ־
        .replace('\u2010', '-') // Hyphen
        .replace('\u2011', '-') // Non-Breaking Hyphen
        .replace('\u2012', '-') // Figure Dash
        .replace('\u2013', '-') // En Dash
        .replace('\u2014', '-') // Em Dash
        .replace('\u2015', '-') // Horizontal Bar
        .replace('\u2212', '-') // Minus
        .replace(Regex("\\s*-\\s*"), "-")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    // ✅ canonical key = displayName מנורמל ואז normHeb
    fun canonicalKeyFor(rawItem: String): String =
        il.kmi.shared.questions.model.util.ExerciseTitleFormatter
            .displayName(rawItem)
            .trim()
            .normHeb()

    // ✅ fallback של displayName (אם יש לך עוד שימושים)
    fun displayName(rawItem: String): String =
        il.kmi.shared.questions.model.util.ExerciseTitleFormatter
            .displayName(rawItem)
            .trim()

    // =====================================================================================

    // ----- תוכן לתרגול -----
    // ✅ FIX: מקור האמת עבר ל-shared, לכן fallback חייב לקרוא מ-SharedContentRepo
     fun sharedTopicTitlesFor(b: Belt): List<String> {
        return SharedContentRepo.data[b]?.topics
            ?.map { it.title.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

     fun sharedItemsFor(b: Belt, topicTitle: String, subTopicTitle: String? = null): List<String> {
        return SharedContentRepo.getAllItemsFor(
            belt = b,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle
        )
    }

    // <<< נוסיף גם את רשימת שמות הנושאים כדי לקרוא את המועדפים/לא-יודע מה-SharedPreferences
    val allTopicTitles: List<String> = remember(belt) {
        runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt) }
            .getOrDefault(emptyList())
    }

    // 🔎 מאגר חיפוש גלובלי – כל החגורות, כל הנושאים
    val globalSearchItems = remember {
        Belt.order.flatMap { b ->
            val titles = sharedTopicTitlesFor(b)
            titles.flatMap { tp ->
                sharedItemsFor(b, tp, subTopicTitle = null)
                    .map { item -> Triple(b, tp, item) }
            }
        }
    }

    // תוצאה שנבחרה מהחיפוש להצגת הסבר
    var pickedSearchHit by rememberSaveable { mutableStateOf<Triple<Belt, String, String>?>(null) }

    // ✅ מקור עיקרי: shared PracticeFacade (כולל __UNKNOWN__/__FAVS_ALL__/__ALL__/TOPICS_PICK_TOKEN)
    val practiceItems: List<il.kmi.shared.practice.PracticeItem> = remember(belt, topicFilter) {
        val rawFilter = topicFilter?.trim().orEmpty()

        // אם הגיע טוקן עם encoding, נעשה decode כאן (כמו קודם)
        val decodedTopicsToken = if (rawFilter.isNotBlank() && rawFilter.startsWith("$TOPICS_PICK_TOKEN:")) {
            val payload = rawFilter.removePrefix("$TOPICS_PICK_TOKEN:")
            val decoded = payload.split(';').joinToString(";") { seg ->
                val parts = seg.split('|', limit = 2)
                if (parts.size != 2) seg
                else {
                    val beltId = parts[0]
                    val topicsDecoded = parts[1]
                        .split(',')
                        .joinToString(",") { enc -> decTokenPart(enc.trim()) }
                    "$beltId|$topicsDecoded"
                }
            }
            "$TOPICS_PICK_TOKEN:$decoded"
        } else {
            rawFilter
        }

        // ✅ Guard: אם ה"פילטר" הוא בעצם חגורה/אקראי/ריק — נלך על ALL
        val fixedFilter = when {
            decodedTopicsToken.isBlank() -> il.kmi.shared.practice.PracticeFilters.ALL

            decodedTopicsToken.equals(belt.heb.trim(), ignoreCase = true) -> il.kmi.shared.practice.PracticeFilters.ALL
            decodedTopicsToken.equals(belt.id.trim(), ignoreCase = true) -> il.kmi.shared.practice.PracticeFilters.ALL

            decodedTopicsToken.equals("אקראי", ignoreCase = true) -> il.kmi.shared.practice.PracticeFilters.ALL
            decodedTopicsToken.equals("random", ignoreCase = true) -> il.kmi.shared.practice.PracticeFilters.ALL
            decodedTopicsToken.equals("all", ignoreCase = true) -> il.kmi.shared.practice.PracticeFilters.ALL

            else -> decodedTopicsToken
        }

        // ✅ האם זה "פילטר של נושא בודד" (לא טוקן/לא ALL/UNKNOWN/FAVS)?
        val isSingleTopicFilter =
            fixedFilter.isNotBlank() &&
                    !fixedFilter.startsWith("$TOPICS_PICK_TOKEN:") &&
                    fixedFilter != il.kmi.shared.practice.PracticeFilters.ALL &&
                    fixedFilter != il.kmi.shared.practice.PracticeFilters.UNKNOWN &&
                    fixedFilter != il.kmi.shared.practice.PracticeFilters.FAVS_ALL

        // ✅ אם זה נושא בודד — בונים ALL ואז מסננים אצלנו עם normHeb כדי שלא ניפול על התאמה מדויקת
        val requestFilterForFacade =
            if (isSingleTopicFilter) il.kmi.shared.practice.PracticeFilters.ALL else fixedFilter

        // ===================== ✅ NEW: resolve נושא יחיד =====================
        val resolvedSingleTopic: String? =
            if (!isSingleTopicFilter) null
            else {
                val wanted = fixedFilter.trim()
                val wantedN = wanted.normHeb()

                val titlesFromBridge = runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt) }
                    .getOrDefault(emptyList())

                val titles = titlesFromBridge.ifEmpty { sharedTopicTitlesFor(belt) }

                when {
                    titles.isEmpty() -> wanted
                    titles.any { it.trim() == wanted } -> titles.first { it.trim() == wanted }
                    titles.any { it.trim().normHeb() == wantedN } -> titles.first { it.trim().normHeb() == wantedN }
                    else -> wanted
                }
            }
        // ================================================================
        android.util.Log.d("RandomPractice", "belt=${belt.id} filter='$fixedFilter' single=$isSingleTopicFilter resolved='$resolvedSingleTopic'")
        android.util.Log.d("RandomPractice", "sharedTopics=${sharedTopicTitlesFor(belt).size}")

        val built = il.kmi.shared.practice.PracticeFacade.buildPracticeItems(
            request = il.kmi.shared.practice.PracticeRequest(
                beltId = belt.id,
                topicFilter = requestFilterForFacade
            ),

            // ===================== ✅ FIX: TopicTitlesProvider =====================
            topicTitlesProvider = il.kmi.shared.practice.PracticeFacade.TopicTitlesProvider { beltId ->
                val b = Belt.fromId(beltId) ?: belt

                // ✅ אם זה תרגול של נושא יחיד — תחזיר רק אותו
                if (isSingleTopicFilter && b.id == belt.id) {
                    return@TopicTitlesProvider listOf(resolvedSingleTopic ?: fixedFilter.trim())
                }

                // ✅ קודם shared (האמת)
                val sharedTitles = sharedTopicTitlesFor(b)
                if (sharedTitles.isNotEmpty()) return@TopicTitlesProvider sharedTitles

                // ואז Bridge אם צריך
                runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(b) }
                    .getOrDefault(emptyList())
            },

            // =====================================================================

            itemsProvider = il.kmi.shared.practice.PracticeFacade.ItemsProvider { beltId, topicTitle ->
                val b = Belt.fromId(beltId) ?: belt

                // ✅ קודם shared (האמת)
                val sharedItems = sharedItemsFor(b, topicTitle, subTopicTitle = null)
                if (sharedItems.isNotEmpty()) return@ItemsProvider sharedItems

                // ואז Bridge אם צריך
                runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(b, topicTitle) }
                    .getOrDefault(emptyList())
            },
            setsProvider = il.kmi.shared.practice.PracticeFacade.SetProvider { key ->
                sp.getStringSet(key, emptySet()) ?: emptySet()
            },
            excludedProvider = il.kmi.shared.practice.PracticeFacade.ExcludedProvider { beltId, topicTitle, rawItem, disp ->
                val excluded = sp.getStringSet("excluded_${beltId}_${topicTitle}", emptySet()) ?: emptySet()
                (rawItem in excluded) || (disp in excluded)
            },
            canonicalKeyFor = { rawItem -> canonicalKeyFor(rawItem) },
            displayNameFor = { rawItem -> displayName(rawItem) }
        )

        if (!isSingleTopicFilter) {
            built
        } else {
            val want = (resolvedSingleTopic ?: fixedFilter).normHeb()
            // ✅ סינון רך לפי נושא (משווה נרמול)
            built.filter { it.topicTitle.normHeb() == want }
        }
    }

    // ✅ מפתח יציב לשמירת "לא יודע" (קאנוני) ב-SP לפי belt+filter
    val practiceKey = remember(topicFilter) {
        (topicFilter?.takeIf { it.isNotBlank() } ?: il.kmi.shared.practice.PracticeFilters.ALL)
            .replace(' ', '_')
    }

    // ✅ טעויות נשמרות כ-canonicalKeys (יציב, לא תלוי תצוגה)
    val wrongCanonicalKeys = remember(belt.id, practiceKey) {
        (sp.getStringSet("wrong_${belt.id}_$practiceKey", emptySet()) ?: emptySet())
            .toMutableSet()
    }

    fun addWrongForCurrent(item: il.kmi.shared.practice.PracticeItem?) {
        if (item == null) return
        if (wrongCanonicalKeys.add(item.canonicalKey)) {
            sp.edit().putStringSet("wrong_${belt.id}_$practiceKey", wrongCanonicalKeys).apply()
        }
    }

    // ✅ רשימה משוקללת דרך shared (Wrong first + weight)
    val weightedPracticeItems: List<il.kmi.shared.practice.PracticeItem> =
        remember(practiceItems, wrongCanonicalKeys) {
            il.kmi.shared.practice.PracticeFacade.buildWeightedOrder(
                items = practiceItems,
                wrongCanonicalKeys = wrongCanonicalKeys,
                wrongWeight = 3,
                seed = null
            )
        }

    // ✅ לתצוגה קיימת: Strings
    val weightedItems: List<String> = remember(weightedPracticeItems) {
        weightedPracticeItems.map { it.displayTitle }
    }

    var currentIndex by remember { mutableStateOf(0) }

// ✅ Guard: אם הרשימה השתנתה והאינדקס יצא מהטווח – נתקן בעדינות
    LaunchedEffect(weightedItems.size) {
        if (weightedItems.isEmpty()) {
            currentIndex = 0
        } else if (currentIndex !in weightedItems.indices) {
            currentIndex = 0
        }
    }

    // ----- הגדרות טיימר -----
    var durationMinutes by remember {
        mutableStateOf(
            practiceDurationMinutes.takeIf { it > 0 } ?: sp.getInt("timer_minutes", 3)
        )
    }
    var beepLast10State by remember { mutableStateOf(sp.getBoolean("beep_last10", beepLast10)) }
    var beepHalfTimeState by remember { mutableStateOf(sp.getBoolean("beep_half", true)) }
    var timeLeft by remember { mutableStateOf(durationMinutes * 60) }

    var isRunning by remember { mutableStateOf(false) }

    // 🔊 שליטה בהקראה
    var isMuted by remember { mutableStateOf(false) }
    var sessionStarted by rememberSaveable { mutableStateOf(false) }
    var lastSpokenIndex by remember { mutableStateOf(-1) }
    var halfAnnouncementDone by remember { mutableStateOf(false) }

    // ✅ Guard יציאה: ברגע שזה true — אין הקריאות/טיימרים/Callbacks
    var isExiting by rememberSaveable { mutableStateOf(false) }

    // === TTS (✅ גלובלי אחיד) ===
    LaunchedEffect(Unit) {
        KmiTtsManager.init(context)
    }

    // 🔊 נגן צלילים רב-פלטפורמי (Android + iOS)
    val soundPlayer = remember { PlatformSoundPlayer(context) }

    // ✅ יציאה בטוחה: עוצרים הכול *לפני* ניווט
    fun requestExit() {
        if (isExiting) return
        isExiting = true

        isRunning = false
        sessionStarted = false
        lastSpokenIndex = -1
        halfAnnouncementDone = false

        runCatching { KmiTtsManager.stop() }
        onBack()
    }

    // ניקוי משאבים כשעוזבים את המסך (✅ בלי כפילות)
    DisposableEffect(Unit) {
        onDispose {
            // לא shutdown כדי לא לפגוע במסכים אחרים; רק עוצרים
            runCatching { KmiTtsManager.stop() }
            runCatching { soundPlayer.release() }
        }
    }

    // ניגון LETSGO ואז פעולה כשמסתיים
    fun playLetsGo(onFinished: () -> Unit) {
        runCatching { soundPlayer.play("letsgo") }
        if (!isExiting) onFinished()
    }

    // ניגון STOP_REST ואז פעולה כשמסתיים
    fun playStopRest(onFinished: () -> Unit) {
        runCatching { soundPlayer.play("stop_rest") }
        if (!isExiting) onFinished()
    }

    fun beep(ms: Int = 120) {
        runCatching { soundPlayer.play("beep") }
    }

// בכל שינוי משך – מאפסים את הזמן הנותר
    LaunchedEffect(durationMinutes) {
        timeLeft = durationMinutes.coerceAtLeast(1) * 60
    }

    // ===== דיאלוג בחירת זמן =====
    var showDurationDialog by rememberSaveable { mutableStateOf(true) }
    if (showDurationDialog) {
        DurationPickerDialog(
            show = showDurationDialog,
            initMinutes = durationMinutes,
            initHalfAlert = beepHalfTimeState,
            initLast10 = beepLast10State,
            onDismiss = {
                requestExit() // ✅ יציאה בטוחה (לא נכנס לתרגיל ראשון)
            },
            onConfirm = { durationSeconds: Int, playHalf: Boolean, playCountdown: Boolean ->
                durationMinutes = (durationSeconds / 60).coerceAtLeast(1)
                beepHalfTimeState = playHalf
                beepLast10State = playCountdown

                sp.edit()
                    .putInt("timer_minutes", durationMinutes)
                    .putBoolean("beep_half", beepHalfTimeState)
                    .putBoolean("beep_last10", beepLast10State)
                    .apply()

                timeLeft = durationMinutes * 60
                currentIndex = 0
                lastSpokenIndex = -1
                halfAnnouncementDone = false
                sessionStarted = true
                isRunning = false          // לא מתחילים טיימר עדיין
                showDurationDialog = false

                // 🔊 קודם LETSGO, ואז מתחיל הטיימר והתרגיל הראשון מוקרא
                playLetsGo {
                    if (isExiting) return@playLetsGo
                    isRunning = true
                    if (currentIndex in weightedItems.indices) {
                        speak(weightedItems[currentIndex])
                        lastSpokenIndex = currentIndex
                    }
                }
            }
        )
    }

    // ===== טיימר =====
    LaunchedEffect(
        currentIndex,
        isRunning,
        durationMinutes,
        beepLast10State,
        beepHalfTimeState,
        sessionStarted,
        isExiting
    ) {
        if (isExiting) return@LaunchedEffect

        if (isRunning && sessionStarted && weightedItems.isNotEmpty()) {
            timeLeft = durationMinutes * 60

            if (currentIndex != lastSpokenIndex && currentIndex in weightedItems.indices) {
                speak(weightedItems[currentIndex])
                lastSpokenIndex = currentIndex
            }

            val halfTime = timeLeft / 2
            halfAnnouncementDone = false

            while (timeLeft > 0 && isRunning) {
                if (isExiting) return@LaunchedEffect
                delay(1000)
                timeLeft--

                if (!halfAnnouncementDone && beepHalfTimeState && timeLeft == halfTime) {
                    beep()
                    if (!isMuted) speak("עבר חצי מזמן התרגול")
                    halfAnnouncementDone = true
                }

                if (beepLast10State && timeLeft in 1..10) {
                    beep()
                }
            }

            if (!isExiting && timeLeft == 0 && currentIndex < weightedItems.lastIndex) {
                currentIndex++
            }
        }
    }

    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchResults by remember(searchQuery, globalSearchItems) {
        mutableStateOf(
            if (searchQuery.isBlank()) emptyList()
            else {
                val q = searchQuery.trim()
                globalSearchItems
                    .filter { (_, _, item) -> item.contains(q, ignoreCase = true) }
                    .take(50)
            }
        )
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = if (!topicFilter.isNullOrBlank() && topicFilter.startsWith("$TOPICS_PICK_TOKEN:")) {
                    "תרגול לפי נושא"
                } else {
                    "תרגול אקראי – ${belt.heb}"
                },
                onBack = null,
                showBottomActions = true,
                onHome = onHome,
                onSearch = { showSearch = true },
                lockSearch = false,
                showTopHome = false,
                showTopSearch = false
            )
        },
    ) { padding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = belt.lightColor
        ) {
            if (weightedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("אין תרגילים זמינים לנושא זה")

                        Spacer(Modifier.height(10.dp))

                        // ✅ דיבאג: עוזר לנו לוודא ש־topicFilter לא הגיע בטעות כחגורה/טקסט אחר
                        Text(
                            text = "debug: beltId=${belt.id} | belt=${belt.heb} | topicFilter='${topicFilter ?: ""}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                isRunning = !isRunning
                                if (isRunning && sessionStarted && currentIndex in weightedItems.indices) {
                                    speak(weightedItems[currentIndex])
                                    lastSpokenIndex = currentIndex
                                } else {
                                    KmiTtsManager.stop()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                        Icon(
                                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isRunning) "השהה" else "המשך"
                            )
                        }
                    }

                    if (currentIndex in weightedItems.indices) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp))
                                .clickable {
                                    // נפתח את דיאלוג ההסבר על התרגיל הנוכחי
                                    showHelp = true
                                }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = weightedItems[currentIndex],
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    ModernActionsRow(
                        onSkip = {
                            if (currentIndex < weightedItems.lastIndex) currentIndex++
                        },
                        onDontKnow = {
                            if (currentIndex in weightedItems.indices) {
                                addWrongForCurrent(weightedPracticeItems.getOrNull(currentIndex))
                            }
                            if (currentIndex < weightedItems.lastIndex) currentIndex++
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassHelpButton(
                            label = "עזרה",
                            onClick = { showHelp = true }
                        )

                        IconButton(onClick = {
                            isMuted = !isMuted
                            if (!isMuted && sessionStarted && currentIndex in weightedItems.indices) {
                                speak(weightedItems[currentIndex])
                                lastSpokenIndex = currentIndex
                            } else {
                                KmiTtsManager.stop()
                            }
                        }) {
                        Icon(
                                if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "המשך קול" else "השתק"
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFFE0E0E0),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    // ✅ עוצרים את הטיימר/הקראה עכשיו
                                    isRunning = false
                                    sessionStarted = false
                                    runCatching { KmiTtsManager.stop() }

                                    // ✅ לא נועלים isExiting כאן!
                                    // קודם צליל STOP_REST ואז חזרה למסך הקודם
                                    playStopRest {
                                        requestExit()
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                            ) {
                                Text(
                                    "סיום וחזרה",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }


        // ----- סדר עדיפויות לדיאלוגים -----
        // 1) אם נבחר תרגיל מהחיפוש – מציגים רק את ההסבר הזה
        // 2) אחרת אם נלחץ "עזרה" – מציגים עזרה לתרגיל הנוכחי
        // 3) אחרת אם פתוח חיפוש – מציגים חיפוש

        when {
            pickedSearchHit != null -> {
                val (b, t, item) = pickedSearchHit!!

                val explanation = remember(b, item, t) {
                    val direct = il.kmi.app.domain.Explanations.get(b, item).trim()
                    if (direct.isNotBlank()) direct
                    else {
                        val alt = item.substringAfter("::", item).substringAfter(":", item).trim()
                        il.kmi.app.domain.Explanations.get(b, alt).ifBlank {
                            "לא נמצא הסבר עבור \"$item\"."
                        }
                    }
                }

                // ✅ מועדפים – דרך FavoritesStore (ללא SP)
                val favId = remember(item) {
                    il.kmi.shared.questions.model.util.ExerciseTitleFormatter
                        .displayName(item)
                        .ifBlank { item }
                        .trim()
                }
                val isFav = item != null && favorites.contains(favId)

                fun toggleFav() {
                    if (item.isNullOrBlank()) return
                    // ✅ FIX: FavoritesStore כרגע גלובלי – אין toggleScoped
                    FavoritesStore.toggle(favId)
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = {
                        pickedSearchHit = null
                        showHelp = false
                        showSearch = false
                        searchQuery = ""
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            // ⭐ בצד שמאל
                            IconButton(
                                onClick = { toggleFav() },
                                modifier = Modifier.align(AbsoluteAlignment.CenterLeft)
                            ) {
                                if (isFav) {
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

                            // שם התרגיל + החגורה בצד ימין
                            Column(
                                modifier = Modifier
                                    .align(AbsoluteAlignment.CenterRight)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "(${b.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                pickedSearchHit = null
                                showHelp = false
                                showSearch = false
                                searchQuery = ""
                            }) {
                                Text("סגור")
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            showHelp -> {
                val item = weightedItems.getOrNull(currentIndex)

                val explanation = remember(belt, item) {
                    if (item.isNullOrBlank()) {
                        "לא נבחר תרגיל להצגה."
                    } else {
                        val raw = il.kmi.app.domain.Explanations.get(belt, item).trim()
                        if (raw.isNotBlank()) raw
                        else {
                            val alt = item.substringAfter("::", item).substringAfter(":", item).trim()
                            il.kmi.app.domain.Explanations.get(belt, alt).ifBlank {
                                "אין כרגע הסבר לתרגיל הזה."
                            }
                        }
                    }
                }

                // ✅ מועדפים גלובליים – source of truth אחד (FavoritesStore)
                val safeItem = item.orEmpty()

                val favId = remember(key1 = safeItem) {
                    il.kmi.shared.questions.model.util.ExerciseTitleFormatter
                        .displayName(safeItem)
                        .ifBlank { safeItem }
                        .trim()
                }

                val isFav = safeItem.isNotBlank() && favorites.contains(favId)

                fun toggleFav() {
                    if (safeItem.isBlank()) return
                    FavoritesStore.toggle(favId)
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { showHelp = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            IconButton(
                                onClick = { toggleFav() },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                if (isFav) {
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
                                    .align(Alignment.CenterEnd)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = item ?: "תרגיל",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "(${belt.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showHelp = false }) {
                                Text("סגור")
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            showSearch -> {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = {
                        showSearch = false
                        searchQuery = ""
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "חפש תרגיל (למשל: \"בעיטה\", \"הגנה\")",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("הקלד/י שם תרגיל") }
                        )

                        Spacer(Modifier.height(8.dp))

                        if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                            Text(
                                "לא נמצאו תרגילים תואמים.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                searchResults.forEach { (hitBelt, hitTopic, hitItem) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 1.dp,
                                        shadowElevation = 0.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pickedSearchHit = Triple(hitBelt, hitTopic, hitItem)
                                                showSearch = false
                                                searchQuery = ""
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = hitItem,
                                                style = MaterialTheme.typography.titleMedium,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "(${hitBelt.heb})",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                        }) { Text("סגור") }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }

// -------------------------------------------------------------------------------------

    if (showHelp) {
            val item = weightedItems.getOrNull(currentIndex)

            val favTopic = topicFilter?.takeIf { it.isNotBlank() } ?: "general"
            val favKey = "fav_${belt.id}_$favTopic"
            val favState = remember(showHelp, favKey) {
                mutableStateOf(
                    sp.getStringSet(favKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                )
            }
            val isFav = item != null && favState.value.contains(item)

            fun toggleFavorite() {
                if (item.isNullOrBlank()) return
                val set = favState.value.toMutableSet()
                if (!set.add(item)) set.remove(item)
                favState.value = set
                sp.edit().putStringSet(favKey, set).apply()
            }

            val explanation = if (item.isNullOrBlank()) {
                "לא נבחר תרגיל להצגה."
            } else {
                val raw = il.kmi.app.domain.Explanations.get(belt, item).trim()
                if (raw.isNotBlank()) raw
                else {
                    val alt = item.substringAfter("::", item).substringAfter(":", item).trim()
                    il.kmi.app.domain.Explanations.get(belt, alt).ifBlank {
                        "אין כרגע הסבר לתרגיל הזה."
                    }
                }
            }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showHelp = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        IconButton(
                            onClick = { toggleFavorite() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            if (isFav) {
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
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = item ?: "תרגיל",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "(${belt.heb})",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showHelp = false }) {
                            Text("סגור")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

// -------------------------------------------------------------------------------------

/* ===== דיאלוג בחירת זמן תרגול (Top-level) ===== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPickerDialog(
    show: Boolean,
    initMinutes: Int,
    initHalfAlert: Boolean,
    initLast10: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (durationSeconds: Int, playHalf: Boolean, playCountdown: Boolean) -> Unit
) {
    if (!show) return

    var selectedMin by remember { mutableStateOf(initMinutes.coerceIn(1, 60)) }
    var playHalf by remember { mutableStateOf(initHalfAlert) }
    var playCountdown by remember { mutableStateOf(initLast10) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // גרדיאנט רך שמתכנס לטון המותג
    val headerBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        dragHandle = {
            // ידית שקופה ונקייה
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 8.dp)
        ) {
            // ===== Header זכוכיתי עם טיימר גדול =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Transparent),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Box(
                    modifier = Modifier
                        .background(headerBrush)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "בחר זמן תרגול",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // תצוגת הזמן הנבחר (אנימציה דקה)
                        AnimatedContent(
                            targetState = selectedMin,
                            transitionSpec = {
                                (fadeIn(spring()) togetherWith fadeOut(spring()))
                                    .using(SizeTransform(clip = false))
                            },
                            label = "count-animation"
                        ) { m ->
                            Text(
                                text = String.format("%02d:00", m),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ===== בחירת זמן בסגמנטים עגולים =====
            SegmentedTimeChooser(
                values = listOf(1, 3, 5),
                selected = selectedMin,
                onSelect = { selectedMin = it }
            )

            Spacer(Modifier.height(10.dp))

            // ===== מתגים עם תיאור משנה =====
            SettingRow(
                title = "התראה באמצע הזמן",
                subtitle = "צפצוף + הודעה קולית בחצי הזמן",
                checked = playHalf,
                onCheckedChange = { playHalf = it }
            )
            SettingRow(
                title = "צליל ב־10 השניות האחרונות",
                subtitle = "צפצוף קצר כל שנייה עד לסיום",
                checked = playCountdown,
                onCheckedChange = { playCountdown = it }
            )

            Spacer(Modifier.height(6.dp))

            // ===== כפתורי פעולה – מודרניים ורחבים =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        scope.launch { runCatching { sheetState.hide() } }
                    },
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("בטל", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            onConfirm(selectedMin * 60, playHalf, playCountdown)
                            runCatching { sheetState.hide() }
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp)
                ) {
                    Text("התחל", fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

/* ---------- רכיבים קטנים ומלוטשים ---------- */

/** Segmented control בעיצוב עגול/מודרני (1,3,5 דקות) */
@Composable
private fun SegmentedTimeChooser(
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        values.sortedDescending().forEach { v ->
            val isSelected = selected == v
            val container = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                color = container,
                contentColor = content,
                tonalElevation = if (isSelected) 6.dp else 0.dp,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelect(v) }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        Text("דק׳", modifier = Modifier.alpha(0.9f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ====================== כפתורים חדשניים ====================== */

@Composable
private fun ModernActionsRow(
    onSkip: () -> Unit,
    onDontKnow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModernPillButton(
            text = "דלג",
            leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            container = MaterialTheme.colorScheme.primary,
            onClick = onSkip,
            modifier = Modifier.weight(1f)      // ✅ ה-weight עובר להורה (RowScope)
        )
        ModernPillButton(
            text = "לא יודע",
            leading = { Icon(Icons.Filled.Close, contentDescription = null) },
            container = Color(0xFF7F1D1D),
            overlayGradient = Brush.horizontalGradient(
                listOf(Color(0x33FFFFFF), Color(0x11FFFFFF), Color.Transparent)
            ),
            onClick = onDontKnow,
            modifier = Modifier.weight(1f)      // ✅ כאן גם
        )
    }
}

@Composable
private fun ModernPillButton(
    text: String,
    leading: @Composable (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    overlayGradient: Brush? = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.05f)
        )
    ),
    modifier: Modifier = Modifier,              // ✅ חדש
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = modifier                     // ✅ מקבלים מבחוץ
            .height(56.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (overlayGradient != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(overlayGradient)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun GlassHelpButton(
    label: String = "עזרה",
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    // “זכוכית”: שכבה חצי-שקופה + קו מתאר בהיר + צל עדין
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

/** שורת הגדרה עם סוויץ’ ותיאור משנה */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, thumbContent = {
                if (checked) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            })
        }
    }
}
