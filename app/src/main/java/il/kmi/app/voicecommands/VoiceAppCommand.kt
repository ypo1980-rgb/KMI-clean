package il.kmi.app.voicecommands

/**
 * יעדים מתוך המגירה הראשית של האפליקציה.
 * הפעולה עצמה תבוצע דרך אותם handlers של AppDrawerContent.
 */
enum class VoiceDrawerDestination {
    MY_PROFILE,

    COACH_ATTENDANCE,
    COACH_BROADCAST,
    COACH_TRAINEES,
    COACH_PAYMENTS_REPORT,
    COACH_INTERNAL_EXAM,

    ADMIN_USERS,
    ADMIN_DIAGNOSTICS,

    ABOUT_AVI,
    NETWORK_COACHES,
    ABOUT_METHOD,
    EXERCISES_DEMO,
    FORMS_AND_PAYMENTS,
    CONTACT_US,
    BRANCH_FORUM,

    LANGUAGE,
    LANGUAGE_HEBREW,
    LANGUAGE_ENGLISH,

    MANAGE_SUBSCRIPTION,
    RATE_US,
    LOGOUT
}

/**
 * פקודות קוליות להפעלת האפליקציה.
 *
 * המנגנון נפרד מעוזר ה־AI:
 * הוא מפעיל פעולות וניווט במקום לחיצה על כפתורים.
 */
sealed interface VoiceAppCommand {

    data object OpenHome : VoiceAppCommand

    data object OpenSettings : VoiceAppCommand

    data object OpenProgress : VoiceAppCommand

    data object OpenTrainings : VoiceAppCommand

    data object OpenTopics : VoiceAppCommand

    data object OpenBelts : VoiceAppCommand

    /*
     * יעדי התפריט המהיר במסכי התרגילים.
     *
     * בדיקת המנוי תתבצע בשכבת הניווט,
     * כדי שפקודה קולית לא תוכל לעקוף מסך נעול.
     */
    data object OpenWeakPoints : VoiceAppCommand

    data object OpenAllLists : VoiceAppCommand

    data object OpenPractice : VoiceAppCommand

    data object OpenTrainingSummary : VoiceAppCommand

    data object OpenVoiceAssistant : VoiceAppCommand

    data class OpenBelt(
        val beltQuery: String
    ) : VoiceAppCommand

    data class OpenTopic(
        val topicQuery: String
    ) : VoiceAppCommand

    data object OpenSearch : VoiceAppCommand

    data object GoBack : VoiceAppCommand

    data class OpenDrawerItem(
        val destination: VoiceDrawerDestination
    ) : VoiceAppCommand

    data class FindAndOpen(
        val query: String
    ) : VoiceAppCommand

    data class ExplainExercise(
        val query: String
    ) : VoiceAppCommand

    data class Search(
        val query: String
    ) : VoiceAppCommand

    data class Unknown(
        val originalText: String
    ) : VoiceAppCommand
}

object VoiceAppCommandParser {

    fun parse(rawText: String): VoiceAppCommand {
        val original = rawText.trim()
        val normalized = normalize(original)

        if (normalized.isBlank()) {
            return VoiceAppCommand.Unknown(original)
        }

        /*
         * יעדי התפריט המהיר נבדקים לפני החיפוש הכללי,
         * פתיחת נושא או פתיחת חגורה.
         *
         * כך למשל "פתח מסך סיכום" לא יטופל בטעות
         * כחיפוש חופשי לפי המילים "מסך סיכום".
         */
        resolveQuickMenuCommand(normalized)?.let { command ->
            return command
        }

        resolveDrawerDestination(normalized)?.let { destination ->
            return VoiceAppCommand.OpenDrawerItem(destination)
        }

        /*
         * בודקים נושא לפני חגורה.
         *
         * כך פקודה כמו:
         * "פתח נושא בעיטות בחגורה כחולה"
         * תישמר כ־OpenTopic עם כל הביטוי,
         * ולא תיעצר כ־OpenBelt בגלל המילה "כחולה".
         */
        extractTopicQuery(normalized)?.let { topicQuery ->
            return VoiceAppCommand.OpenTopic(topicQuery)
        }

        /*
         * אם לא נמצא נושא ורק חגורה הוזכרה,
         * ממשיכים להתנהגות הרגילה של פתיחת החגורה.
         */
        extractBeltQuery(normalized)?.let { beltQuery ->
            return VoiceAppCommand.OpenBelt(beltQuery)
        }

        return when {
            isHomeCommand(normalized) ->
                VoiceAppCommand.OpenHome

            isSettingsCommand(normalized) ->
                VoiceAppCommand.OpenSettings

            isProgressCommand(normalized) ->
                VoiceAppCommand.OpenProgress

            isTrainingsCommand(normalized) ->
                VoiceAppCommand.OpenTrainings

            isTopicsCommand(normalized) ->
                VoiceAppCommand.OpenTopics

            isBeltsCommand(normalized) ->
                VoiceAppCommand.OpenBelts

            isBackCommand(normalized) ->
                VoiceAppCommand.GoBack

            isOpenSearchCommand(normalized) ->
                VoiceAppCommand.OpenSearch

            isExplanationCommand(normalized) -> {
                val query = removeCommandWords(
                    normalized,
                    explanationWords
                )

                if (query.isBlank()) {
                    VoiceAppCommand.OpenSearch
                } else {
                    VoiceAppCommand.ExplainExercise(query)
                }
            }

            isSearchCommand(normalized) -> {
                val query = removeCommandWords(
                    normalized,
                    searchWords
                )

                if (query.isBlank()) {
                    VoiceAppCommand.OpenSearch
                } else {
                    VoiceAppCommand.Search(query)
                }
            }

            isOpenContentCommand(normalized) -> {
                val query = removeCommandWords(
                    normalized,
                    openWords
                )

                if (query.isBlank()) {
                    VoiceAppCommand.Unknown(original)
                } else {
                    VoiceAppCommand.FindAndOpen(query)
                }
            }

            else ->
                VoiceAppCommand.FindAndOpen(normalized)
        }
    }

    /**
     * מזהה פקודות לפתיחת המסכים שמופיעים
     * בתפריט המהיר של מסכי התרגילים.
     */
    private fun resolveQuickMenuCommand(
        text: String
    ): VoiceAppCommand? {
        return when {
            containsAny(
                text,
                "נקודות תורפה",
                "נקודת תורפה",
                "החולשות שלי",
                "התרגילים החלשים שלי",
                "פתח נקודות תורפה",
                "פתח את נקודות התורפה",
                "עבור לנקודות תורפה",
                "תראה לי נקודות תורפה",
                "weak points",
                "my weak points",
                "open weak points",
                "weak exercises"
            ) ->
                VoiceAppCommand.OpenWeakPoints

            containsAny(
                text,
                "כל הרשימות",
                "הרשימות שלי",
                "מסך הרשימות",
                "פתח את כל הרשימות",
                "פתח כל הרשימות",
                "פתח רשימות",
                "תראה לי את כל הרשימות",
                "עבור לכל הרשימות",
                "all lists",
                "my lists",
                "open all lists",
                "lists screen"
            ) ->
                VoiceAppCommand.OpenAllLists

            containsAny(
                text,
                "מסך תרגול",
                "מצב תרגול",
                "פתח תרגול",
                "פתח את התרגול",
                "עבור לתרגול",
                "התחל תרגול",
                "בוא נתרגל",
                "תרגול תרגילים",
                "practice screen",
                "practice mode",
                "open practice",
                "start practice"
            ) ->
                VoiceAppCommand.OpenPractice

            containsAny(
                text,
                "מסך סיכום",
                "סיכום תרגילים",
                "סיכום האימון",
                "סיכום אימון",
                "פתח מסך סיכום",
                "פתח את מסך הסיכום",
                "פתח סיכום",
                "עבור לסיכום",
                "תראה לי סיכום",
                "training summary",
                "summary screen",
                "exercise summary",
                "open summary"
            ) ->
                VoiceAppCommand.OpenTrainingSummary

            containsAny(
                text,
                "עוזר קולי",
                "העוזר הקולי",
                "עוזר אישי",
                "העוזר האישי",
                "פתח עוזר קולי",
                "פתח את העוזר הקולי",
                "פתח עוזר אישי",
                "פתח את העוזר האישי",
                "דבר עם העוזר",
                "voice assistant",
                "personal assistant",
                "open voice assistant",
                "open assistant"
            ) ->
                VoiceAppCommand.OpenVoiceAssistant

            else -> null
        }
    }

    private fun resolveDrawerDestination(
        text: String
    ): VoiceDrawerDestination? {
        return when {
            /*
             * כל משפט שמכיל את המילה "נוכחות"
             * מפנה למסך עדכון נוכחות.
             */
            containsAny(
                text,
                "נוכחות",
                "attendance"
            ) ->
                VoiceDrawerDestination.COACH_ATTENDANCE

            containsAny(
                text,
                "שליחת הודעה",
                "שלח הודעה",
                "שידור מאמן",
                "הודעה למתאמנים",
                "פתח שליחת הודעה",
                "send message",
                "coach broadcast",
                "message trainees"
            ) ->
                VoiceDrawerDestination.COACH_BROADCAST

            containsAny(
                text,
                "רשימת מתאמנים",
                "המתאמנים שלי",
                "פתח רשימת מתאמנים",
                "פתח מתאמנים",
                "מסך המתאמנים",
                "trainees list",
                "my trainees",
                "open trainees",
                "trainees screen"
            ) ->
                VoiceDrawerDestination.COACH_TRAINEES

            /*
 * כל משפט שמכיל "תשלומים" מפנה לדוח התשלומים,
 * למעט "טפסים ותשלומים" שמטופל כיעד נפרד.
 */
            (
                    (
                            text.contains("תשלומים") &&
                                    !text.contains("טפסים") &&
                                    !text.contains("מסמכים")
                            ) ||
                            (
                                    text.contains("payments") &&
                                            !text.contains("forms") &&
                                            !text.contains("documents") &&
                                            !text.contains("payment forms")
                                    ) ||
                            containsAny(
                                text,
                                "דוח תשלום",
                                "דו״ח תשלום",
                                "דוח התשלום",
                                "תשלומי מתאמנים",
                                "payment report",
                                "trainee payment"
                            )
                    ) ->
                VoiceDrawerDestination.COACH_PAYMENTS_REPORT

            containsAny(
                text,
                "מבחן פנימי",
                "מבחן פנימי לחגורה",
                "פתח מבחן פנימי",
                "מבחן חגורה",
                "internal exam",
                "internal belt exam",
                "open internal exam"
            ) ->
                VoiceDrawerDestination.COACH_INTERNAL_EXAM

            /*
             * כל משפט שמבקש ניהול או צפייה במשתמשים
             * מפנה למסך ניהול המשתמשים.
             */
            (
                    (
                            text.contains("משתמש") &&
                                    containsAny(
                                        text,
                                        "ניהול",
                                        "נהל",
                                        "רשימת",
                                        "כל ה",
                                        "מסך",
                                        "פתח",
                                        "תפתח",
                                        "תציג",
                                        "הצג"
                                    )
                            ) ||
                            containsAny(
                                text,
                                "ניהול משתמשים",
                                "משתמשי האפליקציה",
                                "manage users",
                                "user management",
                                "users list",
                                "all users",
                                "users screen",
                                "open users"
                            )
                    ) ->
                VoiceDrawerDestination.ADMIN_USERS

            /*
             * מילות המפתח "בקרה", "לוגים" או "אבחון"
             * מספיקות לזיהוי מסך מרכז הבקרה.
             */
            containsAny(
                text,
                "מרכז בקרה",
                "בקרה",
                "לוגים",
                "לוג",
                "אבחון האפליקציה",
                "נתוני אבחון",
                "מסך אבחון",
                "control center",
                "application logs",
                "app logs",
                "logs screen",
                "app diagnostics",
                "diagnostics"
            ) ->
                VoiceDrawerDestination.ADMIN_DIAGNOSTICS

            containsAny(
                text,
                "הפרופיל שלי",
                "פתח פרופיל",
                "פתח את הפרופיל",
                "הפרטים האישיים שלי",
                "my profile",
                "open my profile",
                "personal profile"
            ) ->
                VoiceDrawerDestination.MY_PROFILE

            /*
             * מנוע זיהוי הדיבור עשוי לתמלל את שם המשפחה
             * כ"אביסידון" או "אביסדון". לכן מספיק לזהות
             * את השם אבי בצירוף אחת מצורות שם המשפחה.
             */
            /*
  * מסך אבי אביסידון מזוהה גם לפי שמו
  * וגם לפי תפקידו כראש השיטה.
  */
            (
                    text.contains("אבי") &&
                            containsAny(
                                text,
                                "אביסידון",
                                "אביסדון",
                                "אביסידאן"
                            )
                    ) ||
                    containsAny(
                        text,
                        "אודות אבי",
                        "מי זה אבי",
                        "ראש השיטה",
                        "מי ראש השיטה",
                        "אודות ראש השיטה",
                        "פתח את ראש השיטה",
                        "about avi avisidon",
                        "about avi avisdon",
                        "avi avisidon",
                        "avi avisdon",
                        "head of the method",
                        "method founder"
                    ) ->
                VoiceDrawerDestination.ABOUT_AVI

            containsAny(
                text,
                "אודות המאמנים ברשת",
                "מאמני הרשת",
                "רשימת המאמנים",
                "מי המאמנים",
                "about network coaches",
                "network coaches",
                "coaches list"
            ) ->
                VoiceDrawerDestination.NETWORK_COACHES

            containsAny(
                text,
                "אודות השיטה",
                "השיטה",
                "שיטת קמי",
                "מה זה קמי",
                "about the method",
                "kami method",
                "about kami"
            ) ->
                VoiceDrawerDestination.ABOUT_METHOD

            containsAny(
                text,
                "תרגילים הדגמה",
                "תרגילי הדגמה",
                "סרטוני הדגמה",
                "הדגמת תרגילים",
                "פתח סרטוני תרגילים",
                "exercises demo",
                "exercise demonstrations",
                "demo videos"
            ) ->
                VoiceDrawerDestination.EXERCISES_DEMO

            containsAny(
                text,
                "טפסים ותשלומים",
                "טפסים",
                "פתח טפסים",
                "מסמכים ותשלומים",
                "forms and payments",
                "open forms",
                "payment forms"
            ) ->
                VoiceDrawerDestination.FORMS_AND_PAYMENTS

            containsAny(
                text,
                "צור קשר",
                "יצירת קשר",
                "פתח צור קשר",
                "דבר עם קמי",
                "contact us",
                "open contact",
                "contact kami"
            ) ->
                VoiceDrawerDestination.CONTACT_US

            containsAny(
                text,
                "פורום הסניף",
                "פורום פנימי",
                "פתח פורום",
                "הפורום",
                "branch forum",
                "internal forum",
                "open forum"
            ) ->
                VoiceDrawerDestination.BRANCH_FORUM

            /*
             * כאשר נאמר שם השפה במפורש, עוברים ישירות
             * אליה בלי קשר לניסוח המלא של המשפט.
             */
            containsAny(
                text,
                "עברית",
                "לעברית",
                "תחזיר לעברית",
                "חזור לעברית",
                "החזר לעברית",
                "תעביר לעברית",
                "ממשק עברי",
                "hebrew",
                "switch to hebrew",
                "change to hebrew",
                "back to hebrew",
                "hebrew language"
            ) ->
                VoiceDrawerDestination.LANGUAGE_HEBREW

            containsAny(
                text,
                "אנגלית",
                "לאנגלית",
                "תעביר לאנגלית",
                "החלף לאנגלית",
                "ממשק אנגלי",
                "english",
                "switch to english",
                "change to english",
                "back to english",
                "english language"
            ) ->
                VoiceDrawerDestination.LANGUAGE_ENGLISH

            /*
             * אם המשתמש ביקש רק להחליף שפה בלי לציין
             * איזו שפה, עוברים לשפה ההפוכה.
             */
            containsAny(
                text,
                "החלף שפה",
                "החלפת שפה",
                "שנה שפה",
                "שינוי שפה",
                "תעביר שפה",
                "תעבור שפה",
                "change language",
                "switch language",
                "language settings"
            ) ->
                VoiceDrawerDestination.LANGUAGE

            containsAny(
                text,
                "ניהול מנוי",
                "המנוי שלי",
                "פתח מנוי",
                "שדרוג מנוי",
                "manage subscription",
                "my subscription",
                "open subscription"
            ) ->
                VoiceDrawerDestination.MANAGE_SUBSCRIPTION

            containsAny(
                text,
                "דרגו אותנו",
                "דירוג האפליקציה",
                "דרג את האפליקציה",
                "תן דירוג",
                "rate us",
                "rate the app",
                "app rating"
            ) ->
                VoiceDrawerDestination.RATE_US

            containsAny(
                text,
                "התנתקות",
                "התנתק",
                "תנתק אותי",
                "יציאה מהחשבון",
                "logout",
                "log out",
                "sign out"
            ) ->
                VoiceDrawerDestination.LOGOUT

            else -> null
        }
    }

    private fun isTopicsCommand(text: String): Boolean =
        containsAny(
            text,
            "תרגילים לפי נושא",
            "תרגילים לפי נושאים",
            "נושאי תרגילים",
            "מסך הנושאים",
            "פתח נושאים",
            "פתח את הנושאים",
            "עבור לנושאים",
            "topics",
            "exercise topics",
            "exercises by topic",
            "topics screen",
            "open topics"
        )
    private val openWords = listOf(
        "פתח",
        "פתחי",
        "תפתח",
        "תפתחי",
        "תראה",
        "תראי",
        "תציג",
        "תציגי",
        "עבור אל",
        "עבור ל",
        "תעבור אל",
        "תעבור ל",
        "תעברי אל",
        "תעברי ל",
        "תגיע אל",
        "תגיע ל",
        "לך אל",
        "לך ל",
        "קח אותי אל",
        "קח אותי ל",
        "open",
        "show",
        "go to",
        "navigate to",
        "take me to"
    )

    private val explanationWords = listOf(
        "תסביר לי את",
        "תסביר לי על",
        "תסביר לי",
        "תסביר את",
        "תסביר על",
        "תסביר",

        "הסבר לי את",
        "הסבר לי על",
        "הסבר לי",
        "הסבר את",
        "הסבר על",
        "הסבר",

        "תן הסבר על",
        "תני הסבר על",

        "מה זה",

        "איך מבצעים את",
        "איך מבצעים",

        "איך עושים את",
        "איך עושים",

        "explain",
        "explain the",
        "tell me about",
        "how to perform",
        "how do i perform"
    )

    private val searchWords = listOf(
        "חפש",
        "חפשי",
        "תחפש",
        "תחפשי",
        "מצא",
        "מצאי",
        "תמצא",
        "תמצאי",
        "search",
        "find",
        "look for"
    )

    private fun isHomeCommand(text: String): Boolean =
        containsAny(
            text,
            "מסך הבית",
            "חזור לבית",
            "חזור למסך הבית",
            "פתח בית",
            "עבור לבית",
            "לך לבית",
            "הביתה",
            "home",
            "open home",
            "home screen",
            "go home"
        )

    private fun isSettingsCommand(text: String): Boolean =
        containsAny(
            text,
            "הגדרות",
            "פתח הגדרות",
            "פתח את ההגדרות",
            "מסך ההגדרות",
            "עבור להגדרות",
            "תעבור להגדרות",
            "לך להגדרות",
            "settings",
            "open settings",
            "settings screen",
            "go to settings"
        )

    private fun isProgressCommand(text: String): Boolean =
        containsAny(
            text,
            "התקדמות",
            "סטטיסטיקה",
            "סטטיסטיקות",
            "ההתקדמות שלי",
            "פתח התקדמות",
            "מסך התקדמות",
            "פתח סטטיסטיקה",
            "פתח סטטיסטיקות",
            "progress",
            "statistics",
            "my progress",
            "open progress",
            "open statistics",
            "progress screen"
        )

    private fun isTrainingsCommand(text: String): Boolean =
        containsAny(
            text,
            "אימונים",
            "יומן אימונים",
            "לוח אימונים",
            "האימונים שלי",
            "אימונים קרובים",
            "פתח אימונים",
            "פתח את האימונים",
            "trainings",
            "training",
            "training calendar",
            "my trainings",
            "open trainings",
            "upcoming trainings"
        )

    private fun isBeltsCommand(text: String): Boolean =
        containsAny(
            text,
            "חגורות",
            "מסך חגורות",
            "רשימת חגורות",
            "תרגילים לפי חגורה",
            "פתח חגורות",
            "פתח את החגורות",
            "belts",
            "open belts",
            "belts screen",
            "belt exercises"
        )

    private fun isBackCommand(text: String): Boolean =
        containsAny(
            text,
            "חזור",
            "חזור אחורה",
            "מסך קודם",
            "go back",
            "previous screen"
        )

    private fun isOpenSearchCommand(text: String): Boolean =
        text == "חיפוש" ||
                text == "פתח חיפוש" ||
                text == "search" ||
                text == "open search"

    private fun isExplanationCommand(text: String): Boolean =
        explanationWords.any { word ->
            text == word || text.startsWith("$word ")
        }

    private fun isSearchCommand(text: String): Boolean =
        searchWords.any { word ->
            text == word || text.startsWith("$word ")
        }

    private fun isOpenContentCommand(text: String): Boolean =
        openWords.any { word ->
            text == word || text.startsWith("$word ")
        }

    private fun removeCommandWords(
        text: String,
        words: List<String>
    ): String {
        var result = text

        words
            .sortedByDescending { it.length }
            .forEach { word ->
                if (result == word) {
                    result = ""
                    return@forEach
                }

                if (result.startsWith("$word ")) {
                    result = result.removePrefix(word).trim()
                    return@forEach
                }
            }

        return result
            .removePrefix("את ")
            .removePrefix("את ה")
            .removePrefix("הנושא ")
            .removePrefix("התרגיל ")
            .trim()
    }

    private fun containsAny(
        text: String,
        vararg candidates: String
    ): Boolean {
        return candidates.any { candidate ->
            text == candidate || text.contains(candidate)
        }
    }

    private fun extractBeltQuery(text: String): String? {
        val beltAliases = listOf(
            "לבנה", "לבן", "white",
            "צהובה", "צהוב", "yellow",
            "כתומה", "כתום", "orange",
            "ירוקה", "ירוק", "green",
            "כחולה", "כחול", "blue",
            "חומה", "חום", "brown",
            "שחורה", "שחור", "black"
        )

        val mentionsBelt =
            text.contains("חגורה") ||
                    text.contains("belt")

        if (!mentionsBelt) return null

        return beltAliases.firstOrNull { alias ->
            text == alias ||
                    text.contains(" $alias") ||
                    text.startsWith("$alias ")
        }
    }

    private fun extractTopicQuery(text: String): String? {
        val prefixes = listOf(
            "תרגילים בנושא",
            "תרגילים לפי נושא",
            "פתח את הנושא",
            "פתח נושא",
            "עבור לנושא",
            "תעבור לנושא",
            "נושא",
            "exercises by topic",
            "exercises about",
            "open topic",
            "go to topic",
            "topic"
        )

        val query = prefixes
            .sortedByDescending { it.length }
            .firstNotNullOfOrNull { prefix ->
                if (text.startsWith("$prefix ")) {
                    text.removePrefix(prefix).trim()
                } else {
                    null
                }
            }
            .orEmpty()

        return query.takeIf {
            it.isNotBlank() &&
                    it != "מסוים" &&
                    it != "מסוים בבקשה"
        }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("ק.מ.י", "קמי")
            .replace("k.a.m.i", "kami")
            .replace("k.m.i", "kami")
            .replace("–", " ")
            .replace("—", " ")
            .replace("-", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}