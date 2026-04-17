package il.kmi.app.ui.assistant.material

import android.util.Log
import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt

object MaterialAssistantEngine {

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return try {
            val hits = ExerciseSearchService.searchExercisesForQuestion(
                question = question,
                beltEnum = preferredBelt
            )

            val best = ExerciseSearchService.buildBestHitExplanation(
                hits = hits,
                preferredBelt = preferredBelt,
                isEnglish = isEnglish
            )

            best ?: run {
                val list = ExerciseSearchService.formatHitsAsExerciseList(
                    hits = hits,
                    maxItems = 8,
                    isEnglish = isEnglish
                )

                if (list.isNotBlank()) {
                    if (isEnglish) {
                        "Here is relevant K.M.I material I found:\n$list"
                    } else {
                        "הנה חומר ק.מ.י רלוונטי שמצאתי:\n$list"
                    }
                } else {
                    if (isEnglish) {
                        "I couldn't find relevant K.M.I material. Try writing a topic, sub-topic, exercise name, or belt."
                    } else {
                        "לא מצאתי חומר ק.מ.י רלוונטי. נסה לכתוב נושא, תת־נושא, שם תרגיל או חגורה."
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("KMI-MATERIAL-AI", "MaterialAssistantEngine failed", t)
            if (isEnglish) {
                "There is a temporary issue processing the K.M.I material request."
            } else {
                "יש תקלה רגעית בעיבוד בקשת חומר ק.מ.י."
            }
        }
    }
}