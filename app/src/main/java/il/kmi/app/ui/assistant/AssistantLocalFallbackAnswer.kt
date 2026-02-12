package il.kmi.app.ui.assistant

import android.util.Log
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.app.training.TrainingDirectory
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.search.KmiSearch
import il.kmi.shared.search.SearchHit

/**
 * מנוע fallback "ישן" שמחזיר תשובות מקומיות:
 * - אימונים קרובים
 * - הסבר לתרגיל לפי טקסט
 * - רשימות תרגילים לפי חגורה/נושא
 * - תשובות כלליות
 *
 * הוצאנו אותו מה-UI כדי להקטין את AiAssistantDialog.
 */
object AssistantLocalFallbackAnswer {

    data class Params(
        val question: String,
        val contextLabel: String? = null,
        val getExternalDefenses: ((Belt) -> List<String>)? = null,
        val getExerciseExplanation: ((String) -> String?)? = null,
        val getUpcomingTrainings: (() -> List<TrainingDirectory.UpcomingTraining>)? = null
    )

    fun answer(p: Params): String {
        val question = p.question
        val t = question.lowercase().trim()

        // ✅ אם זו שאלה שנשמעת כמו הסבר לתרגיל – אל תתן למסלול אימונים לתפוס אותה
        val looksLikeExplainQuestion =
            ("הסבר" in t || "תסביר" in t || "איך עושים" in t || "איך מבצעים" in t) &&
                    ("תרגיל" in t || "בעיטה" in t || "מכה" in t || "הגנה" in t || "שחרור" in t)

        // זיהוי חגורה מהטקסט
        val beltEnum = detectBeltEnum(question)
        val beltHeb = beltEnum?.let { beltToHebrew(it) }

        fun prefix() = ""

        fun extractExerciseNameFromText(): String {
            val cleaned = t
                .replace("על תרגיל", "תרגיל")
                .replace("על", "")

            val idx = cleaned.indexOf("תרגיל")
            if (idx == -1) return ""

            return cleaned.substring(idx + "תרגיל".length)
                .replace("הזה", "")
                .replace("הזאת", "")
                .replace(":", "")
                .replace("?", "")
                .replace("\"", "")
                .trim()
        }

        // האם השאלה נשמעת כמו שאלה על אימונים / לוח אימונים
        val trainingKeywords = listOf(
            "אימון", "אימונים",
            "אימון הקרוב", "האימון הקרוב",
            "אימון הבא", "האימון הבא",
            "האימונים הקרובים", "האימונים הבאים",
            "לוח אימונים", "לו\"ז", "לוז",
            "שעות אימון", "שעת אימון",
            "קבוצת אימון", "קבוצה שלי"
        )

        val looksLikeTrainingQuestion =
            trainingKeywords.any { kw -> kw in t }

        val looksLikeExplanationQuestion =
            ("הסבר" in t || "תסביר" in t || "פירוט" in t || "איך עושים" in t || "איך מבצעים" in t) &&
                    !(
                            "אימון הקרוב" in t ||
                                    "האימון הקרוב" in t ||
                                    "אימון הבא" in t ||
                                    "האימון הבא" in t ||
                                    "אימונים קרובים" in t ||
                                    "האימונים הקרובים" in t ||
                                    "לוח אימונים" in t ||
                                    "לו\"ז" in t ||
                                    "לוז" in t
                            )

        // אם יש לנו פונקציה שמביאה אימונים קרובים, והשאלה נראית קשורה לאימונים
        // ⚠️ אבל אם זו שאלה בסגנון "הסבר" — לא ניכנס לפה כדי לא להחזיר לוח אימונים בטעות
        if (p.getUpcomingTrainings != null && looksLikeTrainingQuestion && !looksLikeExplanationQuestion) {
            val upcoming = try {
                p.getUpcomingTrainings.invoke()
            } catch (th: Throwable) {
                emptyList()
            }

            if (upcoming.isNotEmpty()) {
                val listText = upcoming.joinToString("\n\n") { tr ->
                    TrainingDirectory.formatUpcoming(tr)
                }

                return "הנה האימונים הקרובים שלך לפי הסניף והקבוצה שנבחרו באפליקציה:\n\n" +
                        listText +
                        "\n\nאם תרצה לראות אימונים מסניף אחר – שנה סניף וקבוצה במסך הרישום ואז שאל שוב."
            }

            return "לא מצאתי אצלך כרגע אימונים קרובים לפי הסניף והקבוצה שנבחרו.\n" +
                    "בדוק במסך הרישום שבחרת סניף וקבוצת אימון, ואז נסה לשאול שוב ״מה האימון הקרוב שלי?״"
        }

        // 1) הגנות חיצוניות
        if ("הגנות חיצוניות" in t) {
            if (p.getExternalDefenses != null && beltEnum != null) {
                val list = p.getExternalDefenses.invoke(beltEnum)
                if (list.isNotEmpty()) {
                    val titleBelt = beltHeb ?: beltToHebrew(beltEnum)
                    val listText = list.joinToString("\n") { "• $it" }

                    return prefix() +
                            "הגנות חיצוניות בחגורה $titleBelt:\n\n$listText\n"
                }
            }

            val beltLine = when {
                beltHeb != null -> "בחגורה $beltHeb"
                else -> "לרמה שלך"
            }

            return prefix() +
                    "כרגע לא מצאתי רשימה מדויקת של הגנות חיצוניות $beltLine, אבל במסכי הנושאים תמצא את כל ההגנות בחלוקה לפי נושאים ותתי נושאים.\n"
        }

        // 2) הסבר מפורט לתרגיל
        if (("הסבר" in t || "תסביר" in t) && "תרגיל" in t) {
            val exName = extractExerciseNameFromText()

            if (exName.isNotBlank() && p.getExerciseExplanation != null) {
                val real = p.getExerciseExplanation.invoke(exName)
                if (!real.isNullOrBlank()) {
                    return prefix() +
                            "ההסבר לתרגיל \"$exName\":\n\n$real\n"
                }
            }

            val hits = searchExercisesForQuestion(question, beltEnum)
            val best = AssistantExerciseSearchFallback.buildBestHitExplanation(hits, beltEnum)
            if (best != null) return prefix() + best

            val header = if (exName.isNotBlank()) {
                "לא מצאתי הסבר מדויק לתרגיל \"$exName\".\nהנה עקרונות כלליים לביצוע תרגיל:\n\n"
            } else {
                "הנה עקרונות כלליים לביצוע תרגיל:\n\n"
            }

            return prefix() + header +
                    "1. עמידת מוצא: עמוד יציב, ברכיים מעט כפופות, גב ישר ומבט קדימה.\n" +
                    "2. בצע את התנועה לאט כמה פעמים, בלי כוח, כדי להבין את המסלול והכיוון.\n" +
                    "3. יד השמירה נשארת גבוהה ולא נופלת בזמן הביצוע.\n" +
                    "4. לא לנעול מרפקים או ברכיים – התנועה זורמת ורכה.\n" +
                    "5. אחרי שהטכניקה נקייה, אפשר להוסיף מהירות ועוצמה בהדרגה.\n"
        }

        // 3) רשימת תרגילים / חימום
        if (("רשימה" in t || "תן לי" in t) &&
            ("תרגיל" in t || "תרגילים" in t || "חימום" in t)
        ) {
            val hits = searchExercisesForQuestion(question, beltEnum)
            if (hits.isNotEmpty()) {
                val beltStr = beltHeb?.let { " לחגורה $it" } ?: ""
                val listText = formatHitsAsExerciseList(hits)

                return prefix() +
                        "מצאתי עבורך תרגילים$beltStr שקשורים לשאלה שלך:\n\n$listText\n\n" +
                        "את כל התרגילים ניתן לראות במסכי הנושאים של החגורה."
            }

            val beltStr = beltHeb?.let { " לחגורה $it" } ?: ""

            return prefix() +
                    "לא הצלחתי למצוא תרגילים מדויקים$beltStr לשאלה הזאת.\n" +
                    "נסה לנסח מחדש עם שם נושא (למשל \"בעיטות\", \"הגנות חיצוניות\") או שם תרגיל מדויק.\n"
        }

        // 4) הצעות תרגול ושיפור
        if ("מה כדאי" in t || "מה לתרגל" in t ||
            "להתקדם" in t || "איך להשתפר" in t
        ) {
            val hits = searchExercisesForQuestion(question, beltEnum)

            if (hits.isNotEmpty()) {
                val beltLine = beltHeb?.let { "לחגורה $it " } ?: ""
                val listText = formatHitsAsExerciseList(hits, maxItems = 5)

                return prefix() +
                        "כדי להתקדם ${beltLine}מומלץ לעבוד באופן עקבי על התרגילים הבאים מתוך החומר הרשמי:\n\n" +
                        listText +
                        "\n\nבחר 3–5 תרגילים מהרשימה, תרגל אותם כמעט בכל אימון, ועבור למסכים המתאימים באפליקציה כדי לראות פירוט ותמונות."
            }

            return prefix() +
                    "לא מצאתי תרגילים מדויקים לשאלה הזאת, אבל כללית כדאי לבחור 3–5 תרגילים בסיסיים מהחגורה שלך ולתרגל אותם כמעט בכל אימון.\n"
        }

        // 5) הסברים כלליים
        if ("הסבר" in t || "מה זה" in t || "תסביר" in t) {
            val hits = searchExercisesForQuestion(question, beltEnum)
            val best = AssistantExerciseSearchFallback.buildBestHitExplanation(hits, beltEnum)
            if (best != null) return prefix() + best

            return prefix() +
                    "כדי לקבל הסבר מדויק לתרגיל ספציפי, חפש אותו במסכי התרגילים ולחץ על אייקון ה־ℹ️ ליד השם.\n" +
                    "באופן כללי חשוב לשים לב ל:\n" +
                    "• עמידת מוצא יציבה ונוחה.\n" +
                    "• נשימה רגועה לאורך כל התרגיל.\n" +
                    "• תנועה זורמת בלי לנעול מפרקים.\n" +
                    "• חזרה מהירה לעמדת הגנה בסיום כל תנועה.\n"
        }

        // 6) אימונים קרובים (גרסת ברירת מחדל אם לא תפס למעלה)
        if (
            "אימון הקרוב" in t ||
            "האימון הקרוב" in t ||
            "האימון הבא" in t ||
            "האימונים הקרובים" in t ||
            "האימונים הבאים" in t ||
            "אימונים קרובים" in t
        ) {
            if (p.getUpcomingTrainings != null) {
                val upcoming = p.getUpcomingTrainings.invoke()
                if (upcoming.isNotEmpty()) {
                    val listText =
                        upcoming.joinToString("\n\n") { TrainingDirectory.formatUpcoming(it) }

                    return prefix() +
                            "האימונים הקרובים שלך לפי הסניף והקבוצה שנבחרו:\n\n$listText\n\n" +
                            "אם תרצה לבדוק מרכז אחר – שנה סניף/קבוצה במסך הרישום."
                }
            }

            return prefix() +
                    "כרגע לא מצאתי אימונים קרובים בפרטי המשתמש שלך.\n" +
                    "ודא שבחרת סניף וקבוצת אימון במסך הרישום, ואז נסה שוב לשאול \"מה האימון הקרוב שלי?\""
        }

        // 7) ברירת מחדל — חיפוש תרגילים שקשורים לשאלה
        val defaultHits = searchExercisesForQuestion(question, beltEnum)
        if (defaultHits.isNotEmpty()) {
            val listText = formatHitsAsExerciseList(defaultHits)
            val beltLine = beltHeb?.let { " לחגורה $it " } ?: ""

            return prefix() +
                    "כשמחפשים מתוך חומר התרגילים שלך${beltLine}מצאתי כמה תרגילים:\n\n" +
                    listText +
                    "\n\nאם תרצה, אפשר לבקש הסבר מפורט על אחד מהם בשם המדויק שלו."
        }

        // תשובת fallback
        return prefix() +
                "אני יכול לעזור לך עם תרגילים אמיתיים מתוך החומר של ק.מ.י – לפי חגורה, נושא ותתי נושאים.\n\n" +
                "אפשר לשאול למשל:\n" +
                "• \"תן לי רשימה של תרגילי חימום לחגורה צהובה\"\n" +
                "• \"מה כדאי לי לתרגל כדי להשתפר בבעיטות?\"\n" +
                "• \"תן את כל ההגנות החיצוניות בחגורה כתומה\"\n" +
                "• \"תן את ההסבר לתרגיל בעיטת מיאגרי קדמית\"\n"
    }

    // ───────────────────────────────
    // זיהוי חגורה מהטקסט (הועתק מ-AiAssistantDialog)
    // ───────────────────────────────
    private fun detectBeltEnum(text: String): Belt? = when {
        "לבן" in text || "לבנה" in text -> Belt.WHITE
        "צהוב" in text || "צהובה" in text -> Belt.YELLOW
        "כתום" in text || "כתומה" in text -> Belt.ORANGE
        "ירוק" in text || "ירוקה" in text -> Belt.GREEN
        "כחול" in text || "כחולה" in text -> Belt.BLUE
        "חום" in text || "חומה" in text -> Belt.BROWN
        "שחור" in text || "שחורה" in text -> Belt.BLACK
        else -> null
    }

    private fun beltToHebrew(belt: Belt): String = when (belt) {
        Belt.WHITE -> "לבנה"
        Belt.YELLOW -> "צהובה"
        Belt.ORANGE -> "כתומה"
        Belt.GREEN -> "ירוקה"
        Belt.BLUE -> "כחולה"
        Belt.BROWN -> "חומה"
        Belt.BLACK -> "שחורה"
    }

    // ───────────────────────────────
    // חיפוש תרגילים מתוך ה-Repo (KmiSearch)
    // ───────────────────────────────
    private fun searchExercisesForQuestion(
        question: String,
        beltEnum: Belt?
    ): List<SearchHit> {
        return try {
            val sharedRepo = ContentRepo.asSharedRepo()
            KmiSearch.search(
                repo = sharedRepo,
                query = question,
                belt = beltEnum?.toShared()
            )
        } catch (t: Throwable) {
            Log.e("KMI-AI", "KmiSearch failed", t)
            emptyList()
        }
    }

    // ───────────────────────────────
    // המרת תוצאות חיפוש לרשימת תרגילים קריאה
    // ───────────────────────────────
    private fun formatHitsAsExerciseList(
        hits: List<SearchHit>,
        maxItems: Int = 6
    ): String {
        if (hits.isEmpty()) return ""

        return hits.take(maxItems).joinToString("\n") { hit ->
            val appBelt = runCatching { Belt.valueOf(hit.belt.name) }
                .getOrElse { Belt.WHITE }

            val topicTitle = hit.topic
            val rawItem = hit.item ?: ""

            // ✅ FIX: ניקוי שם תרגיל בצורה אחידה (def:* / def_* / בשני הכיוונים)
            val displayName = ExerciseTitleFormatter
                .displayName(rawItem)
                .ifBlank { rawItem }
                .ifBlank { topicTitle }
                .trim()

            "• $displayName (${topicTitle} – חגורה ${appBelt.heb})"
        }
    }
}