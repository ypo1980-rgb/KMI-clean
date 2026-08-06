package il.kmi.app.ui.assistant.core

import il.kmi.shared.domain.Belt

/**
 * סוג הבקשה מוגדר בקובץ AssistantIntent.kt.
 * ההקשר משתמש באותה הגדרה מרכזית כדי למנוע כפילות.
 */

/**
 * מקור המידע שממנו התקבלה התוצאה.
 */
enum class AssistantKnowledgeSource {
    EXERCISES,
    MATERIAL,
    TRAININGS,
    USER_PROFILE,
    NAVIGATION,
    UNKNOWN
}

/**
 * תוצאה קודמת שניתן להתייחס אליה בשאלת המשך.
 */
data class AssistantContextResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val source: AssistantKnowledgeSource = AssistantKnowledgeSource.UNKNOWN,
    val belt: Belt? = null,
    val topicName: String? = null,
    val exerciseName: String? = null
)

/**
 * הודעה אחת בזיכרון השיחה.
 *
 * נשמר מספר מצומצם של הודעות בלבד כדי שההקשר יישאר
 * ממוקד ולא יגדל ללא הגבלה.
 */
data class AssistantConversationTurn(
    val userText: String,
    val resolvedText: String,
    val answerPreview: String,
    val intent: AssistantIntent,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * ההקשר הפעיל של השיחה עם יובל.
 *
 * המחלקה אינה תלויה ב-Compose ולכן אפשר להשתמש בה
 * מתוך מנועי העוזר, ViewModel או בדיקות.
 */
data class AssistantConversationContext(
    val intent: AssistantIntent? = null,
    val source: AssistantKnowledgeSource? = null,

    val exerciseName: String? = null,
    val topicName: String? = null,
    val subTopicName: String? = null,
    val belt: Belt? = null,

    val branchName: String? = null,
    val groupName: String? = null,

    val lastUserQuestion: String? = null,
    val lastResolvedQuestion: String? = null,
    val lastAnswer: String? = null,

    val lastResults: List<AssistantContextResult> = emptyList(),
    val recentTurns: List<AssistantConversationTurn> = emptyList(),

    val awaitingClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val clarificationOptions: List<String> = emptyList(),

    val turnCount: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {

    /**
     * עדכון ההקשר לאחר זיהוי בקשה חדשה.
     *
     * ערכים שלא זוהו בבקשה הנוכחית אינם מוחקים אוטומטית
     * את ההקשר הקודם. כך ניתן להבין שאלות המשך קצרות.
     */
    fun withDetectedRequest(
        detectedIntent: AssistantIntent?,
        detectedSource: AssistantKnowledgeSource? = null,
        detectedExerciseName: String? = null,
        detectedTopicName: String? = null,
        detectedSubTopicName: String? = null,
        detectedBelt: Belt? = null,
        userQuestion: String,
        resolvedQuestion: String
    ): AssistantConversationContext {
        return copy(
            intent = detectedIntent ?: intent,
            source = detectedSource ?: source,
            exerciseName = detectedExerciseName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: exerciseName,
            topicName = detectedTopicName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: topicName,
            subTopicName = detectedSubTopicName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: subTopicName,
            belt = detectedBelt ?: belt,
            lastUserQuestion = userQuestion.trim(),
            lastResolvedQuestion = resolvedQuestion.trim(),
            awaitingClarification = false,
            clarificationQuestion = null,
            clarificationOptions = emptyList(),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * עדכון ההקשר לאחר תשובה מוצלחת.
     */
    fun withAnswer(
        answer: String,
        results: List<AssistantContextResult> = emptyList()
    ): AssistantConversationContext {
        val cleanAnswer = answer.trim()

        val turn = AssistantConversationTurn(
            userText = lastUserQuestion.orEmpty(),
            resolvedText = lastResolvedQuestion.orEmpty(),
            answerPreview = cleanAnswer.take(MAX_ANSWER_PREVIEW_LENGTH),
            intent = intent ?: AssistantIntent.UNKNOWN
        )

        /*
         * תשובה ללא תוצאות חדשות אינה מוחקת את
         * התוצאות הקודמות.
         *
         * כך גם אם שאלת המשך לא הובנה, המשתמש עדיין
         * יכול לומר "התכוונתי לתרגיל בחגורה ירוקה"
         * או "תסביר את האפשרות השנייה".
         */
        val rememberedResults =
            if (results.isNotEmpty()) {
                results.take(MAX_RESULTS)
            } else {
                lastResults
            }

        return copy(
            lastAnswer = cleanAnswer,
            lastResults = rememberedResults,
            recentTurns =
                (recentTurns + turn)
                    .takeLast(MAX_RECENT_TURNS),
            awaitingClarification = false,
            clarificationQuestion = null,
            clarificationOptions = emptyList(),
            turnCount = turnCount + 1,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * מעבר למצב שבו יובל ממתין לבחירת המשתמש.
     */
    fun awaitingClarification(
        question: String,
        options: List<String>,
        results: List<AssistantContextResult> = emptyList()
    ): AssistantConversationContext {
        val cleanOptions = options
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_CLARIFICATION_OPTIONS)

        return copy(
            awaitingClarification = true,
            clarificationQuestion = question.trim(),
            clarificationOptions = cleanOptions,
            lastResults = results.take(MAX_RESULTS),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * בחירה מתוך תוצאות קודמות לפי המספר שהמשתמש אמר או לחץ.
     */
    fun resultAt(position: Int): AssistantContextResult? {
        if (position <= 0) return null
        return lastResults.getOrNull(position - 1)
    }

    /**
     * האם קיימת ישות קודמת שאליה שאלת המשך יכולה להתייחס.
     */
    fun hasConversationSubject(): Boolean {
        return !exerciseName.isNullOrBlank() ||
                !topicName.isNullOrBlank() ||
                belt != null ||
                lastResults.isNotEmpty()
    }

    /**
     * ניקוי בקשת הבהרה בלבד, ללא מחיקת נושא השיחה.
     */
    fun clearClarification(): AssistantConversationContext {
        return copy(
            awaitingClarification = false,
            clarificationQuestion = null,
            clarificationOptions = emptyList(),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * פתיחת שיחה חדשה לחלוטין.
     */
    fun reset(): AssistantConversationContext {
        return AssistantConversationContext()
    }

    companion object {
        /*
         * שומרים הקשר רחב מספיק לשאלות המשך,
         * בלי להפוך את הזיכרון לבלתי מוגבל.
         */
        private const val MAX_RECENT_TURNS = 16
        private const val MAX_RESULTS = 50
        private const val MAX_CLARIFICATION_OPTIONS = 20
        private const val MAX_ANSWER_PREVIEW_LENGTH = 1_000
    }
}