package il.kmi.shared.catalog

/**
 * קטלוג בזיכרון:
 * היום אנחנו יכולים למלא אותו ידנית/מתוך Android,
 * ומחר (עם מק) SwiftUI יקרא ממנו בלי שינוי API.
 */
object InMemoryCatalog {

    private val belts = mutableListOf<BeltDto>()

    // beltId -> topics
    private val topicsByBelt = linkedMapOf<String, MutableList<TopicDto>>()

    // beltId|topicId -> subTopics
    private val subTopicsByTopic = linkedMapOf<String, MutableList<SubTopicDto>>()

    // beltId|topicId|subTopicId? -> exercises
    private val exercisesByBucket = linkedMapOf<String, MutableList<ExerciseDto>>()

    // exerciseId -> content
    private val contentByExerciseId = linkedMapOf<String, ExerciseContentDto>()

    fun clear() {
        belts.clear()
        topicsByBelt.clear()
        subTopicsByTopic.clear()
        exercisesByBucket.clear()
        contentByExerciseId.clear()
    }

    fun setBelts(list: List<BeltDto>) {
        belts.clear()
        belts.addAll(list.sortedBy { it.order })
    }

    fun setTopics(beltId: String, list: List<TopicDto>) {
        topicsByBelt.getOrPut(beltId) { mutableListOf() }.apply {
            clear(); addAll(list)
        }
    }

    fun setSubTopics(beltId: String, topicId: String, list: List<SubTopicDto>) {
        subTopicsByTopic
            .getOrPut(keyTopic(beltId, topicId)) { mutableListOf() }
            .apply {
                clear()
                addAll(list)
            }
    }

    fun setExercises(
        beltId: String,
        topicId: String,
        subTopicId: String?,
        list: List<ExerciseDto>
    ) {
        exercisesByBucket
            .getOrPut(keyBucket(beltId, topicId, subTopicId)) { mutableListOf() }
            .apply {
                clear()
                addAll(list)
            }
    }

    fun setExerciseContent(content: ExerciseContentDto) {
        contentByExerciseId[content.id] = content
    }

    fun getBelts(): List<BeltDto> = belts.toList()

    fun getTopics(beltId: String): List<TopicDto> =
        topicsByBelt[beltId]?.toList().orEmpty()

    fun getSubTopics(beltId: String, topicId: String): List<SubTopicDto> =
        subTopicsByTopic[keyTopic(beltId, topicId)]?.toList().orEmpty()

    fun getExercises(beltId: String, topicId: String, subTopicId: String?): List<ExerciseDto> =
        exercisesByBucket[keyBucket(beltId, topicId, subTopicId)]?.toList().orEmpty()

    fun getExerciseContent(exerciseId: String): ExerciseContentDto? =
        contentByExerciseId[exerciseId]

    // ---------- keys ----------
    private fun keyTopic(beltId: String, topicId: String) = "$beltId|$topicId"
    private fun keyBucket(beltId: String, topicId: String, subTopicId: String?) =
        if (subTopicId.isNullOrBlank()) "$beltId|$topicId|_"
        else "$beltId|$topicId|$subTopicId"
}
