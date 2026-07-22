package il.kmi.app.ui.assistant.trainings

import android.content.SharedPreferences
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.assistant.utils.HebrewNormalize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

/* ============================================================================
   ⭐ AssistantTrainingKnowledge – מנוע NLP מלא לשאלות על אימוני KAMI / ק.מ.י ⭐
   ============================================================================ */

// ============================================================================
// 1) מחלקת זיכרון — AssistantMemory
// ============================================================================

class AssistantMemory(private val sp: SharedPreferences) {

    /**
     * פרטי הרישום האמיתיים של המשתמש.
     * העוזר רשאי לקרוא אותם, אך לעולם אינו כותב אליהם.
     */
    fun getRegisteredBranch(): String? =
        listOf(
            "branch",
            "branch_name",
            "selected_branch",
            "user_branch",
            "training_branch"
        )
            .firstNotNullOfOrNull { key ->
                sp.getString(key, null)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }

    fun getRegisteredGroup(): String? =
        listOf(
            "group",
            "group_name",
            "selected_group",
            "user_group",
            "training_group"
        )
            .firstNotNullOfOrNull { key ->
                sp.getString(key, null)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
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

    fun requestedTrainingCount(norm: String): Int? {
        val hasUpcomingListContext = listOf(
            "רשימה",
            "תן",
            "תני",
            "תראה",
            "הצג",
            "אימונים הבאים",
            "האימונים הבאים",
            "אימונים קרובים",
            "האימונים הקרובים",
            "list",
            "give",
            "show",
            "next trainings",
            "upcoming trainings",
            "next classes",
            "upcoming classes"
        ).any { it in norm }

        if (!hasUpcomingListContext) return null

        val numericCount = Regex(
            """(?<!\d)(10|[1-9])(?!\d)"""
        )
            .find(norm)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        if (numericCount != null) {
            return numericCount.coerceIn(1, 10)
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
    val startAtMillis: Long
)

    object TrainingTableBuilder {

        private val dayFormatter = SimpleDateFormat("EEEE", Locale("he", "IL"))

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
                             * נתוני הקטלוג מייצגים אימון שבועי קבוע.
                             * יוצרים מופעים לארבעת השבועות הקרובים כדי
                             * לתמוך בשאלות על השבוע הבא ורשימות עתידיות.
                             */
                            val firstOccurrence =
                                (td.cal.clone() as Calendar).apply {
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)

                                    while (
                                        timeInMillis <
                                        System.currentTimeMillis()
                                    ) {
                                        add(Calendar.DAY_OF_YEAR, 7)
                                    }
                                }

                            repeat(5) { weekOffset ->
                                val occurrence =
                                    (firstOccurrence.clone() as Calendar).apply {
                                        add(
                                            Calendar.DAY_OF_YEAR,
                                            weekOffset * 7
                                        )
                                    }

                                rows += TrainingRow(
                                    branchName = branch,
                                    groupName = normGroup,
                                    dayName = dayFormatter.format(
                                        occurrence.time
                                    ),
                                    timeRange = "${td.start}–${td.end}",
                                    location = TrainingCatalog.placeFor(branch),
                                    coachName = td.coach,
                                    startAtMillis = occurrence.timeInMillis
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
        limit: Int = 5,
        isEnglish: Boolean = false
    ): String {

        val safeLimit = limit.coerceIn(1, 10)

        /*
         * startAtMillis כולל את התאריך והשעה בפועל.
         * מיון לפי שם יום עלול להציג את יום ראשון לפני
         * אימון קרוב יותר שמתקיים בסוף השבוע הנוכחי.
         */
        val sorted = list
            .filter { it.startAtMillis >= System.currentTimeMillis() }
            .distinctBy { training ->
                listOf(
                    training.branchName,
                    TrainingCatalog.normalizeGroupName(training.groupName),
                    training.startAtMillis
                ).joinToString("|")
            }
            .sortedBy { it.startAtMillis }
            .take(safeLimit)

        if (sorted.isEmpty()) {
            return tr(isEnglish, "לא מצאתי אימונים קרובים.", "I could not find upcoming trainings.")
        }

        val title = if (isEnglish) {
            when {
                branch != null && group != null -> "Upcoming trainings at $branch for $group:"
                branch != null -> "Upcoming trainings at $branch:"
                group != null -> "Upcoming trainings for $group:"
                else -> "Upcoming trainings I found:"
            }
        } else {
            when {
                branch != null && group != null -> "האימונים הבאים בסניף $branch לקבוצה $group:"
                branch != null -> "האימונים הבאים בסניף $branch:"
                group != null -> "האימונים הבאים לקבוצה $group:"
                else -> "האימונים הבאים שמצאתי:"
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

                if (isEnglish) {
                    append(
                        "• ${training.dayName}, $dateText – " +
                                "${training.timeRange} – " +
                                "${training.branchName} – " +
                                "${training.groupName} – " +
                                "Coach: ${training.coachName}\n"
                    )
                } else {
                    append(
                        "• ${training.dayName}, $dateText – " +
                                "${training.timeRange} – " +
                                "${training.branchName} – " +
                                "${training.groupName} – " +
                                "מאמן: ${training.coachName}\n"
                    )
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
        isEnglish: Boolean = false
    ): String {
        if (list.isEmpty()) {
            return tr(isEnglish, "לא מצאתי את שם המאמן.", "I could not find the coach name.")
        }

        // ✅ תשובה עניינית: המאמן של האימון הקרוב ביותר מתוך הסינון
        val next = list.minByOrNull { it.startAtMillis }
            ?: return tr(isEnglish, "לא מצאתי את שם המאמן.", "I could not find the coach name.")

        return if (isEnglish) {
            "The coach is ${next.coachName} (nearest training: ${next.branchName}, group ${next.groupName}, ${next.dayName} ${next.timeRange})."
        } else {
            "המאמן הוא ${next.coachName} (באימון הקרוב: ${next.branchName}, קבוצה ${next.groupName}, ${next.dayName} ${next.timeRange})."
        }
    }

    fun buildLocation(
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {
        val locs = list.map { it.location }.distinct()
        return when (locs.size) {
            0 -> tr(isEnglish, "לא מצאתי את מיקום האימון.", "I could not find the training location.")
            1 -> if (isEnglish) {
                "The location is: ${locs.first()}."
            } else {
                "המקום הוא: ${locs.first()}."
            }
            else -> if (isEnglish) {
                "Possible training locations:\n${locs.joinToString("\n")}"
            } else {
                "מקומות האימון האפשריים:\n${locs.joinToString("\n")}"
            }
        }
    }

    fun buildNextTraining(
        list: List<TrainingRow>,
        isEnglish: Boolean = false
    ): String {

        val next = list.minByOrNull { it.startAtMillis }
            ?: return tr(isEnglish, "לא מצאתי אימון קרוב.", "I could not find an upcoming training.")

        val spokenTime = next.timeRange.substringBefore("–").substringBefore("-").trim()
        val dayText = dayPhrase(next.dayName, isEnglish)

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
        question: String,
        memory: AssistantMemory,
        isEnglish: Boolean = false
    ): String {

        val norm = HebrewNormalize.normalize(question).lowercase(Locale("he", "IL"))

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

        val wantsNearest = EntityExtractor.wantsNearest(norm)
        val wantsUpcoming = EntityExtractor.wantsUpcoming(norm)
        val wantsThisWeek = EntityExtractor.wantsThisWeek(norm)
        val wantsNextWeek = EntityExtractor.wantsNextWeek(norm)

        /*
         * שומרים בנפרד אם המשתמש ביקש מספר אימונים מפורש.
         * ברירת המחדל 5 משמשת רק כאשר באמת נדרשת רשימה,
         * ואינה הופכת שאלה על אימון יחיד לבקשת רשימה.
         */
        val explicitlyRequestedTrainingCount =
            EntityExtractor.requestedTrainingCount(norm)

        val requestedTrainingCount =
            explicitlyRequestedTrainingCount ?: 5

        // ישויות מפורשות מהשאלה
        val explicitBranch = EntityExtractor.detectBranch(norm)
        val explicitGroup = EntityExtractor.detectGroup(norm)
        val explicitDay = EntityExtractor.detectDay(norm)
        val timeRange = EntityExtractor.detectTimeRange(norm)

        val isPersonalQuestion =
            isMyTrainingQuestion(norm) ||
                    intent == AssistantIntent.ASK_NEXT_TRAINING ||
                    intent == AssistantIntent.ASK_WHAT_TODAY ||
                    (
                            wantsNextWeek &&
                                    (
                                            "שלי" in norm ||
                                                    "my " in norm
                                            )
                            )

        val registeredBranch = memory.getRegisteredBranch()
        val registeredGroup = memory.getRegisteredGroup()

        if (
            isPersonalQuestion &&
            explicitBranch == null &&
            registeredBranch.isNullOrBlank()
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
            registeredGroup.isNullOrBlank()
        ) {
            return if (isEnglish) {
                "Your group is missing from your profile. Update your group before asking about your personal trainings."
            } else {
                "לא מוגדרת קבוצה בפרופיל שלך. יש לעדכן את הקבוצה לפני שאפשר להציג את האימונים האישיים שלך."
            }
        }

        // בשאלה אישית משתמשים בפרופיל האמיתי ולא בזיכרון מתשובה קודמת.
        var branch = explicitBranch ?: if (isPersonalQuestion) {
            registeredBranch
        } else {
            memory.getLastBranch()
        }

        var group = explicitGroup ?: if (isPersonalQuestion) {
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
            (intent == AssistantIntent.ASK_NEXT_TRAINING) || wantsUpcoming

        if (isNextOrUpcoming || isScheduleQuestion) {
            if (!isPersonalQuestion && explicitGroup == null) {
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
        var seq = allTrainings.asSequence()

        if (
            isNextOrUpcoming ||
            isPersonalQuestion ||
            wantsThisWeek ||
            wantsNextWeek
        ) {
            seq = seq.filter { training ->
                training.startAtMillis >= nowMillis
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

        group?.let { expectedGroup ->
            seq = seq.filter { training ->
                TrainingCatalog.normalizeGroupName(
                    training.groupName
                ) == TrainingCatalog.normalizeGroupName(
                    expectedGroup
                )
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

        var results = seq.toList()

        if (isPersonalQuestion && explicitGroup == null) {
            val personalGroup = registeredGroup
                ?.let { TrainingCatalog.normalizeGroupName(it) }

            if (!personalGroup.isNullOrBlank()) {
                results = results.filter { training ->
                    TrainingCatalog.normalizeGroupName(
                        training.groupName
                    ) == personalGroup
                }
            }
        }

        if (
            (intent == AssistantIntent.ASK_NEXT_TRAINING || wantsUpcoming) &&
            isPersonalQuestion
        ) {
            results = results.sortedBy {
                it.startAtMillis
            }
        }

        // עדכון זיכרון רק לפי מה שנשאל מפורשות (כדי לא “לנעול” בטעות)
        explicitBranch?.let { memory.setLastBranch(it) }
        explicitGroup ?.let { memory.setLastGroup(it) }
        explicitDay   ?.let { memory.setLastDay(it) }

        // אם אין תוצאה אישית, אסור להחליף אותה באימון של משתמש אחר.
        if (results.isEmpty()) {
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
                val sameCity = allTrainings.filter { branchCity(it.branchName) == city }
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

                val altAnyToday = pickEarliestToday(allTrainings)
                if (altAnyToday != null) {
                    return if (isEnglish) {
                        "There is no training today at $branch, but there is training at ${altAnyToday.branchName} " +
                                "for ${altAnyToday.groupName} at ${altAnyToday.timeRange} (Coach: ${altAnyToday.coachName})."
                    } else {
                        "אין היום אימון בסניף $branch, אבל יש ב-${altAnyToday.branchName} " +
                                "לקבוצה ${altAnyToday.groupName} ב-${altAnyToday.timeRange} (מאמן: ${altAnyToday.coachName})."
                    }
                }

                val nextAny = nextUpcomingTraining(allTrainings)
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
                    allTrainings.filter { row ->
                        userGroups(memory).any { g -> row.groupName.contains(g) }
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
            if (wantsNearest)  return AnswerBuilder.buildNextTraining(allTrainings, isEnglish)
            if (wantsUpcoming) return AnswerBuilder.buildUpcomingTrainings(
                list = allTrainings,
                branch = null,
                group = null,
                limit = requestedTrainingCount,
                isEnglish = isEnglish
            )

            // 4) fallback חכם כללי
            val nextAny = nextUpcomingTraining(allTrainings)
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

        // בונה תשובה – ✅ “האימונים הבאים” יקבל רשימה קצרה
        val answer = when {

            isTodayTrainingQuestion(norm, isEnglish) ->
                AnswerBuilder.buildTodayTraining(results, isEnglish)

            wantsUpcoming ->
                AnswerBuilder.buildUpcomingTrainings(
                    list = results,
                    branch = branch,
                    group = group,
                    limit = requestedTrainingCount,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_DURATION -> AnswerBuilder.buildDuration(results, isEnglish)
            intent == AssistantIntent.ASK_COACH    -> AnswerBuilder.buildCoach(results, isEnglish)
            intent == AssistantIntent.ASK_LOCATION ->
                AnswerBuilder.buildLocation(results, isEnglish)

            intent == AssistantIntent.ASK_NEXT_TRAINING -> {
                val asksForMultipleTrainings =
                    explicitlyRequestedTrainingCount != null ||
                            listOf(
                                "האימונים הבאים",
                                "אימונים הבאים",
                                "האימונים הקרובים",
                                "אימונים קרובים",
                                "next trainings",
                                "upcoming trainings",
                                "upcoming classes",
                                "next classes",
                                "next workouts"
                            ).any { it in norm }

                if (asksForMultipleTrainings) {
                    AnswerBuilder.buildUpcomingTrainings(
                        list = results,
                        branch = branch,
                        group = group,
                        limit = requestedTrainingCount,
                        isEnglish = isEnglish
                    )
                } else {
                    AnswerBuilder.buildNextTraining(
                        list = results,
                        isEnglish = isEnglish
                    )
                }
            }

            intent == AssistantIntent.ASK_EQUIPMENT ->
                AnswerBuilder.buildEquipment(isEnglish)

            intent == AssistantIntent.ASK_GROUPS_AND_LEVELS ->
                AnswerBuilder.buildGroupsAndLevels(
                    branch = explicitBranch ?: registeredBranch,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_WHAT_TODAY -> {
                val todayDay = EntityExtractor.detectDay("היום")
                val todayList = results.filter { it.dayName.contains(todayDay!!) }
                AnswerBuilder.buildFullSchedule(todayList, branch, group, todayDay, isEnglish)
            }

            intent == AssistantIntent.ASK_TIME ->
                AnswerBuilder.buildFullSchedule(results, branch, group, day, isEnglish)
            intent == AssistantIntent.ASK_WEEKLY_COUNT ->
                AnswerBuilder.buildWeeklyCountAnswer(allTrainings, branch, group, isEnglish)

            intent == AssistantIntent.ASK_SPECIAL_WEEK ->
                AnswerBuilder.buildSpecialWeekAnswer(isEnglish)

            else ->
                AnswerBuilder.buildFullSchedule(results, branch, group, day, isEnglish)
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