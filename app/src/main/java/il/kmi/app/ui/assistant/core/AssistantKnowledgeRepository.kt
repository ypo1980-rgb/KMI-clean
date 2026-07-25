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
        /*
         * גם כאשר המשתמש נמצא במצב "חומר ק.מ.י",
         * בקשת הסבר על תרגיל צריכה לעבור למקור ההסברים
         * המרכזי ולא להישלח שוב למנוע רשימות החומר.
         *
         * resolvedQuestion עשויה כבר להכיל את שמו של
         * התרגיל שנבחר מתוך הרשימה.
         */
        val explanationRequested =
            isExerciseExplanationRequest(
                question = request.originalQuestion
            ) ||
                    isExerciseExplanationRequest(
                        question = resolvedQuestion
                    )

        if (explanationRequested) {
            return queryExercise(
                request = request.copy(
                    resolvedQuestion = resolvedQuestion,
                    intent =
                        AssistantIntent.EXPLAIN_EXERCISE
                ),
                resolvedQuestion = resolvedQuestion
            )
        }

        val rawAnswer =
            MaterialAssistantEngine.answer(
                question = resolvedQuestion,
                preferredBelt = request.preferredBelt,
                isEnglish = request.isEnglish
            ).trim()

        /*
         * לפני יצירת ResultList מטפלים בתשובות שגיאה,
         * מידע חסר או תוצאה שלא נמצאה.
         */
        if (
            rawAnswer.isBlank() ||
            looksLikeMissingInformation(rawAnswer) ||
            looksLikeNotFound(rawAnswer)
        ) {
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

        val parsedMaterial =
            parseMaterialResult(rawAnswer)

        /*
         * שתי תוצאות ומעלה מוצגות כרשימה מובנית.
         * תשובות קצרות, ספירות והבהרות נשארות Answer.
         */
        if (parsedMaterial.items.size >= 2) {
            return AssistantResult.ResultList(
                originalQuestion =
                    request.originalQuestion,
                resolvedQuestion =
                    resolvedQuestion,
                intent = request.intent,
                source =
                    AssistantKnowledgeSource.MATERIAL,
                title = text(
                    isEnglish = request.isEnglish,
                    he = "חומר ק.מ.י שמצאתי",
                    en = "KAMI material I found"
                ),
                introduction =
                    parsedMaterial.introduction,
                items =
                    parsedMaterial.items,
                matchQuality =
                    AssistantMatchQuality.HIGH,
                suggestedActions =
                    materialActions(
                        question =
                            request.originalQuestion,
                        isEnglish =
                            request.isEnglish
                    )
            )
        }

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

    private data class ParsedMaterialResult(
        val introduction: String?,
        val items: List<AssistantResultItem>
    )

    /**
     * ממיר רשימות הטקסט שמוחזרות ממנוע חומר ק.מ.י
     * לפריטים מובנים. אין שינוי בתוכן המקורי של המאגר.
     */
    private fun parseMaterialResult(
        rawAnswer: String
    ): ParsedMaterialResult {
        /*
         * כותרת נושא בחומר מלא:
         * 1. הגנות (12)
         *
         * אין להפוך שורה כזאת לתרגיל.
         */
        val topicHeaderLine =
            Regex("""^(\d+)\.\s+(.+?)\s+\((\d+)\)\s*$""")

        /*
         * תרגיל ממוספר. קבוצת הלכידה הראשונה שומרת
         * את ההזחה כדי להבדיל בין כותרת לבין תרגיל פנימי.
         */
        val numberedLine =
            Regex("""^(\s*)(\d+)\.\s+(.+?)\s*$""")

        /*
         * כותרת תת־נושא:
         * • הגנות פנימיות
         */
        val sectionHeaderLine =
            Regex("""^\s*•\s+(.+?)\s*$""")

        val lines =
            rawAnswer
                .lines()
                .map { line ->
                    line.trimEnd()
                }

        /*
         * רשימת נושאים בלבד אינה רשימת תרגילים.
         * במקרה כזה משאירים את התשובה כטקסט רגיל.
         */
        val normalizedAnswer =
            rawAnswer.lowercase()

        val isTopicOnlyList =
            normalizedAnswer.contains("הנושאים ב") ||
                    normalizedAnswer.contains(
                        "כמה נושאים אפשריים"
                    ) ||
                    normalizedAnswer.contains(
                        "לאיזה מהם התכוונת"
                    ) ||
                    normalizedAnswer.contains(
                        "topics in"
                    ) ||
                    normalizedAnswer.contains(
                        "possible topics"
                    )

        if (isTopicOnlyList) {
            return ParsedMaterialResult(
                introduction = rawAnswer.trim(),
                items = emptyList()
            )
        }

        val firstStructuredLineIndex =
            lines.indexOfFirst { line ->
                topicHeaderLine.matches(line) ||
                        sectionHeaderLine.matches(line) ||
                        numberedLine.matches(line)
            }

        val introduction =
            if (firstStructuredLineIndex > 0) {
                lines
                    .take(firstStructuredLineIndex)
                    .map { line ->
                        line.trim()
                    }
                    .filter { line ->
                        line.isNotBlank()
                    }
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }

        val parsedItems =
            mutableListOf<AssistantResultItem>()

        var currentTopic: String? = null
        var currentSection: String? = null

        lines.forEach { line ->
            /*
             * כותרת נושא מעדכנת את הקבוצה הנוכחית בלבד.
             * היא אינה נוספת לרשימת התרגילים.
             */
            val topicHeaderMatch =
                topicHeaderLine.matchEntire(line)

            if (topicHeaderMatch != null) {
                currentTopic =
                    topicHeaderMatch
                        .groupValues[2]
                        .trim()

                currentSection = null
                return@forEach
            }

            /*
             * גם תת־נושא הוא כותרת קבוצה בלבד.
             */
            val sectionHeaderMatch =
                sectionHeaderLine.matchEntire(line)

            if (sectionHeaderMatch != null) {
                currentSection =
                    sectionHeaderMatch
                        .groupValues[1]
                        .trim()

                return@forEach
            }

            val numberedMatch =
                numberedLine.matchEntire(line)
                    ?: return@forEach

            val rawContent =
                numberedMatch
                    .groupValues[3]
                    .trim()

            if (rawContent.isBlank()) {
                return@forEach
            }

            /*
             * הגנה נוספת: תוכן שמסתיים בספירת פריטים
             * ונמצא ללא הזחה הוא כנראה כותרת נושא.
             */
            val looksLikeTopicHeader =
                numberedMatch.groupValues[1].isBlank() &&
                        Regex(""".+\s+\(\d+\)$""")
                            .matches(rawContent)

            if (looksLikeTopicHeader) {
                currentTopic =
                    rawContent
                        .replace(
                            Regex("""\s+\(\d+\)$"""),
                            ""
                        )
                        .trim()

                currentSection = null
                return@forEach
            }

            val title =
                rawContent
                    .substringBefore(" — ")
                    .trim()

            if (title.isBlank()) {
                return@forEach
            }

            val explicitDetails =
                rawContent
                    .substringAfter(
                        " — ",
                        missingDelimiterValue = ""
                    )
                    .trim()
                    .takeIf { it.isNotBlank() }

            /*
             * תת־נושא מקבל עדיפות; אם אין תת־נושא
             * משתמשים בשם הנושא הראשי.
             */
            val groupTitle =
                currentSection
                    ?.takeIf { it.isNotBlank() }
                    ?: currentTopic
                        ?.takeIf { it.isNotBlank() }

            val subtitle =
                explicitDetails ?: groupTitle

            val stableKey =
                listOf(
                    currentTopic.orEmpty()
                        .lowercase()
                        .trim(),
                    currentSection.orEmpty()
                        .lowercase()
                        .trim(),
                    title.lowercase().trim()
                )
                    .joinToString("|")
                    .hashCode()
                    .toUInt()
                    .toString(16)

            parsedItems +=
                AssistantResultItem(
                    id = "material_$stableKey",
                    title = title,
                    subtitle = subtitle,
                    description =
                        currentTopic
                            ?.takeIf { topic ->
                                topic.isNotBlank() &&
                                        topic != subtitle
                            },
                    source =
                        AssistantKnowledgeSource.MATERIAL,
                    topicName = groupTitle,
                    matchQuality =
                        AssistantMatchQuality.HIGH
                )
        }

        val distinctItems =
            parsedItems
                .distinctBy { item ->
                    listOf(
                        item.description.orEmpty()
                            .lowercase()
                            .trim(),
                        item.subtitle.orEmpty()
                            .lowercase()
                            .trim(),
                        item.title
                            .lowercase()
                            .trim()
                    ).joinToString("|")
                }
                .take(60)

        return ParsedMaterialResult(
            introduction = introduction,
            items = distinctItems
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

    /**
     * מזהה בקשה להסבר על תרגיל גם כאשר הכוונה הראשית
     * של המסך נשארה MATERIAL.
     */
    private fun isExerciseExplanationRequest(
        question: String
    ): Boolean {
        val normalized =
            question
                .lowercase()
                .replace("־", " ")
                .replace("–", " ")
                .replace("—", " ")
                .replace("-", " ")
                .replace("?", " ")
                .replace("!", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        if (normalized.isBlank()) {
            return false
        }

        val hasExplanationExpression =
            listOf(
                "הסבר",
                "תסביר",
                "תסבירי",
                "איך עושים",
                "איך מבצעים",
                "איך לבצע",
                "תן פירוט",
                "תני פירוט",
                "פרט על",
                "תפרט",
                "explain",
                "how to do",
                "how do i do",
                "how to perform",
                "how is it performed",
                "give details"
            ).any { expression ->
                normalized.contains(expression)
            }

        if (!hasExplanationExpression) {
            return false
        }

        /*
         * בקשה להסבר על השוואה, חגורה או חומר מלא
         * נשארת במנוע חומר ק.מ.י.
         */
        val isClearlyMaterialQuestion =
            listOf(
                "מה ההבדל",
                "השווה",
                "השוואה",
                "לעומת",
                "כל החומר",
                "חומר מלא",
                "כמה תרגילים",
                "כמה נושאים",
                "difference between",
                "compare",
                "comparison",
                "full material",
                "all material",
                "how many exercises",
                "how many topics"
            ).any { expression ->
                normalized.contains(expression)
            }

        if (isClearlyMaterialQuestion) {
            return false
        }

        /*
         * מספר מתוך הרשימה או שם תרגיל לאחר מילת הסבר
         * נחשבים בקשת הסבר לתרגיל.
         */
        val referencesListItem =
            Regex(
                pattern =
                    """(?:מספר|תרגיל|פריט|אפשרות)\s*\d+"""
            ).containsMatchIn(normalized) ||
                    Regex(
                        pattern =
                            """(?:number|exercise|item|option)\s*\d+"""
                    ).containsMatchIn(normalized)

        val wordsAfterExplanation =
            normalized
                .replace("תן לי הסבר על", " ")
                .replace("תני לי הסבר על", " ")
                .replace("תן הסבר על", " ")
                .replace("תני הסבר על", " ")
                .replace("תסביר לי על", " ")
                .replace("תסביר על", " ")
                .replace("הסבר על", " ")
                .replace("הסבר", " ")
                .replace("איך עושים את", " ")
                .replace("איך עושים", " ")
                .replace("איך מבצעים את", " ")
                .replace("איך מבצעים", " ")
                .replace(
                    "explain the exercise",
                    " "
                )
                .replace("explain", " ")
                .replace("how to perform", " ")
                .replace("how to do", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        return referencesListItem ||
                wordsAfterExplanation.isNotBlank()
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
        return emptyList()
    }

    private fun materialActions(
        question: String,
        isEnglish: Boolean
    ): List<AssistantSuggestedAction> {
        return listOf(
            AssistantSuggestedAction(
                id = "material_full_belt",
                labelHe = "כל חומר החגורה",
                labelEn = "Full belt material",
                queryHe = "תן לי את כל החומר בחגורה שלי",
                queryEn = "Show me all material for my belt",
                targetMode =
                    AssistantIntent.SEARCH_MATERIAL
            ),
            AssistantSuggestedAction(
                id = "material_count",
                labelHe = "כמה תרגילים?",
                labelEn = "How many exercises?",
                queryHe = "כמה תרגילים יש בחגורה שלי?",
                queryEn = "How many exercises are in my belt?",
                targetMode =
                    AssistantIntent.SEARCH_MATERIAL
            ),
            AssistantSuggestedAction(
                id = "material_locate",
                labelHe = "איתור תרגיל",
                labelEn = "Locate exercise",
                queryHe = "באיזו חגורה נמצא התרגיל $question?",
                queryEn = "Which belt contains the exercise $question?",
                targetMode =
                    AssistantIntent.SEARCH_MATERIAL
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