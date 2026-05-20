package il.kmi.app.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.StarBorder
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTtsManager.speak
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.platform.PlatformSoundPlayer
import java.net.URLDecoder   // ✅ נשאר רק זה
import il.kmi.app.KmiViewModel
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import il.kmi.shared.domain.content.ExerciseTitlesEn

//==========================================================================

// ✅ NEW: טוקן לתרגול לפי נושאים (חגורות+נושאים) – חייב להתאים ל-HomeNavGraph/PracticeFabMenu
private const val TOPICS_PICK_TOKEN = "__TOPICS_PICK__"

private fun findExplanationForPractice(
    belt: Belt,
    rawItem: String,
    isEnglish: Boolean = false
): String {

    val display = il.kmi.shared.questions.model.util.ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    fun String.clean(): String =
        this.replace('-', ' ')
            .replace("־", " ")
            .replace("  ", " ")
            .trim()

    val candidates = listOf(
        rawItem,
        display,
        display.clean(),
        display.substringBefore("(").trim().clean()
    ).distinct()

    for (candidate in candidates) {
        val got = il.kmi.app.domain.Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()) return got
    }

    return if (isEnglish) {
        "No explanation is currently available for this exercise."
    } else {
        "אין עדיין הסבר זמין לתרגיל זה."
    }
}

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

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
    vm: KmiViewModel? = null,
    onOpenSettings: () -> Unit = {},
    onHome: () -> Unit = {},
    onSearch: () -> Unit = {}
) {

    val context = LocalContext.current
    val langManager = remember { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    val notePrefs = remember(context) {
        context.getSharedPreferences("kmi_exercise_notes", android.content.Context.MODE_PRIVATE)
    }

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

    // ✅ טעויות נשמרות כ-canonicalKeys — עדיין משמש לתרגול משוקלל / כל הרשימות
    val wrongCanonicalKeys = remember(belt.id, practiceKey) {
        (sp.getStringSet("wrong_${belt.id}_$practiceKey", emptySet()) ?: emptySet())
            .toMutableSet()
    }

    // ✅ מצב סימון משותף למסך תרגול:
    // true = יודע / וי ירוק
    // false = לא יודע / איקס אדום
    // null = לא סומן / עיגול ריק
    val practiceStatusMap = remember(belt.id, practiceKey) {
        mutableStateMapOf<String, Boolean?>()
    }

    // ✅ מאזין לשינויים של סימוני יודע/לא יודע מכל המסכים
    val marksVersion by (vm?.marksVersion ?: kotlinx.coroutines.flow.flowOf(0))
        .collectAsState(initial = 0)

    fun statusTopicFor(item: il.kmi.shared.practice.PracticeItem): String {
        return item.topicTitle.trim().ifBlank {
            topicFilter?.trim()?.takeIf { it.isNotBlank() } ?: "כללי"
        }
    }

    // ✅ מחזיר את ה־raw item המקורי מתוך ContentRepo לפי שם התצוגה.
    // זה חשוב כדי שה־canonicalId יהיה זהה ל־MaterialsScreen.
    fun statusRawItemFor(item: il.kmi.shared.practice.PracticeItem): String {
        val statusTopic = statusTopicFor(item)
        val wantedDisplay = item.displayTitle.trim()
        val wantedNorm = wantedDisplay.normHeb()

        val allRawItems = sharedItemsFor(
            b = belt,
            topicTitle = statusTopic,
            subTopicTitle = null
        )

        return allRawItems.firstOrNull { raw ->
            val rawDisplay = displayName(raw).ifBlank { raw }.trim()

            raw.trim().normHeb() == wantedNorm ||
                    rawDisplay.normHeb() == wantedNorm
        } ?: wantedDisplay
    }

    fun statusCanonicalFor(item: il.kmi.shared.practice.PracticeItem): String {
        val statusTopic = statusTopicFor(item)
        val rawItem = statusRawItemFor(item)

        return CanonicalIds.canonicalFor(
            belt = belt,
            topicTitle = statusTopic,
            displayItem = rawItem
        )
    }

    // ✅ מחזיר את כל מפתחות הסימון האפשריים:
    // 1) המפתח שהתרגול חושב עליו
    // 2) כללי — כי MaterialsScreen שומר תרגילי חגורה כלליים תחת "כללי"
    // 3) כל נושא אמיתי ב-ContentRepo שבו התרגיל נמצא
    // 4) תתי-נושאים בפורמט: נושא__תת-נושא
    fun statusTopicKeysFor(item: il.kmi.shared.practice.PracticeItem): List<String> {
        val baseTopic = statusTopicFor(item).trim()
        val rawItem = statusRawItemFor(item)
        val rawNorm = rawItem.normHeb()
        val displayNorm = item.displayTitle.normHeb()

        fun matchesItem(candidateRaw: String): Boolean {
            val candidateDisplay = displayName(candidateRaw).ifBlank { candidateRaw }.trim()

            return candidateRaw.trim().normHeb() == rawNorm ||
                    candidateDisplay.normHeb() == rawNorm ||
                    candidateRaw.trim().normHeb() == displayNorm ||
                    candidateDisplay.normHeb() == displayNorm
        }

        val directTopicKeys =
            runCatching {
                SharedContentRepo.data[belt]?.topics
                    ?.filter { topic ->
                        topic.items.any { raw -> matchesItem(raw) } ||
                                topic.subTopics.any { sub ->
                                    sub.items.any { raw -> matchesItem(raw) }
                                }
                    }
                    ?.map { it.title.trim() }
                    .orEmpty()
            }.getOrDefault(emptyList())

        val subTopicKeys =
            runCatching {
                SharedContentRepo.data[belt]?.topics
                    ?.flatMap { topic ->
                        topic.subTopics
                            .filter { subTopic ->
                                subTopic.items.any { raw -> matchesItem(raw) }
                            }
                            .map { subTopic ->
                                "${topic.title.trim()}__${subTopic.title.trim()}"
                            }
                    }
                    .orEmpty()
            }.getOrDefault(emptyList())

        return (
                listOf(
                    baseTopic,
                    "כללי",
                    topicFilter?.trim().orEmpty()
                ) + directTopicKeys + subTopicKeys
                )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun persistWrongKeys() {
        sp.edit()
            .putStringSet("wrong_${belt.id}_$practiceKey", wrongCanonicalKeys.toSet())
            .apply()
    }

    fun setPracticeStatus(
        item: il.kmi.shared.practice.PracticeItem?,
        newStatus: Boolean?
    ) {
        if (item == null) return

        val statusTopic = statusTopicFor(item)
        val canonicalId = statusCanonicalFor(item)
        val topicKeys = statusTopicKeysFor(item)

        android.util.Log.d(
            "KMI_MARK_SYNC",
            "RandomPractice save | belt=${belt.id} | topic=$statusTopic | topicKeys=$topicKeys | display=${item.displayTitle} | canonicalId=$canonicalId | value=$newStatus"
        )

        // ✅ עדכון מיידי במסך התרגול
        practiceStatusMap[canonicalId] = newStatus

        // ✅ שמירה לכל מפתחות הסימון האפשריים:
        // גם לנושא הראשי, גם ל"כללי", וגם לתת־נושא אם קיים.
        topicKeys.forEach { key ->
            vm?.setItemStatusNullable(
                belt = belt,
                topic = key,
                item = canonicalId,
                value = newStatus
            )

            // ✅ שמירה מקומית תואמת ל-MaterialsScreen
            // כדי שכאשר חוזרים למסך החומר, הרינדור הראשון כבר יראה וי/איקס
            // ולא יחכה רק לטעינת ה-VM.
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet = (sp.getStringSet(masteredKey, emptySet()) ?: emptySet()).toMutableSet()
            val unknownSet = (sp.getStringSet(unknownKey, emptySet()) ?: emptySet()).toMutableSet()

            when (newStatus) {
                true -> {
                    masteredSet.add(canonicalId)
                    unknownSet.remove(canonicalId)
                }

                false -> {
                    unknownSet.add(canonicalId)
                    masteredSet.remove(canonicalId)
                }

                null -> {
                    masteredSet.remove(canonicalId)
                    unknownSet.remove(canonicalId)
                }
            }

            sp.edit()
                .putStringSet(masteredKey, masteredSet)
                .putStringSet(unknownKey, unknownSet)
                .apply()
        }

        // ✅ תאימות למסך "כל הרשימות" / רשימת לא יודע:
        // false = נכנס ללא יודע
        // true/null = יוצא מלא יודע
        when (newStatus) {
            false -> wrongCanonicalKeys.add(item.canonicalKey)
            true, null -> wrongCanonicalKeys.remove(item.canonicalKey)
        }

        persistWrongKeys()
    }

    fun addWrongForCurrent(item: il.kmi.shared.practice.PracticeItem?) {
        setPracticeStatus(item, false)
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
    fun uiTitleFor(item: il.kmi.shared.practice.PracticeItem): String {
        return item.displayTitle
    }

    val weightedItems: List<String> = remember(weightedPracticeItems, isEnglish) {
        weightedPracticeItems.map { uiTitleFor(it) }
    }

    var currentIndex by remember { mutableStateOf(0) }

    val currentPracticeItem = weightedPracticeItems.getOrNull(currentIndex)
    val currentStatusCanonical = currentPracticeItem?.let { statusCanonicalFor(it) }
    val currentPracticeStatus: Boolean? =
        currentStatusCanonical?.let { practiceStatusMap[it] }

    suspend fun readPracticeStatusFromSources(
        safeVm: KmiViewModel?,
        item: il.kmi.shared.practice.PracticeItem,
        canonicalId: String
    ): Pair<Boolean?, String?> {
        val topicKeys = statusTopicKeysFor(item)

        // ✅ 1. אם יש VM — קוראים קודם מהמקור הראשי
        if (safeVm != null) {
            for (key in topicKeys) {
                val valueFromKey: Boolean? =
                    runCatching {
                        safeVm.getItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = canonicalId
                        )
                    }.getOrNull()
                        ?: runCatching {
                            if (
                                safeVm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = canonicalId
                                )
                            ) true else null
                        }.getOrNull()

                if (valueFromKey != null) {
                    return valueFromKey to key
                }
            }
        }

        // ✅ 2. גם אם אין VM — קוראים מהשמירה המקומית ש-MaterialsScreen כותב אליה
        for (key in topicKeys) {
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                sp.getStringSet(masteredKey, emptySet()) ?: emptySet()

            val unknownSet =
                sp.getStringSet(unknownKey, emptySet()) ?: emptySet()

            val localValue: Boolean? = when {
                masteredSet.contains(canonicalId) -> true
                unknownSet.contains(canonicalId) -> false
                else -> null
            }

            if (localValue != null) {
                // ✅ אם יש VM — נרפא גם אותו
                safeVm?.setItemStatusNullable(
                    belt = belt,
                    topic = key,
                    item = canonicalId,
                    value = localValue
                )

                android.util.Log.d(
                    "KMI_MARK_SYNC",
                    "RandomPractice SP fallback | belt=${belt.id} | key=$key | display=${item.displayTitle} | canonicalId=$canonicalId | value=$localValue | vmIsNull=${safeVm == null}"
                )

                return localValue to "$key/SP"
            }
        }

        return null to null
    }

    // ✅ טעינה ממוקדת לתרגיל הנוכחי בכל מעבר תרגיל.
    // זה מבטיח שהעיגול העליון יקבל וי/איקס גם אם הסימון נטען אחרי הרינדור הראשון.
LaunchedEffect(currentIndex, currentStatusCanonical, marksVersion) {
    val item = currentPracticeItem ?: return@LaunchedEffect
    val canonicalId = currentStatusCanonical ?: return@LaunchedEffect

    val statusTopic = statusTopicFor(item)
    val topicKeys = statusTopicKeysFor(item)

    val (fromSources, matchedKey) = readPracticeStatusFromSources(
        safeVm = vm,
        item = item,
        canonicalId = canonicalId
    )

    practiceStatusMap[canonicalId] = fromSources

    android.util.Log.d(
        "KMI_MARK_SYNC",
        "RandomPractice current load | belt=${belt.id} | baseTopic=$statusTopic | topicKeys=$topicKeys | matchedKey=$matchedKey | display=${item.displayTitle} | canonicalId=$canonicalId | value=$fromSources | vmIsNull=${vm == null}"
    )
}

    // ✅ טעינת כל הסימונים בתחילת התרגול / אחרי שינוי סימון.
    // חשוב: לא עושים clear למפה, כדי שלא יהיה רגע שבו העיגול חוזר לריק.
    LaunchedEffect(weightedPracticeItems, vm, marksVersion) {
        weightedPracticeItems.forEach { item ->
            val statusTopic = statusTopicFor(item)
            val canonicalId = statusCanonicalFor(item)
            val topicKeys = statusTopicKeysFor(item)

            val (fromSources, matchedKey) = readPracticeStatusFromSources(
                safeVm = vm,
                item = item,
                canonicalId = canonicalId
            )

            practiceStatusMap[canonicalId] = fromSources

            android.util.Log.d(
                "KMI_MARK_SYNC",
                "RandomPractice load | belt=${belt.id} | baseTopic=$statusTopic | topicKeys=$topicKeys | matchedKey=$matchedKey | display=${item.displayTitle} | canonicalId=$canonicalId | value=$fromSources | vmIsNull=${vm == null}"
            )
        }
    }

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

    // ✅ צפצוף יציב לספירה לאחור — לא תלוי בקובץ beep ולא נבלע בין שניות
    val countdownTone = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, 95)
    }

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
            runCatching { countdownTone.release() }
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
        runCatching {
            countdownTone.startTone(ToneGenerator.TONE_PROP_BEEP, ms)
        }
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
            isEnglish = isEnglish,
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
                        weightedPracticeItems.getOrNull(currentIndex)?.let { currentItem ->
                            speak(uiTitleFor(currentItem))
                        }
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
                weightedPracticeItems.getOrNull(currentIndex)?.let { currentItem ->
                    speak(uiTitleFor(currentItem))
                }
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
                    if (!isMuted) speak(if (isEnglish) "Half of the practice time has passed" else "עבר חצי מזמן התרגול")
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

    fun searchTitleForUi(rawItem: String): String {
        val display = displayName(rawItem).ifBlank { rawItem }.trim()
        return if (isEnglish) {
            ExerciseTitlesEn.getOrSame(display)
        } else {
            display
        }
    }

    fun topicTitleForUi(rawTopic: String): String {
        return if (isEnglish) {
            ExerciseTitlesEn.getOrSame(rawTopic.trim())
        } else {
            rawTopic.trim()
        }
    }

    val searchResults by remember(searchQuery, globalSearchItems, isEnglish) {
        mutableStateOf(
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                val q = searchQuery.trim()

                globalSearchItems
                    .filter { (hitBelt, hitTopic, hitItem) ->
                        val rawItem = hitItem.trim()
                        val displayItem = displayName(hitItem).ifBlank { hitItem }.trim()
                        val translatedItem = ExerciseTitlesEn.getOrSame(displayItem)
                        val translatedTopic = ExerciseTitlesEn.getOrSame(hitTopic.trim())
                        val beltName = if (isEnglish) hitBelt.en else hitBelt.heb

                        rawItem.contains(q, ignoreCase = true) ||
                                displayItem.contains(q, ignoreCase = true) ||
                                translatedItem.contains(q, ignoreCase = true) ||
                                hitTopic.contains(q, ignoreCase = true) ||
                                translatedTopic.contains(q, ignoreCase = true) ||
                                beltName.contains(q, ignoreCase = true)
                    }
                    .take(50)
            }
        )
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = if (!topicFilter.isNullOrBlank()) {
                    if (isEnglish) {
                        "Practice by Topic - ${belt.en}"
                    } else {
                        "תרגול לפי נושא - ${belt.heb}"
                    }
                } else {
                    if (isEnglish) {
                        "Random Practice - ${belt.en}"
                    } else {
                        "תרגול אקראי - ${belt.heb}"
                    }
                },
                onBack = null,
                showBottomActions = true,
                onHome = onHome,
                onSearch = { showSearch = true },
                lockSearch = false,
                showTopHome = false,
                showTopSearch = false,
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
                        Text(if (isEnglish) "No exercises are available for this topic" else "אין תרגילים זמינים לנושא זה")

                        Spacer(Modifier.height(10.dp))

                        // ✅ דיבאג: עוזר לנו לוודא ש־topicFilter לא הגיע בטעות כחגורה/טקסט אחר
                        Text(
                            text = "debug: beltId=${belt.id} | belt=${if (isEnglish) belt.en else belt.heb} | topicFilter='${topicFilter ?: ""}'",
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ✅ שורת טיימר נקייה בלבד — בלי רמקול ובלי כפתור עצירה
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏳", fontSize = 30.sp)

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = Color(0xFF6D56B8),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                )
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        if (currentIndex in weightedItems.indices) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF9C4), RoundedCornerShape(20.dp))
                                    .clickable {
                                        showHelp = true
                                    }
                                    .padding(horizontal = 22.dp, vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = weightedPracticeItems
                                        .getOrNull(currentIndex)
                                        ?.let { uiTitleFor(it) }
                                        .orEmpty(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF171717)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            // ✅ רמקול מצד אחד, סטטוס באמצע, עצירה/המשך מצד שני
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PremiumSoundIconButton(
                                    isMuted = isMuted,
                                    isEnglish = isEnglish,
                                    onClick = {
                                        isMuted = !isMuted
                                        if (!isMuted && sessionStarted && currentIndex in weightedItems.indices) {
                                            weightedPracticeItems.getOrNull(currentIndex)?.let { currentItem ->
                                                speak(uiTitleFor(currentItem))
                                            }
                                            lastSpokenIndex = currentIndex
                                        } else {
                                            KmiTtsManager.stop()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )

                                key(currentStatusCanonical, currentPracticeStatus) {
                                    Box(modifier = Modifier.align(Alignment.Center)) {
                                        PracticeStatusCircle(
                                            status = currentPracticeStatus,
                                            beltColor = belt.lightColor,
                                            isEnglish = isEnglish,
                                            onClick = {
                                                val nextStatus = when (currentPracticeStatus) {
                                                    null -> true
                                                    true -> false
                                                    false -> null
                                                }

                                                setPracticeStatus(
                                                    weightedPracticeItems.getOrNull(currentIndex),
                                                    nextStatus
                                                )
                                            }
                                        )
                                    }
                                }

                                PremiumPauseResumeButton(
                                    isRunning = isRunning,
                                    isEnglish = isEnglish,
                                    onClick = {
                                        isRunning = !isRunning
                                        if (isRunning && sessionStarted && currentIndex in weightedItems.indices) {
                                            weightedPracticeItems.getOrNull(currentIndex)?.let { currentItem ->
                                                speak(uiTitleFor(currentItem))
                                            }
                                            lastSpokenIndex = currentIndex
                                        } else {
                                            KmiTtsManager.stop()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }

                    PracticeBottomActionCard(
                        isEnglish = isEnglish,
                        onHelp = { showHelp = true },
                        onSkip = {
                            if (currentIndex < weightedItems.lastIndex) {
                                currentIndex++
                            }
                        },
                        onFinish = {
                            isRunning = false
                            sessionStarted = false
                            runCatching { KmiTtsManager.stop() }

                            playStopRest {
                                requestExit()
                            }
                        }
                    )
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

                val explanation = remember(b, item, t, isEnglish) {
                    findExplanationForPractice(
                        belt = b,
                        rawItem = item,
                        isEnglish = isEnglish
                    )
                }

                val itemTitleUi = remember(item, isEnglish) {
                    searchTitleForUi(item)
                }

                val topicTitleUi = remember(t, isEnglish) {
                    topicTitleForUi(t)
                }

                val sheetTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                val sheetHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                val actionAlignment = if (isEnglish) Alignment.Start else Alignment.End

                val favId = remember(item) { normalizeFavoriteId(item) }
                val isFav = favorites.contains(favId)

                val noteKey = remember(b, t, favId) {
                    "note_${b.id}_${t.trim()}_${favId}"
                }
                var noteText by remember(noteKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }
                var showNoteEditor by remember { mutableStateOf(false) }

                fun toggleFav() {
                    if (item.isBlank()) return
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
                            Row(
                                modifier = Modifier.align(if (isEnglish) Alignment.CenterEnd else Alignment.CenterStart),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(onClick = { toggleFav() }) {
                                    if (isFav) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = if (isEnglish) "Remove from favorites" else "הסר ממועדפים",
                                            tint = Color(0xFFFFC107)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.StarBorder,
                                            contentDescription = if (isEnglish) "Add to favorites" else "הוסף למועדפים"
                                        )
                                    }
                                }

                                IconButton(onClick = { showNoteEditor = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = if (isEnglish) "Edit note" else "ערוך הערה",
                                        tint = Color(0xFF42A5F5)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd)
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (isEnglish) 0.dp else 92.dp,
                                        end = if (isEnglish) 92.dp else 0.dp
                                    ),
                                horizontalAlignment = sheetHorizontalAlignment
                            ) {
                                Text(
                                    text = itemTitleUi,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = sheetTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "$topicTitleUi • ${if (isEnglish) b.en else b.heb}",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = sheetTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = sheetHorizontalAlignment
                        ) {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = sheetTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (noteText.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = if (isEnglish) "Trainee note:" else "הערה של המתאמן:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = sheetTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = sheetTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                        ) {
                            TextButton(onClick = {
                                pickedSearchHit = null
                                showHelp = false
                                showSearch = false
                                searchQuery = ""
                            }) {
                                Text(if (isEnglish) "Close" else "סגור")
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (showNoteEditor) {
                    AlertDialog(
                        onDismissRequest = { showNoteEditor = false },
                        title = {
                            Text(
                                text = if (isEnglish) "Exercise Note" else "הערה לתרגיל",
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right),
                                label = { Text(if (isEnglish) "Write a note" else "כתוב הערה") }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    notePrefs.edit()
                                        .putString(noteKey, noteText.trim())
                                        .apply()
                                    showNoteEditor = false
                                }
                            ) {
                                Text(if (isEnglish) "Save" else "שמור")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNoteEditor = false }) {
                                Text(if (isEnglish) "Cancel" else "ביטול")
                            }
                        }
                    )
                }
            }

            showHelp -> {
                val item = weightedItems.getOrNull(currentIndex)

                val explanation = remember(belt, item, isEnglish) {
                    if (item.isNullOrBlank()) {
                        if (isEnglish) "No exercise selected to display." else "לא נבחר תרגיל להצגה."
                    } else {
                        findExplanationForPractice(
                            belt = belt,
                            rawItem = item,
                            isEnglish = isEnglish
                        )
                    }
                }

                val safeItem = item.orEmpty()

                val safeItemTitleUi = remember(safeItem, isEnglish) {
                    if (safeItem.isBlank()) {
                        if (isEnglish) "Exercise" else "תרגיל"
                    } else {
                        searchTitleForUi(safeItem)
                    }
                }

                val helpTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                val helpHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

                val favId = remember(safeItem) { normalizeFavoriteId(safeItem) }

                val isFav = safeItem.isNotBlank() && favorites.contains(favId)

                val noteTopic = remember(topicFilter) {
                    topicFilter?.takeIf { it.isNotBlank() } ?: "general"
                }
                val noteKey = remember(belt, noteTopic, favId) {
                    "note_${belt.id}_${noteTopic.trim()}_${favId}"
                }
                var noteText by remember(noteKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }
                var showNoteEditor by remember { mutableStateOf(false) }

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
                            Row(
                                modifier = Modifier.align(if (isEnglish) Alignment.CenterEnd else Alignment.CenterStart),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(onClick = { toggleFav() }) {
                                    if (isFav) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = if (isEnglish) "Remove from favorites" else "הסר ממועדפים",
                                            tint = Color(0xFFFFC107)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.StarBorder,
                                            contentDescription = if (isEnglish) "Add to favorites" else "הוסף למועדפים"
                                        )
                                    }
                                }

                                IconButton(onClick = { showNoteEditor = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = if (isEnglish) "Edit note" else "ערוך הערה",
                                        tint = Color(0xFF42A5F5)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd)
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (isEnglish) 0.dp else 92.dp,
                                        end = if (isEnglish) 92.dp else 0.dp
                                    ),
                                horizontalAlignment = helpHorizontalAlignment
                            ) {
                                Text(
                                    text = safeItemTitleUi,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = helpTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "(${if (isEnglish) belt.en else belt.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = helpTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = helpHorizontalAlignment
                        ) {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = helpTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (noteText.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = if (isEnglish) "Trainee note:" else "הערה של המתאמן:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = helpTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = helpTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                        ) {
                            TextButton(onClick = { showHelp = false }) {
                                Text(if (isEnglish) "Close" else "סגור")
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (showNoteEditor) {
                    AlertDialog(
                        onDismissRequest = { showNoteEditor = false },
                        title = {
                            Text(
                                text = if (isEnglish) "Exercise Note" else "הערה לתרגיל",
                                textAlign = helpTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(textAlign = helpTextAlign),
                                label = { Text(if (isEnglish) "Write a note" else "כתוב הערה") }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    notePrefs.edit()
                                        .putString(noteKey, noteText.trim())
                                        .apply()
                                    showNoteEditor = false
                                }
                            ) {
                                Text(if (isEnglish) "Save" else "שמור")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNoteEditor = false }) {
                                Text(if (isEnglish) "Cancel" else "ביטול")
                            }
                        }
                    )
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
                            text = if (isEnglish) {
                                "Search exercise (for example: \"kick\", \"defense\")"
                            } else {
                                "חפש תרגיל (למשל: \"בעיטה\", \"הגנה\")"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(if (isEnglish) "Type exercise name" else "הקלד/י שם תרגיל") }
                        )

                        Spacer(Modifier.height(8.dp))

                        if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                            Text(
                                if (isEnglish) "No matching exercises found." else "לא נמצאו תרגילים תואמים.",
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
                                    val resultTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                                    val resultHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                    val hitItemUi = searchTitleForUi(hitItem)
                                    val hitTopicUi = topicTitleForUi(hitTopic)

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
                                            horizontalAlignment = resultHorizontalAlignment
                                        ) {
                                            Text(
                                                text = hitItemUi,
                                                style = MaterialTheme.typography.titleMedium,
                                                textAlign = resultTextAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "$hitTopicUi • ${if (isEnglish) hitBelt.en else hitBelt.heb}",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = resultTextAlign,
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
                        }) { Text(if (isEnglish) "Close" else "סגור") }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/* ===== דיאלוג בחירת זמן תרגול (Top-level) ===== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPickerDialog(
    show: Boolean,
    isEnglish: Boolean,
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
                            if (isEnglish) "Choose Practice Duration" else "בחר זמן תרגול",
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
                isEnglish = isEnglish,
                onSelect = { selectedMin = it }
            )

            Spacer(Modifier.height(10.dp))

            // ===== מתגים עם תיאור משנה =====
            SettingRow(
                title = if (isEnglish) "Mid-time alert" else "התראה באמצע הזמן",
                subtitle = if (isEnglish) "Beep + voice announcement at halfway point" else "צפצוף + הודעה קולית בחצי הזמן",                checked = playHalf,
                onCheckedChange = { playHalf = it }
            )
            SettingRow(
                title = if (isEnglish) "Sound in the last 10 seconds" else "צליל ב־10 השניות האחרונות",
                subtitle = if (isEnglish) "Short beep every second until the end" else "צפצוף קצר כל שנייה עד לסיום",                checked = playCountdown,
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
                    Text(if (isEnglish) "Cancel" else "בטל", fontWeight = FontWeight.SemiBold)
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
                    Text(if (isEnglish) "Start" else "התחל", fontWeight = FontWeight.ExtraBold)
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
    isEnglish: Boolean,
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
                        Text(if (isEnglish) "min" else "דק׳", modifier = Modifier.alpha(0.9f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ====================== כפתורים חדשניים ====================== */

@Composable
private fun ModernActionsRow(
    isEnglish: Boolean,
    onHelp: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModernPillButton(
            text = if (isEnglish) "Help" else "עזרה",
            leading = { Icon(Icons.Outlined.Info, contentDescription = null) },
            container = Color(0xFFEEE7FF),
            content = Color(0xFF6D56B8),
            overlayGradient = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.22f),
                    Color(0xFF6D56B8).copy(alpha = 0.10f)
                )
            ),
            onClick = onHelp,
            modifier = Modifier.weight(1f)
        )

        ModernPillButton(
            text = if (isEnglish) "Skip" else "דלג",
            leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            container = Color(0xFF6D56B8),
            content = Color.White,
            overlayGradient = Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                )
            ),
            onClick = onSkip,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PracticeBottomActionCard(
    isEnglish: Boolean,
    onHelp: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFF7F4FF).copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModernActionsRow(
                isEnglish = isEnglish,
                onHelp = onHelp,
                onSkip = onSkip
            )

            Surface(
                onClick = onFinish,
                color = Color.White.copy(alpha = 0.97f),
                contentColor = Color(0xFF111827),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 7.dp,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB).copy(alpha = 0.92f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Finish and Return" else "סיום וחזרה",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF111827),
                            letterSpacing = 0.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSoundIconButton(
    isMuted: Boolean,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.88f),
        contentColor = if (isMuted) Color(0xFF94A3B8) else Color(0xFF6D56B8),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f)),
        modifier = modifier.size(58.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) {
                    if (isEnglish) "Resume audio" else "המשך קול"
                } else {
                    if (isEnglish) "Mute" else "השתק"
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PremiumPauseResumeButton(
    isRunning: Boolean,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.88f),
        contentColor = Color(0xFF111827),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.80f)),
        modifier = modifier.size(58.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) {
                    if (isEnglish) "Pause" else "השהה"
                } else {
                    if (isEnglish) "Resume" else "המשך"
                },
                modifier = Modifier.size(28.dp)
            )
        }
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
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.06f)
        )
    ),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        shadowElevation = 9.dp,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.52f)),
        modifier = modifier.height(58.dp)
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
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PremiumSoundButton(
    isMuted: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.White.copy(alpha = 0.72f),
        contentColor = Color(0xFF111827),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
        modifier = Modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) {
                    if (isEnglish) "Resume audio" else "המשך קול"
                } else {
                    if (isEnglish) "Mute" else "השתק"
                },
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = if (isMuted) {
                    if (isEnglish) "Audio off" else "קול כבוי"
                } else {
                    if (isEnglish) "Audio on" else "קול פעיל"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
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

@Composable
private fun PracticeStatusCircle(
    status: Boolean?,
    beltColor: Color,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val label = when (status) {
        true -> if (isEnglish) "Known" else "יודע"
        false -> if (isEnglish) "Don't know" else "לא יודע"
        null -> if (isEnglish) "Not marked" else "לא סומן"
    }

    val circleColor = when (status) {
        true -> Color(0xFF22C55E)
        false -> Color(0xFFDC2626)
        null -> Color.White
    }

    val borderColor = when (status) {
        true -> Color(0xFF16A34A)
        false -> Color(0xFFB91C1C)
        null -> beltColor.copy(alpha = 0.42f)
    }

    val iconColor = when (status) {
        true, false -> Color.White
        null -> beltColor.copy(alpha = 0.72f)
    }

    val textColor = when (status) {
        true -> Color(0xFF15803D)
        false -> Color(0xFFB91C1C)
        null -> Color(0xFF334155)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.86f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, beltColor.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = circleColor,
                border = BorderStroke(2.dp, borderColor),
                modifier = Modifier.size(34.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (status) {
                        true -> Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(21.dp)
                        )

                        false -> Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(21.dp)
                        )

                        null -> Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.18f))
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}