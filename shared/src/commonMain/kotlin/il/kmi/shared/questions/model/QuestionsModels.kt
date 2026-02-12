package il.kmi.shared.questions.model

/**
 * מודלים "Shared" לשכבת Questions.
 * המטרה: לייצר API יציב שמאפשר להביא תרגילים לפי חגורה/נושא/תת-נושא,
 * בלי שה-UI (ב-app) יהיה תלוי בפרטים פנימיים של מקור הנתונים.
 */

data class BeltRef(
    val id: String,
    val heb: String
)

data class TopicRef(
    val title: String
)

data class SubTopicRef(
    val title: String
)

data class QuestionItem(
    /** מזהה יציב של התרגיל. יכול להיות גם "key" פנימי של ContentRepo. */
    val id: String,
    /** שם לתצוגה */
    val title: String,
    /** אופציונלי: תיאור/שורה שנייה */
    val subtitle: String? = null,
    /** אופציונלי: מידע נוסף למסך פרטים */
    val body: String? = null
)

/**
 * תוצאות מוכנות ל-UI למסך "לפי נושא" / "לפי חגורה".
 */
data class TopicBucket(
    val topic: TopicRef,
    val count: Int,
    val subTopics: List<SubTopicBucket> = emptyList()
)

data class SubTopicBucket(
    val subTopic: SubTopicRef,
    val count: Int
)
