package il.kmi.app.ui.assistant.core

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import il.kmi.app.domain.ExplanationSearchIndex
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import kotlinx.coroutines.tasks.await

/**
 * הודעה קודמת בשיחה שנשלחת למנוע המרוחק.
 *
 * role חייב להיות:
 * - user
 * - assistant
 */
data class RemoteAssistantMessage(
    val role: String,
    val text: String
)

/**
 * תשובה מוצלחת ממנוע ה־AI.
 */
data class RemoteAssistantAnswer(
    val title: String?,
    val text: String,
    val needsClarification: Boolean,
    val followUpQuestion: String?,
    val suggestedAction: String?,
    val model: String?,
    val requestCostMicros: Long,
    val spentMicros: Long,
    val remainingMicros: Long,
    val monthlyLimitMicros: Long
)

/**
 * תוצאת הקריאה למנוע המרוחק.
 *
 * Success:
 * התקבלה תשובת AI תקינה.
 *
 * Fallback:
 * יש להשתמש ב־AssistantBrain המקומי.
 */
sealed interface RemoteAssistantResult {

    data class Success(
        val answer: RemoteAssistantAnswer
    ) : RemoteAssistantResult

    data class Fallback(
        val reason: String,
        val localAnswer: String
    ) : RemoteAssistantResult
}

/**
 * שכבת התקשורת המאובטחת עם kmiAiAssistant.
 *
 * מפתח OpenAI אינו נמצא באפליקציה.
 * האפליקציה פונה ל־Firebase Callable Function בלבד.
 */
object RemoteAssistantEngine {

    private const val FUNCTION_NAME =
        "kmiAiAssistant"

    private const val MAX_HISTORY_ITEMS =
        12

    private const val MAX_HISTORY_TEXT_LENGTH =
        2_000

    private const val MAX_PROFILE_LENGTH =
        4_000

    private const val MAX_KNOWLEDGE_CONTEXT_LENGTH =
        24_000

    private const val MAX_KNOWLEDGE_MATCHES =
        6

    private const val MAX_EXPLANATION_LENGTH =
        1_800

    private const val MAX_CONTEXTUAL_USER_QUERIES =
        3

    private const val MAX_MATERIAL_SECTIONS =
        12

    private const val MAX_MATERIAL_ITEMS_PER_SECTION =
        18

    private data class MaterialKnowledgeSection(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val nestedSubTopicTitle: String?,
        val items: List<String>,
        val score: Int
    )

    private val firebaseFunctions:
            FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance()
    }

    private val firebaseAuth:
            FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    /**
     * מנסה לקבל תשובת AI.
     *
     * בכל תקלה, חוסר מנוי או חריגה מהמכסה,
     * מוחזרת תשובת fallback מהמנוע המקומי.
     */
    suspend fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean,
        conversationHistory:
        List<RemoteAssistantMessage> =
            emptyList(),
        additionalUserProfile: String = "",
        verifiedLocalAnswer: String? = null
    ): RemoteAssistantResult {
        val cleanQuestion =
            question
                .trim()
                .replace(
                    Regex("""\s+"""),
                    " "
                )

        val localAnswer =
            verifiedLocalAnswer
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: AssistantBrain.answer(
                    question = cleanQuestion,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )

        if (cleanQuestion.length < 2) {
            return RemoteAssistantResult.Fallback(
                reason = "question_too_short",
                localAnswer = localAnswer
            )
        }

        /*
         * רשימת תרגילים היא תוצאה דטרמיניסטית שמגיעה
         * מהמאגרים המאומתים של האפליקציה.
         *
         * אין לשלוח אותה למודל המרוחק, שעלול לפרש
         * את שם המשפחה כשם של תרגיל יחיד או להשמיט
         * חלק מהתרגילים שנמצאו.
         */
        if (isVerifiedExerciseListRequest(cleanQuestion)) {
            return RemoteAssistantResult.Fallback(
                reason = "verified_local_exercise_list",
                localAnswer = localAnswer
            )
        }

        /*
         * Callable Functions דורשות משתמש מחובר.
         * במקרה שאין Firebase User ממשיכים מיד
         * עם העוזר המקומי.
         */
        if (firebaseAuth.currentUser == null) {
            return RemoteAssistantResult.Fallback(
                reason = "firebase_user_missing",
                localAnswer = localAnswer
            )
        }

        val cleanHistory =
            conversationHistory
                .takeLast(
                    MAX_HISTORY_ITEMS
                )
                .mapNotNull { message ->
                    val role =
                        when (
                            message.role
                                .trim()
                                .lowercase()
                        ) {
                            "assistant" ->
                                "assistant"

                            "user" ->
                                "user"

                            else ->
                                return@mapNotNull null
                        }

                    val text =
                        message.text
                            .trim()
                            .replace(
                                Regex("""\s+"""),
                                " "
                            )
                            .take(
                                MAX_HISTORY_TEXT_LENGTH
                            )

                    if (text.isBlank()) {
                        null
                    } else {
                        mapOf(
                            "role" to role,
                            "text" to text
                        )
                    }
                }

        val userProfile =
            buildUserProfile(
                preferredBelt =
                    preferredBelt,
                additionalUserProfile =
                    additionalUserProfile,
                isEnglish =
                    isEnglish
            )

        /*
         * בונים הקשר מאומת גם מהשאלה הנוכחית וגם
         * משאלות קודמות של המשתמש.
         *
         * כך שאלות המשך כגון "ומה הטעות בתרגיל הזה?"
         * יכולות להסתמך על התרגיל שהוזכר קודם.
         */
        val knowledgeContext =
            buildKnowledgeContext(
                question =
                    cleanQuestion,
                preferredBelt =
                    preferredBelt,
                conversationHistory =
                    conversationHistory,
                localAnswer =
                    localAnswer
            )

        val requestData =
            hashMapOf<String, Any>(
                "question" to
                        cleanQuestion,

                "isEnglish" to
                        isEnglish,

                "userProfile" to
                        userProfile,

                "knowledgeContext" to
                        knowledgeContext,

                "conversationHistory" to
                        cleanHistory
            )

        return try {
            val callableResult =
                firebaseFunctions
                    .getHttpsCallable(
                        FUNCTION_NAME
                    )
                    .call(
                        requestData
                    )
                    .await()

            parseResponse(
                rawData =
                    callableResult.data,
                localAnswer =
                    localAnswer
            )
        } catch (_: Throwable) {
            /*
             * אין מציגים למשתמש תקלה טכנית.
             * העוזר המקומי ממשיך לעבוד כרגיל.
             */
            RemoteAssistantResult.Fallback(
                reason =
                    "remote_call_failed",
                localAnswer =
                    localAnswer
            )
        }
    }

    private fun focusKnowledgeMatches(
        question: String,
        matches: List<
                Pair<
                        ExplanationSearchIndex.Match,
                        String
                        >
                >
    ): List<
            Pair<
                    ExplanationSearchIndex.Match,
                    String
                    >
            > {
        if (matches.size <= 1) {
            return matches
        }

        val questionRoots =
            assistantSemanticRoots(
                question
            )

        val titleRoots =
            matches.associateWith { match ->
                assistantSemanticRoots(
                    match.first.title
                )
            }

        val frequencies =
            questionRoots.mapNotNull { root ->
                val count =
                    titleRoots.values.count { roots ->
                        root in roots
                    }

                if (count > 0) {
                    root to count
                } else {
                    null
                }
            }

        if (frequencies.isEmpty()) {
            return matches
        }

        val lowestFrequency =
            frequencies.minOf { (_, count) ->
                count
            }

        val distinctiveRoots =
            frequencies
                .filter { (_, count) ->
                    count == lowestFrequency
                }
                .map { (root, _) ->
                    root
                }
                .toSet()

        val focused =
            matches.filter { match ->
                val roots =
                    titleRoots[match]
                        .orEmpty()

                distinctiveRoots.any { root ->
                    root in roots
                }
            }

        return focused
            .takeIf { it.isNotEmpty() }
            ?: matches
    }

    private fun assistantSemanticRoots(
        value: String
    ): Set<String> {
        return value
            .lowercase()
            .replace("־", " ")
            .replace("–", " ")
            .replace("—", " ")
            .replace("-", " ")
            .replace(
                Regex("[^א-תa-z0-9\\s]"),
                " "
            )
            .split(Regex("\\s+"))
            .mapNotNull { token ->
                assistantSemanticRoot(token)
                    .takeIf { root ->
                        root.length >= 3
                    }
            }
            .toSet()
    }

    private fun assistantSemanticRoot(
        value: String
    ): String {
        var root =
            value
                .lowercase()
                .trim()

        root =
            when {
                root.length > 4 &&
                        root.endsWith("ות") ->
                    root.dropLast(2)

                root.length > 4 &&
                        root.endsWith("ים") ->
                    root.dropLast(2)

                else ->
                    root
            }

        root =
            when {
                root.length > 4 &&
                        root.endsWith("ה") ->
                    root.dropLast(1)

                root.length > 4 &&
                        root.endsWith("ת") ->
                    root.dropLast(1)

                else ->
                    root
            }

        return root
    }

    /**
     * בונה ידע מאומת עבור המודל מתוך מאגרי האפליקציה.
     *
     * החיפוש מתבצע בנפרד עבור השאלה הנוכחית ועבור
     * שאלות המשתמש האחרונות. אין כאן רשימות קשיחות
     * של שמות תרגילים או משפחות תרגילים.
     */
    private fun buildKnowledgeContext(
        question: String,
        preferredBelt: Belt?,
        conversationHistory:
        List<RemoteAssistantMessage>,
        localAnswer: String
    ): String {
        val contextualQueries =
            buildList {
                add(question)

                conversationHistory
                    .asReversed()
                    .asSequence()
                    .filter { message ->
                        message.role
                            .trim()
                            .equals(
                                other = "user",
                                ignoreCase = true
                            )
                    }
                    .map { message ->
                        message.text
                            .trim()
                            .replace(
                                Regex("""\s+"""),
                                " "
                            )
                    }
                    .filter { previousQuestion ->
                        previousQuestion.length >= 2
                    }
                    .filterNot { previousQuestion ->
                        previousQuestion.equals(
                            other = question,
                            ignoreCase = true
                        )
                    }
                    .distinct()
                    .take(
                        MAX_CONTEXTUAL_USER_QUERIES
                    )
                    .forEach(::add)
            }

        /*
         * אותו תרגיל עשוי להתאים לכמה שאלות או כינויים.
         * מאחדים אותו לפי חגורה וכותרת ושומרים את
         * ההתאמה בעלת הציון הגבוה ביותר.
         */
        val matchesByExercise =
            linkedMapOf<
                    String,
                    Pair<
                            ExplanationSearchIndex.Match,
                            String
                            >
                    >()

        contextualQueries.forEach { sourceQuery ->
            val matches =
                runCatching {
                    ExplanationSearchIndex.findMatches(
                        query =
                            sourceQuery,
                        preferredBelt =
                            preferredBelt,
                        minScore =
                            55,
                        maxItems =
                            MAX_KNOWLEDGE_MATCHES
                    )
                }
                    .getOrElse {
                        emptyList()
                    }

            matches.forEach { match ->
                val key =
                    buildString {
                        append(
                            match.belt.id
                        )
                        append("::")
                        append(
                            match.title
                                .trim()
                                .lowercase()
                        )
                    }

                val existing =
                    matchesByExercise[key]

                if (
                    existing == null ||
                    match.score >
                    existing.first.score
                ) {
                    matchesByExercise[key] =
                        match to sourceQuery
                }
            }
        }

        val rankedMatches =
            matchesByExercise
                .values
                .sortedWith(
                    compareByDescending<
                            Pair<
                                    ExplanationSearchIndex.Match,
                                    String
                                    >
                            > {
                        it.first.score
                    }
                        .thenBy {
                            it.first.title.length
                        }
                )

        /*
         * בשאלה הנוכחית נותנים עדיפות למונח שמבדיל
         * בין משפחות התרגילים שנמצאו.
         *
         * כך "הגנות נגד בעיטות" לא מתערבבות עם
         * תוצאות שחולקות רק את המילים "הגנות נגד".
         */
        val selectedMatches =
            focusKnowledgeMatches(
                question = question,
                matches = rankedMatches
            )
                .take(
                    MAX_KNOWLEDGE_MATCHES
                )

        val materialSections =
            findRelevantMaterialSections(
                contextualQueries =
                    contextualQueries,
                preferredBelt =
                    preferredBelt
            )

        return buildString {
            appendLine(
                "GROUNDING_RULES:"
            )
            appendLine(
                "- The following data comes from the app's verified repositories."
            )
            appendLine(
                "- Prefer verified exercise data over assumptions."
            )
            appendLine(
                "- A local not-found result does not cancel verified matches listed below."
            )
            appendLine(
                "- If several matches are plausible, ask one concise clarification question."
            )
            appendLine(
                "- Never invent an exercise title or technical explanation."
            )

            appendLine()
            appendLine(
                "CURRENT_USER_QUESTION:"
            )
            appendLine(
                question
            )

            if (
                contextualQueries.size > 1
            ) {
                appendLine()
                appendLine(
                    "RECENT_USER_CONTEXT:"
                )

                contextualQueries
                    .drop(1)
                    .forEachIndexed { index,
                                      previousQuestion ->

                        appendLine(
                            "${index + 1}. $previousQuestion"
                        )
                    }
            }

            appendLine()
            appendLine(
                "LOCAL_ENGINE_RESULT:"
            )
            appendLine(
                localAnswer
            )

            appendLine()
            appendLine(
                "VERIFIED_EXERCISE_MATCHES:"
            )

            if (
                selectedMatches.isEmpty()
            ) {
                appendLine(
                    "No verified exercise match was found."
                )
            } else {
                selectedMatches
                    .forEachIndexed { index,
                                      matchWithSource ->

                        val match =
                            matchWithSource.first

                        val sourceQuery =
                            matchWithSource.second

                        appendLine()
                        appendLine(
                            "MATCH_${index + 1}:"
                        )
                        appendLine(
                            "title=${match.title}"
                        )
                        appendLine(
                            "beltId=${match.belt.id}"
                        )
                        appendLine(
                            "beltName=${match.belt.name}"
                        )
                        appendLine(
                            "score=${match.score}"
                        )
                        appendLine(
                            "matchedFromQuestion=$sourceQuery"
                        )
                        appendLine(
                            "verifiedExplanation:"
                        )
                        appendLine(
                            match.explanation
                                .trim()
                                .take(
                                    MAX_EXPLANATION_LENGTH
                                )
                        )
                    }
            }

            appendLine()
            appendLine(
                "VERIFIED_MATERIAL_SECTIONS:"
            )

            if (
                materialSections.isEmpty()
            ) {
                appendLine(
                    "No relevant material section was found."
                )
            } else {
                materialSections
                    .forEachIndexed { index,
                                      section ->

                        appendLine()
                        appendLine(
                            "SECTION_${index + 1}:"
                        )
                        appendLine(
                            "beltId=${section.belt.id}"
                        )
                        appendLine(
                            "beltName=${section.belt.name}"
                        )
                        appendLine(
                            "beltHebrewName=${section.belt.heb}"
                        )
                        appendLine(
                            "topic=${section.topicTitle}"
                        )

                        section.subTopicTitle
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let { subTopic ->
                                appendLine(
                                    "subTopic=$subTopic"
                                )
                            }

                        section.nestedSubTopicTitle
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let { nestedSubTopic ->
                                appendLine(
                                    "nestedSubTopic=$nestedSubTopic"
                                )
                            }

                        appendLine(
                            "relevanceScore=${section.score}"
                        )

                        appendLine(
                            "verifiedItems:"
                        )

                        if (
                            section.items.isEmpty()
                        ) {
                            appendLine(
                                "- No direct items in this section."
                            )
                        } else {
                            section.items
                                .take(
                                    MAX_MATERIAL_ITEMS_PER_SECTION
                                )
                                .forEach { item ->
                                    appendLine(
                                        "- $item"
                                    )
                                }

                            val remainingCount =
                                section.items.size -
                                        MAX_MATERIAL_ITEMS_PER_SECTION

                            if (remainingCount > 0) {
                                appendLine(
                                    "- $remainingCount additional verified items omitted for context size."
                                )
                            }
                        }
                    }
            }
        }
            .trim()
            .take(
                MAX_KNOWLEDGE_CONTEXT_LENGTH
            )
    }

    /**
     * מוצא אזורי חומר רלוונטיים ללא שמות חגורות,
     * נושאים או תרגילים קשיחים.
     *
     * כל המידע מגיע ישירות מ־ContentRepo.data.
     */
    private fun findRelevantMaterialSections(
        contextualQueries: List<String>,
        preferredBelt: Belt?
    ): List<MaterialKnowledgeSection> {
        val normalizedQueries =
            contextualQueries
                .map(::normalizeMaterialText)
                .filter {
                    it.length >= 2
                }
                .distinct()

        if (normalizedQueries.isEmpty()) {
            return emptyList()
        }

        val allSections =
            buildList {
                ContentRepo.data
                    .forEach { belt,
                               beltContent ->

                        beltContent.topics
                            .forEach { topic ->
                                if (
                                    topic.items.isNotEmpty()
                                ) {
                                    add(
                                        MaterialKnowledgeSection(
                                            belt =
                                                belt,
                                            topicTitle =
                                                topic.title,
                                            subTopicTitle =
                                                null,
                                            nestedSubTopicTitle =
                                                null,
                                            items =
                                                topic.items,
                                            score =
                                                0
                                        )
                                    )
                                }

                                topic.subTopics
                                    .forEach { subTopic ->
                                        if (
                                            subTopic.items
                                                .isNotEmpty()
                                        ) {
                                            add(
                                                MaterialKnowledgeSection(
                                                    belt =
                                                        belt,
                                                    topicTitle =
                                                        topic.title,
                                                    subTopicTitle =
                                                        subTopic.title,
                                                    nestedSubTopicTitle =
                                                        null,
                                                    items =
                                                        subTopic.items,
                                                    score =
                                                        0
                                                )
                                            )
                                        }

                                        subTopic.subTopics
                                            .forEach { nestedSubTopic ->

                                                add(
                                                    MaterialKnowledgeSection(
                                                        belt =
                                                            belt,
                                                        topicTitle =
                                                            topic.title,
                                                        subTopicTitle =
                                                            subTopic.title,
                                                        nestedSubTopicTitle =
                                                            nestedSubTopic.title,
                                                        items =
                                                            nestedSubTopic.items,
                                                        score =
                                                            0
                                                    )
                                                )
                                            }
                                    }
                            }
                    }
            }

        val scoredSections =
            allSections
                .map { section ->
                    val score =
                        normalizedQueries
                            .maxOfOrNull { normalizedQuery ->

                                scoreMaterialSection(
                                    normalizedQuery =
                                        normalizedQuery,
                                    section =
                                        section,
                                    preferredBelt =
                                        preferredBelt
                                )
                            }
                            ?: 0

                    section.copy(
                        score =
                            score
                    )
                }
                .filter { section ->
                    section.score > 0
                }
                .sortedWith(
                    compareByDescending<
                            MaterialKnowledgeSection
                            > {
                        it.score
                    }
                        .thenBy {
                            it.belt.id
                        }
                        .thenBy {
                            it.topicTitle
                        }
                        .thenBy {
                            it.subTopicTitle.orEmpty()
                        }
                        .thenBy {
                            it.nestedSubTopicTitle.orEmpty()
                        }
                )

        if (scoredSections.isNotEmpty()) {
            return scoredSections
                .distinctBy { section ->
                    buildString {
                        append(
                            section.belt.id
                        )
                        append("::")
                        append(
                            normalizeMaterialText(
                                section.topicTitle
                            )
                        )
                        append("::")
                        append(
                            normalizeMaterialText(
                                section.subTopicTitle
                                    .orEmpty()
                            )
                        )
                        append("::")
                        append(
                            normalizeMaterialText(
                                section.nestedSubTopicTitle
                                    .orEmpty()
                            )
                        )
                    }
                }
                .take(
                    MAX_MATERIAL_SECTIONS
                )
        }

        /*
         * אם לא נמצאה התאמה מילולית אבל קיימת חגורה
         * מועדפת, שולחים תקציר מצומצם של החומר שלה.
         *
         * הדבר מאפשר לענות גם על שאלות כלליות כמו
         * "מה כדאי לי ללמוד עכשיו?".
         */
        if (preferredBelt != null) {
            return allSections
                .asSequence()
                .filter { section ->
                    section.belt ==
                            preferredBelt
                }
                .distinctBy { section ->
                    section.topicTitle to
                            section.subTopicTitle
                }
                .take(
                    MAX_MATERIAL_SECTIONS
                )
                .map { section ->
                    section.copy(
                        score =
                            1
                    )
                }
                .toList()
        }

        return emptyList()
    }

    private fun scoreMaterialSection(
        normalizedQuery: String,
        section: MaterialKnowledgeSection,
        preferredBelt: Belt?
    ): Int {
        val normalizedTopic =
            normalizeMaterialText(
                section.topicTitle
            )

        val normalizedSubTopic =
            normalizeMaterialText(
                section.subTopicTitle
                    .orEmpty()
            )

        val normalizedNestedSubTopic =
            normalizeMaterialText(
                section.nestedSubTopicTitle
                    .orEmpty()
            )

        val normalizedItems =
            section.items
                .joinToString(" ") { item ->
                    normalizeMaterialText(
                        item
                    )
                }

        val normalizedBeltValues =
            listOf(
                section.belt.id,
                section.belt.name,
                section.belt.heb
            )
                .map(::normalizeMaterialText)
                .filter {
                    it.isNotBlank()
                }

        val queryWords =
            normalizedQuery
                .split(" ")
                .filter { word ->
                    word.length >= 2
                }
                .distinct()

        val searchableSection =
            listOf(
                normalizedTopic,
                normalizedSubTopic,
                normalizedNestedSubTopic,
                normalizedItems
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(" ")

        var score = 0

        if (
            normalizedTopic.isNotBlank() &&
            normalizedTopic in
            normalizedQuery
        ) {
            score += 180
        }

        if (
            normalizedSubTopic.isNotBlank() &&
            normalizedSubTopic in
            normalizedQuery
        ) {
            score += 210
        }

        if (
            normalizedNestedSubTopic.isNotBlank() &&
            normalizedNestedSubTopic in
            normalizedQuery
        ) {
            score += 230
        }

        if (
            normalizedQuery in
            searchableSection
        ) {
            score += 160
        }

        val wordHits =
            queryWords.count { word ->
                word in
                        searchableSection
            }

        score +=
            wordHits * 28

        if (
            queryWords.isNotEmpty() &&
            wordHits ==
            queryWords.size
        ) {
            score += 90
        }

        val explicitlyRequestedBelt =
            normalizedBeltValues.any { beltValue ->

                beltValue in
                        normalizedQuery
            }

        if (explicitlyRequestedBelt) {
            score += 140
        }

        if (
            preferredBelt != null &&
            preferredBelt ==
            section.belt &&
            score > 0
        ) {
            score += 25
        }

        return score
    }

    private fun normalizeMaterialText(
        value: String
    ): String {
        return value
            .lowercase()
            .replace("\u200f", "")
            .replace("\u200e", "")
            .replace("\u00a0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(
                Regex(
                    """[^\p{L}\p{N}]+"""
                ),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }

    private fun buildUserProfile(
        preferredBelt: Belt?,
        additionalUserProfile: String,
        isEnglish: Boolean
    ): String {
        return buildString {
            appendLine(
                "language=" +
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        }
            )

            preferredBelt
                ?.let { belt ->
                    appendLine(
                        "preferredBeltId=" +
                                belt.id
                    )

                    appendLine(
                        "preferredBeltName=" +
                                belt.name
                    )
                }

            additionalUserProfile
                .trim()
                .takeIf {
                    it.isNotBlank()
                }
                ?.let { profile ->
                    appendLine(
                        "additionalProfile:"
                    )

                    append(
                        profile
                    )
                }
        }
            .trim()
            .take(
                MAX_PROFILE_LENGTH
            )
    }

    private fun parseResponse(
        rawData: Any?,
        localAnswer: String
    ): RemoteAssistantResult {
        val response =
            rawData as? Map<*, *>
                ?: return RemoteAssistantResult
                    .Fallback(
                        reason =
                            "invalid_server_response",
                        localAnswer =
                            localAnswer
                    )

        val success =
            response["success"]
                    as? Boolean
                ?: false

        val fallbackRequired =
            response["fallbackRequired"]
                    as? Boolean
                ?: !success

        val fallbackReason =
            response["fallbackReason"]
                ?.toString()
                ?.trim()
                .orEmpty()

        if (
            !success ||
            fallbackRequired
        ) {
            return RemoteAssistantResult.Fallback(
                reason =
                    fallbackReason.ifBlank {
                        "server_requested_fallback"
                    },
                localAnswer =
                    localAnswer
            )
        }

        val answerTitle =
            response["answerTitle"]
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val reply =
            response["reply"]
                ?.toString()
                ?.trim()
                .orEmpty()

        if (reply.isBlank()) {
            return RemoteAssistantResult.Fallback(
                reason =
                    "empty_ai_reply",
                localAnswer =
                    localAnswer
            )
        }

        val needsClarification =
            response["needsClarification"]
                    as? Boolean
                ?: false

        val followUpQuestion =
            response["followUpQuestion"]
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val suggestedAction =
            response["suggestedAction"]
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        return RemoteAssistantResult.Success(
            answer =
                RemoteAssistantAnswer(
                    title =
                        answerTitle,

                    text =
                        reply,

                    needsClarification =
                        needsClarification,

                    followUpQuestion =
                        followUpQuestion,

                    suggestedAction =
                        suggestedAction,

                    model =
                        response["model"]
                            ?.toString()
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            },

                    requestCostMicros =
                        response
                            .longValue(
                                "requestCostMicros"
                            ),

                    spentMicros =
                        response
                            .longValue(
                                "spentMicros"
                            ),

                    remainingMicros =
                        response
                            .longValue(
                                "remainingMicros"
                            ),

                    monthlyLimitMicros =
                        response
                            .longValue(
                                "monthlyLimitMicros"
                            )
                )
        )
    }

    private fun isVerifiedExerciseListRequest(
        question: String
    ): Boolean {
        val normalized =
            question
                .lowercase()
                .replace("־", " ")
                .replace("–", " ")
                .replace("—", " ")
                .replace("-", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        return listOf(
            "כל תרגיל",
            "כל התרגיל",
            "כל הגנה",
            "כל ההגנות",
            "איזה תרגיל",
            "אילו תרגיל",
            "איזה הגנות",
            "אילו הגנות",
            "מהם התרגילים",
            "מה הם התרגילים",
            "מה הן ההגנות",
            "רשימת תרגיל",
            "רשימת הגנות",
            "תרגילים יש",
            "הגנות יש",
            "all exercises",
            "all the exercises",
            "which exercises",
            "what exercises",
            "exercise list",
            "list exercises",
            "list of exercises"
        ).any { marker ->
            marker in normalized
        }
    }
}

private fun Map<*, *>.longValue(
    key: String
): Long {
    return when (
        val value =
            this[key]
    ) {
        is Number ->
            value.toLong()

        is String ->
            value
                .toLongOrNull()
                ?: 0L

        else ->
            0L
    }
}
