package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.TopicsEngine
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
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
                    clean == "topic_kicks" -> {
                "topic_kicks"
            }

            clean == "הגנות נגד בעיטות" ||
                    clean == "kicks" ||
                    clean == "kicks_hard" -> {
                "kicks_hard"
            }

            else -> null
        }
    }

    private fun hardSectionExerciseKeysForTopic(
        belt: Belt,
        topicTitle: String
    ): Set<String> {
        val cleanTopic =
            normalize(topicTitle)

        val candidates =
            listOfNotNull(
                hardSectionIdForTopic(cleanTopic),
                cleanTopic
            )
                .map { candidate ->
                    normalize(candidate)
                }
                .filter { candidate ->
                    candidate.isNotBlank()
                }
                .distinct()

        fun collectDeep(
            section: HardSectionsCatalog.Section,
            destination: MutableSet<String>
        ) {
            section.beltGroups
                .asSequence()
                .filter { group ->
                    group.belt == belt
                }
                .flatMap { group ->
                    group.items.asSequence()
                }
                .map { item ->
                    exerciseIdentityKey(
                        belt = belt,
                        topicTitle = cleanTopic,
                        rawItem = item
                    )
                }
                .filter { identityKey ->
                    identityKey.isNotBlank()
                }
                .forEach { identityKey ->
                    destination += identityKey
                }

            section.subSections.forEach { child ->
                collectDeep(
                    section = child,
                    destination = destination
                )
            }
        }

        for (candidate in candidates) {
            val subjectItems =
                linkedSetOf<String>()

            runCatching {
                HardSectionsCatalog
                    .sectionsForSubject(candidate)
            }
                .getOrNull()
                .orEmpty()
                .forEach { section ->
                    collectDeep(
                        section = section,
                        destination = subjectItems
                    )
                }

            if (subjectItems.isNotEmpty()) {
                return subjectItems
            }

            val sectionItems =
                linkedSetOf<String>()

            runCatching {
                HardSectionsCatalog
                    .findAnySectionById(candidate)
            }
                .getOrNull()
                ?.let { section ->
                    collectDeep(
                        section = section,
                        destination = sectionItems
                    )
                }

            if (sectionItems.isNotEmpty()) {
                return sectionItems
            }
        }

        return emptySet()
    }

    private fun hardSectionExerciseCountForTopic(
        belt: Belt,
        topicTitle: String
    ): Int {
        return hardSectionExerciseKeysForTopic(
            belt = belt,
            topicTitle = topicTitle
        ).size
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

    private fun exerciseIdentityKey(
        belt: Belt,
        topicTitle: String,
        rawItem: String
    ): String {
        val cleanItem = normalizeItem(rawItem)

        if (cleanItem.isBlank()) {
            return ""
        }

        return ExerciseIdentityRegistry
            .resolve(
                belt = belt,
                hebrewTitle = cleanItem,
                topicKey = normalize(topicTitle)
            )
            .id
            .trim()
            .ifBlank { cleanItem }
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

        val hardCount =
            hardSectionExerciseCountForTopic(
                belt = belt,
                topicTitle = cleanTopic
            )

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

        val directExerciseKeys = runCatching {
            SharedContentRepo.getAllItemsFor(
                belt = belt,
                topicTitle = cleanTopic,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())
            .asSequence()
            .map { item ->
                exerciseIdentityKey(
                    belt = belt,
                    topicTitle = cleanTopic,
                    rawItem = item
                )
            }
            .filter { identityKey ->
                identityKey.isNotBlank()
            }
            .toSet()

        val subTopicExerciseKeys =
            linkedSetOf<String>()

        subTopics.forEach { subTopic ->
            collectSubTopicExerciseKeys(
                belt = belt,
                topicTitle = cleanTopic,
                subTopic = subTopic,
                destination = subTopicExerciseKeys
            )
        }

        val exerciseCount = when {
            subTopicExerciseKeys.isNotEmpty() ->
                subTopicExerciseKeys.size

            else ->
                directExerciseKeys.size
        }

        return ExerciseCountStats(
            subTopicCount = subTopics.size,
            exerciseCount = exerciseCount
        )
    }

    fun beltStats(
        belt: Belt
    ): ExerciseCountStats {
        val exerciseKeys =
            linkedSetOf<String>()

        val subTopicTitles =
            linkedSetOf<String>()

        TopicsEngine
            .topicTitlesFor(belt)
            .asSequence()
            .map { topicTitle ->
                normalize(topicTitle)
            }
            .filter { topicTitle ->
                topicTitle.isNotBlank()
            }
            .distinct()
            .forEach { topicTitle ->
                val hardItems =
                    hardSectionExerciseKeysForTopic(
                        belt = belt,
                        topicTitle = topicTitle
                    )

                if (hardItems.isNotEmpty()) {
                    exerciseKeys.addAll(hardItems)
                } else {
                    runCatching {
                        SharedContentRepo.getAllItemsFor(
                            belt = belt,
                            topicTitle = topicTitle,
                            subTopicTitle = null
                        )
                    }
                        .getOrDefault(emptyList())
                        .asSequence()
                        .map { item ->
                            exerciseIdentityKey(
                                belt = belt,
                                topicTitle = topicTitle,
                                rawItem = item
                            )
                        }
                        .filter { identityKey ->
                            identityKey.isNotBlank()
                        }
                        .forEach { identityKey ->
                            exerciseKeys += identityKey
                        }

                    runCatching {
                        SharedContentRepo.getSubTopicsFor(
                            belt = belt,
                            topicTitle = topicTitle
                        )
                    }
                        .getOrDefault(emptyList())
                        .forEach { subTopic ->
                            val cleanSubTopicTitle =
                                normalize(
                                    subTopic.title
                                )

                            if (
                                cleanSubTopicTitle.isNotBlank() &&
                                cleanSubTopicTitle != topicTitle
                            ) {
                                subTopicTitles +=
                                    cleanSubTopicTitle
                            }

                            collectSubTopicExerciseKeys(
                                belt = belt,
                                topicTitle = topicTitle,
                                subTopic = subTopic,
                                destination = exerciseKeys
                            )
                        }
                }
            }

        val knownExerciseCount =
            ExerciseIdentityRegistry
                .allKnown()
                .asSequence()
                .filter { exercise ->
                    exercise.belt == belt
                }
                .map { exercise ->
                    exercise.id.trim()
                }
                .filter { exerciseId ->
                    exerciseId.isNotBlank()
                }
                .distinct()
                .count()

        return ExerciseCountStats(
            subTopicCount = subTopicTitles.size,
            exerciseCount = knownExerciseCount
                .takeIf { it > 0 }
                ?: exerciseKeys.size
        )
    }

    private fun collectSubTopicExerciseKeys(
        belt: Belt,
        topicTitle: String,
        subTopic: SharedContentRepo.SubTopic,
        destination: MutableSet<String>
    ) {
        val topicKey = buildString {
            append(topicTitle)

            val cleanSubTopicTitle =
                normalize(subTopic.title)

            if (cleanSubTopicTitle.isNotBlank()) {
                append("__")
                append(cleanSubTopicTitle)
            }
        }

        subTopic.items
            .asSequence()
            .map { item ->
                exerciseIdentityKey(
                    belt = belt,
                    topicTitle = topicKey,
                    rawItem = item
                )
            }
            .filter { identityKey ->
                identityKey.isNotBlank()
            }
            .forEach { identityKey ->
                destination += identityKey
            }

        subTopic.subTopics.forEach { child ->
            collectSubTopicExerciseKeys(
                belt = belt,
                topicTitle = topicKey,
                subTopic = child,
                destination = destination
            )
        }
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
                subTopicCount = 0,
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
        return if (isEnglish) {
            if (stats.exerciseCount == 1) {
                "1 exercise"
            } else {
                "${stats.exerciseCount} exercises"
            }
        } else {
            "${stats.exerciseCount} תרגילים"
        }
    }
}