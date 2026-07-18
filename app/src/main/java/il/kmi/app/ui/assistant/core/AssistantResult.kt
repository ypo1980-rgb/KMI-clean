package il.kmi.app.ui.assistant.core

import il.kmi.shared.domain.Belt

/**
 * רמת ההתאמה בין בקשת המשתמש לתוצאה שנמצאה.
 *
 * אין צורך להציג למשתמש ציון מספרי.
 * ה-UI יכול לתרגם את הרמה לתווית ידידותית.
 */
enum class AssistantMatchQuality {
    EXACT,
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

/**
 * פעולה שיובל יכול להציע לאחר התשובה.
 */
data class AssistantSuggestedAction(
    val id: String,
    val labelHe: String,
    val labelEn: String,
    val queryHe: String,
    val queryEn: String,
    val targetMode: AssistantIntent? = null
) {
    fun label(isEnglish: Boolean): String {
        return if (isEnglish) labelEn else labelHe
    }

    fun query(isEnglish: Boolean): String {
        return if (isEnglish) queryEn else queryHe
    }
}

/**
 * פריט מובנה שהתקבל ממאגר הידע.
 *
 * המבנה מתאים לתרגיל, נושא, תת-נושא או אימון.
 */
data class AssistantResultItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,

    val source: AssistantKnowledgeSource =
        AssistantKnowledgeSource.UNKNOWN,

    val belt: Belt? = null,
    val topicName: String? = null,
    val subTopicName: String? = null,
    val exerciseName: String? = null,

    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val branchName: String? = null,
    val groupName: String? = null,

    val matchQuality: AssistantMatchQuality =
        AssistantMatchQuality.NONE,

    /**
     * ציון פנימי לצורך מיון בלבד.
     * אין להציג אותו למשתמש.
     */
    val internalScore: Float = 0f
) {
    fun toContextResult(): AssistantContextResult {
        return AssistantContextResult(
            id = id,
            title = title,
            subtitle = subtitle,
            source = source,
            belt = belt,
            topicName = topicName,
            exerciseName = exerciseName
        )
    }
}

/**
 * תשובה מובנית מכל אחד ממנועי העוזר.
 *
 * במקום שכל מנוע יחזיר String בלבד, הוא יוכל להחזיר
 * אחת מהתוצאות הבאות. ה-UI יבחר כיצד להציג אותה.
 */
sealed interface AssistantResult {

    val originalQuestion: String
    val resolvedQuestion: String
    val intent: AssistantIntent
    val source: AssistantKnowledgeSource
    val suggestedActions: List<AssistantSuggestedAction>

    /**
     * תשובה ישירה לשאלה.
     *
     * מתאימה להסבר תרגיל, פרטי אימון או מידע קצר.
     */
    data class Answer(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent,
        override val source: AssistantKnowledgeSource,

        val title: String,
        val answer: String,
        val item: AssistantResultItem? = null,
        val matchQuality: AssistantMatchQuality =
            AssistantMatchQuality.HIGH,

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult

    /**
     * רשימת תוצאות.
     *
     * מתאימה למספר תרגילים, נושאים או אימונים.
     */
    data class ResultList(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent,
        override val source: AssistantKnowledgeSource,

        val title: String,
        val introduction: String? = null,
        val items: List<AssistantResultItem>,
        val matchQuality: AssistantMatchQuality =
            AssistantMatchQuality.HIGH,

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult

    /**
     * מספר תוצאות אפשריות נמצאו ולא נכון לבחור
     * אחת מהן ללא אישור המשתמש.
     */
    data class Clarification(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent,
        override val source: AssistantKnowledgeSource,

        val question: String,
        val options: List<AssistantResultItem>,
        val allowManualAnswer: Boolean = true,

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult

    /**
     * לא נמצאה התאמה מספקת, אך קיימות הצעות שימושיות.
     */
    data class NotFound(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent,
        override val source: AssistantKnowledgeSource,

        val message: String,
        val possibleMatches: List<AssistantResultItem> =
            emptyList(),

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult

    /**
     * לא ניתן להשלים את הבקשה בגלל נתון חסר.
     *
     * לדוגמה: אין סניף או קבוצה בפרופיל המשתמש.
     */
    data class MissingInformation(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent,
        override val source: AssistantKnowledgeSource,

        val message: String,
        val missingFields: List<String>,

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult

    /**
     * תקלה טכנית שאינה קשורה לניסוח השאלה.
     */
    data class Error(
        override val originalQuestion: String,
        override val resolvedQuestion: String,
        override val intent: AssistantIntent =
            AssistantIntent.UNKNOWN,
        override val source: AssistantKnowledgeSource =
            AssistantKnowledgeSource.UNKNOWN,

        val userMessage: String,
        val technicalMessage: String? = null,
        val recoverable: Boolean = true,

        override val suggestedActions:
        List<AssistantSuggestedAction> = emptyList()
    ) : AssistantResult
}

/**
 * פונקציות עזר משותפות לכל סוגי התוצאות.
 */
fun AssistantResult.contextResults(): List<AssistantContextResult> {
    return when (this) {
        is AssistantResult.Answer ->
            listOfNotNull(item?.toContextResult())

        is AssistantResult.ResultList ->
            items.map { it.toContextResult() }

        is AssistantResult.Clarification ->
            options.map { it.toContextResult() }

        is AssistantResult.NotFound ->
            possibleMatches.map { it.toContextResult() }

        is AssistantResult.MissingInformation,
        is AssistantResult.Error ->
            emptyList()
    }
}

fun AssistantResult.primaryText(): String {
    return when (this) {
        is AssistantResult.Answer ->
            answer

        is AssistantResult.ResultList ->
            buildString {
                introduction
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append(it)
                        append("\n\n")
                    }

                items.forEachIndexed { index, item ->
                    append("${index + 1}. ${item.title}")

                    item.subtitle
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            append(" — ")
                            append(it)
                        }

                    if (index < items.lastIndex) {
                        append("\n")
                    }
                }
            }

        is AssistantResult.Clarification ->
            buildString {
                append(question)

                if (options.isNotEmpty()) {
                    append("\n\n")

                    options.forEachIndexed { index, item ->
                        append("${index + 1}. ${item.title}")

                        if (index < options.lastIndex) {
                            append("\n")
                        }
                    }
                }
            }

        is AssistantResult.NotFound ->
            message

        is AssistantResult.MissingInformation ->
            message

        is AssistantResult.Error ->
            userMessage
    }.trim()
}

fun AssistantResult.matchQuality(): AssistantMatchQuality {
    return when (this) {
        is AssistantResult.Answer ->
            matchQuality

        is AssistantResult.ResultList ->
            matchQuality

        is AssistantResult.Clarification ->
            AssistantMatchQuality.MEDIUM

        is AssistantResult.NotFound ->
            if (possibleMatches.isEmpty()) {
                AssistantMatchQuality.NONE
            } else {
                AssistantMatchQuality.LOW
            }

        is AssistantResult.MissingInformation,
        is AssistantResult.Error ->
            AssistantMatchQuality.NONE
    }
}

fun AssistantResult.requiresUserChoice(): Boolean {
    return this is AssistantResult.Clarification
}

fun AssistantResult.isSuccessful(): Boolean {
    return this is AssistantResult.Answer ||
            this is AssistantResult.ResultList
}