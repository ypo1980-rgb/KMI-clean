package il.kmi.app.ui.assistant.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun hebrewDayName(dayIndex: Int): String {
    return when (dayIndex) {
        1 -> "ראשון"
        2 -> "שני"
        3 -> "שלישי"
        4 -> "רביעי"
        5 -> "חמישי"
        6 -> "שישי"
        7 -> "שבת"
        else -> ""
    }
}

private fun englishDayName(dayIndex: Int): String {
    return when (dayIndex) {
        1 -> "Sunday"
        2 -> "Monday"
        3 -> "Tuesday"
        4 -> "Wednesday"
        5 -> "Thursday"
        6 -> "Friday"
        7 -> "Saturday"
        else -> ""
    }
}

private fun hebrewMonthName(month: Int): String {
    return when (month) {
        1 -> "ינואר"
        2 -> "פברואר"
        3 -> "מרץ"
        4 -> "אפריל"
        5 -> "מאי"
        6 -> "יוני"
        7 -> "יולי"
        8 -> "אוגוסט"
        9 -> "ספטמבר"
        10 -> "אוקטובר"
        11 -> "נובמבר"
        12 -> "דצמבר"
        else -> ""
    }
}

private fun englishMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

private fun numberToHebrewSpeech(n: Int): String {
    return when (n) {
        0 -> "אפס"
        1 -> "אחת"
        2 -> "שתיים"
        3 -> "שלוש"
        4 -> "ארבע"
        5 -> "חמש"
        6 -> "שש"
        7 -> "שבע"
        8 -> "שמונה"
        9 -> "תשע"
        10 -> "עשר"
        11 -> "אחת עשרה"
        12 -> "שתים עשרה"
        13 -> "שלוש עשרה"
        14 -> "ארבע עשרה"
        15 -> "חמש עשרה"
        16 -> "שש עשרה"
        17 -> "שבע עשרה"
        18 -> "שמונה עשרה"
        19 -> "תשע עשרה"
        20 -> "עשרים"
        21 -> "עשרים ואחת"
        22 -> "עשרים ושתיים"
        23 -> "עשרים ושלוש"
        24 -> "עשרים וארבע"
        25 -> "עשרים וחמש"
        26 -> "עשרים ושש"
        27 -> "עשרים ושבע"
        28 -> "עשרים ושמונה"
        29 -> "עשרים ותשע"
        30 -> "שלושים"
        31 -> "שלושים ואחת"
        else -> n.toString()
    }
}

private fun formatDateForTrainingSpeech(rawDate: String, isEnglish: Boolean): String {
    val value = rawDate.trim()

    val parsedDate = runCatching {
        when {
            value.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> {
                LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }

            value.matches(Regex("""\d{1,2}/\d{1,2}/\d{4}""")) -> {
                LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/yyyy"))
            }

            value.matches(Regex("""\d{1,2}-\d{1,2}-\d{4}""")) -> {
                LocalDate.parse(value, DateTimeFormatter.ofPattern("d-M-yyyy"))
            }

            else -> null
        }
    }.getOrNull() ?: return rawDate

    val dayOfWeekIndex = when (parsedDate.dayOfWeek.value) {
        1 -> 2 // Monday -> שני
        2 -> 3 // Tuesday -> שלישי
        3 -> 4 // Wednesday -> רביעי
        4 -> 5 // Thursday -> חמישי
        5 -> 6 // Friday -> שישי
        6 -> 7 // Saturday -> שבת
        7 -> 1 // Sunday -> ראשון
        else -> 1
    }

    val day = parsedDate.dayOfMonth
    val month = parsedDate.monthValue
    val year = parsedDate.year

    return if (isEnglish) {
        "${englishDayName(dayOfWeekIndex)}, $day ${englishMonthName(month)} $year"
    } else {
        "${hebrewDayName(dayOfWeekIndex)}, ${numberToHebrewSpeech(day)} ב${hebrewMonthName(month)}"
    }
}

private fun formatTimeForTrainingSpeech(rawTime: String, isEnglish: Boolean): String {
    val parts = rawTime.trim().split(":")
    if (parts.size != 2) return rawTime

    val hour = parts[0].toIntOrNull() ?: return rawTime
    val minute = parts[1].toIntOrNull() ?: return rawTime

    if (hour !in 0..23 || minute !in 0..59) return rawTime

    return if (isEnglish) {
        when (minute) {
            0 -> "$hour o'clock"
            else -> "$hour ${minute.toString().padStart(2, '0')}"
        }
    } else {
        when (minute) {
            0 -> "בשעה ${numberToHebrewSpeech(hour)}"
            else -> "בשעה ${numberToHebrewSpeech(hour)} ו${numberToHebrewSpeech(minute)} דקות"
        }
    }
}

private fun prepareTrainingTextForSpeech(text: String, isEnglish: Boolean): String {
    return text
        // תאריכים: 2026-05-11
        .replace(Regex("""\b\d{4}-\d{2}-\d{2}\b""")) { match ->
            formatDateForTrainingSpeech(match.value, isEnglish)
        }

        // תאריכים: 11/05/2026
        .replace(Regex("""\b\d{1,2}/\d{1,2}/\d{4}\b""")) { match ->
            formatDateForTrainingSpeech(match.value, isEnglish)
        }

        // תאריכים: 11-05-2026
        .replace(Regex("""\b\d{1,2}-\d{1,2}-\d{4}\b""")) { match ->
            formatDateForTrainingSpeech(match.value, isEnglish)
        }

        // שעות: 18:30 / 20:00
        .replace(Regex("""\b\d{1,2}:\d{2}\b""")) { match ->
            formatTimeForTrainingSpeech(match.value, isEnglish)
        }

        // טווח שעות אחרי המרה, כדי שלא יישמע מקוטע מדי
        .replace(" עד בשעה ", " עד ")
}

private fun sanitizeTrainingTextForSpeech(text: String, isEnglish: Boolean): String {
    val speechReadyText = prepareTrainingTextForSpeech(
        text = text,
        isEnglish = isEnglish
    )

    val cleaned = speechReadyText
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            line
                .replace("•", "")
                .replace("(", ". ")
                .replace(")", "")
                .replace(" - ", ". ")
                .replace(" – ", ". ")
                .replace(":", " ")
                .replace(Regex("""ביום\s+יום\s+"""), "ביום ")
                .replace(Regex("""יום\s+יום\s+"""), "יום ")
                .replace(Regex("""\s+"""), " ")
                .replace("נוער + בוגרים", "נוער ובוגרים")
                .replace("Youth + Adults", "Youth and Adults")
                .trim()
        }
        .joinToString(". ")

    return if (isEnglish) {
        cleaned
            .replace(Regex("\\s+"), " ")
            .trim()
    } else {
        cleaned
            .replace(Regex("[A-Za-z_]{2,}[A-Za-z0-9_().-]*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

private fun sanitizeAssistantTextForSpeech(
    text: String,
    isEnglish: Boolean
): String {
    /*
     * תגיות הצבע מיועדות לתצוגה בלבד.
     * מסירים אותן לפני עיבוד הטקסט להקראה.
     */
    val speechSource =
        text
            .replace(
                Regex(
                    pattern = """\[\[\s*/?\s*RED_BOLD\s*]]""",
                    option = RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    pattern = """\[\[\s*/?\s*BLUE_BOLD\s*]]""",
                    option = RegexOption.IGNORE_CASE
                ),
                ""
            )

    fun isCodeLikeLine(line: String): Boolean {
        val trimmed = line.trim()

        if (trimmed.isBlank()) return true

        if (
            trimmed.startsWith("```") ||
            trimmed.startsWith("import ") ||
            trimmed.startsWith("package ") ||
            trimmed.startsWith("class ") ||
            trimmed.startsWith("fun ") ||
            trimmed.startsWith("val ") ||
            trimmed.startsWith("var ") ||
            trimmed.startsWith("const ") ||
            trimmed.startsWith("@Composable") ||
            trimmed.startsWith("private fun ") ||
            trimmed.startsWith("override fun ")
        ) return true

        if (
            trimmed.contains("->") ||
            trimmed.contains("==") ||
            trimmed.contains(" = ") ||
            trimmed.contains("Icons.") ||
            trimmed.contains("MaterialTheme.") ||
            trimmed.contains("TextField") ||
            trimmed.contains("IconButton") ||
            trimmed.contains("Modifier.") ||
            trimmed.contains("mutableStateOf") ||
            trimmed.contains("remember {") ||
            trimmed.contains("LaunchedEffect(")
        ) return true

        val codeSymbolCount = trimmed.count { it in setOf('{', '}', '(', ')', '=', '<', '>', '@') }
        if (codeSymbolCount >= 4) return true

        return false
    }

    var spokenSubTopicIndex = 0

    val cleaned = speechSource
        .lineSequence()
        .map { line ->
            val originalLine = line.trim()
            val startsWithSubTopicBullet =
                originalLine.startsWith("•") ||
                        originalLine.startsWith("●") ||
                        originalLine.startsWith("▪") ||
                        originalLine.startsWith("◦")

            val cleanedLine = originalLine
                // מסיר bullets / נקודות רשימה לפני כותרות
                .replace(Regex("""^[•●▪◦]\s*"""), "")
                .replace(Regex("""^[-–—]\s*"""), "")

                // מסיר מספור בתחילת שורה: 1. / 2. / 10.
                .replace(Regex("""^\d+\.\s*"""), "")

                // במקום להגיד מקף / סימן — עושים הפסקה טבעית בדיבור
                .replace(Regex("""\s+[-–—]\s+"""), ". ")
                .replace(Regex("""[-–—]"""), ". ")

                // מנקה תווים שלא צריכים להיקרא בקול
                .replace("•", "")
                .replace("●", "")
                .replace("▪", "")
                .replace("◦", "")
                .replace("`", "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (startsWithSubTopicBullet && cleanedLine.isNotBlank()) {
                spokenSubTopicIndex++

                when {
                    spokenSubTopicIndex == 1 && isEnglish ->
                        "First sub-topic. $cleanedLine"

                    spokenSubTopicIndex == 1 && !isEnglish ->
                        "תת נושא ראשון. $cleanedLine"

                    isEnglish ->
                        "Next sub-topic. $cleanedLine"

                    else ->
                        "תת נושא הבא. $cleanedLine"
                }
            } else {
                cleanedLine
            }
        }
        .filter { it.isNotBlank() }
        .filterNot { line -> isCodeLikeLine(line) }
        .filterNot { line ->
            line.equals("Can I help you with anything else?", ignoreCase = true) ||
                    line.equals("אני יכול לעזור לך בעוד משהו?", ignoreCase = true)
        }
        .joinToString(". ")

    return if (isEnglish) {
        cleaned
            .replace(Regex("""[`"']"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    } else {
        cleaned
            .replace(Regex("""[`"']"""), "")
            .replace(Regex("""[A-Za-z_]{2,}[A-Za-z0-9_().]*"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * מחזיר להקראה רק את משפט הפתיחה של תשובת רשימה.
 *
 * לדוגמה, מתוך:
 * "מצאתי 21 תרגילים בנושא שביקשת:
 *
 * 1. תרגיל ראשון
 * 2. תרגיל שני"
 *
 * יוקרא רק:
 * "מצאתי 21 תרגילים בנושא שביקשת."
 */
private fun shortMaterialSpeech(
    answer: String,
    isEnglish: Boolean
): String {
    val openingLine =
        answer
            .lineSequence()
            .map { line ->
                line
                    .trim()
                    .removeSuffix(":")
                    .trim()
            }
            .firstOrNull { line ->
                line.isNotBlank()
            }
            .orEmpty()

    if (openingLine.isBlank()) {
        return if (isEnglish) {
            "I found the requested items. They are shown on the screen."
        } else {
            "מצאתי את הפריטים שביקשת. הם מופיעים על המסך."
        }
    }

    return sanitizeAssistantTextForSpeech(
        text = openingLine,
        isEnglish = isEnglish
    )
}

/**
 * קובע מה יוקרא בעת לחיצה על "הקרא שוב".
 *
 * אם התשובה מכילה רשימה ממוספרת או רשימת נקודות,
 * מקריאים רק את משפט הפתיחה. בתשובה רגילה ממשיכים
 * להשתמש בניקוי ההקראה הקיים.
 */
internal fun assistantAnswerTextForSpeech(
    answer: String,
    isEnglish: Boolean,
    exerciseName: String? = null,
    isExerciseExplanation: Boolean = false
): String {
    val nonBlankLines =
        answer
            .lineSequence()
            .map { line ->
                line.trim()
            }
            .filter { line ->
                line.isNotBlank()
            }
            .toList()

    val containsStructuredList =
        nonBlankLines
            .drop(1)
            .any { line ->
                line.matches(
                    Regex(
                        """^(?:\d+[.)]|[•●▪◦]|[-–—])\s+.+"""
                    )
                )
            }

    /*
     * רשימה מקבלת עדיפות, גם אם מקור התשובה
     * הוא מנוע התרגילים.
     */
    if (containsStructuredList) {
        return shortMaterialSpeech(
            answer = answer,
            isEnglish = isEnglish
        )
    }

    /*
     * תשובת ספירה היא משפט קצר שכבר מתאים להקראה.
     *
     * היא עשויה להגיע ממקור EXERCISES, אבל אינה
     * הסבר על תרגיל ולכן אסור להחליף אותה במשפט
     * "מצאתי את ההסבר...".
     */
    val normalizedAnswer =
        answer
            .lowercase()
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()

    val isExerciseCountAnswer =
        Regex(
            """(?:יש\s+\d+\s+תרגילים|there\s+(?:are|is)\s+\d+\s+exercises)""",
            RegexOption.IGNORE_CASE
        )
            .containsMatchIn(normalizedAnswer) ||
                (
                        "תרגילים בסך הכול" in
                                normalizedAnswer
                        ) ||
                (
                        "exercises in total" in
                                normalizedAnswer
                        )

    if (isExerciseCountAnswer) {
        return sanitizeAssistantTextForSpeech(
            text = answer,
            isEnglish = isEnglish
        )
    }

    /*
     * בהסבר על תרגיל מקריאים רק הקדמה קצרה.
     * ההסבר המלא נשאר מוצג בכרטיס.
     */
    if (isExerciseExplanation) {
        val cleanExerciseName =
            exerciseName
                ?.substringBefore(" • ")
                ?.trim()
                ?.takeIf { name ->
                    name.isNotBlank() &&
                            !name.equals(
                                "תשובה על תרגילים",
                                ignoreCase = true
                            ) &&
                            !name.equals(
                                "Exercise answer",
                                ignoreCase = true
                            )
                }

        val introduction =
            if (isEnglish) {
                cleanExerciseName
                    ?.let { name ->
                        "I found the explanation for $name."
                    }
                    ?: "I found the explanation for the exercise you requested."
            } else {
                cleanExerciseName
                    ?.let { name ->
                        "מצאתי את ההסבר על תרגיל $name."
                    }
                    ?: "מצאתי את ההסבר על התרגיל שביקשת."
            }

        return sanitizeAssistantTextForSpeech(
            text = introduction,
            isEnglish = isEnglish
        )
    }

    return sanitizeAssistantTextForSpeech(
        text = answer,
        isEnglish = isEnglish
    )
}

internal fun shortTrainingSpeech(
    question: String,
    answer: String,
    isEnglish: Boolean
): String {
    val q = question.lowercase().trim()
    val a = answer.lowercase().trim()

    val looksLikeNextTraining =
        q.contains("האימון הבא") ||
                q.contains("אימון הבא") ||
                q.contains("מתי האימון") ||
                q.contains("next training") ||
                q.contains("next workout") ||
                a.contains("האימון הבא") ||
                a.contains("next training")

    val looksLikeTrainingList =
        q.contains("רשימת") ||
                q.contains("אימונים לשבוע") ||
                q.contains("שבוע הקרוב") ||
                q.contains("האימונים הקרובים") ||
                q.contains("show me") ||
                q.contains("upcoming") ||
                q.contains("this week")

    return when {
        looksLikeNextTraining && isEnglish ->
            "I found your next training. The details are shown on the screen."

        looksLikeNextTraining && !isEnglish ->
            "מצאתי את האימון הבא שלך. הפרטים מופיעים על המסך."

        looksLikeTrainingList && isEnglish ->
            "Here is the training list you asked for. The full details are shown on the screen."

        looksLikeTrainingList && !isEnglish ->
            "הנה רשימת האימונים שביקשת. הפרטים המלאים מופיעים על המסך."

        isEnglish ->
            "I found the training information you asked for. The details are shown on the screen."

        else ->
            "מצאתי את פרטי האימונים שביקשת. הם מופיעים על המסך."
    }
}

internal fun normalizeForTts(
    text: String
): String {
    return text
        .replace(
            "ק.מ.י",
            "קמי"
        )
        .replace(
            "ק מ י",
            "קמי"
        )
        .replace(
            "K.A.M.I",
            "KAMI",
            ignoreCase = true
        )
        .replace(
            "K M I",
            "KAMI",
            ignoreCase = true
        )
        .replace(
            "יובל",
            "יוּבַל"
        )
        /*
         * אין להחליף כאן את המילה "שלך".
         *
         * ההגייה שלה מטופלת במקום המרכזי
         * ב־KmiTtsManager.normalizeForTts().
         */
        .replace(
            "Yuval",
            "You-val",
            ignoreCase = true
        )
        .replace(
            "קמי",
            "קָמִי"
        )
}