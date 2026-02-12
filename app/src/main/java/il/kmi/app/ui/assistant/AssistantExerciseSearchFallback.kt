package il.kmi.app.ui.assistant

import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.search.SearchHit

/**
 * Fallback להסבר "הכי טוב" מתוך תוצאות חיפוש (SearchHit),
 * כאשר אין לנו match ייעודי דרך AssistantExerciseExplanationKnowledge.
 *
 * הקובץ הזה מוציא את לוגיקת ה-fallback מה-UI (AiAssistantDialog).
 */
object AssistantExerciseSearchFallback {

    fun buildBestHitExplanation(
        hits: List<SearchHit>,
        preferredBelt: Belt?
    ): String? {
        val first = hits.firstOrNull() ?: return null

        val appBelt = runCatching { Belt.valueOf(first.belt.name) }
            .getOrElse { preferredBelt ?: Belt.WHITE }

        val topic = first.topic
        val rawItem = first.item ?: return null
        val explanation = findExplanationForHit(appBelt, rawItem, topic)
        val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

        return "ההסבר לתרגיל \"$display\":\n\n$explanation"
    }

    private fun findExplanationForHit(
        belt: Belt,
        rawItem: String,
        topic: String
    ): String {
        val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

        fun String.clean(): String = this
            .replace('–', '-')
            .replace('־', '-')
            .replace("  ", " ")
            .trim()

        val candidates = buildList {
            add(rawItem)
            add(display)
            add(display.clean())
            add(display.substringBefore("(").trim().clean())
        }.distinct()

        for (candidate in candidates) {
            val got = Explanations.get(belt, candidate).trim()
            if (got.isNotBlank() &&
                !got.startsWith("הסבר מפורט על") &&
                !got.startsWith("אין כרגע")
            ) {
                return if ("::" in got) got.substringAfter("::").trim() else got
            }
        }

        return "אין כרגע הסבר מפורט לתרגיל הזה במאגר."
    }
}
