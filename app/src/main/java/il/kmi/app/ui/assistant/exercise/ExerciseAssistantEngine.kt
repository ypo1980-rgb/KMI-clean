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
      * חיפוש כללי מול האינדקס המרכזי.
      *
      * אין כאן שמות קשיחים של תרגילים, סוגי תקיפה,
      * כיוונים או איברי גוף. אותה לוגיקה חלה על כל
      * משפחות התרגילים הקיימות והעתידיות.
      */
            val searchQuestion =
                exerciseName.ifBlank {
                    cleanQuestion
                }

            val indexedMatches =
                ExplanationSearchIndex.findMatches(
                    query = searchQuestion,
                    preferredBelt = preferredBelt,
                    minScore = 180,
                    maxItems = 12
                )

            /*
             * ציון של 1000 ומעלה מצביע על התאמה מילולית
             * מדויקת לשם רשמי או לכינוי שהוגדר ב־Registry.
             *
             * אם קיימת התאמה מדויקת אחת בעלת הציון הגבוה
             * ביותר, אפשר להציג אותה ישירות.
             *
             * אם אין התאמה מדויקת ויש מספר תוצאות, השאילתה
             * כללית או חלקית ולכן המשתמש חייב לבחור.
             */
            val bestIndexedScore =
                indexedMatches
                    .firstOrNull()
                    ?.score
                    ?: 0

            val bestMatchIsExact =
                bestIndexedScore >= 1000

            val competingMatches =
                if (bestMatchIsExact) {
                    /*
                     * חגורה מועדפת מקבלת תוספת ציון ולכן תכריע
                     * בין אותו שם שמופיע בכמה חגורות.
                     * ללא הכרעה נשאיר את כל התוצאות המובילות.
                     */
                    indexedMatches.filter { match ->
                        match.score == bestIndexedScore
                    }
                } else {
                    /*
                     * בשאילתה חלקית כל ההתאמות רלוונטיות לבחירה,
                     * גם אם אחת מהן קיבלה מעט יותר נקודות.
                     */
                    indexedMatches
                }

            val shouldRequestExerciseChoice =
                competingMatches.size > 1

            if (shouldRequestExerciseChoice) {
                val optionsText =
                    competingMatches
                        .take(8)
                        .mapIndexed { index, match ->
                            val beltText =
                                if (isEnglish) {
                                    match.belt.name
                                        .lowercase()
                                        .replaceFirstChar { character ->
                                            character.uppercase()
                                        }
                                } else {
                                    match.belt.heb
                                }

                            if (isEnglish) {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(match.title)
                                    append(" — ")
                                    append(beltText)
                                    append(" belt")
                                }
                            } else {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(match.title)
                                    append(" — ")
                                    /*
                                     * belt.heb כבר מחזיר שם מלא,
                                     * לדוגמה: "חגורה כחולה".
                                     */
                                    append(beltText)
                                }
                            }
                        }
                        .joinToString("\n")

                return if (isEnglish) {
                    buildString {
                        appendLine(
                            "I found several exercises that match your request."
                        )
                        appendLine(
                            "Which exercise did you mean?"
                        )
                        appendLine()
                        append(optionsText)
                        appendLine()
                        append(
                            "Say or enter the full exercise name."
                        )
                    }
                } else {
                    buildString {
                        appendLine(
                            "מצאתי מספר תרגילים שמתאימים לבקשה."
                        )
                        appendLine(
                            "לאיזה תרגיל התכוונת?"
                        )
                        appendLine()
                        append(optionsText)
                        appendLine()
                        append(
                            "אמור או הקלד את שמו המלא של התרגיל."
                        )
                    }
                }
            }

            /*
             * תוצאות החיפוש הכללי נשמרות גם עבור מנגנוני
             * הגיבוי בהמשך הפונקציה.
             */
            val initialHits =
                ExerciseSearchService
                    .searchExercisesForQuestion(
                        question = searchQuestion,
                        beltEnum = preferredBelt
                    )

            /*
             * רק בקשה חד־משמעית יכולה להמשיך לחיפוש הסבר.
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