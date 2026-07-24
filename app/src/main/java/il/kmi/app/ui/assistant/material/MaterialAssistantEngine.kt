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
             * מאתרים את כל החגורות שנאמרו בשאלה.
             * detectBeltFromText לבדו מחזיר חגורה אחת בלבד
             * ולכן אינו מתאים לשאלות השוואה.
             */
            val explicitlyRequestedBelts =
                detectBeltsFromText(cleanQuestion)

            val comparisonRequested =
                isBeltComparisonRequest(cleanQuestion)

            val comparisonBelts =
                buildList {
                    addAll(explicitlyRequestedBelts)

                    /*
                     * מאפשר שאלות כמו:
                     * "השווה בין חגורה כחולה לחגורה שלי".
                     */
                    if (
                        comparisonRequested &&
                        size == 1 &&
                        preferredBelt != null &&
                        preferredBelt !in this
                    ) {
                        add(preferredBelt)
                    }
                }
                    .distinct()
                    .take(2)

            if (comparisonRequested) {
                if (comparisonBelts.size < 2) {
                    return buildMissingComparisonBeltAnswer(
                        selectedBelts = comparisonBelts,
                        isEnglish = isEnglish
                    )
                }

                return buildBeltComparisonAnswer(
                    firstBelt = comparisonBelts[0],
                    secondBelt = comparisonBelts[1],
                    isEnglish = isEnglish
                )
            }

            /*
             * שאלת איתור תרגיל מחפשת בכל החגורות, אלא אם
             * המשתמש ביקש במפורש לחפש בחגורה מסוימת.
             */
            if (isExerciseLocationRequest(cleanQuestion)) {
                val beltsToSearch =
                    explicitlyRequestedBelts
                        .takeIf { it.isNotEmpty() }
                        ?: ContentRepo.listBeltsInOrder()

                return buildExerciseLocationAnswer(
                    question = cleanQuestion,
                    belts = beltsToSearch,
                    isEnglish = isEnglish
                )
            }

            /*
             * בבקשה רגילה משתמשים בחגורה שנאמרה בשאלה,
             * ואם לא נאמרה חגורה — בחגורת המשתמש.
             */
            val requestedBelt =
                explicitlyRequestedBelts.firstOrNull()
                    ?: preferredBelt

            if (requestedBelt == null) {
                return buildMissingBeltAnswer(isEnglish)
            }

            /*
             * בקשה מפורשת להצגת כל חומר החגורה מקבלת
             * תשובה מלאה ולא רק רשימת נושאים ראשיים.
             */
            if (isFullBeltMaterialRequest(cleanQuestion)) {
                return buildFullBeltMaterialAnswer(
                    belt = requestedBelt,
                    isEnglish = isEnglish
                )
            }

            /*
             * מחפשים במקביל גם נושאים ראשיים וגם תתי־נושאים.
             * כך בקשה כמו "שחרור מחניקות בחגורה ירוקה"
             * אינה מצטמצמת אוטומטית לנושא הראשי "שחרורים".
             */
            val topicMatches = findTopicMatches(
                question = cleanQuestion,
                belt = requestedBelt
            )

            val subTopicMatches = findSubTopicMatches(
                question = cleanQuestion,
                belt = requestedBelt
            )

            val bestTopicMatch = topicMatches.firstOrNull()
            val bestSubTopicMatch = subTopicMatches.firstOrNull()

            /*
             * שאלת ספירה נענית לפי רמת הפירוט שנמצאה:
             * חגורה שלמה, נושא ראשי או תת־נושא.
             */
            if (isMaterialCountRequest(cleanQuestion)) {
                return buildMaterialCountAnswer(
                    belt = requestedBelt,
                    topicMatch = bestTopicMatch,
                    subTopicMatch = bestSubTopicMatch,
                    isEnglish = isEnglish
                )
            }

            /*
             * התאמה חזקה לתת־נושא מקבלת עדיפות כאשר היא
             * מדויקת יותר מההתאמה לנושא הראשי.
             */
            val shouldOpenSubTopic =
                bestSubTopicMatch != null &&
                        bestSubTopicMatch.score >=
                        STRONG_TOPIC_MATCH_SCORE &&
                        (
                                bestTopicMatch == null ||
                                        bestSubTopicMatch.score >
                                        bestTopicMatch.score
                                )

            if (shouldOpenSubTopic) {
                val matchedSubTopic =
                    requireNotNull(bestSubTopicMatch)

                return buildSubTopicExercisesAnswer(
                    belt = requestedBelt,
                    topicTitle = matchedSubTopic.topicTitle,
                    subTopicTitle = matchedSubTopic.subTopicTitle,
                    isEnglish = isEnglish
                )
            }

            if (bestTopicMatch == null) {
                /*
                 * נמצאו תתי־נושאים, אבל ההתאמה אינה מספיק
                 * חזקה לבחירה אוטומטית — מציגים אפשרויות.
                 */
                if (subTopicMatches.isNotEmpty()) {
                    return buildSubTopicClarificationAnswer(
                        belt = requestedBelt,
                        matches = subTopicMatches,
                        isEnglish = isEnglish
                    )
                }

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

    private fun detectBeltsFromText(
        text: String
    ): List<Belt> {
        val q = normalizeText(text)

        return buildList {
            if (
                q.contains("צהובה") ||
                q.contains("צהוב") ||
                q.contains("yellow")
            ) {
                add(Belt.YELLOW)
            }

            if (
                q.contains("כתומה") ||
                q.contains("כתום") ||
                q.contains("orange")
            ) {
                add(Belt.ORANGE)
            }

            if (
                q.contains("ירוקה") ||
                q.contains("ירוק") ||
                q.contains("green")
            ) {
                add(Belt.GREEN)
            }

            if (
                q.contains("כחולה") ||
                q.contains("כחול") ||
                q.contains("blue")
            ) {
                add(Belt.BLUE)
            }

            if (
                q.contains("חומה") ||
                q.contains("חום") ||
                q.contains("brown")
            ) {
                add(Belt.BROWN)
            }

            if (
                q.contains("שחורה") ||
                q.contains("שחור") ||
                q.contains("black")
            ) {
                add(Belt.BLACK)
            }
        }.distinct()
    }

    private fun detectBeltFromText(
        text: String
    ): Belt? {
        return detectBeltsFromText(text)
            .firstOrNull()
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

    private data class SubTopicMatch(
        val topicTitle: String,
        val subTopicTitle: String,
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

    /**
     * מחפש התאמות בכל תתי־הנושאים של החגורה.
     *
     * אין כאן שמות קשיחים: כל תת־נושא חדש שיתווסף
     * ל־ContentRepo ישתתף אוטומטית בחיפוש.
     */
    private fun findSubTopicMatches(
        question: String,
        belt: Belt
    ): List<SubTopicMatch> {
        val cleanedQuestion =
            cleanMaterialQuestion(question)

        val normalizedQuestion =
            normalizeText(cleanedQuestion)

        if (normalizedQuestion.isBlank()) {
            return emptyList()
        }

        return ContentRepo
            .listTopicTitles(belt)
            .asSequence()
            .map { topicTitle ->
                topicTitle.trim()
            }
            .filter { topicTitle ->
                topicTitle.isNotBlank()
            }
            .flatMap { topicTitle ->
                ContentRepo
                    .listSubTopicTitles(
                        belt = belt,
                        topicTitle = topicTitle
                    )
                    .asSequence()
                    .map { subTopicTitle ->
                        SubTopicMatch(
                            topicTitle = topicTitle,
                            subTopicTitle =
                                subTopicTitle.trim(),
                            score = calculateTopicScore(
                                normalizedQuestion =
                                    normalizedQuestion,
                                topic = subTopicTitle
                            )
                        )
                    }
            }
            .filter { match ->
                match.subTopicTitle.isNotBlank() &&
                        match.score >= MIN_TOPIC_MATCH_SCORE
            }
            .distinctBy { match ->
                listOf(
                    normalizeText(match.topicTitle),
                    normalizeText(match.subTopicTitle)
                ).joinToString("|")
            }
            .sortedWith(
                compareByDescending<SubTopicMatch> {
                    it.score
                }
                    .thenByDescending {
                        normalizeText(
                            it.subTopicTitle
                        ).length
                    }
            )
            .take(MAX_TOPIC_SUGGESTIONS)
            .toList()
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
    // Request types
    // ---------------------------------------------------------

    private fun isExerciseLocationRequest(
        question: String
    ): Boolean {
        val normalized = normalizeText(question)

        return listOf(
            "באיזו חגורה",
            "באיזה חגורה",
            "באיזה נושא",
            "באיזו נושא",
            "באיזה תת נושא",
            "איפה נמצא",
            "איפה נמצאת",
            "היכן נמצא",
            "היכן נמצאת",
            "איפה מופיע",
            "איפה מופיעה",
            "מצא לי את התרגיל",
            "חפש את התרגיל",
            "which belt",
            "what belt",
            "which topic",
            "what topic",
            "where is",
            "where can i find",
            "find the exercise",
            "locate the exercise"
        ).any { expression ->
            normalized.contains(
                normalizeText(expression)
            )
        }
    }

    private fun cleanExerciseLocationQuestion(
        question: String
    ): String {
        var result = question

        val expressionsToRemove = listOf(
            "באיזו חגורה נמצא",
            "באיזו חגורה נמצאת",
            "באיזה חגורה נמצא",
            "באיזה חגורה נמצאת",
            "באיזה נושא נמצא",
            "באיזה נושא נמצאת",
            "באיזה תת נושא נמצא",
            "באיזה תת נושא נמצאת",
            "איפה נמצא",
            "איפה נמצאת",
            "היכן נמצא",
            "היכן נמצאת",
            "איפה מופיע",
            "איפה מופיעה",
            "מצא לי את התרגיל",
            "חפש את התרגיל",
            "באיזו חגורה",
            "באיזה חגורה",
            "באיזה נושא",
            "באיזה תת נושא",
            "which belt contains",
            "what belt contains",
            "which belt is",
            "what belt is",
            "which topic contains",
            "what topic contains",
            "where can i find",
            "where is",
            "find the exercise",
            "locate the exercise"
        )

        expressionsToRemove
            .sortedByDescending { it.length }
            .forEach { expression ->
                result = result.replace(
                    expression,
                    " ",
                    ignoreCase = true
                )
            }

        return cleanMaterialQuestion(result)
            .replace("נמצא", " ")
            .replace("נמצאת", " ")
            .replace("מופיע", " ")
            .replace("מופיעה", " ")
            .replace("תרגיל", " ")
            .replace("exercise", " ", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isBeltComparisonRequest(
        question: String
    ): Boolean {
        val normalized = normalizeText(question)

        return listOf(
            "השווה",
            "השוואה",
            "מה ההבדל",
            "מה נוסף",
            "מה שונה",
            "הבדלים בין",
            "לעומת",
            "מול",
            "compare",
            "comparison",
            "difference",
            "differences",
            "versus",
            " vs "
        ).any { expression ->
            normalized.contains(
                normalizeText(expression)
            )
        }
    }

    private fun isMaterialCountRequest(
        question: String
    ): Boolean {
        val normalized = normalizeText(question)

        val hasCountExpression =
            normalized.contains("כמה") ||
                    normalized.contains("מספר") ||
                    normalized.contains("כמות") ||
                    normalized.contains("ספירה") ||
                    normalized.contains("how many") ||
                    normalized.contains("number of") ||
                    normalized.contains("count")

        val hasMaterialExpression =
            normalized.contains("תרגיל") ||
                    normalized.contains("נושא") ||
                    normalized.contains("חומר") ||
                    normalized.contains("exercise") ||
                    normalized.contains("topic") ||
                    normalized.contains("material")

        return hasCountExpression && hasMaterialExpression
    }

    private fun isFullBeltMaterialRequest(
        question: String
    ): Boolean {
        val normalized = normalizeText(question)

        return listOf(
            "כל החומר",
            "החומר המלא",
            "חומר מלא",
            "כל התרגילים",
            "רשימת כל התרגילים",
            "כל הנושאים והתרגילים",
            "full material",
            "all material",
            "complete material",
            "all exercises",
            "complete exercise list"
        ).any { expression ->
            normalized.contains(
                normalizeText(expression)
            )
        }
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

    private data class BeltMaterialItem(
        val topicTitle: String,
        val itemTitle: String
    )

    private data class ExerciseLocation(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val itemTitle: String,
        val score: Int
    )

    private fun findExerciseLocations(
        question: String,
        belts: List<Belt>
    ): List<ExerciseLocation> {
        val requestedName =
            cleanExerciseLocationQuestion(question)

        val normalizedRequestedName =
            normalizeText(requestedName)

        if (normalizedRequestedName.isBlank()) {
            return emptyList()
        }

        return buildList {
            belts
                .distinct()
                .forEach { belt ->
                    ContentRepo
                        .listTopicTitles(belt)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { topicTitle ->
                            val subTopics =
                                ContentRepo
                                    .listSubTopicTitles(
                                        belt = belt,
                                        topicTitle = topicTitle
                                    )
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinctBy(::normalizeText)

                            val locations =
                                listOf<String?>(null) +
                                        subTopics

                            locations.forEach { subTopicTitle ->
                                ContentRepo
                                    .listItemTitles(
                                        belt = belt,
                                        topicTitle = topicTitle,
                                        subTopicTitle = subTopicTitle
                                    )
                                    .forEach { rawItem ->
                                        val itemTitle =
                                            displayItemName(rawItem)

                                        val normalizedItem =
                                            normalizeText(itemTitle)

                                        val score =
                                            when {
                                                normalizedItem ==
                                                        normalizedRequestedName ->
                                                    100

                                                normalizedItem.contains(
                                                    normalizedRequestedName
                                                ) ->
                                                    96

                                                normalizedRequestedName.contains(
                                                    normalizedItem
                                                ) ->
                                                    90

                                                else ->
                                                    phraseSimilarity(
                                                        normalizedRequestedName,
                                                        normalizedItem
                                                    )
                                            }

                                        if (score >= 55) {
                                            add(
                                                ExerciseLocation(
                                                    belt = belt,
                                                    topicTitle = topicTitle,
                                                    subTopicTitle =
                                                        subTopicTitle,
                                                    itemTitle = itemTitle,
                                                    score = score
                                                )
                                            )
                                        }
                                    }
                            }
                        }
                }
        }
            .distinctBy { location ->
                listOf(
                    location.belt.name,
                    normalizeText(location.topicTitle),
                    normalizeText(
                        location.subTopicTitle.orEmpty()
                    ),
                    normalizeText(location.itemTitle)
                ).joinToString("|")
            }
            .sortedWith(
                compareByDescending<ExerciseLocation> {
                    it.score
                }
                    .thenBy {
                        ContentRepo
                            .listBeltsInOrder()
                            .indexOf(it.belt)
                    }
                    .thenBy {
                        normalizeText(it.itemTitle)
                    }
            )
    }

    private fun allMaterialItemsForBelt(
        belt: Belt
    ): List<BeltMaterialItem> {
        return ContentRepo
            .listTopicTitles(belt)
            .asSequence()
            .map { topicTitle ->
                topicTitle.trim()
            }
            .filter { topicTitle ->
                topicTitle.isNotBlank()
            }
            .flatMap { topicTitle ->
                allItemsForTopic(
                    belt = belt,
                    topicTitle = topicTitle
                )
                    .asSequence()
                    .map { itemTitle ->
                        BeltMaterialItem(
                            topicTitle = topicTitle,
                            itemTitle = itemTitle
                        )
                    }
            }
            .distinctBy { item ->
                listOf(
                    normalizeText(item.topicTitle),
                    normalizeText(item.itemTitle)
                ).joinToString("|")
            }
            .toList()
    }

    private fun buildExerciseLocationAnswer(
        question: String,
        belts: List<Belt>,
        isEnglish: Boolean
    ): String {
        val requestedName =
            cleanExerciseLocationQuestion(question)

        if (requestedName.isBlank()) {
            return if (isEnglish) {
                "Which exercise would you like me to locate?"
            } else {
                "איזה תרגיל לחפש בחומר ק.מ.י?"
            }
        }

        val locations =
            findExerciseLocations(
                question = question,
                belts = belts
            )

        if (locations.isEmpty()) {
            return if (isEnglish) {
                buildString {
                    append("I couldn't find an exercise matching \"")
                    append(requestedName)
                    append("\".\n\n")
                    append(
                        "Try writing the full exercise name " +
                                "or a more specific part of it."
                    )
                }
            } else {
                buildString {
                    append("לא מצאתי תרגיל שמתאים ל־\"")
                    append(requestedName)
                    append("\".\n\n")
                    append(
                        "נסה לכתוב את שמו המלא של התרגיל " +
                                "או חלק מדויק יותר מהשם."
                    )
                }
            }
        }

        val bestScore =
            locations.first().score

        /*
         * שומרים תוצאות קרובות לתוצאה הטובה ביותר.
         * כך אותו תרגיל יכול להופיע בכמה חגורות,
         * בלי להציג התאמות חלשות ולא רלוונטיות.
         */
        val relevantLocations =
            locations
                .filter { location ->
                    location.score >= bestScore - 8
                }
                .take(12)

        val differentExerciseNames =
            relevantLocations
                .map { location ->
                    normalizeText(location.itemTitle)
                }
                .distinct()
                .size

        return buildString {
            if (isEnglish) {
                if (differentExerciseNames > 1) {
                    appendLine(
                        "I found several exercises matching " +
                                "\"$requestedName\":"
                    )
                } else {
                    appendLine(
                        "I found \"$requestedName\" in the " +
                                "following location:"
                    )
                }
            } else {
                if (differentExerciseNames > 1) {
                    appendLine(
                        "מצאתי כמה תרגילים שמתאימים ל־" +
                                "\"$requestedName\":"
                    )
                } else {
                    appendLine(
                        "מצאתי את \"$requestedName\" " +
                                "במיקום הבא:"
                    )
                }
            }

            appendLine()

            relevantLocations.forEachIndexed { index, location ->
                append(index + 1)
                append(". ")
                appendLine(location.itemTitle)

                if (isEnglish) {
                    append("   Belt: ")
                    appendLine(beltDisplayEn(location.belt))
                    append("   Topic: ")
                    appendLine(location.topicTitle)

                    location.subTopicTitle
                        ?.takeIf { it.isNotBlank() }
                        ?.let { subTopic ->
                            append("   Section: ")
                            appendLine(subTopic)
                        }
                } else {
                    append("   חגורה: ")
                    appendLine(beltDisplayHe(location.belt))
                    append("   נושא: ")
                    appendLine(location.topicTitle)

                    location.subTopicTitle
                        ?.takeIf { it.isNotBlank() }
                        ?.let { subTopic ->
                            append("   תת־נושא: ")
                            appendLine(subTopic)
                        }
                }

                if (index < relevantLocations.lastIndex) {
                    appendLine()
                }
            }

            appendLine()

            if (differentExerciseNames > 1) {
                if (isEnglish) {
                    append(
                        "Choose the exact exercise if you want " +
                                "to see its explanation."
                    )
                } else {
                    append(
                        "בחר את התרגיל המדויק אם תרצה לקבל עליו הסבר."
                    )
                }
            } else {
                if (isEnglish) {
                    append(
                        "You can now ask me to explain this exercise."
                    )
                } else {
                    append(
                        "אפשר עכשיו לבקש ממני הסבר על התרגיל."
                    )
                }
            }
        }.trim()
    }

    private fun buildMissingComparisonBeltAnswer(
        selectedBelts: List<Belt>,
        isEnglish: Boolean
    ): String {
        val selectedBelt =
            selectedBelts.firstOrNull()

        return if (isEnglish) {
            if (selectedBelt == null) {
                "Which two belts would you like to compare?\n\n" +
                        "For example: compare the green and blue belts."
            } else {
                "Which belt should I compare with " +
                        "${beltDisplayEn(selectedBelt)}?"
            }
        } else {
            if (selectedBelt == null) {
                "בין אילו שתי חגורות לבצע השוואה?\n\n" +
                        "לדוגמה: השווה בין חגורה ירוקה לחגורה כחולה."
            } else {
                "לאיזו חגורה להשוות את " +
                        beltDisplayHe(selectedBelt) +
                        "?"
            }
        }
    }

    private fun buildBeltComparisonAnswer(
        firstBelt: Belt,
        secondBelt: Belt,
        isEnglish: Boolean
    ): String {
        val firstTopics =
            ContentRepo
                .listTopicTitles(firstBelt)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy(::normalizeText)

        val secondTopics =
            ContentRepo
                .listTopicTitles(secondBelt)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy(::normalizeText)

        val firstItems =
            allMaterialItemsForBelt(firstBelt)

        val secondItems =
            allMaterialItemsForBelt(secondBelt)

        val firstNames =
            firstItems
                .map { item ->
                    normalizeText(item.itemTitle)
                }
                .toSet()

        val secondNames =
            secondItems
                .map { item ->
                    normalizeText(item.itemTitle)
                }
                .toSet()

        val uniqueToFirst =
            firstItems.filter { item ->
                normalizeText(item.itemTitle) !in secondNames
            }

        val uniqueToSecond =
            secondItems.filter { item ->
                normalizeText(item.itemTitle) !in firstNames
            }

        val commonCount =
            firstNames.intersect(secondNames).size

        fun appendItemList(
            items: List<BeltMaterialItem>,
            maxItems: Int = 12
        ): String {
            if (items.isEmpty()) {
                return if (isEnglish) {
                    "No unique exercises found."
                } else {
                    "לא נמצאו תרגילים ייחודיים."
                }
            }

            return buildString {
                items
                    .take(maxItems)
                    .forEachIndexed { index, item ->
                        append(index + 1)
                        append(". ")
                        append(item.itemTitle)
                        append(" — ")
                        appendLine(item.topicTitle)
                    }

                val remaining =
                    items.size - maxItems

                if (remaining > 0) {
                    if (isEnglish) {
                        append("And $remaining more exercises.")
                    } else {
                        append("ועוד $remaining תרגילים.")
                    }
                }
            }.trim()
        }

        return buildString {
            if (isEnglish) {
                appendLine(
                    "Comparison: ${beltDisplayEn(firstBelt)} " +
                            "and ${beltDisplayEn(secondBelt)}"
                )
                appendLine()

                appendLine(
                    "• ${beltDisplayEn(firstBelt)}: " +
                            "${firstTopics.size} topics, " +
                            "${firstItems.size} exercises"
                )
                appendLine(
                    "• ${beltDisplayEn(secondBelt)}: " +
                            "${secondTopics.size} topics, " +
                            "${secondItems.size} exercises"
                )
                appendLine("• Exercises with matching names: $commonCount")

                appendLine()
                appendLine(
                    "Unique to ${beltDisplayEn(firstBelt)}:"
                )
                appendLine(
                    appendItemList(uniqueToFirst)
                )

                appendLine()
                appendLine(
                    "Unique to ${beltDisplayEn(secondBelt)}:"
                )
                append(
                    appendItemList(uniqueToSecond)
                )
            } else {
                appendLine(
                    "השוואה בין ${beltDisplayHe(firstBelt)} " +
                            "ל${beltDisplayHe(secondBelt)}"
                )
                appendLine()

                appendLine(
                    "• ${beltDisplayHe(firstBelt)}: " +
                            "${firstTopics.size} נושאים, " +
                            "${firstItems.size} תרגילים"
                )
                appendLine(
                    "• ${beltDisplayHe(secondBelt)}: " +
                            "${secondTopics.size} נושאים, " +
                            "${secondItems.size} תרגילים"
                )
                appendLine(
                    "• תרגילים בעלי שם זהה: $commonCount"
                )

                appendLine()
                appendLine(
                    "ייחודי ל${beltDisplayHe(firstBelt)}:"
                )
                appendLine(
                    appendItemList(uniqueToFirst)
                )

                appendLine()
                appendLine(
                    "ייחודי ל${beltDisplayHe(secondBelt)}:"
                )
                append(
                    appendItemList(uniqueToSecond)
                )
            }
        }.trim()
    }

    /**
     * אוסף את כל תרגילי הנושא, כולל תתי־נושאים,
     * ומונע ספירה כפולה של אותו תרגיל.
     */
    private fun allItemsForTopic(
        belt: Belt,
        topicTitle: String
    ): List<String> {
        val directItems =
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = null
            )

        val subTopicItems =
            ContentRepo
                .listSubTopicTitles(
                    belt = belt,
                    topicTitle = topicTitle
                )
                .flatMap { subTopicTitle ->
                    ContentRepo.listItemTitles(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = subTopicTitle
                    )
                }

        return (directItems + subTopicItems)
            .map { item ->
                displayItemName(item)
            }
            .filter { item ->
                item.isNotBlank()
            }
            .distinctBy { item ->
                normalizeText(item)
            }
    }

    private fun buildMaterialCountAnswer(
        belt: Belt,
        topicMatch: TopicMatch?,
        subTopicMatch: SubTopicMatch?,
        isEnglish: Boolean
    ): String {
        val useSubTopic =
            subTopicMatch != null &&
                    subTopicMatch.score >=
                    STRONG_TOPIC_MATCH_SCORE &&
                    (
                            topicMatch == null ||
                                    subTopicMatch.score >
                                    topicMatch.score
                            )

        if (useSubTopic) {
            val selectedSubTopic =
                requireNotNull(subTopicMatch)

            val items =
                ContentRepo
                    .listItemTitles(
                        belt = belt,
                        topicTitle =
                            selectedSubTopic.topicTitle,
                        subTopicTitle =
                            selectedSubTopic.subTopicTitle
                    )
                    .map { item ->
                        displayItemName(item)
                    }
                    .filter { item ->
                        item.isNotBlank()
                    }
                    .distinctBy { item ->
                        normalizeText(item)
                    }

            return if (isEnglish) {
                buildString {
                    append("\"${selectedSubTopic.subTopicTitle}\" ")
                    append("in ${beltDisplayEn(belt)} contains ")
                    append("${items.size} exercises.")
                    append("\n\nMain topic: ")
                    append(selectedSubTopic.topicTitle)
                }
            } else {
                buildString {
                    append("בתת־הנושא \"")
                    append(selectedSubTopic.subTopicTitle)
                    append("\" ב")
                    append(beltDisplayHe(belt))
                    append(" יש ${items.size} תרגילים.")
                    append("\n\nנושא ראשי: ")
                    append(selectedSubTopic.topicTitle)
                }
            }
        }

        if (topicMatch != null) {
            val items =
                allItemsForTopic(
                    belt = belt,
                    topicTitle = topicMatch.title
                )

            val subTopicCount =
                ContentRepo
                    .listSubTopicTitles(
                        belt = belt,
                        topicTitle = topicMatch.title
                    )
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy(::normalizeText)
                    .size

            return if (isEnglish) {
                buildString {
                    append("\"${topicMatch.title}\" ")
                    append("in ${beltDisplayEn(belt)} contains ")
                    append("${items.size} exercises")

                    if (subTopicCount > 0) {
                        append(" across $subTopicCount sections")
                    }

                    append(".")
                }
            } else {
                buildString {
                    append("בנושא \"")
                    append(topicMatch.title)
                    append("\" ב")
                    append(beltDisplayHe(belt))
                    append(" יש ${items.size} תרגילים")

                    if (subTopicCount > 0) {
                        append(" המחולקים ל־$subTopicCount תתי־נושאים")
                    }

                    append(".")
                }
            }
        }

        val topics =
            ContentRepo
                .listTopicTitles(belt)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy(::normalizeText)

        val totalExercises =
            topics.sumOf { topicTitle ->
                allItemsForTopic(
                    belt = belt,
                    topicTitle = topicTitle
                ).size
            }

        return if (isEnglish) {
            buildString {
                append("${beltDisplayEn(belt)} contains ")
                append("${topics.size} topics and ")
                append("$totalExercises exercises.")
            }
        } else {
            buildString {
                append("ב")
                append(beltDisplayHe(belt))
                append(" יש ${topics.size} נושאים ו־")
                append("$totalExercises תרגילים.")
            }
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

    private fun buildFullBeltMaterialAnswer(
        belt: Belt,
        isEnglish: Boolean
    ): String {
        val topics =
            ContentRepo
                .listTopicTitles(belt)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy(::normalizeText)

        if (topics.isEmpty()) {
            return if (isEnglish) {
                "I couldn't find material for ${beltDisplayEn(belt)}."
            } else {
                "לא מצאתי חומר עבור ${beltDisplayHe(belt)}."
            }
        }

        val totalExercises =
            topics.sumOf { topicTitle ->
                allItemsForTopic(
                    belt = belt,
                    topicTitle = topicTitle
                ).size
            }

        return buildString {
            if (isEnglish) {
                appendLine(
                    "Full material for ${beltDisplayEn(belt)}"
                )
                appendLine(
                    "${topics.size} topics · $totalExercises exercises"
                )
            } else {
                appendLine(
                    "החומר המלא ל${beltDisplayHe(belt)}"
                )
                appendLine(
                    "${topics.size} נושאים · $totalExercises תרגילים"
                )
            }

            appendLine()

            topics.forEachIndexed { topicIndex, topicTitle ->
                val items =
                    allItemsForTopic(
                        belt = belt,
                        topicTitle = topicTitle
                    )

                append(topicIndex + 1)
                append(". ")
                append(topicTitle)
                append(" (${items.size})")
                appendLine()

                items.forEachIndexed { itemIndex, item ->
                    append("   ")
                    append(itemIndex + 1)
                    append(". ")
                    appendLine(item)
                }

                if (topicIndex < topics.lastIndex) {
                    appendLine()
                }
            }
        }.trim()
    }

    private fun buildSubTopicClarificationAnswer(
        belt: Belt,
        matches: List<SubTopicMatch>,
        isEnglish: Boolean
    ): String {
        val relevantMatches =
            matches
                .take(MAX_TOPIC_SUGGESTIONS)
                .distinctBy { match ->
                    listOf(
                        normalizeText(match.topicTitle),
                        normalizeText(match.subTopicTitle)
                    ).joinToString("|")
                }

        if (relevantMatches.isEmpty()) {
            return buildTopicsForBeltAnswer(
                belt = belt,
                isEnglish = isEnglish
            )
        }

        return buildString {
            if (isEnglish) {
                appendLine(
                    "I found several possible sections in " +
                            "${beltDisplayEn(belt)}."
                )
                appendLine("Which section did you mean?")
            } else {
                appendLine(
                    "מצאתי כמה תתי־נושאים אפשריים " +
                            "ב${beltDisplayHe(belt)}."
                )
                appendLine("לאיזה מהם התכוונת?")
            }

            appendLine()

            relevantMatches.forEachIndexed { index, match ->
                append(index + 1)
                append(". ")
                append(match.subTopicTitle)
                append(" — ")
                appendLine(match.topicTitle)
            }
        }.trim()
    }

    private fun buildSubTopicExercisesAnswer(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String,
        isEnglish: Boolean
    ): String {
        val items =
            ContentRepo
                .listItemTitles(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = subTopicTitle
                )
                .map { item ->
                    displayItemName(item)
                }
                .filter { item ->
                    item.isNotBlank()
                }
                .distinctBy { item ->
                    normalizeText(item)
                }

        if (items.isEmpty()) {
            return if (isEnglish) {
                "I found \"$subTopicTitle\" under " +
                        "\"$topicTitle\" in ${beltDisplayEn(belt)}, " +
                        "but no exercises were found inside it."
            } else {
                "מצאתי את תת־הנושא \"$subTopicTitle\" " +
                        "בתוך \"$topicTitle\" ב${beltDisplayHe(belt)}, " +
                        "אבל לא נמצאו בו תרגילים."
            }
        }

        return buildString {
            if (isEnglish) {
                appendLine(
                    "\"$subTopicTitle\" in ${beltDisplayEn(belt)}"
                )
                appendLine(
                    "Main topic: $topicTitle"
                )
                appendLine(
                    "${items.size} exercises found:"
                )
            } else {
                appendLine(
                    "\"$subTopicTitle\" ב${beltDisplayHe(belt)}"
                )
                appendLine(
                    "נושא ראשי: $topicTitle"
                )
                appendLine(
                    "נמצאו ${items.size} תרגילים:"
                )
            }

            appendLine()

            items.forEachIndexed { index, item ->
                appendLine("${index + 1}. $item")
            }

            appendLine()

            if (isEnglish) {
                append(
                    "You can ask me to explain any exercise " +
                            "from this list."
                )
            } else {
                append(
                    "אפשר לבקש ממני הסבר על כל תרגיל מהרשימה."
                )
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