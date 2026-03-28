package il.kmi.shared.domain.catalog

import il.kmi.shared.domain.Belt

/**
 * API אחיד לקריאת הקטלוג.
 * מקור האמת: HardSectionsCatalog דרך CatalogRepoBuilder.
 *
 * שיפור ביצועים:
 * - בונים snapshot אחד lazy של כל הקטלוג
 * - כל הקריאות אחר כך עובדות מול cache בזיכרון
 */
object CatalogRepo {

    private data class CachedBeltContent(
        val topics: List<CatalogTopic>,
        val topicByTitle: Map<String, CatalogTopic>,
        val topicTitles: List<String>
    )

    private data class CachePayload(
        val byBelt: Map<Belt, CachedBeltContent>
    )

    private val cache: CachePayload by lazy {
        val byBelt = Belt.order
            .filter { it != Belt.WHITE }
            .associateWith { belt ->
                val topics = CatalogRepoBuilder.buildTopicsForBelt(belt)
                CachedBeltContent(
                    topics = topics,
                    topicByTitle = topics.associateBy { it.title },
                    topicTitles = topics.map { it.title }
                )
            }

        CachePayload(byBelt = byBelt)
    }

    fun listTopicTitles(belt: Belt): List<String> {
        return cache.byBelt[belt]?.topicTitles.orEmpty()
    }

    fun listSubTopicTitles(belt: Belt, topicTitle: String): List<String> {
        val topic = findTopic(belt, topicTitle) ?: return emptyList()
        return topic.subTopics.map { it.title }
    }

    fun listItems(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<String> {
        val topic = findTopic(belt, topicTitle) ?: return emptyList()

        if (subTopicTitle.isNullOrBlank()) {
            val top = topic.items
            val subs = topic.subTopics.flatMap { it.items }
            return (top + subs).distinct()
        }

        val subTopic = topic.subTopics.firstOrNull { it.title == subTopicTitle }
            ?: return emptyList()

        return subTopic.items.distinct()
    }

    fun hasTopic(belt: Belt, topicTitle: String): Boolean {
        return findTopic(belt, topicTitle) != null
    }

    fun findTopic(belt: Belt, topicTitle: String): CatalogTopic? {
        return cache.byBelt[belt]?.topicByTitle?.get(topicTitle)
    }

    fun warmUp() {
        cache
    }
}