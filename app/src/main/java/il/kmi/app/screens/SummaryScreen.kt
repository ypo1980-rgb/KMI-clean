package il.kmi.app.screens

import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.color
import il.kmi.app.ui.lightColor
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import java.io.File
import java.io.FileOutputStream
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.Activity
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.ExerciseExplanationResolver

/* ------------------------------ MarkState (3 states) ------------------------------ */

private enum class MarkState { YES, NO, NONE }

// ===== canonical בדיוק כמו MaterialsScreen =====

private fun cleanItem(topic: String, item: String): String {
    var s = item.trim()

    // ✅ אם הפריט מגיע עם prefix של topic:: — מסירים רק את ה-prefix הזה
    //    אבל לא "חותכים" לקטע האחרון, כדי לא ליצור התנגשויות ("ימין"/"שמאל")
    if (topic.isNotBlank() && s.startsWith("$topic::")) {
        s = s.removePrefix("$topic::").trim()
    }

    // ✅ חשוב: לא לעשות substringAfterLast("::") !
    // אם יש :: זה חלק מהייחודיות של הפריט (subTopic::item)

    s = s.replace(Regex("\\s+"), " ").trim()
    return s
}

private fun norm(s: String) = s
    .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
    .replace(Regex("[\u0591-\u05C7]"), "")
    .replace("[\\-–—:_]".toRegex(), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .lowercase()

// ✅ ADD
private fun topicKey(s: String): String = s
    .replace("\u200F","")
    .replace("\u200E","")
    .replace("\u00A0"," ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun normalizeStatusPart(s: String): String =
    s.replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun summaryLegacyStatusIdFor(
    belt: Belt,
    topicKey: String,
    index: Int,
    item: String
): String {
    val cleanItem = normalizeStatusPart(item)

    // מפתח ישן — נשאר רק כ-fallback זמני לסימונים ישנים
    return "status_${belt.id}_${topicKey}_${index}_${cleanItem}"
}

private fun summaryExerciseIdentityIdFor(
    belt: Belt,
    topicKey: String,
    topicTitle: String,
    index: Int,
    item: String
): String {
    val cleanOriginal = cleanItem(topicTitle, item).trim()

    val resolved = ExerciseIdentityRegistry.resolve(
        belt = belt,
        hebrewTitle = cleanOriginal,
        topicKey = topicKey
    )

    if (resolved.isKnown) {
        return resolved.id
    }

    // חייב להיות זהה ל-fallback שהגדרנו ב-MaterialsScreen
    return "${resolved.id}_row_$index"
}

private fun findCanonicalItem(b: Belt, t: String, displayItem: String): String? {
    val wanted = norm(displayItem)

    // 1) פריטים ישירים של נושא
    val direct = SharedContentRepo.getAllItemsFor(b, t, subTopicTitle = null)
    direct.firstOrNull { raw ->
        val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
        norm(disp) == wanted || norm(raw) == wanted
    }?.let { return it }

    // 2) פריטים מתוך תתי-נושאים
    val subs = SharedContentRepo.getSubTopicsFor(b, t)
    subs.forEach { st ->
        st.items.firstOrNull { raw ->
            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
            norm(disp) == wanted || norm(raw) == wanted
        }?.let { return it }
    }

    return null
}

private fun canonicalFor(belt: Belt, topicTitle: String, displayItem: String): String {
    val cleaned = cleanItem(topicTitle, displayItem)
    return findCanonicalItem(belt, topicTitle, cleaned) ?: cleaned
}

private fun resolveCanonicalIdForExplanation(belt: Belt, topicTitle: String, rawItemFromRepo: String): String {
    val displayKey = cleanItem(topicTitle, rawItemFromRepo)
    return findCanonicalItem(belt, topicTitle, displayKey) ?: displayKey
}

private fun beltContentFor(belt: Belt): SharedContentRepo.BeltContent? {
    // ✅ מקור אמת: shared ContentRepo.data
    return SharedContentRepo.data[belt]
}

/**
 * ✅ טקסט לתצוגה בלבד (כמו MaterialsScreen):
 * מנקה prefixים ומחזיר displayName מה-formatter.
 */

private fun canonicalFromRepo(topicTitle: String, rawItemFromRepo: String): String {
    return cleanItem(topicTitle, rawItemFromRepo).trim()
}

private fun uiDisplayName(topicTitle: String, rawItem: String): String {
    val cleaned = cleanItem(topicTitle, rawItem)
    return ExerciseTitleFormatter.displayName(cleaned)
        .ifBlank { cleaned }
        .trim()
}

private fun topicDisplayName(topicTitle: String, isEnglish: Boolean): String {
    val clean = topicTitle.trim()
    if (!isEnglish) return clean

    return ExerciseTitlesEn.get(clean)?.takeIf { it.isNotBlank() } ?: when (clean) {
        "כללי" -> "General"
        "עבודת ידיים" -> "Hand Strikes"
        "בעיטות" -> "Kicks"
        "שחרורים" -> "Releases"
        "הגנות" -> "Defences"
        "נפילות" -> "Break-Falls and Rolls"
        "קרקע" -> "Ground-Work"
        "כושר" -> "Fitness"
        "קוואלר" -> "Kavaler"
        else -> clean
    }
}

private fun exerciseDisplayNameForUi(topicTitle: String, rawItem: String, isEnglish: Boolean): String {
    val topicTrim = topicTitle.trim()

    fun normalizeForLookup(s: String): String =
        s.trim()
            .replace("–", "-")
            .replace("—", "-")
            .replace(" - ", " - ")
            .replace("- ", "-")
            .replace(" -", "-")
            .replace(Regex("\\s*/\\s*"), "/")
            .replace(Regex("\\s+"), " ")
            .trim()

    val cleaned = buildString {
        var s = rawItem.trim()

        if (topicTrim.isNotBlank() && s.startsWith("$topicTrim::")) {
            s = s.removePrefix("$topicTrim::").trim()
        }

        if (topicTrim.isNotBlank() && s.startsWith(topicTrim)) {
            s = s.removePrefix(topicTrim).trim()
            s = s.trimStart('-', '–', '—', ':').trim()
        }

        append(s)
    }

    val base = ExerciseTitleFormatter.displayName(cleaned)
        .ifBlank { cleaned }
        .trim()

    if (!isEnglish) return base

    val candidates = listOf(
        base,
        normalizeForLookup(base),
        cleaned,
        normalizeForLookup(cleaned),
        rawItem.trim(),
        normalizeForLookup(rawItem.trim()),
        rawItem.substringAfter("::", rawItem).trim(),
        normalizeForLookup(rawItem.substringAfter("::", rawItem).trim())
    ).distinct()

    val translated = candidates.firstNotNullOfOrNull { candidate ->
        ExerciseTitlesEn.get(candidate)?.takeIf { it.isNotBlank() }
    }

    return translated ?: base
}

/* ------------------------------ ProgressMeter ------------------------------ */

@Composable
fun ProgressMeter(
    vm: KmiViewModel,
    belt: Belt,
    topic: String? = null,
    modifier: Modifier = Modifier,
    meterSize: Dp = 180.dp,
    stroke: Dp = 14.dp
) {
    var done by remember(belt, topic) { mutableStateOf(0) }
    var total by remember(belt, topic) { mutableStateOf(0) }

    val marksVer by vm.marksVersion.collectAsState()

    LaunchedEffect(belt, topic, marksVer) {
        val beltContent = beltContentFor(belt)

        val titles: List<String> =
            if (topic.isNullOrBlank()) beltContent?.topics?.map { it.title }.orEmpty()
            else listOf(topic)

        var t = 0
        var d = 0

        titles.forEach { tp ->
            val tpObj = beltContent?.topics?.firstOrNull { norm(it.title) == norm(tp) }
            val items = if (tpObj == null) emptyList()
            else (tpObj.items + tpObj.subTopics.flatMap { it.items }).distinct()

            t += items.size

            // ✅ Snapshot מהיר ומדויק לנושא (ה-VM עושה canonicalTopicKey בפנים)
            val topicSnap = vm.getTopicStatusSnapshot(belt, tp)

            items.forEachIndexed { index, raw ->
                val statusId = summaryExerciseIdentityIdFor(
                    belt = belt,
                    topicKey = tp.trim(),
                    topicTitle = tp,
                    index = index,
                    item = raw
                )

                val legacyStatusId = summaryLegacyStatusIdFor(
                    belt = belt,
                    topicKey = tp.trim(),
                    index = index,
                    item = raw
                )

                if ((topicSnap[statusId] ?: topicSnap[legacyStatusId]) == true) d++
            }
        }

        total = t
        done = d
    }

    val pct: Int = if (total == 0) 0 else (done * 100 / total)
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val fg = belt.color
    val txt = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.size(meterSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = stroke.toPx()
            val sweep = 360f * (pct / 100f)

            drawArc(
                color = bg,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = fg,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = txt
            )
            val context = LocalContext.current
            val languageManager = remember { AppLanguageManager(context) }
            val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

            Text(
                text = if (isEnglish) "$done out of $total" else "$done מתוך $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!topic.isNullOrBlank()) {
                Text(
                    text = topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ------------------------------ SummaryScreen ------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String = "",
    subTopicFilter: String? = null,
    onBack: () -> Unit,
    onBackHome: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val notesSp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }

    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    var showProgress by rememberSaveable { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun noteSuffixFor(topicTitle: String): String {
        val cleanTopic = topicTitle.trim().ifBlank { "כללי" }
        return if (
            topic.isNotBlank() &&
            !subTopicFilter.isNullOrBlank() &&
            norm(cleanTopic) == norm(topic)
        ) {
            "${topic.trim()}__${subTopicFilter.trim()}"
        } else {
            cleanTopic
        }
    }

    fun statusTopicKeyFor(topicTitle: String): String {
        val cleanTopic = topicTitle.trim().ifBlank { "כללי" }

        return if (
            topic.isNotBlank() &&
            !subTopicFilter.isNullOrBlank() &&
            norm(cleanTopic) == norm(topic)
        ) {
            "${topic.trim()}__${subTopicFilter.trim()}"
        } else {
            cleanTopic
        }
    }

    fun noteKeyFor(topicTitle: String, itemId: String): String {
        return "note_${belt.id}_${noteSuffixFor(topicTitle)}_${cleanItem(topicTitle, itemId)}"
    }

    fun loadNote(topicTitle: String, itemId: String): String =
        notesSp.getString(noteKeyFor(topicTitle, itemId), "")?.trim().orEmpty()

    fun saveNote(topicTitle: String, itemId: String, value: String) {
        val key = noteKeyFor(topicTitle, itemId)
        with(notesSp.edit()) {
            if (value.isBlank()) {
                remove(key)
            } else {
                putString(key, value.trim())
            }
            apply()
        }
    }

    fun hasNote(topicTitle: String, itemId: String): Boolean =
        loadNote(topicTitle, itemId).isNotBlank()

    // ✅ במקום scope.launch מתוך onClick (שמייצר את השגיאה), עושים את זה כאן
    LaunchedEffect(showProgress) {
        if (showProgress) {
            scroll.animateScrollTo(0)
        }
    }

    // === רשימת פריטים לפי נושא (ישירות מה-shared ContentRepo) ===
    var itemsByTopic by remember(belt, topic, subTopicFilter) {
        mutableStateOf(LinkedHashMap<String, List<String>>())
    }

    LaunchedEffect(belt, topic, subTopicFilter) {
        val beltContent = beltContentFor(belt)
        val topics = beltContent?.topics.orEmpty()

        val out = LinkedHashMap<String, List<String>>()

        if (topic.isNotBlank() && !subTopicFilter.isNullOrBlank()) {
            val items = withContext(Dispatchers.Default) {
                SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topic.trim(),
                    subTopicTitle = subTopicFilter.trim()
                )
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

            out[topic.trim()] = items
            itemsByTopic = out
            return@LaunchedEffect
        }

        val allTitles = topics.map { it.title }
        val orderedTitles: List<String> =
            if (topic.isNotBlank()) listOf(topic) + allTitles.filterNot { norm(it) == norm(topic) }
            else allTitles

        orderedTitles.forEach { title ->
            val tObj = topics.firstOrNull { norm(it.title) == norm(title) }
            val items = if (tObj == null) emptyList()
            else (tObj.items + tObj.subTopics.flatMap { it.items }).distinct()
            out[title] = items
        }

        itemsByTopic = out
    }

    /**
     * ✅ masteredMap נשמר לפי (topicTitle, statusId)
     * statusId חייב להיות זהה ל-MaterialsScreen:
     * ExerciseIdentityRegistry / fallback legacy_row_index.
     * לא משתמשים כאן ב-canonicalId כדי למנוע הדבקת סימונים בין תרגילים דומים.
     */
    var masteredMap by remember(belt, itemsByTopic) {
        mutableStateOf<Map<Pair<String, String>, MarkState>>(emptyMap())
    }

    LaunchedEffect(belt, itemsByTopic, subTopicFilter, topic) {
        loadError = null
        try {
            // ✅ מחשבים ברקע כדי למנוע תקיעה של ה-UI
            val computed: Map<Pair<String, String>, MarkState> = withContext(Dispatchers.Default) {
                val map = mutableMapOf<Pair<String, String>, MarkState>()

                itemsByTopic.forEach { (topicTitle, items) ->

                    val statusTopicKey = if (
                        topic.isNotBlank() &&
                        !subTopicFilter.isNullOrBlank() &&
                        norm(topicTitle) == norm(topic)
                    ) {
                        "${topic.trim()}__${subTopicFilter.trim()}"
                    } else {
                        topicTitle.trim()
                    }

                    // ✅ Snapshot לפי אותו topicKey שבו MaterialsScreen שומר
                    val topicSnap = vm.getTopicStatusSnapshot(belt, statusTopicKey)

                    items.forEachIndexed { index, itemRaw ->
                        val canonicalId = canonicalFromRepo(topicTitle, itemRaw)

                        // ✅ אותו מפתח בדיוק כמו MaterialsScreen אחרי המעבר ל-ExerciseIdentityRegistry
                        val statusId = summaryExerciseIdentityIdFor(
                            belt = belt,
                            topicKey = statusTopicKey,
                            topicTitle = topicTitle,
                            index = index,
                            item = itemRaw
                        )

                        // fallback זמני רק למפתח status הישן — לא ל-canonicalId
                        val legacyStatusId = summaryLegacyStatusIdFor(
                            belt = belt,
                            topicKey = statusTopicKey,
                            index = index,
                            item = itemRaw
                        )

                        val v: Boolean? = topicSnap[statusId] ?: topicSnap[legacyStatusId]

                        val state = when (v) {
                            true  -> MarkState.YES
                            false -> MarkState.NO
                            null  -> MarkState.NONE
                        }

                        // ✅ שומרים לפי statusId, לא לפי canonicalId
                        map[topicTitle to statusId] = state
                    }
                }

                map
            }

            masteredMap = computed
        } catch (e: Exception) {
            loadError = e.message ?: tr("שגיאה בקריאת הנתונים", "Error reading data")
            masteredMap = emptyMap()
        }
    }

    // ✅ סטטיסטיקות לפי נושא (מבוסס canonicalFromRepo)
    val topicStats: Map<String, Pair<Int, Int>> = remember(
        masteredMap,
        itemsByTopic,
        topic,
        subTopicFilter
    ) {
        itemsByTopic.mapValues { (topicTitle, items) ->
            val total = items.size
            val statusTopicKey = statusTopicKeyFor(topicTitle)

            val done = items.withIndex().count { indexed ->
                val statusId = summaryExerciseIdentityIdFor(
                    belt = belt,
                    topicKey = statusTopicKey,
                    topicTitle = topicTitle,
                    index = indexed.index,
                    item = indexed.value
                )

                masteredMap[topicTitle to statusId] == MarkState.YES
            }

            done to total
        }
    }

    val overallDone = topicStats.values.sumOf { it.first }
    val overallTotal = topicStats.values.sumOf { it.second }
    val overallPct = if (overallTotal <= 0) 0 else ((overallDone * 100f) / overallTotal).toInt()

    // === חיפוש/הסבר ===
    var explainFromSearch: Triple<Belt, String, String>? by rememberSaveable { mutableStateOf(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteEditorTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val handlePickFromTopBar: (String) -> Unit = { key ->
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val resolved = runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()
        if (resolved != null) {
            explainFromSearch = Triple(resolved.belt, resolved.topicTitle, resolved.itemTitle)
        } else {
            val parts = when {
                '|' in key -> key.split('|', limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/' in key -> key.split('/', limit = 3)
                else -> listOf("", "", "")
            }.map(::dec)

            val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: belt
            val topicTitle = parts.getOrNull(1).orEmpty().trim()
            val itemTitleRaw = parts.getOrNull(2).orEmpty().trim()
            val itemTitle = cleanItem(topicTitle, itemTitleRaw)

            explainFromSearch = Triple(beltFromKey, topicTitle, itemTitle)
        }
    }


    // ✳️ שיתוף PDF
    val sharePdf: (String?) -> Unit = { targetPackage ->
        runCatching {
            val dir = File(ctx.cacheDir, "pdfs").apply { mkdirs() }
            val pdf = createSummaryPdf(
                dir = dir,
                belt = belt,
                itemsByTopic = itemsByTopic,
                masteredMap = masteredMap,
                isEnglish = isEnglish,
                topic = topic,
                subTopicFilter = subTopicFilter
            )
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", pdf
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                targetPackage?.let { setPackage(it) }
            }
            ctx.startActivity(android.content.Intent.createChooser(intent, tr("שיתוף דו\"ח סיכום", "Share summary report")))
        }.onFailure {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    if (isEnglish) "Summary report ${belt.id}" else "דו\"ח סיכום ${belt.heb}"
                )
                targetPackage?.let { setPackage(it) }
            }
            runCatching {
                ctx.startActivity(android.content.Intent.createChooser(intent, tr("שיתוף", "Share")))
            }
        }
    }

    Scaffold(
        topBar = {
            val beltLabel = remember(belt, isEnglish) {
                if (isEnglish) {
                    when (belt) {
                        Belt.WHITE -> "White Belt"
                        Belt.YELLOW -> "Yellow Belt"
                        Belt.ORANGE -> "Orange Belt"
                        Belt.GREEN -> "Green Belt"
                        Belt.BLUE -> "Blue Belt"
                        Belt.BROWN -> "Brown Belt"
                        Belt.BLACK -> "Black Belt"
                    }
                } else {
                    val h = belt.heb.trim()
                    if (h.startsWith("חגורה")) h else "חגורה $h"
                }
            }

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = if (isEnglish) "Summary $beltLabel - ${overallPct}%" else "סיכום $beltLabel - ${overallPct}%",
                onShare = { sharePdf(null) },
                onPickSearchResult = { key -> handlePickFromTopBar(key) },
                onShareWhatsApp = { sharePdf("com.whatsapp") },
                onHome = { onBackHome() },
                showBottomActions = true,
                extraActions = { },
                centerTitle = false,
                showTopHome = false,
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
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val shineAnim = rememberInfiniteTransition(label = "summaryButtonShine")
            val shineOffset by shineAnim.animateFloat(
                initialValue = -140f,
                targetValue = 900f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3200)
                ),
                label = "summaryButtonShineOffset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7F00FF),
                                        Color(0xFF3F51B5),
                                        Color(0xFF03A9F4)
                                    )
                                )
                            )
                            .graphicsLayer { clip = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { clip = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = shineOffset.dp)
                                    .width(52.dp)
                                    .fillMaxHeight(0.70f)
                                    .align(Alignment.CenterStart)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.14f),
                                                Color.White.copy(alpha = 0.30f),
                                                Color.White.copy(alpha = 0.14f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = tr("חזרה למסך הנושאים", "Back to topics screen"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        // ===== דיאלוג הסבר + מועדפים + הערה =====
        explainFromSearch?.let { (b, t, iRaw) ->
            val canonical = resolveCanonicalIdForExplanation(b, t, iRaw)

            val explanation = remember(b, t, iRaw, canonical, isEnglish) {
                val resolved = ExerciseExplanationResolver.get(
                    belt = b,
                    topic = t,
                    item = iRaw,
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
                    cleaned
                } else {
                    tr(
                        "לא נמצא הסבר עבור \"$canonical\".",
                        "No explanation was found for \"$canonical\"."
                    )
                }
            }

            val cleanFavId = cleanItem(t, canonical)
            val isFav = favorites.contains(cleanFavId)
            val noteText = remember(t, cleanFavId, notesRefreshKey) {
                loadNote(t, cleanFavId)
            }

            val dialogTitle = exerciseDisplayNameForUi(
                topicTitle = t,
                rawItem = canonical,
                isEnglish = isEnglish
            )

            val dialogBeltLabel = if (isEnglish) {
                when (b) {
                    Belt.WHITE -> "(White belt)"
                    Belt.YELLOW -> "(Yellow belt)"
                    Belt.ORANGE -> "(Orange belt)"
                    Belt.GREEN -> "(Green belt)"
                    Belt.BLUE -> "(Blue belt)"
                    Belt.BROWN -> "(Brown belt)"
                    Belt.BLACK -> "(Black belt)"
                }
            } else {
                "(${b.heb})"
            }

            ExerciseExplanationDialog(
                title = dialogTitle,
                beltLabel = dialogBeltLabel,
                explanation = explanation,
                noteText = noteText,
                isFavorite = isFav,
                accentColor = b.color,
                isEnglish = isEnglish,
                onDismiss = {
                    explainFromSearch = null
                    focusManager.clearFocus()
                },
                onEditNote = {
                    noteEditorTopic = t
                    noteEditorFor = cleanFavId
                    noteDraft = loadNote(t, cleanFavId)
                },
                onDeleteNote = {
                    saveNote(t, cleanFavId, "")
                    noteDraft = ""
                    notesRefreshKey++
                },
                onToggleFavorite = {
                    FavoritesStore.toggle(cleanFavId)
                }
            )
        }

        noteEditorFor?.let { item ->
            val noteTopic = noteEditorTopic ?: topic.ifBlank { "כללי" }

            ExerciseNoteEditorDialog(
                noteText = noteDraft,
                isEnglish = isEnglish,
                accentColor = belt.color,
                onNoteChange = { noteDraft = it },
                onDismiss = {
                    noteEditorFor = null
                    noteEditorTopic = null
                },
                onSave = {
                    val cleanNote = noteDraft.trim()
                    noteDraft = cleanNote

                    saveNote(noteTopic, item, cleanNote)

                    notesRefreshKey++
                    noteEditorFor = null
                    noteEditorTopic = null
                }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = belt.lightColor,
            contentColor = Color(0xFF1B1B1B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 2.dp)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.End
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val beltRes = when (belt) {
                        Belt.WHITE  -> il.kmi.app.R.drawable.belt_white
                        Belt.YELLOW -> il.kmi.app.R.drawable.belt_yellow
                        Belt.ORANGE -> il.kmi.app.R.drawable.belt_orange
                        Belt.GREEN  -> il.kmi.app.R.drawable.belt_green
                        Belt.BLUE   -> il.kmi.app.R.drawable.belt_blue
                        Belt.BROWN  -> il.kmi.app.R.drawable.belt_brown
                        Belt.BLACK  -> il.kmi.app.R.drawable.belt_black
                    }

                    val beltLabel = if (isEnglish) {
                        when (belt) {
                            Belt.WHITE -> "White Belt"
                            Belt.YELLOW -> "Yellow Belt"
                            Belt.ORANGE -> "Orange Belt"
                            Belt.GREEN -> "Green Belt"
                            Belt.BLUE -> "Blue Belt"
                            Belt.BROWN -> "Brown Belt"
                            Belt.BLACK -> "Black Belt"
                        }
                    } else {
                        belt.heb
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.88f),
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            belt.color.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = beltRes),
                                contentDescription = beltLabel,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Surface(
                        onClick = { showProgress = !showProgress },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.88f),
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            belt.color.copy(alpha = 0.16f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = null,
                                tint = belt.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tr("התקדמות", "Progress"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }

                loadError?.let { err ->
                    Text(
                        text = tr("שגיאה: $err", "Error: $err"),
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (showProgress) {
                    Spacer(Modifier.height(8.dp))

                    val beltLabel = remember(belt, isEnglish) {
                        if (isEnglish) {
                            when (belt) {
                                Belt.WHITE -> "White Belt"
                                Belt.YELLOW -> "Yellow Belt"
                                Belt.ORANGE -> "Orange Belt"
                                Belt.GREEN -> "Green Belt"
                                Belt.BLUE -> "Blue Belt"
                                Belt.BROWN -> "Brown Belt"
                                Belt.BLACK -> "Black Belt"
                            }
                        } else {
                            val h = belt.heb.trim()
                            if (h.startsWith("חגורה")) h else "חגורה $h"
                        }
                    }

                    Text(
                        text = tr("מד התקדמות – $beltLabel", "Progress meter - $beltLabel"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF535353),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                    ) {
                        ProgressMeter(
                            vm = vm,
                            belt = belt,
                            topic = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(200.dp),
                            meterSize = 200.dp,
                            stroke = 16.dp
                        )

                        IconButton(
                            onClick = { showProgress = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("סגור מד התקדמות", "Close progress meter"),
                                tint = Color(0xFF444444)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (itemsByTopic.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = tr(
                                    "לא נמצאו פריטים להצגה עבור החגורה או הנושא שנבחרו.",
                                    "No items were found for the selected belt or topic."
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.98f),
                                                belt.color.copy(alpha = 0.06f),
                                                Color.White.copy(alpha = 0.95f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(16.dp),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                            )
                        }
                    } else {
                        itemsByTopic.forEach { (topicTitle, items) ->
                            val (done, total) = topicStats[topicTitle] ?: (0 to 0)
                            val pct = if (total > 0) (done * 100 / total) else 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.98f),
                                                    belt.color.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.95f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = belt.color.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(28.dp)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            belt.color,
                                                            belt.color.copy(alpha = 0.60f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        Text(
                                            text = if (isEnglish)
                                                "${topicDisplayName(topicTitle, true)} - $pct%"
                                            else
                                                "$topicTitle – $pct%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFF263238)
                                        )
                                    }

                                    if (items.isEmpty()) {
                                        Text(
                                            text = tr("אין פריטים בנושא הזה.", "No items in this topic."),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        items.forEachIndexed { index, itemRaw ->
                                            val canonicalId = canonicalFromRepo(topicTitle, itemRaw)

                                            val statusId = summaryExerciseIdentityIdFor(
                                                belt = belt,
                                                topicKey = statusTopicKeyFor(topicTitle),
                                                topicTitle = topicTitle,
                                                index = index,
                                                item = itemRaw
                                            )

                                            val state = masteredMap[topicTitle to statusId] ?: MarkState.NONE

                                            val (bg, fg, mark) = when (state) {
                                                MarkState.YES  -> Triple(Color(0xFF4CAF50), Color.White, "✓")
                                                MarkState.NO   -> Triple(Color(0xFFE53935), Color.White, "✗")
                                                MarkState.NONE -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), "○")
                                            }

                                            val cleanFavId = cleanItem(topicTitle, canonicalId)
                                            val isFav = favorites.contains(cleanFavId)
                                            val itemHasNote = hasNote(topicTitle, cleanFavId)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = if (state == MarkState.YES) {
                                                            belt.color.copy(alpha = 0.08f)
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .border(
                                                        width = if (state == MarkState.YES) 1.dp else 0.dp,
                                                        color = if (state == MarkState.YES) {
                                                            belt.color.copy(alpha = 0.12f)
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(24.dp),
                                                    shape = CircleShape,
                                                    color = bg,
                                                    shadowElevation = 2.dp,
                                                    tonalElevation = 0.dp
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = mark,
                                                            color = fg,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(8.dp))

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                                ) {
                                                    Text(
                                                        text = exerciseDisplayNameForUi(topicTitle, itemRaw, isEnglish),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = Color(0xFF1B1B1B)
                                                    )

                                                    if (itemHasNote) {
                                                        Text(
                                                            text = tr("יש הערה שמורה", "Saved note exists"),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(8.dp))

                                                var menuExpanded by remember { mutableStateOf(false) }
                                                val infoScale by animateFloatAsState(
                                                    targetValue = if (menuExpanded) 1.08f else 1f,
                                                    animationSpec = tween(180),
                                                    label = "summaryInfoScale"
                                                )
                                                val infoRotation by animateFloatAsState(
                                                    targetValue = if (menuExpanded) 12f else 0f,
                                                    animationSpec = tween(180),
                                                    label = "summaryInfoRotation"
                                                )

                                                Box {

                                                    Surface(
                                                        onClick = { menuExpanded = true },
                                                        shape = CircleShape,
                                                        color = Color.White.copy(alpha = 0.94f),
                                                        shadowElevation = 6.dp,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            belt.color.copy(alpha = 0.16f)
                                                        ),
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .graphicsLayer {
                                                                scaleX = infoScale
                                                                scaleY = infoScale
                                                            }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    brush = Brush.verticalGradient(
                                                                        colors = listOf(
                                                                            Color.White.copy(alpha = 0.98f),
                                                                            belt.color.copy(alpha = 0.06f),
                                                                            Color.White.copy(alpha = 0.94f)
                                                                        )
                                                                    ),
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Info,
                                                                contentDescription = tr("אפשרויות", "Options"),
                                                                tint = Color(0xFF455A64),
                                                                modifier = Modifier
                                                                    .size(18.dp)
                                                                    .graphicsLayer {
                                                                        rotationZ = infoRotation
                                                                    }
                                                            )
                                                        }
                                                    }

                                                    DropdownMenu(
                                                        expanded = menuExpanded,
                                                        onDismissRequest = { menuExpanded = false },
                                                        modifier = Modifier
                                                            .background(
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        Color.White.copy(alpha = 0.99f),
                                                                        belt.color.copy(alpha = 0.05f),
                                                                        Color.White.copy(alpha = 0.97f)
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(18.dp)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                belt.color.copy(alpha = 0.12f),
                                                                RoundedCornerShape(18.dp)
                                                            )
                                                    ) {

                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    tr("מידע", "Info"),
                                                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            },
                                                            onClick = {
                                                                menuExpanded = false
                                                                explainFromSearch = Triple(belt, topicTitle, itemRaw)
                                                            }
                                                        )

                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    if (isFav)
                                                                        tr("הסר ממועדפים", "Remove from favorites")
                                                                    else
                                                                        tr("הוסף למועדפים", "Add to favorites"),
                                                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            },
                                                            onClick = {
                                                                menuExpanded = false
                                                                FavoritesStore.toggle(cleanFavId)
                                                            }
                                                        )

                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    tr("הוסף הערה למתרגל", "Add note for trainee"),
                                                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            },
                                                            onClick = {
                                                                menuExpanded = false
                                                                noteEditorTopic = topicTitle
                                                                noteEditorFor = cleanFavId
                                                                noteDraft = loadNote(topicTitle, cleanFavId)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ------------------------------ PDF: Summary ------------------------------ */

private fun createSummaryPdf(
    dir: File,
    belt: Belt,
    itemsByTopic: Map<String, List<String>>,
    masteredMap: Map<Pair<String, String>, MarkState>,
    isEnglish: Boolean = false,
    topic: String = "",
    subTopicFilter: String? = null
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val pageWidth = 595f
    val leftX = 45f
    val rightX = 550f
    val textX = if (isEnglish) leftX else rightX

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
        color = android.graphics.Color.BLACK
    }

    var y = 50f
    canvas.drawText(
        if (isEnglish) "Summary report - ${belt.id.replaceFirstChar { it.uppercase() }}" else "דו״ח סיכום – ${belt.heb}",
        textX,
        y,
        paint
    )
    y += 30f

    itemsByTopic.forEach { (topicTitle, items) ->
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 16f
        val topicLabel = if (isEnglish) topicDisplayName(topicTitle, true) else topicTitle
        canvas.drawText(if (isEnglish) "Topic: $topicLabel" else "נושא: $topicTitle", textX, y, paint)
        y += 22f

        paint.textSize = 13.5f
        items.forEachIndexed { index, itemRaw ->
            val statusTopicKey = if (
                topic.isNotBlank() &&
                !subTopicFilter.isNullOrBlank() &&
                norm(topicTitle) == norm(topic)
            ) {
                "${topic.trim()}__${subTopicFilter.trim()}"
            } else {
                topicTitle.trim()
            }

            val statusId = summaryExerciseIdentityIdFor(
                belt = belt,
                topicKey = statusTopicKey,
                topicTitle = topicTitle,
                index = index,
                item = itemRaw
            )

            // ✅ אותו מפתח בדיוק כמו ב-SummaryScreen / MaterialsScreen
            val state = masteredMap[topicTitle to statusId] ?: MarkState.NONE

            val circleX = if (isEnglish) pageWidth - 60f else 60f
            val circleY = y - 7f

            val circleColor = when (state) {
                MarkState.YES  -> android.graphics.Color.rgb(76, 175, 80)
                MarkState.NO   -> android.graphics.Color.rgb(229, 57, 53)
                MarkState.NONE -> android.graphics.Color.rgb(189, 189, 189)
            }

            val markText = when (state) {
                MarkState.YES  -> "✓"
                MarkState.NO   -> "✘"
                MarkState.NONE -> "○"
            }

            val markTextColor = when (state) {
                MarkState.NONE -> android.graphics.Color.rgb(97, 97, 97)
                else -> android.graphics.Color.WHITE
            }

            val circlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = circleColor
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(circleX, circleY, 8f, circlePaint)

            paint.color = android.graphics.Color.BLACK
            paint.textAlign = if (isEnglish) {
                android.graphics.Paint.Align.LEFT
            } else {
                android.graphics.Paint.Align.RIGHT
            }
            val display = exerciseDisplayNameForUi(topicTitle, itemRaw, isEnglish)
            canvas.drawText(display, textX, y, paint)

            val markPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = markTextColor
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.SANS_SERIF,
                    android.graphics.Typeface.BOLD
                )
            }
            canvas.drawText(markText, circleX, circleY + 4f, markPaint)

            y += 22f
            if (y > 790f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        y += 10f
        if (y > 790f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
    }

    document.finishPage(page)

    val file = File(dir, "summary_${belt.id}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}