package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import il.kmi.app.ui.practice.PracticeStartConfigDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTtsManager.speak
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.ui.ext.color
import il.kmi.shared.platform.PlatformSoundPlayer
import java.net.URLDecoder   // ✅ נשאר רק זה
import il.kmi.app.KmiViewModel
import il.kmi.app.R
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import il.kmi.app.ui.practice.PracticeBottomControls
import il.kmi.app.ui.practice.PracticeExerciseCenterCard

//==========================================================================

// ✅ NEW: טוקן לתרגול לפי נושאים (חגורות+נושאים) – חייב להתאים ל-HomeNavGraph/PracticeFabMenu
private const val TOPICS_PICK_TOKEN = "__TOPICS_PICK__"

private fun findExplanationForPractice(
    belt: Belt,
    topic: String,
    rawItem: String,
    isEnglish: Boolean = false
): String {
    val cleanTopic = topic.trim()

    val cleanItem = il.kmi.shared.questions.model.util.ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = cleanTopic,
        item = cleanItem,
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
        return cleaned
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

private fun savePracticeNote(
    prefs: android.content.SharedPreferences,
    key: String,
    text: String
) {
    val clean = text.trim()

    prefs.edit().apply {
        if (clean.isBlank()) {
            remove(key)
        } else {
            putString(key, clean)
        }
    }.apply()
}

private fun decTokenPart(s: String): String =
    runCatching {
        URLDecoder.decode(s, Charsets.UTF_8.name())
    }.getOrDefault(s)

private data class PracticeSubjectFilter(
    val topic: String,
    val subTopic: String?
)

private fun resolvePracticeSubjectFilter(
    rawFilter: String?,
    belt: Belt
): PracticeSubjectFilter? {
    val cleanFilter =
        rawFilter?.trim().orEmpty()

    if (cleanFilter.isBlank()) {
        return null
    }

    val selectedValue =
        if (
            cleanFilter.startsWith(
                "$TOPICS_PICK_TOKEN:"
            )
        ) {
            val beltSegment =
                cleanFilter
                    .removePrefix(
                        "$TOPICS_PICK_TOKEN:"
                    )
                    .split(';')
                    .firstOrNull { segment ->
                        segment
                            .substringBefore('|')
                            .trim() == belt.id
                    }
                    ?: return null

            val selectedTopics =
                beltSegment
                    .substringAfter('|', "")
                    .split(',')
                    .map { decTokenPart(it.trim()) }
                    .filter { it.isNotBlank() }

            selectedTopics.singleOrNull()
                ?: return null
        } else {
            decTokenPart(cleanFilter)
        }

    if (
        selectedValue.startsWith(
            "__SUBTOPIC__:"
        )
    ) {
        val selectionParts =
            selectedValue
                .removePrefix("__SUBTOPIC__:")
                .split("::", limit = 2)

        val topic =
            selectionParts
                .getOrNull(0)
                ?.trim()
                .orEmpty()

        val subTopic =
            selectionParts
                .getOrNull(1)
                ?.trim()
                .orEmpty()

        if (
            topic.isBlank() ||
            subTopic.isBlank()
        ) {
            return null
        }

        return PracticeSubjectFilter(
            topic = topic,
            subTopic = subTopic
        )
    }

    if (
        selectedValue.startsWith("__") ||
        selectedValue.equals("all", ignoreCase = true) ||
        selectedValue.equals("random", ignoreCase = true) ||
        selectedValue == belt.id ||
        selectedValue == belt.heb
    ) {
        return null
    }

    return PracticeSubjectFilter(
        topic = selectedValue.trim(),
        subTopic = null
    )
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

    val sp = remember {
        context.getSharedPreferences(
            "kmi_settings",
            android.content.Context.MODE_PRIVATE
        )
    }
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
    var pickedSearchHit by rememberSaveable {
        mutableStateOf<Triple<Belt, String, String>?>(
            null
        )
    }

    val selectedSubjectFilter =
        remember(belt, topicFilter) {
            resolvePracticeSubjectFilter(
                rawFilter = topicFilter,
                belt = belt
            )
        }

    // ✅ מקור עיקרי: shared PracticeFacade (כולל __UNKNOWN__/__FAVS_ALL__/__ALL__/TOPICS_PICK_TOKEN)
    val practiceItems:
            List<il.kmi.shared.practice.PracticeItem> =
        remember(
            belt,
            topicFilter,
            favorites
        ) {
            val rawFilter = topicFilter?.trim().orEmpty()

            // אם הגיע טוקן עם encoding, נעשה decode כאן (כמו קודם)
            val decodedTopicsToken =
                if (rawFilter.isNotBlank() && rawFilter.startsWith("$TOPICS_PICK_TOKEN:")) {
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

            // בתרגול של תת־נושא, PracticeFacade מקבל את נושא האב.
            val fixedFilter = when {
                selectedSubjectFilter != null ->
                    selectedSubjectFilter.topic

                decodedTopicsToken.isBlank() ->
                    il.kmi.shared.practice.PracticeFilters.ALL

                decodedTopicsToken.equals(
                    belt.heb.trim(),
                    ignoreCase = true
                ) -> il.kmi.shared.practice.PracticeFilters.ALL

                decodedTopicsToken.equals(
                    belt.id.trim(),
                    ignoreCase = true
                ) -> il.kmi.shared.practice.PracticeFilters.ALL

                decodedTopicsToken.equals(
                    "אקראי",
                    ignoreCase = true
                ) -> il.kmi.shared.practice.PracticeFilters.ALL

                decodedTopicsToken.equals(
                    "random",
                    ignoreCase = true
                ) -> il.kmi.shared.practice.PracticeFilters.ALL

                decodedTopicsToken.equals(
                    "all",
                    ignoreCase = true
                ) -> il.kmi.shared.practice.PracticeFilters.ALL

                else -> decodedTopicsToken
            }

            /*
       * טוקנים שמייצגים טאב במסך הרשימות.
       * הם אינם שמות של נושאים.
       */
            val isUnknownTab =
                fixedFilter ==
                        "__UNKNOWN__"

            val isFavoritesTab =
                fixedFilter ==
                        "__FAVS_ALL__"

            val coachStatusForTab: String? =
                when (fixedFilter) {
                    "__COACH_TAUGHT__" ->
                        "taught"

                    "__COACH_PRACTICE__" ->
                        "practiced"

                    "__COACH_IMPROVEMENT__" ->
                        "needs_reinforcement"

                    else ->
                        null
                }

            val isTabFilter =
                isUnknownTab ||
                        isFavoritesTab ||
                        coachStatusForTab != null

            // האם זה פילטר של נושא אמיתי בודד.
            val isSingleTopicFilter =
                fixedFilter.isNotBlank() &&
                        !fixedFilter.startsWith(
                            "$TOPICS_PICK_TOKEN:"
                        ) &&
                        fixedFilter !=
                        il.kmi.shared.practice
                            .PracticeFilters.ALL &&
                        fixedFilter !=
                        il.kmi.shared.practice
                            .PracticeFilters.UNKNOWN &&
                        fixedFilter !=
                        il.kmi.shared.practice
                            .PracticeFilters.FAVS_ALL &&
                        !isTabFilter

            /*
             * עבור טאב בונים תחילה את כל תרגילי החגורה,
             * ולאחר מכן מסננים לפי מקור האמת של אותו טאב.
             */
            val requestFilterForFacade =
                if (
                    isSingleTopicFilter ||
                    isTabFilter
                ) {
                    il.kmi.shared.practice
                        .PracticeFilters.ALL
                } else {
                    fixedFilter
                }

            // ===================== ✅ NEW: resolve נושא יחיד =====================
            val resolvedSingleTopic: String? =
                if (!isSingleTopicFilter) null
                else {
                    val wanted = fixedFilter.trim()
                    val wantedN = wanted.normHeb()

                    val titlesFromBridge =
                        runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt) }
                            .getOrDefault(emptyList())

                    val titles = titlesFromBridge.ifEmpty { sharedTopicTitlesFor(belt) }

                    when {
                        titles.isEmpty() -> wanted
                        titles.any { it.trim() == wanted } -> titles.first { it.trim() == wanted }
                        titles.any { it.trim().normHeb() == wantedN } -> titles.first {
                            it.trim().normHeb() == wantedN
                        }

                        else -> wanted
                    }
                }
            // ================================================================

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

                itemsProvider =
                    il.kmi.shared.practice.PracticeFacade
                        .ItemsProvider { beltId, topicTitle ->

                            val currentBelt =
                                Belt.fromId(beltId) ?: belt

                            val matchingSubTopic =
                                selectedSubjectFilter
                                    ?.takeIf {
                                        currentBelt.id == belt.id &&
                                                it.topic.normHeb() ==
                                                topicTitle.normHeb()
                                    }
                                    ?.subTopic

                            val sharedItems =
                                sharedItemsFor(
                                    b = currentBelt,
                                    topicTitle = topicTitle,
                                    subTopicTitle =
                                        matchingSubTopic
                                )

                            if (sharedItems.isNotEmpty()) {
                                return@ItemsProvider sharedItems
                            }

                            // Bridge הוא fallback לנושא ללא תת־נושא.
                            if (matchingSubTopic == null) {
                                runCatching {
                                    il.kmi.app.search
                                        .KmiSearchBridge
                                        .itemsFor(
                                            currentBelt,
                                            topicTitle
                                        )
                                }.getOrDefault(emptyList())
                            } else {
                                emptyList()
                            }
                        },
                setsProvider = il.kmi.shared.practice.PracticeFacade.SetProvider { key ->
                    sp.getStringSet(key, emptySet()) ?: emptySet()
                },
                excludedProvider = il.kmi.shared.practice.PracticeFacade.ExcludedProvider { beltId, topicTitle, rawItem, disp ->
                    val excluded = sp.getStringSet("excluded_${beltId}_${topicTitle}", emptySet())
                        ?: emptySet()
                    (rawItem in excluded) || (disp in excluded)
                },
                canonicalKeyFor = { rawItem -> canonicalKeyFor(rawItem) },
                displayNameFor = { rawItem -> displayName(rawItem) }
            )

            /*
       * מחזיר את אותו ex_XXX שבו משתמש
       * ExercisesTabsScreen עבור אותו תרגיל.
       */
            fun exerciseIdForPracticeItem(
                item:
                il.kmi.shared.practice.PracticeItem
            ): String {
                return ExerciseIdentityRegistry.idFor(
                    belt = belt,
                    hebrewTitle =
                        item.displayTitle.trim(),
                    topicKey =
                        item.topicTitle.trim()
                )
            }

            when {
                isUnknownTab -> {
                    val storedUnknownIds =
                        sp.all.keys
                            .asSequence()
                            .filter { key ->
                                key.startsWith(
                                    "unknown_${belt.id}_"
                                )
                            }
                            .flatMap { key ->
                                (
                                        sp.getStringSet(
                                            key,
                                            emptySet()
                                        )
                                            ?: emptySet()
                                        )
                                    .asSequence()
                            }
                            .map {
                                it.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .toSet()

                    built.filter { item ->
                        val exerciseId =
                            exerciseIdForPracticeItem(item)

                        exerciseId in storedUnknownIds ||
                                item.canonicalKey.trim() in
                                storedUnknownIds ||
                                item.displayTitle.trim() in
                                storedUnknownIds
                    }
                }

                isFavoritesTab -> {
                    val favoriteIds =
                        favorites
                            .mapTo(linkedSetOf()) { storedValue ->

                                val cleanValue =
                                    storedValue.trim()

                                if (
                                    cleanValue.matches(
                                        Regex("ex_\\d+")
                                    )
                                ) {
                                    cleanValue
                                } else {
                                    ExerciseIdentityRegistry
                                        .idFor(
                                            belt = belt,
                                            hebrewTitle =
                                                cleanValue,
                                            topicKey = null
                                        )
                                }
                            }

                    built.filter { item ->
                        exerciseIdForPracticeItem(item) in
                                favoriteIds
                    }
                }

                coachStatusForTab != null -> {
                    built.filter { item ->
                        val exerciseId =
                            exerciseIdForPracticeItem(item)

                        val topicKey =
                            item.topicTitle.trim()

                        val baseKey =
                            buildString {
                                append(
                                    "coach_material_progress_"
                                )
                                append(belt.id)
                                append("_")
                                append(topicKey)
                                append("_")
                                append(exerciseId)
                            }

                        val selectedInNewStorage =
                            sp.getBoolean(
                                "${baseKey}_${coachStatusForTab}_selected",
                                false
                            )

                        val selectedInLegacyStorage =
                            sp.getString(
                                "${baseKey}_status",
                                null
                            ) == coachStatusForTab

                        selectedInNewStorage ||
                                selectedInLegacyStorage
                    }
                }

                isSingleTopicFilter -> {
                    val wantedTopic =
                        (
                                resolvedSingleTopic
                                    ?: fixedFilter
                                )
                            .normHeb()

                    built.filter { item ->
                        item.topicTitle
                            .normHeb() ==
                                wantedTopic
                    }
                }

                else -> {
                    built
                }
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
            subTopicTitle =
                selectedSubjectFilter
                    ?.takeIf {
                        it.topic.normHeb() ==
                                statusTopic.normHeb()
                    }
                    ?.subTopic
        )

        return allRawItems.firstOrNull { raw ->
            val rawDisplay = displayName(raw).ifBlank { raw }.trim()

            raw.trim().normHeb() == wantedNorm ||
                    rawDisplay.normHeb() == wantedNorm
        } ?: wantedDisplay
    }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun topicTitleFromStatusKey(statusKey: String): String {
        return statusKey.substringBefore("__").trim().ifBlank { "כללי" }
    }

    fun subTopicTitleFromStatusKey(statusKey: String): String? {
        return statusKey.substringAfter("__", "")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    fun rawAndIndexForStatusKey(
        item: il.kmi.shared.practice.PracticeItem,
        statusKey: String
    ): Pair<String, Int> {
        val topicTitle = topicTitleFromStatusKey(statusKey)
        val subTopicTitle = subTopicTitleFromStatusKey(statusKey)

        val wantedDisplay = item.displayTitle.trim()
        val wantedNorm = wantedDisplay.normHeb()

        val itemsForKey = sharedItemsFor(
            b = belt,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle
        )

        val index = itemsForKey.indexOfFirst { raw ->
            val rawDisplay = displayName(raw).ifBlank { raw }.trim()

            raw.trim().normHeb() == wantedNorm ||
                    rawDisplay.normHeb() == wantedNorm
        }

        if (index >= 0) {
            return itemsForKey[index] to index
        }

        // fallback בטוח: אם לא מצאנו ברשימת הנושא/תת־נושא,
        // משתמשים בשם הגולמי שכבר פתרנו קודם ומיקום 0.
        return statusRawItemFor(item) to 0
    }

    fun statusIdForPractice(
        item: il.kmi.shared.practice.PracticeItem,
        statusKey: String
    ): String {
        val topicTitle = topicTitleFromStatusKey(statusKey)
        val (rawItem, index) = rawAndIndexForStatusKey(
            item = item,
            statusKey = statusKey
        )

        val cleanOriginal = CanonicalIds
            .cleanItem(topicTitle, rawItem)
            .trim()

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanOriginal,
            topicKey = statusKey
        )

        if (resolved.isKnown) {
            return resolved.id
        }

        // חייב להיות זהה ל-MaterialsScreen / SummaryScreen
        return "${resolved.id}_row_$index"
    }

    fun legacyStatusIdForPractice(
        item: il.kmi.shared.practice.PracticeItem,
        statusKey: String
    ): String {
        val (rawItem, index) = rawAndIndexForStatusKey(
            item = item,
            statusKey = statusKey
        )

        val cleanItem = normalizeStatusPart(rawItem)

        return "status_${belt.id}_${statusKey}_${index}_${cleanItem}"
    }

    fun primaryStatusIdForPractice(
        item: il.kmi.shared.practice.PracticeItem
    ): String {
        return statusIdForPractice(
            item = item,
            statusKey = statusTopicFor(item)
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

        val topicKeys = statusTopicKeysFor(item)

        val primaryStatusId = primaryStatusIdForPractice(item)

        // ✅ עדכון מיידי במסך התרגול לפי statusId החדש
        practiceStatusMap[primaryStatusId] = newStatus

        // ✅ שמירה לכל מפתחות הסימון האפשריים:
        // נושא ראשי / כללי / תת־נושא.
        topicKeys.forEach { key ->
            val statusId = statusIdForPractice(
                item = item,
                statusKey = key
            )

            val legacyStatusId = legacyStatusIdForPractice(
                item = item,
                statusKey = key
            )

            practiceStatusMap[statusId] = newStatus

            vm?.setItemStatusNullable(
                belt = belt,
                topic = key,
                item = statusId,
                value = newStatus
            )

            // ✅ שמירה מקומית תואמת ל-MaterialsScreen / SummaryScreen
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                (sp.getStringSet(masteredKey, emptySet()) ?: emptySet()).toMutableSet()
            val unknownSet = (sp.getStringSet(unknownKey, emptySet()) ?: emptySet()).toMutableSet()

            when (newStatus) {
                true -> {
                    masteredSet.add(statusId)
                    unknownSet.remove(statusId)

                    // מנקים fallback ישן אם היה
                    masteredSet.remove(legacyStatusId)
                    unknownSet.remove(legacyStatusId)
                }

                false -> {
                    unknownSet.add(statusId)
                    masteredSet.remove(statusId)

                    // מנקים fallback ישן אם היה
                    masteredSet.remove(legacyStatusId)
                    unknownSet.remove(legacyStatusId)
                }

                null -> {
                    masteredSet.remove(statusId)
                    unknownSet.remove(statusId)

                    masteredSet.remove(legacyStatusId)
                    unknownSet.remove(legacyStatusId)
                }
            }

            sp.edit()
                .putStringSet(masteredKey, masteredSet)
                .putStringSet(unknownKey, unknownSet)
                .apply()
        }

        // ✅ תאימות למסך "כל הרשימות" / רשימת לא יודע:
        // נשאר לפי canonicalKey של PracticeFacade, כי זה מנגנון בחירת תרגילים בלבד.
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
    // עברית מציגה את שם התרגיל המקורי.
    // אנגלית מציגה תרגום דרך ExerciseTitlesEn ומשמשת גם ל־TTS.
    fun uiTitleFor(item: il.kmi.shared.practice.PracticeItem): String {
        val display = item.displayTitle.trim()
        return if (isEnglish) {
            ExerciseTitlesEn.getOrSame(display)
        } else {
            display
        }
    }

    val weightedItems: List<String> = remember(weightedPracticeItems, isEnglish) {
        weightedPracticeItems.map { uiTitleFor(it) }
    }

    var currentIndex by remember { mutableStateOf(0) }

    val currentPracticeItem =
        remember(
            weightedPracticeItems,
            currentIndex
        ) {
            weightedPracticeItems.getOrNull(currentIndex)
        }

    val currentStatusId =
        remember(
            currentPracticeItem
        ) {
            currentPracticeItem
                ?.let {
                    primaryStatusIdForPractice(it)
                }
        }

    val currentPracticeStatus: Boolean? =
        currentStatusId?.let {
            practiceStatusMap[it]
        }

    suspend fun readPracticeStatusFromSources(
        safeVm: KmiViewModel?,
        item: il.kmi.shared.practice.PracticeItem
    ): Pair<Boolean?, String?> {
        val topicKeys = statusTopicKeysFor(item)

        // ✅ 1. אם יש VM — קוראים קודם לפי statusId החדש,
        // ואחריו fallback למפתח status הישן.
        if (safeVm != null) {
            for (key in topicKeys) {
                val statusId = statusIdForPractice(
                    item = item,
                    statusKey = key
                )

                val legacyStatusId = legacyStatusIdForPractice(
                    item = item,
                    statusKey = key
                )

                val valueFromKey: Boolean? =
                    runCatching {
                        safeVm.getItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = statusId
                        )
                    }.getOrNull()
                        ?: runCatching {
                            if (
                                safeVm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = statusId
                                )
                            ) true else null
                        }.getOrNull()
                        ?: runCatching {
                            safeVm.getItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = legacyStatusId
                            )
                        }.getOrNull()
                        ?: runCatching {
                            if (
                                safeVm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = legacyStatusId
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
            val statusId = statusIdForPractice(
                item = item,
                statusKey = key
            )

            val legacyStatusId = legacyStatusIdForPractice(
                item = item,
                statusKey = key
            )

            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                sp.getStringSet(masteredKey, emptySet()) ?: emptySet()

            val unknownSet =
                sp.getStringSet(unknownKey, emptySet()) ?: emptySet()

            val localValue: Boolean? = when {
                masteredSet.contains(statusId) || masteredSet.contains(legacyStatusId) -> true
                unknownSet.contains(statusId) || unknownSet.contains(legacyStatusId) -> false
                else -> null
            }

            if (localValue != null) {
                // ✅ אם יש VM — נרפא גם אותו לפי statusId החדש בלבד
                safeVm?.setItemStatusNullable(
                    belt = belt,
                    topic = key,
                    item = statusId,
                    value = localValue
                )

                return localValue to "$key/SP"
            }
        }

        return null to null
    }

    // ✅ טעינה ממוקדת לתרגיל הנוכחי בכל מעבר תרגיל.
    // זה מבטיח שהעיגול העליון יקבל וי/איקס גם אם הסימון נטען אחרי הרינדור הראשון.
    LaunchedEffect(currentIndex, currentStatusId, marksVersion) {
        val item = currentPracticeItem ?: return@LaunchedEffect
        val statusId = currentStatusId ?: return@LaunchedEffect

        val (fromSources, _) = readPracticeStatusFromSources(
            safeVm = vm,
            item = item
        )

        practiceStatusMap[statusId] = fromSources
    }

    // הסטטוס נטען רק עבור התרגיל הנוכחי.
    // אין לבצע טעינה מלאה של כל רשימת התרגילים בזמן פתיחת המסך.

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

    val practiceSoundScope =
        rememberCoroutineScope()

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

    // מתחילים את התרגול מיד.
    // הצליל מנוגן ברקע ואינו מעכב את הטיימר או את ה־UI.
    fun playLetsGo(onFinished: () -> Unit) {
        if (!isExiting) {
            onFinished()
        }

        practiceSoundScope.launch(Dispatchers.IO) {
            runCatching {
                soundPlayer.play("letsgo")
            }
        }
    }

    // הצליל אינו מעכב את פעולת הסיום.
    fun playStopRest() {
        practiceSoundScope.launch(Dispatchers.IO) {
            runCatching {
                soundPlayer.play("stop_rest")
            }
        }
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
        PracticeStartConfigDialog(
            show = showDurationDialog,
            isEnglish = isEnglish,
            initialMinutes = durationMinutes,
            initialHalfAlert = beepHalfTimeState,
            initialLast10Alert = beepLast10State,
            onDismiss = {
                requestExit()
            },
            onConfirm = { durationSeconds,
                          playHalf,
                          playCountdown ->

                durationMinutes =
                    (durationSeconds / 60)
                        .coerceAtLeast(1)

                beepHalfTimeState =
                    playHalf

                beepLast10State =
                    playCountdown

                sp.edit()
                    .putInt(
                        "timer_minutes",
                        durationMinutes
                    )
                    .putBoolean(
                        "beep_half",
                        beepHalfTimeState
                    )
                    .putBoolean(
                        "beep_last10",
                        beepLast10State
                    )
                    .apply()

                timeLeft =
                    durationMinutes * 60

                currentIndex = 0
                lastSpokenIndex = -1
                halfAnnouncementDone = false
                sessionStarted = true
                isRunning = false
                showDurationDialog = false

                playLetsGo {
                    if (isExiting) {
                        return@playLetsGo
                    }

                    isRunning = true

                    if (
                        currentIndex in
                        weightedItems.indices
                    ) {
                        weightedPracticeItems
                            .getOrNull(
                                currentIndex
                            )
                            ?.let { currentItem ->
                                if (!isMuted) {
                                    speak(
                                        uiTitleFor(
                                            currentItem
                                        )
                                    )
                                }
                            }

                        lastSpokenIndex =
                            currentIndex
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
                    if (!isMuted) {
                        speak(uiTitleFor(currentItem))
                    }
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
                        "Practice by Topic"
                    } else {
                        "תרגול לפי נושא"
                    }
                } else {
                    if (isEnglish) {
                        "Random Practice"
                    } else {
                        "תרגול אקראי"
                    }
                },
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
                showBottomActions = true,
                onHome = onHome,
                onSettings = onOpenSettings,
                onSearch = {
                    showSearch = true
                    onSearch()
                },
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
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
                            selectedSubjectFilter?.let { selectedFilter ->
                                val topic =
                                    topicTitleForUi(
                                        selectedFilter.topic
                                    )

                                val subTopic =
                                    selectedFilter.subTopic
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let(::topicTitleForUi)

                                if (subTopic != null) {
                                    if (isEnglish) {
                                        "$topic\nSubtopic: $subTopic"
                                    } else {
                                        "$topic\nתת־נושא: $subTopic"
                                    }
                                } else {
                                    topic
                                }
                            } ?: if (isEnglish) {
                                "Practice and improve\nyour skills"
                            } else {
                                "תרגלו ושפרו את הידע\nוהביצועים שלכם"
                            },
                        modifier = Modifier.fillMaxWidth(),
                        color = kmiSectionHeaderContentColor(),
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )
                }
            }

            val subjectHeaderPadding = 56.dp

            if (weightedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = subjectHeaderPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) {
                                "No exercises are available for this topic"
                            } else {
                                "אין תרגילים זמינים לנושא זה"
                            },
                            style = KmiTypography.sectionTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = subjectHeaderPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = 150.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PracticeExerciseCenterCard(
                                belt = belt,
                                exerciseTitle =
                                    weightedPracticeItems
                                        .getOrNull(currentIndex)
                                        ?.let { uiTitleFor(it) }
                                        .orEmpty(),
                                exerciseSubtitle =
                                    currentPracticeItem
                                        ?.topicTitle
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?.let {
                                            topicTitleForUi(it)
                                        },
                                timeText =
                                    String.format(
                                        "%02d:%02d",
                                        timeLeft / 60,
                                        timeLeft % 60
                                    ),
                                currentIndex =
                                    currentIndex,
                                totalCount =
                                    weightedItems.size,
                                centerLabel =
                                    when (currentPracticeStatus) {
                                        true ->
                                            if (isEnglish) {
                                                "Known"
                                            } else {
                                                "יודע"
                                            }

                                        false ->
                                            if (isEnglish) {
                                                "Not known"
                                            } else {
                                                "לא יודע"
                                            }

                                        null ->
                                            if (isEnglish) {
                                                "Not marked"
                                            } else {
                                                "לא סומן"
                                            }
                                    },
                                isRunning =
                                    isRunning,
                                isMuted =
                                    isMuted,
                                onToggleRunning = {
                                    isRunning =
                                        !isRunning

                                    if (
                                        isRunning &&
                                        sessionStarted &&
                                        currentIndex in
                                        weightedItems.indices
                                    ) {
                                        weightedPracticeItems
                                            .getOrNull(currentIndex)
                                            ?.let { currentItem ->
                                                speak(
                                                    uiTitleFor(currentItem)
                                                )
                                            }

                                        lastSpokenIndex =
                                            currentIndex
                                    } else {
                                        KmiTtsManager.stop()
                                    }
                                },
                                onToggleMute = {
                                    isMuted =
                                        !isMuted

                                    if (
                                        !isMuted &&
                                        sessionStarted &&
                                        currentIndex in
                                        weightedItems.indices
                                    ) {
                                        weightedPracticeItems
                                            .getOrNull(currentIndex)
                                            ?.let { currentItem ->
                                                speak(
                                                    uiTitleFor(currentItem)
                                                )
                                            }

                                        lastSpokenIndex =
                                            currentIndex
                                    } else {
                                        KmiTtsManager.stop()
                                    }
                                },
                                onCenterClick = {
                                    val nextStatus =
                                        when (currentPracticeStatus) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        }

                                    setPracticeStatus(
                                        weightedPracticeItems
                                            .getOrNull(currentIndex),
                                        nextStatus
                                    )
                                },
                                onCardClick = {
                                    showHelp = true
                                }
                            )
                        }
                    }

                    PracticeBottomControls(
                        isEnglish = isEnglish,
                        showSkip =
                            currentIndex <
                                    weightedItems.lastIndex,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp
                            ),
                        onHelp = {
                            showHelp = true
                        },
                        onSkip = {
                            if (
                                currentIndex <
                                weightedItems.lastIndex
                            ) {
                                currentIndex++
                            }
                        },
                        onFinish = {
                            isRunning = false
                            sessionStarted = false

                            runCatching {
                                KmiTtsManager.stop()
                            }

                            playStopRest()
                            requestExit()
                        }
                    )
                }
            }

            when {
                pickedSearchHit != null -> {
                    val (b, t, item) = pickedSearchHit!!

                    val explanation = remember(b, t, item, isEnglish) {
                        findExplanationForPractice(
                            belt = b,
                            topic = t,
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

                    ExerciseExplanationDialog(
                        title = itemTitleUi,
                        beltLabel = "$topicTitleUi • ${if (isEnglish) b.en else b.heb}",
                        explanation = explanation,
                        noteText = noteText,
                        isFavorite = isFav,
                        accentColor = b.color,
                        isEnglish = isEnglish,
                        onDismiss = {
                            pickedSearchHit = null
                            showHelp = false
                            showSearch = false
                            searchQuery = ""
                        },
                        onEditNote = {
                            showNoteEditor = true
                        },
                        onDeleteNote = {
                            noteText = ""

                            savePracticeNote(
                                prefs = notePrefs,
                                key = noteKey,
                                text = ""
                            )
                        },
                        onToggleFavorite = {
                            toggleFav()
                        }
                    )

                    if (showNoteEditor) {
                        ExerciseNoteEditorDialog(
                            exerciseTitle = itemTitleUi,
                            noteText = noteText,
                            isEnglish = isEnglish,
                            accentColor = b.color,
                            onNoteChange = { noteText = it },
                            onDismiss = {
                                showNoteEditor = false
                            },
                            onSave = {
                                val cleanNote = noteText.trim()
                                noteText = cleanNote

                                savePracticeNote(
                                    prefs = notePrefs,
                                    key = noteKey,
                                    text = cleanNote
                                )

                                showNoteEditor = false
                            }
                        )
                    }
                }

                showHelp -> {
                    val currentHelpItem = weightedPracticeItems.getOrNull(currentIndex)
                    val rawItemForHelp = currentHelpItem?.displayTitle?.trim().orEmpty()

                    val explanation = remember(belt, rawItemForHelp, isEnglish) {
                        if (rawItemForHelp.isBlank()) {
                            if (isEnglish) "No exercise selected to display." else "לא נבחר תרגיל להצגה."
                        } else {
                            findExplanationForPractice(
                                belt = belt,
                                topic = currentHelpItem?.topicTitle.orEmpty(),
                                rawItem = rawItemForHelp,
                                isEnglish = isEnglish
                            )
                        }
                    }

                    val safeItem = rawItemForHelp

                    val safeItemTitleUi = remember(currentHelpItem, safeItem, isEnglish) {
                        if (safeItem.isBlank()) {
                            if (isEnglish) "Exercise" else "תרגיל"
                        } else {
                            currentHelpItem?.let { uiTitleFor(it) } ?: searchTitleForUi(safeItem)
                        }
                    }

                    val helpTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    val helpHorizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

                    val favId = remember(safeItem) { normalizeFavoriteId(safeItem) }

                    val isFav = safeItem.isNotBlank() && favorites.contains(favId)

                    val noteTopic = remember(currentHelpItem, topicFilter) {
                        currentHelpItem?.topicTitle?.trim()?.takeIf { it.isNotBlank() }
                            ?: topicFilter?.takeIf { it.isNotBlank() }
                            ?: "general"
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

                    ExerciseExplanationDialog(
                        title = safeItemTitleUi,
                        beltLabel = "(${if (isEnglish) belt.en else belt.heb})",
                        explanation = explanation,
                        noteText = noteText,
                        isFavorite = isFav,
                        accentColor = belt.color,
                        isEnglish = isEnglish,
                        onDismiss = {
                            showHelp = false
                        },
                        onEditNote = {
                            showNoteEditor = true
                        },
                        onDeleteNote = {
                            noteText = ""

                            savePracticeNote(
                                prefs = notePrefs,
                                key = noteKey,
                                text = ""
                            )
                        },
                        onToggleFavorite = {
                            toggleFav()
                        }
                    )

                    if (showNoteEditor) {
                        ExerciseNoteEditorDialog(
                            exerciseTitle = safeItemTitleUi,
                            noteText = noteText,
                            isEnglish = isEnglish,
                            accentColor = belt.color,
                            onNoteChange = { noteText = it },
                            onDismiss = {
                                showNoteEditor = false
                            },
                            onSave = {
                                val cleanNote = noteText.trim()
                                noteText = cleanNote

                                savePracticeNote(
                                    prefs = notePrefs,
                                    key = noteKey,
                                    text = cleanNote
                                )

                                showNoteEditor = false
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
                                style = KmiTypography.sectionTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                singleLine = true,
                                textStyle = KmiTypography.body,
                                label = {
                                    Text(
                                        text = if (isEnglish) {
                                            "Type exercise name"
                                        } else {
                                            "הקלד/י שם תרגיל"
                                        },
                                        style = KmiTypography.secondary
                                    )
                                }
                            )

                            Spacer(Modifier.height(8.dp))

                            if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                                Text(
                                    text = if (isEnglish) {
                                        "No matching exercises found."
                                    } else {
                                        "לא נמצאו תרגילים תואמים."
                                    },
                                    style = KmiTypography.body,
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
                                        val resultTextAlign =
                                            if (isEnglish) TextAlign.Left else TextAlign.Right
                                        val resultHorizontalAlignment =
                                            if (isEnglish) Alignment.Start else Alignment.End
                                        val hitItemUi = searchTitleForUi(hitItem)
                                        val hitTopicUi = topicTitleForUi(hitTopic)

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            tonalElevation = 1.dp,
                                            shadowElevation = 0.dp,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.35f
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    pickedSearchHit =
                                                        Triple(hitBelt, hitTopic, hitItem)
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
                                                    style = KmiTypography.cardTitle,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = resultTextAlign,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Text(
                                                    text = "$hitTopicUi • ${
                                                        if (isEnglish) {
                                                            hitBelt.en
                                                        } else {
                                                            hitBelt.heb
                                                        }
                                                    }",
                                                    style = KmiTypography.caption,
                                                    textAlign = resultTextAlign,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    showSearch = false
                                    searchQuery = ""
                                }
                            ) {
                                Text(
                                    text = if (isEnglish) "Close" else "סגור",
                                    style = KmiTypography.action
                                )
                            }

                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}


