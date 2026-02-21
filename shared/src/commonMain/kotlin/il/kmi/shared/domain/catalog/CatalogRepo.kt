package il.kmi.shared.domain.catalog

import il.kmi.shared.domain.Belt

/**
 * API אחיד לקריאת הקטלוג (Topic/SubTopic/Items) מתוך CatalogData.
 * KMP safe (commonMain).
 */
object CatalogRepo {

    fun listTopicTitles(belt: Belt): List<String> {
        val bc = CatalogData.data[belt] ?: return emptyList()
        return bc.topics.map { it.title }
    }

    fun listSubTopicTitles(belt: Belt, topicTitle: String): List<String> {
        val t = findTopic(belt, topicTitle) ?: return emptyList()
        return t.subTopics.map { it.title }
    }

    /**
     * מחזיר את כל הפריטים של נושא.
     * אם subTopicTitle == null -> כולל גם items של topic וגם כל items של כל תתי הנושא
     * אם subTopicTitle != null -> רק תת נושא ספציפי
     */
    fun listItems(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<String> {
        val t = findTopic(belt, topicTitle) ?: return emptyList()

        if (subTopicTitle.isNullOrBlank()) {
            val top = t.items
            val subs = t.subTopics.flatMap { it.items }
            return (top + subs).distinct()
        }

        val st = t.subTopics.firstOrNull { it.title == subTopicTitle } ?: return emptyList()
        return st.items.distinct()
    }

    fun hasTopic(belt: Belt, topicTitle: String): Boolean =
        findTopic(belt, topicTitle) != null

    fun findTopic(belt: Belt, topicTitle: String): CatalogData.Topic? {
        val bc = CatalogData.data[belt] ?: return null
        return bc.topics.firstOrNull { it.title == topicTitle }
    }
}
