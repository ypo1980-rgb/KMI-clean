@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package il.kmi.app.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import il.kmi.app.ui.KmiTtsManager
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.collectAsState
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.domain.ContentRepo

@Composable
fun ExercisesTabsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onPractice: (Belt, String) -> Unit,
    subTopicFilter: String? = null,
    onHome: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val sp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }

// ⭐ Favorites גלובלי – source of truth אחד לכל האפליקציה
    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    fun readSet(key: String): MutableSet<String> =
        sp.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

    val allUnknownKeys = remember(belt.id) {
        sp.all.keys.filter { it.startsWith("unknown_${belt.id}_") }
    }

     // --- item list כמו ב-MaterialsScreen ---
    data class TopicItems(val topic: String, val items: Set<String>)

    // ✅ Source of truth דרך ה-Bridge (רץ בפועל על SharedContentRepo)
    fun String.normTitle(): String = this
        .replace("\u200F", "").replace("\u200E", "").replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('־', '-')
        .replace('–', '-')
        .trim()
        .lowercase()

    fun dec(s: String) =
        try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    val allTopicItems: List<TopicItems> = remember(belt, topic) {
        if (topic != "__ALL__") return@remember emptyList()

        val topicTitles = ContentRepo.listTopicTitles(belt)
        topicTitles.mapNotNull { tpTitle ->
            val title = tpTitle.trim()
            if (title.isBlank()) return@mapNotNull null

            val items = ContentRepo
                .listItemTitles(belt, title, subTopicTitle = null)
                .toSet()

            TopicItems(title, items)
        }
    }

    val itemList: List<String> = remember(belt, topic, subTopicFilter, allTopicItems) {

        if (topic == "__ALL__") {
            return@remember allTopicItems.flatMap { it.items }.distinct()
        }

        // אם יש סינון תת-נושא
        subTopicFilter?.takeIf { it.isNotBlank() }?.let { raw ->
            val subRaw = dec(raw)

            // 1) subTopic match (exact/loose) מתוך Bridge
            val subTitles = ContentRepo.listSubTopicTitles(belt, topic)
            val exact = subTitles.firstOrNull { it.normTitle() == subRaw.normTitle() }
            if (exact != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = exact)
                if (items.isNotEmpty()) return@remember items
            }

            val wanted = subRaw.normTitle()
            val loose = subTitles.firstOrNull { st ->
                val a = st.normTitle()
                a.startsWith(wanted) || wanted.startsWith(a) || a.contains(wanted) || wanted.contains(a)
            }
            if (loose != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = loose)
                if (items.isNotEmpty()) return@remember items
            }

            // 2) fallback: KmiSearchBridge (רק אם עדיין קיים אצלך)
            val bySubBridge = runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, subRaw) }
                .getOrDefault(emptyList())
            if (bySubBridge.isNotEmpty()) return@remember bySubBridge

            return@remember emptyList()
        }

        // ללא סינון תת-נושא: כל הפריטים של הנושא (כולל תתי נושאים)
        val byTopic = ContentRepo.listItemTitles(belt, topic, subTopicTitle = null)
        if (byTopic.isNotEmpty()) return@remember byTopic

        // fallback: bridge לפי נושא
        val byTopicBridge = runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, topic) }
            .getOrDefault(emptyList())
        if (byTopicBridge.isNotEmpty()) return@remember byTopicBridge

        emptyList()
    }

    // === KMI_SEARCH_INJECT: STATE — MUST BE ABOVE Scaffold ===
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var explainFromSearch by remember { mutableStateOf<String?>(null) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }   // ← תרגיל שנבחר מחיפוש כללי

    // --- מצב טאבים (0=הכל, 1=לא יודע, 2=מועדפים) — חייב להיות לפני ה-Scaffold ---
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    fun String.norm() = this
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .trim().lowercase()

    val searchResults by remember(searchQuery, itemList) {
        val q = searchQuery.norm()
        mutableStateOf(
            if (q.isBlank()) emptyList()
            else itemList.filter { it.norm().contains(q) }
        )
    }

    // מזהה תרגיל "אחיד" – בלי prefix של נושא וכו'
    fun normalizeItemId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    // סטטוסים מה-VM
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }

    // ✅ אם זה __ALL__ צריך לדעת לאיזה נושא שייך כל item כדי לקרוא סטטוס נכון מה-VM
    fun topicForRawItem(raw: String): String {
        if (topic != "__ALL__") return topic
        return allTopicItems.firstOrNull { it.items.contains(raw) }?.topic ?: topic
    }

    LaunchedEffect(belt, topic, subTopicFilter, itemList, allTopicItems) {
        itemStates.clear()
        itemList.forEach { raw ->
            val tp = topicForRawItem(raw)
            val v = runCatching { vm.getItemStatusNullable(belt, tp, raw) }.getOrNull()
                ?: runCatching { if (vm.isMastered(belt, tp, raw)) true else null }.getOrNull()
            itemStates[raw] = v
        }
    }

// ========= ⭐ / X =========
    val suffix = remember(topic, subTopicFilter) {
        if (subTopicFilter.isNullOrBlank()) topic else "${topic}__${subTopicFilter}"
    }


    var unknowns by remember(belt.id, topic, suffix, allUnknownKeys) {
        mutableStateOf(
            if (topic == "__ALL__") {
                allUnknownKeys
                    .flatMap { key -> readSet(key) }
                    .map { normalizeItemId(it) }
                    .toMutableSet()
            } else {
                readSet("unknown_${belt.id}_$suffix")
                    .map { normalizeItemId(it) }
                    .toMutableSet()
            }
        )
    }

    fun toggleFavorite(rawId: String) {
        val cleanId = normalizeItemId(rawId)
        FavoritesStore.toggle(cleanId)
    }

    /**
     * סימון/הסרה ממועדפים
     */
      /**
     * סימון/הסרה "לא יודע"
     */
    fun setUnknown(id: String, set: Boolean) {
        val cleanId = normalizeItemId(id)

        if (topic == "__ALL__") {
            unknowns = unknowns.toMutableSet().apply {
                if (set) add(cleanId) else remove(cleanId)
            }

            allTopicItems.forEach { ti ->
                if (ti.items.any { normalizeItemId(it) == cleanId }) {
                    val key = "unknown_${belt.id}_${ti.topic}"
                    val s = readSet(key).apply {
                        if (set) add(cleanId) else remove(cleanId)
                    }
                    sp.edit().putStringSet(key, s).apply()
                }
            }
        } else {
            val key = "unknown_${belt.id}_$suffix"
            val s = readSet(key).apply {
                if (set) add(cleanId) else remove(cleanId)
            }
            unknowns = s.map { normalizeItemId(it) }.toMutableSet()
            sp.edit().putStringSet(key, s).apply()
        }
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "כרטיסיות תרגילים",
                onHome = onHome,                       // ✅ אייקון בית עובד
                centerTitle = true,
                showTopHome = false,
                lockSearch = false,                    // ✅ חיפוש פעיל
                onPickSearchResult = { key -> pickedKey = key }, // תרגיל מהחיפוש
                extraActions = { }                     // אם אין אקשנים נוספים
            )
        },

        bottomBar = {
            Surface(
                color = Color(0xFFE0E0E0),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    // ← קובע את מקור התרגול לפי הטאב: הכל/לא-יודע/מועדפים
                    val practiceToken = when (selectedTab) {
                        1 -> "__UNKNOWN__"
                        2 -> "__FAVS_ALL__"
                        else -> {
                            // ✅ אם נכנסנו דרך נושא מסוים — מתרגלים את הנושא הזה
                            if (topic != "__ALL__") topic else "__ALL__"
                        }
                    }

                    ActionButton(
                        text = "תרגול",
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFF6F64FF),
                        onClick = { onPractice(belt, practiceToken) }
                    )
                    ActionButton(
                        text = "איפוס",
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFD32F2F),
                        onClick = {
                            scope.launch {
                                // איפוס סטטוסים בזיכרון הקומפוז
                                itemList.forEach { item -> itemStates[item] = null }

                                // ⭐ איפוס מועדפים גלובלי
                                FavoritesStore.clearAll()

                                // ❓ איפוס unknown – נשאר מקומי לפי חגורה/נושא
                                unknowns = mutableSetOf()

                                val editor = sp.edit()

                                if (topic == "__ALL__") {
                                    sp.all.keys
                                        .filter { it.startsWith("unknown_${belt.id}_") }
                                        .forEach { key -> editor.remove(key) }
                                } else {
                                    val singleUnknownKey = "unknown_${belt.id}_$suffix"
                                    editor.remove(singleUnknownKey)

                                    vm.clearTopic(belt, topic)
                                }

                                editor.apply()
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->

        // פונקציות עזר להסבר דרך החיפוש הכללי
        fun parseSearchKey(key: String): Triple<Belt, String, String> {
            fun dec(s: String): String =
                runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

            val parts0 = when {
                '|' in key  -> key.split('|', limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/' in key  -> key.split('/', limit = 3)
                else        -> listOf("", "", "")
            }
            val parts = (parts0 + listOf("", "", "")).take(3)
            val beltFromKey  = Belt.fromId(parts[0]) ?: belt
            val topicFromKey = dec(parts[1])
            val itemFromKey  = dec(parts[2])
            return Triple(beltFromKey, topicFromKey, itemFromKey)
        }

        fun findExplanationForHit(
            beltHit: Belt,
            rawItem: String,
            topicHit: String
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
                val got = Explanations.get(beltHit, candidate).trim()
                if (got.isNotBlank()
                    && !got.startsWith("הסבר מפורט על")
                    && !got.startsWith("אין כרגע")
                ) {
                    return if ("::" in got) got.substringAfter("::").trim() else got
                }
            }
            return "אין כרגע הסבר לתרגיל הזה."
        }

        // ===== טאבים "מקצה-לקצה" =====
        @Composable
        fun MetricFieldEdgeToEdge(
            title: String,
            number: Int,
            selected: Boolean,
            onClick: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            val baseBg    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            val selBg     = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            val borderCol = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            else MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = modifier
                    .height(64.dp)
                    .background(if (selected) selBg else baseBg, shape = RectangleShape)
                    .border(1.dp, borderCol, RectangleShape)
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        val allCount     = itemList.size
        // ✅ סטים נשמרים מנורמלים → צריך לספור לפי normalizeItemId(...)
        val unknownCount = itemList.count { normalizeItemId(it) in unknowns }
        val favCount     = itemList.count { normalizeItemId(it) in favorites }

        // ✅ DEBUG (זמני): כמה נושאים יש לפי ה-Bridge
        val dbgTopics = remember(belt) { ContentRepo.listTopicTitles(belt).size }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "debug: sharedTopics=$dbgTopics | itemList=${itemList.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricFieldEdgeToEdge(
                    title    = "הכל",
                    number   = allCount,
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = "לא יודע",
                    number   = unknownCount,
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = "מועדפים",
                    number   = favCount,
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            val filtered = when (selectedTab) {
                1 -> itemList.filter { normalizeItemId(it) in unknowns }
                2 -> itemList.filter { normalizeItemId(it) in favorites }
                else -> itemList
            }

// ✅ מפת “raw -> display” אחת, שמשמשת לכל ה-UI
            val displayByRaw = remember(filtered) {
                filtered.associateWith { raw ->
                    ExerciseTitleFormatter.displayName(raw).ifBlank { raw.trim() }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filtered.forEach { item ->
                    val bringer = remember { BringIntoViewRequester() }
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 1.15f else 1f, label = "scale")

                    val displayName = displayByRaw[item]
                        ?: ExerciseTitleFormatter.displayName(item).ifBlank { item.trim() }

                    // ✅ LTR רק לשורה כדי שהאייקון תמיד יהיה בשמאל
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .bringIntoViewRequester(bringer)
                                .clickable { explainFromSearch = item },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ אייקון מודרני/צבעוני בצד שמאל
                            FilledTonalIconButton(
                                onClick = {
                                    pressed = true
                                    explainFromSearch = item
                                    scope.launch { kotlinx.coroutines.delay(150); pressed = false }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(scale),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "הסבר"
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // ✅ הטקסט נשאר RTL
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = displayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    Divider()
                }
            }
        }

        // ===== דיאלוג הסבר מתוצאת חיפוש כללית (אייקון זכוכית מגדלת) =====
        pickedKey?.let { key ->
            val (hitBelt, hitTopic, hitItem) = parseSearchKey(key)
            val displayName = ExerciseTitleFormatter.displayName(hitItem)
            val explanation = remember(hitBelt, hitItem, hitTopic) {
                findExplanationForHit(hitBelt, hitItem, hitTopic)
            }
            val isFav = favorites.contains(normalizeItemId(hitItem))

            AlertDialog(
                onDismissRequest = { pickedKey = null },
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {

                        // ✅ הטקסט בצד ימין – עם אותו סטייל כמו במסך הקודם
                        Column(
                            modifier = Modifier
                                .align(androidx.compose.ui.AbsoluteAlignment.CenterRight)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,   // ← כותרת קטנה
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "(${hitBelt.heb})",
                                style = MaterialTheme.typography.labelSmall,   // ← כמו במסך הקודם
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // ⭐ הכוכבית בצד שמאל
                        IconButton(
                            onClick = { toggleFavorite(hitItem) },
                            modifier = Modifier
                                .align(androidx.compose.ui.AbsoluteAlignment.CenterLeft)
                        ) {
                            if (isFav) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "הסר ממועדפים",
                                    tint = Color(0xFFFFC107)
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.StarBorder,
                                    contentDescription = "הוסף למועדפים"
                                )
                            }
                        }
                    }
                },
                text = {
                    // טקסט עם הדגשה של "עמידת מוצא ..." עד פסיק/נקודה
                    val annotated = buildExplanationWithStanceHighlight(
                        source = explanation,
                        stanceColor = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = annotated,
                        textAlign = TextAlign.Right,
                        // צבע טקסט מותאם לדיאלוג (בהיר/כהה)
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = { pickedKey = null }) { Text("סגור") }
                }
            )
        }

        // ===== דיאלוג הסבר (לחיצה על שורה או אייקון info ברשימה) =====
        explainFromSearch?.let { item ->

            val displayName = ExerciseTitleFormatter.displayName(item)

            // ✅ TTS גלובלי אחיד
            LaunchedEffect(item) {
                KmiTtsManager.init(ctx)
            }
            DisposableEffect(item) {
                onDispose { KmiTtsManager.stop() }
            }

            // ✅ קודם לפי שם תצוגה (לרוב ככה שמור ב-Explanations), ואז fallback ל-raw
            val explanation = Explanations.get(belt, displayName)
                .ifBlank { Explanations.get(belt, item) }
                .ifBlank { "לא נמצא הסבר עבור \"$displayName\"." }

            val isFav = favorites.contains(normalizeItemId(item))

            AlertDialog(
                onDismissRequest = {
                    KmiTtsManager.stop()
                    explainFromSearch = null
                },
                title = {
                    // ✅ LTR רק לכותרת כדי שהכוכבית תהיה פיזית בצד שמאל
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {

                            // ⭐ שמאל פיזי
                            IconButton(
                                onClick = { toggleFavorite(item) },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                if (isFav) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "הסר ממועדפים",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים"
                                    )
                                }
                            }

                            // טקסט ימין (מחזירים RTL רק לטקסט)
                            androidx.compose.runtime.CompositionLocalProvider(
                                androidx.compose.ui.platform.LocalLayoutDirection provides
                                        androidx.compose.ui.unit.LayoutDirection.Rtl
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "(${belt.heb})",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val annotated = buildExplanationWithStanceHighlight(
                            source = explanation,
                            stanceColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            annotated,
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { KmiTtsManager.speak(explanation) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "השמע הסבר")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        KmiTtsManager.stop()
                        explainFromSearch = null
                    }) { Text("סגור") }
                }
            )
        }

    } // ✅ סוגר את Scaffold { padding -> ... }

} // ✅ סוגר את ExercisesTabsScreen(...)


// ========= כפתור מונפש לשימוש חוזר =========
@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "btnScale")
    val scope = rememberCoroutineScope()
    val contentOnContainer = if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true; onClick()
            scope.launch { kotlinx.coroutines.delay(150); pressed = false }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.scale(scale).height(56.dp).defaultMinSize(minWidth = 90.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentOnContainer)
    ) {
        Text(text, color = contentOnContainer, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    // אם אין בכלל "עמידת מוצא" – מחזירים טקסט רגיל
    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    // מחפשים סוף משפט: פסיק או נקודה אחרי "עמידת מוצא"
    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1   // כולל הפסיק/נקודה
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        // מה שלפני
        append(before)

        // המשפט של "עמידת מוצא ..." מודגש וצבוע
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

        // שאר ההסבר
        append(after)
    }
}
