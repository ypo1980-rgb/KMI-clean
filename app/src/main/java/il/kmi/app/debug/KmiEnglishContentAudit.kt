package il.kmi.app.debug

import android.util.Log
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.content.English.ExerciseExplanationsEn
import il.kmi.shared.domain.content.English.ExerciseTitlesEnAliases
import il.kmi.shared.domain.content.English.ExerciseTitlesEnItems
import il.kmi.shared.domain.content.English.ExerciseTitlesEnTopics

object KmiEnglishContentAudit {

    private const val TAG = "KMI_EN_AUDIT"

    private data class AuditExercise(
        val belt: Belt,
        val topic: String,
        val subTopic: String?,
        val item: String
    )

    fun run() {
        val allExercises = collectAllExercises()

        var missingTitleCount = 0
        var hebrewTitleCount = 0
        var missingTopicTitleCount = 0
        var hebrewTopicTitleCount = 0
        var missingExplanationCount = 0
        var fallbackExplanationCount = 0

        Log.e(TAG, "========== English content audit started ==========")
        Log.e(TAG, "Total exercises from ContentRepo: ${allExercises.size}")

        allExercises.forEach { ex ->
            val englishTitle = resolveEnglishTitle(ex.item)

            // ✅ בדיקת שמות תרגילים בפועל:
            // אם תרגיל נשאר בעברית או לא מתורגם — נדפיס שגיאה.
            if (englishTitle.isNullOrBlank() || englishTitle == ex.item) {
                missingTitleCount++

                Log.e(
                    TAG,
                    buildString {
                        append("MISSING_EN_EXERCISE_TITLE | ")
                        append("belt=${ex.belt.id} | ")
                        append("topic=${ex.topic} | ")
                        if (!ex.subTopic.isNullOrBlank()) append("subTopic=${ex.subTopic} | ")
                        append("itemHeb=${ex.item} | ")
                        append("resolved=${englishTitle ?: "null"}")
                    }
                )
            } else if (containsHebrew(englishTitle)) {
                hebrewTitleCount++

                Log.e(
                    TAG,
                    buildString {
                        append("HEBREW_IN_EN_EXERCISE_TITLE | ")
                        append("belt=${ex.belt.id} | ")
                        append("topic=${ex.topic} | ")
                        if (!ex.subTopic.isNullOrBlank()) append("subTopic=${ex.subTopic} | ")
                        append("itemHeb=${ex.item} | ")
                        append("resolved=$englishTitle")
                    }
                )
            } else {
                Log.d(
                    TAG,
                    buildString {
                        append("OK_EN_EXERCISE_TITLE | ")
                        append("belt=${ex.belt.id} | ")
                        append("itemHeb=${ex.item} | ")
                        append("itemEn=$englishTitle")
                    }
                )
            }

            val englishTopicTitle = resolveEnglishTopicTitle(ex.topic)

            if (englishTopicTitle.isNullOrBlank() || englishTopicTitle == ex.topic) {
                missingTopicTitleCount++

                Log.e(
                    TAG,
                    buildString {
                        append("MISSING_EN_TOPIC_TITLE | ")
                        append("belt=${ex.belt.id} | ")
                        append("topic=${ex.topic}")
                    }
                )
            } else if (containsHebrew(englishTopicTitle)) {
                hebrewTopicTitleCount++

                Log.e(
                    TAG,
                    buildString {
                        append("HEBREW_IN_EN_TOPIC_TITLE | ")
                        append("belt=${ex.belt.id} | ")
                        append("topic=${ex.topic} | ")
                        append("resolved=$englishTopicTitle")
                    }
                )
            }

            if (!ex.subTopic.isNullOrBlank()) {
                val englishSubTopicTitle = resolveEnglishTopicTitle(ex.subTopic)

                if (englishSubTopicTitle.isNullOrBlank() || englishSubTopicTitle == ex.subTopic) {
                    missingTopicTitleCount++

                    Log.e(
                        TAG,
                        buildString {
                            append("MISSING_EN_SUB_TOPIC_TITLE | ")
                            append("belt=${ex.belt.id} | ")
                            append("topic=${ex.topic} | ")
                            append("subTopic=${ex.subTopic}")
                        }
                    )
                } else if (containsHebrew(englishSubTopicTitle)) {
                    hebrewTopicTitleCount++

                    Log.e(
                        TAG,
                        buildString {
                            append("HEBREW_IN_EN_SUB_TOPIC_TITLE | ")
                            append("belt=${ex.belt.id} | ")
                            append("topic=${ex.topic} | ")
                            append("subTopic=${ex.subTopic} | ")
                            append("resolved=$englishSubTopicTitle")
                        }
                    )
                }
            }

            val explanation = ExerciseExplanationsEn.get(ex.belt, ex.item).trim()

            when {
                explanation.isBlank() -> {
                    missingExplanationCount++

                    Log.e(
                        TAG,
                        buildString {
                            append("MISSING_EN_EXPLANATION | ")
                            append("belt=${ex.belt.id} | ")
                            append("topic=${ex.topic} | ")
                            if (!ex.subTopic.isNullOrBlank()) append("subTopic=${ex.subTopic} | ")
                            append("item=${ex.item}")
                        }
                    )
                }

                isFallbackExplanation(explanation) -> {
                    fallbackExplanationCount++

                    Log.e(
                        TAG,
                        buildString {
                            append("FALLBACK_EN_EXPLANATION | ")
                            append("belt=${ex.belt.id} | ")
                            append("topic=${ex.topic} | ")
                            if (!ex.subTopic.isNullOrBlank()) append("subTopic=${ex.subTopic} | ")
                            append("item=${ex.item} | ")
                            append("got=$explanation")
                        }
                    )
                }
            }
        }

        Log.e(TAG, "========== English content audit finished ==========")
        Log.e(TAG, "Missing English exercise titles: $missingTitleCount")
        Log.e(TAG, "Hebrew inside English exercise titles: $hebrewTitleCount")
        Log.e(TAG, "Missing English topic/sub-topic titles: $missingTopicTitleCount")
        Log.e(TAG, "Hebrew inside English topic/sub-topic titles: $hebrewTopicTitleCount")
        Log.e(TAG, "Missing English explanations: $missingExplanationCount")
        Log.e(TAG, "Fallback English explanations: $fallbackExplanationCount")
        Log.e(
            TAG,
            "Total English problems: ${
                missingTitleCount +
                        hebrewTitleCount +
                        missingTopicTitleCount +
                        hebrewTopicTitleCount +
                        missingExplanationCount +
                        fallbackExplanationCount
            }"
        )
    }

    private fun collectAllExercises(): List<AuditExercise> {
        val out = mutableListOf<AuditExercise>()

        ContentRepo.data.forEach { (belt, beltContent) ->
            beltContent.topics.forEach { topic ->

                topic.items.forEach { item ->
                    out += AuditExercise(
                        belt = belt,
                        topic = topic.title,
                        subTopic = null,
                        item = item
                    )
                }

                topic.subTopics.forEach { subTopic ->
                    subTopic.items.forEach { item ->
                        out += AuditExercise(
                            belt = belt,
                            topic = topic.title,
                            subTopic = subTopic.title,
                            item = item
                        )
                    }
                }
            }
        }

        return out.distinctBy {
            "${it.belt.id}|${normKey(it.topic)}|${normKey(it.subTopic.orEmpty())}|${normKey(it.item)}"
        }
    }

    private fun resolveEnglishTitle(item: String): String? {
        val candidates = titleCandidates(item)

        candidates.forEach { candidate ->
            ExerciseTitlesEnItems.map[candidate]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            ExerciseTitlesEnAliases.map[candidate]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        val normalizedItemsMap = ExerciseTitlesEnItems.map.entries
            .associateBy { normKey(it.key) }

        val normalizedAliasesMap = ExerciseTitlesEnAliases.map.entries
            .associateBy { normKey(it.key) }

        candidates.forEach { candidate ->
            normalizedItemsMap[normKey(candidate)]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            normalizedAliasesMap[normKey(candidate)]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        return null
    }

    private fun resolveEnglishTopicTitle(title: String): String? {
        val candidates = titleCandidates(title)

        candidates.forEach { candidate ->
            ExerciseTitlesEnTopics.map[candidate]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        val normalizedTopicsMap = ExerciseTitlesEnTopics.map.entries
            .associateBy { normKey(it.key) }

        candidates.forEach { candidate ->
            normalizedTopicsMap[normKey(candidate)]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        return null
    }

    private fun titleCandidates(raw: String): List<String> {
        val t = raw.trim()

        return linkedSetOf(
            t,
            t.replace('–', '-'),
            t.replace('—', '-'),
            t.replace('־', '-'),
            t.replace("-", "–"),
            t.replace("צואר", "צוואר"),
            t.replace("צוואר", "צואר"),
            t.replace("גרוגרת", "גורגרת"),
            t.replace("גורגרת", "גרוגרת")
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun normKey(raw: String): String {
        return raw
            .trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace('–', '-')
            .replace('—', '-')
            .replace('־', '-')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsHebrew(text: String): Boolean {
        return text.any { it in '\u0590'..'\u05FF' }
    }

    private fun isFallbackExplanation(text: String): Boolean {
        val t = text.trim()

        return t.startsWith("Detailed explanation for:") ||
                t.startsWith("There is currently no explanation", ignoreCase = true) ||
                t.equals("No explanation", ignoreCase = true)
    }
}