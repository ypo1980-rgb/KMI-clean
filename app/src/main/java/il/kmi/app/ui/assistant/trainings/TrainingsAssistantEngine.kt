package il.kmi.app.ui.assistant.trainings

import android.content.SharedPreferences
import android.util.Log

object TrainingsAssistantEngine {

    // זיכרון פנימי למנוע האימונים
    private var memory: AssistantMemory? = null

    fun init(sp: SharedPreferences) {
        memory = AssistantMemory(sp)
    }

    fun answer(
        question: String,
        isEnglish: Boolean
    ): String {
        return try {
            val mem = memory

            if (mem == null) {
                if (isEnglish) {
                    "Training assistant is not initialized yet."
                } else {
                    "מנוע האימונים עדיין לא אותחל."
                }
            } else {
                val answer = AssistantTrainingKnowledge.generateAnswer(
                    question = question,
                    memory = mem
                )

                AssistantTrainingKnowledge.updateMemoryFromAnswer(
                    question = question,
                    answer = answer,
                    memory = mem
                )

                answer
            }
        } catch (t: Throwable) {
            Log.e("KMI-TRAININGS-AI", "TrainingsAssistantEngine failed", t)

            if (isEnglish) {
                "There was a temporary issue retrieving training information."
            } else {
                "יש תקלה רגעית בשליפת מידע על אימונים."
            }
        }
    }
}