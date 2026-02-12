package il.kmi.app.screens

import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.app.ui.color
import il.kmi.app.ui.lightColor
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import il.kmi.shared.domain.ContentRepo as SharedContentRepo

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
private fun uiDisplayName(topicTitle: String, rawItem: String): String {
    val cleaned = cleanItem(topicTitle, rawItem)
    return ExerciseTitleFormatter.displayName(cleaned)
        .ifBlank { cleaned }
        .trim()
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

    LaunchedEffect(belt, topic) {
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
            items.forEach { raw ->
                val canonicalId = canonicalFor(belt, tp, raw)
                val ok = runCatching { vm.isMastered(belt, topic = tp, item = canonicalId) }.getOrDefault(false)
                if (ok) d++
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
            Text(
                text = "$done מתוך $total",
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
    val sp = remember(ctx) {
        ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
    }

    val excludedKeySuffix = remember(topic, subTopicFilter) {
        if (subTopicFilter.isNullOrBlank()) topic else "${topic}__${subTopicFilter}"
    }

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showProgress by rememberSaveable { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // === רשימת פריטים לפי נושא (ישירות מה-shared ContentRepo) ===
    var itemsByTopic by remember(belt, topic, subTopicFilter) {
        mutableStateOf(LinkedHashMap<String, List<String>>())
    }

    LaunchedEffect(belt, topic, subTopicFilter) {
        val beltContent = beltContentFor(belt)
        val topics = beltContent?.topics.orEmpty()

        val allTitles = topics.map { it.title }
        val orderedTitles: List<String> =
            if (topic.isNotBlank()) listOf(topic) + allTitles.filterNot { norm(it) == norm(topic) }
            else allTitles

        val out = LinkedHashMap<String, List<String>>()
        orderedTitles.forEach { title ->
            val tObj = topics.firstOrNull { norm(it.title) == norm(title) }
            val items = if (tObj == null) emptyList()
            else (tObj.items + tObj.subTopics.flatMap { it.items }).distinct()
            out[title] = items
        }

        itemsByTopic = out
    }

    /**
     * ✅ masteredMap נשמר לפי (topicTitle, itemKey) כאשר itemKey = uiItemKey(...)
     * זה מה שמיישר 1:1 למסך התרגילים.
     */
    var masteredMap by remember(belt, itemsByTopic) {
        mutableStateOf<Map<Pair<String, String>, MarkState>>(emptyMap())
    }

    fun spKeyPart(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .trim()

    fun suffixFor(topicTitle: String): String {
        val t = spKeyPart(topicTitle)
        val st = subTopicFilter?.let(::spKeyPart).orEmpty()
        return if (st.isBlank()) t else "${t}__${st}"
    }

    fun unknownsFor(topicTitle: String): Set<String> {
        val suffix = suffixFor(topicTitle)
        return sp.getStringSet("unknown_${belt.id}_$suffix", emptySet()) ?: emptySet()
    }

    fun masteredFor(topicTitle: String): Set<String> {
        val suffix = suffixFor(topicTitle)
        return sp.getStringSet("mastered_${belt.id}_$suffix", emptySet()) ?: emptySet()
    }

    // 🔽 הוסף helper לנסות topic בכמה וריאציות (בתוך SummaryScreen)
    fun topicKeysToTry(topicTitle: String): List<String> {
        val a = topicTitle
        val b = topicKey(topicTitle)
        val list = mutableListOf(a, b)

        // אם זה אותו נושא כמו הפרמטר topic שנכנס למסך – נוסיף גם אותו
        if (norm(topicTitle) == norm(topic)) {
            list += topic
            list += topicKey(topic)
        }

        return list.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    fun buildLookupKeys(topicTitle: String, itemRaw: String): List<String> {
        val cleaned = cleanItem(topicTitle, itemRaw).trim()

        val withTopicPrefix = if (topicTitle.isNotBlank() && !cleaned.startsWith("$topicTitle::")) {
            "$topicTitle::$cleaned"
        } else cleaned

        val noTopicPrefix = if (topicTitle.isNotBlank() && cleaned.startsWith("$topicTitle::")) {
            cleaned.removePrefix("$topicTitle::").trim()
        } else cleaned

        return listOf(
            canonicalFor(belt, topicTitle, itemRaw).trim(),
            itemRaw.trim(),
            cleaned,
            withTopicPrefix,
            noTopicPrefix
        )
            .filter { it.isNotBlank() }
            .distinct()
    }

    LaunchedEffect(belt, itemsByTopic, subTopicFilter) {
        loadError = null

        try {
            val map = mutableMapOf<Pair<String, String>, MarkState>()

            suspend fun readNullable(topicTitle: String, key: String): Boolean? {
                return try {
                    vm.getItemStatusNullable(belt, topicTitle, key)
                } catch (_: Exception) {
                    null
                }
            }

            itemsByTopic.forEach { (topicTitle, items) ->
                val unknownSet = unknownsFor(topicTitle)
                val masteredSet = masteredFor(topicTitle)

                items.forEach { itemRaw ->
                    val canonicalId = canonicalFor(belt, topicTitle, itemRaw).trim()
                    val keysToTry = buildLookupKeys(topicTitle, itemRaw)

                    var v: Boolean? = null

                    val topicTry = topicKeysToTry(topicTitle)

// 1) קודם סטטוס מפורש YES/NO מה-VM – עם כמה topic אפשריים
                    run breaking@{
                        for (tp in topicTry) {
                            for (k in keysToTry) {
                                v = runCatching { vm.getItemStatusNullable(belt, tp, k) }.getOrNull()
                                if (v != null) return@breaking
                            }
                        }
                    }

// 2) fallback ל-mastered (✅) – עם כמה topic אפשריים
                    if (v == null) {
                        var mastered = false
                        run breaking2@{
                            for (tp in topicTry) {
                                for (k in keysToTry) {
                                    if (runCatching { vm.isMastered(belt, tp, k) }.getOrDefault(false)) {
                                        mastered = true
                                        return@breaking2
                                    }
                                }
                            }
                        }
                        v = if (mastered) true else null
                    }

                    // ✅ אם אין תשובה מה-VM אבל הפריט מסומן mastered => זה ✅
                    if (v == null && masteredSet.contains(canonicalId)) {
                        v = true
                    }

// ❌ אם אין תשובה מה-VM אבל הפריט מסומן unknown => זה ❌
                    if (v == null && unknownSet.contains(canonicalId)) {
                        v = false
                    }

                    val state = when (v) {
                        true  -> MarkState.YES
                        false -> MarkState.NO
                        null  -> MarkState.NONE
                    }

                    // חשוב: שומרים רק תחת canonicalId אחד (כדי שלא יהיו דריסות)
                    map[topicTitle to canonicalId] = state
                }
            }

            masteredMap = map
        } catch (e: Exception) {
            loadError = e.message ?: "שגיאה בקריאת הנתונים"
            masteredMap = emptyMap()
        }
    }

    // ✅ סטטיסטיקות לפי נושא (מבוסס uiItemKey)
    val topicStats: Map<String, Pair<Int, Int>> = remember(masteredMap, itemsByTopic, belt) {
        itemsByTopic.mapValues { (topicTitle, items) ->
            val total = items.size
            val done = items.count { itemRaw ->
                val canonicalId = canonicalFor(belt, topicTitle, itemRaw)
                masteredMap[topicTitle to canonicalId] == MarkState.YES
            }
            done to total
        }
    }

    val overallDone = topicStats.values.sumOf { it.first }
    val overallTotal = topicStats.values.sumOf { it.second }
    val overallPct = if (overallTotal <= 0) 0 else ((overallDone * 100f) / overallTotal).toInt()

    // === חיפוש/הסבר ===
    var explainFromSearch: Triple<Belt, String, String>? by rememberSaveable { mutableStateOf(null) }

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
            val pdf = createSummaryPdf(dir, belt, itemsByTopic, masteredMap)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", pdf
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                targetPackage?.let { setPackage(it) }
            }
            ctx.startActivity(android.content.Intent.createChooser(intent, "שיתוף דו\"ח סיכום"))
        }.onFailure {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "דו\"ח סיכום ${belt.heb}")
                targetPackage?.let { setPackage(it) }
            }
            runCatching {
                ctx.startActivity(android.content.Intent.createChooser(intent, "שיתוף"))
            }
        }
    }

    Scaffold(
        topBar = {
            val beltLabel = remember(belt) {
                val h = belt.heb.trim()
                if (h.startsWith("חגורה")) h else "חגורה $h"
            }

            il.kmi.app.ui.KmiTopBar(
                title = "סיכום $beltLabel - ${overallPct}%",
                onShare = { sharePdf(null) },
                onPickSearchResult = { key -> handlePickFromTopBar(key) },
                onShareWhatsApp = { sharePdf("com.whatsapp") },
                onHome = { onBackHome() },
                showBottomActions = true,
                extraActions = { },
                centerTitle = false,
                showTopHome = false
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                ElevatedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A2BEA),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        text = "חזרה למסך הנושאים",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        // ===== דיאלוג הסבר + מועדפים =====
        explainFromSearch?.let { (b, t, iRaw) ->
            // ✅ כאן כן משתמשים ב-canonical עבור Explanations בלבד
            val canonical = resolveCanonicalIdForExplanation(b, t, iRaw)

            val explanation = il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                val alt = canonical.substringAfter(":", canonical).trim()
                il.kmi.app.domain.Explanations.get(b, alt)
            }.ifBlank { "לא נמצא הסבר עבור \"$canonical\"." }

            val spFav = remember(ctx) {
                ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
            }
            val favKey = remember(b.id, t) { "fav_${b.id}_$t" }
            val favState = remember(favKey) {
                mutableStateOf(
                    spFav.getStringSet(favKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                )
            }
            val isFav = favState.value.contains(canonical)
            fun toggleFavorite() {
                val s = favState.value.toMutableSet()
                if (!s.add(canonical)) s.remove(canonical)
                favState.value = s
                spFav.edit().putStringSet(favKey, s).apply()
            }

            AlertDialog(
                onDismissRequest = { /* לא סוגרים בלחיצה בחוץ */ },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = true
                ),
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { toggleFavorite() },
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

                        Column(
                            modifier = Modifier
                                .align(AbsoluteAlignment.CenterRight)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = canonical,
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
                },
                text = {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        explainFromSearch = null
                        focusManager.clearFocus()
                    }) { Text("סגור") }
                }
            )

            androidx.activity.compose.BackHandler(enabled = true) {
                explainFromSearch = null
                focusManager.clearFocus()
            }
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
                    .padding(top = 4.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.End
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            showProgress = !showProgress
                            scope.launch { scroll.animateScrollTo(0) }
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Insights, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("התקדמות", style = MaterialTheme.typography.labelLarge)
                    }
                }

                loadError?.let { err ->
                    Text(
                        text = "שגיאה: $err",
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (showProgress) {
                    Spacer(Modifier.height(8.dp))

                    val beltLabel = remember(belt) {
                        val h = belt.heb.trim()
                        if (h.startsWith("חגורה")) h else "חגורה $h"
                    }

                    Text(
                        text = "מד התקדמות – $beltLabel",
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
                                contentDescription = "סגור מד התקדמות",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (itemsByTopic.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "לא נמצאו פריטים להצגה.\nבדוק שהחגורה/נושא קיימים ב-ContentRepo.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    } else {
                        itemsByTopic.forEach { (topicTitle, items) ->
                            val (done, total) = topicStats[topicTitle] ?: (0 to 0)
                            val pct = if (total > 0) (done * 100 / total) else 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$topicTitle – $pct%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF333333)
                                    )

                                    if (items.isEmpty()) {
                                        Text(
                                            text = "אין פריטים בנושא הזה.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        items.forEach { itemRaw ->
                                            val canonicalId = canonicalFor(belt, topicTitle, itemRaw)
                                            val state = masteredMap[topicTitle to canonicalId] ?: MarkState.NONE

                                            val (bg, fg, mark) = when (state) {
                                                MarkState.YES  -> Triple(Color(0xFF4CAF50), Color.White, "✓")
                                                MarkState.NO   -> Triple(Color(0xFFE53935), Color.White, "✗")
                                                MarkState.NONE -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), "—")
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        // בהסבר נשתמש ב-canonical (לא ב-key)
                                                        explainFromSearch = Triple(belt, topicTitle, itemRaw)
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(28.dp),
                                                    shape = CircleShape,
                                                    color = bg,
                                                    shadowElevation = 3.dp,
                                                    tonalElevation = 1.dp
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = mark,
                                                            color = fg,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                Text(
                                                    // ✅ תצוגה נקייה כמו במסך התרגילים
                                                    text = uiDisplayName(topicTitle, itemRaw),
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.weight(1f),
                                                    color = Color.Black
                                                )
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
    masteredMap: Map<Pair<String, String>, MarkState>
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        textAlign = android.graphics.Paint.Align.RIGHT
        color = android.graphics.Color.BLACK
    }

    var y = 50f
    canvas.drawText("דו״ח סיכום – ${belt.heb}", 550f, y, paint)
    y += 30f

    itemsByTopic.forEach { (topicTitle, items) ->
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 16f
        canvas.drawText("נושא: $topicTitle", 550f, y, paint)
        y += 22f

        paint.textSize = 13.5f
        items.forEach { itemRaw ->
            val canonicalId = canonicalFor(belt, topicTitle, itemRaw)

            // ✅ אותו מפתח בדיוק כמו ב-SummaryScreen (בלי candidates)
            val state = masteredMap[topicTitle to canonicalId] ?: MarkState.NONE

            val circleX = 60f
            val circleY = y - 7f

            val circleColor = when (state) {
                MarkState.YES  -> android.graphics.Color.rgb(76, 175, 80)
                MarkState.NO   -> android.graphics.Color.rgb(229, 57, 53)
                MarkState.NONE -> android.graphics.Color.rgb(189, 189, 189)
            }

            val markText = when (state) {
                MarkState.YES  -> "✓"
                MarkState.NO   -> "✘"
                MarkState.NONE -> "—"
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
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            val display = uiDisplayName(topicTitle, itemRaw)
            canvas.drawText(display, 530f, y, paint)

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
