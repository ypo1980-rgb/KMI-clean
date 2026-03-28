package il.kmi.shared.reminders

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import kotlin.random.Random

class DailyExercisePicker {

    fun pickNextExerciseForUser(
        registeredBelt: Belt,
        lastItemKey: String? = null
    ): DailyExerciseItem? {
        val nextBelt = nextBeltAfter(registeredBelt) ?: return null

        val allCandidates = buildCandidates(nextBelt)
        if (allCandidates.isEmpty()) return null

        val filteredCandidates =
            if (!lastItemKey.isNullOrBlank() && allCandidates.size > 1) {
                allCandidates.filterNot { candidateKey(it) == lastItemKey }
            } else {
                allCandidates
            }

        val pool = if (filteredCandidates.isNotEmpty()) {
            filteredCandidates
        } else {
            allCandidates
        }

        return pool.random()
    }

    fun candidateKey(item: DailyExerciseItem): String {
        return "${item.belt.name}|${item.topic}|${item.item}"
    }

    private fun buildCandidates(belt: Belt): List<DailyExerciseItem> {
        val beltContent = ContentRepo.data[belt] ?: return emptyList()

        return beltContent.topics.flatMap { topic ->
            val topicTitle = topic.title.trim()

            if (isGeneralTopic(topicTitle)) {
                emptyList()
            } else {
                val directItems = topic.items
                    .mapNotNull { rawItem ->
                        rawItem.toDailyExerciseItemOrNull(
                            belt = belt,
                            topic = topicTitle
                        )
                    }

                val subTopicItems = topic.subTopics.flatMap { subTopic ->
                    subTopic.items.mapNotNull { rawItem ->
                        rawItem.toDailyExerciseItemOrNull(
                            belt = belt,
                            topic = topicTitle
                        )
                    }
                }

                directItems + subTopicItems
            }
        }
    }

    private fun String.toDailyExerciseItemOrNull(
        belt: Belt,
        topic: String
    ): DailyExerciseItem? {
        val cleanItem = trim()
        if (cleanItem.isBlank()) return null

        return DailyExerciseItem(
            belt = belt,
            topic = topic,
            item = cleanItem
        )
    }

    private fun isGeneralTopic(topic: String): Boolean {
        val clean = topic.trim()
        return clean.equals("כללי", ignoreCase = true) ||
                clean.equals("general", ignoreCase = true)
    }

    private fun nextBeltAfter(belt: Belt): Belt? {
        return when (belt) {
            Belt.WHITE -> Belt.YELLOW
            Belt.YELLOW -> Belt.ORANGE
            Belt.ORANGE -> Belt.GREEN
            Belt.GREEN -> Belt.BLUE
            Belt.BLUE -> Belt.BROWN
            Belt.BROWN -> Belt.BLACK
            Belt.BLACK -> null
        }
    }
}