package il.kmi.shared.domain

/**
 * Engine קטן (commonMain) שמחזיר נושאים/תתי־נושאים/ספירות
 * מתוך ContentRepo (שהוא מקור האמת של התוכן).
 */
object TopicsEngine {

    data class TopicDetails(
        val itemCount: Int,
        val subTitles: List<String>
    )

    fun topicTitlesFor(belt: Belt): List<String> {
        val topics = ContentRepo.data[belt]?.topics.orEmpty()
        return topics.map { it.title.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun subTopicTitlesFor(belt: Belt, topicTitle: String): List<String> {
        return ContentRepo.getSubTopicsFor(belt, topicTitle)
            .map { it.title.trim() }
            .filter { it.isNotBlank() && it != topicTitle.trim() }
            .distinct()
    }

    fun topicDetailsFor(belt: Belt, topicTitle: String): TopicDetails {
        val subs = subTopicTitlesFor(belt, topicTitle)
        val count = ContentRepo.getAllItemsFor(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        ).size

        return TopicDetails(
            itemCount = count,
            subTitles = subs
        )
    }
}
