package il.kmi.app.ui.assistant.material

import android.util.Log
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

object MaterialAssistantEngine {

    private const val TAG = "KMI_MATERIAL_AI"

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return try {
            ContentRepo.initIfNeeded()

            val cleanQuestion = question.trim()
            if (cleanQuestion.isBlank()) {
                return if (isEnglish) {
                    "Write a belt and topic, for example: green belt defenses."
                } else {
                    "כתוב חגורה ונושא, למשל: הגנות בחגורה ירוקה."
                }
            }

            val requestedBelt = preferredBelt ?: detectBeltFromText(cleanQuestion)

            if (requestedBelt == null) {
                return buildMissingBeltAnswer(isEnglish)
            }

            val requestedTopic = detectTopicFromText(
                question = cleanQuestion,
                belt = requestedBelt
            )

            if (requestedTopic == null) {
                return buildTopicsForBeltAnswer(
                    belt = requestedBelt,
                    isEnglish = isEnglish
                )
            }

            buildTopicExercisesAnswer(
                belt = requestedBelt,
                topicTitle = requestedTopic,
                isEnglish = isEnglish
            )
        } catch (t: Throwable) {
            Log.e(TAG, "MaterialAssistantEngine failed", t)

            if (isEnglish) {
                "There is a temporary issue processing the K.A.M.I material request."
            } else {
                "יש תקלה רגעית בעיבוד בקשת חומר ק.מ.י."
            }
        }
    }

    // ---------------------------------------------------------
    // Belt detection
    // ---------------------------------------------------------

    private fun detectBeltFromText(text: String): Belt? {
        val q = normalizeText(text)

        return when {
            q.contains("צהובה") || q.contains("צהוב") || q.contains("yellow") ->
                Belt.YELLOW

            q.contains("כתומה") || q.contains("כתום") || q.contains("orange") ->
                Belt.ORANGE

            q.contains("ירוקה") || q.contains("ירוק") || q.contains("green") ->
                Belt.GREEN

            q.contains("כחולה") || q.contains("כחול") || q.contains("blue") ->
                Belt.BLUE

            q.contains("חומה") || q.contains("חום") || q.contains("brown") ->
                Belt.BROWN

            q.contains("שחורה") || q.contains("שחור") || q.contains("black") ->
                Belt.BLACK

            else -> null
        }
    }

    // ---------------------------------------------------------
    // Topic detection
    // ---------------------------------------------------------

    private fun detectTopicFromText(
        question: String,
        belt: Belt
    ): String? {
        val topics = ContentRepo.listTopicTitles(belt)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (topics.isEmpty()) return null

        val cleanedQuestion = cleanMaterialQuestion(question)
        val normalizedQuestion = normalizeText(cleanedQuestion)

        val directMatch = topics
            .filter { topic ->
                val normalizedTopic = normalizeText(topic)
                normalizedQuestion == normalizedTopic ||
                        normalizedQuestion.contains(normalizedTopic)
            }
            .maxByOrNull { normalizeText(it).length }

        if (directMatch != null) return directMatch

        val topicAliases = linkedMapOf(
            "הגנות" to listOf(
                "הגנות",
                "הגנה",
                "defenses",
                "defences",
                "defense",
                "defence"
            ),
            "שחרורים" to listOf(
                "שחרורים",
                "שחרור",
                "releases",
                "release"
            ),
            "בעיטות" to listOf(
                "בעיטות",
                "בעיטה",
                "kicks",
                "kick"
            ),
            "עבודת ידיים" to listOf(
                "עבודת ידיים",
                "ידיים",
                "אגרופים",
                "אגרוף",
                "hand techniques",
                "punches",
                "punch"
            ),
            "עמידת מוצא" to listOf(
                "עמידת מוצא",
                "עמידה",
                "ready stance",
                "stance"
            ),
            "בלימות וגלגולים" to listOf(
                "בלימות",
                "גלגולים",
                "בלימות וגלגולים",
                "breakfalls",
                "rolls"
            ),
            "עבודת קרקע" to listOf(
                "קרקע",
                "עבודת קרקע",
                "groundwork",
                "ground work"
            ),
            "עבודת קרקע" to listOf(
                "עבודת קרקע",
                "הכנה לקרקע",
                "groundwork preparation"
            )
        )

        for ((canonicalTopicName, aliases) in topicAliases) {
            val aliasHit = aliases.any { alias ->
                normalizedQuestion.contains(normalizeText(alias))
            }

            if (!aliasHit) continue

            val exactCanonical = topics.firstOrNull { topic ->
                normalizeText(topic) == normalizeText(canonicalTopicName)
            }

            if (exactCanonical != null) return exactCanonical

            val containsCanonical = topics.firstOrNull { topic ->
                normalizeText(topic).contains(normalizeText(canonicalTopicName))
            }

            if (containsCanonical != null) return containsCanonical
        }

        return null
    }

    private fun cleanMaterialQuestion(text: String): String {
        return text
            .replace("חגורה", " ")
            .replace("בחגורה", " ")
            .replace("נושא", " ")
            .replace("בנושא", " ")
            .replace("תראה לי", " ")
            .replace("תן לי", " ")
            .replace("תני לי", " ")
            .replace("רשימה של", " ")
            .replace("רשימת", " ")
            .replace("תרגילים של", " ")
            .replace("תרגילי", " ")
            .replace("תרגילים", " ")
            .replace("חומר", " ")
            .replace("קמי", " ")
            .replace("ק.מ.י", " ")
            .replace("K.A.M.I", " ", ignoreCase = true)
            .replace("yellow", " ", ignoreCase = true)
            .replace("orange", " ", ignoreCase = true)
            .replace("green", " ", ignoreCase = true)
            .replace("blue", " ", ignoreCase = true)
            .replace("brown", " ", ignoreCase = true)
            .replace("black", " ", ignoreCase = true)
            .replace("צהובה", " ")
            .replace("צהוב", " ")
            .replace("כתומה", " ")
            .replace("כתום", " ")
            .replace("ירוקה", " ")
            .replace("ירוק", " ")
            .replace("כחולה", " ")
            .replace("כחול", " ")
            .replace("חומה", " ")
            .replace("חום", " ")
            .replace("שחורה", " ")
            .replace("שחור", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace("־", "-")
            .replace("–", "-")
            .replace("-", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("\"", " ")
            .replace("'", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // ---------------------------------------------------------
    // Answers
    // ---------------------------------------------------------

    private fun buildMissingBeltAnswer(isEnglish: Boolean): String {
        return if (isEnglish) {
            "Which belt should I search in?\n\nFor example:\n• Green belt defenses\n• Yellow belt kicks\n• Blue belt releases"
        } else {
            "באיזו חגורה לחפש?\n\nלדוגמה:\n• הגנות בחגורה ירוקה\n• בעיטות בחגורה צהובה\n• שחרורים בחגורה כחולה"
        }
    }

    private fun buildTopicsForBeltAnswer(
        belt: Belt,
        isEnglish: Boolean
    ): String {
        val topics = ContentRepo.listTopicTitles(belt)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeText(it) }

        if (topics.isEmpty()) {
            return if (isEnglish) {
                "I couldn't find topics for ${belt.name.lowercase()} belt."
            } else {
                "לא מצאתי נושאים עבור ${beltDisplayHe(belt)}."
            }
        }

        return buildString {
            if (isEnglish) {
                appendLine("Topics in ${belt.name.lowercase()} belt:")
            } else {
                appendLine("הנושאים ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            topics.forEachIndexed { index, topic ->
                appendLine("${index + 1}. $topic")
            }

            appendLine()

            if (isEnglish) {
                append("You can ask for a specific topic, for example: ${belt.name.lowercase()} belt defenses.")
            } else {
                append("אפשר לבקש נושא מסוים, למשל: הגנות ב${beltDisplayHe(belt)}.")
            }
        }.trim()
    }

    private fun buildTopicExercisesAnswer(
        belt: Belt,
        topicTitle: String,
        isEnglish: Boolean
    ): String {
        val subTopics = ContentRepo.listSubTopicTitles(
            belt = belt,
            topicTitle = topicTitle
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeText(it) }

        val directItems = ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )
            .map { displayItemName(it) }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeText(it) }

        if (subTopics.isEmpty()) {
            return buildFlatExerciseListAnswer(
                belt = belt,
                topicTitle = topicTitle,
                items = directItems,
                isEnglish = isEnglish
            )
        }

        val groupedItems = subTopics.map { subTopic ->
            val items = ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = subTopic
            )
                .map { displayItemName(it) }
                .filter { it.isNotBlank() }
                .distinctBy { normalizeText(it) }

            subTopic to items
        }

        val hasAnyItems = groupedItems.any { (_, items) -> items.isNotEmpty() }

        if (!hasAnyItems) {
            return buildSubTopicsOnlyAnswer(
                belt = belt,
                topicTitle = topicTitle,
                subTopics = subTopics,
                isEnglish = isEnglish
            )
        }

        return buildString {
            if (isEnglish) {
                appendLine("I found \"$topicTitle\" in ${belt.name.lowercase()} belt:")
            } else {
                appendLine("מצאתי את הנושא \"$topicTitle\" ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            groupedItems.forEach { (subTopic, items) ->
                if (items.isEmpty()) return@forEach

                appendLine("• $subTopic")
                items.forEachIndexed { index, item ->
                    appendLine("  ${index + 1}. $item")
                }
                appendLine()
            }
        }.trim()
    }

    private fun buildFlatExerciseListAnswer(
        belt: Belt,
        topicTitle: String,
        items: List<String>,
        isEnglish: Boolean
    ): String {
        if (items.isEmpty()) {
            return if (isEnglish) {
                "I found the topic \"$topicTitle\" in ${belt.name.lowercase()} belt, but I couldn't find exercises under it."
            } else {
                "מצאתי את הנושא \"$topicTitle\" ב${beltDisplayHe(belt)}, אבל לא מצאתי תחתיו תרגילים."
            }
        }

        return buildString {
            if (isEnglish) {
                appendLine("Exercises in \"$topicTitle\" for ${belt.name.lowercase()} belt:")
            } else {
                appendLine("התרגילים בנושא \"$topicTitle\" ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            items.forEachIndexed { index, item ->
                appendLine("${index + 1}. $item")
            }
        }.trim()
    }

    private fun buildSubTopicsOnlyAnswer(
        belt: Belt,
        topicTitle: String,
        subTopics: List<String>,
        isEnglish: Boolean
    ): String {
        return buildString {
            if (isEnglish) {
                appendLine("I found sub-topics for \"$topicTitle\" in ${belt.name.lowercase()} belt:")
            } else {
                appendLine("מצאתי תתי־נושאים עבור \"$topicTitle\" ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            subTopics.forEachIndexed { index, subTopic ->
                appendLine("${index + 1}. $subTopic")
            }
        }.trim()
    }

    private fun beltDisplayHe(belt: Belt): String {
        val raw = belt.heb.trim()
        return if (raw.startsWith("חגורה")) {
            raw
        } else {
            "חגורה $raw"
        }
    }

    private fun displayItemName(rawItem: String): String {
        return ExerciseTitleFormatter
            .displayName(rawItem)
            .ifBlank { rawItem }
            .trim()
    }
}