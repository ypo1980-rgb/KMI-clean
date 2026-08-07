package il.kmi.app.ui.assistant.exercise

import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.domain.ExplanationSearchIndex
import il.kmi.app.ui.assistant.search.ExerciseSearchService
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.content.ExerciseIdentityRegistry

object ExerciseAssistantEngine {

    fun answer(
        question: String,
        preferredBelt: Belt?,
        isEnglish: Boolean
    ): String {
        return try {
            val cleanQuestion = question.trim()

            if (cleanQuestion.isBlank()) {
                return if (isEnglish) {
                    "Write or say the name of an exercise."
                } else {
                    "כתוב או אמור את שם התרגיל."
                }
            }

            val exerciseName = cleanExerciseName(cleanQuestion)

            /*
      * חיפוש כללי מול האינדקס המרכזי.
      *
      * אין כאן שמות קשיחים של תרגילים, סוגי תקיפה,
      * כיוונים או איברי גוף. אותה לוגיקה חלה על כל
      * משפחות התרגילים הקיימות והעתידיות.
      */
            val searchQuestion =
                exerciseName.ifBlank {
                    cleanQuestion
                }

            /*
             * שאלת המשך משוחזרת עשויה להכיל גם את
             * בקשת הספירה הקודמת וגם בקשת רשימה חדשה.
             *
             * לדוגמה:
             * "תן את רשימת התרגילים.
             * כמה תרגילים עם סכין יש בחגורה כחולה"
             *
             * במקרה כזה הפעולה החדשה — הצגת הרשימה —
             * חייבת לקבל עדיפות על פעולת הספירה הישנה.
             */
            val isFamilyListQuestion =
                isExerciseFamilyListQuestion(
                    cleanQuestion
                )

            val isCountQuestion =
                isExerciseCountQuestion(
                    cleanQuestion
                )

            /*
             * בקשת רשימה נבדקת ראשונה.
             */
            if (isFamilyListQuestion) {
                val familyMatches =
                    findExercisesByTopicFamily(
                        query = searchQuestion,
                        preferredBelt = preferredBelt
                    )

                if (familyMatches.isNotEmpty()) {
                    return formatExerciseFamilyList(
                        matches = familyMatches,
                        isEnglish = isEnglish
                    )
                }
            }

            /*
             * ספירה מתבצעת רק כאשר לא קיימת באותה
             * בקשה הוראה חדשה להציג רשימה.
             *
             * כך לא חוזרים בטעות למספר הכולל של
             * התרגילים בחגורה.
             */
            if (
                isCountQuestion &&
                !isFamilyListQuestion &&
                preferredBelt != null
            ) {
                val familyMatches =
                    findExercisesByTopicFamily(
                        query = searchQuestion,
                        preferredBelt = preferredBelt
                    )

                if (familyMatches.isNotEmpty()) {
                    return formatExerciseFamilyCount(
                        matches = familyMatches,
                        belt = preferredBelt,
                        isEnglish = isEnglish
                    )
                }

                return formatExerciseCountAnswer(
                    belt = preferredBelt,
                    isEnglish = isEnglish
                )
            }

            /*
             * בשאלה כללית, למשל "אילו הגנות נגד בעיטות
             * יש בחגורה שלי", אין לצפות להתאמה מלאה לשם
             * של תרגיל יחיד.
             */
            val allIndexedMatches =
                ExplanationSearchIndex.findMatches(
                    query = searchQuestion,
                    preferredBelt = preferredBelt,
                    minScore = 70,
                    maxItems = 20
                )

            /*
             * כאשר ידועה חגורת המשתמש, מעדיפים רשימה
             * שמכילה רק תרגילים מאותה חגורה.
             *
             * אם לא נמצאה אף תוצאה בחגורה, שומרים את
             * התוצאות הכלליות ולא מסתירים מידע אפשרי.
             */
            /*
             * חגורת המשתמש היא העדפה בלבד.
             *
             * מסננים באופן קשיח לפי חגורה רק כאשר
             * המשתמש ציין אותה במפורש בשאלה.
             *
             * כך משתמש בחגורה כתומה עדיין יכול לבקש
             * הסבר על תרגיל שנמצא בחגורה ירוקה.
             */
            val beltWasExplicitlyRequested =
                preferredBelt != null &&
                        questionExplicitlyRequestsBelt(
                            question = cleanQuestion,
                            belt = preferredBelt
                        )

            val beltFilteredMatches =
                if (
                    preferredBelt != null &&
                    beltWasExplicitlyRequested
                ) {
                    allIndexedMatches.filter { match ->
                        match.belt == preferredBelt
                    }
                } else {
                    allIndexedMatches
                }

            /*
             * הפונקציה כבר קיימת בהמשך הקובץ.
             *
             * כאן מפעילים אותה בפועל כדי שהמונח
             * המבחין בשאלה ישמש לסינון הרשימה.
             *
             * לדוגמה:
             * "הגנות נגד בעיטות" ישאיר תוצאות
             * שכותרתן קשורה לבעיטות ולא לידיים.
             */
            val indexedMatches =
                focusMatchesByQueryMeaning(
                    query = searchQuestion,
                    matches = beltFilteredMatches
                )

            /*
             * ציון של 1000 ומעלה מצביע על התאמה מילולית
             * מדויקת לשם רשמי או לכינוי שהוגדר ב־Registry.
             *
             * אם קיימת התאמה מדויקת אחת בעלת הציון הגבוה
             * ביותר, אפשר להציג אותה ישירות.
             *
             * אם אין התאמה מדויקת ויש מספר תוצאות, השאילתה
             * כללית או חלקית ולכן המשתמש חייב לבחור.
             */
            val bestIndexedScore =
                indexedMatches
                    .firstOrNull()
                    ?.score
                    ?: 0

            val bestMatchIsExact =
                bestIndexedScore >= 1000

            val competingMatches =
                if (bestMatchIsExact) {
                    /*
                     * חגורה מועדפת מקבלת תוספת ציון ולכן תכריע
                     * בין אותו שם שמופיע בכמה חגורות.
                     * ללא הכרעה נשאיר את כל התוצאות המובילות.
                     */
                    indexedMatches.filter { match ->
                        match.score == bestIndexedScore
                    }
                } else {
                    /*
                     * בשאילתה חלקית כל ההתאמות רלוונטיות לבחירה,
                     * גם אם אחת מהן קיבלה מעט יותר נקודות.
                     */
                    indexedMatches
                }

            val shouldRequestExerciseChoice =
                competingMatches.size > 1

            if (shouldRequestExerciseChoice) {
                val optionsText =
                    competingMatches
                        .take(8)
                        .mapIndexed { index, match ->
                            val beltText =
                                if (isEnglish) {
                                    match.belt.name
                                        .lowercase()
                                        .replaceFirstChar { character ->
                                            character.uppercase()
                                        }
                                } else {
                                    match.belt.heb
                                }

                            if (isEnglish) {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(match.title)
                                    append(" — ")
                                    append(beltText)
                                    append(" belt")
                                }
                            } else {
                                buildString {
                                    append(index + 1)
                                    append(". ")
                                    append(match.title)
                                    append(" — ")
                                    /*
                                     * belt.heb כבר מחזיר שם מלא,
                                     * לדוגמה: "חגורה כחולה".
                                     */
                                    append(beltText)
                                }
                            }
                        }
                        .joinToString("\n")

                return if (isEnglish) {
                    buildString {
                        appendLine(
                            "I found several exercises that match your request."
                        )
                        appendLine(
                            "Which exercise did you mean?"
                        )
                        appendLine()
                        append(optionsText)
                        appendLine()
                        append(
                            "Say or enter the full exercise name."
                        )
                    }
                } else {
                    buildString {
                        appendLine(
                            "מצאתי מספר תרגילים שמתאימים לבקשה."
                        )
                        appendLine(
                            "לאיזה תרגיל התכוונת?"
                        )
                        appendLine()
                        append(optionsText)
                        appendLine()
                        append(
                            "אמור או הקלד את שמו המלא של התרגיל."
                        )
                    }
                }
            }

            /*
             * תוצאות החיפוש הכללי נשמרות גם עבור מנגנוני
             * הגיבוי בהמשך הפונקציה.
             */
            val initialHits =
                ExerciseSearchService
                    .searchExercisesForQuestion(
                        question = searchQuestion,
                        beltEnum = preferredBelt
                    )

            /*
             * רק בקשה חד־משמעית יכולה להמשיך לחיפוש הסבר.
             */
            if (exerciseName.isNotBlank()) {
                val matchedExercise =
                    ExplanationSearchIndex.findBest(
                        query = exerciseName,
                        preferredBelt = preferredBelt
                    )
                        ?: ExplanationSearchIndex.findBest(
                            query = exerciseName,
                            preferredBelt = null
                        )

                if (matchedExercise != null) {
                    /*
                     * תוצאת החיפוש משמשת רק לזיהוי התרגיל והחגורה.
                     * את ההסבר עצמו שולפים ממאגר ההסברים המקורי
                     * לפי השם שהמשתמש ביקש.
                     */
                    /*
     * קודם פונים ישירות לקובץ Explanations.
     * כך ExerciseIdentityRegistry לא יכול להחליף את התרגיל
     * בתרגיל דומה בעל הסבר אחר.
     */
                    val directExplanation =
                        if (!isEnglish) {
                            Explanations.get(
                                belt = matchedExercise.belt,
                                item = exerciseName
                            ).trim()
                        } else {
                            ""
                        }

                    val directIsFallback =
                        directExplanation.isBlank() ||
                                directExplanation.startsWith("הסבר מפורט על") ||
                                directExplanation.startsWith("אין כרגע")

                    if (!directIsFallback) {
                        return directExplanation
                    }

                    val resolvedExplanation =
                        ExerciseExplanationResolver.get(
                            belt = matchedExercise.belt,
                            topic = "",
                            item = exerciseName,
                            isEnglish = isEnglish
                        ).trim()

                    val isFallback =
                        resolvedExplanation.isBlank() ||
                                resolvedExplanation.startsWith("הסבר מפורט על") ||
                                resolvedExplanation.startsWith("אין כרגע") ||
                                resolvedExplanation.startsWith("Detailed explanation for:") ||
                                resolvedExplanation.startsWith(
                                    "There is currently no explanation"
                                )

                    if (!isFallback) {
                        return resolvedExplanation
                    }

                    /*
                     * אם השם שנאמר הוא כינוי, מנסים שוב עם הכותרת
                     * הקנונית שנמצאה באינדקס.
                     */
                    val canonicalExplanation =
                        ExerciseExplanationResolver.get(
                            belt = matchedExercise.belt,
                            topic = "",
                            item = matchedExercise.title,
                            isEnglish = isEnglish
                        ).trim()

                    val isCanonicalFallback =
                        canonicalExplanation.isBlank() ||
                                canonicalExplanation.startsWith("הסבר מפורט על") ||
                                canonicalExplanation.startsWith("אין כרגע") ||
                                canonicalExplanation.startsWith("Detailed explanation for:") ||
                                canonicalExplanation.startsWith(
                                    "There is currently no explanation"
                                )

                    if (!isCanonicalFallback) {
                        return canonicalExplanation
                    }
                }
            }

            /*
             * אם לא נמצאה התאמה ישירה, מנסים את מנוע
             * ההסברים החכם עם השאלה המלאה.
             */
            val knowledgeAnswer =
                AssistantExerciseExplanationKnowledge.answer(
                    question = cleanQuestion,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            if (knowledgeAnswer != null) {
                return knowledgeAnswer
            }

            /*
  * משתמשים בתוצאות שכבר חושבו בתחילת הבקשה.
  */
            val hits = initialHits

            /*
             * אם לתוצאה הטובה ביותר קיים הסבר,
             * מחזירים אותו ישירות.
             */
            val bestExplanation =
                ExerciseSearchService.buildBestHitExplanation(
                    hits = hits,
                    preferredBelt = preferredBelt,
                    isEnglish = isEnglish
                )
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            if (bestExplanation != null) {
                return bestExplanation
            }

            /*
             * רק כאשר נמצאו כמה תרגילים אך אין התאמה
             * מספקת להסבר, מציגים רשימת אפשרויות.
             */
            val exerciseList =
                ExerciseSearchService.formatHitsAsExerciseList(
                    hits = hits,
                    maxItems = 6,
                    isEnglish = isEnglish
                ).trim()

            if (exerciseList.isNotBlank()) {
                return if (isEnglish) {
                    buildString {
                        appendLine("I found several related exercises:")
                        appendLine()
                        appendLine(exerciseList)
                        appendLine()
                        append("Choose the exact exercise and I’ll explain it.")
                    }
                } else {
                    buildString {
                        appendLine("מצאתי מספר תרגילים קשורים:")
                        appendLine()
                        appendLine(exerciseList)
                        appendLine()
                        append("בחר את התרגיל המדויק ואני אסביר אותו.")
                    }
                }
            }

            if (isEnglish) {
                "I couldn't find a matching exercise."
            } else {
                "לא מצאתי תרגיל מתאים לשאלה."
            }
        } catch (_: Throwable) {
            if (isEnglish) {
                "There was a problem processing the exercise request."
            } else {
                "אירעה תקלה בעיבוד בקשת התרגיל."
            }
        }
    }

    /**
     * בודק אם החגורה הוזכרה במפורש בשאלה.
     *
     * חגורת הפרופיל לבדה אינה נחשבת לבקשת סינון.
     */
    private fun questionExplicitlyRequestsBelt(
        question: String,
        belt: Belt
    ): Boolean {
        val normalizedQuestion =
            question
                .lowercase()
                .replace("־", " ")
                .replace("–", " ")
                .replace("—", " ")
                .replace("-", " ")
                .replace(
                    Regex("""[^\p{L}\p{N}]+"""),
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

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
                .map { value ->
                    value
                        .lowercase()
                        .replace(
                            Regex("""[^\p{L}\p{N}]+"""),
                            " "
                        )
                        .replace(
                            Regex("\\s+"),
                            " "
                        )
                        .trim()
                }
                .filter { value ->
                    value.isNotBlank()
                }

        return beltValues.any { beltValue ->
            beltValue in normalizedQuestion
        }
    }

    /**
     * מזהה שהמשתמש מבקש רשימת תרגילים או משפחה,
     * ולא הסבר לתרגיל יחיד.
     */
    private fun isExerciseFamilyListQuestion(
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

        /*
         * זיהוי לפי שורשי המילים מאפשר לתפוס גם:
         *
         * "רשימת תרגילים"
         * "רשימת התרגילים"
         * "תן את רשימת התרגילים"
         * "הצג רשימת תרגילי סכין"
         *
         * כך אין תלות בכל הטיה אפשרית של המילה תרגיל.
         */
        val hasHebrewExerciseListRequest =
            "רשימ" in normalized &&
                    "תרגיל" in normalized

        if (hasHebrewExerciseListRequest) {
            return true
        }

        return listOf(
            "כל תרגיל",
            "כל התרגיל",
            "כל הגנה",
            "כל ההגנות",
            "איזה תרגיל",
            "אילו תרגיל",
            "איזה הגנות",
            "אילו הגנות",
            "איזה סוג",
            "אילו סוג",
            "מהם התרגילים",
            "מה הם התרגילים",
            "מה הן ההגנות",
            "רשימת תרגיל",
            "רשימת התרגיל",
            "רשימת תרגילים",
            "רשימת התרגילים",
            "רשימת תרגילי",
            "רשימת הגנות",
            "תרגילים יש",
            "הגנות יש",
            "all exercises",
            "all the exercises",
            "which exercises",
            "what exercises",
            "which types",
            "what types",
            "exercise list",
            "list exercises",
            "list of exercises"
        ).any { marker ->
            marker in normalized
        }
    }

    /**
     * מזהה בקשה לספירת כל התרגילים בחגורה,
     * ולא בקשה לשמות של משפחת תרגילים מסוימת.
     */
    private fun isExerciseCountQuestion(
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

        val hasCountMarker =
            listOf(
                "כמה",
                "מספר התרגילים",
                "כמות התרגילים",
                "how many",
                "number of exercises"
            ).any { marker ->
                marker in normalized
            }

        val hasExerciseMarker =
            listOf(
                "תרגיל",
                "תרגילים",
                "exercise",
                "exercises"
            ).any { marker ->
                marker in normalized
            }

        return hasCountMarker && hasExerciseMarker
    }

    /**
     * מחזיר את מספר התרגילים הייחודיים בחגורה.
     */
    private fun formatExerciseCountAnswer(
        belt: Belt,
        isEnglish: Boolean
    ): String {
        val exerciseCount =
            ExerciseIdentityRegistry
                .allKnown()
                .asSequence()
                .filter { identity ->
                    identity.belt == belt
                }
                .distinctBy { identity ->
                    identity.id
                }
                .count()

        return if (isEnglish) {
            val beltName =
                belt.name
                    .lowercase()
                    .replaceFirstChar { character ->
                        character.uppercase()
                    }

            "There are $exerciseCount exercises in the $beltName belt in total."
        } else {
            "ב${belt.heb} יש $exerciseCount תרגילים בסך הכול."
        }
    }

    /**
     * מחזיר את מספר התרגילים במשפחה שהתבקשה,
     * לאחר סינון לפי החגורה.
     */
    private fun formatExerciseFamilyCount(
        matches: List<
                ExerciseIdentityRegistry.ExerciseIdentity
                >,
        belt: Belt,
        isEnglish: Boolean
    ): String {
        val exerciseCount =
            matches
                .distinctBy { identity ->
                    identity.id
                }
                .size

        return if (isEnglish) {
            val beltName =
                belt.name
                    .lowercase()
                    .replaceFirstChar { character ->
                        character.uppercase()
                    }

            "There are $exerciseCount exercises in the requested category in the $beltName belt."
        } else {
            "ב${belt.heb} יש $exerciseCount תרגילים בנושא שביקשת."
        }
    }

    /**
     * מחפש תרגילים לפי שיוך הנושא האמיתי שלהם
     * ב־ExerciseIdentityRegistry.
     */
    private fun findExercisesByTopicFamily(
        query: String,
        preferredBelt: Belt?
    ): List<
            ExerciseIdentityRegistry.ExerciseIdentity
            > {
        /*
         * מסירים רק מילים שמתארות את הבקשה עצמה.
         *
         * המילים "הגנה" ו"הגנות" אינן מילות רעש:
         * הן חיוניות להבחנה בין "בעיטות" לבין
         * "הגנות נגד בעיטות".
         */
        val requestNoiseRoots =
            semanticRoots(
                """
                כל התרגיל התרגילים תרגיל תרגילי תרגילים
                סוג סוגי הסוג הסוגים רשימה
                הצג הציגי תציג תציגי הראה הראי
                תן תני איזה אילו מהם מה הן
                כמה מספר כמות עם בנושא
                יש קיימים קיימות בחגורה חגורה
                all the exercise exercises types list show
                which what available belt
                how many number amount with about
                """.trimIndent()
            )

        val preferredBeltRoots =
            preferredBelt
                ?.let { belt ->
                    semanticRoots(
                        listOf(
                            belt.id,
                            belt.name,
                            belt.heb
                        ).joinToString(" ")
                    )
                }
                .orEmpty()

        val queryRoots =
            semanticRoots(query)
                .minus(requestNoiseRoots)
                .minus(preferredBeltRoots)

        if (queryRoots.isEmpty()) {
            return emptyList()
        }

        val allKnown =
            ExerciseIdentityRegistry.allKnown()

        val beltCandidates =
            preferredBelt
                ?.let { belt ->
                    allKnown.filter { identity ->
                        identity.belt == belt
                    }
                }
                ?: allKnown

        /*
         * לכל תרגיל שומרים את מספר המילים העודפות
         * במפתח הנושא המתאים ביותר.
         *
         * לדוגמה, עבור השאלה "סוגי בעיטות":
         *
         * topicKey "בעיטות"               -> 0 מילים עודפות
         * topicKey "הגנות נגד בעיטות"     -> 2 מילים עודפות
         *
         * לכן יישארו רק תרגילי הבעיטות עצמם.
         *
         * לעומת זאת, עבור "הגנות נגד בעיטות" כל שלוש
         * המילים נדרשות, ולכן יישארו רק ההגנות.
         */
        val candidatesWithSpecificity =
            beltCandidates
                .mapNotNull { identity ->
                    val bestExtraRootCount =
                        identity.topicKeys
                            .mapNotNull { topicKey ->
                                val topicRoots =
                                    semanticRoots(topicKey)

                                /*
                                 * לכל שורש בשאלה בודקים גם גרסה
                                 * ללא אות יחס עברית שצמודה למילה.
                                 *
                                 * לדוגמה:
                                 * בבעיטות -> בעיטות
                                 * בהגנות  -> הגנות
                                 * מסכין   -> סכין
                                 *
                                 * ההתאמה הישירה נבדקת קודם, ולכן
                                 * מילה שהאות הראשונה היא חלק ממנה
                                 * אינה נפגעת.
                                 */
                                val matchedQueryRoots =
                                    queryRoots
                                        .mapNotNull { queryRoot ->
                                            when {
                                                queryRoot in topicRoots ->
                                                    queryRoot

                                                queryRoot.length >= 4 &&
                                                        queryRoot.first() in
                                                        HEBREW_PREFIX_LETTERS &&
                                                        queryRoot
                                                            .drop(1) in
                                                        topicRoots ->
                                                    queryRoot.drop(1)

                                                else ->
                                                    null
                                            }
                                        }
                                        .toSet()

                                if (
                                    matchedQueryRoots.size ==
                                    queryRoots.size
                                ) {
                                    topicRoots
                                        .minus(matchedQueryRoots)
                                        .size
                                } else {
                                    null
                                }
                            }
                            .minOrNull()

                    bestExtraRootCount
                        ?.let { extraRootCount ->
                            identity to extraRootCount
                        }
                }

        val bestSpecificity =
            candidatesWithSpecificity
                .minOfOrNull { (_, extraRootCount) ->
                    extraRootCount
                }
                ?: return emptyList()

        return candidatesWithSpecificity
            .asSequence()
            .filter { (_, extraRootCount) ->
                extraRootCount == bestSpecificity
            }
            .map { (identity, _) ->
                identity
            }
            .distinctBy { identity ->
                identity.id
            }
            .sortedWith(
                compareBy<
                        ExerciseIdentityRegistry.ExerciseIdentity
                        > {
                    it.hebrewTitle.length
                }
                    .thenBy {
                        it.hebrewTitle
                    }
            )
            .toList()
    }

    private fun formatExerciseFamilyList(
        matches: List<
                ExerciseIdentityRegistry.ExerciseIdentity
                >,
        isEnglish: Boolean
    ): String {
        /*
         * בקשת רשימה מפורשת צריכה להציג את כל
         * התרגילים המתאימים ולא לעצור לאחר 15.
         */
        val visibleMatches =
            matches

        val listText =
            visibleMatches
                .mapIndexed { index, identity ->
                    buildString {
                        append(index + 1)
                        append(". ")
                        append(identity.hebrewTitle)
                        append(" — ")

                        if (isEnglish) {
                            append(
                                identity.belt.name
                                    .lowercase()
                                    .replaceFirstChar { character ->
                                        character.uppercase()
                                    }
                            )
                            append(" belt")
                        } else {
                            append(identity.belt.heb)
                        }
                    }
                }
                .joinToString("\n")

        return if (isEnglish) {
            buildString {
                appendLine(
                    "I found ${matches.size} exercises in the requested category:"
                )
                appendLine()
                append(listText)

                if (matches.size > visibleMatches.size) {
                    appendLine()
                    append(
                        "קיימים עוד ${matches.size - visibleMatches.size} תרגילים בנושא."
                    )
                }

                appendLine()
                appendLine()
                append(
                    "Choose an exercise by its number or full name."
                )
            }
        } else {
            buildString {
                appendLine(
                    "מצאתי ${matches.size} תרגילים בנושא שביקשת:"
                )
                appendLine()
                append(listText)

                if (matches.size > visibleMatches.size) {
                    appendLine()
                    append(
                        "קיימים עוד ${matches.size - visibleMatches.size} תרגילים בנושא."
                    )
                }

                appendLine()
                appendLine()
                append(
                    "בחר תרגיל לפי המספר או לפי שמו המלא."
                )
            }
        }
    }

/**
 * משאיר את התוצאות שמתאימות למונח המבחין
    * ביותר בשאלה, בלי להכיר מראש משפחות תרגילים.
     */
    private fun focusMatchesByQueryMeaning(
        query: String,
        matches: List<ExplanationSearchIndex.Match>
    ): List<ExplanationSearchIndex.Match> {
        if (matches.size <= 1) {
            return matches
        }

        val queryRoots =
            semanticRoots(query)

        if (queryRoots.isEmpty()) {
            return matches
        }

        val titleRootsByMatch =
            matches.associateWith { match ->
                semanticRoots(match.title)
            }

        val rootFrequency =
            queryRoots
                .mapNotNull { root ->
                    val count =
                        titleRootsByMatch
                            .values
                            .count { titleRoots ->
                                root in titleRoots
                            }

                    if (count > 0) {
                        root to count
                    } else {
                        null
                    }
                }

        if (rootFrequency.isEmpty()) {
            return matches
        }

        val lowestFrequency =
            rootFrequency.minOf { (_, count) ->
                count
            }

        val distinctiveRoots =
            rootFrequency
                .filter { (_, count) ->
                    count == lowestFrequency
                }
                .map { (root, _) ->
                    root
                }
                .toSet()

        val focusedMatches =
            matches.filter { match ->
                val titleRoots =
                    titleRootsByMatch[match]
                        .orEmpty()

                distinctiveRoots.any { root ->
                    root in titleRoots
                }
            }

        return focusedMatches
            .takeIf { it.isNotEmpty() }
            ?: matches
    }

    private fun semanticRoots(
        value: String
    ): Set<String> {
        return value
            .lowercase()
            /*
             * נרמול גלובלי של הכתיב הישן לכתיב התקני.
             *
             * ההחלפה פועלת גם עם תחיליות:
             * הצואר -> הצוואר
             * בצואר -> בצוואר
             * מחביקת צואר -> מחביקת צוואר
             */
            .replace(
                "צואר",
                "צוואר"
            )
            .replace("\u200f", "")
            .replace("\u200e", "")
            .replace("\u00a0", " ")
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
                semanticRoot(token)
                    .takeIf { root ->
                        root.length >= 3
                    }
            }
            .toSet()
    }

    private fun semanticRoot(
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
     * מסיר מהשאלה ביטויי בקשה כלליים ומשאיר את שם התרגיל.
     *
     * לדוגמה:
     * "הסבר מניעת חניקה" -> "מניעת חניקה"
     * "תן לי הסבר על בעיטת מגל" -> "בעיטת מגל"
     */
    /**
     * אותיות יחס שעשויות להיות מחוברות לתחילת מילה
     * בזיהוי דיבור או בהקלדה בעברית.
     */
    private val HEBREW_PREFIX_LETTERS =
        setOf(
            'ב',
            'ל',
            'כ',
            'מ',
            'ה',
            'ו',
            'ש'
        )

    private fun cleanExerciseName(
        question: String
    ): String {
        return question
            /*
             * ביטויי שאלה כלליים אינם חלק משם התרגיל
             * או ממשפחת התרגילים שאותה מחפשים.
             */
            .replace("איזה תרגילים יש", " ")
            .replace("אילו תרגילים יש", " ")
            .replace("איזה תרגילים", " ")
            .replace("אילו תרגילים", " ")
            .replace("איזה הגנות יש", "הגנות ")
            .replace("אילו הגנות יש", "הגנות ")
            .replace("איזה", " ")
            .replace("אילו", " ")
            .replace("בחגורה שלי", " ")
            .replace("מהחגורה שלי", " ")
            .replace("של החגורה שלי", " ")
            .replace("יש לי", " ")
            .replace("קיימים", " ")
            .replace("קיימות", " ")

            .replace("תן לי הסבר על", " ")
            .replace("תני לי הסבר על", " ")
            .replace("אפשר הסבר על", " ")
            .replace("אני רוצה הסבר על", " ")
            .replace("תסביר לי על", " ")
            .replace("תסביר על", " ")
            .replace("הסבר על", " ")
            .replace("הסבר לתרגיל", " ")
            .replace("הסבר תרגיל", " ")
            .replace("הסבר", " ")
            .replace("איך מבצעים את", " ")
            .replace("איך מבצעים", " ")
            .replace("איך עושים את", " ")
            .replace("איך עושים", " ")

            .replace(
                "which exercises are in my belt",
                " ",
                ignoreCase = true
            )
            .replace(
                "what exercises are in my belt",
                " ",
                ignoreCase = true
            )
            .replace(
                "in my belt",
                " ",
                ignoreCase = true
            )
            .replace(
                "explain the exercise",
                " ",
                ignoreCase = true
            )
            .replace(
                "give me an explanation of",
                " ",
                ignoreCase = true
            )
            .replace(
                "give me an explanation for",
                " ",
                ignoreCase = true
            )
            .replace(
                "explain how to do",
                " ",
                ignoreCase = true
            )
            .replace(
                "how do i perform",
                " ",
                ignoreCase = true
            )
            .replace(
                "how to perform",
                " ",
                ignoreCase = true
            )
            .replace(
                "how do i do",
                " ",
                ignoreCase = true
            )
            .replace(
                "explain",
                " ",
                ignoreCase = true
            )
            .replace("\"", " ")
            .replace("'", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}