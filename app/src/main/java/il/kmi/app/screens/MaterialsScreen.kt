package il.kmi.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.highlightItem
import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import il.kmi.app.ui.color
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.English.ExerciseExplanationsEn
import il.kmi.app.subscription.KmiAccess

//=================================================================================

@Composable
private fun BeltPill(
    belt: Belt,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.surface
    val stroke = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    fun beltDrawableRes(b: Belt): Int = when (b) {
        Belt.WHITE  -> R.drawable.belt_white
        Belt.YELLOW -> R.drawable.belt_yellow
        Belt.ORANGE -> R.drawable.belt_orange
        Belt.GREEN  -> R.drawable.belt_green
        Belt.BLUE   -> R.drawable.belt_blue
        Belt.BROWN  -> R.drawable.belt_brown
        Belt.BLACK  -> R.drawable.belt_black
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = beltDrawableRes(belt)),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun premiumSurfaceGradientForBelt(belt: Belt): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.98f),
            belt.color.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.94f)
        )
    )
}

@Composable
private fun topicTitleForUi(title: String, lang: AppLanguage): String {
    val clean = title.trim()
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(clean)
    } else {
        clean
    }
}

private fun itemTitleForUi(topic: String, rawItem: String, lang: AppLanguage): String {
    val topicTrim = topic.trim()

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

    val display = ExerciseTitleFormatter.displayName(cleaned).ifBlank {
        CanonicalIds.uiDisplayName(topicTrim, rawItem).trim()
    }.trim()

    if (lang != AppLanguage.ENGLISH) return display

    val candidates = listOf(
        display,
        normalizeForLookup(display),
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

    return translated ?: display
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MaterialsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onBack: () -> Unit,
    // היה: onSummary: (Belt) -> Unit,
    onSummary: (Belt, String, String?) -> Unit,
    onPractice: (Belt, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit,
    subTopicFilter: String? = null,
    onOpenSubscription: () -> Unit = {}
) {

    val context = LocalContext.current
    val langManager = remember { AppLanguageManager(context) }
    val currentLang = langManager.getCurrentLanguage()
    val isEnglish = currentLang == AppLanguage.ENGLISH

    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }

    // ✅ גורם למסך להתרענן אחרי סימון יודע/לא יודע ממסכים אחרים, כולל RandomPracticeScreen
    val marksVersion by vm.marksVersion.collectAsState()

    val accessSp = remember(context) {
        context.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val hasFullAccess = remember(accessSp, marksVersion) {
        KmiAccess.hasFullAccess(accessSp)
    }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }

    var explainTriple by remember { mutableStateOf<Triple<Belt, String, String>?>(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val explanationTextColor = if (isDarkSurface) Color.White else Color.Black

    // ✅ NEW: נושא לתצוגה/קאנוניקליזציה — כדי ש"" יתנהג בדיוק כמו "כללי"
    val topicUi = remember(topic) { if (topic.isBlank()) "כללי" else topic }

    val topicKey = remember(topicUi, subTopicFilter) {
        if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
    }

    // ✅ תרגול נעול לכל הנושאים אם אין מנוי פעיל.
    // החומר עצמו יכול להיפתח לפי הלוגיקה הקיימת, אבל כפתור "תרגול" דורש מנוי.
    val isPracticeLocked = remember(hasFullAccess) {
        !hasFullAccess
    }

    // ===== canonical (✅ מקור אמת אחד לכל האפליקציה) =====
    fun canonicalFor(displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicUi, displayItem)

    fun canonicalFor(topicTitle: String, displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicTitle, displayItem)

    fun cleanItem(topicTitle: String, item: String): String =
        CanonicalIds.cleanItem(topicTitle, item)
    // ===== סוף canonical =====

    val handlePickFromTopBar: (String) -> Unit = { key ->
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val r = runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()
        if (r != null) {
            explainTriple = Triple(r.belt, r.topicTitle, r.itemTitle)
        } else {
            val parts = when {
                '|'  in key -> key.split('|',  limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/'  in key -> key.split('/',  limit = 3)
                else        -> listOf("", "", "")
            }.map(::dec)

            val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: belt
            val topicFromKey = parts.getOrNull(1).orEmpty().ifBlank { topicUi }
            val itemRaw = cleanItem(topicFromKey, parts.getOrNull(2).orEmpty())
            explainTriple = Triple(beltFromKey, topicFromKey, itemRaw)
        }
    }

    // === שליפת התרגילים (כולל subTopicFilter) ===
    // ✅ Cache בזיכרון כדי שמעבר בין נושאים שכבר נפתחו יהיה מיידי
    val itemsCache = rememberSaveable { mutableMapOf<String, List<String>>() }
    fun itemsCacheKey(): String = buildString {
        append(belt.id)
        append("||")
        append(topicUi.trim())
        append("||")
        append(subTopicFilter?.trim().orEmpty())
    }

    val itemList by produceState<List<String>>(
        initialValue = itemsCache[itemsCacheKey()] ?: emptyList(),
        key1 = belt.id,
        key2 = topic,
        key3 = subTopicFilter
    ) {
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val key = itemsCacheKey()

        // אם כבר בקאש — חוזרים מיד בלי חישוב
        itemsCache[key]?.let {
            value = it
            return@produceState
        }

        value = withContext(Dispatchers.Default) {
            val topicTrim = topicUi.trim()

            val subTrim = subTopicFilter
                ?.takeIf { it.isNotBlank() }
                ?.let { raw -> dec(raw).trim() }

            val list = if (subTrim != null) {
                SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTrim,
                    subTopicTitle = subTrim
                )
            } else {
                SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTrim,
                    subTopicTitle = null
                )
            }

            list.distinct()
        }

        itemsCache[key] = value
    }

    // ✅ אם כמה שורות מקבלות אותו canonicalId, אסור שסימון יודע/לא יודע ישתמש באותו מפתח.
    // canonicalId נשאר להסברים / מועדפים / הערות.
    // statusId משמש רק לסימונים.
    val duplicatedCanonicalIds = remember(itemList, belt.id, topicUi) {
        itemList
            .map { item -> canonicalFor(item) }
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun statusIdFor(index: Int, item: String): String {
        val cleanItem = normalizeStatusPart(item)

        // ✅ מזהה סימון ייחודי תמיד לפי:
        // חגורה + נושא/תת־נושא מדויק + מיקום השורה + שם השורה.
        // לא משתמשים ב-canonicalId לסימונים, כי תרגילים דומים עלולים להידבק דרך נרמול/מפתחות ישנים.
        return "status_${belt.id}_${topicKey}_${index}_${cleanItem}"
    }

    // הדגשת תרגיל (✅ בלי Reflection: זה top-level flow)
    val highlight by highlightItem.collectAsState(initial = null)

    // ✅ NEW: נרמול אחיד למפתחות SP (כדי שסיכום ותוכן יקראו את אותו מפתח)
    fun spKeyPart(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .trim()

    val excludedKeySuffix = remember(topicUi, subTopicFilter) {
        val t = spKeyPart(topicUi)
        val st = subTopicFilter?.let(::spKeyPart).orEmpty()
        if (st.isBlank()) t else "${t}__${st}"
    }

    val excludedItems = remember { mutableStateListOf<String>() }
    LaunchedEffect(belt, excludedKeySuffix) {
        excludedItems.clear()
        excludedItems.addAll(
            sp.getStringSet("excluded_${belt.id}_$excludedKeySuffix", emptySet()) ?: emptySet()
        )
    }
    fun toggleExclude(item: String) {
        // ✅ item כאן כבר canonicalId
        if (excludedItems.contains(item)) excludedItems.remove(item) else excludedItems.add(item)

        sp.edit()
            .putStringSet("excluded_${belt.id}_$excludedKeySuffix", excludedItems.toSet())
            .apply()
    }

    // ⬇️ מועדפים / הערות נשארים ב-SP (לא קשור לסימונים)
    val favKey = remember(belt.id, excludedKeySuffix) { "fav_${belt.id}_$excludedKeySuffix" }
    var favorites by remember(favKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(favKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun toggleFavorite(id: String) {
        val s = favorites.toMutableSet()
        if (!s.add(id)) s.remove(id)
        favorites = s
        sp.edit().putStringSet(favKey, s).apply()
    }

    // ✅ סימונים (✓/✗/—) — מקור אמת יחיד: ViewModel/DataStore

    val unknownKey = remember(belt.id, excludedKeySuffix) { "unknown_${belt.id}_$excludedKeySuffix" }
    var unknowns by remember(unknownKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(unknownKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun setUnknown(id: String, set: Boolean) {
        val s = unknowns.toMutableSet()
        if (set) s.add(id) else s.remove(id)
        unknowns = s
        sp.edit().putStringSet(unknownKey, s).apply()
    }
// ✅ NEW: נשמור גם mastered (וי ירוק) ב-SP כדי שהסיכום יראה אותו
    val masteredKey = remember(belt.id, excludedKeySuffix) { "mastered_${belt.id}_$excludedKeySuffix" }
    var masteredSet by remember(masteredKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(masteredKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun setMasteredLocal(id: String, set: Boolean) {
        val s = masteredSet.toMutableSet()
        if (set) s.add(id) else s.remove(id)
        masteredSet = s
        sp.edit().putStringSet(masteredKey, s).apply()
    }

    // (SharedPreferences) הערות חופשיות לכל תרגיל – בלי excludedKeySuffix גלובלי
    fun loadNote(itemId: String): String {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        return sp.getString(key, "") ?: ""
    }

    fun saveNote(itemId: String, value: String) {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        with(sp.edit()) {
            if (value.isBlank()) {
                remove(key)
            } else {
                putString(key, value)
            }
            apply()
        }
    }

    // טעינת מצבי שליטה — מקור אמת יחיד: VM/DataStore
    // ✅ טוען את כל הסימונים למפה זמנית ורק בסוף מעדכן UI,
    // כדי למנוע מצב ביניים שבו רוב השורות מצוירות כ-null.
    LaunchedEffect(belt, topicUi, subTopicFilter, itemList, marksVersion) {

        // ✅ חשוב:
        // produceState מתחיל לפעמים עם itemList ריק, ואז נטען שוב עם הרשימה האמיתית.
        // אם ננקה את itemStates בזמן שהרשימה ריקה — כל הוי/איקס נעלמים רגעית.
        if (itemList.isEmpty()) {
            return@LaunchedEffect
        }

        val nextStates = mutableMapOf<String, Boolean?>()

        itemList.forEachIndexed { index, item ->
            val canonicalId = canonicalFor(item)
            val statusId = statusIdFor(index, item)

            // ✅ בתת־נושא קוראים רק מהמפתח המדויק.
            // אחרת סימונים ישנים מ־topicUi / כללי עלולים לסמן אוטומטית תרגילים אחרים.
            val topicKeysToRead = if (subTopicFilter.isNullOrBlank()) {
                listOf(
                    topicKey,
                    topicUi,
                    "כללי"
                )
            } else {
                listOf(topicKey)
            }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            var vFromVm: Boolean? = null
            var matchedKey: String? = null

            for (key in topicKeysToRead) {
                val valueFromKey: Boolean? =
                    runCatching {
                        vm.getItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = statusId
                        )
                    }.getOrNull()
                        ?: runCatching {
                            if (
                                vm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = statusId
                                )
                                ) true else null
                        }.getOrNull()

                if (valueFromKey != null) {
                    vFromVm = valueFromKey
                    matchedKey = key
                    break
                }
            }

            // ✅ fallback לשמירה המקומית הישנה
            val localFallback: Boolean? = when {
                masteredSet.contains(statusId) -> true
                unknowns.contains(statusId) -> false
                else -> null
            }

            val finalValue = vFromVm ?: localFallback

            // ✅ ריפוי VM רק אם מצאנו ערך מקומי אבל ה־VM ריק.
            // בתת־נושא כותבים רק למפתח המדויק כדי לא לזהם את topicUi / כללי.
            if (vFromVm == null && finalValue != null) {
                val topicKeysToHeal = if (subTopicFilter.isNullOrBlank()) {
                    listOf(
                        topicKey,
                        topicUi,
                        "כללי"
                    )
                } else {
                    listOf(topicKey)
                }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                topicKeysToHeal.forEach { key ->
                    vm.setItemStatusNullable(
                        belt = belt,
                        topic = key,
                        item = statusId,
                        value = finalValue
                    )
                }
            }

            // ✅ שומרים זמנית לפי statusId בלבד כדי למנוע סימון כפול בין תרגילים דומים.
            nextStates[statusId] = finalValue
        }

        // ✅ עדכון UI פעם אחת אחרי שכל הרשימה נטענה
        itemStates.clear()
        itemStates.putAll(nextStates)
    }

    Scaffold(
        topBar = {
            val headerTitle =
                if (subTopicFilter.isNullOrBlank()) {
                    topicTitleForUi(topic, currentLang)
                } else {
                    "${topicTitleForUi(topic, currentLang)} - ${topicTitleForUi(subTopicFilter, currentLang)}"
                }

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = headerTitle,
                onHome = onOpenHome,
                // לא רוצים אייקון בית עליון כי הוא כבר קיים
                showTopHome = false,
                showRoleStatus = false,      // מבטל את תג "מאמן" בצד
                centerTitle = true,
                alignTitleEnd = false,
                showBottomActions = true,
                onPickSearchResult = { key -> handlePickFromTopBar(key) },
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
        bottomBar = {
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    belt.color.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.94f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = belt.color.copy(alpha = 0.14f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedButton(
                                text = when {
                                    isPracticeLocked && isEnglish -> "🔒 Practice"
                                    isPracticeLocked -> "🔒 תרגול"
                                    isEnglish -> "Practice"
                                    else -> "תרגול"
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = if (isPracticeLocked) {
                                    Color(0xFF9A7A22)
                                } else {
                                    belt.color.copy(alpha = 0.92f)
                                },
                                onClick = {
                                    if (isPracticeLocked) {
                                        onOpenSubscription()
                                    } else {
                                        onPractice(belt, topicUi)
                                    }
                                }
                            )

                            AnimatedButton(
                                text = if (isEnglish) "Reset" else "איפוס",
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFB3261E),
                                onClick = {
                                    scope.launch {
                                        vm.clearTopic(belt, topicKey)
                                        itemList.forEachIndexed { index, item ->
                                            val statusId = statusIdFor(index, item)
                                            itemStates[statusId] = null
                                        }

                                        excludedItems.clear()

                                        val keysToClear = if (subTopicFilter.isNullOrBlank()) {
                                            listOf(
                                                topicKey,
                                                topicUi,
                                                "כללי"
                                            )
                                        } else {
                                            listOf(topicKey)
                                        }
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                            .distinct()

                                        val editor = sp.edit()
                                            .remove("excluded_${belt.id}_$excludedKeySuffix")
                                            .remove("fav_${belt.id}_$excludedKeySuffix")

                                        keysToClear.forEach { key ->
                                            editor
                                                .remove("mastered_${belt.id}_$key")
                                                .remove("unknown_${belt.id}_$key")
                                        }

                                        editor.apply()

                                        favorites = mutableSetOf()
                                        masteredSet = mutableSetOf()
                                        unknowns = mutableSetOf()
                                    }
                                }
                            )
                        }

                        AnimatedButton(
                            text = if (isEnglish) "Summary Screen" else "מסך סיכום",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFF1F2937),
                            onClick = { onSummary(belt, topicUi, subTopicFilter) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
    Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = belt.lightColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {

            // ===== דיאלוג הסבר בעקבות חיפוש =====
            explainTriple?.let { (b, t, iRaw) ->
                fun norm2(s: String) = s.replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
                    .replace(Regex("[\u0591-\u05C7]"), "")
                    .replace("[\\-–—:_]".toRegex(), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim().lowercase()

                fun cleanItem2(topic2: String, item2: String): String {
                    var s = item2.trim()

                    // ✅ מסירים רק prefix של topic2::
                    if (topic2.isNotBlank() && s.startsWith("$topic2::")) {
                        s = s.removePrefix("$topic2::").trim()
                    }

                    // ✅ חשוב: לא לחתוך substringAfterLast("::")

                    s = s.replace(Regex("\\s+"), " ").trim()
                    return s
                }

                fun findCanonicalItem2(belt2: Belt, topic2: String, displayItem2: String): String? {
                    val wanted = norm2(displayItem2)
                    val topicTrim2 = topic2.trim()

                    val all = SharedContentRepo.getAllItemsFor(
                        belt = belt2,
                        topicTitle = topicTrim2,
                        subTopicTitle = null
                    )

                    all.firstOrNull { raw ->
                        val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                        norm2(disp) == wanted || norm2(raw) == wanted
                    }?.let { return it }

                    val subs = SharedContentRepo.getSubTopicsFor(belt2, topicTrim2)
                    subs.forEach { st ->
                        st.items.firstOrNull { raw ->
                            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                            norm2(disp) == wanted || norm2(raw) == wanted
                        }?.let { return it }
                    }

                    return null
                }

                val canonical = CanonicalIds.resolveCanonicalForExplanation(b, t, iRaw)

                val explanation = remember(b, canonical, isEnglish) {
                    val alt = canonical.substringAfter(":", canonical).trim()

                    if (isEnglish) {
                        ExerciseExplanationsEn.get(b, canonical).let { main ->
                            if (main.startsWith("Detailed explanation for:")) {
                                ExerciseExplanationsEn.get(b, alt)
                            } else {
                                main
                            }
                        }
                    } else {
                        il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                            il.kmi.app.domain.Explanations.get(b, alt)
                        }
                    }
                }.ifBlank {
                    if (isEnglish) {
                        "No explanation found for \"$canonical\"."
                    } else {
                        "לא נמצא הסבר עבור \"$canonical\"."
                    }
                }

                val dialogTitle = itemTitleForUi(
                    topic = t,
                    rawItem = canonical,
                    lang = currentLang
                )

                val dialogBeltLabel = if (isEnglish) {
                    "(${b.en} belt)"
                } else {
                    "(${b.heb})"
                }

                ExerciseExplanationDialog(
                    title = dialogTitle,
                    beltLabel = dialogBeltLabel,
                    explanation = explanation,
                    noteText = loadNote(canonical),
                    isFavorite = favorites.contains(canonical),
                    accentColor = b.color,
                    isEnglish = isEnglish,
                    onDismiss = { explainTriple = null },
                    onEditNote = {
                        noteEditorFor = canonical
                        noteDraft = loadNote(canonical)
                    },
                    onToggleFavorite = { toggleFavorite(canonical) }
                )
            }
        // ===== סוף הדיאלוג =====

        noteEditorFor?.let { itemId ->
            PremiumExerciseNoteDialog(
                isEnglish = isEnglish,
                noteText = noteDraft,
                onNoteChange = { noteDraft = it },
                onDismiss = { noteEditorFor = null },
                onSave = {
                    saveNote(itemId, noteDraft)
                    noteEditorFor = null
                },
                onDelete = {
                    noteDraft = ""
                    saveNote(itemId, "")
                    noteEditorFor = null
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
            val header = if (subTopicFilter.isNullOrBlank()) {
                if (isEnglish) {
                    "Material: ${topicTitleForUi(topicUi, currentLang)}"
                } else {
                    "חומר: $topicUi"
                }
            } else {
                if (isEnglish) {
                    "Material: ${topicTitleForUi(topicUi, currentLang)} – ${topicTitleForUi(subTopicFilter, currentLang)}"
                } else {
                    "חומר: $topicUi – $subTopicFilter"
                }
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                ) {
                    val beltRes: Int = when (belt) {
                        Belt.WHITE  -> R.drawable.belt_white
                        Belt.YELLOW -> R.drawable.belt_yellow
                        Belt.ORANGE -> R.drawable.belt_orange
                        Belt.GREEN  -> R.drawable.belt_green
                        Belt.BLUE   -> R.drawable.belt_blue
                        Belt.BROWN  -> R.drawable.belt_brown
                        Belt.BLACK  -> R.drawable.belt_black
                    }

                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.70f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = belt.color.copy(alpha = 0.18f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = beltRes),
                            contentDescription = if (isEnglish) {
                                "${belt.en} belt"
                            } else {
                                "חגורה ${belt.heb}"
                            },
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

                Divider(
                    color = belt.color.copy(alpha = 0.14f),
                    thickness = 1.dp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(belt.lightColor)
                        .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        horizontalAlignment = Alignment.End
                    ) {

                        val filtered = itemList
                        filtered.forEachIndexed { index, item ->
                            var showNoteDialog by remember { mutableStateOf(false) }

                            // ✅ מזהה אחיד להסבר / מועדפים / הערות
                            val canonicalId = remember(item, belt.id, topicUi) { canonicalFor(item) }

                            // ✅ מזהה לסימון יודע/לא יודע בלבד.
                            // אם canonicalId כפול בין כמה שורות, statusId מפריד ביניהן לפי מיקום השורה.
                            val statusId = remember(index, item, duplicatedCanonicalIds, belt.id, topicKey, topicUi) {
                                statusIdFor(index, item)
                            }

                            // ✅ טקסט לתצוגה בלבד
                            val displayName = remember(item, topicUi, currentLang) {
                                itemTitleForUi(topicUi, item, currentLang)
                            }

                            var noteText by remember(item, belt.id, excludedKeySuffix) {
                                mutableStateOf(loadNote(canonicalId))
                            }

                            val mastered = itemStates[statusId] ?: when {
                                masteredSet.contains(statusId) -> true
                                unknowns.contains(statusId) -> false
                                else -> null
                            }

                            val isExcluded = excludedItems.contains(canonicalId)
                            val isHighlighted = highlight != null && canonicalId == highlight

                            val bringer = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                            LaunchedEffect(isHighlighted) {
                                if (isHighlighted) {
                                    kotlinx.coroutines.delay(120)
                                    bringer.bringIntoView()
                                }
                            }

                            var pressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 1.2f else 1f,
                                label = "scaleAnim"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .bringIntoViewRequester(bringer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 58.dp)
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ItemFloatingActions(
                                        isEnglish = isEnglish,
                                        excluded = isExcluded,
                                        isFav = favorites.contains(canonicalId),
                                        hasNote = noteText.isNotBlank(),
                                        onToggleExclude = { toggleExclude(canonicalId) },
                                        onInfo = {
                                            pressed = true
                                            explainTriple = Triple(belt, topicUi, canonicalId)
                                            scope.launch {
                                                kotlinx.coroutines.delay(150)
                                                pressed = false
                                            }
                                        },
                                        onToggleFavorite = { toggleFavorite(canonicalId) },
                                        onEditNote = { showNoteDialog = true }
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = displayName,
                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        color = when {
                                            isExcluded -> Color.Gray
                                            isHighlighted -> belt.color.copy(alpha = 0.95f)
                                            else -> Color(0xFF111827)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    key(statusId, mastered) {
                                        MasterToggle(
                                            mastered = mastered,
                                            onSelect = { newVal ->
                                                itemStates[statusId] = newVal

                                                // ✅ במסך תת־נושא שומרים רק למפתח המדויק.
                                                // אחרת סימון בתת־נושא עלול להשפיע על תרגילים אחרים דרך topicUi / כללי.
                                                val topicKeysToWriteToVm = if (subTopicFilter.isNullOrBlank()) {
                                                    listOf(
                                                        topicKey,
                                                        topicUi,
                                                        "כללי"
                                                    )
                                                } else {
                                                    listOf(topicKey)
                                                }
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }
                                                    .distinct()

                                                topicKeysToWriteToVm.forEach { key ->
                                                    vm.setItemStatusNullable(
                                                        belt = belt,
                                                        topic = key,
                                                        item = statusId,
                                                        value = newVal
                                                    )
                                                }

                                                // ✅ שמירה מקומית תואמת לסיכום/מסכים ישנים
                                                setMasteredLocal(statusId, newVal == true)
                                                setUnknown(statusId, newVal == false)

                                                // ✅ בתת־נושא שומרים מקומית רק למפתח המדויק.
                                                // במסך נושא רגיל נשארת שמירה רחבה לסנכרון עם תרגול.
                                                val localKeysToWrite = if (subTopicFilter.isNullOrBlank()) {
                                                    listOf(
                                                        topicKey,
                                                        topicUi,
                                                        "כללי"
                                                    )
                                                } else {
                                                    listOf(topicKey)
                                                }
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }
                                                    .distinct()

                                                localKeysToWrite.forEach { key ->
                                                    val masteredKeyForPractice = "mastered_${belt.id}_${key}"
                                                    val unknownKeyForPractice = "unknown_${belt.id}_${key}"

                                                    val masteredSetForPractice =
                                                        (sp.getStringSet(masteredKeyForPractice, emptySet()) ?: emptySet())
                                                            .toMutableSet()

                                                    val unknownSetForPractice =
                                                        (sp.getStringSet(unknownKeyForPractice, emptySet()) ?: emptySet())
                                                            .toMutableSet()

                                                    when (newVal) {
                                                        true -> {
                                                            masteredSetForPractice.add(statusId)
                                                            unknownSetForPractice.remove(statusId)
                                                        }

                                                        false -> {
                                                            unknownSetForPractice.add(statusId)
                                                            masteredSetForPractice.remove(statusId)
                                                        }

                                                        null -> {
                                                            masteredSetForPractice.remove(statusId)
                                                            unknownSetForPractice.remove(statusId)
                                                        }
                                                    }

                                                    sp.edit()
                                                        .putStringSet(masteredKeyForPractice, masteredSetForPractice)
                                                        .putStringSet(unknownKeyForPractice, unknownSetForPractice)
                                                        .apply()
                                                }
                                            }
                                        )
                                    }
                                }

                                Divider(
                                    color = belt.color.copy(alpha = 0.30f),
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp)
                                )
                            }

                            // דיאלוג הערה
                            if (showNoteDialog) {
                                PremiumExerciseNoteDialog(
                                    isEnglish = isEnglish,
                                    noteText = noteText,
                                    onNoteChange = { noteText = it },
                                    onDismiss = { showNoteDialog = false },
                                    onSave = {
                                        saveNote(canonicalId, noteText)
                                        showNoteDialog = false
                                    },
                                    onDelete = {
                                        noteText = ""
                                        saveNote(canonicalId, "")
                                        showNoteDialog = false
                                    }
                                )
                            }

                            Spacer(Modifier.height(0.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumExerciseNoteDialog(
    isEnglish: Boolean,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val noteTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val noteHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(30.dp),
        title = null,
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = Color.White.copy(alpha = 0.98f),
                shadowElevation = 18.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF7E57C2).copy(alpha = 0.16f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF7F2FF),
                                    Color.White
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = noteHorizontalAlignment,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Exercise Note" else "הערה על התרגיל",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = noteTextAlign,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = if (isEnglish) {
                            "Write a personal note that will stay attached to this exercise."
                        } else {
                            "כתוב הערה אישית שתישמר לתרגיל הזה"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = noteTextAlign,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shadowElevation = 7.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF7E57C2).copy(alpha = 0.18f)
                        )
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = onNoteChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            minLines = 4,
                            maxLines = 7,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = noteTextAlign,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            ),
                            placeholder = {
                                Text(
                                    text = if (isEnglish) "Write a free note" else "הקלד הערה חופשית",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = noteTextAlign,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color(0xFF7E57C2)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFF7E57C2).copy(alpha = 0.24f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.78f),
                                contentColor = Color(0xFF6D5BA6)
                            )
                        ) {
                            Text(
                                text = if (isEnglish) "Cancel" else "בטל",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B3FA6),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Text(
                                text = if (isEnglish) "Save" else "שמור",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    if (noteText.isNotBlank() && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEnglish) "Delete note" else "מחק הערה",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB3261E)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

// ===== כפתור מונפש =====
@Composable
fun AnimatedButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "buttonScaleAnim"
    )
    val scope = rememberCoroutineScope()

    val contentOnContainer =
        if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true
            onClick()
            scope.launch {
                kotlinx.coroutines.delay(140)
                pressed = false
            }
        },
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .defaultMinSize(minWidth = 90.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.24f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentOnContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            color = contentOnContainer,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ItemFloatingActions(
    isEnglish: Boolean,
    excluded: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onToggleExclude: () -> Unit,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    var expanded by remember { mutableStateOf(false) }
    var helpSeen by remember { mutableStateOf(sp.getBoolean("exclude_help_seen", false)) }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "materialsInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "materialsInfoRotation"
    )

    LaunchedEffect(expanded) {
        if (expanded && !helpSeen) {
            helpSeen = true
            sp.edit().putBoolean("exclude_help_seen", true).apply()
        }
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = Color(0xFF60717A),
            shadowElevation = 3.dp,
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .size(27.dp)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = infoRotation
                    }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.99f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.97f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(18.dp)
                )
        ) {

            if (!helpSeen) {
                DropdownMenuItem(
                    enabled = false,
                    text = {
                        Text(
                            text = if (isEnglish) {
                                "What does “Exclude” mean?\nRemoves this exercise from practice for the selected topic."
                            } else {
                                "מה זה “החרג”?\nמנטרל את התרגיל מהתרגול של הנושא הנבחר."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                        )
                    },
                    onClick = {}
                )
                Divider()
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isEnglish) "Info" else "מידע",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                },
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = when {
                            isEnglish && isFav -> "Remove from favorites"
                            isEnglish -> "Add to favorites"
                            isFav -> "הסר ממועדפים"
                            else -> "הוסף למועדפים"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                },
                onClick = {
                    expanded = false
                    onToggleFavorite()
                    android.widget.Toast
                        .makeText(
                            context,
                            when {
                                isEnglish && isFav -> "Removed from favorites."
                                isEnglish -> "Added to favorites."
                                isFav -> "הוסר מהמועדפים."
                                else -> "נוסף למועדפים."
                            },
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = when {
                            isEnglish && excluded -> "Cancel exclusion"
                            isEnglish -> "Exclude from practice"
                            excluded -> "בטל החרגה"
                            else -> "החרג (מנטרל מהתרגול)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                },
                onClick = {
                    expanded = false
                    onToggleExclude()
                    android.widget.Toast
                        .makeText(
                            context,
                            when {
                                isEnglish && excluded -> "Exclusion canceled. The exercise will return to practice."
                                isEnglish -> "Exercise excluded. It will not appear in this topic practice."
                                excluded -> "בוטלה ההחרגה – התרגיל יחזור לתרגול."
                                else -> "התרגיל הוחרג – לא יופיע בתרגול הנושא."
                            },
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = when {
                            isEnglish && hasNote -> "Edit / delete note"
                            isEnglish -> "Add exercise note"
                            hasNote -> "ערוך / מחק הערה"
                            else -> "הוסף הערה לתרגיל"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                },
                onClick = {
                    expanded = false
                    onEditNote()
                }
            )
        }
    }
}
