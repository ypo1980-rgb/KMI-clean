package il.kmi.shared.exam

object ExamFacade {

    data class ExamItem(
        val topicTitle: String,
        val rawItem: String
    )

    fun interface TopicTitlesProvider {
        fun topicTitlesFor(beltId: String): List<String>
    }

    fun interface ItemsProvider {
        fun itemsFor(beltId: String, topicTitle: String): List<String>
    }

    /**
     * מחזיר את כל פריטי המבחן לחגורה (כל הנושאים).
     * אין רפלקציה / אין JVM — מתאים ל-iOS.
     */
    fun buildExamItems(
        beltId: String,
        topicTitlesProvider: TopicTitlesProvider,
        itemsProvider: ItemsProvider
    ): List<String> {
        val id = beltId.trim()
        if (id.isBlank()) return emptyList()

        val topics = topicTitlesProvider.topicTitlesFor(id)
        if (topics.isEmpty()) return emptyList()

        val out = topics.flatMap { t -> itemsProvider.itemsFor(id, t) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return out
    }

    /**
     * מחזיר תרגילים יחד עם הנושא המקורי שלהם.
     *
     * הפונקציה הישנה buildExamItems נשארת ללא שינוי
     * כדי לשמור על תאימות Android / iOS.
     */
    fun buildExamItemsWithTopics(
        beltId: String,
        topicTitlesProvider: TopicTitlesProvider,
        itemsProvider: ItemsProvider
    ): List<ExamItem> {
        val id = beltId.trim()
        if (id.isBlank()) return emptyList()

        val topics = topicTitlesProvider.topicTitlesFor(id)
        if (topics.isEmpty()) return emptyList()

        return topics.flatMap { topicTitle ->
            itemsProvider.itemsFor(id, topicTitle)
                .map { rawItem ->
                    ExamItem(
                        topicTitle = topicTitle.trim(),
                        rawItem = rawItem.trim()
                    )
                }
        }
            .filter { item ->
                item.topicTitle.isNotBlank() &&
                        item.rawItem.isNotBlank()
            }
            .distinctBy { item ->
                item.topicTitle to item.rawItem
            }
    }
}
