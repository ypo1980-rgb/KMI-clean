package il.kmi.shared.questions.model.data

import il.kmi.shared.questions.model.BeltRef
import il.kmi.shared.questions.model.QuestionItem
import il.kmi.shared.questions.model.TopicBucket

/**
 * Source ניטרלי לפלטפורמה (KMP).
 * ה-app יכול לממש אותו על בסיס ContentRepo / JSON / Firestore / Assets וכו'.
 */
interface QuestionsContentSource {

    suspend fun listBelts(): List<BeltRef>

    /**
     * מחזיר דליים מוכנים ל-UI: Topic + ספירה + (אופציונלי) SubTopics עם ספירה.
     * אם אין תתי-נושאים, פשוט תחזיר subTopics ריק.
     */
    suspend fun listTopicsForBelt(
        beltId: String
    ): List<TopicBucket>

    /**
     * מביא תרגילים עבור topic (+ subTopic אופציונלי).
     */
    suspend fun listItems(
        beltId: String,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<QuestionItem>
}
