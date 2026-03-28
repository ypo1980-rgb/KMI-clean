package il.kmi.app.screens.SubTopics

import android.net.Uri
import il.kmi.shared.domain.Belt

object SubTopicsByTopicRoute {
    const val beltArg = "beltId"
    const val topicArg = "topic"

    const val route = "sub_topics_by_topic/{$beltArg}/{$topicArg}"

    fun build(
        belt: Belt,
        topic: String
    ): String {
        return "sub_topics_by_topic/${Uri.encode(belt.id)}/${Uri.encode(topic)}"
    }
}