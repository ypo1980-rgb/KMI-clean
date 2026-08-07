package il.kmi.app.ui.assistant.core

import il.kmi.shared.domain.Belt

/**
 * תוצר מלא של סבב שיחה אחד.
 *
 * המסך מקבל גם את התוצאה להצגה וגם את ההקשר המעודכן,
 * בלי להפעיל בעצמו את מנועי החיפוש.
 */
data class AssistantOrchestratorResponse(
    val result: AssistantResult,
    val resolution: AssistantIntentResolution,
    val context: AssistantConversationContext
)

/**
 * מנהל השיחה המרכזי של יובל.
 *
 * סדר הפעולות:
 * 1. בדיקת תשובה לשאלת הבהרה קודמת.
 * 2. זיהוי הכוונה והישויות.
 * 3. השלמת הקשר משאלות קודמות.
 * 4. הפנייה למאגר הידע המתאים.
 * 5. עדכון זיכרון השיחה.
 */
class AssistantOrchestrator(
    initialContext: AssistantConversationContext =
        AssistantConversationContext()
) {

    private var conversationContext: AssistantConversationContext =
        initialContext

    /**
     * עיבוד בקשה חדשה.
     *
     * הפונקציה מסונכרנת כדי למנוע מצב שבו שתי תשובות
     * קוליות או לחיצות מעדכנות יחד את אותו הקשר.
     */
    @Synchronized
    fun process(
        question: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse {
        val cleanQuestion = question.trim()

        if (cleanQuestion.isBlank()) {
            val resolution = emptyResolution(cleanQuestion)

            val result = AssistantResult.Error(
                originalQuestion = cleanQuestion,
                resolvedQuestion = cleanQuestion,
                userMessage = localized(
                    isEnglish = isEnglish,
                    he = "לא התקבלה שאלה. אפשר לכתוב או לומר בקשה.",
                    en = "No question was received. You can type or say a request."
                ),
                technicalMessage = "Empty orchestrator request",
                recoverable = true
            )

            return AssistantOrchestratorResponse(
                result = result,
                resolution = resolution,
                context = conversationContext
            )
        }

        /*
         * תחילה בודקים אם המשתמש מבקש להפוך את
         * התוצאה הקודמת לרשימה, בלי לחזור על הנושא
         * ועל החגורה.
         *
         * לדוגמה:
         * "תן את הרשימה"
         * "תציג את כולם"
         * "מה השמות שלהם"
         */
        resolveContextualExerciseListFollowUp(
            question = cleanQuestion,
            isEnglish = isEnglish
        )?.let { contextualResponse ->
            return contextualResponse
        }

        /*
         * לאחר מכן בודקים אם המשתמש מבקש להמשיך לדבר
         * על התרגיל הפעיל בלי לומר שוב את שמו.
         *
         * לדוגמה:
         * "תסביר אותו"
         * "תן עליו הסבר"
         * "איך מבצעים אותו"
         * "ומה ההסבר שלו"
         */
        resolveContextualExerciseFollowUp(
            question = cleanQuestion,
            isEnglish = isEnglish
        )?.let { contextualResponse ->
            return contextualResponse
        }

        /*
         * לאחר מכן בודקים אם המשתמש מתקן או משלים את
         * השאלה הקודמת באמצעות חגורה.
         *
         * לדוגמה:
         * "התכוונתי לתרגיל שמצאת בחגורה ירוקה"
         * "לא, התרגיל בחגורה הכחולה"
         * "זה שמופיע בחגורה חומה"
         */
        resolveContextualBeltCorrection(
            question = cleanQuestion,
            isEnglish = isEnglish
        )?.let { contextualResponse ->
            return contextualResponse
        }

        /**
         * אם יובל כבר הציג מספר אפשרויות, קודם נבדוק
         * האם המשתמש בחר אחת מהן.
         */
        resolvePreviousClarification(
            answer = cleanQuestion,
            isEnglish = isEnglish
        )?.let { clarificationResponse ->
            return clarificationResponse
        }

        /*
         * ResultList רגיל אינו מסומן כ־awaitingClarification.
         * לכן בודקים בנפרד אם המשתמש ביקש הסבר על פריט
         * מתוך הרשימה האחרונה.
         */
        resolvePreviousResultReference(
            question = cleanQuestion,
            isEnglish = isEnglish
        )?.let { resultResponse ->
            return resultResponse
        }

        val detectedResolution = AssistantIntentResolver.resolve(
            question = cleanQuestion,
            context = conversationContext
        )

        val normalizedQuestion = cleanQuestion
            .lowercase()
            .replace("־", " ")
            .replace("–", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        /*
         * שאלת אימונים שמכילה גם ביטוי של זמן היא בקשה ברורה.
         * גם אם ה־Resolver מתלבט בין "האימון הבא",
         * "רשימת אימונים" או "אימוני השבוע הבא" —
         * כל האפשרויות מופנות לאותו מנוע אימונים,
         * ולכן אין צורך לשאול את המשתמש למה התכוון.
         */
        val hasTrainingMarker = listOf(
            "אימון",
            "אימונים",
            "האימון",
            "האימונים",
            "training",
            "trainings",
            "workout",
            "workouts",
            "class",
            "classes"
        ).any { marker ->
            marker in normalizedQuestion
        }

        val hasTrainingTimeMarker = listOf(
            "מתי",
            "הבא",
            "הבאים",
            "קרוב",
            "קרובים",
            "השבוע",
            "שבוע הבא",
            "היום",
            "מחר",
            "באיזה יום",
            "באיזו שעה",
            "when",
            "next",
            "upcoming",
            "this week",
            "next week",
            "today",
            "tomorrow",
            "what time",
            "which day"
        ).any { marker ->
            marker in normalizedQuestion
        }

        /*
         * בקשת רשימה מפורשת מקבלת עדיפות על התאמה
         * אפשרית לשם של תרגיל יחיד.
         *
         * לדוגמה:
         * "תציג את כל תרגילי הסכין בחגורה ירוקה"
         */
        val hasExerciseListMarker =
            listOf(
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
                marker in normalizedQuestion
            }

        val resolution =
            if (hasExerciseListMarker) {
                detectedResolution.copy(
                    intent =
                        AssistantIntent.LIST_EXERCISES,
                    source =
                        AssistantKnowledgeSource.EXERCISES,
                    confidence =
                        maxOf(
                            detectedResolution.confidence,
                            0.98f
                        ),
                    alternatives =
                        emptyList(),
                    requiresClarification =
                        false
                )
            } else if (
                hasTrainingMarker &&
                hasTrainingTimeMarker
            ) {
                detectedResolution.copy(
                    intent = when {
                        "שבוע הבא" in normalizedQuestion ||
                                "next week" in normalizedQuestion ||
                                "האימונים" in normalizedQuestion ||
                                "אימונים" in normalizedQuestion ||
                                "trainings" in normalizedQuestion ||
                                "classes" in normalizedQuestion ->
                            AssistantIntent.LIST_TRAININGS

                        else ->
                            AssistantIntent.NEXT_TRAINING
                    },
                    source = AssistantKnowledgeSource.TRAININGS,
                    confidence = maxOf(
                        detectedResolution.confidence,
                        0.95f
                    ),
                    alternatives = emptyList(),
                    requiresClarification = false
                )
            } else {
                detectedResolution
            }

        conversationContext =
            conversationContext.withDetectedRequest(
                detectedIntent = resolution.intent,
                detectedSource = resolution.source,
                detectedExerciseName = resolution.exerciseName,
                detectedTopicName = resolution.topicName,
                detectedBelt = resolution.belt,
                userQuestion = cleanQuestion,
                resolvedQuestion = resolution.resolvedQuestion
            )

        if (resolution.requiresClarification) {
            val clarificationResult = createIntentClarification(
                question = cleanQuestion,
                resolution = resolution,
                isEnglish = isEnglish
            )

            conversationContext =
                conversationContext.awaitingClarification(
                    question = clarificationResult.question,
                    options = clarificationResult.options.map {
                        it.title
                    },
                    results = clarificationResult.options.map {
                        it.toContextResult()
                    }
                )

            return AssistantOrchestratorResponse(
                result = clarificationResult,
                resolution = resolution,
                context = conversationContext
            )
        }

        return executeResolution(
            originalQuestion = cleanQuestion,
            resolution = resolution,
            isEnglish = isEnglish
        )
    }

    /**
     * החזרת תמונת מצב לקריאה בלבד.
     */
    @Synchronized
    fun currentContext(): AssistantConversationContext {
        return conversationContext.copy(
            lastResults = conversationContext.lastResults.toList(),
            recentTurns = conversationContext.recentTurns.toList(),
            clarificationOptions =
                conversationContext.clarificationOptions.toList()
        )
    }

    /**
     * פתיחת שיחה חדשה.
     */
    @Synchronized
    fun reset() {
        conversationContext =
            AssistantConversationContext()
    }

    /**
     * ביטול שאלת ההבהרה, תוך שמירת נושא השיחה.
     */
    @Synchronized
    fun cancelClarification() {
        conversationContext =
            conversationContext.clearClarification()
    }

    /**
     * בחירה ישירה של אפשרות לפי המיקום שלה.
     *
     * position מתחיל ב-1, כפי שמוצג למשתמש.
     */
    @Synchronized
    fun selectClarificationOption(
        position: Int,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        val selected =
            conversationContext.resultAt(position)
                ?: return null

        return processSelectedContextResult(
            selected = selected,
            isEnglish = isEnglish
        )
    }

    private fun executeResolution(
        originalQuestion: String,
        resolution: AssistantIntentResolution,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse {
        val result = AssistantKnowledgeRepository.query(
            AssistantKnowledgeRequest(
                originalQuestion = originalQuestion,
                resolvedQuestion = resolution.resolvedQuestion,
                intent = resolution.intent,
                preferredBelt = resolution.belt,
                isEnglish = isEnglish
            )
        )

        conversationContext =
            updateContextFromResult(
                context = conversationContext,
                result = result
            )

        return AssistantOrchestratorResponse(
            result = result,
            resolution = resolution,
            context = conversationContext
        )
    }

    /**
     * הופך שאלת ספירה או חיפוש קודמת לבקשת רשימה,
     * תוך שמירת הנושא והחגורה מההקשר.
     *
     * לדוגמה:
     *
     * שאלה קודמת:
     * "כמה תרגילים עם בעיטות יש בחגורה צהובה"
     *
     * שאלת המשך:
     * "תן את הרשימה"
     *
     * שאלה שנשלחת למנוע:
     * "תן את רשימת התרגילים לפי הבקשה הקודמת:
     * כמה תרגילים עם בעיטות יש בחגורה צהובה"
     */
    private fun resolveContextualExerciseListFollowUp(
        question: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        if (!conversationContext.hasConversationSubject()) {
            return null
        }

        val normalizedQuestion =
            normalizeSelectionText(question)

        val isShortListFollowUp =
            listOf(
                "תן את הרשימה",
                "תני את הרשימה",
                "תן רשימה",
                "תני רשימה",

                "תן את רשימת התרגילים",
                "תני את רשימת התרגילים",
                "תן רשימת תרגילים",
                "תני רשימת תרגילים",
                "תציג את רשימת התרגילים",
                "תציגי את רשימת התרגילים",
                "הצג את רשימת התרגילים",
                "הציגי את רשימת התרגילים",
                "תראה את רשימת התרגילים",
                "תראי את רשימת התרגילים",
                "רשימת התרגילים",
                "רשימת תרגילים",

                "תציג את הרשימה",
                "תציגי את הרשימה",
                "הצג את הרשימה",
                "הציגי את הרשימה",
                "תראה את הרשימה",
                "תראי את הרשימה",
                "תראה את כולם",
                "תראי את כולם",
                "תציג את כולם",
                "תציגי את כולם",
                "תן את כולם",
                "תני את כולם",
                "מה השמות שלהם",
                "מה השמות שלהן",
                "מהם השמות",
                "מה הן השמות",
                "list them",
                "show the list",
                "show them all",
                "give me the list",
                "what are their names"
            ).any { marker ->
                normalizedQuestion == marker ||
                        normalizedQuestion.startsWith(
                            "$marker "
                        )
            }

        if (!isShortListFollowUp) {
            return null
        }

        val previousQuestion =
            conversationContext.lastResolvedQuestion
                ?.trim()
                ?.takeIf { previous ->
                    previous.isNotBlank() &&
                            !previous.equals(
                                question,
                                ignoreCase = true
                            )
                }
                ?: conversationContext.lastUserQuestion
                    ?.trim()
                    ?.takeIf { previous ->
                        previous.isNotBlank() &&
                                !previous.equals(
                                    question,
                                    ignoreCase = true
                                )
                    }
                ?: return null

        /*
         * משאירים את השאלה הקודמת בשלמותה כדי לשמר:
         * נושא, תת־נושא, חגורה ומילות סינון.
         *
         * ExerciseAssistantEngine מסיר בעצמו את מילות
         * הספירה ומזהה את בקשת הרשימה.
         */
        val contextualQuestion =
            if (isEnglish) {
                buildString {
                    append("Show the exercise list. ")
                    append(previousQuestion)
                }
            } else {
                buildString {
                    append("תן את רשימת התרגילים. ")
                    append(previousQuestion)
                }
            }
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        /*
         * החגורה שנאמרה בשאלה הקודמת קודמת לחגורת
         * הפרופיל שנשמרה קודם בהקשר.
         *
         * לדוגמה, אם חגורת המשתמש כתומה אבל הוא שאל
         * על חגורה ירוקה, שאלת ההמשך חייבת להישאר
         * בחגורה הירוקה.
         */
        val contextualBelt =
            detectReferencedBelt(
                previousQuestion
            )
                ?: detectReferencedBelt(
                    contextualQuestion
                )
                ?: conversationContext.belt

        val contextForResolution =
            conversationContext.copy(
                belt = contextualBelt,
                updatedAtMillis =
                    System.currentTimeMillis()
            )

        val detectedResolution =
            AssistantIntentResolver.resolve(
                question = contextualQuestion,
                context = contextForResolution
            )

        /*
         * שאלת ההמשך מבקשת רשימה במפורש.
         * המקור נשאר תרגילים, והנושא והחגורה
         * נלקחים מהבקשה הקודמת.
         */
        val contextualResolution =
            detectedResolution.copy(
                originalQuestion = question,
                resolvedQuestion = contextualQuestion,
                intent = AssistantIntent.LIST_EXERCISES,
                source = AssistantKnowledgeSource.EXERCISES,
                confidence = 1f,
                exerciseName =
                    conversationContext.exerciseName,
                topicName =
                    conversationContext.topicName,
                belt =
                    contextualBelt,
                isFollowUp = true,
                alternatives = emptyList(),
                requiresClarification = false
            )

        conversationContext =
            conversationContext.withDetectedRequest(
                detectedIntent =
                    contextualResolution.intent,
                detectedSource =
                    contextualResolution.source,
                detectedExerciseName =
                    contextualResolution.exerciseName,
                detectedTopicName =
                    contextualResolution.topicName,
                detectedSubTopicName =
                    conversationContext.subTopicName,
                detectedBelt =
                    contextualBelt,
                userQuestion =
                    question,
                resolvedQuestion =
                    contextualQuestion
            )

        return executeResolution(
            originalQuestion = question,
            resolution = contextualResolution,
            isEnglish = isEnglish
        )
    }

    /**
     * פותר שאלת המשך שמתייחסת לתרגיל הפעיל בלי
     * שהמשתמש אומר שוב את שמו.
     *
     * לדוגמה:
     * "תסביר אותו"
     * "תן עליו הסבר"
     * "איך מבצעים אותו"
     * "ומה ההסבר שלו"
     */
    private fun resolveContextualExerciseFollowUp(
        question: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        val rememberedExerciseName =
            conversationContext.exerciseName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: conversationContext.lastResults
                    .singleOrNull()
                    ?.exerciseName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: conversationContext.lastResults
                    .singleOrNull()
                    ?.title
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: return null

        val normalizedQuestion =
            normalizeSelectionText(question)

        val hasExplanationRequest =
            listOf(
                "הסבר",
                "תסביר",
                "תסבירי",
                "תפרט",
                "פרט",
                "איך עושים",
                "איך מבצעים",
                "איך לבצע",
                "explain",
                "explanation",
                "give details",
                "how to do",
                "how do i do",
                "how to perform"
            ).any { marker ->
                marker in normalizedQuestion
            }

        if (!hasExplanationRequest) {
            return null
        }

        val hasContextReference =
            listOf(
                "אותו",
                "אותה",
                "עליו",
                "עליה",
                "שלו",
                "שלה",
                "הזה",
                "הזאת",
                "התרגיל",
                "it",
                "that one",
                "this one",
                "the exercise"
            ).any { marker ->
                marker in normalizedQuestion
            }

        /*
         * אם המשתמש אמר שם מפורש של תרגיל חדש,
         * זו אינה שאלת המשך ויש לנתח אותה כבקשה חדשה.
         */
        if (!hasContextReference) {
            return null
        }

        val rememberedBelt =
            conversationContext.belt

        val contextualQuestion =
            buildString {
                if (isEnglish) {
                    append("Explain exercise ")
                    append(rememberedExerciseName)

                    rememberedBelt?.let { belt ->
                        append(" ")
                        append(belt.name)
                    }
                } else {
                    append("הסבר על תרגיל ")
                    append(rememberedExerciseName)

                    rememberedBelt?.let { belt ->
                        append(" ")
                        append(belt.heb)
                    }
                }
            }
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        val detectedResolution =
            AssistantIntentResolver.resolve(
                question = contextualQuestion,
                context = conversationContext
            )

        val contextualResolution =
            detectedResolution.copy(
                originalQuestion = question,
                resolvedQuestion = contextualQuestion,
                intent = AssistantIntent.EXPLAIN_EXERCISE,
                source = AssistantKnowledgeSource.EXERCISES,
                confidence = 1f,
                exerciseName = rememberedExerciseName,
                belt = rememberedBelt,
                isFollowUp = true,
                alternatives = emptyList(),
                requiresClarification = false
            )

        conversationContext =
            conversationContext.withDetectedRequest(
                detectedIntent =
                    contextualResolution.intent,
                detectedSource =
                    contextualResolution.source,
                detectedExerciseName =
                    rememberedExerciseName,
                detectedTopicName =
                    conversationContext.topicName,
                detectedSubTopicName =
                    conversationContext.subTopicName,
                detectedBelt =
                    rememberedBelt,
                userQuestion =
                    question,
                resolvedQuestion =
                    contextualQuestion
            )

        return executeResolution(
            originalQuestion = question,
            resolution = contextualResolution,
            isEnglish = isEnglish
        )
    }

    /**
     * פותר תיקון או השלמה שמתייחסים לשאלה הקודמת
     * באמצעות חגורה.
     *
     * אם קיימת תוצאה קודמת יחידה בחגורה המבוקשת,
     * היא נבחרת ישירות.
     *
     * אם אין תוצאה כזאת ברשימה האחרונה, מריצים מחדש
     * את השאלה הקודמת עם החגורה החדשה.
     */
    private fun resolveContextualBeltCorrection(
        question: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        if (!conversationContext.hasConversationSubject()) {
            return null
        }

        val normalizedQuestion =
            normalizeSelectionText(question)

        val hasContextReference =
            listOf(
                "התכוונתי",
                "אני מתכוון",
                "לא לזה",
                "לא זה",
                "זה שמצאת",
                "זה שהצגת",
                "התרגיל שמצאת",
                "התרגיל שהצגת",
                "התרגיל בחגורה",
                "זה שבחגורה",
                "בחגורה",
                "אותו תרגיל",
                "אותו אחד",
                "meant",
                "i mean",
                "the one you found",
                "the exercise you found",
                "the one in the",
                "in the belt"
            ).any { marker ->
                marker in normalizedQuestion
            }

        if (!hasContextReference) {
            return null
        }

        val referencedBelt =
            detectReferencedBelt(question)
                ?: return null

        /*
         * תיקון חגורה אינו בהכרח בחירה של תרגיל יחיד.
         *
         * השאלה הקודמת יכולה להיות בקשת ספירה,
         * רשימת תרגילים או הסבר. לכן משחזרים תחילה
         * את הבקשה הקודמת ורק מחליפים את החגורה.
         */
        val previousQuestion =
            conversationContext.lastResolvedQuestion
                ?.trim()
                ?.takeIf { previous ->
                    previous.isNotBlank() &&
                            !previous.equals(
                                question,
                                ignoreCase = true
                            )
                }
                ?: conversationContext.lastUserQuestion
                    ?.trim()
                    ?.takeIf { previous ->
                        previous.isNotBlank() &&
                                !previous.equals(
                                    question,
                                    ignoreCase = true
                                )
                    }
                ?: return null

        val previousQuestionWithoutBelt =
            removeBeltReferences(previousQuestion)
                /*
                 * אם זיהוי הדיבור שמר רק את המילה
                 * "חגורה" ללא צבע, מסירים אותה לפני
                 * שמצרפים את החגורה המתוקנת.
                 */
                .replace(
                    Regex(
                        """(?:^|\s)ב?חגורה\s*$""",
                        RegexOption.IGNORE_CASE
                    ),
                    " "
                )
                .replace(
                    Regex(
                        """(?:^|\s)(?:in\s+the\s+)?belt\s*$""",
                        RegexOption.IGNORE_CASE
                    ),
                    " "
                )
                /*
                 * לאחר הסרת "חגורה כתומה" עשויה
                 * להישאר האות ב' לבדה בסוף המשפט.
                 */
                .replace(
                    Regex("""(?:^|\s)ב\s*$"""),
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()
                .takeIf { previous ->
                    previous.isNotBlank()
                }
                ?: return null

        val contextualQuestion =
            buildString {
                append(previousQuestionWithoutBelt)

                if (isEnglish) {
                    append(" in the ")
                    append(
                        referencedBelt.name
                            .lowercase()
                    )
                    append(" belt")
                } else {
                    append(" ב")
                    append(referencedBelt.heb)
                }
            }
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        val contextWithCorrectedBelt =
            conversationContext
                .clearClarification()
                .copy(
                    belt = referencedBelt,
                    lastUserQuestion = question,
                    lastResolvedQuestion =
                        contextualQuestion,
                    updatedAtMillis =
                        System.currentTimeMillis()
                )

        val detectedResolution =
            AssistantIntentResolver.resolve(
                question = contextualQuestion,
                context = contextWithCorrectedBelt
            )

        /*
         * תיקון חגורה צריך לשמור את משמעות הבקשה:
         *
         * - שאלת ספירה נשארת שאלת ספירה.
         * - בקשת רשימה נשארת בקשת רשימה.
         * - בקשת הסבר נשארת בקשת הסבר.
         */
        val normalizedContextualQuestion =
            normalizeSelectionText(
                contextualQuestion
            )

        val looksLikeCountQuestion =
            listOf(
                "כמה",
                "מספר התרגילים",
                "כמות התרגילים",
                "how many",
                "number of exercises"
            ).any { marker ->
                marker in normalizedContextualQuestion
            }

        val looksLikeListQuestion =
            listOf(
                "איזה תרגילים",
                "אילו תרגילים",
                "איזה הגנות",
                "אילו הגנות",
                "כל התרגילים",
                "כל ההגנות",
                "רשימת תרגילים",
                "רשימת הגנות",
                "which exercises",
                "what exercises",
                "all exercises",
                "exercise list",
                "list of exercises"
            ).any { marker ->
                marker in normalizedContextualQuestion
            }

        val looksLikeExplanationQuestion =
            listOf(
                "הסבר",
                "תסביר",
                "תסבירי",
                "איך עושים",
                "איך מבצעים",
                "איך לבצע",
                "explain",
                "explanation",
                "how to do",
                "how to perform"
            ).any { marker ->
                marker in normalizedContextualQuestion
            }

        val rememberedIntent =
            conversationContext.intent
                ?.takeIf { previousIntent ->
                    previousIntent !=
                            AssistantIntent.UNKNOWN
                }

        val correctedIntent: AssistantIntent =
            when {
                looksLikeCountQuestion ->
                    AssistantIntent.EXERCISE

                looksLikeListQuestion ->
                    AssistantIntent.LIST_EXERCISES

                looksLikeExplanationQuestion ->
                    AssistantIntent.EXPLAIN_EXERCISE

                rememberedIntent != null ->
                    rememberedIntent

                else ->
                    detectedResolution.intent
            }

        val correctedSource: AssistantKnowledgeSource =
            when (correctedIntent) {
                AssistantIntent.EXERCISE,
                AssistantIntent.EXPLAIN_EXERCISE,
                AssistantIntent.SEARCH_EXERCISE,
                AssistantIntent.LIST_EXERCISES ->
                    AssistantKnowledgeSource.EXERCISES

                else ->
                    detectedResolution.source
            }

        val correctedResolution =
            detectedResolution.copy(
                originalQuestion = question,
                resolvedQuestion = contextualQuestion,
                intent = correctedIntent,
                source = correctedSource,
                confidence =
                    maxOf(
                        detectedResolution.confidence,
                        0.98f
                    ),
                exerciseName =
                    detectedResolution.exerciseName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: conversationContext.exerciseName
                            ?.trim()
                            ?.takeIf { it.isNotBlank() },
                belt = referencedBelt,
                isFollowUp = true,
                alternatives = emptyList(),
                requiresClarification = false
            )

        conversationContext =
            contextWithCorrectedBelt.withDetectedRequest(
                detectedIntent =
                    correctedResolution.intent,
                detectedSource =
                    correctedResolution.source,
                detectedExerciseName =
                    correctedResolution.exerciseName,
                detectedTopicName =
                    correctedResolution.topicName,
                detectedBelt =
                    referencedBelt,
                userQuestion =
                    question,
                resolvedQuestion =
                    contextualQuestion
            )

        return executeResolution(
            originalQuestion = question,
            resolution = correctedResolution,
            isEnglish = isEnglish
        )
    }

    /**
     * מזהה חגורה שנאמרה במפורש בשאלת המשך.
     */
    private fun detectReferencedBelt(
        text: String
    ): Belt? {
        val normalizedText =
            normalizeSelectionText(text)

        return enumValues<Belt>()
            .firstOrNull { belt ->
                val beltValues =
                    listOf(
                        belt.id,
                        belt.name,
                        belt.heb,
                        belt.heb
                            .replace(
                                "חגורה",
                                ""
                            )
                            .trim()
                    )
                        .map(::normalizeSelectionText)
                        .filter { value ->
                            value.isNotBlank()
                        }

                beltValues.any { value ->
                    value in normalizedText
                }
            }
    }

    /**
     * מסיר אזכור של חגורה קודמת לפני שמצרפים
     * את החגורה החדשה לשאלה שנפתרת מחדש.
     */
    private fun removeBeltReferences(
        text: String
    ): String {
        return enumValues<Belt>()
            .fold(text) { currentText, belt ->
                listOf(
                    belt.heb,
                    belt.name,
                    belt.id
                )
                    .fold(currentText) {
                            updatedText,
                            beltValue ->

                        updatedText.replace(
                            oldValue = beltValue,
                            newValue = " ",
                            ignoreCase = true
                        )
                    }
            }
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    /**
     * פותר בקשת הסבר על פריט מתוך ResultList קודם.
     *
     * לדוגמה:
     * "תסביר את תרגיל 3"
     * "הסבר על מספר 2"
     * "תסביר את בעיטת המגל"
     */
    private fun resolvePreviousResultReference(
        question: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        if (conversationContext.lastResults.isEmpty()) {
            return null
        }

        val normalized =
            normalizeSelectionText(question)

        val explanationRequested =
            listOf(
                "הסבר",
                "תסביר",
                "תסבירי",
                "איך עושים",
                "איך מבצעים",
                "איך לבצע",
                "פרט",
                "תפרט",
                "explain",
                "how to do",
                "how do i do",
                "how to perform",
                "give details"
            ).any { marker ->
                normalized.contains(marker)
            }

        if (!explanationRequested) {
            return null
        }

        /*
         * ניסיון ראשון: בחירה לפי מספר או מילת מיקום.
         */
        val selectedByPosition =
            parseSelectionPosition(question)
                ?.let { position ->
                    conversationContext.resultAt(position)
                }

        if (selectedByPosition != null) {
            return processSelectedContextResult(
                selected = selectedByPosition,
                isEnglish = isEnglish,
                forcedIntent =
                    AssistantIntent.EXPLAIN_EXERCISE
            )
        }

        /*
         * ניסיון שני: מסירים את ביטויי ההסבר ומשווים
         * את הטקסט שנותר לשמות הפריטים ברשימה.
         */
        val requestedItemText =
            normalized
                .replace("תן לי הסבר על", " ")
                .replace("תני לי הסבר על", " ")
                .replace("תן הסבר על", " ")
                .replace("תני הסבר על", " ")
                .replace("תסביר לי על", " ")
                .replace("תסבירי לי על", " ")
                .replace("תסביר את", " ")
                .replace("תסבירי את", " ")
                .replace("תסביר על", " ")
                .replace("תסבירי על", " ")
                .replace("הסבר על", " ")
                .replace("הסבר", " ")
                .replace("איך עושים את", " ")
                .replace("איך עושים", " ")
                .replace("איך מבצעים את", " ")
                .replace("איך מבצעים", " ")
                .replace("explain the exercise", " ")
                .replace("explain exercise", " ")
                .replace("explain", " ")
                .replace("how to perform", " ")
                .replace("how to do", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        if (requestedItemText.isBlank()) {
            return null
        }

        val selectedByName =
            conversationContext.lastResults
                .map { result ->
                    result to textSimilarity(
                        requestedItemText,
                        normalizeSelectionText(result.title)
                    )
                }
                .filter { (_, similarity) ->
                    similarity >=
                            MIN_SELECTION_SIMILARITY
                }
                .maxByOrNull { (_, similarity) ->
                    similarity
                }
                ?.first

        if (selectedByName != null) {
            return processSelectedContextResult(
                selected = selectedByName,
                isEnglish = isEnglish,
                forcedIntent =
                    AssistantIntent.EXPLAIN_EXERCISE
            )
        }

        return null
    }

    private fun resolvePreviousClarification(
        answer: String,
        isEnglish: Boolean
    ): AssistantOrchestratorResponse? {
        if (!conversationContext.awaitingClarification) {
            return null
        }

        val selectedPosition =
            parseSelectionPosition(answer)

        if (selectedPosition != null) {
            val selected =
                conversationContext.resultAt(selectedPosition)

            if (selected != null) {
                return processSelectedContextResult(
                    selected = selected,
                    isEnglish = isEnglish
                )
            }
        }

        val selectedByText =
            findClarificationResultByText(answer)

        if (selectedByText != null) {
            return processSelectedContextResult(
                selected = selectedByText,
                isEnglish = isEnglish
            )
        }

        /**
         * המשתמש לא בחר אפשרות אלא ניסח בקשה חדשה.
         * מנקים את מצב ההבהרה ומנתחים אותה כרגיל.
         */
        conversationContext =
            conversationContext.clearClarification()

        return null
    }

    private fun processSelectedContextResult(
        selected: AssistantContextResult,
        isEnglish: Boolean,
        forcedIntent: AssistantIntent? = null
    ): AssistantOrchestratorResponse {
        /*
         * פריט שמקורו MATERIAL נשאר פריט חומר בדרך כלל.
         * כאשר המשתמש ביקש הסבר, כופים EXPLAIN_EXERCISE.
         */
        val selectedIntent =
            forcedIntent
                ?: intentForSource(selected.source)

        val selectedQuestion = buildSelectedQuestion(
            selected = selected,
            intent = selectedIntent,
            isEnglish = isEnglish
        )

        conversationContext =
            conversationContext
                .clearClarification()
                .copy(
                    intent = selectedIntent,
                    source = selected.source,
                    /*
                     * אם exerciseName לא מולא בפריט הרשימה,
                     * הכותרת עצמה היא שם התרגיל שנבחר.
                     */
                    exerciseName =
                        selected.exerciseName
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: selected.title
                                .trim()
                                .takeIf { it.isNotBlank() }
                            ?: conversationContext.exerciseName,
                    topicName =
                        selected.topicName
                            ?: conversationContext.topicName,
                    belt =
                        selected.belt
                            ?: conversationContext.belt,
                    lastUserQuestion = selectedQuestion,
                    lastResolvedQuestion = selectedQuestion
                )

        val resolution = AssistantIntentResolution(
            originalQuestion = selectedQuestion,
            normalizedQuestion =
                selectedQuestion.lowercase().trim(),
            resolvedQuestion = selectedQuestion,
            intent = selectedIntent,
            source = selected.source,
            confidence = 1f,
            exerciseName =
                selected.exerciseName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: selected.title
                        .trim()
                        .takeIf { it.isNotBlank() },
            topicName =
                selected.topicName,
            belt =
                selected.belt
                    ?: conversationContext.belt,
            isFollowUp = true,
            alternatives = emptyList(),
            requiresClarification = false
        )

        return executeResolution(
            originalQuestion = selectedQuestion,
            resolution = resolution,
            isEnglish = isEnglish
        )
    }

    private fun createIntentClarification(
        question: String,
        resolution: AssistantIntentResolution,
        isEnglish: Boolean
    ): AssistantResult.Clarification {
        val possibleIntents =
            buildList {
                if (resolution.intent != AssistantIntent.UNKNOWN) {
                    add(resolution.intent)
                }

                addAll(resolution.alternatives)

                if (isEmpty()) {
                    add(AssistantIntent.EXPLAIN_EXERCISE)
                    add(AssistantIntent.SEARCH_MATERIAL)
                    add(AssistantIntent.LIST_TRAININGS)
                }
            }
                .distinct()
                .take(MAX_INTENT_OPTIONS)

        val options = possibleIntents.map { intent ->
            AssistantResultItem(
                id = "intent_${intent.name.lowercase()}",
                title = intentLabel(
                    intent = intent,
                    isEnglish = isEnglish
                ),
                subtitle = intentDescription(
                    intent = intent,
                    isEnglish = isEnglish
                ),
                source = sourceForIntent(intent),
                belt = resolution.belt,
                topicName = resolution.topicName,
                exerciseName = resolution.exerciseName,
                matchQuality =
                    AssistantMatchQuality.MEDIUM
            )
        }

        return AssistantResult.Clarification(
            originalQuestion = question,
            resolvedQuestion = resolution.resolvedQuestion,
            intent = resolution.intent,
            source = resolution.source,
            question = localized(
                isEnglish = isEnglish,
                he = "כדי לתת תשובה מדויקת, למה התכוונת?",
                en = "To give an accurate answer, what did you mean?"
            ),
            options = options,
            allowManualAnswer = true
        )
    }

    private fun updateContextFromResult(
        context: AssistantConversationContext,
        result: AssistantResult
    ): AssistantConversationContext {
        return when (result) {
            is AssistantResult.Clarification -> {
                context.awaitingClarification(
                    question = result.question,
                    options = result.options.map {
                        it.title
                    },
                    results = result.contextResults()
                )
            }

            is AssistantResult.Answer,
            is AssistantResult.ResultList -> {
                context.withAnswer(
                    answer = result.primaryText(),
                    results = result.contextResults()
                )
            }

            is AssistantResult.NotFound -> {
                context.withAnswer(
                    answer = result.message,
                    results = result.contextResults()
                )
            }

            is AssistantResult.MissingInformation -> {
                context.withAnswer(
                    answer = result.message
                )
            }

            is AssistantResult.Error -> {
                /**
                 * תקלה טכנית אינה מוחקת את נושא השיחה.
                 * המשתמש יוכל ללחוץ "נסה שוב".
                 */
                context.copy(
                    lastAnswer = result.userMessage,
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            }
        }
    }

    private fun findClarificationResultByText(
        answer: String
    ): AssistantContextResult? {
        val normalizedAnswer =
            normalizeSelectionText(answer)

        if (normalizedAnswer.isBlank()) {
            return null
        }

        return conversationContext.lastResults
            .map { result ->
                result to textSimilarity(
                    normalizedAnswer,
                    normalizeSelectionText(result.title)
                )
            }
            .filter { (_, similarity) ->
                similarity >= MIN_SELECTION_SIMILARITY
            }
            .maxByOrNull { (_, similarity) ->
                similarity
            }
            ?.first
    }

    private fun parseSelectionPosition(
        answer: String
    ): Int? {
        val normalized =
            normalizeSelectionText(answer)

        normalized.toIntOrNull()?.let { value ->
            return value.takeIf {
                it in 1..MAX_RESULT_SELECTION_OPTIONS
            }
        }

        /*
         * תומך במשפטים מלאים:
         * "תרגיל 3", "מספר 4", "הסבר על אפשרות 2".
         */
        val explicitNumber =
            Regex(
                """(?:מספר|תרגיל|פריט|אפשרות|number|exercise|item|option)\s*(\d+)"""
            )
                .find(normalized)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (
            explicitNumber != null &&
            explicitNumber in
            1..MAX_RESULT_SELECTION_OPTIONS
        ) {
            return explicitNumber
        }

        /*
         * תומך גם ב"הסבר על 3".
         */
        val standaloneNumber =
            Regex("""(?:^|\s)(\d+)(?:\s|$)""")
                .find(normalized)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (
            standaloneNumber != null &&
            standaloneNumber in
            1..MAX_RESULT_SELECTION_OPTIONS
        ) {
            return standaloneNumber
        }

        val positionWords = linkedMapOf(
            "ראשון" to 1,
            "הראשון" to 1,
            "אחד" to 1,
            "אפשרות אחת" to 1,
            "first" to 1,
            "one" to 1,
            "option one" to 1,

            "שני" to 2,
            "השני" to 2,
            "שתיים" to 2,
            "אפשרות שתיים" to 2,
            "second" to 2,
            "two" to 2,
            "option two" to 2,

            "שלישי" to 3,
            "השלישי" to 3,
            "שלוש" to 3,
            "אפשרות שלוש" to 3,
            "third" to 3,
            "three" to 3,
            "option three" to 3,

            "רביעי" to 4,
            "הרביעי" to 4,
            "ארבע" to 4,
            "fourth" to 4,
            "four" to 4,

            "חמישי" to 5,
            "החמישי" to 5,
            "חמש" to 5,
            "fifth" to 5,
            "five" to 5
        )

        return positionWords.entries
            .firstOrNull { (word, _) ->
                normalized == word ||
                        normalized.contains(word)
            }
            ?.value
    }

    private fun buildSelectedQuestion(
        selected: AssistantContextResult,
        intent: AssistantIntent,
        isEnglish: Boolean
    ): String {
        return when (intent) {
            AssistantIntent.EXPLAIN_EXERCISE,
            AssistantIntent.SEARCH_EXERCISE -> {
                localized(
                    isEnglish = isEnglish,
                    he = "תן הסבר על ${selected.exerciseName ?: selected.title}",
                    en = "Explain ${selected.exerciseName ?: selected.title}"
                )
            }

            AssistantIntent.SEARCH_MATERIAL,
            AssistantIntent.LIST_EXERCISES,
            AssistantIntent.LIST_TOPICS -> {
                localized(
                    isEnglish = isEnglish,
                    he = "חפש בחומר ק.מ.י ${selected.topicName ?: selected.title}",
                    en = "Search KAMI material for ${selected.topicName ?: selected.title}"
                )
            }

            AssistantIntent.NEXT_TRAINING,
            AssistantIntent.LIST_TRAININGS,
            AssistantIntent.USER_TRAINING_DETAILS -> {
                localized(
                    isEnglish = isEnglish,
                    he = "הצג מידע על ${selected.title}",
                    en = "Show information about ${selected.title}"
                )
            }

            else ->
                selected.title
        }
    }

    private fun intentForSource(
        source: AssistantKnowledgeSource
    ): AssistantIntent {
        return when (source) {
            AssistantKnowledgeSource.EXERCISES ->
                AssistantIntent.EXPLAIN_EXERCISE

            AssistantKnowledgeSource.MATERIAL ->
                AssistantIntent.SEARCH_MATERIAL

            AssistantKnowledgeSource.TRAININGS,
            AssistantKnowledgeSource.USER_PROFILE ->
                AssistantIntent.LIST_TRAININGS

            AssistantKnowledgeSource.NAVIGATION ->
                AssistantIntent.NAVIGATION

            AssistantKnowledgeSource.UNKNOWN ->
                AssistantIntent.UNKNOWN
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

    private fun intentLabel(
        intent: AssistantIntent,
        isEnglish: Boolean
    ): String {
        return when (intent) {
            AssistantIntent.EXERCISE,
            AssistantIntent.EXPLAIN_EXERCISE ->
                localized(
                    isEnglish,
                    "הסבר על תרגיל",
                    "Exercise explanation"
                )

            AssistantIntent.SEARCH_EXERCISE ->
                localized(
                    isEnglish,
                    "חיפוש תרגיל",
                    "Exercise search"
                )

            AssistantIntent.LIST_EXERCISES ->
                localized(
                    isEnglish,
                    "רשימת תרגילים",
                    "Exercise list"
                )

            AssistantIntent.MATERIAL,
            AssistantIntent.SEARCH_MATERIAL ->
                localized(
                    isEnglish,
                    "חיפוש בחומר ק.מ.י",
                    "KAMI material search"
                )

            AssistantIntent.LIST_TOPICS ->
                localized(
                    isEnglish,
                    "רשימת נושאים",
                    "Topic list"
                )

            AssistantIntent.NEXT_TRAINING ->
                localized(
                    isEnglish,
                    "האימון הבא",
                    "Next training"
                )

            AssistantIntent.TRAININGS,
            AssistantIntent.LIST_TRAININGS ->
                localized(
                    isEnglish,
                    "רשימת אימונים",
                    "Training list"
                )

            AssistantIntent.USER_TRAINING_DETAILS ->
                localized(
                    isEnglish,
                    "פרטי הקבוצה והאימונים",
                    "Group and training details"
                )

            AssistantIntent.NAVIGATION ->
                localized(
                    isEnglish,
                    "ניווט באפליקציה",
                    "App navigation"
                )

            AssistantIntent.UNKNOWN ->
                localized(
                    isEnglish,
                    "בקשה כללית",
                    "General request"
                )
        }
    }

    private fun intentDescription(
        intent: AssistantIntent,
        isEnglish: Boolean
    ): String {
        return when (intent) {
            AssistantIntent.EXERCISE,
            AssistantIntent.EXPLAIN_EXERCISE,
            AssistantIntent.SEARCH_EXERCISE ->
                localized(
                    isEnglish,
                    "איתור תרגיל והסבר מתוך המאגר",
                    "Find an exercise and its explanation"
                )

            AssistantIntent.MATERIAL,
            AssistantIntent.LIST_EXERCISES,
            AssistantIntent.SEARCH_MATERIAL,
            AssistantIntent.LIST_TOPICS ->
                localized(
                    isEnglish,
                    "חיפוש לפי חגורה, נושא או תת-נושא",
                    "Search by belt, topic, or sub-topic"
                )

            AssistantIntent.TRAININGS,
            AssistantIntent.NEXT_TRAINING,
            AssistantIntent.LIST_TRAININGS,
            AssistantIntent.USER_TRAINING_DETAILS ->
                localized(
                    isEnglish,
                    "מידע אישי על האימונים הקרובים",
                    "Personal information about upcoming trainings"
                )

            AssistantIntent.NAVIGATION ->
                localized(
                    isEnglish,
                    "פתיחת מסך או ביצוע פעולת ניווט",
                    "Open a screen or perform navigation"
                )

            AssistantIntent.UNKNOWN ->
                localized(
                    isEnglish,
                    "ניתוח נוסף של הבקשה",
                    "Further request analysis"
                )
        }
    }

    private fun textSimilarity(
        first: String,
        second: String
    ): Float {
        if (first.isBlank() || second.isBlank()) {
            return 0f
        }

        if (first == second) {
            return 1f
        }

        if (first in second || second in first) {
            return 0.90f
        }

        val firstWords = first
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()

        val secondWords = second
            .split(" ")
            .filter { it.isNotBlank() }
            .toSet()

        if (firstWords.isEmpty() || secondWords.isEmpty()) {
            return 0f
        }

        val intersection =
            firstWords.intersect(secondWords).size.toFloat()

        val union =
            firstWords.union(secondWords).size.toFloat()

        return if (union > 0f) {
            intersection / union
        } else {
            0f
        }
    }

    private fun normalizeSelectionText(
        text: String
    ): String {
        return text
            .lowercase()
            .replace(Regex("[?!,.:;\"'`()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun emptyResolution(
        question: String
    ): AssistantIntentResolution {
        return AssistantIntentResolution(
            originalQuestion = question,
            normalizedQuestion = question.lowercase(),
            resolvedQuestion = question,
            intent = AssistantIntent.UNKNOWN,
            source = AssistantKnowledgeSource.UNKNOWN,
            confidence = 0f,
            requiresClarification = true
        )
    }

    private fun localized(
        isEnglish: Boolean,
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    companion object {
        private const val MAX_INTENT_OPTIONS = 3
        private const val MAX_CLARIFICATION_OPTIONS = 5

        /*
         * רשימות חומר ק.מ.י עשויות להכיל הרבה יותר
         * מחמש אפשרויות.
         */
        private const val MAX_RESULT_SELECTION_OPTIONS = 100

        private const val MIN_SELECTION_SIMILARITY = 0.42f
    }
}