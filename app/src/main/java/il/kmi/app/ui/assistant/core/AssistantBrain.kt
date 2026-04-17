package il.kmi.app.ui.assistant.core

import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.app.ui.assistant.material.MaterialAssistantEngine
import il.kmi.app.ui.assistant.trainings.TrainingsAssistantEngine
import il.kmi.shared.domain.Belt

object AssistantBrain {

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return when (AssistantIntentDetector.detect(question)) {
            AssistantIntent.EXERCISE ->
                ExerciseAssistantEngine.answer(
                    question = question,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )

            AssistantIntent.MATERIAL ->
                MaterialAssistantEngine.answer(
                    question = question,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )

            AssistantIntent.TRAININGS ->
                TrainingsAssistantEngine.answer(
                    question = question,
                    isEnglish = isEnglish
                )

            AssistantIntent.UNKNOWN ->
                if (isEnglish) {
                    "I’m not sure what you are asking. Try asking about an exercise, K.M.I material, or training."
                } else {
                    "לא בטוח למה התכוונת. נסה לשאול על תרגיל, חומר ק.מ.י או אימון."
                }
        }
    }
}