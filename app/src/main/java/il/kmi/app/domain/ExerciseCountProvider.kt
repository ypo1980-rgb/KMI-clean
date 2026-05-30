package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.content.HardSectionsCatalog


data class ExerciseCountStats(
    val subTopicCount: Int,
    val exerciseCount: Int
)

object ExerciseCountProvider {

    private fun hardSectionIdForTopic(topicTitle: String): String? {
        val clean = normalize(topicTitle)

        return when {
            clean == "עבודת קרקע" ||
                    clean == "topic_ground_prep" -> {
                "topic_ground_prep"
            }

            clean == "עמידת מוצא" ||
                    clean == "topic_ready_stance" -> {
                "topic_ready_stance"
            }

            clean == "קוואלר" ||
                    clean == "topic_kavaler" ||
                    clean == "kavaler" -> {
                "topic_kavaler"
            }

            clean == "בלימות וגלגולים" ||
                    clean == "גלגולים ובלימות" ||
                    clean == "rolls_breakfalls" ||
                    clean == "topic_breakfalls_rolls" -> {
                "rolls_breakfalls"
            }

            clean == "בעיטות" ||
                    clean == "topic_kicks" ||
                    clean == "kicks" -> {
                "kicks"
            }

            else -> null
        }
    }

    private fun hardSectionExerciseCountForTopic(topicTitle: String): Int {
        val cleanTopic = normalize(topicTitle)

        val candidates = listOfNotNull(
            hardSectionIdForTopic(cleanTopic),
            cleanTopic
        )
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()

        fun countDeep(
            s: HardSectionsCatalog.Section
        ): Int {
            val ownItemsCount = s.beltGroups
                .flatMap { group -> group.items }
                .map { normalizeItem(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .size

            val childItemsCount = s.subSections.sumOf { child ->
                countDeep(child)
            }

            return ownItemsCount + childItemsCount
        }

        for (candidate in candidates) {
            val sectionsForSubject = runCatching {
                HardSectionsCatalog.sectionsForSubject(candidate)
            }.getOrNull()

            val countFromSubject = sectionsForSubject
                .orEmpty()
                .sumOf { section -> countDeep(section) }

            if (countFromSubject > 0) {
                return countFromSubject
            }

            val sectionById = runCatching {
                HardSectionsCatalog.findAnySectionById(candidate)
            }.getOrNull()

            val countFromSection = sectionById?.let { section ->
                countDeep(section)
            } ?: 0

            if (countFromSection > 0) {
                return countFromSection
            }
        }

        return 0
    }

    private fun normalize(value: String): String =
        value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun normalizeItem(value: String): String {
        val display = ExerciseTitleFormatter
            .displayName(value)
            .ifBlank { value }

        return normalize(display)
    }

    private fun SharedContentRepo.SubTopic.totalExercisesCountDeep(): Int {
        val directItems = items
            .map { normalizeItem(it) }
            .filter { it.isNotBlank() }
            .distinct()

        val nestedItemsCount = subTopics.sumOf { child ->
            child.totalExercisesCountDeep()
        }

        return directItems.size + nestedItemsCount
    }

    fun topicStats(
        belt: Belt,
        topicTitle: String
    ): ExerciseCountStats {
        val cleanTopic = normalize(topicTitle)
        if (cleanTopic.isBlank()) {
            return ExerciseCountStats(
                subTopicCount = 0,
                exerciseCount = 0
            )
        }

        val hardCount = hardSectionExerciseCountForTopic(cleanTopic)
        if (hardCount > 0) {
            return ExerciseCountStats(
                subTopicCount = 0,
                exerciseCount = hardCount
            )
        }

        val subTopics = runCatching {
            SharedContentRepo.getSubTopicsFor(
                belt = belt,
                topicTitle = cleanTopic
            )
        }.getOrDefault(emptyList())
            .filter { sub ->
                normalize(sub.title).isNotBlank() &&
                        normalize(sub.title) != cleanTopic
            }

        val directItems = runCatching {
            SharedContentRepo.getAllItemsFor(
                belt = belt,
                topicTitle = cleanTopic,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())
            .map { normalizeItem(it) }
            .filter { it.isNotBlank() }
            .distinct()

        val deepSubTopicItemsCount = subTopics.sumOf { sub ->
            sub.totalExercisesCountDeep()
        }

        val exerciseCount = when {
            deepSubTopicItemsCount > 0 -> deepSubTopicItemsCount
            else -> directItems.size
        }

        return ExerciseCountStats(
            subTopicCount = subTopics.size,
            exerciseCount = exerciseCount
        )
    }

    fun subTopicStats(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String
    ): ExerciseCountStats {
        val cleanTopic = normalize(topicTitle)
        val cleanSubTopic = normalize(subTopicTitle)

        if (cleanTopic.isBlank() || cleanSubTopic.isBlank()) {
            return ExerciseCountStats(
                subTopicCount = 0,
                exerciseCount = 0
            )
        }

        val subTopic = runCatching {
            SharedContentRepo.getSubTopicsFor(
                belt = belt,
                topicTitle = cleanTopic
            )
                .firstOrNull { sub ->
                    normalize(sub.title) == cleanSubTopic
                }
        }.getOrNull()

        if (subTopic != null) {
            return ExerciseCountStats(
                subTopicCount = subTopic.subTopics.size,
                exerciseCount = subTopic.totalExercisesCountDeep()
            )
        }

        val items = runCatching {
            SharedContentRepo.getAllItemsFor(
                belt = belt,
                topicTitle = cleanTopic,
                subTopicTitle = cleanSubTopic
            )
        }.getOrDefault(emptyList())
            .map { normalizeItem(it) }
            .filter { it.isNotBlank() }
            .distinct()

        return ExerciseCountStats(
            subTopicCount = 0,
            exerciseCount = items.size
        )
    }

    fun countText(
        stats: ExerciseCountStats,
        isEnglish: Boolean,
        showZeroSubTopics: Boolean = false
    ): String {
        val shouldShowSubTopics = stats.subTopicCount >= 2 || showZeroSubTopics

        return if (isEnglish) {
            if (shouldShowSubTopics) {
                "${stats.subTopicCount} sub-topics • ${stats.exerciseCount} exercises"
            } else {
                "${stats.exerciseCount} exercises"
            }
        } else {
            if (shouldShowSubTopics) {
                "${stats.subTopicCount} תתי נושאים • ${stats.exerciseCount} תרגילים"
            } else {
                "${stats.exerciseCount} תרגילים"
            }
        }
    }
}