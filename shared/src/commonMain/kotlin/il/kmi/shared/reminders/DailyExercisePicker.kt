package il.kmi.shared.reminders

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo

class DailyExercisePicker {

    fun pickNextExerciseForUser(
        registeredBelt: Belt,
        lastItemKey: String? = null
    ): DailyExerciseItem? {
        val targetBelts = targetBeltsForDailyExercise(
            registeredBelt = registeredBelt
        )

        if (targetBelts.isEmpty()) return null

        val allCandidates = targetBelts
            .flatMap { belt -> buildCandidates(belt) }
            .filter { it.item.isNotBlank() }

        if (allCandidates.isEmpty()) return null

        val lastKey = lastItemKey.orEmpty().trim()
        val lastBeltName = lastKey.substringBefore("|", missingDelimiterValue = "")
            .trim()
            .uppercase()

        val filteredByDifferentBelt = if (
            registeredBelt == Belt.BLACK &&
            lastBeltName.isNotBlank() &&
            allCandidates.map { it.belt.name }.distinct().size > 1
        ) {
            allCandidates.filter { it.belt.name != lastBeltName }
        } else {
            allCandidates
        }

        val filteredByDifferentItem =
            if (lastKey.isNotBlank() && filteredByDifferentBelt.size > 1) {
                filteredByDifferentBelt.filterNot { candidateKey(it) == lastKey }
            } else {
                filteredByDifferentBelt
            }

        val pool = when {
            filteredByDifferentItem.isNotEmpty() -> filteredByDifferentItem
            filteredByDifferentBelt.isNotEmpty() -> filteredByDifferentBelt
            else -> allCandidates
        }

        return pool.random()
    }

    fun candidateKey(item: DailyExerciseItem): String {
        return "${item.belt.name}|${item.topic}|${item.item}"
    }

    private fun targetBeltsForDailyExercise(
        registeredBelt: Belt
    ): List<Belt> {
        return when (registeredBelt) {
            // ✅ כל מי שמוגדר כחגורה שחורה, כולל דאן 1 עד דאן 10,
            // יקבל בכל יום תרגיל מתוך ירוקה / כחולה / חומה / שחורה.
            Belt.BLACK -> listOf(
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            )

            else -> nextBeltAfter(registeredBelt)
                ?.let { listOf(it) }
                .orEmpty()
        }
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