package il.kmi.shared.catalog

/**
 * זה ה-API ש-SwiftUI יקרא ממנו.
 * הכל DTO + String — הכי נוח ויציב ל-iOS.
 */
object KmiCatalogFacade {

    fun hasSubTopics(beltId: String, topicId: String): Boolean =
        InMemoryCatalog.getSubTopics(beltId, topicId).isNotEmpty()

    fun countExercises(
        beltId: String,
        topicId: String,
        subTopicId: String? = null
    ): Int =
        InMemoryCatalog.getExercises(beltId, topicId, subTopicId).size

    fun listBelts(): List<BeltDto> =
        InMemoryCatalog.getBelts()

    fun listTopics(beltId: String): List<TopicDto> =
        InMemoryCatalog.getTopics(beltId)

    fun listSubTopics(beltId: String, topicId: String): List<SubTopicDto> =
        InMemoryCatalog.getSubTopics(beltId, topicId)

    fun listExercises(
        beltId: String,
        topicId: String,
        subTopicId: String? = null
    ): List<ExerciseDto> =
        InMemoryCatalog.getExercises(beltId, topicId, subTopicId)

    fun getExerciseContent(exerciseId: String): ExerciseContentDto? =
        InMemoryCatalog.getExerciseContent(exerciseId)

    // ✅ API יציב ל-iOS / WebView
    fun getExerciseHtml(exerciseId: String): String {
        val c = InMemoryCatalog.getExerciseContent(exerciseId)
        if (c != null) return c.contents

        return """
            <html>
              <body dir="rtl" style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial; line-height:1.5;">
                <h2>אין תוכן עדיין</h2>
                <p>אין הסבר שמור לתרגיל הזה כרגע.</p>
              </body>
            </html>
        """.trimIndent()
    }

    /**
     * 🔍 חיפוש תרגילים בלי תלות ב-KmiSearch/Repo adapters.
     * עובד 100% ב-commonMain ולכן מושלם ל-iOS.
     *
     * beltId:
     *  - null => מחפש בכל החגורות
     *  - "yellow"/"green"/... => מחפש רק בחגורה הזו
     */
    fun searchExercises(
        query: String,
        beltId: String? = null
    ): List<ExerciseDto> {

        val q = query.normalizeForSearch()
        if (q.isBlank()) return emptyList()

        val beltsToScan: List<BeltDto> =
            if (beltId.isNullOrBlank()) listBelts()
            else listBelts().filter { it.id == beltId }

        val out = LinkedHashMap<String, ExerciseDto>() // unique + preserve order

        for (belt in beltsToScan) {
            val topics = listTopics(beltId = belt.id)
            for (topic in topics) {

                // תרגילים ברמת נושא
                listExercises(beltId = belt.id, topicId = topic.id, subTopicId = null)
                    .forEach { ex ->
                        if (ex.title.normalizeForSearch().contains(q)) {
                            if (!out.containsKey(ex.id)) out[ex.id] = ex
                        }
                    }

                // תתי-נושאים
                val subs = listSubTopics(beltId = belt.id, topicId = topic.id)
                for (st in subs) {
                    listExercises(beltId = belt.id, topicId = topic.id, subTopicId = st.id)
                        .forEach { ex ->
                            if (ex.title.normalizeForSearch().contains(q)) {
                                if (!out.containsKey(ex.id)) out[ex.id] = ex
                            }
                        }
                }
            }
        }

        return out.values.toList()
    }

    private fun String.normalizeForSearch(): String =
        this
            .replace("\u200F", "")   // RLM
            .replace("\u200E", "")   // LRM
            .replace("\u00A0", " ")  // NBSP
            .replace('–', '-')
            .replace('—', '-')
            .replace('־', '-')
            .trim()
            .lowercase()
}
