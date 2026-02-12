package il.kmi.shared.exam

object ExamFacade {

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
}
