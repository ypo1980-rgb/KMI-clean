package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.content.ExerciseIdentityRegistry

/** Auto-generated search index for Explanations.kt titles. */
object ExplanationSearchIndex {

    data class Match(
        val belt: il.kmi.shared.domain.Belt,
        val title: String,
        val explanation: String,
        val score: Int
    )

    private data class Entry(
        val belt: Belt,
        val canonicalTitle: String,
        val searchableTitle: String
    )

    /*
     * האינדקס נבנה אוטומטית מכל התרגילים הידועים
     * ב־ExerciseIdentityRegistry.
     *
     * עבור כל תרגיל נכללים:
     * 1. השם העברי הרשמי.
     * 2. כל הכינויים והכתיבים החלופיים שלו.
     *
     * אין צורך לעדכן ידנית את הקובץ הזה כאשר נוסף
     * תרגיל חדש או כינוי חדש ל־Registry.
     */
    private val entries: List<Entry> by lazy {
        ExerciseIdentityRegistry.knownExercises
            .flatMap { identity ->
                linkedSetOf<String>().apply {
                    add(identity.hebrewTitle)
                    addAll(identity.aliases)
                }
                    .mapNotNull { searchableTitle ->
                        searchableTitle
                            .trim()
                            .takeIf { it.isNotBlank() }
                            ?.let { cleanTitle ->
                                Entry(
                                    belt = identity.belt,
                                    canonicalTitle =
                                        identity.hebrewTitle.trim(),
                                    searchableTitle =
                                        cleanTitle
                                )
                            }
                    }
            }
            .distinctBy { entry ->
                Triple(
                    entry.belt,
                    normalize(entry.canonicalTitle),
                    normalize(entry.searchableTitle)
                )
            }
    }

    private fun normalize(value: String): String =
        value
            .lowercase()
            .trim()
            .replace("\u200f", "")
            .replace("\u200e", "")
            .replace("\u00a0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace("/", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun cleanQuestion(value: String): String {
        var t = normalize(value)

        val prefixes = listOf(
            "תן הסבר על", "תן הסבר ל", "תני הסבר על", "תני הסבר ל",
            "תסביר לי את", "תסביר לי", "תסביר את", "תסביר",
            "תסבירי לי את", "תסבירי לי", "תסבירי את", "תסבירי",
            "איך עושים את", "איך עושים", "איך מבצעים את", "איך מבצעים",
            "מה זה", "מה ההסבר של", "הסבר על", "הסבר ל",
            "explain the", "explain", "how to do the", "how to do", "what is"
        )

        prefixes.forEach { p ->
            if (t.startsWith(p)) {
                t = t.removePrefix(p).trim()
            }
        }

        return t
            .removePrefix("את ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun queryAliases(
        query: String
    ): Set<String> {
        val cleanQuery =
            cleanQuestion(query)

        if (cleanQuery.isBlank()) {
            return emptySet()
        }

        /*
         * אין לייצר כינויים לפי מילה שמופיעה בתוך
         * שם מלא של תרגיל.
         *
         * שמות חלופיים מגיעים מ־ExerciseIdentityRegistry,
         * ולכן השאילתה המקורית מספיקה כאן.
         */
        return setOf(cleanQuery)
    }

    private fun scoreTitle(query: String, title: String): Int {
        val q = normalize(query)
        val t = normalize(title)
        if (q.isBlank() || t.isBlank()) return 0

        var score = 0

        if (q == t) score += 1000
        if (t.startsWith(q)) score += 260
        if (t.contains(q)) score += 220
        if (q.contains(t)) score += 180

        val qWords = q.split(" ").filter { it.length >= 2 }
        val tWords = t.split(" ").filter { it.length >= 2 }.toSet()

        val hits = qWords.count { it in tWords || t.contains(it) }
        score += hits * 45

        if (qWords.isNotEmpty() && hits == qWords.size) score += 120

        score -= kotlin.math.abs(t.length - q.length).coerceAtMost(80)

        return score
    }

    fun findBest(
        query: String,
        preferredBelt: Belt? = null,
        minScore: Int = 70
    ): Match? {
        val variants = queryAliases(query)
        if (variants.isEmpty()) return null

        val best =
            entries
                .asSequence()
                .map { entry ->
                    val baseScore =
                        variants.maxOf { variant ->
                            scoreTitle(
                                query = variant,
                                title = entry.searchableTitle
                            )
                        }

                    val beltBoost =
                        if (
                            preferredBelt != null &&
                            preferredBelt == entry.belt
                        ) {
                            35
                        } else {
                            0
                        }

                    entry to (baseScore + beltBoost)
                }
                .filter { (_, score) ->
                    score >= minScore
                }
                .sortedWith(
                    compareByDescending<Pair<Entry, Int>> {
                        it.second
                    }
                        .thenBy {
                            it.first.canonicalTitle.length
                        }
                )
                .firstOrNull()
                ?: return null

        val explanation =
            Explanations.get(
                belt = best.first.belt,
                item = best.first.canonicalTitle
            )
                .trim()
        if (explanation.isBlank() || explanation.startsWith("הסבר מפורט על:")) return null

        return Match(
            belt = best.first.belt,
            title = best.first.canonicalTitle,
            explanation = explanation,
            score = best.second
        )
    }
}