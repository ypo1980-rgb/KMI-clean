package il.kmi.app.debug

import android.util.Log
import il.kmi.app.domain.Explanations
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

object ExplanationAudit {

    private const val TAG = "KMI_EXPLAIN_AUDIT"

    private data class ExerciseRow(
        val belt: Belt,
        val topic: String,
        val item: String,
        val source: String
    )

    fun runHebrewAudit() {
        val rows = collectAllExercises()
            .distinctBy {
                "${it.belt.id}|${normalizeAuditItem(it.item)}"
            }

        val missing = rows.filter { row ->
            !hasRealHebrewExplanation(
                belt = row.belt,
                topic = row.topic,
                rawItem = row.item
            )
        }

        Log.e(TAG, "==============================")
        Log.e(TAG, "בדיקת הסברים בעברית")
        Log.e(TAG, "סה״כ תרגילים שנבדקו: ${rows.size}")
        Log.e(TAG, "חסרים הסברים: ${missing.size}")
        Log.e(TAG, "==============================")

        missing
            .sortedWith(compareBy({ it.belt.id }, { it.topic }, { it.item }))
            .forEachIndexed { index, row ->
                Log.e(
                    TAG,
                    "${index + 1}. belt=${row.belt.id} | beltHeb=${row.belt.heb} | item=${row.item} | topic=${row.topic} | source=${row.source}"
                )
            }

        Log.e(TAG, "========== סוף בדיקה ==========")
    }

    private fun normalizeAuditItem(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

    private fun collectAllExercises(): List<ExerciseRow> {
        val result = mutableListOf<ExerciseRow>()

        val belts = listOf(
            Belt.YELLOW,
            Belt.ORANGE,
            Belt.GREEN,
            Belt.BLUE,
            Belt.BROWN,
            Belt.BLACK
        )

        // 1) סריקה מתוך ContentRepo הרגיל
        belts.forEach { belt ->
            val beltContent = ContentRepo.data[belt]
            beltContent?.topics.orEmpty().forEach { topic ->
                topic.items.forEach { item ->
                    result += ExerciseRow(
                        belt = belt,
                        topic = topic.title,
                        item = item,
                        source = "ContentRepo/direct"
                    )
                }

                topic.subTopics.forEach { subTopic ->
                    subTopic.items.forEach { item ->
                        result += ExerciseRow(
                            belt = belt,
                            topic = topic.title,
                            item = item,
                            source = "ContentRepo/subTopic"
                        )
                    }
                }
            }
        }

        // 2) סריקה מתוך HardSectionsCatalog — נושאים קשיחים
        belts.forEach { belt ->
            HardSectionsCatalog.supportedSubjectIds.forEach { subjectId ->
                val topicTitle =
                    HardSectionsCatalog.subjectDisplayTitle(subjectId) ?: subjectId

                HardSectionsCatalog.subjectItemsFor(subjectId, belt).forEach { item ->
                    result += ExerciseRow(
                        belt = belt,
                        topic = topicTitle,
                        item = item,
                        source = "HardSectionsCatalog"
                    )
                }
            }
        }

        return result
    }

    private fun hasRealHebrewExplanation(
        belt: Belt,
        topic: String,
        rawItem: String
    ): Boolean {
        val display = ExerciseTitleFormatter
            .displayName(rawItem)
            .ifBlank { rawItem }
            .trim()

        val cleaned = cleanItem(topic, rawItem)

        val candidates = listOf(
            rawItem,
            cleaned,
            display,
            display.substringBefore("(").trim()
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return candidates.any { candidate ->
            val explanation = Explanations.get(belt, candidate).trim()
            explanation.isRealExplanation()
        }
    }

    private fun String.isRealExplanation(): Boolean {
        val t = trim()

        if (t.isBlank()) return false

        // fallbackים בעברית
        if (t.startsWith("אין כרגע")) return false
        if (t.startsWith("הסבר מפורט על")) return false
        if (t.contains("לא נמצא הסבר")) return false

        return true
    }

    private fun cleanItem(topic: String, item: String): String {
        var s = item.trim()

        if (topic.isNotBlank() && s.startsWith("$topic::")) {
            s = s.removePrefix("$topic::").trim()
        }

        return s
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}