package il.kmi.app.ui.assistant.trainings

import android.content.SharedPreferences
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.assistant.utils.HebrewNormalize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/* ============================================================================
   ⭐ AssistantTrainingKnowledge – מנוע NLP מלא לשאלות על אימוני KAMI / ק.מ.י ⭐
   ============================================================================ */

// ============================================================================
// 1) מחלקת זיכרון — AssistantMemory
// ============================================================================

class AssistantMemory(private val sp: SharedPreferences) {

    fun setLastBranch(v: String?) =
        sp.edit().putString("branch", v).apply()

    fun getLastBranch(): String? =
        sp.getString("branch", null)
            ?: sp.getString("branch_name", null)
            ?: sp.getString("selected_branch", null)
            ?: sp.getString("user_branch", null)
            ?: sp.getString("training_branch", null)

    fun setLastGroup(v: String?) =
        sp.edit().putString("group", v).apply()

    fun getLastGroup(): String? =
        sp.getString("group", null)
            ?: sp.getString("group_name", null)
            ?: sp.getString("selected_group", null)
            ?: sp.getString("user_group", null)
            ?: sp.getString("training_group", null)

    fun setLastDay(v: String?) =
        sp.edit().putString("day", v).apply()

    fun getLastDay(): String? =
        sp.getString("day", null)

    // פונקציה ישנה שהוסרה — נשמרת כדי לא לשבור קוד קיים
    fun getLastRegion(): String? = null

    fun setLastIntent(v: String?) =
        sp.edit().putString("assistant_last_intent", v).apply()

    fun getLastIntent(): String? =
        sp.getString("assistant_last_intent", null)

    fun setLastAnswerContext(v: String?) =
        sp.edit().putString("assistant_last_answer", v).apply()

    fun getLastAnswerContext(): String? =
        sp.getString("assistant_last_answer", null)

    fun clearMemory() {
        sp.edit()
            .remove("assistant_last_intent")
            .remove("assistant_last_answer")
            .remove("branch")
            .remove("group")
            .remove("day")
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
        // ✅ "מתי האימון הבא" / "when is the next training" => NEXT_TRAINING
        val asksWhen = ("מתי" in norm) ||
                ("באיזו שעה" in norm) ||
                ("שעה" in norm) ||
                ("when" in norm) ||
                ("what time" in norm)

        val asksNext = ("האימון הבא" in norm) ||
                ("אימון הבא" in norm) ||
                (("אימון" in norm) && ("הבא" in norm)) ||
                ("next training" in norm) ||
                ("upcoming training" in norm) ||
                (("training" in norm) && ("next" in norm))

        if (asksWhen && asksNext) return AssistantIntent.ASK_NEXT_TRAINING

        for ((intent, keys) in intentPatterns) {
            if (keys.any { it in norm }) return intent
        }

        if ("אימון" in norm ||
            "אימונים" in norm ||
            "training" in norm ||
            "trainings" in norm ||
            "class" in norm ||
            "classes" in norm ||
            "workout" in norm
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

    // ✅ "השבוע" / "בשבוע הקרוב" / "בשבוע הזה" → חלון 7 ימים קדימה
    fun wantsThisWeek(norm: String): Boolean {
        val keys = listOf(
            "השבוע", "בשבוע הזה", "בשבוע הקרוב", "שבוע קרוב", "שבוע הבא",
            "this week", "coming week", "upcoming week", "next week"
        )
        return keys.any { it in norm }
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
                            rows += TrainingRow(
                                branchName = branch,
                                groupName = normGroup,
                                dayName = dayFormatter.format(td.cal.time),
                                timeRange = "${td.start}–${td.end}",
                                location = TrainingCatalog.placeFor(branch),
                                coachName = td.coach,
                                startAtMillis = td.cal.timeInMillis
                            )
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

        val sorted = list.sortedWith(
            compareBy<TrainingRow> {
                val dayShort = it.dayName.replace("יום ", "").trim()
                EntityExtractor.getDayIndex(dayShort) ?: 99
            }.thenBy {
                EntityExtractor.parseStartMinutes(it.timeRange) ?: Int.MAX_VALUE
            }
        ).take(limit)

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
            sorted.forEach { r ->
                if (isEnglish) {
                    append("• ${r.dayName} – ${r.timeRange} – ${r.branchName} – Coach: ${r.coachName}\n")
                } else {
                    append("• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n")
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

    private val allTrainings: List<TrainingRow> by lazy {
        TrainingTableBuilder.build()
    }

    fun generateAnswer(
        question: String,
        memory: AssistantMemory,
        isEnglish: Boolean = false
    ): String {

        val norm = HebrewNormalize.normalize(question).lowercase(Locale("he", "IL"))

        var intent = IntentDetector.detectIntent(norm)

        val lastIntent = memory.getLastIntent()

        if (intent == AssistantIntent.UNKNOWN && lastIntent != null) {
            intent = runCatching {
                AssistantIntent.valueOf(lastIntent)
            }.getOrDefault(AssistantIntent.UNKNOWN)
        }

        memory.setLastIntent(intent.name)

        val wantsNearest  = EntityExtractor.wantsNearest(norm)
        val wantsUpcoming = EntityExtractor.wantsUpcoming(norm)
        val wantsThisWeek = EntityExtractor.wantsThisWeek(norm)

        // ישויות מפורשות מהשאלה
        val explicitBranch = EntityExtractor.detectBranch(norm)
        val explicitGroup  = EntityExtractor.detectGroup(norm)
        val explicitDay    = EntityExtractor.detectDay(norm)
        val timeRange      = EntityExtractor.detectTimeRange(norm)

        // ישויות בפועל (עם זיכרון)
        var branch = explicitBranch ?: memory.getLastBranch()
        var group  = explicitGroup  ?: memory.getLastGroup()
        var day    = explicitDay    ?: memory.getLastDay()

        // ✅ חשוב: בשאלות "לוז/לוח אימונים" (כמו: "מה הלוז לאימונים בסוקולוב")
        // אם המשתמש לא ציין קבוצה/יום מפורש — לא ננעל על הקבוצה/יום מהזיכרון
        val isScheduleQuestion =
            (intent == AssistantIntent.ASK_SCHEDULE) ||
                    ("לוז" in norm) || ("לו\"ז" in norm) || ("לוח" in norm)

        val isNextOrUpcoming =
            (intent == AssistantIntent.ASK_NEXT_TRAINING) || wantsUpcoming

        if (isNextOrUpcoming || isScheduleQuestion) {
            if (explicitGroup == null) group = null
            if (explicitDay == null) day = null
        }

        // אם ביקש "הכי קרוב אליי" ואין סניף מפורש — נעדיף את הסניף האחרון בזיכרון
        if (wantsNearest && explicitBranch == null) {
            branch = memory.getLastBranch() ?: branch
        }

        // סינון אימונים
        var seq = allTrainings.asSequence()

        branch?.let { b -> seq = seq.filter { it.branchName == b } }
        group?.let  { g -> seq = seq.filter { it.groupName == g } }
        day?.let    { d -> seq = seq.filter { it.dayName.contains(d) } }

        timeRange?.let { rng ->
            seq = seq.filter { tr ->
                EntityExtractor.parseStartHour(tr.timeRange)?.let { it in rng } ?: false
            }
        }

        // ✅ פילטר "השבוע" — 7 ימים קדימה (בעיקר לשאלות לוז/זמנים)
        if (wantsThisWeek) {
            val now = System.currentTimeMillis()
            val weekAhead = now + 7L * 24L * 60L * 60L * 1000L
            seq = seq.filter { it.startAtMillis in now..weekAhead }
        }

        var results = seq.toList()

        if (isMyTrainingQuestion(norm) && explicitGroup == null) {

            val groups = userGroups(memory)

            val filtered = results.filter { r ->
                groups.any { g -> r.groupName.contains(g) }
            }

            if (filtered.isNotEmpty()) {
                results = filtered
            }
        }

        if ((intent == AssistantIntent.ASK_NEXT_TRAINING || wantsUpcoming) &&
            explicitGroup == null &&
            isMyTrainingQuestion(norm)
        ) {
            val preferred = results
                .filterNot { isKidsGroup(it.groupName) }
                .sortedWith(
                    compareBy<TrainingRow>(
                        { personalGroupPriority(it.groupName) },
                        { it.startAtMillis }
                    )
                )

            if (preferred.isNotEmpty()) {
                results = preferred
            }
        }

        // עדכון זיכרון רק לפי מה שנשאל מפורשות (כדי לא “לנעול” בטעות)
        explicitBranch?.let { memory.setLastBranch(it) }
        explicitGroup ?.let { memory.setLastGroup(it) }
        explicitDay   ?.let { memory.setLastDay(it) }

        // ✅ אם אין תוצאות: נסה חלופה חכמה (היום) או הצע את האימון הקרוב הבא
        if (results.isEmpty()) {
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
                limit = 5,
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
                    limit = 5,
                    isEnglish = isEnglish
                )

            intent == AssistantIntent.ASK_DURATION -> AnswerBuilder.buildDuration(results, isEnglish)
            intent == AssistantIntent.ASK_COACH    -> AnswerBuilder.buildCoach(results, isEnglish)
            intent == AssistantIntent.ASK_LOCATION -> AnswerBuilder.buildLocation(results, isEnglish)
            intent == AssistantIntent.ASK_NEXT_TRAINING -> AnswerBuilder.buildNextTraining(results, isEnglish)
            intent == AssistantIntent.ASK_EQUIPMENT -> AnswerBuilder.buildEquipment(isEnglish)

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
            val normAnswer = HebrewNormalize.normalize(answer).lowercase()

            TrainingCatalog.branchesByRegion
                .flatMap { it.value }
                .forEach { branch ->
                    if (normAnswer.contains(branch.lowercase())) {
                        memory.setLastBranch(branch)
                    }
                }

            TrainingCatalog.ageGroupsByBranch.values
                .flatten()
                .map { TrainingCatalog.normalizeGroupName(it) }
                .distinct()
                .forEach { group ->
                    if (group.lowercase() in normAnswer) {
                        memory.setLastGroup(group)
                    }
                }

            val days = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
            days.forEach { d ->
                val n = d.lowercase()
                if (n in normAnswer || "יום $n" in normAnswer) {
                    memory.setLastDay(d)
                }
            }

            Regex("""\b([0-2]?[0-9]):([0-5][0-9])""")
                .find(normAnswer)
                ?.value
                ?.let { hhmm ->
                    memory.setLastAnswerContext("שעה: $hhmm")
                }
        } catch (_: Throwable) {}
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
        "לקבוצה שלי",
        "האימון הבא שלי",
        "האימון הקרוב שלי",
        "my next training",
        "my upcoming training",
        "my training",
        "my trainings",
        "my class",
        "my classes",
        "my workout"
    ).any { it in norm }
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