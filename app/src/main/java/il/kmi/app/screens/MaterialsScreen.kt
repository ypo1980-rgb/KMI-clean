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
import androidx.compose.foundation.clickable
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
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.highlightItem
import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import il.kmi.app.ui.color
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
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

    fun removeTopicPrefixOnlyWithSeparator(value: String): String {
        if (topicTrim.isBlank()) return value

        val s = value.trim()

        return when {
            s.startsWith("$topicTrim::") -> {
                s.removePrefix("$topicTrim::").trim()
            }

            s.startsWith("$topicTrim -") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim –") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim —") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim:") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            else -> s
        }
    }

    val cleaned = removeTopicPrefixOnlyWithSeparator(rawItem)

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
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val explanationTextColor = if (isDarkSurface) Color.White else Color.Black

    // ✅ NEW: נושא לתצוגה/קאנוניקליזציה — כדי ש"" יתנהג בדיוק כמו "כללי"
    val topicUi = remember(topic) { if (topic.isBlank()) "כללי" else topic }

    fun decodeMaterialParam(value: String): String {
        return try {
            java.net.URLDecoder.decode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }.trim()
    }

    val decodedSubTopicFilter = remember(subTopicFilter) {
        subTopicFilter
            ?.takeIf { it.isNotBlank() }
            ?.let { decodeMaterialParam(it) }
    }

    fun isDefenseLevelOneTitle(value: String): Boolean {
        val clean = value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

        return clean == "הגנות נגד מכות" ||
                clean == "הגנות נגד בעיטות" ||
                clean == "הגנות - סכין"
    }

    // ✅ תיקון חשוב:
    // לפעמים המסך נפתח עם topic = "הגנות נגד בעיטות" ו-subTopicFilter = null.
    // במקרה כזה root הנושא האמיתי הוא "הגנות", וה-topic עצמו הוא רמה 1.
    val materialRootTopic = remember(belt, topicUi, decodedSubTopicFilter) {
        if (
            decodedSubTopicFilter.isNullOrBlank() &&
            isDefenseLevelOneTitle(topicUi)
        ) {
            "הגנות"
        } else {
            topicUi
        }
    }

    val materialParentSubTopic = remember(belt, topicUi, decodedSubTopicFilter) {
        when {
            !decodedSubTopicFilter.isNullOrBlank() -> decodedSubTopicFilter

            isDefenseLevelOneTitle(topicUi) -> topicUi

            else -> null
        }
    }

    val nestedSubTopicTitles = remember(belt, materialRootTopic, materialParentSubTopic) {
        materialParentSubTopic
            ?.let { sub ->
                runCatching {
                    SharedContentRepo.getNestedSubTopicTitles(
                        belt = belt,
                        topicTitle = materialRootTopic.trim(),
                        subTopicTitle = sub.trim()
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }.getOrDefault(emptyList())
            }
            .orEmpty()
    }

    var openedNestedSubTopic by rememberSaveable(
        belt.id,
        materialRootTopic,
        materialParentSubTopic
    ) {
        mutableStateOf<String?>(null)
    }

    val isShowingNestedSubTopicPicker = remember(
        materialParentSubTopic,
        openedNestedSubTopic,
        nestedSubTopicTitles
    ) {
        materialParentSubTopic != null &&
                openedNestedSubTopic == null &&
                nestedSubTopicTitles.isNotEmpty()
    }

    val effectiveSubTopicFilter = remember(
        materialParentSubTopic,
        openedNestedSubTopic
    ) {
        openedNestedSubTopic ?: materialParentSubTopic
    }

    androidx.activity.compose.BackHandler(
        enabled = openedNestedSubTopic != null
    ) {
        openedNestedSubTopic = null
    }

    val topicKey = remember(materialRootTopic, materialParentSubTopic, openedNestedSubTopic) {
        when {
            materialParentSubTopic.isNullOrBlank() -> {
                materialRootTopic
            }

            openedNestedSubTopic.isNullOrBlank() -> {
                "${materialRootTopic}__${materialParentSubTopic}"
            }

            else -> {
                "${materialRootTopic}__${materialParentSubTopic}__${openedNestedSubTopic}"
            }
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

    fun exerciseIdentityIdFor(
        index: Int,
        item: String
    ): String {
        val cleanOriginal = cleanItem(topicUi, item).trim()

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanOriginal,
            topicKey = topicKey
        )

        if (resolved.isKnown) {
            return resolved.id
        }

        // עד שנמפה את כל 391 התרגילים ידנית:
        // fallback עם index מונע סימון כפול בין תרגילים דומים/זהים בשם.
        return "${resolved.id}_row_$index"
    }

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
        append(materialRootTopic.trim())
        append("||")
        append(materialParentSubTopic?.trim().orEmpty())
        append("||")
        append(openedNestedSubTopic?.trim().orEmpty())
    }

    val itemList by produceState<List<String>>(
        initialValue = itemsCache[itemsCacheKey()] ?: emptyList(),
        belt.id,
        materialRootTopic,
        materialParentSubTopic,
        openedNestedSubTopic
    ) {
        val key = itemsCacheKey()

        // אם כבר בקאש — חוזרים מיד בלי חישוב
        itemsCache[key]?.let {
            value = it
            return@produceState
        }

        value = withContext(Dispatchers.Default) {
            val topicTrim = materialRootTopic.trim()
            val subTrim = materialParentSubTopic?.trim()
            val nestedTrim = openedNestedSubTopic?.trim()

            val list = when {
                subTrim != null && nestedTrim != null -> {
                    SharedContentRepo.getNestedItemsFor(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = subTrim,
                        nestedSubTopicTitle = nestedTrim
                    )
                }

                subTrim != null -> {
                    val nestedTitles = SharedContentRepo.getNestedSubTopicTitles(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = subTrim
                    )

                    if (nestedTitles.isNotEmpty()) {
                        emptyList()
                    } else {
                        SharedContentRepo.getAllItemsFor(
                            belt = belt,
                            topicTitle = topicTrim,
                            subTopicTitle = subTrim
                        )
                    }
                }

                else -> {
                    SharedContentRepo.getAllItemsFor(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = null
                    )
                }
            }

            list.distinct()
        }

        itemsCache[key] = value
    }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun legacyStatusIdFor(index: Int, item: String): String {
        val cleanItem = normalizeStatusPart(item)

        // מפתח הסימון הישן לפני המעבר ל-ExerciseIdentityRegistry.
        // נשאר רק לקריאת fallback כדי לא לאבד סימונים קיימים.
        return "status_${belt.id}_${topicKey}_${index}_${cleanItem}"
    }

    fun statusIdFor(index: Int, item: String): String {
        return exerciseIdentityIdFor(
            index = index,
            item = item
        )
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
        val clean = value.trim()

        sp.edit().apply {
            if (clean.isBlank()) {
                remove(key)
            } else {
                putString(key, clean)
            }
        }.apply()

        notesRefreshKey++
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
            val statusId = statusIdFor(index, item)
            val legacyStatusId = legacyStatusIdFor(index, item)

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

            for (key in topicKeysToRead) {
                val valueFromStatusId: Boolean? =
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
                        ?: runCatching {
                            vm.getItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = legacyStatusId
                            )
                        }.getOrNull()
                        ?: runCatching {
                            if (
                                vm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = legacyStatusId
                                )
                            ) true else null
                        }.getOrNull()

                // ✅ סימוני וי/איקס נקראים רק לפי statusId.
                // אסור לקרוא לפי canonicalId, כי הוא עלול להיות משותף לכמה שורות דומות.
                val valueFromKey = valueFromStatusId

                if (valueFromKey != null) {
                    vFromVm = valueFromKey
                    break
                }
            }

            // ✅ fallback לשמירה המקומית הישנה
            val localFallback: Boolean? = when {
                masteredSet.contains(statusId) || masteredSet.contains(legacyStatusId) -> true
                unknowns.contains(statusId) || unknowns.contains(legacyStatusId) -> false
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
                when {
                    decodedSubTopicFilter.isNullOrBlank() -> {
                        topicTitleForUi(topic, currentLang)
                    }

                    openedNestedSubTopic.isNullOrBlank() -> {
                        "${topicTitleForUi(topic, currentLang)} - ${topicTitleForUi(decodedSubTopicFilter, currentLang)}"
                    }

                    else -> {
                        "${topicTitleForUi(decodedSubTopicFilter, currentLang)} - ${topicTitleForUi(openedNestedSubTopic ?: "", currentLang)}"
                    }
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

                                        // ✅ מנקה את כל המפתחות שבהם MaterialsScreen עשוי היה לשמור סימונים:
                                        // topicKey / topicUi / כללי.
                                        // כך סימונים ישנים לא חוזרים אחרי איפוס.
                                        keysToClear.forEach { key ->
                                            vm.clearTopic(belt, key)
                                        }

                                        itemList.forEachIndexed { index, item ->
                                            val statusId = statusIdFor(index, item)
                                            val legacyStatusId = legacyStatusIdFor(index, item)

                                            itemStates[statusId] = null
                                            itemStates[legacyStatusId] = null
                                        }

                                        excludedItems.clear()

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

        // ===== דיאלוג הסבר בעקבות חיפוש / מידע =====
        explainTriple?.let { (b, t, iRaw) ->

            val cleanItemForResolver = remember(t, iRaw) {
                cleanItem(t, iRaw).trim()
            }

            val resolvedIdentity = remember(b, t, cleanItemForResolver) {
                ExerciseIdentityRegistry.resolve(
                    belt = b,
                    hebrewTitle = cleanItemForResolver,
                    topicKey = t.trim().ifBlank { null }
                )
            }

            val dialogActionId = remember(b, t, iRaw, cleanItemForResolver, resolvedIdentity.id) {
                if (resolvedIdentity.isKnown) {
                    resolvedIdentity.id
                } else {
                    CanonicalIds.resolveCanonicalForExplanation(
                        belt = b,
                        topicTitle = t,
                        rawItemFromRepo = iRaw
                    )
                }
            }

            val explanation = remember(b, t, cleanItemForResolver, isEnglish) {
                ExerciseExplanationResolver.get(
                    belt = b,
                    topic = t,
                    item = cleanItemForResolver,
                    isEnglish = isEnglish
                ).trim()
            }.ifBlank {
                if (isEnglish) {
                    "No explanation found for \"$cleanItemForResolver\"."
                } else {
                    "לא נמצא הסבר עבור \"$cleanItemForResolver\"."
                }
            }

            val dialogTitle = itemTitleForUi(
                topic = t,
                rawItem = cleanItemForResolver,
                lang = currentLang
            )

            val dialogBeltLabel = if (isEnglish) {
                "(${b.en} belt)"
            } else {
                "(${b.heb})"
            }

            val dialogNoteText = remember(dialogActionId, notesRefreshKey) {
                loadNote(dialogActionId)
            }

            ExerciseExplanationDialog(
                title = dialogTitle,
                beltLabel = dialogBeltLabel,
                explanation = explanation,
                noteText = dialogNoteText,
                isFavorite = favorites.contains(dialogActionId),
                accentColor = b.color,
                isEnglish = isEnglish,
                onDismiss = { explainTriple = null },
                onEditNote = {
                    noteEditorFor = dialogActionId
                    noteDraft = loadNote(dialogActionId)
                },
                onDeleteNote = {
                    noteDraft = ""
                    saveNote(dialogActionId, "")
                },
                onToggleFavorite = {
                    toggleFavorite(dialogActionId)
                }
            )
        }
        // ===== סוף הדיאלוג =====

        noteEditorFor?.let { itemId ->
            ExerciseNoteEditorDialog(
                noteText = noteDraft,
                isEnglish = isEnglish,
                accentColor = belt.color,
                onNoteChange = { noteDraft = it },
                onDismiss = {
                    noteEditorFor = null
                },
                onSave = {
                    val cleanNote = noteDraft.trim()
                    noteDraft = cleanNote
                    saveNote(itemId, cleanNote)
                    noteEditorFor = null
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
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

                        if (isShowingNestedSubTopicPicker) {
                            nestedSubTopicTitles.forEach { nestedTitle ->
                                val count = remember(belt, topicUi, decodedSubTopicFilter, nestedTitle) {
                                    decodedSubTopicFilter
                                        ?.let { sub ->
                                            SharedContentRepo.getNestedItemsFor(
                                                belt = belt,
                                                topicTitle = materialRootTopic.trim(),
                                                subTopicTitle = sub.trim(),
                                                nestedSubTopicTitle = nestedTitle.trim()
                                            ).size
                                        }
                                        ?: 0
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .clickable {
                                            openedNestedSubTopic = nestedTitle
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.92f),
                                    tonalElevation = 2.dp,
                                    shadowElevation = 3.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = belt.color.copy(alpha = 0.35f)
                                    )
                                ) {
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides if (isEnglish) {
                                            LayoutDirection.Ltr
                                        } else {
                                            LayoutDirection.Rtl
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = topicTitleForUi(nestedTitle, currentLang),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                color = Color(0xFF1F2937),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(Modifier.width(10.dp))

                                            Text(
                                                text = if (isEnglish) {
                                                    if (count == 1) "1 exercise" else "$count exercises"
                                                } else {
                                                    "$count תרגילים"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = belt.color,
                                                textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left,
                                                maxLines = 1,
                                                modifier = Modifier.widthIn(min = 74.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val filtered = itemList
                            filtered.forEachIndexed { index, item ->
                            var showNoteDialog by remember { mutableStateOf(false) }

                            // ✅ מזהה אחיד להסבר / מועדפים / הערות
                            val canonicalId = remember(item, belt.id, topicUi) { canonicalFor(item) }

                            // ✅ מזהה לסימון יודע/לא יודע בלבד.
                            // אם canonicalId כפול בין כמה שורות, statusId מפריד ביניהן לפי מיקום השורה.
                            val statusId = remember(index, item, belt.id, topicKey, topicUi) {
                                statusIdFor(index, item)
                            }

                            // ✅ טקסט לתצוגה בלבד
                            val displayName = remember(item, topicUi, currentLang) {
                                itemTitleForUi(topicUi, item, currentLang)
                            }

                            var noteText by remember(item, belt.id, excludedKeySuffix, notesRefreshKey) {
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

                                            // ✅ להסברים שולחים את שם התרגיל המקורי.
                                            // ה-Resolver כבר יפתור אותו ל-ex_... דרך ExerciseIdentityRegistry.
                                            explainTriple = Triple(
                                                belt,
                                                materialRootTopic,
                                                item
                                            )

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
                                ExerciseNoteEditorDialog(
                                    noteText = noteText,
                                    isEnglish = isEnglish,
                                    accentColor = belt.color,
                                    onNoteChange = { noteText = it },
                                    onDismiss = {
                                        showNoteDialog = false
                                    },
                                    onSave = {
                                        val cleanNote = noteText.trim()
                                        noteText = cleanNote
                                        saveNote(canonicalId, cleanNote)
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
