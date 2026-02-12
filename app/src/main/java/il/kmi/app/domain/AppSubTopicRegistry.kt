package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubTopicRegistry as SharedSubTopicRegistry

/**
 * עטיפה (אופציונלית) בצד האפליקציה מעל ה-SubTopicRegistry שב-shared.
 * אחרי המעבר ל-Belt של shared, אין צורך בהמרות בכלל.
 */
object AppSubTopicRegistry {

    fun getSubTopicsFor(belt: Belt, topicTitle: String): List<String> =
        SharedSubTopicRegistry.getSubTopicsFor(
            belt = belt,
            topicTitle = topicTitle
        )

    fun getItemsFor(belt: Belt, topicTitle: String, subTopicTitle: String): List<String> =
        SharedSubTopicRegistry.getItemsFor(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle
        )

    fun allForBelt(belt: Belt): Map<String, List<String>> =
        SharedSubTopicRegistry.allForBelt(belt)
}
