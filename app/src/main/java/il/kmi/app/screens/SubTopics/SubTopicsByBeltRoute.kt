package il.kmi.app.screens.SubTopics

import android.net.Uri
import il.kmi.shared.domain.Belt

object SubTopicsByBeltRoute {
    const val beltArg = "beltId"
    const val topicArg = "topic"

    const val route = "sub_topics_by_belt/{$beltArg}/{$topicArg}"

    fun build(
        belt: Belt,
        topic: String
    ): String {
        return "sub_topics_by_belt/${Uri.encode(belt.id)}/${Uri.encode(topic)}"
    }
}