package il.kmi.app.ui.assistant.core

import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.app.ui.assistant.material.MaterialAssistantEngine
import il.kmi.app.ui.assistant.trainings.TrainingsAssistantEngine
import il.kmi.shared.domain.Belt

/**
 * בקשה אחידה למאגרי הידע של העוזר.
 */
data class AssistantKnowledgeRequest(
    val originalQuestion: String,
    val resolvedQuestion: String = originalQuestion,
    val intent: AssistantIntent = AssistantIntent.UNKNOWN,
    val preferredBelt: Belt? = null,
    val isEnglish: Boolean = false
)

/**
 * שכבת גישה אחת לכל מקורות המידע הקיימים באפליקציה.
 *
 * בשלב הראשון היא עוטפת את המנועים הקיימים בלי לשנות אותם.
 * בהמשך ניתן יהיה לגרום למנועים להחזיר פריטים מובנים ישירות.
 */
object AssistantKnowledgeRepository {

    fun query(
        request: AssistantKnowledgeRequest
    ): AssistantResult {
        val cleanOriginal = request.originalQuestion.trim()
        val cleanResolved = request.resolvedQuestion.trim()
            .ifBlank { cleanOriginal }

        if (cleanOriginal.isBlank()) {
            return AssistantResult.Error(
                originalQuestion = cleanOriginal,
                resolvedQuestion = cleanResolved,
                userMessage = text(
                    isEnglish = request.isEnglish,
                    he = "לא התקבלה שאלה. אפשר לכתוב או לומר בקשה.",
                    en = "No question was received. You can type or say a request."
                ),
                technicalMessage = "Empty assistant request",
                recoverable = true
            )
        }

        return try {
            when (request.intent) {
                AssistantIntent.EXERCISE,
                AssistantIntent.EXPLAIN_EXERCISE,
                AssistantIntent.SEARCH_EXERCISE -> {
                    queryExercise(
                        request = request,
                        resolvedQuestion = cleanResolved
                    )
                }

                AssistantIntent.MATERIAL,
                AssistantIntent.LIST_EXERCISES,
                AssistantIntent.SEARCH_MATERIAL,
                AssistantIntent.LIST_TOPICS -> {
                    queryMaterial(
                        request = request,
                        resolvedQuestion = cleanResolved
                    )
                }

                AssistantIntent.TRAININGS,
                AssistantIntent.NEXT_TRAINING,
                AssistantIntent.LIST_TRAININGS,
                AssistantIntent.USER_TRAINING_DETAILS -> {
                    queryTrainings(
                        request = request,
                        resolvedQuestion = cleanResolved
                    )
                }

                AssistantIntent.NAVIGATION -> {
                    AssistantResult.MissingInformation(
                        originalQuestion = cleanOriginal,
                        resolvedQuestion = cleanResolved,
                        intent = AssistantIntent.NAVIGATION,
                        source = AssistantKnowledgeSource.NAVIGATION,
                        message = text(
                            isEnglish = request.isEnglish,
                            he = "בקשת הניווט זוהתה, אך עליה להתבצע דרך מסך האפליקציה.",
                            en = "The navigation request was recognized, but it must be performed by the app screen."
                        ),
                        missingFields = listOf("navigation_callback")
                    )
                }

                AssistantIntent.UNKNOWN -> {
                    queryUnknown(
                        request = request,
                        resolvedQuestion = cleanResolved
                    )
                }
            }
        } catch (error: Throwable) {
            AssistantResult.Error(
                originalQuestion = cleanOriginal,
                resolvedQuestion = cleanResolved,
                intent = request.intent,
                source = sourceForIntent(request.intent),
                userMessage = text(
                    isEnglish = request.isEnglish,
                    he = "אירעה תקלה רגעית בזמן חיפוש המידע. אפשר לנסות שוב.",
                    en = "A temporary issue occurred while searching. Please try again."
                ),
                technicalMessage = error.message,
                recoverable = true,
                suggestedActions = listOf(
                    retryAction(
                        questionHe = cleanOriginal,
                        questionEn = cleanOriginal
                    )
                )
            )
        }
    }

    private fun queryExercise(
        request: AssistantKnowledgeRequest,
        resolvedQuestion: String
    ): AssistantResult {
        val rawAnswer = ExerciseAssistantEngine.answer(
            question = resolvedQuestion,
            preferredBelt = request.preferredBelt,
            isEnglish = request.isEnglish
        ).trim()

        return resultFromText(
            request = request,
            answer = rawAnswer,
            intent = request.intent,
            source = AssistantKnowledgeSource.EXERCISES,
            successTitle = text(
                isEnglish = request.isEnglish,
                he = "הסבר לתרגיל",
                en = "Exercise explanation"
            ),
            suggestedActions = exerciseActions(
                question = request.originalQuestion,
                isEnglish = request.isEnglish
            )
        )
    }

    private fun queryMaterial(
        request: AssistantKnowledgeRequest,
        resolvedQuestion: String
    ): AssistantResult {
        val rawAnswer = MaterialAssistantEngine.answer(
            question = resolvedQuestion,
            preferredBelt = request.preferredBelt,
            isEnglish = request.isEnglish
        ).trim()

        return resultFromText(
            request = request,
            answer = rawAnswer,
            intent = request.intent,
            source = AssistantKnowledgeSource.MATERIAL,
            successTitle = text(
                isEnglish = request.isEnglish,
                he = "תוצאה מחומר ק.מ.י",
                en = "KAMI material result"
            ),
            suggestedActions = materialActions(
                question = request.originalQuestion,
                isEnglish = request.isEnglish
            )
        )
    }

    private fun queryTrainings(
        request: AssistantKnowledgeRequest,
        resolvedQuestion: String
    ): AssistantResult {
        val response =
            TrainingsAssistantEngine.answerDetailed(
                question = resolvedQuestion,
                isEnglish = request.isEnglish
            )

        val rawAnswer =
            response.text.trim()

        if (response.cards.isNotEmpty()) {
            return AssistantResult.ResultList(
                originalQuestion =
                    request.originalQuestion,
                resolvedQuestion =
                    request.resolvedQuestion,
                intent = request.intent,
                source =
                    AssistantKnowledgeSource.TRAININGS,
                title = text(
                    isEnglish = request.isEnglish,
                    he = "האימונים שמצאתי",
                    en = "Trainings I found"
                ),
                introduction = text(
                    isEnglish = request.isEnglish,
                    he = "להלן פרטי האימונים המתאימים:",
                    en = "Here are the matching trainings:"
                ),
                items = response.cards.map { card ->
                    AssistantResultItem(
                        id = card.id,
                        title = card.title,
                        subtitle = buildString {
                            append(card.startTime)

                            card.endTime?.let {
                                append("–")
                                append(it)
                            }
                        },
                        description = rawAnswer,
                        source =
                            AssistantKnowledgeSource.TRAININGS,
                        date = card.date,
                        startTime = card.startTime,
                        endTime = card.endTime,
                        branchName =
                            card.branchName,
                        groupName =
                            card.groupName,
                        location = card.location,
                        coachName = card.coachName,
                        trainingStatusCode =
                            card.statusCode,
                        trainingStatusHe =
                            card.statusHe,
                        trainingStatusEn =
                            card.statusEn,
                        matchQuality =
                            AssistantMatchQuality.HIGH
                    )
                },
                matchQuality =
                    AssistantMatchQuality.HIGH,
                suggestedActions =
                    trainingActions(
                        isEnglish = request.isEnglish
                    )
            )
        }

        return resultFromText(
            request = request,
            answer = rawAnswer,
            intent = request.intent,
            source =
                AssistantKnowledgeSource.TRAININGS,
            successTitle = text(
                isEnglish = request.isEnglish,
                he = "פרטי האימונים",
                en = "Training information"
            ),
            suggestedActions =
                trainingActions(
                    isEnglish = request.isEnglish
                )
        )
    }

    /**
     * כאשר הכוונה עדיין אינה ידועה, מנסים את המאגרים לפי
     * רמזים בטקסט ולא מחזירים מיד תשובת שגיאה.
     */
    private fun queryUnknown(
        request: AssistantKnowledgeRequest,
        resolvedQuestion: String
    ): AssistantResult {
        val normalized = resolvedQuestion.lowercase()

        val looksLikeTraining = containsAny(
            normalized,
            listOf(
                "אימון",
                "אימונים",
                "מאמן",
                "קבוצה",
                "סניף",
                "training",
                "trainings",
                "workout",
                "coach",
                "group",
                "branch",
                "schedule"
            )
        )

        val looksLikeMaterial = containsAny(
            normalized,
            listOf(
                "חגורה",
                "חומר",
                "נושא",
                "תת נושא",
                "רשימת תרגילים",
                "belt",
                "material",
                "topic",
                "sub topic",
                "exercise list"
            )
        )

        val looksLikeExercise = containsAny(
            normalized,
            listOf(
                "תרגיל",
                "תסביר",
                "הסבר",
                "איך עושים",
                "בעיטה",
                "אגרוף",
                "הגנה",
                "דקירה",
                "חניקה",
                "exercise",
                "explain",
                "how to",
                "kick",
                "punch",
                "defense",
                "stab",
                "choke"
            )
        )

        return when {
            looksLikeTraining -> {
                queryTrainings(
                    request = request.copy(
                        intent = AssistantIntent.LIST_TRAININGS
                    ),
                    resolvedQuestion = resolvedQuestion
                )
            }

            looksLikeMaterial -> {
                queryMaterial(
                    request = request.copy(
                        intent = AssistantIntent.SEARCH_MATERIAL
                    ),
                    resolvedQuestion = resolvedQuestion
                )
            }

            looksLikeExercise -> {
                queryExercise(
                    request = request.copy(
                        intent = AssistantIntent.EXPLAIN_EXERCISE
                    ),
                    resolvedQuestion = resolvedQuestion
                )
            }

            else -> {
                AssistantResult.Clarification(
                    originalQuestion = request.originalQuestion,
                    resolvedQuestion = resolvedQuestion,
                    intent = AssistantIntent.UNKNOWN,
                    source = AssistantKnowledgeSource.UNKNOWN,
                    question = text(
                        isEnglish = request.isEnglish,
                        he = "כדי שאמצא את התשובה המדויקת, באיזה סוג מידע מדובר?",
                        en = "To find the right answer, which type of information do you mean?"
                    ),
                    options = clarificationModeOptions(
                        isEnglish = request.isEnglish
                    ),
                    suggestedActions = clarificationActions(
                        question = request.originalQuestion,
                        isEnglish = request.isEnglish
                    )
                )
            }
        }
    }

    private fun resultFromText(
        request: AssistantKnowledgeRequest,
        answer: String,
        intent: AssistantIntent,
        source: AssistantKnowledgeSource,
        successTitle: String,
        suggestedActions: List<AssistantSuggestedAction>
    ): AssistantResult {
        if (answer.isBlank()) {
            return AssistantResult.NotFound(
                originalQuestion = request.originalQuestion,
                resolvedQuestion = request.resolvedQuestion,
                intent = intent,
                source = source,
                message = text(
                    isEnglish = request.isEnglish,
                    he = "לא התקבלה תוצאה מהמאגר. נסה לנסח את הבקשה בצורה מעט שונה.",
                    en = "No result was returned. Try phrasing the request a little differently."
                ),
                suggestedActions = suggestedActions
            )
        }

        if (looksLikeMissingInformation(answer)) {
            return AssistantResult.MissingInformation(
                originalQuestion = request.originalQuestion,
                resolvedQuestion = request.resolvedQuestion,
                intent = intent,
                source = source,
                message = answer,
                missingFields = detectMissingFields(answer),
                suggestedActions = suggestedActions
            )
        }

        if (looksLikeNotFound(answer)) {
            return AssistantResult.NotFound(
                originalQuestion = request.originalQuestion,
                resolvedQuestion = request.resolvedQuestion,
                intent = intent,
                source = source,
                message = answer,
                suggestedActions = suggestedActions
            )
        }

        return AssistantResult.Answer(
            originalQuestion = request.originalQuestion,
            resolvedQuestion = request.resolvedQuestion,
            intent = intent,
            source = source,
            title = successTitle,
            answer = answer,
            matchQuality = detectMatchQuality(answer),
            suggestedActions = suggestedActions
        )
    }

    private fun looksLikeNotFound(answer: String): Boolean {
        val normalized = answer.lowercase()

        return containsAny(
            normalized,
            listOf(
                "לא מצא",
                "לא נמצא",
                "אין כרגע",
                "לא הצלחתי לזהות",
                "לא הצלחתי למצוא",
                "couldn't find",
                "could not find",
                "not found",
                "no exercise",
                "no results",
                "couldn't identify"
            )
        )
    }

    private fun looksLikeMissingInformation(answer: String): Boolean {
        val normalized = answer.lowercase()

        return containsAny(
            normalized,
            listOf(
                "חסר סניף",
                "לא הוגדר סניף",
                "חסרה קבוצה",
                "לא הוגדרה קבוצה",
                "פרטי המשתמש חסרים",
                "missing branch",
                "branch is not set",
                "missing group",
                "group is not set",
                "user details are missing"
            )
        )
    }

    private fun detectMissingFields(answer: String): List<String> {
        val normalized = answer.lowercase()
        val fields = mutableListOf<String>()

        if ("סניף" in normalized || "branch" in normalized) {
            fields += "branch"
        }

        if ("קבוצה" in normalized || "group" in normalized) {
            fields += "group"
        }

        if ("אזור" in normalized || "region" in normalized) {
            fields += "region"
        }

        return fields.distinct()
    }

    private fun detectMatchQuality(
        answer: String
    ): AssistantMatchQuality {
        val normalized = answer.lowercase()

        return when {
            containsAny(
                normalized,
                listOf(
                    "ייתכן שהתכוונת",
                    "אולי התכוונת",
                    "מספר אפשרויות",
                    "did you mean",
                    "several options",
                    "multiple matches"
                )
            ) -> AssistantMatchQuality.MEDIUM

            containsAny(
                normalized,
                listOf(
                    "תוצאה דומה",
                    "תרגיל דומה",
                    "similar result",
                    "similar exercise"
                )
            ) -> AssistantMatchQuality.LOW

            else -> AssistantMatchQuality.HIGH
        }
    }

    private fun clarificationModeOptions(
        isEnglish: Boolean
    ): List<AssistantResultItem> {
        return listOf(
            AssistantResultItem(
                id = "mode_exercise",
                title = text(
                    isEnglish,
                    he = "הסבר על תרגיל",
                    en = "Exercise explanation"
                ),
                source = AssistantKnowledgeSource.EXERCISES,
                matchQuality = AssistantMatchQuality.MEDIUM
            ),
            AssistantResultItem(
                id = "mode_material",
                title = text(
                    isEnglish,
                    he = "חיפוש בחומר ק.מ.י",
                    en = "Search KAMI material"
                ),
                source = AssistantKnowledgeSource.MATERIAL,
                matchQuality = AssistantMatchQuality.MEDIUM
            ),
            AssistantResultItem(
                id = "mode_trainings",
                title = text(
                    isEnglish,
                    he = "מידע על אימונים",
                    en = "Training information"
                ),
                source = AssistantKnowledgeSource.TRAININGS,
                matchQuality = AssistantMatchQuality.MEDIUM
            )
        )
    }

    private fun exerciseActions(
        question: String,
        isEnglish: Boolean
    ): List<AssistantSuggestedAction> {
        return listOf(
            AssistantSuggestedAction(
                id = "exercise_more_detail",
                labelHe = "הסבר מפורט יותר",
                labelEn = "Explain in more detail",
                queryHe = "תן הסבר מפורט יותר על $question",
                queryEn = "Explain $question in more detail",
                targetMode = AssistantIntent.EXPLAIN_EXERCISE
            ),
            AssistantSuggestedAction(
                id = "exercise_starting_position",
                labelHe = "עמידת מוצא",
                labelEn = "Starting position",
                queryHe = "מה עמידת המוצא של $question",
                queryEn = "What is the starting position for $question",
                targetMode = AssistantIntent.EXPLAIN_EXERCISE
            ),
            AssistantSuggestedAction(
                id = "exercise_similar",
                labelHe = "תרגילים דומים",
                labelEn = "Similar exercises",
                queryHe = "הצג תרגילים דומים ל-$question",
                queryEn = "Show exercises similar to $question",
                targetMode = AssistantIntent.SEARCH_MATERIAL
            )
        )
    }

    private fun materialActions(
        question: String,
        isEnglish: Boolean
    ): List<AssistantSuggestedAction> {
        return listOf(
            AssistantSuggestedAction(
                id = "material_more",
                labelHe = "עוד בנושא",
                labelEn = "More on this topic",
                queryHe = "$question הצג עוד",
                queryEn = "$question show more",
                targetMode = AssistantIntent.SEARCH_MATERIAL
            ),
            AssistantSuggestedAction(
                id = "material_by_belt",
                labelHe = "חיפוש לפי חגורה",
                labelEn = "Search by belt",
                queryHe = "$question לפי חגורה",
                queryEn = "$question by belt",
                targetMode = AssistantIntent.SEARCH_MATERIAL
            )
        )
    }

    private fun trainingActions(
        isEnglish: Boolean
    ): List<AssistantSuggestedAction> {
        return listOf(
            AssistantSuggestedAction(
                id = "training_next",
                labelHe = "האימון הבא",
                labelEn = "Next training",
                queryHe = "מה האימון הבא שלי?",
                queryEn = "What is my next training?",
                targetMode = AssistantIntent.NEXT_TRAINING
            ),
            AssistantSuggestedAction(
                id = "training_week",
                labelHe = "אימוני השבוע",
                labelEn = "This week's trainings",
                queryHe = "הצג את האימונים שלי השבוע",
                queryEn = "Show my trainings this week",
                targetMode = AssistantIntent.LIST_TRAININGS
            )
        )
    }

    private fun clarificationActions(
        question: String,
        isEnglish: Boolean
    ): List<AssistantSuggestedAction> {
        return listOf(
            AssistantSuggestedAction(
                id = "clarify_exercise",
                labelHe = "תרגיל",
                labelEn = "Exercise",
                queryHe = "תן הסבר לתרגיל $question",
                queryEn = "Explain the exercise $question",
                targetMode = AssistantIntent.EXPLAIN_EXERCISE
            ),
            AssistantSuggestedAction(
                id = "clarify_material",
                labelHe = "חומר ק.מ.י",
                labelEn = "KAMI material",
                queryHe = "חפש בחומר ק.מ.י $question",
                queryEn = "Search KAMI material for $question",
                targetMode = AssistantIntent.SEARCH_MATERIAL
            ),
            AssistantSuggestedAction(
                id = "clarify_training",
                labelHe = "אימונים",
                labelEn = "Trainings",
                queryHe = "חפש מידע על אימונים $question",
                queryEn = "Find training information for $question",
                targetMode = AssistantIntent.LIST_TRAININGS
            )
        )
    }

    private fun retryAction(
        questionHe: String,
        questionEn: String
    ): AssistantSuggestedAction {
        return AssistantSuggestedAction(
            id = "retry",
            labelHe = "נסה שוב",
            labelEn = "Try again",
            queryHe = questionHe,
            queryEn = questionEn
        )
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

    private fun containsAny(
        text: String,
        values: List<String>
    ): Boolean {
        return values.any { value ->
            value in text
        }
    }

    private fun text(
        isEnglish: Boolean,
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }
}