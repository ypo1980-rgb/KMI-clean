package il.kmi.shared.domain.content

import il.kmi.shared.domain.content.English.ExerciseTitlesEnAliases
import il.kmi.shared.domain.content.English.ExerciseTitlesEnItems
import il.kmi.shared.domain.content.English.ExerciseTitlesEnTopics

object ExerciseTitlesEn {

    private val map: Map<String, String> = linkedMapOf<String, String>().apply {
        putAll(ExerciseTitlesEnTopics.map)
        putAll(ExerciseTitlesEnItems.map)
        putAll(ExerciseTitlesEnAliases.map)
    }

    fun displayName(text: String, isEnglish: Boolean): String {
        return if (isEnglish) getOrSame(text) else text
    }

    fun get(hebrew: String): String? = map[hebrew]

    private val missingLogged = mutableSetOf<String>()

    fun getOrSame(text: String): String {
        val translated = map[text]

        if (translated == null && missingLogged.add(text)) {
            println("KMI_TRANSLATION_MISSING: $text")
        }

        return translated ?: text
    }
}