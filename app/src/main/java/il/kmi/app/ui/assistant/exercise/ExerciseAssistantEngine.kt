package il.kmi.app.ui.assistant.exercise

import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.domain.ExplanationSearchIndex
import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations

object ExerciseAssistantEngine {

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return try {
            val cleanQuestion = question.trim()

            if (cleanQuestion.isBlank()) {
                return if (isEnglish) {
                    "Write or say the name of an exercise."
                } else {
                    "כתוב או אמור את שם התרגיל."
                }
            }

            val exerciseName = cleanExerciseName(cleanQuestion)

            /*
 * התאמה חד־משמעית לפני החיפוש המקורב.
 * מונעת בלבול עם תרגילים דומים של איום סכין לבטן.
 */
            val normalizedExerciseName = exerciseName
                .replace("–", "-")
                .replace("—", "-")
                .replace("־", "-")
                .replace(Regex("\\s+"), " ")
                .trim()

            val isLowerAbdomenKnifeThreat =
                normalizedExerciseName.contains("איום סכין") &&
                        normalizedExerciseName.contains("חוד") &&
                        (
                                normalizedExerciseName.contains("לבטן התחתונה") ||
                                        normalizedExerciseName.contains("לבטן תחתונה")
                                )

            if (!isEnglish && isLowerAbdomenKnifeThreat) {
                return "ביד שמאל תפיסת שרש כף יד הסכין תוך דחיפת יד התוקף למטה ושמאלה להרחקת הסכין ואגרוף ימין לפנים. במידה והאיום צמוד: נגיחה ולהמשיך התקפות. (יש ללמד גם בשמאל)"
            }

            /*
             * מחפשים את כל המועמדים לפני בחירת התוצאה הטובה ביותר.
             * כאשר שם כללי מתאים לכמה תרגילים, אין לבחור אחד
             * מהם אוטומטית.
             */
            val searchQuestion =
                exerciseName.ifBlank {
                    cleanQuestion
                }

            val initialHits =
                ExerciseSearchService
                    .searchExercisesForQuestion(
                        question = searchQuestion,
                        beltEnum = preferredBelt
                    )

            /*
             * בקשה כללית להגנה מאיום סכין אינה מספיקה
             * לבחירת תרגיל מסוים.
             *
             * בבקשה כללית מחפשים בכל החגורות כדי שלא תיבחר
             * אוטומטית התאמה יחידה מהחגורה המועדפת.
             */
            val normalizedChoiceQuery =
                normalizedExerciseName.lowercase()

            val isKnifeThreatRequest =
                normalizedChoiceQuery.contains("איום סכין") ||
                        normalizedChoiceQuery.contains("knife threat")

            val exerciseDetailMarkers = listOf(
                "ימין",
                "שמאל",
                "מלפנים",
                "מאחור",
                "קדמי",
                "אחורי",
                "בטן",
                "צוואר",
                "גרון",
                "עורק",
                "חוד",
                "להב",
                "צמוד",
                "מרחוק",
                "right",
                "left",
                "front",
                "rear",
                "behind",
                "abdomen",
                "stomach",
                "throat",
                "neck",
                "artery",
                "blade",
                "close"
            )

            val hasExerciseDetails =
                exerciseDetailMarkers.any { marker ->
                    normalizedChoiceQuery.contains(marker)
                }

            val isGenericKnifeThreatRequest =
                isKnifeThreatRequest &&
                        !hasExerciseDetails

            /*
             * בבקשה כללית מציגים את כל תרגילי איום הסכין
             * ישירות ממקור האמת. המשתמש אינו נדרש לזכור
             * את השם המדויק של התרגיל.
             */
            if (isGenericKnifeThreatRequest) {
                val knifeThreatOptions =
                    ContentRepo
                        .findExerciseOptionsContainingAll(
                            requiredTerms = listOf(
                                "איום",
                                "סכין"
                            )
                        )
                        .take(20)

                val optionsText =
                    knifeThreatOptions
                        .mapIndexed { index, option ->
                            val beltText =
                                if (isEnglish) {
                                    option.belt.name
                                        .lowercase()
                                        .replaceFirstChar { character ->
                                            character.uppercase()
                                        }
                                } else {
                                    option.belt.heb
                                }

                            if (isEnglish) {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(option.itemTitle)
                                    append(" — ")
                                    append(option.topicTitle)
                                    append(" — ")
                                    append(beltText)
                                    append(" belt")
                                }
                            } else {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(option.itemTitle)
                                    append(" — ")
                                    append(option.topicTitle)
                                    append(" — חגורה ")
                                    append(beltText)
                                }
                            }
                        }
                        .joinToString("\n")

                return if (isEnglish) {
                    buildString {
                        appendLine(
                            "I found the following knife-threat defence exercises:"
                        )
                        appendLine()
                        appendLine(optionsText)
                        appendLine()
                        append(
                            "Choose an exercise from the list " +
                                    "and I’ll explain it."
                        )
                    }
                } else {
                    buildString {
                        appendLine(
                            "מצאתי את התרגילים הבאים של הגנה מאיום סכין:"
                        )
                        appendLine()
                        appendLine(optionsText)
                        appendLine()
                        append(
                            "בחר תרגיל מהרשימה ואני אסביר אותו."
                        )
                    }
                }
            }

            /*
             * בבקשה כללית לאיום סכין מריצים כמה חיפושים
             * משלימים. כך נאספות אפשרויות שונות גם כאשר
             * החיפוש בביטוי הכללי מחזיר התאמה יחידה בלבד.
             */
            val ambiguitySourceHits =
                if (isGenericKnifeThreatRequest) {
                    listOf(
                        searchQuestion,
                        "איום סכין",
                        "הגנה מאיום סכין",
                        "איום סכין לעורק",
                        "איום סכין לגרגרת",
                        "איום סכין מלפנים",
                        "איום סכין מאחור",
                        "knife threat"
                    )
                        .flatMap { query ->
                            ExerciseSearchService
                                .searchExercisesForQuestion(
                                    question = query,
                                    beltEnum = null
                                )
                        }
                        .distinctBy { hit ->
                            listOf(
                                hit.belt.name,
                                hit.topic,
                                hit.item.orEmpty()
                            ).joinToString("|")
                        }
                } else {
                    initialHits
                }

            val ambiguousHits =
                if (isGenericKnifeThreatRequest) {
                    /*
                     * בקשה כללית תמיד מחייבת בחירה.
                     * אין לדרוש כאן יותר מתוצאה אחת, מפני שגם
                     * תוצאת חיפוש יחידה אינה הופכת את הבקשה
                     * הכללית לשם מדויק של תרגיל.
                     */
                    ambiguitySourceHits.take(8)
                } else {
                    ExerciseSearchService
                        .findAmbiguousExerciseHits(
                            question = searchQuestion,
                            hits = ambiguitySourceHits,
                            maxItems = 8
                        )
                }

            /*
             * חשוב: בקשה כללית לאיום סכין מסתיימת כאן תמיד.
             * אסור להמשיך אחריה ל-findBest ולבחור תרגיל אוטומטית.
             */
            if (
                isGenericKnifeThreatRequest ||
                ambiguousHits.isNotEmpty()
            ) {
                val exerciseList =
                    ExerciseSearchService
                        .formatHitsAsExerciseList(
                            hits = ambiguousHits,
                            maxItems = 8,
                            isEnglish = isEnglish
                        )
                        .trim()

                return if (isEnglish) {
                    buildString {
                        appendLine(
                            if (isGenericKnifeThreatRequest) {
                                "I found several knife-threat defence exercises."
                            } else {
                                "I found several exercises that match your request."
                            }
                        )

                        appendLine("Which exercise did you mean?")

                        if (exerciseList.isNotBlank()) {
                            appendLine()
                            append(exerciseList)
                        } else {
                            appendLine()
                            append(
                                "Please specify the direction, body area, " +
                                        "or exact exercise name."
                            )
                        }
                    }
                } else {
                    buildString {
                        appendLine(
                            if (isGenericKnifeThreatRequest) {
                                "מצאתי מספר תרגילים של הגנה מאיום סכין."
                            } else {
                                "מצאתי מספר תרגילים שמתאימים לבקשה."
                            }
                        )

                        appendLine("לאיזה תרגיל התכוונת?")

                        if (exerciseList.isNotBlank()) {
                            appendLine()
                            append(exerciseList)
                        } else {
                            appendLine()
                            append(
                                "ציין את הכיוון, אזור האיום או את " +
                                        "שמו המדויק של התרגיל."
                            )
                        }
                    }
                }
            }

            /*
             * רק בקשה שאינה כללית ואינה עמומה
             * יכולה להמשיך לחיפוש הסבר ישיר.
             */
            if (exerciseName.isNotBlank()) {
                val matchedExercise =
                    ExplanationSearchIndex.findBest(
                        query = exerciseName,
                        preferredBelt = preferredBelt
                    )
                        ?: ExplanationSearchIndex.findBest(
                            query = exerciseName,
                            preferredBelt = null
                        )

                if (matchedExercise != null) {
                    /*
                     * תוצאת החיפוש משמשת רק לזיהוי התרגיל והחגורה.
                     * את ההסבר עצמו שולפים ממאגר ההסברים המקורי
                     * לפי השם שהמשתמש ביקש.
                     */
                    /*
     * קודם פונים ישירות לקובץ Explanations.
     * כך ExerciseIdentityRegistry לא יכול להחליף את התרגיל
     * בתרגיל דומה בעל הסבר אחר.
     */
                    val directExplanation =
                        if (!isEnglish) {
                            Explanations.get(
                                belt = matchedExercise.belt,
                                item = exerciseName
                            ).trim()
                        } else {
                            ""
                        }

                    val directIsFallback =
                        directExplanation.isBlank() ||
                                directExplanation.startsWith("הסבר מפורט על") ||
                                directExplanation.startsWith("אין כרגע")

                    if (!directIsFallback) {
                        return directExplanation
                    }

                    val resolvedExplanation =
                        ExerciseExplanationResolver.get(
                            belt = matchedExercise.belt,
                            topic = "",
                            item = exerciseName,
                            isEnglish = isEnglish
                        ).trim()

                    val isFallback =
                        resolvedExplanation.isBlank() ||
                                resolvedExplanation.startsWith("הסבר מפורט על") ||
                                resolvedExplanation.startsWith("אין כרגע") ||
                                resolvedExplanation.startsWith("Detailed explanation for:") ||
                                resolvedExplanation.startsWith(
                                    "There is currently no explanation"
                                )

                    if (!isFallback) {
                        return resolvedExplanation
                    }

                    /*
                     * אם השם שנאמר הוא כינוי, מנסים שוב עם הכותרת
                     * הקנונית שנמצאה באינדקס.
                     */
                    val canonicalExplanation =
                        ExerciseExplanationResolver.get(
                            belt = matchedExercise.belt,
                            topic = "",
                            item = matchedExercise.title,
                            isEnglish = isEnglish
                        ).trim()

                    val isCanonicalFallback =
                        canonicalExplanation.isBlank() ||
                                canonicalExplanation.startsWith("הסבר מפורט על") ||
                                canonicalExplanation.startsWith("אין כרגע") ||
                                canonicalExplanation.startsWith("Detailed explanation for:") ||
                                canonicalExplanation.startsWith(
                                    "There is currently no explanation"
                                )

                    if (!isCanonicalFallback) {
                        return canonicalExplanation
                    }
                }
            }

            /*
             * אם לא נמצאה התאמה ישירה, מנסים את מנוע
             * ההסברים החכם עם השאלה המלאה.
             */
            val knowledgeAnswer =
                AssistantExerciseExplanationKnowledge.answer(
                    question = cleanQuestion,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            if (knowledgeAnswer != null) {
                return knowledgeAnswer
            }

            /*
  * משתמשים בתוצאות שכבר חושבו בתחילת הבקשה.
  */
            val hits = initialHits

            /*
             * אם לתוצאה הטובה ביותר קיים הסבר,
             * מחזירים אותו ישירות.
             */
            val bestExplanation =
                ExerciseSearchService.buildBestHitExplanation(
                    hits = hits,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            if (bestExplanation != null) {
                return bestExplanation
            }

            /*
             * רק כאשר נמצאו כמה תרגילים אך אין התאמה
             * מספקת להסבר, מציגים רשימת אפשרויות.
             */
            val exerciseList =
                ExerciseSearchService.formatHitsAsExerciseList(
                    hits = hits,
                    maxItems = 6,
                    isEnglish = isEnglish
                ).trim()

            if (exerciseList.isNotBlank()) {
                return if (isEnglish) {
                    buildString {
                        appendLine("I found several related exercises:")
                        appendLine()
                        appendLine(exerciseList)
                        appendLine()
                        append("Choose the exact exercise and I’ll explain it.")
                    }
                } else {
                    buildString {
                        appendLine("מצאתי מספר תרגילים קשורים:")
                        appendLine()
                        appendLine(exerciseList)
                        appendLine()
                        append("בחר את התרגיל המדויק ואני אסביר אותו.")
                    }
                }
            }

            if (isEnglish) {
                "I couldn't find a matching exercise."
            } else {
                "לא מצאתי תרגיל מתאים לשאלה."
            }
        } catch (_: Throwable) {
            if (isEnglish) {
                "There was a problem processing the exercise request."
            } else {
                "אירעה תקלה בעיבוד בקשת התרגיל."
            }
        }
    }

    /**
     * מסיר מהשאלה ביטויי בקשה כלליים ומשאיר את שם התרגיל.
     *
     * לדוגמה:
     * "הסבר מניעת חניקה" -> "מניעת חניקה"
     * "תן לי הסבר על בעיטת מגל" -> "בעיטת מגל"
     */
    private fun cleanExerciseName(question: String): String {
        return question
            .replace("תן לי הסבר על", " ")
            .replace("תני לי הסבר על", " ")
            .replace("אפשר הסבר על", " ")
            .replace("אני רוצה הסבר על", " ")
            .replace("תסביר לי על", " ")
            .replace("תסביר על", " ")
            .replace("הסבר על", " ")
            .replace("הסבר לתרגיל", " ")
            .replace("הסבר תרגיל", " ")
            .replace("הסבר", " ")
            .replace("איך מבצעים את", " ")
            .replace("איך מבצעים", " ")
            .replace("איך עושים את", " ")
            .replace("איך עושים", " ")
            .replace("explain the exercise", " ", ignoreCase = true)
            .replace("give me an explanation of", " ", ignoreCase = true)
            .replace("give me an explanation for", " ", ignoreCase = true)
            .replace("explain how to do", " ", ignoreCase = true)
            .replace("how do i perform", " ", ignoreCase = true)
            .replace("how to perform", " ", ignoreCase = true)
            .replace("how do i do", " ", ignoreCase = true)
            .replace("explain", " ", ignoreCase = true)
            .replace("\"", " ")
            .replace("'", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}