package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubTopicRegistry as SharedSubTopicRegistry
import il.kmi.shared.domain.content.HardSectionsCatalog

/**
 * עטיפה (אופציונלית) בצד האפליקציה מעל ה-SubTopicRegistry שב-shared.
 * אחרי המעבר ל-Belt של shared, אין צורך בהמרות בכלל.
 */
object AppSubTopicRegistry {

    private fun String.normTopic(): String = this
        .trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")

    private fun extraDefenseSubTopicsFor(belt: Belt, topicTitle: String): List<String> {
        val t = topicTitle.normTopic()

        if (belt != Belt.BLACK) return emptyList()
        if (t != "הגנות") return emptyList()

        return listOf(
            "הגנות עם רובה נגד דקירות סכין",
            "הגנות נגד מספר תוקפים"
        )
    }

    fun getSubTopicsFor(belt: Belt, topicTitle: String): List<String> {
        val base = SharedSubTopicRegistry.getSubTopicsFor(
            belt = belt,
            topicTitle = topicTitle
        )

        val extras = extraDefenseSubTopicsFor(belt, topicTitle)

        return (base + extras)
            .map { it.normTopic() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun getItemsFor(belt: Belt, topicTitle: String, subTopicTitle: String): List<String> {
        val normalizedTopic = topicTitle.normTopic()
        val normalizedSubTopic = subTopicTitle.normTopic()

        if (normalizedTopic == "הגנות" && belt == Belt.BLACK) {
            if (normalizedSubTopic == "הגנות עם רובה נגד דקירות סכין") {
                return HardSectionsCatalog.subjectSubSectionItemsFor(
                    subjectId = "knife_defense",
                    subSectionId = "knife_defense_rifle_against_knife_stabs",
                    belt = belt
                )
            }

            if (normalizedSubTopic == "הגנות נגד מספר תוקפים") {
                return HardSectionsCatalog.subjectItemsFor(
                    subjectId = "multiple_attackers_defense",
                    belt = belt
                )
            }
        }

        return SharedSubTopicRegistry.getItemsFor(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle
        )
    }

    fun allForBelt(belt: Belt): Map<String, List<String>> {
        val base = SharedSubTopicRegistry.allForBelt(belt).toMutableMap()

        val defenseKey = base.keys.firstOrNull { it.normTopic() == "הגנות" } ?: "הגנות"
        val existing = base[defenseKey].orEmpty()
        val extras = extraDefenseSubTopicsFor(belt, defenseKey)

        base[defenseKey] = (existing + extras)
            .map { it.normTopic() }
            .filter { it.isNotBlank() }
            .distinct()

        return base
    }
}