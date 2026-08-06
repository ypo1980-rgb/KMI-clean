package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.content.ExerciseIdentityRegistry

/** Auto-generated search index for Explanations.kt titles. */
object ExplanationSearchIndex {

    data class Match(
        val belt: Belt,
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

    /*
     * משתמשים באותו נרמול גלובלי של ה־Registry.
     *
     * כך צואר/צוואר וכל הצורות עם תחיליות
     * מקבלות אותה משמעות בכל שכבות החיפוש.
     */
    /**
     * נרמול מקומי לצורכי חיפוש בלבד.
     *
     * מאחד את כל צורות הכתיב של צואר/צוואר,
     * בלי לשנות שמות תרגילים, IDs או נתוני התקדמות.
     */
    private fun normalize(
        value: String
    ): String {
        return value
            .lowercase()
            .trim()
            .replace("\u200f", "")
            .replace("\u200e", "")
            .replace("\u00a0", " ")

            /*
             * פועל גם כאשר מחוברת תחילית:
             * הצואר -> הצוואר
             * בצואר -> בצוואר
             * לצואר -> לצוואר
             */
            .replace(
                "צואר",
                "צוואר"
            )

            .replace("–", " ")
            .replace("—", " ")
            .replace("־", " ")
            .replace("-", " ")
            .replace("/", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace(":", " ")
            .replace(";", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

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

    /**
     * מחזיר את כל התרגילים המתאימים לשאילתה.
     *
     * כל התוצאות מגיעות ישירות מ־ExerciseIdentityRegistry
     * ומ־Explanations, ללא רשימות קשיחות של משפחות תרגילים.
     */
    fun findMatches(
        query: String,
        preferredBelt: Belt? = null,
        minScore: Int = 70,
        maxItems: Int = 20
    ): List<Match> {
        val variants = queryAliases(query)

        if (variants.isEmpty()) {
            return emptyList()
        }

        /*
         * אותו תרגיל עשוי להופיע מספר פעמים בגלל כינויים.
         * מאחדים לפי חגורה וכותרת קנונית ושומרים את הציון
         * הגבוה ביותר שהתקבל מאחד הכינויים.
         */
        val scoredEntries =
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
                .groupBy { (entry, _) ->
                    entry.belt to normalize(
                        entry.canonicalTitle
                    )
                }
                .values
                .mapNotNull { sameExerciseEntries ->
                    sameExerciseEntries.maxByOrNull { (_, score) ->
                        score
                    }
                }

        return scoredEntries
            .mapNotNull { (entry, score) ->
                val explanation =
                    Explanations.get(
                        belt = entry.belt,
                        item = entry.canonicalTitle
                    )
                        .trim()

                val isFallback =
                    explanation.isBlank() ||
                            explanation.startsWith(
                                "הסבר מפורט על:"
                            ) ||
                            explanation.startsWith(
                                "אין כרגע"
                            )

                if (isFallback) {
                    null
                } else {
                    Match(
                        belt = entry.belt,
                        title = entry.canonicalTitle,
                        explanation = explanation,
                        score = score
                    )
                }
            }
            .sortedWith(
                compareByDescending<Match> {
                    it.score
                }
                    .thenBy {
                        it.title.length
                    }
                    .thenBy {
                        it.title
                    }
            )
            .take(
                maxItems.coerceAtLeast(1)
            )
    }

    /**
     * נשמרת תאימות לכל המקומות הקיימים באפליקציה
     * שזקוקים להתאמה הטובה ביותר בלבד.
     */
    fun findBest(
        query: String,
        preferredBelt: Belt? = null,
        minScore: Int = 70
    ): Match? {
        return findMatches(
            query = query,
            preferredBelt = preferredBelt,
            minScore = minScore,
            maxItems = 1
        )
            .firstOrNull()
    }
}