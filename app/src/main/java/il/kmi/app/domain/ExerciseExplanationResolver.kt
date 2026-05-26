package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.English.ExerciseExplanationsEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry

object ExerciseExplanationResolver {

    fun get(
        belt: Belt,
        topic: String,
        item: String,
        isEnglish: Boolean
    ): String {
        val cleanItem = item.trim()
        val cleanTopic = topic.trim()

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanItem,
            topicKey = cleanTopic.ifBlank { null }
        )

        if (resolved.isKnown) {
            val identity = ExerciseIdentityRegistry.knownById(resolved.id)

            if (isEnglish) {
                val english = getEnglishByIdentity(
                    belt = identity?.belt ?: belt,
                    canonicalTitle = identity?.hebrewTitle ?: cleanItem,
                    aliases = identity?.aliases.orEmpty(),
                    fallbackItem = cleanItem
                )

                if (!isEnglishFallback(english)) {
                    return english
                }
            } else {
                val hebrew = Explanations.getByExerciseId(
                    exerciseId = resolved.id,
                    fallbackBelt = belt,
                    fallbackItem = cleanItem
                ).trim()

                if (!isHebrewFallback(hebrew)) {
                    return hebrew
                }
            }
        }

        return getLegacyFallback(
            belt = belt,
            item = cleanItem,
            isEnglish = isEnglish
        )
    }

    fun resolveId(
        belt: Belt,
        topic: String,
        item: String
    ): String {
        return ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = item.trim(),
            topicKey = topic.trim().ifBlank { null }
        ).id
    }

    private fun getEnglishByIdentity(
        belt: Belt,
        canonicalTitle: String,
        aliases: Set<String>,
        fallbackItem: String
    ): String {
        val candidates = linkedSetOf<String>().apply {
            add(canonicalTitle)
            addAll(aliases)
            add(fallbackItem)
        }.filter { it.isNotBlank() }

        candidates.forEach { candidate ->
            val got = ExerciseExplanationsEn.get(belt, candidate).trim()

            if (!isEnglishFallback(got)) {
                return got
            }
        }

        return ExerciseExplanationsEn.get(belt, fallbackItem).trim()
    }

    private fun getLegacyFallback(
        belt: Belt,
        item: String,
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            ExerciseExplanationsEn.get(belt, item).trim()
        } else {
            Explanations.get(belt, item).trim()
        }
    }

    private fun isHebrewFallback(text: String): Boolean {
        return text.isBlank() ||
                text.startsWith("הסבר מפורט על") ||
                text.startsWith("אין כרגע")
    }

    private fun isEnglishFallback(text: String): Boolean {
        return text.isBlank() ||
                text.startsWith("Detailed explanation for:") ||
                text.startsWith("There is currently no explanation")
    }
}