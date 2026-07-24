package il.kmi.app.ui.assistant.trainings

import android.content.Context
import android.content.SharedPreferences

data class TrainingsAssistantResponse(
    val text: String,
    val cards: List<TrainingAssistantCard>
)

object TrainingsAssistantEngine {

    private var memory: AssistantMemory? = null

    /*
     * שומרים Application Context בלבד כדי לא להחזיק
     * Activity או Composable מעבר למחזור החיים שלהם.
     */
    private var applicationContext: Context? = null

    fun init(
        context: Context,
        sp: SharedPreferences
    ) {
        applicationContext =
            context.applicationContext

        memory = AssistantMemory(sp)
    }

    /*
     * נשמר לתאימות עם קריאות קיימות שמצפות למחרוזת.
     */
    fun answer(
        question: String,
        isEnglish: Boolean
    ): String {
        return answerDetailed(
            question = question,
            isEnglish = isEnglish
        ).text
    }

    fun answerDetailed(
        question: String,
        isEnglish: Boolean
    ): TrainingsAssistantResponse {
        return try {
            val context = applicationContext
            val mem = memory

            if (
                context == null ||
                mem == null
            ) {
                TrainingsAssistantResponse(
                    text = if (isEnglish) {
                        "Training assistant is not initialized yet."
                    } else {
                        "מנוע האימונים עדיין לא אותחל."
                    },
                    cards = emptyList()
                )
            } else {
                var cards:
                        List<TrainingAssistantCard> =
                    emptyList()

                val answer =
                    AssistantTrainingKnowledge
                        .generateAnswer(
                            context = context,
                            question = question,
                            memory = mem,
                            isEnglish = isEnglish,
                            onCardsReady = {
                                    generatedCards ->
                                cards = generatedCards
                            }
                        )

                AssistantTrainingKnowledge
                    .updateMemoryFromAnswer(
                        question = question,
                        answer = answer,
                        memory = mem
                    )

                TrainingsAssistantResponse(
                    text = answer,
                    cards = cards
                )
            }
        } catch (_: Throwable) {
            TrainingsAssistantResponse(
                text = if (isEnglish) {
                    "There was a temporary issue retrieving training information."
                } else {
                    "יש תקלה רגעית בשליפת מידע על אימונים."
                },
                cards = emptyList()
            )
        }
    }
}