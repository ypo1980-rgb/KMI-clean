package il.kmi.app.ui.assistant.exercise

import android.util.Log
import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt

object ExerciseAssistantEngine {

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        try {
            // 1️⃣ נסה מנוע הסברים החכם
            val explain = AssistantExerciseExplanationKnowledge.answer(
                question = question,
                preferredBelt = preferredBelt
            )

            if (explain != null) {
                return explain
            }

            // 2️⃣ fallback – חיפוש תרגילים
            val hits = ExerciseSearchService.searchExercisesForQuestion(
                question = question,
                beltEnum = preferredBelt
            )

            // 3️⃣ אם נמצא hit טוב עם הסבר – החזר הסבר
            val bestExplanation = ExerciseSearchService.buildBestHitExplanation(
                hits = hits,
                preferredBelt = preferredBelt,
                isEnglish = isEnglish
            )

            if (!bestExplanation.isNullOrBlank()) {
                return if (isEnglish) {
                    bestExplanation
                } else {
                    "$bestExplanation\n\nהאם אני יכול לעזור לך בעוד משהו?"
                }
            }

            // 4️⃣ אם אין הסבר, החזר רשימת תרגילים
            val list = ExerciseSearchService.formatHitsAsExerciseList(
                hits = hits,
                maxItems = 6,
                isEnglish = isEnglish
            )

            if (list.isNotBlank()) {
                return if (isEnglish) {
                    "I found related exercises:\n$list\n\nChoose one and I’ll explain it."
                } else {
                    "מצאתי תרגילים שקשורים לשאלה:\n$list\n\nבחר אחד מהם ואני אתן עליו הסבר."
                }
            }

            return if (isEnglish) {
                "I couldn't find a matching exercise."
            } else {
                "לא מצאתי תרגיל מתאים לשאלה."
            }

        } catch (t: Throwable) {
            Log.e("KMI-EXERCISE-AI", "ExerciseAssistantEngine failed", t)

            return if (isEnglish) {
                "There was a problem processing the exercise request."
            } else {
                "אירעה תקלה בעיבוד השאלה."
            }
        }
    }
}