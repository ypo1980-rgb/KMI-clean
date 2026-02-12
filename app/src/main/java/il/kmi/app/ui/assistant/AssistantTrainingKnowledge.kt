package il.kmi.app.ui.assistant

import android.content.SharedPreferences
import il.kmi.app.training.TrainingCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/* ============================================================================
   ⭐ AssistantTrainingKnowledge – מנוע NLP מלא לשאלות על אימוני ק.מ.י ⭐
   ============================================================================ */

// ============================================================================
// 1) מחלקת זיכרון — AssistantMemory
// ============================================================================

class AssistantMemory(private val sp: SharedPreferences) {

    fun setLastBranch(v: String?) =
        sp.edit().putString("branch", v).apply()

    fun getLastBranch(): String? =
        sp.getString("branch", null)

    fun setLastGroup(v: String?) =
        sp.edit().putString("group", v).apply()

    fun getLastGroup(): String? =
        sp.getString("group", null)

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
            .apply()
    }
}

// ============================================================================
// 2) Normalization Engine – נרמול טקסט עברי
// ============================================================================

    private fun commonHebrewFixes(t: String): String {
        var s = t

        val fixes = listOf(
            "איימון" to "אימון",
            "אימונם" to "אימונים",
            "אימונין" to "אימונים",
            "מאממ" to "מאמן",
            "ממן" to "מאמן",
            "אמון" to "אימון",
            "אאמון" to "אימון",
            "אימנ" to "אימון",
            "אימן" to "אימון"
        )

        fixes.forEach { (wrong, right) ->
            if (s.contains(wrong)) s = s.replace(wrong, right)
        }

        return s
    }


// ============================================================================
// 3) Tokenizer
// ============================================================================

object HebrewTokenizer {
    private val splitRegex = Regex("[ ,:\\-\\n\\t]+")

    fun tokenize(s: String): List<String> =
        s.split(splitRegex).map { it.trim() }.filter { it.isNotEmpty() }
}

// ============================================================================
// 4) Fuzzy Matching Engine
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
// 6.5) Training Info – בנק שאלות לכפתור "מידע על אימונים"
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
        AssistantIntent.ASK_SCHEDULE to listOf("אימונים", "לוח", "לו\"ז", "לוז", "רשימת"),
        AssistantIntent.ASK_NEXT_TRAINING to listOf("האימון הבא", "הבא שלי", "אימון הבא"),
        AssistantIntent.ASK_WHAT_TODAY to listOf("מה יש היום", "מה היום", "היום יש"),
        AssistantIntent.ASK_TIME to listOf("מתי", "באיזו שעה", "שעת", "שעה של"),
        AssistantIntent.ASK_COACH to listOf("מי המאמן", "מי המדריך", "מי מלמד"),
        AssistantIntent.ASK_LOCATION to listOf("איפה", "כתובת", "רחוב", "מיקום"),
        AssistantIntent.ASK_DURATION to listOf("כמה זמן", "משך", "כמה נמשך"),

        AssistantIntent.ASK_EQUIPMENT to listOf(
            "ציוד", "מה להביא", "צריך להביא", "מה צריך להביא", "איזה ציוד",
            "כפפות", "מגני רגליים", "מגן שיניים", "מגן אשכים", "מגנים"
        ),

        AssistantIntent.ASK_WEEKLY_COUNT to listOf(
            "כמה אימונים יש בשבוע", "כמה אימונים בשבוע", "מספר אימונים בשבוע",
            "כמה פעמים בשבוע", "כמה פעמים אני מתאמן בשבוע"
        ),

        AssistantIntent.ASK_SPECIAL_WEEK to listOf(
            "אימון מיוחד השבוע", "יש אימון מיוחד השבוע", "אימון חגורה", "אימון פתוח"
        )
    )

    fun detectIntent(norm: String): AssistantIntent {
        // ✅ "מתי האימון הבא" => NEXT_TRAINING
        val asksWhen = ("מתי" in norm) || ("באיזו שעה" in norm) || ("שעה" in norm)
        val asksNext = ("האימון הבא" in norm) || ("אימון הבא" in norm) || (("אימון" in norm) && ("הבא" in norm))
        if (asksWhen && asksNext) return AssistantIntent.ASK_NEXT_TRAINING

        for ((intent, keys) in intentPatterns) {
            if (keys.any { it in norm }) return intent
        }
        if ("אימון" in norm) return AssistantIntent.ASK_GENERAL
        return AssistantIntent.UNKNOWN
    }
}

object EntityExtractor {

    // ✅ "הכי קרוב אליי" / "הקרוב ביותר" וכו'
    fun wantsNearest(norm: String): Boolean {
        val keys = listOf(
            "הכי קרוב", "קרוב אליי", "הקרוב אליי", "הקרוב ביותר",
            "לידי", "ליד הבית", "קרוב לבית", "בסביבה", "באזור שלי"
        )
        return keys.any { it in norm }
    }

    // ✅ "האימונים הבאים" / "האימונים הקרובים" וכו'
    fun wantsUpcoming(norm: String): Boolean {
        val keys = listOf(
            "האימונים הבאים", "מה האימונים הבאים", "אימונים הבאים",
            "האימונים הקרובים", "מה האימונים הקרובים", "אימונים קרובים",
            "מה האימון הקרוב", "האימון הקרוב"
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

        if ("היום" in norm) return today
        if ("מחרתיים" in norm) return after
        if ("מחר" in norm) return tomorrow

        val days = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
        days.forEach { d ->
            if (d in norm || "ב$d" in norm) return d
        }
        return null
    }

    fun branchCity(branch: String): String =
        branch.substringBefore("–").substringBefore("-").trim()

    // ✅ "השבוע" / "בשבוע הקרוב" / "בשבוע הזה" → חלון 7 ימים קדימה
    fun wantsThisWeek(norm: String): Boolean {
        val keys = listOf(
            "השבוע", "בשבוע הזה", "בשבוע הקרוב", "שבוע קרוב", "שבוע הבא"
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

    fun buildEquipment(): String {
        return """
לאימון מומלץ להגיע עם ציוד מגן בסיסי:
• כפפות אגרוף
• מגני רגליים
• מגן שיניים
בנוסף, מומלץ מאוד להשתמש גם במגן אשכים לשמירה על בטיחות מרבית במהלך האימון.
""".trim()
    }

    fun buildUpcomingTrainings(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        limit: Int = 5
    ): String {

        val sorted = list.sortedWith(
            compareBy<TrainingRow> {
                val dayShort = it.dayName.replace("יום ", "").trim()
                EntityExtractor.getDayIndex(dayShort) ?: 99
            }.thenBy {
                EntityExtractor.parseStartMinutes(it.timeRange) ?: Int.MAX_VALUE
            }
        ).take(limit)

        if (sorted.isEmpty()) return "לא מצאתי אימונים קרובים."

        val title = when {
            branch != null && group != null -> "האימונים הבאים בסניף $branch לקבוצה $group:"
            branch != null -> "האימונים הבאים בסניף $branch:"
            group != null -> "האימונים הבאים לקבוצה $group:"
            else -> "האימונים הבאים שמצאתי:"
        }

        return buildString {
            append(title).append('\n')
            sorted.forEach { r ->
                append("• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n")
            }
            append("\n(אפשר להגיע גם אם אינך רשום לסניף)")
        }.trim()
    }

    fun buildFullSchedule(
        list: List<TrainingRow>,
        branch: String?,
        group: String?,
        day: String?
    ): String = buildString {

        when {
            branch != null && group != null && day != null ->
                append("האימונים בסניף $branch לקבוצה $group ביום $day (אפשר להגיע גם אם אינך רשום לסניף):\n")

            branch != null && group != null ->
                append("האימונים בסניף $branch לקבוצה $group (אפשר להגיע גם אם אינך רשום לסניף):\n")

            branch != null && day != null ->
                append("האימונים בסניף $branch ביום $day (אפשר להגיע גם אם אינך רשום לסניף):\n")

            group != null && day != null ->
                append("האימונים לקבוצה $group ביום $day (אפשר להגיע גם אם אינך רשום לקבוצה הזו):\n")

            branch != null ->
                append("האימונים בסניף $branch (אפשר להגיע גם אם אינך רשום לסניף):\n")

            group != null ->
                append("האימונים לקבוצה $group (אפשר להגיע גם אם אינך רשום לקבוצה הזו):\n")

            day != null ->
                append("האימונים ביום $day:\n")

            else ->
                append("להלן לוח האימונים שמצאתי (ניתן להגיע להתאמן בכל סניף):\n")
        }

        list.groupBy { it.branchName }.forEach { (b, branchList) ->
            append("\nסניף $b:\n")
            branchList.groupBy { it.groupName }.forEach { (g, groupList) ->
                append("  קבוצה: $g\n")
                groupList.forEach { r ->
                    append("    ${r.dayName} – ${r.timeRange} – מאמן: ${r.coachName}\n")
                }
            }
        }
    }

    fun buildDuration(list: List<TrainingRow>): String {
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

        if (durations.isEmpty()) return "לא הצלחתי לחשב את משך האימון."

        val avg = durations.average().toInt()
        return "משך אימון ממוצע הוא בערך ${avg} דקות."
    }

    fun buildCoach(list: List<TrainingRow>): String {
        if (list.isEmpty()) return "לא מצאתי את שם המאמן."

        // ✅ תשובה עניינית: המאמן של האימון הקרוב ביותר מתוך הסינון
        val next = list.minByOrNull { it.startAtMillis } ?: return "לא מצאתי את שם המאמן."

        return "המאמן הוא ${next.coachName} (באימון הקרוב: ${next.branchName}, קבוצה ${next.groupName}, ${next.dayName} ${next.timeRange})."
    }

    fun buildLocation(list: List<TrainingRow>): String {
        val locs = list.map { it.location }.distinct()
        return when (locs.size) {
            0 -> "לא מצאתי את מיקום האימון."
            1 -> "המקום הוא: ${locs.first()}."
            else -> "מקומות האימון האפשריים:\n${locs.joinToString("\n")}"
        }
    }

    fun buildNextTraining(list: List<TrainingRow>): String {
        val next = list.minByOrNull { it.startAtMillis } ?: return "לא מצאתי אימון קרוב."

        return """
        האימון הקרוב:
        סניף: ${next.branchName}
        קבוצה: ${next.groupName}
        יום: ${next.dayName}
        שעה: ${next.timeRange}
        מקום: ${next.location}
        מאמן: ${next.coachName}
    """.trimIndent()
    }

    fun buildNoMatch(branch: String?, group: String?, day: String?): String = buildString {
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
    private fun isMinorGroup(group: String?): Boolean {
        val g = (group ?: "").lowercase(Locale("he", "IL"))
        return listOf("ילד", "ילדים", "כיתה", "נוער", "נער").any { it in g }
    }

    fun buildWeeklyCountAnswer(
        listAll: List<TrainingRow>,
        branch: String?,
        group: String?
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

        // ✅ קטין: מחזירים "לפי האימונים של הקטינים" + פירוט האימונים שמצאנו בשבוע הקרוב
        if (isMinorGroup(group)) {
            if (base.isEmpty()) {
                return "לא מצאתי אימונים בשבוע הקרוב לפי הסניף/קבוצה שלך. נסה לשאול: \"אילו אימונים יש השבוע בסניף סוקולוב\"."
            }
            return buildString {
                append("למתאמן קטין — מספר האימונים השבוע לפי הקבוצה שלך הוא: ${base.size}.\n")
                base.forEach { r ->
                    append("• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n")
                }
            }.trim()
        }

        // ✅ בגיר: תשובה קבועה "פעמיים בשבוע" לפי קבוצת הבוגרים (עם פירוט אם נמצא)
        val adultLine = "למתאמן בגיר — בקבוצת הבוגרים יש בדרך כלל פעמיים בשבוע."
        if (base.isEmpty()) return adultLine

        return buildString {
            append(adultLine).append('\n')
            base.forEach { r ->
                append("• ${r.dayName} – ${r.timeRange} – ${r.branchName} – מאמן: ${r.coachName}\n")
            }
        }.trim()
    }

    fun buildSpecialWeekAnswer(): String {
        return "מומלץ לברר עם המאמן או מאמן בכיר. כרגע לא ידוע על אימון מיוחד השבוע."
    }

}

// ============================================================================
// 9) מנוע התשובות הראשי — generateAnswer()
// ============================================================================

object AssistantTrainingKnowledge {

    private fun branchCity(branch: String): String =
        branch.substringBefore("–").trim()

    private fun startMinutes(timeRange: String): Int? {
        val s = timeRange.substringBefore("–").trim()
        val h = s.substringBefore(":").toIntOrNull() ?: return null
        val m = s.substringAfter(":", "0").toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun pickEarliestToday(list: List<TrainingRow>, todayName: String): TrainingRow? {
        val todayRows = list.filter { it.dayName.contains(todayName) }
        return todayRows.minByOrNull { startMinutes(it.timeRange) ?: Int.MAX_VALUE }
    }

    private val allTrainings: List<TrainingRow> by lazy {
        TrainingTableBuilder.build()
    }

    fun generateAnswer(question: String, memory: AssistantMemory): String {

        log("────────────────────────────────────────────")
        log("שאלה התקבלה: \"$question\"")

        val norm = HebrewNormalize.normalize(question).lowercase(Locale("he", "IL"))
        log("נרמול NLP: $norm")

        val intent = IntentDetector.detectIntent(norm)
        memory.setLastIntent(intent.name)
        log("Intent מזוהה: $intent")

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

        log("Branch מזוהה: $explicitBranch | בפועל: $branch")
        log("Group מזוהה: $explicitGroup  | בפועל: $group")
        log("Day מזוהה: $explicitDay      | בפועל: $day")
        log("TimeRange מזוהה: $timeRange")
        log("wantsNearest=$wantsNearest  wantsUpcoming=$wantsUpcoming")

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

        val results = seq.toList()
        log("כמות אימונים אחרי סינון: ${results.size}")

        // עדכון זיכרון רק לפי מה שנשאל מפורשות (כדי לא “לנעול” בטעות)
        explicitBranch?.let { memory.setLastBranch(it) }
        explicitGroup ?.let { memory.setLastGroup(it) }
        explicitDay   ?.let { memory.setLastDay(it) }

        // ✅ אם אין תוצאות: נסה חלופה חכמה (היום) או החזר הקרוב מכל הסניפים
        if (results.isEmpty()) {
            val todayName = SimpleDateFormat("EEEE", Locale("he", "IL"))
                .format(Calendar.getInstance().time)

            val askedToday =
                ("היום" in norm) ||
                        (intent == AssistantIntent.ASK_WHAT_TODAY) ||
                        (explicitDay != null && norm.contains(explicitDay))

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
                    return "אין היום אימון בסניף $branch, אבל יש ב-${altSameCity.branchName} " +
                            "לקבוצה ${altSameCity.groupName} ב-${altSameCity.timeRange} (מאמן: ${altSameCity.coachName})."
                }

                val altAny = pickEarliestToday(allTrainings)
                if (altAny != null) {
                    return "אין היום אימון בסניף $branch, אבל יש ב-${altAny.branchName} " +
                            "לקבוצה ${altAny.groupName} ב-${altAny.timeRange} (מאמן: ${altAny.coachName})."
                }
            }

            // 2) "הקרוב אליי" או "האימונים הבאים" בלי התאמה → החזר מכל הסניפים
            if (wantsNearest)  return AnswerBuilder.buildNextTraining(allTrainings)
            if (wantsUpcoming) return AnswerBuilder.buildUpcomingTrainings(allTrainings, null, null, limit = 5)

            return AnswerBuilder.buildNoMatch(branch, group, day)
        }

        // בונה תשובה – ✅ “האימונים הבאים” יקבל רשימה קצרה
        val answer = when {
            wantsUpcoming -> AnswerBuilder.buildUpcomingTrainings(results, branch, group, limit = 5)

            intent == AssistantIntent.ASK_DURATION -> AnswerBuilder.buildDuration(results)
            intent == AssistantIntent.ASK_COACH    -> AnswerBuilder.buildCoach(results)
            intent == AssistantIntent.ASK_LOCATION -> AnswerBuilder.buildLocation(results)
            intent == AssistantIntent.ASK_NEXT_TRAINING -> AnswerBuilder.buildNextTraining(results)
            intent == AssistantIntent.ASK_EQUIPMENT -> AnswerBuilder.buildEquipment()

            intent == AssistantIntent.ASK_WHAT_TODAY -> {
                val todayDay = EntityExtractor.detectDay("היום")
                val todayList = results.filter { it.dayName.contains(todayDay!!) }
                AnswerBuilder.buildFullSchedule(todayList, branch, group, todayDay)
            }

            intent == AssistantIntent.ASK_TIME ->
                AnswerBuilder.buildFullSchedule(results, branch, group, day)
            intent == AssistantIntent.ASK_WEEKLY_COUNT ->
                AnswerBuilder.buildWeeklyCountAnswer(allTrainings, branch, group)

            intent == AssistantIntent.ASK_SPECIAL_WEEK ->
                AnswerBuilder.buildSpecialWeekAnswer()

            else ->
                AnswerBuilder.buildFullSchedule(results, branch, group, day)
        }

        memory.setLastAnswerContext(answer)
        log("תשובה סופית:\n$answer")
        log("────────────────────────────────────────────")

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
// ============================================================================
// DEBUG LOGGING – מעקב אחרי כל שלב בלוגיקה
private const val ENABLE_DEBUG_LOGS = true   // כבה/הדלק בזמן פיתוח

private fun log(msg: String) {
    if (ENABLE_DEBUG_LOGS) {
        android.util.Log.d("AiAssistant", msg)
    }
}
