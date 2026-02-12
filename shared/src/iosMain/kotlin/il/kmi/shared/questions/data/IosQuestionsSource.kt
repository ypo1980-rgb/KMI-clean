package il.kmi.shared.questions.data

import il.kmi.shared.questions.model.BeltRef
import il.kmi.shared.questions.model.QuestionItem
import il.kmi.shared.questions.model.TopicBucket
import il.kmi.shared.questions.model.data.QuestionsContentSource
import il.kmi.shared.questions.model.data.SharedRegistryQuestionsSource

/**
 * iOS implementation.
 * Reuses the shared registry-based source from commonMain.
 */
class IosQuestionsSource : QuestionsContentSource {

    private val delegate: QuestionsContentSource = SharedRegistryQuestionsSource

    override suspend fun listBelts(): List<BeltRef> =
        delegate.listBelts()

    override suspend fun listTopicsForBelt(beltId: String): List<TopicBucket> =
        delegate.listTopicsForBelt(beltId)

    override suspend fun listItems(
        beltId: String,
        topicTitle: String,
        subTopicTitle: String?
    ): List<QuestionItem> =
        delegate.listItems(beltId, topicTitle, subTopicTitle)
}
