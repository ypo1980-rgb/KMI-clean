package il.kmi.app.ui.assistant.search

import android.util.Log
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.Explanations
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.search.KmiSearch
import il.kmi.shared.search.SearchHit

object ExerciseSearchService {

    fun searchExercisesForQuestion(
        question: String,
        beltEnum: Belt?
    ): List<SearchHit> {
        return try {
            val sharedRepo = ContentRepo.asSharedRepo()

            val queries = buildSearchQueries(question)

            queries
                .flatMap { query ->
                    runCatching {
                        KmiSearch.search(
                            repo = sharedRepo,
                            query = query,
                            belt = beltEnum?.toShared()
                        )
                    }.getOrElse {
                        Log.e("KMI-AI", "KmiSearch failed for query=$query", it)
                        emptyList()
                    }
                }
                .distinctBy { hit ->
                    listOf(
                        hit.belt.name,
                        hit.topic,
                        hit.item ?: ""
                    ).joinToString("|")
                }
        } catch (t: Throwable) {
            Log.e("KMI-AI", "KmiSearch failed", t)
            emptyList()
        }
    }

    private fun normalizeSearchQuestion(text: String): String {

        var t = text
            .trim()
            .replace("?", "")
            .replace("؟", "")
            .replace("\"", "")
            .replace("“", "")
            .replace("”", "")
            .replace("  ", " ")
            .trim()

        // תיקוני שגיאות נפוצות בדיבור/הקלדה
        val corrections = mapOf(
            "בעית" to "בעיטת",
            "בעיטה מגל" to "בעיטת מגל",
            "מגל" to "בעיטת מגל",
            "הגנה מבעיטה" to "הגנה נגד בעיטה",
            "שחרור חניקה" to "שחרור מחניקה",
            "חניקה" to "מחניקה",
            "אגרוף לפנים" to "אגרוף לפנים"
        )

        corrections.forEach { (wrong, correct) ->
            if (t.contains(wrong)) {
                t = t.replace(wrong, correct)
            }
        }

        return t
    }

    private fun buildSearchQueries(question: String): List<String> {
        val q = normalizeSearchQuestion(question)

        val prefixesToStrip = listOf(
            "תן הסבר ל",
            "תן הסבר על",
            "הסבר ל",
            "הסבר על",
            "איך עושים",
            "איך לבצע",
            "איך מבצעים",
            "מה זה",
            "חפש לי",
            "תחפש לי",
            "תראה לי",
            "explain",
            "what is",
            "how to do",
            "how do i do",
            "show me",
            "find"
        )

        val variants = mutableListOf<String>()

        variants += q

        prefixesToStrip.forEach { prefix ->
            if (q.startsWith(prefix, ignoreCase = true)) {
                variants += q.removePrefix(prefix).trim()
            }
        }

        if (" על " in q) {
            variants += q.substringAfter(" על ").trim()
        }

        if (" ל" in q && q.startsWith("תן הסבר")) {
            variants += q.substringAfter("ל").trim()
        }

        if (" for " in q.lowercase()) {
            variants += q.substringAfter(" for ", missingDelimiterValue = "").trim()
        }

        return variants
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.length }
    }

    fun formatHitsAsExerciseList(
        hits: List<SearchHit>,
        maxItems: Int = 6,
        isEnglish: Boolean = false
    ): String {
        if (hits.isEmpty()) return ""

        return hits.take(maxItems).joinToString("\n") { hit ->
            val appBelt = runCatching { Belt.valueOf(hit.belt.name) }
                .getOrElse { Belt.WHITE }

            val topicTitle = hit.topic
            val rawItem = hit.item ?: ""
            val displayName = displayNameForHit(
                rawItem = rawItem,
                fallbackTopic = topicTitle
            )

            if (isEnglish) {
                "• $displayName ($topicTitle – ${appBelt.name.lowercase()} belt)"
            } else {
                "• $displayName (${topicTitle} – חגורה ${appBelt.heb})"
            }
        }
    }

    fun buildBestHitExplanation(
        hits: List<SearchHit>,
        preferredBelt: Belt?,
        isEnglish: Boolean = false
    ): String? {

        for (hit in hits.take(6)) {

            val appBelt = runCatching { Belt.valueOf(hit.belt.name) }
                .getOrElse { preferredBelt ?: Belt.WHITE }

            val rawItem = hit.item ?: continue

            val explanation = findExplanationForHit(appBelt, rawItem)

            if (!explanation.startsWith("אין כרגע")) {

                val display = displayNameForHit(
                    rawItem = rawItem,
                    fallbackTopic = hit.topic
                )

                return if (isEnglish) {
                    "Explanation for \"$display\":\n\n$explanation"
                } else {
                    "ההסבר לתרגיל \"$display\":\n\n$explanation"
                }
            }
        }

        return null
    }

    private fun displayNameForHit(
        rawItem: String,
        fallbackTopic: String
    ): String {
        return ExerciseTitleFormatter
            .displayName(rawItem)
            .ifBlank { rawItem }
            .ifBlank { fallbackTopic }
            .trim()
    }

    private fun findExplanationForHit(
        belt: Belt,
        rawItem: String
    ): String {
        val display = displayNameForHit(
            rawItem = rawItem,
            fallbackTopic = rawItem
        )

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

            // תוספת חשובה
            add(rawItem.substringBefore("(").trim().clean())
        }.distinct()

        for (candidate in candidates) {
            val got = Explanations.get(belt, candidate).trim()

            if (
                got.isNotBlank() &&
                !got.startsWith("הסבר מפורט על") &&
                !got.startsWith("אין כרגע")
            ) {
                return if ("::" in got) {
                    got.substringAfter("::").trim()
                } else {
                    got
                }
            }
        }

        return "אין כרגע הסבר מפורט לתרגיל הזה במאגר."
    }
}