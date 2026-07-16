package il.kmi.app.voicecommands

import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.KmiSearchBridge
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import il.kmi.shared.domain.content.ExerciseTitlesEn

sealed interface VoiceContentDestination {

    data class BeltScreen(
        val belt: Belt
    ) : VoiceContentDestination

    data class TopicScreen(
        val belt: Belt,
        val topicTitle: String
    ) : VoiceContentDestination

    data class Exercise(
        val belt: Belt,
        val topicTitle: String,
        val itemTitle: String,
        val stableKey: String
    ) : VoiceContentDestination
}

sealed interface VoiceContentResolution {

    data class Found(
        val destination: VoiceContentDestination
    ) : VoiceContentResolution

    data class Multiple(
        val destinations: List<VoiceContentDestination>
    ) : VoiceContentResolution

    data object NotFound : VoiceContentResolution
}

object VoiceContentResolver {

    fun resolve(
        rawQuery: String
    ): VoiceContentResolution {
        val query = normalize(rawQuery)

        if (query.isBlank()) {
            return VoiceContentResolution.NotFound
        }

        val requestedBelt = findBelt(query)
        val queryWithoutBelt = removeBeltWords(
            query = query,
            belt = requestedBelt
        )

        /*
         * אם נאמר רק שם החגורה, פותחים את מסך החגורה.
         */
        if (
            requestedBelt != null &&
            queryWithoutBelt.isBlank()
        ) {
            return VoiceContentResolution.Found(
                VoiceContentDestination.BeltScreen(
                    belt = requestedBelt
                )
            )
        }

        /*
         * לפני חיפוש תרגיל, בודקים אם המשתמש אמר
         * שם של נושא מלא מתוך ContentRepo.
         */
        val topicMatch = findTopic(
            query = queryWithoutBelt.ifBlank { query },
            requestedBelt = requestedBelt
        )

        if (topicMatch != null) {
            return VoiceContentResolution.Found(topicMatch)
        }

        return findExercises(
            query = queryWithoutBelt.ifBlank { query },
            requestedBelt = requestedBelt
        )
    }

    private fun findTopic(
        query: String,
        requestedBelt: Belt?
    ): VoiceContentDestination.TopicScreen? {
        val belts = requestedBelt
            ?.let { listOf(it) }
            ?: Belt.entries

        val candidates = belts.flatMap { belt ->
            SharedContentRepo.data[belt]
                ?.topics
                .orEmpty()
                .map { topic ->
                    val hebrewTitle = topic.title.trim()

                    val englishTitle = ExerciseTitlesEn
                        .get(hebrewTitle)
                        .orEmpty()
                        .trim()

                    Triple(
                        belt,
                        hebrewTitle,
                        englishTitle
                    )
                }
        }

        val exact = candidates.firstOrNull {
            normalize(it.second) == query ||
                    (
                            it.third.isNotBlank() &&
                                    normalize(it.third) == query
                            )
        }

        val selected = exact ?: candidates.firstOrNull {
            val hebrew = normalize(it.second)
            val english = normalize(it.third)

            hebrew.contains(query) ||
                    query.contains(hebrew) ||
                    (
                            english.isNotBlank() &&
                                    (
                                            english.contains(query) ||
                                                    query.contains(english)
                                            )
                            )
        }

        return selected?.let { (belt, title, _) ->
            VoiceContentDestination.TopicScreen(
                belt = belt,
                topicTitle = title
            )
        }
    }

    private fun findExercises(
        query: String,
        requestedBelt: Belt?
    ): VoiceContentResolution {
        val hits = runCatching {
            KmiSearchBridge.searchExercises(query)
        }.getOrElse {
            emptyList()
        }

        val destinations = hits.mapNotNull { hit ->
            val rawKey = hit.id ?: hit.title

            val resolved = runCatching {
                ContentRepo.resolveItemKey(rawKey)
            }.getOrNull() ?: return@mapNotNull null

            if (
                requestedBelt != null &&
                resolved.belt != requestedBelt
            ) {
                return@mapNotNull null
            }

            VoiceContentDestination.Exercise(
                belt = resolved.belt,
                topicTitle = resolved.topicTitle,
                itemTitle = resolved.itemTitle,
                stableKey = rawKey
            )
        }
            .distinctBy { destination ->
                listOf(
                    destination.belt.id,
                    destination.topicTitle,
                    destination.itemTitle
                ).joinToString("|")
            }
            .take(5)

        return when (destinations.size) {
            0 ->
                VoiceContentResolution.NotFound

            1 ->
                VoiceContentResolution.Found(
                    destinations.first()
                )

            else ->
                VoiceContentResolution.Multiple(
                    destinations
                )
        }
    }

    private fun findBelt(
        normalizedQuery: String
    ): Belt? {
        return beltAliases.entries.firstOrNull { entry ->
            entry.value.any { alias ->
                containsPhrase(
                    text = normalizedQuery,
                    phrase = normalize(alias)
                )
            }
        }?.key
    }

    private fun removeBeltWords(
        query: String,
        belt: Belt?
    ): String {
        if (belt == null) return query

        var result = query

        beltAliases[belt]
            .orEmpty()
            .sortedByDescending { it.length }
            .forEach { alias ->
                result = result
                    .replace(
                        normalize(alias),
                        " "
                    )
            }

        return result
            .replace("חגורה", " ")
            .replace("belt", " ")
            .replace("של", " ")
            .replace("בחגורת", " ")
            .replace("בחגורה", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {
        if (phrase.isBlank()) return false

        return text == phrase ||
                text.startsWith("$phrase ") ||
                text.endsWith(" $phrase") ||
                text.contains(" $phrase ")
    }

    private fun normalize(
        value: String
    ): String {
        return value
            .lowercase()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", " ")
            .replace("—", " ")
            .replace("-", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val beltAliases: Map<Belt, List<String>> = mapOf(
        Belt.WHITE to listOf(
            "לבנה",
            "לבן",
            "חגורה לבנה",
            "white",
            "white belt"
        ),
        Belt.YELLOW to listOf(
            "צהובה",
            "צהוב",
            "חגורה צהובה",
            "yellow",
            "yellow belt"
        ),
        Belt.ORANGE to listOf(
            "כתומה",
            "כתום",
            "חגורה כתומה",
            "orange",
            "orange belt"
        ),
        Belt.GREEN to listOf(
            "ירוקה",
            "ירוק",
            "חגורה ירוקה",
            "green",
            "green belt"
        ),
        Belt.BLUE to listOf(
            "כחולה",
            "כחול",
            "חגורה כחולה",
            "blue",
            "blue belt"
        ),
        Belt.BROWN to listOf(
            "חומה",
            "חום",
            "חגורה חומה",
            "brown",
            "brown belt"
        ),
        Belt.BLACK to listOf(
            "שחורה",
            "שחור",
            "חגורה שחורה",
            "דאן",
            "black",
            "black belt",
            "dan"
        )
    )
}