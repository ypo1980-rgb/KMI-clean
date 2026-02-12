package il.kmi.app.catalog

import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.ContentRepo.ResolvedItem

/**
 * ספק HTML לתרגיל לפי exerciseId (= ContentRepo.makeItemKey).
 *
 * כרגע אין תלות ישירה ב-AssistantExerciseExplanationKnowledge כדי שלא יישבר קומפילציה.
 * אפשר "לחבר" Resolver מבחוץ (lambda) שמחזיר HTML לפי ResolvedItem.
 */
object ExerciseHtmlProvider {

    /**
     * Hook אופציונלי: תוכל להציב כאן פונקציה שמחזירה HTML לתרגיל.
     * לדוגמה:
     * ExerciseHtmlProvider.setResolver { r -> AssistantExerciseExplanationKnowledge.explainHtml(...)}.
     */
    @Volatile
    private var resolver: ((ResolvedItem) -> String?)? = null

    fun setResolver(r: ((ResolvedItem) -> String?)?) {
        resolver = r
    }

    /**
     * מקבל exerciseId שזה בעצם ContentRepo.makeItemKey(...)
     * ומנסה להחזיר HTML להסבר/ידע (אם חובר resolver). אחרת מחזיר null.
     */
    fun tryGetHtmlForExerciseId(exerciseId: String): String? {
        val resolved: ResolvedItem = ContentRepo.resolveItemKey(exerciseId) ?: return null
        return runCatching { resolver?.invoke(resolved) }.getOrNull()
    }
}
