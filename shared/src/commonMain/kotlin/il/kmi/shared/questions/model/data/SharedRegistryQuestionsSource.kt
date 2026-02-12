package il.kmi.shared.questions.model.data

import il.kmi.shared.questions.model.BeltRef
import il.kmi.shared.questions.model.QuestionItem
import il.kmi.shared.questions.model.SubTopicBucket
import il.kmi.shared.questions.model.SubTopicRef
import il.kmi.shared.questions.model.TopicBucket
import il.kmi.shared.questions.model.TopicRef

object SharedRegistryQuestionsSource : QuestionsContentSource {

    private val belts: LinkedHashMap<String, BeltRef> = LinkedHashMap()

    // beltId -> topicTitle -> subTopicTitle? -> items
    private val itemsTree:
            LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String?, MutableList<QuestionItem>>>> =
        LinkedHashMap()

    fun reset() {
        belts.clear()
        itemsTree.clear()
    }

    fun registerBelt(id: String, heb: String) {
        belts[id] = BeltRef(id = id, heb = heb)
        if (!itemsTree.containsKey(id)) {
            itemsTree[id] = LinkedHashMap()
        }
    }

    fun registerTopic(beltId: String, topicTitle: String) {
        val beltMap = itemsTree[beltId] ?: LinkedHashMap<String, LinkedHashMap<String?, MutableList<QuestionItem>>>().also {
            itemsTree[beltId] = it
        }

        if (!beltMap.containsKey(topicTitle)) {
            beltMap[topicTitle] = LinkedHashMap()
        }
    }

    fun registerSubTopic(beltId: String, topicTitle: String, subTopicTitle: String) {
        val beltMap = itemsTree[beltId] ?: LinkedHashMap<String, LinkedHashMap<String?, MutableList<QuestionItem>>>().also {
            itemsTree[beltId] = it
        }

        val topicMap = beltMap[topicTitle] ?: LinkedHashMap<String?, MutableList<QuestionItem>>().also {
            beltMap[topicTitle] = it
        }

        if (!topicMap.containsKey(subTopicTitle)) {
            topicMap[subTopicTitle] = mutableListOf()
        }
    }

    fun registerItem(
        beltId: String,
        topicTitle: String,
        subTopicTitle: String? = null,
        item: QuestionItem
    ) {
        val beltMap = itemsTree[beltId] ?: LinkedHashMap<String, LinkedHashMap<String?, MutableList<QuestionItem>>>().also {
            itemsTree[beltId] = it
        }

        val topicMap = beltMap[topicTitle] ?: LinkedHashMap<String?, MutableList<QuestionItem>>().also {
            beltMap[topicTitle] = it
        }

        val list = topicMap[subTopicTitle] ?: mutableListOf<QuestionItem>().also {
            topicMap[subTopicTitle] = it
        }

        list.add(item)
    }

    override suspend fun listBelts(): List<BeltRef> {
        return belts.values.toList()
    }

    override suspend fun listTopicsForBelt(beltId: String): List<TopicBucket> {
        val beltMap = itemsTree[beltId] ?: return emptyList()

        return beltMap.entries.map { (topicTitle, subMap) ->
            val total = subMap.values.sumOf { it.size }

            val subBuckets = subMap
                .filterKeys { it != null }
                .entries
                .map { (subTitle, list) ->
                    SubTopicBucket(
                        subTopic = SubTopicRef(subTitle!!),
                        count = list.size
                    )
                }
                .sortedByDescending { it.count }

            TopicBucket(
                topic = TopicRef(topicTitle),
                count = total,
                subTopics = subBuckets
            )
        }.sortedByDescending { it.count }
    }

    override suspend fun listItems(
        beltId: String,
        topicTitle: String,
        subTopicTitle: String?
    ): List<QuestionItem> {
        val beltMap = itemsTree[beltId] ?: return emptyList()
        val topicMap = beltMap[topicTitle] ?: return emptyList()
        val list = topicMap[subTopicTitle] ?: return emptyList()
        return list.toList()
    }
}
