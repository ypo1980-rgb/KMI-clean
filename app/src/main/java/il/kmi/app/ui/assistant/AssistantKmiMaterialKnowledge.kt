package il.kmi.app.ui.assistant

import android.util.Log
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.search.KmiSearch
import il.kmi.shared.search.SearchHit

/**
 * מנוע "חומר ק.מ.י" (KMI_MATERIAL)
 *
 * תפקיד:
 * - המשתמש מבקש רשימת תרגילים לפי נושא / חגורה / שם תרגיל
 * - מחזיר רשימת תוצאות קריאה + הנחיה להמשך ("תן הסבר ל- ...")
 *
 * הערה:
 * - המנוע לא מחזיר הסברים מלאים (זה שייך ל-AssistantExerciseExplanationKnowledge / מצב EXERCISE)
 *
 * בנוסף:
 * - הקובץ הזה מחזיק את מנוע החיפוש והפורמט לרשימות, כדי להקטין את AiAssistantDialog.
 */
object AssistantKmiMaterialKnowledge {

    private const val TAG = "KMI_MATERIAL"

    data class MaterialAnswer(
        val text: String,
        val hits: List<SearchHit> = emptyList()
    )

    // ─────────────────────────────────────────────────────────────
    // API לשימוש חיצוני (מהדיאלוג ומה-fallbackים)
    // ─────────────────────────────────────────────────────────────

    /**
     * חיפוש raw (אותו SearchHit שהמערכת כבר משתמשת בו).
     * זה מחליף את searchExercisesForQuestion שהיה ב-AiAssistantDialog.
     */
    fun searchHits(query: String, preferredBelt: Belt?): List<SearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        return try {
            val sharedRepo = ContentRepo.asSharedRepo()
            KmiSearch.search(
                repo = sharedRepo,
                query = q,
                belt = preferredBelt?.toShared()
            )
        } catch (t: Throwable) {
            Log.e(TAG, "KmiSearch failed", t)
            emptyList()
        }
    }

    /**
     * פורמט תוצאות לרשימת תרגילים קריאה.
     * זה מחליף את formatHitsAsExerciseList שהיה ב-AiAssistantDialog.
     */
    fun formatHitsAsExerciseList(
        hits: List<SearchHit>,
        maxItems: Int = 8
    ): String {
        if (hits.isEmpty()) return ""

        return hits.take(maxItems).joinToString("\n") { hit ->
            val appBelt = runCatching { Belt.valueOf(hit.belt.name) }
                .getOrElse { Belt.WHITE }

            val topicTitle = hit.topic
            val rawItem = hit.item ?: ""
            val displayName = ExerciseTitleFormatter
                .displayName(rawItem)
                .ifBlank { rawItem }
                .ifBlank { topicTitle }
                .trim()

            "• $displayName (${topicTitle} – חגורה ${appBelt.heb})"
        }
    }

    /**
     * תשובה מלאה למצב חומר ק.מ.י.
     */
    fun answer(question: String, preferredBelt: Belt? = null): MaterialAnswer? {
        val q = question.trim()
        if (q.isBlank()) return null

        // אם המשתמש בעצם ביקש "הסבר" - נכוון אותו למצב EXERCISE
        if (looksLikeExplainRequest(q)) {
            val hint = buildString {
                append("נשמע שביקשת הסבר לתרגיל.\n\n")
                append("כדי לקבל הסבר מדויק, עבור למצב \"מידע / הסבר על תרגיל\" ואז כתוב:\n")
                append("• \"תן הסבר על- <שם תרגיל>\"\n\n")
                append("אם תרצה, תכתוב כאן רק את שם התרגיל ואני אחפש אותו בחומר ק.מ.י.")
            }
            return MaterialAnswer(text = hint)
        }

        val hits = searchHits(q, preferredBelt)

        if (hits.isEmpty()) {
            val beltLine = preferredBelt?.let { " לחגורה ${it.heb}" } ?: ""
            val text =
                "לא מצאתי בחומר ק.מ.י תוצאות שמתאימות לבקשה שלך$beltLine.\n\n" +
                        "נסה ניסוח קצר יותר, למשל:\n" +
                        "• \"בעיטות\" / \"מרפקים\" / \"הגנות חיצוניות\"\n" +
                        "• \"רשימה של שחרורים מחביקות\"\n" +
                        "• \"תרגיל בעיטת מיאגרי\""
            return MaterialAnswer(text = text, hits = emptyList())
        }

        val beltLine = preferredBelt?.let { " לחגורה ${it.heb}" } ?: ""
        val listText = formatHitsAsExerciseList(hits, maxItems = 10)

        val text =
            "מצאתי בחומר ק.מ.י$beltLine תוצאות שקשורות לבקשה שלך:\n\n" +
                    listText +
                    "\n\nאם תרצה הסבר, כתוב:\n" +
                    "• \"תן הסבר ל- <שם תרגיל>\""

        return MaterialAnswer(text = text, hits = hits)
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────
    private fun looksLikeExplainRequest(text: String): Boolean {
        val low = text.trim().lowercase()

        val explainTriggers = listOf(
            "הסבר", "תסביר", "תסבירי", "תן הסבר", "תני הסבר",
            "איך עושים", "איך לבצע", "איך מבצעים", "שלב שלב", "צעד צעד",
            "דגשים", "טיפים", "פירוט"
        )

        return explainTriggers.any { it in low }
    }
}
