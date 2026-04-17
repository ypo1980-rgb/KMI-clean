package il.kmi.app.ui.assistant.exercise

import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt
import il.kmi.shared.search.SearchHit

/**
 * Fallback להסבר "הכי טוב" מתוך תוצאות חיפוש (SearchHit),
 * כאשר אין לנו match ייעודי דרך AssistantExerciseExplanationKnowledge.
 *
 * נשאר כ-wrapper דק בלבד כדי למנוע כפילות לוגיקה.
 */
object AssistantExerciseSearchFallback {

    fun buildBestHitExplanation(
        hits: List<SearchHit>,
        preferredBelt: Belt?
    ): String? {
        return ExerciseSearchService.buildBestHitExplanation(
            hits = hits,
            preferredBelt = preferredBelt,
            isEnglish = false
        )
    }
}