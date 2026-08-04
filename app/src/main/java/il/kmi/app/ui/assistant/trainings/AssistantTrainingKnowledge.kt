package il.kmi.app.ui.assistant.trainings

import android.content.Context
import android.content.SharedPreferences
import il.kmi.app.training.TrainingStatusEngine
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.assistant.utils.HebrewNormalize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import org.json.JSONArray

/* ============================================================================
   ⭐ AssistantTrainingKnowledge – מנוע NLP מלא לשאלות על אימוני KAMI / ק.מ.י ⭐
   ============================================================================ */

// ============================================================================
// 1) מחלקת זיכרון — AssistantMemory
// ============================================================================

class AssistantMemory(private val sp: SharedPreferences) {

    /**
     * כל הסניפים שאליהם המשתמש רשום בפועל.
     *
     * הפרופיל יכול להכיל:
     * - JSON של כמה סניפים.
     * - מחרוזת מופרדת בפסיקים.
     * - StringSet ישן.
     * - סניף פעיל בודד.
     *
     * העוזר קורא בלבד ואינו משנה את פרופיל המשתמש.
     */
    fun getRegisteredBranches(): List<String> {

        fun splitBranchString(
            rawValue: String
        ): List<String> {
            val raw = rawValue.trim()

            if (raw.isBlank()) {
                return emptyList()
            }

            /*
             * תמיכה במערך JSON:
             * ["סוקולוב", "אופק"]
             */
            if (
                raw.startsWith("[") &&
                raw.endsWith("]")
            ) {
                val fromJson =
                    runCatching {
                        val array = JSONArray(raw)

                        buildList {
                            for (
                            index in 0 until array.length()
                            ) {
                                val value =
                                    array
                                        .optString(index)
                                        .trim()

                                if (value.isNotBlank()) {
                                    add(value)
                                }
                            }
                        }
                    }.getOrDefault(emptyList())

                if (fromJson.isNotEmpty()) {
                    return fromJson
                }
            }

            /*
             * תמיכה ב־CSV ובפורמטים הישנים.
             */
            return raw
                .removePrefix("[")
                .removeSuffix("]")
                .split(
                    ',',
                    ';',
                    '|',
                    '\n'
                )
                .map { value ->
                    value
                        .trim()
                        .trim('"')
                }
                .filter { value ->
                    value.isNotBlank()
                }
        }

        fun valuesForKey(
            key: String
        ): List<String> {
            return when (
                val storedValue = sp.all[key]
            ) {
                is String ->
                    splitBranchString(
                        storedValue
                    )

                is Set<*> ->
                    storedValue
                        .mapNotNull { value ->
                            value
                                ?.toString()
                                ?.trim()
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }

                is List<*> ->
                    storedValue
                        .mapNotNull { value ->
                            value
                                ?.toString()
                                ?.trim()
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }

                else ->
                    emptyList()
            }
        }

        /*
         * תחילה קוראים את שדות ריבוי הסניפים,
         * ולאחר מכן את שדות הסניף הבודד לצורכי תאימות.
         */
        return listOf(
            "branches_json",
            "selected_branches",
            "branches",
            "branch",
            "active_branch",
            "activeBranch",
            "branch_name",
            "selected_branch",
            "user_branch",
            "training_branch",
            "branch2",
            "branch3"
        )
            .flatMap { key ->
                valuesForKey(key)
            }
            .map { branch ->
                branch
                    .replace('־', '-')
                    .replace('–', '-')
                    .replace('—', '-')
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()
            }
            .filter { branch ->
                branch.isNotBlank()
            }
            .distinctBy { branch ->
                branch.lowercase(
                    Locale("he", "IL")
                )
            }
    }

    /**
     * תאימות לקוד הקיים:
     * מחזירים את כל הסניפים כמחרוזת אחת.
     *
     * מנגנון הסינון בהמשך הקובץ כבר מפצל
     * את המחרוזת ומשווה כל סניף בנפרד.
     */
    fun getRegisteredBranch(): String? =
        getRegisteredBranches()
            .joinToString(", ")
            .takeIf { value ->
                value.isNotBlank()
            }

    /**
     * כל הקבוצות שאליהן המשתמש רשום בפועל.
     */
    fun getRegisteredGroups(): List<String> {

        fun splitGroupString(
            rawValue: String
        ): List<String> {
            val raw = rawValue.trim()

            if (raw.isBlank()) {
                return emptyList()
            }

            if (
                raw.startsWith("[") &&
                raw.endsWith("]")
            ) {
                val fromJson =
                    runCatching {
                        val array = JSONArray(raw)

                        buildList {
                            for (
                            index in 0 until array.length()
                            ) {
                                val value =
                                    array
                                        .optString(index)
                                        .trim()

                                if (value.isNotBlank()) {
                                    add(value)
                                }
                            }
                        }
                    }.getOrDefault(emptyList())

                if (fromJson.isNotEmpty()) {
                    return fromJson
                }
            }

            return raw
                .removePrefix("[")
                .removeSuffix("]")
                .split(
                    ',',
                    ';',
                    '|',
                    '\n'
                )
                .map { value ->
                    value
                        .trim()
                        .trim('"')
                }
                .filter { value ->
                    value.isNotBlank()
                }
        }

        fun valuesForKey(
            key: String
        ): List<String> {
            return when (
                val storedValue = sp.all[key]
            ) {
                is String ->
                    splitGroupString(
                        storedValue
                    )

                is Set<*> ->
                    storedValue
                        .mapNotNull { value ->
                            value
                                ?.toString()
                                ?.trim()
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }

                is List<*> ->
                    storedValue
                        .mapNotNull { value ->
                            value
                                ?.toString()
                                ?.trim()
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }

                else ->
                    emptyList()
            }
        }

        return listOf(
            "groups_json",
            "selected_groups",
            "groups",
            "age_groups",
            "age_group",
            "group",
            "active_group",
            "activeGroup",
            "group_name",
            "selected_group",
            "user_group",
            "training_group"
        )
            .flatMap { key ->
                valuesForKey(key)
            }
            .map { group ->
                TrainingCatalog
                    .normalizeGroupName(group)
                    .ifBlank { group.trim() }
            }
            .filter { group ->
                group.isNotBlank()
            }
            .distinctBy { group ->
                group.lowercase(
                    Locale("he", "IL")
                )
            }
    }

    /**
     * תאימות לפונקציות הישנות בקובץ.
     */
    fun getRegisteredGroup(): String? =
        getRegisteredGroups()
            .joinToString(", ")
            .takeIf { value ->
                value.isNotBlank()
            }

    /**
     * זיכרון שיחה נפרד. הוא אינו משנה את פרופיל המשתמש.
     */
    fun setLastBranch(v: String?) {
        sp.edit()
            .putString(
                "assistant_context_branch",
                v?.trim()?.takeIf { it.isNotBlank() }
            )
            .apply()
    }

    fun getLastBranch(): String? =
        sp.getString("assistant_context_branch", null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: getRegisteredBranch()

    fun setLastGroup(v: String?) {
        sp.edit()
            .putString(
                "assistant_context_group",
                v?.trim()?.takeIf { it.isNotBlank() }
            )
            .apply()
    }

    fun getLastGroup(): String? =
        sp.getString("assistant_context_group", null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: getRegisteredGroup()

    fun setLastDay(v: String?) {
        sp.edit()
            .putString(
                "assistant_context_day",
                v?.trim()?.takeIf { it.isNotBlank() }
            )
            .apply()
    }

    fun getLastDay(): String? =
        sp.getString("assistant_context_day", null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun getLastRegion(): String? =
        listOf(
            "region",
            "region_name",
            "user_region",
            "selected_region"
        )
            .firstNotNullOfOrNull { key ->
                sp.getString(key, null)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }

    fun setLastIntent(v: String?) {
        sp.edit()
            .putString("assistant_last_intent", v)
            .apply()
    }

    fun getLastIntent(): String? =
        sp.getString("assistant_last_intent", null)

    fun setLastAnswerContext(v: String?) {
        sp.edit()
            .putString("assistant_last_answer", v)
            .apply()
    }

    fun getLastAnswerContext(): String? =
        sp.getString("assistant_last_answer", null)

    fun clearMemory() {
        sp.edit()
            .remove("assistant_last_intent")
            .remove("assistant_last_answer")
            .remove("assistant_context_branch")
            .remove("assistant_context_group")
            .remove("assistant_context_day")
            .apply()
    }
}

// ============================================================================
// 2) Tokenizer
// ============================================================================

object HebrewTokenizer {
    private val splitRegex = Regex("[ ,:\\-\\n\\t]+")

    fun tokenize(s: String): List<String> =
        s.split(splitRegex).map { it.trim() }.filter { it.isNotEmpty() }
}

// ============================================================================
// 3) Fuzzy Matching Engine
// ============================================================================

object FuzzyEngine {

    fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
        }
        return dp[m][n]
    }

    fun score(a: String, b: String): Int {
        val dist = levenshtein(a, b).toDouble()
        val maxLen = maxOf(a.length, b.length).toDouble()
        if (maxLen == 0.0) return 0
        return (100 * (1 - dist / maxLen)).toInt().coerceIn(0, 100)
    }

    fun bestMatch(input: String, options: List<String>, threshold: Int = 55): String? {
        var best: String? = null
        var bestScore = threshold

        options.forEach { opt ->
            val sc = score(input, opt)
            if (sc > bestScore) {
                best = opt
                bestScore = sc
            }
        }
        return best
    }
}

// ============================================================================
// 5) Intent Classification
// ============================================================================

enum class AssistantIntent {
    ASK_SCHEDULE,
    ASK_NEXT_TRAINING,
    ASK_WHAT_TODAY,
    ASK_TIME,
    ASK_COACH,
    ASK_LOCATION,
    ASK_DURATION,
    ASK_EQUIPMENT,
    ASK_GROUPS_AND_LEVELS,
    ASK_GENERAL,
    ASK_WEEKLY_COUNT,
    ASK_SPECIAL_WEEK,
    UNKNOWN
}

// ============================================================================
// 4) Training Info – בנק שאלות לכפתור "מידע על אימונים"
//     חלוקה לקטגוריות: זמנים | רמות | מיקום | ציוד
// ============================================================================

enum class TrainingInfoCategory(val heb: String) {
    TIMES("זמנים"),
    LEVELS("רמות"),
    LOCATION("מיקום"),
    EQUIPMENT("ציוד")
}

data class TrainingInfoQuestion(
    val category: TrainingInfoCategory,
    val text: String
)

object TrainingInfoQuestionBank {

    // ✅ 15 שאלות מוכנות (אפשר להוסיף/לשנות חופשי)
    val questions: List<TrainingInfoQuestion> = listOf(
        // --- זמנים ---
        TrainingInfoQuestion(TrainingInfoCategory.TIMES, "אילו אימונים יש היום בסניף שלי?"),
        TrainingInfoQuestion(TrainingInfoCategory.TIMES, "באילו ימים ושעות מתקיימים האימונים השבוע?"),
        TrainingInfoQuestion(TrainingInfoCategory.TIMES, "מה האימון הקרוב ביותר שיש היום?"),
        TrainingInfoQuestion(TrainingInfoCategory.TIMES, "מתי האימון הבא לקבוצה שלי?"),

        // --- רמות ---
        TrainingInfoQuestion(TrainingInfoCategory.LEVELS, "האם יש אימונים לפי גילאים?"),
        TrainingInfoQuestion(TrainingInfoCategory.LEVELS, "מה ההבדל בין אימון מתחילים לאימון מתקדמים?"),
        TrainingInfoQuestion(TrainingInfoCategory.LEVELS, "האם האימון הקרוב מתאים לחגורה שלי?"),

        // --- מיקום ---
        TrainingInfoQuestion(TrainingInfoCategory.LOCATION, "איפה מתקיים האימון – באיזה אולם או מיקום?"),
        TrainingInfoQuestion(TrainingInfoCategory.LOCATION, "מה הכתובת של הסניף שלי?"),
        TrainingInfoQuestion(TrainingInfoCategory.LOCATION, "האם אפשר להגיע לאימון בסניף אחר?"),

        // --- ציוד ---
        TrainingInfoQuestion(TrainingInfoCategory.EQUIPMENT, "האם צריך ציוד מיוחד לאימון?"),
        TrainingInfoQuestion(TrainingInfoCategory.EQUIPMENT, "מה להביא לאימון ראשון?"),
        TrainingInfoQuestion(TrainingInfoCategory.EQUIPMENT, "האם חובה כפפות/מגן שיניים באימון?"),
        TrainingInfoQuestion(TrainingInfoCategory.EQUIPMENT, "איזה לבוש מומלץ לאימון?")
    )

    fun byCategory(category: TrainingInfoCategory): List<String> =
        questions.filter { it.category == category }.map { it.text }

    fun groupedHebrew(): Map<String, List<String>> =
        TrainingInfoCategory.entries.associate { cat ->
            cat.heb to byCategory(cat)
        }

    fun allAsPlainList(): List<String> = questions.map { it.text }
}

object IntentDetector {

    private val intentPatterns = mapOf(
        AssistantIntent.ASK_SCHEDULE to listOf(
            "אימונים", "לוח", "לו\"ז", "לוז", "רשימת",
            "schedule", "training schedule", "trainings", "classes", "class list"
        ),
        AssistantIntent.ASK_NEXT_TRAINING to listOf(
            "האימון הבא", "הבא שלי", "אימון הבא",
            "next training", "my next training", "upcoming training", "nearest training"
        ),
        AssistantIntent.ASK_WHAT_TODAY to listOf(
            "מה יש היום", "מה היום", "היום יש",
            "what is today", "what trainings today", "training today", "today training"
        ),
        AssistantIntent.ASK_TIME to listOf(
            "מתי", "באיזו שעה", "שעת", "שעה של",
            "when", "what time", "at what time", "training time"
        ),
        AssistantIntent.ASK_COACH to listOf(
            "מי המאמן", "מי המדריך", "מי מלמד",
            "who is the coach", "coach", "instructor", "who teaches"
        ),
        AssistantIntent.ASK_LOCATION to listOf(
            "איפה", "כתובת", "רחוב", "מיקום",
            "where", "address", "location", "place"
        ),
        AssistantIntent.ASK_DURATION to listOf(
            "כמה זמן", "משך", "כמה נמשך",
            "how long", "duration", "how long is training"
        ),

        AssistantIntent.ASK_EQUIPMENT to listOf(
            "ציוד", "מה להביא", "צריך להביא", "מה צריך להביא", "איזה ציוד",
            "כפפות", "מגני רגליים", "מגן שיניים", "מגן אשכים", "מגנים",
            "equipment", "what to bring", "bring to training", "gloves", "shin guards",
            "mouth guard", "groin guard", "protective gear"
        ),

        AssistantIntent.ASK_WEEKLY_COUNT to listOf(
            "כמה אימונים יש בשבוע", "כמה אימונים בשבוע", "מספר אימונים בשבוע",
            "כמה פעמים בשבוע", "כמה פעמים אני מתאמן בשבוע",
            "how many trainings per week", "how many times a week",
            "weekly trainings", "trainings per week"
        ),

        AssistantIntent.ASK_SPECIAL_WEEK to listOf(
            "אימון מיוחד השבוע", "יש אימון מיוחד השבוע", "אימון חגורה", "אימון פתוח",
            "special training this week", "special class this week",
            "belt training", "open training"
        )
    )

    fun detectIntent(norm: String): AssistantIntent {
        val q = norm.trim()

        fun containsAny(vararg values: String): Boolean {
            return values.any { value -> value in q }
        }

        val asksNext = containsAny(
            "האימון הבא",
            "אימון הבא",
            "האימון הקרוב",
            "אימון קרוב",
            "האימונים הבאים",
            "אימונים הבאים",
            "האימונים הקרובים",
            "אימונים קרובים",
            "הבא שלי",
            "הבאים שלי",
            "הקרוב שלי",
            "הקרובים שלי",
            "מתי אני מתאמן",
            "מתי האימונים הבאים שלי",
            "next training",
            "next trainings",
            "next workout",
            "next workouts",
            "upcoming training",
            "upcoming trainings",
            "upcoming class",
            "upcoming classes",
            "nearest training",
            "when do i train next",
            "my next trainings",
            "my upcoming trainings"
        ) || (
                containsAny(
                    "אימון",
                    "אימונים",
                    "training",
                    "trainings",
                    "workout",
                    "workouts",
                    "class",
                    "classes"
                ) &&
                        containsAny(
                            "הבא",
                            "הבאים",
                            "קרוב",
                            "קרובים",
                            "next",
                            "upcoming"
                        )
                )

        if (asksNext) {
            return AssistantIntent.ASK_NEXT_TRAINING
        }

        if (
            containsAny(
                "מה יש היום",
                "איזה אימון יש היום",
                "אילו אימונים יש היום",
                "יש לי היום",
                "האימון היום",
                "אימון היום",
                "היום יש",
                "today's training",
                "training today",
                "trainings today",
                "classes today",
                "what is today",
                "what do i have today",
                "do i train today"
            )
        ) {
            return AssistantIntent.ASK_WHAT_TODAY
        }

        if (
            containsAny(
                "מי המאמן",
                "מי המדריך",
                "מי מעביר",
                "מי מלמד",
                "שם המאמן",
                "איזה מאמן",
                "who is the coach",
                "which coach",
                "coach name",
                "instructor",
                "who teaches",
                "who runs the training"
            )
        ) {
            return AssistantIntent.ASK_COACH
        }

        if (
            containsAny(
                "איפה",
                "היכן",
                "כתובת",
                "רחוב",
                "מיקום",
                "אולם",
                "איך מגיעים",
                "where",
                "address",
                "location",
                "venue",
                "hall",
                "how do i get"
            )
        ) {
            return AssistantIntent.ASK_LOCATION
        }

        if (
            containsAny(
                "כמה זמן",
                "משך האימון",
                "כמה נמשך",
                "מתי מסתיים",
                "שעת סיום",
                "עד איזו שעה",
                "how long",
                "duration",
                "when does it end",
                "end time",
                "what time does it finish"
            )
        ) {
            return AssistantIntent.ASK_DURATION
        }

        if (
            containsAny(
                "ציוד",
                "מה להביא",
                "מה צריך",
                "לבוש",
                "איך להתלבש",
                "כפפות",
                "מגנים",
                "מגן שיניים",
                "אימון ראשון",
                "פעם ראשונה",
                "equipment",
                "what to bring",
                "what do i need",
                "what to wear",
                "clothing",
                "gloves",
                "protective gear",
                "mouth guard",
                "first training",
                "first class"
            )
        ) {
            return AssistantIntent.ASK_EQUIPMENT
        }

        if (
            containsAny(
                "איזה קבוצות",
                "אילו קבוצות",
                "רשימת קבוצות",
                "קבוצות בסניף",
                "לפי גיל",
                "לפי גילאים",
                "איזה גילאים",
                "קבוצת גיל",
                "מתחילים",
                "מתקדמים",
                "רמות אימון",
                "מתאים לילדים",
                "מתאים לנוער",
                "מתאים למבוגרים",
                "which groups",
                "group list",
                "groups at",
                "age groups",
                "which ages",
                "beginners",
                "advanced",
                "training levels",
                "for children",
                "for youth",
                "for adults"
            )
        ) {
            return AssistantIntent.ASK_GROUPS_AND_LEVELS
        }

        if (
            containsAny(
                "כמה אימונים בשבוע",
                "כמה פעמים בשבוע",
                "מספר אימונים",
                "כמות אימונים",
                "how many trainings",
                "how many classes",
                "how many times a week",
                "weekly count"
            )
        ) {
            return AssistantIntent.ASK_WEEKLY_COUNT
        }

        if (
            containsAny(
                "אימון מיוחד",
                "אימון פתוח",
                "אימון חגורה",
                "אירוע השבוע",
                "special training",
                "special class",
                "open training",
                "belt training"
            )
        ) {
            return AssistantIntent.ASK_SPECIAL_WEEK
        }

        if (
            containsAny(
                "מתי",
                "באיזו שעה",
                "איזו שעה",
                "שעת האימון",
                "שעות אימון",
                "ימים ושעות",
                "when",
                "what time",
                "at what time",
                "training time",
                "class time",
                "days and times"
            )
        ) {
            return AssistantIntent.ASK_TIME
        }

        if (
            containsAny(
                "לוח אימונים",
                "לוח",
                "לו\"ז",
                "לוז",
                "רשימת אימונים",
                "כל האימונים",
                "אילו אימונים",
                "איזה אימונים",
                "תראה אימונים",
                "הצג אימונים",
                "השבוע",
                "שבוע הבא",
                "schedule",
                "training schedule",
                "class schedule",
                "training list",
                "all trainings",
                "all classes",
                "show trainings",
                "show classes",
                "this week",
                "next week"
            )
        ) {
            return AssistantIntent.ASK_SCHEDULE
        }

        if (
            containsAny(
                "אימון",
                "אימונים",
                "קבוצה",
                "סניף",
                "מאמן",
                "training",
                "trainings",
                "workout",
                "class",
                "classes",
                "group",
                "branch",
                "coach"
            )
        ) {
            return AssistantIntent.ASK_GENERAL
        }

        return AssistantIntent.UNKNOWN
    }
}

object EntityExtractor {

    // ✅ "הכי קרוב אליי" / "הקרוב ביותר" וכו'
    fun wantsNearest(norm: String): Boolean {
        val keys = listOf(
            "הכי קרוב", "קרוב אליי", "הקרוב אליי", "הקרוב ביותר",
            "לידי", "ליד הבית", "קרוב לבית", "בסביבה", "באזור שלי",
            "nearest", "closest", "near me", "nearby", "closest to me", "around me"
        )
        return keys.any { it in norm }
    }

    // ✅ "האימונים הבאים" / "האימונים הקרובים" וכו'
    fun wantsUpcoming(norm: String): Boolean {
        val keys = listOf(
            "האימונים הבאים", "מה האימונים הבאים", "אימונים הבאים",
            "האימונים הקרובים", "מה האימונים הקרובים", "אימונים קרובים",
            "מה האימון הקרוב", "האימון הקרוב",
            "upcoming trainings", "next trainings", "upcoming classes",
            "next classes", "next training", "upcoming training"
        )
        return keys.any { it in norm }
    }

    fun wantsPast(norm: String): Boolean {
        val keys = listOf(
            "האימון האחרון",
            "אימון אחרון",
            "האימונים האחרונים",
            "אימונים אחרונים",
            "אימונים קודמים",
            "האימונים הקודמים",
            "אימונים שהיו",
            "האימונים שהיו",
            "אימונים שעברו",
            "אימונים מהעבר",
            "באילו אימונים הייתי",
            "last training",
            "last trainings",
            "my last training",
            "my last trainings",
            "recent training",
            "recent trainings",
            "past training",
            "past trainings",
            "previous training",
            "previous trainings",
            "trainings i attended",
            "trainings i had",
            "workouts i attended"
        )

        val containsKnownPhrase =
            keys.any { key ->
                key in norm
            }

        /*
         * תמיכה במספר שמופיע בין מילת הזמן לבין סוג האימון:
         * last 3 trainings
         * last four workouts
         * previous 5 classes
         */
        val containsNumberedEnglishPastRequest =
            Regex(
                pattern =
                    """\b(last|recent|past|previous)\s+(?:\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s+(training|trainings|workout|workouts|class|classes)\b""",
                option = RegexOption.IGNORE_CASE
            ).containsMatchIn(norm)

        return containsKnownPhrase ||
                containsNumberedEnglishPastRequest
    }

    // ✅ יום בשבוע → Calendar.*
    fun getDayIndex(hebrewDay: String): Int? {
        return when (hebrewDay.trim()) {
            "ראשון" -> Calendar.SUNDAY
            "שני" -> Calendar.MONDAY
            "שלישי" -> Calendar.TUESDAY
            "רביעי" -> Calendar.WEDNESDAY
            "חמישי" -> Calendar.THURSDAY
            "שישי" -> Calendar.FRIDAY
            "שבת" -> Calendar.SATURDAY
            else -> null
        }
    }

    // ✅ דקות התחלה מתוך "HH:mm–HH:mm" (תומך גם ב-"-")
    fun parseStartMinutes(timeRange: String): Int? {
        return try {
            val s = timeRange.substringBefore("–").substringBefore("-").trim()
            val h = s.substringBefore(":").toInt()
            val m = s.substringAfter(":", "0").toIntOrNull() ?: 0
            h * 60 + m
        } catch (_: Throwable) {
            null
        }
    }

    fun parseStartHour(range: String): Int? {
        return try {
            range.substringBefore("–").substringBefore("-").substringBefore(":").toInt()
        } catch (_: Throwable) {
            null
        }
    }

    fun detectDay(norm: String): String? {
        val cal = Calendar.getInstance(Locale("he", "IL"))

        fun dayNameOf(c: Calendar): String =
            SimpleDateFormat("EEEE", Locale("he", "IL"))
                .format(c.time)
                .replace("יום ", "")
                .trim()

        val today = dayNameOf(cal)
        val tomorrow = dayNameOf((cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) })
        val after = dayNameOf((cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) })

        if ("היום" in norm || "today" in norm) return today
        if ("מחרתיים" in norm || "day after tomorrow" in norm) return after
        if ("מחר" in norm || "tomorrow" in norm) return tomorrow

        val days = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
        days.forEach { d ->
            if (d in norm || "ב$d" in norm) return d
        }

        val englishDays = mapOf(
            "sunday" to "ראשון",
            "monday" to "שני",
            "tuesday" to "שלישי",
            "wednesday" to "רביעי",
            "thursday" to "חמישי",
            "friday" to "שישי",
            "saturday" to "שבת"
        )

        englishDays.forEach { (en, he) ->
            if (en in norm || "on $en" in norm) return he
        }

        return null
    }

    fun branchCity(branch: String): String =
        branch.substringBefore("–").substringBefore("-").trim()

    fun wantsNextWeek(norm: String): Boolean {
        val keys = listOf(
            "שבוע הבא",
            "בשבוע הבא",
            "לשבוע הבא",
            "של שבוע הבא",
            "האימונים בשבוע הבא",
            "האימונים שלי בשבוע הבא",
            "האימונים שלי לשבוע הבא",
            "next week",
            "for next week",
            "during next week",
            "trainings next week",
            "my trainings next week",
            "classes next week",
            "my classes next week"
        )

        return keys.any { it in norm }
    }

    fun wantsThisWeek(norm: String): Boolean {
        if (wantsNextWeek(norm)) return false

        val keys = listOf(
            "השבוע",
            "בשבוע הזה",
            "השבוע הנוכחי",
            "במהלך השבוע",
            "בשבוע הקרוב",
            "שבוע קרוב",
            "this week",
            "current week",
            "coming week",
            "upcoming week"
        )

        return keys.any { it in norm }
    }

    fun wantsTrainingList(
        norm: String
    ): Boolean {
        val hasTrainingWord =
            listOf(
                "אימון",
                "אימונים",
                "אימוני",
                "training",
                "trainings",
                "workout",
                "workouts",
                "class",
                "classes"
            ).any { word ->
                word in norm
            }

        if (!hasTrainingWord) {
            return false
        }

        val hasListMeaning =
            listOf(
                "רשימה",
                "תן",
                "תני",
                "תביא",
                "תראה",
                "תציג",
                "הצג",
                "ספר לי",
                "פרט",
                "כמה אימונים",
                "אימונים הבאים",
                "האימונים הבאים",
                "אימונים קרובים",
                "האימונים הקרובים",
                "עוד אימונים",
                "השבוע",
                "שבוע קרוב",
                "שבוע הבא",
                "בשבוע הבא",
                "לשבוע הבא",
                "שבעת הימים הקרובים",
                "ימים הקרובים",
                "מה צפוי",
                "מה מתוכנן",
                "list",
                "show",
                "give",
                "display",
                "upcoming",
                "next trainings",
                "next classes",
                "this week",
                "next week",
                "coming week",
                "scheduled"
            ).any { phrase ->
                phrase in norm
            }

        val hasRequestedNumber =
            Regex(
                """(?<!\d)(20|1[0-9]|[2-9])(?!\d)"""
            ).containsMatchIn(norm)
                    ||
                    listOf(
                        "שניים",
                        "שתיים",
                        "שלושה",
                        "שלוש",
                        "ארבעה",
                        "ארבע",
                        "חמישה",
                        "חמש",
                        "שישה",
                        "שש",
                        "שבעה",
                        "שבע",
                        "שמונה",
                        "תשעה",
                        "תשע",
                        "עשרה",
                        "עשר"
                    ).any { it in norm }

        return hasListMeaning ||
                hasRequestedNumber
    }

    fun requestedTrainingCount(norm: String): Int? {
        val hasUpcomingListContext =
            wantsTrainingList(norm)

        if (!hasUpcomingListContext) {
            return null
        }

        val numericCount = Regex(
            """(?<!\d)(20|1[0-9]|[1-9])(?!\d)"""
        )
            .find(norm)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        if (numericCount != null) {
            return numericCount.coerceIn(1, 20)
        }

        val numberWords = linkedMapOf(
            "אחד" to 1,
            "אחת" to 1,
            "שניים" to 2,
            "שתיים" to 2,
            "שני" to 2,
            "שתי" to 2,
            "שלושה" to 3,
            "שלוש" to 3,
            "ארבעה" to 4,
            "ארבע" to 4,
            "חמישה" to 5,
            "חמש" to 5,
            "שישה" to 6,
            "שש" to 6,
            "שבעה" to 7,
            "שבע" to 7,
            "שמונה" to 8,
            "תשעה" to 9,
            "תשע" to 9,
            "עשרה" to 10,
            "עשר" to 10
        )

        numberWords.forEach { (word, value) ->
            if (word in norm) {
                return value
            }
        }

        val countWords = linkedMapOf(
            10 to listOf("עשרה", "עשר", "ten"),
            9 to listOf("תשעה", "תשע", "nine"),
            8 to listOf("שמונה", "eight"),
            7 to listOf("שבעה", "שבע", "seven"),
            6 to listOf("שישה", "שש", "six"),
            5 to listOf("חמישה", "חמש", "five"),
            4 to listOf("ארבעה", "ארבע", "four"),
            3 to listOf("שלושה", "שלוש", "three"),
            2 to listOf("שני", "שניים", "שתיים", "two"),
            1 to listOf("אחד", "אחת", "one")
        )

        return countWords.entries
            .firstOrNull { (_, words) ->
                words.any { word ->
                    Regex(
                        "(^|\\s)${Regex.escape(word)}(?=\\s|$)"
                    ).containsMatchIn(norm)
                }
            }
            ?.key
    }

    fun detectBranch(norm: String): String? {
        val allBranches = TrainingCatalog.branchesByRegion.flatMap { it.value }.distinct()
        fun n(s: String): String = HebrewNormalize.normalize(s).lowercase(Locale("he", "IL"))

        val aliases: Map<String, List<String>> = allBranches.associateWith { branch ->
            val clean = n(branch).replace("–", " ").replace("-", " ")
            val parts = clean.split(" ").filter { it.length >= 3 }
            buildList {
                add(clean)
                addAll(parts)
                if ("סוקולוב" in clean) add("סוקולוב")
                if ("אופק" in clean) add("אופק")
                if ("נורדאו" in clean) add("נורדאו")
                if ("עזריאל" in clean) add("עזריאל")
            }.distinct()
        }

        val tokens = HebrewTokenizer.tokenize(norm).map {
            it.removePrefix("בסניף").removePrefix("בס").removePrefix("ב").trim()
        }

        val normText = n(norm)
        aliases.forEach { (branch, keys) ->
            if (keys.any { k -> normText.contains(k) }) return branch
        }

        var best: String? = null
        var bestScore = 0
        for (token in tokens) {
            val tk = n(token)
            for (branch in allBranches) {
                val sc = FuzzyEngine.score(tk, n(branch))
                if (sc > bestScore) {
                    bestScore = sc
                    best = branch
                }
            }
        }
        return if (bestScore >= 70) best else null
    }

    private val groupKeywords: Map<String, List<String>> =
        TrainingCatalog.ageGroupsByBranch.values
            .flatten()
            .map { TrainingCatalog.normalizeGroupName(it) }
            .distinct()
            .associateWith { group ->
                val g = group.lowercase(Locale("he", "IL"))
                val keys = mutableListOf<String>()
                keys += g.split(" ", "-", "–").map { it.trim() }.filter { it.isNotEmpty() }
                if ("ילד" in g || "כיתה" in g) keys += listOf("ילדים", "כיתה", "כיתות")
                if ("נוער" in g) keys += "נוער"
                if ("בוגר" in g) keys += listOf("בוגרים", "מבוגרים")
                if (g == "נוער + בוגרים") keys += listOf("נוער ובוגרים", "נוער בוגרים", "נוער+בוגרים")
                keys.distinct()
            }

    fun detectGroup(norm: String): String? {
        return groupKeywords.entries.firstOrNull { (_, keys) -> keys.any { it in norm } }?.key
    }

    fun detectTimeRange(norm: String): IntRange? {
        return when {
            "בוקר" in norm -> 6..12
            "צהריים" in norm || "צהרים" in norm -> 12..15
            "אחר הצהריים" in norm || "אחה\"צ" in norm -> 15..18
            "ערב" in norm -> 18..23
            "עכשיו" in norm -> {
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                h..h
            }
            else -> null
        }
    }
}

// ============================================================================
// 7) TrainingRow + בניית טבלת אימונים
// ============================================================================

data class TrainingRow(
    val branchName: String,
    val groupName: String,
    val dayName: String,
    val timeRange: String,
    val location: String,
    val coachName: String,
    val startAtMillis: Long,
    val endAtMillis: Long?
)

data class TrainingAssistantCard(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String?,
    val branchName: String,
    val groupName: String,
    val location: String,
    val coachName: String,
    val statusCode: String,
    val statusHe: String,
    val statusEn: String
)

object TrainingTableBuilder {

    private val heLocale =
        Locale("he", "IL")

    private val dayFormatter =
        SimpleDateFormat(
            "EEEE",
            heLocale
        )

    private val timeFormatter =
        SimpleDateFormat(
            "HH:mm",
            heLocale
        )

    fun build(): List<TrainingRow> {
            val rows = mutableListOf<TrainingRow>()

            TrainingCatalog.branchesByRegion.forEach { (_, branches) ->
                branches.forEach { branch ->
                    val groups = TrainingCatalog.ageGroupsByBranch[branch] ?: emptyList()

                    groups.forEach { groupRaw ->
                        val normGroup = TrainingCatalog.normalizeGroupName(groupRaw)
                        val trainings = TrainingCatalog.trainingsFor(branch, groupRaw)

                        trainings.forEach { td ->
                            /*
                             * משך האימון נשמר כדי לזהות אימון
                             * שמתקיים כעת ולא לקדם אותו מיד לשבוע הבא.
                             */
                            val durationMillis =
                                td.endMillis
                                    ?.minus(td.startMillis)
                                    ?.takeIf { duration ->
                                        duration > 0L
                                    }

                            val nowMillis =
                                System.currentTimeMillis()

                            val firstOccurrence =
                                (td.cal.clone() as Calendar).apply {
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)

                                    while (true) {
                                        val effectiveEndMillis =
                                            durationMillis?.let { duration ->
                                                timeInMillis + duration
                                            } ?: timeInMillis

                                        if (
                                            effectiveEndMillis >
                                            nowMillis
                                        ) {
                                            break
                                        }

                                        add(Calendar.DAY_OF_YEAR, 7)
                                    }
                                }

                            /*
                             * יוצרים חמישה מופעים קודמים ואת חמשת
                             * המופעים הנוכחיים/הבאים של כל אימון.
                             */
                            repeat(10) { occurrenceIndex ->
                                val weekOffset =
                                    occurrenceIndex - 5

                                val occurrence =
                                    (firstOccurrence.clone() as Calendar).apply {
                                        add(
                                            Calendar.DAY_OF_YEAR,
                                            weekOffset * 7
                                        )
                                    }

                                val occurrenceStartMillis =
                                    occurrence.timeInMillis

                                val occurrenceEndMillis =
                                    durationMillis?.let { duration ->
                                        occurrenceStartMillis + duration
                                    }

                                val cleanStartTime =
                                    timeFormatter.format(
                                        java.util.Date(
                                            occurrenceStartMillis
                                        )
                                    )

                                val cleanEndTime =
                                    occurrenceEndMillis?.let { endMillis ->
                                        timeFormatter.format(
                                            java.util.Date(endMillis)
                                        )
                                    } ?: Regex(
                                        """(?:[01]\d|2[0-3]):[0-5]\d"""
                                    )
                                        .find(td.end)
                                        ?.value
                                        .orEmpty()

                                val cleanTimeRange =
                                    if (cleanEndTime.isNotBlank()) {
                                        "$cleanStartTime–$cleanEndTime"
                                    } else {
                                        cleanStartTime
                                    }

                                rows += TrainingRow(
                                    branchName = branch,
                                    groupName = normGroup,
                                    dayName = dayFormatter.format(
                                        occurrence.time
                                    ),
                                    timeRange = cleanTimeRange,
                                    location =
                                        TrainingCatalog.placeFor(branch),
                                    coachName = td.coach,
                                    startAtMillis =
                                        occurrenceStartMillis,
                                    endAtMillis =
                                        occurrenceEndMillis
                                )
                            }
                        }
                    }
                }
            }

            return rows.sortedBy { it.startAtMillis }
        }
    }


// ============================================================================
// 8) Answer Builder
// ============================================================================

object AnswerBuilder {

    private fun tr(isEnglish: Boolean, he: String, en: String): String {
        return if (isEnglish) en else he
    }

    private fun cleanDayName(dayName: String): String {
        return dayName
            .replace("יום", "")
            .trim()
    }

    private fun dayPhrase(
        dayName: String,
        isEnglish: Boolean = false
    ): String {
        val clean = cleanDayName(dayName)
        return when {
            clean.isBlank() -> ""
            isEnglish -> "on $clean"
            else -> "ביום $clean"
        }
    }

    fun buildEquipment(isEnglish: Boolean = false): String {
        return if (isEnglish) {
            """
For training, it is recommended to bring basic protective equipment:
• Boxing gloves
• Shin guards
• Mouth guard
In addition, a groin guard is strongly recommended for maximum safety during training.
""".trim()
        } else {
            """
לאימון מומלץ להגיע עם ציוד מגן בסיסי:
• כפפות אגרוף
• מגני רגליים
• מגן שיניים
בנוסף, מומלץ מאוד להשתמש גם במגן אשכים לשמירה על בטיחות מרבית במהלך האימון.
""".trim()
        }
    }

    fun buildTodayTraining(
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {

        val todayName = SimpleDateFormat("EEEE", Locale("he", "IL"))
            .format(Calendar.getInstance().time)

        val todayTrainings = list.filter { it.dayName.contains(todayName) }

        if (todayTrainings.isEmpty()) {
            return tr(isEnglish, "אין לך אימון היום.", "You do not have training today.")
        }

        val next = todayTrainings.minByOrNull { it.startAtMillis }
            ?: return tr(isEnglish, "אין אימון היום.", "There is no training today.")

        val spokenTime = next.timeRange.substringBefore("–").substringBefore("-").trim()

        return if (isEnglish) {
            "Today you have training at $spokenTime, " +
                    "at ${next.branchName}, " +
                    "for ${next.groupName}. " +
                    "The coach is ${next.coachName}."
        } else {
            "היום יש לך אימון בשעה $spokenTime, " +
                    "בסניף ${next.branchName}, " +
                    "לקבוצת ${next.groupName}. " +
                    "המאמן הוא ${next.coachName}."
        }
    }

    fun buildUpcomingTrainings(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        limit: Int = 8,
        isEnglish: Boolean = false,
        past: Boolean = false,
        statusProvider:
        ((TrainingRow) ->
        TrainingStatusEngine.Status)? = null
    ): String {

        val safeLimit =
            limit.coerceIn(1, 20)

        val nowMillis =
            System.currentTimeMillis()

        val filtered = list
            .filter { training ->
                val effectiveEndMillis =
                    training.endAtMillis
                        ?: training.startAtMillis

                if (past) {
                    effectiveEndMillis <= nowMillis
                } else {
                    effectiveEndMillis > nowMillis
                }
            }
            .distinctBy { training ->
                listOf(
                    training.branchName,
                    TrainingCatalog.normalizeGroupName(training.groupName),
                    training.startAtMillis
                ).joinToString("|")
            }

        val sorted =
            if (past) {
                filtered.sortedByDescending { training ->
                    training.startAtMillis
                }
            } else {
                filtered.sortedBy { training ->
                    training.startAtMillis
                }
            }.take(safeLimit)

        if (sorted.isEmpty()) {
            return if (past) {
                tr(
                    isEnglish,
                    "לא מצאתי אימונים קודמים.",
                    "I could not find previous trainings."
                )
            } else {
                tr(
                    isEnglish,
                    "לא מצאתי אימונים קרובים.",
                    "I could not find upcoming trainings."
                )
            }
        }

        val title =
            if (past) {
                if (isEnglish) {
                    when {
                        branch != null && group != null ->
                            "Recent trainings at $branch for $group:"

                        branch != null ->
                            "Recent trainings at $branch:"

                        group != null ->
                            "Recent trainings for $group:"

                        else ->
                            "Recent trainings I found:"
                    }
                } else {
                    when {
                        branch != null && group != null ->
                            "האימונים האחרונים בסניף $branch לקבוצה $group:"

                        branch != null ->
                            "האימונים האחרונים בסניף $branch:"

                        group != null ->
                            "האימונים האחרונים לקבוצה $group:"

                        else ->
                            "האימונים האחרונים שמצאתי:"
                    }
                }
            } else if (isEnglish) {
                when {
                    branch != null && group != null ->
                        "Upcoming trainings at $branch for $group:"

                    branch != null ->
                        "Upcoming trainings at $branch:"

                    group != null ->
                        "Upcoming trainings for $group:"

                    else ->
                        "Upcoming trainings I found:"
                }
            } else {
                when {
                    branch != null && group != null ->
                        "האימונים הבאים בסניף $branch לקבוצה $group:"

                    branch != null ->
                        "האימונים הבאים בסניף $branch:"

                    group != null ->
                        "האימונים הבאים לקבוצה $group:"

                    else ->
                        "האימונים הבאים שמצאתי:"
                }
            }

        return buildString {
            append(title).append('\n')
            val dateFormatter = SimpleDateFormat(
                "dd/MM/yyyy",
                if (isEnglish) Locale.US else Locale("he", "IL")
            )

            sorted.forEach { training ->
                val dateText = dateFormatter.format(
                    Date(training.startAtMillis)
                )

                val trainingStatus =
                    statusProvider?.invoke(training)

                val statusText =
                    trainingStatus?.displayText(
                        isEnglish
                    ).orEmpty()

                if (isEnglish) {
                    append("• ${training.dayName}, $dateText\n")
                    append("  Time: ${training.timeRange}\n")
                    append("  Branch: ${training.branchName}\n")
                    append("  Group: ${training.groupName}\n")

                    if (training.location.isNotBlank()) {
                        append("  Location: ${training.location}\n")
                    }

                    if (training.coachName.isNotBlank()) {
                        append("  Coach: ${training.coachName}\n")
                    }

                    if (statusText.isNotBlank()) {
                        append("  Status: $statusText\n")
                    }

                    append('\n')
                } else {
                    append("• ${training.dayName}, $dateText\n")
                    append("  שעה: ${training.timeRange}\n")
                    append("  סניף: ${training.branchName}\n")
                    append("  קבוצה: ${training.groupName}\n")

                    if (training.location.isNotBlank()) {
                        append("  מיקום: ${training.location}\n")
                    }

                    if (training.coachName.isNotBlank()) {
                        append("  מאמן: ${training.coachName}\n")
                    }

                    if (statusText.isNotBlank()) {
                        append("  סטטוס: $statusText\n")
                    }

                    append('\n')
                }
            }

            append(
                if (isEnglish) {
                    "\n(You may attend even if you are not registered to this branch)"
                } else {
                    "\n(אפשר להגיע גם אם אינך רשום לסניף)"
                }
            )
        }.trim()
    }

    fun buildFullSchedule(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        day: String?,
        isEnglish: Boolean = false
    ): String = buildString {

        when {
            branch != null && group != null && day != null ->
                append(
                    if (isEnglish) {
                        "Trainings at $branch for $group on $day (you may attend even if you are not registered to this branch):\n"
                    } else {
                        "האימונים בסניף $branch לקבוצה $group ביום $day (אפשר להגיע גם אם אינך רשום לסניף):\n"
                    }
                )

            branch != null && group != null ->
                append(
                    if (isEnglish) {
                        "Trainings at $branch for $group (you may attend even if you are not registered to this branch):\n"
                    } else {
                        "האימונים בסניף $branch לקבוצה $group (אפשר להגיע גם אם אינך רשום לסניף):\n"
                    }
                )

            branch != null && day != null ->
                append(
                    if (isEnglish) {
                        "Trainings at $branch on $day (you may attend even if you are not registered to this branch):\n"
                    } else {
                        "האימונים בסניף $branch ביום $day (אפשר להגיע גם אם אינך רשום לסניף):\n"
                    }
                )

            group != null && day != null ->
                append(
                    if (isEnglish) {
                        "Trainings for $group on $day (you may attend even if you are not registered to this group):\n"
                    } else {
                        "האימונים לקבוצה $group ביום $day (אפשר להגיע גם אם אינך רשום לקבוצה הזו):\n"
                    }
                )

            branch != null ->
                append(
                    if (isEnglish) {
                        "Trainings at $branch (you may attend even if you are not registered to this branch):\n"
                    } else {
                        "האימונים בסניף $branch (אפשר להגיע גם אם אינך רשום לסניף):\n"
                    }
                )

            group != null ->
                append(
                    if (isEnglish) {
                        "Trainings for $group (you may attend even if you are not registered to this group):\n"
                    } else {
                        "האימונים לקבוצה $group (אפשר להגיע גם אם אינך רשום לקבוצה הזו):\n"
                    }
                )

            day != null ->
                append(
                    if (isEnglish) {
                        "Trainings on $day:\n"
                    } else {
                        "האימונים ביום $day:\n"
                    }
                )

            else ->
                append(
                    if (isEnglish) {
                        "Here is the training schedule I found (you may train at any branch):\n"
                    } else {
                        "להלן לוח האימונים שמצאתי (ניתן להגיע להתאמן בכל סניף):\n"
                    }
                )
        }

        list.groupBy { it.branchName }.forEach { (b, branchList) ->
            append(
                if (isEnglish) {
                    "\nBranch $b:\n"
                } else {
                    "\nסניף $b:\n"
                }
            )

            branchList.groupBy { it.groupName }.forEach { (g, groupList) ->
                append(
                    if (isEnglish) {
                        "  Group: $g\n"
                    } else {
                        "  קבוצה: $g\n"
                    }
                )

                groupList.forEach { r ->
                    append(
                        if (isEnglish) {
                            "    ${r.dayName} – ${r.timeRange} – Coach: ${r.coachName}\n"
                        } else {
                            "    ${r.dayName} – ${r.timeRange} – מאמן: ${r.coachName}\n"
                        }
                    )
                }
            }
        }
    }

    fun buildDuration(
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {
        val durations = list.mapNotNull {
            val (s, e) = it.timeRange.split("–").takeIf { it.size == 2 } ?: return@mapNotNull null

            val sh = s.substringBefore(":").toIntOrNull() ?: return@mapNotNull null
            val sm = s.substringAfter(":").toIntOrNull() ?: return@mapNotNull null
            val eh = e.substringBefore(":").toIntOrNull() ?: return@mapNotNull null
            val em = e.substringAfter(":").toIntOrNull() ?: return@mapNotNull null

            val start = sh * 60 + sm
            val end = eh * 60 + em
            if (end >= start) end - start else end + 1440 - start
        }

        if (durations.isEmpty()) {
            return tr(
                isEnglish,
                "לא הצלחתי לחשב את משך האימון.",
                "I could not calculate the training duration."
            )
        }

        val avg = durations.average().toInt()
        return tr(
            isEnglish,
            "משך אימון ממוצע הוא בערך ${avg} דקות.",
            "The average training duration is about ${avg} minutes."
        )
    }

    fun buildCoach(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        isEnglish: Boolean = false
    ): String {
        val weeklyRows =
            list
                .filter { training ->
                    training.coachName.isNotBlank()
                }
                .distinctBy { training ->
                    listOf(
                        training.branchName,
                        TrainingCatalog.normalizeGroupName(
                            training.groupName
                        ),
                        training.dayName,
                        training.timeRange,
                        training.coachName
                    ).joinToString("|")
                }

        if (weeklyRows.isEmpty()) {
            return tr(
                isEnglish,
                "לא מצאתי מאמן התואם לסניף או לקבוצה שביקשת.",
                "I could not find a coach matching the requested branch or group."
            )
        }

        val coachesByGroup =
            weeklyRows
                .groupBy { training ->
                    training.groupName
                }
                .mapValues { (_, rows) ->
                    rows
                        .map { training ->
                            training.coachName
                        }
                        .filter { coachName ->
                            coachName.isNotBlank()
                        }
                        .distinct()
                }
                .filterValues { coaches ->
                    coaches.isNotEmpty()
                }

        if (coachesByGroup.isEmpty()) {
            return tr(
                isEnglish,
                "לא מצאתי את שם המאמן.",
                "I could not find the coach name."
            )
        }

        if (
            group != null &&
            coachesByGroup.size == 1
        ) {
            val coaches =
                coachesByGroup.values
                    .first()
                    .joinToString(", ")

            return if (isEnglish) {
                buildString {
                    append("The coach")

                    if (coachesByGroup.values.first().size > 1) {
                        append("es are ")
                    } else {
                        append(" is ")
                    }

                    append(coaches)

                    if (!branch.isNullOrBlank()) {
                        append(" at ")
                        append(branch)
                    }

                    append(" for ")
                    append(group)
                    append(".")
                }
            } else {
                buildString {
                    append(
                        if (
                            coachesByGroup.values
                                .first()
                                .size > 1
                        ) {
                            "המאמנים"
                        } else {
                            "המאמן"
                        }
                    )

                    if (!branch.isNullOrBlank()) {
                        append(" בסניף ")
                        append(branch)
                    }

                    append(" לקבוצת ")
                    append(group)
                    append(" הם: ")
                    append(coaches)
                    append(".")
                }
            }
        }

        return buildString {
            if (isEnglish) {
                if (!branch.isNullOrBlank()) {
                    append("Coaches at ")
                    append(branch)
                    append(":\n")
                } else {
                    append("Coaches found:\n")
                }
            } else {
                if (!branch.isNullOrBlank()) {
                    append("המאמנים בסניף ")
                    append(branch)
                    append(":\n")
                } else {
                    append("המאמנים שמצאתי:\n")
                }
            }

            coachesByGroup.forEach { (groupName, coaches) ->
                append("• ")

                if (isEnglish) {
                    append(groupName)
                    append(": ")
                } else {
                    append("קבוצת ")
                    append(groupName)
                    append(": ")
                }

                append(
                    coaches.joinToString(", ")
                )
                append('\n')
            }
        }.trim()
    }

    fun buildLocation(
        branch: String?,
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {
        val requestedBranches =
            if (!branch.isNullOrBlank()) {
                branch
                    .split(
                        ',',
                        ';',
                        '|',
                        '\n'
                    )
                    .map { branchName ->
                        branchName.trim()
                    }
                    .filter { branchName ->
                        branchName.isNotBlank()
                    }
                    .distinct()
            } else {
                list
                    .map { training ->
                        training.branchName.trim()
                    }
                    .filter { branchName ->
                        branchName.isNotBlank()
                    }
                    .distinct()
            }

        if (requestedBranches.isEmpty()) {
            return tr(
                isEnglish,
                "לא מצאתי את הסניף שביקשת.",
                "I could not find the requested branch."
            )
        }

        val branchAddresses =
            requestedBranches
                .mapNotNull { branchName ->
                    val address =
                        TrainingCatalog
                            .addressFor(
                                branchName,
                                isEnglish
                            )
                            .trim()

                    address
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            branchName to address
                        }
                }

        if (branchAddresses.isEmpty()) {
            return tr(
                isEnglish,
                "לא מצאתי כתובת לסניף שביקשת.",
                "I could not find an address for the requested branch."
            )
        }

        if (branchAddresses.size == 1) {
            val (branchName, address) =
                branchAddresses.first()

            return if (isEnglish) {
                "The address of $branchName is:\n$address"
            } else {
                "הכתובת של סניף $branchName היא:\n$address"
            }
        }

        return buildString {
            append(
                if (isEnglish) {
                    "Branch addresses:\n"
                } else {
                    "כתובות הסניפים:\n"
                }
            )

            branchAddresses.forEach { (branchName, address) ->
                append("• ")
                append(branchName)
                append(":\n")
                append(address)
                append('\n')
            }
        }.trim()
    }

    fun buildTrainingTimes(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        day: String?,
        isEnglish: Boolean = false
    ): String {
        val weeklyRows =
            list
                .distinctBy { training ->
                    listOf(
                        training.branchName,
                        TrainingCatalog.normalizeGroupName(
                            training.groupName
                        ),
                        training.dayName,
                        training.timeRange,
                        training.coachName
                    ).joinToString("|")
                }
                .sortedWith(
                    compareBy<TrainingRow> {
                        it.branchName
                    }
                        .thenBy {
                            it.groupName
                        }
                        .thenBy {
                            EntityExtractor.getDayIndex(
                                it.dayName
                                    .replace("יום ", "")
                                    .trim()
                            ) ?: Int.MAX_VALUE
                        }
                        .thenBy {
                            EntityExtractor.parseStartMinutes(
                                it.timeRange
                            ) ?: Int.MAX_VALUE
                        }
                )

        if (weeklyRows.isEmpty()) {
            return tr(
                isEnglish,
                "לא מצאתי שעות אימון התואמות לבקשה.",
                "I could not find training times matching the request."
            )
        }

        return buildString {
            if (isEnglish) {
                when {
                    branch != null && group != null ->
                        append(
                            "Training times at $branch for $group:\n"
                        )

                    branch != null ->
                        append(
                            "Training times at $branch:\n"
                        )

                    group != null ->
                        append(
                            "Training times for $group:\n"
                        )

                    else ->
                        append(
                            "Training times:\n"
                        )
                }
            } else {
                when {
                    branch != null && group != null ->
                        append(
                            "שעות האימון בסניף $branch לקבוצת $group:\n"
                        )

                    branch != null ->
                        append(
                            "שעות האימון בסניף $branch:\n"
                        )

                    group != null ->
                        append(
                            "שעות האימון לקבוצת $group:\n"
                        )

                    else ->
                        append(
                            "שעות האימון:\n"
                        )
                }
            }

            weeklyRows
                .groupBy { training ->
                    training.groupName
                }
                .forEach { (groupName, groupRows) ->
                    if (
                        group == null ||
                        weeklyRows
                            .map { it.groupName }
                            .distinct()
                            .size > 1
                    ) {
                        append('\n')

                        if (isEnglish) {
                            append("Group: ")
                        } else {
                            append("קבוצה: ")
                        }

                        append(groupName)
                        append('\n')
                    }

                    groupRows.forEach { training ->
                        append("• ")
                        append(training.dayName)
                        append(": ")
                        append(training.timeRange)

                        if (
                            day == null &&
                            training.coachName.isNotBlank()
                        ) {
                            if (isEnglish) {
                                append(" — Coach: ")
                            } else {
                                append(" — מאמן: ")
                            }

                            append(training.coachName)
                        }

                        append('\n')
                    }
                }
        }.trim()
    }

    fun buildNextTraining(
        list: List<TrainingRow>,
        isEnglish: Boolean = false,
        nowMillis: Long =
            System.currentTimeMillis()
    ): String {
        val next =
            list.minByOrNull {
                it.startAtMillis
            } ?: return tr(
                isEnglish,
                "לא מצאתי אימון קרוב.",
                "I could not find an upcoming training."
            )

        val isOngoing =
            next.startAtMillis <= nowMillis &&
                    next.endAtMillis?.let { endMillis ->
                        nowMillis < endMillis
                    } == true

        if (isOngoing) {
            val endTime =
                next.endAtMillis?.let { endMillis ->
                    SimpleDateFormat(
                        "HH:mm",
                        if (isEnglish) {
                            Locale.ENGLISH
                        } else {
                            Locale("he", "IL")
                        }
                    ).format(
                        java.util.Date(endMillis)
                    )
                }.orEmpty()

            return if (isEnglish) {
                buildString {
                    append("Your training is in progress now")
                    append(" at ${next.branchName}")
                    append(", for ${next.groupName}.")

                    if (endTime.isNotBlank()) {
                        append(" It is expected to end at ")
                        append(endTime)
                        append(".")
                    }

                    if (next.coachName.isNotBlank()) {
                        append(" The coach is ")
                        append(next.coachName)
                        append(".")
                    }
                }
            } else {
                buildString {
                    append("האימון שלך מתקיים כעת")
                    append(" בסניף ${next.branchName}")
                    append(", לקבוצת ${next.groupName}.")

                    if (endTime.isNotBlank()) {
                        append(" האימון צפוי להסתיים בשעה ")
                        append(endTime)
                        append(".")
                    }

                    if (next.coachName.isNotBlank()) {
                        append(" המאמן הוא ")
                        append(next.coachName)
                        append(".")
                    }
                }
            }
        }

        val spokenTime =
            next.timeRange
                .substringBefore("–")
                .substringBefore("-")
                .trim()

        val dayText =
            dayPhrase(
                next.dayName,
                isEnglish
            )

        return if (isEnglish) {
            "The next training is $dayText at $spokenTime, " +
                    "at ${next.branchName}, " +
                    "for ${next.groupName}. " +
                    "The coach is ${next.coachName}."
        } else {
            "האימון הבא הוא $dayText בשעה $spokenTime, " +
                    "בסניף ${next.branchName}, " +
                    "לקבוצת ${next.groupName}. " +
                    "המאמן הוא ${next.coachName}."
        }
    }

    fun buildLastTraining(
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {
        val last =
            list.maxByOrNull { training ->
                training.startAtMillis
            } ?: return tr(
                isEnglish,
                "לא מצאתי אימון קודם.",
                "I could not find a previous training."
            )

        val locale =
            if (isEnglish) {
                Locale.US
            } else {
                Locale("he", "IL")
            }

        val dateText =
            SimpleDateFormat(
                "dd/MM/yyyy",
                locale
            ).format(
                Date(last.startAtMillis)
            )

        val startTime =
            last.timeRange
                .substringBefore("–")
                .substringBefore("-")
                .trim()

        val cleanDay =
            last.dayName
                .replace("יום ", "")
                .trim()

        return if (isEnglish) {
            buildString {
                append("Your last training was on ")
                append(cleanDay)
                append(", ")
                append(dateText)
                append(" at ")
                append(startTime)
                append(".")
                append("\nBranch: ")
                append(last.branchName)
                append("\nGroup: ")
                append(last.groupName)

                if (last.location.isNotBlank()) {
                    append("\nLocation: ")
                    append(last.location)
                }

                if (last.coachName.isNotBlank()) {
                    append("\nCoach: ")
                    append(last.coachName)
                }
            }
        } else {
            buildString {
                append("האימון האחרון שלך היה ביום ")
                append(cleanDay)
                append(", ")
                append(dateText)
                append(" בשעה ")
                append(startTime)
                append(".")
                append("\nסניף: ")
                append(last.branchName)
                append("\nקבוצה: ")
                append(last.groupName)

                if (last.location.isNotBlank()) {
                    append("\nמיקום: ")
                    append(last.location)
                }

                if (last.coachName.isNotBlank()) {
                    append("\nמאמן: ")
                    append(last.coachName)
                }
            }
        }
    }

    fun buildNoMatch(
        branch: String?,
        group: String?,
        day: String?,
        isEnglish: Boolean = false
    ): String = buildString {
        if (isEnglish) {
            append("I could not find trainings matching your question.\n")

            if (branch != null) append("• Branch searched: $branch\n")
            if (group != null) append("• Group searched: $group\n")
            if (day != null) append("• Day searched: $day\n")

            append(
                """

Try asking in another way:
• What is my next training?
• Which trainings are on Wednesday?
• When is the next training at Sokolov?
• Which trainings are at Ofek?
• Youth trainings in Netanya
""".trimIndent()
            )
        } else {
            append("לא מצאתי אימונים מתאימים לשאלה שלך.\n")

            if (branch != null) append("• סניף שחיפשתי: $branch\n")
            if (group != null) append("• קבוצה שחיפשתי: $group\n")
            if (day != null) append("• יום שחיפשתי: $day\n")

            append(
                """

נסה לשאול בצורה אחרת:
• מה האימון הבא שלי?
• אילו אימונים יש ביום רביעי?
• מתי האימון הבא בסוקולוב?
• אילו אימונים יש באופק?
• אימוני נוער בסניף נתניה
""".trimIndent()
            )
        }
    }
    private fun isMinorGroup(group: String?): Boolean {
        val g = (group ?: "").lowercase(Locale("he", "IL"))
        return listOf("ילד", "ילדים", "כיתה", "נוער", "נער").any { it in g }
    }

    fun buildWeeklyCountAnswer(
        listAll: List<TrainingRow>,
        branch: String?,
        group: String?,
        isEnglish: Boolean = false
    ): String {
        val now = System.currentTimeMillis()
        val weekAhead = now + 7L * 24L * 60L * 60L * 1000L

        val base = listAll.asSequence()
            .filter { it.startAtMillis in now..weekAhead }
            .let { seq ->
                var s = seq
                branch?.let { b -> s = s.filter { it.branchName == b } }
                group?.let  { g -> s = s.filter { it.groupName == g } }
                s
            }
            .toList()
            .sortedBy { it.startAtMillis }

        // ✅ קטין: מחזירים לפי האימונים של הקבוצה + פירוט האימונים בשבוע הקרוב
        if (isMinorGroup(group)) {
            if (base.isEmpty()) {
                return tr(
                    isEnglish,
                    "לא מצאתי אימונים בשבוע הקרוב לפי הסניף/קבוצה שלך. נסה לשאול: \"אילו אימונים יש השבוע בסניף סוקולוב\".",
                    "I could not find trainings for the coming week based on your branch or group. Try asking: \"Which trainings are this week at Sokolov?\""
                )
            }

            return buildString {
                append(
                    if (isEnglish) {
                        "For a minor trainee, the number of trainings this week according to your group is: ${base.size}.\n"
                    } else {
                        "למתאמן קטין — מספר האימונים השבוע לפי הקבוצה שלך הוא: ${base.size}.\n"
                    }
                )

                base.forEach { r ->
                    append(
                        if (isEnglish) {
                            "• ${r.dayName} – ${r.timeRange} – ${r.branchName} – Coach: ${r.coachName}\n"
                        } else {
                            "• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n"
                        }
                    )
                }
            }.trim()
        }

        // ✅ בגיר: תשובה קבועה לפי קבוצת הבוגרים, עם פירוט אם נמצא
        val adultLine = tr(
            isEnglish,
            "למתאמן בגיר — בקבוצת הבוגרים יש בדרך כלל פעמיים בשבוע.",
            "For an adult trainee, the adults group usually trains twice a week."
        )
        if (base.isEmpty()) return adultLine

        return buildString {
            append(adultLine).append('\n')
            base.forEach { r ->
                append(
                    if (isEnglish) {
                        "• ${r.dayName} – ${r.timeRange} – ${r.branchName} – Coach: ${r.coachName}\n"
                    } else {
                        "• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n"
                    }
                )
            }
        }.trim()
    }

    fun buildGroupsAndLevels(
        branch: String?,
        isEnglish: Boolean = false
    ): String {
        val groupsByBranch = if (!branch.isNullOrBlank()) {
            val groups = TrainingCatalog.ageGroupsByBranch[branch]
                .orEmpty()
                .map { TrainingCatalog.normalizeGroupName(it) }
                .filter { it.isNotBlank() }
                .distinct()

            mapOf(branch to groups)
        } else {
            TrainingCatalog.ageGroupsByBranch
                .mapValues { (_, groups) ->
                    groups
                        .map { TrainingCatalog.normalizeGroupName(it) }
                        .filter { it.isNotBlank() }
                        .distinct()
                }
                .filterValues { it.isNotEmpty() }
        }

        if (groupsByBranch.isEmpty()) {
            return tr(
                isEnglish,
                "לא נמצאו קבוצות במאגר האימונים המעודכן.",
                "No groups were found in the current training database."
            )
        }

        return buildString {
            append(
                if (isEnglish) {
                    if (branch != null) {
                        "Groups currently listed at $branch:"
                    } else {
                        "Groups currently listed in the training database:"
                    }
                } else {
                    if (branch != null) {
                        "הקבוצות המופיעות כרגע בסניף $branch:"
                    } else {
                        "הקבוצות המופיעות כרגע במאגר האימונים:"
                    }
                }
            )

            append("\n\n")

            groupsByBranch.forEach { (branchName, groups) ->
                if (branch == null) {
                    append(
                        if (isEnglish) {
                            "• $branchName:\n"
                        } else {
                            "• $branchName:\n"
                        }
                    )
                }

                groups.forEach { groupName ->
                    append("  • $groupName\n")
                }
            }

            append(
                if (isEnglish) {
                    "\nThe list is based on the groups currently configured in the app. Suitability for a specific level should be confirmed with the coach."
                } else {
                    "\nהרשימה מבוססת על הקבוצות המוגדרות כרגע באפליקציה. התאמה לרמה מסוימת מומלץ לאשר מול המאמן."
                }
            )
        }.trim()
    }

    fun buildSpecialWeekAnswer(isEnglish: Boolean = false): String {
        return tr(
            isEnglish,
            "מומלץ לברר עם המאמן או מאמן בכיר. כרגע לא ידוע על אימון מיוחד השבוע.",
            "It is recommended to check with your coach or a senior coach. At the moment, no special training is known for this week."
        )
    }

}

// ============================================================================
// 9) מנוע התשובות הראשי — generateAnswer()
// ============================================================================

object AssistantTrainingKnowledge {

    /**
     * בנייה מחדש בכל בקשה מבטיחה שהעוזר משתמש
     * בנתוני TrainingCatalog העדכניים.
     */
    private val allTrainings: List<TrainingRow>
        get() = TrainingTableBuilder.build()

    fun generateAnswer(
        context: Context,
        question: String,
        memory: AssistantMemory,
        isEnglish: Boolean = false,
        onCardsReady:
            (List<TrainingAssistantCard>) -> Unit = {}
    ): String {

        val appContext = context.applicationContext

        val norm = HebrewNormalize
            .normalize(question)
            .lowercase(Locale("he", "IL"))

        var intent = IntentDetector.detectIntent(norm)

        val lastIntent = memory.getLastIntent()

        val looksLikeFollowUp = listOf(
            "ומה",
            "ומה לגבי",
            "ומה עם",
            "ושם",
            "ומתי",
            "באיזה יום",
            "באיזו שעה",
            "מי המאמן",
            "איפה זה",
            "כמה זמן",
            "אותו",
            "אותה",
            "הבא",
            "אחריו",
            "עוד",
            "and what",
            "what about",
            "and there",
            "and when",
            "which day",
            "what time",
            "who is the coach",
            "where is it",
            "how long",
            "same one",
            "next one",
            "after that",
            "more"
        ).any { marker ->
            marker in norm
        }

        if (
            intent == AssistantIntent.UNKNOWN &&
            looksLikeFollowUp &&
            lastIntent != null
        ) {
            intent = runCatching {
                AssistantIntent.valueOf(lastIntent)
            }.getOrDefault(AssistantIntent.UNKNOWN)
        }

        memory.setLastIntent(intent.name)

        val wantsNearest =
            EntityExtractor.wantsNearest(norm)

        val wantsUpcoming =
            EntityExtractor.wantsUpcoming(norm)

        val wantsPast =
            EntityExtractor.wantsPast(norm)

        val wantsThisWeek =
            EntityExtractor.wantsThisWeek(norm)

        val wantsNextWeek =
            EntityExtractor.wantsNextWeek(norm)

        /*
         * שאלה מפורשת על האימון הבא היא תמיד בקשה
         * לאימון יחיד — גם אם שכבת הניתוב הוסיפה לשאלה
         * את הביטוי "רשימת אימונים".
         */
        val asksForSingleNextTraining =
            intent == AssistantIntent.ASK_NEXT_TRAINING &&
                    listOf(
                        "האימון הבא",
                        "אימון הבא",
                        "האימון הקרוב",
                        "אימון קרוב",
                        "הבא שלי",
                        "הקרוב שלי",
                        "מתי אני מתאמן",
                        "next training",
                        "my next training",
                        "upcoming training",
                        "nearest training",
                        "when do i train next"
                    ).any { phrase ->
                        phrase in norm
                    } &&
                    listOf(
                        "האימונים הבאים",
                        "אימונים הבאים",
                        "האימונים הקרובים",
                        "אימונים קרובים",
                        "next trainings",
                        "upcoming trainings",
                        "upcoming classes",
                        "next classes",
                        "next workouts",
                        "this week",
                        "next week",
                        "השבוע הקרוב",
                        "שבוע הבא"
                    ).none { phrase ->
                        phrase in norm
                    }

        /*
         * שאלה על "האימון האחרון" היא בקשה לאימון עבר
         * יחיד. לעומת זאת, "האימונים האחרונים" היא
         * בקשה לרשימת אימוני עבר.
         */
        val asksForSinglePastTraining =
            wantsPast &&
                    listOf(
                        "האימון האחרון",
                        "אימון אחרון",
                        "האימון הקודם",
                        "אימון קודם",
                        "מתי התאמנתי לאחרונה",
                        "מתי היה לי אימון לאחרונה",
                        "last training",
                        "my last training",
                        "most recent training",
                        "previous training"
                    ).any { phrase ->
                        phrase in norm
                    } &&
                    listOf(
                        "האימונים האחרונים",
                        "אימונים אחרונים",
                        "האימונים הקודמים",
                        "אימונים קודמים",
                        "באילו אימונים הייתי",
                        "last trainings",
                        "my last trainings",
                        "recent trainings",
                        "past trainings",
                        "previous trainings",
                        "trainings i attended",
                        "trainings i had",
                        "workouts i attended"
                    ).none { phrase ->
                        phrase in norm
                    }

        /*
         * הביטוי "רשימת אימונים" עשוי להתווסף בשכבת הניתוב,
         * ולכן הוא אינו רשאי להפוך שאלה מפורשת על אימון יחיד
         * לבקשה של כמה אימונים.
         */
        val wantsTrainingList =
            EntityExtractor.wantsTrainingList(norm) &&
                    !asksForSingleNextTraining &&
                    !asksForSinglePastTraining

        /*
         * מספר אימונים רלוונטי רק לבקשת רשימה.
         * בשאלה על אימון יחיד תמיד מוחזר אימון אחד.
         */
        val explicitlyRequestedTrainingCount =
            if (
                asksForSingleNextTraining ||
                asksForSinglePastTraining
            ) {
                null
            } else {
                EntityExtractor.requestedTrainingCount(norm)
            }

        /*
         * זהו המקור היחיד שקובע אם יש להחזיר רשימה.
         * שאלה על האימון הבא או האחרון מחזירה אימון אחד.
         */
        val wantsMultipleTrainings =
            !asksForSingleNextTraining &&
                    !asksForSinglePastTraining &&
                    (
                            wantsTrainingList ||
                                    wantsUpcoming ||
                                    wantsPast ||
                                    wantsThisWeek ||
                                    wantsNextWeek ||
                                    explicitlyRequestedTrainingCount != null
                            )

        /*
         * בקשה על שבוע שלם מקבלת רשימה רחבה.
         * בקשת רשימה כללית מקבלת עד שמונה אימונים.
         */
        val requestedTrainingCount =
            explicitlyRequestedTrainingCount
                ?: when {
                    wantsThisWeek ||
                            wantsNextWeek ->
                        20

                    wantsMultipleTrainings ->
                        8

                    else ->
                        1
                }

        // ישויות מפורשות מהשאלה
        val explicitBranch = EntityExtractor.detectBranch(norm)
        val explicitGroup = EntityExtractor.detectGroup(norm)
        val explicitDay = EntityExtractor.detectDay(norm)
        val timeRange = EntityExtractor.detectTimeRange(norm)

        /*
      * בקשת רשימת אימונים ללא סניף מפורש נחשבת לבקשה
      * על האימונים של המשתמש בכל הסניפים הרשומים שלו.
      *
      * כך לא משתמשים בטעות בסניף האחרון שנשמר בזיכרון השיחה.
      */
        val isRegisteredTrainingsQuestion =
            explicitBranch == null &&
                    (
                            isMyTrainingQuestion(norm) ||
                                    intent == AssistantIntent.ASK_NEXT_TRAINING ||
                                    intent == AssistantIntent.ASK_WHAT_TODAY ||
                                    wantsTrainingList ||
                                    (wantsUpcoming && !asksForSingleNextTraining) ||
                                    wantsPast ||
                                    wantsThisWeek ||
                                    wantsNextWeek
                            )

        val isPersonalQuestion =
            isRegisteredTrainingsQuestion

        val registeredBranches =
            memory.getRegisteredBranches()

        val registeredBranch =
            registeredBranches
                .joinToString(", ")
                .takeIf { value ->
                    value.isNotBlank()
                }

        val registeredGroups =
            memory.getRegisteredGroups()

        val registeredGroup =
            registeredGroups
                .joinToString(", ")
                .takeIf { value ->
                    value.isNotBlank()
                }

        if (
            isPersonalQuestion &&
            explicitBranch == null &&
            registeredBranches.isEmpty()
        ) {
            return if (isEnglish) {
                "Your branch is missing from your profile. Update your branch before asking about your personal trainings."
            } else {
                "לא מוגדר סניף בפרופיל שלך. יש לעדכן את הסניף לפני שאפשר להציג את האימונים האישיים שלך."
            }
        }

        if (
            isPersonalQuestion &&
            explicitGroup == null &&
            registeredGroups.isEmpty()
        ) {
            return if (isEnglish) {
                "Your group is missing from your profile. Update your group before asking about your personal trainings."
            } else {
                "לא מוגדרת קבוצה בפרופיל שלך. יש לעדכן את הקבוצה לפני שאפשר להציג את האימונים האישיים שלך."
            }
        }

        /*
    * ברשימת האימונים האישית משתמשים בכל הסניפים הרשומים.
    * רק שאלה כללית שאינה אישית רשאית להשתמש בהקשר
    * מהשאלה הקודמת.
    */
        var branch =
            explicitBranch
                ?: if (isRegisteredTrainingsQuestion) {
                    registeredBranch
                } else {
                    memory.getLastBranch()
                }

        var group =
            explicitGroup
                ?: if (isRegisteredTrainingsQuestion) {
                    registeredGroup
                } else {
                    memory.getLastGroup()
                }

        var day = explicitDay ?: memory.getLastDay()

        // ✅ חשוב: בשאלות "לוז/לוח אימונים" (כמו: "מה הלוז לאימונים בסוקולוב")
        // אם המשתמש לא ציין קבוצה/יום מפורש — לא ננעל על הקבוצה/יום מהזיכרון
        val isScheduleQuestion =
            (intent == AssistantIntent.ASK_SCHEDULE) ||
                    ("לוז" in norm) || ("לו\"ז" in norm) || ("לוח" in norm)

        val isNextOrUpcoming =
            (intent == AssistantIntent.ASK_NEXT_TRAINING) ||
                    wantsUpcoming

        if (isNextOrUpcoming || isScheduleQuestion) {
            if (!isPersonalQuestion && explicitGroup == null) {
                group = null
            }

            if (explicitDay == null) {
                day = null
            }
        }

        /*
         * כאשר המשתמש מבקש מידע על סניף מסוים,
         * אסור להשתמש בקבוצה או ביום שנשמרו משאלה קודמת.
         *
         * קבוצה ויום מסננים את התוצאה רק כאשר הם
         * נאמרו במפורש בשאלה הנוכחית.
         */
        val isExplicitBranchInformationQuestion =
            explicitBranch != null &&
                    (
                            intent == AssistantIntent.ASK_COACH ||
                                    intent == AssistantIntent.ASK_LOCATION ||
                                    intent == AssistantIntent.ASK_TIME
                            )

        if (isExplicitBranchInformationQuestion) {
            if (explicitGroup == null) {
                group = null
            }

            if (explicitDay == null) {
                day = null
            }
        }

        // אם ביקש "הכי קרוב אליי" ואין סניף מפורש — נעדיף את הסניף האחרון בזיכרון
        if (wantsNearest && explicitBranch == null) {
            branch = memory.getLastBranch() ?: branch
        }

        // סינון אימונים
        val nowMillis = System.currentTimeMillis()

        /*
         * מטמון מקומי לבקשה הנוכחית בלבד.
         * כמה סניפים וקבוצות יכולים להתאמן באותו מועד,
         * ולכן אין צורך לחשב שוב את מצב החג לאותו זמן.
         */
        val statusByTrainingTime =
            mutableMapOf<
                    Pair<Long, Long?>,
                    TrainingStatusEngine.Status
                    >()

        fun statusFor(
            training: TrainingRow
        ): TrainingStatusEngine.Status {
            val timeKey =
                training.startAtMillis to
                        training.endAtMillis

            return statusByTrainingTime.getOrPut(
                timeKey
            ) {
                TrainingStatusEngine.evaluate(
                    context = appContext,
                    trainingStartMillis =
                        training.startAtMillis,
                    trainingEndMillis =
                        training.endAtMillis,
                    nowMillis = nowMillis
                )
            }
        }

        /*
   * משמש להצעת חלופות בלבד.
   * חלופה יכולה להיות אימון עתידי או אימון שמתקיים כעת.
   * אין להציע אימון שבוטל, הסתיים או אינו תקין.
   */
        val activeAllTrainings =
            allTrainings.filter { training ->
                val status =
                    statusFor(training)

                status.isScheduled ||
                        status.isOngoing
            }

        var seq = allTrainings.asSequence()

        /*
    * ברשימת אימונים שבועית מציגים גם אימונים
    * שבוטלו, כדי שהמשתמש יראה שהם קיימים בלוח
    * אך אינם מתקיימים.
    *
    * בבקשה לאימון הבא ממשיכים לבחור רק אימון
    * מתוכנן או אימון שמתקיים כעת.
    */
        val shouldIncludeCancelledInList =
            wantsTrainingList ||
                    wantsThisWeek ||
                    wantsNextWeek

        if (wantsPast) {
            /*
             * אימון נחשב לאימון עבר רק לאחר שהסתיים.
             * אם אין שעת סיום, משתמשים בשעת ההתחלה.
             */
            seq = seq.filter { training ->
                val effectiveEndMillis =
                    training.endAtMillis
                        ?: training.startAtMillis

                effectiveEndMillis <= nowMillis
            }
        } else if (
            isNextOrUpcoming ||
            isPersonalQuestion ||
            wantsThisWeek ||
            wantsNextWeek
        ) {
            seq = seq.filter { training ->
                val effectiveEndMillis =
                    training.endAtMillis
                        ?: training.startAtMillis

                /*
                 * בבקשת אימונים עתידיים אסור להחזיר
                 * שום אימון שכבר הסתיים — גם אם הוא
                 * מסומן כמבוטל בגלל חג.
                 */
                val isCurrentOrFutureTraining =
                    effectiveEndMillis > nowMillis

                if (!isCurrentOrFutureTraining) {
                    false
                } else {
                    val status =
                        statusFor(training)

                    status.isScheduled ||
                            status.isOngoing ||
                            (
                                    shouldIncludeCancelledInList &&
                                            status.isCancelled
                                    )
                }
            }
        }

        /*
         * בפרופיל יכולים להישמר כמה סניפים באותה מחרוזת,
         * למשל: "סניף א, סניף ב".
         * כל ערך מושווה בנפרד לסניף שב־TrainingCatalog.
         */
        branch?.let { expectedBranch ->

            fun normalizeBranchName(value: String): String {
                return value
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }

            val expectedBranches = expectedBranch
                .split(
                    ',',
                    ';',
                    '|',
                    '\n'
                )
                .map(::normalizeBranchName)
                .filter { it.isNotBlank() }
                .distinct()

            if (expectedBranches.isNotEmpty()) {
                seq = seq.filter { training ->
                    val trainingBranch =
                        normalizeBranchName(training.branchName)

                    expectedBranches.any { registeredBranch ->
                        trainingBranch == registeredBranch
                    }
                }
            }
        }

        /*
   * סינון לפי כל הקבוצות הרשומות ולא לפי קבוצה אחת בלבד.
   */
        group?.let { expectedGroup ->

            val expectedGroups =
                expectedGroup
                    .split(
                        ',',
                        ';',
                        '|',
                        '\n'
                    )
                    .map { groupName ->
                        TrainingCatalog
                            .normalizeGroupName(groupName)
                            .ifBlank { groupName.trim() }
                    }
                    .filter { groupName ->
                        groupName.isNotBlank()
                    }
                    .distinct()

            if (expectedGroups.isNotEmpty()) {
                seq = seq.filter { training ->
                    val trainingGroup =
                        TrainingCatalog.normalizeGroupName(
                            training.groupName
                        )

                    expectedGroups.any { registeredGroup ->
                        trainingGroup == registeredGroup
                    }
                }
            }
        }

        day?.let { expectedDay ->
            seq = seq.filter { training ->
                training.dayName.contains(expectedDay)
            }
        }

        timeRange?.let { rng ->
            seq = seq.filter { tr ->
                EntityExtractor.parseStartHour(tr.timeRange)?.let { it in rng } ?: false
            }
        }

        /*
         * “השבוע” – שבעת הימים הקרובים.
         */
        if (wantsThisWeek) {
            val now = System.currentTimeMillis()
            val weekAhead =
                now + 7L * 24L * 60L * 60L * 1000L

            seq = seq.filter { training ->
                training.startAtMillis in now until weekAhead
            }
        }

        /*
         * “שבוע הבא” – השבוע הקלנדרי הבא:
         * מיום ראשון בשעה 00:00 ועד יום ראשון שאחריו.
         */
        if (wantsNextWeek) {
            val startOfNextWeek = Calendar.getInstance(
                Locale("he", "IL")
            ).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                val daysUntilNextSunday =
                    (Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7

                add(
                    Calendar.DAY_OF_YEAR,
                    if (daysUntilNextSunday == 0) 7
                    else daysUntilNextSunday
                )
            }

            val endOfNextWeek =
                (startOfNextWeek.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 7)
                }

            val startMillis = startOfNextWeek.timeInMillis
            val endMillis = endOfNextWeek.timeInMillis

            seq = seq.filter { training ->
                training.startAtMillis in startMillis until endMillis
            }
        }

        var matchedResults = seq.toList()


        /*
         * שומרים התאמות מבוטלות כדי להסביר למשתמש
         * שהאימון קיים בלוח אך אינו מתקיים בגלל חג.
         */
        val cancelledResults =
            matchedResults.filter { training ->
                statusFor(training).isCancelled
            }

        /*
         * אימון שבוטל אינו נחשב לאימון שהמשתמש התאמן בו.
         * ברשימת שבוע עתידית עדיין ניתן להציג אימון מבוטל
         * כדי להסביר שהוא מופיע בלוח אך אינו מתקיים.
         */
        var results =
            when {
                wantsPast ->
                    matchedResults.filter { training ->
                        !statusFor(training).isCancelled
                    }

                shouldIncludeCancelledInList ->
                    matchedResults

                else ->
                    matchedResults.filter { training ->
                        !statusFor(training).isCancelled
                    }
            }

        results =
            if (wantsPast) {
                results.sortedByDescending { training ->
                    training.startAtMillis
                }
            } else if (
                (intent == AssistantIntent.ASK_NEXT_TRAINING || wantsUpcoming) &&
                isPersonalQuestion
            ) {
                results.sortedBy { training ->
                    training.startAtMillis
                }
            } else {
                results
            }

        // עדכון זיכרון רק לפי מה שנשאל מפורשות (כדי לא “לנעול” בטעות)
        explicitBranch?.let { memory.setLastBranch(it) }
        explicitGroup ?.let { memory.setLastGroup(it) }
        explicitDay   ?.let { memory.setLastDay(it) }

        // אם אין תוצאה אישית, אסור להחליף אותה באימון של משתמש אחר.
        if (results.isEmpty()) {
            val cancelledTraining =
                cancelledResults.minByOrNull {
                    it.startAtMillis
                }

            if (cancelledTraining != null) {
                val cancellationStatus =
                    statusFor(cancelledTraining)

                val cancellationReason =
                    cancellationStatus
                        .reason(isEnglish)
                        .orEmpty()

                val cleanDay =
                    cancelledTraining.dayName
                        .replace("יום", "")
                        .trim()

                val cleanTime =
                    cancelledTraining.timeRange
                        .substringBefore("–")
                        .substringBefore("-")
                        .trim()

                return if (isEnglish) {
                    buildString {
                        append("The scheduled training on ")
                        append(cleanDay)
                        append(" at ")
                        append(cleanTime)
                        append(" is cancelled")

                        if (cancellationReason.isNotBlank()) {
                            append(" due to ")
                            append(cancellationReason)
                        }

                        append(".")
                        append("\nBranch: ")
                        append(cancelledTraining.branchName)
                        append("\nGroup: ")
                        append(cancelledTraining.groupName)

                        if (
                            cancelledTraining.coachName
                                .isNotBlank()
                        ) {
                            append("\nCoach: ")
                            append(
                                cancelledTraining.coachName
                            )
                        }
                    }
                } else {
                    buildString {
                        append("האימון המתוכנן ביום ")
                        append(cleanDay)
                        append(" בשעה ")
                        append(cleanTime)
                        append(" מבוטל")

                        if (cancellationReason.isNotBlank()) {
                            append(" עקב ")
                            append(cancellationReason)
                        }

                        append(".")
                        append("\nסניף: ")
                        append(cancelledTraining.branchName)
                        append("\nקבוצה: ")
                        append(cancelledTraining.groupName)

                        if (
                            cancelledTraining.coachName
                                .isNotBlank()
                        ) {
                            append("\nמאמן: ")
                            append(
                                cancelledTraining.coachName
                            )
                        }
                    }
                }
            }

            if (isPersonalQuestion) {
                return if (isEnglish) {
                    buildString {
                        append("I could not find an upcoming training matching your registered branch and group.")

                        if (!registeredBranch.isNullOrBlank()) {
                            append("\nBranch: $registeredBranch")
                        }

                        if (!registeredGroup.isNullOrBlank()) {
                            append("\nGroup: $registeredGroup")
                        }

                        append("\nCheck that your profile details and the training schedule are up to date.")
                    }
                } else {
                    buildString {
                        append("לא נמצא אימון קרוב התואם לסניף ולקבוצה הרשומים בפרופיל שלך.")

                        if (!registeredBranch.isNullOrBlank()) {
                            append("\nסניף: $registeredBranch")
                        }

                        if (!registeredGroup.isNullOrBlank()) {
                            append("\nקבוצה: $registeredGroup")
                        }

                        append("\nמומלץ לבדוק שפרטי הפרופיל ולוח האימונים מעודכנים.")
                    }
                }
            }

            val todayName = SimpleDateFormat("EEEE", Locale("he", "IL"))
                .format(Calendar.getInstance().time)

            val askedToday =
                ("היום" in norm) ||
                        (intent == AssistantIntent.ASK_WHAT_TODAY) ||
                        (explicitDay != null && norm.contains(explicitDay))

            fun nextUpcomingTraining(list: List<TrainingRow>): TrainingRow? {
                val now = System.currentTimeMillis()
                return list
                    .filter { it.startAtMillis >= now }
                    .minByOrNull { it.startAtMillis }
            }

            // 1) "אין היום בסניף X" → הצע חלופה באותה עיר, ואם אין אז בכלל
            if (askedToday && branch != null) {
                fun branchCity(b: String): String =
                    b.substringBefore("–").substringBefore("-").trim()

                fun pickEarliestToday(list: List<TrainingRow>): TrainingRow? {
                    val todayRows = list.filter { it.dayName.contains(todayName) }
                    return todayRows.minByOrNull {
                        EntityExtractor.parseStartMinutes(it.timeRange) ?: Int.MAX_VALUE
                    }
                }

                val city = branchCity(branch!!)
                val sameCity = activeAllTrainings.filter {
                    branchCity(it.branchName) == city
                }
                val altSameCity = pickEarliestToday(sameCity)

                if (altSameCity != null) {
                    return if (isEnglish) {
                        "There is no training today at $branch, but there is training at ${altSameCity.branchName} " +
                                "for ${altSameCity.groupName} at ${altSameCity.timeRange} (Coach: ${altSameCity.coachName})."
                    } else {
                        "אין היום אימון בסניף $branch, אבל יש ב-${altSameCity.branchName} " +
                                "לקבוצה ${altSameCity.groupName} ב-${altSameCity.timeRange} (מאמן: ${altSameCity.coachName})."
                    }
                }

                val altAnyToday =
                    pickEarliestToday(activeAllTrainings)
                if (altAnyToday != null) {
                    return if (isEnglish) {
                        "There is no training today at $branch, but there is training at ${altAnyToday.branchName} " +
                                "for ${altAnyToday.groupName} at ${altAnyToday.timeRange} (Coach: ${altAnyToday.coachName})."
                    } else {
                        "אין היום אימון בסניף $branch, אבל יש ב-${altAnyToday.branchName} " +
                                "לקבוצה ${altAnyToday.groupName} ב-${altAnyToday.timeRange} (מאמן: ${altAnyToday.coachName})."
                    }
                }

                val nextAny =
                    nextUpcomingTraining(activeAllTrainings)
                if (nextAny != null) {
                    return if (isEnglish) {
                        "There is no training today at $branch. " +
                                "The next upcoming training is on ${nextAny.dayName} at ${nextAny.timeRange}, " +
                                "at ${nextAny.branchName}, for ${nextAny.groupName}. " +
                                "The coach is ${nextAny.coachName}."
                    } else {
                        "אין היום אימון בסניף $branch. " +
                                "האימון הקרוב הבא הוא ביום ${nextAny.dayName} בשעה ${nextAny.timeRange}, " +
                                "בסניף ${nextAny.branchName}, לקבוצת ${nextAny.groupName}. " +
                                "המאמן הוא ${nextAny.coachName}."
                    }
                }
            }

            // 2) שאלות על "שלי" בלי התאמה → מחזירים תשובה נקייה עם האימון הקרוב.
            // לא מציגים "לא מצאתי התאמה" אם בפועל כן נמצא אימון קרוב.
            if (isMyTrainingQuestion(norm)) {
                val personalNext = nextUpcomingTraining(
                    activeAllTrainings.filter { row ->
                        userGroups(memory).any { groupName ->
                            row.groupName.contains(groupName)
                        }
                    }
                )

                if (personalNext != null) {
                    val cleanDay = personalNext.dayName
                        .replace("יום", "")
                        .trim()

                    val spokenTime = personalNext.timeRange
                        .substringBefore("–")
                        .substringBefore("-")
                        .trim()

                    return if (isEnglish) {
                        "Your upcoming training is on $cleanDay at $spokenTime, " +
                                "at ${personalNext.branchName}, " +
                                "for ${personalNext.groupName}. " +
                                "The coach is ${personalNext.coachName}."
                    } else {
                        "האימון הקרוב שלך הוא ביום $cleanDay בשעה $spokenTime, " +
                                "בסניף ${personalNext.branchName}, " +
                                "לקבוצת ${personalNext.groupName}. " +
                                "המאמן הוא ${personalNext.coachName}."
                    }
                }
            }

            // 3) "האימונים הבאים" בלי התאמה → החזר מכל הסניפים
            if (wantsNearest) {
                return AnswerBuilder.buildNextTraining(
                    activeAllTrainings,
                    isEnglish
                )
            }
            if (wantsUpcoming) return AnswerBuilder.buildUpcomingTrainings(
                list = activeAllTrainings,
                branch = null,
                group = null,
                limit = requestedTrainingCount,
                isEnglish = isEnglish
            )

            // 4) fallback חכם כללי
            val nextAny =
                nextUpcomingTraining(activeAllTrainings)
            if (nextAny != null) {
                val cleanDay = nextAny.dayName
                    .replace("יום", "")
                    .trim()

                val spokenTime = nextAny.timeRange
                    .substringBefore("–")
                    .substringBefore("-")
                    .trim()

                return if (isEnglish) {
                    "The next upcoming training I found is on $cleanDay at $spokenTime, " +
                            "at ${nextAny.branchName}, " +
                            "for ${nextAny.groupName}. " +
                            "The coach is ${nextAny.coachName}."
                } else {
                    "האימון הקרוב הבא שמצאתי הוא ביום $cleanDay בשעה $spokenTime, " +
                            "בסניף ${nextAny.branchName}, " +
                            "לקבוצת ${nextAny.groupName}. " +
                            "המאמן הוא ${nextAny.coachName}."
                }
            }

            return AnswerBuilder.buildNoMatch(branch, group, day, isEnglish)
        }

        /*
   * רשימת כרטיסים מובנית נוצרת מאותו מקור נתונים
   * שממנו נבנית התשובה הטקסטואלית.
   */
        val shouldCreateTrainingCards =
            wantsMultipleTrainings

        val structuredCards =
            if (shouldCreateTrainingCards) {
                val dateFormatter =
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale("he", "IL")
                    )

                results
                    .distinctBy { training ->
                        listOf(
                            training.branchName,
                            training.groupName,
                            training.startAtMillis
                        ).joinToString("|")
                    }
                    .let { trainings ->
                        if (wantsPast) {
                            trainings.sortedByDescending { training ->
                                training.startAtMillis
                            }
                        } else {
                            trainings.sortedBy { training ->
                                training.startAtMillis
                            }
                        }
                    }
                    .take(requestedTrainingCount)
                    .map { training ->
                        val status =
                            statusFor(training)

                        val startTime =
                            training.timeRange
                                .substringBefore("–")
                                .trim()

                        val endTime =
                            training.timeRange
                                .substringAfter(
                                    "–",
                                    ""
                                )
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                }

                        TrainingAssistantCard(
                            id = listOf(
                                training.branchName,
                                training.groupName,
                                training.startAtMillis
                            ).joinToString("|"),
                            title = buildString {
                                append(training.dayName)
                                append(", ")
                                append(
                                    dateFormatter.format(
                                        Date(
                                            training.startAtMillis
                                        )
                                    )
                                )
                            },
                            date = dateFormatter.format(
                                Date(
                                    training.startAtMillis
                                )
                            ),
                            startTime = startTime,
                            endTime = endTime,
                            branchName =
                                training.branchName,
                            groupName =
                                training.groupName,
                            location =
                                training.location,
                            coachName =
                                training.coachName,
                            statusCode =
                                status.state.name,
                            statusHe =
                                status.displayText(false),
                            statusEn =
                                status.displayText(true)
                        )
                    }
            } else {
                emptyList()
            }

        onCardsReady(structuredCards)

// בונה תשובה
        val answer = when {

            isTodayTrainingQuestion(
                norm,
                isEnglish
            ) ->
                AnswerBuilder.buildTodayTraining(
                    results,
                    isEnglish
                )

            /*
             * שאלות על אימון יחיד מקבלות קדימות על פני
             * מילות ניתוב כמו "רשימת אימונים".
             */
            asksForSingleNextTraining ->
                AnswerBuilder.buildNextTraining(
                    list = results.take(1),
                    isEnglish = isEnglish
                )

            asksForSinglePastTraining ->
                AnswerBuilder.buildLastTraining(
                    list = results.take(1),
                    isEnglish = isEnglish
                )

            /*
             * בקשת רבים מחזירה טקסט מסכם ובמקביל
             * כרטיס מובנה ונפרד לכל אימון.
             */
            wantsMultipleTrainings ->
                AnswerBuilder.buildUpcomingTrainings(
                    list = results,
                    branch = branch,
                    group = group,
                    limit = requestedTrainingCount,
                    isEnglish = isEnglish,
                    past = wantsPast,
                    statusProvider = { training ->
                        statusFor(training)
                    }
                )

            intent == AssistantIntent.ASK_DURATION ->
                AnswerBuilder.buildDuration(
                    results,
                    isEnglish
                )

            intent == AssistantIntent.ASK_COACH ->
                AnswerBuilder.buildCoach(
                    list = results,
                    branch = branch,
                    group = group,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_LOCATION ->
                AnswerBuilder.buildLocation(
                    branch = branch,
                    list = results,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_NEXT_TRAINING ->
                AnswerBuilder.buildNextTraining(
                    list = results.take(1),
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_EQUIPMENT ->
                AnswerBuilder.buildEquipment(isEnglish)

            intent == AssistantIntent.ASK_GROUPS_AND_LEVELS ->
                AnswerBuilder.buildGroupsAndLevels(
                    branch = explicitBranch ?: registeredBranch,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_WHAT_TODAY -> {
                val todayDay =
                    EntityExtractor.detectDay("היום")

                val todayList =
                    results.filter { training ->
                        training.dayName.contains(
                            todayDay.orEmpty()
                        )
                    }

                AnswerBuilder.buildFullSchedule(
                    list = todayList,
                    branch = branch,
                    group = group,
                    day = todayDay,
                    isEnglish = isEnglish
                )
            }

            intent == AssistantIntent.ASK_TIME ->
                AnswerBuilder.buildTrainingTimes(
                    list = results,
                    branch = branch,
                    group = group,
                    day = day,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_WEEKLY_COUNT ->
                AnswerBuilder.buildWeeklyCountAnswer(
                    allTrainings,
                    branch,
                    group,
                    isEnglish
                )

            intent == AssistantIntent.ASK_SPECIAL_WEEK ->
                AnswerBuilder.buildSpecialWeekAnswer(
                    isEnglish
                )

            else ->
                AnswerBuilder.buildFullSchedule(
                    list = results,
                    branch = branch,
                    group = group,
                    day = day,
                    isEnglish = isEnglish
                )
        }

        memory.setLastAnswerContext(answer)

        return answer
    }

    // ========================================================================
    // updateMemoryFromAnswer() — זיהוי סניף/קבוצה/יום מתוך התשובה
    // ========================================================================

    fun updateMemoryFromAnswer(
        question: String,
        answer: String,
        memory: AssistantMemory
    ) {
        try {
            val normalizedQuestion = HebrewNormalize
                .normalize(question)
                .lowercase(Locale("he", "IL"))

            EntityExtractor.detectBranch(normalizedQuestion)
                ?.let { explicitBranch ->
                    memory.setLastBranch(explicitBranch)
                }

            EntityExtractor.detectGroup(normalizedQuestion)
                ?.let { explicitGroup ->
                    memory.setLastGroup(explicitGroup)
                }

            EntityExtractor.detectDay(normalizedQuestion)
                ?.let { explicitDay ->
                    memory.setLastDay(explicitDay)
                }

            memory.setLastAnswerContext(
                answer.trim().take(500)
            )
        } catch (_: Throwable) {
            // כשל בזיכרון אינו משפיע על התשובה למשתמש.
        }
    }
}
private fun userGroups(memory: AssistantMemory): List<String> {

    val lastGroup = memory.getLastGroup()

    if (lastGroup != null) {
        return listOf(lastGroup)
    }

    return emptyList()
}

private fun isMyTrainingQuestion(norm: String): Boolean {
    return listOf(
        "שלי",
        "עבורי",
        "בשבילי",
        "לקבוצה שלי",
        "בסניף שלי",
        "המאמן שלי",
        "האימון הבא שלי",
        "האימון הקרוב שלי",
        "מתי אני מתאמן",
        "איפה אני מתאמן",
        "יש לי אימון",
        "האימונים שלי",
        "הלוז שלי",
        "לו\"ז שלי",
        "my next training",
        "my upcoming training",
        "my training",
        "my trainings",
        "my class",
        "my classes",
        "my workout",
        "my schedule",
        "my group",
        "my branch",
        "my coach",
        "when do i train",
        "where do i train",
        "do i have training"
    ).any { marker ->
        marker in norm
    }
}

private fun isKidsGroup(groupName: String): Boolean {
    val g = groupName.lowercase(Locale("he", "IL"))
    return ("ילד" in g) || ("ילדים" in g) || ("כיתה" in g)
}

private fun personalGroupPriority(groupName: String): Int {
    val g = groupName.lowercase(Locale("he", "IL"))
    return when {
        "נוער + בוגרים" in g -> 0
        "בוגרים" in g -> 1
        "נוער" in g -> 2
        else -> 9
    }
}

private fun isTodayTrainingQuestion(
    norm: String,
    isEnglish: Boolean = false
): Boolean {
    val heKeys = listOf(
        "יש לי אימון היום",
        "מתי האימון היום",
        "האימון שלי היום",
        "מה האימון שלי היום",
        "יש אימון היום"
    )

    val enKeys = listOf(
        "do i have training today",
        "do i have a training today",
        "when is training today",
        "my training today",
        "what is my training today",
        "is there training today",
        "training today"
    )

    return if (isEnglish) {
        enKeys.any { it in norm } || heKeys.any { it in norm }
    } else {
        heKeys.any { it in norm } || enKeys.any { it in norm }
    }
}