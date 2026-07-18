package il.kmi.app.ui.assistant.core

import il.kmi.shared.domain.Belt

/**
 * תוצאת ניתוח הבקשה לפני הפנייה למאגר הידע.
 */
data class AssistantIntentResolution(
    val originalQuestion: String,
    val normalizedQuestion: String,
    val resolvedQuestion: String,

    val intent: AssistantIntent,
    val source: AssistantKnowledgeSource,
    val confidence: Float,

    val exerciseName: String? = null,
    val topicName: String? = null,
    val belt: Belt? = null,

    val isFollowUp: Boolean = false,
    val alternatives: List<AssistantIntent> = emptyList(),
    val requiresClarification: Boolean = false
)

/**
 * מזהה אוטומטית את מטרת הבקשה ואת הישויות שנאמרו בה.
 *
 * המנוע מקומי ואינו דורש חיבור רשת. הוא אינו מחליף את
 * מאגר הידע אלא מחליט לאיזה מקור מידע להעביר את הבקשה.
 */
object AssistantIntentResolver {

    fun resolve(
        question: String,
        context: AssistantConversationContext =
            AssistantConversationContext()
    ): AssistantIntentResolution {
        val original = question.trim()
        val normalized = normalize(original)

        if (normalized.isBlank()) {
            return AssistantIntentResolution(
                originalQuestion = original,
                normalizedQuestion = normalized,
                resolvedQuestion = original,
                intent = AssistantIntent.UNKNOWN,
                source = AssistantKnowledgeSource.UNKNOWN,
                confidence = 0f,
                requiresClarification = true
            )
        }

        val detectedBelt = detectBelt(normalized)
        val isFollowUp = detectFollowUp(normalized)

        val scores = linkedMapOf(
            AssistantIntent.EXPLAIN_EXERCISE to
                    scoreExplainExercise(normalized),

            AssistantIntent.SEARCH_EXERCISE to
                    scoreSearchExercise(normalized),

            AssistantIntent.LIST_EXERCISES to
                    scoreListExercises(normalized),

            AssistantIntent.SEARCH_MATERIAL to
                    scoreSearchMaterial(normalized),

            AssistantIntent.LIST_TOPICS to
                    scoreListTopics(normalized),

            AssistantIntent.NEXT_TRAINING to
                    scoreNextTraining(normalized),

            AssistantIntent.LIST_TRAININGS to
                    scoreListTrainings(normalized),

            AssistantIntent.USER_TRAINING_DETAILS to
                    scoreTrainingDetails(normalized),

            AssistantIntent.NAVIGATION to
                    scoreNavigation(normalized)
        )

        applyContextScores(
            scores = scores,
            normalizedQuestion = normalized,
            context = context,
            isFollowUp = isFollowUp
        )

        val ranked = scores.entries
            .filter { it.value > 0f }
            .sortedByDescending { it.value }

        val best = ranked.firstOrNull()
        val second = ranked.getOrNull(1)

        val bestIntent = best?.key
            ?: context.intent
            ?: AssistantIntent.UNKNOWN

        val bestScore = best?.value ?: 0f
        val secondScore = second?.value ?: 0f

        val confidence = calculateConfidence(
            bestScore = bestScore,
            secondScore = secondScore,
            usedContext = isFollowUp && context.intent != null
        )

        val alternatives = ranked
            .drop(1)
            .take(2)
            .map { it.key }

        val exerciseName = extractExerciseName(
            originalQuestion = original,
            normalizedQuestion = normalized,
            intent = bestIntent,
            context = context,
            isFollowUp = isFollowUp
        )

        val topicName = extractTopicName(
            originalQuestion = original,
            normalizedQuestion = normalized,
            intent = bestIntent,
            context = context,
            isFollowUp = isFollowUp
        )

        val resolvedQuestion = buildResolvedQuestion(
            originalQuestion = original,
            intent = bestIntent,
            exerciseName = exerciseName,
            topicName = topicName,
            belt = detectedBelt ?: context.belt,
            context = context,
            isFollowUp = isFollowUp
        )

        val requiresClarification =
            bestIntent == AssistantIntent.UNKNOWN ||
                    confidence < MIN_CONFIDENCE ||
                    (
                            second != null &&
                                    bestScore - secondScore <
                                    MIN_SCORE_DIFFERENCE
                            )

        return AssistantIntentResolution(
            originalQuestion = original,
            normalizedQuestion = normalized,
            resolvedQuestion = resolvedQuestion,
            intent = bestIntent,
            source = sourceForIntent(bestIntent),
            confidence = confidence,
            exerciseName = exerciseName,
            topicName = topicName,
            belt = detectedBelt ?: context.belt,
            isFollowUp = isFollowUp,
            alternatives = alternatives,
            requiresClarification = requiresClarification
        )
    }

    private fun scoreExplainExercise(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "תסביר" to 4f,
                "הסבר" to 4f,
                "איך עושים" to 4f,
                "איך מבצעים" to 4f,
                "עמידת מוצא" to 3f,
                "שלבי ביצוע" to 3f,
                "explain" to 4f,
                "how to do" to 4f,
                "how do i" to 3f,
                "starting position" to 3f,
                "execution steps" to 3f
            )
        )

        if (containsExerciseVocabulary(text)) {
            score += 3f
        }

        if (containsListVocabulary(text)) {
            score -= 2f
        }

        return score.coerceAtLeast(0f)
    }

    private fun scoreSearchExercise(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "חפש תרגיל" to 5f,
                "מצא תרגיל" to 5f,
                "תרגיל דומה" to 4f,
                "תרגילים דומים" to 4f,
                "איפה נמצא התרגיל" to 4f,
                "find exercise" to 5f,
                "search exercise" to 5f,
                "similar exercise" to 4f,
                "similar exercises" to 4f
            )
        )

        if (containsExerciseVocabulary(text)) {
            score += 2f
        }

        return score
    }

    private fun scoreListExercises(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "רשימת תרגילים" to 6f,
                "כל התרגילים" to 6f,
                "איזה תרגילים" to 5f,
                "תרגילים בחגורה" to 5f,
                "הצג תרגילים" to 5f,
                "list exercises" to 6f,
                "all exercises" to 6f,
                "which exercises" to 5f,
                "exercises in" to 4f,
                "show exercises" to 5f
            )
        )

        if (containsListVocabulary(text) && "תרגיל" in text) {
            score += 3f
        }

        if (containsListVocabulary(text) && "exercise" in text) {
            score += 3f
        }

        return score
    }

    private fun scoreSearchMaterial(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "חומר קמי" to 6f,
                "בחומר" to 4f,
                "חפש בנושא" to 4f,
                "תת נושא" to 4f,
                "לפי חגורה" to 3f,
                "kami material" to 6f,
                "kmi material" to 6f,
                "search material" to 5f,
                "sub topic" to 4f,
                "by belt" to 3f
            )
        )

        if (detectBelt(text) != null) {
            score += 2f
        }

        if (containsMaterialVocabulary(text)) {
            score += 2f
        }

        return score
    }

    private fun scoreListTopics(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "רשימת נושאים" to 6f,
                "כל הנושאים" to 6f,
                "איזה נושאים" to 5f,
                "הצג נושאים" to 5f,
                "נושאים בחגורה" to 5f,
                "topic list" to 6f,
                "all topics" to 6f,
                "which topics" to 5f,
                "show topics" to 5f,
                "topics in" to 4f
            )
        )

        return score
    }

    private fun scoreNextTraining(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "האימון הבא" to 7f,
                "אימון הבא" to 7f,
                "האימון הקרוב" to 7f,
                "מתי האימון" to 5f,
                "מתי אני מתאמן" to 5f,
                "next training" to 7f,
                "next workout" to 7f,
                "upcoming training" to 6f,
                "when is my training" to 5f,
                "when do i train" to 5f
            )
        )

        return score
    }

    private fun scoreListTrainings(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "רשימת אימונים" to 7f,
                "כל האימונים" to 7f,
                "אימוני השבוע" to 7f,
                "אימונים השבוע" to 7f,
                "אימונים קרובים" to 6f,
                "הצג אימונים" to 6f,
                "training list" to 7f,
                "all trainings" to 7f,
                "this week's trainings" to 7f,
                "trainings this week" to 7f,
                "upcoming trainings" to 6f,
                "show trainings" to 6f
            )
        )

        return score
    }

    private fun scoreTrainingDetails(text: String): Float {
        var score = 0f

        score += keywordScore(
            text,
            mapOf(
                "הקבוצה שלי" to 6f,
                "הסניף שלי" to 6f,
                "המאמן שלי" to 6f,
                "פרטי הקבוצה" to 6f,
                "פרטי האימון" to 5f,
                "באיזו קבוצה" to 5f,
                "באיזה סניף" to 5f,
                "my group" to 6f,
                "my branch" to 6f,
                "my coach" to 6f,
                "group details" to 6f,
                "training details" to 5f,
                "which group" to 5f,
                "which branch" to 5f
            )
        )

        return score
    }

    private fun scoreNavigation(text: String): Float {
        return keywordScore(
            text,
            mapOf(
                "חזור למסך הבית" to 8f,
                "פתח מסך הבית" to 8f,
                "עבור למסך" to 6f,
                "פתח אימון" to 6f,
                "התרגיל הבא" to 6f,
                "go home" to 8f,
                "open home" to 8f,
                "home screen" to 7f,
                "go to screen" to 6f,
                "open training" to 6f,
                "next exercise" to 6f
            )
        )
    }

    private fun applyContextScores(
        scores: MutableMap<AssistantIntent, Float>,
        normalizedQuestion: String,
        context: AssistantConversationContext,
        isFollowUp: Boolean
    ) {
        if (!isFollowUp) return

        context.intent?.let { previousIntent ->
            scores[previousIntent] =
                scores.getValue(previousIntent) + 3.5f
        }

        if (
            !context.exerciseName.isNullOrBlank() &&
            containsAny(
                normalizedQuestion,
                listOf(
                    "אותו",
                    "אותה",
                    "עליו",
                    "עליה",
                    "התרגיל",
                    "תסביר יותר",
                    "again",
                    "it",
                    "this exercise",
                    "explain more"
                )
            )
        ) {
            scores[AssistantIntent.EXPLAIN_EXERCISE] =
                scores.getValue(
                    AssistantIntent.EXPLAIN_EXERCISE
                ) + 5f
        }

        if (
            context.source == AssistantKnowledgeSource.TRAININGS &&
            containsAny(
                normalizedQuestion,
                listOf(
                    "הבא",
                    "אחריו",
                    "השבוע",
                    "מתי",
                    "next",
                    "after that",
                    "this week",
                    "when"
                )
            )
        ) {
            scores[AssistantIntent.NEXT_TRAINING] =
                scores.getValue(
                    AssistantIntent.NEXT_TRAINING
                ) + 4f
        }

        if (
            context.source == AssistantKnowledgeSource.MATERIAL &&
            containsAny(
                normalizedQuestion,
                listOf(
                    "עוד",
                    "גם",
                    "בחגורה",
                    "בנושא",
                    "more",
                    "also",
                    "belt",
                    "topic"
                )
            )
        ) {
            scores[AssistantIntent.SEARCH_MATERIAL] =
                scores.getValue(
                    AssistantIntent.SEARCH_MATERIAL
                ) + 4f
        }
    }

    private fun detectFollowUp(text: String): Boolean {
        return containsAny(
            text,
            listOf(
                "אותו",
                "אותה",
                "אותם",
                "אותן",
                "עליו",
                "עליה",
                "זה",
                "זאת",
                "הזה",
                "הזאת",
                "ומה עוד",
                "מה עוד",
                "ומה לגבי",
                "ומה עם",
                "תסביר יותר",
                "תן עוד פרטים",
                "גם בחגורה",
                "ואחריו",
                "שוב",
                "it",
                "this",
                "that",
                "same",
                "again",
                "what else",
                "what about",
                "explain more",
                "more details",
                "also",
                "after that"
            )
        )
    }

    private fun extractExerciseName(
        originalQuestion: String,
        normalizedQuestion: String,
        intent: AssistantIntent,
        context: AssistantConversationContext,
        isFollowUp: Boolean
    ): String? {
        val exerciseIntent =
            intent == AssistantIntent.EXPLAIN_EXERCISE ||
                    intent == AssistantIntent.SEARCH_EXERCISE

        if (!exerciseIntent) {
            return if (isFollowUp) context.exerciseName else null
        }

        if (
            isFollowUp &&
            !context.exerciseName.isNullOrBlank() &&
            !containsExerciseVocabulary(normalizedQuestion)
        ) {
            return context.exerciseName
        }

        val prefixes = listOf(
            "תן הסבר מפורט יותר על",
            "תן הסבר על",
            "תן הסבר ל",
            "תסביר לי את",
            "תסביר לי על",
            "תסביר לי",
            "תסביר את",
            "תסביר על",
            "תסביר",
            "איך עושים את",
            "איך עושים",
            "איך מבצעים את",
            "איך מבצעים",
            "מה זה",
            "חפש תרגיל",
            "מצא תרגיל",
            "explain in more detail",
            "explain the",
            "explain",
            "how to do the",
            "how to do",
            "find exercise",
            "search exercise",
            "what is"
        )

        var cleaned = originalQuestion.trim()

        prefixes.forEach { prefix ->
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned
                    .substring(prefix.length)
                    .trim()
            }
        }

        cleaned = cleaned
            .removePrefix("את ")
            .removePrefix("על ")
            .trim()
            .trimEnd('?', '!', '.', ',')

        return cleaned.takeIf {
            it.length >= MIN_ENTITY_LENGTH
        } ?: context.exerciseName
    }

    private fun extractTopicName(
        originalQuestion: String,
        normalizedQuestion: String,
        intent: AssistantIntent,
        context: AssistantConversationContext,
        isFollowUp: Boolean
    ): String? {
        val materialIntent =
            intent == AssistantIntent.SEARCH_MATERIAL ||
                    intent == AssistantIntent.LIST_TOPICS ||
                    intent == AssistantIntent.LIST_EXERCISES

        if (!materialIntent) {
            return if (isFollowUp) context.topicName else null
        }

        val knownTopic = TOPIC_ALIASES.entries
            .firstOrNull { (alias, _) ->
                alias in normalizedQuestion
            }
            ?.value

        if (!knownTopic.isNullOrBlank()) {
            return knownTopic
        }

        if (isFollowUp && !context.topicName.isNullOrBlank()) {
            return context.topicName
        }

        val cleaned = originalQuestion
            .replace(
                Regex(
                    pattern = "(?i)הצג|חפש|רשימת|כל|נושא|תרגילים|לפי חגורה|show|search|list|all|topic|exercises|by belt"
                ),
                replacement = " "
            )
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('?', '!', '.', ',')

        return cleaned.takeIf {
            it.length >= MIN_ENTITY_LENGTH
        }
    }

    private fun buildResolvedQuestion(
        originalQuestion: String,
        intent: AssistantIntent,
        exerciseName: String?,
        topicName: String?,
        belt: Belt?,
        context: AssistantConversationContext,
        isFollowUp: Boolean
    ): String {
        if (!isFollowUp || !context.hasConversationSubject()) {
            return originalQuestion
        }

        val parts = mutableListOf<String>()

        when (intent) {
            AssistantIntent.EXPLAIN_EXERCISE,
            AssistantIntent.SEARCH_EXERCISE -> {
                exerciseName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { parts += it }
            }

            AssistantIntent.SEARCH_MATERIAL,
            AssistantIntent.LIST_EXERCISES,
            AssistantIntent.LIST_TOPICS -> {
                topicName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { parts += it }

                belt?.let {
                    parts += beltSearchName(it)
                }
            }

            else -> Unit
        }

        parts += originalQuestion

        return parts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(". ")
    }

    private fun calculateConfidence(
        bestScore: Float,
        secondScore: Float,
        usedContext: Boolean
    ): Float {
        if (bestScore <= 0f) {
            return if (usedContext) 0.45f else 0f
        }

        val base = (bestScore / 10f).coerceIn(0f, 0.90f)
        val separation =
            ((bestScore - secondScore) / 10f)
                .coerceIn(0f, 0.10f)

        val contextBonus = if (usedContext) 0.05f else 0f

        return (base + separation + contextBonus)
            .coerceIn(0f, 1f)
    }

    private fun detectBelt(text: String): Belt? {
        return when {
            containsAny(
                text,
                listOf(
                    "חגורה לבנה",
                    "חגורה לבן",
                    "white belt"
                )
            ) -> Belt.WHITE

            containsAny(
                text,
                listOf(
                    "חגורה צהובה",
                    "חגורה צהוב",
                    "yellow belt"
                )
            ) -> Belt.YELLOW

            containsAny(
                text,
                listOf(
                    "חגורה כתומה",
                    "חגורה כתום",
                    "orange belt"
                )
            ) -> Belt.ORANGE

            containsAny(
                text,
                listOf(
                    "חגורה ירוקה",
                    "חגורה ירוק",
                    "green belt"
                )
            ) -> Belt.GREEN

            containsAny(
                text,
                listOf(
                    "חגורה כחולה",
                    "חגורה כחול",
                    "blue belt"
                )
            ) -> Belt.BLUE

            containsAny(
                text,
                listOf(
                    "חגורה חומה",
                    "חגורה חום",
                    "brown belt"
                )
            ) -> Belt.BROWN

            containsAny(
                text,
                listOf(
                    "חגורה שחורה",
                    "חגורה שחור",
                    "black belt"
                )
            ) -> Belt.BLACK

            else -> null
        }
    }

    private fun containsExerciseVocabulary(
        text: String
    ): Boolean {
        return containsAny(
            text,
            listOf(
                "תרגיל",
                "בעיטה",
                "אגרוף",
                "הגנה",
                "דקירה",
                "חניקה",
                "שחרור",
                "מגל",
                "סטירה",
                "מרפק",
                "ברך",
                "exercise",
                "kick",
                "punch",
                "defense",
                "defence",
                "stab",
                "choke",
                "release",
                "roundhouse",
                "elbow",
                "knee"
            )
        )
    }

    private fun containsMaterialVocabulary(
        text: String
    ): Boolean {
        return containsAny(
            text,
            listOf(
                "חומר",
                "נושא",
                "תת נושא",
                "חגורה",
                "הגנות פנימיות",
                "הגנות חיצוניות",
                "שחרורים",
                "בלימות",
                "גלגולים",
                "material",
                "topic",
                "sub topic",
                "belt",
                "inside defenses",
                "outside defenses",
                "releases",
                "breakfalls",
                "rolls"
            )
        )
    }

    private fun containsListVocabulary(
        text: String
    ): Boolean {
        return containsAny(
            text,
            listOf(
                "רשימה",
                "רשימת",
                "כולם",
                "כל ה",
                "איזה",
                "הצג",
                "תראה",
                "list",
                "all",
                "which",
                "show"
            )
        )
    }

    private fun keywordScore(
        text: String,
        keywords: Map<String, Float>
    ): Float {
        return keywords.entries.sumOf { (keyword, weight) ->
            if (keyword in text) {
                weight.toDouble()
            } else {
                0.0
            }
        }.toFloat()
    }

    private fun containsAny(
        text: String,
        values: List<String>
    ): Boolean {
        return values.any { value ->
            value in text
        }
    }

    private fun sourceForIntent(
        intent: AssistantIntent
    ): AssistantKnowledgeSource {
        return when (intent) {
            AssistantIntent.EXERCISE,
            AssistantIntent.EXPLAIN_EXERCISE,
            AssistantIntent.SEARCH_EXERCISE ->
                AssistantKnowledgeSource.EXERCISES

            AssistantIntent.MATERIAL,
            AssistantIntent.LIST_EXERCISES,
            AssistantIntent.SEARCH_MATERIAL,
            AssistantIntent.LIST_TOPICS ->
                AssistantKnowledgeSource.MATERIAL

            AssistantIntent.TRAININGS,
            AssistantIntent.NEXT_TRAINING,
            AssistantIntent.LIST_TRAININGS,
            AssistantIntent.USER_TRAINING_DETAILS ->
                AssistantKnowledgeSource.TRAININGS

            AssistantIntent.NAVIGATION ->
                AssistantKnowledgeSource.NAVIGATION

            AssistantIntent.UNKNOWN ->
                AssistantKnowledgeSource.UNKNOWN
        }
    }

    private fun beltSearchName(belt: Belt): String {
        return when (belt) {
            Belt.WHITE -> "חגורה לבנה"
            Belt.YELLOW -> "חגורה צהובה"
            Belt.ORANGE -> "חגורה כתומה"
            Belt.GREEN -> "חגורה ירוקה"
            Belt.BLUE -> "חגורה כחולה"
            Belt.BROWN -> "חגורה חומה"
            Belt.BLACK -> "חגורה שחורה"
            else -> belt.toString()
        }
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace("ק.מ.י", "קמי")
            .replace("ק מ י", "קמי")
            .replace("k.a.m.i", "kami")
            .replace("k m i", "kami")
            .replace("־", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("[?!,.:;\"'`()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val TOPIC_ALIASES = linkedMapOf(
        "הגנות פנימיות" to "הגנות פנימיות",
        "הגנה פנימית" to "הגנות פנימיות",
        "inside defenses" to "הגנות פנימיות",
        "inside defence" to "הגנות פנימיות",

        "הגנות חיצוניות" to "הגנות חיצוניות",
        "הגנה חיצונית" to "הגנות חיצוניות",
        "outside defenses" to "הגנות חיצוניות",
        "outside defence" to "הגנות חיצוניות",

        "שחרורים" to "שחרורים",
        "שחרור" to "שחרורים",
        "releases" to "שחרורים",
        "release" to "שחרורים",

        "בלימות וגלגולים" to "בלימות וגלגולים",
        "בלימות" to "בלימות וגלגולים",
        "גלגולים" to "בלימות וגלגולים",
        "breakfalls and rolls" to "בלימות וגלגולים",
        "breakfalls" to "בלימות וגלגולים",
        "rolls" to "בלימות וגלגולים"
    )

    private const val MIN_CONFIDENCE = 0.42f
    private const val MIN_SCORE_DIFFERENCE = 1.25f
    private const val MIN_ENTITY_LENGTH = 2
}