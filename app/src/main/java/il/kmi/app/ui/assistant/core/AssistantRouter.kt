package il.kmi.app.ui.assistant.core

import il.kmi.app.ui.assistant.coach.CoachEngine
import il.kmi.app.ui.assistant.coach.CoachRequest
import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.app.ui.assistant.explain.ExplainEngine
import il.kmi.app.ui.assistant.explain.ExplainRequest
import il.kmi.app.ui.assistant.material.MaterialAssistantEngine
import il.kmi.app.ui.assistant.search.SearchEngine
import il.kmi.app.ui.assistant.search.SearchRequest
import il.kmi.app.ui.assistant.trainings.TrainingsAssistantEngine
import il.kmi.shared.domain.Belt

class AssistantRouter(
    private val explainEngine: ExplainEngine = ExplainEngine(),
    private val searchEngine: SearchEngine = SearchEngine(),
    private val coachEngine: CoachEngine = CoachEngine()
) {
    fun explain(query: String, belt: String?) =
        explainEngine.handle(
            ExplainRequest(
                query = query,
                belt = belt,
                topic = null,
                exerciseId = null
            )
        )

    fun search(query: String, belt: String?) =
        searchEngine.handle(
            SearchRequest(
                query = query,
                belt = belt
            )
        )

    fun coach(query: String, belt: String?, topic: String?, traineeLevel: String?) =
        coachEngine.handle(
            CoachRequest(
                query = query,
                belt = belt,
                topic = topic,
                traineeLevel = traineeLevel
            )
        )

    fun exercise(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return ExerciseAssistantEngine.answer(
            question = question,
            preferredBelt = preferredBelt,
            isEnglish = isEnglish
        )
    }

    fun material(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return MaterialAssistantEngine.answer(
            question = question,
            preferredBelt = preferredBelt,
            isEnglish = isEnglish
        )
    }

    fun trainings(
        question: String,
        isEnglish: Boolean
    ): String {
        return TrainingsAssistantEngine.answer(
            question = question,
            isEnglish = isEnglish
        )
    }
}