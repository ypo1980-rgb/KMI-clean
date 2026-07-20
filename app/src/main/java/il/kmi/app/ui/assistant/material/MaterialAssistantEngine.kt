package il.kmi.app.ui.assistant.material

import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

object MaterialAssistantEngine {

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

            /*
             * חגורה שנאמרה במפורש בשאלה קודמת לחגורת ברירת המחדל.
             * כך בקשה כמו "חומר בחגורה כחולה" לא תחפש בטעות
             * בחגורה המועדפת של המשתמש.
             */
            val requestedBelt =
                detectBeltFromText(cleanQuestion) ?: preferredBelt

            if (requestedBelt == null) {
                return buildMissingBeltAnswer(isEnglish)
            }

            val topicMatches = findTopicMatches(
                question = cleanQuestion,
                belt = requestedBelt
            )

            val bestTopicMatch = topicMatches.firstOrNull()

            if (bestTopicMatch == null) {
                return buildTopicsForBeltAnswer(
                    belt = requestedBelt,
                    isEnglish = isEnglish
                )
            }

            val secondTopicMatch = topicMatches.getOrNull(1)

            /*
             * כאשר שתי התאמות קרובות מאוד, לא מנחשים.
             * מציגים למשתמש חלופות מתוך ContentRepo בלבד.
             */
            if (
                secondTopicMatch != null &&
                bestTopicMatch.score < STRONG_TOPIC_MATCH_SCORE &&
                bestTopicMatch.score - secondTopicMatch.score <=
                AMBIGUOUS_TOPIC_SCORE_DIFFERENCE
            ) {
                return buildTopicClarificationAnswer(
                    belt = requestedBelt,
                    matches = topicMatches,
                    isEnglish = isEnglish
                )
            }

            buildTopicExercisesAnswer(
                belt = requestedBelt,
                topicTitle = bestTopicMatch.title,
                isEnglish = isEnglish
            )
        } catch (_: Throwable) {
            if (isEnglish) {
                "There is a temporary issue processing the KAMI material request."
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

    private const val MIN_TOPIC_MATCH_SCORE = 46
    private const val STRONG_TOPIC_MATCH_SCORE = 82
    private const val AMBIGUOUS_TOPIC_SCORE_DIFFERENCE = 8
    private const val MAX_TOPIC_SUGGESTIONS = 4

    private data class TopicMatch(
        val title: String,
        val score: Int
    )

    private val topicAliasGroups: Map<String, List<String>> = linkedMapOf(
        "הגנות" to listOf(
            "הגנות",
            "הגנה",
            "התגוננות",
            "defense",
            "defenses",
            "defence",
            "defences",
            "blocking",
            "blocks"
        ),
        "הגנות חיצוניות" to listOf(
            "הגנות חיצוניות",
            "הגנה חיצונית",
            "external defense",
            "external defenses",
            "outside defense",
            "outside defenses"
        ),
        "הגנות פנימיות" to listOf(
            "הגנות פנימיות",
            "הגנה פנימית",
            "internal defense",
            "internal defenses",
            "inside defense",
            "inside defenses"
        ),
        "שחרורים" to listOf(
            "שחרורים",
            "שחרור",
            "השתחררות",
            "יציאה מאחיזה",
            "יציאה מחניקה",
            "release",
            "releases",
            "escape",
            "escapes",
            "grip release",
            "choke release"
        ),
        "בעיטות" to listOf(
            "בעיטות",
            "בעיטה",
            "בעיטת",
            "kick",
            "kicks",
            "kicking"
        ),
        "עבודת ידיים" to listOf(
            "עבודת ידיים",
            "טכניקות ידיים",
            "ידיים",
            "מכות ידיים",
            "אגרופים",
            "אגרוף",
            "hand work",
            "hand techniques",
            "punch",
            "punches",
            "strike",
            "strikes"
        ),
        "עמידת מוצא" to listOf(
            "עמידת מוצא",
            "עמידת קרב",
            "עמידה",
            "מוצא",
            "ready stance",
            "fighting stance",
            "stance"
        ),
        "בלימות וגלגולים" to listOf(
            "בלימות וגלגולים",
            "בלימות",
            "בלימה",
            "נפילות",
            "נפילה",
            "גלגולים",
            "גלגול",
            "breakfalls",
            "breakfall",
            "falls",
            "fall",
            "rolls",
            "roll"
        ),
        "עבודת קרקע" to listOf(
            "עבודת קרקע",
            "קרקע",
            "הכנה לקרקע",
            "לחימת קרקע",
            "groundwork",
            "ground work",
            "ground fighting",
            "ground techniques",
            "floor work"
        ),
        "הטלות" to listOf(
            "הטלות",
            "הטלה",
            "הפלות",
            "הפלה",
            "throws",
            "throw",
            "takedowns",
            "takedown"
        ),
        "מקל" to listOf(
            "מקל",
            "הגנה ממקל",
            "הגנות ממקל",
            "stick",
            "stick defense",
            "stick defenses",
            "club defense"
        ),
        "סכין" to listOf(
            "סכין",
            "הגנה מסכין",
            "הגנות מסכין",
            "knife",
            "knife defense",
            "knife defenses"
        ),
        "אקדח" to listOf(
            "אקדח",
            "נשק חם",
            "הגנה מאקדח",
            "gun",
            "pistol",
            "firearm",
            "gun defense",
            "gun defenses"
        ),
        "חניקות" to listOf(
            "חניקות",
            "חניקה",
            "שחרור מחניקה",
            "chokes",
            "choke",
            "strangulation",
            "strangulations"
        ),
        "אחיזות" to listOf(
            "אחיזות",
            "אחיזה",
            "תפיסות",
            "תפיסה",
            "grabs",
            "grab",
            "holds",
            "hold",
            "grips",
            "grip"
        )
    )

    private fun findTopicMatches(
        question: String,
        belt: Belt
    ): List<TopicMatch> {
        val topics = ContentRepo.listTopicTitles(belt)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeText(it) }

        if (topics.isEmpty()) return emptyList()

        val cleanedQuestion = cleanMaterialQuestion(question)
        val normalizedQuestion = normalizeText(cleanedQuestion)

        if (normalizedQuestion.isBlank()) return emptyList()

        return topics
            .map { topic ->
                TopicMatch(
                    title = topic,
                    score = calculateTopicScore(
                        normalizedQuestion = normalizedQuestion,
                        topic = topic
                    )
                )
            }
            .filter { it.score >= MIN_TOPIC_MATCH_SCORE }
            .sortedWith(
                compareByDescending<TopicMatch> { it.score }
                    .thenByDescending { normalizeText(it.title).length }
            )
            .take(MAX_TOPIC_SUGGESTIONS)
    }

    private fun calculateTopicScore(
        normalizedQuestion: String,
        topic: String
    ): Int {
        val normalizedTopic = normalizeText(topic)

        if (normalizedQuestion == normalizedTopic) return 100

        if (containsWholePhrase(normalizedQuestion, normalizedTopic)) {
            return 96
        }

        var score = 0

        val questionTokens = significantTokens(normalizedQuestion)
        val topicTokens = significantTokens(normalizedTopic)

        if (topicTokens.isNotEmpty()) {
            val matchingTokens = topicTokens.count { topicToken ->
                questionTokens.any { questionToken ->
                    questionToken == topicToken ||
                            tokenSimilarity(questionToken, topicToken) >= 0.82
                }
            }

            val coverage = matchingTokens.toDouble() / topicTokens.size.toDouble()
            score = maxOf(score, (coverage * 84.0).toInt())
        }

        topicAliasGroups.forEach { (canonicalName, aliases) ->
            val normalizedCanonical = normalizeText(canonicalName)
            val topicBelongsToGroup =
                normalizedTopic.contains(normalizedCanonical) ||
                        normalizedCanonical.contains(normalizedTopic) ||
                        aliases.any { alias ->
                            val normalizedAlias = normalizeText(alias)
                            normalizedTopic.contains(normalizedAlias) ||
                                    normalizedAlias.contains(normalizedTopic)
                        }

            if (!topicBelongsToGroup) return@forEach

            val matchingAlias = aliases
                .map(::normalizeText)
                .filter { it.isNotBlank() }
                .maxByOrNull { alias ->
                    when {
                        containsWholePhrase(normalizedQuestion, alias) -> 100
                        else -> phraseSimilarity(normalizedQuestion, alias)
                    }
                }

            if (matchingAlias != null) {
                val aliasScore = when {
                    containsWholePhrase(
                        normalizedQuestion,
                        matchingAlias
                    ) -> 88

                    else -> {
                        val similarity = phraseSimilarity(
                            normalizedQuestion,
                            matchingAlias
                        )

                        if (similarity >= 72) 72 else 0
                    }
                }

                score = maxOf(score, aliasScore)
            }
        }

        val wholePhraseSimilarity = phraseSimilarity(
            normalizedQuestion,
            normalizedTopic
        )

        if (wholePhraseSimilarity >= 74) {
            score = maxOf(score, wholePhraseSimilarity)
        }

        return score.coerceIn(0, 100)
    }

    private fun significantTokens(text: String): List<String> {
        val ignoredTokens = setOf(
            "של",
            "על",
            "עם",
            "את",
            "מה",
            "איזה",
            "איזו",
            "איך",
            "לי",
            "the",
            "a",
            "an",
            "of",
            "for",
            "in",
            "on",
            "about",
            "show",
            "give",
            "what",
            "which",
            "how"
        )

        return normalizeText(text)
            .split(" ")
            .map { it.trim() }
            .filter { token ->
                token.length >= 2 && token !in ignoredTokens
            }
            .distinct()
    }

    private fun containsWholePhrase(
        text: String,
        phrase: String
    ): Boolean {
        if (text.isBlank() || phrase.isBlank()) return false

        return " $text ".contains(" $phrase ")
    }

    private fun phraseSimilarity(
        first: String,
        second: String
    ): Int {
        val normalizedFirst = normalizeText(first)
        val normalizedSecond = normalizeText(second)

        if (normalizedFirst.isBlank() || normalizedSecond.isBlank()) {
            return 0
        }

        val firstTokens = significantTokens(normalizedFirst)
        val secondTokens = significantTokens(normalizedSecond)

        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0
        }

        val matched = secondTokens.count { secondToken ->
            firstTokens.any { firstToken ->
                tokenSimilarity(firstToken, secondToken) >= 0.78
            }
        }

        return (
                matched.toDouble() /
                        maxOf(firstTokens.size, secondTokens.size).toDouble() *
                        100.0
                ).toInt()
    }

    private fun tokenSimilarity(
        first: String,
        second: String
    ): Double {
        if (first == second) return 1.0
        if (first.isBlank() || second.isBlank()) return 0.0

        val distance = levenshteinDistance(first, second)
        val longestLength = maxOf(first.length, second.length)

        return 1.0 - distance.toDouble() / longestLength.toDouble()
    }

    private fun levenshteinDistance(
        first: String,
        second: String
    ): Int {
        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length

        var previousRow = IntArray(second.length + 1) { it }
        var currentRow = IntArray(second.length + 1)

        for (firstIndex in first.indices) {
            currentRow[0] = firstIndex + 1

            for (secondIndex in second.indices) {
                val insertion = currentRow[secondIndex] + 1
                val deletion = previousRow[secondIndex + 1] + 1
                val substitution = previousRow[secondIndex] +
                        if (first[firstIndex] == second[secondIndex]) 0 else 1

                currentRow[secondIndex + 1] = minOf(
                    insertion,
                    deletion,
                    substitution
                )
            }

            val temporaryRow = previousRow
            previousRow = currentRow
            currentRow = temporaryRow
        }

        return previousRow[second.length]
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
            .replace("show me", " ", ignoreCase = true)
            .replace("give me", " ", ignoreCase = true)
            .replace("list of", " ", ignoreCase = true)
            .replace("exercise list", " ", ignoreCase = true)
            .replace("exercises of", " ", ignoreCase = true)
            .replace("exercises", " ", ignoreCase = true)
            .replace("topic", " ", ignoreCase = true)
            .replace("material", " ", ignoreCase = true)
            .replace("חומר", " ")
            .replace("קמי", " ")
            .replace("ק.מ.י", " ")
            .replace("ק מ י", " ")
            .replace("KAMI", " ", ignoreCase = true)
            .replace("K.A.M.I", " ", ignoreCase = true)
            .replace("K.M.I", " ", ignoreCase = true)
            .replace("K M I", " ", ignoreCase = true)
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
            .replace("ק.מ.י", "קמי")
            .replace("ק מ י", "קמי")
            .replace("k.a.m.i", "kami")
            .replace("k.m.i", "kami")
            .replace("k m i", "kami")
            .replace("k a m i", "kami")
            .replace("kmi", "kami")
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
            "Which belt should I search in?\n\nFor example:\n• Green belt defenses\n• Yellow belt kicks\n• Blue belt releases\n• KAMI material in blue belt"
        } else {
            "באיזו חגורה לחפש?\n\nלדוגמה:\n• הגנות בחגורה ירוקה\n• בעיטות בחגורה צהובה\n• שחרורים בחגורה כחולה"
        }
    }

    private fun buildTopicClarificationAnswer(
        belt: Belt,
        matches: List<TopicMatch>,
        isEnglish: Boolean
    ): String {
        val relevantMatches = matches
            .take(MAX_TOPIC_SUGGESTIONS)
            .distinctBy { normalizeText(it.title) }

        if (relevantMatches.isEmpty()) {
            return buildTopicsForBeltAnswer(
                belt = belt,
                isEnglish = isEnglish
            )
        }

        return buildString {
            if (isEnglish) {
                appendLine(
                    "I found several possible topics in " +
                            "${beltDisplayEn(belt)}."
                )
                appendLine("Which one did you mean?")
            } else {
                appendLine(
                    "מצאתי כמה נושאים אפשריים ב${beltDisplayHe(belt)}."
                )
                appendLine("לאיזה מהם התכוונת?")
            }

            appendLine()

            relevantMatches.forEachIndexed { index, match ->
                appendLine("${index + 1}. ${match.title}")
            }

            appendLine()

            if (isEnglish) {
                append(
                    "You can reply with the topic name, " +
                            "for example: show me ${relevantMatches.first().title}."
                )
            } else {
                append(
                    "אפשר לענות בשם הנושא, למשל: " +
                            "תראה לי ${relevantMatches.first().title}."
                )
            }
        }.trim()
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
                "I couldn't find topics for ${beltDisplayEn(belt)}."
            } else {
                "לא מצאתי נושאים עבור ${beltDisplayHe(belt)}."
            }
        }

        return buildString {
            if (isEnglish) {
                appendLine("Topics in ${beltDisplayEn(belt)}:")
            } else {
                appendLine("הנושאים ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            topics.forEachIndexed { index, topic ->
                appendLine("${index + 1}. $topic")
            }

            appendLine()

            if (isEnglish) {
                append("You can ask for a specific topic, for example: ${beltDisplayEn(belt)} defenses.")
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
                appendLine("I found \"$topicTitle\" in ${beltDisplayEn(belt)}:")
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
                "I found the topic \"$topicTitle\" in ${beltDisplayEn(belt)}, but I couldn't find exercises under it."
            } else {
                "מצאתי את הנושא \"$topicTitle\" ב${beltDisplayHe(belt)}, אבל לא מצאתי תחתיו תרגילים."
            }
        }

        return buildString {
            if (isEnglish) {
                appendLine("Exercises in \"$topicTitle\" for ${beltDisplayEn(belt)}:")
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
                appendLine("I found sub-topics for \"$topicTitle\" in ${beltDisplayEn(belt)}:")
            } else {
                appendLine("מצאתי תתי־נושאים עבור \"$topicTitle\" ב${beltDisplayHe(belt)}:")
            }

            appendLine()

            subTopics.forEachIndexed { index, subTopic ->
                appendLine("${index + 1}. $subTopic")
            }
        }.trim()
    }

    private fun beltDisplayEn(belt: Belt): String {
        val name = belt.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        return "$name belt"
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