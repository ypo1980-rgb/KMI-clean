package il.kmi.app.ui.assistant.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val stancePrefix = "עמידת מוצא"
    val idx = source.indexOf(stancePrefix)
    if (idx < 0) return AnnotatedString(source)

    val before = source.substring(0, idx)

    val endPunctIndex = listOf(',', '.')
        .map { ch -> source.indexOf(ch, idx + stancePrefix.length) }
        .filter { it >= 0 }
        .minOrNull()

    val stanceEndExclusive = if (endPunctIndex != null) {
        endPunctIndex + 1
    } else {
        source.indexOf('\n', idx + stancePrefix.length)
            .takeIf { it >= 0 } ?: source.length
    }

    val stanceText = source.substring(idx, stanceEndExclusive)
    val after = source.substring(stanceEndExclusive)

    return buildAnnotatedString {
        append(before)

        val start = length
        append(stanceText)
        val end = length

        addStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start,
            end
        )

        append(after)
    }
}

// ───────────────────────────────
// ניקוי תגיות עיצוב פנימיות לפני הצגה
// ───────────────────────────────
internal fun sanitizeAssistantMarkup(
    source: String
): String {
    /*
     * RED_BOLD ו־BLUE_BOLD נשמרות עד לרכיב התצוגה,
     * כדי ש־StyledExplanationText יוכל לצבוע אותן.
     *
     * תגיות ישנות שאינן נתמכות ברכיב נשארות מוסרות.
     */
    return source
        .replace(
            Regex(
                pattern = """\[\[\s*/?\s*BOLD\s*]]""",
                option = RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(
            Regex(
                pattern = """\[\[\s*/?\s*RED\s*]]""",
                option = RegexOption.IGNORE_CASE
            ),
            ""
        )
        .replace(
            Regex("""[ \t]+\n"""),
            "\n"
        )
        .replace(
            Regex("""\n{3,}"""),
            "\n\n"
        )
        .trim()
}

// ───────────────────────────────
// מציאת הסבר מתוך Explanations
// ───────────────────────────────
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean = false
): String {
    val cleanRawItem =
        rawItem
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )

    val displayItem =
        ExerciseTitleFormatter
            .displayName(cleanRawItem)
            .ifBlank {
                cleanRawItem
            }
            .trim()

    val canonicalAlias =
        resolveExerciseAlias(
            displayItem
        )
            .trim()

    /*
     * משתמשים רק בשמות מלאים ובטוחים.
     *
     * אסור לשלוח ל־Resolver מילה אחרונה בלבד כמו:
     * "מלפנים", "חיצונית", "בסיבוב" או "ימין",
     * משום שהיא עלולה להתאים לתרגיל אחר.
     */
    val candidates =
        linkedSetOf<String>().apply {
            add(cleanRawItem)
            add(displayItem)
            add(canonicalAlias)

            add(
                cleanRawItem
                    .substringBefore("(")
                    .trim()
            )

            add(
                displayItem
                    .substringBefore("(")
                    .trim()
            )
        }
            .map { candidate ->
                candidate
                    .trim()
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
            }
            .filter { candidate ->
                candidate.isNotBlank()
            }
            .distinct()

    candidates.forEach { candidate ->
        val resolved =
            ExerciseExplanationResolver.get(
                belt = belt,
                topic = topic,
                item = candidate,
                isEnglish = isEnglish
            )
                .trim()

        val cleaned =
            if ("::" in resolved) {
                resolved
                    .split("::")
                    .map { part ->
                        part.trim()
                    }
                    .lastOrNull { part ->
                        part.isNotBlank()
                    }
                    ?: resolved
            } else {
                resolved
            }
                .trim()

        val isFallback =
            if (isEnglish) {
                cleaned.isBlank() ||
                        cleaned.startsWith(
                            "Detailed explanation for:"
                        ) ||
                        cleaned.startsWith(
                            "There is currently no explanation"
                        )
            } else {
                cleaned.isBlank() ||
                        cleaned.startsWith(
                            "הסבר מפורט על"
                        ) ||
                        cleaned.startsWith(
                            "אין כרגע"
                        )
            }

        if (!isFallback) {
            return cleaned
        }
    }

    return if (isEnglish) {
        "There is currently no detailed explanation for this exact exercise in the database."
    } else {
        "אין כרגע הסבר מפורט לתרגיל המדויק הזה במאגר."
    }
}

internal fun normalizeExerciseQuery(text: String): String {
    return text
        .lowercase()

        // זכר
        .replace("תסביר לי", "")
        .replace("תן הסבר", "")
        .replace("הסבר על", "")
        .replace("איך עושים", "")
        .replace("איך מבצעים", "")

        // נקבה
        .replace("תסבירי לי", "")
        .replace("תסבירי", "")
        .replace("תני הסבר", "")
        .replace("איך תבצעי", "")
        .replace("איך עושים", "")
        .replace("איך מבצעים", "")

        // כללי
        .replace("בבקשה", "")
        .replace("אפשר", "")
        .replace("תרגיל", "")
        .replace("בעברית", "")
        .replace("באנגלית", "")
        .replace("?", "")
        .replace("!", "")
        .replace(",", " ")
        .replace(".", " ")
        .replace("־", "-")
        .replace("–", "-")
        .replace("—", "-")
        .replace(
            Regex("""\s*-\s*"""),
            " - "
        )
        .replace("\\s+".toRegex(), " ")
        .trim()
}

/*
 * נרמול לצורך השוואת זהות בין שמות תרגילים.
 *
 * המקפים השונים, סימני הפיסוק והרווחים אינם חלק
 * מזהות התרגיל. כך:
 *
 * "קוואלר - מרפק"
 * "קוואלר–מרפק"
 * "קוואלר מרפק"
 *
 * נחשבים לאותו שם, בלי לאפשר התאמה לתרגיל אחר.
 */
private fun normalizeExerciseIdentity(
    text: String
): String {
    return normalizeExerciseQuery(text)
        .replace("־", " ")
        .replace("–", " ")
        .replace("—", " ")
        .replace("-", " ")
        .replace("/", " ")
        .replace("\\", " ")
        .replace(":", " ")
        .replace(";", " ")
        .replace(
            Regex("""[()\[\]{}"'׳״]"""),
            " "
        )
        .replace(
            Regex("""\s+"""),
            " "
        )
        .trim()
}

/*
 * תיקון טעויות נפוצות של מנוע זיהוי הדיבור
 * במילה "קוואלר".
 *
 * התיקון מופעל רק כאשר המשפט מכיל מאפיין ברור
 * של אחד מתרגילי הקוואלר, כדי שלא להחליף בטעות
 * את המילה "קבלה" במשפטים אחרים.
 */
internal fun normalizeRecognizedExerciseSpeech(
    raw: String
): String {
    val clean =
        raw
            .trim()
            .replace(
                Regex("""\s+"""),
                " "
            )

    if (clean.isBlank()) {
        return clean
    }

    val normalized =
        normalizeExerciseIdentity(
            clean
        )

    val looksLikeKavalerExercise =
        listOf(
            "אגודלים",
            "מרפק",
            "הליכה לאחור",
            "הליכה לפנים",
            "נגד התנגדות",
            "נגד ההתנגדות"
        ).any { marker ->
            marker in normalized
        }

    if (!looksLikeKavalerExercise) {
        return clean
    }

    return clean
        .replace(
            Regex(
                pattern =
                    """(?<![\p{L}])(?:קבלה|קבלר|קוואל|קוואלר|קוואלרר|קאוולר|קוואולר)(?![\p{L}])""",
                option =
                    RegexOption.IGNORE_CASE
            ),
            "קוואלר"
        )
        .replace(
            Regex("""\s+"""),
            " "
        )
        .trim()
}

/*
 * מנוע זיהוי הדיבור מפרק לעיתים את המילה "מהם"
 * לשתי מילים: "מה עם".
 *
 * התיקון מתבצע רק לפני שם עצם ברבים, כדי לא לפגוע
 * בשאלה תקינה כמו "מה עם האימון ביום חמישי".
 */
internal fun normalizeRecognizedAssistantSpeech(
    raw: String
): String {
    val clean =
        raw
            .trim()
            .replace(
                Regex("""\s+"""),
                " "
            )

    if (clean.isBlank()) {
        return clean
    }

    return clean
        .replace(
            Regex(
                pattern =
                    """^מה\s+עם\s+((?:ה)?(?:אימונים|תרגילים|נושאים|סניפים|מאמנים|ימים|שעות|קבוצות|חגורות))(?=\s|$)"""
            ),
            "מהם $1"
        )
        .replace(
            Regex(
                pattern =
                    """^מה\s+הם\s+((?:ה)?(?:אימונים|תרגילים|נושאים|סניפים|מאמנים|ימים|שעות|קבוצות|חגורות))(?=\s|$)"""
            ),
            "מהם $1"
        )
        .replace(
            Regex("""\s+"""),
            " "
        )
        .trim()
}

private fun resolveExerciseAlias(raw: String): String {
    val q = normalizeExerciseQuery(raw)

    val aliases = linkedMapOf(
        // עברית
        "מגל" to "בעיטת מגל",
        "בעיטת מגל" to "בעיטת מגל",
        "בעיטת מגל ימנית" to "בעיטת מגל",
        "בעיטת מגל שמאלית" to "בעיטת מגל",

        "סטירה" to "בעיטת סטירה",
        "בעיטת סטירה" to "בעיטת סטירה",
        "בעיטת סטירה חיצונית" to "בעיטת סטירה חיצונית",
        "בעיטת סטירה פנימית" to "בעיטת סטירה פנימית",
        "בעיטת סטירה חיצונית בסיבוב" to "בעיטת סטירה חיצונית בסיבוב",

        "דקירה" to "הגנה נגד דקירה",
        "הגנה נגד דקירה" to "הגנה נגד דקירה",
        "דקירה מזרחית" to "הגנה נגד דקירה מזרחית",
        "דקירה מערבית" to "הגנה נגד דקירה מערבית",
        "דקירת מלפנים" to "הגנה נגד דקירה מלפנים",
        "דקירת מלמטה" to "הגנה נגד דקירה מלמטה",
        "דקירה נמוכה" to "הגנה נגד דקירה נמוכה",

        "roundhouse" to "בעיטת מגל",
        "roundhouse kick" to "בעיטת מגל",

        "outside slap kick" to "בעיטת סטירה חיצונית",
        "slap kick" to "בעיטת סטירה",
        "inside slap kick" to "בעיטת סטירה פנימית",
        "spinning outside slap kick" to "בעיטת סטירה חיצונית בסיבוב",

        "knife defense" to "הגנה נגד דקירה",
        "knife stab defense" to "הגנה נגד דקירה",
        "eastern stab defense" to "הגנה נגד דקירה מזרחית",
        "western stab defense" to "הגנה נגד דקירה מערבית",

        "בעיטה קדמית" to "בעיטה קדמית",
        "קדמית" to "בעיטה קדמית",
        "בעיטת צד" to "בעיטת צד",
        "צד" to "בעיטת צד",
        "אגרוף ישר" to "אגרוף ישר",
        "ישר" to "אגרוף ישר",
        "אפרקאט" to "אפרקאט",
        "וו" to "וו",
        "הוק" to "וו",
        "מרפק" to "מכת מרפק",
        "ברך" to "מכת ברך",

        // אנגלית
        "roundhouse" to "בעיטת מגל",
        "roundhouse kick" to "בעיטת מגל",
        "front kick" to "בעיטה קדמית",
        "side kick" to "בעיטת צד",
        "straight punch" to "אגרוף ישר",
        "jab" to "אגרוף ישר",
        "cross" to "אגרוף ישר",
        "uppercut" to "אפרקאט",
        "hook" to "וו",
        "elbow" to "מכת מרפק",
        "knee" to "מכת ברך"
    )

    /*
   * ממירים רק כינוי שזהה לכל השאלה.
   *
   * אסור להשתמש ב־contains, משום ששם מלא כמו
   * "קוואלר מרפק" מכיל את המילה "מרפק",
   * אך אינו התרגיל הכללי "מכת מרפק".
   */
    aliases[q]?.let { exactAlias ->
        return exactAlias
    }

    return raw
        .trim()
        .replace("־", "-")
        .replace("–", "-")
        .replace("—", "-")
        .replace(
            Regex("""\s*-\s*"""),
            " - "
        )
        .replace(
            Regex("""\s+"""),
            " "
        )
}

private fun getExerciseAnswerWithFallback(
    question: String,
    preferredBelt: Belt?,
    isEnglish: Boolean
): String {

    val rawExercise =
        extractExerciseNameFromQuestion(
            question
        )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?: question.trim()

    /*
     * חיפוש יחיד ומרכזי מול ExplanationSearchIndex.
     *
     * האינדקס נבנה אוטומטית מתוך
     * ExerciseIdentityRegistry, ולכן אין כאן
     * שום רשימת תרגילים ידנית.
     */
    val verifiedMatch =
        il.kmi.app.domain.ExplanationSearchIndex
            .findBest(
                query = rawExercise,
                preferredBelt = preferredBelt,
                minScore = 180
            )

    if (verifiedMatch != null) {
        val verifiedExplanation =
            verifiedMatch.explanation
                .trim()

        val isRealExplanation =
            verifiedExplanation.isNotBlank() &&
                    !verifiedExplanation.startsWith(
                        "הסבר מפורט על:"
                    ) &&
                    !verifiedExplanation.startsWith(
                        "אין כרגע"
                    ) &&
                    !verifiedExplanation.startsWith(
                        "Detailed explanation for:"
                    ) &&
                    !verifiedExplanation.startsWith(
                        "There is currently no explanation"
                    )

        if (isRealExplanation) {
            return verifiedExplanation
        }
    }

    /*
     * ניסיון נוסף רק כאשר קיימת חגורה מועדפת.
     *
     * גם כאן לא מייצרים הסבר חדש, אלא פונים
     * ל־Resolver הקיים שמחזיר רק תוכן מהמאגר.
     */
    if (preferredBelt != null) {
        val localExplanation =
            findExplanationForHit(
                belt = preferredBelt,
                rawItem = rawExercise,
                topic = "",
                isEnglish = isEnglish
            )
                .trim()

        val isLocalFallback =
            if (isEnglish) {
                localExplanation.isBlank() ||
                        localExplanation.startsWith(
                            "There is currently no"
                        ) ||
                        localExplanation.startsWith(
                            "Detailed explanation for:"
                        )
            } else {
                localExplanation.isBlank() ||
                        localExplanation.startsWith(
                            "אין כרגע"
                        ) ||
                        localExplanation.startsWith(
                            "הסבר מפורט על"
                        )
            }

        if (!isLocalFallback) {
            return localExplanation
        }
    }

    /*
     * אין להעביר את הבקשה ל־ExerciseAssistantEngine,
     * משום שהוא עשוי לנסח תשובה כללית שאינה קיימת
     * במאגר ההסברים.
     */
    return if (isEnglish) {
        "I could not find a verified explanation for this exact exercise in the database. Try specifying the full exercise name and belt."
    } else {
        "לא נמצא במאגר הסבר מאומת לתרגיל המדויק הזה. נסה לציין את השם המלא ואת החגורה."
    }
}

internal fun hasVerifiedExerciseMatch(
    rawQuestion: String,
    preferredBelt: Belt? = null
): Boolean {

    val exerciseQuery =
        extractExerciseNameFromQuestion(
            rawQuestion
        )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?: rawQuestion.trim()

    if (exerciseQuery.isBlank()) {
        return false
    }

    return il.kmi.app.domain.ExplanationSearchIndex
        .findBest(
            query = exerciseQuery,
            preferredBelt = preferredBelt,
            minScore = 180
        ) != null
}

internal fun extractExerciseNameFromQuestion(
    question: String
): String? {
    val q = question.lowercase().trim()

    val prefixes = listOf(
        "תן הסבר על",
        "תן הסבר ל",
        "תני הסבר על",
        "תני הסבר ל",
        "מה זה",
        "תסבירי לי את",
        "תסבירי לי",
        "תסבירי את",
        "תסבירי",
        "תסביר לי את",
        "תסביר לי",
        "תסביר את",
        "תסביר",
        "איך עושים את",
        "איך עושים",
        "איך מבצעים את",
        "איך מבצעים",
        "איך תבצעי את",
        "איך תבצעי",
        "הסבר על",
        "explain the",
        "explain",
        "how to do the",
        "how to do"
    )

    var cleaned = question.trim()

    prefixes.forEach { prefix ->
        if (q.startsWith(prefix)) {
            cleaned = question.trim().substring(prefix.length).trim()
            return@forEach
        }
    }

    return cleaned
        .removePrefix("את ")
        .removeSuffix("?")
        .trim()
        .takeIf { it.length > 1 }
}