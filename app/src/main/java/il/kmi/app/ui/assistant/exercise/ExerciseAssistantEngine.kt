package il.kmi.app.ui.assistant.exercise

import il.kmi.app.domain.ExplanationSearchIndex
import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt

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
             * חיפוש ישיר במאגר ההסברים קודם לכל מנגנון אחר.
             * אם שם התרגיל קיים ומתקבלת התאמה טובה,
             * מחזירים מיד את ההסבר ולא שאלת הבהרה כללית.
             */
            if (exerciseName.isNotBlank()) {
                val exactExplanation =
                    ExplanationSearchIndex.findBest(
                        query = exerciseName,
                        preferredBelt = preferredBelt
                    )
                        ?: ExplanationSearchIndex.findBest(
                            query = exerciseName,
                            preferredBelt = null
                        )

                exactExplanation
                    ?.explanation
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { explanation ->
                        return explanation
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
             * חיפוש תרגילים מתוך מאגר החומר.
             */
            val hits =
                ExerciseSearchService.searchExercisesForQuestion(
                    question = exerciseName.ifBlank { cleanQuestion },
                    beltEnum = preferredBelt
                )

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